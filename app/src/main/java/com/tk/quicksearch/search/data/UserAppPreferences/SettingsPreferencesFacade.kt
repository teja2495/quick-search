package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.SettingsPreferences

/**
 * Facade for settings preference operations
 */
class SettingsPreferencesFacade(
    private val settingsPreferences: SettingsPreferences
) {
    fun getPinnedSettingIds(): Set<String> = settingsPreferences.getPinnedSettingIds()

    fun getExcludedSettingIds(): Set<String> = settingsPreferences.getExcludedSettingIds()

    fun pinSetting(id: String): Set<String> = settingsPreferences.pinSetting(id)

    fun unpinSetting(id: String): Set<String> = settingsPreferences.unpinSetting(id)

    fun excludeSetting(id: String): Set<String> = settingsPreferences.excludeSetting(id)

    fun removeExcludedSetting(id: String): Set<String> =
            settingsPreferences.removeExcludedSetting(id)

    fun clearAllExcludedSettings(): Set<String> = settingsPreferences.clearAllExcludedSettings()

    fun isAssistantLaunchVoiceModeEnabled(): Boolean =
            settingsPreferences.isAssistantLaunchVoiceModeEnabled()

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) =
            settingsPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
}