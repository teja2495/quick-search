package com.tk.quicksearch.search.apps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.HomeHorizontalSwipe
import com.tk.quicksearch.search.searchScreen.LocalHomeHorizontalSwipeHandler
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.search.folders.AppFolder
import com.tk.quicksearch.search.folders.AppFolderMember
import com.tk.quicksearch.search.folders.AppGridFolderActions
import com.tk.quicksearch.search.folders.FolderContentsPopup
import com.tk.quicksearch.search.folders.appFolderMemberKey
import com.tk.quicksearch.search.folders.ResolvedAppFolder
import com.tk.quicksearch.search.folders.resolveAppFolders
import kotlin.math.min
import java.util.Locale

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
        showAllAppsButton: Boolean,
        onAppClick: (AppInfo) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onDisableAppShortcut: (StaticShortcut) -> Unit = {},
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onReorderPinnedApps: (List<AppInfo>) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        onOpenInSplitScreen: (AppInfo) -> Unit,
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
        notificationDotsEnabled: Boolean = false,
        onGridAppeared: (() -> Unit)? = null,
        suppressSuggestionsEnterAnimation: Boolean = false,
        pinnedGridShortcuts: List<StaticShortcut> = emptyList(),
        pinnedAppGridOrder: List<String> = emptyList(),
        onReorderPinnedAppGrid: (List<String>, List<AppInfo>, List<StaticShortcut>) -> Unit =
                { _, _, _ -> },
        pinnedGridShortcutActions: AppGridShortcutActions? = null,
        appFolders: List<AppFolder> = emptyList(),
        folderActions: AppGridFolderActions? = null,
) {
    val notificationDotKeys = rememberNotificationDotKeys(notificationDotsEnabled)
    val context = LocalContext.current
    LaunchedEffect(notificationDotsEnabled) {
        if (notificationDotsEnabled) {
            NotificationDotsPermission.requestRebind(context)
        }
    }
    val onHomeHorizontalSwipe = LocalHomeHorizontalSwipeHandler.current
    val pinnedTitle = stringResource(R.string.app_suggestions_tab_pinned)
    val recentsTitle = stringResource(R.string.app_suggestions_tab_recent)
    val newUpdatedTitle = stringResource(R.string.app_suggestions_tab_new_updated)
    val mostUsedTitle = stringResource(R.string.common_most_used)
    val allAppsTitle = stringResource(R.string.settings_app_shortcuts_filter_all_apps)
    val gridColumns = getAppGridColumns(phoneColumnOverride)
    val suggestionSlotCount = (rowCount * gridColumns).coerceAtLeast(1)
    val alphabeticalApps =
            remember(allApps) {
                allApps.sortedWith(
                        compareBy<AppInfo> { it.appName.lowercase(Locale.getDefault()) }
                                .thenBy { it.packageName.lowercase(Locale.getDefault()) }
                                .thenBy { it.userHandleId ?: Int.MIN_VALUE },
                )
            }
    // Folders only live in the Pinned tab, which can't be turned off while suggestions are on.
    val pinnedFolders =
            remember(appFolders, allApps, appShortcuts, disabledShortcutIds, isSearching) {
                if (isSearching) {
                    emptyList()
                } else {
                    resolveAppFolders(appFolders, allApps, appShortcuts, disabledShortcutIds)
                }
            }
    // Members are unpinned when moved into a folder; hiding them here too avoids a flash of their
    // old tiles until the pinned lists refresh.
    val folderMemberKeys =
            remember(pinnedFolders) {
                pinnedFolders.flatMapTo(HashSet()) { folder -> folder.members.map { it.memberKey } }
            }
    val folderByMemberKey =
            remember(pinnedFolders) {
                buildMap {
                    pinnedFolders.forEach { folder ->
                        folder.members.forEach { member -> put(member.memberKey, folder) }
                    }
                }
            }
    val effectivePinnedPackageNames =
            remember(pinnedPackageNames, pinnedFolders) {
                pinnedPackageNames +
                        pinnedFolders.flatMap { folder ->
                            folder.members.mapNotNull { member ->
                                (member as? AppFolderMember.App)?.app?.launchCountKey()
                            }
                        }
            }
    val hasPinnedGridItems =
            pinnedApps.isNotEmpty() || pinnedGridShortcuts.isNotEmpty() || pinnedFolders.isNotEmpty()
    val suggestionTabs =
            remember(
                    hasUsagePermission,
                    isSearching,
                    newUpdatedTitle,
                    pinnedTitle,
                    recentsTitle,
                    mostUsedTitle,
                    pinnedApps,
                    pinnedGridShortcuts,
                    hasPinnedGridItems,
                    newOrUpdatedApps,
                    pinnedAndRecentApps,
                    mostUsedApps,
                    enabledSuggestionTabs,
                    suggestionSlotCount,
            ) {
                if (isSearching) return@remember emptyList()
                val recentsApps =
                    if (AppSuggestionTabType.PINNED !in enabledSuggestionTabs) {
                        replaceSuggestionAppsWithPinned(
                                pinnedApps = pinnedApps,
                                suggestedApps = pinnedAndRecentApps,
                                slotCount = suggestionSlotCount,
                        )
                    } else {
                        pinnedAndRecentApps
                    }
                val mostUsedAppsWithPinned =
                    if (
                        AppSuggestionTabType.PINNED !in enabledSuggestionTabs &&
                            AppSuggestionTabType.RECENTS !in enabledSuggestionTabs
                    ) {
                        replaceSuggestionAppsWithPinned(
                                pinnedApps = pinnedApps,
                                suggestedApps = mostUsedApps,
                                slotCount = suggestionSlotCount,
                        )
                    } else {
                        mostUsedApps
                    }
                if (hasUsagePermission) {
                    buildList {
                        if (AppSuggestionTabType.NEW_UPDATED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.NEW_UPDATED, newUpdatedTitle, newOrUpdatedApps))
                        }
                        if (
                            hasPinnedGridItems &&
                                AppSuggestionTabType.PINNED in enabledSuggestionTabs
                        ) {
                            add(AppSuggestionTab(AppSuggestionTabType.PINNED, pinnedTitle, pinnedApps))
                        }
                        if (AppSuggestionTabType.RECENTS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.RECENTS, recentsTitle, recentsApps))
                        }
                        if (AppSuggestionTabType.MOST_USED in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.MOST_USED, mostUsedTitle, mostUsedAppsWithPinned))
                        }
                    }
                } else {
                    buildList {
                        if (
                            hasPinnedGridItems &&
                                AppSuggestionTabType.PINNED in enabledSuggestionTabs
                        ) {
                            add(AppSuggestionTab(AppSuggestionTabType.PINNED, pinnedTitle, pinnedApps))
                        }
                        if (AppSuggestionTabType.RECENTS in enabledSuggestionTabs) {
                            add(AppSuggestionTab(AppSuggestionTabType.RECENTS, recentsTitle, recentsApps))
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
    val minSuggestionGridItems = suggestionSlotCount
    val suggestionFallbackApps = remember(pinnedAndRecentApps, apps) { pinnedAndRecentApps + apps }
    val selectedSuggestionTabType = suggestionTabs.getOrNull(selectedSuggestionTabIndex)?.type
    val selectedSuggestionTabItem = suggestionTabs.getOrNull(selectedSuggestionTabIndex)
    val activeApps =
            if (selectedSuggestionTabItem != null) {
                val selectedTab = selectedSuggestionTabItem
                if (selectedTab.type == AppSuggestionTabType.PINNED) {
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
    // Pinned shortcuts sit right after the pinned apps: in the Pinned tab, or in the tab the
    // pinned apps are merged into when that tab is off.
    val pinnedShortcutsTabType =
            when {
                AppSuggestionTabType.PINNED in enabledSuggestionTabs -> AppSuggestionTabType.PINNED
                AppSuggestionTabType.RECENTS in enabledSuggestionTabs -> AppSuggestionTabType.RECENTS
                else -> AppSuggestionTabType.MOST_USED
            }
    // Interleaves pinned apps, shortcuts and (in the Pinned tab) folders by the saved grid order
    // while keeping the apps' and shortcuts' own pinned orders authoritative, so reorders made
    // elsewhere still apply. Folders have no order of their own, so the grid order places them.
    // The Pinned tab also keeps the empty cells left by drags into empty space, and fills its last
    // row with empty cells so items can be dropped there.
    fun orderedPinnedItems(pinned: List<AppInfo>, includeFolders: Boolean): List<AppGridEntry> {
        val appEntries =
                pinned
                        .filterNot { includeFolders && appFolderMemberKey(it) in folderMemberKeys }
                        .map { AppGridEntry.App(it) }
        val shortcutEntries =
                pinnedGridShortcuts
                        .filterNot { includeFolders && appFolderMemberKey(it) in folderMemberKeys }
                        .map { AppGridEntry.Shortcut(it) }
        val rank = pinnedAppGridOrder.withIndex().associate { (index, key) -> key to index }
        val folderEntries =
                if (includeFolders) {
                    pinnedFolders
                            .map { AppGridEntry.Folder(it) }
                            .sortedBy { rank[it.key] ?: Int.MAX_VALUE }
                } else {
                    emptyList()
                }
        val gapEntries =
                if (includeFolders) {
                    gapKeysBeforeLastItem(
                                    pinnedAppGridOrder,
                                    (appEntries + shortcutEntries + folderEntries)
                                            .mapTo(HashSet()) { it.key },
                            )
                            .map { AppGridEntry.Gap(it) }
                } else {
                    emptyList()
                }
        if (shortcutEntries.isEmpty() && folderEntries.isEmpty() && gapEntries.isEmpty()) {
            return appEntries
        }
        // Items not in the grid order yet, such as new pins, fill empty cells before new rows.
        val (placedEntries, newEntries) =
                (appEntries + shortcutEntries + folderEntries + gapEntries).partition {
                    it.key in rank
                }
        val appIterator = placedEntries.filterIsInstance<AppGridEntry.App>().iterator()
        val shortcutIterator = placedEntries.filterIsInstance<AppGridEntry.Shortcut>().iterator()
        val folderIterator = placedEntries.filterIsInstance<AppGridEntry.Folder>().iterator()
        val placedInGridOrder =
                placedEntries
                        .sortedBy { rank.getValue(it.key) }
                        .map { entry ->
                            when (entry) {
                                is AppGridEntry.App -> appIterator.next()
                                is AppGridEntry.Shortcut -> shortcutIterator.next()
                                is AppGridEntry.Folder -> folderIterator.next()
                                is AppGridEntry.Gap -> entry
                            }
                        }
        var gapIndex = 0
        return itemsFillingGaps(
                        ordered = placedInGridOrder,
                        added = newEntries,
                        isGap = { it is AppGridEntry.Gap },
                )
                // Renumbered so the gaps added to fill the last row get unused keys.
                .map { if (it is AppGridEntry.Gap) AppGridEntry.Gap(pinnedGridGapKey(gapIndex++)) else it }
    }
    fun orderedPinnedEntries(pinned: List<AppInfo>, includeFolders: Boolean): List<AppGridEntry> {
        if (!includeFolders) return orderedPinnedItems(pinned, includeFolders = false)
        return itemsFilledToGridRows(
                items = orderedPinnedItems(pinned, includeFolders = true),
                columns = gridColumns,
                isGap = { it is AppGridEntry.Gap },
                gap = { AppGridEntry.Gap(pinnedGridGapKey(it)) },
        )
    }
    fun gridEntriesFor(tabType: AppSuggestionTabType?, tabApps: List<AppInfo>): List<AppGridEntry> {
        if (tabType == AppSuggestionTabType.PINNED) {
            return orderedPinnedEntries(tabApps, includeFolders = true)
        }
        if (tabType != pinnedShortcutsTabType || pinnedGridShortcuts.isEmpty()) {
            return tabApps.map { AppGridEntry.App(it) }
        }
        val leadingPinnedCount = min(pinnedApps.size, suggestionSlotCount)
        val appSlots = (suggestionSlotCount - pinnedGridShortcuts.size).coerceAtLeast(leadingPinnedCount)
        val limitedApps = tabApps.take(appSlots)
        return orderedPinnedEntries(limitedApps.take(leadingPinnedCount), includeFolders = false) +
                limitedApps.drop(leadingPinnedCount).map { AppGridEntry.App(it) }
    }
    val pinnedTabOrderKeys: () -> List<String> = {
        orderedPinnedEntries(pinnedApps, includeFolders = true).map { it.key }
    }
    val onUnpinAppIncludingFolders: (AppInfo) -> Unit = { app ->
        val memberKey = appFolderMemberKey(app)
        val folder = folderByMemberKey[memberKey]
        if (folder != null && folderActions != null) {
            folderActions.onUnpinFromFolder(folder.id, memberKey, pinnedTabOrderKeys())
        } else {
            onUnpinApp(app)
        }
    }
    val onHideAppIncludingFolders: (AppInfo) -> Unit = { app ->
        val memberKey = appFolderMemberKey(app)
        folderByMemberKey[memberKey]?.let { folder ->
            folderActions?.onUnpinFromFolder(folder.id, memberKey, pinnedTabOrderKeys())
        }
        onHideApp(app)
    }
    val onReorderPinnedEntries: (List<AppGridEntry>) -> Unit = { displayed ->
        val entries = displayed.dropLastWhile { it is AppGridEntry.Gap }
        val reorderedApps = entries.filterIsInstance<AppGridEntry.App>().map { it.app }
        // Gaps live only in the grid order, which also has to drop gaps no longer shown.
        val hasGaps =
                entries.any { it is AppGridEntry.Gap } ||
                        pinnedAppGridOrder.any(::isPinnedGridGapKey)
        if (pinnedGridShortcuts.isEmpty() && pinnedFolders.isEmpty() && !hasGaps) {
            onReorderPinnedApps(reorderedApps)
        } else {
            onReorderPinnedAppGrid(
                    entries.map { it.key },
                    reorderedApps,
                    entries.filterIsInstance<AppGridEntry.Shortcut>().map { it.shortcut },
            )
        }
    }
    val activeGridEntries = gridEntriesFor(selectedSuggestionTabItem?.type, activeApps)
    var openFolderId by remember { mutableStateOf<String?>(null) }
    val onMergePinnedEntries: ((AppGridEntry, AppGridEntry, List<AppGridEntry>) -> Unit)? =
            folderActions?.let { actions ->
                { dragged, target, entries ->
                    val orderKeys = entries.map { it.key }
                    if (target is AppGridEntry.Folder) {
                        actions.onAddToFolder(target.folder.id, dragged.key, orderKeys)
                    } else {
                        actions.onCreateFolder(target.key, dragged.key, orderKeys)
                    }
                }
            }
    val onDeletePinnedFolder: (ResolvedAppFolder) -> Unit = { folder ->
        folderActions?.onDeleteFolder?.invoke(folder.id, pinnedTabOrderKeys())
    }
    val shortcutsByPackage =
            remember(appShortcuts, disabledShortcutIds) {
                appShortcuts
                        .asSequence()
                        .filterNot { shortcut ->
                            isShortcutDisabled(shortcut, disabledShortcutIds)
                        }
                        .groupBy { it.packageName }
            }
    // Fade the suggestions grid (empty query) in when it first appears. It stays in its final
    // position throughout so nothing slides. Search results appear immediately without animation.
    val initialSuggestionsAlpha = if (suppressSuggestionsEnterAnimation) 1f else 0f
    val suggestionsAlpha = remember { Animatable(initialSuggestionsAlpha) }
    val tabSwipeModifier =
            if (suggestionTabs.size > 1) {
                Modifier.pointerInput(suggestionTabs, selectedSuggestionTabIndex, onHomeHorizontalSwipe) {
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
                                    dragAmount > SuggestionTabSwipeThresholdPx ->
                                            onHomeHorizontalSwipe(HomeHorizontalSwipe.RIGHT)
                                    dragAmount < -SuggestionTabSwipeThresholdPx &&
                                            selectedSuggestionTabIndex < suggestionTabs.lastIndex ->
                                            selectSuggestionTab(selectedSuggestionTabIndex + 1)
                                    dragAmount < -SuggestionTabSwipeThresholdPx ->
                                            onHomeHorizontalSwipe(HomeHorizontalSwipe.LEFT)
                                }
                                dragAmount = 0f
                            },
                            onDragCancel = { dragAmount = 0f },
                    )
                }
            } else {
                Modifier
            }
    var showAllAppsDialog by remember { mutableStateOf(false) }
    val shouldShowAllAppsButton = showAllAppsButton && !isSearching && allApps.isNotEmpty()
    Column(
            modifier = modifier.fillMaxWidth().then(tabSwipeModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
    ) {
        val showAppGrid = activeGridEntries.isNotEmpty()

        LaunchedEffect(showAppGrid, isSearching) {
            if (!showAppGrid) return@LaunchedEffect
            if (!isSearching) {
                StartupTrace.mark("QS.Home.AppGridComposed")
                if (showAppLabels) {
                    StartupTrace.mark("QS.Home.AppLabelsComposed")
                }
            }
            if (isSearching || suppressSuggestionsEnterAnimation) {
                suggestionsAlpha.snapTo(1f)
            } else if (suggestionsAlpha.value < 1f) {
                suggestionsAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = SuggestionsEnterDurationMillis),
                )
            }
            if (!isSearching) {
                StartupTrace.mark("QS.Home.AppGridInteractive")
            }
            onGridAppeared?.invoke()
        }

        if (showAppGrid) {
            val suggestionsContentModifier = Modifier.graphicsLayer {
                alpha = suggestionsAlpha.value
            }
            Column(
                    modifier = suggestionsContentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
            ) {
                if (suggestionTabs.size > 1) {
                    AppSuggestionTabStrip(
                            tabs = suggestionTabs,
                            selectedIndex = selectedSuggestionTabIndex,
                            onSelectedIndexChange = ::selectSuggestionTab,
                    )
                }
                if (suggestionTabs.size > 1 && !isSearching) {
                    AnimatedContent(
                            targetState = requireNotNull(selectedSuggestionTabItem),
                            contentKey = { it.type },
                            transitionSpec = {
                                val movingForward = targetState.type.ordinal > initialState.type.ordinal
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
                    ) { selectedTab ->
                        val tabGridEntries =
                                gridEntriesFor(
                                        selectedTab.type,
                                        if (selectedTab.type == AppSuggestionTabType.PINNED) {
                                            selectedTab.apps
                                        } else {
                                            fillSuggestionGridApps(
                                                    primaryApps = selectedTab.apps,
                                                    fallbackApps = suggestionFallbackApps,
                                                    minItems = minSuggestionGridItems,
                                            )
                                        },
                                )
                        AppGrid(
                                entries = tabGridEntries,
                                shortcutActions = pinnedGridShortcutActions,
                                isSearching = isSearching,
                                onAppClick = onAppClick,
                                onAppShortcutClick = onAppShortcutClick,
                                onAppInfoClick = onAppInfoClick,
                                onUninstallClick = onUninstallClick,
                                onHideApp = onHideAppIncludingFolders,
                                onDisableAppShortcut = onDisableAppShortcut,
                                onPinApp = onPinApp,
                                onUnpinApp = onUnpinAppIncludingFolders,
                                onReorderPinnedEntries = onReorderPinnedEntries,
                                onNicknameClick = onNicknameClick,
                                onTriggerClick = onTriggerClick,
                                onOpenInSplitScreen = onOpenInSplitScreen,
                                getAppNickname = getAppNickname,
                                getAppTrigger = getAppTrigger,
                                pinnedPackageNames = effectivePinnedPackageNames,
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
                                notificationDotKeys = notificationDotKeys,
                                showPinnedIndicators =
                                        AppSuggestionTabType.PINNED !in enabledSuggestionTabs,
                                reorderPinnedApps = selectedTab.type == AppSuggestionTabType.PINNED,
                                onMergeEntries = onMergePinnedEntries,
                                onFolderClick = { openFolderId = it.id },
                                onDeleteFolder = onDeletePinnedFolder,
                        )
                    }
                } else {
                    AppGrid(
                            entries = activeGridEntries,
                            shortcutActions = pinnedGridShortcutActions,
                            isSearching = isSearching,
                            onAppClick = onAppClick,
                            onAppShortcutClick = onAppShortcutClick,
                            onAppInfoClick = onAppInfoClick,
                            onUninstallClick = onUninstallClick,
                            onHideApp = onHideAppIncludingFolders,
                            onDisableAppShortcut = onDisableAppShortcut,
                            onPinApp = onPinApp,
                            onUnpinApp = onUnpinAppIncludingFolders,
                            onReorderPinnedEntries = onReorderPinnedEntries,
                            onNicknameClick = onNicknameClick,
                            onTriggerClick = onTriggerClick,
                            onOpenInSplitScreen = onOpenInSplitScreen,
                            getAppNickname = getAppNickname,
                            getAppTrigger = getAppTrigger,
                            pinnedPackageNames = effectivePinnedPackageNames,
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
                            notificationDotKeys = notificationDotKeys,
                            showPinnedIndicators =
                                    !isSearching &&
                                            AppSuggestionTabType.PINNED !in enabledSuggestionTabs,
                            reorderPinnedApps =
                                    selectedSuggestionTabType == AppSuggestionTabType.PINNED,
                            onMergeEntries = onMergePinnedEntries,
                            onFolderClick = { openFolderId = it.id },
                            onDeleteFolder = onDeletePinnedFolder,
                    )
                }
                if (shouldShowAllAppsButton) {
                    ExpandButton(
                            onClick = { showAllAppsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            textResId = R.string.settings_app_shortcuts_filter_all_apps,
                            usePillBackground = true,
                            showWallpaperBackground = showWallpaperBackground,
                            icon = Icons.Rounded.ChevronRight,
                    )
                }
            }
        } else if (shouldShowAllAppsButton) {
            ExpandButton(
                    onClick = { showAllAppsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    textResId = R.string.settings_app_shortcuts_filter_all_apps,
                    usePillBackground = true,
                    showWallpaperBackground = showWallpaperBackground,
                    icon = Icons.Rounded.ChevronRight,
            )
        }
    }

    val openFolder = openFolderId?.let { id -> pinnedFolders.firstOrNull { it.id == id } }
    LaunchedEffect(openFolder == null) {
        // Closed for good once the folder is gone, e.g. after its last item is removed.
        if (openFolder == null) openFolderId = null
    }
    if (openFolder != null && folderActions != null) {
        FolderContentsPopup(
                folder = openFolder,
                onRename = { name -> folderActions.onRenameFolder(openFolder.id, name) },
                onDismiss = { openFolderId = null },
        ) { removeZone, dismiss ->
            val memberEntries =
                    remember(openFolder.members) {
                        openFolder.members.map { member ->
                            when (member) {
                                is AppFolderMember.App -> AppGridEntry.App(member.app)
                                is AppFolderMember.Shortcut -> AppGridEntry.Shortcut(member.shortcut)
                            }
                        }
                    }
            // Members count as pinned: their menus offer "Unpin", which takes them out of the
            // folder and the Pinned tab.
            val memberAppKeys =
                    remember(openFolder.members) {
                        openFolder.members.mapNotNullTo(HashSet()) { member ->
                            (member as? AppFolderMember.App)?.app?.launchCountKey()
                        }
                    }
            val unpinMember: (String) -> Unit = { memberKey ->
                folderActions.onUnpinFromFolder(openFolder.id, memberKey, pinnedTabOrderKeys())
            }
            AppGrid(
                    entries = memberEntries,
                    shortcutActions =
                            pinnedGridShortcutActions?.copy(
                                    onTogglePin = { shortcut ->
                                        unpinMember(appFolderMemberKey(shortcut))
                                    },
                            ),
                    isSearching = false,
                    onAppClick = { app ->
                        dismiss()
                        onAppClick(app)
                    },
                    onAppShortcutClick = { shortcut ->
                        dismiss()
                        onAppShortcutClick(shortcut)
                    },
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick,
                    onHideApp = onHideAppIncludingFolders,
                    onDisableAppShortcut = onDisableAppShortcut,
                    onPinApp = onPinApp,
                    onUnpinApp = { app -> unpinMember(appFolderMemberKey(app)) },
                    onReorderPinnedEntries = { entries ->
                        folderActions.onReorderFolder(
                                openFolder.id,
                                entries.mapNotNull { it.folderMemberKey() },
                        )
                    },
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    onOpenInSplitScreen = onOpenInSplitScreen,
                    getAppNickname = getAppNickname,
                    getAppTrigger = getAppTrigger,
                    pinnedPackageNames = memberAppKeys,
                    shortcutsByPackage = shortcutsByPackage,
                    phoneColumnOverride = phoneColumnOverride,
                    appIconSizeStep = appIconSizeStep,
                    iconPackPackage = iconPackPackage,
                    showAppLabels = true,
                    oneHandedMode = false,
                    isOverlayPresentation = false,
                    predictedTarget = null,
                    suppressTopResultIndicator = true,
                    appIconShape = appIconShape,
                    themedIconsEnabled = themedIconsEnabled,
                    notificationDotKeys = notificationDotKeys,
                    reorderPinnedApps = true,
                    // Dropping a member on the popup's remove zone takes it out of the folder.
                    // The popup stays open unless that empties the folder.
                    dropTarget =
                            AppGridDropTarget(
                                    contains = removeZone::contains,
                                    onActiveChange = { removeZone.isActive = it },
                                    onHoverChange = { removeZone.isHovered = it },
                                    onDrop = { entry ->
                                        entry.folderMemberKey()?.let { memberKey ->
                                            folderActions.onRemoveFromFolder(
                                                    openFolder.id,
                                                    memberKey,
                                                    pinnedTabOrderKeys(),
                                            )
                                        }
                                    },
                            ),
            )
        }
    }

    if (showAllAppsDialog) {
        AllAppsDialog(
                title = allAppsTitle,
                apps = alphabeticalApps,
                onDismiss = { showAllAppsDialog = false },
                onAppClick = { app ->
                    showAllAppsDialog = false
                    onAppClick(app)
                },
                onAppShortcutClick = onAppShortcutClick,
                onAppInfoClick = onAppInfoClick,
                onUninstallClick = onUninstallClick,
                onHideApp = onHideAppIncludingFolders,
                onDisableAppShortcut = onDisableAppShortcut,
                onPinApp = onPinApp,
                onUnpinApp = onUnpinAppIncludingFolders,
                onNicknameClick = onNicknameClick,
                onTriggerClick = onTriggerClick,
                onOpenInSplitScreen = onOpenInSplitScreen,
                getAppNickname = getAppNickname,
                getAppTrigger = getAppTrigger,
                pinnedPackageNames = effectivePinnedPackageNames,
                shortcutsByPackage = shortcutsByPackage,
                phoneColumnOverride = phoneColumnOverride,
                appIconSizeStep = appIconSizeStep,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                notificationDotKeys = notificationDotKeys,
        )
    }
}
