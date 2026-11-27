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
    fun `a full-width widget dragged up on a full page stays up`() {
        val above = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 4)
        val note = widget(id = 2, column = 0, row = 4, columnSpan = 4, rowSpan = 2)
        val widgets = listOf(above, note)

        val moved = moveWidgetToCell(widgets, appWidgetId = 2, targetColumn = 0, targetRow = 3, pageRows = 6)
        val resolved = resolveWidgetGridLayout(moved, emptyMap(), pageRows = 6)

        assertEquals(0, resolved.byId(2).row)
        assertEquals(2, resolved.byId(1).row)
        assertNoOverlaps(resolved)
    }

    @Test
    fun `a full-width widget dragged up past a side-by-side pair on a full page stays up`() {
        val left = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val right = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)
        val note = widget(id = 3, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        val moved = moveWidgetToCell(listOf(left, right, note), appWidgetId = 3, targetColumn = 0, targetRow = 1, pageRows = 4)
        val resolved = resolveWidgetGridLayout(moved, emptyMap(), pageRows = 4)

        assertEquals(0, resolved.byId(3).row)
        assertEquals(2, resolved.byId(1).row)
        assertEquals(2, resolved.byId(2).row)
        assertNoOverlaps(resolved)
    }

    @Test
    fun `a full-width widget dragged down on a full page stays down`() {
        val note = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val below = widget(id = 2, column = 0, row = 2, columnSpan = 4, rowSpan = 4)

        val moved = moveWidgetToCell(listOf(note, below), appWidgetId = 1, targetColumn = 0, targetRow = 1, pageRows = 6)
        val resolved = resolveWidgetGridLayout(moved, emptyMap(), pageRows = 6)

        assertEquals(4, resolved.byId(1).row)
        assertEquals(0, resolved.byId(2).row)
        assertNoOverlaps(resolved)
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

    @Test
    fun `widgets that don't fit on a page carry to the next page`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 3)
        val b = widget(id = 2, column = 0, row = 3, columnSpan = 4, rowSpan = 3)

        val result = resolveWidgetGridLayout(listOf(a, b), emptyMap(), pageRows = 4)

        assertEquals(0, result.byId(1).pageOrZero())
        assertEquals(1, result.byId(2).pageOrZero())
        assertEquals(0, result.byId(2).row)
    }

    @Test
    fun `resolving drops pages left empty`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 2)

        val compacted = resolveWidgetGridLayout(listOf(a, b), emptyMap(), pageRows = 4)
        val kept = resolveWidgetGridLayout(listOf(a, b), emptyMap(), pageRows = 4, compactPages = false)

        assertEquals(1, compacted.byId(2).pageOrZero())
        assertEquals(2, kept.byId(2).pageOrZero())
    }

    @Test
    fun `widgets on different pages don't block each other`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 1)

        val result = resolveWidgetGridLayout(listOf(a, b), emptyMap(), pageRows = 4)

        assertEquals(0, result.byId(2).row)
        assertEquals(1, result.byId(2).pageOrZero())
        assertEquals(0, result.byId(1).row)
    }

    @Test
    fun `moving a widget to another page places it there`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 2, targetColumn = 0, targetRow = 0, targetPage = 1)

        assertEquals(1, result.byId(2).pageOrZero())
        assertEquals(0, result.byId(2).row)
        assertEquals(a, result.byId(1))
    }

    @Test
    fun `a new widget goes on the current page when it fits, otherwise on a new page`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val fits = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val tooTall = widget(id = 3, column = 0, row = 0, columnSpan = 4, rowSpan = 3)

        val placed = listOf(a, fits).withNewPanelWidgetPlaced(appWidgetId = 2, page = 0, pageRows = 4)
        val overflowed = listOf(a, tooTall).withNewPanelWidgetPlaced(appWidgetId = 3, page = 0, pageRows = 4)

        assertEquals(0, placed.byId(2).pageOrZero())
        assertEquals(2, placed.byId(2).row)
        assertEquals(1, overflowed.byId(3).pageOrZero())
        assertEquals(0, overflowed.byId(3).row)
    }

    @Test
    fun `dropping on the next page zone moves the widget to the next page's first free spot`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 1)

        val result =
            listOf(a, b).withPanelWidgetMovedToPage(1, WidgetPanelPageZone.Next, currentPage = 0, pageRows = 4)

        assertEquals(1, result.byId(1).pageOrZero())
        assertEquals(2, result.byId(1).row)
        assertEquals(b, result.byId(2))
    }

    @Test
    fun `dropping on a full previous page inserts a new page before the current one`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 4)
        val b = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 1)
        val c = widget(id = 3, column = 0, row = 2, columnSpan = 4, rowSpan = 2).copy(panelPage = 1)

        val result =
            listOf(a, b, c).withPanelWidgetMovedToPage(2, WidgetPanelPageZone.Previous, currentPage = 1, pageRows = 4)

        assertEquals(0, result.byId(1).pageOrZero())
        assertEquals(1, result.byId(2).pageOrZero())
        assertEquals(0, result.byId(2).row)
        assertEquals(2, result.byId(3).pageOrZero())
    }

    @Test
    fun `dropping on the new page zone starts a page after the current one`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val b = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)

        val result =
            listOf(a, b).withPanelWidgetMovedToPage(2, WidgetPanelPageZone.NewPage, currentPage = 0, pageRows = 4)

        assertEquals(0, result.byId(1).pageOrZero())
        assertEquals(1, result.byId(2).pageOrZero())
        assertEquals(0, result.byId(2).column)
    }

    @Test
    fun `an empty first page stays while later empty pages close`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 1)
        val b = widget(id = 2, column = 0, row = 0, columnSpan = 4, rowSpan = 2).copy(panelPage = 3)

        val result = resolveWidgetGridLayout(listOf(a, b), emptyMap(), pageRows = 4)

        assertEquals(1, result.byId(1).pageOrZero())
        assertEquals(2, result.byId(2).pageOrZero())
    }
}
