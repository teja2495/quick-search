package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.preferences.glanceOccurrenceKey
import com.tk.quicksearch.search.models.CalendarEventInfo

/**
 * Today's events for At a Glance, shared by home and the widget: not excluded or done, not yet
 * ended, and past their [GlanceTiming] start. [hidden] holds the ones that show later today.
 */
internal class TodayGlanceEvents(
    val visible: List<CalendarEventInfo>,
    val hidden: List<CalendarEventInfo>,
    private val visibleFrom: Map<String, Long>,
) {
    /** When the next hidden event shows, or null when none will today. */
    val nextVisibleAtMillis: Long?
        get() = hidden.mapNotNull { visibleFrom[it.occurrenceKey] }.minOrNull()
}

private val CalendarEventInfo.occurrenceKey: String get() = glanceOccurrenceKey(eventId, startMillis)

internal fun loadTodayGlanceEvents(
    repository: CalendarRepository,
    preferences: CalendarPreferences,
    nowMillis: Long = System.currentTimeMillis(),
): TodayGlanceEvents {
    val excluded = preferences.getExcludedEventIds()
    val hiddenUntil = preferences.getGlanceHiddenEvents()
    val today = GlanceTiming.localDate(nowMillis)
    val morning = GlanceTiming.morningStartMillis(today)
    val startOfTomorrow = GlanceTiming.startOfDayMillis(today.plusDays(1))
    val events =
        repository.getTodayEvents()
            .filterNot { it.eventId in excluded }
            .distinctBy { it.eventId }
            .sortedBy { it.startMillis }
            .filter { it.allDay || it.endMillis >= nowMillis }
    val visibleFrom =
        events.associate { event ->
            val start = if (event.allDay) morning else event.startMillis - GlanceTiming.LEAD_MILLIS
            event.occurrenceKey to maxOf(start, hiddenUntil[event.occurrenceKey] ?: 0L)
        }
    // An event hidden until it's over (or, for an all-day event, until tomorrow) is done for today.
    val shown =
        events.filter { event ->
            val from = visibleFrom.getValue(event.occurrenceKey)
            if (event.allDay) from < startOfTomorrow else from < event.endMillis
        }
    val (visible, hidden) = shown.partition { visibleFrom.getValue(it.occurrenceKey) <= nowMillis }
    return TodayGlanceEvents(visible, hidden, visibleFrom)
}

/**
 * Done: hides this occurrence for good. An all-day event that runs for several days comes back the
 * next morning.
 */
internal fun markTodayGlanceEventDone(
    preferences: CalendarPreferences,
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val until =
        if (event.allDay) {
            GlanceTiming.startOfDayMillis(GlanceTiming.localDate(nowMillis).plusDays(1))
        } else {
            event.endMillis
        }
    preferences.hideGlanceEvent(event.occurrenceKey, maxOf(until, nowMillis + 1), nowMillis)
}

/**
 * The widget's close button on today's event [eventId]. A timed event dismissed more than 30 minutes
 * before it starts comes back when it starts; later, or for an all-day event, it is done.
 */
internal fun dismissTodayGlanceEvent(
    repository: CalendarRepository,
    preferences: CalendarPreferences,
    eventId: Long,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val event = loadTodayGlanceEvents(repository, preferences, nowMillis).visible.firstOrNull { it.eventId == eventId } ?: return
    if (!event.allDay && nowMillis < event.startMillis - GlanceTiming.DISMISS_DONE_WINDOW_MILLIS) {
        preferences.hideGlanceEvent(event.occurrenceKey, event.startMillis, nowMillis)
    } else {
        markTodayGlanceEventDone(preferences, event, nowMillis)
    }
}

/**
 * Tomorrow's all-day events, then its first timed event when that starts in the morning, for the
 * evening rows. Events already running today show as today's instead, and dismissing the morning
 * event doesn't bring up the next one.
 */
internal fun loadTomorrowGlanceEvents(
    repository: CalendarRepository,
    preferences: CalendarPreferences,
    nowMillis: Long = System.currentTimeMillis(),
): List<CalendarEventInfo> {
    val tomorrow = GlanceTiming.localDate(nowMillis).plusDays(1)
    val startOfTomorrow = GlanceTiming.startOfDayMillis(tomorrow)
    val excluded = preferences.getExcludedEventIds()
    val hiddenUntil = preferences.getGlanceHiddenEvents()
    val events =
        repository.getEventsOn(tomorrow).filter { it.eventId !in excluded && it.startMillis >= startOfTomorrow }
    val morningEvent =
        events.firstOrNull { !it.allDay }?.takeIf { GlanceTiming.isTomorrowMorning(it.startMillis, tomorrow) }
    return (events.filter { it.allDay } + listOfNotNull(morningEvent))
        .filterNot { (hiddenUntil[it.occurrenceKey] ?: 0L) > nowMillis }
        .distinctBy { it.eventId }
}

/**
 * Dismisses tomorrow's [event] the evening before: it comes back the next morning, or when it starts
 * if that is earlier.
 */
internal fun dismissTomorrowGlanceEvent(
    preferences: CalendarPreferences,
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val morning = GlanceTiming.morningStartMillis(GlanceTiming.localDate(nowMillis).plusDays(1))
    val until = if (event.allDay) morning else minOf(morning, event.startMillis)
    preferences.hideGlanceEvent(event.occurrenceKey, until, nowMillis)
}

/** The widget's close button on tomorrow's event [eventId]; see [dismissTomorrowGlanceEvent]. */
internal fun dismissTomorrowGlanceEvent(
    repository: CalendarRepository,
    preferences: CalendarPreferences,
    eventId: Long,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val event = loadTomorrowGlanceEvents(repository, preferences, nowMillis).firstOrNull { it.eventId == eventId } ?: return
    dismissTomorrowGlanceEvent(preferences, event, nowMillis)
}
