package com.tk.quicksearch.search.core

import android.content.Intent
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface SearchViewModelManagementApi {
    val managementApiDelegate: SearchViewModelManagementApiDelegate

    fun deleteRecentItem(entry: RecentSearchEntry) = managementApiDelegate.deleteRecentItem(entry)

    fun refreshAppShortcutsCacheFirst() = managementApiDelegate.refreshAppShortcutsCacheFirst()

    fun refreshUsageAccess() = managementApiDelegate.refreshUsageAccess()

    fun refreshApps(showToast: Boolean = false, forceUiUpdate: Boolean = false) =
        managementApiDelegate.refreshApps(showToast, forceUiUpdate)

    fun refreshContacts(showToast: Boolean = false) = managementApiDelegate.refreshContacts(showToast)

    fun refreshFiles(showToast: Boolean = false) = managementApiDelegate.refreshFiles(showToast)

    fun setDirectDialEnabled(enabled: Boolean, manual: Boolean = true) =
        managementApiDelegate.setDirectDialEnabled(enabled, manual)

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) =
        managementApiDelegate.setAssistantLaunchVoiceModeEnabled(enabled)

    fun hideApp(appInfo: AppInfo) = managementApiDelegate.hideApp(appInfo)

    fun unhideAppFromSuggestions(appInfo: AppInfo) =
        managementApiDelegate.unhideAppFromSuggestions(appInfo)

    fun unhideAppFromResults(appInfo: AppInfo) = managementApiDelegate.unhideAppFromResults(appInfo)

    fun clearAllHiddenApps() = managementApiDelegate.clearAllHiddenApps()

    fun pinApp(appInfo: AppInfo) = managementApiDelegate.pinApp(appInfo)

    fun unpinApp(appInfo: AppInfo) = managementApiDelegate.unpinApp(appInfo)

    fun setAppNickname(appInfo: AppInfo, nickname: String?) =
        managementApiDelegate.setAppNickname(appInfo, nickname)

    fun getAppNickname(packageName: String): String? = managementApiDelegate.getAppNickname(packageName)

    fun clearCachedApps() = managementApiDelegate.clearCachedApps()

    fun pinContact(contactInfo: ContactInfo) = managementApiDelegate.pinContact(contactInfo)

    fun unpinContact(contactInfo: ContactInfo) = managementApiDelegate.unpinContact(contactInfo)

    fun excludeContact(contactInfo: ContactInfo) = managementApiDelegate.excludeContact(contactInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) =
        managementApiDelegate.removeExcludedContact(contactInfo)

    fun clearAllExcludedContacts() = managementApiDelegate.clearAllExcludedContacts()

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) =
        managementApiDelegate.setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = managementApiDelegate.getContactNickname(contactId)

    fun pinFile(deviceFile: DeviceFile) = managementApiDelegate.pinFile(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) = managementApiDelegate.unpinFile(deviceFile)

    fun excludeFile(deviceFile: DeviceFile) = managementApiDelegate.excludeFile(deviceFile)

    fun excludeFileExtension(deviceFile: DeviceFile) = managementApiDelegate.excludeFileExtension(deviceFile)

    fun removeExcludedFileExtension(extension: String) =
        managementApiDelegate.removeExcludedFileExtension(extension)

    fun removeExcludedFile(deviceFile: DeviceFile) = managementApiDelegate.removeExcludedFile(deviceFile)

    fun clearAllExcludedFiles() = managementApiDelegate.clearAllExcludedFiles()

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) =
        managementApiDelegate.setFileNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = managementApiDelegate.getFileNickname(uri)

    fun pinSetting(setting: DeviceSetting) = managementApiDelegate.pinSetting(setting)

    fun unpinSetting(setting: DeviceSetting) = managementApiDelegate.unpinSetting(setting)

    fun excludeSetting(setting: DeviceSetting) = managementApiDelegate.excludeSetting(setting)

    fun setSettingNickname(setting: DeviceSetting, nickname: String?) =
        managementApiDelegate.setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = managementApiDelegate.getSettingNickname(id)

    fun removeExcludedSetting(setting: DeviceSetting) = managementApiDelegate.removeExcludedSetting(setting)

    fun clearAllExcludedSettings() = managementApiDelegate.clearAllExcludedSettings()

    fun pinCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.pinCalendarEvent(event)

    fun unpinCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.unpinCalendarEvent(event)

    fun excludeCalendarEvent(event: CalendarEventInfo) = managementApiDelegate.excludeCalendarEvent(event)

    fun removeExcludedCalendarEvent(event: CalendarEventInfo) =
        managementApiDelegate.removeExcludedCalendarEvent(event)

    fun clearAllExcludedCalendarEvents() = managementApiDelegate.clearAllExcludedCalendarEvents()

    fun setCalendarEventNickname(event: CalendarEventInfo, nickname: String?) =
        managementApiDelegate.setCalendarEventNickname(event, nickname)

    fun getCalendarEventNickname(eventId: Long): String? =
        managementApiDelegate.getCalendarEventNickname(eventId)

    fun pinNote(noteInfo: NoteInfo) = managementApiDelegate.pinNote(noteInfo)

    fun unpinNote(noteInfo: NoteInfo) = managementApiDelegate.unpinNote(noteInfo)

    fun stageDeleteNote(noteInfo: NoteInfo): NoteInfo? = managementApiDelegate.stageDeleteNote(noteInfo)

    fun undoDeleteNote(noteId: Long) = managementApiDelegate.undoDeleteNote(noteId)

    fun finalizeDeleteNote(noteId: Long) = managementApiDelegate.finalizeDeleteNote(noteId)

    fun pinAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.pinAppShortcut(shortcut)

    fun unpinAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.unpinAppShortcut(shortcut)

    fun excludeAppShortcut(shortcut: StaticShortcut) = managementApiDelegate.excludeAppShortcut(shortcut)

    fun setAppShortcutNickname(shortcut: StaticShortcut, nickname: String?) =
        managementApiDelegate.setAppShortcutNickname(shortcut, nickname)

    fun setAppShortcutEnabled(shortcut: StaticShortcut, enabled: Boolean) =
        managementApiDelegate.setAppShortcutEnabled(shortcut, enabled)

    fun setAppShortcutIconOverride(shortcut: StaticShortcut, iconBase64: String?) =
        managementApiDelegate.setAppShortcutIconOverride(shortcut, iconBase64)

    fun getAppShortcutIconOverride(shortcutId: String): String? =
        managementApiDelegate.getAppShortcutIconOverride(shortcutId)

    fun getAppShortcutNickname(shortcutId: String): String? =
        managementApiDelegate.getAppShortcutNickname(shortcutId)

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

class SearchViewModelManagementApiDelegate internal constructor(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val resultsStateProvider: () -> SearchResultsState,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val historyDelegate: SearchHistoryDelegate,
    private val staticDataDelegate: SearchStaticDataDelegate,
    private val appManager: () -> com.tk.quicksearch.search.apps.AppManagementService,
    private val contactManager: () -> com.tk.quicksearch.search.contacts.utils.ContactManagementHandler,
    private val fileManager: () -> com.tk.quicksearch.search.files.FileManagementHandler,
    private val settingsManager: () -> com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler,
    private val calendarManager: () -> com.tk.quicksearch.search.calendar.CalendarManagementHandler,
    private val appShortcutManager: () -> com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler,
    private val notesRepository: () -> com.tk.quicksearch.search.data.NotesRepository,
    private val appSearchManager: () -> com.tk.quicksearch.search.apps.AppSearchManager,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val legacyPreferenceState: SearchViewModelLegacyPreferenceState,
    private val lockedAliasSearchSectionProvider: () -> SearchSection?,
    private val refreshRecentItems: () -> Unit,
) {
    fun deleteRecentItem(entry: RecentSearchEntry) {
        historyDelegate.deleteRecentItem(entry, lockedAliasSearchSectionProvider())
    }

    fun refreshAppShortcutsCacheFirst() {
        staticDataDelegate.refreshAppShortcutsCacheFirst()
    }

    fun refreshUsageAccess() {
        staticDataDelegate.refreshUsageAccess()
    }

    fun refreshApps(showToast: Boolean, forceUiUpdate: Boolean) {
        staticDataDelegate.refreshApps(showToast, forceUiUpdate)
    }

    fun refreshContacts(showToast: Boolean) {
        staticDataDelegate.refreshContacts(showToast)
    }

    fun refreshFiles(showToast: Boolean) {
        staticDataDelegate.refreshFiles(showToast)
    }

    fun setDirectDialEnabled(enabled: Boolean, manual: Boolean) {
        legacyPreferenceState.directDialEnabled = enabled
        legacyPreferenceState.hasSeenDirectDialChoice = true
        userPreferences.setDirectDialEnabled(enabled)
        userPreferences.setHasSeenDirectDialChoice(true)
        if (manual) {
            userPreferences.setDirectDialManuallyDisabled(!enabled)
        }
        updateFeatureState { it.copy(directDialEnabled = enabled) }
    }

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            legacyPreferenceState.assistantLaunchVoiceModeEnabled = enabled
            userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
            updateFeatureState { it.copy(assistantLaunchVoiceModeEnabled = enabled) }
        }
    }

    fun hideApp(appInfo: AppInfo) {
        appManager().hideApp(appInfo, resultsStateProvider().query.isNotBlank())
    }

    fun unhideAppFromSuggestions(appInfo: AppInfo) = appManager().unhideAppFromSuggestions(appInfo)

    fun unhideAppFromResults(appInfo: AppInfo) = appManager().unhideAppFromResults(appInfo)

    fun clearAllHiddenApps() = appManager().clearAllHiddenApps()

    fun pinApp(appInfo: AppInfo) = appManager().pinApp(appInfo)

    fun unpinApp(appInfo: AppInfo) = appManager().unpinApp(appInfo)

    fun setAppNickname(appInfo: AppInfo, nickname: String?) = appManager().setAppNickname(appInfo, nickname)

    fun getAppNickname(packageName: String): String? = appManager().getAppNickname(packageName)

    fun clearCachedApps() = appSearchManager().clearCachedApps()

    fun pinContact(contactInfo: ContactInfo) = contactManager().pinContact(contactInfo)

    fun unpinContact(contactInfo: ContactInfo) {
        contactManager().unpinContact(contactInfo)
        refreshRecentItems()
    }

    fun excludeContact(contactInfo: ContactInfo) = contactManager().excludeContact(contactInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) =
        contactManager().removeExcludedContact(contactInfo)

    fun clearAllExcludedContacts() = contactManager().clearAllExcludedContacts()

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) =
        contactManager().setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = contactManager().getContactNickname(contactId)

    fun pinFile(deviceFile: DeviceFile) = fileManager().pinFile(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) {
        fileManager().unpinFile(deviceFile)
        refreshRecentItems()
    }

    fun excludeFile(deviceFile: DeviceFile) = fileManager().excludeFile(deviceFile)

    fun excludeFileExtension(deviceFile: DeviceFile) {
        legacyPreferenceState.excludedFileExtensions = fileManager().excludeFileExtension(deviceFile)
        updateUiState { it.copy(excludedFileExtensions = legacyPreferenceState.excludedFileExtensions) }
    }

    fun removeExcludedFileExtension(extension: String) {
        legacyPreferenceState.excludedFileExtensions =
            fileManager().removeExcludedFileExtension(extension)
        updateUiState { it.copy(excludedFileExtensions = legacyPreferenceState.excludedFileExtensions) }
    }

    fun removeExcludedFile(deviceFile: DeviceFile) = fileManager().removeExcludedFile(deviceFile)

    fun clearAllExcludedFiles() = fileManager().clearAllExcludedFiles()

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) =
        fileManager().setFileNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = fileManager().getFileNickname(uri)

    fun pinSetting(setting: DeviceSetting) = settingsManager().pinSetting(setting)

    fun unpinSetting(setting: DeviceSetting) {
        settingsManager().unpinSetting(setting)
        refreshRecentItems()
    }

    fun excludeSetting(setting: DeviceSetting) = settingsManager().excludeSetting(setting)

    fun setSettingNickname(setting: DeviceSetting, nickname: String?) =
        settingsManager().setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = settingsManager().getSettingNickname(id)

    fun removeExcludedSetting(setting: DeviceSetting) = settingsManager().removeExcludedSetting(setting)

    fun clearAllExcludedSettings() = settingsManager().clearAllExcludedSettings()

    fun pinCalendarEvent(event: CalendarEventInfo) = calendarManager().pinItem(event)

    fun unpinCalendarEvent(event: CalendarEventInfo) = calendarManager().unpinItem(event)

    fun excludeCalendarEvent(event: CalendarEventInfo) = calendarManager().excludeItem(event)

    fun removeExcludedCalendarEvent(event: CalendarEventInfo) = calendarManager().removeExcludedItem(event)

    fun clearAllExcludedCalendarEvents() = calendarManager().clearAllExcludedItems()

    fun setCalendarEventNickname(event: CalendarEventInfo, nickname: String?) =
        calendarManager().setItemNickname(event, nickname)

    fun getCalendarEventNickname(eventId: Long): String? = userPreferences.getCalendarEventNickname(eventId)

    fun pinNote(noteInfo: NoteInfo) {
        scope.launch(Dispatchers.IO) {
            notesRepository().pinNote(noteInfo.noteId)
            refreshNotesState()
        }
    }

    fun unpinNote(noteInfo: NoteInfo) {
        scope.launch(Dispatchers.IO) {
            notesRepository().unpinNote(noteInfo.noteId)
            refreshNotesState()
            refreshRecentItems()
        }
    }

    fun stageDeleteNote(noteInfo: NoteInfo): NoteInfo? {
        val deleted = notesRepository().stageDelete(noteInfo.noteId) ?: return null
        refreshNotesState()
        return deleted
    }

    fun undoDeleteNote(noteId: Long) {
        notesRepository().undoDelete(noteId)
        refreshNotesState()
    }

    fun finalizeDeleteNote(noteId: Long) {
        notesRepository().finalizeDelete(noteId)
        refreshNotesState()
    }

    fun pinAppShortcut(shortcut: StaticShortcut) = appShortcutManager().pinShortcut(shortcut)

    fun unpinAppShortcut(shortcut: StaticShortcut) {
        appShortcutManager().unpinShortcut(shortcut)
        refreshRecentItems()
    }

    fun excludeAppShortcut(shortcut: StaticShortcut) = appShortcutManager().excludeShortcut(shortcut)

    fun setAppShortcutNickname(shortcut: StaticShortcut, nickname: String?) =
        appShortcutManager().setShortcutNickname(shortcut, nickname)

    fun setAppShortcutEnabled(shortcut: StaticShortcut, enabled: Boolean) {
        staticDataDelegate.setAppShortcutEnabled(shortcut, enabled)
    }

    fun setAppShortcutIconOverride(shortcut: StaticShortcut, iconBase64: String?) {
        staticDataDelegate.setAppShortcutIconOverride(shortcut, iconBase64)
    }

    fun getAppShortcutIconOverride(shortcutId: String): String? =
        staticDataDelegate.getAppShortcutIconOverride(shortcutId)

    fun getAppShortcutNickname(shortcutId: String): String? =
        appShortcutManager().getShortcutNickname(shortcutId)

    fun removeExcludedAppShortcut(shortcut: StaticShortcut) =
        appShortcutManager().removeExcludedShortcut(shortcut)

    fun addCustomAppShortcutFromPickerResult(
        resultData: Intent?,
        sourcePackageName: String?,
        showDefaultToast: Boolean,
        onShortcutAdded: ((StaticShortcut) -> Unit)?,
        onAddFailed: (() -> Unit)?,
    ) {
        staticDataDelegate.addCustomAppShortcutFromPickerResult(
            resultData = resultData,
            sourcePackageName = sourcePackageName,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )
    }

    fun addSearchTargetQueryShortcut(
        target: SearchTarget,
        shortcutName: String,
        shortcutQuery: String,
        mode: SearchTargetShortcutMode,
        showDefaultToast: Boolean,
        onShortcutAdded: ((StaticShortcut) -> Unit)?,
        onAddFailed: (() -> Unit)?,
    ) {
        staticDataDelegate.addSearchTargetQueryShortcut(
            target = target,
            shortcutName = shortcutName,
            shortcutQuery = shortcutQuery,
            mode = mode,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )
    }

    fun addCustomAppActivityShortcut(
        packageName: String,
        activityClassName: String,
        activityLabel: String,
        showDefaultToast: Boolean,
        onShortcutAdded: ((StaticShortcut) -> Unit)?,
        onAddFailed: (() -> Unit)?,
    ) {
        staticDataDelegate.addCustomAppActivityShortcut(
            packageName = packageName,
            activityClassName = activityClassName,
            activityLabel = activityLabel,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )
    }

    fun addCustomAppDeepLinkShortcut(
        packageName: String,
        shortcutName: String,
        deepLink: String,
        iconBase64: String?,
        showDefaultToast: Boolean,
        onShortcutAdded: ((StaticShortcut) -> Unit)?,
        onAddFailed: (() -> Unit)?,
    ) {
        staticDataDelegate.addCustomAppDeepLinkShortcut(
            packageName = packageName,
            shortcutName = shortcutName,
            deepLink = deepLink,
            iconBase64 = iconBase64,
            showDefaultToast = showDefaultToast,
            onShortcutAdded = onShortcutAdded,
            onAddFailed = onAddFailed,
        )
    }

    fun deleteCustomAppShortcut(shortcut: StaticShortcut) {
        staticDataDelegate.deleteCustomAppShortcut(shortcut)
    }

    fun updateCustomAppShortcut(
        shortcut: StaticShortcut,
        shortcutName: String,
        shortcutValue: String?,
        iconBase64: String?,
    ) {
        staticDataDelegate.updateCustomAppShortcut(shortcut, shortcutName, shortcutValue, iconBase64)
    }

    private fun refreshNotesState() {
        val repository = notesRepository()
        val allNotes = repository.getAllNotes()
        val pinnedIds = repository.getPinnedNoteIds()
        val pinnedNotes = allNotes.filter { pinnedIds.contains(it.noteId) }
        updateUiState { state ->
            val refreshedResults =
                if (state.query.isBlank()) {
                    allNotes
                } else {
                    repository.searchNotes(state.query)
                }
            state.copy(
                noteResults = refreshedResults,
                pinnedNotes = pinnedNotes,
            )
        }
    }

    fun clearAllExclusions() = staticDataDelegate.clearAllExclusions()
}
