package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.searchHistory.SearchHistoryPreferences

/**
 * Facade for recent searches preference operations
 */
class RecentSearchesPreferencesFacade(
    private val recentSearchesPreferences: SearchHistoryPreferences
) {
    fun getRecentItems(): List<com.tk.quicksearch.search.searchHistory.RecentSearchEntry> =
            recentSearchesPreferences.getRecentItems()

    fun addRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) =
            recentSearchesPreferences.addRecentItem(entry)

    fun clearRecentQueries() = recentSearchesPreferences.clearRecentQueries()

    fun deleteRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) =
            recentSearchesPreferences.deleteRecentItem(entry)

    fun areRecentQueriesEnabled(): Boolean = recentSearchesPreferences.areRecentQueriesEnabled()

    fun setRecentQueriesEnabled(enabled: Boolean) =
            recentSearchesPreferences.setRecentQueriesEnabled(enabled)
}