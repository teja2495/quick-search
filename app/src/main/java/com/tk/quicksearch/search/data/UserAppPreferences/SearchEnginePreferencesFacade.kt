package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.data.preferences.SearchEnginePreferences

/**
 * Facade for search engine preference operations
 */
class SearchEnginePreferencesFacade(
    private val searchEnginePreferences: SearchEnginePreferences
) {
    fun hasDisabledSearchEnginesPreference(): Boolean =
            searchEnginePreferences.hasDisabledSearchEnginesPreference()

    fun getDisabledSearchEngines(): Set<String> = searchEnginePreferences.getDisabledSearchEngines()

    fun setDisabledSearchEngines(disabled: Set<String>) =
            searchEnginePreferences.setDisabledSearchEngines(disabled)

    fun getSearchEngineOrder(): List<String> = searchEnginePreferences.getSearchEngineOrder()

    fun setSearchEngineOrder(order: List<String>) =
            searchEnginePreferences.setSearchEngineOrder(order)

    fun isSearchEngineCompactMode(): Boolean = searchEnginePreferences.isSearchEngineCompactMode()

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineCompactMode(enabled)

    fun getSearchEngineCompactRowCount(): Int = searchEnginePreferences.getSearchEngineCompactRowCount()

    fun setSearchEngineCompactRowCount(rowCount: Int) =
            searchEnginePreferences.setSearchEngineCompactRowCount(rowCount)

    fun hasSeenSearchEngineOnboarding(): Boolean =
            searchEnginePreferences.hasSeenSearchEngineOnboarding()

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) =
            searchEnginePreferences.setHasSeenSearchEngineOnboarding(seen)

    fun getCustomSearchEngines(): List<CustomSearchEngine> =
            searchEnginePreferences.getCustomSearchEngines()

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) =
            searchEnginePreferences.setCustomSearchEngines(engines)
}