package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.fuzzy.BaseFuzzySearchStrategy
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicy
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.fuzzy.FuzzySearchStrategy
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.NicknameUtils
import com.tk.quicksearch.search.utils.PreparedSearchText
import com.tk.quicksearch.search.utils.SearchTextCache
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.search.utils.SearchQueryContext

data class PreparedAppFuzzyQuery(
    val query: String,
    val normalizedQuery: PreparedSearchText,
    val policy: FuzzySearchPolicy,
)

/**
 * Fuzzy search strategy specifically for app search.
 * Handles fuzzy matching of app names and nicknames.
 */
class FuzzyAppSearchStrategy(
    override val config: FuzzySearchConfig,
    private val isLowRamDevice: Boolean = false,
    private val isFuzzySearchEnabled: () -> Boolean = { true },
    private val textCache: SearchTextCache = SearchTextCache(),
) : BaseFuzzySearchStrategy<AppInfo>() {
    private val subsequenceCache = AppNameSubsequence.Cache()

    /**
     * Finds fuzzy matches for apps based on the query.
     * Searches both app names and nicknames.
     */
    override fun findMatches(
        query: String,
        candidates: List<AppInfo>,
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        return findMatchesWithNicknames(query, candidates) { null }
    }

    /**
     * Creates matches with nickname support.
     * This is the main method AppSearchManager will use.
     */
    fun findMatchesWithNicknames(
        query: String,
        candidates: List<AppInfo>,
        nicknameProvider: (AppInfo) -> String?,
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        val preparedQuery = prepareQuery(query)
        val policy = preparedQuery.policy
        if (!policy.enabled) return emptyList()
        val candidateCount = minOf(candidates.size, policy.candidateLimit)

        return FuzzySearchPerformanceLogger.measure(
            section = SearchSection.APPS,
            query = query,
            candidateCount = candidateCount,
        ) {
            candidates
                .asSequence()
                .take(candidateCount)
                .mapNotNull { app -> computeMatch(preparedQuery, app, nicknameProvider(app)) }
                .sortedByDescending { it.score }
                .toList()
        }
    }

    fun computeMatch(
        query: String,
        app: AppInfo,
        nickname: String?,
        initials: List<String> = emptyList(),
    ): FuzzySearchStrategy.Match<AppInfo>? =
        computeMatch(prepareQuery(query), app, nickname, initials)

    private fun computeMatch(
        preparedQuery: PreparedAppFuzzyQuery,
        app: AppInfo,
        nickname: String?,
        initials: List<String> = emptyList(),
    ): FuzzySearchStrategy.Match<AppInfo>? {
        if (!preparedQuery.policy.enabled) return null
        val alternateNames = buildAlternateNames(app.searchAliases, nickname, initials)
        val subsequenceScore = computeSubsequenceScore(preparedQuery, app.appName, app.searchAliases, nickname)
        if (subsequenceScore == 0 && !isWithinTypoTolerance(preparedQuery, app.appName, alternateNames)) {
            return null
        }
        return scoreEligibleCandidate(preparedQuery, app, alternateNames, subsequenceScore)
    }

    internal fun prepareQuery(query: String): PreparedAppFuzzyQuery =
        PreparedAppFuzzyQuery(
            query = query,
            normalizedQuery = SearchTextNormalizer.prepareForSearch(query.trim()),
            policy = appPolicyFor(query),
        )

    internal fun prepareQuery(query: SearchQueryContext): PreparedAppFuzzyQuery =
        PreparedAppFuzzyQuery(
            query = query.normalizedQuery,
            normalizedQuery = query.preparedQuery,
            policy = appPolicyFor(query.normalizedQuery),
        )

    internal fun prepareNormalizedToken(token: String): PreparedAppFuzzyQuery =
        PreparedAppFuzzyQuery(
            query = token,
            normalizedQuery = SearchTextNormalizer.prepareNormalizedForSearch(token),
            policy = appPolicyFor(token),
        )

    internal fun isTypoEligibleCandidate(
        preparedQuery: PreparedAppFuzzyQuery,
        appName: String,
        searchAliases: List<String> = emptyList(),
        nickname: String?,
        initials: List<String> = emptyList(),
    ): Boolean {
        if (!preparedQuery.policy.enabled) return false
        val alternateNames = buildAlternateNames(searchAliases, nickname, initials)
        return isWithinTypoTolerance(
            preparedQuery,
            appName,
            alternateNames,
        ) || computeSubsequenceScore(preparedQuery, appName, searchAliases, nickname) > 0
    }

    internal fun scoreEligibleCandidate(
        preparedQuery: PreparedAppFuzzyQuery,
        app: AppInfo,
        searchAliases: List<String> = emptyList(),
        nickname: String?,
        initials: List<String> = emptyList(),
    ): FuzzySearchStrategy.Match<AppInfo>? =
        scoreEligibleCandidate(
            preparedQuery = preparedQuery,
            app = app,
            alternateNames = buildAlternateNames(searchAliases, nickname, initials),
            subsequenceScore = computeSubsequenceScore(preparedQuery, app.appName, searchAliases, nickname),
        )

    private fun scoreEligibleCandidate(
        preparedQuery: PreparedAppFuzzyQuery,
        app: AppInfo,
        alternateNames: String?,
        subsequenceScore: Int,
    ): FuzzySearchStrategy.Match<AppInfo>? {
        val policy = preparedQuery.policy
        if (!policy.enabled) return null
        val score = maxOf(computeScore(preparedQuery, app.appName, alternateNames), subsequenceScore)
        return if (score >= policy.minimumScore) {
            FuzzySearchStrategy.Match(
                item = app,
                score = score,
                priority = config.priority,
                isFuzzyMatch = true,
            )
        } else {
            null
        }
    }

    fun isTokenCoveredByApp(
        preparedToken: PreparedAppFuzzyQuery,
        appName: String,
        searchAliases: List<String> = emptyList(),
        nickname: String?,
        initials: List<String> = emptyList(),
    ): Boolean {
        val tokenLower = preparedToken.normalizedQuery.normalized
        val nameLower = textCache.prepare(appName).normalized
        if (nameLower.contains(tokenLower)) return true
        if (
            searchAliases.any {
                textCache.prepare(it).normalized.contains(tokenLower)
            }
        ) {
            return true
        }
        nickname?.let { nick ->
            if (textCache.prepare(nick).normalized.contains(tokenLower)) return true
        }
        if (initials.any { it.contains(tokenLower) }) return true

        val alternateNames =
            searchAliases.asSequence()
                .plus(NicknameUtils.split(nickname).asSequence())
                .plus(initials.asSequence())
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .ifBlank { null }
        val policy = preparedToken.policy
        if (!policy.enabled) return false
        val score = computeScore(preparedToken, appName, alternateNames)
        return score >= policy.minimumScore && isWithinTypoTolerance(preparedToken, appName, alternateNames)
    }

    private fun buildAlternateNames(
        searchAliases: List<String>,
        nickname: String?,
        initials: List<String>,
    ): String? =
        searchAliases.asSequence()
            .plus(NicknameUtils.split(nickname).asSequence())
            .plus(initials.asSequence())
            .filter { it.isNotBlank() }
            .joinToString(separator = " ")
            .ifBlank { null }

    private fun computeSubsequenceScore(
        preparedQuery: PreparedAppFuzzyQuery,
        appName: String,
        searchAliases: List<String>,
        nickname: String?,
    ): Int {
        val query = preparedQuery.normalizedQuery
        if (query.fuzzyTokens.size != 1) return 0
        var bestScore = AppNameSubsequence.score(query.compact, subsequenceCache.prepare(appName))
        for (name in searchAliases.asSequence().plus(NicknameUtils.split(nickname).asSequence())) {
            bestScore = maxOf(bestScore, AppNameSubsequence.score(query.compact, subsequenceCache.prepare(name)))
        }
        return bestScore
    }

    private fun isWithinTypoTolerance(
        preparedQuery: PreparedAppFuzzyQuery,
        appName: String,
        alternateNames: String?,
    ): Boolean {
        val query = preparedQuery.query
        val policy = preparedQuery.policy
        if (query.length < policy.minimumQueryLength) return true
        val normalizedQuery = preparedQuery.normalizedQuery.normalized
        val normalizedAppName = textCache.prepare(appName).normalized
        if (
            FuzzyMatcher.hasTokenWithinEditDistance(
                normalizedQuery,
                normalizedAppName,
                policy.maximumEditDistance,
            )
        ) {
            return true
        }
        return alternateNames?.let {
            FuzzyMatcher.hasTokenWithinEditDistance(
                normalizedQuery,
                textCache.prepare(it).normalized,
                policy.maximumEditDistance,
            )
        } ?: false
    }

    private fun appPolicyFor(query: String): FuzzySearchPolicy =
        FuzzySearchPolicyResolver.effectivePolicy(
            section = SearchSection.APPS,
            query = query,
            isLowRamDevice = isLowRamDevice,
        ).let { policy ->
            policy.copy(enabled = policy.enabled && !isLowRamDevice && isFuzzySearchEnabled())
        }

    private fun computeScore(
        preparedQuery: PreparedAppFuzzyQuery,
        appName: String,
        alternateNames: String?,
    ): Int =
        engine.computeScore(
            query = preparedQuery.normalizedQuery,
            queryLength = preparedQuery.query.trim().length,
            target = textCache.prepare(appName),
            nickname = alternateNames?.let(textCache::prepare),
            minQueryLength = preparedQuery.policy.minimumQueryLength,
        )
}
