package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.shared.util.hapticGestureThreshold
import kotlinx.coroutines.launch
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.searchScreen.LocalFrostedCardSource
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.theme.AppMotion

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** Pages hosted beyond those in view, so a slide doesn't wait on widget views to inflate. */
private const val HostedWidgetOverscanPages = 1

private class WidgetMovePreview {
    private var widgetId: Int? = null
    private var page = 0
    private var column = 0
    private var row = 0

    /** The last requested cell, with its row unclamped, so a page turn can place the widget again. */
    var lastRequest: Triple<Int, Int, Int>? = null
        private set

    fun reset() {
        widgetId = null
        lastRequest = null
    }

    fun request(appWidgetId: Int, targetColumn: Int, requestedRow: Int) {
        lastRequest = Triple(appWidgetId, targetColumn, requestedRow)
    }

    fun update(appWidgetId: Int, targetPage: Int, targetColumn: Int, targetRow: Int): Boolean {
        if (widgetId == appWidgetId && page == targetPage && column == targetColumn && row == targetRow) return false
        widgetId = appWidgetId
        page = targetPage
        column = targetColumn
        row = targetRow
        return true
    }
}


@Composable
internal fun WidgetPanelGrid(
    widgets: List<PanelWidgetInfo>,
    showHostedWidgets: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    widgetContentAvailable: Map<Int, Boolean>,
    editingWidgetId: Int?,
    layoutEditingEnabled: Boolean,
    density: Density,
    pager: WidgetPanelPagerState,
    viewportHeightPx: Int,
    /** Space above the grid on the first page; every page is [WidgetPanelPagerState.pageHeightPx] tall. */
    topInset: Dp,
    onPersist: (List<PanelWidgetInfo>) -> Unit,
    onSetEditingWidgetId: (Int?) -> Unit,
    onWidgetTouch: (Int) -> Boolean,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
    onRemoveWidget: (widget: PanelWidgetInfo, pageId: Int) -> Unit,
    onConfigureWidget: (PanelWidgetInfo, Intent) -> Unit,
    onPinWidgetToHome: (PanelWidgetInfo) -> Unit,
    onAddToStack: (PanelWidgetInfo, WidgetStackFit) -> Unit,
    onReorderStack: (PanelWidgetInfo) -> Unit,
    modifier: Modifier = Modifier,
    folderTiles: PanelFolderTiles? = null,
    /** Bumped to flash every stack's page indicator, such as when the panel comes into view. */
    stackIndicatorRevealKey: Int = 0,
) {
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    BoxWithConstraints(modifier = modifier) {
        val cellWidth =
            (maxWidth - WidgetPanelGridGap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        val rowHeight = WidgetPanelGridRowHeight
        val gap = WidgetPanelGridGap
        val gridUnitWidthPx = with(density) { (cellWidth + gap).toPx() }
        val gridUnitHeightPx = with(density) { (rowHeight + gap).toPx() }

        val specs =
            remember(widgets, appWidgetHost, cellWidth) {
                widgets.associate { widget ->
                    // A stack is as small as its largest page allows.
                    widget.appWidgetId to
                        widget.pageIds
                            .map { id ->
                                if (isQuickNotePage(id)) {
                                    QuickNoteGridSpec
                                } else if (isFolderPanelPage(id)) {
                                    FolderTileGridSpec
                                } else {
                                    widgetMinGridSpec(appWidgetHost.providerInfo(id), density, cellWidth.value)
                                }
                            }
                            .reduce { a, b ->
                                WidgetGridSpec(
                                    minColumnSpan = maxOf(a.minColumnSpan, b.minColumnSpan),
                                    minRowSpan = maxOf(a.minRowSpan, b.minRowSpan),
                                )
                            }
                }
            }

        val pageRows = pager.pageRows
        val laidOut =
            remember(widgets, specs, pageRows) { resolveWidgetGridLayout(widgets, specs, pageRows) }
        // Snapshot at entry: once shown, a kept view is attached and no longer reported as kept.
        val keptViewIds = remember(appWidgetHost) { appWidgetHost.keptViewIds() }

        // When the widgets on the page in view are all removed, that page closes (unless it's the
        // first) and the panel goes to the page before it.
        val previousLaidOut = remember { arrayOf(laidOut) }
        LaunchedEffect(laidOut) {
            val before = previousLaidOut[0]
            previousLaidOut[0] = laidOut
            if (laidOut != widgets) onPersist(laidOut)
            val viewPage = pager.targetPage
            if (viewPage == 0 || pager.dragZones.isNotEmpty()) return@LaunchedEffect
            val idsOnPage = before.filter { it.pageOrZero() == viewPage }.map { it.appWidgetId }
            if (idsOnPage.isNotEmpty() && idsOnPage.none { id -> laidOut.any { it.appWidgetId == id } }) {
                pager.animateToPage(viewPage - 1)
            }
        }

        var liveLayout by remember { mutableStateOf<List<PanelWidgetInfo>?>(null) }
        val displayLayout = liveLayout.takeIf { layoutEditingEnabled } ?: laidOut
        val movePreview = remember(laidOut) { WidgetMovePreview() }
        var hostDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        var quickNoteDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        val scope = rememberCoroutineScope()
        val view = LocalView.current

        val currentLaidOut by rememberUpdatedState(laidOut)
        val restingPageCount = (laidOut.maxOfOrNull { it.pageOrZero() } ?: 0) + 1

        /**
         * Shows [appWidgetId] at [column] and [requestedRow] on the page in view. A row above or
         * below the page lights the page zone on that edge instead.
         */
        fun previewMove(appWidgetId: Int, column: Int, requestedRow: Int) {
            val widget = laidOut.firstOrNull { it.appWidgetId == appWidgetId } ?: return
            movePreview.request(appWidgetId, column, requestedRow)
            val page = pager.targetPage
            val maxRow = (pageRows - (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)).coerceAtLeast(0)
            val othersOnPage = laidOut.any { it.appWidgetId != appWidgetId && it.pageOrZero() == page }
            val zones =
                buildSet {
                    if (page > 0) add(WidgetPanelPageZone.Previous)
                    if (page + 1 < restingPageCount) {
                        add(WidgetPanelPageZone.Next)
                    } else if (othersOnPage) {
                        // A widget alone on the last page has nowhere new to go.
                        add(WidgetPanelPageZone.NewPage)
                    }
                }
            pager.dragZones = zones
            pager.hoveredZone =
                when {
                    requestedRow < 0 -> WidgetPanelPageZone.Previous.takeIf { it in zones }
                    requestedRow > maxRow -> zones.firstOrNull { it != WidgetPanelPageZone.Previous }
                    else -> null
                }
            if (pager.hoveredZone != null) {
                // Over a page zone the widget leaves the grid and reaches into the zone, one row past
                // the page's edge; the rest of the page goes back to how it was. Dropping it there
                // moves it to that page.
                movePreview.update(appWidgetId, page, column, requestedRow.coerceIn(-1, maxRow + 1))
                liveLayout =
                    laidOut.map {
                        if (it.appWidgetId == appWidgetId) {
                            it.copy(column = column, row = requestedRow.coerceIn(-1, maxRow + 1))
                        } else {
                            it
                        }
                    }
                return
            }
            val row = requestedRow.coerceIn(0, maxRow)
            if (movePreview.update(appWidgetId, page, column, row)) {
                liveLayout =
                    resolveWidgetGridLayout(
                        moveWidgetToCell(laidOut, appWidgetId, column, row, targetPage = page, pageRows = pageRows),
                        specs,
                        pageRows,
                        compactPages = false,
                    )
            }
        }

        /**
         * Saves the previewed layout, dropping pages it left empty. A widget dropped on a page zone
         * moves to that page while the panel stays on the page in view.
         */
        fun finishInteraction() {
            val final = liveLayout
            val droppedZone = pager.hoveredZone
            val droppedId = movePreview.lastRequest?.first
            liveLayout = null
            movePreview.reset()
            pager.clearDragZones()
            if (final == null) return
            val moved =
                if (droppedZone != null && droppedId != null) {
                    // Leave the rest of the page as it was before the drag.
                    laidOut.withPanelWidgetMovedToPage(droppedId, droppedZone, pager.targetPage, pageRows)
                } else {
                    final
                }
            val settled = resolveWidgetGridLayout(moved, specs, pageRows)
            if (settled != laidOut) onPersist(settled)
            // Keep the page in view on screen when pages before it were added or removed.
            val viewPage = pager.targetPage
            val anchorId = laidOut.firstOrNull { it.appWidgetId != droppedId && it.pageOrZero() == viewPage }?.appWidgetId
            // A page the dropped widget left empty closes, so the panel follows the widget instead.
            val keptPage =
                anchorId?.let { id -> settled.firstOrNull { it.appWidgetId == id }?.pageOrZero() }
                    ?: settled.firstOrNull { it.appWidgetId == droppedId }?.pageOrZero()?.takeIf { droppedZone != null }
                    ?: viewPage.coerceAtMost(settled.maxOfOrNull { it.pageOrZero() } ?: 0)
            if (keptPage != viewPage) {
                scope.launch {
                    // Wait a frame so the panel has grown to fit an added page.
                    withFrameNanos { }
                    pager.jumpToPage(keptPage)
                }
            }
        }

        fun moveByStep(appWidgetId: Int, columnDelta: Int, rowDelta: Int): Boolean {
            val next = moveWidgetByStep(laidOut, appWidgetId, columnDelta, rowDelta, pageRows) ?: return false
            val moved = next.firstOrNull { it.appWidgetId == appWidgetId } ?: return false
            if ((moved.row ?: 0) + (moved.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN) > pageRows) return false
            onPersist(resolveWidgetGridLayout(next, specs, pageRows))
            return true
        }

        fun previewResize(appWidgetId: Int, resize: WidgetGridResize) {
            liveLayout =
                resolveWidgetGridLayout(
                    resizeWidgetInGrid(liveLayout ?: laidOut, appWidgetId, resize.withinPage(pageRows), specs),
                    specs,
                    pageRows,
                    compactPages = false,
                )
        }

        // A tick when the dragged widget enters a page zone, where dropping it moves it there.
        val hoveredZone = pager.hoveredZone
        LaunchedEffect(hoveredZone) {
            if (hoveredZone != null) hapticGestureThreshold(view, true)
        }

        val currentGridUnitWidthPx by rememberUpdatedState(gridUnitWidthPx)
        val currentHorizontalSign by rememberUpdatedState(horizontalSign)
        val currentGridUnitHeightPx by rememberUpdatedState(gridUnitHeightPx)
        val currentOnSetEditing by rememberUpdatedState(onSetEditingWidgetId)
        val currentOnWidgetTouch by rememberUpdatedState(onWidgetTouch)

        fun moveQuickNoteBy(totalDragX: Float, totalDragY: Float) {
            val start = quickNoteDragStart ?: return
            val columnSpan = start.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS
            val nextColumn =
                ((start.column ?: 0) + (totalDragX * horizontalSign / gridUnitWidthPx).roundToInt())
                    .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
            val nextRow = (start.row ?: 0) + (totalDragY / gridUnitHeightPx).roundToInt()
            previewMove(start.appWidgetId, nextColumn, nextRow)
        }

        SideEffect {
            appWidgetHost.onWidgetLongPress = if (layoutEditingEnabled) {
                { pageId ->
                    // A long-press on any page of a stack edits the whole stack.
                    val widget = currentLaidOut.stackOwnerOf(pageId)
                    if (widget != null) {
                        movePreview.reset()
                        hostDragStart = widget
                        currentOnSetEditing(widget.appWidgetId)
                    }
                }
            } else {
                null
            }
            appWidgetHost.onWidgetDragMove = { pageId, dx, dy ->
                val start = hostDragStart
                if (start != null && pageId in start.pageIds) {
                    val columnSpan = start.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                    // Every page sits at the same place on screen, so a page turn needs no
                    // correction: the row is always counted from the top of the page in view.
                    val nextColumn =
                        ((start.column ?: 0) + (dx * currentHorizontalSign / currentGridUnitWidthPx).roundToInt())
                            .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
                    val nextRow = (start.row ?: 0) + (dy / currentGridUnitHeightPx).roundToInt()
                    previewMove(start.appWidgetId, nextColumn, nextRow)
                }
            }
            appWidgetHost.onWidgetDragEnd = { _ ->
                hostDragStart = null
                finishInteraction()
            }
            appWidgetHost.onWidgetTouch = { pageId ->
                currentOnWidgetTouch(currentLaidOut.stackOwnerOf(pageId)?.appWidgetId ?: pageId)
            }
        }

        val isDragging = liveLayout != null
        val currentViewportHeightPx by rememberUpdatedState(viewportHeightPx)
        val hostedPages by remember(pager) {
            // Scroll changes only invalidate this reader when the range of pages in view changes.
            derivedStateOf(structuralEqualityPolicy()) {
                val visible = pager.visiblePages(currentViewportHeightPx)
                (visible.first - HostedWidgetOverscanPages)..(visible.last + HostedWidgetOverscanPages)
            }
        }

        // Keep the pre-drag page count while dragging, so the page the widget left stays in place.
        val pageCount =
            maxOf(
                restingPageCount,
                if (isDragging) (displayLayout.maxOfOrNull { it.pageOrZero() } ?: 0) + 1 else 0,
            )
        SideEffect { pager.pageCount = pageCount }
        val pageHeight = with(density) { pager.pageHeightPx.toDp() }
        // The grid starts below the page's top overhang, which the scroll content pads in.
        val panelHeight = (pageHeight * pageCount - topInset).coerceAtLeast(0.dp)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(panelHeight),
        ) {
          CompositionLocalProvider(LocalWidgetPanelPageHeight provides pageHeight) {
            displayLayout.forEach { widget ->
                key(widget.appWidgetId) {
                    val isEditing = editingWidgetId == widget.appWidgetId
                    val isNearViewport = widget.pageOrZero() in hostedPages
                    // Views kept from an earlier entry are already inflated, so they show during the
                    // slide; only new views wait for the page to settle.
                    val canHost = showHostedWidgets || widget.appWidgetId in keptViewIds
                    val folder = widget.panelFolderId?.let { folderTiles?.folders?.get(it) }
                    if (folderTiles != null && folder != null) {
                        FolderPanelGridItem(
                            widget = widget,
                            folder = folder,
                            appearance = folderTiles.appearance,
                            actions = folderTiles.actions,
                            isEditing = isEditing,
                            layoutEditingEnabled = layoutEditingEnabled,
                            cellWidth = cellWidth,
                            rowHeight = rowHeight,
                            gap = gap,
                            gridUnitWidthPx = gridUnitWidthPx,
                            gridUnitHeightPx = gridUnitHeightPx,
                            onEdit = { onSetEditingWidgetId(widget.appWidgetId) },
                            onMoveBy = { columnDelta, rowDelta -> moveByStep(widget.appWidgetId, columnDelta, rowDelta) },
                            onMovePreview = { c, r -> previewMove(widget.appWidgetId, c, r) },
                            onResizePreview = { resize -> previewResize(widget.appWidgetId, resize) },
                            onInteractionEnd = ::finishInteraction,
                            onEditEnd = { onSetEditingWidgetId(null) },
                        )
                    } else if (widget.panelFolderId == null) {
                        WidgetPanelGridItem(
                            widget = widget,
                            isNearViewport = isNearViewport,
                            showHostedWidget = canHost && (isNearViewport || isEditing),
                            appWidgetManager = appWidgetManager,
                            appWidgetHost = appWidgetHost,
                            widgetContentAvailable = widgetContentAvailable,
                            isEditing = isEditing,
                            layoutEditingEnabled = layoutEditingEnabled,
                            cellWidth = cellWidth,
                            rowHeight = rowHeight,
                            gap = gap,
                            gridUnitWidthPx = gridUnitWidthPx,
                            gridUnitHeightPx = gridUnitHeightPx,
                            spec = specs[widget.appWidgetId],
                            onEdit = { onSetEditingWidgetId(widget.appWidgetId) },
                            onMoveBy = { columnDelta, rowDelta -> moveByStep(widget.appWidgetId, columnDelta, rowDelta) },
                            onMovePreview = { targetColumn, targetRow ->
                                previewMove(widget.appWidgetId, targetColumn, targetRow)
                            },
                            onResizePreview = { resize -> previewResize(widget.appWidgetId, resize) },
                            onInteractionEnd = ::finishInteraction,
                            onRemove = { pageId -> onRemoveWidget(widget, pageId) },
                            onConfigure = { intent -> onConfigureWidget(widget, intent) },
                            onPinToHome = { onPinWidgetToHome(widget) },
                            onAddToStack = { onAddToStack(widget, it) },
                            onReorderStack = { onReorderStack(widget) },
                            onQuickNoteDragStart = {
                                movePreview.reset()
                                quickNoteDragStart = widget
                                onSetEditingWidgetId(widget.appWidgetId)
                            },
                            onQuickNoteDrag = { totalDragX, totalDragY ->
                                moveQuickNoteBy(totalDragX, totalDragY)
                            },
                            onQuickNoteDragEnd = {
                                quickNoteDragStart = null
                                finishInteraction()
                            },
                            onQuickNoteFocusChanged = onQuickNoteFocusChanged,
                            stackIndicatorRevealKey = stackIndicatorRevealKey,
                        )
                    }
                }
            }
          }
        }
    }
}

@Composable
private fun BoxScope.WidgetPanelGridItem(
    widget: PanelWidgetInfo,
    isNearViewport: Boolean,
    showHostedWidget: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    widgetContentAvailable: Map<Int, Boolean>,
    isEditing: Boolean,
    layoutEditingEnabled: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    spec: WidgetGridSpec?,
    onEdit: () -> Unit,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: (pageId: Int) -> Unit,
    onConfigure: (Intent) -> Unit,
    onPinToHome: () -> Unit,
    onAddToStack: (WidgetStackFit) -> Unit,
    onReorderStack: () -> Unit,
    onQuickNoteDragStart: () -> Unit,
    onQuickNoteDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onQuickNoteDragEnd: () -> Unit,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
    stackIndicatorRevealKey: Int,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val column = widget.column ?: 0
    val row = widget.row ?: 0
    val columnSpan = widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val width = cellWidth * columnSpan + gap * (columnSpan - 1)
    val height = rowHeight * rowSpan + gap * (rowSpan - 1)
    val x = (cellWidth + gap) * column
    val y = widgetPanelPageOffset(widget) + (rowHeight + gap) * row

    // Animate each grid-step update, including the widget being edited, so resizing and the
    // resulting reflow feel continuous instead of snapping between row/column boundaries.
    val animatedX by animateDpAsState(targetValue = x, animationSpec = WidgetLayoutMotion, label = "widgetX")
    val animatedY by animateDpAsState(targetValue = y, animationSpec = WidgetLayoutMotion, label = "widgetY")
    val animatedWidth by
        animateDpAsState(targetValue = width, animationSpec = WidgetLayoutMotion, label = "widgetWidth")
    val animatedHeight by
        animateDpAsState(targetValue = height, animationSpec = WidgetLayoutMotion, label = "widgetHeight")
    val editScale by animateFloatAsState(
        targetValue = if (isEditing) WIDGET_EDIT_LIFT_SCALE else 1f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "widgetEditScale",
    )

    if (widget.isQuickNoteWidget() && widget.stack.isEmpty()) {
        QuickNotePanelGridItem(
            widget = widget,
            isEditing = isEditing,
            cellWidth = cellWidth,
            rowHeight = rowHeight,
            gap = gap,
            gridUnitHeightPx = gridUnitHeightPx,
            minRowSpan = spec?.minRowSpan ?: 1,
            onDragStart = onQuickNoteDragStart.takeIf { layoutEditingEnabled },
            onDrag = onQuickNoteDrag,
            onDragEnd = onQuickNoteDragEnd,
            onFocusChanged = onQuickNoteFocusChanged,
            onMoveBy = onMoveBy,
            onMovePreview = onMovePreview,
            onResizePreview = onResizePreview,
            onInteractionEnd = onInteractionEnd,
            onRemove = { onRemove(QUICK_NOTE_PANEL_WIDGET_ID) },
            onAddToStack = { onAddToStack(WidgetStackFit(columnSpan, rowSpan, cellWidth.value)) },
        )
        return
    }

    val pageIds = widget.pageIds
    // The Quick Note page has no provider info; every other page needs one to be shown.
    val pageInfos: List<Pair<Int, AppWidgetProviderInfo?>> =
        remember(pageIds, appWidgetHost) {
            pageIds.mapNotNull { id ->
                if (isQuickNotePage(id)) id to null else appWidgetHost.providerInfo(id)?.let { id to it }
            }
        }
    if (pageInfos.isEmpty()) return
    val isStack = pageInfos.size > 1
    val stackState = rememberWidgetStackState()
    SyncWidgetStackPages(pageCount = pageInfos.size, state = stackState)
    LaunchedEffect(stackIndicatorRevealKey) {
        if (stackIndicatorRevealKey > 0) stackState.revealIndicator()
    }
    // Edit actions act on the page in view.
    val currentPageId = pageInfos.getOrNull(stackState.settledPage)?.first ?: pageInfos.first().first
    val currentPageInfo = pageInfos.firstOrNull { it.first == currentPageId }?.second

    val configureIntent =
        remember(currentPageInfo, currentPageId) {
            currentPageInfo?.configure?.let {
                WidgetConfigureTrampolineActivity.intent(context, currentPageId)
            }
        }

    Box(
        modifier =
            Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .widgetEditAction(onEdit.takeIf { layoutEditingEnabled && !isEditing })
                .offset {
                    IntOffset(
                        x = with(density) { animatedX.roundToPx() },
                        y = with(density) { animatedY.roundToPx() },
                    )
                }
                .zIndex(if (isEditing) 1f else 0f)
                .graphicsLayer {
                    scaleX = editScale
                    scaleY = editScale
                },
    ) {
        val pageContent: @Composable BoxScope.(Int) -> Unit = { pageId ->
            if (isQuickNotePage(pageId)) {
                QuickNoteStackPage(
                    layoutEditingEnabled = layoutEditingEnabled,
                    onFocusChanged = onQuickNoteFocusChanged,
                    onDragStart = onQuickNoteDragStart,
                    onDrag = onQuickNoteDrag,
                    onDragEnd = onQuickNoteDragEnd,
                )
            }
            pageInfos.firstOrNull { it.first == pageId }?.second?.let { info ->
                val id = pageId
                WidgetPanelPageContent(
                    appWidgetId = id,
                    providerInfo = info,
                    isNearViewport = isNearViewport,
                    showHostedWidget = showHostedWidget,
                    hasWidgetContent = widgetContentAvailable[id] == true,
                    appWidgetManager = appWidgetManager,
                    appWidgetHost = appWidgetHost,
                    width = width,
                    height = height,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            }
        }
        // A single widget goes through the stack layout too, so its hosted view keeps its place
        // (and its loaded content) when a second page turns it into a stack.
        WidgetStackPager(
            pageIds = pageInfos.map { it.first },
            state = stackState,
            userScrollEnabled = !isEditing,
            pageContent = pageContent,
        )

        AnimatedWidgetEditOverlay(visible = isEditing) {
            if (!layoutEditingEnabled) return@AnimatedWidgetEditOverlay
            WidgetEditOverlay(
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = spec?.minColumnSpan ?: 1,
                minRowSpan = spec?.minRowSpan ?: 1,
                hasConfigure = configureIntent != null,
                // Home has no Quick Note, so a stack holding it stays in the panel.
                canPinToHome = pageIds.none(::isQuickNotePage),
                onMoveBy = onMoveBy,
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = { onRemove(currentPageId) },
                onConfigure = { configureIntent?.let(onConfigure) },
                onPinToHome = onPinToHome,
                onAddToStack = {
                    onAddToStack(WidgetStackFit(columnSpan, rowSpan, cellWidth.value))
                },
                onReorderStack = onReorderStack.takeIf { isStack },
            )
        }
    }
}

private val QuickNoteGridSpec = WidgetGridSpec(minColumnSpan = WIDGET_PANEL_GRID_COLUMNS, minRowSpan = 1)

/** The Quick Note as a page of a widget stack; a long press on its title drags the whole stack. */
@Composable
private fun QuickNoteStackPage(
    layoutEditingEnabled: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    CompactQuickNoteWidget(
        modifier = Modifier.fillMaxSize(),
        onFocusChanged = onFocusChanged,
        onDragStart =
            if (layoutEditingEnabled) {
                {
                    totalDragX = 0f
                    totalDragY = 0f
                    onDragStart()
                }
            } else {
                null
            },
        onDrag = { dragX, dragY ->
            totalDragX += dragX
            totalDragY += dragY
            onDrag(totalDragX, totalDragY)
        },
        onDragEnd = onDragEnd,
    )
}
