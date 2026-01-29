package com.tk.quicksearch.search.recentSearches

import android.content.Context
import com.tk.quicksearch.search.data.preferences.BasePreferences

/**
 * Preferences for recent search queries.
 */
class RecentSearchesPreferences(
    context: Context,
) : BasePreferences(context) {
    companion object {
        private const val MAX_RECENT_QUERIES = 10
    }

    // ============================================================================
    // Recent Queries Preferences
    // ============================================================================

    /**
     * Get the list of recent search entries (up to 10).
     * Returns an empty list if no recent entries exist.
     */
    fun getRecentItems(): List<RecentSearchEntry> {
        val rawItems =
            com.tk.quicksearch.search.data.preferences.PreferenceUtils.getStringListPref(
                sessionPrefs,
                BasePreferences.KEY_RECENT_QUERIES,
            )
        return rawItems.mapNotNull { RecentSearchEntry.fromRaw(it) }
    }

    /**
     * Add a new recent item to the list.
     * Maintains only the last 10 items, with the newest first.
     * Duplicates are moved to the front rather than being added again.
     */
    fun addRecentItem(entry: RecentSearchEntry) {
        if (entry is RecentSearchEntry.Query && entry.trimmedQuery.isBlank()) return

        val currentItems = getRecentItems().toMutableList()

        // Remove if it already exists (we'll add it to the front)
        currentItems.removeAll { it.stableKey == entry.stableKey }

        // Add to the front
        currentItems.add(0, entry)

        // Keep only the last MAX_RECENT_QUERIES
        val limitedItems = currentItems.take(MAX_RECENT_QUERIES)

        val serialized = limitedItems.map { it.toJsonString() }
        com.tk.quicksearch.search.data.preferences.PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_QUERIES,
            serialized,
        )
    }

    /**
     * Clear all recent queries.
     */
    fun clearRecentQueries() {
        com.tk.quicksearch.search.data.preferences.PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_QUERIES,
            emptyList<String>(),
        )
    }

    /**
     * Delete a specific recent item from the list.
     */
    fun deleteRecentItem(entry: RecentSearchEntry) {
        val currentItems = getRecentItems().toMutableList()
        currentItems.removeAll { it.stableKey == entry.stableKey }
        val serialized = currentItems.map { it.toJsonString() }
        com.tk.quicksearch.search.data.preferences.PreferenceUtils.setStringListPref(
            sessionPrefs,
            BasePreferences.KEY_RECENT_QUERIES,
            serialized,
        )
    }

    /**
     * Check if recent queries feature is enabled.
     * Default is true.
     */
    fun areRecentQueriesEnabled(): Boolean = getBooleanPref(BasePreferences.KEY_RECENT_QUERIES_ENABLED, true)

    /**
     * Set whether recent queries feature is enabled.
     */
    fun setRecentQueriesEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_RECENT_QUERIES_ENABLED, enabled)
    }

    /**
     * Get the maximum number of recent queries to show.
     * Default is 3.
     */
    fun getRecentQueriesCount(): Int = prefs.getInt(BasePreferences.KEY_RECENT_QUERIES_COUNT, 3)

    /**
     * Set the maximum number of recent queries to show.
     */
    fun setRecentQueriesCount(count: Int) {
        prefs.edit().putInt(BasePreferences.KEY_RECENT_QUERIES_COUNT, count).apply()
    }
}
