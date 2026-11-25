package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.searchScreen.rememberImeSettledHeight
import com.tk.quicksearch.search.searchScreen.rememberImeTargetVisible
import com.tk.quicksearch.search.searchScreen.extendAboveImeSlide
import com.tk.quicksearch.search.searchScreen.SearchTopMatchesState
import com.tk.quicksearch.search.searchScreen.LocalInsetCardBackdrop
import com.tk.quicksearch.search.searchScreen.contentBottomInset
import com.tk.quicksearch.search.searchScreen.contentTopInset
import com.tk.quicksearch.search.searchScreen.resultsClipShape
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.derivedStateOf
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.shared.util.isLikelyWebUrl
import com.tk.quicksearch.search.bookmarks.LocalBookmarksSectionParams
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.ui.theme.overlayThemeTints
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.components.CollapseButton
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlin.math.min
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.widgetsPanel.HomeAddWidgetSheet

/** Renders the scrollable content area with sections based on layout mode. */
@Composable
internal fun SearchContentArea(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    remindersParams: RemindersSectionParams? = null,
    appsParams: AppsSectionParams,
    predictedTarget: PredictedSubmitTarget? = null,
    isPhysicalKeyboardConnected: Boolean,
    onRequestUsagePermission: () -> Unit,
    onOtherSearchItemAction: OtherSearchItemActionHandler,
    scrollState: androidx.compose.foundation.ScrollState,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenAiSearchConfigure: () -> Unit = {},
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onClearRecentItems: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
    onSearchHistoryExpandedChange: (Boolean) -> Unit = {},
    searchHistoryCollapseRequestKey: Int = 0,
    isSearchHistoryExpanded: Boolean = false,
    clearRevealKey: Int = 0,
    onSearchHistoryCollapseRequested: () -> Unit = {},
    showCalculator: Boolean = false,
    showCurrencyConverter: Boolean = false,
    showWorldClock: Boolean = false,
    showDictionary: Boolean = false,
    showWeather: Boolean = false,
    showAiSearch: Boolean = false,
    aiSearchState: AiSearchState? = null,
    isOverlayPresentation: Boolean = false,
    onOpenPermissionsSettings: () -> Unit = {},
    onHomePinnedSectionOrderChange: (List<SearchSection>) -> Unit = {},
    onChangeWallpaperClick: () -> Unit = {},
    onOpenGesturesSettingsClick: () -> Unit = {},
    onOpenSettingsClick: () -> Unit = {},
    showOpenSettingsOption: Boolean = true,
    onBottomOneHandedOverscrollUp: () -> Unit = {},
    onLauncherOverscrollUp: () -> Unit = {},
    onLauncherOverscrollDown: () -> Unit = {},
    onHomeDoubleTap: () -> Unit = {},
    selectedTopMatchIndex: Int? = null,
    topMatches: SearchTopMatchesState,
    /** True while the bottom chrome animates its height; card heights hold until it settles. */
    isBottomChromeAnimating: () -> Boolean = { false },
    /** True when the screen slides with the keyboard on a layer (see safeDrawingPaddingWithImeSlide). */
    isImeSliding: Boolean = false,
) {
    val currentOnHomeDoubleTap by rememberUpdatedState(onHomeDoubleTap)
    val useOneHandedMode =
        state.oneHandedMode &&
            renderingState.expandedSection == ExpandedSection.NONE &&
            !isSearchHistoryExpanded
    val hideOtherResults =
                showAiSearch ||
                showCalculator ||
                showCurrencyConverter ||
                showWorldClock ||
                showDictionary ||
                showWeather ||
                (state.detectedShortcutTarget != null) ||
                (state.detectedAliasSearchSection != null) ||
                state.isCurrencyConverterAliasMode ||
                state.isWorldClockAliasMode ||
                state.isDictionaryAliasMode ||
                state.isWeatherAliasMode ||
                state.detectedCustomToolId != null
                || state.detectedTaskerIntentId != null
                || com.tk.quicksearch.tools.aiSearch.QuickSearchHelp.questionOrNull(state.query) != null
    val hasQuery = state.query.isNotBlank()
    val imeInsets = WindowInsets.ime
    val imeDensity = LocalDensity.current
    val isImeVisible by remember(imeInsets, imeDensity) {
        derivedStateOf { imeInsets.getBottom(imeDensity) > 0 }
    }
    // Swap back as soon as the keyboard starts closing, mirroring how opening swaps on its first
    // frame; waiting for the inset to reach zero left the grid alone on screen before Home faded in.
    val isImeTargetVisible by rememberImeTargetVisible()
    // With Open Keyboard off, focusing the bar means the user is about to type: clear Home
    // and wait for the query instead of showing suggestions behind the keyboard.
    val hideHomeWhileAwaitingQuery =
        !state.openKeyboardOnLaunch &&
            isImeVisible &&
            isImeTargetVisible &&
            !hasQuery &&
            !hideOtherResults &&
            renderingState.expandedSection == ExpandedSection.NONE &&
            !isSearchHistoryExpanded
    // Clearing the query hands Home its empty-query content (app suggestions); fading that out
    // would flash it, so hide instantly when the hide comes from the query going empty.
    // Latched for the whole hide so the exit spec can't switch back to a fade mid-animation.
    var hadQueryLastFrame by remember { mutableStateOf(hasQuery) }
    var homeHiddenByQueryClear by remember { mutableStateOf(false) }
    val homeHideFromQueryClear =
        hideHomeWhileAwaitingQuery && (homeHiddenByQueryClear || (hadQueryLastFrame && !hasQuery))
    SideEffect {
        hadQueryLastFrame = hasQuery
        homeHiddenByQueryClear = homeHideFromQueryClear
    }
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
    val alignResultsToBottom =
            useOneHandedMode &&
                    !showAiSearch &&
                    !showCalculator &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary
                    && !showWeather
    val expandedSectionBottomInset = 80.dp
    val aliasExpandedSectionBottomInset = 12.dp
    // Result cards need breathing room so they do not touch the bar/engine strip.
    val resultCardBottomGap =
        if (alignResultsToBottom && hasQuery) DesignTokens.SpacingMedium - DesignTokens.SpacingXSmall else 0.dp
    val expandedCardExtraReduction = 20.dp
    // Compute "no results" state once - shared by both places that need it.
    val shouldShowNoResults = computeShouldShowNoResults(state)

    val hasInlineSearchEngines = hasQuery && (!state.isSearchEngineCompactMode || isUrlQuery)

    val showRetryButton =
        showAiSearch &&
                aiSearchState?.status == AiSearchStatus.Error &&
                !aiSearchState.activeQuery.isNullOrBlank() && aiSearchState.canRetry &&
                renderingState.expandedSection == ExpandedSection.NONE
    val isSectionAliasMode = state.detectedAliasSearchSection != null
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val edgeFadeHeight = if (isDarkMode) 32.dp else 16.dp
    val edgeFadeMinAlpha = if (isDarkMode) 0f else 0.4f
    val overlayThemeTints = overlayThemeTints(state, isDarkMode)
    val overlayCardColor = overlayThemeTints.cardColor
    val overlayDividerTint = overlayThemeTints.dividerTint
    val overlayActionTint = overlayThemeTints.actionTint
    // Derived: the scroll range changes on every frame of the keyboard slide; only its sign matters here.
    val hasScrollRange by remember(scrollState) { derivedStateOf { scrollState.maxValue > 0 } }
    val keyboardGesturesEnabled = !hasScrollRange
    val launcherOverscrollUpEnabled =
        keyboardGesturesEnabled &&
            hasQuery.not() &&
            renderingState.expandedSection == ExpandedSection.NONE
    // A keyboard-open search can already contain text. Keep the downward
    // overscroll active in that state so the configured close-keyboard action
    // still receives the gesture before the notification panel does.
    val launcherOverscrollDownEnabled = keyboardGesturesEnabled
    val bottomOneHandedOverscrollEnabled =
        state.bottomSearchBarEnabled && useOneHandedMode && alignResultsToBottom
    val bottomOneHandedOverscrollConnection = rememberSearchContentOverscrollConnection(
        bottomOneHandedOverscrollEnabled = bottomOneHandedOverscrollEnabled,
        launcherOverscrollUpEnabled = launcherOverscrollUpEnabled,
        launcherOverscrollDownEnabled = launcherOverscrollDownEnabled,
        onBottomOneHandedOverscrollUp = onBottomOneHandedOverscrollUp,
        onLauncherOverscrollUp = onLauncherOverscrollUp,
        onLauncherOverscrollDown = onLauncherOverscrollDown,
    )
    var searchHistorySelectedTab by rememberSaveable { mutableStateOf(SearchHistoryTab.SEARCHES) }
    val canSwitchSearchHistoryTabs =
        state.recentItems.any { it is RecentSearchItem.Query } &&
            state.recentItems.any { it !is RecentSearchItem.Query }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var showHomeWidgetPicker by rememberSaveable { mutableStateOf(false) }
    var backgroundMenuOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val edgeBleed = compactEdgeBleed(isOverlayPresentation)
    val backgroundMenuDpOffset =
        remember(backgroundMenuOffset, density, edgeBleed) {
            with(density) {
                DpOffset(
                    x = backgroundMenuOffset.x.toDp() - edgeBleed,
                    y = backgroundMenuOffset.y.toDp(),
                )
            }
        }
    val canChangeImageBackground = state.showWallpaperBackground
    CompositionLocalProvider(
        LocalOverlayResultCardColor provides overlayCardColor,
        LocalOverlayDividerColor provides overlayDividerTint,
        LocalOverlayActionColor provides overlayActionTint,
    ) {
        BoxWithConstraints(
            modifier =
                modifier
                    .fillMaxWidth()
                    // Overlay content may be shorter than its weighted slot. Keep the
                    // overscroll listener on that slot so the visible blank area above
                    // Open Keyboard receives the same Home gestures as regular mode.
                    .nestedScroll(bottomOneHandedOverscrollConnection)
                    // A short overlay has no scrollable child in its unused weighted space,
                    // so nested scroll alone never observes a drag that starts there.
                    .then(
                        if (isOverlayPresentation && keyboardGesturesEnabled) {
                            Modifier.pointerInput(
                                bottomOneHandedOverscrollEnabled,
                                launcherOverscrollUpEnabled,
                                launcherOverscrollDownEnabled,
                            ) {
                                var accumulatedDragY = 0f
                                var gestureHandled = false
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        if (gestureHandled) return@detectVerticalDragGestures
                                        accumulatedDragY += dragAmount
                                        when {
                                            accumulatedDragY < -OVERSCROLL_FOCUS_THRESHOLD_PX -> {
                                                if (bottomOneHandedOverscrollEnabled) {
                                                    onBottomOneHandedOverscrollUp()
                                                }
                                                if (launcherOverscrollUpEnabled) {
                                                    onLauncherOverscrollUp()
                                                }
                                                gestureHandled = true
                                            }
                                            accumulatedDragY > OVERSCROLL_FOCUS_THRESHOLD_PX -> {
                                                if (launcherOverscrollDownEnabled) {
                                                    onLauncherOverscrollDown()
                                                }
                                                gestureHandled = true
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        accumulatedDragY = 0f
                                        gestureHandled = false
                                    },
                                    onDragCancel = {
                                        accumulatedDragY = 0f
                                        gestureHandled = false
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    )
                    .searchHistoryExpandedTabSwipe(
                        enabled = isSearchHistoryExpanded,
                        canSwitchTabs = canSwitchSearchHistoryTabs,
                        selectedTab = searchHistorySelectedTab,
                        onTabSelected = { searchHistorySelectedTab = it },
                    ),
        ) {
            // Card heights follow the area's settled height, so the keyboard sliding in or out (or
            // the bottom chrome resizing with it) does not recompose the results on every frame.
            val settledMaxHeight = rememberImeSettledHeight(maxHeight, holdWhile = isBottomChromeAnimating)
            val bottomOverlayInset = LocalInsetCardBackdrop.current?.contentBottomInset() ?: 0.dp
            val topOverlayInset = LocalInsetCardBackdrop.current?.contentTopInset() ?: 0.dp
            val footerBottomPadding = 28.dp + bottomOverlayInset
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
                        !showWorldClock &&
                        !showDictionary &&
                        !showWeather &&
                        !showAiSearch &&
                        !com.tk.quicksearch.search.other.OtherSearchItemRegistry.hasVisibleResult(
                            query = state.query,
                            pinnedItemOrder = state.pinnedNonAppItemOrder,
                            screenTimeState = state.screenTimeState,
                        ) &&
                        !hasInlineSearchEngines

            val heightModifier =
                if (isOverlayPresentation) {
                    val shouldFillOverlayHeight =
                        alignResultsToBottom ||
                                renderingState.expandedSection != ExpandedSection.NONE ||
                                isSearchHistoryExpanded ||
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
                !shouldHideScrollView && hasScrollRange
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

            val expandRevealModifier =
                rememberExpandRevealModifier(renderingState.expandedSection, isSearchHistoryExpanded)
                    .then(rememberClearRevealModifier(clearRevealKey))
            val roundExpandedViewport = LocalSearchCardLayoutEnabled.current &&
                (renderingState.expandedSection != ExpandedSection.NONE || isSearchHistoryExpanded)
            Box {
                Column(
                    modifier =
                        Modifier
                            .extendAboveImeSlide(isImeSliding)
                            .edgeBleed(edgeBleed).fillMaxWidth()
                            .then(heightModifier)
                            .clip(LocalInsetCardBackdrop.current.resultsClipShape(TopRoundedShape, edgeBleed))
                            .then(
                                if (roundExpandedViewport) {
                                    Modifier.clip(ExpandedResultsViewportShape(edgeBleed, LocalInsetCardBackdrop.current?.topTarget == null))
                                } else {
                                    Modifier
                                },
                            )
                            .then(edgeFadeModifier)
                            .then(
                                if (!hasQuery) {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { currentOnHomeDoubleTap() },
                                            onLongPress = { pressOffset ->
                                                if (canChangeImageBackground) {
                                                    backgroundMenuOffset = pressOffset
                                                    showBackgroundMenu = true
                                                }
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (shouldHideScrollView) {
                                    Modifier.padding(
                                        top = topOverlayInset, bottom = DesignTokens.SpacingMedium + bottomOverlayInset,
                                    )
                                } else {
                                    Modifier
                                        .verticalScroll(
                                            scrollState,
                                            reverseScrolling =
                                            alignResultsToBottom,
                                        ).padding(top = topOverlayInset, bottom = bottomOverlayInset).padding(
                                            bottom =
                                                if (
                                                    renderingState.expandedSection != ExpandedSection.NONE ||
                                                    isSearchHistoryExpanded
                                                ) {
                                                    if (isSectionAliasMode) {
                                                        aliasExpandedSectionBottomInset
                                                    } else {
                                                        expandedSectionBottomInset
                                                    }
                                                } else if (alignResultsToBottom) {
                                                    // The app grid reads fine tucked against the search
                                                    // bar; ContentLayout adds resultCardBottomGap under a card.
                                                    DesignTokens.SpacingXSmall
                                                } else {
                                                    DesignTokens
                                                        .SpacingMedium
                                                },
                                        )
                                },
                            ).padding(horizontal = edgeBleed),
                    verticalArrangement =
                        if (shouldHideScrollView) Arrangement.Top else verticalArrangement,
                ) {
                    if (shouldHideScrollView) {
                        // Show only the shared no-results message layout without the scroll view.
                        NoResultsMessage(state)
                    } else {
                        // Overlaid so Home and the recents grid cross-fade in place.
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (alignResultsToBottom) Alignment.BottomCenter else Alignment.TopCenter,
                        ) {
                        // Composed ahead of the keyboard: building the grid on its first frame dropped frames.
                        if (!state.openKeyboardOnLaunch && !hasQuery && shouldShowAwaitingQueryRecentApps(appsParams)) {
                            AwaitingSwapSlot(visible = hideHomeWhileAwaitingQuery, enterOffset = DesignTokens.SpacingLarge) {
                                AwaitingQueryRecentApps(appsParams = appsParams)
                            }
                        }
                        // Home stays composed while hidden, so the keyboard closing doesn't rebuild it either.
                        // A query snaps it in, as the recents grid leaves with the first character.
                        AwaitingSwapSlot(
                            visible = !hideHomeWhileAwaitingQuery,
                            enterOffset = -DesignTokens.SpacingLarge,
                            snap = homeHideFromQueryClear || hasQuery,
                        ) {
                        key(state.backgroundSource, state.showWallpaperBackground) {
                            ContentLayout(
                                modifier = Modifier.fillMaxWidth().then(expandRevealModifier),
                                state = state,
                                renderingState = renderingState,
                                contactsParams = contactsParams,
                                filesParams = filesParams,
                                appShortcutsParams = appShortcutsParams,
                                settingsParams = settingsParams,
                                calendarParams = calendarParams,
                                notesParams = notesParams,
                                remindersParams = remindersParams,
                                appsParams = appsParams,
                                predictedTarget = predictedTarget,
                                isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
                                onRequestUsagePermission = onRequestUsagePermission,
                                onOtherSearchItemAction = onOtherSearchItemAction,
                                minContentHeight =
                                    if (isOverlayPresentation) {
                                        0.dp
                                    } else {
                                        settledMaxHeight - bottomOverlayInset - topOverlayInset
                                    },
                                expandedCardMaxHeight =
                                    (
                                        settledMaxHeight - topOverlayInset - bottomOverlayInset -
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
                                                !showAiSearch &&
                                                !showCurrencyConverter &&
                                                !showWorldClock &&
                                                !showDictionary &&
                                                !showWeather,
                                resultCardBottomGap = resultCardBottomGap,
                                hideResults = hideOtherResults,
                                showCalculator = showCalculator,
                                showCurrencyConverter = showCurrencyConverter,
                                showWorldClock = showWorldClock,
                                showDictionary = showDictionary,
                                showWeather = showWeather,
                                showAiSearch = showAiSearch,
                                aiSearchState = aiSearchState,
                                isOverlayPresentation = isOverlayPresentation,
                                onPhoneNumberClick = onPhoneNumberClick,
                                onEmailClick = onEmailClick,
                                onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                                onWebSuggestionClick = onWebSuggestionClick,
                                onRecentQueryClick = onRecentQueryClick,
                                onSearchEngineLongPress = onSearchEngineLongPress,
                                onCustomizeSearchEnginesClick =
                                onCustomizeSearchEnginesClick,
                                onOpenAiSearchConfigure = onOpenAiSearchConfigure,
                                onSearchTargetClick = onSearchTargetClick,
                                onDeleteRecentItem = onDeleteRecentItem,
                                onClearRecentItems = onClearRecentItems,
                                onGeminiModelInfoClick = onGeminiModelInfoClick,
                                isSearchHistoryExpanded = isSearchHistoryExpanded,
                                onSearchHistoryExpandedChange = onSearchHistoryExpandedChange,
                                searchHistoryCollapseRequestKey = searchHistoryCollapseRequestKey,
                                searchHistorySelectedTab = searchHistorySelectedTab,
                                onSearchHistorySelectedTabChange = { searchHistorySelectedTab = it },
                                onOpenPermissionsSettings = onOpenPermissionsSettings,
                                onHomePinnedSectionOrderChange = onHomePinnedSectionOrderChange,
                                selectedTopMatchIndex = selectedTopMatchIndex,
                                topMatches = topMatches,
                                isScrollInProgress = { scrollState.isScrollInProgress },
                            )
                        }
                        }
                        }
                    }
                }
                AppDropdownMenu(
                    expanded = showBackgroundMenu,
                    onDismissRequest = { showBackgroundMenu = false },
                    offset = backgroundMenuDpOffset,
                ) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.action_change_wallpaper),
                        icon = MaterialSymbols.Image,
                        onClick = {
                            showBackgroundMenu = false
                            onChangeWallpaperClick()
                        },
                    )
                    AppDropdownMenuDivider()
                    AppDropdownMenuItem(
                        text = stringResource(R.string.settings_gestures_title),
                        icon = MaterialSymbols.Swipe,
                        onClick = {
                            showBackgroundMenu = false
                            onOpenGesturesSettingsClick()
                        },
                    )
                    if (showOpenSettingsOption) {
                        AppDropdownMenuDivider()
                        AppDropdownMenuItem(
                            text = stringResource(R.string.action_open_settings),
                            icon = MaterialSymbols.Settings,
                            onClick = {
                                showBackgroundMenu = false
                                onOpenSettingsClick()
                            },
                        )
                    }
                    if (!isOverlayPresentation) {
                        AppDropdownMenuDivider()
                        AppDropdownMenuItem(
                            text = stringResource(R.string.widgets_panel_title),
                            icon = MaterialSymbols.Widgets,
                            onClick = {
                                showBackgroundMenu = false
                                showHomeWidgetPicker = true
                            },
                        )
                    }
                }
                if (showHomeWidgetPicker) {
                    HomeAddWidgetSheet(onDismiss = { showHomeWidgetPicker = false })
                }
            }

            // Keep the last expanded target while the pill fades out, so a late tap still collapses it.
            val showCollapseButton =
                !isSectionAliasMode &&
                    (renderingState.expandedSection != ExpandedSection.NONE || isSearchHistoryExpanded)
            var collapseTargetSection by remember { mutableStateOf(renderingState.expandedSection) }
            if (showCollapseButton) collapseTargetSection = renderingState.expandedSection
            val bookmarksParams = LocalBookmarksSectionParams.current
            AnimatedCollapseButton(
                visible = showCollapseButton,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = footerBottomPadding),
            ) {
                CollapseButton(
                    showWallpaperBackground = state.showWallpaperBackground,
                    onClick = {
                        when (collapseTargetSection) {
                            ExpandedSection.NONE -> onSearchHistoryCollapseRequested()

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

                            ExpandedSection.NOTES -> {
                                notesParams.onExpandClick()
                            }

                            ExpandedSection.REMINDERS -> {
                                remindersParams?.onExpandClick?.invoke()
                            }

                            ExpandedSection.BOOKMARKS -> bookmarksParams?.onExpandClick?.invoke()
                        }
                    },
                )
            }

            if (showRetryButton) {
                RetryAiSearchButton(
                    onClick = {
                        val retryQuery = aiSearchState.activeQuery.trim()
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
