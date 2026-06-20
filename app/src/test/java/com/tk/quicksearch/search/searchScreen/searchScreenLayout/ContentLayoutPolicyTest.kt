package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.core.SearchSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentLayoutPolicyTest {
    @Test
    fun calendarSlotRendersStandaloneTodayEvents() {
        assertTrue(
            shouldRenderStandaloneTodayCalendarSection(
                section = SearchSection.CALENDAR,
                todayCalendarEventsCount = 1,
            ),
        )
    }

    @Test
    fun calendarSlotDoesNotRenderWithoutTodayEvents() {
        assertFalse(
            shouldRenderStandaloneTodayCalendarSection(
                section = SearchSection.CALENDAR,
                todayCalendarEventsCount = 0,
            ),
        )
    }

    @Test
    fun anotherSectionDoesNotRenderTodayCalendarEvents() {
        assertFalse(
            shouldRenderStandaloneTodayCalendarSection(
                section = SearchSection.APPS,
                todayCalendarEventsCount = 1,
            ),
        )
    }
}
