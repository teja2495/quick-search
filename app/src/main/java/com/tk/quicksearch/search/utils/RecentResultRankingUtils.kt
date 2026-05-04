package com.tk.quicksearch.search.utils

import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import java.util.Locale

object RecentResultRankingUtils {
    data class RecencyIndex(
        val contactScores: Map<Long, Int> = emptyMap(),
        val fileScores: Map<String, Int> = emptyMap(),
        val settingScores: Map<String, Int> = emptyMap(),
        val appShortcutScores: Map<String, Int> = emptyMap(),
        val appSettingScores: Map<String, Int> = emptyMap(),
        val noteScores: Map<Long, Int> = emptyMap(),
    )

    fun buildRecencyIndex(entries: List<RecentSearchEntry>): RecencyIndex {
        if (entries.isEmpty()) return RecencyIndex()

        val contactScores = LinkedHashMap<Long, Int>()
        val fileScores = LinkedHashMap<String, Int>()
        val settingScores = LinkedHashMap<String, Int>()
        val appShortcutScores = LinkedHashMap<String, Int>()
        val appSettingScores = LinkedHashMap<String, Int>()
        val noteScores = LinkedHashMap<Long, Int>()

        val maxScore = entries.size
        entries.forEachIndexed { index, entry ->
            val recencyScore = maxScore - index
            when (entry) {
                is RecentSearchEntry.Contact -> contactScores.putIfAbsent(entry.contactId, recencyScore)
                is RecentSearchEntry.File -> fileScores.putIfAbsent(entry.uri, recencyScore)
                is RecentSearchEntry.Setting -> settingScores.putIfAbsent(entry.id, recencyScore)
                is RecentSearchEntry.AppShortcut ->
                    appShortcutScores.putIfAbsent(entry.shortcutKey, recencyScore)
                is RecentSearchEntry.AppSetting -> appSettingScores.putIfAbsent(entry.id, recencyScore)
                is RecentSearchEntry.Note -> noteScores.putIfAbsent(entry.noteId, recencyScore)
                is RecentSearchEntry.Query -> Unit
            }
        }

        return RecencyIndex(
            contactScores = contactScores,
            fileScores = fileScores,
            settingScores = settingScores,
            appShortcutScores = appShortcutScores,
            appSettingScores = appSettingScores,
            noteScores = noteScores,
        )
    }

    fun <T, K> matchThenRecencyThenAlphabeticalComparator(
        recencyScores: Map<K, Int>,
        keySelector: (T) -> K,
        labelSelector: (T) -> String,
    ): Comparator<Pair<T, Int>> =
        compareBy<Pair<T, Int>> { it.second }
            .thenByDescending { recencyScores[keySelector(it.first)] ?: 0 }
            .thenBy { labelSelector(it.first).lowercase(Locale.getDefault()) }
}
