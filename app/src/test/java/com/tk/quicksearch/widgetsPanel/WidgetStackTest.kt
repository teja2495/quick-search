package com.tk.quicksearch.widgetsPanel

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStackTest {
    private fun page(id: Int) = StackedPanelWidget(id, "pkg$id", "cls$id")

    private fun widget(
        id: Int,
        stack: List<StackedPanelWidget> = emptyList(),
        home: HomeWidgetPlacement? = null,
    ) = PanelWidgetInfo(
        appWidgetId = id,
        providerPackage = "pkg$id",
        providerClassName = "cls$id",
        column = 1,
        row = 2,
        columnSpan = 2,
        rowSpan = 3,
        home = home,
        stack = stack,
    )

    private val homePlacement = HomeWidgetPlacement(HOME_WIDGET_ANCHOR_END, 0, 0, 4, 2)

    @Test
    fun removingLaterPageKeepsStack() {
        val result = listOf(widget(1, listOf(page(2), page(3)))).withoutStackPage(1, 2)
        assertEquals(listOf(widget(1, listOf(page(3)))), result)
    }

    @Test
    fun removingFirstPagePromotesNextPageInPlace() {
        val result = listOf(widget(9), widget(1, listOf(page(2), page(3)))).withoutStackPage(1, 1)
        assertEquals(listOf(widget(9), widget(2, listOf(page(3)))), result)
    }

    @Test
    fun stackOfTwoBecomesRegularWidget() {
        val result = listOf(widget(1, listOf(page(2)))).withoutStackPage(1, 1)
        assertEquals(listOf(widget(2)), result)
    }

    @Test
    fun firstPageOnHomeStaysOnHomeAlone() {
        val result = listOf(widget(1, listOf(page(2)), home = homePlacement)).withoutStackPage(1, 1)
        assertEquals(
            listOf(widget(2), widget(1, home = homePlacement).copy(isInPanel = false)),
            result,
        )
    }

    @Test
    fun removingRegularWidgetDropsIt() {
        assertEquals(emptyList<PanelWidgetInfo>(), listOf(widget(1)).withoutStackPage(1, 1))
    }

    @Test
    fun unboundPagesAreDroppedAndFirstPagePromoted() {
        val widgets = listOf(widget(1, listOf(page(2), page(3))), widget(4))
        assertEquals(
            listOf(widget(3), widget(4)),
            widgets.dropUnboundPages(setOf(1, 2)),
        )
    }

    @Test
    fun stackOwnerFindsStackedPage() {
        val widgets = listOf(widget(9), widget(1, listOf(page(2))))
        assertEquals(1, widgets.stackOwnerOf(2)?.appWidgetId)
        assertEquals(9, widgets.stackOwnerOf(9)?.appWidgetId)
    }

    @Test
    fun reorderingLaterPagesKeepsOwner() {
        val result = listOf(widget(1, listOf(page(2), page(3)))).withStackOrder(1, listOf(1, 3, 2))
        assertEquals(listOf(widget(1, listOf(page(3), page(2)))), result)
    }

    @Test
    fun newFirstPageTakesOverPlacement() {
        val result =
            listOf(widget(9), widget(1, listOf(page(2), page(3)), home = homePlacement))
                .withStackOrder(1, listOf(3, 1, 2))
        assertEquals(listOf(widget(9), widget(3, listOf(page(1), page(2)), home = homePlacement)), result)
    }

    @Test
    fun orderThatIsNotAReorderingIsIgnored() {
        val stack = listOf(widget(1, listOf(page(2))))
        assertEquals(stack, stack.withStackOrder(1, listOf(2)))
        assertEquals(stack, stack.withStackOrder(1, listOf(2, 5)))
        assertEquals(stack, stack.withStackOrder(1, listOf(2, 2)))
    }

    private val quickNotePage =
        StackedPanelWidget(QUICK_NOTE_PANEL_WIDGET_ID, QUICK_NOTE_PROVIDER, QUICK_NOTE_PROVIDER)

    @Test
    fun removingQuickNoteOwnerPromotesWidgetPage() {
        val quickNote = newQuickNotePanelWidget().copy(column = 0, row = 1, stack = listOf(page(2)))
        val result = listOf(quickNote).withoutStackPage(QUICK_NOTE_PANEL_WIDGET_ID, QUICK_NOTE_PANEL_WIDGET_ID)
        assertEquals(
            listOf(quickNote.copy(appWidgetId = 2, providerPackage = "pkg2", providerClassName = "cls2", stack = emptyList())),
            result,
        )
        assertEquals(false, result.containsQuickNote())
    }

    @Test
    fun quickNoteAsStackPageCounts() {
        val stack = listOf(widget(1, listOf(quickNotePage)))
        assertEquals(true, stack.containsQuickNote())
        assertEquals(listOf(widget(1)), stack.withoutStackPage(1, QUICK_NOTE_PANEL_WIDGET_ID))
        assertEquals(false, listOf(widget(1)).containsQuickNote())
    }

    @Test
    fun soloQuickNoteRemovedEntirely() {
        assertEquals(
            emptyList<PanelWidgetInfo>(),
            listOf(newQuickNotePanelWidget()).withoutStackPage(QUICK_NOTE_PANEL_WIDGET_ID, QUICK_NOTE_PANEL_WIDGET_ID),
        )
    }
}
