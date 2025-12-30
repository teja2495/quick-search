package com.tk.quicksearch.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
    scrollState: androidx.compose.foundation.ScrollState,
    onRetryDirectSearch: () -> Unit,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {}
) {
    val useKeyboardAlignedLayout = state.keyboardAlignedLayout &&
        renderingState.expandedSection == ExpandedSection.NONE
    val DirectSearchState = state.DirectSearchState
    val showDirectSearch = DirectSearchState.status != DirectSearchStatus.Idle
    val showCalculator = state.calculatorState.result != null
    val hideResultsForDirectSearch = showDirectSearch || showCalculator
    val hasQuery = state.query.isNotBlank()
    val hasAnySearchContent =
        shouldShowAppsSection(renderingState) ||
            shouldShowContactsSection(renderingState, contactsParams) ||
            shouldShowFilesSection(renderingState, filesParams) ||
            shouldShowSettingsSection(renderingState)
    val shouldShowEmptyResultsMessage = hasQuery && !hasAnySearchContent
    val alignResultsToBottom = useKeyboardAlignedLayout &&
        !showDirectSearch &&
        !showCalculator &&
        !shouldShowEmptyResultsMessage

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        // Ignore bottom alignment when direct answer card, calculator result, or empty state is showing
        val verticalArrangement = if (alignResultsToBottom && !showCalculator) {
            Arrangement.spacedBy(12.dp, Alignment.Bottom)
        } else {
            Arrangement.spacedBy(12.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(
                    scrollState,
                    reverseScrolling = alignResultsToBottom
                )
                .padding(vertical = 12.dp),
            verticalArrangement = verticalArrangement
        ) {
            val showCalculator = state.calculatorState.result != null
            if (showCalculator) {
                CalculatorResult(
                    calculatorState = state.calculatorState,
                    showWallpaperBackground = state.showWallpaperBackground
                )
            }
            if (showDirectSearch) {
                DirectSearchResult(
                    DirectSearchState = DirectSearchState,
                    onRetry = onRetryDirectSearch,
                    showWallpaperBackground = state.showWallpaperBackground,
                    onPhoneNumberClick = onPhoneNumberClick,
                    onEmailClick = onEmailClick
                )
            }
            ContentLayout(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                renderingState = renderingState,
                contactsParams = contactsParams,
                filesParams = filesParams,
                settingsParams = settingsParams,
                appsParams = appsParams,
                onRequestUsagePermission = onRequestUsagePermission,
                // Ignore keyboard-aligned layout when direct answer card or calculator is showing
                minContentHeight = this@BoxWithConstraints.maxHeight,
                isReversed = useKeyboardAlignedLayout && !showDirectSearch && !showCalculator,
                hideResults = hideResultsForDirectSearch,
                onWebSuggestionClick = onWebSuggestionClick
            )
        }
    }
}

/**
 * Unified content layout that handles both keyboard-aligned and top-aligned layouts.
 */
@Composable
fun ContentLayout(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onRequestUsagePermission: () -> Unit,
    minContentHeight: Dp,
    isReversed: Boolean,
    hideResults: Boolean,
    onWebSuggestionClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show error banner if there's an error message
        state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            InfoBanner(message = message)
        }

        if (hideResults) {
            return
        }

        val hasQuery = state.query.isNotBlank()
        val isExpanded = renderingState.expandedSection != ExpandedSection.NONE
        val hasAnySearchContent =
            shouldShowAppsSection(renderingState) ||
                shouldShowContactsSection(renderingState, contactsParams) ||
                shouldShowFilesSection(renderingState, filesParams) ||
                shouldShowSettingsSection(renderingState)

        if (hasQuery && !hasAnySearchContent) {
            // Show web suggestions if available and enabled, otherwise show empty message
            if (state.webSuggestions.isNotEmpty() && state.webSuggestionsEnabled) {
                WebSuggestionsSection(
                    suggestions = state.webSuggestions,
                    onSuggestionClick = onWebSuggestionClick,
                    showWallpaperBackground = state.showWallpaperBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                EmptyResultsMessage(
                    enabledSections = renderingState.orderedSections,
                    showWallpaperBackground = state.showWallpaperBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            return
        }

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



