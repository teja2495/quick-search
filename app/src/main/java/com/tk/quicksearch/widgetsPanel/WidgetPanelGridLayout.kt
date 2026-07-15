package com.tk.quicksearch.widgetsPanel

import kotlin.math.abs
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

    return compactEmptyRows(widgets.map { placedById[it.appWidgetId] ?: it })
}

/**
 * Moves a widget to the requested grid cell and gives it priority over widgets already there.
 * Widgets displaced by the move are placed in the nearest available cell, making every crossed
 * cell respond during a drag instead of rejecting intermediate positions.
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
    val clampedTargetColumn = targetColumn.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
    val clampedTargetRow = targetRow.coerceAtLeast(0)
    val moved =
        current.copy(
            column = clampedTargetColumn,
            row = clampedTargetRow,
        )
    val crossedWidget =
        widgets
            .asSequence()
            .filterNot { it.appWidgetId == appWidgetId }
            .filter { widgetsOverlap(it, moved) }
            .minByOrNull { item ->
                abs((item.column ?: 0) - clampedTargetColumn) +
                    abs((item.row ?: 0) - clampedTargetRow)
            }
    if (crossedWidget != null) {
        val swappedLayout =
            widgets.map { item ->
                when (item.appWidgetId) {
                    appWidgetId ->
                        current.copy(
                            column = crossedWidget.column ?: 0,
                            row = crossedWidget.row ?: 0,
                        )
                    crossedWidget.appWidgetId ->
                        crossedWidget.copy(
                            column = current.column ?: 0,
                            row = current.row ?: 0,
                        )
                    else -> item
                }
            }
        if (isValidGridLayout(swappedLayout)) return swappedLayout
    }

    val occupied = mutableSetOf<Pair<Int, Int>>()
    markOccupied(
        occupied = occupied,
        column = clampedTargetColumn,
        row = clampedTargetRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
    )

    val placed = mutableMapOf(appWidgetId to moved)
    widgets.filterNot { it.appWidgetId == appWidgetId }.forEach { item ->
        val itemColumnSpan = item.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
        val itemRowSpan = item.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
        val itemColumn = (item.column ?: 0).coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - itemColumnSpan)
        val itemRow = (item.row ?: 0).coerceAtLeast(0)
        val position =
            if (canPlace(occupied, itemColumn, itemRow, itemColumnSpan, itemRowSpan)) {
                itemColumn to itemRow
            } else {
                findNearestAvailablePosition(
                    occupied = occupied,
                    targetColumn = itemColumn,
                    targetRow = itemRow,
                    columnSpan = itemColumnSpan,
                    rowSpan = itemRowSpan,
                )
            }
        markOccupied(
            occupied = occupied,
            column = position.first,
            row = position.second,
            columnSpan = itemColumnSpan,
            rowSpan = itemRowSpan,
        )
        placed[item.appWidgetId] = item.copy(column = position.first, row = position.second)
    }
    return widgets.map { placed[it.appWidgetId] ?: it }
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

private fun horizontallyOverlaps(a: PanelWidgetInfo, b: PanelWidgetInfo): Boolean {
    val aLeft = a.column ?: 0
    val aRight = aLeft + (a.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
    val bLeft = b.column ?: 0
    val bRight = bLeft + (b.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
    return aLeft < bRight && bLeft < aRight
}

private fun widgetsOverlap(a: PanelWidgetInfo, b: PanelWidgetInfo): Boolean =
    horizontallyOverlaps(a, b) && a.rowOrZero() < b.bottomRow() && b.rowOrZero() < a.bottomRow()

private fun isValidGridLayout(widgets: List<PanelWidgetInfo>): Boolean {
    widgets.forEachIndexed { index, widget ->
        val column = widget.column ?: 0
        val columnSpan = widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
        if (column < 0 || column + columnSpan > WIDGET_PANEL_GRID_COLUMNS || widget.rowOrZero() < 0) {
            return false
        }
        for (otherIndex in index + 1 until widgets.size) {
            if (widgetsOverlap(widget, widgets[otherIndex])) return false
        }
    }
    return true
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
            val maxLeftDelta = startColumnSpan - minColumnSpan
            val appliedDelta = columnDelta.coerceIn(-startColumn, maxLeftDelta)
            nextColumn = startColumn + appliedDelta
            nextColumnSpan = startColumnSpan - appliedDelta
        }

        1 -> {
            nextColumnSpan =
                (startColumnSpan + columnDelta)
                    .coerceIn(minColumnSpan, WIDGET_PANEL_GRID_COLUMNS - startColumn)
        }
    }

    when (verticalDirection) {
        -1 -> {
            val maxUpDelta = startRowSpan - minRowSpan
            val appliedDelta = rowDelta.coerceIn(-startRow, maxUpDelta)
            nextRow = startRow + appliedDelta
            nextRowSpan = startRowSpan - appliedDelta
        }

        1 -> {
            nextRowSpan =
                (startRowSpan + rowDelta)
                    .coerceIn(minRowSpan, WIDGET_PANEL_MAX_ROW_SPAN)
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

/** Removes fully empty grid rows while preserving widget order, spans, and columns. */
private fun compactEmptyRows(widgets: List<PanelWidgetInfo>): List<PanelWidgetInfo> {
    if (widgets.isEmpty()) return widgets

    val occupiedRows = mutableSetOf<Int>()
    widgets.forEach { widget ->
        val top = widget.row ?: 0
        val bottom = top + (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
        for (row in top until bottom) occupiedRows += row
    }

    val highestBottom = occupiedRows.maxOrNull()?.plus(1) ?: return widgets
    val emptyRowsBefore = IntArray(highestBottom)
    var emptyRowCount = 0
    for (row in 0 until highestBottom) {
        emptyRowsBefore[row] = emptyRowCount
        if (row !in occupiedRows) emptyRowCount++
    }
    if (emptyRowCount == 0) return widgets

    return widgets.map { widget ->
        val row = widget.row ?: 0
        widget.copy(row = row - emptyRowsBefore[row])
    }
}

private fun findNearestAvailablePosition(
    occupied: Set<Pair<Int, Int>>,
    targetColumn: Int,
    targetRow: Int,
    columnSpan: Int,
    rowSpan: Int,
): Pair<Int, Int> {
    var radius = 0
    val maxRows = (occupied.maxOfOrNull { it.second } ?: targetRow) + rowSpan + 2
    while (radius <= maxRows + WIDGET_PANEL_GRID_COLUMNS) {
        var best: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        for (row in (targetRow - radius)..(targetRow + radius)) {
            if (row < 0) continue
            for (column in 0..(WIDGET_PANEL_GRID_COLUMNS - columnSpan)) {
                val distance = abs(column - targetColumn) + abs(row - targetRow)
                if (distance > radius || distance >= bestDistance) continue
                if (canPlace(occupied, column, row, columnSpan, rowSpan)) {
                    best = column to row
                    bestDistance = distance
                }
            }
        }
        if (best != null) return best
        radius++
    }
    return targetColumn to targetRow
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
