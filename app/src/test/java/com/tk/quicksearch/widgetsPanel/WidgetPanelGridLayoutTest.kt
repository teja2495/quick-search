package com.tk.quicksearch.widgetsPanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPanelGridLayoutTest {

    private fun widget(
        id: Int,
        column: Int,
        row: Int,
        columnSpan: Int,
        rowSpan: Int,
    ) = PanelWidgetInfo(
        appWidgetId = id,
        providerPackage = "pkg",
        providerClassName = "cls",
        column = column,
        row = row,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
    )

    private fun List<PanelWidgetInfo>.byId(id: Int) = first { it.appWidgetId == id }

    @Test
    fun `dropping into empty space keeps the gaps and leaves other widgets alone`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 5, columnSpan = 4, rowSpan = 2)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 2, targetRow = 2)

        assertEquals(2, result.byId(1).column)
        assertEquals(2, result.byId(1).row)
        assertEquals(b, result.byId(2))
    }

    @Test
    fun `the bottom widget can move down to leave space above it`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 2, columnSpan = 4, rowSpan = 3)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 2, targetColumn = 0, targetRow = 6)

        assertEquals(0, result.byId(1).row)
        assertEquals(6, result.byId(2).row)
        assertEquals(7, checkNotNull(moveWidgetByStep(result, 2, columnDelta = 0, rowDelta = 1)).byId(2).row)
    }

    @Test
    fun `resolving a layout keeps empty rows between widgets`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 4, columnSpan = 4, rowSpan = 2)

        val result = resolveWidgetGridLayout(listOf(a, b), emptyMap())

        assertEquals(4, result.byId(2).row)
    }

    @Test
    fun `a covered widget moves to the nearest free rows in its column`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 3)
        val b = widget(id = 2, column = 0, row = 3, columnSpan = 4, rowSpan = 5)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 0, targetRow = 6)

        assertEquals(6, result.byId(1).row)
        assertEquals(1, result.byId(2).row)
        assertNoOverlaps(result)
    }

    @Test
    fun `equal-size widgets swap`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 0, targetRow = 3)

        assertEquals(0, result.byId(2).row)
        assertEquals(2, result.byId(1).row)
    }

    @Test
    fun `an accessible move steps one row and can come back`() {
        val first = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val second = widget(id = 2, column = 0, row = 4, columnSpan = 4, rowSpan = 2)
        val widgets = listOf(first, second)

        val moved = checkNotNull(moveWidgetByStep(widgets, first.appWidgetId, columnDelta = 0, rowDelta = 1))
        assertEquals(1, moved.byId(first.appWidgetId).row)
        assertEquals(4, moved.byId(second.appWidgetId).row)
        assertEquals(null, moveWidgetByStep(widgets, first.appWidgetId, columnDelta = 0, rowDelta = -1))

        val restored = moveWidgetByStep(moved, first.appWidgetId, columnDelta = 0, rowDelta = -1)
        assertEquals(widgets, restored)
    }

    @Test
    fun `dragging one of a side-by-side pair does not move the other`() {
        val left = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val right = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)
        val bottom = widget(id = 3, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        val result =
            moveWidgetToCell(listOf(left, right, bottom), appWidgetId = 1, targetColumn = 0, targetRow = 3)

        assertEquals(right, result.byId(2))
        assertEquals(0, result.byId(1).column)
        assertNoOverlaps(result)
    }

    @Test
    fun `equal-height side-by-side widgets swap columns cleanly when dragged across`() {
        val left = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val right = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)

        val result =
            moveWidgetToCell(listOf(left, right), appWidgetId = 1, targetColumn = 2, targetRow = 0)

        assertEquals(2, result.byId(1).column)
        assertEquals(0, result.byId(2).column)
        assertNoOverlaps(result)
    }

    private fun assertNoOverlaps(widgets: List<PanelWidgetInfo>) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        widgets.forEach { w ->
            val col = w.column ?: 0
            val row = w.row ?: 0
            for (c in col until col + (w.columnSpan ?: 2)) {
                for (r in row until row + (w.rowSpan ?: 2)) {
                    assertTrue("overlap at ($c,$r)", occupied.add(c to r))
                }
            }
        }
    }

    @Test
    fun `resize does not crash when min span exceeds room left of position`() {
        // Widget with min span 4 stored at column 1 (only 3 columns of room).
        for (horizontal in listOf(-1, 1)) {
            val result =
                calculateGridResize(
                    startColumn = 1,
                    startRow = 0,
                    startColumnSpan = 3,
                    startRowSpan = 2,
                    horizontalDirection = horizontal,
                    verticalDirection = -1,
                    horizontalDeltaPx = 50f,
                    verticalDeltaPx = 50f,
                    gridUnitWidthPx = 100f,
                    gridUnitHeightPx = 100f,
                    minColumnSpan = 4,
                    minRowSpan = 3,
                )
            assertTrue(result.column + result.columnSpan <= WIDGET_PANEL_GRID_COLUMNS)
        }
    }
}
