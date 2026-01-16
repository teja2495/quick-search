package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams


// ============================================================================
// Section Visibility Helpers
// ============================================================================

/**
 * Determines if files section should be shown.
 */
fun shouldShowFilesSection(
    renderingState: SectionRenderingState,
    filesParams: FilesSectionParams
): Boolean = shouldShowSectionWithPermission(
    shouldShow = renderingState.shouldShowFiles,
    hasPermission = filesParams.hasPermission,
    hasResults = renderingState.hasFileResults
)

/**
 * Determines if contacts section should be shown.
 */
fun shouldShowContactsSection(
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams
): Boolean = shouldShowSectionWithPermission(
    shouldShow = renderingState.shouldShowContacts,
    hasPermission = contactsParams.hasPermission,
    hasResults = renderingState.hasContactResults
)

fun shouldShowSettingsSection(renderingState: SectionRenderingState): Boolean {
    return renderingState.hasSettingResults &&
        renderingState.shouldShowSettings &&
        (renderingState.expandedSection == ExpandedSection.NONE || renderingState.expandedSection == ExpandedSection.SETTINGS)
}

/**
 * Determines if apps section should be shown.
 */
fun shouldShowAppsSection(renderingState: SectionRenderingState): Boolean {
    return renderingState.hasAppResults &&
        renderingState.shouldShowApps &&
        renderingState.expandedSection == ExpandedSection.NONE &&
        !renderingState.shortcutDetected
}

fun shouldShowAppShortcutsSection(renderingState: SectionRenderingState): Boolean {
    return renderingState.hasAppShortcutResults &&
        renderingState.shouldShowAppShortcuts &&
        renderingState.expandedSection == ExpandedSection.NONE
}

/**
 * Generic helper for determining section visibility based on permission and results.
 * Shows section if:
 * - It should be shown AND
 * - (No permission required OR has results)
 */
private fun shouldShowSectionWithPermission(
    shouldShow: Boolean,
    hasPermission: Boolean,
    hasResults: Boolean
): Boolean {
    return shouldShow && (!hasPermission || hasResults)
}

// ============================================================================
// List Processing Helpers
// ============================================================================

/**
 * Gets the appropriate contact list based on expansion state.
 * Returns reversed list when expanded AND keyboard-aligned layout is enabled, original order otherwise.
 */
fun getContactListForRendering(
    renderingState: SectionRenderingState,
    isContactsExpanded: Boolean,
    keyboardAlignedLayout: Boolean
): List<ContactInfo> = getListForRendering(
    list = renderingState.contactResults,
    isExpanded = isContactsExpanded,
    autoExpand = renderingState.autoExpandContacts,
    keyboardAlignedLayout = keyboardAlignedLayout
)

/**
 * Gets the appropriate file list based on expansion state.
 * Returns reversed list when expanded AND keyboard-aligned layout is enabled, original order otherwise.
 */
fun getFileListForRendering(
    renderingState: SectionRenderingState,
    isFilesExpanded: Boolean,
    keyboardAlignedLayout: Boolean
): List<DeviceFile> = getListForRendering(
    list = renderingState.fileResults,
    isExpanded = isFilesExpanded,
    autoExpand = renderingState.autoExpandFiles,
    keyboardAlignedLayout = keyboardAlignedLayout
)

fun getSettingsListForRendering(
    renderingState: SectionRenderingState,
    isSettingsExpanded: Boolean,
    keyboardAlignedLayout: Boolean
): List<DeviceSetting> = getListForRendering(
    list = renderingState.settingResults,
    isExpanded = isSettingsExpanded,
    autoExpand = renderingState.autoExpandSettings,
    keyboardAlignedLayout = keyboardAlignedLayout
)

fun getAppShortcutListForRendering(
    renderingState: SectionRenderingState,
    isAppShortcutsExpanded: Boolean,
    keyboardAlignedLayout: Boolean
): List<StaticShortcut> = getListForRendering(
    list = renderingState.appShortcutResults,
    isExpanded = isAppShortcutsExpanded,
    autoExpand = renderingState.autoExpandAppShortcuts,
    keyboardAlignedLayout = keyboardAlignedLayout
)

/**
 * Generic helper for getting list in correct order based on expansion state.
 * Returns reversed view when expanded AND keyboard-aligned layout is enabled, original list otherwise.
 */
private fun <T> getListForRendering(
    list: List<T>,
    isExpanded: Boolean,
    autoExpand: Boolean,
    keyboardAlignedLayout: Boolean
): List<T> {
    // Keep natural order when user expands a section; only reverse for auto-expanded
    return if (!isExpanded && autoExpand && keyboardAlignedLayout) {
        list.reversed() // Returns a reversed view, more efficient than asReversed()
    } else {
        list
    }
}

/**
 * Gets the ordered sections list, optionally reversed.
 * Returns reversed view when requested, original list otherwise.
 */
fun getOrderedSections(
    renderingState: SectionRenderingState,
    isReversed: Boolean
): List<SearchSection> {
    val sections: List<SearchSection> = renderingState.orderedSections
    return if (isReversed) {
        sections.reversed() // Returns a reversed view
    } else {
        sections
    }
}

// ============================================================================
// Section Rendering Data Classes
// ============================================================================

/**
 * Parameters for rendering a single section.
 */
data class SectionRenderParams(
    val renderingState: SectionRenderingState,
    val contactsParams: ContactsSectionParams,
    val filesParams: FilesSectionParams,
    val settingsParams: SettingsSectionParams? = null,
    val appShortcutsParams: AppShortcutsSectionParams? = null,
    val appsParams: AppsSectionParams? = null,
    val isReversed: Boolean
)

/**
 * Context for rendering sections, containing all the state needed for rendering decisions.
 */
data class SectionRenderContext(
    val shouldRenderFiles: Boolean = false,
    val shouldRenderContacts: Boolean = false,
    val shouldRenderApps: Boolean = false,
    val shouldRenderAppShortcuts: Boolean = false,
    val isFilesExpanded: Boolean = false,
    val isContactsExpanded: Boolean = false,
    val isAppShortcutsExpanded: Boolean = false,
    val filesList: List<DeviceFile> = emptyList(),
    val contactsList: List<ContactInfo> = emptyList(),
    val appShortcutsList: List<StaticShortcut> = emptyList(),
    val showAllFilesResults: Boolean = false,
    val showAllContactsResults: Boolean = false,
    val showAllAppShortcutsResults: Boolean = false,
    val showFilesExpandControls: Boolean = false,
    val showContactsExpandControls: Boolean = false,
    val showAppShortcutsExpandControls: Boolean = false,
    val filesExpandClick: () -> Unit = {},
    val contactsExpandClick: () -> Unit = {},
    val appShortcutsExpandClick: () -> Unit = {},
    val shouldRenderSettings: Boolean = false,
    val isSettingsExpanded: Boolean = false,
    val settingsList: List<DeviceSetting> = emptyList(),
    val showAllSettingsResults: Boolean = false,
    val showSettingsExpandControls: Boolean = false,
    val settingsExpandClick: () -> Unit = {}
)
