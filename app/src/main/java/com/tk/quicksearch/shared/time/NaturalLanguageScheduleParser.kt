package com.tk.quicksearch.shared.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

// SHARED FILE. The same file (apart from the package line) lives in quick-search and noted-android,
// and NaturalLanguageScheduleParser.swift in mac-search and noted-mac follows the same rules.
// Change all four together; ~/Projects/misc/check-time-logic-sync.sh reports copies that drifted,
// and ~/Projects/misc/time-logic-standardization.md lists every supported phrase.

/** UTF-16 offsets into the parsed text. */
data class ScheduleTextRange(val start: Int, val endExclusive: Int) {
    fun overlaps(other: ScheduleTextRange): Boolean = start < other.endExclusive && other.start < endExclusive
}

enum class ScheduleRepeatUnit { Day, Week, Month, Year }

data class ScheduleRepeat(val interval: Int, val unit: ScheduleRepeatUnit)

data class ScheduleParseOptions(
    /** Reads "daily", "every Friday", "every 2 weeks", "5th monthly". Off where items can't repeat. */
    val recurrence: Boolean = true,
    /** Reads an hour/minute count at the very start or end ("Stretch 10 min", "1h30min tea") as "in ...". */
    val bareDurations: Boolean = false,
)

data class ParsedSchedule(
    /** The text with the recognized phrases removed. */
    val title: String,
    /** Start of the day when [hasTime] is false. */
    val dueDate: Instant,
    val hasTime: Boolean,
    val repeat: ScheduleRepeat?,
    /** The recognized phrases, merged where they overlap and sorted. */
    val highlightedRanges: List<ScheduleTextRange>,
)

/** Finds a date, time and repeat rule typed inside a title ("Call mom tomorrow at 4:30pm"). */
object NaturalLanguageScheduleParser {
    private enum class DateKind { Month, Numeric, Duration, RelativeDay, Ordinal, Weekday }

    private enum class TimeKind { TwelveHour, TwentyFourHour, Named }

    private const val LEAD_IN = """(?:(?:on|by|due)\s+)?"""
    private const val MONTH_NAMES = """january|february|march|april|may|june|july|august|september|october|november|december|jan\.?|feb\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|sept\.?|oct\.?|nov\.?|dec\.?"""
    private const val WEEKDAY_NAMES = """monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon\.?|tues\.?|tue\.?|wed\.?|weds\.?|thu\.?|thur\.?|thurs\.?|fri\.?|sat\.?|sun\.?"""
    private const val TIME_UNITS = """(?:hours?|hrs?|hr|h|minutes?|mins?|min)"""
    private const val DURATION_UNITS = """(?:years?|months?|weeks?|days?|hours?|hrs?|hr|h|minutes?|mins?|min)"""
    private const val REPEAT_UNITS = """daily|weekly|monthly|yearly|annually|day|week|month|year"""

    private fun regex(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private val monthPattern = regex("""\b$LEAD_IN($MONTH_NAMES)\s+([0-3]?\d)(?:st|nd|rd|th)?(?:\s*,?\s*(\d{4}))?\b""")
    private val numericDatePattern = regex("""\b$LEAD_IN(0?[1-9]|1[0-2])[/-]([0-3]?\d)(?:[/-](\d{2}|\d{4}))?\b""")
    private val durationPattern = regex("""\b(?:in\s*(?:\d+\s*$DURATION_UNITS\s*)+|(?:\d+\s*$DURATION_UNITS\s*)+ago)\b""")
    private val durationComponentPattern = regex("""(\d+)\s*(years?|months?|weeks?|days?|hours?|hrs?|hr|h|minutes?|mins?|min)(?=\s|$|\d|ago\b)""")
    private val bareTrailingDurationPattern = regex("""\b(?:\d+\s*$TIME_UNITS\s*)*\d+\s*$TIME_UNITS(?=\s*$)""")
    private val bareLeadingDurationPattern = regex("""^\s*(?:\d+\s*$TIME_UNITS\s*)*\d+\s*$TIME_UNITS\b""")
    private val relativeDayPattern = regex("""\b$LEAD_IN(today|tomorrow|yesterday)\b""")
    private val ordinalRecurringPattern = regex("""\b(?:on\s+)?([0-3]?\d)(st|nd|rd|th)\s+(monthly|month|yearly|annually|year)\b""")
    private val weekdayPattern = regex("""\b$LEAD_IN($WEEKDAY_NAMES)s?\b""")
    private val recurringWeekdayPattern = regex("""\b(?:(?:on|by|due|every|each)\s+)?($WEEKDAY_NAMES)s?\b""")
    private val twelveHourTimePattern = regex("""\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""")
    private val twentyFourHourTimePattern = regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val namedTimePattern = regex("""\b(?:at\s+)?(noon|midnight)\b""")
    private val repeatIntervalPattern = regex("""\b(?:repeat(?:s|ing)?\s+)?(?:every|each)\s+(\d+)\s+($REPEAT_UNITS|$WEEKDAY_NAMES)s?\b""")
    private val repeatPattern = regex("""\b(?:(?:repeat(?:s|ing)?\s+|(?:repeat(?:s|ing)?\s+)?(?:every|each)\s+)($REPEAT_UNITS|$WEEKDAY_NAMES)s?|(daily|weekly|monthly|yearly|annually))\b""")
    private val whitespacePattern = Regex("""\s+""")

    private val timePatterns = listOf(
        TimeKind.TwelveHour to twelveHourTimePattern,
        TimeKind.TwentyFourHour to twentyFourHourTimePattern,
        TimeKind.Named to namedTimePattern,
    )
    private val timeUnitTokens = setOf("hour", "hours", "hr", "hrs", "h", "minute", "minutes", "min", "mins")
    private val months = mapOf(
        "january" to 1, "jan" to 1, "february" to 2, "feb" to 2, "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4, "may" to 5, "june" to 6, "jun" to 6, "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8, "september" to 9, "sep" to 9, "sept" to 9, "october" to 10,
        "oct" to 10, "november" to 11, "nov" to 11, "december" to 12, "dec" to 12,
    )

    /** Sunday is 1, as in `java.util.Calendar` and Foundation's `Calendar`. */
    private val weekdays = mapOf(
        "sunday" to 1, "sun" to 1, "monday" to 2, "mon" to 2, "tuesday" to 3, "tues" to 3,
        "tue" to 3, "wednesday" to 4, "wed" to 4, "weds" to 4, "thursday" to 5, "thu" to 5,
        "thur" to 5, "thurs" to 5, "friday" to 6, "fri" to 6, "saturday" to 7, "sat" to 7,
    )

    /** Null when the text holds no date, time or repeat phrase. */
    fun parse(
        text: String,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        options: ScheduleParseOptions = ScheduleParseOptions(),
    ): ParsedSchedule? {
        val ranges = mutableListOf<ScheduleTextRange>()
        val today = now.atZone(zoneId).toLocalDate()

        // 1. The date: the first pattern, in priority order, that yields a real date.
        var date: ZonedDateTime? = null
        var dateKind: DateKind? = null
        var durationSetsTime = false
        for ((kind, pattern) in datePatterns(options)) {
            val match = pattern.find(text) ?: continue
            date = parsedDate(kind, match, now, zoneId) ?: continue
            dateKind = kind
            durationSetsTime = kind == DateKind.Duration && hasTimeComponent(match.value)
            ranges += match.range.toTextRange()
            break
        }

        // 2. The time of day, outside the date phrase.
        var time: LocalTime? = null
        for ((kind, pattern) in timePatterns) {
            val match = pattern.findAll(text).firstOrNull { found ->
                ranges.none { it.overlaps(found.range.toTextRange()) }
            } ?: continue
            time = parsedTime(kind, match) ?: continue
            ranges += match.range.toTextRange()
            break
        }

        // 3. The repeat rule.
        var repeat: ScheduleRepeat? = null
        if (options.recurrence) {
            val intervalMatch = repeatIntervalPattern.find(text)
            val match = intervalMatch ?: repeatPattern.find(text)
            if (match != null) {
                val interval = if (intervalMatch != null) match.groupValues[1].toIntOrNull() ?: 1 else 1
                val unitToken = if (intervalMatch != null) match.groupValues[2] else match.groupValues[1].ifEmpty { match.groupValues[2] }
                repeat = repeatUnit(unitToken)?.let { ScheduleRepeat(interval.coerceAtLeast(1), it) }
                if (repeat != null) ranges += match.range.toTextRange()
            }
        }

        if (date == null && time == null && repeat == null) return null

        // 4. Combine. A time or repeat rule without a date means today.
        var due = date ?: today.atStartOfDay(zoneId)
        if (time != null) {
            due = due.toLocalDate().atTime(time).atZone(zoneId)
            // "at 4pm" typed at 6pm means tomorrow, and "Friday at 4pm" typed on Friday at 6pm means next week.
            if (due.toInstant().isBefore(now)) {
                if (date == null) due = due.plusDays(1) else if (dateKind == DateKind.Weekday) due = due.plusDays(7)
            }
        }

        val merged = mergedRanges(ranges)
        return ParsedSchedule(
            title = cleanedTitle(text, merged),
            dueDate = due.toInstant(),
            hasTime = time != null || durationSetsTime,
            repeat = repeat,
            highlightedRanges = merged,
        )
    }

    /** [text] without [ranges], with whitespace collapsed and stray commas trimmed. */
    fun cleanedTitle(text: String, ranges: List<ScheduleTextRange>): String {
        val builder = StringBuilder(text)
        mergedRanges(ranges).sortedByDescending { it.start }.forEach { builder.replace(it.start, it.endExclusive, " ") }
        return whitespacePattern.replace(builder.toString(), " ").trim(' ', '\n', '\t', ',')
    }

    private fun datePatterns(options: ScheduleParseOptions): List<Pair<DateKind, Regex>> = buildList {
        add(DateKind.Month to monthPattern)
        add(DateKind.Numeric to numericDatePattern)
        add(DateKind.Duration to durationPattern)
        add(DateKind.RelativeDay to relativeDayPattern)
        if (options.recurrence) add(DateKind.Ordinal to ordinalRecurringPattern)
        add(DateKind.Weekday to if (options.recurrence) recurringWeekdayPattern else weekdayPattern)
        if (options.bareDurations) {
            add(DateKind.Duration to bareTrailingDurationPattern)
            add(DateKind.Duration to bareLeadingDurationPattern)
        }
    }

    private fun parsedDate(kind: DateKind, match: MatchResult, now: Instant, zoneId: ZoneId): ZonedDateTime? {
        val today = now.atZone(zoneId).toLocalDate()
        val values = match.groupValues
        val day: LocalDate? = when (kind) {
            DateKind.Duration -> return parsedDuration(match.value, now, zoneId)
            DateKind.RelativeDay -> today.plusDays(
                when (token(values[1])) {
                    "tomorrow" -> 1L
                    "yesterday" -> -1L
                    else -> 0L
                },
            )
            DateKind.Weekday -> weekdays[token(values[1])]?.let { weekday ->
                val current = today.dayOfWeek.value % 7 + 1
                today.plusDays(((weekday - current + 7) % 7).toLong())
            }
            DateKind.Ordinal -> values[1].toIntOrNull()?.let { dayOfMonth ->
                if (token(values[3]) in setOf("monthly", "month")) nextMonthlyDate(dayOfMonth, today) else nextYearlyDate(dayOfMonth, today)
            }
            DateKind.Month, DateKind.Numeric -> {
                val month = if (kind == DateKind.Month) months[token(values[1])] else values[1].toIntOrNull()
                val dayOfMonth = values[2].toIntOrNull()
                val rawYear = values.getOrNull(3).orEmpty()
                when {
                    month == null || dayOfMonth == null -> null
                    // Without a year: the next time that date comes around, today included.
                    rawYear.isEmpty() -> (0 until 10).firstNotNullOfOrNull { offset ->
                        validDate(today.year + offset, month, dayOfMonth)?.takeIf { !it.isBefore(today) }
                    }
                    else -> year(rawYear)?.let { validDate(it, month, dayOfMonth) }
                }
            }
        }
        return day?.atStartOfDay(zoneId)
    }

    private fun parsedTime(kind: TimeKind, match: MatchResult): LocalTime? {
        val values = match.groupValues
        if (kind == TimeKind.Named) return if (token(values[1]) == "noon") LocalTime.NOON else LocalTime.MIDNIGHT
        var hour = values[1].toIntOrNull() ?: return null
        val minute = values.getOrNull(2)?.toIntOrNull() ?: 0
        if (kind == TimeKind.TwelveHour) hour = hour % 12 + if (token(values[3]) == "pm") 12 else 0
        return LocalTime.of(hour, minute)
    }

    /** "in 2 hours 15 min" counts from now; "in 3 days" counts from the start of today. */
    private fun parsedDuration(phrase: String, now: Instant, zoneId: ZoneId): ZonedDateTime? {
        var years = 0L
        var months = 0L
        var days = 0L
        var duration = Duration.ZERO
        var found = false
        val sign = if (token(phrase.trim()).endsWith("ago")) -1 else 1
        for (match in durationComponentPattern.findAll(phrase)) {
            val value = (match.groupValues[1].toLongOrNull() ?: continue) * sign
            found = true
            when (token(match.groupValues[2])) {
                "year", "years" -> years += value
                "month", "months" -> months += value
                "week", "weeks" -> days += value * 7
                "day", "days" -> days += value
                "hour", "hours", "hr", "hrs", "h" -> duration = duration.plusHours(value)
                else -> duration = duration.plusMinutes(value)
            }
        }
        if (!found) return null
        val base = if (hasTimeComponent(phrase)) now.atZone(zoneId) else now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId)
        return base.plusYears(years).plusMonths(months).plusDays(days).plus(duration)
    }

    private fun hasTimeComponent(phrase: String): Boolean =
        durationComponentPattern.findAll(phrase).any { token(it.groupValues[2]) in timeUnitTokens }

    private fun repeatUnit(rawToken: String): ScheduleRepeatUnit? = when (val unit = token(rawToken)) {
        "daily", "day" -> ScheduleRepeatUnit.Day
        "weekly", "week" -> ScheduleRepeatUnit.Week
        "monthly", "month" -> ScheduleRepeatUnit.Month
        "yearly", "annually", "year" -> ScheduleRepeatUnit.Year
        else -> if (unit in weekdays) ScheduleRepeatUnit.Week else null
    }

    private fun nextMonthlyDate(dayOfMonth: Int, today: LocalDate): LocalDate? = (0 until 24).firstNotNullOfOrNull { offset ->
        val month = today.plusMonths(offset.toLong())
        validDate(month.year, month.monthValue, dayOfMonth)?.takeIf { !it.isBefore(today) }
    }

    private fun nextYearlyDate(dayOfMonth: Int, today: LocalDate): LocalDate? = (0 until 10).firstNotNullOfOrNull { offset ->
        validDate(today.year + offset, today.monthValue, dayOfMonth)?.takeIf { !it.isBefore(today) }
    }

    private fun mergedRanges(ranges: List<ScheduleTextRange>): List<ScheduleTextRange> {
        val merged = mutableListOf<ScheduleTextRange>()
        for (range in ranges.sortedBy { it.start }) {
            val last = merged.lastOrNull()
            if (last != null && range.start < last.endExclusive) {
                merged[merged.lastIndex] = ScheduleTextRange(last.start, maxOf(last.endExclusive, range.endExclusive))
            } else {
                merged += range
            }
        }
        return merged
    }

    private fun validDate(year: Int, month: Int, day: Int): LocalDate? = runCatching { LocalDate.of(year, month, day) }.getOrNull()

    private fun year(raw: String): Int? = raw.toIntOrNull()?.let { if (raw.length == 2) (if (it >= 70) 1900 + it else 2000 + it) else it }

    private fun token(raw: String): String = raw.lowercase(Locale.US).trimEnd('.')

    private fun IntRange.toTextRange() = ScheduleTextRange(first, last + 1)
}
