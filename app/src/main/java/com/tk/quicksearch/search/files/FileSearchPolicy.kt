package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileSearchTextNormalizer
import com.tk.quicksearch.search.utils.NicknameUtils
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTokenCoveragePolicy

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

        val normalizedName = FileSearchTextNormalizer.normalizeForFileSearch(displayName)
        val queryTokens = FileSearchTextNormalizer.queryTokens(fileQuery)
        // File normalization turns commas into spaces, so split the aliases before normalizing.
        val aliases = NicknameUtils.split(nickname)
        if (aliases.isEmpty()) {
            return SearchRankingUtils.calculateMatchPriorityWithNickname(
                primaryText = normalizedName,
                nickname = null,
                normalizedQuery = fileQuery,
                queryTokens = queryTokens,
            )
        }
        return aliases.minOf { alias ->
            SearchRankingUtils.calculateMatchPriorityWithNickname(
                primaryText = normalizedName,
                nickname = FileSearchTextNormalizer.normalizeForFileSearch(alias),
                normalizedQuery = fileQuery,
                queryTokens = queryTokens,
            )
        }
    }

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        displayName: String,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
    ): Boolean {
        return SearchTokenCoveragePolicy.areAllTokensCovered(
            query = query,
            primaryText = displayName,
            supportingText = NicknameUtils.searchText(nickname),
            fuzzyMinScore = fuzzyMinScore,
            fuzzyMaxEditDistance = fuzzyMaxEditDistance,
        )
    }

    /**
     * Same result as [areAllQueryTokensCovered], with [normalize] standing in for
     * [com.tk.quicksearch.search.utils.SearchTextNormalizer.normalizeForSearch] so callers can reuse
     * normalized names across keystrokes.
     */
    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        displayName: String,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
        normalize: (String) -> String,
    ): Boolean {
        if (query.tokens.size <= 1) return true
        return SearchTokenCoveragePolicy.areAllTokensCoveredNormalized(
            query = query,
            normalizedPrimary = normalize(displayName),
            normalizedSupporting =
                NicknameUtils.searchText(nickname)
                    ?.takeIf { it.isNotBlank() }
                    ?.let(normalize),
            fuzzyMinScore = fuzzyMinScore,
            fuzzyMaxEditDistance = fuzzyMaxEditDistance,
        )
    }
}
