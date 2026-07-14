package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.core.SearchSection

internal fun shouldRenderStandaloneTodayCalendarSection(
    section: SearchSection,
    todayCalendarEventsCount: Int,
): Boolean = section == SearchSection.CALENDAR && todayCalendarEventsCount > 0
