package com.tk.quicksearch.search.deviceSettings

import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.SettingsManagementConfig
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Handles settings management operations like pinning, excluding, and nicknames.
 */
class DeviceSettingsManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) : ManagementHandler<DeviceSetting> by GenericManagementHandler(
    SettingsManagementConfig(),
    userPreferences,
    scope,
    onStateChanged,
    onUiStateUpdate
) {

    // Convenience methods that delegate to the interface
    fun pinSetting(setting: DeviceSetting) = pinItem(setting)
    fun unpinSetting(setting: DeviceSetting) = unpinItem(setting)
    fun excludeSetting(setting: DeviceSetting) = excludeItem(setting)
    fun removeExcludedSetting(setting: DeviceSetting) = removeExcludedItem(setting)
    fun setSettingNickname(setting: DeviceSetting, nickname: String?) = setItemNickname(setting, nickname)
    fun getSettingNickname(id: String): String? = getItemNickname(DeviceSetting(id, "", "", emptyList(), ""))
    fun clearAllExcludedSettings() = clearAllExcludedItems()
}
