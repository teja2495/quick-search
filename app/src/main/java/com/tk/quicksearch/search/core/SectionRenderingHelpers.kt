package com.tk.quicksearch.search.core

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.NoteInfo

// ============================================================================
// Section Visibility Helpers
// ============================================================================

/** Determines if files section should be shown. */
fun shouldShowFilesSection(
    renderingState: SectionRenderingState,
    filesParams: FilesSectionParams,
): Boolean =
    shouldShowSectionWithPermission(
        shouldShow = renderingState.shouldShowFiles,
        hasPermission = filesParams.hasPermission,
        hasResults = renderingState.hasFileResults,
    )

/** Determines if contacts section should be shown. */
fun shouldShowContactsSection(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
): Boolean =
    shouldShowSectionWithPermission(
        shouldShow = renderingState.shouldShowContacts,
        hasPermission = contactsParams.hasPermission,
        hasResults = renderingState.hasContactResults,
    )

fun shouldShowSettingsSection(renderingState: SectionRenderingState): Boolean =
    (renderingState.hasSettingResults || renderingState.hasAppSettingResults) &&
        renderingState.shouldShowSettings &&
        (
            renderingState.expandedSection == ExpandedSection.NONE ||
                renderingState.expandedSection == ExpandedSection.SETTINGS ||
                renderingState.expandedSection == ExpandedSection.APP_SETTINGS
        )

/** Determines if apps section should be shown. */
fun shouldShowAppsSection(renderingState: SectionRenderingState): Boolean =
    renderingState.hasAppResults &&
        renderingState.shouldShowApps &&
        renderingState.expandedSection == ExpandedSection.NONE &&
        !renderingState.shortcutDetected

fun shouldShowAppShortcutsSection(renderingState: SectionRenderingState): Boolean =
    renderingState.hasAppShortcutResults &&
        renderingState.shouldShowAppShortcuts &&
        renderingState.expandedSection == ExpandedSection.NONE

fun shouldShowCalendarSection(
    renderingState: SectionRenderingState,
    calendarParams: CalendarSectionParams,
): Boolean =
    shouldShowSectionWithPermission(
        shouldShow = renderingState.shouldShowCalendar,
        hasPermission = calendarParams.hasPermission,
        hasResults = renderingState.hasCalendarResults,
    )

fun shouldShowNotesSection(renderingState: SectionRenderingState): Boolean =
    renderingState.hasNoteResults &&
        renderingState.shouldShowNotes &&
        (
            renderingState.expandedSection == ExpandedSection.NONE ||
                renderingState.expandedSection == ExpandedSection.NOTES
        )

/**
 * Generic helper for determining section visibility based on permission and results. Shows section
 * if:
 * - It should be shown AND
 * - (No permission required OR has results)
 */
private fun shouldShowSectionWithPermission(
    shouldShow: Boolean,
    hasPermission: Boolean,
    hasResults: Boolean,
): Boolean = shouldShow && (!hasPermission || hasResults)

// ============================================================================
// List Processing Helpers
// ============================================================================

/**
 * Gets the appropriate contact list based on expansion state. Returns reversed list when expanded
 * AND one-handed mode is enabled, original order otherwise.
 */
fun getContactListForRendering(
    renderingState: SectionRenderingState,
    isContactsExpanded: Boolean,
    oneHandedMode: Boolean,
): List<ContactInfo> =
    getListForRendering(
        list = renderingState.contactResults,
        isExpanded = isContactsExpanded,
        autoExpand = false,
        oneHandedMode = oneHandedMode,
    )

/**
 * Gets the appropriate file list based on expansion state. Returns reversed list when expanded AND
 * one-handed mode is enabled, original order otherwise.
 */
fun getFileListForRendering(
    renderingState: SectionRenderingState,
    isFilesExpanded: Boolean,
    oneHandedMode: Boolean,
): List<DeviceFile> =
    getListForRendering(
        list = renderingState.fileResults,
        isExpanded = isFilesExpanded,
        autoExpand = false,
        oneHandedMode = oneHandedMode,
    )

fun getSettingsListForRendering(
    renderingState: SectionRenderingState,
    isSettingsExpanded: Boolean,
    oneHandedMode: Boolean,
): List<DeviceSetting> =
    getListForRendering(
        list = renderingState.settingResults,
        isExpanded = isSettingsExpanded,
        autoExpand = false,
        oneHandedMode = oneHandedMode,
    )

fun getAppSettingsListForRendering(
    renderingState: SectionRenderingState,
    isSettingsExpanded: Boolean,
    oneHandedMode: Boolean,
): List<AppSettingResult> =
    getListForRendering(
        list = renderingState.appSettingResults,
        isExpanded = isSettingsExpanded,
        autoExpand = false,
        oneHandedMode = oneHandedMode,
    )

fun getAppShortcutListForRendering(
    renderingState: SectionRenderingState,
    isAppShortcutsExpanded: Boolean,
    oneHandedMode: Boolean,
): List<StaticShortcut> =
    getListForRendering(
        list = renderingState.appShortcutResults,
        isExpanded = isAppShortcutsExpanded,
        autoExpand = false,
        oneHandedMode = oneHandedMode,
    )

/**
 * Generic helper for getting list in correct order based on expansion state. Returns reversed view
 * when expanded AND one-handed mode is enabled, original list otherwise.
 */
private fun <T> getListForRendering(
    list: List<T>,
    isExpanded: Boolean,
    autoExpand: Boolean,
    oneHandedMode: Boolean,
): List<T> {
    // Keep natural order when user expands a section; only reverse for auto-expanded
    return if (!isExpanded && autoExpand && oneHandedMode) {
        list.reversed() // Returns a reversed view, more efficient than asReversed()
    } else {
        list
    }
}

// ============================================================================
// Section Rendering Data Classes
// ============================================================================

@Composable
fun rememberSectionRenderContext(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    filesParams: FilesSectionParams,
    contactsParams: ContactsSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    appsParams: AppsSectionParams?,
    isSearching: Boolean,
    oneHandedMode: Boolean,
): SectionRenderContext {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS
    val isAppSettingsExpanded = renderingState.expandedSection == ExpandedSection.APP_SETTINGS
    val isCalendarExpanded = renderingState.expandedSection == ExpandedSection.CALENDAR
    val isAppShortcutsExpanded = renderingState.expandedSection == ExpandedSection.APP_SHORTCUTS
    val isNotesExpanded = renderingState.expandedSection == ExpandedSection.NOTES

    // Determine visibility based on state (Search vs Pinned)
    val shouldRenderApps: Boolean
    val shouldRenderFiles: Boolean
    val shouldRenderContacts: Boolean
    val shouldRenderAppShortcuts: Boolean
    val shouldRenderSettings: Boolean
    val shouldRenderCalendar: Boolean
    val shouldRenderNotes: Boolean

    if (isSearching) {
        shouldRenderApps =
            when (state.appsSectionState) {
                is AppsSectionVisibility.ShowingResults -> {
                    !isFilesExpanded &&
                        !isContactsExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isCalendarExpanded &&
                        !isNotesExpanded
                }

                else -> {
                    false
                }
            }
        shouldRenderFiles =
            when (state.filesSectionState) {
                is FilesSectionVisibility.ShowingResults -> {
                    !isContactsExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isCalendarExpanded &&
                        !isNotesExpanded
                }

                else -> {
                    false
                }
            }
        shouldRenderContacts =
            when (state.contactsSectionState) {
                is ContactsSectionVisibility.ShowingResults -> {
                    !isFilesExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isCalendarExpanded &&
                        !isNotesExpanded
                }

                else -> {
                    false
                }
            }
        shouldRenderAppShortcuts =
            when (state.appShortcutsSectionState) {
                is AppShortcutsSectionVisibility.ShowingResults -> {
                        !isFilesExpanded &&
                        !isContactsExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isCalendarExpanded &&
                        !isNotesExpanded
                }

                else -> {
                    false
                }
            }
        shouldRenderSettings =
            when (state.settingsSectionState) {
                is SettingsSectionVisibility.ShowingResults -> {
                        !isFilesExpanded &&
                        !isContactsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isCalendarExpanded &&
                        !isNotesExpanded
                }

                else -> {
                    false
                }
            }
        shouldRenderCalendar =
            when (state.calendarSectionState) {
                is CalendarSectionVisibility.ShowingResults -> {
                    !isFilesExpanded &&
                        !isContactsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isNotesExpanded
                }

                else -> false
            }
        shouldRenderNotes =
            when (state.notesSectionState) {
                is NotesSectionVisibility.ShowingResults -> {
                    !isFilesExpanded &&
                        !isContactsExpanded &&
                        !isAppShortcutsExpanded &&
                        !isSettingsExpanded &&
                        !isAppSettingsExpanded &&
                        !isCalendarExpanded
                }

                else -> false
            }
    } else {
        // Pinned state
        shouldRenderApps =
            when (state.appsSectionState) {
                is AppsSectionVisibility.ShowingResults -> true
                else -> false
            }
        shouldRenderFiles =
            when (state.filesSectionState) {
                is FilesSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedFiles
                }

                else -> {
                    false
                }
            }
        shouldRenderContacts =
            when (state.contactsSectionState) {
                is ContactsSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedContacts
                }

                else -> {
                    false
                }
            }
        shouldRenderAppShortcuts =
            when (state.appShortcutsSectionState) {
                is AppShortcutsSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedAppShortcuts
                }

                else -> {
                    false
                }
            }
        shouldRenderSettings =
            when (state.settingsSectionState) {
                is SettingsSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedSettings
                }

                else -> {
                    false
                }
            }
        shouldRenderCalendar =
            when (state.calendarSectionState) {
                is CalendarSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedCalendarEvents ||
                        state.detectedAliasSearchSection == SearchSection.CALENDAR
                }

                else -> false
            }
        shouldRenderNotes =
            when (state.notesSectionState) {
                is NotesSectionVisibility.ShowingResults -> {
                    renderingState.hasPinnedNotes ||
                        state.detectedAliasSearchSection == SearchSection.NOTES
                }

                else -> false
            }
    }

    return SectionRenderContext(
        shouldRenderFiles = shouldRenderFiles,
        shouldRenderContacts = shouldRenderContacts,
        shouldRenderApps = shouldRenderApps,
        shouldRenderAppShortcuts = shouldRenderAppShortcuts,
        shouldRenderSettings = shouldRenderSettings,
        shouldRenderCalendar = shouldRenderCalendar,
        shouldRenderNotes = shouldRenderNotes,
        isFilesExpanded = isFilesExpanded || !isSearching,
        isContactsExpanded = isContactsExpanded || !isSearching,
        isDeviceSettingsExpanded = isSettingsExpanded || !isSearching,
        isAppSettingsExpanded = isAppSettingsExpanded || !isSearching,
        isCalendarExpanded = isCalendarExpanded || !isSearching,
        isAppShortcutsExpanded = isAppShortcutsExpanded || !isSearching,
        isNotesExpanded = isNotesExpanded || !isSearching,
        filesList =
            if (isSearching) {
                getFileListForRendering(
                    renderingState,
                    isFilesExpanded,
                    oneHandedMode,
                )
            } else {
                renderingState.pinnedFiles
            },
        contactsList =
            if (isSearching) {
                getContactListForRendering(
                    renderingState,
                    isContactsExpanded,
                    oneHandedMode,
                )
            } else {
                renderingState.pinnedContacts
            },
        settingsList =
            if (isSearching) {
                getSettingsListForRendering(
                    renderingState,
                    isSettingsExpanded,
                    oneHandedMode,
                )
            } else {
                renderingState.pinnedSettings
            },
        appSettingsList =
            if (isSearching) {
                getAppSettingsListForRendering(
                    renderingState,
                    isAppSettingsExpanded,
                    oneHandedMode,
                )
            } else {
                emptyList()
            },
        appShortcutsList =
            if (isSearching) {
                getAppShortcutListForRendering(
                    renderingState,
                    isAppShortcutsExpanded,
                    oneHandedMode,
                )
            } else {
                renderingState.pinnedAppShortcuts
            },
        calendarEventsList =
            if (isSearching || state.detectedAliasSearchSection == SearchSection.CALENDAR) {
                renderingState.calendarEvents
            } else {
                renderingState.pinnedCalendarEvents
            },
        notesList =
            if (isSearching || state.detectedAliasSearchSection == SearchSection.NOTES) {
                renderingState.noteResults
            } else {
                renderingState.pinnedNotes
            },
        showAllFilesResults = !isSearching,
        showAllContactsResults = !isSearching,
        showAllSettingsResults = !isSearching,
        showAllCalendarResults = !isSearching,
        showAllNotesResults = !isSearching,
        showAllAppShortcutsResults = !isSearching,
        showFilesExpandControls = isSearching,
        showContactsExpandControls = isSearching,
        showDeviceSettingsExpandControls = isSearching && renderingState.hasSettingResults,
        showAppSettingsExpandControls = isSearching && renderingState.hasAppSettingResults,
        showCalendarExpandControls = isSearching,
        showNotesExpandControls = isSearching,
        showAppShortcutsExpandControls = isSearching,
        filesExpandClick = filesParams.onExpandClick,
        contactsExpandClick = contactsParams.onExpandClick,
        deviceSettingsExpandClick = settingsParams.onExpandClick,
        appSettingsExpandClick = settingsParams.onAppSettingExpandClick,
        calendarExpandClick = calendarParams.onExpandClick,
        notesExpandClick = notesParams.onExpandClick,
        appShortcutsExpandClick = appShortcutsParams.onExpandClick,
        isSectionAliasMode = state.detectedAliasSearchSection != null,
    )
}

/** Parameters for rendering a single section. */
data class SectionRenderParams(
    val renderingState: SectionRenderingState,
    val contactsParams: ContactsSectionParams,
    val filesParams: FilesSectionParams,
    val settingsParams: SettingsSectionParams? = null,
    val calendarParams: CalendarSectionParams? = null,
    val notesParams: NotesSectionParams? = null,
    val appShortcutsParams: AppShortcutsSectionParams? = null,
    val appsParams: AppsSectionParams? = null,
    val isReversed: Boolean,
)

/** Context for rendering sections, containing all the state needed for rendering decisions. */
data class SectionRenderContext(
    val shouldRenderFiles: Boolean = false,
    val shouldRenderContacts: Boolean = false,
    val shouldRenderApps: Boolean = false,
    val shouldRenderAppShortcuts: Boolean = false,
    val shouldRenderCalendar: Boolean = false,
    val shouldRenderNotes: Boolean = false,
    val isFilesExpanded: Boolean = false,
    val isContactsExpanded: Boolean = false,
    val isCalendarExpanded: Boolean = false,
    val isAppShortcutsExpanded: Boolean = false,
    val isNotesExpanded: Boolean = false,
    val filesList: List<DeviceFile> = emptyList(),
    val contactsList: List<ContactInfo> = emptyList(),
    val appShortcutsList: List<StaticShortcut> = emptyList(),
    val calendarEventsList: List<CalendarEventInfo> = emptyList(),
    val notesList: List<NoteInfo> = emptyList(),
    val showAllFilesResults: Boolean = false,
    val showAllContactsResults: Boolean = false,
    val showAllAppShortcutsResults: Boolean = false,
    val showAllCalendarResults: Boolean = false,
    val showAllNotesResults: Boolean = false,
    val showFilesExpandControls: Boolean = false,
    val showContactsExpandControls: Boolean = false,
    val showAppShortcutsExpandControls: Boolean = false,
    val showCalendarExpandControls: Boolean = false,
    val showNotesExpandControls: Boolean = false,
    val filesExpandClick: () -> Unit = {},
    val contactsExpandClick: () -> Unit = {},
    val appShortcutsExpandClick: () -> Unit = {},
    val calendarExpandClick: () -> Unit = {},
    val notesExpandClick: () -> Unit = {},
    val shouldRenderSettings: Boolean = false,
    val isDeviceSettingsExpanded: Boolean = false,
    val isAppSettingsExpanded: Boolean = false,
    val settingsList: List<DeviceSetting> = emptyList(),
    val appSettingsList: List<AppSettingResult> = emptyList(),
    val showAllSettingsResults: Boolean = false,
    val showDeviceSettingsExpandControls: Boolean = false,
    val showAppSettingsExpandControls: Boolean = false,
    val deviceSettingsExpandClick: () -> Unit = {},
    val appSettingsExpandClick: () -> Unit = {},
    val isSectionAliasMode: Boolean = false,
)
