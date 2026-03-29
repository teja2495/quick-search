package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext

object AppShortcutSearchAlgorithm {
    fun search(
        fullList: List<StaticShortcut>,
        query: String,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        shortcutNicknames: Map<String, String>,
        recentShortcutScores: Map<String, Int> = emptyMap(),
        minQueryLength: Int = 1,
        resultLimit: Int = 25,
    ): List<StaticShortcut> {
        if (fullList.isEmpty()) return emptyList()
        val trimmed = query.trim()
        if (trimmed.length < minQueryLength) return emptyList()
        return search(
            fullList = fullList,
            queryContext = SearchQueryContext.fromRawQuery(trimmed),
            excludedIds = excludedIds,
            disabledIds = disabledIds,
            shortcutNicknames = shortcutNicknames,
            recentShortcutScores = recentShortcutScores,
            resultLimit = resultLimit,
        )
    }

    fun search(
        fullList: List<StaticShortcut>,
        queryContext: SearchQueryContext,
        excludedIds: Set<String>,
        disabledIds: Set<String>,
        shortcutNicknames: Map<String, String>,
        recentShortcutScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
    ): List<StaticShortcut> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        return fullList
            .asSequence()
            .filterNot { excludedIds.contains(shortcutKey(it)) }
            .filterNot { disabledIds.contains(shortcutKey(it)) }
            .mapNotNull { shortcut ->
                val shortcutId = shortcutKey(shortcut)
                val displayName = shortcutDisplayName(shortcut)
                val nickname = shortcutNicknames[shortcutId]
                val priority =
                    AppShortcutSearchPolicy.matchPriority(
                        displayName = displayName,
                        appLabel = shortcut.appLabel,
                        nickname = nickname,
                        query = queryContext,
                    )

                if (!DefaultSearchMatcher.isMatch(priority)) {
                    null
                } else {
                    shortcut to priority
                }
            }.sortedWith(
                RecentResultRankingUtils.matchThenRecencyThenAlphabeticalComparator(
                    recencyScores = recentShortcutScores,
                    keySelector = { shortcutKey(it) },
                    labelSelector = { shortcutDisplayName(it) },
                ),
            ).take(resultLimit)
            .map { it.first }
            .toList()
    }
}
