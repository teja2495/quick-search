package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.directSearch.CalculatorResult
import com.tk.quicksearch.search.directSearch.DirectSearchResult
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.recentSearches.RecentSearchEntry
import com.tk.quicksearch.search.recentSearches.RecentSearchesSection
import com.tk.quicksearch.search.searchEngines.*
import com.tk.quicksearch.search.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlin.math.min

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
    val shortcutDetected: Boolean = false,
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
    predictedTarget: PredictedSubmitTarget? = null,
    onRequestUsagePermission: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onPersonalContextHintDismissed: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onDisableSearchHistory: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
    showCalculator: Boolean = false,
    showDirectSearch: Boolean = false,
    directSearchState: DirectSearchState? = null,
    isOverlayPresentation: Boolean = false,
) {
    val useOneHandedMode =
        state.oneHandedMode && renderingState.expandedSection == ExpandedSection.NONE
    val hideOtherResults =
        showDirectSearch || showCalculator || (state.detectedShortcutTarget != null)
    val hasQuery = state.query.isNotBlank()
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
    val hasAnySearchContent =
        shouldShowAppsSection(renderingState) ||
            shouldShowAppShortcutsSection(renderingState) ||
            shouldShowContactsSection(renderingState, contactsParams) ||
            shouldShowFilesSection(renderingState, filesParams) ||
            shouldShowSettingsSection(renderingState)
    val alignResultsToBottom = useOneHandedMode && !showDirectSearch && !showCalculator
    val edgeFadeHeight = 32.dp

    // Compute "no results" state once - shared by both places that need it
    val shouldShowNoResults =
        remember(
            state.query,
            state.webSuggestionsEnabled,
            state.webSuggestions,
            state.detectedShortcutTarget,
        ) {
            val hasAnySearchResults = hasAnySearchResults(state)
            val trimmedQuery = state.query.trim()
            val queryLength = trimmedQuery.length
            trimmedQuery.isNotBlank() &&
                !hasAnySearchResults &&
                state.detectedShortcutTarget == null &&
                (
                    !state.webSuggestionsEnabled ||
                        (queryLength >= 2 && state.webSuggestions.isEmpty())
                )
        }

    val hasInlineSearchEngines = hasQuery && (!state.isSearchEngineCompactMode || isUrlQuery)

    val showRetryButton =
        showDirectSearch &&
            directSearchState?.status == DirectSearchStatus.Error &&
            !directSearchState.activeQuery.isNullOrBlank() &&
            renderingState.expandedSection == ExpandedSection.NONE
    val effectiveShowWallpaperBackground = state.showWallpaperBackground
    val useOverlayThemeTints = state.backgroundSource == BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val overlayCardColor =
        if (useOverlayThemeTints) {
            overlayResultCardColor(
                theme = state.overlayGradientTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
            )
        } else {
            null
        }
    val overlayDividerTint =
        if (useOverlayThemeTints) {
            overlayDividerColor(
                theme = state.overlayGradientTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
            )
        } else {
            null
        }
    val overlayActionTint =
        if (useOverlayThemeTints) {
            overlayActionColor(
                theme = state.overlayGradientTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
            )
        } else {
            null
        }

    CompositionLocalProvider(
        LocalOverlayResultCardColor provides overlayCardColor,
        LocalOverlayDividerColor provides overlayDividerTint,
        LocalOverlayActionColor provides overlayActionTint,
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Use bottom alignment when one-handed mode is enabled and no special states are
        // showing
        val verticalArrangement =
            if (alignResultsToBottom) {
                Arrangement.spacedBy(DesignTokens.SpacingMedium, Alignment.Bottom)
            } else {
                Arrangement.spacedBy(DesignTokens.SpacingMedium)
            }

        // Hide scroll view only when there's nothing else to render.
        val shouldHideScrollView =
            shouldShowNoResults &&
                !showCalculator &&
                !showDirectSearch &&
                !hasInlineSearchEngines

        val heightModifier =
            if (isOverlayPresentation) {
                if (alignResultsToBottom) {
                    // Ensure overlay content occupies full available height so one-handed
                    // bottom arrangement can position short content (e.g., app suggestions only)
                    // correctly after IME visibility changes.
                    Modifier.heightIn(min = maxHeight, max = maxHeight)
                } else {
                    Modifier.heightIn(min = 0.dp, max = maxHeight)
                }
            } else {
                Modifier.heightIn(min = maxHeight)
            }

        val needsEdgeFade =
            !shouldHideScrollView && scrollState.maxValue > 0
        val edgeFadeModifier =
            if (needsEdgeFade) {
                Modifier
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val fadePx =
                            min(
                                edgeFadeHeight.toPx(),
                                size.height / 2f,
                            )
                        val scrollValue = scrollState.value.toFloat()
                        val maxScroll = scrollState.maxValue.toFloat()

                        fun getFadeProgress(value: Float): Float {
                            val progress =
                                (value / fadePx).coerceIn(0f, 1f)
                            return progress * progress * (3 - 2 * progress)
                        }

                        val topFadeProgress =
                            if (maxScroll > 0) {
                                if (alignResultsToBottom) {
                                    getFadeProgress(maxScroll - scrollValue)
                                } else {
                                    getFadeProgress(scrollValue)
                                }
                            } else {
                                0f
                            }
                        val bottomFadeProgress =
                            if (maxScroll > 0) {
                                if (alignResultsToBottom) {
                                    getFadeProgress(scrollValue)
                                } else {
                                    getFadeProgress(maxScroll - scrollValue)
                                }
                            } else {
                                0f
                            }
                        val topEdgeAlpha = 1f - topFadeProgress
                        val bottomEdgeAlpha = 1f - bottomFadeProgress

                        if (fadePx > 0f) {
                            if (topEdgeAlpha < 1f) {
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Black.copy(alpha = topEdgeAlpha),
                                                    Color.Black,
                                                ),
                                            startY = 0f,
                                            endY = fadePx,
                                        ),
                                    size = Size(size.width, fadePx),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                            if (bottomEdgeAlpha < 1f) {
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Black,
                                                    Color.Black.copy(alpha = bottomEdgeAlpha),
                                                ),
                                            startY = size.height - fadePx,
                                            endY = size.height,
                                        ),
                                    topLeft = Offset(0f, size.height - fadePx),
                                    size = Size(size.width, fadePx),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                    }
            } else {
                Modifier
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(heightModifier)
                    .clip(TopRoundedShape)
                    .then(edgeFadeModifier)
                    .then(
                        if (shouldHideScrollView) {
                            Modifier.padding(
                                bottom = DesignTokens.SpacingMedium,
                            )
                        } else {
                            Modifier
                                .verticalScroll(
                                    scrollState,
                                    reverseScrolling =
                                    alignResultsToBottom,
                                ).padding(
                                    bottom =
                                        if (renderingState
                                                .expandedSection !=
                                            ExpandedSection
                                                .NONE
                                        ) {
                                            80.dp
                                        } else {
                                            DesignTokens
                                                .SpacingMedium
                                        },
                                )
                        },
                    ),
            verticalArrangement =
                if (shouldHideScrollView) Arrangement.Top else verticalArrangement,
        ) {
            if (shouldHideScrollView) {
                // Show only the no results text without scroll view
                // Add delay before showing no results text to avoid flashing before
                // web suggestions load (only when web suggestions are enabled)
                var showNoResultsText by remember { mutableStateOf(false) }
                LaunchedEffect(
                    shouldShowNoResults,
                    state.query,
                    state.webSuggestionsEnabled,
                ) {
                    if (shouldShowNoResults) {
                        // Only delay if web suggestions are enabled and
                        // might still be loading
                        if (state.webSuggestionsEnabled) {
                            delay(500L) // Wait for web suggestions load
                        }
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
                                alpha = 0.6f,
                            ),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    top =
                                        DesignTokens
                                            .SpacingSmall,
                                    start =
                                        DesignTokens
                                            .SpacingLarge,
                                    end =
                                        DesignTokens
                                            .SpacingLarge,
                                ),
                    )
                }
            } else {
                key(state.backgroundSource, state.showWallpaperBackground) {
                    ContentLayout(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        renderingState = renderingState,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        appShortcutsParams = appShortcutsParams,
                        settingsParams = settingsParams,
                        appsParams = appsParams,
                        predictedTarget = predictedTarget,
                        onRequestUsagePermission = onRequestUsagePermission,
                        minContentHeight =
                            if (isOverlayPresentation) {
                                0.dp
                            } else {
                                this@BoxWithConstraints.maxHeight
                            },
                        isReversed = useOneHandedMode && !showDirectSearch,
                        hideResults = hideOtherResults,
                        showCalculator = showCalculator,
                        showDirectSearch = showDirectSearch,
                        directSearchState = directSearchState,
                        isOverlayPresentation = isOverlayPresentation,
                        onPhoneNumberClick = onPhoneNumberClick,
                        onEmailClick = onEmailClick,
                        onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                        onPersonalContextHintDismissed =
                        onPersonalContextHintDismissed,
                        onWebSuggestionClick = onWebSuggestionClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        onCustomizeSearchEnginesClick =
                        onCustomizeSearchEnginesClick,
                        onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
                        onSearchTargetClick = onSearchTargetClick,
                        onDeleteRecentItem = onDeleteRecentItem,
                        onDisableSearchHistory = onDisableSearchHistory,
                        onGeminiModelInfoClick = onGeminiModelInfoClick,
                    )
                }
            }
        }

        if (renderingState.expandedSection != ExpandedSection.NONE) {
            com.tk.quicksearch.search.contacts.CollapseButton(
                onClick = {
                    when (renderingState.expandedSection) {
                        ExpandedSection.FILES -> {
                            filesParams.onExpandClick()
                        }

                        ExpandedSection.CONTACTS -> {
                            contactsParams.onExpandClick()
                        }

                        ExpandedSection.APP_SHORTCUTS -> {
                            appShortcutsParams.onExpandClick()
                        }

                        ExpandedSection.SETTINGS -> {
                            settingsParams.onExpandClick()
                        }

                        else -> {}
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
            )
        }

        if (showRetryButton) {
            RetryDirectSearchButton(
                onClick = {
                    val retryQuery =
                        directSearchState?.activeQuery?.trim().orEmpty()
                    if (retryQuery.isNotEmpty()) {
                        onSearchTargetClick(
                            retryQuery,
                            SearchTarget.Engine(
                                SearchEngine.DIRECT_SEARCH,
                            ),
                        )
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
            )
        }
        }
    }
}

@Composable
private fun RetryDirectSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.action_retry),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_retry),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
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
    predictedTarget: PredictedSubmitTarget? = null,
    onRequestUsagePermission: () -> Unit,
    minContentHeight: Dp,
    isReversed: Boolean,
    hideResults: Boolean,
    showCalculator: Boolean = false,
    showDirectSearch: Boolean = false,
    directSearchState: DirectSearchState? = null,
    isOverlayPresentation: Boolean = false,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onPersonalContextHintDismissed: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onDisableSearchHistory: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val effectiveContactsParams = contactsParams.copy(predictedTarget = predictedTarget)
    val effectiveFilesParams = filesParams.copy(predictedTarget = predictedTarget)
    val effectiveAppShortcutsParams = appShortcutsParams.copy(predictedTarget = predictedTarget)
    val effectiveSettingsParams = settingsParams.copy(predictedTarget = predictedTarget)
    val effectiveAppsParams = appsParams.copy(predictedTarget = predictedTarget)

    // 1. Determine Layout Order based on ItemPriorityConfig
    val hasQuery = state.query.isNotBlank()
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
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
            filesParams = effectiveFilesParams,
            contactsParams = effectiveContactsParams,
            settingsParams = effectiveSettingsParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            appsParams = effectiveAppsParams,
            isSearching = hasQuery,
            oneHandedMode =
                state.oneHandedMode, // This affects list reversal inside helpers
        )

    val sectionParams =
        SectionRenderParams(
            renderingState = renderingState,
            contactsParams = effectiveContactsParams,
            filesParams = effectiveFilesParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            settingsParams = effectiveSettingsParams,
            appsParams = effectiveAppsParams,
            isReversed = isReversed,
        )

    val effectiveShowWallpaperBackground = state.showWallpaperBackground
    val inlineTargets =
        remember(
            state.searchTargetsOrder,
            state.disabledSearchTargetIds,
            isUrlQuery,
            context,
        ) {
            if (isUrlQuery) {
                orderedBrowserTargets(
                    targets = state.searchTargetsOrder,
                    defaultBrowserPackage = resolveDefaultBrowserPackage(context),
                )
            } else {
                state.searchTargetsOrder.filter { it.getId() !in state.disabledSearchTargetIds }
            }
        }

    // Pre-calculate common states
    val isExpanded = renderingState.expandedSection != ExpandedSection.NONE
    val hasAnySearchResults = hasAnySearchResults(state)
    // Web Suggestions Logic
    val suggestionsNotEmpty = state.webSuggestions.isNotEmpty()
    val suggestionsEnabled = state.webSuggestionsEnabled
    val suggestionWasSelected = state.webSuggestionWasSelected

    val showWebSuggestions =
        hasQuery &&
            !isUrlQuery &&
            !showDirectSearch &&
            !showCalculator &&
            suggestionsNotEmpty &&
            suggestionsEnabled &&
            !suggestionWasSelected

    // Recent Queries Logic (for App Open State mainly, but CONFIG has RECENT_QUERIES item)
    val showRecentItems =
        !hasQuery && state.recentQueriesEnabled && state.recentItems.isNotEmpty()

    var recentSearchesExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(showRecentItems) {
        if (!showRecentItems) recentSearchesExpanded = false
    }

    val hidePinnedAndAppsWhenSearchHistoryExpanded =
        showRecentItems && recentSearchesExpanded

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        finalLayoutOrder.forEach { itemType ->
            // Check if this is a section item
            val isSectionItem =
                when (itemType) {
                    ItemPriorityConfig.ItemType.APPS_SECTION,
                    ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION,
                    ItemPriorityConfig.ItemType.FILES_SECTION,
                    ItemPriorityConfig.ItemType.CONTACTS_SECTION,
                    ItemPriorityConfig.ItemType.SETTINGS_SECTION,
                    -> true

                    else -> false
                }

            // If a section is expanded, we hide all OTHER non-section items.
            if (isExpanded && !isSectionItem) return@forEach

            when (itemType) {
                ItemPriorityConfig.ItemType.ERROR_BANNER -> {
                    if (state.screenState is ScreenVisibilityState.Error) {
                        InfoBanner(
                            message =
                                (
                                    state.screenState as
                                        ScreenVisibilityState.Error
                                ).message,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.CALCULATOR_RESULT -> {
                    if (showCalculator) {
                        CalculatorResult(
                            calculatorState = state.calculatorState,
                            showWallpaperBackground =
                                effectiveShowWallpaperBackground,
                            oneHandedMode = state.oneHandedMode,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.DIRECT_SEARCH_RESULT -> {
                    if (showDirectSearch && directSearchState != null) {
                        val shouldAllowPersonalContextHint =
                            state.showPersonalContextHint &&
                                state.personalContext.isBlank() &&
                                directSearchState.status ==
                                DirectSearchStatus
                                    .Success &&
                                !directSearchState.answer
                                    .isNullOrBlank()
                        var isPersonalContextHintVisible by
                            remember(directSearchState.activeQuery) {
                                mutableStateOf(false)
                            }

                        LaunchedEffect(shouldAllowPersonalContextHint) {
                            if (shouldAllowPersonalContextHint) {
                                delay(1000L)
                                isPersonalContextHintVisible = true
                            } else {
                                isPersonalContextHintVisible = false
                            }
                        }

                        Column {
                            AnimatedVisibility(
                                visible =
                                isPersonalContextHintVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Column {
                                    PersonalContextHintBanner(
                                        onOpenPersonalContext =
                                        onOpenPersonalContextDialog,
                                        onOpenDirectSearchConfigure =
                                        onOpenDirectSearchConfigure,
                                        onDismiss = {
                                            isPersonalContextHintVisible =
                                                false
                                            onPersonalContextHintDismissed()
                                        },
                                    )
                                    Spacer(
                                        modifier =
                                            Modifier.size(
                                                DesignTokens
                                                    .SpacingSmall,
                                            ),
                                    )
                                }
                            }
                            DirectSearchResult(
                                DirectSearchState =
                                directSearchState,
                                showWallpaperBackground =
                                    effectiveShowWallpaperBackground,
                                oneHandedMode = state.oneHandedMode,
                                onGeminiModelInfoClick = onGeminiModelInfoClick,
                                onOpenDirectSearchConfigure =
                                onOpenDirectSearchConfigure,
                                onPhoneNumberClick =
                                onPhoneNumberClick,
                                onEmailClick = onEmailClick,
                            )
                        }
                    }
                }

                // --- Sections ---
                // We delegate to renderSection provided by
                // SectionRenderingComposables
                // using the pre-calculated context.
                // When search history is expanded, hide app suggestions and pinned items.
                ItemPriorityConfig.ItemType.APPS_SECTION -> {
                    if (
                        !hideResults &&
                            !isUrlQuery &&
                            !hidePinnedAndAppsWhenSearchHistoryExpanded
                    ) {
                        renderSection(
                            SearchSection.APPS,
                            sectionParams,
                            sectionContext,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION -> {
                    if (!hideResults && !hidePinnedAndAppsWhenSearchHistoryExpanded) {
                        renderSection(
                            SearchSection.APP_SHORTCUTS,
                            sectionParams,
                            sectionContext,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.FILES_SECTION -> {
                    if (!hideResults && !hidePinnedAndAppsWhenSearchHistoryExpanded) {
                        renderSection(
                            SearchSection.FILES,
                            sectionParams,
                            sectionContext,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.CONTACTS_SECTION -> {
                    if (!hideResults && !hidePinnedAndAppsWhenSearchHistoryExpanded) {
                        renderSection(
                            SearchSection.CONTACTS,
                            sectionParams,
                            sectionContext,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.SETTINGS_SECTION -> {
                    if (!hideResults && !hidePinnedAndAppsWhenSearchHistoryExpanded) {
                        renderSection(
                            SearchSection.SETTINGS,
                            sectionParams,
                            sectionContext,
                        )
                    }
                }

                // --- Suggestions & Engines ---
                ItemPriorityConfig.ItemType.WEB_SUGGESTIONS -> {
                    val allowWebSuggestions =
                        !hideResults || state.detectedShortcutTarget != null
                    if (allowWebSuggestions && hasQuery) {
                        // Only show if logic approves
                        AnimatedVisibility(
                            visible = showWebSuggestions,
                            enter = fadeIn(),
                            exit = shrinkVertically(),
                        ) {
                            WebSuggestionsSection(
                                suggestions = state.webSuggestions,
                                onSuggestionClick =
                                onWebSuggestionClick,
                                showWallpaperBackground =
                                    effectiveShowWallpaperBackground,
                                reverseOrder = isReversed,
                                isShortcutDetected =
                                    state.detectedShortcutTarget !=
                                        null,
                                isRecentQuery = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                ItemPriorityConfig.ItemType.RECENT_QUERIES -> {
                    if (!hideResults && showRecentItems) {
                        val orderedRecentItems = state.recentItems
                        AnimatedVisibility(
                            visible = true, // showRecentItems is
                            // already checked
                            enter = fadeIn(),
                            exit = shrinkVertically(),
                        ) {
                            RecentSearchesSection(
                                items = orderedRecentItems,
                                callingApp =
                                    effectiveContactsParams.callingApp
                                        ?: CallingApp.CALL,
                                messagingApp =
                                    effectiveContactsParams.messagingApp
                                        ?: MessagingApp
                                            .MESSAGES,
                                onRecentQueryClick =
                                onWebSuggestionClick,
                                onContactClick =
                                    effectiveContactsParams
                                        .onContactClick,
                                onShowContactMethods =
                                    effectiveContactsParams
                                        .onShowContactMethods,
                                onCallContact =
                                    effectiveContactsParams
                                        .onCallContact,
                                onSmsContact =
                                    effectiveContactsParams.onSmsContact,
                                onContactMethodClick =
                                    effectiveContactsParams
                                        .onContactMethodClick,
                                getPrimaryContactCardAction =
                                    effectiveContactsParams
                                        .getPrimaryContactCardAction,
                                getSecondaryContactCardAction =
                                    effectiveContactsParams
                                        .getSecondaryContactCardAction,
                                onPrimaryActionLongPress =
                                    effectiveContactsParams
                                        .onPrimaryActionLongPress,
                                onSecondaryActionLongPress =
                                    effectiveContactsParams
                                        .onSecondaryActionLongPress,
                                onCustomAction =
                                    effectiveContactsParams
                                        .onCustomAction,
                                onFileClick =
                                    effectiveFilesParams.onFileClick,
                                onSettingClick =
                                    effectiveSettingsParams
                                        .onSettingClick,
                                onAppShortcutClick =
                                    effectiveAppShortcutsParams
                                        .onShortcutClick,
                                onDeleteRecentItem =
                                onDeleteRecentItem,
                                onDisableSearchHistory =
                                onDisableSearchHistory,
                                onExpandedChange = { recentSearchesExpanded = it },
                                showWallpaperBackground =
                                    effectiveShowWallpaperBackground,
                                isOverlayPresentation = isOverlayPresentation,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                ItemPriorityConfig.ItemType.SEARCH_ENGINES_INLINE -> {
                    // Inline search engines.
                    // Condition: Not compact mode.
                    if (!hideResults &&
                        hasQuery &&
                        (!state.isSearchEngineCompactMode || isUrlQuery)
                    ) {
                        NoResultsSearchEngineCards(
                            query = state.query,
                            enabledEngines = inlineTargets,
                            onSearchEngineClick = onSearchTargetClick,
                            onCustomizeClick =
                            onCustomizeSearchEnginesClick,
                            onSearchEngineLongPress =
                            onSearchEngineLongPress,
                            showCustomizeCard = false,
                            isReversed = isReversed,
                            showWallpaperBackground =
                                effectiveShowWallpaperBackground,
                            predictedTarget = predictedTarget,
                            modifier = Modifier.fillMaxWidth(),
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
    // Determine whether to show "No results" message when there's a query but no results and no
    // search engine shortcut is detected
    val shouldShowNoResults =
        remember(
            state.query,
            state.webSuggestionsEnabled,
            state.webSuggestions,
            state.detectedShortcutTarget,
        ) {
            val hasAnySearchResults = hasAnySearchResults(state)
            val trimmedQuery = state.query.trim()
            val queryLength = trimmedQuery.length
            trimmedQuery.isNotBlank() &&
                !hasAnySearchResults &&
                state.detectedShortcutTarget == null &&
                (
                    !state.webSuggestionsEnabled ||
                        (queryLength >= 2 && state.webSuggestions.isEmpty())
                )
        }

    // Add delay before showing no results text to avoid flashing before
    // web suggestions load (only when web suggestions are enabled)
    var showNoResultsText by remember { mutableStateOf(false) }
    LaunchedEffect(shouldShowNoResults, state.query, state.webSuggestionsEnabled) {
        if (shouldShowNoResults) {
            // Only delay if web suggestions are enabled and might still be loading
            if (state.webSuggestionsEnabled) {
                delay(500L) // Wait for web suggestions load
            }
            showNoResultsText = true
        } else {
            showNoResultsText = false
        }
    }

    if (showNoResultsText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
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
                        end = DesignTokens.SpacingLarge,
                    ),
            )
        }
    }
}
