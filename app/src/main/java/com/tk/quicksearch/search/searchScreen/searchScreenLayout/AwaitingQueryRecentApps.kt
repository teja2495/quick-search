package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.searchScreen.AppsSectionParams

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
