package com.tk.quicksearch.reminders

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Extracts a one-time reminder date and optional time from the reminder title. */
internal object ReminderNaturalLanguageParser {
    internal data class TextRange(val start: Int, val endExclusive: Int)

    internal data class Schedule(
        val title: String,
        val date: LocalDate,
        val time: LocalTime?,
        val highlightedRanges: List<TextRange>,
    )

    private val monthPattern = Regex("""\b(?:(?:on|by|due)\s+)?(january|february|march|april|may|june|july|august|september|october|november|december|jan\.?|feb\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|sept\.?|oct\.?|nov\.?|dec\.?)\s+([0-3]?\d)(?:st|nd|rd|th)?(?:\s*,?\s*(\d{4}))?\b""", RegexOption.IGNORE_CASE)
    private val numericDatePattern = Regex("""\b(?:(?:on|by|due)\s+)?(0?[1-9]|1[0-2])[/-]([0-3]?\d)(?:[/-](\d{2}|\d{4}))?\b""", RegexOption.IGNORE_CASE)
    private val relativeDurationPattern = Regex("""\b(?:in\s*(?:(?:\d+)\s*(?:years?|months?|weeks?|days?|hours?|hrs?|hr|minutes?|mins?|min)\s*)+|(?:(?:\d+)\s*(?:years?|months?|weeks?|days?|hours?|hrs?|hr|minutes?|mins?|min)\s*)+ago)\b""", RegexOption.IGNORE_CASE)
    private val relativeDurationComponentPattern = Regex("""(\d+)\s*(years?|months?|weeks?|days?|hours?|hrs?|hr|h|minutes?|mins?|min)(?=\s|$|\d|ago\b)""", RegexOption.IGNORE_CASE)
    // A bare hour/minute duration ("Stretch 10 min", "1h30min tea") reads as "in <duration>", but
    // only at the very start or end of the text so counts inside a title stay part of the title.
    private val bareDurationUnits = """(?:hours?|hrs?|hr|h|minutes?|mins?|min)"""
    private val bareTrailingDurationPattern = Regex("""\b(?:\d+\s*$bareDurationUnits\s*)*\d+\s*$bareDurationUnits(?=\s*$)""", RegexOption.IGNORE_CASE)
    private val bareLeadingDurationPattern = Regex("""^\s*(?:\d+\s*$bareDurationUnits\s*)*\d+\s*$bareDurationUnits\b""", RegexOption.IGNORE_CASE)
    private val relativeDatePattern = Regex("""\b(?:(?:on|by|due)\s+)?(today|tomorrow|yesterday)\b""", RegexOption.IGNORE_CASE)
    private val weekdayDatePattern = Regex("""\b(?:(?:on|by|due)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon\.?|tues\.?|tue\.?|wed\.?|weds\.?|thu\.?|thur\.?|thurs\.?|fri\.?|sat\.?|sun\.?)\b""", RegexOption.IGNORE_CASE)
    private val twelveHourTimePattern = Regex("""\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val twentyFourHourTimePattern = Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)
    private val namedTimePattern = Regex("""\b(?:at\s+)?(noon|midnight)\b""", RegexOption.IGNORE_CASE)

    private val durationPatterns = setOf(relativeDurationPattern, bareTrailingDurationPattern, bareLeadingDurationPattern)

    private val months = mapOf(
        "january" to 1, "jan" to 1, "february" to 2, "feb" to 2, "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4, "may" to 5, "june" to 6, "jun" to 6, "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8, "september" to 9, "sep" to 9, "sept" to 9, "october" to 10,
        "oct" to 10, "november" to 11, "nov" to 11, "december" to 12, "dec" to 12,
    )
    private val weekdays = mapOf(
        "sunday" to 1, "sun" to 1, "monday" to 2, "mon" to 2, "tuesday" to 3, "tues" to 3,
        "tue" to 3, "wednesday" to 4, "wed" to 4, "weds" to 4, "thursday" to 5, "thu" to 5,
        "thur" to 5, "thurs" to 5, "friday" to 6, "fri" to 6, "saturday" to 7, "sat" to 7,
    )

    fun parse(text: String, now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): Schedule? {
        val dateMatch = listOf(monthPattern, numericDatePattern, relativeDurationPattern, relativeDatePattern, weekdayDatePattern, bareTrailingDurationPattern, bareLeadingDurationPattern)
            .firstNotNullOfOrNull { regex -> regex.find(text)?.let { regex to it } }
        val timeMatch = listOf(twelveHourTimePattern, twentyFourHourTimePattern, namedTimePattern)
            .firstNotNullOfOrNull { regex -> regex.find(text)?.let { regex to it } }
        val time = timeMatch?.let { (regex, match) -> parsedTime(regex, match) }
        val dateTime = dateMatch?.let { (regex, match) -> parsedDate(regex, match, now, zoneId) }

        if (dateTime == null && time == null) return null
        val date = (dateTime ?: now).atZone(zoneId).toLocalDate()
        val relativeTime = dateMatch
            ?.takeIf { (regex, match) -> regex in durationPatterns && relativeDurationHasTimeComponent(match.value) }
            ?.second
            ?.let { dateTime?.atZone(zoneId)?.toLocalTime() }
        val ranges = buildList {
            if (dateTime != null) add(dateMatch.second.range.toTextRange())
            if (time != null) add(timeMatch.second.range.toTextRange())
        }.distinct().sortedBy { it.start }
        return Schedule(
            title = cleanedTitle(text, ranges),
            date = date,
            time = time ?: relativeTime,
            highlightedRanges = ranges,
        )
    }

    private fun parsedDate(regex: Regex, match: MatchResult, now: Instant, zoneId: ZoneId): Instant? {
        val today = now.atZone(zoneId).toLocalDate()
        return when (regex) {
            relativeDurationPattern, bareTrailingDurationPattern, bareLeadingDurationPattern ->
                parsedRelativeDuration(match.value, now, zoneId)
            relativeDatePattern -> today.plusDays(when (token(match.groupValues[1])) {
                "tomorrow" -> 1
                "yesterday" -> -1
                else -> 0
            }).atStartOfDay(zoneId).toInstant()
            weekdayDatePattern -> weekdays[token(match.groupValues[1])]?.let { nextWeekday(it, today).atStartOfDay(zoneId).toInstant() }
            else -> {
                val month = if (regex == monthPattern) months[token(match.groupValues[1])] else match.groupValues[1].toIntOrNull()
                val day = match.groupValues[2].toIntOrNull()
                if (month == null || day == null) return null
                val rawYear = match.groupValues.getOrNull(3).orEmpty()
                val localDate = if (rawYear.isEmpty()) nextUpcomingDate(month, day, today) else year(rawYear)?.let { validDate(it, month, day) }
                localDate?.atStartOfDay(zoneId)?.toInstant()
            }
        }
    }

    private fun parsedTime(regex: Regex, match: MatchResult): LocalTime? {
        val values = match.groupValues
        if (regex == namedTimePattern) return if (token(values[1]) == "noon") LocalTime.NOON else LocalTime.MIDNIGHT
        var hour = values[1].toIntOrNull() ?: return null
        val minute = values.getOrNull(2)?.toIntOrNull() ?: 0
        if (regex == twelveHourTimePattern) hour = hour % 12 + if (token(values[3]) == "pm") 12 else 0
        return LocalTime.of(hour, minute)
    }

    private fun parsedRelativeDuration(phrase: String, now: Instant, zoneId: ZoneId): Instant? {
        var years = 0L
        var months = 0L
        var days = 0L
        var duration = Duration.ZERO
        val multiplier = if (token(phrase).endsWith("ago")) -1 else 1
        var found = false
        relativeDurationComponentPattern.findAll(phrase).forEach { match ->
            val value = (match.groupValues[1].toLongOrNull() ?: return@forEach) * multiplier
            found = true
            when (token(match.groupValues[2])) {
                "year", "years" -> years += value
                "month", "months" -> months += value
                "week", "weeks" -> days += value * 7
                "day", "days" -> days += value
                "hour", "hours", "hr", "hrs", "h" -> duration = duration.plusHours(value)
                "minute", "minutes", "min", "mins" -> duration = duration.plusMinutes(value)
            }
        }
        if (!found) return null
        val hasTime = relativeDurationHasTimeComponent(phrase)
        val base = if (hasTime) now.atZone(zoneId) else LocalDate.now(zoneId).atStartOfDay(zoneId)
        return base.plusYears(years).plusMonths(months).plusDays(days).plus(duration).toInstant()
    }

    private fun relativeDurationHasTimeComponent(phrase: String) = relativeDurationComponentPattern.findAll(phrase).any {
        token(it.groupValues[2]) in setOf("hour", "hours", "hr", "hrs", "h", "minute", "minutes", "min", "mins")
    }

    private fun cleanedTitle(text: String, ranges: List<TextRange>): String {
        val builder = StringBuilder(text)
        ranges.sortedByDescending { it.start }.forEach { range -> builder.replace(range.start, range.endExclusive, "") }
        return Regex("""\s+""").replace(builder.toString(), " ").trim(' ', '\n', '\t', ',')
    }

    private fun nextUpcomingDate(month: Int, day: Int, today: LocalDate) = (0 until 10).firstNotNullOfOrNull { offset ->
        validDate(today.year + offset, month, day)?.takeIf { !it.isBefore(today) }
    }
    private fun nextWeekday(weekday: Int, today: LocalDate): LocalDate {
        val current = today.dayOfWeek.value % 7 + 1
        return today.plusDays(((weekday - current + 7) % 7).toLong())
    }
    private fun validDate(year: Int, month: Int, day: Int) = runCatching { LocalDate.of(year, month, day) }.getOrNull()
    private fun year(raw: String): Int? = raw.toIntOrNull()?.let { if (raw.length == 2) if (it >= 70) 1900 + it else 2000 + it else it }
    private fun token(raw: String) = raw.lowercase(Locale.US).trimEnd('.')
    private fun IntRange.toTextRange() = TextRange(first, last + 1)
}
