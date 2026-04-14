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

    fun getShowTodayEvents(): Boolean = getBooleanPref(BasePreferences.KEY_CALENDAR_SHOW_TODAY_EVENTS, defaultValue = true)

    fun setShowTodayEvents(value: Boolean) = setBooleanPref(BasePreferences.KEY_CALENDAR_SHOW_TODAY_EVENTS, value)

    fun getArchivedTodayEventIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_ARCHIVED_TODAY_CALENDAR_EVENT_IDS)

    fun archiveTodayEvent(eventId: Long): Set<Long> = pinLongItem(BasePreferences.KEY_ARCHIVED_TODAY_CALENDAR_EVENT_IDS, eventId)

    fun getCustomEventsJson(): String = prefs.getString(BasePreferences.KEY_CUSTOM_CALENDAR_EVENTS_DATA, null).orEmpty()

    fun setCustomEventsJson(json: String) {
        prefs.edit().putString(BasePreferences.KEY_CUSTOM_CALENDAR_EVENTS_DATA, json).apply()
    }

    fun nextCustomEventId(): Long {
        val counter = prefs.getLong(BasePreferences.KEY_CUSTOM_CALENDAR_EVENT_ID_COUNTER, 1L)
        prefs.edit().putLong(BasePreferences.KEY_CUSTOM_CALENDAR_EVENT_ID_COUNTER, counter + 1L).apply()
        return -counter
    }
}
