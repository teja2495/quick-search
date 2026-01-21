package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.core.*

/**
 * Renders search result sections.
 */
@Composable
fun SearchResultsSections(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean,
    oneHandedMode: Boolean
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS
    val isAppShortcutsExpanded = renderingState.expandedSection == ExpandedSection.APP_SHORTCUTS

    // Use new visibility states to determine what should render
    val shouldRenderApps = when (state.appsSectionState) {
        is AppsSectionVisibility.ShowingResults -> !isFilesExpanded && !isContactsExpanded && !isSettingsExpanded && !isAppShortcutsExpanded
        else -> false
    }
    val shouldRenderAppShortcuts = when (state.appShortcutsSectionState) {
        is AppShortcutsSectionVisibility.ShowingResults ->
            !isFilesExpanded && !isContactsExpanded && !isSettingsExpanded
        else -> false
    }
    val shouldRenderContacts = when (state.contactsSectionState) {
        is ContactsSectionVisibility.ShowingResults ->
            !isFilesExpanded && !isSettingsExpanded && !isAppShortcutsExpanded
        else -> false
    }
    val shouldRenderFiles = when (state.filesSectionState) {
        is FilesSectionVisibility.ShowingResults ->
            !isContactsExpanded && !isSettingsExpanded && !isAppShortcutsExpanded
        else -> false
    }
    val shouldRenderSettings = when (state.settingsSectionState) {
        is SettingsSectionVisibility.ShowingResults ->
            !isFilesExpanded && !isContactsExpanded && !isAppShortcutsExpanded
        else -> false
    }

    val context = SectionRenderContext(
        shouldRenderFiles = shouldRenderFiles,
        shouldRenderContacts = shouldRenderContacts,
        shouldRenderApps = shouldRenderApps,
        shouldRenderAppShortcuts = shouldRenderAppShortcuts,
        shouldRenderSettings = shouldRenderSettings,
        isFilesExpanded = isFilesExpanded,
        isContactsExpanded = isContactsExpanded,
        isSettingsExpanded = isSettingsExpanded,
        isAppShortcutsExpanded = isAppShortcutsExpanded,
        filesList = getFileListForRendering(renderingState, isFilesExpanded, oneHandedMode),
        contactsList = getContactListForRendering(renderingState, isContactsExpanded, oneHandedMode),
        settingsList = getSettingsListForRendering(renderingState, isSettingsExpanded, oneHandedMode),
        appShortcutsList = getAppShortcutListForRendering(
            renderingState,
            isAppShortcutsExpanded,
            oneHandedMode
        ),
        showAllFilesResults = false,
        showAllContactsResults = false,
        showAllSettingsResults = false,
        showAllAppShortcutsResults = false,
        showFilesExpandControls = renderingState.hasMultipleExpandableSections,
        showContactsExpandControls = renderingState.hasMultipleExpandableSections,
        showSettingsExpandControls = renderingState.hasMultipleExpandableSections || renderingState.hasSettingResults,
        showAppShortcutsExpandControls = renderingState.hasMultipleExpandableSections,
        filesExpandClick = filesParams.onExpandClick,
        contactsExpandClick = contactsParams.onExpandClick,
        settingsExpandClick = settingsParams.onExpandClick,
        appShortcutsExpandClick = appShortcutsParams.onExpandClick
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        appShortcutsParams = appShortcutsParams,
        settingsParams = settingsParams,
        appsParams = appsParams,
        isReversed = isReversed
    )

    getOrderedSections(renderingState, isReversed).forEach { section ->
        renderSection(section, params, context)
    }
}

/**
 * Renders pinned items sections.
 */
@Composable
fun PinnedItemsSections(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean
) {
    val shouldShowPinned = !renderingState.isSearching

    // Use new visibility states for pinned items rendering
    val shouldRenderFiles = shouldShowPinned && when (state.filesSectionState) {
        is FilesSectionVisibility.ShowingResults -> renderingState.hasPinnedFiles
        else -> false
    }
    val shouldRenderContacts = shouldShowPinned && when (state.contactsSectionState) {
        is ContactsSectionVisibility.ShowingResults -> renderingState.hasPinnedContacts
        else -> false
    }
    val shouldRenderApps = shouldShowPinned && when (state.appsSectionState) {
        is AppsSectionVisibility.ShowingResults -> true
        else -> false
    }
    val shouldRenderAppShortcuts = shouldShowPinned && when (state.appShortcutsSectionState) {
        is AppShortcutsSectionVisibility.ShowingResults -> renderingState.hasPinnedAppShortcuts
        else -> false
    }
    val shouldRenderSettings = shouldShowPinned && when (state.settingsSectionState) {
        is SettingsSectionVisibility.ShowingResults -> renderingState.hasPinnedSettings
        else -> false
    }

    val context = SectionRenderContext(
        shouldRenderFiles = shouldRenderFiles,
        shouldRenderContacts = shouldRenderContacts,
        shouldRenderApps = shouldRenderApps,
        shouldRenderAppShortcuts = shouldRenderAppShortcuts,
        shouldRenderSettings = shouldRenderSettings,
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        isAppShortcutsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        appShortcutsList = renderingState.pinnedAppShortcuts,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showAllAppShortcutsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false,
        showAppShortcutsExpandControls = false,
        filesExpandClick = {},
        contactsExpandClick = {},
        settingsExpandClick = {},
        appShortcutsExpandClick = {}
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        appShortcutsParams = appShortcutsParams,
        settingsParams = settingsParams,
        appsParams = appsParams,
        isReversed = isReversed
    )

    getOrderedSections(renderingState, isReversed).forEach { section ->
        renderSection(section, params, context)
    }
}

/**
 * Renders expanded pinned sections when query is blank.
 */
@Composable
fun ExpandedPinnedSections(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS
    val isAppShortcutsExpanded = renderingState.expandedSection == ExpandedSection.APP_SHORTCUTS

    // Use new visibility states for expanded pinned sections
    val shouldRenderFiles = isFilesExpanded && when (state.filesSectionState) {
        is FilesSectionVisibility.ShowingResults -> renderingState.hasPinnedFiles
        else -> false
    }
    val shouldRenderContacts = isContactsExpanded && when (state.contactsSectionState) {
        is ContactsSectionVisibility.ShowingResults -> renderingState.hasPinnedContacts
        else -> false
    }
    val shouldRenderAppShortcuts = isAppShortcutsExpanded && when (state.appShortcutsSectionState) {
        is AppShortcutsSectionVisibility.ShowingResults -> renderingState.hasPinnedAppShortcuts
        else -> false
    }
    val shouldRenderSettings = isSettingsExpanded && when (state.settingsSectionState) {
        is SettingsSectionVisibility.ShowingResults -> renderingState.hasPinnedSettings
        else -> false
    }

    val context = SectionRenderContext(
        shouldRenderFiles = shouldRenderFiles,
        shouldRenderContacts = shouldRenderContacts,
        shouldRenderSettings = shouldRenderSettings,
        shouldRenderAppShortcuts = shouldRenderAppShortcuts,
        shouldRenderApps = false, // Apps section doesn't need expansion handling
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        isAppShortcutsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        appShortcutsList = renderingState.pinnedAppShortcuts,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showAllAppShortcutsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        settingsParams = settingsParams,
        appShortcutsParams = appShortcutsParams,
        appsParams = null, // Apps section doesn't need expansion handling
        isReversed = false
    )

    renderingState.orderedSections.forEach { section ->
        renderSection(section, params, context)
    }
}
