package com.tk.quicksearch.tools.aiSearch

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomToolPromptUtilsTest {
    @Test
    fun expandsTimePlaceholderWithDateTimeAndTimezone() {
        val now = ZonedDateTime.of(2026, 7, 30, 9, 45, 12, 0, ZoneId.of("America/New_York"))

        assertEquals(
            "Current time: 2026-07-30 09:45:12 EDT (America/New_York)",
            expandCustomToolPrompt("Current time: {time}", now),
        )
    }
}
