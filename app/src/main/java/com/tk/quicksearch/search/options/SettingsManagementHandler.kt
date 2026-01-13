package com.tk.quicksearch.search.options

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.SettingsManagementConfig
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Handles settings management operations like pinning, excluding, and nicknames.
 */
class SettingsManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) : ManagementHandler<SettingShortcut> by GenericManagementHandler(
    SettingsManagementConfig(),
    userPreferences,
    scope,
    onStateChanged,
    onUiStateUpdate
) {

    // Convenience methods that delegate to the interface
    fun pinSetting(setting: SettingShortcut) = pinItem(setting)
    fun unpinSetting(setting: SettingShortcut) = unpinItem(setting)
    fun excludeSetting(setting: SettingShortcut) = excludeItem(setting)
    fun removeExcludedSetting(setting: SettingShortcut) = removeExcludedItem(setting)
    fun setSettingNickname(setting: SettingShortcut, nickname: String?) = setItemNickname(setting, nickname)
    fun getSettingNickname(id: String): String? = getItemNickname(SettingShortcut(id, "", "", emptyList(), ""))
    fun clearAllExcludedSettings() = clearAllExcludedItems()
}
