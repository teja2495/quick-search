package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.data.assets.ManagedAssetStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferences for search engine-related settings such as disabled engines and ordering.
 */
class SearchEnginePreferences(
    context: Context,
) : BasePreferences(context) {
    private val assetStore = ManagedAssetStore(context)
    companion object {
        const val ONE_ROW = 1
        const val TWO_ROWS = 2
    }

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

    fun getSearchEngineCompactRowCount(): Int =
        prefs
            .getInt(BasePreferences.KEY_SEARCH_ENGINE_COMPACT_ROW_COUNT, ONE_ROW)
            .coerceIn(ONE_ROW, TWO_ROWS)

    fun setSearchEngineCompactRowCount(rowCount: Int) {
        prefs.edit()
            .putInt(
                BasePreferences.KEY_SEARCH_ENGINE_COMPACT_ROW_COUNT,
                rowCount.coerceIn(ONE_ROW, TWO_ROWS),
            )
            .apply()
    }

    fun isSearchEngineAliasSuffixEnabled(): Boolean =
        getBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_ALIAS_SUFFIX_ENABLED, true)

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_SEARCH_ENGINE_ALIAS_SUFFIX_ENABLED, enabled)
    }

    fun isAliasTriggerAfterSpaceEnabled(): Boolean =
        getBooleanPref(BasePreferences.KEY_ALIAS_TRIGGER_AFTER_SPACE, true)

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_ALIAS_TRIGGER_AFTER_SPACE, enabled)
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
                            faviconBase64 =
                                assetStore.getBase64("${ManagedAssetStore.SEARCH_ENGINE_ICON_PREFIX}$id")
                                    ?: item.optString("faviconBase64").ifBlank { null }?.also { legacy ->
                                        assetStore.putBase64(
                                            "${ManagedAssetStore.SEARCH_ENGINE_ICON_PREFIX}$id",
                                            legacy,
                                        )
                                    },
                            browserPackage = item.optString("browserPackage").ifBlank { null },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    // ============================================================================
    // Custom Tool Preferences
    // ============================================================================

    fun getCustomTools(): List<CustomTool> {
        val stored = prefs.getString(BasePreferences.KEY_CUSTOM_TOOLS, null)
        if (stored.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val prompt = item.optString("prompt")
                    val modelId = item.optString("modelId")
                    val providerId =
                        com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId.fromStorageValue(
                            item.optString("providerId").takeIf { it.isNotBlank() },
                        )
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        CustomTool(
                            id = id,
                            name = name,
                            prompt = prompt,
                            modelId = modelId.ifBlank { "gemini-flash-latest" },
                            providerId = providerId,
                            groundingEnabled = item.optBoolean("groundingEnabled", false),
                            thinkingEnabled = item.optBoolean("thinkingEnabled", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setCustomTools(tools: List<CustomTool>) {
        val array = JSONArray()
        tools.forEach { tool ->
            val item =
                JSONObject().apply {
                    put("id", tool.id)
                    put("name", tool.name)
                    put("prompt", tool.prompt)
                    put("modelId", tool.modelId)
                    put("providerId", tool.providerId.storageValue)
                    put("groundingEnabled", tool.groundingEnabled)
                    put("thinkingEnabled", tool.thinkingEnabled)
                }
            array.put(item)
        }
        prefs.edit().putString(BasePreferences.KEY_CUSTOM_TOOLS, array.toString()).apply()
    }

    fun getDisabledCustomTools(): Set<String> =
        getStringSet(BasePreferences.KEY_DISABLED_CUSTOM_TOOLS).toSet()

    fun setDisabledCustomTools(disabled: Set<String>) {
        prefs.edit().putStringSet(BasePreferences.KEY_DISABLED_CUSTOM_TOOLS, HashSet(disabled)).apply()
    }

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) {
        val array = JSONArray()
        engines.forEach { engine ->
            val assetId = "${ManagedAssetStore.SEARCH_ENGINE_ICON_PREFIX}${engine.id}"
            if (engine.faviconBase64.isNullOrBlank()) {
                assetStore.remove(assetId)
            } else {
                assetStore.putBase64(assetId, engine.faviconBase64)
            }
            val item =
                JSONObject().apply {
                    put("id", engine.id)
                    put("name", engine.name)
                    put("urlTemplate", engine.urlTemplate)
                    put("faviconBase64", engine.faviconBase64 ?: "")
                    put("browserPackage", engine.browserPackage ?: "")
                }
            array.put(item)
        }
        prefs.edit().putString(BasePreferences.KEY_CUSTOM_SEARCH_ENGINES, array.toString()).apply()
    }
}
