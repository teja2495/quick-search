package com.tk.quicksearch.search.core

import android.content.Intent
import com.tk.quicksearch.search.data.appShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.preferences.ResultTrigger
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.noted.withoutQuickNoteWhileNoted
import com.tk.quicksearch.search.data.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModelManagementApiDelegate internal constructor(
    private val scope: CoroutineScope,
    private val userPreferences: com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences,
    private val resultsStateProvider: () -> SearchResultsState,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val historyDelegate: SearchHistoryDelegate,
    private val staticDataDelegate: SearchStaticDataDelegate,
    private val appManager: () -> com.tk.quicksearch.search.apps.AppManagementService,
    private val contactManager: () -> com.tk.quicksearch.search.contacts.ContactManagementHandler,
    private val fileManager: () -> com.tk.quicksearch.search.files.FileManagementHandler,
    private val settingsManager: () -> com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler,
    private val calendarManager: () -> com.tk.quicksearch.search.calendar.CalendarManagementHandler,
    private val reminderManager: () -> ReminderManagementHandler,
    private val appShortcutManager: () -> com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler,
    private val notesRepository: () -> com.tk.quicksearch.search.data.NotesRepository,
    private val appSearchManager: () -> com.tk.quicksearch.search.apps.AppSearchManager,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val legacyPreferenceState: SearchViewModelLegacyPreferenceState,
    private val lockedAliasSearchSectionProvider: () -> SearchSection?,
    private val refreshRecentItems: () -> Unit,
    private val uiStateFlow: kotlinx.coroutines.flow.StateFlow<SearchUiState>,
    private val refreshSecondarySearches: () -> Unit,
) {
    private var aliasFileTypeSearchJob: Job? = null

    val folderManager by lazy {
        com.tk.quicksearch.search.folders.FolderManager(
            scope = scope,
            userPreferences = userPreferences,
            resultsStateProvider = resultsStateProvider,
            updateFeatureState = updateFeatureState,
            pinApp = ::pinApp,
            unpinApp = ::unpinApp,
            pinShortcut = ::pinAppShortcut,
            unpinShortcut = ::unpinAppShortcut,
            reorderPinnedAppGrid = ::reorderPinnedAppGrid,
        )
    }

    fun deleteRecentItem(entry: RecentSearchEntry) {
        historyDelegate.deleteRecentItem(entry, lockedAliasSearchSectionProvider())
    }

    fun clearRecentItems() {
        historyDelegate.clearRecentItems(lockedAliasSearchSectionProvider())
    }

    fun setAliasFileTypeFilter(fileType: FileType?) {
        historyDelegate.setAliasFileTypeFilter(fileType)
        aliasFileTypeSearchJob?.cancel()
        if (resultsStateProvider().query.isBlank()) return
        // Searches read the combined UI state, which picks up the new chip a moment later.
        aliasFileTypeSearchJob =
            scope.launch {
                uiStateFlow.first { it.aliasFileTypeFilter == fileType }
                refreshSecondarySearches()
            }
    }

    suspend fun refreshAppShortcutsAndAwait() {
        staticDataDelegate.refreshAppShortcutsAndAwait()
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

    fun setNumberSearchEnabled(enabled: Boolean) {
        userPreferences.setNumberSearchEnabled(enabled)
        updateFeatureState { it.copy(numberSearchEnabled = enabled) }
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

    fun reorderPinnedApps(apps: List<AppInfo>) = appManager().reorderPinnedApps(apps)

    /** Persists a drag reorder of the app grid's mixed pinned apps and pinned shortcuts. */
    fun reorderPinnedAppGrid(
        orderKeys: List<String>,
        apps: List<AppInfo>,
        shortcuts: List<StaticShortcut>,
    ) {
        updateFeatureState { it.copy(pinnedAppGridOrder = orderKeys) }
        scope.launch(Dispatchers.IO) { userPreferences.setPinnedAppGridOrder(orderKeys) }
        if (apps.isNotEmpty()) appManager().reorderPinnedApps(apps)
        if (shortcuts.isEmpty()) return
        val shortcutRank = shortcuts.withIndex().associate { (index, shortcut) -> shortcutKey(shortcut) to index }
        var reordered: List<StaticShortcut> = emptyList()
        updateUiState { state ->
            state.pinnedAppShortcuts
                .sortedBy { shortcutRank[shortcutKey(it)] ?: Int.MAX_VALUE }
                .also { reordered = it }
                .let { state.copy(pinnedAppShortcuts = it) }
        }
        userPreferences.setPinnedAppShortcutOrder(reordered.map { shortcutKey(it) })
    }

    fun setAppNickname(appInfo: AppInfo, nickname: String?) = appManager().setAppNickname(appInfo, nickname)

    fun getAppNickname(appKey: String): String? = appManager().getAppNickname(appKey)

    fun setAppTrigger(appInfo: AppInfo, trigger: ResultTrigger?) {
        userPreferences.setAppTrigger(appInfo.launchCountKey(), trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getAppTrigger(packageName: String): ResultTrigger? = userPreferences.getAppTrigger(packageName)

    fun getAllTriggerWordsById(): Map<String, String> = userPreferences.getAllTriggerWordsById()

    fun getAllAliasWordsById(): Map<String, String> = userPreferences.getAllAliasWordsById()

    fun clearCachedApps() = appSearchManager().clearCachedApps()

    fun pinContact(contactInfo: ContactInfo) {
        contactManager().pinContact(contactInfo)
        appendPinnedNonAppItem(contactInfo.pinnedNonAppItemKey())
    }

    fun unpinContact(contactInfo: ContactInfo) {
        contactManager().unpinContact(contactInfo)
        removePinnedNonAppItem(contactInfo.pinnedNonAppItemKey())
        refreshRecentItems()
    }

    fun movePinnedContact(contactInfo: ContactInfo, moveUp: Boolean) =
        movePinnedItem(
            item = contactInfo,
            nonAppKey = contactInfo.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedContacts },
            sameItem = { a, b -> a.contactId == b.contactId },
            withItems = { copy(pinnedContacts = it) },
            persistOrder = { reordered -> userPreferences.setPinnedContactOrder(reordered.map { it.contactId }) },
        )

    fun excludeContact(contactInfo: ContactInfo) = contactManager().excludeContact(contactInfo)

    fun removeExcludedContact(contactInfo: ContactInfo) =
        contactManager().removeExcludedContact(contactInfo)

    fun clearAllExcludedContacts() = contactManager().clearAllExcludedContacts()

    fun setContactNickname(contactInfo: ContactInfo, nickname: String?) =
        contactManager().setContactNickname(contactInfo, nickname)

    fun getContactNickname(contactId: Long): String? = contactManager().getContactNickname(contactId)

    fun setContactTrigger(contactInfo: ContactInfo, trigger: ResultTrigger?) {
        userPreferences.setContactTrigger(contactInfo.contactId, trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getContactTrigger(contactId: Long): ResultTrigger? = userPreferences.getContactTrigger(contactId)

    fun setContactActionTrigger(
        contactInfo: ContactInfo,
        action: ContactCardAction,
        trigger: ResultTrigger?,
    ) {
        userPreferences.setContactActionTrigger(contactInfo.contactId, action, trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getContactActionTrigger(
        contactId: Long,
        action: ContactCardAction,
    ): ResultTrigger? = userPreferences.getContactActionTrigger(contactId, action)

    fun getAllContactActionTriggers():
        Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, ResultTrigger> =
        userPreferences.getAllContactActionTriggers()

    fun pinFile(deviceFile: DeviceFile) {
        fileManager().pinFile(deviceFile)
        appendPinnedNonAppItem(deviceFile.pinnedNonAppItemKey())
    }

    fun unpinFile(deviceFile: DeviceFile) {
        fileManager().unpinFile(deviceFile)
        removePinnedNonAppItem(deviceFile.pinnedNonAppItemKey())
        refreshRecentItems()
    }

    fun movePinnedFile(deviceFile: DeviceFile, moveUp: Boolean) =
        movePinnedItem(
            item = deviceFile,
            nonAppKey = deviceFile.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedFiles },
            sameItem = { a, b -> a.uri.toString() == b.uri.toString() },
            withItems = { copy(pinnedFiles = it) },
            persistOrder = { reordered -> userPreferences.setPinnedFileOrder(reordered.map { it.uri.toString() }) },
        )

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

    fun setFileTrigger(deviceFile: DeviceFile, trigger: ResultTrigger?) {
        userPreferences.setFileTrigger(deviceFile.uri.toString(), trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getFileTrigger(uri: String): ResultTrigger? = userPreferences.getFileTrigger(uri)

    fun pinSetting(setting: DeviceSetting) {
        settingsManager().pinSetting(setting)
        appendPinnedNonAppItem(setting.pinnedNonAppItemKey())
    }

    fun unpinSetting(setting: DeviceSetting) {
        settingsManager().unpinSetting(setting)
        removePinnedNonAppItem(setting.pinnedNonAppItemKey())
        refreshRecentItems()
    }

    fun movePinnedSetting(setting: DeviceSetting, moveUp: Boolean) =
        movePinnedItem(
            item = setting,
            nonAppKey = setting.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedSettings },
            sameItem = { a, b -> a.id == b.id },
            withItems = { copy(pinnedSettings = it) },
            persistOrder = { reordered -> userPreferences.setPinnedSettingOrder(reordered.map { it.id }) },
        )

    fun excludeSetting(setting: DeviceSetting) = settingsManager().excludeSetting(setting)

    fun setSettingNickname(setting: DeviceSetting, nickname: String?) =
        settingsManager().setSettingNickname(setting, nickname)

    fun getSettingNickname(id: String): String? = settingsManager().getSettingNickname(id)

    fun setSettingTrigger(setting: DeviceSetting, trigger: ResultTrigger?) {
        userPreferences.setSettingTrigger(setting.id, trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getSettingTrigger(id: String): ResultTrigger? = userPreferences.getSettingTrigger(id)

    fun removeExcludedSetting(setting: DeviceSetting) = settingsManager().removeExcludedSetting(setting)

    fun clearAllExcludedSettings() = settingsManager().clearAllExcludedSettings()

    fun pinCalendarEvent(event: CalendarEventInfo) {
        calendarManager().pinItem(event)
        appendPinnedNonAppItem(event.pinnedNonAppItemKey())
    }

    fun unpinCalendarEvent(event: CalendarEventInfo) {
        calendarManager().unpinItem(event)
        removePinnedNonAppItem(event.pinnedNonAppItemKey())
    }

    fun movePinnedCalendarEvent(event: CalendarEventInfo, moveUp: Boolean) =
        movePinnedItem(
            item = event,
            nonAppKey = event.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedCalendarEvents },
            sameItem = { a, b -> a.eventId == b.eventId },
            withItems = { copy(pinnedCalendarEvents = it) },
            persistOrder = { reordered -> userPreferences.setPinnedCalendarEventOrder(reordered.map { it.eventId }) },
        )

    fun excludeCalendarEvent(event: CalendarEventInfo) = calendarManager().excludeItem(event)

    fun removeExcludedCalendarEvent(event: CalendarEventInfo) = calendarManager().removeExcludedItem(event)

    fun clearAllExcludedCalendarEvents() = calendarManager().clearAllExcludedItems()

    fun setCalendarEventNickname(event: CalendarEventInfo, nickname: String?) =
        calendarManager().setItemNickname(event, nickname)

    fun getCalendarEventNickname(eventId: Long): String? = userPreferences.getCalendarEventNickname(eventId)

    fun pinReminder(reminder: ReminderInfo) {
        reminderManager().pinItem(reminder)
        appendPinnedNonAppItem(reminder.pinnedNonAppItemKey())
    }

    fun unpinReminder(reminder: ReminderInfo) {
        reminderManager().unpinItem(reminder)
        removePinnedNonAppItem(reminder.pinnedNonAppItemKey())
    }

    fun movePinnedReminder(reminder: ReminderInfo, moveUp: Boolean) =
        movePinnedItem(
            item = reminder,
            nonAppKey = reminder.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedReminders },
            sameItem = { a, b -> a.reminderId == b.reminderId },
            withItems = { copy(pinnedReminders = it) },
            persistOrder = { reordered -> userPreferences.setPinnedReminderOrder(reordered.map { it.reminderId }) },
        )

    fun pinNote(noteInfo: NoteInfo) {
        scope.launch(Dispatchers.IO) {
            notesRepository().pinNote(noteInfo.noteId)
            appendPinnedNonAppItem(noteInfo.pinnedNonAppItemKey())
            refreshNotesState()
        }
    }

    fun unpinNote(noteInfo: NoteInfo) {
        scope.launch(Dispatchers.IO) {
            notesRepository().unpinNote(noteInfo.noteId)
            removePinnedNonAppItem(noteInfo.pinnedNonAppItemKey())
            refreshNotesState()
            refreshRecentItems()
        }
    }

    fun movePinnedNote(noteInfo: NoteInfo, moveUp: Boolean) =
        movePinnedItem(
            item = noteInfo,
            nonAppKey = noteInfo.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedNotes },
            sameItem = { a, b -> a.noteId == b.noteId },
            withItems = { copy(pinnedNotes = it) },
            persistOrder = { reordered -> notesRepository().setPinnedNoteOrder(reordered.map { it.noteId }) },
        )

    suspend fun stageDeleteNote(noteInfo: NoteInfo): NoteInfo? {
        val deleted =
            withContext(Dispatchers.IO) { notesRepository().stageDelete(noteInfo.noteId) } ?: return null
        // Drop the row right away; the refresh below reloads both lists from storage.
        updateUiState { state ->
            state.copy(
                noteResults = state.noteResults.filterNot { it.noteId == deleted.noteId },
                pinnedNotes = state.pinnedNotes.filterNot { it.noteId == deleted.noteId },
            )
        }
        refreshNotesState()
        return deleted
    }

    fun undoDeleteNote(noteId: Long) {
        scope.launch(Dispatchers.IO) {
            notesRepository().undoDelete(noteId)
            refreshNotesState()
        }
    }

    fun finalizeDeleteNote(noteId: Long) {
        scope.launch(Dispatchers.IO) {
            notesRepository().finalizeDelete(noteId)
            refreshNotesState()
        }
    }

    fun setNoteTrigger(noteInfo: NoteInfo, trigger: ResultTrigger?) {
        userPreferences.setNoteTrigger(noteInfo.noteId, trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
        refreshNotesState()
    }

    fun getNoteTrigger(noteId: Long): ResultTrigger? = userPreferences.getNoteTrigger(noteId)

    fun pinAppShortcut(shortcut: StaticShortcut) {
        appShortcutManager().pinShortcut(shortcut)
        appendPinnedNonAppItem(shortcut.pinnedNonAppItemKey())
    }

    fun unpinAppShortcut(shortcut: StaticShortcut) {
        appShortcutManager().unpinShortcut(shortcut)
        removePinnedNonAppItem(shortcut.pinnedNonAppItemKey())
        refreshRecentItems()
    }

    fun movePinnedAppShortcut(shortcut: StaticShortcut, moveUp: Boolean) =
        movePinnedItem(
            item = shortcut,
            nonAppKey = shortcut.pinnedNonAppItemKey(),
            moveUp = moveUp,
            items = { pinnedAppShortcuts },
            sameItem = { a, b -> shortcutKey(a) == shortcutKey(b) },
            withItems = { copy(pinnedAppShortcuts = it) },
            persistOrder = { reordered -> userPreferences.setPinnedAppShortcutOrder(reordered.map { shortcutKey(it) }) },
        )

    fun excludeAppShortcut(shortcut: StaticShortcut) = appShortcutManager().excludeShortcut(shortcut)

    fun setAppShortcutNickname(shortcut: StaticShortcut, nickname: String?) =
        appShortcutManager().setShortcutNickname(shortcut, nickname)

    fun setAppShortcutEnabled(shortcut: StaticShortcut, enabled: Boolean) {
        staticDataDelegate.setAppShortcutEnabled(shortcut, enabled)
    }

    fun setAppShortcutsEnabled(shortcutIds: Collection<String>, enabled: Boolean) {
        staticDataDelegate.setAppShortcutsEnabled(shortcutIds, enabled)
    }

    fun setAllAppShortcutsEnabled(packageName: String, enabled: Boolean) {
        staticDataDelegate.setAllAppShortcutsEnabled(packageName, enabled)
    }

    fun setAppShortcutIconOverride(shortcut: StaticShortcut, iconBase64: String?) {
        staticDataDelegate.setAppShortcutIconOverride(shortcut, iconBase64)
    }

    fun getAppShortcutIconOverride(shortcutId: String): String? =
        staticDataDelegate.getAppShortcutIconOverride(shortcutId)

    fun getAppShortcutNickname(shortcutId: String): String? =
        appShortcutManager().getShortcutNickname(shortcutId)

    fun setAppShortcutTrigger(shortcut: StaticShortcut, trigger: ResultTrigger?) {
        userPreferences.setAppShortcutTrigger(shortcutKey(shortcut), trigger)
        updateUiState { it.copy(nicknameUpdateVersion = it.nicknameUpdateVersion + 1) }
    }

    fun getAppShortcutTrigger(shortcutId: String): ResultTrigger? =
        userPreferences.getAppShortcutTrigger(shortcutId)

    // Updaters run under the state lock on the caller's thread, so they only compute; the
    // preference writes follow once the lock is released.
    private fun appendPinnedNonAppItem(key: String) {
        var updatedOrder: List<String>? = null
        updateUiState { state ->
            if (key in state.pinnedNonAppItemOrder) {
                state
            } else {
                (state.pinnedNonAppItemOrder + key)
                    .also { updatedOrder = it }
                    .let { state.copy(pinnedNonAppItemOrder = it) }
            }
        }
        updatedOrder?.let(userPreferences::setPinnedNonAppItemOrder)
    }

    private fun removePinnedNonAppItem(key: String) {
        var updatedOrder: List<String>? = null
        updateUiState { state ->
            if (key !in state.pinnedNonAppItemOrder) {
                state
            } else {
                state.pinnedNonAppItemOrder.filterNot { it == key }
                    .also { updatedOrder = it }
                    .let { state.copy(pinnedNonAppItemOrder = it) }
            }
        }
        updatedOrder?.let(userPreferences::setPinnedNonAppItemOrder)
    }

    /** Moves [item] within its pinned list and within the mixed pinned order, then saves both. */
    private fun <T> movePinnedItem(
        item: T,
        nonAppKey: String,
        moveUp: Boolean,
        items: SearchUiState.() -> List<T>,
        sameItem: (T, T) -> Boolean,
        withItems: SearchUiState.(List<T>) -> SearchUiState,
        persistOrder: (List<T>) -> Unit,
    ) {
        var reordered: List<T>? = null
        var updatedOrder: List<String>? = null
        updateUiState { state ->
            val movedItems = state.items().moveItem(item = item, moveUp = moveUp, sameItem = sameItem)
            val currentOrder = state.completePinnedNonAppItemOrder()
            val movedOrder = currentOrder.moveItem(nonAppKey, moveUp) { a, b -> a == b } ?: currentOrder
            reordered = movedItems
            updatedOrder = movedOrder
            state.withItems(movedItems ?: state.items()).copy(pinnedNonAppItemOrder = movedOrder)
        }
        reordered?.let(persistOrder)
        updatedOrder?.let(userPreferences::setPinnedNonAppItemOrder)
    }

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

    fun refreshNotes() {
        refreshNotesState()
    }

    private val refreshNotesLock = Any()
    private var refreshNotesJob: Job? = null

    /**
     * Reloads notes on IO and applies them; reads happen before the state lock is taken. A newer
     * refresh replaces one still running, so an older read never lands last.
     */
    private fun refreshNotesState() {
        synchronized(refreshNotesLock) {
            refreshNotesJob?.cancel()
            refreshNotesJob =
                scope.launch(Dispatchers.IO) {
                    val repository = notesRepository()
                    val allNotes = repository.getAllNotes()
                    val pinnedIds = repository.getPinnedNoteIds()
                    val pinnedNotes =
                        allNotes
                            .filter { pinnedIds.contains(it.noteId) }
                            .sortedByPinnedOrder(repository.getPinnedNoteOrder()) { it.noteId }
                    val snapshot = resultsStateProvider()
                    val query = snapshot.query
                    val isNotesAlias = snapshot.detectedAliasSearchSection == SearchSection.NOTES
                    val refreshedResults =
                        (
                            if (query.isBlank()) {
                                allNotes
                            } else {
                                repository.searchNotes(
                                    query = query,
                                    includeContent = isNotesAlias,
                                    resultLimit =
                                        if (isNotesAlias) {
                                            NotesRepository.ALIAS_SEARCH_RESULT_LIMIT
                                        } else {
                                            NotesRepository.SEARCH_RESULT_LIMIT
                                        },
                                )
                            }
                        ).withoutQuickNoteWhileNoted(repository)
                    ensureActive()
                    updateUiState { state ->
                        state.copy(
                            // The query moved on while reading; its own search fills the results.
                            noteResults = if (state.query == query) refreshedResults else state.noteResults,
                            pinnedNotes = pinnedNotes,
                        )
                    }
                }
        }
    }

    fun clearAllExclusions() = staticDataDelegate.clearAllExclusions()
}

private fun <T, K> List<T>.sortedByPinnedOrder(
    order: List<K>,
    keySelector: (T) -> K,
): List<T> {
    if (order.isEmpty()) return this
    val orderIndex = order.withIndex().associate { it.value to it.index }
    return sortedBy { orderIndex[keySelector(it)] ?: Int.MAX_VALUE }
}

private fun <T> List<T>.moveItem(
    item: T,
    moveUp: Boolean,
    sameItem: (T, T) -> Boolean,
): List<T>? {
    val currentIndex = indexOfFirst { sameItem(it, item) }
    if (currentIndex == -1) return null
    val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
    if (targetIndex !in indices) return null
    return toMutableList().apply {
        val moved = removeAt(currentIndex)
        add(targetIndex, moved)
    }
}

private fun ContactInfo.pinnedNonAppItemKey(): String = "contact:$contactId"

private fun DeviceFile.pinnedNonAppItemKey(): String = "file:${uri}"

private fun DeviceSetting.pinnedNonAppItemKey(): String = "setting:$id"

private fun CalendarEventInfo.pinnedNonAppItemKey(): String = "calendar:$eventId"

private fun NoteInfo.pinnedNonAppItemKey(): String = "note:$noteId"

private fun ReminderInfo.pinnedNonAppItemKey(): String = "reminder:$reminderId"

private fun StaticShortcut.pinnedNonAppItemKey(): String = "shortcut:${shortcutKey(this)}"

private fun SearchUiState.completePinnedNonAppItemOrder(): List<String> {
    val liveKeys =
        buildList {
            addAll(pinnedNonAppItemOrder.filter(OtherSearchItemRegistry::isOtherPinnedItemKey))
            addAll(pinnedAppShortcuts.map { it.pinnedNonAppItemKey() })
            addAll(pinnedContacts.map { it.pinnedNonAppItemKey() })
            addAll(pinnedFiles.map { it.pinnedNonAppItemKey() })
            addAll(pinnedCalendarEvents.map { it.pinnedNonAppItemKey() })
            addAll(pinnedReminders.map { it.pinnedNonAppItemKey() })
            addAll(pinnedSettings.map { it.pinnedNonAppItemKey() })
            addAll(pinnedNotes.map { it.pinnedNonAppItemKey() })
        }
    val liveKeySet = liveKeys.toSet()
    return pinnedNonAppItemOrder.filter { it in liveKeySet } + liveKeys.filterNot { it in pinnedNonAppItemOrder }
}
