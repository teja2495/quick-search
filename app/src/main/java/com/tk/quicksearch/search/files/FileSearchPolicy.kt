package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileSearchTextNormalizer
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

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
}
