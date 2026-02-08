package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.CustomSearchEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferences for search engine-related settings such as disabled engines and ordering.
 */
class SearchEnginePreferences(
    context: Context,
) : BasePreferences(context) {
    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean = prefs.contains(BasePreferences.KEY_DISABLED_SEARCH_ENGINES)

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

    fun getCustomSearchEngines(): List<CustomSearchEngine> {
        val stored = prefs.getString(BasePreferences.KEY_CUSTOM_SEARCH_ENGINES, null)
        if (stored.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val urlTemplate = item.optString("urlTemplate")
                    if (id.isBlank() || name.isBlank() || urlTemplate.isBlank()) continue
                    add(
                        CustomSearchEngine(
                            id = id,
                            name = name,
                            urlTemplate = urlTemplate,
                            faviconBase64 = item.optString("faviconBase64").ifBlank { null },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) {
        val array = JSONArray()
        engines.forEach { engine ->
            val item =
                JSONObject().apply {
                    put("id", engine.id)
                    put("name", engine.name)
                    put("urlTemplate", engine.urlTemplate)
                    put("faviconBase64", engine.faviconBase64 ?: "")
                }
            array.put(item)
        }
        prefs.edit().putString(BasePreferences.KEY_CUSTOM_SEARCH_ENGINES, array.toString()).apply()
    }
}
