package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.NicknameUtils
import com.tk.quicksearch.search.utils.SearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import com.tk.quicksearch.search.utils.SearchTokenCoveragePolicy

/**
 * Keeps each shortcut's combined "name + app" search fields between queries, so typing reuses the
 * same strings (and their cached hashes) instead of rebuilding them for every shortcut. Clear it
 * whenever the shortcut list is reloaded.
 */
class AppShortcutCombinedFieldsCache {
    private class Entry(
        val displayName: String,
        val appLabel: String,
        val nickname: String?,
        val fields: Array<String>,
    )

    private val entries = java.util.concurrent.ConcurrentHashMap<String, Entry>()

    fun get(
        shortcutKey: String,
        displayName: String,
        appLabel: String,
        nickname: String?,
    ): Array<String> {
        entries[shortcutKey]?.let { entry ->
            if (entry.displayName == displayName && entry.appLabel == appLabel && entry.nickname == nickname) {
                return entry.fields
            }
        }
        val fields = AppShortcutSearchPolicy.buildCombinedFields(displayName, appLabel, nickname)
        entries[shortcutKey] = Entry(displayName, appLabel, nickname, fields)
        return fields
    }

    fun clear() = entries.clear()
}

object AppShortcutSearchPolicy {
    fun matchPriority(
        displayName: String,
        appLabel: String,
        nickname: String?,
        query: SearchQueryContext,
        matcher: SearchMatcher = DefaultSearchMatcher,
        combinedFields: Array<String>? = null,
    ): Int {
        val displayNamePriority = matcher.match(displayName, query, nickname)
        val appLabelPriority = matcher.match(appLabel, query)
        val combinedPriority =
            matcher.matchAny(
                query,
                *(combinedFields ?: buildCombinedFields(displayName, appLabel, nickname)),
            )
        return minOf(displayNamePriority, appLabelPriority, combinedPriority)
    }

    internal fun buildCombinedFields(
        displayName: String,
        appLabel: String,
        nickname: String?,
    ): Array<String> =
        arrayOf(
            "$displayName $appLabel",
            "$appLabel $displayName",
            *buildCombinedNicknameFields(nickname, appLabel),
        )

    fun areAllQueryTokensCovered(
        query: SearchQueryContext,
        displayName: String,
        appLabel: String,
        nickname: String?,
        fuzzyMinScore: Int,
        fuzzyMaxEditDistance: Int,
        textCache: SearchTextCache? = null,
    ): Boolean {
        val supportingText = listOfNotNull(appLabel, NicknameUtils.searchText(nickname)).joinToString(" ")
        if (textCache != null) {
            return SearchTokenCoveragePolicy.areAllTokensCovered(
                query = query,
                primaryText = textCache.prepare(displayName),
                supportingText = supportingText.takeIf { it.isNotBlank() }?.let(textCache::prepare),
                fuzzyMinScore = fuzzyMinScore,
                fuzzyMaxEditDistance = fuzzyMaxEditDistance,
            )
        }
        return SearchTokenCoveragePolicy.areAllTokensCovered(
            query = query,
            primaryText = displayName,
            supportingText = supportingText,
            fuzzyMinScore = fuzzyMinScore,
            fuzzyMaxEditDistance = fuzzyMaxEditDistance,
        )
    }

    private fun buildCombinedNicknameFields(
        nickname: String?,
        appLabel: String,
    ): Array<String> {
        val aliases = NicknameUtils.split(nickname)
        if (aliases.isEmpty()) return emptyArray()
        return aliases
            .flatMap { alias -> listOf("$alias $appLabel", "$appLabel $alias") }
            .toTypedArray()
    }
}
