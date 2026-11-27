package com.tk.quicksearch.widgetsPanel

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal const val WIDGET_PANEL_GRID_COLUMNS = 4
internal const val WIDGET_PANEL_DEFAULT_COLUMN_SPAN = 2
internal const val WIDGET_PANEL_DEFAULT_ROW_SPAN = 2
internal const val WIDGET_PANEL_MAX_ROW_SPAN = 6

internal data class WidgetGridSpec(
    val minColumnSpan: Int,
    val minRowSpan: Int,
)

internal data class WidgetGridResize(
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
)

/**
 * Computes the initial column / row span for a freshly added widget.
 *
 * Prefers `targetCellWidth/Height` (API 31+) when present. Falls back to
 * `minWidth/minHeight` mapped through the panel's cell size.
 */
internal fun computeInitialSpan(
    targetCellWidth: Int,
    targetCellHeight: Int,
    minWidthDp: Float,
    minHeightDp: Float,
    cellWidthDp: Float,
    rowHeightDp: Float,
    gapDp: Float,
): Pair<Int, Int> {
    val columnSpan =
        if (targetCellWidth > 0) {
            targetCellWidth.coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
        } else {
            calculateGridColumnSpan(minWidthDp = minWidthDp, cellWidthDp = cellWidthDp, gapDp = gapDp)
        }
    val rowSpan =
        if (targetCellHeight > 0) {
            targetCellHeight.coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN)
        } else {
            calculateGridRowSpan(minHeightDp = minHeightDp, rowHeightDp = rowHeightDp, gapDp = gapDp)
        }
    return columnSpan to rowSpan
}

/**
 * Lays out widgets on the panel's pages, each [pageRows] rows tall.
 *
 * Honors stored coordinates when valid, non-overlapping, and inside their page; otherwise flows
 * the widget into the first free slot on its page. A widget its page has no room for moves on to
 * the next page, after the widgets already there. With [compactPages], pages left empty are
 * dropped so later pages move up. Always returns widgets in their original input order.
 */
internal fun resolveWidgetGridLayout(
    widgets: List<PanelWidgetInfo>,
    specs: Map<Int, WidgetGridSpec>,
    pageRows: Int = Int.MAX_VALUE,
    compactPages: Boolean = true,
): List<PanelWidgetInfo> {
    if (widgets.isEmpty()) return widgets

    val placedById = LinkedHashMap<Int, PanelWidgetInfo>()
    val byPage = widgets.groupBy { it.pageOrZero() }
    val lastStoredPage = byPage.keys.max()
    var carried = emptyList<PanelWidgetInfo>()
    var page = 0
    while (page <= lastStoredPage || carried.isNotEmpty()) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        val native = byPage[page].orEmpty()
        val nextCarried = mutableListOf<PanelWidgetInfo>()
        (native + carried).forEachIndexed { index, widget ->
            val spec = specs[widget.appWidgetId]
            val minColumnSpan = spec?.minColumnSpan ?: 1
            val minRowSpan = spec?.minRowSpan ?: 1
            val columnSpan =
                (widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
                    .coerceIn(minColumnSpan, WIDGET_PANEL_GRID_COLUMNS)
            val rowSpan =
                (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
                    .coerceIn(minRowSpan, WIDGET_PANEL_MAX_ROW_SPAN)
                    .coerceAtMost(maxOf(pageRows, minRowSpan))
            val maxRow = pageRows - rowSpan
            val requestedColumn = widget.column?.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
            val requestedRow = widget.row?.coerceAtLeast(0)
            val position =
                if (
                    index < native.size &&
                    requestedColumn != null &&
                    requestedRow != null &&
                    requestedRow <= maxRow &&
                    canPlace(occupied, requestedColumn, requestedRow, columnSpan, rowSpan)
                ) {
                    requestedColumn to requestedRow
                } else {
                    findFirstAvailablePosition(occupied, columnSpan, rowSpan, maxRow)
                        // Taller than a page: it gets an empty page to itself.
                        ?: (0 to 0).takeIf { occupied.isEmpty() }
                }
            if (position == null) {
                nextCarried += widget
                return@forEachIndexed
            }
            markOccupied(occupied, position.first, position.second, columnSpan, rowSpan)
            placedById[widget.appWidgetId] =
                widget.copy(
                    column = position.first,
                    row = position.second,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                    panelPage = page.takeIf { it > 0 },
                )
        }
        carried = nextCarried
        page++
    }

    val placed = widgets.map { placedById[it.appWidgetId] ?: it }
    if (!compactPages) return placed
    // Pages left empty close, except the first, which always stays.
    val usedPages =
        (placed.mapTo(sortedSetOf()) { it.pageOrZero() } + 0)
            .sorted()
            .withIndex()
            .associate { (index, used) -> used to index }
    return placed.map { widget ->
        val compacted = usedPages.getValue(widget.pageOrZero())
        if (compacted == widget.pageOrZero()) widget else widget.copy(panelPage = compacted.takeIf { it > 0 })
    }
}

/**
 * Places the dragged widget at the requested cell, keeping any empty space the user left between
 * widgets. Widgets the dragged one doesn't touch stay exactly where they are.
 *
 * When the target lands on exactly one same-sized widget, the two swap. Otherwise each widget the
 * dragged one now covers moves, within its own column, to the nearest rows where it fits (upward
 * on a tie, so it can take the space the dragged widget left). Any widget, including the last,
 * can move down to leave empty space above it.
 *
 * When that would push a covered widget past the bottom of a page [pageRows] tall, the widgets
 * between the dragged one's old and new rows shift past it instead (see [reorderWithinColumns]),
 * so a widget dragged up stays up rather than being pushed back down when the page is resolved.
 */
internal fun moveWidgetToCell(
    widgets: List<PanelWidgetInfo>,
    appWidgetId: Int,
    targetColumn: Int,
    targetRow: Int,
    targetPage: Int? = null,
    pageRows: Int = Int.MAX_VALUE,
): List<PanelWidgetInfo> {
    val current = widgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return widgets
    val columnSpan = current.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan = current.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val page = targetPage ?: current.pageOrZero()
    // Widgets on other pages are never touched.
    val others = widgets.filter { it.appWidgetId != appWidgetId && it.pageOrZero() == page }
    val clampedTargetColumn = targetColumn.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
    val clampedTargetRow = targetRow.coerceAtLeast(0)
    val moved =
        current.copy(column = clampedTargetColumn, row = clampedTargetRow, panelPage = page.takeIf { it > 0 })
    if (moved == current) return widgets

    val overlapping = others.filter { widgetsOverlap(it, moved) }
    if (overlapping.isEmpty()) {
        return widgets.map { if (it.appWidgetId == appWidgetId) moved else it }
    }

    // Fast path: if the target rect lands on exactly one same-sized widget, swap the two. Such a
    // swap is always valid: the two rects were each in-bounds and occupied only by the other.
    val swapTarget = overlapping.singleOrNull()
    if (
        swapTarget != null &&
        current.pageOrZero() == page &&
        (swapTarget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN) == columnSpan &&
        (swapTarget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN) == rowSpan
    ) {
        return widgets.map { item ->
            when (item.appWidgetId) {
                appWidgetId -> current.copy(column = swapTarget.column, row = swapTarget.row)
                swapTarget.appWidgetId -> swapTarget.copy(column = current.column, row = current.row)
                else -> item
            }
        }
    }

    val displacedIds = overlapping.mapTo(mutableSetOf()) { it.appWidgetId }
    val occupied = mutableSetOf<Pair<Int, Int>>()
    markOccupied(occupied, clampedTargetColumn, clampedTargetRow, columnSpan, rowSpan)
    others
        .filterNot { it.appWidgetId in displacedIds }
        .forEach { item ->
            markOccupied(
                occupied,
                item.column ?: 0,
                item.rowOrZero(),
                item.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN,
                item.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN,
            )
        }

    val placedById = mutableMapOf(appWidgetId to moved)
    overlapping.sortedWith(compareBy({ it.rowOrZero() }, { it.column ?: 0 })).forEach { item ->
        val itemColumnSpan = item.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
        val itemRowSpan = item.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
        val itemColumn = (item.column ?: 0).coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - itemColumnSpan)
        val row = nearestFreeRow(occupied, itemColumn, item.rowOrZero(), itemColumnSpan, itemRowSpan)
        markOccupied(occupied, itemColumn, row, itemColumnSpan, itemRowSpan)
        placedById[item.appWidgetId] = item.copy(column = itemColumn, row = row)
    }
    val overflowsPage = placedById.values.any { it.appWidgetId != appWidgetId && it.bottomRow() > pageRows }
    if (overflowsPage && current.pageOrZero() == page && clampedTargetColumn == current.column) {
        reorderWithinColumns(widgets, current, clampedTargetRow)?.let { return it }
    }
    return widgets.map { placedById[it.appWidgetId] ?: it }
}

/**
 * Moves [current] vertically to [targetRow] by sliding the widgets between its old and new rows
 * past it, like reordering a list. Only widgets that sit entirely within [current]'s columns can
 * slide; returns null when another widget straddles those columns in the way.
 */
private fun reorderWithinColumns(
    widgets: List<PanelWidgetInfo>,
    current: PanelWidgetInfo,
    targetRow: Int,
): List<PanelWidgetInfo>? {
    val column = current.column ?: 0
    val columnSpan = current.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan = current.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val movingUp = targetRow < current.rowOrZero()
    if (targetRow == current.rowOrZero()) return null
    val band =
        widgets.filter { other ->
            other.appWidgetId != current.appWidgetId &&
                other.pageOrZero() == current.pageOrZero() &&
                horizontallyOverlaps(other, current) &&
                if (movingUp) {
                    other.bottomRow() > targetRow && other.rowOrZero() < current.rowOrZero()
                } else {
                    other.rowOrZero() < targetRow + rowSpan && other.rowOrZero() >= current.bottomRow()
                }
        }
    if (band.isEmpty()) return null
    val fitsColumns =
        band.all { other ->
            val otherColumn = other.column ?: 0
            otherColumn >= column &&
                otherColumn + (other.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN) <= column + columnSpan
        }
    if (!fitsColumns) return null
    val bandIds = band.mapTo(mutableSetOf()) { it.appWidgetId }
    val newRow =
        if (movingUp) {
            minOf(targetRow, band.minOf { it.rowOrZero() })
        } else {
            maxOf(targetRow + rowSpan, band.maxOf { it.bottomRow() }) - rowSpan
        }
    val shift = if (movingUp) rowSpan else -rowSpan
    val result =
        widgets.map { item ->
            when {
                item.appWidgetId == current.appWidgetId -> current.copy(row = newRow)
                item.appWidgetId in bandIds -> item.copy(row = item.rowOrZero() + shift)
                else -> item
            }
        }
    val onPage = result.filter { it.pageOrZero() == current.pageOrZero() }
    val overlaps = onPage.indices.any { i -> (i + 1 until onPage.size).any { j -> widgetsOverlap(onPage[i], onPage[j]) } }
    return result.takeUnless { overlaps }
}

/** The free row closest to [preferredRow] for a rect in [column]; upward wins a tie. */
private fun nearestFreeRow(
    occupied: Set<Pair<Int, Int>>,
    column: Int,
    preferredRow: Int,
    columnSpan: Int,
    rowSpan: Int,
): Int {
    var distance = 0
    while (true) {
        val up = preferredRow - distance
        if (up >= 0 && canPlace(occupied, column, up, columnSpan, rowSpan)) return up
        val down = preferredRow + distance
        if (distance > 0 && canPlace(occupied, column, down, columnSpan, rowSpan)) return down
        distance++
    }
}

/** Move by one column or one row; the result keeps any empty space between widgets. */
internal fun moveWidgetByStep(
    widgets: List<PanelWidgetInfo>,
    appWidgetId: Int,
    columnDelta: Int,
    rowDelta: Int,
    pageRows: Int = Int.MAX_VALUE,
): List<PanelWidgetInfo>? {
    val widget = widgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return null
    if (columnDelta == 0 && rowDelta == 0) return null
    val moved =
        moveWidgetToCell(
            widgets,
            appWidgetId,
            (widget.column ?: 0) + columnDelta.coerceIn(-1, 1),
            widget.rowOrZero() + rowDelta.coerceIn(-1, 1),
            pageRows = pageRows,
        )
    return moved.takeUnless { it == widgets }
}

/**
 * Applies a resize to a widget, clamping to the panel bounds. When the new bounds would overlap
 * other widgets, those widgets are pushed downward to make room. If a widget *above* the anchor
 * would need to move (the anchor grew upward into it), the resize is rejected (we can't push
 * widgets above off the top of the grid).
 */
internal fun resizeWidgetInGrid(
    widgets: List<PanelWidgetInfo>,
    appWidgetId: Int,
    resize: WidgetGridResize,
    specs: Map<Int, WidgetGridSpec>,
): List<PanelWidgetInfo> {
    val current = widgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return widgets
    val spec = specs[appWidgetId]
    val minColumnSpan = spec?.minColumnSpan ?: 1
    val minRowSpan = spec?.minRowSpan ?: 1
    val columnSpan = resize.columnSpan.coerceIn(minColumnSpan, WIDGET_PANEL_GRID_COLUMNS)
    val rowSpan = resize.rowSpan.coerceIn(minRowSpan, WIDGET_PANEL_MAX_ROW_SPAN)
    val column = resize.column.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
    val row = resize.row.coerceAtLeast(0)

    val updated =
        current.copy(column = column, row = row, columnSpan = columnSpan, rowSpan = rowSpan)
    val anchorTopBefore = current.row ?: 0

    // Reject if anchor's top moved upward into an existing widget that we'd need to push up.
    if (updated.rowOrZero() < anchorTopBefore) {
        val blocksAbove =
            widgets.any { other ->
                other.appWidgetId != appWidgetId &&
                    other.pageOrZero() == updated.pageOrZero() &&
                    horizontallyOverlaps(other, updated) &&
                    other.rowOrZero() < updated.rowOrZero() &&
                    other.bottomRow() > updated.rowOrZero()
            }
        if (blocksAbove) return widgets
    }

    val updatedList = widgets.map { if (it.appWidgetId == appWidgetId) updated else it }
    return cascadePushDown(updatedList, anchorAppWidgetId = appWidgetId)
}

private fun cascadePushDown(
    widgets: List<PanelWidgetInfo>,
    anchorAppWidgetId: Int,
): List<PanelWidgetInfo> {
    val byId = widgets.associateBy { it.appWidgetId }.toMutableMap()
    val anchor = byId[anchorAppWidgetId] ?: return widgets
    val occupied = mutableSetOf<Pair<Int, Int>>()
    markOccupied(
        occupied = occupied,
        column = anchor.column ?: 0,
        row = anchor.row ?: 0,
        columnSpan = anchor.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN,
        rowSpan = anchor.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN,
    )

    val others =
        byId.values
            .filter { it.appWidgetId != anchorAppWidgetId && it.pageOrZero() == anchor.pageOrZero() }
            .sortedBy { it.row ?: 0 }

    for (widget in others) {
        val colSpan = widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
        val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
        val col = widget.column ?: 0
        var row = widget.row ?: 0
        while (!canPlace(occupied, col, row, colSpan, rowSpan)) {
            row++
        }
        markOccupied(occupied, col, row, colSpan, rowSpan)
        if (row != (widget.row ?: 0)) {
            byId[widget.appWidgetId] = widget.copy(row = row)
        }
    }

    return widgets.map { byId[it.appWidgetId] ?: it }
}

private fun PanelWidgetInfo.rowOrZero(): Int = row ?: 0
internal fun PanelWidgetInfo.pageOrZero(): Int = panelPage ?: 0
private fun PanelWidgetInfo.bottomRow(): Int =
    (row ?: 0) + (rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)

private fun widgetsOverlap(a: PanelWidgetInfo, b: PanelWidgetInfo): Boolean =
    a.pageOrZero() == b.pageOrZero() &&
        horizontallyOverlaps(a, b) && a.rowOrZero() < b.bottomRow() && b.rowOrZero() < a.bottomRow()

private fun horizontallyOverlaps(a: PanelWidgetInfo, b: PanelWidgetInfo): Boolean {
    val aLeft = a.column ?: 0
    val aRight = aLeft + (a.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
    val bLeft = b.column ?: 0
    val bRight = bLeft + (b.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
    return aLeft < bRight && bLeft < aRight
}

internal fun calculateGridColumnSpan(
    minWidthDp: Float,
    cellWidthDp: Float,
    gapDp: Float,
): Int {
    return dpToGridSpan(
        sizeDp = minWidthDp,
        cellSizeDp = cellWidthDp,
        gapDp = gapDp,
        fallback = 1,
    ).coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
}

internal fun calculateGridRowSpan(
    minHeightDp: Float,
    rowHeightDp: Float,
    gapDp: Float,
): Int =
    dpToGridSpan(
        sizeDp = minHeightDp,
        cellSizeDp = rowHeightDp,
        gapDp = gapDp,
        fallback = 1,
    ).coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN)

/**
 * Computes a proposed resize given the start state, the active handle direction, and a cumulative
 * drag delta in pixels.
 */
internal fun calculateGridResize(
    startColumn: Int,
    startRow: Int,
    startColumnSpan: Int,
    startRowSpan: Int,
    horizontalDirection: Int,
    verticalDirection: Int,
    horizontalDeltaPx: Float,
    verticalDeltaPx: Float,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
): WidgetGridResize {
    val columnDelta =
        if (horizontalDirection == 0 || gridUnitWidthPx <= 0f) {
            0
        } else {
            (horizontalDeltaPx / gridUnitWidthPx).roundToInt()
        }
    val rowDelta =
        if (verticalDirection == 0 || gridUnitHeightPx <= 0f) {
            0
        } else {
            (verticalDeltaPx / gridUnitHeightPx).roundToInt()
        }

    var nextColumn = startColumn
    var nextRow = startRow
    var nextColumnSpan = startColumnSpan
    var nextRowSpan = startRowSpan

    when (horizontalDirection) {
        -1 -> {
            val maxLeftDelta = (startColumnSpan - minColumnSpan).coerceAtLeast(0)
            val appliedDelta = columnDelta.coerceIn(-startColumn.coerceAtLeast(0), maxLeftDelta)
            nextColumn = startColumn + appliedDelta
            nextColumnSpan = startColumnSpan - appliedDelta
        }

        1 -> {
            // A stored position can leave less room than the widget's min span; never build an
            // empty range from that.
            val maxColumnSpan = (WIDGET_PANEL_GRID_COLUMNS - startColumn).coerceAtLeast(1)
            nextColumnSpan =
                (startColumnSpan + columnDelta)
                    .coerceIn(minColumnSpan.coerceAtMost(maxColumnSpan), maxColumnSpan)
        }
    }

    when (verticalDirection) {
        -1 -> {
            val maxUpDelta = (startRowSpan - minRowSpan).coerceAtLeast(0)
            val appliedDelta = rowDelta.coerceIn(-startRow.coerceAtLeast(0), maxUpDelta)
            nextRow = startRow + appliedDelta
            nextRowSpan = startRowSpan - appliedDelta
        }

        1 -> {
            nextRowSpan =
                (startRowSpan + rowDelta)
                    .coerceIn(minRowSpan.coerceAtMost(WIDGET_PANEL_MAX_ROW_SPAN), WIDGET_PANEL_MAX_ROW_SPAN)
        }
    }

    return WidgetGridResize(
        column = nextColumn,
        row = nextRow,
        columnSpan = nextColumnSpan,
        rowSpan = nextRowSpan,
    )
}

private fun dpToGridSpan(
    sizeDp: Float,
    cellSizeDp: Float,
    gapDp: Float,
    fallback: Int,
): Int {
    if (cellSizeDp <= 0f) return fallback
    val rawSpan = ceil((sizeDp + gapDp) / (cellSizeDp + gapDp)).toInt()
    return max(1, rawSpan)
}

/** First free slot whose top row is at most [maxRow], or null when there is none. */
internal fun findFirstAvailablePosition(
    occupied: Set<Pair<Int, Int>>,
    columnSpan: Int,
    rowSpan: Int,
    maxRow: Int = Int.MAX_VALUE,
): Pair<Int, Int>? {
    var row = 0
    while (row <= maxRow) {
        for (column in 0..(WIDGET_PANEL_GRID_COLUMNS - columnSpan)) {
            if (canPlace(occupied, column, row, columnSpan, rowSpan)) {
                return column to row
            }
        }
        row++
    }
    return null
}

internal fun canPlace(
    occupied: Set<Pair<Int, Int>>,
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
): Boolean {
    for (occupiedColumn in column until column + columnSpan) {
        for (occupiedRow in row until row + rowSpan) {
            if ((occupiedColumn to occupiedRow) in occupied) return false
        }
    }
    return true
}

internal fun markOccupied(
    occupied: MutableSet<Pair<Int, Int>>,
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
) {
    for (occupiedColumn in column until column + columnSpan) {
        for (occupiedRow in row until row + rowSpan) {
            occupied += occupiedColumn to occupiedRow
        }
    }
}
