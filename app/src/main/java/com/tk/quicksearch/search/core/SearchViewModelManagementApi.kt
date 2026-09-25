package com.tk.quicksearch.search.core

import android.content.Intent
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.preferences.ResultTrigger
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface SearchViewModelManagementApi {
    val managementApiDelegate: SearchViewModelManagementApiDelegate

    fun deleteRecentItem(entry: RecentSearchEntry) = managementApiDelegate.deleteRecentItem(entry)

    fun clearRecentItems() = managementApiDelegate.clearRecentItems()

    suspend fun refreshAppShortcutsAndAwait() = managementApiDelegate.refreshAppShortcutsAndAwait()

    fun refreshUsageAccess() = managementApiDelegate.refreshUsageAccess()

    fun refreshApps(showToast: Boolean = false, forceUiUpdate: Boolean = false) =
        managementApiDelegate.refreshApps(showToast, forceUiUpdate)

    fun refreshContacts(showToast: Boolean = false) = managementApiDelegate.refreshContacts(showToast)

    fun refreshFiles(showToast: Boolean = false) = managementApiDelegate.refreshFiles(showToast)

    fun setDirectDialEnabled(enabled: Boolean, manual: Boolean = true) =
        managementApiDelegate.setDirectDialEnabled(enabled, manual)

    fun setNumberSearchEnabled(enabled: Boolean) =
        managementApiDelegate.setNumberSearchEnabled(enabled)

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) =
        managementApiDelegate.setAssistantLaunchVoiceModeEnabled(enabled)

    fun hideApp(appInfo: AppInfo) = managementApiDelegate.hideApp(appInfo)

    fun unhideAppFromSuggestions(appInfo: AppInfo) =
        managementApiDelegate.unhideAppFromSuggestions(appInfo)

    fun unhideAppFromResults(appInfo: AppInfo) = managementApiDelegate.unhideAppFromResults(appInfo)

    fun clearAllHiddenApps() = managementApiDelegate.clearAllHiddenApps()

    fun pinApp(appInfo: AppInfo) = managementApiDelegate.pinApp(appInfo)

    fun unpinApp(appInfo: AppInfo) = managementApiDelegate.unpinApp(appInfo)

    fun reorderPinnedApps(apps: List<AppInfo>) = managementApiDelegate.reorderPinnedApps(apps)

    fun setAppNickname(appInfo: AppInfo, nickname: String?) =
        managementApiDelegate.setAppNickname(appInfo, nickname)

    fun getAppNickname(packageName: String): String? = managementApiDelegate.getAppNickname(packageName)

    fun setAppTrigger(appInfo: AppInfo, trigger: ResultTrigger?) =
        managementApiDelegate.setAppTrigger(appInfo, trigger)

    fun getAppTrigger(packageName: String): ResultTrigger? =
        managementApiDelegate.getAppTrigger(packageName)
    
    fun getAllTriggerWordsById(): Map<String, String> =
        managementApiDelegate.getAllTriggerWordsById()

    fun getAllAliasWordsById(): Map<String, String> =
        managementApiDelegate.getAllAliasWordsById()

    fun clearCachedApps() = managementApiDelegate.clearCachedApps()

    fun pinContact(contactInfo: ContactInfo) = managementApiDelegate.pinContact(contactInfo)

    fun unpinContact(contactInfo: ContactInfo) = managementApiDelegate.unpinContact(contactInfo)

    fun movePinnedContact(contactInfo: ContactInfo, moveUp: Boolean) =
        managementApiDelegate.movePinnedContact(contactInfo, moveUp)

    fun excludeContact(contactInfo: ContactInfo) = managementApiDelegate.excludeContact(contactInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) =
        managementApiDelegate.removeExcludedContact(contactInfo)

    fun clearAllExcludedContacts() = managementApiDelegate.clearAllExcludedContacts()

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) =
        managementApiDelegate.setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = managementApiDelegate.getContactNickname(contactId)

    fun setContactTrigger(contactInfo: ContactInfo, trigger: ResultTrigger?) =
        managementApiDelegate.setContactTrigger(contactInfo, trigger)

    fun getContactTrigger(contactId: Long): ResultTrigger? =
        managementApiDelegate.getContactTrigger(contactId)

    fun setContactActionTrigger(
        contactInfo: ContactInfo,
        action: ContactCardAction,
        trigger: ResultTrigger?,
    ) = managementApiDelegate.setContactActionTrigger(contactInfo, action, trigger)

    fun getContactActionTrigger(
        contactId: Long,
        action: ContactCardAction,
    ): ResultTrigger? = managementApiDelegate.getContactActionTrigger(contactId, action)

    fun getAllContactActionTriggers():
        Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, ResultTrigger> =
        managementApiDelegate.getAllContactActionTriggers()

    fun pinFile(deviceFile: DeviceFile) = managementApiDelegate.pinFile(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) = managementApiDelegate.unpinFile(deviceFile)

    fun movePinnedFile(deviceFile: DeviceFile, moveUp: Boolean) =
        managementApiDelegate.movePinnedFile(deviceFile, moveUp)

    fun excludeFile(deviceFile: DeviceFile) = managementApiDelegate.excludeFile(deviceFile)

    fun excludeFileExtension(deviceFile: DeviceFile) = managementApiDelegate.excludeFileExtension(deviceFile)

    fun removeExcludedFileExtension(extension: String) =
        managementApiDelegate.removeExcludedFileExtension(extension)

    fun removeExcludedFile(deviceFile: DeviceFile) = managementApiDelegate.removeExcludedFile(deviceFile)

    fun clearAllExcludedFiles() = managementApiDelegate.clearAllExcludedFiles()

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) =
        managementApiDelegate.setFileNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = managementApiDelegate.getFileNickname(uri)

    fun setFileTrigger(deviceFile: DeviceFile, trigger: ResultTrigger?) =
        managementApiDelegate.setFileTrigger(deviceFile, trigger)

    fun getFileTrigger(uri: String): ResultTrigger? = managementApiDelegate.getFileTrigger(uri)

    fun pinSetting(setting: DeviceSetting) = managementApiDelegate.pinSetting(setting)

    fun unpinSetting(setting: DeviceSetting) = managementApiDelegate.unpinSetting(setting)

    fun movePinnedSetting(setting: DeviceSetting, moveUp: Boolean) =
        managementApiDelegate.movePinnedSetting(setting, moveUp)

    fun excludeSetting(setting: DeviceSetting) = managementApiDelegate.excludeSetting(setting)

    fun setSettingNickname(setting: DeviceSetting, nickname: String?) =
        managementApiDelegate.setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = managementApiDelegate.getSettingNickname(id)

    fun setSettingTrigger(setting: DeviceSetting, trigger: ResultTrigger?) =
        managementApiDelegate.setSettingTrigger(setting, trigger)

    fun getSettingTrigger(id: String): ResultTrigger? =
        managementApiDelegate.getSettingTrigger(id)

    fun removeExcludedSetting(setting: DeviceSetting) = managementApiDelegate.removeExcludedSetting(setting)

    fun clearAllExcludedSettings() = managementApiDelegate.clearAllExcludedSettings()

    fun pinCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.pinCalendarEvent(event)

    fun unpinCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.unpinCalendarEvent(event)

    fun movePinnedCalendarEvent(event: CalendarEventInfo, moveUp: Boolean) =
        managementApiDelegate.movePinnedCalendarEvent(event, moveUp)

    fun excludeCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.excludeCalendarEvent(event)

    fun removeExcludedCalendarEvent(event: CalendarEventInfo) =
        managementApiDelegate.removeExcludedCalendarEvent(event)

    fun clearAllExcludedCalendarEvents() = managementApiDelegate.clearAllExcludedCalendarEvents()

    fun setCalendarEventNickname(event: CalendarEventInfo, nickname: String?) =
        managementApiDelegate.setCalendarEventNickname(event, nickname)

    fun getCalendarEventNickname(eventId: Long): String? =
        managementApiDelegate.getCalendarEventNickname(eventId)

    fun pinReminder(reminder: ReminderInfo) = managementApiDelegate.pinReminder(reminder)

    fun unpinReminder(reminder: ReminderInfo) = managementApiDelegate.unpinReminder(reminder)

    fun movePinnedReminder(reminder: ReminderInfo, moveUp: Boolean) =
        managementApiDelegate.movePinnedReminder(reminder, moveUp)

    fun pinNote(noteInfo: NoteInfo) = managementApiDelegate.pinNote(noteInfo)

    fun unpinNote(noteInfo: NoteInfo) = managementApiDelegate.unpinNote(noteInfo)

    fun movePinnedNote(noteInfo: NoteInfo, moveUp: Boolean) =
        managementApiDelegate.movePinnedNote(noteInfo, moveUp)

    fun stageDeleteNote(noteInfo: NoteInfo): NoteInfo? = managementApiDelegate.stageDeleteNote(noteInfo)

    fun undoDeleteNote(noteId: Long) = managementApiDelegate.undoDeleteNote(noteId)

    fun finalizeDeleteNote(noteId: Long) = managementApiDelegate.finalizeDeleteNote(noteId)

    fun setNoteTrigger(noteInfo: NoteInfo, trigger: ResultTrigger?) =
        managementApiDelegate.setNoteTrigger(noteInfo, trigger)

    fun getNoteTrigger(noteId: Long): ResultTrigger? = managementApiDelegate.getNoteTrigger(noteId)

    fun pinAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.pinAppShortcut(shortcut)

    fun unpinAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.unpinAppShortcut(shortcut)

    fun movePinnedAppShortcut(shortcut: StaticShortcut, moveUp: Boolean) =
        managementApiDelegate.movePinnedAppShortcut(shortcut, moveUp)

    fun reorderPinnedAppGrid(
        orderKeys: List<String>,
        apps: List<AppInfo>,
        shortcuts: List<StaticShortcut>,
    ) = managementApiDelegate.reorderPinnedAppGrid(orderKeys, apps, shortcuts)

    fun createAppFolder(targetKey: String, draggedKey: String, orderKeys: List<String>) =
        managementApiDelegate.folderManager.createFolder(targetKey, draggedKey, orderKeys)

    fun addToAppFolder(folderId: String, draggedKey: String, orderKeys: List<String>) =
        managementApiDelegate.folderManager.addToFolder(folderId, draggedKey, orderKeys)

    fun removeFromAppFolder(folderId: String, memberKey: String, orderKeys: List<String>) =
        managementApiDelegate.folderManager.removeFromFolder(folderId, memberKey, orderKeys)

    fun unpinFromAppFolder(folderId: String, memberKey: String, orderKeys: List<String>) =
        managementApiDelegate.folderManager.removeFromFolder(folderId, memberKey, orderKeys, repin = false)

    fun reorderAppFolder(folderId: String, memberKeys: List<String>) =
        managementApiDelegate.folderManager.reorderFolderMembers(folderId, memberKeys)

    fun renameAppFolder(folderId: String, name: String) =
        managementApiDelegate.folderManager.renameFolder(folderId, name)

    fun deleteAppFolder(folderId: String, orderKeys: List<String>) =
        managementApiDelegate.folderManager.deleteFolder(folderId, orderKeys)

    fun excludeAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.excludeAppShortcut(shortcut)

    fun setAppShortcutNickname(shortcut: StaticShortcut, nickname: String?) =
        managementApiDelegate.setAppShortcutNickname(shortcut, nickname)

    fun setAppShortcutEnabled(shortcut: StaticShortcut, enabled: Boolean) =
        managementApiDelegate.setAppShortcutEnabled(shortcut, enabled)

    fun setAppShortcutsEnabled(shortcutIds: Collection<String>, enabled: Boolean) =
        managementApiDelegate.setAppShortcutsEnabled(shortcutIds, enabled)

    fun setAllAppShortcutsEnabled(packageName: String, enabled: Boolean) =
        managementApiDelegate.setAllAppShortcutsEnabled(packageName, enabled)

    fun setAppShortcutIconOverride(shortcut: StaticShortcut, iconBase64: String?) =
        managementApiDelegate.setAppShortcutIconOverride(shortcut, iconBase64)

    fun getAppShortcutIconOverride(shortcutId: String): String? =
        managementApiDelegate.getAppShortcutIconOverride(shortcutId)

    fun getAppShortcutNickname(shortcutId: String): String? =
        managementApiDelegate.getAppShortcutNickname(shortcutId)

    fun setAppShortcutTrigger(shortcut: StaticShortcut, trigger: ResultTrigger?) =
        managementApiDelegate.setAppShortcutTrigger(shortcut, trigger)

    fun getAppShortcutTrigger(shortcutId: String): ResultTrigger? =
        managementApiDelegate.getAppShortcutTrigger(shortcutId)

    fun removeExcludedAppShortcut(shortcut: StaticShortcut) =
        managementApiDelegate.removeExcludedAppShortcut(shortcut)

    fun addCustomAppShortcutFromPickerResult(
        resultData: Intent?,
        sourcePackageName: String? = null,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) =
        managementApiDelegate.addCustomAppShortcutFromPickerResult(
            resultData = resultData,
            sourcePackageName = sourcePackageName,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )

    fun addSearchTargetQueryShortcut(
        target: SearchTarget,
        shortcutName: String,
        shortcutQuery: String,
        mode: SearchTargetShortcutMode = SearchTargetShortcutMode.AUTO,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) =
        managementApiDelegate.addSearchTargetQueryShortcut(
            target = target,
            shortcutName = shortcutName,
            shortcutQuery = shortcutQuery,
            mode = mode,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )

    fun addCustomAppActivityShortcut(
        packageName: String,
        activityClassName: String,
        activityLabel: String,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) =
        managementApiDelegate.addCustomAppActivityShortcut(
            packageName = packageName,
            activityClassName = activityClassName,
            activityLabel = activityLabel,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )

    fun addCustomAppDeepLinkShortcut(
        packageName: String,
        shortcutName: String,
        deepLink: String,
        iconBase64: String?,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) =
        managementApiDelegate.addCustomAppDeepLinkShortcut(
            packageName = packageName,
            shortcutName = shortcutName,
            deepLink = deepLink,
            iconBase64 = iconBase64,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )

    fun deleteCustomAppShortcut(shortcut: StaticShortcut) =
        managementApiDelegate.deleteCustomAppShortcut(shortcut)

    fun updateCustomAppShortcut(
        shortcut: StaticShortcut,
        shortcutName: String,
        shortcutValue: String?,
        iconBase64: String?,
    ) = managementApiDelegate.updateCustomAppShortcut(shortcut, shortcutName, shortcutValue, iconBase64)

    fun clearAllExclusions() = managementApiDelegate.clearAllExclusions()
}
