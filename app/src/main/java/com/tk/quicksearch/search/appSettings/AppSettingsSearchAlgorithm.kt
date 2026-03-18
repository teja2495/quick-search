package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext

object AppSettingsSearchAlgorithm {
    fun search(
        fullList: List<AppSettingResult>,
        queryContext: SearchQueryContext,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
    ): List<AppSettingResult> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        return fullList
            .asSequence()
            .mapNotNull { setting ->
                val matchResult =
                    AppSettingsSearchPolicy.evaluateMatch(
                        setting = setting,
                        query = queryContext,
                    )
                if (!matchResult.hasMatch) return@mapNotNull null
                val priority = AppSettingsSearchPolicy.rankingPriority(matchResult)
                setting to priority
            }.sortedWith(
                RecentResultRankingUtils.matchThenRecencyThenAlphabeticalComparator(
                    recencyScores = recentSettingScores,
                    keySelector = { it.id },
                    labelSelector = { it.title },
                ),
            ).take(resultLimit)
            .map { it.first }
            .toList()
    }
}
