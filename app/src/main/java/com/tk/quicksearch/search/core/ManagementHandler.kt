package com.tk.quicksearch.search.core

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Configuration for a management handler that defines how to handle different item types.
 */
interface ManagementHandlerConfig<T> {
    fun getItemId(item: T): String
    fun canPinItem(item: T): Boolean = true
    fun updateUiForPin(item: T, state: SearchUiState): SearchUiState = state
    fun updateUiForUnpin(item: T, state: SearchUiState): SearchUiState = state
    fun updateUiForExclude(item: T, state: SearchUiState): SearchUiState = state
    fun updateUiForRemoveExclusion(item: T, state: SearchUiState): SearchUiState = state
    fun pinItemInPreferences(item: T, preferences: UserAppPreferences)
    fun unpinItemInPreferences(item: T, preferences: UserAppPreferences)
    fun excludeItemInPreferences(item: T, preferences: UserAppPreferences)
    fun removeExcludedItemInPreferences(item: T, preferences: UserAppPreferences)
    fun setItemNicknameInPreferences(item: T, nickname: String?, preferences: UserAppPreferences)
    fun getItemNicknameFromPreferences(item: T, preferences: UserAppPreferences): String?
    fun clearAllExcludedItemsInPreferences(preferences: UserAppPreferences)
}

/**
 * Common interface for management handlers that handle pinning, excluding, and naming operations.
 */
interface ManagementHandler<T> {

    /**
     * Pin an item.
     */
    fun pinItem(item: T)

    /**
     * Unpin an item.
     */
    fun unpinItem(item: T)

    /**
     * Exclude an item from results.
     */
    fun excludeItem(item: T)

    /**
     * Remove an item from the excluded list.
     */
    fun removeExcludedItem(item: T)

    /**
     * Set a custom nickname for an item.
     */
    fun setItemNickname(item: T, nickname: String?)

    /**
     * Get the custom nickname for an item.
     */
    fun getItemNickname(item: T): String?

/**
 * Clear all excluded items.
 */
fun clearAllExcludedItems()
}


/**
 * Generic management handler that uses configuration to handle different item types.
 */
class GenericManagementHandler<T>(
    private val config: ManagementHandlerConfig<T>,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) : ManagementHandler<T> {

    override fun pinItem(item: T) {
        if (!config.canPinItem(item)) return

        // Update UI immediately (optimistic)
        onUiStateUpdate { config.updateUiForPin(item, it) }

        // Update preferences
        config.pinItemInPreferences(item, userPreferences)
        onStateChanged()
    }

    override fun unpinItem(item: T) {
        // Update UI immediately
        onUiStateUpdate { config.updateUiForUnpin(item, it) }

        // Update preferences
        config.unpinItemInPreferences(item, userPreferences)
        onStateChanged()
    }

    override fun excludeItem(item: T) {
        // Update UI immediately
        onUiStateUpdate { config.updateUiForExclude(item, it) }

        // Update preferences
        config.excludeItemInPreferences(item, userPreferences)
        onStateChanged()
    }

    override fun removeExcludedItem(item: T) {
        // Update UI immediately (optimistic)
        onUiStateUpdate { config.updateUiForRemoveExclusion(item, it) }

        // Update preferences
        config.removeExcludedItemInPreferences(item, userPreferences)
        onStateChanged()
    }

    override fun setItemNickname(item: T, nickname: String?) {
        config.setItemNicknameInPreferences(item, nickname, userPreferences)
        onStateChanged()
    }

    override fun getItemNickname(item: T): String? {
        return config.getItemNicknameFromPreferences(item, userPreferences)
    }

    override fun clearAllExcludedItems() {
        config.clearAllExcludedItemsInPreferences(userPreferences)
        onStateChanged()
    }
}

// =============================================================================
// Configuration implementations for different item types
// =============================================================================

/**
 * Configuration for managing AppInfo items.
 */
class AppManagementConfig : ManagementHandlerConfig<AppInfo> {
    override fun getItemId(item: AppInfo): String = item.packageName

    override fun canPinItem(item: AppInfo): Boolean = true // Apps can always be pinned - validation done elsewhere

    override fun pinItemInPreferences(item: AppInfo, preferences: UserAppPreferences) {
        preferences.pinPackage(item.packageName)
    }

    override fun unpinItemInPreferences(item: AppInfo, preferences: UserAppPreferences) {
        preferences.unpinPackage(item.packageName)
    }

    override fun excludeItemInPreferences(item: AppInfo, preferences: UserAppPreferences) {
        preferences.hidePackageInSuggestions(item.packageName)
        if (preferences.getPinnedPackages().contains(item.packageName)) {
            preferences.unpinPackage(item.packageName)
        }
    }

    override fun removeExcludedItemInPreferences(item: AppInfo, preferences: UserAppPreferences) {
        preferences.unhidePackageInSuggestions(item.packageName)
    }

    override fun setItemNicknameInPreferences(item: AppInfo, nickname: String?, preferences: UserAppPreferences) {
        preferences.setAppNickname(item.packageName, nickname)
    }

    override fun getItemNicknameFromPreferences(item: AppInfo, preferences: UserAppPreferences): String? {
        return preferences.getAppNickname(item.packageName)
    }

    override fun clearAllExcludedItemsInPreferences(preferences: UserAppPreferences) {
        preferences.clearAllHiddenAppsInSuggestions()
    }
}

/**
 * Configuration for managing ContactInfo items.
 */
class ContactManagementConfig : ManagementHandlerConfig<ContactInfo> {
    override fun getItemId(item: ContactInfo): String = item.contactId.toString()

    override fun updateUiForPin(item: ContactInfo, state: SearchUiState): SearchUiState {
        return if (state.pinnedContacts.any { it.contactId == item.contactId }) {
            state
        } else {
            state.copy(pinnedContacts = state.pinnedContacts + item)
        }
    }

    override fun updateUiForUnpin(item: ContactInfo, state: SearchUiState): SearchUiState {
        return state.copy(
            pinnedContacts = state.pinnedContacts.filterNot { pinned -> pinned.contactId == item.contactId }
        )
    }

    override fun updateUiForExclude(item: ContactInfo, state: SearchUiState): SearchUiState {
        return state.copy(
            contactResults = state.contactResults.filterNot { result -> result.contactId == item.contactId },
            pinnedContacts = state.pinnedContacts.filterNot { pinned -> pinned.contactId == item.contactId }
        )
    }

    override fun updateUiForRemoveExclusion(item: ContactInfo, state: SearchUiState): SearchUiState {
        return state.copy(
            excludedContacts = state.excludedContacts.filterNot { it.contactId == item.contactId }
        )
    }

    override fun pinItemInPreferences(item: ContactInfo, preferences: UserAppPreferences) {
        preferences.pinContact(item.contactId)
    }

    override fun unpinItemInPreferences(item: ContactInfo, preferences: UserAppPreferences) {
        preferences.unpinContact(item.contactId)
    }

    override fun excludeItemInPreferences(item: ContactInfo, preferences: UserAppPreferences) {
        preferences.excludeContact(item.contactId)
        if (preferences.getPinnedContactIds().contains(item.contactId)) {
            preferences.unpinContact(item.contactId)
        }
    }

    override fun removeExcludedItemInPreferences(item: ContactInfo, preferences: UserAppPreferences) {
        preferences.removeExcludedContact(item.contactId)
    }

    override fun setItemNicknameInPreferences(item: ContactInfo, nickname: String?, preferences: UserAppPreferences) {
        preferences.setContactNickname(item.contactId, nickname)
    }

    override fun getItemNicknameFromPreferences(item: ContactInfo, preferences: UserAppPreferences): String? {
        return preferences.getContactNickname(item.contactId)
    }

    override fun clearAllExcludedItemsInPreferences(preferences: UserAppPreferences) {
        preferences.clearAllExcludedContacts()
    }
}

/**
 * Configuration for managing SettingShortcut items.
 */
class SettingsManagementConfig : ManagementHandlerConfig<SettingShortcut> {
    override fun getItemId(item: SettingShortcut): String = item.id

    override fun canPinItem(item: SettingShortcut): Boolean = true // Settings can always be pinned - validation done elsewhere

    override fun updateUiForPin(item: SettingShortcut, state: SearchUiState): SearchUiState {
        return if (state.pinnedSettings.any { it.id == item.id }) {
            state
        } else {
            state.copy(pinnedSettings = state.pinnedSettings + item)
        }
    }

    override fun updateUiForUnpin(item: SettingShortcut, state: SearchUiState): SearchUiState {
        return state.copy(
            pinnedSettings = state.pinnedSettings.filterNot { it.id == item.id }
        )
    }

    override fun updateUiForExclude(item: SettingShortcut, state: SearchUiState): SearchUiState {
        return state.copy(
            settingResults = state.settingResults.filterNot { it.id == item.id },
            pinnedSettings = state.pinnedSettings.filterNot { it.id == item.id },
            excludedSettings = state.excludedSettings + item // Optimistically add to excluded
        )
    }

    override fun updateUiForRemoveExclusion(item: SettingShortcut, state: SearchUiState): SearchUiState {
        return state.copy(
            excludedSettings = state.excludedSettings.filterNot { it.id == item.id }
        )
    }

    override fun pinItemInPreferences(item: SettingShortcut, preferences: UserAppPreferences) {
        preferences.pinSetting(item.id)
    }

    override fun unpinItemInPreferences(item: SettingShortcut, preferences: UserAppPreferences) {
        preferences.unpinSetting(item.id)
    }

    override fun excludeItemInPreferences(item: SettingShortcut, preferences: UserAppPreferences) {
        preferences.excludeSetting(item.id)
        if (preferences.getPinnedSettingIds().contains(item.id)) {
            preferences.unpinSetting(item.id)
        }
    }

    override fun removeExcludedItemInPreferences(item: SettingShortcut, preferences: UserAppPreferences) {
        preferences.removeExcludedSetting(item.id)
    }

    override fun setItemNicknameInPreferences(item: SettingShortcut, nickname: String?, preferences: UserAppPreferences) {
        preferences.setSettingNickname(item.id, nickname)
    }

    override fun getItemNicknameFromPreferences(item: SettingShortcut, preferences: UserAppPreferences): String? {
        return preferences.getSettingNickname(item.id)
    }

    override fun clearAllExcludedItemsInPreferences(preferences: UserAppPreferences) {
        preferences.clearAllExcludedSettings()
    }
}

/**
 * Configuration for managing DeviceFile items.
 */
class FileManagementConfig : ManagementHandlerConfig<DeviceFile> {
    override fun getItemId(item: DeviceFile): String = item.uri.toString()

    override fun updateUiForPin(item: DeviceFile, state: SearchUiState): SearchUiState {
        val uriString = item.uri.toString()
        return if (state.pinnedFiles.any { it.uri.toString() == uriString }) {
            state
        } else {
            state.copy(pinnedFiles = state.pinnedFiles + item)
        }
    }

    override fun updateUiForUnpin(item: DeviceFile, state: SearchUiState): SearchUiState {
        val uriString = item.uri.toString()
        return state.copy(
            pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
        )
    }

    override fun updateUiForExclude(item: DeviceFile, state: SearchUiState): SearchUiState {
        val uriString = item.uri.toString()
        return state.copy(
            fileResults = state.fileResults.filterNot { it.uri.toString() == uriString },
            pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
        )
    }

    override fun updateUiForRemoveExclusion(item: DeviceFile, state: SearchUiState): SearchUiState {
        val uriString = item.uri.toString()
        return state.copy(
            excludedFiles = state.excludedFiles.filterNot { it.uri.toString() == uriString }
        )
    }

    override fun pinItemInPreferences(item: DeviceFile, preferences: UserAppPreferences) {
        preferences.pinFile(item.uri.toString())
    }

    override fun unpinItemInPreferences(item: DeviceFile, preferences: UserAppPreferences) {
        preferences.unpinFile(item.uri.toString())
    }

    override fun excludeItemInPreferences(item: DeviceFile, preferences: UserAppPreferences) {
        val uriString = item.uri.toString()
        preferences.excludeFile(uriString)
        if (preferences.getPinnedFileUris().contains(uriString)) {
            preferences.unpinFile(uriString)
        }
    }

    override fun removeExcludedItemInPreferences(item: DeviceFile, preferences: UserAppPreferences) {
        preferences.removeExcludedFile(item.uri.toString())
    }

    override fun setItemNicknameInPreferences(item: DeviceFile, nickname: String?, preferences: UserAppPreferences) {
        preferences.setFileNickname(item.uri.toString(), nickname)
    }

    override fun getItemNicknameFromPreferences(item: DeviceFile, preferences: UserAppPreferences): String? {
        return preferences.getFileNickname(item.uri.toString())
    }

    override fun clearAllExcludedItemsInPreferences(preferences: UserAppPreferences) {
        preferences.clearAllExcludedFiles()
    }
}