package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object AppSettingsSearchPolicy {
    data class MatchResult(
        val hasMatch: Boolean,
        val titlePriority: Int,
        val fieldPriority: Int,
    )

    fun evaluateMatch(
        setting: AppSettingResult,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): MatchResult {
        val titlePriority = matcher.match(setting.title, query)
        val fieldPriority =
            matcher.matchAny(
                query,
                setting.title,
                setting.keywords.joinToString(" "),
            )

        return MatchResult(
            hasMatch = matcher.isMatch(titlePriority) || matcher.isMatch(fieldPriority),
            titlePriority = titlePriority,
            fieldPriority = fieldPriority,
        )
    }

    fun rankingPriority(matchResult: MatchResult): Int =
        minOf(
            matchResult.titlePriority,
            matchResult.fieldPriority + 2,
        )

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        title: String,
        description: String?,
        keywords: List<String>,
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
