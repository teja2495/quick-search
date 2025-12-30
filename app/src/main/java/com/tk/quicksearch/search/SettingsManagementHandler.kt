package com.tk.quicksearch.search

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.SettingShortcut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles settings management operations like pinning, excluding, and nicknames.
 */
class SettingsManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    fun pinSetting(setting: SettingShortcut) {
        if (userPreferences.getExcludedSettingIds().contains(setting.id)) return
        scope.launch(Dispatchers.IO) {
            userPreferences.pinSetting(setting.id)
            onStateChanged()
        }
    }

    fun unpinSetting(setting: SettingShortcut) {
        scope.launch(Dispatchers.IO) {
            userPreferences.unpinSetting(setting.id)
            onStateChanged()
        }
    }

    fun excludeSetting(setting: SettingShortcut) {
        scope.launch(Dispatchers.IO) {
            userPreferences.excludeSetting(setting.id)
            if (userPreferences.getPinnedSettingIds().contains(setting.id)) {
                userPreferences.unpinSetting(setting.id)
            }
            onUiStateUpdate { state ->
                state.copy(
                    settingResults = state.settingResults.filterNot { it.id == setting.id },
                    pinnedSettings = state.pinnedSettings.filterNot { it.id == setting.id }
                )
            }
            onStateChanged()
        }
    }

    fun setSettingNickname(setting: SettingShortcut, nickname: String?) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setSettingNickname(setting.id, nickname)
            onStateChanged()
        }
    }

    fun getSettingNickname(id: String): String? {
        return userPreferences.getSettingNickname(id)
    }

    fun removeExcludedSetting(setting: SettingShortcut) {
        scope.launch(Dispatchers.IO) {
            userPreferences.removeExcludedSetting(setting.id)
            onStateChanged()
        }
    }

    fun clearAllExcludedSettings() {
        scope.launch(Dispatchers.IO) {
            userPreferences.clearAllExcludedSettings()
            onStateChanged()
        }
    }
}
