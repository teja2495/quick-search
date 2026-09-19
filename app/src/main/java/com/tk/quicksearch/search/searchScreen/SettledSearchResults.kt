package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import kotlinx.coroutines.delay

private const val TOP_MATCH_SETTLE_DEADLINE_MS = 110L

internal class SettledSearchResultsBuffer<T>(private val emptyValue: T) {
    private var settledQuery = ""
    private var settledValue = emptyValue

    fun displayedValue(
        query: String,
        currentValue: T,
        isSearchRefreshing: Boolean,
    ): T {
        if (query.isBlank()) {
            settledQuery = ""
            settledValue = emptyValue
        } else if (!isSearchRefreshing) {
            settledQuery = query
            settledValue = currentValue
        }

        val canReuseSettledValue = queriesAreOnSamePrefixPath(query, settledQuery)
        return if (isSearchRefreshing && !canReuseSettledValue) emptyValue else settledValue
    }
}

private fun queriesAreOnSamePrefixPath(
    first: String,
    second: String,
): Boolean =
    first.isNotBlank() &&
        second.isNotBlank() &&
        (first.startsWith(second, ignoreCase = true) || second.startsWith(first, ignoreCase = true))

internal data class SettledTopMatchesState(
    val matches: List<TopMatchItem> = emptyList(),
    val isReady: Boolean = false,
)

internal class StableTopMatchesBuffer {
    private var activeQuery = ""
    private var state = SettledTopMatchesState()

    fun displayedValue(
        query: String,
        currentMatches: List<TopMatchItem>,
        isSearchRefreshing: Boolean,
        deadlineReached: Boolean,
        limit: Int,
    ): SettledTopMatchesState {
        if (query.isBlank()) {
            activeQuery = ""
            state = SettledTopMatchesState()
            return state
        }
        if (query != activeQuery) {
            val canReuseSettledMatches = queriesAreOnSamePrefixPath(query, activeQuery)
            activeQuery = query
            state =
                SettledTopMatchesState(
                    matches = if (canReuseSettledMatches) state.matches else emptyList(),
                    isReady = false,
                )
        }

        if (!state.isReady && (!isSearchRefreshing || deadlineReached)) {
            state = SettledTopMatchesState(currentMatches.take(limit), isReady = true)
        } else if (state.isReady && !isSearchRefreshing) {
            val mergedMatches = state.matches.toMutableList()
            val existingIndexByKey =
                mergedMatches.mapIndexed { index, match -> match.stableKey() to index }.toMap()
            currentMatches.forEach { candidate ->
                val existingIndex = existingIndexByKey[candidate.stableKey()]
                if (existingIndex == null) {
                    if (mergedMatches.size < limit) mergedMatches += candidate
                } else {
                    val existing = mergedMatches[existingIndex]
                    if (existing is TopMatchItem.AppGrid && candidate is TopMatchItem.AppGrid) {
                        val packageNames = existing.apps.mapTo(mutableSetOf()) { it.packageName }
                        mergedMatches[existingIndex] =
                            existing.copy(
                                apps = existing.apps + candidate.apps.filter { packageNames.add(it.packageName) },
                            )
                    }
                }
            }
            state = state.copy(matches = mergedMatches)
        }
        return state
    }
}

@Composable
internal fun rememberSettledTopMatches(
    query: String,
    currentMatches: List<TopMatchItem>,
    isSearchRefreshing: Boolean,
    limit: Int,
): SettledTopMatchesState {
    var deadlineReached by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(TOP_MATCH_SETTLE_DEADLINE_MS)
            deadlineReached = true
        }
    }
    return remember { StableTopMatchesBuffer() }
        .displayedValue(
            query = query,
            currentMatches = currentMatches,
            isSearchRefreshing = isSearchRefreshing,
            deadlineReached = deadlineReached,
            limit = limit,
        )
}

internal fun TopMatchItem.stableKey(): String =
    when (this) {
        is TopMatchItem.App -> "app:${app.packageName}"
        is TopMatchItem.AppGrid -> "app-grid"
        is TopMatchItem.AppShortcut -> "shortcut:${shortcutKey(shortcut)}"
        is TopMatchItem.Contact -> "contact:${contact.contactId}"
        is TopMatchItem.File -> "file:${file.uri}"
        is TopMatchItem.Setting -> "setting:${setting.id}"
        is TopMatchItem.AppSetting -> "app-setting:${setting.id}"
        is TopMatchItem.Calendar -> "calendar:${event.eventId}"
        is TopMatchItem.Reminder -> "reminder:${reminder.reminderId}"
        is TopMatchItem.Note -> "note:${note.noteId}"
        is TopMatchItem.Other -> "other:$itemId"
    }

private data class RegularSearchResultsSnapshot(
    val apps: List<AppInfo> = emptyList(),
    val appShortcuts: List<StaticShortcut> = emptyList(),
    val contacts: List<ContactInfo> = emptyList(),
    val files: List<DeviceFile> = emptyList(),
    val settings: List<DeviceSetting> = emptyList(),
    val appSettings: List<AppSettingResult> = emptyList(),
    val calendarEvents: List<CalendarEventInfo> = emptyList(),
    val reminders: List<ReminderInfo> = emptyList(),
    val notes: List<NoteInfo> = emptyList(),
)

@Composable
internal fun rememberSettledRegularSearchRenderingState(
    query: String,
    currentState: SectionRenderingState,
    isAppSearchRefreshing: Boolean,
    secondarySectionsRefreshing: Set<SearchSection>,
): SectionRenderingState {
    val displayedSnapshot =
        RegularSearchResultsSnapshot(
            apps =
                remember { SettledSearchResultsBuffer(emptyList<AppInfo>()) }
                    .displayedValue(query, currentState.displayApps, isAppSearchRefreshing),
            appShortcuts =
                remember { SettledSearchResultsBuffer(emptyList<StaticShortcut>()) }
                    .displayedValue(
                        query,
                        currentState.appShortcutResults,
                        SearchSection.APP_SHORTCUTS in secondarySectionsRefreshing,
                    ),
            contacts =
                remember { SettledSearchResultsBuffer(emptyList<ContactInfo>()) }
                    .displayedValue(
                        query,
                        currentState.contactResults,
                        SearchSection.CONTACTS in secondarySectionsRefreshing,
                    ),
            files =
                remember { SettledSearchResultsBuffer(emptyList<DeviceFile>()) }
                    .displayedValue(
                        query,
                        currentState.fileResults,
                        SearchSection.FILES in secondarySectionsRefreshing,
                    ),
            settings =
                remember { SettledSearchResultsBuffer(emptyList<DeviceSetting>()) }
                    .displayedValue(
                        query,
                        currentState.settingResults,
                        SearchSection.SETTINGS in secondarySectionsRefreshing,
                    ),
            appSettings =
                remember { SettledSearchResultsBuffer(emptyList<AppSettingResult>()) }
                    .displayedValue(
                        query,
                        currentState.appSettingResults,
                        SearchSection.APP_SETTINGS in secondarySectionsRefreshing,
                    ),
            calendarEvents =
                remember { SettledSearchResultsBuffer(emptyList<CalendarEventInfo>()) }
                    .displayedValue(
                        query,
                        currentState.calendarEvents,
                        SearchSection.CALENDAR in secondarySectionsRefreshing,
                    ),
            reminders =
                remember { SettledSearchResultsBuffer(emptyList<ReminderInfo>()) }
                    .displayedValue(
                        query,
                        currentState.reminderResults,
                        SearchSection.REMINDERS in secondarySectionsRefreshing,
                    ),
            notes =
                remember { SettledSearchResultsBuffer(emptyList<NoteInfo>()) }
                    .displayedValue(
                        query,
                        currentState.noteResults,
                        SearchSection.NOTES in secondarySectionsRefreshing,
                    ),
        )

    if (query.isBlank()) return currentState

    return currentState.copy(
        hasAppResults = displayedSnapshot.apps.isNotEmpty(),
        hasAppShortcutResults = displayedSnapshot.appShortcuts.isNotEmpty(),
        hasContactResults = displayedSnapshot.contacts.isNotEmpty(),
        hasFileResults = displayedSnapshot.files.isNotEmpty(),
        hasSettingResults = displayedSnapshot.settings.isNotEmpty(),
        hasAppSettingResults = displayedSnapshot.appSettings.isNotEmpty(),
        hasCalendarResults = displayedSnapshot.calendarEvents.isNotEmpty(),
        hasReminderResults = displayedSnapshot.reminders.isNotEmpty(),
        hasNoteResults = displayedSnapshot.notes.isNotEmpty(),
        displayApps = displayedSnapshot.apps,
        appShortcutResults = displayedSnapshot.appShortcuts,
        contactResults = displayedSnapshot.contacts,
        fileResults = displayedSnapshot.files,
        settingResults = displayedSnapshot.settings,
        appSettingResults = displayedSnapshot.appSettings,
        calendarEvents = displayedSnapshot.calendarEvents,
        reminderResults = displayedSnapshot.reminders,
        noteResults = displayedSnapshot.notes,
    )
}
