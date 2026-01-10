package com.tk.quicksearch.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchengines.SearchEngineIconsSection
import com.tk.quicksearch.util.CalculatorUtils

@Composable
internal fun SearchScreenContent(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    onAppClick: (com.tk.quicksearch.model.AppInfo) -> Unit,
    onRequestUsagePermission: () -> Unit,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    onDirectSearchEmailClick: (String) -> Unit,
    onPhoneNumberClick: (String) -> Unit,
    onWebSuggestionClick: (String) -> Unit,
    onKeyboardSwitchToggle: () -> Unit,
    expandedSection: ExpandedSection,
    manuallySwitchedToNumberKeyboard: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onClearDetectedShortcut: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate enabled engines
    val enabledEngines: List<SearchEngine> = remember(
        state.searchEngineOrder,
        state.disabledSearchEngines
    ) {
        state.searchEngineOrder.filter { it !in state.disabledSearchEngines }
    }

    // Search engine scroll state for auto-scroll during onboarding
    val searchEngineScrollState = rememberLazyListState()

    // Auto-scroll search engines during onboarding
    LaunchedEffect(state.showSearchEngineOnboarding) {
        if (state.showSearchEngineOnboarding) {
            // Smooth continuous scroll that loops
            while (true) {
                // Check if we can scroll further
                val layoutInfo = searchEngineScrollState.layoutInfo
                val canScrollForward = layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
                    lastItem.index < layoutInfo.totalItemsCount - 1 || 
                    lastItem.offset + lastItem.size > layoutInfo.viewportEndOffset
                } ?: false
                
                if (!canScrollForward) {
                    // Reached the end, scroll back to start and continue
                    delay(500) // Pause briefly at the end
                    searchEngineScrollState.animateScrollToItem(index = 0, scrollOffset = 0)
                    delay(500) // Pause briefly at the start
                    continue
                }
                
                // Get current scroll position
                val currentIndex = searchEngineScrollState.firstVisibleItemIndex
                val currentOffset = searchEngineScrollState.firstVisibleItemScrollOffset
                
                // Increment by small amount for smooth scroll
                val newOffset = currentOffset + 2
                
                // Smooth scroll
                delay(30) // Small delay for smooth effect
                
                searchEngineScrollState.scrollToItem(
                    index = currentIndex,
                    scrollOffset = newOffset
                )
            }
        } else {
            // When onboarding is dismissed, scroll back to start
            searchEngineScrollState.animateScrollToItem(index = 0, scrollOffset = 0)
        }
    }

    // Check for math expressions to determine pill visibility
    val hasMathExpression = CalculatorUtils.isMathExpression(state.query)

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp
            ),
        verticalArrangement = Arrangement.Top
    ) {
        // Fixed search bar at the top
        PersistentSearchField(
            query = state.query,
            onQueryChange = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            enabledEngines = enabledEngines,
            shouldUseNumberKeyboard = manuallySwitchedToNumberKeyboard,
            detectedShortcutEngine = state.detectedShortcutEngine,
            onSearchAction = {
                val trimmedQuery = state.query.trim()

                // If query has trailing/leading spaces, trim it first
                if (state.query != trimmedQuery) {
                    onQueryChanged(trimmedQuery)
                }

                val firstApp = renderingState.displayApps.firstOrNull()
                if (firstApp != null) {
                    onAppClick(firstApp)
                } else {
                    // Check if a shortcut is detected
                    if (state.detectedShortcutEngine != null) {
                        // Remove the shortcut (first word) from the query
                        val queryWithoutShortcut = trimmedQuery.split("\\s+".toRegex()).drop(1).joinToString(" ")
                        onSearchEngineClick(queryWithoutShortcut, state.detectedShortcutEngine)
                    } else {
                        val primaryEngine = enabledEngines.firstOrNull()
                        if (primaryEngine != null && trimmedQuery.isNotBlank()) {
                            onSearchEngineClick(trimmedQuery, primaryEngine)
                        }
                    }
                }
            }
        )

        // Add spacing between search bar and apps list when bottom aligned setting is off
        if (!state.keyboardAlignedLayout) {
            Spacer(modifier = Modifier.padding(top = 8.dp))
        }

        // Scrollable content between search bar and search engines
        SearchContentArea(
            modifier = Modifier.weight(1f),
            state = state,
            renderingState = renderingState,
            contactsParams = contactsParams,
            filesParams = filesParams,
            settingsParams = settingsParams,
            appsParams = appsParams,
            onRequestUsagePermission = onRequestUsagePermission,
            scrollState = scrollState,
            onPhoneNumberClick = onPhoneNumberClick,
            onEmailClick = onDirectSearchEmailClick,
            onWebSuggestionClick = onWebSuggestionClick,
            showCalculator = state.calculatorState.result != null,
            showDirectSearch = state.DirectSearchState.status != DirectSearchStatus.Idle,
            DirectSearchState = state.DirectSearchState
        )

        // Keyboard switch pill - appears above search engines
        if (expandedSection == ExpandedSection.NONE) {
            val pillText = if (manuallySwitchedToNumberKeyboard) {
                stringResource(R.string.keyboard_switch_back)
            } else if (state.query.isNotEmpty() && state.query.none { it.isLetter() }) {
                stringResource(R.string.keyboard_switch_to_number)
            } else {
                null
            }

            pillText?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    KeyboardSwitchPill(
                        text = it,
                        onClick = onKeyboardSwitchToggle,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, or when search engine section is disabled
        // Exception: always show when query starts with a search engine shortcut
        if (expandedSection == ExpandedSection.NONE && (state.searchEngineSectionEnabled || state.detectedShortcutEngine != null)) {
            SearchEngineIconsSection(
                query = state.query,
                hasAppResults = renderingState.hasAppResults,
                enabledEngines = enabledEngines,
                onSearchEngineClick = onSearchEngineClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                externalScrollState = searchEngineScrollState,
                detectedShortcutEngine = state.detectedShortcutEngine,
                onClearDetectedShortcut = onClearDetectedShortcut,
                modifier = Modifier.imePadding()
            )
        } else if (expandedSection == ExpandedSection.NONE && !state.searchEngineSectionEnabled) {
            // Add padding when search engine section is disabled to prevent keyboard from covering content
            Spacer(modifier = Modifier.imePadding())
        }
    }
}
