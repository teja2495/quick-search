package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorPills
import com.tk.quicksearch.search.searchScreen.components.OpenKeyboardAction
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.ColumnScope
import com.tk.quicksearch.searchEngines.inline.SearchEngineSectionConstants
import com.tk.quicksearch.searchEngines.inline.compactTopDividerColor
import com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorRowTopPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Column
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.zIndex
import com.tk.quicksearch.searchEngines.inline.rememberInsetCardStyle
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.inline.AiFollowUpInputSection
import com.tk.quicksearch.searchEngines.inline.LocalSearchEngineAreaFooter
import com.tk.quicksearch.search.core.SearchEnginesVisibility
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser

@Composable
internal fun SearchScreenBottomChrome(
    state: SearchUiState,
    expandedSection: ExpandedSection,
    keyboardSwitchText: String?,
    shouldShowPhoneCallAction: Boolean,
    detectedAlarmTime: java.time.LocalTime?,
    detectedTimerSeconds: Int?,
    detectedReminderSchedule: ReminderNaturalLanguageParser.Schedule?,
    onKeyboardSwitchToggle: () -> Unit,
    onQuickActionCompleted: () -> Unit,
    isSearchHistoryExpanded: Boolean,
    overlayCardColor: androidx.compose.ui.graphics.Color?,
    overlayDividerTint: androidx.compose.ui.graphics.Color?,
    overlayActionTint: androidx.compose.ui.graphics.Color?,
    isAiFollowUpInputVisible: Boolean,
    aiFollowUpText: String,
    onAiFollowUpTextChange: (String) -> Unit,
    onAiFollowUpInputVisibilityChange: (Boolean) -> Unit,
    onAiFollowUpSubmit: (String) -> Unit,
    useInsetEngineStrip: Boolean,
    insetEngineStripOverlap: androidx.compose.ui.unit.Dp,
    insetEngineStripFullBleedFraction: () -> Float,
    searchEnginesModifier: Modifier,
    bottomBarSwipeModifier: Modifier,
    enabledTargets: List<SearchTarget>,
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    searchEngineScrollState: androidx.compose.foundation.lazy.LazyListState,
    onClearDetectedShortcut: () -> Unit,
    predictedTargetForIndicator: PredictedSubmitTarget?,
    activeToolCardConfig: ToolCardConfig?,
    showOnlyToolActionInCompactSection: Boolean,
    shouldRenderInlineNumberKeyboardOperators: Boolean,
    showBottomSearchBar: Boolean,
    isOverlayPresentation: Boolean,
    onQueryChanged: (String) -> Unit,
    showSearchField: Boolean,
    searchFieldContent: @Composable () -> Unit,
    shouldShowOpenKeyboardAction: Boolean,
    delayedOpenKeyboardActionVisible: Boolean,
    hideOpenKeyboardActionInstantly: Boolean,
    openKeyboardText: String,
    onVoiceClick: () -> Unit,
    onOpenKeyboardActionClicked: () -> Unit,
) {
    // With compact engines the bar sits at the top of a card that the engine row (or the AI follow-up
    // input) paints from below, so that content moves under the bar. The bar keeps the same slot in
    // composition either way, so toggling the card never resets its text field.
    val engineArea: @Composable () -> Unit = {
        if (expandedSection == ExpandedSection.NONE && !isSearchHistoryExpanded) {
            CompositionLocalProvider(
                    LocalOverlayResultCardColor provides overlayCardColor,
                    LocalOverlayDividerColor provides overlayDividerTint,
                    LocalOverlayActionColor provides overlayActionTint,
            ) {
                if (isAiFollowUpInputVisible) {
                    AiFollowUpInputSection(
                            value = aiFollowUpText,
                            onValueChange = onAiFollowUpTextChange,
                            onSend = {
                                val followUp = aiFollowUpText.trim()
                                if (followUp.isNotEmpty()) {
                                    onAiFollowUpInputVisibilityChange(false)
                                    onAiFollowUpTextChange("")
                                    onAiFollowUpSubmit(followUp)
                                }
                            },
                            showWallpaperBackground = state.showWallpaperBackground,
                            useInsetContainer = useInsetEngineStrip,
                            insetOverlap = insetEngineStripOverlap,
                            insetFullBleedFraction = insetEngineStripFullBleedFraction,
                            insetCardStyle =
                                    rememberInsetCardStyle(
                                            state.searchBarStyle,
                                            state.showWallpaperBackground,
                                    ),
                            modifier = searchEnginesModifier,
                    )
                } else {
                    AnimatedSearchEngineStrip(
                        state = state,
                        bottomBarSwipeModifier = bottomBarSwipeModifier,
                        enabledTargets = enabledTargets,
                        onSearchTargetClick = onSearchTargetClick,
                        onSearchEngineLongPress = onSearchEngineLongPress,
                        searchEngineScrollState = searchEngineScrollState,
                        onClearDetectedShortcut = onClearDetectedShortcut,
                        predictedTargetForIndicator = predictedTargetForIndicator,
                        activeToolCardConfig = activeToolCardConfig,
                        showOnlyToolActionInCompactSection = showOnlyToolActionInCompactSection,
                        useInsetEngineStrip = useInsetEngineStrip,
                        insetEngineStripOverlap = insetEngineStripOverlap,
                        insetEngineStripFullBleedFraction = insetEngineStripFullBleedFraction,
                        searchEnginesModifier = searchEnginesModifier,
                    )
                }
            }
        }
    }

    val quickActionPills: @Composable (androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit = {
        topPadding,
        bottomPadding,
    ->
        SearchQuickActionPills(
            query = state.query,
            keyboardSwitchText = keyboardSwitchText,
            shouldShowPhoneCallAction = shouldShowPhoneCallAction,
            detectedAlarmTime = detectedAlarmTime,
            detectedTimerSeconds = detectedTimerSeconds,
            detectedReminderSchedule = detectedReminderSchedule,
            onKeyboardSwitchToggle = onKeyboardSwitchToggle,
            onQuickActionCompleted = onQuickActionCompleted,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
        )
    }

    val hasQuickActions =
            keyboardSwitchText != null ||
                    shouldShowPhoneCallAction ||
                    detectedAlarmTime != null ||
                    detectedTimerSeconds != null ||
                    detectedReminderSchedule != null
    // With the bottom search bar the pills go below it; otherwise below the engine area.
    val pillsBelowBottomSearchBar = showSearchField && showBottomSearchBar
    // When the engine area (compact card or full-bleed strip) sits directly above the keyboard, it
    // hosts the pills at its bottom so they share its surface; otherwise they get their own band.
    val engineAreaHostsPills =
            (useInsetEngineStrip || !pillsBelowBottomSearchBar) &&
                    !isSearchHistoryExpanded &&
                    (isAiFollowUpInputVisible ||
                            state.searchEnginesState is SearchEnginesVisibility.Compact ||
                            state.searchEnginesState is SearchEnginesVisibility.Full ||
                            (state.searchEnginesState is SearchEnginesVisibility.Hidden &&
                                    activeToolCardConfig != null))
    val numberOperatorsBelowPills =
            shouldRenderInlineNumberKeyboardOperators &&
                    (pillsBelowBottomSearchBar || useInsetEngineStrip || !showBottomSearchBar)
    // The pills' bottom padding mirrors the space above them. A number operator row directly below
    // adds its own top padding, so it is taken off here to keep the pills centered.
    val pillsBottomPadding: (androidx.compose.ui.unit.Dp) -> androidx.compose.ui.unit.Dp = { gapAbove ->
        if (numberOperatorsBelowPills && !useInsetEngineStrip) {
            (gapAbove - NumberKeyboardOperatorRowTopPadding).coerceAtLeast(0.dp)
        } else {
            gapAbove
        }
    }
    // Hosted pills sit right under the engine row, whose own bottom padding is the gap above them.
    val hostedQuickActionPills: (@Composable ColumnScope.() -> Unit)? =
            if (engineAreaHostsPills) {
                {
                    quickActionPills(
                            0.dp,
                            pillsBottomPadding(SearchEngineSectionConstants.VERTICAL_PADDING),
                    )
                }
            } else {
                null
            }
    val standaloneQuickActionPills: @Composable () -> Unit = {
        if (!engineAreaHostsPills) {
            Surface(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .extendToScreenEdges()
                                    .insetCardBackdrop(true, RectangleShape),
                    color =
                            insetCardBackground(
                                    true,
                                    AppColors.getSearchEngineSectionBackground(state.showWallpaperBackground),
                            ),
                    shape = RectangleShape,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Same hairline the compact engine strip draws along its top edge.
                    if (hasQuickActions) {
                        HorizontalDivider(
                                color = compactTopDividerColor(state.showWallpaperBackground),
                                thickness = DesignTokens.BorderWidth,
                        )
                    }
                    quickActionPills(
                            DesignTokens.SpacingMedium,
                            pillsBottomPadding(DesignTokens.SpacingMedium),
                    )
                }
            }
        }
    }

    val operatorsVisible = shouldRenderInlineNumberKeyboardOperators
    // Under the bottom search card the operator row joins the card's own surface, so the card
    // carries one frosted background down to the keyboard instead of stacking a separate band.
    val numberOperatorsInInsetCard = useInsetEngineStrip && engineAreaHostsPills
    val numberKeyboardOperators: @Composable (Boolean, Boolean) -> Unit = { visible, insideCard ->
        AnimatedVisibility(
                visible = visible,
                modifier = Modifier.fillMaxWidth(),
                enter =
                        fadeIn(animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) +
                                expandVertically(
                                        expandFrom = Alignment.Bottom,
                                        animationSpec = tween(durationMillis = AppMotion.DurationStandard, easing = AppMotion.EmphasizedDecelerateEasing),
                                ),
                exit =
                        fadeOut(animationSpec = tween(durationMillis = AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)) +
                                shrinkVertically(
                                        shrinkTowards = Alignment.Bottom,
                                        animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing),
                                ),
        ) {
            NumberKeyboardOperatorPills(
                    modifier = if (insideCard) Modifier else Modifier.imePadding(),
                    isOverlayPresentation = isOverlayPresentation,
                    extendToScreenEdges = false,
                    showWallpaperBackground = state.showWallpaperBackground,
                    transparentBackground = insideCard,
                    onOperatorClick = { operator -> onQueryChanged(state.query + operator) },
            )
        }
    }

    // Surfaces pinned here float over the results, which scroll beneath them and show through blurred.
    CompositionLocalProvider(LocalInsetCardBackdropTargetsEnabled provides true) {
        if (expandedSection == ExpandedSection.NONE) {
            if (!useInsetEngineStrip) {
                CompositionLocalProvider(
                        LocalSearchEngineAreaFooter provides
                                hostedQuickActionPills.takeIf { !pillsBelowBottomSearchBar },
                ) {
                    engineArea()
                }
            }
            if (!useInsetEngineStrip && !pillsBelowBottomSearchBar) standaloneQuickActionPills()

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                androidx.compose.animation.AnimatedVisibility(
                        visible = shouldRenderInlineNumberKeyboardOperators && !showBottomSearchBar,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) +
                                        expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = AppMotion.DurationStandard, easing = AppMotion.EmphasizedDecelerateEasing),
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(durationMillis = AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing),
                                        ),
                ) {
                    NumberKeyboardOperatorPills(
                            modifier = Modifier.imePadding(),
                            isOverlayPresentation = isOverlayPresentation,
                            extendToScreenEdges = false,
                            showWallpaperBackground = state.showWallpaperBackground,
                            onOperatorClick = { operator ->
                                onQueryChanged(state.query + operator)
                            },
                    )
                }
            }
        }

        if (showSearchField && showBottomSearchBar) {
            // The compact engine card carries the bar's background, so the bar itself stays
            // transparent over it instead of sitting on a full-bleed band.
            // Drawn above the card that reaches up behind it from the engine area below.
            Box(modifier = Modifier.fillMaxWidth().zIndex(1f).then(bottomBarSwipeModifier)) {
                searchFieldContent()
            }
            if (!useInsetEngineStrip && expandedSection == ExpandedSection.NONE) {
                standaloneQuickActionPills()
            }
            if (useInsetEngineStrip) {
                CompositionLocalProvider(
                        LocalSearchEngineAreaFooter provides
                                if (numberOperatorsInInsetCard) {
                                    {
                                        hostedQuickActionPills?.invoke(this)
                                        numberKeyboardOperators(
                                                operatorsVisible && expandedSection == ExpandedSection.NONE,
                                                true,
                                        )
                                    }
                                } else {
                                    hostedQuickActionPills
                                },
                ) {
                    engineArea()
                }
                standaloneQuickActionPills()
                // Keeps standalone operator pills below from sitting flush against the card's
                // bottom edge. Otherwise it collapses as the card goes full bleed so the card (and
                // any operator row inside it) meets the keyboard.
                val showsStandaloneNumberKeyboardPills =
                        expandedSection == ExpandedSection.NONE &&
                                operatorsVisible &&
                                !numberOperatorsInInsetCard
                // Sized in layout so the full-bleed animation does not recompose this chrome.
                Spacer(
                        modifier =
                                Modifier.layout { measurable, constraints ->
                                    val gapFraction =
                                            if (showsStandaloneNumberKeyboardPills) 0f else insetEngineStripFullBleedFraction()
                                    val height =
                                            constraints.constrainHeight(
                                                    (DesignTokens.SpacingSmall * (1f - gapFraction)).roundToPx(),
                                            )
                                    val placeable =
                                            measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                                    layout(placeable.width, height) { placeable.place(0, 0) }
                                },
                )
            }

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                numberKeyboardOperators(
                        expandedSection == ExpandedSection.NONE &&
                                operatorsVisible &&
                                !numberOperatorsInInsetCard,
                        false,
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
            androidx.compose.animation.AnimatedVisibility(
                    visible = shouldShowOpenKeyboardAction && delayedOpenKeyboardActionVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter =
                            fadeIn(animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) +
                                    expandVertically(
                                            expandFrom = Alignment.Bottom,
                                            animationSpec = tween(durationMillis = AppMotion.DurationStandard, easing = AppMotion.EmphasizedDecelerateEasing),
                                    ),
                    exit =
                            if (hideOpenKeyboardActionInstantly) {
                                ExitTransition.None
                            } else {
                                shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing),
                                )
                            },
            ) {
                OpenKeyboardAction(
                        text = openKeyboardText,
                        onVoiceClick = onVoiceClick,
                        // The search bar's own mic already covers voice search.
                        showMic = !state.searchBarStyle.showMicIcon,
                        showWallpaperBackground = state.showWallpaperBackground,
                        extendBehindNavigationBar = !isOverlayPresentation,
                        // Only the engine strip / AI follow-up input bring their own bottom spacing.
                        padTop =
                                isSearchHistoryExpanded ||
                                        (!isAiFollowUpInputVisible &&
                                                state.searchEnginesState is
                                                        com.tk.quicksearch.search.core.SearchEnginesVisibility.Hidden),
                        // Keep the Open Keyboard surface in the same vertical-swipe path as
                        // the fixed search field and engine strip. This routes configured
                        // keyboard gestures first, then the regular Home swipe actions.
                        modifier = Modifier.fillMaxWidth().then(bottomBarSwipeModifier),
                        onClick = {
                            onOpenKeyboardActionClicked()
                        },
                )
            }
        }
    }
}
