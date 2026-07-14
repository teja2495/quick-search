package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.PhoneNumberUtils
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object ContactSearchPolicy {
    fun matchPriority(
        displayName: String,
        nickname: String?,
        query: SearchQueryContext,
        phoneNumbers: List<String> = emptyList(),
        allowNumberSearch: Boolean = false,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int {
        val namePriority = matcher.match(primaryText = displayName, query = query, nickname = nickname)
        if (!allowNumberSearch || query.normalizedQuery.none(Char::isDigit)) return namePriority

        val queryDigits = PhoneNumberUtils.extractDigits(query.normalizedQuery)
        if (queryDigits.isEmpty()) return namePriority
        val digitQuery = SearchQueryContext.fromRawQuery(queryDigits)
        val phonePriority =
            matcher.matchAny(
                digitQuery,
                *phoneNumbers.map(PhoneNumberUtils::extractDigits).toTypedArray(),
            )
        return minOf(namePriority, phonePriority)
    }

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        displayName: String,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        if (query.tokens.size <= 1) return true

        val normalizedDisplayName = SearchTextNormalizer.normalizeForSearch(displayName)
        val normalizedNickname = nickname?.let(SearchTextNormalizer::normalizeForSearch)

        return query.tokens.all { token ->
            isTokenCovered(
                token = token,
                normalizedPrimary = normalizedDisplayName,
                normalizedSecondary = normalizedNickname,
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
