package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class SearchHistoryDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val settingsSearchHandler: DeviceSettingsSearchHandler,
    private val appShortcutSearchHandler: AppShortcutSearchHandler,
    private val appSettingsSearchHandler: AppSettingsSearchHandler,
    private val calendarRepository: CalendarRepository,
    private val featureStateProvider: () -> SearchFeatureState,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    fun refreshRecentItems() {
        scope.launch(Dispatchers.IO) {
            if (!featureStateProvider().recentQueriesEnabled) {
                updateResultsState { it.copy(recentItems = emptyList()) }
                return@launch
            }

            val entries = userPreferences.getRecentItems().take(MAX_RECENT_ITEMS)

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

            val contactsById =
                contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri =
                fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()

            val items =
                buildList {
                    entries.forEach { entry ->
                        when (entry) {
                            is RecentSearchEntry.Query -> add(RecentSearchItem.Query(entry.trimmedQuery))
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
                            is RecentSearchEntry.AppSetting -> Unit
                        }
                    }
                }
            updateResultsState { it.copy(recentItems = items) }
        }
    }

    fun refreshAliasRecentItems(section: SearchSection?) {
        scope.launch(Dispatchers.IO) {
            if (section == SearchSection.CALENDAR) {
                val excludedEventIds = userPreferences.getExcludedCalendarEventIds()
                val upcoming =
                    calendarRepository.getUpcomingEventsSortedAscending(limit = MAX_RECENT_ITEMS)
                        .filterNot { excludedEventIds.contains(it.eventId) }
                updateUiState { it.copy(calendarEvents = upcoming, aliasRecentItems = emptyList()) }
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
                    else -> {
                        updateResultsState { it.copy(aliasRecentItems = emptyList()) }
                        return@launch
                    }
                }

            val limited = filteredEntries.take(MAX_RECENT_ITEMS)

            val contactIds = limited.filterIsInstance<RecentSearchEntry.Contact>().map { it.contactId }.toSet()
            val fileUris = limited.filterIsInstance<RecentSearchEntry.File>().map { it.uri }.toSet()
            val settingIds = limited.filterIsInstance<RecentSearchEntry.Setting>().map { it.id }.toSet()
            val shortcutKeys = limited.filterIsInstance<RecentSearchEntry.AppShortcut>().map { it.shortcutKey }.toSet()
            val appSettingIds = limited.filterIsInstance<RecentSearchEntry.AppSetting>().map { it.id }.toSet()

            val contactsById = contactRepository.getContactsByIds(contactIds).associateBy { it.contactId }
            val filesByUri = fileRepository.getFilesByUris(fileUris).associateBy { it.uri.toString() }
            val settingsById = settingsSearchHandler.getSettingsByIds(settingIds)
            val shortcutsByKey = appShortcutSearchHandler.getShortcutsByKeys(shortcutKeys)
            val appSettingsById = appSettingsSearchHandler.getSettingsByIds(appSettingIds)

            val pinnedContactIds = userPreferences.getPinnedContactIds()
            val pinnedFileUris = userPreferences.getPinnedFileUris()
            val pinnedSettingIds = userPreferences.getPinnedSettingIds()
            val pinnedAppShortcutIds = userPreferences.getPinnedAppShortcutIds()

            val items =
                buildList {
                    limited.forEach { entry ->
                        when (entry) {
                            is RecentSearchEntry.Contact -> contactsById[entry.contactId]?.let {
                                if (entry.contactId !in pinnedContactIds) add(RecentSearchItem.Contact(entry, it))
                            }
                            is RecentSearchEntry.File -> filesByUri[entry.uri]?.let {
                                if (entry.uri !in pinnedFileUris) add(RecentSearchItem.File(entry, it))
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
                            is RecentSearchEntry.Query -> Unit
                        }
                    }
                }
            updateResultsState { it.copy(aliasRecentItems = items) }
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

    fun trackRecentContactTap(contactInfo: ContactInfo) {
        scope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
        }
    }

    fun trackRecentSettingTap(settingId: String) {
        scope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.Setting(settingId))
        }
    }

    fun trackRecentAppSettingTap(
        settingId: String,
        lockedAliasSearchSection: SearchSection?,
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.addRecentItem(RecentSearchEntry.AppSetting(settingId))
            refreshAliasRecentItems(lockedAliasSearchSection)
        }
    }

    private companion object {
        const val MAX_RECENT_ITEMS = 10
    }
}
