package com.tk.quicksearch.search.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When calendar events and reminders join At a Glance, shared by home and the widget. All-day items
 * show from [MORNING_HOUR]; timed ones show [LEAD_MILLIS] before they start. Dismissing a timed item
 * more than [DISMISS_DONE_WINDOW_MILLIS] before it starts brings it back when it starts; dismissing
 * it later marks it done.
 */
internal object GlanceTiming {
    const val MORNING_HOUR = 5
    const val LEAD_MILLIS = 90L * 60L * 1000L
    const val DISMISS_DONE_WINDOW_MILLIS = 30L * 60L * 1000L

    /** Tomorrow's items show from this hour of the evening until midnight. */
    const val EVENING_HOUR = 21

    /** Tomorrow's timed items show the evening before only when they start before this hour. */
    const val TOMORROW_MORNING_END_HOUR = 12

    fun localDate(
        millis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

    fun morningStartMillis(
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = day.atTime(MORNING_HOUR, 0).atZone(zoneId).toInstant().toEpochMilli()

    fun startOfDayMillis(
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = day.atStartOfDay(zoneId).toInstant().toEpochMilli()

    /** Whether [nowMillis] is in the evening, when tomorrow's items show. */
    fun isEvening(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean = Instant.ofEpochMilli(nowMillis).atZone(zoneId).hour >= EVENING_HOUR

    /** Whether a timed item at [startMillis] on [day] is a morning one, shown the evening before. */
    fun isTomorrowMorning(
        startMillis: Long,
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean =
        startMillis >= startOfDayMillis(day, zoneId) &&
            startMillis < day.atTime(TOMORROW_MORNING_END_HOUR, 0).atZone(zoneId).toInstant().toEpochMilli()
}
