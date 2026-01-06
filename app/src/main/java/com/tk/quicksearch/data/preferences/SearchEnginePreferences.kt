package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for search engine-related settings such as disabled engines and ordering.
 */
class SearchEnginePreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun getDisabledSearchEngines(): Set<String> = getStringSet(KEY_DISABLED_SEARCH_ENGINES)

    fun setDisabledSearchEngines(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SEARCH_ENGINES, disabled).apply()
    }

    fun getSearchEngineOrder(): List<String> = getStringListPref(KEY_SEARCH_ENGINE_ORDER)

    fun setSearchEngineOrder(order: List<String>) {
        setStringListPref(KEY_SEARCH_ENGINE_ORDER, order)
    }

    fun isSearchEngineSectionEnabled(): Boolean = getBooleanPref(KEY_SEARCH_ENGINE_SECTION_ENABLED, true)

    fun setSearchEngineSectionEnabled(enabled: Boolean) {
        setBooleanPref(KEY_SEARCH_ENGINE_SECTION_ENABLED, enabled)
    }

    fun hasSeenSearchEngineOnboarding(): Boolean = getBooleanPref(KEY_SEARCH_ENGINE_ONBOARDING_SEEN, false)

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) {
        setBooleanPref(KEY_SEARCH_ENGINE_ONBOARDING_SEEN, seen)
    }
}
