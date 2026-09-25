package com.tk.quicksearch.search.searchScreen

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
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.inline.AiFollowUpInputSection
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
    insetEngineStripFullBleedFraction: Float,
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
        if (expandedSection == ExpandedSection.NONE) {
            SearchQuickActionPills(
                query = state.query,
                keyboardSwitchText = keyboardSwitchText,
                shouldShowPhoneCallAction = shouldShowPhoneCallAction,
                detectedAlarmTime = detectedAlarmTime,
                detectedTimerSeconds = detectedTimerSeconds,
                detectedReminderSchedule = detectedReminderSchedule,
                onKeyboardSwitchToggle = onKeyboardSwitchToggle,
            )
            if (!isSearchHistoryExpanded) {
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

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                androidx.compose.animation.AnimatedVisibility(
                        visible = shouldRenderInlineNumberKeyboardOperators && !showBottomSearchBar,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(durationMillis = 180)) +
                                        expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 220),
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
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
            // The compact engine strip carries its own rounded background, so the search bar
            // stays transparent over the wallpaper instead of sitting on a full-bleed band.
            Box(modifier = Modifier.fillMaxWidth().then(bottomBarSwipeModifier)) {
                searchFieldContent()
            }
            if (useInsetEngineStrip) {
                // Keeps the pills below from sitting flush against the card's bottom edge. Without
                // the pills it collapses as the card goes full bleed so the card meets the keyboard.
                val showsNumberKeyboardPills =
                        expandedSection == ExpandedSection.NONE &&
                                shouldRenderInlineNumberKeyboardOperators
                val gapFraction =
                        if (showsNumberKeyboardPills) 0f else insetEngineStripFullBleedFraction
                Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall * (1f - gapFraction)))
            }

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                androidx.compose.animation.AnimatedVisibility(
                        visible =
                                expandedSection == ExpandedSection.NONE &&
                                        shouldRenderInlineNumberKeyboardOperators,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(durationMillis = 180)) +
                                        expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 220),
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
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

        Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
            androidx.compose.animation.AnimatedVisibility(
                    visible = shouldShowOpenKeyboardAction && delayedOpenKeyboardActionVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter =
                            fadeIn(animationSpec = tween(durationMillis = 180)) +
                                    expandVertically(
                                            expandFrom = Alignment.Bottom,
                                            animationSpec = tween(durationMillis = 220),
                                    ),
                    exit =
                            if (hideOpenKeyboardActionInstantly) {
                                ExitTransition.None
                            } else {
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
                                        )
                            },
            ) {
                OpenKeyboardAction(
                        text = openKeyboardText,
                        onVoiceClick = onVoiceClick,
                        showWallpaperBackground = state.showWallpaperBackground,
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
