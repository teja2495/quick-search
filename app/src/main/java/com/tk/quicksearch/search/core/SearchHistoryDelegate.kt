package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.tk.quicksearch.app.startup.StartupTrace

internal class SearchHistoryDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val settingsSearchHandler: DeviceSettingsSearchHandler,
    private val appShortcutSearchHandler: AppShortcutSearchHandler,
    private val appSettingsSearchHandler: AppSettingsSearchHandler,
    private val calendarRepository: CalendarRepository,
    private val notesRepository: NotesRepository,
    private val reminderRepository: ReminderRepository,
    private val featureStateProvider: () -> SearchFeatureState,
    private val configStateProvider: () -> SearchUiConfigState,
    private val currentQueryProvider: () -> String,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    private var recentItemsRefreshJob: Job? = null
    // Selected file type chip in the blank-query Files alias view; null means All.
    private var aliasFileTypeFilter: FileType? = null
    private val aliasFillLoader by lazy {
        AliasRecentFillLoader(
            userPreferences = userPreferences,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            settingsSearchHandler = settingsSearchHandler,
            appShortcutSearchHandler = appShortcutSearchHandler,
            appSettingsSearchHandler = appSettingsSearchHandler,
            notesRepository = notesRepository,
            configStateProvider = configStateProvider,
        )
    }

    fun refreshRecentItems() {
        recentItemsRefreshJob?.cancel()
        recentItemsRefreshJob = scope.launch(Dispatchers.IO) {
            if (!featureStateProvider().recentQueriesEnabled) {
                updateResultsState { it.copy(recentItems = emptyList()) }
                StartupTrace.mark("QS.Home.SearchHistoryAvailable")
                return@launch
            }

            val queryEntries =
                userPreferences
                    .getRecentItems()
                    .filterIsInstance<RecentSearchEntry.Query>()
                    .take(MAX_RECENT_ITEMS)
            val openedEntries = userPreferences.getRecentResultOpens().take(MAX_RECENT_ITEMS)
            val entries = queryEntries + openedEntries

            val contactIds =
                entries.filterIsInstance<RecentSearchEntry.Contact>()
                    .map { it.contactId }
                    .toSet()
            val fileUris = entries.filterIsInstance<RecentSearchEntry.File>().map { it.uri }.toSet()
            val settingIds =
                entries.filterIsInstance<RecentSearchEntry.Setting>().map { it.id }.toSet()
            val shortcutKeys =
                entries.filterIsInstance<RecentSearchEntry.AppShortcut>()
                    .map { it.shortcutKey }
                    .toSet()
            val appSettingIds =
                entries.filterIsInstance<RecentSearchEntry.AppSetting>()
                    .map { it.id }
                    .toSet()
            val noteIds =
                entries.filterIsInstance<RecentSearchEntry.Note>()
                    .map { it.noteId }
                    .toSet()

            val contactsById =
                contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri =
                fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)
            val appSettingsById = appSettingsSearchHandler.getSettingsByIds(appSettingIds)
            val notesById =
                noteIds
                    .mapNotNull { notesRepository.getNoteById(it) }
                    .filterNot { notesRepository.isQuickNote(it.noteId) }
                    .associateBy { it.noteId }

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()
            val pinnedNoteIds = notesRepository.getPinnedNoteIds()

            val items =
                buildList {
                    entries.forEach { entry ->
                        when (entry) {
                            is RecentSearchEntry.Query -> add(RecentSearchItem.Query(entry))
                            is RecentSearchEntry.Contact -> {
                                contactsById[entry.contactId]?.let {
                                    if (entry.contactId !in pinnedContactIds) {
                                        add(RecentSearchItem.Contact(entry, it))
                                    }
                                }
                            }
                            is RecentSearchEntry.File -> {
                                filesByUri[entry.uri]?.let {
                                    if (entry.uri !in pinnedFileUris) {
                                        add(RecentSearchItem.File(entry, it))
                                    }
                                }
                            }
                            is RecentSearchEntry.Setting -> {
                                settingsById[entry.id]?.let {
                                    if (entry.id !in pinnedSettingIds) {
                                        add(RecentSearchItem.Setting(entry, it))
                                    }
                                }
                            }
                            is RecentSearchEntry.AppShortcut -> {
                                shortcutsByKey[entry.shortcutKey]?.let {
                                    if (entry.shortcutKey !in pinnedAppShortcutIds) {
                                        add(RecentSearchItem.AppShortcut(entry, it))
                                    }
                                }
                            }
                            is RecentSearchEntry.AppSetting -> {
                                appSettingsById[entry.id]?.let {
                                    add(RecentSearchItem.AppSetting(entry, it))
                                }
                            }
                            is RecentSearchEntry.Note -> {
                                notesById[entry.noteId]?.let {
                                    if (entry.noteId !in pinnedNoteIds) {
                                        add(RecentSearchItem.Note(entry, it))
                                    }
                                }
                            }
                        }
                    }
                }
            updateResultsState { it.copy(recentItems = items) }
            StartupTrace.mark("QS.Home.SearchHistoryAvailable")
        }
    }

    suspend fun awaitRecentItemsReady() {
        recentItemsRefreshJob?.join()
    }

    fun setAliasFileTypeFilter(fileType: FileType?) {
        aliasFileTypeFilter = fileType
        updateResultsState { it.copy(aliasFileTypeFilter = fileType) }
        refreshAliasRecentItems(SearchSection.FILES)
    }

    fun refreshAliasRecentItems(section: SearchSection?) {
        if (section != SearchSection.FILES) aliasFileTypeFilter = null
        val fileTypeFilter = aliasFileTypeFilter
        scope.launch(Dispatchers.IO) {
            if (section == SearchSection.CALENDAR) {
                val excludedEventIds = userPreferences.getExcludedCalendarEventIds()
                // Recently opened events first, then upcoming ones.
                val events =
                    (
                        recentlyOpenedCalendarEvents() +
                            calendarRepository.getUpcomingEventsSortedAscending(limit = MAX_ALIAS_ITEMS)
                    ).distinctBy { it.eventId }
                        .filterNot { excludedEventIds.contains(it.eventId) }
                        .take(MAX_ALIAS_ITEMS)
                updateUiState { it.copy(calendarEvents = events, aliasRecentItems = emptyList()) }
                return@launch
            }
            if (section == SearchSection.REMINDERS) {
                val upcoming =
                    reminderRepository.getUpcomingRemindersAndNotedTasks(limit = MAX_ALIAS_ITEMS)
                updateUiState { it.copy(reminderResults = upcoming, aliasRecentItems = emptyList()) }
                return@launch
            }

            val allOpens = userPreferences.getRecentResultOpens()
            val filteredEntries: List<RecentSearchEntry> =
                when (section) {
                    SearchSection.CONTACTS -> allOpens.filterIsInstance<RecentSearchEntry.Contact>()
                    SearchSection.FILES -> allOpens.filterIsInstance<RecentSearchEntry.File>()
                    SearchSection.SETTINGS -> allOpens.filterIsInstance<RecentSearchEntry.Setting>()
                    SearchSection.APP_SHORTCUTS -> allOpens.filterIsInstance<RecentSearchEntry.AppShortcut>()
                    SearchSection.APP_SETTINGS -> allOpens.filterIsInstance<RecentSearchEntry.AppSetting>()
                    SearchSection.NOTES -> allOpens.filterIsInstance<RecentSearchEntry.Note>()
                    else -> {
                        updateResultsState {
                            it.copy(
                                aliasRecentItems = emptyList(),
                                aliasRecentRemovableKeys = emptySet(),
                                aliasFileTypeFilter = null,
                            )
                        }
                        return@launch
                    }
                }

            val limited = filteredEntries.take(MAX_ALIAS_ITEMS)

            val contactIds = limited.filterIsInstance<RecentSearchEntry.Contact>().map { it.contactId }.toSet()
            val fileUris = limited.filterIsInstance<RecentSearchEntry.File>().map { it.uri }.toSet()
            val settingIds = limited.filterIsInstance<RecentSearchEntry.Setting>().map { it.id }.toSet()
            val shortcutKeys = limited.filterIsInstance<RecentSearchEntry.AppShortcut>().map { it.shortcutKey }.toSet()
            val appSettingIds = limited.filterIsInstance<RecentSearchEntry.AppSetting>().map { it.id }.toSet()
            val noteIds = limited.filterIsInstance<RecentSearchEntry.Note>().map { it.noteId }.toSet()

            val contactsById = contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri = fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)
            val appSettingsById = appSettingsSearchHandler.getSettingsByIds(appSettingIds)
            val notesById =
                noteIds
                    .mapNotNull { notesRepository.getNoteById(it) }
                    .filterNot { notesRepository.isQuickNote(it.noteId) }
                    .associateBy { it.noteId }

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()
            val pinnedNoteIds = notesRepository.getPinnedNoteIds()

            val recentItems =
                buildList {
                    limited.forEach { entry ->
                        when (entry) {
                            is RecentSearchEntry.Contact -> contactsById[entry.contactId]?.let {
                                if (entry.contactId !in pinnedContactIds) add(RecentSearchItem.Contact(entry, it))
                            }
                            is RecentSearchEntry.File -> filesByUri[entry.uri]?.let {
                                if (entry.uri !in pinnedFileUris && matchesFileType(it, fileTypeFilter)) {
                                    add(RecentSearchItem.File(entry, it))
                                }
                            }
                            is RecentSearchEntry.Setting -> settingsById[entry.id]?.let {
                                if (entry.id !in pinnedSettingIds) add(RecentSearchItem.Setting(entry, it))
                            }
                            is RecentSearchEntry.AppShortcut -> shortcutsByKey[entry.shortcutKey]?.let {
                                if (entry.shortcutKey !in pinnedAppShortcutIds) add(RecentSearchItem.AppShortcut(entry, it))
                            }
                            is RecentSearchEntry.AppSetting -> appSettingsById[entry.id]?.let {
                                add(RecentSearchItem.AppSetting(entry, it))
                            }
                            is RecentSearchEntry.Note -> notesById[entry.noteId]?.let {
                                if (entry.noteId !in pinnedNoteIds) add(RecentSearchItem.Note(entry, it))
                            }
                            is RecentSearchEntry.Query -> Unit
                        }
                    }
                }.take(MAX_ALIAS_ITEMS)
            val removableKeys = recentItems.map { it.entry.stableKey }.toSet()
            // Opened items stay on top; the rest of the section fills the list up to the cap.
            val fillItems =
                aliasFillLoader.loadFill(
                    section = section,
                    shownKeys = removableKeys,
                    limit = MAX_ALIAS_ITEMS - recentItems.size,
                    fileTypeFilter = fileTypeFilter,
                )
            updateResultsState {
                it.copy(
                    aliasRecentItems = recentItems + fillItems,
                    aliasRecentRemovableKeys = removableKeys,
                    aliasFileTypeFilter = fileTypeFilter,
                )
            }
        }
    }

    fun deleteRecentItem(
        entry: RecentSearchEntry,
        lockedAliasSearchSection: SearchSection?,
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.deleteRecentItem(entry)
            refreshRecentItems()
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    fun clearRecentItems(lockedAliasSearchSection: SearchSection?) {
        scope.launch(Dispatchers.IO) {
            userPreferences.clearRecentQueries()
            refreshRecentItems()
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    fun trackRecentContactTap(contactInfo: ContactInfo) {
        scope.launch(Dispatchers.IO) {
            recordQueryBeforeOpen()
            userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
        }
    }

    fun trackRecentSettingTap(settingId: String) {
        scope.launch(Dispatchers.IO) {
            recordQueryBeforeOpen()
            userPreferences.addRecentItem(RecentSearchEntry.Setting(settingId))
        }
    }

    fun trackRecentAppSettingTap(
        settingId: String,
        lockedAliasSearchSection: SearchSection?,
    ) {
        scope.launch(Dispatchers.IO) {
            recordQueryBeforeOpen()
            userPreferences.addRecentItem(RecentSearchEntry.AppSetting(settingId))
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    fun trackRecentNoteTap(noteInfo: NoteInfo) {
        scope.launch(Dispatchers.IO) {
            if (notesRepository.isQuickNote(noteInfo.noteId)) return@launch
            recordQueryBeforeOpen()
            userPreferences.addRecentItem(RecentSearchEntry.Note(noteInfo.noteId))
        }
    }

    private fun recordQueryBeforeOpen() {
        val query = currentQueryProvider().trim()
        if (query.isNotEmpty()) {
            userPreferences.addRecentItem(RecentSearchEntry.Query(query))
        }
    }

    private fun recentlyOpenedCalendarEvents(): List<CalendarEventInfo> {
        val eventIds =
            userPreferences
                .getRecentResultLastOpenedTimes()
                .filterKeys { it.startsWith(CALENDAR_OPEN_KEY_PREFIX) }
                .entries
                .sortedByDescending { it.value }
                .mapNotNull { it.key.removePrefix(CALENDAR_OPEN_KEY_PREFIX).toLongOrNull() }
                .take(MAX_ALIAS_ITEMS)
        if (eventIds.isEmpty()) return emptyList()
        val eventsById =
            calendarRepository.getEventsByIds(eventIds.toSet()).associateBy { it.eventId }
        return eventIds.mapNotNull { eventsById[it] }
    }

    private companion object {
        const val MAX_RECENT_ITEMS = 15
        // Blank-query alias view: recently opened items first, then the rest of the section.
        const val MAX_ALIAS_ITEMS = 50
        const val CALENDAR_OPEN_KEY_PREFIX = "calendar:"
    }
}
