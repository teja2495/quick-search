package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileSearchTextNormalizer
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer

object FileSearchPolicy {
    fun matchPriority(
        displayName: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int {
        if (matcher != DefaultSearchMatcher) {
            return matcher.match(primaryText = displayName, query = query, nickname = nickname)
        }

        val fileQuery = FileSearchTextNormalizer.normalizeForFileSearch(query.normalizedQuery)
        if (fileQuery.isBlank()) {
            return SearchRankingUtils.calculateMatchPriority("", fileQuery, emptyList())
        }

        return SearchRankingUtils.calculateMatchPriorityWithNickname(
            primaryText = FileSearchTextNormalizer.normalizeForFileSearch(displayName),
            nickname = nickname?.let { FileSearchTextNormalizer.normalizeForFileSearch(it) },
            normalizedQuery = fileQuery,
            queryTokens = FileSearchTextNormalizer.queryTokens(fileQuery),
        )
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
