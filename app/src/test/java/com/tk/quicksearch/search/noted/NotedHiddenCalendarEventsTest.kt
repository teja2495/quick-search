package com.tk.quicksearch.search.noted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotedHiddenCalendarEventsTest {
    @Test
    fun normalizeMatchesNoted() {
        assertEquals("team stand up", NotedHiddenCalendarEvents.normalize("  Team Stand-up! "))
        assertEquals("room 2", NotedHiddenCalendarEvents.normalize("Room #2"))
        assertEquals("", NotedHiddenCalendarEvents.normalize(" -- "))
    }

    @Test
    fun hidesByNormalizedTitleOnly() {
        val names = setOf("team stand up")
        assertTrue(NotedHiddenCalendarEvents.isHidden("TEAM stand up", names))
        assertTrue(NotedHiddenCalendarEvents.isHidden("Team Stand-Up", names))
        assertFalse(NotedHiddenCalendarEvents.isHidden("Team stand up notes", names))
        assertFalse(NotedHiddenCalendarEvents.isHidden("Team stand up", emptySet()))
    }
}
