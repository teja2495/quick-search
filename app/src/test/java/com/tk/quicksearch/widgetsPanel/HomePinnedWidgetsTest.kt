package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.search.core.ItemPriorityConfig.ItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePinnedWidgetsTest {
    private val order = listOf(ItemType.APPS_SECTION, ItemType.UPCOMING_ALARM, ItemType.NOTES_SECTION)

    private fun widget(
        id: Int,
        anchor: String,
        order: Int = 0,
    ) = PanelWidgetInfo(
        appWidgetId = id,
        providerPackage = "pkg",
        providerClassName = "cls",
        home = HomeWidgetPlacement(anchor = anchor, order = order, column = 0, columnSpan = 4, rowSpan = 2),
    )

    private fun List<HomeLayoutEntry>.keys(): List<Any> = map { it.key }

    @Test
    fun widgetsSitBeforeTheirAnchorAndUnknownAnchorsGoToTheEnd() {
        val entries =
            homeLayoutEntries(
                layoutOrder = order,
                isReversed = false,
                widgets =
                    listOf(
                        widget(2, ItemType.UPCOMING_ALARM.name, order = 1),
                        widget(1, ItemType.UPCOMING_ALARM.name, order = 0),
                        widget(3, "MISSING"),
                    ),
            )

        assertEquals(
            listOf(ItemType.APPS_SECTION, 1, 2, ItemType.UPCOMING_ALARM, ItemType.NOTES_SECTION, 3),
            entries.keys(),
        )
    }

    @Test
    fun reversedLayoutKeepsWidgetsNextToTheSameItem() {
        val entries =
            homeLayoutEntries(
                layoutOrder = order.reversed(),
                isReversed = true,
                widgets = listOf(widget(1, ItemType.UPCOMING_ALARM.name)),
            )

        assertEquals(
            listOf(ItemType.NOTES_SECTION, ItemType.UPCOMING_ALARM, 1, ItemType.APPS_SECTION),
            entries.keys(),
        )
    }

    @Test
    fun movingAWidgetRewritesAnchorsForItsNewNeighbours() {
        val widgets =
            listOf(
                widget(1, ItemType.UPCOMING_ALARM.name, order = 0),
                widget(2, ItemType.UPCOMING_ALARM.name, order = 1),
            )
        val visual = homeLayoutEntries(order, isReversed = false, widgets = widgets)

        // Remaining: APPS, 2, ALARM, NOTES -> drop widget 1 at the end.
        val moved = moveHomeWidget(visual, widgetId = 1, targetIndex = 4, isReversed = false)

        val placements = moved.associate { it.appWidgetId to it.home!!.let { home -> home.anchor to home.order } }
        assertEquals(ItemType.UPCOMING_ALARM.name to 0, placements[2])
        assertEquals(HOME_WIDGET_ANCHOR_END to 0, placements[1])
    }

    @Test
    fun movingAWidgetInReversedLayoutUsesLogicalAnchors() {
        val widgets = listOf(widget(1, ItemType.UPCOMING_ALARM.name))
        val visual = homeLayoutEntries(order.reversed(), isReversed = true, widgets = widgets)

        // Remaining visual: NOTES, ALARM, APPS -> place widget visually above NOTES (top).
        val moved = moveHomeWidget(visual, widgetId = 1, targetIndex = 0, isReversed = true)

        assertEquals(HOME_WIDGET_ANCHOR_END, moved.single().home!!.anchor)
    }
}
