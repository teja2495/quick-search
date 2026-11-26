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
 * Lays out widgets on the panel grid.
 *
 * Honors stored coordinates when valid and non-overlapping; otherwise flows the widget into the
 * first free slot. Always returns widgets in their original input order.
 */
internal fun resolveWidgetGridLayout(
    widgets: List<PanelWidgetInfo>,
    specs: Map<Int, WidgetGridSpec>,
): List<PanelWidgetInfo> {
    if (widgets.isEmpty()) return widgets

    val occupied = mutableSetOf<Pair<Int, Int>>()
    val placedById = LinkedHashMap<Int, PanelWidgetInfo>()

    widgets.forEach { widget ->
        val spec = specs[widget.appWidgetId]
        val minColumnSpan = spec?.minColumnSpan ?: 1
        val minRowSpan = spec?.minRowSpan ?: 1
        val columnSpan =
            (widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
                .coerceIn(minColumnSpan, WIDGET_PANEL_GRID_COLUMNS)
        val rowSpan =
            (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
                .coerceIn(minRowSpan, WIDGET_PANEL_MAX_ROW_SPAN)
        val requestedColumn = widget.column?.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
        val requestedRow = widget.row?.coerceAtLeast(0)
        val position =
            if (
                requestedColumn != null &&
                requestedRow != null &&
                canPlace(
                    occupied = occupied,
                    column = requestedColumn,
                    row = requestedRow,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            ) {
                requestedColumn to requestedRow
            } else {
                findFirstAvailablePosition(
                    occupied = occupied,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            }

        markOccupied(
            occupied = occupied,
            column = position.first,
            row = position.second,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
        )
        placedById[widget.appWidgetId] =
            widget.copy(
                column = position.first,
                row = position.second,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
            )
    }

    return widgets.map { placedById[it.appWidgetId] ?: it }
}

/**
 * Lowest row [dragged] may move to, fixed when the drag begins: just below the other widgets,
 * anywhere in the rows already on screen ([visibleBottomPx] from the grid top), or where it
 * started, whichever is lowest. Without this cap a widget dragged near the bottom edge grows the
 * grid, edge auto-scroll follows it, and it runs away downward.
 */
internal fun panelDragMaxRow(
    widgets: List<PanelWidgetInfo>,
    dragged: PanelWidgetInfo,
    visibleBottomPx: Float,
    gridUnitHeightPx: Float,
): Int {
    val visibleRows = if (visibleBottomPx.isFinite() && gridUnitHeightPx > 0f) (visibleBottomPx / gridUnitHeightPx).toInt() + 1 else 0
    val othersBottom =
        widgets
            .filter { it.appWidgetId != dragged.appWidgetId }
            .maxOfOrNull { it.rowOrZero() + (it.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN) } ?: 0
    val rowSpan = dragged.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    return maxOf(othersBottom, visibleRows - rowSpan, dragged.rowOrZero())
}

/**
 * Places the dragged widget at the requested cell, keeping any empty space the user left between
 * widgets. Widgets the dragged one doesn't touch stay exactly where they are.
 *
 * When the target lands on exactly one same-sized widget, the two swap. Otherwise each widget the
 * dragged one now covers moves, within its own column, to the nearest rows where it fits (upward
 * on a tie, so it can take the space the dragged widget left). Any widget, including the last,
 * can move down to leave empty space above it.
 */
internal fun moveWidgetToCell(
    widgets: List<PanelWidgetInfo>,
    appWidgetId: Int,
    targetColumn: Int,
    targetRow: Int,
): List<PanelWidgetInfo> {
    val current = widgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return widgets
    val columnSpan = current.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan = current.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val others = widgets.filterNot { it.appWidgetId == appWidgetId }
    val clampedTargetColumn = targetColumn.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
    val clampedTargetRow = targetRow.coerceAtLeast(0)
    val moved = current.copy(column = clampedTargetColumn, row = clampedTargetRow)
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
    return widgets.map { placedById[it.appWidgetId] ?: it }
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
): List<PanelWidgetInfo>? {
    val widget = widgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return null
    if (columnDelta == 0 && rowDelta == 0) return null
    val moved =
        moveWidgetToCell(
            widgets,
            appWidgetId,
            (widget.column ?: 0) + columnDelta.coerceIn(-1, 1),
            widget.rowOrZero() + rowDelta.coerceIn(-1, 1),
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
            .filter { it.appWidgetId != anchorAppWidgetId }
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
private fun PanelWidgetInfo.bottomRow(): Int =
    (row ?: 0) + (rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)

private fun widgetsOverlap(a: PanelWidgetInfo, b: PanelWidgetInfo): Boolean =
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

private fun findFirstAvailablePosition(
    occupied: Set<Pair<Int, Int>>,
    columnSpan: Int,
    rowSpan: Int,
): Pair<Int, Int> {
    var row = 0
    while (true) {
        for (column in 0..(WIDGET_PANEL_GRID_COLUMNS - columnSpan)) {
            if (canPlace(occupied, column, row, columnSpan, rowSpan)) {
                return column to row
            }
        }
        row++
    }
}

private fun canPlace(
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

private fun markOccupied(
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
