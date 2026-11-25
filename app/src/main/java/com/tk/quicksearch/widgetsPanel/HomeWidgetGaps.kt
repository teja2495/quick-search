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
 * logical order. [entries] are in visual order, which is reversed in one-handed mode.
 */
internal fun homeGapSlots(
    entries: List<HomeLayoutEntry>,
    isReversed: Boolean,
    heightOf: (key: Any) -> Int,
): List<HomeGapSlot> {
    val visual =
        buildList {
            add(HomeGapSlot(heightOf(HOME_STACK_LEADING_KEY), null))
            entries.forEach { entry ->
                add(HomeGapSlot(heightOf(entry.key), (entry as? HomeLayoutEntry.Widget)?.widget?.appWidgetId))
            }
            add(HomeGapSlot(heightOf(HOME_STACK_TRAILING_KEY), null))
        }
    return if (isReversed) visual.asReversed() else visual
}
