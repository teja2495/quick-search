package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

private data class CustomCalendarEvent(
    val eventId: Long,
    val title: String,
    val dateTimeMillis: Long,
    val allDay: Boolean,
)

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val MILLIS_PER_HOUR = 60L * 60L * 1000L

class CustomCalendarEventRepository(context: Context) {

    private val calendarPreferences = CalendarPreferences(context)

    fun getAllCustomEvents(): List<CalendarEventInfo> =
        readCustomEvents().map { it.toCalendarEventInfo() }

    fun searchCustomEvents(
        query: String,
        includePastEvents: Boolean,
    ): List<CalendarEventInfo> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank()) return emptyList()

        val todayStartMillis = startOfTodayMillis()
        return readCustomEvents()
            .asSequence()
            .filter { event -> includePastEvents || event.dateTimeMillis >= todayStartMillis }
            .mapNotNull { event ->
                val priority = SearchRankingUtils.calculateMatchPriority(event.title, normalizedQuery)
                if (SearchRankingUtils.isOtherMatch(priority)) null else event to priority
            }
            .sortedWith(compareBy { it.second })
            .map { it.first.toCalendarEventInfo() }
            .toList()
    }

    fun createCustomEvent(title: String, dateTimeMillis: Long, allDay: Boolean): CalendarEventInfo {
        val event = CustomCalendarEvent(
            eventId = calendarPreferences.nextCustomEventId(),
            title = title.trim(),
            dateTimeMillis = dateTimeMillis,
            allDay = allDay,
        )
        val events = readCustomEvents().toMutableList().apply { add(event) }
        writeCustomEvents(events)
        return event.toCalendarEventInfo()
    }

    fun updateCustomEvent(eventId: Long, title: String, dateTimeMillis: Long, allDay: Boolean): CalendarEventInfo? {
        val events = readCustomEvents().toMutableList()
        val index = events.indexOfFirst { it.eventId == eventId }
        if (index == -1) return null
        val updated = events[index].copy(title = title.trim(), dateTimeMillis = dateTimeMillis, allDay = allDay)
        events[index] = updated
        writeCustomEvents(events)
        return updated.toCalendarEventInfo()
    }

    fun deleteCustomEvent(eventId: Long) {
        val events = readCustomEvents().filterNot { it.eventId == eventId }
        writeCustomEvents(events)
    }

    fun getCustomEventById(eventId: Long): CalendarEventInfo? =
        readCustomEvents().firstOrNull { it.eventId == eventId }?.toCalendarEventInfo()

    private fun readCustomEvents(): List<CustomCalendarEvent> {
        val raw = calendarPreferences.getCustomEventsJson()
        if (raw.isBlank()) return emptyList()
        val jsonArray = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val events = mutableListOf<CustomCalendarEvent>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val eventId = obj.optLong(FIELD_EVENT_ID, 0L)
            if (eventId == 0L) continue
            val title = obj.optString(FIELD_TITLE).orEmpty()
            val dateTimeMillis = obj.optLong(FIELD_DATE_TIME_MILLIS, 0L)
            val allDay = obj.optBoolean(FIELD_ALL_DAY, true)
            if (title.isNotBlank() && dateTimeMillis > 0L) {
                events.add(CustomCalendarEvent(eventId = eventId, title = title, dateTimeMillis = dateTimeMillis, allDay = allDay))
            }
        }
        return events
    }

    private fun writeCustomEvents(events: List<CustomCalendarEvent>) {
        val jsonArray = JSONArray()
        events.forEach { event ->
            jsonArray.put(
                JSONObject()
                    .put(FIELD_EVENT_ID, event.eventId)
                    .put(FIELD_TITLE, event.title)
                    .put(FIELD_DATE_TIME_MILLIS, event.dateTimeMillis)
                    .put(FIELD_ALL_DAY, event.allDay),
            )
        }
        calendarPreferences.setCustomEventsJson(jsonArray.toString())
    }

    private fun startOfTodayMillis(): Long {
        val zoneId = ZoneId.systemDefault()
        return Instant.now().atZone(zoneId).toLocalDate()
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun CustomCalendarEvent.toCalendarEventInfo() = CalendarEventInfo(
        eventId = eventId,
        title = title,
        startMillis = dateTimeMillis,
        endMillis = if (allDay) dateTimeMillis + MILLIS_PER_DAY else dateTimeMillis + MILLIS_PER_HOUR,
        allDay = allDay,
        recurrenceRule = null,
    )

    companion object {
        private const val FIELD_EVENT_ID = "eventId"
        private const val FIELD_TITLE = "title"
        private const val FIELD_DATE_TIME_MILLIS = "dateTimeMillis"
        private const val FIELD_ALL_DAY = "allDay"
    }
}
