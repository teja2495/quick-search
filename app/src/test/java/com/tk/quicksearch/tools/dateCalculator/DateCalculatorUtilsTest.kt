package com.tk.quicksearch.tools.dateCalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateCalculatorUtilsTest {

    @Test
    fun parseTimeDiffQuery_returnsSameDayDuration() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("9:00 AM to 6:00 PM")
        assertNotNull(result)
        assertEquals("9 hours", result?.label)
    }

    @Test
    fun parseTimeDiffQuery_returnsOvernightForwardDuration() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("6:00 PM to 9:00 AM")
        assertNotNull(result)
        assertEquals("15 hours", result?.label)
    }

    @Test
    fun parseTimeDiffQuery_returnsZeroForEqualTimes() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("6:00 PM to 6:00 PM")
        assertNotNull(result)
        assertEquals("0 minutes", result?.label)
    }

    @Test
    fun parseTimeArithmeticQuery_readsBareDurationAsFromNow() {
        val bare = DateCalculatorUtils.parseTimeArithmeticQuery("5min")
        val explicit = DateCalculatorUtils.parseTimeArithmeticQuery("5 minutes from now")
        assertNotNull(bare)
        assertEquals(explicit?.label, bare?.label)
        assertEquals(true, bare?.isAbsolute)
    }

    @Test
    fun parseTimeArithmeticQuery_readsBareCompoundAndHourAlias() {
        assertEquals(
            DateCalculatorUtils.parseTimeArithmeticQuery("2 hours 30 minutes from now")?.label,
            DateCalculatorUtils.parseTimeArithmeticQuery("2 hours 30 minutes")?.label,
        )
        assertEquals(
            DateCalculatorUtils.parseTimeArithmeticQuery("3 hours from now")?.label,
            DateCalculatorUtils.parseTimeArithmeticQuery("3h")?.label,
        )
    }

    @Test
    fun parseTimeArithmeticQuery_acceptsInPrefixAndBareSeconds() {
        assertNotNull(DateCalculatorUtils.parseTimeArithmeticQuery("in 45 minutes"))
        assertNotNull(DateCalculatorUtils.parseTimeArithmeticQuery("30 sec"))
    }

    @Test
    fun parseTrailingTimeArithmeticQuery_readsReminderStyleQuery() {
        val standalone = DateCalculatorUtils.parseTimeArithmeticQuery("in 1 hr")
        val reminderStyle = DateCalculatorUtils.parseTrailingTimeArithmeticQuery("Laundry in 1 hr")

        assertEquals(standalone?.label, reminderStyle?.label)
        assertEquals(standalone?.contextLabel, reminderStyle?.contextLabel)
    }

    @Test
    fun parseTrailingTimeArithmeticQuery_ignoresNonTrailingExpression() {
        assertNull(DateCalculatorUtils.parseTrailingTimeArithmeticQuery("in 1 hr laundry"))
    }

    @Test
    fun parseTimeArithmeticQuery_ignoresNonDurationQueries() {
        assertNull(DateCalculatorUtils.parseTimeArithmeticQuery("5"))
        assertNull(DateCalculatorUtils.parseTimeArithmeticQuery("5 m"))
        assertNull(DateCalculatorUtils.parseTimeArithmeticQuery("5pm"))
        assertNull(DateCalculatorUtils.parseTimeArithmeticQuery("hours"))
        assertNull(DateCalculatorUtils.parseTimeArithmeticQuery("3 hours after 5pm"))
    }

    @Test
    fun parseRelativeDateQuery_readsBareDurationAsFutureDate() {
        assertEquals(
            LocalDate.now().plusWeeks(2),
            DateCalculatorUtils.parseRelativeDateQuery("2 weeks"),
        )
        assertEquals(
            LocalDate.now().plusMonths(3),
            DateCalculatorUtils.parseRelativeDateQuery("3 months"),
        )
        assertEquals(
            LocalDate.now().minusDays(10),
            DateCalculatorUtils.parseRelativeDateQuery("10 days ago"),
        )
    }

    @Test
    fun parseRelativeDateQuery_ignoresNonDurationQueries() {
        assertNull(DateCalculatorUtils.parseRelativeDateQuery("march 12"))
        assertNull(DateCalculatorUtils.parseRelativeDateQuery("5 days from march 2"))
        assertNull(DateCalculatorUtils.parseRelativeDateQuery("2020"))
    }

    /** Mirrors the parser precedence [DateCalculatorHandler] relies on for bare durations. */
    @Test
    fun bareDuration_onlyMatchesTheArithmeticParser() {
        listOf("5min", "3h", "2 hours 30 minutes").forEach { query ->
            assertNull(DateCalculatorUtils.parseTimeDiffQuery(query))
            assertNull(DateCalculatorUtils.parseTimeOffsetQuery(query))
            assertNull(DateCalculatorUtils.parseAbsoluteTimeQuery(query))
            assertNull(DateCalculatorUtils.parseDateQuery(query))
            assertNotNull(DateCalculatorUtils.parseTimeArithmeticQuery(query))
        }
        listOf("2 weeks", "3 months").forEach { query ->
            assertNull(DateCalculatorUtils.parseTimeArithmeticQuery(query))
            assertNull(DateCalculatorUtils.parseDateDiffQuery(query))
            assertNull(DateCalculatorUtils.parseOffsetFromDateQuery(query))
            assertNull(DateCalculatorUtils.parseDateQuery(query))
            assertNotNull(DateCalculatorUtils.parseRelativeDateQuery(query))
        }
    }

    @Test
    fun durationLabelsUseTheSharedUnitWording() {
        val start = LocalDate.of(2026, 6, 4)
        assertEquals("1 year 2 months 3 days", DateCalculatorUtils.diffLabel(start, LocalDate.of(2027, 8, 7)))
        assertEquals("1 month", DateCalculatorUtils.diffLabel(LocalDate.of(2026, 7, 4), start))
        assertEquals("0 days", DateCalculatorUtils.diffLabel(start, start))
        assertEquals("in 4 hours 30 minutes", DateCalculatorUtils.timeRelativeLabel(270))
        assertEquals("1 hour 1 minute ago", DateCalculatorUtils.timeRelativeLabel(-61))
        assertEquals("in 0 minutes", DateCalculatorUtils.timeRelativeLabel(0))
        assertEquals("8 hours 30 minutes", DateCalculatorUtils.parseTimeDiffQuery("9am to 5:30pm")?.label)
        assertEquals("0 minutes", DateCalculatorUtils.parseTimeDiffQuery("9am to 9am")?.label)
    }
}
