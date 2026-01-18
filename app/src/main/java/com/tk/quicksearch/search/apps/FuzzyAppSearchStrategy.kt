package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.BaseFuzzySearchStrategy
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.fuzzy.FuzzySearchStrategy
import com.tk.quicksearch.search.models.AppInfo

/**
 * Fuzzy search strategy specifically for app search.
 * Handles fuzzy matching of app names and nicknames.
 */
class FuzzyAppSearchStrategy(
    override val config: FuzzySearchConfig
) : BaseFuzzySearchStrategy<AppInfo>() {

    /**
     * Finds fuzzy matches for apps based on the query.
     * Searches both app names and nicknames.
     */
    override fun findMatches(
        query: String,
        candidates: List<AppInfo>
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        if (!config.enabled || query.isBlank()) return emptyList()

        return candidates.filterByFuzzySearch(query) { app ->
            // Get nickname from app (this would be provided by AppSearchManager)
            val nickname = getAppNickname(app)
            engine.computeScore(query, app.appName, nickname, config.minQueryLength)
        }
    }

    /**
     * Gets the nickname for an app.
     * This method would typically access cached nicknames from AppSearchManager.
     * For now, returns null - AppSearchManager will provide nicknames.
     */
    private fun getAppNickname(app: AppInfo): String? {
        // This will be overridden or nicknames will be passed in
        // when AppSearchManager uses this strategy
        return null
    }

    /**
     * Creates matches with nickname support.
     * This is the main method AppSearchManager will use.
     */
    fun findMatchesWithNicknames(
        query: String,
        candidates: List<AppInfo>,
        nicknameProvider: (AppInfo) -> String?
    ): List<FuzzySearchStrategy.Match<AppInfo>> {
        if (!config.enabled || query.isBlank()) return emptyList()

        return candidates.mapNotNull { app ->
            val nickname = nicknameProvider(app)
            val score = engine.computeScore(query, app.appName, nickname, config.minQueryLength)
            if (score >= config.matchThreshold) {
                FuzzySearchStrategy.Match(
                    item = app,
                    score = score,
                    priority = config.priority,
                    isFuzzyMatch = true
                )
            } else {
                null
            }
        }.sortedByDescending { it.score }
    }
}