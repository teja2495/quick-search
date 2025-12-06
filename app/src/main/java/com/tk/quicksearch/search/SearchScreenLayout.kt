package com.tk.quicksearch.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut

/**
 * Data class holding all the state needed for section rendering.
 */
data class SectionRenderingState(
    val isSearching: Boolean,
    val expandedSection: ExpandedSection,
    val hasAppResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val shouldShowApps: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean,
    val autoExpandFiles: Boolean,
    val autoExpandContacts: Boolean,
    val autoExpandSettings: Boolean,
    val hasMultipleExpandableSections: Boolean,
    val displayApps: List<AppInfo>,
    val contactResults: List<ContactInfo>,
    val fileResults: List<DeviceFile>,
    val settingResults: List<SettingShortcut>,
    val pinnedContacts: List<ContactInfo>,
    val pinnedFiles: List<DeviceFile>,
    val pinnedSettings: List<SettingShortcut>,
    val orderedSections: List<SearchSection>
)

/**
 * Renders the scrollable content area with sections based on layout mode.
 */
@Composable
fun SearchContentArea(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onRequestUsagePermission: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val useKeyboardAlignedLayout = state.keyboardAlignedLayout && 
        renderingState.expandedSection == ExpandedSection.NONE

    Box(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        ContentLayout(
            modifier = Modifier
                .align(if (useKeyboardAlignedLayout) Alignment.BottomCenter else Alignment.TopStart)
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 12.dp),
            state = state,
            renderingState = renderingState,
            contactsParams = contactsParams,
            filesParams = filesParams,
            settingsParams = settingsParams,
            appsParams = appsParams,
            onRequestUsagePermission = onRequestUsagePermission,
            isReversed = useKeyboardAlignedLayout
        )
    }
}

/**
 * Unified content layout that handles both keyboard-aligned and top-aligned layouts.
 */
@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onRequestUsagePermission: () -> Unit,
    isReversed: Boolean
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show error banner if there's an error message
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            InfoBanner(message = message)
        }

        val hasQuery = state.query.isNotBlank()
        val isExpanded = renderingState.expandedSection != ExpandedSection.NONE

        when {
            isReversed -> {
                // Keyboard-aligned: search results first, then pinned items
                if (hasQuery) {
                    SearchResultsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = true,
                        keyboardAlignedLayout = state.keyboardAlignedLayout
                    )
                }
                if (!isExpanded) {
                    PinnedItemsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = true
                    )
                }
            }
            else -> {
                // Top-aligned: pinned items first, then search results
                when {
                    !isExpanded -> {
                        PinnedItemsSections(
                            renderingState = renderingState,
                            contactsParams = contactsParams,
                            filesParams = filesParams,
                        settingsParams = settingsParams,
                            appsParams = appsParams,
                            isReversed = false
                        )
                    }
                    !hasQuery -> {
                        ExpandedPinnedSections(
                            renderingState = renderingState,
                            contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams
                        )
                    }
                }
                if (hasQuery) {
                    SearchResultsSections(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                    settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = false,
                        keyboardAlignedLayout = state.keyboardAlignedLayout
                    )
                }
            }
        }
    }
}

/**
 * Renders search result sections.
 */
@Composable
private fun SearchResultsSections(
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
        shouldRenderFiles = shouldShowFilesSection(renderingState, filesParams) && !isContactsExpanded,
        shouldRenderContacts = shouldShowContactsSection(renderingState, contactsParams) && !isFilesExpanded,
        shouldRenderApps = shouldShowAppsSection(renderingState),
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
        showSettingsExpandControls = renderingState.hasMultipleExpandableSections,
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
private fun PinnedItemsSections(
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
private fun ExpandedPinnedSections(
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
