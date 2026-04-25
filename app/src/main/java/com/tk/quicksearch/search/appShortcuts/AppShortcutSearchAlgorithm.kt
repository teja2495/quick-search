package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

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
        enableFuzzyMatching: Boolean = false,
        isLowRamDevice: Boolean = false,
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
            enableFuzzyMatching = enableFuzzyMatching,
            isLowRamDevice = isLowRamDevice,
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
        enableFuzzyMatching: Boolean = false,
        isLowRamDevice: Boolean = false,
    ): List<StaticShortcut> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val searchableShortcuts =
            fullList
                .asSequence()
                .filterNot { excludedIds.contains(shortcutKey(it)) }
                .filterNot { disabledIds.contains(shortcutKey(it)) }
                .toList()

        val exactMatches = searchableShortcuts
            .asSequence()
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

        if (!enableFuzzyMatching) return exactMatches

        val fuzzyPolicy =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APP_SHORTCUTS,
                query = queryContext.normalizedQuery,
                isLowRamDevice = isLowRamDevice,
            )
        if (!fuzzyPolicy.enabled) return exactMatches

        val exactMatchIds = exactMatches.map { shortcutKey(it) }.toSet()
        val fuzzyCandidateCount = minOf(searchableShortcuts.size, fuzzyPolicy.candidateLimit)
        val fuzzyMatches =
            FuzzySearchPerformanceLogger.measure(
                section = SearchSection.APP_SHORTCUTS,
                query = queryContext.normalizedQuery,
                candidateCount = fuzzyCandidateCount,
            ) {
            searchableShortcuts
                .asSequence()
                .take(fuzzyCandidateCount)
                .filterNot { exactMatchIds.contains(shortcutKey(it)) }
                .mapNotNull { shortcut ->
                    val shortcutId = shortcutKey(shortcut)
                    val normalizedDisplayName =
                        SearchTextNormalizer.normalizeForSearch(shortcutDisplayName(shortcut))
                    val normalizedSupportingText =
                        SearchTextNormalizer.normalizeForSearch(
                            listOfNotNull(
                                shortcut.appLabel,
                                shortcutNicknames[shortcutId],
                            ).joinToString(" "),
                        )
                    val fuzzyScore =
                        FuzzyMatcher.score(
                            query = queryContext.normalizedQuery,
                            primaryTarget = normalizedDisplayName,
                            secondaryTarget = normalizedSupportingText,
                            maxEditDistance = fuzzyPolicy.maximumEditDistance,
                        )
                    if (fuzzyScore < fuzzyPolicy.minimumScore) {
                        null
                    } else {
                        shortcut to fuzzyScore
                    }
                }.sortedWith(
                    compareByDescending<Pair<StaticShortcut, Int>> { it.second }
                        .thenBy { shortcutDisplayName(it.first).lowercase() },
                ).map { it.first }
                .toList()
            }

        return (exactMatches + fuzzyMatches).take(resultLimit)
    }
}
