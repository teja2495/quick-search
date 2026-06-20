package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.models.CalendarEventInfo
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVisibilityStateResolverTest {
    private val resolver = SearchVisibilityStateResolver()
    private val event =
        CalendarEventInfo(
            eventId = 1L,
            title = "Today",
            startMillis = 1L,
            endMillis = 2L,
            allDay = false,
        )

    @Test
    fun disabledSearchSectionStillShowsCalendarHomeContent() {
        val state =
            SearchUiState(
                query = "",
                hasCalendarPermission = true,
                disabledSections = setOf(SearchSection.CALENDAR),
                todayCalendarEvents = listOf(event),
            )

        val resolved = resolver.apply(state)

        assertTrue(resolved.calendarSectionState is CalendarSectionVisibility.ShowingResults)
    }

    @Test
    fun disabledSearchSectionRemainsHiddenForSearchQuery() {
        val state =
            SearchUiState(
                query = "today",
                hasCalendarPermission = true,
                disabledSections = setOf(SearchSection.CALENDAR),
                calendarEvents = listOf(event),
            )

        val resolved = resolver.apply(state)

        assertTrue(resolved.calendarSectionState is CalendarSectionVisibility.Hidden)
    }
}
