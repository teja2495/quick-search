package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.SearchEnginesVisibility
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.searchEngines.inline.SearchEngineIconsSection

private const val ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS = 280
private const val ONE_HANDED_COMPACT_ENGINES_FADE_IN_DURATION_MS = 180
private const val ONE_HANDED_COMPACT_ENGINES_FADE_IN_DELAY_MS = 40
private const val ONE_HANDED_COMPACT_ENGINES_FADE_OUT_DURATION_MS = 130

@Composable
internal fun AnimatedSearchEngineStrip(
    state: SearchUiState,
    bottomBarSwipeModifier: Modifier,
    enabledTargets: List<SearchTarget>,
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: () -> Unit,
    searchEngineScrollState: androidx.compose.foundation.lazy.LazyListState,
    onClearDetectedShortcut: () -> Unit,
    predictedTargetForIndicator: PredictedSubmitTarget?,
    activeToolCardConfig: ToolCardConfig?,
    showOnlyToolActionInCompactSection: Boolean,
    useInsetEngineStrip: Boolean,
    insetEngineStripOverlap: androidx.compose.ui.unit.Dp,
    insetEngineStripFullBleedFraction: Float,
    searchEnginesModifier: Modifier,
) {
                        AnimatedContent(
                            targetState = state.searchEnginesState,
                            modifier = Modifier.fillMaxWidth().then(bottomBarSwipeModifier),
                            contentKey = { it::class },
                            transitionSpec = {
                                if (
                                    state.oneHandedMode &&
                                        (initialState is SearchEnginesVisibility.Compact ||
                                            targetState is SearchEnginesVisibility.Compact)
                                ) {
                                    val enterTransition =
                                        if (targetState is SearchEnginesVisibility.Compact) {
                                            fadeIn(
                                                animationSpec =
                                                    tween(
                                                        durationMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_IN_DURATION_MS,
                                                        delayMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_IN_DELAY_MS,
                                                    ),
                                            ) +
                                                expandVertically(
                                                    expandFrom = Alignment.Bottom,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                            easing = FastOutSlowInEasing,
                                                        ),
                                                )
                                        } else {
                                            EnterTransition.None
                                        }
                                    val exitTransition =
                                        if (initialState is SearchEnginesVisibility.Compact) {
                                            fadeOut(
                                                animationSpec =
                                                    tween(
                                                        durationMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_OUT_DURATION_MS,
                                                    ),
                                            ) +
                                                shrinkVertically(
                                                    shrinkTowards = Alignment.Bottom,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                            easing = FastOutSlowInEasing,
                                                        ),
                                                )
                                        } else {
                                            ExitTransition.None
                                        }
                                    enterTransition
                                        .togetherWith(exitTransition)
                                        .using(
                                            SizeTransform(clip = false) { _, _ ->
                                                tween(
                                                    durationMillis =
                                                        ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                    easing = FastOutSlowInEasing,
                                                )
                                            },
                                        )
                                } else {
                                    (EnterTransition.None togetherWith ExitTransition.None)
                                        .using(null)
                                }
                            },
                            label = "oneHandedCompactSearchEnginesReflow",
                        ) { animatedEnginesState ->
                            SearchEnginesVisibility(
                                enginesState = animatedEnginesState,
                            compactContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = state.detectedShortcutTarget,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = state.searchEngineCompactRowCount,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                        toolActionLabel = activeToolCardConfig?.label,
                                        toolActionIcon = activeToolCardConfig?.icon,
                                        toolActionAppIconPackage = activeToolCardConfig?.appIconPackage,
                                        onToolActionClick = activeToolCardConfig?.onClick,
                                        showOnlyToolAction = showOnlyToolActionInCompactSection,
                                        useInsetContainer = useInsetEngineStrip,
                                        insetOverlap = insetEngineStripOverlap,
                                        insetFullBleedFraction = insetEngineStripFullBleedFraction,
                                )
                            },
                            fullContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = state.detectedShortcutTarget,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = 1,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                )
                            },
                            shortcutContent = { target ->
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = target,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = 1,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                )
                            },
                            hiddenContent = {
                                if (activeToolCardConfig != null) {
                                    SearchEngineIconsSection(
                                            modifier = searchEnginesModifier,
                                            query = state.query,
                                            enabledEngines = enabledTargets,
                                            onSearchEngineClick = onSearchTargetClick,
                                            onSearchEngineLongPress = onSearchEngineLongPress,
                                            externalScrollState = searchEngineScrollState,
                                            detectedShortcutTarget = state.detectedShortcutTarget,
                                            onClearDetectedShortcut = onClearDetectedShortcut,
                                            showWallpaperBackground = state.showWallpaperBackground,
                                            compactRowCount = state.searchEngineCompactRowCount,
                                            predictedTarget = predictedTargetForIndicator,
                                            appIconShape = state.appIconShape,
                                            iconPackPackage = state.selectedIconPackPackage,
                                            toolActionLabel = activeToolCardConfig.label,
                                            toolActionIcon = activeToolCardConfig.icon,
                                            toolActionAppIconPackage = activeToolCardConfig.appIconPackage,
                                            onToolActionClick = activeToolCardConfig.onClick,
                                            showOnlyToolAction = true,
                                            useInsetContainer = useInsetEngineStrip,
                                            insetOverlap = insetEngineStripOverlap,
                                            insetFullBleedFraction = insetEngineStripFullBleedFraction,
                                    )
                                } else {
                                    // Add padding when search engines are hidden to prevent keyboard from
                                    // covering content
                                    Spacer(modifier = searchEnginesModifier)
                                }
                            },
                            )
                        }
}
