package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Preferences for search engine-related settings such as disabled engines and ordering.
 */
class SearchEnginePreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean {
        return prefs.contains(BasePreferences.KEY_DISABLED_SEARCH_ENGINES)
    }

    fun getDisabledSearchEngines(): Set<String> {
        // Create a defensive copy to avoid SharedPreferences StringSet bugs
        return getStringSet(BasePreferences.KEY_DISABLED_SEARCH_ENGINES).toSet()
    }

    fun setDisabledSearchEngines(disabled: Set<String>) {
        // Create a new HashSet to ensure Android persists the changes correctly
        // This is required due to a known Android bug with StringSet in SharedPreferences
        prefs.edit().putStringSet(BasePreferences.KEY_DISABLED_SEARCH_ENGINES, HashSet(disabled)).apply()
    }

    fun getSearchEngineOrder(): List<String> = getStringListPref(BasePreferences.KEY_SEARCH_ENGINE_ORDER)

    fun setSearchEngineOrder(order: List<String>) {
        setStringListPref(BasePreferences.KEY_SEARCH_ENGINE_ORDER, order)
    }

    fun isSearchEngineCompactMode(): Boolean = getBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_COMPACT_MODE, false)

    fun setSearchEngineCompactMode(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_COMPACT_MODE, enabled)
    }

    fun hasSeenSearchEngineOnboarding(): Boolean = getBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_ONBOARDING_SEEN, false)

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) {
        setBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_ONBOARDING_SEEN, seen)
    }

}
