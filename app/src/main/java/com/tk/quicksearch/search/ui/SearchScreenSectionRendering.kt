package com.tk.quicksearch.search.ui

import androidx.compose.runtime.Composable
import com.tk.quicksearch.search.core.*

/**
 * Renders search result sections.
 */
@Composable
fun SearchResultsSections(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean,
    keyboardAlignedLayout: Boolean
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS

    val context = SectionRenderContext(
        shouldRenderFiles = shouldShowFilesSection(renderingState, filesParams) && !isContactsExpanded && !isSettingsExpanded,
        shouldRenderContacts = shouldShowContactsSection(renderingState, contactsParams) && !isFilesExpanded && !isSettingsExpanded,
        shouldRenderApps = shouldShowAppsSection(renderingState) && !isSettingsExpanded,
        shouldRenderSettings = shouldShowSettingsSection(renderingState) && !isFilesExpanded && !isContactsExpanded,
        isFilesExpanded = isFilesExpanded,
        isContactsExpanded = isContactsExpanded,
        isSettingsExpanded = isSettingsExpanded,
        filesList = getFileListForRendering(renderingState, isFilesExpanded, keyboardAlignedLayout),
        contactsList = getContactListForRendering(renderingState, isContactsExpanded, keyboardAlignedLayout),
        settingsList = getSettingsListForRendering(renderingState, isSettingsExpanded, keyboardAlignedLayout),
        showAllFilesResults = renderingState.autoExpandFiles,
        showAllContactsResults = renderingState.autoExpandContacts,
        showAllSettingsResults = renderingState.autoExpandSettings,
        showFilesExpandControls = renderingState.hasMultipleExpandableSections,
        showContactsExpandControls = renderingState.hasMultipleExpandableSections,
        showSettingsExpandControls = renderingState.hasMultipleExpandableSections || renderingState.hasSettingResults,
        filesExpandClick = filesParams.onExpandClick,
        contactsExpandClick = contactsParams.onExpandClick,
        settingsExpandClick = settingsParams.onExpandClick
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
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
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appsParams: AppsSectionParams,
    settingsParams: SettingsSectionParams,
    isReversed: Boolean
) {
    val shouldShowPinned = !renderingState.isSearching

    val context = SectionRenderContext(
        shouldRenderFiles = shouldShowPinned && renderingState.hasPinnedFiles && renderingState.shouldShowFiles,
        shouldRenderContacts = shouldShowPinned && renderingState.hasPinnedContacts && renderingState.shouldShowContacts,
        shouldRenderApps = shouldShowPinned && shouldShowAppsSection(renderingState),
        shouldRenderSettings = shouldShowPinned && renderingState.hasPinnedSettings && renderingState.shouldShowSettings,
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false,
        filesExpandClick = {},
        contactsExpandClick = {},
        settingsExpandClick = {}
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
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
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams
) {
    val isContactsExpanded = renderingState.expandedSection == ExpandedSection.CONTACTS
    val isFilesExpanded = renderingState.expandedSection == ExpandedSection.FILES
    val isSettingsExpanded = renderingState.expandedSection == ExpandedSection.SETTINGS

    val context = SectionRenderContext(
        shouldRenderFiles = isFilesExpanded && renderingState.hasPinnedFiles && renderingState.shouldShowFiles,
        shouldRenderContacts = isContactsExpanded && renderingState.hasPinnedContacts && renderingState.shouldShowContacts,
        shouldRenderSettings = isSettingsExpanded && renderingState.hasPinnedSettings && renderingState.shouldShowSettings,
        shouldRenderApps = false, // Apps section doesn't need expansion handling
        isFilesExpanded = true,
        isContactsExpanded = true,
        isSettingsExpanded = true,
        filesList = renderingState.pinnedFiles,
        contactsList = renderingState.pinnedContacts,
        settingsList = renderingState.pinnedSettings,
        showAllFilesResults = true,
        showAllContactsResults = true,
        showAllSettingsResults = true,
        showFilesExpandControls = false,
        showContactsExpandControls = false,
        showSettingsExpandControls = false
    )

    val params = SectionRenderParams(
        renderingState = renderingState,
        contactsParams = contactsParams,
        filesParams = filesParams,
        settingsParams = settingsParams,
        appsParams = null, // Apps section doesn't need expansion handling
        isReversed = false
    )

    renderingState.orderedSections.forEach { section ->
        renderSection(section, params, context)
    }
}
