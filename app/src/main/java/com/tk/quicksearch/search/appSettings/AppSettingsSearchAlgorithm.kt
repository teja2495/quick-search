package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object AppSettingsSearchAlgorithm {
    fun search(
        fullList: List<AppSettingResult>,
        queryContext: SearchQueryContext,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
        enableFuzzyMatching: Boolean = false,
        isLowRamDevice: Boolean = false,
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

        val fuzzyPolicy =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APP_SETTINGS,
                query = queryContext.normalizedQuery,
                isLowRamDevice = isLowRamDevice,
            )
        if (!fuzzyPolicy.enabled) return exactMatches

        val exactMatchIds = exactMatches.map { it.id }.toSet()
        val fuzzyCandidateCount = minOf(fullList.size, fuzzyPolicy.candidateLimit)
        val fuzzyMatches =
            FuzzySearchPerformanceLogger.measure(
                section = SearchSection.APP_SETTINGS,
                query = queryContext.normalizedQuery,
                candidateCount = fuzzyCandidateCount,
            ) {
            fullList
                .asSequence()
                .take(fuzzyCandidateCount)
                .filterNot { exactMatchIds.contains(it.id) }
                .mapNotNull { setting ->
                    val normalizedTitle = SearchTextNormalizer.normalizeForSearch(setting.title)
                    val normalizedSupportingText =
                        SearchTextNormalizer.normalizeForSearch(
                            buildString {
                                append(setting.description.orEmpty())
                                if (setting.keywords.isNotEmpty()) {
                                    append(' ')
                                    append(setting.keywords.joinToString(" "))
                                }
                            },
                        )
                    val fuzzyScore =
                        FuzzyMatcher.score(
                            query = queryContext.normalizedQuery,
                            primaryTarget = normalizedTitle,
                            secondaryTarget = normalizedSupportingText,
                            maxEditDistance = fuzzyPolicy.maximumEditDistance,
                        )
                    if (fuzzyScore < fuzzyPolicy.minimumScore) {
                        null
                    } else {
                        setting to fuzzyScore
                    }
                }.sortedWith(
                    compareByDescending<Pair<AppSettingResult, Int>> { it.second }
                        .thenBy { it.first.title.lowercase() },
                ).map { it.first }
                .toList()
            }

        return (exactMatches + fuzzyMatches).take(resultLimit)
    }
}
