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

    private fun homeWidget(
        id: Int,
        stack: List<StackedPanelWidget> = emptyList(),
    ) = widget(id, stack, home = homePlacement).copy(isInPanel = false)

    @Test
    fun promotedPageKeepsStackOnHome() {
        val result = listOf(homeWidget(1, listOf(page(2), page(3)))).withoutStackPage(1, 1, onHome = true)
        assertEquals(listOf(homeWidget(2, listOf(page(3)))), result)
    }

    @Test
    fun removingSoloHomeWidgetDropsIt() {
        assertEquals(emptyList<PanelWidgetInfo>(), listOf(homeWidget(1)).withoutStackPage(1, 1, onHome = true))
    }

    @Test
    fun panelAndHomeRecordsAreEditedIndependently() {
        // A shared id is left over from before the panel and Home kept separate instances.
        val widgets = listOf(widget(1, listOf(page(2))), homeWidget(1, listOf(page(3))))
        assertEquals(listOf(widget(1, listOf(page(2))), homeWidget(1)), widgets.withoutStackPage(1, 3, onHome = true))
        assertEquals(listOf(widget(1), homeWidget(1, listOf(page(3)))), widgets.withoutStackPage(1, 2))
        assertEquals(
            listOf(widget(2, listOf(page(1))), homeWidget(1, listOf(page(3)))),
            widgets.withStackOrder(1, listOf(2, 1)),
        )
    }

    @Test
    fun sharedRecordSplitsIntoPanelAndHomeRecords() {
        val shared = widget(1, listOf(page(2)), home = homePlacement)
        assertEquals(listOf(widget(1, listOf(page(2))), homeWidget(1)), listOf(shared).splitSharedRecords())
        assertEquals(listOf(homeWidget(4)), listOf(homeWidget(4)).splitSharedRecords())
    }

    @Test
    fun widgetIdStaysReferencedWhileAnyRecordShowsIt() {
        val widgets = listOf(widget(1, listOf(page(2))), homeWidget(1))
        assertEquals(true, widgets.referencesWidgetId(2))
        assertEquals(true, widgets.withoutStackPage(1, 1).referencesWidgetId(1))
        assertEquals(false, widgets.withoutStackPage(1, 2).referencesWidgetId(2))
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
    fun unboundFirstPageKeepsStackOnHome() {
        val widgets = listOf(homeWidget(1, listOf(page(2))))
        assertEquals(listOf(homeWidget(2)), widgets.dropUnboundPages(setOf(1)))
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
            listOf(widget(9), homeWidget(1, listOf(page(2), page(3))))
                .withStackOrder(1, listOf(3, 1, 2), onHome = true)
        assertEquals(listOf(widget(9), homeWidget(3, listOf(page(1), page(2)))), result)
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

    @Test
    fun movingHomeWidgetToPanelKeepsStackAndSize() {
        val result = listOf(widget(9), homeWidget(1, listOf(page(2)))).withHomeWidgetMovedToPanel(1)
        val moved = widget(1, listOf(page(2))).copy(column = null, row = null, columnSpan = 4, rowSpan = 2)
        assertEquals(listOf(widget(9), moved), result)
    }

    @Test
    fun movingHomeWidgetAlreadyInPanelDropsDuplicatePages() {
        val panel = widget(1, listOf(page(3)))
        val result = listOf(panel, homeWidget(1, listOf(page(2), page(3)))).withHomeWidgetMovedToPanel(1)
        val moved = widget(2).copy(column = null, row = null, columnSpan = 4, rowSpan = 2)
        assertEquals(listOf(panel, moved), result)
    }

    @Test
    fun movingHomeWidgetFullyShownInPanelRemovesIt() {
        val panel = widget(1)
        val result = listOf(panel, homeWidget(1)).withHomeWidgetMovedToPanel(1)
        assertEquals(listOf(panel), result)
    }
}
