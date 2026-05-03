package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object DeviceSettingsSearchPolicy {
    data class MatchResult(
        val hasMatch: Boolean,
        val hasNicknameMatch: Boolean,
        val titleOrNicknamePriority: Int,
        val fieldPriority: Int,
    )

    fun evaluateMatch(
        setting: DeviceSetting,
        query: SearchQueryContext,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): MatchResult {
        val nickname = nicknameCache[setting.id]
        val titleOrNicknamePriority = matcher.match(setting.title, query, nickname)
        val hasNicknameMatch = matchingNicknameIds.contains(setting.id)

        val fieldPriority =
            matcher.matchAny(
                query,
                setting.title,
                setting.description.orEmpty(),
                setting.keywords.joinToString(" "),
            )

        return MatchResult(
            hasMatch =
                matcher.isMatch(titleOrNicknamePriority) ||
                    matcher.isMatch(fieldPriority) ||
                    hasNicknameMatch,
            hasNicknameMatch = hasNicknameMatch,
            titleOrNicknamePriority = titleOrNicknamePriority,
            fieldPriority = fieldPriority,
        )
    }

    fun rankingPriority(matchResult: MatchResult): Int {
        return minOf(
            matchResult.titleOrNicknamePriority,
            matchResult.fieldPriority + 2,
        )
    }

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        title: String,
        description: String?,
        keywords: List<String>,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (query.tokens.size <= 1) return true

        val normalizedTitle = SearchTextNormalizer.normalizeForSearch(title)
        val normalizedSupportingText =
            SearchTextNormalizer.normalizeForSearch(
                buildString {
                    append(description.orEmpty())
                    if (keywords.isNotEmpty()) {
                        append(' ')
                        append(keywords.joinToString(" "))
                    }
                    if (!nickname.isNullOrBlank()) {
                        append(' ')
                        append(nickname)
                    }
                },
            )

        return query.tokens.all { token ->
            isTokenCovered(
                token = token,
                normalizedPrimary = normalizedTitle,
                normalizedSecondary = normalizedSupportingText,
                fuzzyMinScore = fuzzyMinScore,
                fuzzyMaxEditDistance = fuzzyMaxEditDistance,
            )
        }
    }

    private fun isTokenCovered(
        token: String,
        normalizedPrimary: String,
        normalizedSecondary: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (normalizedPrimary.contains(token)) return true
        if (!normalizedSecondary.isNullOrBlank() && normalizedSecondary.contains(token)) return true

        val fuzzyScore =
            FuzzyMatcher.score(
                query = token,
                primaryTarget = normalizedPrimary,
                secondaryTarget = normalizedSecondary,
                maxEditDistance = fuzzyMaxEditDistance,
            )
        return fuzzyScore >= fuzzyMinScore
    }
}
