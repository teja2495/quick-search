package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext

object DeviceSettingsSearchPolicy {
    data class MatchResult(
        val hasMatch: Boolean,
        val hasNicknameMatch: Boolean,
        val titleOrNicknamePriority: Int,
        val fieldPriority: Int,
    )

    fun evaluateMatch(
        setting: DeviceSetting,
        query: SearchQueryContext,
        matchingNicknameIds: Set<String>,
        nicknameCache: Map<String, String?>,
        matcher: SearchMatcher = DefaultSearchMatcher,
    ): MatchResult {
        val nickname = nicknameCache[setting.id]
        val titleOrNicknamePriority = matcher.match(setting.title, query, nickname)
        val hasNicknameMatch = matchingNicknameIds.contains(setting.id)

        val fieldPriority =
            matcher.matchAny(
                query,
                setting.title,
                setting.description.orEmpty(),
                setting.keywords.joinToString(" "),
            )

        return MatchResult(
            hasMatch =
                matcher.isMatch(titleOrNicknamePriority) ||
                    matcher.isMatch(fieldPriority) ||
                    hasNicknameMatch,
            hasNicknameMatch = hasNicknameMatch,
            titleOrNicknamePriority = titleOrNicknamePriority,
            fieldPriority = fieldPriority,
        )
    }

    fun rankingPriority(matchResult: MatchResult): Int {
        return minOf(
            matchResult.titleOrNicknamePriority,
            matchResult.fieldPriority + 2,
        )
    }
}
