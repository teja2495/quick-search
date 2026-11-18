package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.models.CalendarEventInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarInstanceSelectionTest {
    private val now = 10_000_000L

    @Test
    fun nearestInstanceIsTheNextUpcomingOne() {
        val instances = listOf(instance(1, now - 500), instance(1, now + 300), instance(1, now + 100))

        val selected = selectNearestInstancePerEvent(mapOf(1L to instances), now)

        assertEquals(now + 100, selected.getValue(1).startMillis)
    }

    @Test
    fun nearestInstanceFallsBackToTheLatestPastOne() {
        val instances = listOf(instance(1, now - 500), instance(1, now - 100))

        val selected = selectNearestInstancePerEvent(mapOf(1L to instances), now)

        assertEquals(now - 100, selected.getValue(1).startMillis)
    }

    @Test
    fun nearestInstanceSkipsEventsWithoutInstances() {
        val selected = selectNearestInstancePerEvent(mapOf(1L to emptyList()), now)

        assertTrue(selected.isEmpty())
    }

    @Test
    fun resolvedInstancesKeepOnlyEventsWithAnUpcomingInstanceBeforeTheLimit() {
        val instances =
            mapOf(
                1L to listOf(instance(1, now - 50), instance(1, now + 200), instance(1, now + 100)),
                2L to listOf(instance(2, now - 50)),
                3L to listOf(instance(3, now + 5_000)),
            )

        val resolved = resolvedUpcomingInstances(instances, anchorMillis = now, resolvedBeforeMillis = now + 1_000)

        assertEquals(setOf(1L), resolved.keys)
        assertEquals(now + 100, resolved.getValue(1).startMillis)
    }

    private fun instance(
        eventId: Long,
        startMillis: Long,
    ) = CalendarEventInfo(
        eventId = eventId,
        title = "Event $eventId",
        startMillis = startMillis,
        endMillis = startMillis + 60_000L,
        allDay = false,
    )
}
