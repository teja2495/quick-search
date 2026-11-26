package com.tk.quicksearch.search.calendar

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.shared.ui.theme.AppMotion

/** Rows arriving while the card is on screen fade in, like At a Glance rows. */
internal val CalendarEventRowEnterTransition: EnterTransition =
    fadeIn(tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedEasing))

/** A row marked done (or ended) collapses the way the At a Glance card does when its last row goes. */
internal val CalendarEventRowExitTransition: ExitTransition =
    shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()

internal class AnimatedCalendarEventRow(
    val event: CalendarEventInfo,
    val visibility: MutableTransitionState<Boolean>,
)

/** The rows last shown; not snapshot state, only read while composing. */
private class AnimatedCalendarEventRowsHolder {
    var rows: List<AnimatedCalendarEventRow> = emptyList()
    var initialized = false
}

/**
 * [events] with the rows that just left kept in their old places until they finish collapsing.
 * Rows present the first time (or after [resetKey] changes, such as expanding the card) show at
 * once without animating.
 */
@Composable
internal fun rememberAnimatedCalendarEventRows(
    events: List<CalendarEventInfo>,
    resetKey: Any?,
): List<AnimatedCalendarEventRow> {
    val holder = remember(resetKey) { AnimatedCalendarEventRowsHolder() }
    val previous = holder.rows.associateBy { it.event.eventId }
    fun rowFor(event: CalendarEventInfo): AnimatedCalendarEventRow {
        val visibility =
            previous[event.eventId]?.visibility
                ?: MutableTransitionState(!holder.initialized).apply { targetState = true }
        return AnimatedCalendarEventRow(event, visibility)
    }
    val currentIndex = events.withIndex().associate { (index, event) -> event.eventId to index }
    val merged = mutableListOf<AnimatedCalendarEventRow>()
    val emitted = mutableSetOf<Long>()
    var next = 0
    for (old in holder.rows) {
        val index = currentIndex[old.event.eventId]
        if (index != null) {
            while (next <= index) {
                val event = events[next++]
                if (emitted.add(event.eventId)) merged += rowFor(event)
            }
        } else {
            // Reading the transition state here recomposes once the row has finished leaving.
            val gone = !old.visibility.targetState && old.visibility.isIdle
            if (!gone && emitted.add(old.event.eventId)) merged += old
        }
    }
    while (next < events.size) {
        val event = events[next++]
        if (emitted.add(event.eventId)) merged += rowFor(event)
    }
    holder.rows = merged
    holder.initialized = true
    SideEffect {
        merged.forEach { row -> row.visibility.targetState = row.event.eventId in currentIndex }
    }
    return merged
}
