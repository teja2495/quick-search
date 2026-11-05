package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.models.ReminderInfo
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlanceReminderTimingTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 9, 18)
    private val tomorrow = today.plusDays(1)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Long =
        day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun reminder(day: LocalDate, hour: Int? = null, minute: Int = 0) =
        ReminderInfo(reminderId = 1L, title = "Test", date = day, timeMinutes = hour?.let { it * 60 + minute })

    @Test
    fun undatedReminderShowsFromFiveAm() {
        assertEquals(at(today, 5), ReminderRepository.glanceVisibleFromMillis(reminder(today)))
    }

    @Test
    fun timedReminderShowsNinetyMinutesEarly() {
        assertEquals(at(today, 13, 30), ReminderRepository.glanceVisibleFromMillis(reminder(today, 15)))
    }

    @Test
    fun dismissingMoreThanThirtyMinutesEarlyHidesUntilDue() {
        val reminder = reminder(today, 15)

        assertTrue(ReminderRepository.canDismissUntilDue(reminder, at(today, 14, 29)))
        assertFalse(ReminderRepository.canDismissUntilDue(reminder, at(today, 14, 30)))
        assertFalse(ReminderRepository.canDismissUntilDue(reminder, at(today, 16)))
    }

    @Test
    fun tomorrowsUndatedAndMorningRemindersShowTheEveningBefore() {
        val evening = at(today, 21, 30)

        assertTrue(ReminderRepository.isTomorrowEveningItem(reminder(tomorrow), evening))
        assertTrue(ReminderRepository.isTomorrowEveningItem(reminder(tomorrow, 8), evening))
        assertFalse(ReminderRepository.isTomorrowEveningItem(reminder(tomorrow, 14), evening))
        assertFalse(ReminderRepository.isTomorrowEveningItem(reminder(today), evening))
    }

    @Test
    fun eveningStartsAtNinePm() {
        assertFalse(GlanceTiming.isEvening(at(today, 20, 59)))
        assertTrue(GlanceTiming.isEvening(at(today, 21)))
    }
}
