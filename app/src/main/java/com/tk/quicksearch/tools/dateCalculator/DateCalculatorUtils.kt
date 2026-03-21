package com.tk.quicksearch.tools.dateCalculator

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.util.Locale

object DateCalculatorUtils {
    private val monthNames: Map<String, Int> = buildMap {
        put("january", 1); put("jan", 1)
        put("february", 2); put("feb", 2)
        put("march", 3); put("mar", 3)
        put("april", 4); put("apr", 4)
        put("may", 5)
        put("june", 6); put("jun", 6)
        put("july", 7); put("jul", 7)
        put("august", 8); put("aug", 8)
        put("september", 9); put("sep", 9)
        put("october", 10); put("oct", 10)
        put("november", 11); put("nov", 11)
        put("december", 12); put("dec", 12)
    }

    // Matches month names (full or abbreviated, case-insensitive)
    private val monthPattern = Regex(
        "\\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|october|oct|november|nov|december|dec)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val yearPattern = Regex("\\b(\\d{4})\\b")
    private val dayPattern = Regex("\\b(\\d{1,2})\\b")

    /**
     * Parses a relative date expression and returns the resulting [LocalDate] relative to today.
     *
     * Supported formats (case-insensitive):
     *   2 years ago | 1 year 3 months ago | 6 months ago | 10 days ago | 2 weeks ago
     *   in 2 years | in 1 year 6 months | in 3 months | in 10 days | in 2 weeks
     */
    fun parseRelativeDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        val isFuture = lower.startsWith("in ") ||
            lower.endsWith(" from now") ||
            lower.endsWith(" from today")
        val isPast = lower.endsWith(" ago")
        if (!isFuture && !isPast) return null

        val core = when {
            lower.startsWith("in ") -> lower.removePrefix("in ").trim()
            lower.endsWith(" from now") -> lower.removeSuffix(" from now").trim()
            lower.endsWith(" from today") -> lower.removeSuffix(" from today").trim()
            else -> lower.removeSuffix(" ago").trim()
        }

        val unitPattern = Regex("""(\d+)\s*(years?|months?|weeks?|days?)""")
        var years = 0
        var months = 0
        var weeks = 0
        var days = 0

        var remaining = core
        for (match in unitPattern.findAll(core)) {
            val value = match.groupValues[1].toIntOrNull() ?: continue
            when {
                match.groupValues[2].startsWith("year") -> years = value
                match.groupValues[2].startsWith("month") -> months = value
                match.groupValues[2].startsWith("week") -> weeks = value
                match.groupValues[2].startsWith("day") -> days = value
            }
            remaining = remaining.replace(match.value, " ")
        }

        if (remaining.trim().any { it.isLetterOrDigit() }) return null
        if (years == 0 && months == 0 && weeks == 0 && days == 0) return null

        val today = LocalDate.now()
        return if (isFuture) {
            today.plusYears(years.toLong())
                .plusMonths(months.toLong())
                .plusWeeks(weeks.toLong())
                .plusDays(days.toLong())
        } else {
            today.minusYears(years.toLong())
                .minusMonths(months.toLong())
                .minusWeeks(weeks.toLong())
                .minusDays(days.toLong())
        }
    }

    /**
     * Parses an offset-from-date query such as "5 days from march 2" or "3 months before march 30"
     * and returns the resulting [LocalDate].
     *
     * Supported separators:
     *   <units> from <date>   → adds units to the base date
     *   <units> after <date>  → adds units to the base date
     *   <units> before <date> → subtracts units from the base date
     */
    fun parseOffsetFromDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        val separators = listOf(" from " to true, " after " to true, " before " to false)

        for ((sep, isFuture) in separators) {
            val sepIndex = lower.indexOf(sep)
            if (sepIndex < 0) continue

            val unitsPart = lower.substring(0, sepIndex).trim()
            val datePart = query.substring(sepIndex + sep.length).trim()

            val baseDate = parseDateQuery(datePart) ?: continue

            val unitPattern = Regex("""(\d+)\s*(years?|months?|weeks?|days?)""")
            var years = 0; var months = 0; var weeks = 0; var days = 0
            var remaining = unitsPart
            for (match in unitPattern.findAll(unitsPart)) {
                val value = match.groupValues[1].toIntOrNull() ?: continue
                when {
                    match.groupValues[2].startsWith("year") -> years = value
                    match.groupValues[2].startsWith("month") -> months = value
                    match.groupValues[2].startsWith("week") -> weeks = value
                    match.groupValues[2].startsWith("day") -> days = value
                }
                remaining = remaining.replace(match.value, " ")
            }

            if (remaining.trim().any { it.isLetterOrDigit() }) continue
            if (years == 0 && months == 0 && weeks == 0 && days == 0) continue

            return if (isFuture) {
                baseDate.plusYears(years.toLong()).plusMonths(months.toLong())
                    .plusWeeks(weeks.toLong()).plusDays(days.toLong())
            } else {
                baseDate.minusYears(years.toLong()).minusMonths(months.toLong())
                    .minusWeeks(weeks.toLong()).minusDays(days.toLong())
            }
        }
        return null
    }

    /**
     * Parses a date-difference query of the form "<date1> to <date2>" and returns the two dates.
     * Each part is parsed with [parseDateQuery].
     */
    fun parseDateDiffQuery(query: String): Pair<LocalDate, LocalDate>? {
        val lower = query.trim().lowercase(Locale.US)
        val toIndex = lower.indexOf(" to ")
        if (toIndex < 0) return null
        val part1 = query.substring(0, toIndex).trim()
        val part2 = query.substring(toIndex + 4).trim()
        val date1 = parseDateQuery(part1) ?: return null
        val date2 = parseDateQuery(part2) ?: return null
        return date1 to date2
    }

    /** Computes a human-readable difference label between two dates, e.g. "1 year 2 months 3 days". */
    fun diffLabel(date1: LocalDate, date2: LocalDate): String {
        val (start, end) = if (!date1.isAfter(date2)) date1 to date2 else date2 to date1
        val period = Period.between(start, end)
        val parts = buildList {
            if (period.years > 0) add("${period.years} ${if (period.years == 1) "year" else "years"}")
            if (period.months > 0) add("${period.months} ${if (period.months == 1) "month" else "months"}")
            if (period.days > 0) add("${period.days} ${if (period.days == 1) "day" else "days"}")
        }
        return if (parts.isEmpty()) "0 days" else parts.joinToString(" ")
    }

    // -------------------------------------------------------------------------
    // Time helpers
    // -------------------------------------------------------------------------

    /** Carries a formatted time result with optional day context ("tomorrow" / "yesterday"). */
    data class TimeResult(val label: String, val contextLabel: String? = null, val isAbsolute: Boolean = false)

    /**
     * Parses a standalone time string into [LocalTime].
     * Accepted formats: "5am", "5:00 AM", "14:30", "5:30pm", "5 pm".
     * Hour-only without am/pm is rejected (too ambiguous).
     */
    fun parseTimeString(s: String): LocalTime? {
        val lower = s.trim().lowercase(Locale.US)
        val match = Regex("""^(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$""").matchEntire(lower) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].let { if (it.isEmpty()) 0 else it.toIntOrNull() ?: return null }
        val ampm = match.groupValues[3].takeIf { it.isNotEmpty() }

        // Require am/pm for bare-hour (no colon) to avoid matching plain integers
        if (match.groupValues[2].isEmpty() && ampm == null) return null

        if (ampm != null) {
            hour = when {
                ampm == "am" && hour == 12 -> 0
                ampm == "pm" && hour != 12 -> hour + 12
                else -> hour
            }
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return try { LocalTime.of(hour, minute) } catch (_: Exception) { null }
    }

    /** Formats a [LocalTime] as 12-hour AM/PM, e.g. "3:45 PM", "12:00 AM". */
    fun formatTime12Hr(time: LocalTime): String {
        val hour12 = when (val h = time.hour) {
            0 -> 12
            in 1..12 -> h
            else -> h - 12
        }
        val ampm = if (time.hour < 12) "AM" else "PM"
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour12:$minute $ampm"
    }

    /**
     * Returns the formatted time and an optional day context label ("tomorrow" / "yesterday").
     * No context is returned for today — the time alone is sufficient.
     */
    fun formatTimeWithDayContext(dateTime: LocalDateTime): Pair<String, String?> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val resultDate = dateTime.toLocalDate()
        val time = formatTime12Hr(dateTime.toLocalTime())
        val context = when (resultDate) {
            today -> null
            today.plusDays(1) -> "tomorrow"
            today.minusDays(1) -> "yesterday"
            else -> null
        }
        return time to context
    }

    /** Formats an absolute minute count as "X hours Y minutes" with no prefix/suffix. */
    private fun timeDurationLabel(absMinutes: Long): String {
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val parts = buildList {
            if (hours > 0) add("$hours ${if (hours == 1L) "hour" else "hours"}")
            if (mins > 0) add("$mins ${if (mins == 1L) "minute" else "minutes"}")
        }
        return if (parts.isEmpty()) "0 minutes" else parts.joinToString(" ")
    }

    /**
     * Builds a human-readable relative time label from a signed minute offset.
     * e.g. +270 → "in 4 hours 30 minutes", -90 → "1 hour 30 minutes ago"
     */
    fun timeRelativeLabel(totalMinutes: Long): String {
        val duration = timeDurationLabel(kotlin.math.abs(totalMinutes))
        return if (totalMinutes >= 0) "in $duration" else "$duration ago"
    }

    /**
     * Parses time arithmetic queries like "6 hours from now", "45 minutes ago",
     * "2 hours 30 minutes from now". Returns a [TimeResult] with the formatted time,
     * an optional day context ("tomorrow" / "yesterday"), and isAbsolute = true.
     *
     * Returns null if the query doesn't match.
     */
    fun parseTimeArithmeticQuery(query: String): TimeResult? {
        val lower = query.trim().lowercase(Locale.US)

        val isFuture = lower.endsWith(" from now") || lower.endsWith(" later")
        val isPast = lower.endsWith(" ago")
        if (!isFuture && !isPast) return null

        val core = when {
            lower.endsWith(" from now") -> lower.removeSuffix(" from now").trim()
            lower.endsWith(" later") -> lower.removeSuffix(" later").trim()
            else -> lower.removeSuffix(" ago").trim()
        }

        val unitPattern = Regex("""(\d+)\s*(hours?|hrs?|minutes?|mins?|seconds?|secs?)""")
        var hours = 0L
        var minutes = 0L
        var remaining = core
        for (match in unitPattern.findAll(core)) {
            val value = match.groupValues[1].toLongOrNull() ?: continue
            when {
                match.groupValues[2].startsWith("hour") || match.groupValues[2].startsWith("hr") -> hours = value
                match.groupValues[2].startsWith("min") -> minutes = value
                match.groupValues[2].startsWith("sec") -> minutes += value / 60
            }
            remaining = remaining.replace(match.value, " ")
        }

        if (remaining.trim().any { it.isLetterOrDigit() }) return null
        if (hours == 0L && minutes == 0L) return null

        val totalMinutes = hours * 60 + minutes
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val resultDt = if (isFuture) now.plusMinutes(totalMinutes) else now.minusMinutes(totalMinutes)
        val (time, context) = formatTimeWithDayContext(resultDt)
        return TimeResult(label = time, contextLabel = context, isAbsolute = true)
    }

    /**
     * Parses an absolute time reference like "5am", "5:00 AM", "14:30" and returns both
     * the past occurrence and the next future occurrence as a pair (past, future).
     *
     * e.g. "1pm" at 10:55 PM → past: "9 hours 55 minutes ago" / "today", future: "in 14 hours 5 minutes" / "tomorrow"
     * e.g. "1pm" at  9:00 AM → past: "20 hours ago" / "yesterday", future: "in 4 hours" / "today"
     *
     * Returns null if the query is not a standalone time expression.
     */
    fun parseAbsoluteTimeQuery(query: String): Pair<TimeResult, TimeResult>? {
        val time = parseTimeString(query.trim()) ?: return null
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val nowTime = now.toLocalTime()
        // Signed diff: positive = time is still ahead today, negative = time already passed today
        val diffMinutes = java.time.Duration.between(nowTime, time).toMinutes()

        val past: TimeResult
        val future: TimeResult
        if (diffMinutes >= 0) {
            // Time is still in the future today
            val minutesUntil = diffMinutes
            val minutesSince = 24 * 60 - diffMinutes
            future = TimeResult(label = "${timeDurationLabel(minutesUntil)} later", contextLabel = "today", isAbsolute = false)
            past = TimeResult(label = "${timeDurationLabel(minutesSince)} ago", contextLabel = "yesterday", isAbsolute = false)
        } else {
            // Time already passed today
            val minutesSince = -diffMinutes
            val minutesUntil = 24 * 60 - minutesSince
            past = TimeResult(label = "${timeDurationLabel(minutesSince)} ago", contextLabel = "today", isAbsolute = false)
            future = TimeResult(label = "${timeDurationLabel(minutesUntil)} later", contextLabel = "tomorrow", isAbsolute = false)
        }
        return past to future
    }

    /**
     * Parses "3 hours after 5pm", "30 minutes before 9am" and similar mixed expressions.
     * Returns a [TimeResult] with the formatted time and isAbsolute = true.
     */
    fun parseTimeOffsetQuery(query: String): TimeResult? {
        val lower = query.trim().lowercase(Locale.US)

        val separators = listOf(" after " to true, " before " to false)
        for ((sep, isAddition) in separators) {
            val idx = lower.indexOf(sep)
            if (idx < 0) continue

            val unitsPart = lower.substring(0, idx).trim()
            val timePart = query.substring(idx + sep.length).trim()
            val baseTime = parseTimeString(timePart) ?: continue

            val unitPattern = Regex("""(\d+)\s*(hours?|hrs?|minutes?|mins?)""")
            var hours = 0L
            var minutes = 0L
            var remaining = unitsPart
            for (match in unitPattern.findAll(unitsPart)) {
                val value = match.groupValues[1].toLongOrNull() ?: continue
                when {
                    match.groupValues[2].startsWith("hour") || match.groupValues[2].startsWith("hr") -> hours = value
                    match.groupValues[2].startsWith("min") -> minutes = value
                }
                remaining = remaining.replace(match.value, " ")
            }

            if (remaining.trim().any { it.isLetterOrDigit() }) continue
            if (hours == 0L && minutes == 0L) continue

            val totalMinutes = hours * 60 + minutes
            val result = if (isAddition) baseTime.plusMinutes(totalMinutes) else baseTime.minusMinutes(totalMinutes)
            return TimeResult(label = formatTime12Hr(result), isAbsolute = true)
        }
        return null
    }

    /**
     * Parses "9am to 5:30pm" or "14:00 to 17:30" and returns the duration,
     * e.g. "8 hours 30 minutes". Returns null if both sides are not valid times.
     */
    fun parseTimeDiffQuery(query: String): TimeResult? {
        val lower = query.trim().lowercase(Locale.US)
        val toIdx = lower.indexOf(" to ")
        if (toIdx < 0) return null

        val part1 = query.substring(0, toIdx).trim()
        val part2 = query.substring(toIdx + 4).trim()
        val t1 = parseTimeString(part1) ?: return null
        val t2 = parseTimeString(part2) ?: return null

        val duration = java.time.Duration.between(t1, t2).abs()
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val parts = buildList {
            if (hours > 0) add("$hours ${if (hours == 1L) "hour" else "hours"}")
            if (mins > 0) add("$mins ${if (mins == 1L) "minute" else "minutes"}")
        }
        val label = if (parts.isEmpty()) "0 minutes" else parts.joinToString(" ")
        return TimeResult(label = label, isAbsolute = false)
    }

    /**
     * Parses a date query containing a month name, a 4-digit year, and a 1-2 digit day in any order.
     *
     * Supported formats:
     *   March 12 2025 | 12 March 2025 | 2025 March 12 | 2025 12 March | Mar 12 2025 | etc.
     */
    fun parseDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        // Must contain a recognizable month name
        val monthMatch = monthPattern.find(lower) ?: return null
        val monthNum = monthNames[monthMatch.value.lowercase(Locale.US)] ?: return null

        // Remove the month name to isolate numbers
        val withoutMonth = lower.replace(monthMatch.value, " ")

        // Find 4-digit year, defaulting to current year if not provided
        val yearMatch = yearPattern.find(withoutMonth)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: LocalDate.now().year

        // Remove year to isolate the day
        val withoutYear = if (yearMatch != null) withoutMonth.replace(yearMatch.value, " ") else withoutMonth

        // Remaining numeric token is the day
        val dayMatch = dayPattern.find(withoutYear.trim()) ?: return null
        val day = dayMatch.groupValues[1].toIntOrNull() ?: return null

        // Ensure no unexpected extra tokens remain
        val remaining = withoutYear.replace(dayMatch.value, " ").trim()
        if (remaining.any { it.isLetterOrDigit() }) return null

        return try {
            LocalDate.of(year, monthNum, day)
        } catch (_: Exception) {
            null
        }
    }
}
