package com.tk.quicksearch.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchengines.*

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
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    showCalculator: Boolean = false,
    showDirectSearch: Boolean = false,
    DirectSearchState: DirectSearchState? = null
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
        // Use bottom alignment when keyboard-aligned layout is enabled and no special states are showing
        val verticalArrangement = if (alignResultsToBottom) {
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
            ContentLayout(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                renderingState = renderingState,
                contactsParams = contactsParams,
                filesParams = filesParams,
                settingsParams = settingsParams,
                appsParams = appsParams,
                onRequestUsagePermission = onRequestUsagePermission,
                // Pass calculator and direct search state to ContentLayout
                minContentHeight = this@BoxWithConstraints.maxHeight,
                isReversed = useKeyboardAlignedLayout && !showDirectSearch,
                hideResults = false, // Always show content layout, let it handle visibility internally
                showCalculator = showCalculator,
                showDirectSearch = showDirectSearch,
                DirectSearchState = DirectSearchState,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onEmailClick,
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
    showCalculator: Boolean = false,
    showDirectSearch: Boolean = false,
    DirectSearchState: DirectSearchState? = null,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
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

        // Show calculator result if present
        if (showCalculator) {
            CalculatorResult(
                calculatorState = state.calculatorState,
                showWallpaperBackground = state.showWallpaperBackground
            )
        }

        // Show direct search result if present
        if (showDirectSearch && DirectSearchState != null) {
            DirectSearchResult(
                DirectSearchState = DirectSearchState,
                showWallpaperBackground = state.showWallpaperBackground,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onEmailClick
            )
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

        if (hasQuery && !hasAnySearchContent && !showCalculator) {
            val showWebSuggestions = state.webSuggestions.isNotEmpty() && state.webSuggestionsEnabled

            AnimatedVisibility(
                visible = showWebSuggestions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WebSuggestionsSection(
                    suggestions = state.webSuggestions,
                    onSuggestionClick = onWebSuggestionClick,
                    showWallpaperBackground = state.showWallpaperBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (!showWebSuggestions) {
                EmptyResultsMessage(
                    query = state.query,
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



