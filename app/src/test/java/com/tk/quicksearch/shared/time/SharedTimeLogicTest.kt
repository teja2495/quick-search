package com.tk.quicksearch.shared.time

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SHARED FILE. The same tests (apart from the package line) live in quick-search and noted-android.

/** Thursday, June 4 2026, 09:30 UTC. */
class SharedTimeLogicTest {
    private val now = Instant.parse("2026-06-04T09:30:00Z")
    private val zone = ZoneId.of("UTC")
    private val reminders = ScheduleParseOptions(recurrence = false, bareDurations = true)

    private fun parse(text: String, options: ScheduleParseOptions = ScheduleParseOptions()) =
        NaturalLanguageScheduleParser.parse(text, now, zone, options)

    private fun assertParsed(
        text: String,
        title: String,
        due: String,
        hasTime: Boolean,
        repeat: ScheduleRepeat? = null,
        options: ScheduleParseOptions = ScheduleParseOptions(),
    ) {
        val result = parse(text, options)
        assertEquals(text, title, result?.title)
        assertEquals(text, Instant.parse(due), result?.dueDate)
        assertEquals(text, hasTime, result?.hasTime)
        assertEquals(text, repeat, result?.repeat)
    }

    private fun label(
        date: String,
        hasTime: Boolean,
        style: RelativeTimeStyle = RelativeTimeStyle.Detailed,
        end: String? = null,
    ) = RelativeTimeFormatter.label(Instant.parse(date), hasTime, now, zone, style, end?.let(Instant::parse))

    @Test
    fun parsesDates() {
        assertParsed("Pay rent September 10", "Pay rent", "2026-09-10T00:00:00Z", false)
        assertParsed("Pay rent on Sep. 10th, 2027", "Pay rent", "2027-09-10T00:00:00Z", false)
        assertParsed("Taxes due 4/15", "Taxes", "2027-04-15T00:00:00Z", false)
        assertParsed("Party 12/31/26", "Party", "2026-12-31T00:00:00Z", false)
        assertParsed("Call mom tomorrow", "Call mom", "2026-06-05T00:00:00Z", false)
        assertParsed("Standup today", "Standup", "2026-06-04T00:00:00Z", false)
        assertParsed("Gym by Monday", "Gym", "2026-06-08T00:00:00Z", false)
        assertParsed("Review thu", "Review", "2026-06-04T00:00:00Z", false)
        // An impossible date falls through to the next phrase.
        assertParsed("Feb 30 tomorrow", "Feb 30", "2026-06-05T00:00:00Z", false)
    }

    @Test
    fun parsesTimes() {
        assertParsed("Call mom tomorrow at 4:30pm", "Call mom", "2026-06-05T16:30:00Z", true)
        assertParsed("Lunch 12 PM", "Lunch", "2026-06-04T12:00:00Z", true)
        assertParsed("Deploy 18:05", "Deploy", "2026-06-04T18:05:00Z", true)
        assertParsed("Lunch at noon", "Lunch", "2026-06-04T12:00:00Z", true)
        // A time that already passed today means tomorrow; with a weekday it means next week.
        assertParsed("Wake up at 7am", "Wake up", "2026-06-05T07:00:00Z", true)
        assertParsed("Sleep at midnight", "Sleep", "2026-06-05T00:00:00Z", true)
        assertParsed("Standup Thursday 9am", "Standup", "2026-06-11T09:00:00Z", true)
        // An explicit day is kept even when the time has passed.
        assertParsed("Standup today 9am", "Standup", "2026-06-04T09:00:00Z", true)
    }

    @Test
    fun parsesDurations() {
        assertParsed("Take a break in 2 hours", "Take a break", "2026-06-04T11:30:00Z", true)
        assertParsed("Take a break in 2min", "Take a break", "2026-06-04T09:32:00Z", true)
        assertParsed("Tea in 1h30min", "Tea", "2026-06-04T11:00:00Z", true)
        assertParsed("Check oven in 1 hr 5 min", "Check oven", "2026-06-04T10:35:00Z", true)
        assertParsed("Follow up in 1day 2hours", "Follow up", "2026-06-05T11:30:00Z", true)
        assertParsed("Submit report in 3 days", "Submit report", "2026-06-07T00:00:00Z", false)
        assertParsed("Renew passport in 2 months 5 days", "Renew passport", "2026-08-09T00:00:00Z", false)
        assertParsed("Trip in 2 weeks", "Trip", "2026-06-18T00:00:00Z", false)
        assertParsed("Log medication 20 min ago", "Log medication", "2026-06-04T09:10:00Z", true)
        assertParsed("Backfill report 2 days ago", "Backfill report", "2026-06-02T00:00:00Z", false)
    }

    @Test
    fun bareDurationsAreOptional() {
        assertParsed("test 1 min", "test", "2026-06-04T09:31:00Z", true, options = reminders)
        assertParsed("1h30min check oven", "check oven", "2026-06-04T11:00:00Z", true, options = reminders)
        assertEquals("", parse("10 min", reminders)?.title)
        assertNull(parse("Run 5 min intervals", reminders))
        assertNull(parse("Plank 2 min"))
    }

    @Test
    fun parsesRecurrence() {
        val weekly = ScheduleRepeat(1, ScheduleRepeatUnit.Week)
        assertParsed("Review inbox daily", "Review inbox", "2026-06-04T00:00:00Z", false, ScheduleRepeat(1, ScheduleRepeatUnit.Day))
        assertParsed("Pickup Harsha every Friday at 4pm", "Pickup Harsha", "2026-06-05T16:00:00Z", true, weekly)
        assertParsed("Pickup Harsha Friday at 4pm", "Pickup Harsha", "2026-06-05T16:00:00Z", true)
        assertParsed("Water plants every 3 days", "Water plants", "2026-06-04T00:00:00Z", false, ScheduleRepeat(3, ScheduleRepeatUnit.Day))
        assertParsed("Bins every 2 fridays", "Bins", "2026-06-05T00:00:00Z", false, ScheduleRepeat(2, ScheduleRepeatUnit.Week))
        assertParsed("Pay rent 1st monthly", "Pay rent", "2026-07-01T00:00:00Z", false, ScheduleRepeat(1, ScheduleRepeatUnit.Month))
        assertParsed("Backup repeats weekly", "Backup", "2026-06-04T00:00:00Z", false, weekly)
        assertParsed("Vitamins daily at 8am", "Vitamins", "2026-06-05T08:00:00Z", true, ScheduleRepeat(1, ScheduleRepeatUnit.Day))
        assertParsed("Renew domain yearly", "Renew domain", "2026-06-04T00:00:00Z", false, ScheduleRepeat(1, ScheduleRepeatUnit.Year))
    }

    @Test
    fun recurrenceCanBeTurnedOff() {
        assertNull(parse("Review inbox daily", reminders))
        assertParsed("Gym every Friday", "Gym every", "2026-06-05T00:00:00Z", false, options = reminders)
    }

    @Test
    fun ignoresOrdinaryTextAndReportsRanges() {
        assertNull(parse("Buy oat milk"))
        assertNull(parse("Read 20 pages within 5 chapters"))
        assertEquals(listOf(ScheduleTextRange(9, 17), ScheduleTextRange(18, 27)), parse("Call mom tomorrow at 4:30pm")?.highlightedRanges)
        assertEquals(listOf(ScheduleTextRange(5, 20)), parse("Bins every 2 fridays")?.highlightedRanges)
    }

    @Test
    fun labelsTimedItems() {
        assertEquals("Now", label("2026-06-04T09:30:40Z", hasTime = true))
        assertEquals("Now", label("2026-06-04T09:00:00Z", hasTime = true, end = "2026-06-04T10:00:00Z"))
        assertEquals("30 minutes ago", label("2026-06-04T09:00:00Z", hasTime = true, end = "2026-06-04T09:15:00Z"))
        assertEquals("in 1 minute", label("2026-06-04T09:31:00Z", hasTime = true))
        assertEquals("in 45 minutes", label("2026-06-04T10:15:00Z", hasTime = true))
        assertEquals("in 1 hour", label("2026-06-04T11:29:00Z", hasTime = true))
        assertEquals("in 14 hours", label("2026-06-04T23:59:00Z", hasTime = true))
        assertEquals("2 hours ago", label("2026-06-04T07:00:00Z", hasTime = true))
        assertEquals("Tomorrow", label("2026-06-05T09:00:00Z", hasTime = true))
        assertEquals("Yesterday", label("2026-06-03T23:00:00Z", hasTime = true))
        assertEquals("in 5 days", label("2026-06-09T09:00:00Z", hasTime = true))
    }

    @Test
    fun labelsTimedItemsAcrossMidnightWhenClose() {
        val lateNight = Instant.parse("2026-06-04T23:50:00Z")
        val format = { date: String -> RelativeTimeFormatter.label(Instant.parse(date), true, lateNight, zone) }
        assertEquals("in 25 minutes", format("2026-06-05T00:15:00Z"))
        assertEquals("in 5 hours", format("2026-06-05T05:00:00Z"))
        assertEquals("Tomorrow", format("2026-06-05T07:00:00Z"))
    }

    @Test
    fun labelsDatesInEveryStyle() {
        assertEquals("Today", label("2026-06-04T00:00:00Z", hasTime = false))
        assertEquals("Tomorrow", label("2026-06-05T00:00:00Z", hasTime = false))
        assertEquals("Yesterday", label("2026-06-03T00:00:00Z", hasTime = false))
        assertEquals("in 2 days", label("2026-06-06T00:00:00Z", hasTime = false))
        assertEquals("3 days ago", label("2026-06-01T00:00:00Z", hasTime = false))
        assertEquals("in 1 month", label("2026-07-04T00:00:00Z", hasTime = false))
        assertEquals("in 1 month 4 days", label("2026-07-08T00:00:00Z", hasTime = false))
        assertEquals("in 1 month", label("2026-07-08T00:00:00Z", hasTime = false, style = RelativeTimeStyle.Concise))
        assertEquals("in 34 days", label("2026-07-08T00:00:00Z", hasTime = false, style = RelativeTimeStyle.TotalDays))
        assertEquals("2 years 3 months ago", label("2024-02-20T00:00:00Z", hasTime = false))
        assertEquals("2 years ago", label("2024-02-20T00:00:00Z", hasTime = false, style = RelativeTimeStyle.Concise))
        assertEquals("in 1 year", label("2027-06-10T00:00:00Z", hasTime = false))
        assertEquals("in 1 day", label("2026-06-05T00:00:00Z", hasTime = false, style = RelativeTimeStyle.TotalDays))
        assertEquals("Today", label("2026-06-04T15:00:00Z", hasTime = true, style = RelativeTimeStyle.TotalDays))
    }

    @Test
    fun exposesStructuredValuesForTranslatedApps() {
        val span = RelativeTimeFormatter.relativeTime(Instant.parse("2026-07-08T00:00:00Z"), hasTime = false, now = now, zoneId = zone)
        assertEquals(
            RelativeTime.Span(true, listOf(RelativeTime.Part(1, RelativeTimeUnit.Month), RelativeTime.Part(4, RelativeTimeUnit.Day))),
            span,
        )
        assertTrue(RelativeTimeFormatter.timeDistance(Instant.parse("2026-06-05T15:30:00Z"), now) is RelativeTime.Span)
        assertEquals("in 30 hours", RelativeTimeFormatter.format(RelativeTimeFormatter.timeDistance(Instant.parse("2026-06-05T15:30:00Z"), now)))
        assertFalse((RelativeTimeFormatter.timeDistance(Instant.parse("2026-06-04T08:30:00Z"), now) as RelativeTime.Span).isFuture)
    }
}
