package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.BaseFuzzySearchStrategy
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.fuzzy.FuzzySearchStrategy
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchTextNormalizer

private const val MAX_APP_FUZZY_EDIT_DISTANCE = 2

/**
 * Fuzzy search strategy specifically for app search.
 * Handles fuzzy matching of app names and nicknames.
 */
class FuzzyAppSearchStrategy(
    override val config: FuzzySearchConfig,
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
        if (query.isBlank()) return emptyList()

        return candidates
            .mapNotNull { app -> computeMatch(query, app, nicknameProvider(app)) }
            .sortedByDescending { it.score }
    }

    fun computeMatch(
        query: String,
        app: AppInfo,
        nickname: String?,
        initials: List<String> = emptyList(),
    ): FuzzySearchStrategy.Match<AppInfo>? {
        if (query.isBlank()) return null
        val alternateNames =
            sequenceOf(nickname)
                .filterNotNull()
                .plus(initials.asSequence())
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .ifBlank { null }
        val score = engine.computeScore(query, app.appName, alternateNames, config.minQueryLength)
        return if (score >= config.matchThreshold && isWithinTypoTolerance(query, app.appName, alternateNames)) {
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
        val score = engine.computeScore(token, appName, alternateNames, config.minQueryLength)
        return score >= config.matchThreshold && isWithinTypoTolerance(token, appName, alternateNames)
    }

    private fun isWithinTypoTolerance(
        query: String,
        appName: String,
        alternateNames: String?,
    ): Boolean {
        if (query.length < config.minQueryLength) return true
        if (FuzzyMatcher.hasTokenWithinEditDistance(query, appName, MAX_APP_FUZZY_EDIT_DISTANCE)) {
            return true
        }
        return alternateNames?.let {
            FuzzyMatcher.hasTokenWithinEditDistance(query, it, MAX_APP_FUZZY_EDIT_DISTANCE)
        } ?: false
    }
}
