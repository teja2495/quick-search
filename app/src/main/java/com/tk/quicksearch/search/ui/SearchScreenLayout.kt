package com.tk.quicksearch.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.searchEngines.*

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
    val orderedSections: List<SearchSection>,
    val shortcutDetected: Boolean = false
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
    onSearchEngineClick: (String, SearchEngine) -> Unit = { _, _ -> },
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onDeleteRecentQuery: (String) -> Unit = {},
    showCalculator: Boolean = false,
    showDirectSearch: Boolean = false,
    DirectSearchState: DirectSearchState? = null
) {
    val useKeyboardAlignedLayout = state.keyboardAlignedLayout &&
        renderingState.expandedSection == ExpandedSection.NONE
    val DirectSearchState = state.DirectSearchState
    val showDirectSearch = DirectSearchState.status != DirectSearchStatus.Idle
    val showCalculator = state.calculatorState.result != null
    val hideOtherResults = showDirectSearch || showCalculator
    val hasQuery = state.query.isNotBlank()
    val hasAnySearchContent =
        shouldShowAppsSection(renderingState) ||
            shouldShowContactsSection(renderingState, contactsParams) ||
            shouldShowFilesSection(renderingState, filesParams) ||
            shouldShowSettingsSection(renderingState)
    val alignResultsToBottom = useKeyboardAlignedLayout &&
        !showDirectSearch &&
        !showCalculator

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
                .padding(top = 4.dp, bottom = 12.dp),
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
                hideResults = hideOtherResults,
                showCalculator = showCalculator,
                showDirectSearch = showDirectSearch,
                DirectSearchState = DirectSearchState,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onEmailClick,
                onWebSuggestionClick = onWebSuggestionClick,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onSearchEngineClick = onSearchEngineClick,
                onDeleteRecentQuery = onDeleteRecentQuery
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
    onWebSuggestionClick: (String) -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onSearchEngineClick: (String, SearchEngine) -> Unit = { _, _ -> },
    onDeleteRecentQuery: (String) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
        val hasDirectSearchAnswer = showDirectSearch && DirectSearchState != null

        // Determine whether to show recent queries (when empty) or web suggestions (when query but no results)
        val showRecentQueries = !hasQuery && 
                                 state.recentQueriesEnabled && 
                                 state.recentQueries.isNotEmpty()
        val showWebSuggestions = hasQuery && 
                                  !hasAnySearchContent && 
                                  !showCalculator && 
                                  !hasDirectSearchAnswer &&
                                  state.webSuggestions.isNotEmpty() &&
                                  state.webSuggestionsEnabled &&
                                  !state.webSuggestionWasSelected

        // When query exists with no results, show web suggestions/search engines and return early
        // Only show search engine list if inline style is enabled (!isSearchEngineCompactMode)
        if (hasQuery && !hasAnySearchContent && !showCalculator && !hasDirectSearchAnswer) {

            // When results are at bottom (reversed), show search engine cards first, then web suggestions
            // When results are at top (normal), show web suggestions first, then search engine cards
            if (isReversed) {
                // Reversed layout: search engine cards first, then web suggestions at bottom
                if (state.detectedShortcutEngine == null && !state.isSearchEngineCompactMode) {
                    NoResultsSearchEngineCards(
                        query = state.query,
                        enabledEngines = state.searchEngineOrder.filter { it !in state.disabledSearchEngines },
                        onSearchEngineClick = onSearchEngineClick,
                        onCustomizeClick = onCustomizeSearchEnginesClick,
                        isReversed = isReversed,
                        showWallpaperBackground = state.showWallpaperBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Show web suggestions at the bottom when reversed
                AnimatedVisibility(
                    visible = showWebSuggestions,
                    enter = fadeIn(),
                    exit = shrinkVertically()
                ) {
                    WebSuggestionsSection(
                        suggestions = state.webSuggestions,
                        onSuggestionClick = onWebSuggestionClick,
                        showWallpaperBackground = state.showWallpaperBackground,
                        reverseOrder = isReversed,
                        isShortcutDetected = state.detectedShortcutEngine != null,
                        isRecentQuery = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Normal layout: web suggestions first, then search engine cards
                AnimatedVisibility(
                    visible = showWebSuggestions,
                    enter = fadeIn(),
                    exit = shrinkVertically()
                ) {
                    WebSuggestionsSection(
                        suggestions = state.webSuggestions,
                        onSuggestionClick = onWebSuggestionClick,
                        showWallpaperBackground = state.showWallpaperBackground,
                        reverseOrder = false,
                        isShortcutDetected = state.detectedShortcutEngine != null,
                        isRecentQuery = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.detectedShortcutEngine == null && !state.isSearchEngineCompactMode) {
                    NoResultsSearchEngineCards(
                        query = state.query,
                        enabledEngines = state.searchEngineOrder.filter { it !in state.disabledSearchEngines },
                        onSearchEngineClick = onSearchEngineClick,
                        onCustomizeClick = onCustomizeSearchEnginesClick,
                        isReversed = isReversed,
                        showWallpaperBackground = state.showWallpaperBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Show "No results" message when search engine compact mode is enabled
            // and web suggestions API was called but returned no results or web suggestions are disabled
            val shouldShowNoResults = state.isSearchEngineCompactMode && (
                !state.webSuggestionsEnabled ||
                (state.webSuggestionsEnabled && state.query.trim().length >= 2 && state.webSuggestions.isEmpty())
            )

            // Add delay before showing no results text to avoid flashing before web suggestions load
            var showNoResultsText by remember { mutableStateOf(false) }
            LaunchedEffect(shouldShowNoResults, state.query) {
                if (shouldShowNoResults) {
                    delay(1000L) // 300ms delay to match web suggestions delay
                    showNoResultsText = true
                } else {
                    showNoResultsText = false
                }
            }

            if (showNoResultsText) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }

            return
        }

        when {
            isReversed -> {
                // Keyboard-aligned (reversed): search results first (when hasQuery), then other pinned items, then recent queries, then apps at bottom
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
                    val shouldShowPinned = !renderingState.isSearching
                    
                    val pinnedParams = SectionRenderParams(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = true
                    )
                    
                    val orderedSections = getOrderedSections(renderingState, true)
                    
                    orderedSections.forEach { section ->
                        when (section) {
                            SearchSection.APPS -> {
                                // In reversed layout, show recent queries before apps (since apps will be at bottom)
                                if (showRecentQueries) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(),
                                        exit = shrinkVertically()
                                    ) {
                                        WebSuggestionsSection(
                                            suggestions = state.recentQueries,
                                            onSuggestionClick = onWebSuggestionClick,
                                            showWallpaperBackground = state.showWallpaperBackground,
                                            reverseOrder = isReversed,
                                            isShortcutDetected = false,
                                            isRecentQuery = true,
                                            onDeleteRecentQuery = onDeleteRecentQuery,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                
                                // Render apps section at the bottom for reversed layout
                                if (shouldShowPinned && shouldShowAppsSection(renderingState)) {
                                    val appsContext = SectionRenderContext(
                                        shouldRenderApps = true
                                    )
                                    renderSection(SearchSection.APPS, pinnedParams, appsContext)
                                }
                            }
                            else -> {
                                // Render other pinned sections
                                val pinnedContext = SectionRenderContext(
                                    shouldRenderFiles = shouldShowPinned && renderingState.hasPinnedFiles && renderingState.shouldShowFiles && section == SearchSection.FILES,
                                    shouldRenderContacts = shouldShowPinned && renderingState.hasPinnedContacts && renderingState.shouldShowContacts && section == SearchSection.CONTACTS,
                                    shouldRenderSettings = shouldShowPinned && renderingState.hasPinnedSettings && renderingState.shouldShowSettings && section == SearchSection.SETTINGS,
                                    shouldRenderApps = false,
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
                                renderSection(section, pinnedParams, pinnedContext)
                            }
                        }
                    }
                }
            }
            else -> {
                // Top-aligned: pinned items first, then search results
                when {
                    !isExpanded -> {
                        val shouldShowPinned = !renderingState.isSearching
                        
                        val pinnedParams = SectionRenderParams(
                            renderingState = renderingState,
                            contactsParams = contactsParams,
                            filesParams = filesParams,
                            settingsParams = settingsParams,
                            appsParams = appsParams,
                            isReversed = false
                        )
                        
                        val orderedSections = getOrderedSections(renderingState, false)
                        
                        orderedSections.forEach { section ->
                            when (section) {
                                SearchSection.APPS -> {
                                    // Render apps section
                                    if (shouldShowPinned && shouldShowAppsSection(renderingState)) {
                                        val appsContext = SectionRenderContext(
                                            shouldRenderApps = true
                                        )
                                        renderSection(SearchSection.APPS, pinnedParams, appsContext)
                                    }
                                    
                                    // Show recent queries right after apps
                                    if (showRecentQueries) {
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(),
                                            exit = shrinkVertically()
                                        ) {
                                            WebSuggestionsSection(
                                                suggestions = state.recentQueries,
                                                onSuggestionClick = onWebSuggestionClick,
                                                showWallpaperBackground = state.showWallpaperBackground,
                                                reverseOrder = isReversed,
                                                isShortcutDetected = false,
                                                isRecentQuery = true,
                                                onDeleteRecentQuery = onDeleteRecentQuery,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    // Render other pinned sections
                                    val pinnedContext = SectionRenderContext(
                                        shouldRenderFiles = shouldShowPinned && renderingState.hasPinnedFiles && renderingState.shouldShowFiles && section == SearchSection.FILES,
                                        shouldRenderContacts = shouldShowPinned && renderingState.hasPinnedContacts && renderingState.shouldShowContacts && section == SearchSection.CONTACTS,
                                        shouldRenderSettings = shouldShowPinned && renderingState.hasPinnedSettings && renderingState.shouldShowSettings && section == SearchSection.SETTINGS,
                                        shouldRenderApps = false,
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
                                    renderSection(section, pinnedParams, pinnedContext)
                                }
                            }
                        }
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



