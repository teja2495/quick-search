package com.tk.quicksearch.shared.time

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import kotlin.math.abs

// SHARED FILE. The same file (apart from the package line) lives in quick-search and noted-android,
// and RelativeTime.swift in mac-search and noted-mac follows the same rules.
// Change all four together; ~/Projects/misc/check-time-logic-sync.sh reports copies that drifted,
// and ~/Projects/misc/time-logic-standardization.md lists every label.

enum class RelativeTimeUnit { Minute, Hour, Day, Month, Year }

enum class RelativeTimeStyle {
    /** The two largest units: "in 1 month 4 days", "2 years 3 months ago". */
    Detailed,

    /** The largest unit only: "in 1 month", "2 years ago". */
    Concise,

    /** A plain day count, never named days or a time of day: "in 34 days", "1 day ago". */
    TotalDays,
}

/** How far a date is from now, before it is turned into words. */
sealed interface RelativeTime {
    object Now : RelativeTime

    object Today : RelativeTime

    object Tomorrow : RelativeTime

    object Yesterday : RelativeTime

    data class Span(val isFuture: Boolean, val parts: List<Part>) : RelativeTime

    data class Part(val value: Int, val unit: RelativeTimeUnit)
}

/** The words for a [RelativeTime]; apps with translated strings supply their own. */
interface RelativeTimeStrings {
    val now: String
    val today: String
    val tomorrow: String
    val yesterday: String

    /** "1 minute", "4 days". */
    fun unit(value: Int, unit: RelativeTimeUnit): String

    /** "in 4 days". */
    fun future(duration: String): String

    /** "4 days ago". */
    fun past(duration: String): String
}

object EnglishRelativeTimeStrings : RelativeTimeStrings {
    override val now = "Now"
    override val today = "Today"
    override val tomorrow = "Tomorrow"
    override val yesterday = "Yesterday"

    override fun unit(value: Int, unit: RelativeTimeUnit): String = "$value ${unit.name.lowercase()}${if (value == 1) "" else "s"}"

    override fun future(duration: String): String = "in $duration"

    override fun past(duration: String): String = "$duration ago"
}

object RelativeTimeFormatter {
    /** A timed item on another calendar day still counts in minutes or hours when it is this close. */
    const val CROSS_MIDNIGHT_HOURS = 6L

    /**
     * The one relative label for events, reminders and tasks.
     *
     * - Timed and running (`date <= now < endDate`): Now.
     * - Timed, and today or under [CROSS_MIDNIGHT_HOURS] hours away: Now under a minute, then whole
     *   minutes, then whole hours ("in 5 minutes", "2 hours ago").
     * - Otherwise by calendar day: Today, Tomorrow, Yesterday, then days, months and years in [style].
     */
    fun relativeTime(
        date: Instant,
        hasTime: Boolean,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        style: RelativeTimeStyle = RelativeTimeStyle.Detailed,
        endDate: Instant? = null,
    ): RelativeTime {
        val today = now.atZone(zoneId).toLocalDate()
        val day = date.atZone(zoneId).toLocalDate()
        val dayDelta = day.toEpochDay() - today.toEpochDay()
        val isFuture = dayDelta > 0

        if (style == RelativeTimeStyle.TotalDays) {
            if (dayDelta == 0L) return RelativeTime.Today
            return RelativeTime.Span(isFuture, listOf(RelativeTime.Part(abs(dayDelta).toInt(), RelativeTimeUnit.Day)))
        }
        if (hasTime) {
            if (endDate != null && !now.isBefore(date) && now.isBefore(endDate)) return RelativeTime.Now
            val isClose = abs(Duration.between(now, date).seconds) < CROSS_MIDNIGHT_HOURS * 3_600
            if (dayDelta == 0L || isClose) return timeDistance(date, now)
        }
        when (dayDelta) {
            0L -> return RelativeTime.Today
            1L -> return RelativeTime.Tomorrow
            -1L -> return RelativeTime.Yesterday
        }

        val period = if (isFuture) Period.between(today, day) else Period.between(day, today)
        val all = listOf(
            RelativeTime.Part(period.years, RelativeTimeUnit.Year),
            RelativeTime.Part(period.months, RelativeTimeUnit.Month),
            RelativeTime.Part(period.days, RelativeTimeUnit.Day),
        )
        val first = all.indexOfFirst { it.value > 0 }
        val count = if (style == RelativeTimeStyle.Concise) 1 else 2
        val parts = all.subList(first, minOf(first + count, all.size)).filter { it.value > 0 }
        return RelativeTime.Span(isFuture, parts)
    }

    /** Minutes or hours only, however far away: Now under a minute, "in 5 minutes", "30 hours ago". */
    fun timeDistance(date: Instant, now: Instant = Instant.now()): RelativeTime {
        val seconds = abs(Duration.between(now, date).seconds)
        if (seconds < 60) return RelativeTime.Now
        val minutes = seconds / 60
        val part = if (minutes < 60) {
            RelativeTime.Part(minutes.toInt(), RelativeTimeUnit.Minute)
        } else {
            RelativeTime.Part((minutes / 60).toInt(), RelativeTimeUnit.Hour)
        }
        return RelativeTime.Span(isFuture = !date.isBefore(now), parts = listOf(part))
    }

    fun format(relativeTime: RelativeTime, strings: RelativeTimeStrings = EnglishRelativeTimeStrings): String = when (relativeTime) {
        RelativeTime.Now -> strings.now
        RelativeTime.Today -> strings.today
        RelativeTime.Tomorrow -> strings.tomorrow
        RelativeTime.Yesterday -> strings.yesterday
        is RelativeTime.Span -> {
            val duration = relativeTime.parts.joinToString(" ") { strings.unit(it.value, it.unit) }
            if (relativeTime.isFuture) strings.future(duration) else strings.past(duration)
        }
    }

    /** [relativeTime] in words. */
    fun label(
        date: Instant,
        hasTime: Boolean,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        style: RelativeTimeStyle = RelativeTimeStyle.Detailed,
        endDate: Instant? = null,
        strings: RelativeTimeStrings = EnglishRelativeTimeStrings,
    ): String = format(relativeTime(date, hasTime, now, zoneId, style, endDate), strings)
}
