package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for search engine-related settings such as disabled engines and ordering.
 */
class SearchEnginePreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean {
        return prefs.contains(SearchEnginePreferences.KEY_DISABLED_SEARCH_ENGINES)
    }

    fun getDisabledSearchEngines(): Set<String> {
        // Create a defensive copy to avoid SharedPreferences StringSet bugs
        return getStringSet(SearchEnginePreferences.KEY_DISABLED_SEARCH_ENGINES).toSet()
    }

    fun setDisabledSearchEngines(disabled: Set<String>) {
        // Create a new HashSet to ensure Android persists the changes correctly
        // This is required due to a known Android bug with StringSet in SharedPreferences
        prefs.edit().putStringSet(SearchEnginePreferences.KEY_DISABLED_SEARCH_ENGINES, HashSet(disabled)).apply()
    }

    fun getSearchEngineOrder(): List<String> = getStringListPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_ORDER)

    fun setSearchEngineOrder(order: List<String>) {
        setStringListPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_ORDER, order)
    }

    fun isSearchEngineCompactMode(): Boolean = getBooleanPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_COMPACT_MODE, false)

    fun setSearchEngineCompactMode(enabled: Boolean) {
        setBooleanPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_COMPACT_MODE, enabled)
    }

    fun hasSeenSearchEngineOnboarding(): Boolean = getBooleanPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_ONBOARDING_SEEN, false)

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) {
        setBooleanPref(SearchEnginePreferences.KEY_SEARCH_ENGINE_ONBOARDING_SEEN, seen)
    }

    companion object {
        // Search engine preferences keys
        const val KEY_DISABLED_SEARCH_ENGINES = "disabled_search_engines"
        const val KEY_SEARCH_ENGINE_ORDER = "search_engine_order"
        const val KEY_SEARCH_ENGINE_COMPACT_MODE = "search_engine_compact_mode"
        const val KEY_SEARCH_ENGINE_ONBOARDING_SEEN = "search_engine_onboarding_seen"
    }
}
