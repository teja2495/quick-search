package com.tk.quicksearch.search.searchScreen.searchScreenLayout

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.directSearch.CalculatorResult
import com.tk.quicksearch.tools.directSearch.DirectSearchResult
import kotlin.math.min

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
    calendarParams: CalendarSectionParams,
    appsParams: AppsSectionParams,
    predictedTarget: PredictedSubmitTarget? = null,
    onRequestUsagePermission: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onOpenSearchHistorySettings: () -> Unit = {},
    onDismissSearchHistoryTip: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
    onSearchHistoryExpandedChange: (Boolean) -> Unit = {},
    showCalculator: Boolean = false,
    showCurrencyConverter: Boolean = false,
    showWordClock: Boolean = false,
    showDictionary: Boolean = false,
    showDirectSearch: Boolean = false,
    directSearchState: DirectSearchState? = null,
    isOverlayPresentation: Boolean = false,
    onOpenPermissionsSettings: () -> Unit = {},
) {
    val useOneHandedMode =
        state.oneHandedMode && renderingState.expandedSection == ExpandedSection.NONE
    val hideOtherResults =
                showDirectSearch ||
                showCalculator ||
                showWordClock ||
                showDictionary ||
                (state.detectedShortcutTarget != null) ||
                (state.detectedAliasSearchSection != null) ||
                state.isCurrencyConverterAliasMode ||
                state.isWordClockAliasMode ||
                state.isDictionaryAliasMode
    val hasQuery = state.query.isNotBlank()
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
    val hasAnySearchContent =
        shouldShowAppsSection(renderingState) ||
                shouldShowAppShortcutsSection(renderingState) ||
                shouldShowContactsSection(renderingState, contactsParams) ||
                shouldShowFilesSection(renderingState, filesParams) ||
                shouldShowSettingsSection(renderingState) ||
                shouldShowCalendarSection(renderingState, calendarParams)
    val alignResultsToBottom =
            useOneHandedMode &&
                    !showDirectSearch &&
                    !showCalculator &&
                    !showCurrencyConverter &&
                    !showWordClock &&
                    !showDictionary
    val expandedSectionBottomInset = 80.dp
    val aliasExpandedSectionBottomInset = 12.dp
    val footerBottomPadding = 28.dp
    val expandedCardExtraReduction = 20.dp

    // Compute "no results" state once - shared by both places that need it
    val shouldShowNoResults =
        remember(
            state.query,
            state.webSuggestionsEnabled,
            state.webSuggestions,
            state.detectedShortcutTarget,
            state.detectedAliasSearchSection,
            state.isCurrencyConverterAliasMode,
            state.isWordClockAliasMode,
            state.isDictionaryAliasMode,
        ) {
            computeShouldShowNoResults(state)
        }

    val hasInlineSearchEngines = hasQuery && (!state.isSearchEngineCompactMode || isUrlQuery)

    val showRetryButton =
        showDirectSearch &&
                directSearchState?.status == DirectSearchStatus.Error &&
                !directSearchState.activeQuery.isNullOrBlank() &&
                renderingState.expandedSection == ExpandedSection.NONE
    val isSectionAliasMode = state.detectedAliasSearchSection != null
    val useOverlayThemeTints = state.backgroundSource == BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val edgeFadeHeight = if (isDarkMode) 32.dp else 16.dp
    val edgeFadeMinAlpha = if (isDarkMode) 0f else 0.4f
    val overlayCardColor =
        if (useOverlayThemeTints) {
            appThemeResultCardColor(
                theme = state.appTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
            )
        } else {
            null
        }
    val overlayDividerTint =
        if (useOverlayThemeTints) {
            appThemeDividerColor(
                theme = state.appTheme,
                isDarkMode = isDarkMode,
                intensity = state.overlayThemeIntensity,
            )
        } else {
            null
        }
    val overlayActionTint =
        if (useOverlayThemeTints) {
            appThemeActionColor(
                theme = state.appTheme,
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
                        !showCurrencyConverter &&
                        !showWordClock &&
                        !showDictionary &&
                        !showDirectSearch &&
                        !hasInlineSearchEngines

            val heightModifier =
                if (isOverlayPresentation) {
                    val shouldFillOverlayHeight =
                        alignResultsToBottom ||
                                renderingState.expandedSection != ExpandedSection.NONE ||
                                showRetryButton
                    if (shouldFillOverlayHeight) {
                        // Ensure overlay content occupies full available height so one-handed
                        // and footer actions can stay pinned at the bottom even with short content.
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
                                                        Color.Black.copy(alpha = topEdgeAlpha.coerceAtLeast(edgeFadeMinAlpha)),
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
                                                        Color.Black.copy(alpha = bottomEdgeAlpha.coerceAtLeast(edgeFadeMinAlpha)),
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
                                                if (isSectionAliasMode) {
                                                    aliasExpandedSectionBottomInset
                                                } else {
                                                    expandedSectionBottomInset
                                                }
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
                    // Show only the shared no-results message layout without the scroll view.
                    NoResultsMessage(state)
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
            calendarParams = calendarParams,
            appsParams = appsParams,
                            predictedTarget = predictedTarget,
                            onRequestUsagePermission = onRequestUsagePermission,
                            minContentHeight =
                                if (isOverlayPresentation) {
                                    0.dp
                                } else {
                                    this@BoxWithConstraints.maxHeight
                                },
                            expandedCardMaxHeight =
                                (
                                    this@BoxWithConstraints.maxHeight -
                                        if (isSectionAliasMode) {
                                            aliasExpandedSectionBottomInset
                                        } else {
                                            expandedSectionBottomInset +
                                                expandedCardExtraReduction
                                        }
                                )
                                    .coerceAtLeast(220.dp),
                            isReversed =
                                    useOneHandedMode &&
                                            !showDirectSearch &&
                                            !showCurrencyConverter &&
                                            !showWordClock &&
                                            !showDictionary,
                            hideResults = hideOtherResults,
                            showCalculator = showCalculator,
                            showCurrencyConverter = showCurrencyConverter,
                            showWordClock = showWordClock,
                            showDictionary = showDictionary,
                            showDirectSearch = showDirectSearch,
                            directSearchState = directSearchState,
                            isOverlayPresentation = isOverlayPresentation,
                            onPhoneNumberClick = onPhoneNumberClick,
                            onEmailClick = onEmailClick,
                            onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                            onWebSuggestionClick = onWebSuggestionClick,
                            onSearchEngineLongPress = onSearchEngineLongPress,
                            onCustomizeSearchEnginesClick =
                            onCustomizeSearchEnginesClick,
                            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
                            onSearchTargetClick = onSearchTargetClick,
                            onDeleteRecentItem = onDeleteRecentItem,
                            onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                            onDismissSearchHistoryTip = onDismissSearchHistoryTip,
                            onGeminiModelInfoClick = onGeminiModelInfoClick,
                            onSearchHistoryExpandedChange = onSearchHistoryExpandedChange,
                            onOpenPermissionsSettings = onOpenPermissionsSettings,
                        )
                    }
                }
            }

            if (renderingState.expandedSection != ExpandedSection.NONE && !isSectionAliasMode) {
                CollapseButton(
                    showWallpaperBackground = state.showWallpaperBackground,
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

                            ExpandedSection.APP_SETTINGS -> {
                                settingsParams.onAppSettingExpandClick()
                            }

                            ExpandedSection.CALENDAR -> {
                                calendarParams.onExpandClick()
                            }

                            else -> {}
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = footerBottomPadding),
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
                            .padding(bottom = footerBottomPadding),
                )
            }
        }
    }
}
