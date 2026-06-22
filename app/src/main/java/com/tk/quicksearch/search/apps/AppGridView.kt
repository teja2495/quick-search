package com.tk.quicksearch.search.apps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.LocalIsSystemWallpaperActive
import com.tk.quicksearch.shared.ui.theme.LocalWallpaperDynamicAccentActive
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlin.math.min
import java.util.Locale

private const val ROW_COUNT = 2
private const val ALL_APPS_ROW_COUNT = 3
private const val TabSlideOffsetPx = 64
private const val SuggestionsEnterDurationMillis = 320
private const val SuggestionsEnterOffsetDp = 12f
private const val SuggestionTabInactiveAlpha = 0.34f
private const val SuggestionTabSwipeThresholdPx = 48f
private val AppGridRowSpacing = DesignTokens.SpacingXSmall
private val AppGridWidthRoundingSlack = 1.dp
private val RegularAppIconSize = DesignTokens.IconSizeXLarge - DesignTokens.SpacingXXSmall
private val OverlayAppIconSurfaceSize = 52.dp
private val OverlayAppIconSize = 36.dp
private val TopResultIndicatorTopPadding = 0.dp
private val TopResultIndicatorBottomPadding = DesignTokens.SpacingSmall
private val TopResultIndicatorHorizontalPadding = DesignTokens.SpacingSmall
private const val TopResultIndicatorBackgroundAlpha = 0.12f
private const val TopResultIndicatorBorderAlpha = 0.22f
private const val LightWallpaperAppIconShadowAmbientAlpha = 0.28f
private const val LightWallpaperAppIconShadowSpotAlpha = 0.45f
private const val ThemedMonochromeGlyphScale = 1.42f
private const val UnsupportedThemedIconGlyphScale = 0.62f
private const val UnsupportedThemedIconGlyphAlpha = 0.72f
private const val DraggedPinnedAppScale = 1.08f
private const val DraggedPinnedAppAlpha = 0.92f

private enum class AppIconDisplayMode {
    OVERLAY,
    REGULAR,
}

private data class PinnedAppDragState(
        val key: String,
        val startIndex: Int,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
)

private data class AppSuggestionTab(
        val type: AppSuggestionTabType,
        val title: String,
        val apps: List<AppInfo>,
)

/** Data class containing all app actions to reduce parameter count in composables. */
private data class AppActions(
        val onClick: () -> Unit,
        val onAppInfoClick: () -> Unit,
        val onUninstallClick: () -> Unit,
        val onHideApp: () -> Unit,
        val onPinApp: () -> Unit,
        val onUnpinApp: () -> Unit,
        val onNicknameClick: () -> Unit,
        val onTriggerClick: () -> Unit,
        val onAddToHome: () -> Unit,
)

/** Data class containing app state information to reduce parameter count in composables. */
private data class AppState(
        val hasNickname: Boolean,
        val hasTrigger: Boolean,
        val isPinned: Boolean,
        val showUninstall: Boolean,
        val showAppLabel: Boolean,
        val isOverlayPresentation: Boolean,
)

@Composable
fun AppGridView(
        apps: List<AppInfo>,
        allApps: List<AppInfo>,
        pinnedAndRecentApps: List<AppInfo>,
        pinnedApps: List<AppInfo>,
        newOrUpdatedApps: List<AppInfo>,
        mostUsedApps: List<AppInfo>,
        appShortcuts: List<StaticShortcut>,
        isSearching: Boolean,
        hasUsagePermission: Boolean,
        selectedSuggestionTab: AppSuggestionTabType,
        enabledSuggestionTabs: Set<AppSuggestionTabType>,
        onSuggestionTabSelected: (AppSuggestionTabType) -> Unit,
        hasAppResults: Boolean,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onReorderPinnedApps: (List<AppInfo>) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        disabledShortcutIds: Set<String>,
        modifier: Modifier = Modifier,
        rowCount: Int = ROW_COUNT,
        phoneColumnOverride: Int = 5,
        appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        iconPackPackage: String? = null,
        showAppLabels: Boolean = true,
        oneHandedMode: Boolean = false,
        isInitializing: Boolean = false,
        isOverlayPresentation: Boolean = false,
        startupPhase: StartupPhase = StartupPhase.COMPLETE,
        predictedTarget: PredictedSubmitTarget? = null,
        suppressTopResultIndicator: Boolean = false,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
        onGridAppeared: (() -> Unit)? = null,
        suppressSuggestionsEnterAnimation: Boolean = false,
) {
    val pinnedTitle = stringResource(R.string.app_suggestions_tab_pinned)
    val recentsTitle = stringResource(R.string.app_suggestions_tab_recent)
    val newUpdatedTitle = stringResource(R.string.app_suggestions_tab_new_updated)
    val mostUsedTitle = stringResource(R.string.common_most_used)
    val allAppsTitle = stringResource(R.string.settings_app_shortcuts_filter_all_apps)
    val alphabeticalApps =
            remember(allApps) {
                allApps.sortedWith(
                        compareBy<AppInfo> { it.appName.lowercase(Locale.getDefault()) }
                                .thenBy { it.packageName.lowercase(Locale.getDefault()) }
                                .thenBy { it.userHandleId ?: Int.MIN_VALUE },
                )
            }
    val suggestionTabs =
            remember(
                    hasUsagePermission,
                    isSearching,
                    allAppsTitle,
                    newUpdatedTitle,
                    pinnedTitle,
                    recentsTitle,
                    mostUsedTitle,
                    alphabeticalApps,
                    pinnedApps,
                    newOrUpdatedApps,
                    pinnedAndRecentApps,
                    mostUsedApps,
                    enabledSuggestionTabs,
            ) {
                if (isSearching) return@remember emptyList()
                if (hasUsagePermission) {
                    buildList {
                        if (AppSuggestionTabType.NEW_UPDATED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.NEW_UPDATED, newUpdatedTitle, newOrUpdatedApps))
                        }
                        if (pinnedApps.isNotEmpty() && AppSuggestionTabType.PINNED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.PINNED, pinnedTitle, pinnedApps))
                        }
                        if (AppSuggestionTabType.RECENTS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.RECENTS, recentsTitle, pinnedAndRecentApps))
                        }
                        if (AppSuggestionTabType.MOST_USED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.MOST_USED, mostUsedTitle, mostUsedApps))
                        }
                        if (AppSuggestionTabType.ALL_APPS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.ALL_APPS, allAppsTitle, alphabeticalApps))
                        }
                    }
                } else {
                    buildList {
                        if (pinnedApps.isNotEmpty() && AppSuggestionTabType.PINNED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.PINNED, pinnedTitle, pinnedApps))
                        }
                        if (AppSuggestionTabType.RECENTS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.RECENTS, recentsTitle, pinnedAndRecentApps))
                        }
                        if (AppSuggestionTabType.ALL_APPS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.ALL_APPS, allAppsTitle, alphabeticalApps))
                        }
                    }
                }
            }
    val selectedSuggestionTabIndex =
            remember(suggestionTabs, selectedSuggestionTab) {
                val preferredIndex = suggestionTabs.indexOfFirst { it.type == selectedSuggestionTab }
                if (preferredIndex >= 0) {
                    preferredIndex
                } else {
                    suggestionTabs.indexOfFirst { it.type == AppSuggestionTabType.RECENTS }
                            .takeIf { it >= 0 }
                            ?: 0
                }
            }
    fun selectSuggestionTab(index: Int) {
        suggestionTabs.getOrNull(index)?.let { tab ->
            onSuggestionTabSelected(tab.type)
        }
    }
    val minSuggestionGridItems = (rowCount * getAppGridColumns(phoneColumnOverride)).coerceAtLeast(1)
    val suggestionFallbackApps = remember(pinnedAndRecentApps, apps) { pinnedAndRecentApps + apps }
    val selectedSuggestionTabType = suggestionTabs.getOrNull(selectedSuggestionTabIndex)?.type
    val scrollableSuggestionRowCount =
            if (selectedSuggestionTabType == AppSuggestionTabType.ALL_APPS) {
                ALL_APPS_ROW_COUNT
            } else {
                null
            }
    val activeApps =
            if (suggestionTabs.isNotEmpty()) {
                val selectedTab = suggestionTabs[selectedSuggestionTabIndex]
                if (selectedTab.type == AppSuggestionTabType.PINNED || selectedTab.type == AppSuggestionTabType.ALL_APPS) {
                    selectedTab.apps
                } else {
                    fillSuggestionGridApps(
                            primaryApps = selectedTab.apps,
                            fallbackApps = suggestionFallbackApps,
                            minItems = minSuggestionGridItems,
                    )
                }
            } else {
                if (isSearching) apps else emptyList()
            }
    val shortcutsByPackage =
            remember(appShortcuts, disabledShortcutIds) {
                appShortcuts
                        .asSequence()
                        .filterNot { shortcut ->
                            disabledShortcutIds.contains(shortcutKey(shortcut))
                        }
                        .groupBy { it.packageName }
            }
    // Search results should not wait for the cold-start icon pipeline. If they do,
    // secondary sections can render first even when app results are already in state.
    val waitForAppIcons =
            activeApps.isNotEmpty() &&
                    !isSearching &&
                    selectedSuggestionTabType != AppSuggestionTabType.ALL_APPS
    val areAppIconsLoaded =
            if (waitForAppIcons) {
                activeApps.all { app ->
                    val iconResult =
                            rememberAppIcon(
                                    packageName = app.packageName,
                                    iconPackPackage = iconPackPackage,
                                    userHandleId = app.userHandleId,
                                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                            )
                    iconResult.bitmap != null
                }
            } else {
                true
            }

    // Once the grid has appeared with all icons loaded, keep it visible even when new fuzzy-search
    // results arrive with icons still loading — avoids flicker when the list grows mid-search.
    var gridHasBeenVisible by remember { mutableStateOf(false) }

    // Animate the suggestions grid (empty query) when it first appears. Search results should
    // appear immediately without animation.
    val initialSuggestionsAlpha = if (suppressSuggestionsEnterAnimation) 1f else 0f
    val initialSuggestionsOffset = if (suppressSuggestionsEnterAnimation) 0f else SuggestionsEnterOffsetDp
    val suggestionsAlpha = remember { Animatable(initialSuggestionsAlpha) }
    val suggestionsTranslationYDp = remember { Animatable(initialSuggestionsOffset) }
    val density = LocalDensity.current
    val tabSwipeModifier =
            if (suggestionTabs.size > 1) {
                Modifier.pointerInput(suggestionTabs, selectedSuggestionTabIndex) {
                    var dragAmount = 0f
                    detectHorizontalDragGestures(
                            onDragStart = { dragAmount = 0f },
                            onHorizontalDrag = { _, dragDelta ->
                                dragAmount += dragDelta
                            },
                            onDragEnd = {
                                when {
                                    dragAmount > SuggestionTabSwipeThresholdPx &&
                                            selectedSuggestionTabIndex > 0 ->
                                            selectSuggestionTab(selectedSuggestionTabIndex - 1)
                                    dragAmount < -SuggestionTabSwipeThresholdPx &&
                                            selectedSuggestionTabIndex < suggestionTabs.lastIndex ->
                                            selectSuggestionTab(selectedSuggestionTabIndex + 1)
                                }
                                dragAmount = 0f
                            },
                            onDragCancel = { dragAmount = 0f },
                    )
                }
            } else {
                Modifier
            }

    Column(
            modifier = modifier.fillMaxWidth().then(tabSwipeModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
    ) {
        val showAppGrid = activeApps.isNotEmpty() && (areAppIconsLoaded || gridHasBeenVisible)

        LaunchedEffect(showAppGrid, isSearching) {
            if (!showAppGrid) return@LaunchedEffect
            if (isSearching || suppressSuggestionsEnterAnimation) {
                suggestionsAlpha.snapTo(1f)
                suggestionsTranslationYDp.snapTo(0f)
            } else if (suggestionsAlpha.value < 1f) {
                suggestionsAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = SuggestionsEnterDurationMillis),
                )
            }
            onGridAppeared?.invoke()
        }
        LaunchedEffect(showAppGrid, isSearching) {
            if (!showAppGrid || isSearching || suppressSuggestionsEnterAnimation) return@LaunchedEffect
            if (suggestionsTranslationYDp.value != 0f) {
                suggestionsTranslationYDp.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = SuggestionsEnterDurationMillis),
                )
            }
        }

        if (showAppGrid) {
            gridHasBeenVisible = true
            val suggestionsContentModifier = Modifier.graphicsLayer {
                alpha = suggestionsAlpha.value
                translationY = with(density) { suggestionsTranslationYDp.value.dp.toPx() }
            }
            Column(
                    modifier = suggestionsContentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
            ) {
                if (suggestionTabs.isNotEmpty()) {
                    AppSuggestionTabStrip(
                            tabs = suggestionTabs,
                            selectedIndex = selectedSuggestionTabIndex,
                            onSelectedIndexChange = ::selectSuggestionTab,
                    )
                }
                if (suggestionTabs.size > 1 && !isSearching) {
                    AnimatedContent(
                            targetState = selectedSuggestionTabIndex,
                            transitionSpec = {
                                val movingForward = targetState > initialState
                                val stiffness = Spring.StiffnessMediumLow
                                if (movingForward) {
                                    slideInHorizontally(
                                            initialOffsetX = { TabSlideOffsetPx },
                                            animationSpec = spring(stiffness = stiffness),
                                    ) + fadeIn() togetherWith
                                            slideOutHorizontally(
                                                    targetOffsetX = { -TabSlideOffsetPx },
                                                    animationSpec = spring(stiffness = stiffness),
                                            ) + fadeOut()
                                } else {
                                    slideInHorizontally(
                                            initialOffsetX = { -TabSlideOffsetPx },
                                            animationSpec = spring(stiffness = stiffness),
                                    ) + fadeIn() togetherWith
                                            slideOutHorizontally(
                                                    targetOffsetX = { TabSlideOffsetPx },
                                                    animationSpec = spring(stiffness = stiffness),
                                            ) + fadeOut()
                                }
                            },
                            label = "appSuggestionTabSlide",
                    ) { selectedIndex ->
                        val selectedTab = suggestionTabs[selectedIndex]
                        AppGrid(
                                apps =
                                        if (
                                                selectedTab.type == AppSuggestionTabType.PINNED ||
                                                        selectedTab.type == AppSuggestionTabType.ALL_APPS
                                        ) {
                                            selectedTab.apps
                                        } else {
                                            fillSuggestionGridApps(
                                                    primaryApps = selectedTab.apps,
                                                    fallbackApps = suggestionFallbackApps,
                                                    minItems = minSuggestionGridItems,
                                            )
                                        },
                                isSearching = isSearching,
                                onAppClick = onAppClick,
                                onAppInfoClick = onAppInfoClick,
                                onUninstallClick = onUninstallClick,
                                onHideApp = onHideApp,
                                onPinApp = onPinApp,
                                onUnpinApp = onUnpinApp,
                                onReorderPinnedApps = onReorderPinnedApps,
                                onNicknameClick = onNicknameClick,
                                onTriggerClick = onTriggerClick,
                                getAppNickname = getAppNickname,
                                getAppTrigger = getAppTrigger,
                                pinnedPackageNames = pinnedPackageNames,
                                shortcutsByPackage = shortcutsByPackage,
                                rowCount = rowCount,
                                phoneColumnOverride = phoneColumnOverride,
                                appIconSizeStep = appIconSizeStep,
                                iconPackPackage = iconPackPackage,
                                showAppLabels = showAppLabels,
                                oneHandedMode = oneHandedMode,
                                isOverlayPresentation = isOverlayPresentation,
                                predictedTarget = predictedTarget,
                                suppressTopResultIndicator = suppressTopResultIndicator,
                                appIconShape = appIconShape,
                                themedIconsEnabled = themedIconsEnabled,
                                showWallpaperBackground = showWallpaperBackground,
                                reorderPinnedApps = selectedTab.type == AppSuggestionTabType.PINNED,
                                scrollableRowCount =
                                        if (selectedTab.type == AppSuggestionTabType.ALL_APPS) {
                                            ALL_APPS_ROW_COUNT
                                        } else {
                                            null
                                        },
                        )
                    }
                } else {
                    AppGrid(
                            apps = activeApps,
                            isSearching = isSearching,
                            onAppClick = onAppClick,
                            onAppInfoClick = onAppInfoClick,
                            onUninstallClick = onUninstallClick,
                            onHideApp = onHideApp,
                            onPinApp = onPinApp,
                            onUnpinApp = onUnpinApp,
                            onReorderPinnedApps = onReorderPinnedApps,
                            onNicknameClick = onNicknameClick,
                            onTriggerClick = onTriggerClick,
                            getAppNickname = getAppNickname,
                            getAppTrigger = getAppTrigger,
                            pinnedPackageNames = pinnedPackageNames,
                            shortcutsByPackage = shortcutsByPackage,
                            rowCount = rowCount,
                            phoneColumnOverride = phoneColumnOverride,
                            appIconSizeStep = appIconSizeStep,
                            iconPackPackage = iconPackPackage,
                            showAppLabels = showAppLabels,
                            oneHandedMode = oneHandedMode,
                            isOverlayPresentation = isOverlayPresentation,
                            predictedTarget = predictedTarget,
                            suppressTopResultIndicator = suppressTopResultIndicator,
                            appIconShape = appIconShape,
                            themedIconsEnabled = themedIconsEnabled,
                            showWallpaperBackground = showWallpaperBackground,
                            reorderPinnedApps =
                                    selectedSuggestionTabType == AppSuggestionTabType.PINNED,
                            scrollableRowCount = scrollableSuggestionRowCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSuggestionTabStrip(
        tabs: List<AppSuggestionTab>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.onSurface
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SuggestionTabInactiveAlpha)
    val leftTab = tabs.getOrNull(selectedIndex - 1)
    val rightTab = tabs.getOrNull(selectedIndex + 1)

    Row(
            modifier =
                    Modifier
                            .fillMaxWidth(0.86f)
                            .padding(bottom = DesignTokens.SpacingXSmall),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        EdgeSuggestionTabLabel(
                title = leftTab?.title.orEmpty(),
                color = inactiveColor,
                alignment = TextAlign.Start,
                onClick = {
                    if (selectedIndex > 0) onSelectedIndexChange(selectedIndex - 1)
                },
        )
        Text(
                text = tabs[selectedIndex].title,
                modifier = Modifier.weight(1f),
                color = activeColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
        )
        EdgeSuggestionTabLabel(
                title = rightTab?.title.orEmpty(),
                color = inactiveColor,
                alignment = TextAlign.End,
                onClick = {
                    if (selectedIndex < tabs.lastIndex) onSelectedIndexChange(selectedIndex + 1)
                },
        )
    }
}

@Composable
private fun RowScope.EdgeSuggestionTabLabel(
        title: String,
        color: Color,
        alignment: TextAlign,
        onClick: () -> Unit,
) {
    Text(
            text = title,
            modifier =
                    Modifier
                            .weight(1f)
                            .clickable(onClick = onClick),
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = alignment,
    )
}

private fun fillSuggestionGridApps(
        primaryApps: List<AppInfo>,
        fallbackApps: List<AppInfo>,
        minItems: Int,
): List<AppInfo> {
    if (primaryApps.size >= minItems) return primaryApps

    val seen = LinkedHashSet<String>(primaryApps.size + fallbackApps.size)
    val result = ArrayList<AppInfo>(minItems)
    primaryApps.forEach { app ->
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    fallbackApps.forEach { app ->
        if (result.size >= minItems) return@forEach
        if (seen.add(app.launchCountKey())) {
            result.add(app)
        }
    }
    return result
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppGrid(
        apps: List<AppInfo>,
        isSearching: Boolean,
        modifier: Modifier = Modifier,
        onAppClick: (AppInfo) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onReorderPinnedApps: (List<AppInfo>) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        rowCount: Int = ROW_COUNT,
        phoneColumnOverride: Int = 5,
        appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        iconPackPackage: String?,
        showAppLabels: Boolean,
        oneHandedMode: Boolean,
        isOverlayPresentation: Boolean,
        predictedTarget: PredictedSubmitTarget?,
        suppressTopResultIndicator: Boolean,
        appIconShape: AppIconShape,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
        reorderPinnedApps: Boolean = false,
        scrollableRowCount: Int? = null,
) {
    var displayedApps by remember(apps, reorderPinnedApps) { mutableStateOf(apps) }
    var dragState by remember { mutableStateOf<PinnedAppDragState?>(null) }
    var measuredItemHeightPx by remember { mutableStateOf(0f) }
    val maxVisibleColumns = getAppGridColumns(phoneColumnOverride)
    val columns =
            remember(apps, maxVisibleColumns) {
                if (apps.isEmpty()) {
                    1
                } else {
                    maxVisibleColumns.coerceAtLeast(1)
                }
            }
    val visibleAppLimit =
            remember(isOverlayPresentation, reorderPinnedApps, rowCount, columns) {
                if (isOverlayPresentation && !reorderPinnedApps) {
                    (rowCount * columns).coerceAtLeast(1)
                } else {
                    Int.MAX_VALUE
                }
            }
    val visibleDisplayedApps =
            remember(displayedApps, visibleAppLimit) {
                displayedApps.take(visibleAppLimit)
            }
    val orderedApps =
            remember(visibleDisplayedApps, oneHandedMode, columns) {
                if (oneHandedMode) {
                    visibleDisplayedApps.chunked(columns).reversed().flatten()
                } else {
                    visibleDisplayedApps
                }
            }
    val predictedAppKey = remember(predictedTarget, suppressTopResultIndicator) {
        if (suppressTopResultIndicator) {
            null
        } else {
            (predictedTarget as? PredictedSubmitTarget.App)?.let { target ->
                if (target.userHandleId == null) {
                    target.packageName
                } else {
                    "${target.packageName}:${target.userHandleId}"
                }
            }
        }
    }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val horizontalSpacing = DesignTokens.SpacingMedium
        val widthRoundingSlack =
                if (columns > 1) AppGridWidthRoundingSlack else 0.dp
        val rowItemWidth =
                if (columns <= 1) {
                    maxWidth
                } else {
                    (
                        ((maxWidth - (horizontalSpacing * (columns - 1))) / columns) -
                            widthRoundingSlack
                    ).coerceAtLeast(0.dp)
                }
        val spacingPx = with(LocalDensity.current) { horizontalSpacing.toPx() }
        val rowItemWidthPx = with(LocalDensity.current) { rowItemWidth.toPx() }
        val scrollContainerHeight =
                scrollableRowCount?.takeIf { it > 0 }?.let { visibleRows ->
                    val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
                    val estimatedIconSurfaceHeight =
                            if (isOverlayPresentation) {
                                OverlayAppIconSurfaceSize * sizeScale
                            } else {
                                DesignTokens.AppIconSize * sizeScale
                            }
                    val estimatedLabelHeight =
                            if (showAppLabels) {
                                val labelTextHeight =
                                        with(density) {
                                            MaterialTheme.typography.labelSmall.fontSize.toDp()
                                        }
                                val labelSpacing =
                                        if (isOverlayPresentation) {
                                            4.dp
                                        } else {
                                            DesignTokens.SpacingXSmall
                                        }
                                labelSpacing + labelTextHeight
                            } else {
                                0.dp
                            }
                    val estimatedItemHeight =
                            estimatedIconSurfaceHeight +
                                    TopResultIndicatorTopPadding +
                                    TopResultIndicatorBottomPadding +
                                    estimatedLabelHeight
                    val measuredItemHeight =
                            with(density) { measuredItemHeightPx.takeIf { it > 0f }?.toDp() }
                                    ?: estimatedItemHeight
                    (measuredItemHeight * visibleRows) +
                            (AppGridRowSpacing * (visibleRows - 1).coerceAtLeast(0))
                }

        fun movePinnedApp(fromIndex: Int, toIndex: Int) {
            if (!reorderPinnedApps || fromIndex == toIndex) return
            val current = displayedApps
            if (fromIndex !in current.indices || toIndex !in current.indices) return
            displayedApps =
                    current.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
        }

        fun targetIndexForDrag(state: PinnedAppDragState): Int {
            val itemHeightPx = measuredItemHeightPx.takeIf { it > 0f } ?: rowItemWidthPx
            val startColumn = state.startIndex % columns
            val startRow = state.startIndex / columns
            val targetColumn =
                    ((startColumn * (rowItemWidthPx + spacingPx) + rowItemWidthPx / 2f + state.offsetX) /
                            (rowItemWidthPx + spacingPx))
                            .toInt()
                            .coerceIn(0, columns - 1)
            val targetRow =
                    ((startRow * (itemHeightPx + spacingPx) + itemHeightPx / 2f + state.offsetY) /
                            (itemHeightPx + spacingPx))
                            .toInt()
                            .coerceAtLeast(0)
            val maxTargetIndex = min(displayedApps.lastIndex, visibleAppLimit - 1)
            return (targetRow * columns + targetColumn).coerceIn(0, maxTargetIndex)
        }

        val context = LocalContext.current
        val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
        val createAppActions =
                remember(
                        onAppClick,
                        onAppInfoClick,
                        onUninstallClick,
                        onHideApp,
                        onPinApp,
                        onUnpinApp,
                        onNicknameClick,
                        onTriggerClick,
                        addToHomeHandler
                ) {
                    { app: AppInfo ->
                        AppActions(
                                onClick = { onAppClick(app) },
                                onAppInfoClick = { onAppInfoClick(app) },
                                onUninstallClick = { onUninstallClick(app) },
                                onHideApp = { onHideApp(app) },
                                onPinApp = { onPinApp(app) },
                                onUnpinApp = { onUnpinApp(app) },
                                onNicknameClick = { onNicknameClick(app) },
                                onTriggerClick = { onTriggerClick(app) },
                                onAddToHome = { addToHomeHandler.addAppToHome(app) },
                        )
                    }
                }

        val createAppState =
                remember(getAppNickname, getAppTrigger, pinnedPackageNames) {
                    { app: AppInfo ->
                        AppState(
                                hasNickname = !getAppNickname(app.packageName).isNullOrBlank(),
                                hasTrigger = getAppTrigger(app.packageName)?.word?.isNotBlank() == true,
                                isPinned = pinnedPackageNames.contains(app.launchCountKey()),
                                showUninstall =
                                        !app.isSystemApp &&
                                                app.userHandleId == null &&
                                                app.packageName != context.packageName,
                                showAppLabel = showAppLabels,
                                isOverlayPresentation = isOverlayPresentation,
                        )
                    }
                }

        val handleDragStart: (AppInfo) -> Unit = handleStart@{ app ->
            if (!reorderPinnedApps) return@handleStart
            val index = displayedApps.indexOfFirst { it.launchCountKey() == app.launchCountKey() }
            if (index >= 0) {
                dragState = PinnedAppDragState(app.launchCountKey(), index)
            }
        }
        val handleDrag: (Float, Float) -> Unit = handleDrag@{ dragX, dragY ->
            if (!reorderPinnedApps) return@handleDrag
            val currentState = dragState ?: return@handleDrag
            val updatedState =
                    currentState.copy(
                            offsetX = currentState.offsetX + dragX,
                            offsetY = currentState.offsetY + dragY,
                    )
            val currentIndex =
                    displayedApps.indexOfFirst { it.launchCountKey() == updatedState.key }
            val targetIndex = targetIndexForDrag(updatedState)
            if (currentIndex >= 0 && targetIndex != currentIndex) {
                val itemHeightPx =
                        measuredItemHeightPx.takeIf { it > 0f } ?: rowItemWidthPx
                val oldCol = currentIndex % columns
                val oldRow = currentIndex / columns
                val newCol = targetIndex % columns
                val newRow = targetIndex / columns
                val layoutShiftX = (newCol - oldCol) * (rowItemWidthPx + spacingPx)
                val layoutShiftY = (newRow - oldRow) * (itemHeightPx + spacingPx)
                movePinnedApp(currentIndex, targetIndex)
                dragState =
                        updatedState.copy(
                                startIndex = targetIndex,
                                offsetX = updatedState.offsetX - layoutShiftX,
                                offsetY = updatedState.offsetY - layoutShiftY,
                        )
            } else {
                dragState = updatedState
            }
        }
        val handleDragEnd: () -> Unit = handleEnd@{
            if (!reorderPinnedApps) return@handleEnd
            dragState = null
            onReorderPinnedApps(displayedApps)
        }

        val gridModifier =
                Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (scrollContainerHeight != null) {
                                base.height(scrollContainerHeight)
                            } else {
                                base
                            }
                        }
        if (scrollContainerHeight != null && !reorderPinnedApps) {
            LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = gridModifier,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
            ) {
                items(
                        items = orderedApps,
                        key = { app -> app.launchCountKey() },
                ) { app ->
                    val appShortcuts = shortcutsByPackage[app.packageName].orEmpty()
                    AppGridItem(
                            modifier = Modifier.fillMaxWidth(),
                            appInfo = app,
                            shortcuts = appShortcuts,
                            appActions = createAppActions(app),
                            appState = createAppState(app),
                            iconPackPackage = iconPackPackage,
                            isPredicted = app.launchCountKey() == predictedAppKey,
                            oneHandedMode = oneHandedMode,
                            appIconSizeStep = appIconSizeStep,
                            appIconShape = appIconShape,
                            themedIconsEnabled = themedIconsEnabled,
                            showWallpaperBackground = showWallpaperBackground,
                            onItemMeasured = { height ->
                                measuredItemHeightPx = height.toFloat()
                            },
                    )
                }
            }
        } else {
            FlowRow(
                    modifier = gridModifier,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
                    maxItemsInEachRow = columns,
            ) {
                orderedApps.forEach { app ->
                    key(app.launchCountKey()) {
                        val appShortcuts = shortcutsByPackage[app.packageName].orEmpty()
                        val isThisDragging = app.launchCountKey() == dragState?.key
                        AppGridItem(
                                modifier = Modifier.width(rowItemWidth),
                                appInfo = app,
                                shortcuts = appShortcuts,
                                appActions = createAppActions(app),
                                appState = createAppState(app),
                                iconPackPackage = iconPackPackage,
                                isPredicted = app.launchCountKey() == predictedAppKey,
                                oneHandedMode = oneHandedMode,
                                appIconSizeStep = appIconSizeStep,
                                appIconShape = appIconShape,
                                themedIconsEnabled = themedIconsEnabled,
                                showWallpaperBackground = showWallpaperBackground,
                                isDragging = isThisDragging,
                                dragOffset =
                                        if (isThisDragging) {
                                            dragState?.let {
                                                IntOffset(it.offsetX.toInt(), it.offsetY.toInt())
                                            }
                                        } else {
                                            null
                                        },
                                onItemMeasured = { height ->
                                    measuredItemHeightPx = height.toFloat()
                                },
                                onPinnedDragStart =
                                        if (reorderPinnedApps) {
                                            { handleDragStart(app) }
                                        } else {
                                            null
                                        },
                                onPinnedDrag = if (reorderPinnedApps) handleDrag else null,
                                onPinnedDragEnd = if (reorderPinnedApps) handleDragEnd else null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
        modifier: Modifier = Modifier,
        appInfo: AppInfo,
        shortcuts: List<StaticShortcut>,
        appActions: AppActions,
        appState: AppState,
        iconPackPackage: String?,
        isPredicted: Boolean = false,
        oneHandedMode: Boolean = false,
        appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
        isDragging: Boolean = false,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
    val indicatorUseLightFill =
            if (showWallpaperBackground && imageBackgroundIsDark != null) {
                imageBackgroundIsDark
            } else {
                LocalAppIsDarkTheme.current
            }
    val primary = MaterialTheme.colorScheme.primary
    val indicatorFillBase =
            if (indicatorUseLightFill) {
                lerp(Color.White, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
            } else {
                lerp(Color.Black, primary, DesignTokens.PredictedSubmitHighlightAccentBlend)
            }
    val iconResult =
            rememberAppIcon(
                    packageName = appInfo.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = appInfo.userHandleId,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            )
    var showOptions by remember { mutableStateOf(false) }
    val appIconSize =
            remember(appState.isOverlayPresentation, appIconSizeStep) {
                val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
                when (
                    if (appState.isOverlayPresentation) {
                        AppIconDisplayMode.OVERLAY
                    } else {
                        AppIconDisplayMode.REGULAR
                    }
                ) {
                    AppIconDisplayMode.OVERLAY -> OverlayAppIconSize * sizeScale
                    AppIconDisplayMode.REGULAR -> RegularAppIconSize * sizeScale
                }
            }
    val appIconSurfaceSize =
            remember(appState.isOverlayPresentation, appIconSizeStep) {
                val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
                if (appState.isOverlayPresentation) {
                    OverlayAppIconSurfaceSize * sizeScale
                } else {
                    DesignTokens.AppIconSize * sizeScale
                }
            }
    val indicatorAlpha = if (isPredicted) 1f else 0f
    var isLocalDragging by remember { mutableStateOf(false) }
    val showDraggedPresentation = isDragging || isLocalDragging
    val dragScale by animateFloatAsState(
            targetValue = if (showDraggedPresentation) DraggedPinnedAppScale else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragScale",
    )
    val dragAlpha by animateFloatAsState(
            targetValue = if (showDraggedPresentation) DraggedPinnedAppAlpha else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragAlpha",
    )
    val currentAppClick by rememberUpdatedState(appActions.onClick)
    val currentPinnedDragStart by rememberUpdatedState(onPinnedDragStart)
    val currentPinnedDrag by rememberUpdatedState(onPinnedDrag)
    val currentPinnedDragEnd by rememberUpdatedState(onPinnedDragEnd)

    val dragModifier =
            if (onPinnedDragStart != null && onPinnedDrag != null && onPinnedDragEnd != null) {
                Modifier.pointerInput(appInfo.launchCountKey()) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress == null) {
                            if (currentEvent.changes.any { it.id == down.id && it.changedToUp() }) {
                                currentEvent.changes.forEach { it.consume() }
                                hapticConfirm(view)()
                                currentAppClick()
                            }
                            return@awaitEachGesture
                        }
                        var overSlop = Offset.Zero
                        val drag =
                                awaitTouchSlopOrCancellation(longPress.id) { change, dragAmount ->
                                    change.consume()
                                    overSlop = dragAmount
                                }
                        if (drag == null) {
                            showOptions = true
                            currentEvent.changes.forEach { it.consume() }
                            return@awaitEachGesture
                        }

                        showOptions = false
                        isLocalDragging = true
                        currentPinnedDragStart?.invoke()
                        try {
                            if (overSlop != Offset.Zero) {
                                currentPinnedDrag?.invoke(overSlop.x, overSlop.y)
                            }
                            drag(drag.id) { change ->
                                val dragAmount = change.positionChange()
                                change.consume()
                                currentPinnedDrag?.invoke(dragAmount.x, dragAmount.y)
                            }
                        } finally {
                            isLocalDragging = false
                            currentPinnedDragEnd?.invoke()
                        }
                    }
                }
            } else {
                Modifier
            }

    Box(
            modifier =
                    modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) {
                                    onItemMeasured(coordinates.size.height)
                                }
                            }
                            .zIndex(if (showDraggedPresentation) 1f else 0f)
                            .graphicsLayer {
                                if (showDraggedPresentation && dragOffset != null) {
                                    translationX = dragOffset.x.toFloat()
                                    translationY = dragOffset.y.toFloat()
                                }
                                scaleX = dragScale
                                scaleY = dragScale
                                alpha = dragAlpha
                            },
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                                indicatorFillBase.copy(
                                        alpha =
                                                TopResultIndicatorBackgroundAlpha * indicatorAlpha,
                                ),
                        shape = DesignTokens.ShapeLarge,
                    )
                    .then(
                        if (showWallpaperBackground) {
                            Modifier.border(
                                width = DesignTokens.BorderWidth,
                                color = indicatorFillBase.copy(alpha = TopResultIndicatorBorderAlpha * indicatorAlpha),
                                shape = DesignTokens.ShapeLarge,
                            )
                        } else Modifier
                    )
                    .padding(
                        top = TopResultIndicatorTopPadding,
                        bottom = TopResultIndicatorBottomPadding,
                        start = if (isPredicted) TopResultIndicatorHorizontalPadding else 0.dp,
                        end = if (isPredicted) TopResultIndicatorHorizontalPadding else 0.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            val isDraggable = onPinnedDragStart != null && onPinnedDrag != null && onPinnedDragEnd != null
            AppIconSurface(
                    iconBitmap = iconResult.bitmap,
                    iconIsLegacy = iconResult.isLegacy,
                    monochromeData = iconResult.monochromeData,
                    appName = appInfo.appName,
                    onClick = { if (!showOptions) appActions.onClick() },
                    onLongClick = if (isDraggable) null else ({ showOptions = true }),
                    gestureModifier = dragModifier,
                    clickGesturesEnabled = !isDraggable,
                    appIconSurfaceSize = appIconSurfaceSize,
                    appIconSize = appIconSize,
                    appIconShape = appIconShape,
                    hasCustomIconPack = iconPackPackage != null,
                    oneHandedMode = oneHandedMode,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = showWallpaperBackground,
            )
            if (appState.showAppLabel) {
                AppLabelText(
                        appName = appInfo.appName,
                        isOverlayPresentation = appState.isOverlayPresentation,
                )
            }
        }

        AppItemDropdownMenu(
                expanded = showOptions,
                onDismiss = { showOptions = false },
                isPinned = appState.isPinned,
                showUninstall = appState.showUninstall,
                hasNickname = appState.hasNickname,
                hasTrigger = appState.hasTrigger,
                shortcuts = shortcuts,
                appInfo = appInfo,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                onShortcutClick = { shortcut -> launchStaticShortcut(context, shortcut) },
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconSurface(
        iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
        iconIsLegacy: Boolean,
        monochromeData: androidx.compose.ui.graphics.ImageBitmap? = null,
        appName: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?,
        gestureModifier: Modifier = Modifier,
        clickGesturesEnabled: Boolean = true,
        appIconSurfaceSize: Dp = DesignTokens.AppIconSize,
        appIconSize: Dp,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        hasCustomIconPack: Boolean = false,
        oneHandedMode: Boolean = false,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = LocalAppIsDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val useLightWallpaperShadow = showWallpaperBackground && !isDarkTheme
    val showThemedIcon = themedIconsEnabled && !hasCustomIconPack &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    val useSystemMaterialYouTones =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    (LocalDeviceDynamicColorsActive.current || LocalIsSystemWallpaperActive.current)
    val useWallpaperDerivedAccent = LocalWallpaperDynamicAccentActive.current
    val useCustomThemeDarkThemedIconColors =
            isDarkTheme && !useSystemMaterialYouTones && !useWallpaperDerivedAccent
    val themedIconBackground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent2_800
                                } else {
                                    android.R.color.system_accent1_100
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.secondaryContainer
            } else {
                colorScheme.primaryContainer
            }
    val themedIconForeground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent1_200
                                } else {
                                    android.R.color.system_accent1_600
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.onSecondaryContainer
            } else {
                colorScheme.primary
            }
    val themedIconContainerShape = CircleShape

    Surface(
            modifier = Modifier.requiredSize(appIconSurfaceSize).then(gestureModifier),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = DesignTokens.ShapeLarge,
    ) {
        val clickModifier =
                if (!clickGesturesEnabled) {
                    Modifier
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                            onClick = {
                                hapticConfirm(view)()
                                onClick()
                            },
                            onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable {
                        hapticConfirm(view)()
                        onClick()
                    }
                }
        Box(
                modifier = Modifier.fillMaxSize().then(clickModifier),
                contentAlignment = Alignment.Center,
        ) {
            if (showThemedIcon && monochromeData != null) {
                Box(
                        modifier = Modifier
                                .then(
                                        if (useLightWallpaperShadow) {
                                            Modifier.shadow(
                                                    elevation = DesignTokens.ElevationLevel2,
                                                    shape = themedIconContainerShape,
                                                    ambientColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                            ),
                                                    spotColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                            ),
                                            )
                                        } else {
                                            Modifier
                                        },
                                )
                                .size(appIconSize)
                                .clip(themedIconContainerShape)
                                .background(themedIconBackground),
                        contentAlignment = Alignment.Center,
                ) {
                    Image(
                            bitmap = monochromeData,
                            contentDescription = stringResource(R.string.desc_launch_app, appName),
                            modifier = Modifier.requiredSize(appIconSize * ThemedMonochromeGlyphScale),
                            colorFilter = ColorFilter.tint(themedIconForeground),
                    )
                }
            } else if (iconBitmap != null) {
                val isUnsupportedThemedIcon = showThemedIcon && monochromeData == null
                if (isUnsupportedThemedIcon) {
                    Box(
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = themedIconContainerShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                                            .size(appIconSize)
                                            .clip(themedIconContainerShape)
                                            .background(themedIconBackground),
                            contentAlignment = Alignment.Center,
                    ) {
                        Image(
                                bitmap = iconBitmap,
                                contentDescription = stringResource(R.string.desc_launch_app, appName),
                                modifier = Modifier.requiredSize(appIconSize * UnsupportedThemedIconGlyphScale),
                                colorFilter = ColorFilter.tint(
                                        themedIconForeground.copy(alpha = UnsupportedThemedIconGlyphAlpha),
                                        BlendMode.SrcAtop,
                                ),
                        )
                    }
                } else {
                    val clipModifier =
                            when {
                                appIconShape == AppIconShape.CIRCLE ->
                                        Modifier.clip(CircleShape)
                                iconIsLegacy -> Modifier.clip(DesignTokens.ShapeLarge)
                                else -> Modifier
                            }
                    val bitmapShadowShape =
                            when {
                                appIconShape == AppIconShape.CIRCLE -> CircleShape
                                iconIsLegacy -> DesignTokens.ShapeLarge
                                else -> DesignTokens.ShapeLarge
                            }
                    Image(
                            bitmap = iconBitmap,
                            contentDescription =
                                    stringResource(
                                            R.string.desc_launch_app,
                                            appName,
                                    ),
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = bitmapShadowShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                                            .size(appIconSize)
                                            .then(clipModifier),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
) {
    val imageBackgroundIsDark = LocalImageBackgroundIsDark.current
    val labelColor = when (imageBackgroundIsDark) {
        true -> Color.White
        false -> Color.Black
        null -> MaterialTheme.colorScheme.onSurface
    }
    Spacer(
            modifier =
                    Modifier.height(
                            if (isOverlayPresentation) {
                                4.dp
                            } else {
                                DesignTokens.SpacingXSmall
                            },
                    ),
    )
    Text(
            text = appName,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}
