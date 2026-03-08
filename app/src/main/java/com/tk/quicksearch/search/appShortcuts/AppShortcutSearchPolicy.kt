package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

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
