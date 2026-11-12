package com.tk.quicksearch.reminders

import android.content.res.Resources
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.shared.time.ScheduleRepeat
import com.tk.quicksearch.shared.time.ScheduleRepeatUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Repeat rules for reminders: the next date, the label, and the names used when saving them. */
internal object ReminderRepeat {
    private const val MAX_STEPS = 10_000

    /** The first date after [reminder]'s own that is still ahead of [nowMillis]. */
    fun nextDate(
        reminder: ReminderInfo,
        repeat: ScheduleRepeat,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        var next = step(reminder.date, repeat)
        var steps = 0
        while (reminder.copy(date = next, isDone = true).isPast(nowMillis, zoneId) && steps < MAX_STEPS) {
            next = step(next, repeat)
            steps++
        }
        return next
    }

    /** "Daily", "Every 2 weeks", from the strings calendar events already use. */
    fun label(resources: Resources, repeat: ScheduleRepeat): String {
        val interval = repeat.interval.coerceAtLeast(1)
        return when (repeat.unit) {
            ScheduleRepeatUnit.Day ->
                if (interval == 1) resources.getString(R.string.calendar_repeats_daily)
                else resources.getQuantityString(R.plurals.calendar_repeats_every_days, interval, interval)
            ScheduleRepeatUnit.Week ->
                if (interval == 1) resources.getString(R.string.calendar_repeats_weekly)
                else resources.getQuantityString(R.plurals.calendar_repeats_every_weeks, interval, interval)
            ScheduleRepeatUnit.Month ->
                if (interval == 1) resources.getString(R.string.calendar_repeats_monthly)
                else resources.getQuantityString(R.plurals.calendar_repeats_every_months, interval, interval)
            ScheduleRepeatUnit.Year ->
                if (interval == 1) resources.getString(R.string.calendar_repeats_yearly)
                else resources.getQuantityString(R.plurals.calendar_repeats_every_years, interval, interval)
        }
    }

    /** The unit's saved name: "day", "week", "month", "year". Noted reads the same names. */
    fun unitName(unit: ScheduleRepeatUnit): String = unit.name.lowercase()

    fun repeat(interval: Int, unitName: String?): ScheduleRepeat? {
        val unit = ScheduleRepeatUnit.entries.firstOrNull { unitName(it) == unitName } ?: return null
        return ScheduleRepeat(interval.coerceAtLeast(1), unit)
    }

    private fun step(date: LocalDate, repeat: ScheduleRepeat): LocalDate {
        val interval = repeat.interval.coerceAtLeast(1).toLong()
        return when (repeat.unit) {
            ScheduleRepeatUnit.Day -> date.plusDays(interval)
            ScheduleRepeatUnit.Week -> date.plusWeeks(interval)
            ScheduleRepeatUnit.Month -> date.plusMonths(interval)
            ScheduleRepeatUnit.Year -> date.plusYears(interval)
        }
    }

    /** Timed: due at or before now. Without a time: its day is before today. */
    private fun ReminderInfo.isPast(nowMillis: Long, zoneId: ZoneId): Boolean =
        if (hasTime) {
            dueMillis(zoneId) <= nowMillis
        } else {
            date.isBefore(Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate())
        }
}
