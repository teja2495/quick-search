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
    fun `dragging a short widget below a tall one reorders without gaps`() {
        // Full-width stack: A (3 rows) on top, B (5 rows) below.
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 3)
        val b = widget(id = 2, column = 0, row = 3, columnSpan = 4, rowSpan = 5)

        // Drag A's top past B's center (B center row = 3 + 5/2 ≈ 5).
        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 0, targetRow = 6)

        // B should now be on top starting at row 0, A directly beneath it with no gap.
        assertEquals(0, result.byId(2).row)
        assertEquals(5, result.byId(1).row)
    }

    @Test
    fun `dragging a tall widget above a short one reorders without gaps`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 3)
        val b = widget(id = 2, column = 0, row = 3, columnSpan = 4, rowSpan = 5)

        // Drag B up above A's center (A center ≈ 1).
        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 2, targetColumn = 0, targetRow = 0)

        assertEquals(0, result.byId(2).row)
        assertEquals(5, result.byId(1).row)
    }

    @Test
    fun `equal-height vertical reorder still swaps`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 2)
        val b = widget(id = 2, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 0, targetRow = 3)

        assertEquals(0, result.byId(2).row)
        assertEquals(2, result.byId(1).row)
    }

    @Test
    fun `small vertical drag that does not pass a center leaves order unchanged`() {
        val a = widget(id = 1, column = 0, row = 0, columnSpan = 4, rowSpan = 3)
        val b = widget(id = 2, column = 0, row = 3, columnSpan = 4, rowSpan = 5)

        // Target row 1: A center = 1 + 3/2 ≈ 2, still above B center ≈ 5.
        val result = moveWidgetToCell(listOf(a, b), appWidgetId = 1, targetColumn = 0, targetRow = 1)

        assertEquals(0, result.byId(1).row)
        assertEquals(3, result.byId(2).row)
    }

    @Test
    fun `dragging one of a side-by-side pair does not collapse the other`() {
        // Two half-width widgets sharing the top row, plus one full-width below.
        val left = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val right = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)
        val bottom = widget(id = 3, column = 0, row = 2, columnSpan = 4, rowSpan = 2)

        // Drag the left widget down past the bottom widget's center.
        val result =
            moveWidgetToCell(listOf(left, right, bottom), appWidgetId = 1, targetColumn = 0, targetRow = 3)

        // The right widget must keep its own column — the pair does not collapse into a stack.
        assertEquals(2, result.byId(2).column)
        assertEquals(0, result.byId(2).row)
        // The dragged (narrow) widget keeps its column and the layout stays gap-free.
        assertEquals(0, result.byId(1).column)
        assertNoOverlapsOrGaps(result)
    }

    @Test
    fun `equal-height side-by-side widgets swap columns cleanly when dragged across`() {
        val left = widget(id = 1, column = 0, row = 0, columnSpan = 2, rowSpan = 2)
        val right = widget(id = 2, column = 2, row = 0, columnSpan = 2, rowSpan = 2)

        // Drag the left widget into the right widget's column; both stay on the top row.
        val result =
            moveWidgetToCell(listOf(left, right), appWidgetId = 1, targetColumn = 2, targetRow = 0)

        assertEquals(0, result.byId(1).row)
        assertEquals(0, result.byId(2).row)
        assertNoOverlapsOrGaps(result)
    }

    private fun assertNoOverlapsOrGaps(widgets: List<PanelWidgetInfo>) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        widgets.forEach { w ->
            val col = w.column ?: 0
            val row = w.row ?: 0
            val cSpan = w.columnSpan ?: 2
            val rSpan = w.rowSpan ?: 2
            for (c in col until col + cSpan) {
                for (r in row until row + rSpan) {
                    assertTrue("overlap at ($c,$r)", occupied.add(c to r))
                }
            }
        }
        val maxRow = occupied.maxOfOrNull { it.second } ?: return
        for (r in 0..maxRow) {
            assertTrue("empty row $r", occupied.any { it.second == r })
        }
    }
}
