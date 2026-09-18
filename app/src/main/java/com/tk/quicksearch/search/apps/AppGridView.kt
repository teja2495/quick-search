package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.apps.swipeGestures.appSwipeGestures
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.StartupPhase
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.HomeHorizontalSwipe
import com.tk.quicksearch.search.searchScreen.LocalHomeHorizontalSwipeHandler
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.rememberPredictedSubmitIndicatorAlpha
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.ui.theme.LocalIsSystemWallpaperActive
import com.tk.quicksearch.shared.ui.theme.LocalWallpaperDynamicAccentActive
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.search.folders.AppFolder
import com.tk.quicksearch.search.folders.AppFolderMember
import com.tk.quicksearch.search.folders.AppGridFolderActions
import com.tk.quicksearch.search.folders.FolderContentsPopup
import com.tk.quicksearch.search.folders.FolderGridItem
import com.tk.quicksearch.search.folders.appFolderMemberKey
import com.tk.quicksearch.search.folders.ResolvedAppFolder
import com.tk.quicksearch.search.folders.folderMergePreview
import com.tk.quicksearch.search.folders.resolveAppFolders
import com.tk.quicksearch.search.folders.shortcutGridKey
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.hapticToggle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import java.util.Locale

private const val ROW_COUNT = 2
private const val TabSlideOffsetPx = 64
private const val SuggestionsEnterDurationMillis = 320
private const val SuggestionsEnterOffsetDp = 12f
private const val SuggestionTabInactiveAlpha = 0.34f
private const val SuggestionTabSwipeThresholdPx = 48f
private val AppGridRowSpacing = DesignTokens.SpacingXSmall
private val AppGridWidthRoundingSlack = 1.dp
internal val RegularAppIconSize = DesignTokens.IconSizeXLarge - DesignTokens.SpacingXXSmall
internal val OverlayAppIconSurfaceSize = 52.dp
internal val OverlayAppIconSize = 36.dp
internal val TopResultIndicatorTopPadding = 0.dp
internal val TopResultIndicatorBottomPadding = DesignTokens.SpacingSmall
private val TopResultIndicatorHorizontalPadding = DesignTokens.SpacingSmall
private const val TopResultIndicatorBackgroundAlpha = 0.12f
private const val LightWallpaperAppIconShadowAmbientAlpha = 0.28f
private const val LightWallpaperAppIconShadowSpotAlpha = 0.45f
private const val ThemedMonochromeGlyphScale = 1.42f
private const val UnsupportedThemedIconGlyphScale = 0.62f
private const val UnsupportedThemedIconGlyphAlpha = 0.72f
internal const val DraggedPinnedAppScale = 1.08f
internal const val DraggedPinnedAppAlpha = 0.92f
internal const val MergeSourcePinnedAppScale = 0.92f
// How long a dragged item must rest on another item's center before a drop creates a folder.
private const val FolderMergeDwellMillis = 200L
// While merging is possible, reorders wait for the dragged item to rest this long on another
// cell, so the drag can pass over an item's edge toward its center without the item moving away.
private const val MergeableReorderDelayMillis = 100L
// Movement that counts as no longer resting, restarting the reorder or merge wait.
private val MergeableReorderRestSlop = 12.dp
// Share of a cell, centered on its item, where a dragged item starts merging instead of reordering
// (roughly where the two icons mostly overlap), and the larger share it can drift within once it is
// resting on that item. Resting anywhere else on the cell reorders.
private const val FolderMergeEnterZoneFraction = 0.4f
private const val FolderMergeStayZoneFraction = 0.6f
private val AllAppsDialogIconSurfaceSize = DesignTokens.AppIconSize
private val AllAppsDialogRowSpacing = DesignTokens.SpacingXXSmall

private enum class AppIconDisplayMode {
    OVERLAY,
    REGULAR,
}

private sealed interface AppGridEntry {
    val key: String

    data class App(val app: AppInfo) : AppGridEntry {
        override val key: String get() = app.launchCountKey()
    }

    data class Shortcut(val shortcut: StaticShortcut) : AppGridEntry {
        override val key: String get() = shortcutGridKey(shortcut)
    }

    data class Folder(val folder: ResolvedAppFolder) : AppGridEntry {
        override val key: String get() = folder.folder.gridKey
    }
}

private fun AppGridEntry.folderMemberKey(): String? =
        when (this) {
            is AppGridEntry.App -> appFolderMemberKey(app)
            is AppGridEntry.Shortcut -> appFolderMemberKey(shortcut)
            is AppGridEntry.Folder -> null
        }

/**
 * The cell under the dragged item's center. [offsetFractionX] and [offsetFractionY] are the
 * center's distance from that cell's item center, as a share of the item's width and height.
 */
private data class DragCellHit(
        val index: Int,
        val isOnItem: Boolean,
        val offsetFractionX: Float,
        val offsetFractionY: Float,
) {
    fun isInMergeZone(zoneFraction: Float): Boolean =
            isOnItem &&
                    offsetFractionX <= zoneFraction / 2f &&
                    offsetFractionY <= zoneFraction / 2f
}

/** An item the dragged one is over, waiting for it to rest near ([anchorX], [anchorY]) to merge. */
private data class MergeCandidate(
        val key: String,
        val anchorX: Float,
        val anchorY: Float,
)

/** A reorder waiting for the dragged item to rest on cell [index] near ([anchorX], [anchorY]). */
private data class PendingReorder(
        val index: Int,
        val anchorX: Float,
        val anchorY: Float,
)

/**
 * An area outside a reorderable grid that takes dropped items, hit-tested against the dragged
 * item's center in root coordinates. [onActiveChange] reports an item being held or dragged.
 */
private class AppGridDropTarget(
        val contains: (rootPosition: Offset) -> Boolean,
        val onActiveChange: (Boolean) -> Unit,
        val onHoverChange: (Boolean) -> Unit,
        val onDrop: (AppGridEntry) -> Unit,
)

private data class PinnedAppDragState(
        val key: String,
        val startIndex: Int,
        val originIndex: Int,
        val originEntries: List<AppGridEntry>,
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
        val onShortcutClick: (StaticShortcut) -> Unit,
        val onAppInfoClick: () -> Unit,
        val onUninstallClick: () -> Unit,
        val onHideApp: () -> Unit,
        val onDisableAppShortcut: (StaticShortcut) -> Unit,
        val onPinApp: () -> Unit,
        val onUnpinApp: () -> Unit,
        val onNicknameClick: () -> Unit,
        val onTriggerClick: () -> Unit,
        val onAddToHome: () -> Unit,
        val onOpenInSplitScreen: () -> Unit,
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
    val suggestionSlotCount =
            (rowCount * getAppGridColumns(phoneColumnOverride)).coerceAtLeast(1)
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
    fun orderedPinnedEntries(pinned: List<AppInfo>, includeFolders: Boolean): List<AppGridEntry> {
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
        if (shortcutEntries.isEmpty() && folderEntries.isEmpty()) return appEntries
        val appIterator = appEntries.iterator()
        val shortcutIterator = shortcutEntries.iterator()
        val folderIterator = folderEntries.iterator()
        return (appEntries + shortcutEntries + folderEntries)
                .sortedBy { rank[it.key] ?: Int.MAX_VALUE }
                .map { entry ->
                    when (entry) {
                        is AppGridEntry.App -> appIterator.next()
                        is AppGridEntry.Shortcut -> shortcutIterator.next()
                        is AppGridEntry.Folder -> folderIterator.next()
                    }
                }
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
    val onReorderPinnedEntries: (List<AppGridEntry>) -> Unit = { entries ->
        val reorderedApps = entries.filterIsInstance<AppGridEntry.App>().map { it.app }
        if (pinnedGridShortcuts.isEmpty() && pinnedFolders.isEmpty()) {
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
                            disabledShortcutIds.contains(shortcutKey(shortcut))
                        }
                        .groupBy { it.packageName }
            }
    // Animate the suggestions grid (empty query) when it first appears. Search results should
    // appear immediately without animation.
    val initialSuggestionsAlpha = if (suppressSuggestionsEnterAnimation) 1f else 0f
    val initialSuggestionsOffset = if (suppressSuggestionsEnterAnimation) 0f else SuggestionsEnterOffsetDp
    val suggestionsAlpha = remember { Animatable(initialSuggestionsAlpha) }
    val suggestionsTranslationYDp = remember { Animatable(initialSuggestionsOffset) }
    val density = LocalDensity.current
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
                suggestionsTranslationYDp.snapTo(0f)
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
            val suggestionsContentModifier = Modifier.graphicsLayer {
                alpha = suggestionsAlpha.value
                translationY = with(density) { suggestionsTranslationYDp.value.dp.toPx() }
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
                                onHideApp = onHideApp,
                                onDisableAppShortcut = onDisableAppShortcut,
                                onPinApp = onPinApp,
                                onUnpinApp = onUnpinApp,
                                onReorderPinnedEntries = onReorderPinnedEntries,
                                onNicknameClick = onNicknameClick,
                                onTriggerClick = onTriggerClick,
                                onOpenInSplitScreen = onOpenInSplitScreen,
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
                            onHideApp = onHideApp,
                            onDisableAppShortcut = onDisableAppShortcut,
                            onPinApp = onPinApp,
                            onUnpinApp = onUnpinApp,
                            onReorderPinnedEntries = onReorderPinnedEntries,
                            onNicknameClick = onNicknameClick,
                            onTriggerClick = onTriggerClick,
                            onOpenInSplitScreen = onOpenInSplitScreen,
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
                    onHideApp = onHideApp,
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
                onHideApp = onHideApp,
                onDisableAppShortcut = onDisableAppShortcut,
                onPinApp = onPinApp,
                onUnpinApp = onUnpinApp,
                onNicknameClick = onNicknameClick,
                onTriggerClick = onTriggerClick,
                onOpenInSplitScreen = onOpenInSplitScreen,
                getAppNickname = getAppNickname,
                getAppTrigger = getAppTrigger,
                pinnedPackageNames = pinnedPackageNames,
                shortcutsByPackage = shortcutsByPackage,
                phoneColumnOverride = phoneColumnOverride,
                appIconSizeStep = appIconSizeStep,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                notificationDotKeys = notificationDotKeys,
        )
    }
}

@Composable
private fun AllAppsDialog(
        title: String,
        apps: List<AppInfo>,
        onDismiss: () -> Unit,
        onAppClick: (AppInfo) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onDisableAppShortcut: (StaticShortcut) -> Unit = {},
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        onOpenInSplitScreen: (AppInfo) -> Unit,
        getAppNickname: (String) -> String?,
        getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        pinnedPackageNames: Set<String>,
        shortcutsByPackage: Map<String, List<StaticShortcut>>,
        phoneColumnOverride: Int,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationDotKeys: Set<String> = emptySet(),
) {
    val dialogColumns = getAppGridColumns(phoneColumnOverride)
    val context = LocalContext.current
    val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
    AppAlertDialog(
            modifier = Modifier.fillMaxWidth(0.94f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                val gridState = rememberLazyGridState()
                var isScrollbarDragging by remember { mutableStateOf(false) }
                Box(
                        modifier =
                                Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 520.dp),
                ) {
                    LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(dialogColumns),
                            modifier =
                                    Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 520.dp)
                                            .padding(
                                                    end =
                                                            (LazyGridScrollbarTouchWidth - AllAppsDialogSeekEdgeNudge)
                                                                    .coerceAtLeast(DesignTokens.SpacingSmall),
                                            ),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(AllAppsDialogRowSpacing),
                    ) {
                        items(
                                items = apps,
                                key = { app -> app.launchCountKey() },
                        ) { app ->
                            AllAppsDialogGridItem(
                                    app = app,
                                    appIconSizeStep = appIconSizeStep,
                                    iconPackPackage = iconPackPackage,
                                    appIconShape = appIconShape,
                                    notificationDotKeys = notificationDotKeys,
                                    onClick = { onAppClick(app) },
                                    shortcuts = shortcutsByPackage[app.packageName].orEmpty(),
                                    appActions =
                                            AppActions(
                                                    onClick = { onAppClick(app) },
                                                    onShortcutClick = onAppShortcutClick,
                                                    onAppInfoClick = { onAppInfoClick(app) },
                                                    onUninstallClick = { onUninstallClick(app) },
                                                    onHideApp = { onHideApp(app) },
                                                    onDisableAppShortcut = onDisableAppShortcut,
                                                    onPinApp = { onPinApp(app) },
                                                    onUnpinApp = { onUnpinApp(app) },
                                                    onNicknameClick = { onNicknameClick(app) },
                                                    onTriggerClick = { onTriggerClick(app) },
                                                    onAddToHome = { addToHomeHandler.addAppToHome(app) },
                                                    onOpenInSplitScreen = { onOpenInSplitScreen(app) },
                                            ),
                                    appState =
                                            AppState(
                                                    hasNickname =
                                                            !getAppNickname(app.packageName)
                                                                    .isNullOrBlank(),
                                                    hasTrigger =
                                                            getAppTrigger(app.packageName)
                                                                    ?.word
                                                                    ?.isNotBlank() == true,
                                                    isPinned =
                                                            pinnedPackageNames.contains(
                                                                    app.launchCountKey(),
                                                            ),
                                                    showUninstall =
                                                            !app.isSystemApp &&
                                                                    app.userHandleId == null &&
                                                                    app.packageName != context.packageName,
                                                    showAppLabel = true,
                                                    isOverlayPresentation = false,
                                            ),
                            )
                        }
                    }
                    AllAppsScrollLetterPopup(
                            apps = apps,
                            gridState = gridState,
                            isScrollbarDragging = isScrollbarDragging,
                    )
                    LazyGridVerticalScrollbar(
                            state = gridState,
                            modifier =
                                    Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = AllAppsDialogSeekEdgeNudge),
                            onDraggingChange = { isScrollbarDragging = it },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.common_close))
                }
            },
        )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllAppsDialogGridItem(
        app: AppInfo,
        appIconSizeStep: Int,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        notificationDotKeys: Set<String>,
        onClick: () -> Unit,
        shortcuts: List<StaticShortcut>,
        appActions: AppActions,
        appState: AppState,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }
    val sizeScale = remember(appIconSizeStep) { UiPreferences.appIconSizeScale(appIconSizeStep) }
    val iconSize = remember(sizeScale) { RegularAppIconSize * sizeScale }
    val iconSurfaceSize = remember(sizeScale) { AllAppsDialogIconSurfaceSize * sizeScale }
    val iconResult =
            rememberAppIcon(
                    packageName = app.packageName,
                    iconPackPackage = iconPackPackage,
                    userHandleId = app.userHandleId,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            )
    Box(
            modifier =
                    Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                modifier =
                        Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                        onClick = {
                                            hapticConfirm(view)()
                                            onClick()
                                        },
                                        onLongClick = { showOptions = true },
                                )
                                .padding(
                                        horizontal = DesignTokens.SpacingXSmall,
                                        vertical = DesignTokens.SpacingXSmall,
                                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Box(
                    modifier = Modifier.size(iconSurfaceSize).appSwipeGestures(app),
                    contentAlignment = Alignment.Center,
            ) {
                iconResult.bitmap?.let { icon ->
                    Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                    )
                } ?: Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize),
                )
                AppNotificationDot(visible = app.hasNotificationDot(notificationDotKeys))
            }
            Text(
                    text = app.appName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
            )
        }

        AppItemDropdownMenu(
                expanded = showOptions,
                onDismiss = { showOptions = false },
                isPinned = appState.isPinned,
                showUninstall = appState.showUninstall,
                hasNickname = appState.hasNickname,
                hasTrigger = appState.hasTrigger,
                shortcuts = shortcuts,
                appInfo = app,
                iconPackPackage = iconPackPackage,
                appIconShape = appIconShape,
                onShortcutClick = appActions.onShortcutClick,
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onDisableShortcut = appActions.onDisableAppShortcut,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
                onOpenInSplitScreen = appActions.onOpenInSplitScreen,
        )
    }
}

@Composable
private fun AppSuggestionTabStrip(
        tabs: List<AppSuggestionTab>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
) {
    val activeColor = homeTextColor()
    val inactiveColor = activeColor.copy(alpha = SuggestionTabInactiveAlpha)
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

private fun replaceSuggestionAppsWithPinned(
        pinnedApps: List<AppInfo>,
        suggestedApps: List<AppInfo>,
        slotCount: Int,
): List<AppInfo> {
    val seen = LinkedHashSet<String>(slotCount)
    return (pinnedApps + suggestedApps)
            .asSequence()
            .filter { seen.add(it.launchCountKey()) }
            .take(slotCount)
            .toList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppGrid(
        entries: List<AppGridEntry>,
        isSearching: Boolean,
        modifier: Modifier = Modifier,
        onAppClick: (AppInfo) -> Unit,
        onAppShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: (AppInfo) -> Unit,
        onUninstallClick: (AppInfo) -> Unit,
        onHideApp: (AppInfo) -> Unit,
        onDisableAppShortcut: (StaticShortcut) -> Unit = {},
        onPinApp: (AppInfo) -> Unit,
        onUnpinApp: (AppInfo) -> Unit,
        onReorderPinnedEntries: (List<AppGridEntry>) -> Unit,
        onNicknameClick: (AppInfo) -> Unit,
        onTriggerClick: (AppInfo) -> Unit,
        onOpenInSplitScreen: (AppInfo) -> Unit,
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
        notificationDotKeys: Set<String> = emptySet(),
        showPinnedIndicators: Boolean = false,
        reorderPinnedApps: Boolean = false,
        scrollableRowCount: Int? = null,
        shortcutActions: AppGridShortcutActions? = null,
        onMergeEntries: ((AppGridEntry, AppGridEntry, List<AppGridEntry>) -> Unit)? = null,
        onFolderClick: (ResolvedAppFolder) -> Unit = {},
        onDeleteFolder: (ResolvedAppFolder) -> Unit = {},
        dropTarget: AppGridDropTarget? = null,
) {
    var displayedEntries by remember(reorderPinnedApps) { mutableStateOf(entries) }
    var displayedSourceEntries by remember(reorderPinnedApps) { mutableStateOf(entries) }
    var dragState by remember { mutableStateOf<PinnedAppDragState?>(null) }
    // Takes new entries except mid-drag: a background refresh (e.g. the app list loading after
    // launch) would otherwise reset the grid under the finger and drop the reorder in progress.
    if (dragState == null && entries != displayedSourceEntries) {
        displayedSourceEntries = entries
        displayedEntries = entries
    }
    // Item the dragged one is resting on; merging arms once it has stayed still there for the
    // dwell, so sliding across an item's center doesn't merge.
    var mergeCandidate by remember { mutableStateOf<MergeCandidate?>(null) }
    var isMergeArmed by remember { mutableStateOf(false) }
    var pendingReorder by remember { mutableStateOf<PendingReorder?>(null) }
    // The dragged item is over [dropTarget], so dropping it hands it over.
    var isOverDropTarget by remember { mutableStateOf(false) }
    val gridCoordinatesHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val view = LocalView.current
    LaunchedEffect(mergeCandidate) {
        isMergeArmed = false
        if (mergeCandidate == null) return@LaunchedEffect
        delay(FolderMergeDwellMillis)
        isMergeArmed = true
        hapticToggle(view)()
    }
    val armedMergeTargetKey = mergeCandidate?.key?.takeIf { isMergeArmed }
    var measuredItemHeightPx by remember { mutableStateOf(0f) }
    val maxVisibleColumns = getAppGridColumns(phoneColumnOverride)
    val columns =
            remember(entries, maxVisibleColumns) {
                if (entries.isEmpty()) {
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
    val visibleEntries =
            remember(displayedEntries, visibleAppLimit) {
                displayedEntries.take(visibleAppLimit)
            }
    val orderedEntries =
            remember(visibleEntries, oneHandedMode, columns) {
                appsInVisualGridOrder(visibleEntries, columns, oneHandedMode)
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

    BoxWithConstraints(
            modifier =
                    modifier.fillMaxWidth().onGloballyPositioned { gridCoordinatesHolder[0] = it },
    ) {
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
        val rowSpacingPx = with(LocalDensity.current) { AppGridRowSpacing.toPx() }
        val reorderRestSlopPx = with(LocalDensity.current) { MergeableReorderRestSlop.toPx() }
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

        // Rearranges relative to the order at drag start: moving to a different row swaps the
        // two apps, moving within the same row shifts the apps in between.
        fun movePinnedApp(state: PinnedAppDragState, toVisualIndex: Int) {
            if (!reorderPinnedApps) return
            val originVisualOrder =
                    appsInVisualGridOrder(state.originEntries, columns, oneHandedMode)
            val fromVisualIndex = state.originIndex
            if (fromVisualIndex !in originVisualOrder.indices ||
                    toVisualIndex !in originVisualOrder.indices
            ) {
                return
            }
            val reorderedVisualApps =
                    originVisualOrder.toMutableList().apply {
                        if (fromVisualIndex / columns != toVisualIndex / columns) {
                            this[fromVisualIndex] = originVisualOrder[toVisualIndex]
                            this[toVisualIndex] = originVisualOrder[fromVisualIndex]
                        } else if (fromVisualIndex != toVisualIndex) {
                            add(toVisualIndex, removeAt(fromVisualIndex))
                        }
                    }
            displayedEntries =
                    appsInPersistedGridOrder(reorderedVisualApps, columns, oneHandedMode)
        }

        // The dragged item's center, in grid coordinates.
        fun dragCenter(state: PinnedAppDragState): Offset {
            val itemHeightPx = measuredItemHeightPx.takeIf { it > 0f } ?: rowItemWidthPx
            val startColumn = state.startIndex % columns
            val startRow = state.startIndex / columns
            return Offset(
                    startColumn * (rowItemWidthPx + spacingPx) + rowItemWidthPx / 2f + state.offsetX,
                    startRow * (itemHeightPx + rowSpacingPx) + itemHeightPx / 2f + state.offsetY,
            )
        }

        // The cell under the dragged item's center, and how far the center is from that cell's
        // item center.
        fun cellHitForDrag(state: PinnedAppDragState): DragCellHit {
            val itemHeightPx = measuredItemHeightPx.takeIf { it > 0f } ?: rowItemWidthPx
            val cellWidthPx = rowItemWidthPx + spacingPx
            val cellHeightPx = itemHeightPx + rowSpacingPx
            val (centerX, centerY) = dragCenter(state)
            val targetColumn = (centerX / cellWidthPx).toInt().coerceIn(0, columns - 1)
            val targetRow = (centerY / cellHeightPx).toInt().coerceAtLeast(0)
            val maxTargetIndex = min(displayedEntries.lastIndex, visibleAppLimit - 1)
            val rawIndex = targetRow * columns + targetColumn
            val targetIndex = rawIndex.coerceIn(0, maxTargetIndex)
            val offsetFromItemCenterX =
                    centerX - (targetColumn * cellWidthPx + rowItemWidthPx / 2f)
            val offsetFromItemCenterY =
                    centerY - (targetRow * cellHeightPx + itemHeightPx / 2f)
            return DragCellHit(
                    index = targetIndex,
                    isOnItem = centerX >= 0f && centerY >= 0f && rawIndex == targetIndex,
                    offsetFractionX = abs(offsetFromItemCenterX) / rowItemWidthPx.coerceAtLeast(1f),
                    offsetFractionY = abs(offsetFromItemCenterY) / itemHeightPx.coerceAtLeast(1f),
            )
        }

        // Moves the dragged item to [targetIndex], keeping it under the finger.
        fun reorderDraggedItem(targetIndex: Int) {
            val state = dragState ?: return
            val currentIndex =
                    appsInVisualGridOrder(displayedEntries, columns, oneHandedMode)
                            .indexOfFirst { it.key == state.key }
            if (currentIndex < 0 || targetIndex == currentIndex) return
            val itemHeightPx = measuredItemHeightPx.takeIf { it > 0f } ?: rowItemWidthPx
            val oldCol = currentIndex % columns
            val oldRow = currentIndex / columns
            val newCol = targetIndex % columns
            val newRow = targetIndex / columns
            val layoutShiftX = (newCol - oldCol) * (rowItemWidthPx + spacingPx)
            val layoutShiftY = (newRow - oldRow) * (itemHeightPx + rowSpacingPx)
            movePinnedApp(state, targetIndex)
            dragState =
                    state.copy(
                            startIndex = targetIndex,
                            offsetX = state.offsetX - layoutShiftX,
                            offsetY = state.offsetY - layoutShiftY,
                    )
        }
        val currentReorderDraggedItem by rememberUpdatedState(::reorderDraggedItem)
        LaunchedEffect(pendingReorder) {
            val pending = pendingReorder ?: return@LaunchedEffect
            delay(MergeableReorderDelayMillis)
            currentReorderDraggedItem(pending.index)
            pendingReorder = null
        }

        val context = LocalContext.current
        val addToHomeHandler = remember(context) { AddToHomeHandler(context) }
        val createAppActions =
                remember(
                        onAppClick,
                        onAppShortcutClick,
                        onAppInfoClick,
                        onUninstallClick,
                        onHideApp,
                        onDisableAppShortcut,
                        onPinApp,
                        onUnpinApp,
                        onNicknameClick,
                        onTriggerClick,
                        onOpenInSplitScreen,
                        addToHomeHandler
                ) {
                    { app: AppInfo ->
                        AppActions(
                                onClick = { onAppClick(app) },
                                onShortcutClick = onAppShortcutClick,
                                onAppInfoClick = { onAppInfoClick(app) },
                                onUninstallClick = { onUninstallClick(app) },
                                onHideApp = { onHideApp(app) },
                                onDisableAppShortcut = onDisableAppShortcut,
                                onPinApp = { onPinApp(app) },
                                onUnpinApp = { onUnpinApp(app) },
                                onNicknameClick = { onNicknameClick(app) },
                                onTriggerClick = { onTriggerClick(app) },
                                onAddToHome = { addToHomeHandler.addAppToHome(app) },
                                onOpenInSplitScreen = { onOpenInSplitScreen(app) },
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

        val handleDragStart: (AppGridEntry) -> Unit = handleStart@{ entry ->
            if (!reorderPinnedApps) return@handleStart
            val index =
                    appsInVisualGridOrder(displayedEntries, columns, oneHandedMode)
                            .indexOfFirst { it.key == entry.key }
            if (index >= 0) {
                mergeCandidate = null
                pendingReorder = null
                dragState =
                        PinnedAppDragState(
                                key = entry.key,
                                startIndex = index,
                                originIndex = index,
                                originEntries = displayedEntries,
                        )
            }
        }
        // Resting on another item's center arms a merge; elsewhere the drag reorders. While merging
        // is possible, a reorder only happens once the dragged item rests on another cell, so
        // items don't slide out from under it on the way to their center. Folders never merge.
        val handleDrag: (Float, Float) -> Unit = handleDrag@{ dragX, dragY ->
            if (!reorderPinnedApps) return@handleDrag
            val currentState = dragState ?: return@handleDrag
            val updatedState =
                    currentState.copy(
                            offsetX = currentState.offsetX + dragX,
                            offsetY = currentState.offsetY + dragY,
                    )
            dragState = updatedState
            if (dropTarget != null) {
                val rootCenter =
                        gridCoordinatesHolder[0]
                                ?.takeIf { it.isAttached }
                                ?.localToRoot(dragCenter(updatedState))
                val isOver = rootCenter != null && dropTarget.contains(rootCenter)
                if (isOver != isOverDropTarget) {
                    isOverDropTarget = isOver
                    dropTarget.onHoverChange(isOver)
                    if (isOver) hapticToggle(view)()
                }
                if (isOver) {
                    mergeCandidate = null
                    pendingReorder = null
                    return@handleDrag
                }
            }
            val visualEntries = appsInVisualGridOrder(displayedEntries, columns, oneHandedMode)
            val currentIndex = visualEntries.indexOfFirst { it.key == updatedState.key }
            if (currentIndex < 0) return@handleDrag
            val hit = cellHitForDrag(updatedState)
            val canMerge =
                    onMergeEntries != null && visualEntries[currentIndex] !is AppGridEntry.Folder
            val hitKey = visualEntries.getOrNull(hit.index)?.key
            val mergeZoneFraction =
                    if (hitKey != null && hitKey == mergeCandidate?.key) {
                        FolderMergeStayZoneFraction
                    } else {
                        FolderMergeEnterZoneFraction
                    }
            if (
                canMerge &&
                    hitKey != null &&
                    hit.index != currentIndex &&
                    hit.isInMergeZone(mergeZoneFraction)
            ) {
                pendingReorder = null
                val candidate = mergeCandidate
                val stillResting =
                        candidate != null &&
                                candidate.key == hitKey &&
                                (
                                    isMergeArmed ||
                                        (
                                            abs(updatedState.offsetX - candidate.anchorX) <= reorderRestSlopPx &&
                                                abs(updatedState.offsetY - candidate.anchorY) <= reorderRestSlopPx
                                        )
                                )
                if (!stillResting) {
                    mergeCandidate =
                            MergeCandidate(
                                    key = hitKey,
                                    anchorX = updatedState.offsetX,
                                    anchorY = updatedState.offsetY,
                            )
                }
                return@handleDrag
            }
            mergeCandidate = null
            when {
                hit.index == currentIndex -> pendingReorder = null
                canMerge -> {
                    val pending = pendingReorder
                    val stillResting =
                            pending != null &&
                                    pending.index == hit.index &&
                                    abs(updatedState.offsetX - pending.anchorX) <= reorderRestSlopPx &&
                                    abs(updatedState.offsetY - pending.anchorY) <= reorderRestSlopPx
                    if (!stillResting) {
                        pendingReorder =
                                PendingReorder(
                                        index = hit.index,
                                        anchorX = updatedState.offsetX,
                                        anchorY = updatedState.offsetY,
                                )
                    }
                }
                else -> reorderDraggedItem(hit.index)
            }
        }
        val handleDragEnd: (Boolean) -> Unit = handleEnd@{ completed ->
            if (!reorderPinnedApps) return@handleEnd
            val state = dragState
            val mergeTarget =
                    armedMergeTargetKey
                            ?.takeIf { completed }
                            ?.let { key -> displayedEntries.firstOrNull { it.key == key } }
            val draggedEntry = state?.let { displayedEntries.firstOrNull { it.key == state.key } }
            val pendingIndex = pendingReorder?.index
            val droppedOnTarget = completed && isOverDropTarget
            mergeCandidate = null
            isMergeArmed = false
            pendingReorder = null
            isOverDropTarget = false
            dropTarget?.onHoverChange(false)
            if (droppedOnTarget && draggedEntry != null && dropTarget != null) {
                dragState = null
                // The grid keeps any reorder made so far, without the item.
                displayedEntries = displayedEntries.filterNot { it.key == draggedEntry.key }
                onReorderPinnedEntries(displayedEntries)
                dropTarget.onDrop(draggedEntry)
                return@handleEnd
            }
            if (mergeTarget != null && draggedEntry != null && onMergeEntries != null) {
                dragState = null
                // The merge saves the order itself; showing it without the dragged item avoids
                // a flash of the old tile until the new folder arrives.
                onMergeEntries(draggedEntry, mergeTarget, displayedEntries)
                displayedEntries = displayedEntries.filterNot { it.key == draggedEntry.key }
                return@handleEnd
            }
            if (completed && pendingIndex != null) reorderDraggedItem(pendingIndex)
            dragState = null
            onReorderPinnedEntries(displayedEntries)
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
                        items = orderedEntries,
                        key = { entry -> entry.key },
                ) { entry ->
                    if (entry is AppGridEntry.Folder) {
                        FolderGridItem(
                                modifier = Modifier.fillMaxWidth(),
                                folder = entry.folder,
                                onClick = { onFolderClick(entry.folder) },
                                onDelete = { onDeleteFolder(entry.folder) },
                                iconPackPackage = iconPackPackage,
                                showLabel = showAppLabels,
                                isOverlayPresentation = isOverlayPresentation,
                                appIconSizeStep = appIconSizeStep,
                                appIconShape = appIconShape,
                                showWallpaperBackground = showWallpaperBackground,
                                onItemMeasured = { height ->
                                    measuredItemHeightPx = height.toFloat()
                                },
                        )
                    } else if (entry is AppGridEntry.Shortcut) {
                        AppShortcutGridItem(
                                modifier = Modifier.fillMaxWidth(),
                                shortcut = entry.shortcut,
                                onClick = onAppShortcutClick,
                                actions = shortcutActions,
                                iconPackPackage = iconPackPackage,
                                showLabel = showAppLabels,
                                isOverlayPresentation = isOverlayPresentation,
                                appIconSizeStep = appIconSizeStep,
                                appIconShape = appIconShape,
                                onItemMeasured = { height ->
                                    measuredItemHeightPx = height.toFloat()
                                },
                        )
                    } else {
                        val app = (entry as AppGridEntry.App).app
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
                                notificationDotKeys = notificationDotKeys,
                                showPinnedIndicators = showPinnedIndicators,
                                onItemMeasured = { height ->
                                    measuredItemHeightPx = height.toFloat()
                                },
                        )
                    }
                }
            }
        } else {
            FlowRow(
                    modifier = gridModifier,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppGridRowSpacing),
                    maxItemsInEachRow = columns,
            ) {
                orderedEntries.forEach { entry ->
                    key(entry.key) {
                        val isThisDragging = entry.key == dragState?.key
                        val isMergeTarget = entry.key == armedMergeTargetKey
                        // Also shrinks an item about to be dropped on the drop target.
                        val isMergeSource =
                                isThisDragging && (armedMergeTargetKey != null || isOverDropTarget)
                        val entryDragOffset =
                                if (isThisDragging) {
                                    dragState?.let {
                                        IntOffset(it.offsetX.toInt(), it.offsetY.toInt())
                                    }
                                } else {
                                    null
                                }
                        val tileModifier =
                                Modifier.width(rowItemWidth)
                                        .animatePinnedGridPlacement(
                                                enabled = reorderPinnedApps,
                                                isDragging = isThisDragging,
                                                dragOffset = entryDragOffset,
                                        )
                        if (entry is AppGridEntry.Folder) {
                            FolderGridItem(
                                    modifier = tileModifier,
                                    folder = entry.folder,
                                    onClick = { onFolderClick(entry.folder) },
                                    onDelete = { onDeleteFolder(entry.folder) },
                                    iconPackPackage = iconPackPackage,
                                    showLabel = showAppLabels,
                                    isOverlayPresentation = isOverlayPresentation,
                                    appIconSizeStep = appIconSizeStep,
                                    appIconShape = appIconShape,
                                    showWallpaperBackground = showWallpaperBackground,
                                    isDragging = isThisDragging,
                                    isMergeTarget = isMergeTarget,
                                    dragOffset = entryDragOffset,
                                    onItemMeasured = { height ->
                                        measuredItemHeightPx = height.toFloat()
                                    },
                                    onPinnedDragStart =
                                            if (reorderPinnedApps) {
                                                { handleDragStart(entry) }
                                            } else {
                                                null
                                            },
                                    onPinnedDrag = if (reorderPinnedApps) handleDrag else null,
                                    onPinnedDragEnd = if (reorderPinnedApps) handleDragEnd else null,
                            )
                        } else if (entry is AppGridEntry.Shortcut) {
                            AppShortcutGridItem(
                                    modifier = tileModifier,
                                    shortcut = entry.shortcut,
                                    onClick = onAppShortcutClick,
                                    actions = shortcutActions,
                                    iconPackPackage = iconPackPackage,
                                    showLabel = showAppLabels,
                                    isOverlayPresentation = isOverlayPresentation,
                                    appIconSizeStep = appIconSizeStep,
                                    appIconShape = appIconShape,
                                    showWallpaperBackground = showWallpaperBackground,
                                    isDragging = isThisDragging,
                                    isMergeSource = isMergeSource,
                                    isMergeTarget = isMergeTarget,
                                    dragOffset = entryDragOffset,
                                    onItemMeasured = { height ->
                                        measuredItemHeightPx = height.toFloat()
                                    },
                                    onPinnedDragStart =
                                            if (reorderPinnedApps) {
                                                { handleDragStart(entry) }
                                            } else {
                                                null
                                            },
                                    onPinnedDrag = if (reorderPinnedApps) handleDrag else null,
                                    onPinnedDragEnd = if (reorderPinnedApps) handleDragEnd else null,
                                    onHoldChange = dropTarget?.onActiveChange,
                            )
                        } else {
                            val app = (entry as AppGridEntry.App).app
                            val appShortcuts = shortcutsByPackage[app.packageName].orEmpty()
                            AppGridItem(
                                    modifier = tileModifier,
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
                                    notificationDotKeys = notificationDotKeys,
                                    showPinnedIndicators = showPinnedIndicators,
                                    isDragging = isThisDragging,
                                    isMergeSource = isMergeSource,
                                    isMergeTarget = isMergeTarget,
                                    dragOffset = entryDragOffset,
                                    onItemMeasured = { height ->
                                        measuredItemHeightPx = height.toFloat()
                                    },
                                    onPinnedDragStart =
                                            if (reorderPinnedApps) {
                                                { handleDragStart(entry) }
                                            } else {
                                                null
                                            },
                                    onPinnedDrag = if (reorderPinnedApps) handleDrag else null,
                                    onPinnedDragEnd = if (reorderPinnedApps) handleDragEnd else null,
                                    onHoldChange = dropTarget?.onActiveChange,
                            )
                        }
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
        notificationDotKeys: Set<String> = emptySet(),
        showPinnedIndicators: Boolean = false,
        isDragging: Boolean = false,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: ((Boolean) -> Unit)? = null,
        isMergeSource: Boolean = false,
        isMergeTarget: Boolean = false,
        onHoldChange: ((Boolean) -> Unit)? = null,
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
    LaunchedEffect(iconResult.bitmap) {
        if (iconResult.bitmap != null) {
            StartupTrace.mark("QS.Home.AppIconLoaded")
        }
    }
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
    val indicatorAlpha by rememberPredictedSubmitIndicatorAlpha(isPredicted)
    val showTopResultIndicator = indicatorAlpha > 0f
    var isLocalDragging by remember { mutableStateOf(false) }
    val showDraggedPresentation = isDragging || isLocalDragging
    val dragScale by animateFloatAsState(
            targetValue =
                    when {
                        showDraggedPresentation && isMergeSource -> MergeSourcePinnedAppScale
                        showDraggedPresentation -> DraggedPinnedAppScale
                        else -> 1f
                    },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragScale",
    )
    val dragAlpha by animateFloatAsState(
            targetValue = if (showDraggedPresentation) DraggedPinnedAppAlpha else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedAppDragAlpha",
    )
    val dragModifier =
            rememberPinnedGridDragModifier(
                    key = appInfo.launchCountKey(),
                    onClick = appActions.onClick,
                    onShowOptions = { showOptions = true },
                    onLocalDraggingChange = { isLocalDragging = it },
                    onPinnedDragStart = onPinnedDragStart,
                    onPinnedDrag = onPinnedDrag,
                    onPinnedDragEnd = onPinnedDragEnd,
                    onHoldChange = onHoldChange,
            )

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
                    .padding(
                        top = TopResultIndicatorTopPadding,
                        bottom = TopResultIndicatorBottomPadding,
                        start = if (showTopResultIndicator) TopResultIndicatorHorizontalPadding else 0.dp,
                        end = if (showTopResultIndicator) TopResultIndicatorHorizontalPadding else 0.dp,
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
                    gestureModifier =
                            Modifier.folderMergePreview(
                                            active = isMergeTarget,
                                            iconSize = appIconSize,
                                            appIconShape = appIconShape,
                                            showWallpaperBackground = showWallpaperBackground,
                                    )
                                    .appSwipeGestures(appInfo)
                                    .then(dragModifier),
                    clickGesturesEnabled = !isDraggable,
                    appIconSurfaceSize = appIconSurfaceSize,
                    appIconSize = appIconSize,
                    appIconShape = appIconShape,
                    hasCustomIconPack = iconPackPackage != null,
                    oneHandedMode = oneHandedMode,
                    themedIconsEnabled = themedIconsEnabled,
                    showWallpaperBackground = showWallpaperBackground,
                    showPinnedIndicator = showPinnedIndicators && appState.isPinned,
                    showNotificationDot = appInfo.hasNotificationDot(notificationDotKeys),
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
                onShortcutClick = appActions.onShortcutClick,
                onAppInfoClick = appActions.onAppInfoClick,
                onHideApp = appActions.onHideApp,
                onDisableShortcut = appActions.onDisableAppShortcut,
                onPinApp = appActions.onPinApp,
                onUnpinApp = appActions.onUnpinApp,
                onUninstallClick = appActions.onUninstallClick,
                onNicknameClick = appActions.onNicknameClick,
                onTriggerClick = appActions.onTriggerClick,
                onAddToHome = appActions.onAddToHome,
                onOpenInSplitScreen = appActions.onOpenInSplitScreen,
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
        showPinnedIndicator: Boolean = false,
        showNotificationDot: Boolean = false,
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
    val pinnedIndicatorInset = (appIconSurfaceSize - appIconSize) / 2

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
            } else {
                val placeholderShape =
                        if (appIconShape == AppIconShape.CIRCLE) {
                            CircleShape
                        } else {
                            DesignTokens.ShapeLarge
                        }
                Box(
                        modifier =
                                Modifier.size(appIconSize)
                                        .clip(placeholderShape)
                                        .background(
                                                if (showWallpaperBackground) {
                                                    colorScheme.surface.copy(alpha = 0.28f)
                                                } else {
                                                    colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                },
                                        ),
                )
            }
            if (showPinnedIndicator) {
                Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        modifier =
                                Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = pinnedIndicatorInset, end = pinnedIndicatorInset)
                                        .size(12.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
            AppNotificationDot(
                    visible = showNotificationDot,
                    modifier =
                            if (showPinnedIndicator) {
                                Modifier.padding(top = pinnedIndicatorInset + 12.dp, end = pinnedIndicatorInset)
                            } else {
                                Modifier.padding(top = pinnedIndicatorInset, end = pinnedIndicatorInset)
                            },
            )
        }
    }
}

@Composable
internal fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
) {
    val labelColor = homeTextColor()
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
            text = rememberQueryHighlightedText(appName),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}
