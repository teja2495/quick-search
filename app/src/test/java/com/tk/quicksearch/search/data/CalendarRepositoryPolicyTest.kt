package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.models.CalendarEventInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRepositoryPolicyTest {
    private val startOfDay = 1_000_000L
    private val startOfTomorrow = startOfDay + 24L * 60L * 60L * 1000L

    @Test
    fun allDayEventRemainsVisibleAfterItsStartTime() {
        val event = event(startMillis = startOfDay, allDay = true)

        assertTrue(
            shouldShowTodayEvent(
                event = event,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun timedEventRemainsAvailableForTheDay() {
        val eventStart = startOfDay + 60L * 60L * 1000L
        val event = event(startMillis = eventStart, allDay = false)

        assertTrue(
            shouldShowTodayEvent(
                event = event,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun timedEventFromTodayRemainsAvailableAfterItEnds() {
        val eventStart = startOfDay + 60L * 60L * 1000L
        val event = event(startMillis = eventStart, allDay = false)

        assertTrue(
            shouldShowTodayEvent(
                event = event,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun allDayEventFromAnotherDayIsNotShown() {
        val event = event(startMillis = startOfTomorrow, allDay = true)

        assertFalse(
            shouldShowTodayEvent(
                event = event,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun multiDayEventStillRunningTodayIsShown() {
        val event = event(startMillis = startOfDay - DAY, allDay = true, endMillis = startOfTomorrow + DAY)

        assertTrue(shouldShowTodayEvent(event, startOfDay, startOfTomorrow))
    }

    @Test
    fun eventRunningPastMidnightIsShownTheNextDay() {
        val event = event(startMillis = startOfDay - HOUR, allDay = false, endMillis = startOfDay + HOUR)

        assertTrue(shouldShowTodayEvent(event, startOfDay, startOfTomorrow))
    }

    @Test
    fun eventThatEndedAtMidnightIsNotShown() {
        val event = event(startMillis = startOfDay - DAY, allDay = true, endMillis = startOfDay)

        assertFalse(shouldShowTodayEvent(event, startOfDay, startOfTomorrow))
    }

    private fun event(
        startMillis: Long,
        allDay: Boolean,
        endMillis: Long = startMillis + 1L,
    ) = CalendarEventInfo(
        eventId = 1L,
        title = "Test",
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
    )

    private companion object {
        const val HOUR = 60L * 60L * 1000L
        const val DAY = 24L * HOUR
    }
}
