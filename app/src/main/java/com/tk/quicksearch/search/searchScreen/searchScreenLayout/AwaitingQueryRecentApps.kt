package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.shared.ui.theme.AppMotion

/**
 * One side of the swap between Home and the recents grid. The content stays composed either way, so
 * swapping on the keyboard's first frame only animates a layer; hidden, it is neither measured,
 * placed, drawn nor touchable. The outgoing side fades out quickly on a linear curve; the incoming one
 * fades in just behind it while drifting [enterOffset] into place, carrying on the keyboard's motion.
 * Opacity stays linear: eased fades hold, then pop, which reads as a stutter at these durations.
 */
@Composable
internal fun AwaitingSwapSlot(
    visible: Boolean,
    enterOffset: Dp,
    /** Switch without fading, such as when the query changes what the slot shows. */
    snap: Boolean = false,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    var entering by remember { mutableStateOf(false) }
    val currentSnap by rememberUpdatedState(snap)
    LaunchedEffect(visible) {
        entering = visible
        val target = if (visible) 1f else 0f
        when {
            currentSnap -> progress.snapTo(target)
            visible -> progress.animateTo(target, SwapEnter)
            else -> progress.animateTo(target, SwapExit)
        }
    }
    Box(
        Modifier
            .layout { measurable, constraints ->
                if (progress.value == 0f) {
                    layout(0, 0) {}
                } else {
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
            .graphicsLayer {
                val p = progress.value
                alpha = p
                translationY =
                    if (entering) (1f - AppMotion.EmphasizedEasing.transform(p)) * enterOffset.toPx() else 0f
            },
    ) { content() }
}

private val SwapExit = tween<Float>(AppMotion.DurationQuick, easing = LinearEasing)

private val SwapEnter = tween<Float>(AppMotion.DurationStandard, delayMillis = AppMotion.DurationMicro, easing = LinearEasing)

private val RecentsOnlyTabs = setOf(AppSuggestionTabType.RECENTS)

/**
 * Usage access gives a real recents list; without it there is nothing worth showing here.
 * Independent of the Home suggestion tab settings: this is its own surface, not the Recents tab.
 */
internal fun shouldShowAwaitingQueryRecentApps(appsParams: AppsSectionParams): Boolean =
    appsParams.hasUsagePermission && appsParams.pinnedAndRecentApps.isNotEmpty()

/**
 * The app grid shown in place of Home while the keyboard is open with an empty query and
 * Open Keyboard off: just the Recents tab, with no tab strip, pinned items or folders.
 */
@Composable
internal fun AwaitingQueryRecentApps(
    appsParams: AppsSectionParams,
    modifier: Modifier = Modifier,
) {
    val recentApps = appsParams.pinnedAndRecentApps
    AppGridView(
        apps = recentApps,
        allApps = appsParams.allApps,
        pinnedAndRecentApps = recentApps,
        pinnedApps = emptyList(),
        newOrUpdatedApps = emptyList(),
        mostUsedApps = emptyList(),
        appShortcuts = appsParams.appShortcuts,
        isSearching = false,
        hasUsagePermission = appsParams.hasUsagePermission,
        selectedSuggestionTab = AppSuggestionTabType.RECENTS,
        enabledSuggestionTabs = RecentsOnlyTabs,
        // Keep the user's saved Home tab untouched.
        onSuggestionTabSelected = {},
        showAllAppsButton = false,
        onAppClick = appsParams.onAppClick,
        onAppShortcutClick = appsParams.onAppShortcutClick,
        onAppInfoClick = appsParams.onAppInfoClick,
        onUninstallClick = appsParams.onUninstallClick,
        onHideApp = appsParams.onHideApp,
        onDisableAppShortcut = appsParams.onDisableAppShortcut,
        onPinApp = appsParams.onPinApp,
        onUnpinApp = appsParams.onUnpinApp,
        onReorderPinnedApps = appsParams.onReorderPinnedApps,
        onNicknameClick = appsParams.onNicknameClick,
        onTriggerClick = appsParams.onTriggerClick,
        onOpenInSplitScreen = appsParams.onOpenInSplitScreen,
        getAppNickname = appsParams.getAppNickname,
        getAppTrigger = appsParams.getAppTrigger,
        pinnedPackageNames = appsParams.pinnedPackageNames,
        disabledShortcutIds = appsParams.disabledAppShortcutIds,
        modifier = modifier.fillMaxWidth(),
        rowCount = appsParams.rowCount,
        phoneColumnOverride = appsParams.phoneColumnOverride,
        appIconSizeStep = appsParams.appIconSizeStep,
        iconPackPackage = appsParams.iconPackPackage,
        appIconShape = appsParams.appIconShape,
        themedIconsEnabled = appsParams.themedIconsEnabled,
        showAppLabels = appsParams.showAppLabels,
        oneHandedMode = appsParams.oneHandedMode,
        isOverlayPresentation = appsParams.isOverlayPresentation,
        predictedTarget = null,
        suppressTopResultIndicator = true,
        showWallpaperBackground = appsParams.showWallpaperBackground,
        notificationDotsEnabled = appsParams.notificationDotsEnabled,
        notificationCountsEnabled = appsParams.notificationCountsEnabled,
        suppressSuggestionsEnterAnimation = true,
    )
}
