package com.tk.quicksearch.search.models

/** Calendar event row model used by search and rendering pipelines. */
data class CalendarEventInfo(
    val eventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
)
