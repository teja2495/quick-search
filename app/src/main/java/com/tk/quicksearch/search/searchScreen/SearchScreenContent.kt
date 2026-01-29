package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.calculator.CalculatorUtils
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry
import com.tk.quicksearch.search.searchEngines.getId
import com.tk.quicksearch.search.searchEngines.inline.SearchEngineIconsSection
import com.tk.quicksearch.ui.theme.DesignTokens
import kotlinx.coroutines.delay

@Composable
internal fun SearchScreenContent(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    settingsParams: SettingsSectionParams,
    appsParams: AppsSectionParams,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
    onRequestUsagePermission: () -> Unit,
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    onDirectSearchEmailClick: (String) -> Unit,
    onPhoneNumberClick: (String) -> Unit,
    onWebSuggestionClick: (String) -> Unit,
    onOpenPersonalContextDialog: () -> Unit,
    onPersonalContextHintDismissed: () -> Unit,
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onKeyboardSwitchToggle: () -> Unit,
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    expandedSection: ExpandedSection,
    manuallySwitchedToNumberKeyboard: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onClearDetectedShortcut: () -> Unit,
    modifier: Modifier = Modifier,
    isOverlayPresentation: Boolean = false,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Calculate enabled engines
    val enabledTargets: List<SearchTarget> =
        remember(state.searchTargetsOrder, state.disabledSearchTargetIds) {
            state.searchTargetsOrder.filter { it.getId() !in state.disabledSearchTargetIds }
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
                val canScrollForward =
                    layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
                        lastItem.index < layoutInfo.totalItemsCount - 1 ||
                            lastItem.offset + lastItem.size > layoutInfo.viewportEndOffset
                    }
                        ?: false

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

                searchEngineScrollState.scrollToItem(index = currentIndex, scrollOffset = newOffset)
            }
        } else {
            // When onboarding is dismissed, scroll back to start
            searchEngineScrollState.animateScrollToItem(index = 0, scrollOffset = 0)
        }
    }

    // Check for math expressions to determine pill visibility
    val hasMathExpression = CalculatorUtils.isMathExpression(state.query)

    val contentModifier =
        if (isOverlayPresentation) {
            modifier
                .fillMaxWidth()
                .padding(
                    start = DesignTokens.SpacingXLarge,
                    top = DesignTokens.SpacingLarge,
                    end = DesignTokens.SpacingXLarge,
                )
        } else {
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    start = DesignTokens.SpacingXLarge,
                    top = DesignTokens.SpacingLarge,
                    end = DesignTokens.SpacingXLarge,
                )
        }

    val searchEnginesModifier =
        if (isOverlayPresentation) {
            Modifier
        } else {
            Modifier.imePadding()
        }

    Column(modifier = contentModifier, verticalArrangement = Arrangement.Top) {
        // Fixed search bar at the top
        PersistentSearchField(
            query = state.query,
            onQueryChange = onQueryChanged,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            enabledTargets = enabledTargets,
            shouldUseNumberKeyboard = manuallySwitchedToNumberKeyboard,
            detectedShortcutTarget = state.detectedShortcutTarget,
            showWelcomeAnimation = state.showSearchBarWelcomeAnimation,
            onClearDetectedShortcut = onClearDetectedShortcut,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            modifier =
                Modifier.padding(
                    bottom =
                        if (state.oneHandedMode) {
                            DesignTokens.SpacingMedium
                        } else {
                            DesignTokens.SpacingXSmall
                        },
                ),
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
                    if (state.detectedShortcutTarget != null) {
                        // Remove the shortcut (first word) from the query
                        val queryWithoutShortcut =
                            trimmedQuery.split("\\s+".toRegex()).drop(1).joinToString(" ")
                        onSearchTargetClick(queryWithoutShortcut, state.detectedShortcutTarget)
                    } else {
                        val primaryTarget = enabledTargets.firstOrNull()
                        if (primaryTarget != null && trimmedQuery.isNotBlank()) {
                            onSearchTargetClick(trimmedQuery, primaryTarget)
                        }
                    }
                }
            },
        )

        // Add spacing between search bar and apps list when bottom aligned setting is off
        if (!state.oneHandedMode) {
            Spacer(modifier = Modifier.padding(top = DesignTokens.SpacingSmall))
        }

        // Scrollable content between search bar and search engines
        SearchContentArea(
            modifier =
                if (isOverlayPresentation) {
                    // In overlay mode, keep the compact search engine bar pinned to the
                    // bottom by letting the content area fill the remaining space (similar
                    // to the regular home screen layout).
                    val shouldFillRemainingSpace =
                        state.isSearchEngineCompactMode &&
                            expandedSection == ExpandedSection.NONE
                    Modifier.fillMaxWidth().weight(1f, fill = shouldFillRemainingSpace)
                } else {
                    Modifier.weight(1f)
                },
            state = state,
            renderingState = renderingState,
            contactsParams = contactsParams,
            filesParams = filesParams,
            appShortcutsParams = appShortcutsParams,
            settingsParams = settingsParams,
            appsParams = appsParams,
            onRequestUsagePermission = onRequestUsagePermission,
            scrollState = scrollState,
            onPhoneNumberClick = onPhoneNumberClick,
            onEmailClick = onDirectSearchEmailClick,
            onOpenPersonalContextDialog = onOpenPersonalContextDialog,
            onPersonalContextHintDismissed = onPersonalContextHintDismissed,
            onWebSuggestionClick = onWebSuggestionClick,
            onSearchTargetClick = onSearchTargetClick,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onDeleteRecentItem = onDeleteRecentItem,
            showCalculator = state.calculatorState.result != null,
            showDirectSearch = state.DirectSearchState.status != DirectSearchStatus.Idle,
            directSearchState = state.DirectSearchState,
            isOverlayPresentation = isOverlayPresentation,
        )

        // Keyboard switch pill - appears above search engines
        if (expandedSection == ExpandedSection.NONE) {
            val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

            if (!isKeyboardVisible) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KeyboardSwitchPill(
                        text = stringResource(R.string.action_open_keyboard),
                        onClick = { keyboardController?.show() },
                        modifier =
                            Modifier.padding(
                                top = DesignTokens.SpacingMedium,
                                bottom = DesignTokens.SpacingMedium,
                            ),
                    )
                }
            } else {
                val pillText =
                    if (manuallySwitchedToNumberKeyboard) {
                        stringResource(R.string.keyboard_switch_back)
                    } else if (state.query.isNotEmpty() &&
                        state.query.none { it.isLetter() } &&
                        state.detectedShortcutTarget == null
                    ) {
                        stringResource(R.string.keyboard_switch_to_number)
                    } else {
                        null
                    }

                pillText?.let {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KeyboardSwitchPill(
                            text = it,
                            onClick = onKeyboardSwitchToggle,
                            modifier =
                                Modifier.padding(
                                    top = DesignTokens.SpacingMedium,
                                    bottom = DesignTokens.SpacingMedium,
                                ),
                        )
                    }
                }
            }
        }

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, when search engine section is disabled,
        // or when a shortcut is detected
        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded
        if (expandedSection == ExpandedSection.NONE) {
            SearchEnginesVisibility(
                enginesState = state.searchEnginesState,
                modifier = searchEnginesModifier,
                compactContent = {
                    SearchEngineIconsSection(
                        query = state.query,
                        hasAppResults = renderingState.hasAppResults,
                        enabledEngines = enabledTargets,
                        onSearchEngineClick = onSearchTargetClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        externalScrollState = searchEngineScrollState,
                        detectedShortcutTarget = state.detectedShortcutTarget,
                        onClearDetectedShortcut = onClearDetectedShortcut,
                        showWallpaperBackground = state.showWallpaperBackground,
                        isOverlayPresentation = isOverlayPresentation,
                    )
                },
                fullContent = {
                    SearchEngineIconsSection(
                        query = state.query,
                        hasAppResults = renderingState.hasAppResults,
                        enabledEngines = enabledTargets,
                        onSearchEngineClick = onSearchTargetClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        externalScrollState = searchEngineScrollState,
                        detectedShortcutTarget = state.detectedShortcutTarget,
                        onClearDetectedShortcut = onClearDetectedShortcut,
                        showWallpaperBackground = state.showWallpaperBackground,
                        isOverlayPresentation = isOverlayPresentation,
                    )
                },
                shortcutContent = { target ->
                    SearchEngineIconsSection(
                        query = state.query,
                        hasAppResults = renderingState.hasAppResults,
                        enabledEngines = enabledTargets,
                        onSearchEngineClick = onSearchTargetClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        externalScrollState = searchEngineScrollState,
                        detectedShortcutTarget = target,
                        onClearDetectedShortcut = onClearDetectedShortcut,
                        showWallpaperBackground = state.showWallpaperBackground,
                        isOverlayPresentation = isOverlayPresentation,
                    )
                },
                hiddenContent = {
                    // Add padding when search engines are hidden to prevent keyboard from
                    // covering content
                    Spacer(modifier = searchEnginesModifier)
                },
            )
        }
    }
}
