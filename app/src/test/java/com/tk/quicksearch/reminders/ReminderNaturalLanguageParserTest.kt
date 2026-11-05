package com.tk.quicksearch.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderNaturalLanguageParserTest {
    private val now = Instant.parse("2026-06-04T09:30:00Z")
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun parsesNamedDateAndTime() {
        val result = ReminderNaturalLanguageParser.parse("Call mom tomorrow at 4:30pm", now, zoneId)

        assertEquals("Call mom", result?.title)
        assertEquals(LocalDate.of(2026, 6, 5), result?.date)
        assertEquals(LocalTime.of(16, 30), result?.time)
    }

    @Test
    fun parsesRelativeTimeAsADateAndTime() {
        val result = ReminderNaturalLanguageParser.parse("Take a break in 2 hours", now, zoneId)

        assertEquals("Take a break", result?.title)
        assertEquals(LocalDate.of(2026, 6, 4), result?.date)
        assertEquals(LocalTime.of(11, 30), result?.time)
    }

    @Test
    fun parsesCompactRelativeTime() {
        val result = ReminderNaturalLanguageParser.parse("Take a break in 2min", now, zoneId)

        assertEquals(LocalDate.of(2026, 6, 4), result?.date)
        assertEquals(LocalTime.of(9, 32), result?.time)
    }

    @Test
    fun parsesCompactRelativeDateAndTimeComponents() {
        val result = ReminderNaturalLanguageParser.parse("Follow up in 1day 2hours", now, zoneId)

        assertEquals(LocalDate.of(2026, 6, 5), result?.date)
        assertEquals(LocalTime.of(11, 30), result?.time)
    }

    @Test
    fun parsesTrailingBareDurationAsRelativeTime() {
        listOf("test 1 min", "test 1min", "Test 1 minute").forEach { query ->
            val result = ReminderNaturalLanguageParser.parse(query, now, zoneId)

            assertEquals(query, "test", result?.title?.lowercase())
            assertEquals(query, LocalDate.of(2026, 6, 4), result?.date)
            assertEquals(query, LocalTime.of(9, 31), result?.time)
        }
    }

    @Test
    fun parsesLeadingAndCompoundBareDuration() {
        val result = ReminderNaturalLanguageParser.parse("1h30min check oven", now, zoneId)

        assertEquals("check oven", result?.title)
        assertEquals(LocalTime.of(11, 0), result?.time)
    }

    @Test
    fun bareDurationAloneHasBlankTitle() {
        assertEquals("", ReminderNaturalLanguageParser.parse("10 min", now, zoneId)?.title)
    }

    @Test
    fun ignoresBareDurationInsideTitle() {
        assertNull(ReminderNaturalLanguageParser.parse("Run 5 min intervals", now, zoneId))
    }

    @Test
    fun parsesDateWithoutAddingATime() {
        val result = ReminderNaturalLanguageParser.parse("Pay rent September 10", now, zoneId)

        assertEquals(LocalDate.of(2026, 9, 10), result?.date)
        assertNull(result?.time)
    }

    @Test
    fun ignoresOrdinaryReminderText() {
        assertNull(ReminderNaturalLanguageParser.parse("Buy oat milk", now, zoneId))
    }
}
