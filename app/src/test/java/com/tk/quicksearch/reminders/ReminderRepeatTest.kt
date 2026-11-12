package com.tk.quicksearch.reminders

import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.shared.time.ScheduleRepeat
import com.tk.quicksearch.shared.time.ScheduleRepeatUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Thursday, June 4 2026, 09:30 UTC. */
class ReminderRepeatTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-06-04T09:30:00Z").toEpochMilli()
    private val today = LocalDate.of(2026, 6, 4)

    private fun next(date: LocalDate, timeMinutes: Int?, repeat: ScheduleRepeat) =
        ReminderRepeat.nextDate(ReminderInfo(1L, "Test", date, timeMinutes), repeat, now, zone)

    @Test
    fun movesToTheNextOccurrence() {
        val daily = ScheduleRepeat(1, ScheduleRepeatUnit.Day)
        assertEquals(today.plusDays(1), next(today, null, daily))
        assertEquals(today.plusDays(1), next(today, 8 * 60, daily))
        assertEquals(today.plusWeeks(2), next(today, 17 * 60, ScheduleRepeat(2, ScheduleRepeatUnit.Week)))
        assertEquals(LocalDate.of(2026, 7, 4), next(today, null, ScheduleRepeat(1, ScheduleRepeatUnit.Month)))
        assertEquals(LocalDate.of(2027, 6, 4), next(today, null, ScheduleRepeat(1, ScheduleRepeatUnit.Year)))
    }

    @Test
    fun skipsOccurrencesThatAlreadyPassed() {
        val daily = ScheduleRepeat(1, ScheduleRepeatUnit.Day)
        // Done days late: an all-day reminder lands on today, a timed one on its next future time.
        assertEquals(today, next(today.minusDays(5), null, daily))
        assertEquals(today.plusDays(1), next(today.minusDays(5), 8 * 60, daily))
        assertEquals(today, next(today.minusDays(5), 17 * 60, daily))
        assertEquals(today.plusDays(4), next(today.minusDays(10), null, ScheduleRepeat(1, ScheduleRepeatUnit.Week)))
    }

    @Test
    fun savesAndReadsTheRepeatRule() {
        val reminders =
            listOf(
                ReminderInfo(1L, "Bins", today, 8 * 60, repeat = ScheduleRepeat(2, ScheduleRepeatUnit.Week)),
                ReminderInfo(2L, "Once", today, null),
            )
        val restored = ReminderRepository.parseReminders(ReminderRepository.serializeReminders(reminders))
        assertEquals(ScheduleRepeat(2, ScheduleRepeatUnit.Week), restored[0].repeat)
        assertNull(restored[1].repeat)
    }

    @Test
    fun readsARepeatRuleFromTheTitle() {
        val schedule = ReminderNaturalLanguageParser.parse("Gym every Friday at 6pm", Instant.ofEpochMilli(now), zone)
        assertEquals("Gym", schedule?.title)
        assertEquals(ScheduleRepeat(1, ScheduleRepeatUnit.Week), schedule?.repeat)
        assertNull(ReminderNaturalLanguageParser.parse("Gym Friday at 6pm", Instant.ofEpochMilli(now), zone)?.repeat)
    }
}
