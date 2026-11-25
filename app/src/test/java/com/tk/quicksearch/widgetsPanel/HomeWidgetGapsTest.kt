package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.search.core.ItemPriorityConfig.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeWidgetGapsTest {
    private fun item(height: Int) = HomeGapSlot(height, null)

    private fun widget(
        id: Int,
        height: Int = 50,
    ) = HomeGapSlot(height, id)

    private fun widgetInfo(id: Int) =
        PanelWidgetInfo(appWidgetId = id, providerPackage = "pkg", providerClassName = "cls")

    @Test
    fun onlyWidgetsWithNothingButWidgetsBeyondThemCanHoldSpace() {
        val eligible = homeGapEligible(listOf(item(100), widget(1), item(40), widget(2), item(0))).toList()

        assertEquals(listOf(false, false, false, true, false), eligible)
    }

    @Test
    fun emptySectionsDoNotCloseTheOpenEnd() {
        val eligible = homeGapEligible(listOf(item(100), widget(1), item(0), widget(2), item(0)))

        assertTrue(eligible[1])
        assertTrue(eligible[3])
    }

    @Test
    fun hiddenWidgetsHoldNoSpace() {
        assertFalse(homeGapEligible(listOf(item(100), widget(1, height = 0)))[1])
    }

    @Test
    fun widgetsLandAtTheirTargetDistances() {
        val gaps =
            homeWidgetGapsPx(listOf(item(100), widget(1), widget(2), item(0)), spacingPx = 10) { id ->
                mapOf(1 to 300f, 2 to 400f)[id]
            }

        // 100 item + 10 spacing + 190 gap puts widget 1 at 300; 350 + 10 + 40 puts widget 2 at 400.
        assertEquals(mapOf(1 to 190f, 2 to 40f), gaps)
    }

    @Test
    fun aWidgetMovedOutwardPushesTheOnesBeyondIt() {
        val gaps =
            homeWidgetGapsPx(listOf(item(100), widget(1), widget(2)), spacingPx = 10) { id ->
                mapOf(1 to 380f, 2 to 400f)[id]
            }

        assertEquals(mapOf(1 to 270f, 2 to 0f), gaps)
    }

    @Test
    fun widgetsBetweenItemsAndWidgetsWithoutTargetsGetNoSpace() {
        val gaps =
            homeWidgetGapsPx(listOf(widget(1), item(100), widget(2)), spacingPx = 10) { id ->
                if (id == 1) 500f else null
            }

        assertEquals(mapOf(2 to 0f), gaps)
    }

    @Test
    fun reversedSlotsAreListedFromTheBottom() {
        val entries =
            listOf(
                HomeLayoutEntry.Widget(widgetInfo(7)),
                HomeLayoutEntry.Item(ItemType.APPS_SECTION),
            )
        val heights = mapOf<Any, Int>(7 to 50, ItemType.APPS_SECTION to 200)

        val slots = homeGapSlots(entries, isReversed = true) { key -> heights[key] ?: 0 }

        assertEquals(listOf(HomeGapSlot(0, null), HomeGapSlot(200, null), HomeGapSlot(50, 7), HomeGapSlot(0, null)), slots)
    }
}
