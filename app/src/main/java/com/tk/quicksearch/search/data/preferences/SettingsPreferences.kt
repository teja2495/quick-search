package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.data.preferences.BasePreferences

/**
 * Preferences for settings-related configurations such as pinned/excluded settings.
 */
class SettingsPreferences(
    context: Context,
) : BasePreferences(context) {
    companion object {
        private const val DEFAULT_ASSISTANT_LAUNCH_VOICE_MODE_ENABLED = false
    }

    // ============================================================================
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED_SETTINGS)

    fun getExcludedSettingIds(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_SETTINGS)

    fun pinSetting(id: String): Set<String> = pinStringItem(BasePreferences.KEY_PINNED_SETTINGS, id)

    fun unpinSetting(id: String): Set<String> = unpinStringItem(BasePreferences.KEY_PINNED_SETTINGS, id)

    fun excludeSetting(id: String): Set<String> = excludeStringItem(BasePreferences.KEY_EXCLUDED_SETTINGS, id)

    fun removeExcludedSetting(id: String): Set<String> = removeExcludedStringItem(BasePreferences.KEY_EXCLUDED_SETTINGS, id)

    fun clearAllExcludedSettings(): Set<String> = clearAllExcludedStringItems(BasePreferences.KEY_EXCLUDED_SETTINGS)

    fun isAssistantLaunchVoiceModeEnabled(): Boolean =
        getBooleanPref(
            BasePreferences.KEY_ASSISTANT_LAUNCH_VOICE_MODE_ENABLED,
            DEFAULT_ASSISTANT_LAUNCH_VOICE_MODE_ENABLED,
        )

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) =
        setBooleanPref(BasePreferences.KEY_ASSISTANT_LAUNCH_VOICE_MODE_ENABLED, enabled)
}
