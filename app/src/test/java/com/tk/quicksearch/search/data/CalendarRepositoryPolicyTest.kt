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
                now = startOfTomorrow - 1,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun timedEventRemainsVisibleThroughHideDelay() {
        val eventStart = startOfDay + 60L * 60L * 1000L
        val event = event(startMillis = eventStart, allDay = false)

        assertTrue(
            shouldShowTodayEvent(
                event = event,
                now = eventStart + CalendarRepository.TIMED_EVENT_HIDE_DELAY_MILLIS,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    @Test
    fun timedEventIsHiddenAfterHideDelay() {
        val eventStart = startOfDay + 60L * 60L * 1000L
        val event = event(startMillis = eventStart, allDay = false)

        assertFalse(
            shouldShowTodayEvent(
                event = event,
                now = eventStart + CalendarRepository.TIMED_EVENT_HIDE_DELAY_MILLIS + 1,
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
                now = startOfDay,
                startOfDay = startOfDay,
                startOfTomorrow = startOfTomorrow,
            ),
        )
    }

    private fun event(
        startMillis: Long,
        allDay: Boolean,
    ) = CalendarEventInfo(
        eventId = 1L,
        title = "Test",
        startMillis = startMillis,
        endMillis = startMillis + 1L,
        allDay = allDay,
    )
}
