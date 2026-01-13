package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for recent search queries.
 */
class RecentQueriesPreferences(context: Context) : BasePreferences(context) {

    companion object {
        private const val MAX_RECENT_QUERIES = 10
    }

    // ============================================================================
    // Recent Queries Preferences
    // ============================================================================

    /**
     * Get the list of recent search queries (up to 10).
     * Returns an empty list if no recent queries exist.
     */
    fun getRecentQueries(): List<String> {
        return getStringListPref(KEY_RECENT_QUERIES)
    }

    /**
     * Add a new recent query to the list.
     * Maintains only the last 10 queries, with the newest first.
     * Duplicates are moved to the front rather than being added again.
     */
    fun addRecentQuery(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        val currentQueries = getRecentQueries().toMutableList()
        
        // Remove if it already exists (we'll add it to the front)
        currentQueries.remove(trimmedQuery)
        
        // Add to the front
        currentQueries.add(0, trimmedQuery)
        
        // Keep only the last MAX_RECENT_QUERIES
        val limitedQueries = currentQueries.take(MAX_RECENT_QUERIES)
        
        setStringListPref(KEY_RECENT_QUERIES, limitedQueries)
    }

    /**
     * Clear all recent queries.
     */
    fun clearRecentQueries() {
        setStringListPref(KEY_RECENT_QUERIES, emptyList())
    }

    /**
     * Delete a specific recent query from the list.
     */
    fun deleteRecentQuery(query: String) {
        val currentQueries = getRecentQueries().toMutableList()
        currentQueries.remove(query)
        setStringListPref(KEY_RECENT_QUERIES, currentQueries)
    }

    /**
     * Check if recent queries feature is enabled.
     * Default is true.
     */
    fun areRecentQueriesEnabled(): Boolean {
        return getBooleanPref(KEY_RECENT_QUERIES_ENABLED, true)
    }

    /**
     * Set whether recent queries feature is enabled.
     */
    fun setRecentQueriesEnabled(enabled: Boolean) {
        setBooleanPref(KEY_RECENT_QUERIES_ENABLED, enabled)
    }

    /**
     * Get the maximum number of recent queries to show.
     * Default is 3.
     */
    fun getRecentQueriesCount(): Int {
        return prefs.getInt(KEY_RECENT_QUERIES_COUNT, 3)
    }

    /**
     * Set the maximum number of recent queries to show.
     */
    fun setRecentQueriesCount(count: Int) {
        prefs.edit().putInt(KEY_RECENT_QUERIES_COUNT, count).apply()
    }
}
