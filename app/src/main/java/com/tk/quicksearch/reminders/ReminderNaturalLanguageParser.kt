package com.tk.quicksearch.reminders

import com.tk.quicksearch.shared.time.NaturalLanguageScheduleParser
import com.tk.quicksearch.shared.time.ScheduleParseOptions
import com.tk.quicksearch.shared.time.ScheduleRepeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Extracts a reminder date, optional time and repeat rule ("every Friday", "daily") from the
 * reminder title. The rules live in the shared [NaturalLanguageScheduleParser]; a bare duration at
 * either end ("Stretch 10 min") counts as "in 10 min".
 */
internal object ReminderNaturalLanguageParser {
    internal data class TextRange(val start: Int, val endExclusive: Int)

    internal data class Schedule(
        val title: String,
        val date: LocalDate,
        val time: LocalTime?,
        val repeat: ScheduleRepeat?,
        val highlightedRanges: List<TextRange>,
    )

    private val options = ScheduleParseOptions(recurrence = true, bareDurations = true)

    fun parse(text: String, now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): Schedule? {
        val parsed = NaturalLanguageScheduleParser.parse(text, now, zoneId, options) ?: return null
        val due = parsed.dueDate.atZone(zoneId)
        return Schedule(
            title = parsed.title,
            date = due.toLocalDate(),
            time = if (parsed.hasTime) due.toLocalTime() else null,
            repeat = parsed.repeat,
            highlightedRanges = parsed.highlightedRanges.map { TextRange(it.start, it.endExclusive) },
        )
    }
}
