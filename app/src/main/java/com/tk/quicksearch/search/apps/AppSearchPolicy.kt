package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

object AppSearchPolicy {
    // Keep in sync with SearchRankingUtils priority semantics:
    // 1 = startsWith, 2 = wordStartsWith, 3 = contains, 4 = no match.
    private const val INITIALS_MATCH_PRIORITY = 3

    fun matchPriority(
        appName: String,
        nickname: String?,
        query: SearchQueryContext,
        initials: List<String> = emptyList(),
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Int {
        val basePriority = matcher.match(primaryText = appName, query = query, nickname = nickname)
        if (matcher.isMatch(basePriority) || initials.isEmpty()) return basePriority

        val initialsPriority = matcher.matchAny(query, *initials.toTypedArray())
        if (!matcher.isMatch(initialsPriority)) return basePriority
        return INITIALS_MATCH_PRIORITY
    }

    fun hasMatch(
        priority: Int,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): Boolean = matcher.isMatch(priority)

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        appName: String,
        nickname: String?,
        initials: List<String>,
        fuzzySearchStrategy: FuzzyAppSearchStrategy,
    ): Boolean {
        if (query.tokens.size <= 1) return true
        return query.tokens.all { token ->
            fuzzySearchStrategy.isTokenCoveredByApp(token, appName, nickname, initials)
        }
    }
}
