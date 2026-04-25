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
import com.tk.quicksearch.search.utils.SearchTextNormalizer

/**
 * Fuzzy search strategy specifically for app search.
 * Handles fuzzy matching of app names and nicknames.
 */
class FuzzyAppSearchStrategy(
    override val config: FuzzySearchConfig,
    private val isLowRamDevice: Boolean = false,
) : BaseFuzzySearchStrategy<AppInfo>() {
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
        val policy = appPolicyFor(query)
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
                .mapNotNull { app -> computeMatch(query, app, nicknameProvider(app)) }
                .sortedByDescending { it.score }
                .toList()
        }
    }

    fun computeMatch(
        query: String,
        app: AppInfo,
        nickname: String?,
        initials: List<String> = emptyList(),
    ): FuzzySearchStrategy.Match<AppInfo>? {
        val policy = appPolicyFor(query)
        if (!policy.enabled) return null
        val alternateNames =
            sequenceOf(nickname)
                .filterNotNull()
                .plus(initials.asSequence())
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .ifBlank { null }
        val score = engine.computeScore(query, app.appName, alternateNames, policy.minimumQueryLength)
        return if (score >= policy.minimumScore && isWithinTypoTolerance(query, app.appName, alternateNames, policy)) {
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

    fun canUseFuzzySearch(query: String): Boolean = appPolicyFor(query).enabled

    fun fuzzyCandidateLimitFor(query: String): Int = appPolicyFor(query).candidateLimit

    fun isTokenCoveredByApp(
        token: String,
        appName: String,
        nickname: String?,
        initials: List<String> = emptyList(),
    ): Boolean {
        val tokenLower = SearchTextNormalizer.normalizeForSearch(token)
        val nameLower = SearchTextNormalizer.normalizeForSearch(appName)
        if (nameLower.contains(tokenLower)) return true
        nickname?.let { nick ->
            if (SearchTextNormalizer.normalizeForSearch(nick).contains(tokenLower)) return true
        }
        if (initials.any { it.contains(tokenLower) }) return true

        val alternateNames =
            sequenceOf(nickname)
                .filterNotNull()
                .plus(initials.asSequence())
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .ifBlank { null }
        val policy = appPolicyFor(token)
        if (!policy.enabled) return false
        val score = engine.computeScore(token, appName, alternateNames, policy.minimumQueryLength)
        return score >= policy.minimumScore && isWithinTypoTolerance(token, appName, alternateNames, policy)
    }

    private fun isWithinTypoTolerance(
        query: String,
        appName: String,
        alternateNames: String?,
        policy: FuzzySearchPolicy,
    ): Boolean {
        if (query.length < policy.minimumQueryLength) return true
        if (FuzzyMatcher.hasTokenWithinEditDistance(query, appName, policy.maximumEditDistance)) {
            return true
        }
        return alternateNames?.let {
            FuzzyMatcher.hasTokenWithinEditDistance(query, it, policy.maximumEditDistance)
        } ?: false
    }

    private fun appPolicyFor(query: String): FuzzySearchPolicy =
        FuzzySearchPolicyResolver.effectivePolicy(
            section = SearchSection.APPS,
            query = query,
            isLowRamDevice = isLowRamDevice,
        )
}
