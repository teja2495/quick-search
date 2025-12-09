package com.tk.quicksearch.search

import androidx.compose.runtime.Composable
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut

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
): List<SettingShortcut> = getListForRendering(
    list = renderingState.settingResults,
    isExpanded = isSettingsExpanded,
    autoExpand = renderingState.autoExpandSettings,
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
    return if (isReversed) {
        renderingState.orderedSections.reversed() // Returns a reversed view
    } else {
        renderingState.orderedSections
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
    val isFilesExpanded: Boolean = false,
    val isContactsExpanded: Boolean = false,
    val filesList: List<DeviceFile> = emptyList(),
    val contactsList: List<ContactInfo> = emptyList(),
    val showAllFilesResults: Boolean = false,
    val showAllContactsResults: Boolean = false,
    val showFilesExpandControls: Boolean = false,
    val showContactsExpandControls: Boolean = false,
    val filesExpandClick: () -> Unit = {},
    val contactsExpandClick: () -> Unit = {},
    val shouldRenderSettings: Boolean = false,
    val isSettingsExpanded: Boolean = false,
    val settingsList: List<SettingShortcut> = emptyList(),
    val showAllSettingsResults: Boolean = false,
    val showSettingsExpandControls: Boolean = false,
    val settingsExpandClick: () -> Unit = {}
)

// ============================================================================
// Section Rendering Functions
// ============================================================================

/**
 * Renders a single section based on its type and current state.
 */
@Composable
fun renderSection(
    section: SearchSection,
    params: SectionRenderParams,
    sectionContext: SectionRenderContext
) {
    when (section) {
        SearchSection.FILES -> renderFilesSection(params, sectionContext)
        SearchSection.CONTACTS -> renderContactsSection(params, sectionContext)
        SearchSection.APPS -> renderAppsSection(params, sectionContext)
        SearchSection.SETTINGS -> renderSettingsSection(params, sectionContext)
    }
}

/**
 * Renders the files section if it should be displayed.
 */
@Composable
private fun renderFilesSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderFiles) {
        RenderFilesSection(
            params = params.filesParams.copy(
                files = context.filesList,
                isExpanded = context.isFilesExpanded,
                showAllResults = context.showAllFilesResults,
                showExpandControls = context.showFilesExpandControls,
                onExpandClick = context.filesExpandClick
            )
        )
    }
}

/**
 * Renders the contacts section if it should be displayed.
 */
@Composable
private fun renderContactsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderContacts) {
        RenderContactsSection(
            params = params.contactsParams.copy(
                contacts = context.contactsList,
                isExpanded = context.isContactsExpanded,
                showAllResults = context.showAllContactsResults,
                showExpandControls = context.showContactsExpandControls,
                onExpandClick = context.contactsExpandClick
            )
        )
    }
}

/**
 * Renders the apps section if it should be displayed.
 */
@Composable
private fun renderAppsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderApps && params.appsParams != null) {
        RenderAppsSection(params = params.appsParams)
    }
}

/**
 * Renders the settings section if it should be displayed.
 */
@Composable
private fun renderSettingsSection(
    params: SectionRenderParams,
    context: SectionRenderContext
) {
    if (context.shouldRenderSettings && params.settingsParams != null) {
        SettingsResultsSection(
            settings = context.settingsList,
            isExpanded = context.isSettingsExpanded,
            pinnedSettingIds = params.settingsParams.pinnedSettingIds,
            onSettingClick = params.settingsParams.onSettingClick,
            onTogglePin = params.settingsParams.onTogglePin,
            onExclude = params.settingsParams.onExclude,
            onNicknameClick = params.settingsParams.onNicknameClick,
            getSettingNickname = params.settingsParams.getSettingNickname,
            showAllResults = context.showAllSettingsResults,
            showExpandControls = context.showSettingsExpandControls,
            onExpandClick = context.settingsExpandClick,
            showWallpaperBackground = params.settingsParams.showWallpaperBackground
        )
    }
}
