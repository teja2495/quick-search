package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

object ContactSearchPolicy {
    fun matchPriority(
        displayName: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int = matcher.match(primaryText = displayName, query = query, nickname = nickname)
}
