package com.tk.quicksearch.search.data.preferences

import android.content.Context

/** Identifies one occurrence of a calendar event, so a repeating event's other days are separate. */
internal fun glanceOccurrenceKey(eventId: Long, startMillis: Long): String = "$eventId:$startMillis"

/** Preferences for calendar event pin/exclude management. */
class CalendarPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getPinnedEventIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS)

    fun getPinnedEventOrder(): List<Long> =
        getStringListPref(BasePreferences.KEY_PINNED_CALENDAR_EVENT_ORDER).mapNotNull { it.toLongOrNull() }

    fun setPinnedEventOrder(order: List<Long>): List<Long> =
        order.distinct().also {
            setStringListPref(BasePreferences.KEY_PINNED_CALENDAR_EVENT_ORDER, it.map(Long::toString))
        }

    fun getExcludedEventIds(): Set<Long> = getExcludedLongItems(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS)

    fun pinEvent(eventId: Long): Set<Long> =
        pinLongItem(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS, eventId).also {
            if (eventId !in getPinnedEventOrder()) {
                setPinnedEventOrder(getPinnedEventOrder() + eventId)
            }
        }

    fun unpinEvent(eventId: Long): Set<Long> =
        unpinLongItem(BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS, eventId).also {
            setPinnedEventOrder(getPinnedEventOrder().filterNot { it == eventId })
        }

    fun excludeEvent(eventId: Long): Set<Long> = excludeLongItem(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS, eventId)

    fun removeExcludedEvent(eventId: Long): Set<Long> = removeExcludedLongItem(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS, eventId)

    fun clearAllExcludedEvents(): Set<Long> = clearAllExcludedLongItems(BasePreferences.KEY_EXCLUDED_CALENDAR_EVENT_IDS)

    fun getIncludePastEvents(): Boolean = getBooleanPref(BasePreferences.KEY_CALENDAR_INCLUDE_PAST_EVENTS, defaultValue = false)

    fun setIncludePastEvents(value: Boolean) = setBooleanPref(BasePreferences.KEY_CALENDAR_INCLUDE_PAST_EVENTS, value)

    fun getShowTodayEvents(): Boolean = getBooleanPref(BasePreferences.KEY_CALENDAR_SHOW_TODAY_EVENTS, defaultValue = true)

    fun setShowTodayEvents(value: Boolean) = setBooleanPref(BasePreferences.KEY_CALENDAR_SHOW_TODAY_EVENTS, value)

    /** Null means to let Android use the system default calendar handler. */
    fun getDefaultCalendarPackage(): String? =
        prefs.getString(BasePreferences.KEY_DEFAULT_CALENDAR_PACKAGE, null)?.takeIf(String::isNotBlank)

    fun setDefaultCalendarPackage(packageName: String?) {
        prefs.edit().putString(BasePreferences.KEY_DEFAULT_CALENDAR_PACKAGE, packageName).apply()
    }

    /**
     * At a Glance hides, keyed by [glanceOccurrenceKey] and mapped to when each shows again. Keyed per
     * occurrence, so hiding today's instance of a repeating event leaves its other days alone. Hides
     * that have ended are dropped on the next write.
     */
    fun getGlanceHiddenEvents(): Map<String, Long> =
        getStringSet(BasePreferences.KEY_GLANCE_HIDDEN_CALENDAR_EVENTS).mapNotNull { entry ->
            val key = entry.substringBeforeLast('=')
            val until = entry.substringAfterLast('=').toLongOrNull() ?: return@mapNotNull null
            key to until
        }.toMap()

    fun hideGlanceEvent(
        occurrenceKey: String,
        untilMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val entries = getGlanceHiddenEvents().filter { (key, until) -> key != occurrenceKey && until > nowMillis } +
            (occurrenceKey to untilMillis)
        prefs.edit()
            .putStringSet(BasePreferences.KEY_GLANCE_HIDDEN_CALENDAR_EVENTS, entries.map { (key, until) -> "$key=$until" }.toSet())
            .apply()
    }

    /** Calendar events created inside Quick Search before reminders replaced them; read only to migrate them. */
    fun getCustomEventsJson(): String = prefs.getString(BasePreferences.KEY_CUSTOM_CALENDAR_EVENTS_DATA, null).orEmpty()

    fun setCustomEventsJson(json: String) {
        prefs.edit().putString(BasePreferences.KEY_CUSTOM_CALENDAR_EVENTS_DATA, json).apply()
    }
}
