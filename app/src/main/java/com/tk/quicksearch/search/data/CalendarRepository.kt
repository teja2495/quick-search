package com.tk.quicksearch.search.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class CalendarRepository(
    private val context: Context,
) {
    private val contentResolver = context.contentResolver

    companion object {
        private val INSTANCE_PROJECTION =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )
        private val EVENT_RECURRENCE_PROJECTION =
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.RRULE,
            )

        private const val MAX_EVENT_SEARCH_CANDIDATES = 500
        private const val PAST_WINDOW_YEARS = 2L
        private const val FUTURE_WINDOW_YEARS = 2L
        private const val MILLIS_PER_YEAR = 1000L * 60L * 60L * 24L * 365L
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun searchFutureEventsByTitle(
        query: String,
        limit: Int,
        includePastEvents: Boolean = false,
    ): List<CalendarEventInfo> {
        if (query.isBlank() || limit <= 0 || !hasPermission()) return emptyList()

        val now = System.currentTimeMillis()
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank()) return emptyList()

        val candidates = queryEventsAroundNow(now = now, limit = MAX_EVENT_SEARCH_CANDIDATES)
        val todayStartMillis = startOfTodayMillis()

        return candidates
            .asSequence()
            .filter { event -> includePastEvents || event.startMillis >= todayStartMillis }
            .mapNotNull { event ->
                val priority = SearchRankingUtils.calculateMatchPriority(event.title, normalizedQuery)
                if (SearchRankingUtils.isOtherMatch(priority)) {
                    null
                } else {
                    event to priority
                }
            }
            .sortedWith(
                compareBy<Pair<CalendarEventInfo, Int>> { it.second }
                    .thenBy { calendarFutureFirstGroup(it.first.startMillis, now) }
                    .thenBy { calendarFutureFirstOrderKey(it.first.startMillis, now) }
                    .thenBy { it.first.title.lowercase(Locale.getDefault()) },
            )
            .map { it.first }
            .take(limit)
            .toList()
    }

    private fun startOfTodayMillis(): Long =
        java.util.Calendar.getInstance()
            .apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

    fun getEventsByIds(ids: Set<Long>): List<CalendarEventInfo> {
        if (ids.isEmpty() || !hasPermission()) return emptyList()
        val now = System.currentTimeMillis()
        return queryEventsAroundNow(now = now, limit = ids.size, eventIds = ids)
    }

    fun getFutureEvents(limit: Int = MAX_EVENT_SEARCH_CANDIDATES): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        return queryEventsAroundNow(now = System.currentTimeMillis(), limit = limit)
    }

    fun getUpcomingEventsSortedAscending(limit: Int): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        val now = System.currentTimeMillis()
        val windowEnd = now + (MILLIS_PER_YEAR * FUTURE_WINDOW_YEARS)
        val rows = queryInstancesInWindow(now, windowEnd, limit * 4, eventIds = null)
        if (rows.isEmpty()) return emptyList()
        return rows
            .filter { it.startMillis >= now }
            .groupBy { it.eventId }
            .mapValues { (_, instances) -> instances.minByOrNull { it.startMillis }!! }
            .values
            .sortedBy { it.startMillis }
            .take(limit)
    }

    fun getTodayEvents(limit: Int = 50): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24L * 60 * 60 * 1000 - 1
        val now = System.currentTimeMillis()
        val cutoffMillis = now - 15L * 60 * 1000
        return queryInstancesInWindow(startOfDay, endOfDay, limit, null)
            .filter { event ->
                // Exclude events that don't actually start today — the instance query can return
                // all-day events from tomorrow when their UTC midnight falls inside the window.
                event.startMillis in startOfDay..endOfDay &&
                    // All-day events are valid the whole day; timed events are hidden once they are
                    // more than 15 minutes past their start time.
                    (event.allDay || event.startMillis >= cutoffMillis)
            }
            .sortedBy { it.startMillis }
    }

    fun getEventInstancesAroundNow(limit: Int = 2000): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        return queryEventInstancesAroundNow(now = System.currentTimeMillis(), limit = limit)
    }

    fun getEventRecurrenceRules(eventIds: Set<Long>): Map<Long, String?> {
        if (eventIds.isEmpty() || !hasPermission()) return emptyMap()

        val selection =
            buildString {
                append(CalendarContract.Events._ID)
                append(" IN (")
                append(eventIds.joinToString(","))
                append(")")
            }

        val recurrenceByEventId = mutableMapOf<Long, String?>()
        contentResolver
            .query(
                CalendarContract.Events.CONTENT_URI,
                EVENT_RECURRENCE_PROJECTION,
                selection,
                null,
                null,
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val rruleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)

                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(idIndex)
                    val recurrenceRule = cursor.getString(rruleIndex)?.trim().orEmpty()
                    recurrenceByEventId[eventId] = recurrenceRule.ifBlank { null }
                }
            }

        return recurrenceByEventId
    }

    fun createViewEventIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun queryEventsAroundNow(
        now: Long,
        limit: Int,
        eventIds: Set<Long>? = null,
    ): List<CalendarEventInfo> {
        if (limit <= 0) return emptyList()

        val windowStart = now - (MILLIS_PER_YEAR * PAST_WINDOW_YEARS)
        val windowEnd = now + (MILLIS_PER_YEAR * FUTURE_WINDOW_YEARS)
        val rows = queryInstancesInWindow(windowStart, windowEnd, limit * 8, eventIds)
        if (rows.isEmpty()) return emptyList()

        val nearestInstances = selectNearestInstancePerEvent(rows, now)
        return nearestInstances.values
            .sortedWith(
                compareBy<CalendarEventInfo> { eventDistanceToNow(it.startMillis, now) }
                    .thenByDescending { it.startMillis >= now }
                    .thenBy { it.startMillis },
            )
            .take(limit)
    }

    private fun queryEventInstancesAroundNow(
        now: Long,
        limit: Int,
    ): List<CalendarEventInfo> {
        val windowStart = now - (MILLIS_PER_YEAR * PAST_WINDOW_YEARS)
        val windowEnd = now + (MILLIS_PER_YEAR * FUTURE_WINDOW_YEARS)
        return queryInstancesInWindow(windowStart, windowEnd, limit, eventIds = null)
            .sortedBy { it.startMillis }
    }

    private fun queryInstancesInWindow(
        windowStart: Long,
        windowEnd: Long,
        limit: Int,
        eventIds: Set<Long>?,
    ): List<CalendarEventInfo> {
        val uri: Uri = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
            .appendPath(windowStart.toString())
            .appendPath(windowEnd.toString())
            .build()

        val selection =
            if (eventIds.isNullOrEmpty()) {
                null
            } else {
                buildString {
                    append(CalendarContract.Instances.EVENT_ID)
                    append(" IN (")
                    append(eventIds.joinToString(","))
                    append(")")
                }
            }

        val rows = mutableListOf<CalendarEventInfo>()
        contentResolver
            .query(
                uri,
                INSTANCE_PROJECTION,
                selection,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val rawLimit = if (eventIds.isNullOrEmpty()) limit else Int.MAX_VALUE

                while (cursor.moveToNext() && rows.size < rawLimit) {
                    val id = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex)?.trim().orEmpty()
                    val rawStartMillis = cursor.getLong(beginIndex)
                    val rawEndMillis = cursor.getLong(endIndex)
                    val allDay = cursor.getInt(allDayIndex) == 1
                    // Android Calendar stores all-day event timestamps as UTC midnight.
                    // Normalize to local midnight so date formatting and filtering work correctly
                    // regardless of the device timezone.
                    val startMillis = if (allDay) utcMidnightToLocalMidnight(rawStartMillis) else rawStartMillis
                    val endMillis = if (allDay) utcMidnightToLocalMidnight(rawEndMillis) else rawEndMillis

                    if (title.isNotBlank()) {
                        rows.add(
                            CalendarEventInfo(
                                eventId = id,
                                title = title,
                                startMillis = startMillis,
                                endMillis = endMillis,
                                allDay = allDay,
                            ),
                        )
                    }
                }
            }

        if (rows.isEmpty()) return emptyList()

        val recurrenceByEventId = getEventRecurrenceRules(rows.map { it.eventId }.toSet())
        return rows.map { event -> event.copy(recurrenceRule = recurrenceByEventId[event.eventId]) }
    }

    private fun selectNearestInstancePerEvent(
        instances: List<CalendarEventInfo>,
        now: Long,
    ): Map<Long, CalendarEventInfo> =
        instances.groupBy { it.eventId }
            .mapValues { (_, candidates) ->
                val nearestFuture = candidates.filter { it.startMillis >= now }.minByOrNull { it.startMillis }
                nearestFuture ?: candidates.maxByOrNull { it.startMillis } ?: candidates.first()
            }

    private fun eventDistanceToNow(
        startMillis: Long,
        now: Long,
    ): Long = if (startMillis >= now) startMillis - now else now - startMillis

    private fun utcMidnightToLocalMidnight(utcMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun calendarFutureFirstGroup(
        startMillis: Long,
        now: Long,
    ): Int = if (startMillis >= now) 0 else 1

    private fun calendarFutureFirstOrderKey(
        startMillis: Long,
        now: Long,
    ): Long = if (startMillis >= now) startMillis else Long.MAX_VALUE - startMillis
}
