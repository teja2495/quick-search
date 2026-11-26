package com.tk.quicksearch.widgetsPanel

import kotlin.math.max
import kotlin.math.min

/*
 * Home widgets normally take a row each. A widget with [HomeWidgetPlacement.sharesRow] joins the
 * row of the widget just before it in logical order, so narrow widgets can sit side by side. A
 * row's height is its tallest member's; it is laid out (and holds empty space) as one slot.
 */

/**
 * For each slot in logical order, the logical index of the slot that leads its row (itself when it
 * leads one). [placements] holds each slot's Home placement, or null for Home items and widgets
 * with nothing to show. A widget joins the row before it only when it asks to, directly follows a
 * member of that row, and overlaps none of its members' columns.
 */
internal fun homeRowLeaders(placements: List<HomeWidgetPlacement?>): IntArray {
    val leaders = IntArray(placements.size) { it }
    for (index in 1 until placements.size) {
        val placement = placements[index] ?: continue
        if (!placement.sharesRow || placements[index - 1] == null) continue
        val leader = leaders[index - 1]
        val fits =
            (leader until index).none { member ->
                val other = placements[member] ?: return@none false
                placement.column < other.column + other.columnSpan &&
                    other.column < placement.column + placement.columnSpan
            }
        if (fits) leaders[index] = leader
    }
    return leaders
}

/** Slot heights in logical order with each row at its leader: tallest member, 0 for followers. */
internal fun homeRowSlotHeights(
    heights: IntArray,
    leaders: IntArray,
): IntArray {
    val rowHeights = IntArray(heights.size)
    heights.indices.forEach { index -> rowHeights[leaders[index]] = max(rowHeights[leaders[index]], heights[index]) }
    return IntArray(heights.size) { index -> if (leaders[index] == index) rowHeights[index] else 0 }
}

/** Where the Home stack puts its children (visual order), and the empty space before each. */
internal class HomeStackPlacement(
    val positions: IntArray,
    val gaps: IntArray,
    val height: Int,
)

/**
 * Lays the Home stack's children out top to bottom. All arguments are in visual order; [placements]
 * is null for non-widget children. Widgets with only widgets beyond them in logical order hold their
 * empty space ([HomeWidgetPlacement.offsetSteps] or [HomeWidgetPlacement.gapSteps]); between Home
 * items there is none. Row members share their leader's slot, aligned toward Home's anchored end.
 */
internal fun placeHomeStack(
    heights: IntArray,
    placements: List<HomeWidgetPlacement?>,
    widgetIds: List<Int?>,
    isReversed: Boolean,
    spacingPx: Int,
    gapStepPx: Int,
): HomeStackPlacement {
    val count = heights.size
    fun logicalOf(index: Int) = if (isReversed) count - 1 - index else index
    val logical = (0 until count).map { logicalIndex -> logicalOf(logicalIndex) }
    val leaders = homeRowLeaders(logical.map { index -> placements[index]?.takeIf { heights[index] > 0 } })
    val slotHeights = homeRowSlotHeights(IntArray(count) { heights[logical[it]] }, leaders)
    val eligible =
        homeGapEligible(logical.mapIndexed { logicalIndex, index -> HomeGapSlot(slotHeights[logicalIndex], widgetIds[index]) })
    // Walk outward from the anchored end: a widget with an offset holds that distance and the
    // content before it fills the empty space first, pushing it only once there is none left.
    val gaps = IntArray(count)
    var logicalY = 0
    var hasVisibleLogical = false
    logical.forEachIndexed { logicalIndex, index ->
        val height = slotHeights[logicalIndex]
        if (height > 0 && hasVisibleLogical) logicalY += min(height, spacingPx)
        if (eligible[logicalIndex]) {
            val placement = placements[index]
            val offset = placement?.offsetSteps
            gaps[index] =
                if (offset != null) {
                    (offset * gapStepPx - logicalY).coerceAtLeast(0)
                } else {
                    (placement?.gapSteps ?: 0) * gapStepPx
                }
            logicalY += gaps[index]
        }
        logicalY += height
        if (height > 0) hasVisibleLogical = true
    }
    val rowTops = IntArray(count)
    var y = 0
    var hasVisibleChild = false
    for (index in 0 until count) {
        val logicalIndex = logicalOf(index)
        if (leaders[logicalIndex] != logicalIndex) continue
        val height = slotHeights[logicalIndex]
        // Empty slots (sections with nothing to show) take no spacing. Growing children ease into
        // the gap so height animations don't jump by the full spacing on their first frame.
        if (height > 0 && hasVisibleChild) y += min(height, spacingPx)
        // A widget's empty space sits before it in logical Home order: above it normally, below it
        // once the layout is reversed for one-handed mode.
        val gap = if (height > 0) gaps[index] else 0
        if (!isReversed) y += gap
        rowTops[logicalIndex] = y
        y += height
        if (isReversed) y += gap
        if (height > 0) hasVisibleChild = true
    }
    val positions =
        IntArray(count) { index ->
            val leader = leaders[logicalOf(index)]
            rowTops[leader] + if (isReversed) slotHeights[leader] - heights[index] else 0
        }
    return HomeStackPlacement(positions = positions, gaps = gaps, height = y)
}

/** Row leaders, in logical order, of [entries] (visual order) as the stack lays them out. */
internal fun homeEntryRowLeaders(
    entries: List<HomeLayoutEntry>,
    isReversed: Boolean,
    heightOf: (key: Any) -> Int,
): IntArray {
    val logical = if (isReversed) entries.asReversed() else entries
    return homeRowLeaders(
        logical.map { entry -> (entry as? HomeLayoutEntry.Widget)?.widget?.home?.takeIf { heightOf(entry.key) > 0 } },
    )
}

/**
 * Where a dragged widget dropped into [others] (the visual entries without it) joins a row: the
 * visual insert index right after that row, or null when the widget's center isn't level with a
 * widget whose row has room for [column]..[column] + [columnSpan].
 */
internal fun homeRowJoinIndex(
    others: List<HomeLayoutEntry>,
    isReversed: Boolean,
    heightOf: (key: Any) -> Int,
    topOf: (key: Any) -> Float?,
    centerY: Float,
    column: Int,
    columnSpan: Int,
): Int? {
    val logical = if (isReversed) others.asReversed() else others
    val leaders = homeEntryRowLeaders(others, isReversed, heightOf)
    val hit =
        logical.indices.firstOrNull { index ->
            val entry = logical[index]
            val top = topOf(entry.key)
            val height = heightOf(entry.key)
            entry is HomeLayoutEntry.Widget && top != null && height > 0 && centerY in top..(top + height)
        } ?: return null
    val members = logical.indices.filter { leaders[it] == leaders[hit] }
    val overlaps =
        members.any { index ->
            val home = (logical[index] as HomeLayoutEntry.Widget).widget.home ?: return@any true
            column < home.column + home.columnSpan && home.column < column + columnSpan
        }
    if (overlaps) return null
    val logicalInsert = members.last() + 1
    return if (isReversed) others.size - logicalInsert else logicalInsert
}

/** Moves a visual insert index into [others] that would split a row to just past that row. */
internal fun snapHomeInsertOutOfRows(
    others: List<HomeLayoutEntry>,
    isReversed: Boolean,
    heightOf: (key: Any) -> Int,
    visualIndex: Int,
): Int {
    val leaders = homeEntryRowLeaders(others, isReversed, heightOf)
    var logicalInsert = if (isReversed) others.size - visualIndex else visualIndex
    while (logicalInsert in 1 until others.size && leaders[logicalInsert] != logicalInsert) logicalInsert++
    return if (isReversed) others.size - logicalInsert else logicalInsert
}
