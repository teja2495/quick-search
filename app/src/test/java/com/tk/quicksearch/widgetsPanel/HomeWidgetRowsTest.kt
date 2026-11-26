package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.search.core.ItemPriorityConfig.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeWidgetRowsTest {
    private val order = listOf(ItemType.APPS_SECTION, ItemType.UPCOMING_ALARM, ItemType.NOTES_SECTION)

    private fun placement(
        column: Int,
        columnSpan: Int = 2,
        sharesRow: Boolean = false,
        order: Int = 0,
    ) = HomeWidgetPlacement(
        anchor = ItemType.NOTES_SECTION.name,
        order = order,
        column = column,
        columnSpan = columnSpan,
        rowSpan = 1,
        sharesRow = sharesRow,
    )

    private fun widget(
        id: Int,
        home: HomeWidgetPlacement,
    ) = PanelWidgetInfo(appWidgetId = id, providerPackage = "pkg", providerClassName = "cls", home = home)

    @Test
    fun widgetJoinsThePreviousRowOnlyWhenItAsksAndFits() {
        val leaders =
            homeRowLeaders(
                listOf(
                    null,
                    placement(column = 0),
                    placement(column = 2, sharesRow = true),
                    placement(column = 1, sharesRow = true),
                    placement(column = 0, columnSpan = 1, sharesRow = true),
                ),
            )

        // 2 fits beside 1; 3 overlaps both, so it starts a row that 4 can join.
        assertEquals(listOf(0, 1, 1, 3, 3), leaders.toList())
    }

    @Test
    fun widgetAfterAHomeItemCannotShareARow() {
        assertEquals(listOf(0, 1), homeRowLeaders(listOf(null, placement(column = 2, sharesRow = true))).toList())
    }

    @Test
    fun rowMembersShareOneSlotAsTallAsTheTallest() {
        val stack =
            placeHomeStack(
                heights = intArrayOf(100, 60, 80, 40),
                placements = listOf(null, placement(column = 0), placement(column = 2, sharesRow = true), placement(0)),
                widgetIds = listOf(null, 1, 2, 3),
                isReversed = false,
                spacingPx = 10,
                gapStepPx = 8,
            )

        assertEquals(listOf(0, 110, 110, 200), stack.positions.toList())
        assertEquals(240, stack.height)
    }

    @Test
    fun reversedRowsAlignTowardTheAnchoredEnd() {
        // Visual order in one-handed mode: widget 2, widget 1, Home item.
        val stack =
            placeHomeStack(
                heights = intArrayOf(40, 80, 100),
                placements = listOf(placement(column = 2, sharesRow = true), placement(column = 0), null),
                widgetIds = listOf(2, 1, null),
                isReversed = true,
                spacingPx = 10,
                gapStepPx = 8,
            )

        assertEquals(listOf(40, 0, 90), stack.positions.toList())
        assertEquals(190, stack.height)
    }

    @Test
    fun dragLevelWithAWidgetJoinsItsRowWhenThereIsRoom() {
        val visual = homeLayoutEntries(order, isReversed = false, widgets = listOf(widget(1, placement(column = 0))))
        val heights = mapOf<Any, Int>(ItemType.APPS_SECTION to 100, ItemType.UPCOMING_ALARM to 50, 1 to 80, ItemType.NOTES_SECTION to 30)
        val tops = mapOf<Any, Float>(ItemType.APPS_SECTION to 0f, ItemType.UPCOMING_ALARM to 110f, 1 to 170f, ItemType.NOTES_SECTION to 260f)

        fun join(column: Int, centerY: Float) =
            homeRowJoinIndex(visual, false, { heights[it] ?: 0 }, { tops[it] }, centerY, column, columnSpan = 2)

        assertEquals(3, join(column = 2, centerY = 200f))
        assertNull(join(column = 1, centerY = 200f))
        assertNull(join(column = 2, centerY = 300f))
    }

    @Test
    fun movingARowLeaderAwayLeavesTheOtherMemberOnItsOwnRow() {
        val widgets =
            listOf(
                widget(1, placement(column = 0, order = 0)),
                widget(2, placement(column = 2, sharesRow = true, order = 1)),
            )
        val visual = homeLayoutEntries(order, isReversed = false, widgets = widgets)

        // Remaining: APPS, ALARM, 2, NOTES -> drop widget 1 at the end.
        val moved = moveHomeWidget(visual, widgetId = 1, targetIndex = 4, isReversed = false).associateBy { it.appWidgetId }

        assertFalse(moved.getValue(2).home!!.sharesRow)
    }

    @Test
    fun movingAnUnrelatedWidgetKeepsRows() {
        val widgets =
            listOf(
                widget(1, placement(column = 0, order = 0)),
                widget(2, placement(column = 2, sharesRow = true, order = 1)),
                widget(3, placement(column = 0, order = 2)),
            )
        val visual = homeLayoutEntries(order, isReversed = false, widgets = widgets)

        // Remaining: APPS, ALARM, 1, 2, NOTES -> drop widget 3 at the end.
        val moved = moveHomeWidget(visual, widgetId = 3, targetIndex = 5, isReversed = false).associateBy { it.appWidgetId }

        assertTrue(moved.getValue(2).home!!.sharesRow)
    }

    @Test
    fun insertingInsideARowSnapsPastIt() {
        val widgets =
            listOf(
                widget(1, placement(column = 0, order = 0)),
                widget(2, placement(column = 2, sharesRow = true, order = 1)),
            )
        val visual = homeLayoutEntries(order, isReversed = false, widgets = widgets)

        assertEquals(4, snapHomeInsertOutOfRows(visual, false, { 50 }, visualIndex = 3))
        assertEquals(2, snapHomeInsertOutOfRows(visual, false, { 50 }, visualIndex = 2))
    }
}
