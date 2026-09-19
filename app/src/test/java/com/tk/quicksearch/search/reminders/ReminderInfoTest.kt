package com.tk.quicksearch.search.reminders

import com.tk.quicksearch.search.models.ReminderInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderInfoTest {
    private val zone = ZoneId.of("America/New_York")
    private val date = LocalDate.of(2026, 9, 18)

    @Test
    fun reminderWithoutTimeIsDueAtNineAm() {
        val reminder = ReminderInfo(reminderId = 1L, title = "Pay rent", date = date, timeMinutes = null)

        assertFalse(reminder.hasTime)
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 9, 0).atZone(zone).toInstant().toEpochMilli(),
            reminder.dueMillis(zone),
        )
    }

    @Test
    fun reminderWithTimeIsDueAtThatWallClockTime() {
        val reminder = ReminderInfo(reminderId = 1L, title = "Call mom", date = date, timeMinutes = 17 * 60 + 45)

        assertTrue(reminder.hasTime)
        assertEquals(
            LocalDateTime.of(2026, 9, 18, 17, 45).atZone(zone).toInstant().toEpochMilli(),
            reminder.dueMillis(zone),
        )
    }

    @Test
    fun dueTimeFollowsTheZoneItIsResolvedIn() {
        val reminder = ReminderInfo(reminderId = 1L, title = "Standup", date = date, timeMinutes = 10 * 60)

        // Same wall-clock time in a zone 3 hours behind is 3 hours later in absolute time.
        assertEquals(
            3L * 60L * 60L * 1000L,
            reminder.dueMillis(ZoneId.of("America/Los_Angeles")) - reminder.dueMillis(zone),
        )
    }
}
