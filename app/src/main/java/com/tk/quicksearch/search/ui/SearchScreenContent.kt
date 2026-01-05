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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
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
    onRetryDirectSearch: () -> Unit,
    onDirectSearchEmailClick: (String) -> Unit,
    onPhoneNumberClick: (String) -> Unit,
    onWebSuggestionClick: (String) -> Unit,
    onKeyboardSwitchToggle: () -> Unit,
    expandedSection: ExpandedSection,
    manuallySwitchedToNumberKeyboard: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    // Calculate enabled engines
    val enabledEngines: List<SearchEngine> = remember(
        state.searchEngineOrder,
        state.disabledSearchEngines
    ) {
        state.searchEngineOrder.filter { it !in state.disabledSearchEngines }
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
                    val primaryEngine = enabledEngines.firstOrNull()
                    if (primaryEngine != null && trimmedQuery.isNotBlank()) {
                        onSearchEngineClick(trimmedQuery, primaryEngine)
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
            onRetryDirectSearch = onRetryDirectSearch,
            onPhoneNumberClick = onPhoneNumberClick,
            onEmailClick = onDirectSearchEmailClick,
            onWebSuggestionClick = onWebSuggestionClick
        )

        // Keyboard switch pill - appears above search engines
        if (expandedSection == ExpandedSection.NONE) {
            val pillText = if (manuallySwitchedToNumberKeyboard) {
                stringResource(R.string.keyboard_switch_back)
            } else if (hasMathExpression && state.calculatorEnabled) {
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
        if (expandedSection == ExpandedSection.NONE && state.searchEngineSectionEnabled) {
            SearchEngineIconsSection(
                query = state.query,
                hasAppResults = renderingState.hasAppResults,
                enabledEngines = enabledEngines,
                onSearchEngineClick = onSearchEngineClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                modifier = Modifier.imePadding()
            )
        } else if (expandedSection == ExpandedSection.NONE && !state.searchEngineSectionEnabled) {
            // Add padding when search engine section is disabled to prevent keyboard from covering content
            Spacer(modifier = Modifier.imePadding())
        }
    }
}
