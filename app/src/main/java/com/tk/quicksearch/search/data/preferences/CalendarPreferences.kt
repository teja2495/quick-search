package com.tk.quicksearch.search.data.preferences

import android.content.Context

/** Preferences for calendar event pin/exclude management. */
class CalendarPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getPinnedEventIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS)

    fun getExcludedEventIds(): Set<Long> = getExcludedLongItems(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS)

    fun pinEvent(eventId: Long): Set<Long> = pinLongItem(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS, eventId)

    fun unpinEvent(eventId: Long): Set<Long> = unpinLongItem(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS, eventId)

    fun excludeEvent(eventId: Long): Set<Long> = excludeLongItem(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS, eventId)

    fun removeExcludedEvent(eventId: Long): Set<Long> = removeExcludedLongItem(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS, eventId)

    fun clearAllExcludedEvents(): Set<Long> = clearAllExcludedLongItems(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS)

    fun getIncludePastEvents(): Boolean = getBooleanPref(BasePreferences.KEY_CALENDAR_INCLUDE_PAST_EVENTS, defaultValue = false)

    fun setIncludePastEvents(value: Boolean) = setBooleanPref(BasePreferences.KEY_CALENDAR_INCLUDE_PAST_EVENTS, value)
}
