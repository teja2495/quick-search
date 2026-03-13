package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object AppSettingsSearchAlgorithm {
    private const val FUZZY_MIN_SCORE = 78

    fun search(
        fullList: List<AppSettingResult>,
        queryContext: SearchQueryContext,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
        enableFuzzyMatching: Boolean = false,
    ): List<AppSettingResult> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val exactMatches =
            fullList
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

        if (!enableFuzzyMatching) return exactMatches

        val exactIds = exactMatches.map { it.id }.toSet()
        val fuzzyMatches =
            fullList
                .asSequence()
                .filterNot { exactIds.contains(it.id) }
                .mapNotNull { setting ->
                    val fuzzyScore =
                        FuzzyMatcher.score(
                            query = queryContext.normalizedQuery,
                            primaryTarget = SearchTextNormalizer.normalizeForSearch(setting.title),
                            secondaryTarget =
                                SearchTextNormalizer.normalizeForSearch(
                                    buildString {
                                        append(setting.description.orEmpty())
                                        if (setting.keywords.isNotEmpty()) {
                                            append(' ')
                                            append(setting.keywords.joinToString(" "))
                                        }
                                    },
                                ),
                        )
                    if (fuzzyScore < FUZZY_MIN_SCORE) {
                        null
                    } else {
                        setting to fuzzyScore
                    }
                }.sortedWith(
                    compareByDescending<Pair<AppSettingResult, Int>> { it.second }
                        .thenBy { it.first.title.lowercase() },
                ).map { it.first }
                .toList()

        return (exactMatches + fuzzyMatches).take(resultLimit)
    }
}
