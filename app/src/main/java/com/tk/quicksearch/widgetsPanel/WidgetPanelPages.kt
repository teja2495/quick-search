package com.tk.quicksearch.widgetsPanel

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppMotion
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Page slides use the grid's layout motion, so a widget moved to the next page keeps pace with it. */
private val WidgetPanelPageMotion = AppMotion.settle<Float>()

/** Drop zones at the panel's top and bottom edges while a widget is dragged. */
internal enum class WidgetPanelPageZone { Previous, Next, NewPage }

/**
 * The widgets panel's vertical pages. Each page is one viewport tall and the panel only rests on
 * page boundaries: [WidgetPanelPageFling] snaps every swipe to a page.
 */
@Stable
internal class WidgetPanelPagerState(
    val scrollState: ScrollState,
) {
    /** Height of one page: the panel's viewport, kept while the keyboard shrinks it. */
    var pageHeightPx by mutableIntStateOf(0)

    /** Grid rows that fit on one page. */
    var pageRows by mutableIntStateOf(0)

    var pageCount by mutableIntStateOf(1)

    /** The page the panel rests on, or is sliding to. */
    var targetPage by mutableIntStateOf(0)

    /** The page zones a dragged widget can enter; empty when nothing is dragged. */
    var dragZones by mutableStateOf(emptySet<WidgetPanelPageZone>())

    /** The page zone the dragged widget is in. */
    var hoveredZone by mutableStateOf<WidgetPanelPageZone?>(null)

    val isReady: Boolean get() = pageHeightPx > 0 && pageRows > 0

    fun clearDragZones() {
        dragZones = emptySet()
        hoveredZone = null
    }

    /** Pages at least partly in view. */
    fun visiblePages(viewportHeightPx: Int): IntRange {
        if (pageHeightPx <= 0) return 0..0
        val top = scrollState.value
        return (top / pageHeightPx)..((top + viewportHeightPx.coerceAtLeast(1) - 1) / pageHeightPx)
    }

    /** Moves to [page] without a slide, for when the pages around the one in view change. */
    suspend fun jumpToPage(page: Int) {
        if (pageHeightPx <= 0) return
        targetPage = page.coerceAtLeast(0)
        scrollState.scrollTo(targetPage * pageHeightPx)
    }

    suspend fun animateToPage(page: Int) {
        if (pageHeightPx <= 0) return
        val target = page.coerceAtLeast(0)
        targetPage = target
        // Scrolls by delta so a page added this frame, before the content grows, is still reached.
        scrollState.animateScrollBy((target * pageHeightPx - scrollState.value).toFloat(), WidgetPanelPageMotion)
    }
}

/** Settles a swipe on the next page in the fling's direction, or the nearest page for a slow drag. */
internal class WidgetPanelPageFling(
    private val pager: WidgetPanelPagerState,
    private val velocityThresholdPx: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val height = pager.pageHeightPx
        if (height <= 0) return initialVelocity
        val value = pager.scrollState.value.toFloat()
        val position = value / height
        val lastPage = ((pager.scrollState.maxValue + height / 2) / height).coerceAtLeast(0)
        val target =
            when {
                initialVelocity > velocityThresholdPx -> floor(position).toInt() + 1
                initialVelocity < -velocityThresholdPx -> ceil(position).toInt() - 1
                else -> position.roundToInt()
            }.coerceIn(0, lastPage)
        pager.targetPage = target
        var previous = 0f
        animate(0f, target * height - value, initialVelocity, WidgetPanelPageMotion) { current, _ ->
            previous += scrollBy(current - previous)
        }
        return 0f
    }
}

/** Height of one panel page, so grid items can offset themselves onto their page. */
internal val LocalWidgetPanelPageHeight = compositionLocalOf { 0.dp }

/** How far down the grid [widget]'s page starts. */
@Composable
internal fun widgetPanelPageOffset(widget: PanelWidgetInfo): Dp = LocalWidgetPanelPageHeight.current * widget.pageOrZero()

/** Grid rows that fit in a page [pageHeightPx] tall, after [reservedPx] of edit overhang. */
internal fun widgetPanelPageRows(
    pageHeightPx: Int,
    reservedPx: Int,
    gridUnitHeightPx: Float,
    gapPx: Float,
): Int {
    if (pageHeightPx <= 0 || gridUnitHeightPx <= 0f) return 0
    return floor((pageHeightPx - reservedPx + gapPx) / gridUnitHeightPx).toInt().coerceAtLeast(1)
}

/**
 * Places the newly added panel widget [appWidgetId] in the first free spot on [page], or at the
 * top of a new last page when [page] has no room for it.
 */
internal fun List<PanelWidgetInfo>.withNewPanelWidgetPlaced(
    appWidgetId: Int,
    page: Int,
    pageRows: Int,
): List<PanelWidgetInfo> {
    if (pageRows <= 0) return this
    val added = firstOrNull { it.isInPanel && it.appWidgetId == appWidgetId } ?: return this
    val others = filter { it.isInPanel && it.appWidgetId != appWidgetId }
    val columnSpan = (added.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN).coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
    val rowSpan =
        (added.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN).coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN).coerceAtMost(pageRows)
    val occupied = mutableSetOf<Pair<Int, Int>>()
    others.forEach { widget ->
        val column = widget.column ?: return@forEach
        val row = widget.row ?: return@forEach
        if (widget.pageOrZero() != page) return@forEach
        markOccupied(
            occupied,
            column,
            row,
            widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN,
            widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN,
        )
    }
    val slot = findFirstAvailablePosition(occupied, columnSpan, rowSpan, maxRow = pageRows - rowSpan)
    val placed =
        if (slot != null) {
            added.copy(panelPage = page.takeIf { it > 0 }, column = slot.first, row = slot.second)
        } else {
            val newPage = (others.maxOfOrNull { it.pageOrZero() } ?: -1) + 1
            added.copy(panelPage = newPage.takeIf { it > 0 }, column = 0, row = 0)
        }
    return map { if (it.isInPanel && it.appWidgetId == appWidgetId) placed else it }
}

/**
 * Moves [appWidgetId], dropped on [zone] while [currentPage] is in view, to the first free spot on
 * the page before or after it. When that page has no room, a new page is inserted there instead.
 */
internal fun List<PanelWidgetInfo>.withPanelWidgetMovedToPage(
    appWidgetId: Int,
    zone: WidgetPanelPageZone,
    currentPage: Int,
    pageRows: Int,
): List<PanelWidgetInfo> {
    if (pageRows <= 0) return this
    val moving = firstOrNull { it.isInPanel && it.appWidgetId == appWidgetId } ?: return this
    val targetPage = if (zone == WidgetPanelPageZone.Previous) currentPage - 1 else currentPage + 1
    if (targetPage < 0) return this
    val columnSpan = (moving.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN).coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
    val rowSpan = (moving.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN).coerceAtMost(pageRows)
    val slot =
        if (zone == WidgetPanelPageZone.NewPage) {
            null
        } else {
            val occupied = mutableSetOf<Pair<Int, Int>>()
            forEach { widget ->
                if (!widget.isInPanel || widget.appWidgetId == appWidgetId || widget.pageOrZero() != targetPage) {
                    return@forEach
                }
                markOccupied(
                    occupied,
                    widget.column ?: return@forEach,
                    widget.row ?: return@forEach,
                    widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN,
                    widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN,
                )
            }
            findFirstAvailablePosition(occupied, columnSpan, rowSpan, maxRow = pageRows - rowSpan)
        }
    // With no room, a fresh page takes the target page's place and later pages shift down.
    val insertedPage = if (slot == null) (if (zone == WidgetPanelPageZone.Previous) currentPage else targetPage) else null
    return map { widget ->
        when {
            widget.isInPanel && widget.appWidgetId == appWidgetId ->
                widget.copy(
                    panelPage = (insertedPage ?: targetPage).takeIf { it > 0 },
                    column = slot?.first ?: 0,
                    row = slot?.second ?: 0,
                )
            insertedPage != null && widget.isInPanel && widget.pageOrZero() >= insertedPage ->
                widget.copy(panelPage = widget.pageOrZero() + 1)
            else -> widget
        }
    }
}

/** Keeps a resize inside the widget's page. */
internal fun WidgetGridResize.withinPage(pageRows: Int): WidgetGridResize =
    if (pageRows <= 0) this else copy(rowSpan = rowSpan.coerceAtMost((pageRows - row).coerceAtLeast(1)))
