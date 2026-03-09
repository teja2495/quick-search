package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

object FileSearchPolicy {
    fun matchPriority(
        displayName: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int = matcher.match(primaryText = displayName, query = query, nickname = nickname)
}
