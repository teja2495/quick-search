package com.tk.quicksearch.search.apps

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.search.folders.FolderGridItem
import com.tk.quicksearch.search.folders.ResolvedAppFolder
import com.tk.quicksearch.shared.util.hapticToggle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppGrid(
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
    val isArmedMergeTargetFolderFull =
            (displayedEntries.firstOrNull { it.key == armedMergeTargetKey } as? AppGridEntry.Folder)
                    ?.folder
                    ?.members
                    ?.size
                    ?.let { it >= 4 } == true
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

        // Rearranges relative to the order at drag start: moving to a different row or into an
        // empty cell swaps the two, moving within the same row shifts the apps in between.
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
                        // Dropping into an empty cell leaves one where the item was.
                        if (
                            fromVisualIndex / columns != toVisualIndex / columns ||
                                originVisualOrder[toVisualIndex] is AppGridEntry.Gap
                        ) {
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
            // Below the last row keeps the finger's column instead of jumping to the last cell.
            val targetIndex =
                    (targetRow.coerceAtMost(maxTargetIndex.coerceAtLeast(0) / columns) * columns +
                                    targetColumn)
                            .coerceIn(0, maxTargetIndex.coerceAtLeast(0))
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
            val hitKey =
                    visualEntries.getOrNull(hit.index)?.takeIf { it !is AppGridEntry.Gap }?.key
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
                    if (entry is AppGridEntry.Gap) {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    } else if (entry is AppGridEntry.Folder) {
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
                        val fadeMergeSource =
                                isThisDragging && armedMergeTargetKey != null && isArmedMergeTargetFolderFull
                        val mergePreviewMember =
                                dragState
                                        ?.key
                                        ?.let { draggedKey ->
                                            displayedEntries.firstOrNull { it.key == draggedKey }
                                        }
                                        ?.asFolderMember()
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
                        if (entry is AppGridEntry.Gap) {
                            // Sized like a tile so a row of only empty cells keeps its height.
                            Spacer(
                                    modifier =
                                            Modifier.width(rowItemWidth)
                                                    .height(
                                                            measuredItemHeightPx
                                                                    .takeIf { it > 0f }
                                                                    ?.let { with(density) { it.toDp() } }
                                                                    ?: rowItemWidth,
                                                    ),
                            )
                        } else if (entry is AppGridEntry.Folder) {
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
                                    mergePreviewMember = mergePreviewMember,
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
                                    fadeMergeSource = fadeMergeSource,
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
                                    fadeMergeSource = fadeMergeSource,
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
