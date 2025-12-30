package com.tk.quicksearch.data.preferences

import android.content.Context

/**
 * Preferences for settings-related configurations such as pinned/excluded settings.
 */
class SettingsPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = getStringSet(KEY_PINNED_SETTINGS)

    fun getExcludedSettingIds(): Set<String> = getStringSet(KEY_EXCLUDED_SETTINGS)

    fun pinSetting(id: String): Set<String> = updateStringSet(KEY_PINNED_SETTINGS) {
        it.add(id)
    }

    fun unpinSetting(id: String): Set<String> = updateStringSet(KEY_PINNED_SETTINGS) {
        it.remove(id)
    }

    fun excludeSetting(id: String): Set<String> = updateStringSet(KEY_EXCLUDED_SETTINGS) {
        it.add(id)
    }

    fun removeExcludedSetting(id: String): Set<String> = updateStringSet(KEY_EXCLUDED_SETTINGS) {
        it.remove(id)
    }

    fun clearAllExcludedSettings(): Set<String> = clearStringSet(KEY_EXCLUDED_SETTINGS)
}
