package com.tk.quicksearch.search.calendar

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.shared.time.RelativeTimeFormatter
import com.tk.quicksearch.shared.time.RelativeTimeStrings
import com.tk.quicksearch.shared.time.RelativeTimeUnit
import java.text.SimpleDateFormat
import java.time.Instant
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

/** A time-less date: "Today", "Tomorrow", "in 1 month 4 days". */
@Composable
fun calendarRelativeDateLabel(
    eventStartMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String = calendarRelativeDateLabel(eventStartMillis, isAllDay = true, nowMillis)

/** An event: "Now" while it runs, otherwise as for its start. */
@Composable
fun calendarRelativeDateLabel(
    event: CalendarEventInfo,
    nowMillis: Long = System.currentTimeMillis(),
): String = calendarRelativeDateLabel(event.startMillis, event.allDay, nowMillis, endMillis = event.endMillis)

/**
 * The shared relative label ([RelativeTimeFormatter]) in this app's translated strings: minutes or
 * hours for timed items that are today or a few hours away, otherwise "Today", "Tomorrow",
 * "Yesterday", or days, months and years.
 */
@Composable
fun calendarRelativeDateLabel(
    eventStartMillis: Long,
    isAllDay: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    endMillis: Long? = null,
): String {
    // Read so the label follows locale changes, as stringResource does.
    LocalConfiguration.current
    return RelativeTimeFormatter.label(
        date = Instant.ofEpochMilli(eventStartMillis),
        hasTime = !isAllDay,
        now = Instant.ofEpochMilli(nowMillis),
        endDate = endMillis?.let(Instant::ofEpochMilli),
        strings = ResourceRelativeTimeStrings(LocalContext.current.resources),
    )
}

@Composable
internal fun calendarRelativeTimeLabel(
    eventStartMillis: Long,
    nowMillis: Long,
): String {
    // Read so the label follows locale changes, as stringResource does.
    LocalConfiguration.current
    return calendarRelativeTimeLabel(LocalContext.current.resources, eventStartMillis, nowMillis)
}

/** "Now", "in 5 minutes" or "2 hours ago": whole minutes, or whole hours from an hour away. */
internal fun calendarRelativeTimeLabel(
    resources: Resources,
    eventStartMillis: Long,
    nowMillis: Long,
): String =
    RelativeTimeFormatter.format(
        RelativeTimeFormatter.timeDistance(Instant.ofEpochMilli(eventStartMillis), Instant.ofEpochMilli(nowMillis)),
        ResourceRelativeTimeStrings(resources),
    )

/** The shared relative-time wording, read from this app's translated resources. */
internal class ResourceRelativeTimeStrings(private val resources: Resources) : RelativeTimeStrings {
    override val now: String get() = resources.getString(R.string.calendar_relative_now)
    override val today: String get() = resources.getString(R.string.calendar_relative_today)
    override val tomorrow: String get() = resources.getString(R.string.calendar_relative_tomorrow)
    override val yesterday: String get() = resources.getString(R.string.calendar_relative_yesterday)

    override fun unit(value: Int, unit: RelativeTimeUnit): String =
        resources.getQuantityString(
            when (unit) {
                RelativeTimeUnit.Minute -> R.plurals.calendar_relative_minutes
                RelativeTimeUnit.Hour -> R.plurals.calendar_relative_hours
                RelativeTimeUnit.Day -> R.plurals.calendar_relative_days
                RelativeTimeUnit.Month -> R.plurals.calendar_relative_months
                RelativeTimeUnit.Year -> R.plurals.calendar_relative_years
            },
            value,
            value,
        )

    override fun future(duration: String): String = resources.getString(R.string.calendar_relative_in_format, duration)

    override fun past(duration: String): String = resources.getString(R.string.calendar_relative_ago_format, duration)
}

@Composable
private fun formatCalendarEventStartTime(startMillis: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(startMillis))

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
