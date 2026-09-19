package com.tk.quicksearch.search.calendar

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.CalendarEventInfo
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

private const val MILLIS_PER_MINUTE = 60_000L

@Composable
fun calendarHomeScheduleLabel(
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    if (event.allDay) return stringResource(R.string.calendar_relative_today)
    if (nowMillis in event.startMillis..event.endMillis) {
        return "${formatCalendarEventStartTime(event.startMillis)} • ${stringResource(R.string.calendar_relative_now)}"
    }

    return "${formatCalendarEventStartTime(event.startMillis)} • ${calendarRelativeTimeLabel(event.startMillis, nowMillis)}"
}

fun isCalendarEventCurrentlyRelevant(
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean =
    !event.allDay &&
        nowMillis in (event.startMillis - (15L * MILLIS_PER_MINUTE))..event.endMillis

private enum class CalendarRecurrenceFrequency(
    @StringRes val repeatsSingularResId: Int,
    val repeatsPluralResId: Int,
) {
    DAILY(
        repeatsSingularResId = R.string.calendar_repeats_daily,
        repeatsPluralResId = R.plurals.calendar_repeats_every_days,
    ),
    WEEKLY(
        repeatsSingularResId = R.string.calendar_repeats_weekly,
        repeatsPluralResId = R.plurals.calendar_repeats_every_weeks,
    ),
    MONTHLY(
        repeatsSingularResId = R.string.calendar_repeats_monthly,
        repeatsPluralResId = R.plurals.calendar_repeats_every_months,
    ),
    YEARLY(
        repeatsSingularResId = R.string.calendar_repeats_yearly,
        repeatsPluralResId = R.plurals.calendar_repeats_every_years,
    ),
}

private data class CalendarRecurrenceInfo(
    val frequency: CalendarRecurrenceFrequency,
    val interval: Int,
)

/** Returns the full day-of-week name (e.g. "Monday") for the given epoch millis. */
fun getDayOfWeekName(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}

/** Returns a formatted absolute date string (e.g. "20th March, 2024") for the given epoch millis. */
fun formatAbsoluteDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val day = date.dayOfMonth
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "${day}${suffix} ${month}, ${date.year}"
}

@Composable
fun formatCalendarEventDate(event: CalendarEventInfo): String {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val date = Date(event.startMillis)
    val dateText = SimpleDateFormat("EEE, MMM d", locale).format(date)
    return if (event.allDay) {
        dateText
    } else {
        "$dateText • ${DateFormat.getTimeFormat(context).format(date)}"
    }
}

@Composable
fun calendarRecurrenceLabel(
    recurrenceRule: String?,
    instanceCount: Int = 1,
): String? {
    val recurrenceInfo = parseRecurrenceRule(recurrenceRule)
    if (recurrenceInfo == null) {
        return if (instanceCount > 1) {
            stringResource(R.string.calendar_repeats_generic)
        } else {
            null
        }
    }

    val interval = recurrenceInfo.interval.coerceAtLeast(1)
    if (interval == 1) {
        return stringResource(recurrenceInfo.frequency.repeatsSingularResId)
    }

    return pluralStringResource(
        recurrenceInfo.frequency.repeatsPluralResId,
        interval,
        interval,
    )
}

@Composable
fun calendarRelativeDateLabel(
    eventStartMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String = calendarRelativeDateLabel(eventStartMillis, isAllDay = true, nowMillis)

@Composable
fun calendarRelativeDateLabel(
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
): String = calendarRelativeDateLabel(event.startMillis, event.allDay, nowMillis)

@Composable
fun calendarRelativeDateLabel(
    eventStartMillis: Long,
    isAllDay: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val zoneId = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val eventDate = Instant.ofEpochMilli(eventStartMillis).atZone(zoneId).toLocalDate()

    val dayDelta = eventDate.toEpochDay() - today.toEpochDay()
    if (dayDelta == 0L) {
        return if (isAllDay) stringResource(R.string.calendar_relative_today)
        else calendarRelativeTimeLabel(eventStartMillis, nowMillis)
    }
    if (dayDelta == -1L) return stringResource(R.string.calendar_relative_yesterday)
    if (dayDelta == 1L) return stringResource(R.string.calendar_relative_tomorrow)

    val isFuture = dayDelta > 0L
    val period =
        if (isFuture) {
            Period.between(today, eventDate)
        } else {
            Period.between(eventDate, today)
        }
    val deltaText = formatRelativeUnits(period)

    return if (isFuture) {
        stringResource(R.string.calendar_relative_in_format, deltaText)
    } else {
        stringResource(R.string.calendar_relative_ago_format, deltaText)
    }
}

@Composable
internal fun calendarRelativeTimeLabel(
    eventStartMillis: Long,
    nowMillis: Long,
): String {
    val minutes = (kotlin.math.abs(eventStartMillis - nowMillis) / MILLIS_PER_MINUTE).coerceAtLeast(1L)
    val hours = (minutes / 60).toInt()
    val duration =
        if (hours > 0) {
            pluralStringResource(R.plurals.calendar_relative_hours, hours, hours)
        } else {
            pluralStringResource(R.plurals.calendar_relative_minutes, minutes.toInt(), minutes.toInt())
        }

    return if (eventStartMillis >= nowMillis) {
        stringResource(R.string.calendar_relative_in_format, duration)
    } else {
        stringResource(R.string.calendar_relative_ago_format, duration)
    }
}

@Composable
private fun formatCalendarEventStartTime(startMillis: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(startMillis))

@Composable
private fun formatRelativeUnits(period: Period): String {
    val years = period.years.coerceAtLeast(0)
    val months = period.months.coerceAtLeast(0)
    val days = period.days.coerceAtLeast(0)

    if (years > 0) {
        val yearLabel = pluralStringResource(R.plurals.calendar_relative_years, years, years)
        val monthLabel =
            if (months > 0) {
                pluralStringResource(R.plurals.calendar_relative_months, months, months)
            } else {
                null
            }
        return listOfNotNull(yearLabel, monthLabel).joinToString(separator = " ")
    }

    if (months > 0) {
        val monthLabel = pluralStringResource(R.plurals.calendar_relative_months, months, months)
        val dayLabel =
            if (days > 0) {
                pluralStringResource(R.plurals.calendar_relative_days, days, days)
            } else {
                null
            }
        return listOfNotNull(monthLabel, dayLabel).joinToString(separator = " ")
    }

    val safeDays = days.coerceAtLeast(1)
    return pluralStringResource(R.plurals.calendar_relative_days, safeDays, safeDays)
}

private fun parseRecurrenceRule(rule: String?): CalendarRecurrenceInfo? {
    if (rule.isNullOrBlank()) return null

    val parts =
        rule.split(";")
            .mapNotNull { token ->
                val keyValue = token.split("=", limit = 2)
                if (keyValue.size != 2) return@mapNotNull null
                keyValue[0].trim().uppercase(Locale.US) to keyValue[1].trim().uppercase(Locale.US)
            }.toMap()

    val frequency =
        when (parts["FREQ"]) {
            "DAILY" -> CalendarRecurrenceFrequency.DAILY
            "WEEKLY" -> CalendarRecurrenceFrequency.WEEKLY
            "MONTHLY" -> CalendarRecurrenceFrequency.MONTHLY
            "YEARLY" -> CalendarRecurrenceFrequency.YEARLY
            else -> return null
        }

    val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
    return CalendarRecurrenceInfo(frequency = frequency, interval = interval)
}
