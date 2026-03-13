package com.tk.quicksearch.search.calendar

import com.tk.quicksearch.search.core.CalendarEventManagementConfig
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import kotlinx.coroutines.CoroutineScope

class CalendarManagementHandler(
    userPreferences: UserAppPreferences,
    scope: CoroutineScope,
    onStateChanged: () -> Unit,
    onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit,
) : ManagementHandler<CalendarEventInfo> by GenericManagementHandler(
    config = CalendarEventManagementConfig(),
    userPreferences = userPreferences,
    scope = scope,
    onStateChanged = onStateChanged,
    onUiStateUpdate = onUiStateUpdate,
)
