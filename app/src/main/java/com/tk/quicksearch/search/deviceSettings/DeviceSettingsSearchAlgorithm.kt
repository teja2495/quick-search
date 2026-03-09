package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.RecentResultRankingUtils

object DeviceSettingsSearchAlgorithm {
    fun search(
        fullList: List<DeviceSetting>,
        query: String,
        excludedIds: Set<String>,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
    ): List<DeviceSetting> {
        if (fullList.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        return search(
            fullList = fullList,
            queryContext = SearchQueryContext.fromRawQuery(trimmed),
            excludedIds = excludedIds,
            matchingNicknameIds = matchingNicknameIds,
            nicknameCache = nicknameCache,
            recentSettingScores = recentSettingScores,
            resultLimit = resultLimit,
        )
    }

    fun search(
        fullList: List<DeviceSetting>,
        queryContext: SearchQueryContext,
        excludedIds: Set<String>,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
    ): List<DeviceSetting> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val settingsToSearch = fullList.filterNot { excludedIds.contains(it.id) }

        return settingsToSearch
            .asSequence()
            .mapNotNull { shortcut ->
                val matchResult =
                    DeviceSettingsSearchPolicy.evaluateMatch(
                        setting = shortcut,
                        query = queryContext,
                        matchingNicknameIds = matchingNicknameIds,
                        nicknameCache = nicknameCache,
                    )
                if (!matchResult.hasMatch) return@mapNotNull null

                val priority =
                    DeviceSettingsSearchPolicy.rankingPriority(matchResult)
                shortcut to priority
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
