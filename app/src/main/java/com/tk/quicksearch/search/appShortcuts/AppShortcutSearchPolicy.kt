package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object AppShortcutSearchPolicy {
    fun matchPriority(
        displayName: String,
        appLabel: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int {
        val displayNamePriority = matcher.match(displayName, query, nickname)
        val appLabelPriority = matcher.match(appLabel, query)
        val combinedPriority =
            matcher.matchAny(
                query,
                "$displayName $appLabel",
                "$appLabel $displayName",
                *buildCombinedNicknameFields(nickname, appLabel),
            )
        return minOf(displayNamePriority, appLabelPriority, combinedPriority)
    }

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        displayName: String,
        appLabel: String,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (query.tokens.size <= 1) return true

        val normalizedDisplayName = SearchTextNormalizer.normalizeForSearch(displayName)
        val normalizedSupportingText =
            SearchTextNormalizer.normalizeForSearch(
                listOfNotNull(appLabel, nickname).joinToString(" "),
            )

        return query.tokens.all { token ->
            isTokenCovered(
                token = token,
                normalizedPrimary = normalizedDisplayName,
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

    private fun buildCombinedNicknameFields(
        nickname: String?,
        appLabel: String,
    ): Array<String> {
        val normalizedNickname = nickname?.trim().orEmpty()
        if (normalizedNickname.isBlank()) return emptyArray()
        return arrayOf(
            "$normalizedNickname $appLabel",
            "$appLabel $normalizedNickname",
        )
    }
}
