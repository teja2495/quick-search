package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.search.searchHistory.RecentSearchEntry

/**
 * Stores a bounded list of recently opened non-query search results for ranking boosts.
 */
class RecentResultOpensPreferences(
    context: android.content.Context,
) : BasePreferences(context) {
    fun getRecentResultOpens(): List<RecentSearchEntry> {
        val rawItems =
            PreferenceUtils.getStringListPref(
                sessionPrefs,
                BasePreferences.KEY_RECENT_RESULT_OPENS,
            )
        return rawItems
            .mapNotNull { RecentSearchEntry.fromRaw(it) }
            .filter(::isRankableEntry)
    }

    fun addRecentResultOpen(entry: RecentSearchEntry) {
        if (!isRankableEntry(entry)) return

        val currentItems = getRecentResultOpens().toMutableList()
        currentItems.removeAll { it.stableKey == entry.stableKey }
        currentItems.add(0, entry)

        val limited = currentItems.take(MAX_RECENT_RESULT_OPENS)
        PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_RESULT_OPENS,
            limited.map { it.toJsonString() },
        )
    }

    fun deleteRecentResultOpen(entry: RecentSearchEntry) {
        val currentItems = getRecentResultOpens().toMutableList()
        currentItems.removeAll { it.stableKey == entry.stableKey }
        PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_RESULT_OPENS,
            currentItems.map { it.toJsonString() },
        )
    }

    private fun isRankableEntry(entry: RecentSearchEntry): Boolean =
        entry is RecentSearchEntry.Contact ||
            entry is RecentSearchEntry.File ||
            entry is RecentSearchEntry.Setting ||
            entry is RecentSearchEntry.AppSetting ||
            entry is RecentSearchEntry.Note ||
            entry is RecentSearchEntry.AppShortcut

    companion object {
        private const val MAX_RECENT_RESULT_OPENS = 100
    }
}
