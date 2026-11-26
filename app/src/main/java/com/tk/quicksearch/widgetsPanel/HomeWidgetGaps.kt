package com.tk.quicksearch.widgetsPanel

import kotlin.math.min

/** One child of the Home stack, listed in logical order (outward from Home's anchored end). */
internal data class HomeGapSlot(
    val height: Int,
    val widgetId: Int?,
)

/**
 * Which [slots] may carry empty space before them: visible widgets with only widgets (or slots
 * with nothing to show) beyond them. Empty space can then open up between widgets at Home's open
 * end, but never between regular Home items, which always reflow normally.
 */
internal fun homeGapEligible(slots: List<HomeGapSlot>): BooleanArray {
    val eligible = BooleanArray(slots.size)
    var onlyWidgetsBeyond = true
    for (index in slots.indices.reversed()) {
        val slot = slots[index]
        eligible[index] = onlyWidgetsBeyond && slot.widgetId != null && slot.height > 0
        if (slot.height > 0 && slot.widgetId == null) onlyWidgetsBeyond = false
    }
    return eligible
}

/**
 * Lays [slots] out from Home's anchored end, the way the Home stack measures them, and returns the
 * empty space before each eligible widget that puts it [targetDistance] from that end, or as close
 * as it can get without overlapping what comes before it. Eligible widgets without a target get
 * none; ineligible widgets are left out.
 */
internal fun homeWidgetGapsPx(
    slots: List<HomeGapSlot>,
    spacingPx: Int,
    targetDistance: (widgetId: Int) -> Float?,
): Map<Int, Float> {
    val eligible = homeGapEligible(slots)
    val gaps = HashMap<Int, Float>()
    var y = 0f
    var hasVisibleSlot = false
    slots.forEachIndexed { index, slot ->
        if (slot.height > 0 && hasVisibleSlot) y += min(slot.height, spacingPx)
        val widgetId = slot.widgetId
        if (widgetId != null && eligible[index]) {
            val gap = targetDistance(widgetId)?.let { (it - y).coerceAtLeast(0f) } ?: 0f
            gaps[widgetId] = gap
            y += gap
        }
        y += slot.height
        if (slot.height > 0) hasVisibleSlot = true
    }
    return gaps
}

/**
 * The Home stack's children (leading content, [entries], trailing content) as [HomeGapSlot]s in
 * logical order. [entries] are in visual order, which is reversed in one-handed mode. A row of
 * widgets is one slot as tall as its tallest member, at its leader; the other members are empty.
 */
internal fun homeGapSlots(
    entries: List<HomeLayoutEntry>,
    isReversed: Boolean,
    heightOf: (key: Any) -> Int,
): List<HomeGapSlot> {
    val visualKeys = listOf<Any>(HOME_STACK_LEADING_KEY) + entries.map { it.key } + HOME_STACK_TRAILING_KEY
    val visualPlacements = listOf(null) + entries.map { (it as? HomeLayoutEntry.Widget)?.widget?.home } + null
    val keys = if (isReversed) visualKeys.asReversed() else visualKeys
    val placements = if (isReversed) visualPlacements.asReversed() else visualPlacements
    val heights = IntArray(keys.size) { heightOf(keys[it]) }
    val leaders = homeRowLeaders(placements.mapIndexed { index, home -> home?.takeIf { heights[index] > 0 } })
    val slotHeights = homeRowSlotHeights(heights, leaders)
    return keys.mapIndexed { index, key -> HomeGapSlot(slotHeights[index], key as? Int) }
}
