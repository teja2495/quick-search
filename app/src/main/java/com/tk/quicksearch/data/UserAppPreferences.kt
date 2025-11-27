package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.model.FileType

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 */
class UserAppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()

    fun getPinnedPackages(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()).orEmpty().toSet()

    fun shouldShowAppLabels(): Boolean = prefs.getBoolean(KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(showLabels: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_LABELS, showLabels).apply()
    }

    fun hidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.add(packageName)
    }

    fun unhidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.add(packageName)
    }

    fun unpinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.remove(packageName)
    }

    fun getDisabledSearchEngines(): Set<String> = prefs.getStringSet(KEY_DISABLED_SEARCH_ENGINES, emptySet()).orEmpty().toSet()

    fun setDisabledSearchEngines(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SEARCH_ENGINES, disabled).apply()
    }

    fun getSearchEngineOrder(): List<String> {
        val orderString = prefs.getString(KEY_SEARCH_ENGINE_ORDER, null)
        return if (orderString.isNullOrBlank()) {
            emptyList()
        } else {
            orderString.split(",").filter { it.isNotBlank() }
        }
    }

    fun setSearchEngineOrder(order: List<String>) {
        prefs.edit().putString(KEY_SEARCH_ENGINE_ORDER, order.joinToString(",")).apply()
    }

    fun getPreferredPhoneNumber(contactId: Long): String? {
        return prefs.getString("$KEY_PREFERRED_PHONE_PREFIX$contactId", null)
    }

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("$KEY_PREFERRED_PHONE_PREFIX$contactId", phoneNumber).apply()
    }

    fun getEnabledFileTypes(): Set<FileType> {
        val enabledNames = prefs.getStringSet(KEY_ENABLED_FILE_TYPES, null)
        return if (enabledNames == null) {
            // Default: all file types enabled
            FileType.values().toSet()
        } else {
            val migratedNames = enabledNames.map { name ->
                // Migrate old IMAGES or VIDEOS to PHOTOS_AND_VIDEOS
                when (name) {
                    "IMAGES", "VIDEOS" -> "PHOTOS_AND_VIDEOS"
                    else -> name
                }
            }.toSet()
            val result = migratedNames.mapNotNull { name ->
                FileType.values().find { it.name == name }
            }.toSet()
            // If migration occurred, save the migrated preferences
            if (migratedNames != enabledNames) {
                setEnabledFileTypes(result)
            }
            result
        }
    }

    fun setEnabledFileTypes(enabled: Set<FileType>) {
        prefs.edit().putStringSet(KEY_ENABLED_FILE_TYPES, enabled.map { it.name }.toSet()).apply()
    }

    private inline fun updateStringSet(
        key: String,
        block: (MutableSet<String>) -> Unit
    ): Set<String> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot
    }

    private companion object {
        private const val PREFS_NAME = "user_app_preferences"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_PINNED = "pinned_packages"
        private const val KEY_SHOW_APP_LABELS = "show_app_labels"
        private const val KEY_DISABLED_SEARCH_ENGINES = "disabled_search_engines"
        private const val KEY_SEARCH_ENGINE_ORDER = "search_engine_order"
        private const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"
        private const val KEY_ENABLED_FILE_TYPES = "enabled_file_types"
    }
}


