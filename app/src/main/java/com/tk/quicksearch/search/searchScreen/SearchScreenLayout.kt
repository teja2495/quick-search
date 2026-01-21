package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.directSearch.CalculatorResult
import com.tk.quicksearch.search.directSearch.DirectSearchResult
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchEngines.*
import com.tk.quicksearch.search.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.ui.theme.DesignTokens
import kotlinx.coroutines.delay

/** Custom shape that rounds only the top corners */
private val TopRoundedShape =
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

/** Data class holding all the state needed for section rendering. */
data class SectionRenderingState(
        val isSearching: Boolean,
        val expandedSection: ExpandedSection,
        val hasAppResults: Boolean,
        val hasAppShortcutResults: Boolean,
        val hasContactResults: Boolean,
        val hasFileResults: Boolean,
        val hasSettingResults: Boolean,
        val hasPinnedAppShortcuts: Boolean,
        val hasPinnedContacts: Boolean,
        val hasPinnedFiles: Boolean,
        val hasPinnedSettings: Boolean,
        val shouldShowApps: Boolean,
        val shouldShowAppShortcuts: Boolean,
        val shouldShowContacts: Boolean,
        val shouldShowFiles: Boolean,
        val shouldShowSettings: Boolean,
        val hasMultipleExpandableSections: Boolean,
        val displayApps: List<AppInfo>,
        val appShortcutResults: List<com.tk.quicksearch.search.data.StaticShortcut>,
        val contactResults: List<ContactInfo>,
        val fileResults: List<DeviceFile>,
        val settingResults: List<DeviceSetting>,
        val pinnedAppShortcuts: List<com.tk.quicksearch.search.data.StaticShortcut>,
        val pinnedContacts: List<ContactInfo>,
        val pinnedFiles: List<DeviceFile>,
        val pinnedSettings: List<DeviceSetting>,
        val orderedSections: List<SearchSection>,
        val shortcutDetected: Boolean = false
)

/** Renders the scrollable content area with sections based on layout mode. */
@Composable
fun SearchContentArea(
        modifier: Modifier = Modifier,
        state: SearchUiState,
        renderingState: SectionRenderingState,
        contactsParams: ContactsSectionParams,
        filesParams: FilesSectionParams,
        appShortcutsParams: AppShortcutsSectionParams,
        settingsParams: SettingsSectionParams,
        appsParams: AppsSectionParams,
        onRequestUsagePermission: () -> Unit,
        scrollState: androidx.compose.foundation.ScrollState,
        onPhoneNumberClick: (String) -> Unit = {},
        onEmailClick: (String) -> Unit = {},
        onWebSuggestionClick: (String) -> Unit = {},
        onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onDeleteRecentQuery: (String) -> Unit = {},
        showCalculator: Boolean = false,
        showDirectSearch: Boolean = false,
        directSearchState: DirectSearchState? = null
) {
        val useOneHandedMode =
                state.oneHandedMode && renderingState.expandedSection == ExpandedSection.NONE
        val hideOtherResults = showDirectSearch || showCalculator
        val hasQuery = state.query.isNotBlank()
        val hasAnySearchContent =
                shouldShowAppsSection(renderingState) ||
                        shouldShowAppShortcutsSection(renderingState) ||
                        shouldShowContactsSection(renderingState, contactsParams) ||
                        shouldShowFilesSection(renderingState, filesParams) ||
                        shouldShowSettingsSection(renderingState)
        val alignResultsToBottom = useOneHandedMode && !showDirectSearch && !showCalculator
        val hasAnySearchResults = hasAnySearchResults(state)

        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                // Use bottom alignment when one-handed mode is enabled and no special states are
                // showing
                val verticalArrangement =
                        if (alignResultsToBottom) {
                                Arrangement.spacedBy(DesignTokens.SpacingMedium, Alignment.Bottom)
                        } else {
                                Arrangement.spacedBy(DesignTokens.SpacingMedium)
                        }

                // When there are no results and inline search engine style is not enabled,
                // hide the scroll view and show only the no results text
                val shouldHideScrollView =
                        hasQuery && !hasAnySearchResults && state.isSearchEngineCompactMode

                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(min = maxHeight)
                                        .clip(TopRoundedShape)
                                        .then(
                                                if (shouldHideScrollView) {
                                                        Modifier.padding(
                                                                bottom = DesignTokens.SpacingMedium
                                                        )
                                                } else {
                                                        Modifier.verticalScroll(
                                                                        scrollState,
                                                                        reverseScrolling =
                                                                                alignResultsToBottom
                                                                )
                                                                .padding(
                                                                        bottom =
                                                                                DesignTokens
                                                                                        .SpacingMedium
                                                                )
                                                }
                                        ),
                        verticalArrangement =
                                if (shouldHideScrollView) Arrangement.Top else verticalArrangement
                ) {
                        if (shouldHideScrollView) {
                                // Show only the no results text without scroll view
                                // Determine whether to show "No results" message when search engine
                                // compact mode is enabled
                                // and web suggestions API was called but returned no results or web
                                // suggestions are disabled
                                val shouldShowNoResults =
                                        state.isSearchEngineCompactMode &&
                                                (!state.webSuggestionsEnabled ||
                                                        (state.webSuggestionsEnabled &&
                                                                state.query.trim().length >= 2 &&
                                                                state.webSuggestions.isEmpty()))

                                // Add delay before showing no results text to avoid flashing before
                                // web suggestions load
                                var showNoResultsText by remember { mutableStateOf(false) }
                                LaunchedEffect(shouldShowNoResults, state.query) {
                                        if (shouldShowNoResults) {
                                                delay(500L) // Wait for web suggestions load
                                                showNoResultsText = true
                                        } else {
                                                showNoResultsText = false
                                        }
                                }

                                if (showNoResultsText) {
                                        Text(
                                                text = stringResource(R.string.no_results_found),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.6f
                                                        ),
                                                textAlign = TextAlign.Center,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        top =
                                                                                DesignTokens
                                                                                        .SpacingSmall,
                                                                        start =
                                                                                DesignTokens
                                                                                        .SpacingLarge,
                                                                        end =
                                                                                DesignTokens
                                                                                        .SpacingLarge
                                                                )
                                        )
                                }
                        } else {
                                ContentLayout(
                                        modifier = Modifier.fillMaxWidth(),
                                        state = state,
                                        renderingState = renderingState,
                                        contactsParams = contactsParams,
                                        filesParams = filesParams,
                                        appShortcutsParams = appShortcutsParams,
                                        settingsParams = settingsParams,
                                        appsParams = appsParams,
                                        onRequestUsagePermission = onRequestUsagePermission,
                                        // Pass calculator and direct search state to ContentLayout
                                        minContentHeight = this@BoxWithConstraints.maxHeight,
                                        isReversed = useOneHandedMode && !showDirectSearch,
                                        hideResults = hideOtherResults,
                                        showCalculator = showCalculator,
                                        showDirectSearch = showDirectSearch,
                                        directSearchState = directSearchState,
                                        onPhoneNumberClick = onPhoneNumberClick,
                                        onEmailClick = onEmailClick,
                                        onWebSuggestionClick = onWebSuggestionClick,
                                        onCustomizeSearchEnginesClick =
                                                onCustomizeSearchEnginesClick,
                                        onSearchTargetClick = onSearchTargetClick,
                                        onDeleteRecentQuery = onDeleteRecentQuery
                                )
                        }
                }

                if (renderingState.expandedSection != ExpandedSection.NONE) {
                        com.tk.quicksearch.search.contacts.CollapseButton(
                                onClick = {
                                        when (renderingState.expandedSection) {
                                                ExpandedSection.FILES -> filesParams.onExpandClick()
                                                ExpandedSection.CONTACTS ->
                                                        contactsParams.onExpandClick()
                                                ExpandedSection.APP_SHORTCUTS ->
                                                        appShortcutsParams.onExpandClick()
                                                ExpandedSection.SETTINGS ->
                                                        settingsParams.onExpandClick()
                                                else -> {}
                                        }
                                },
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 28.dp)
                        )
                }
        }
}

/** Unified content layout that handles both one-handed mode and top-aligned layouts. */
@Composable
fun ContentLayout(
        modifier: Modifier = Modifier,
        state: SearchUiState,
        renderingState: SectionRenderingState,
        contactsParams: ContactsSectionParams,
        filesParams: FilesSectionParams,
        appShortcutsParams: AppShortcutsSectionParams,
        settingsParams: SettingsSectionParams,
        appsParams: AppsSectionParams,
        onRequestUsagePermission: () -> Unit,
        minContentHeight: Dp,
        isReversed: Boolean,
        hideResults: Boolean,
        showCalculator: Boolean = false,
        showDirectSearch: Boolean = false,
        directSearchState: DirectSearchState? = null,
        onPhoneNumberClick: (String) -> Unit = {},
        onEmailClick: (String) -> Unit = {},
        onWebSuggestionClick: (String) -> Unit = {},
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
        onDeleteRecentQuery: (String) -> Unit = {}
) {
        // 1. Determine Layout Order based on ItemPriorityConfig
        val hasQuery = state.query.isNotBlank()
        val baseLayoutOrder = ItemPriorityConfig.getLayoutOrder(hasQuery)

        // 2. Apply One-Handed Mode Reversal if needed
        // User Requirement: "When one handed mode is enabled the same order is reversed."
        // isReversed flag passed here reflects one-handed mode state.
        val finalLayoutOrder = if (isReversed) baseLayoutOrder.reversed() else baseLayoutOrder

        // 3. Prepare Shared Rendering Context and Params
        // We reuse the extracted logic to determine visibility and expansion states
        val sectionContext =
                rememberSectionRenderContext(
                        state = state,
                        renderingState = renderingState,
                        filesParams = filesParams,
                        contactsParams = contactsParams,
                        settingsParams = settingsParams,
                        appShortcutsParams = appShortcutsParams,
                        appsParams = appsParams,
                        isSearching = hasQuery,
                        oneHandedMode =
                                state.oneHandedMode // This affects list reversal inside helpers
                )

        val sectionParams =
                SectionRenderParams(
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        appShortcutsParams = appShortcutsParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        isReversed = isReversed
                )

        // Pre-calculate common states
        val isExpanded = renderingState.expandedSection != ExpandedSection.NONE
        val hasAnySearchResults = hasAnySearchResults(state)
        val hasDirectSearchAnswer = showDirectSearch && directSearchState != null

        // Web Suggestions Logic
        val showWebSuggestions =
                hasQuery &&
                        !showCalculator &&
                        !hasDirectSearchAnswer &&
                        state.webSuggestions.isNotEmpty() &&
                        state.webSuggestionsEnabled &&
                        !state.webSuggestionWasSelected

        // Recent Queries Logic (for App Open State mainly, but CONFIG has RECENT_QUERIES item)
        val showRecentQueries =
                !hasQuery && state.recentQueriesEnabled && state.recentQueries.isNotEmpty()

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                finalLayoutOrder.forEach { itemType ->
                        // Check if this is a section item
                        val isSectionItem =
                                when (itemType) {
                                        ItemPriorityConfig.ItemType.APPS_SECTION,
                                        ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION,
                                        ItemPriorityConfig.ItemType.FILES_SECTION,
                                        ItemPriorityConfig.ItemType.CONTACTS_SECTION,
                                        ItemPriorityConfig.ItemType.SETTINGS_SECTION -> true
                                        else -> false
                                }

                        // If a section is expanded, we hide all OTHER non-section items.
                        if (isExpanded && !isSectionItem) return@forEach

                        when (itemType) {
                                ItemPriorityConfig.ItemType.ERROR_BANNER -> {
                                        if (state.screenState is ScreenVisibilityState.Error) {
                                                InfoBanner(
                                                        message =
                                                                (state.screenState as
                                                                                ScreenVisibilityState.Error)
                                                                        .message
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.CALCULATOR_RESULT -> {
                                        if (showCalculator) {
                                                CalculatorResult(
                                                        calculatorState = state.calculatorState,
                                                        showWallpaperBackground =
                                                                state.showWallpaperBackground,
                                                        oneHandedMode = state.oneHandedMode
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.DIRECT_SEARCH_RESULT -> {
                                        if (showDirectSearch && directSearchState != null) {
                                                DirectSearchResult(
                                                        DirectSearchState = directSearchState,
                                                        showWallpaperBackground =
                                                                state.showWallpaperBackground,
                                                        oneHandedMode = state.oneHandedMode,
                                                        onPhoneNumberClick = onPhoneNumberClick,
                                                        onEmailClick = onEmailClick
                                                )
                                        }
                                }

                                // --- Sections ---
                                // We delegate to renderSection provided by
                                // SectionRenderingComposables
                                // using the pre-calculated context.
                                ItemPriorityConfig.ItemType.APPS_SECTION -> {
                                        if (!hideResults) {
                                                renderSection(
                                                        SearchSection.APPS,
                                                        sectionParams,
                                                        sectionContext
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION -> {
                                        if (!hideResults) {
                                                renderSection(
                                                        SearchSection.APP_SHORTCUTS,
                                                        sectionParams,
                                                        sectionContext
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.FILES_SECTION -> {
                                        if (!hideResults) {
                                                renderSection(
                                                        SearchSection.FILES,
                                                        sectionParams,
                                                        sectionContext
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.CONTACTS_SECTION -> {
                                        if (!hideResults) {
                                                renderSection(
                                                        SearchSection.CONTACTS,
                                                        sectionParams,
                                                        sectionContext
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.SETTINGS_SECTION -> {
                                        if (!hideResults) {
                                                renderSection(
                                                        SearchSection.SETTINGS,
                                                        sectionParams,
                                                        sectionContext
                                                )
                                        }
                                }

                                // --- Suggestions & Engines ---
                                ItemPriorityConfig.ItemType.WEB_SUGGESTIONS -> {
                                        if (!hideResults && hasQuery) {
                                                // Only show if logic approves
                                                AnimatedVisibility(
                                                        visible = showWebSuggestions,
                                                        enter = fadeIn(),
                                                        exit = shrinkVertically()
                                                ) {
                                                        WebSuggestionsSection(
                                                                suggestions = state.webSuggestions,
                                                                onSuggestionClick =
                                                                        onWebSuggestionClick,
                                                                showWallpaperBackground =
                                                                        state.showWallpaperBackground,
                                                                reverseOrder = isReversed,
                                                                isShortcutDetected =
                                                                        state.detectedShortcutTarget !=
                                                                                null,
                                                                isRecentQuery = false,
                                                                modifier = Modifier.fillMaxWidth()
                                                        )
                                                }
                                        }
                                }
                                ItemPriorityConfig.ItemType.RECENT_QUERIES -> {
                                        if (!hideResults && showRecentQueries) {
                                                AnimatedVisibility(
                                                        visible = true, // showRecentQueries is
                                                        // already checked
                                                        enter = fadeIn(),
                                                        exit = shrinkVertically()
                                                ) {
                                                        WebSuggestionsSection(
                                                                suggestions = state.recentQueries,
                                                                onSuggestionClick =
                                                                        onWebSuggestionClick,
                                                                showWallpaperBackground =
                                                                        state.showWallpaperBackground,
                                                                reverseOrder = isReversed,
                                                                isShortcutDetected = false,
                                                                isRecentQuery = true,
                                                                onDeleteRecentQuery =
                                                                        onDeleteRecentQuery,
                                                                modifier = Modifier.fillMaxWidth()
                                                        )
                                                }
                                        }
                                }
                                ItemPriorityConfig.ItemType.SEARCH_ENGINES_INLINE -> {
                                        // Inline search engines.
                                        // Condition: Not compact mode.
                                        if (!hideResults &&
                                                        hasQuery &&
                                                        !state.isSearchEngineCompactMode
                                        ) {
                                                NoResultsSearchEngineCards(
                                                        query = state.query,
                                                        enabledEngines =
                                                                state.searchTargetsOrder.filter {
                                                                        it.getId() !in
                                                                                state.disabledSearchTargetIds
                                                                },
                                                        onSearchEngineClick = onSearchTargetClick,
                                                        onCustomizeClick =
                                                                onCustomizeSearchEnginesClick,
                                                        isReversed = isReversed,
                                                        showWallpaperBackground =
                                                                state.showWallpaperBackground,
                                                        modifier = Modifier.fillMaxWidth()
                                                )
                                        }
                                }
                                ItemPriorityConfig.ItemType.SEARCH_ENGINES_COMPACT -> {
                                        // If we ever need to render compact engines in the list, do
                                        // it here.
                                        // Currently checking isSearchEngineCompactMode to HIDE
                                        // inline ones.
                                        // If compact engines are intended to be in the list, add
                                        // logic here.
                                        // For now, config doesn't use this in
                                        // SEARCHING_STATE_LAYOUT, but
                                        // we handle it for completeness.
                                }
                                ItemPriorityConfig.ItemType.NO_RESULTS_MESSAGE -> {
                                        if (!hideResults) {
                                                NoResultsMessage(state)
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun NoResultsMessage(state: SearchUiState) {
        // Determine whether to show "No results" message when search engine
        // compact mode is enabled and web suggestions API was called but returned no results
        val shouldShowNoResults =
                state.isSearchEngineCompactMode &&
                        (!state.webSuggestionsEnabled ||
                                (state.webSuggestionsEnabled &&
                                        state.query.trim().length >= 2 &&
                                        state.webSuggestions.isEmpty()))

        // Add delay before showing no results text to avoid flashing before
        // web suggestions load
        var showNoResultsText by remember { mutableStateOf(false) }
        LaunchedEffect(shouldShowNoResults, state.query) {
                if (shouldShowNoResults) {
                        delay(500L) // Wait for web suggestions load
                        showNoResultsText = true
                } else {
                        showNoResultsText = false
                }
        }

        if (showNoResultsText) {
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)
                ) {
                        Text(
                                text = stringResource(R.string.no_results_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier =
                                        Modifier.padding(
                                                top = DesignTokens.SpacingSmall,
                                                start = DesignTokens.SpacingLarge,
                                                end = DesignTokens.SpacingLarge
                                        )
                        )
                }
        }
}
