package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

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
}
