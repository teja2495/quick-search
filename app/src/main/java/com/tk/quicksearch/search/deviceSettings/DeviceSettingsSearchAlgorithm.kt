package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.fuzzy.FuzzySearchPerformanceLogger
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object DeviceSettingsSearchAlgorithm {
    fun search(
        fullList: List<DeviceSetting>,
        query: String,
        excludedIds: Set<String>,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        recentSettingScores: Map<String, Int> = emptyMap(),
        resultLimit: Int = 25,
        enableFuzzyMatching: Boolean = false,
        isLowRamDevice: Boolean = false,
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
            enableFuzzyMatching = enableFuzzyMatching,
            isLowRamDevice = isLowRamDevice,
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
        enableFuzzyMatching: Boolean = false,
        isLowRamDevice: Boolean = false,
    ): List<DeviceSetting> {
        if (fullList.isEmpty()) return emptyList()
        if (queryContext.normalizedQuery.isBlank()) return emptyList()

        val settingsToSearch = fullList.filterNot { excludedIds.contains(it.id) }

        val exactMatches =
            settingsToSearch
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

        if (!enableFuzzyMatching) return exactMatches

        val fuzzyPolicy =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.SETTINGS,
                query = queryContext.normalizedQuery,
                isLowRamDevice = isLowRamDevice,
            )
        if (!fuzzyPolicy.enabled) return exactMatches

        val exactMatchIds = exactMatches.map { it.id }.toSet()
        val fuzzyCandidateCount = minOf(settingsToSearch.size, fuzzyPolicy.candidateLimit)
        val fuzzyMatches =
            FuzzySearchPerformanceLogger.measure(
                section = SearchSection.SETTINGS,
                query = queryContext.normalizedQuery,
                candidateCount = fuzzyCandidateCount,
            ) {
            settingsToSearch
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
                                val nickname = nicknameCache[setting.id]
                                if (!nickname.isNullOrBlank()) {
                                    append(' ')
                                    append(nickname)
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
                    compareByDescending<Pair<DeviceSetting, Int>> { it.second }
                        .thenBy { it.first.title.lowercase() },
                ).map { it.first }
                .toList()
            }

        return (exactMatches + fuzzyMatches).take(resultLimit)
    }
}
