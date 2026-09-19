package com.tk.quicksearch.search.reminders

import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.ReminderRepository.Companion.LegacyCustomEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCalendarEventMigrationTest {
    private val zone = ZoneId.of("America/New_York")
    private val now = LocalDateTime.of(2026, 9, 18, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun millis(dateTime: LocalDateTime) = dateTime.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun timedEventKeepsItsWallClockTime() {
        val event = LegacyCustomEvent(-1L, " Dentist ", millis(LocalDateTime.of(2026, 9, 20, 14, 30)), allDay = false)

        val reminder = ReminderRepository.legacyCustomEventToReminder(event, 7L, now, zone)

        assertEquals(7L, reminder.reminderId)
        assertEquals("Dentist", reminder.title)
        assertEquals(LocalDate.of(2026, 9, 20), reminder.date)
        assertEquals(14 * 60 + 30, reminder.timeMinutes)
        assertFalse(reminder.isDone)
        assertFalse(reminder.isDismissedFromHome)
    }

    @Test
    fun allDayEventBecomesReminderWithoutTime() {
        val event = LegacyCustomEvent(-2L, "Birthday", millis(LocalDateTime.of(2026, 9, 25, 0, 0)), allDay = true)

        val reminder = ReminderRepository.legacyCustomEventToReminder(event, 1L, now, zone)

        assertEquals(LocalDate.of(2026, 9, 25), reminder.date)
        assertNull(reminder.timeMinutes)
        assertFalse(reminder.isDone)
    }

    @Test
    fun allDayEventTodayStaysActive() {
        val event = LegacyCustomEvent(-3L, "Today", millis(LocalDateTime.of(2026, 9, 18, 0, 0)), allDay = true)

        val reminder = ReminderRepository.legacyCustomEventToReminder(event, 1L, now, zone)

        assertFalse(reminder.isDone)
    }

    @Test
    fun pastEventsAreMarkedDoneAndKeptOffHome() {
        val pastTimed = LegacyCustomEvent(-4L, "Earlier", millis(LocalDateTime.of(2026, 9, 18, 8, 0)), allDay = false)
        val pastAllDay = LegacyCustomEvent(-5L, "Yesterday", millis(LocalDateTime.of(2026, 9, 17, 0, 0)), allDay = true)

        listOf(pastTimed, pastAllDay).forEach { event ->
            val reminder = ReminderRepository.legacyCustomEventToReminder(event, 1L, now, zone)
            assertTrue(reminder.isDone)
            assertTrue(reminder.isDismissedFromHome)
        }
    }

}
