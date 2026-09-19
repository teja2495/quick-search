package com.tk.quicksearch.search.models

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A reminder created in Quick Search. The date and optional time are stored as wall-clock values so a
 * reminder keeps its local time when the time zone changes; [dueMillis] resolves them in the current zone.
 */
data class ReminderInfo(
    val reminderId: Long,
    val title: String,
    val date: LocalDate,
    /** Minutes after midnight, or null when the reminder has no time. */
    val timeMinutes: Int?,
    val isDone: Boolean = false,
    val isDismissedFromHome: Boolean = false,
) {
    val hasTime: Boolean get() = timeMinutes != null

    val dueMillis: Long
        get() = dueMillis(ZoneId.systemDefault())

    fun dueMillis(zoneId: ZoneId): Long =
        date.atTime(effectiveTime).atZone(zoneId).toInstant().toEpochMilli()

    /** Start of the reminder's day, used to show time-less reminders as all-day items. */
    val dayStartMillis: Long
        get() = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Undone and past due; a reminder without a time is only overdue once its day has passed. */
    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean =
        !isDone &&
            if (hasTime) {
                dueMillis < nowMillis
            } else {
                date.isBefore(Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate())
            }

    private val effectiveTime: LocalTime
        get() = LocalTime.ofSecondOfDay((timeMinutes ?: DEFAULT_TIME_MINUTES) * 60L)

    companion object {
        /** Reminders without a time notify at 9:00 AM. */
        const val DEFAULT_TIME_MINUTES = 9 * 60
    }
}
