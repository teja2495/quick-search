package com.tk.quicksearch.search.settings

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.SearchUiState
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

        onUiStateUpdate { state ->
            if (state.pinnedSettings.any { it.id == setting.id }) {
                state
            } else {
                state.copy(pinnedSettings = state.pinnedSettings + setting)
            }
        }

        scope.launch(Dispatchers.IO) {
            userPreferences.pinSetting(setting.id)
            onStateChanged()
        }
    }

    fun unpinSetting(setting: SettingShortcut) {
        onUiStateUpdate { state ->
            state.copy(
                pinnedSettings = state.pinnedSettings.filterNot { it.id == setting.id }
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.unpinSetting(setting.id)
            onStateChanged()
        }
    }

    fun excludeSetting(setting: SettingShortcut) {
        onUiStateUpdate { state ->
            state.copy(
                settingResults = state.settingResults.filterNot { it.id == setting.id },
                pinnedSettings = state.pinnedSettings.filterNot { it.id == setting.id },
                excludedSettings = state.excludedSettings + setting // Optimistically add to excluded
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.excludeSetting(setting.id)
            if (userPreferences.getPinnedSettingIds().contains(setting.id)) {
                userPreferences.unpinSetting(setting.id)
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
        onUiStateUpdate { state ->
            state.copy(
                excludedSettings = state.excludedSettings.filterNot { it.id == setting.id }
            )
        }

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
