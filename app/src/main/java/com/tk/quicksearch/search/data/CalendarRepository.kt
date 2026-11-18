package com.tk.quicksearch.search.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

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

        // Instances expose the event's RRULE, which saves a second Events query per read.
        private val INSTANCE_PROJECTION_WITH_RRULE = INSTANCE_PROJECTION + CalendarContract.Instances.RRULE
        private val EVENT_RECURRENCE_PROJECTION =
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.RRULE,
            )

        private const val MAX_EVENT_SEARCH_CANDIDATES = 500
        private const val PAST_WINDOW_YEARS = 2L
        private const val FUTURE_WINDOW_YEARS = 2L
        private const val MILLIS_PER_YEAR = 1000L * 60L * 60L * 24L * 365L

        // Without past events, search reads from a day before today: all-day instances are stored
        // at UTC midnight, which can fall before local midnight.
        private const val SEARCH_WINDOW_PAST_MARGIN_MILLIS = 24L * 60L * 60L * 1000L

        // getEventsByIds first reads this near window; only events without an upcoming instance in
        // it need the multi-year window.
        private const val NEAR_WINDOW_FUTURE_MILLIS = 31L * 24L * 60L * 60L * 1000L
        private const val NEAR_WINDOW_PAST_MILLIS = 24L * 60L * 60L * 1000L
        private const val NEAR_WINDOW_RESOLVED_MARGIN_MILLIS = 24L * 60L * 60L * 1000L

        private const val TAG = "CalendarRepository"
        private val WHITESPACE_REGEX = "\\s+".toRegex()

        private val isCalendarObserverRegistered = AtomicBoolean(false)

        @Volatile private var instancesRruleColumnSupported = true
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

        val todayStartMillis = startOfTodayMillis()
        val nearestInstanceAnchorMillis = if (includePastEvents) now else todayStartMillis
        // Candidates don't depend on the query, so typing reuses one provider read.
        val searchRows = getSearchRows(todayStartMillis, includePastEvents)
        val candidates =
            nearestEventsAroundNow(
                instancesByEvent = searchRows.instancesByEvent,
                now = now,
                limit = MAX_EVENT_SEARCH_CANDIDATES,
                nearestInstanceAnchorMillis = nearestInstanceAnchorMillis,
            )
        val queryTokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        val compactQuery = SearchTextNormalizer.compactForSearch(normalizedQuery)

        return candidates
            .asSequence()
            .filter { event -> includePastEvents || event.startMillis >= todayStartMillis }
            .mapNotNull { event ->
                val priority =
                    SearchRankingUtils.calculateMatchPriority(
                        text = searchRows.titleTexts.prepare(event.title),
                        normalizedQuery = normalizedQuery,
                        queryTokens = queryTokens,
                        compactQuery = compactQuery,
                    )
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

    private fun getSearchRows(
        todayStartMillis: Long,
        includePastEvents: Boolean,
    ): CalendarSearchRowsCache.Entry {
        ensureCalendarObserverRegistered()
        return CalendarSearchRowsCache.get(searchRowsKey(todayStartMillis, includePastEvents)) {
            val now = System.currentTimeMillis()
            // Read oldest first under a row cap, so past instances that search would drop must not
            // use up the cap before today's and upcoming ones.
            val windowStart =
                if (includePastEvents) {
                    now - (MILLIS_PER_YEAR * PAST_WINDOW_YEARS)
                } else {
                    todayStartMillis - SEARCH_WINDOW_PAST_MARGIN_MILLIS
                }
            queryInstancesInWindow(
                windowStart = windowStart,
                windowEnd = now + (MILLIS_PER_YEAR * FUTURE_WINDOW_YEARS),
                limit = MAX_EVENT_SEARCH_CANDIDATES * 8,
                eventIds = null,
            )
        }
    }

    private fun searchRowsKey(
        todayStartMillis: Long,
        includePastEvents: Boolean,
    ) = CalendarSearchRowsCache.Key(todayStartMillis, includePastEvents)

    private fun ensureCalendarObserverRegistered() {
        if (!isCalendarObserverRegistered.compareAndSet(false, true)) return
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    CalendarSearchRowsCache.markChanged()
                }
            }
        val registered =
            runCatching {
                context.applicationContext.contentResolver.registerContentObserver(
                    CalendarContract.CONTENT_URI,
                    true,
                    observer,
                )
            }.isSuccess
        // Without an observer the TTL alone bounds staleness; retry registration next search.
        if (!registered) isCalendarObserverRegistered.set(false)
        CalendarSearchRowsCache.isObserverRegistered = registered
    }

    private fun startOfTodayMillis(): Long =
        java.util.Calendar.getInstance()
            .apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

    /**
     * Each event's instance nearest now: its next one, or its latest when none is upcoming. Events
     * with an upcoming instance in the next month resolve from a short window; only the rest read
     * the multi-year window.
     */
    fun getEventsByIds(ids: Set<Long>): List<CalendarEventInfo> {
        if (ids.isEmpty() || !hasPermission()) return emptyList()
        val now = System.currentTimeMillis()
        val nearRows =
            queryInstancesInWindow(
                windowStart = now - NEAR_WINDOW_PAST_MILLIS,
                windowEnd = now + NEAR_WINDOW_FUTURE_MILLIS,
                limit = ids.size,
                eventIds = ids,
            )
        val resolved =
            resolvedUpcomingInstances(
                instancesByEvent = nearRows.groupBy { it.eventId },
                anchorMillis = now,
                resolvedBeforeMillis = now + NEAR_WINDOW_FUTURE_MILLIS - NEAR_WINDOW_RESOLVED_MARGIN_MILLIS,
            )
        val missingIds = ids - resolved.keys
        val farRows =
            if (missingIds.isEmpty()) {
                emptyList()
            } else {
                queryInstancesInWindow(
                    windowStart = now - (MILLIS_PER_YEAR * PAST_WINDOW_YEARS),
                    windowEnd = now + (MILLIS_PER_YEAR * FUTURE_WINDOW_YEARS),
                    limit = missingIds.size,
                    eventIds = missingIds,
                )
            }
        val selected = resolved.values + selectNearestInstancePerEvent(farRows.groupBy { it.eventId }, now).values
        return sortByDistanceToNow(selected, now).take(ids.size)
    }

    /**
     * [getEventsByIds] for search, which reuses the cached search rows when they hold an event's
     * upcoming instance and reads the provider only for the rest.
     */
    fun getEventsByIdsForSearch(
        ids: Set<Long>,
        includePastEvents: Boolean,
    ): List<CalendarEventInfo> {
        if (ids.isEmpty() || !hasPermission()) return emptyList()
        val now = System.currentTimeMillis()
        val cachedRows = CalendarSearchRowsCache.peek(searchRowsKey(startOfTodayMillis(), includePastEvents))
        // Cached rows are a prefix of the window in start order, so an event's upcoming instance
        // found there is its next one.
        val fromCache =
            cachedRows?.let { entry ->
                resolvedUpcomingInstances(
                    instancesByEvent = ids.mapNotNull { id -> entry.instancesByEvent[id]?.let { id to it } }.toMap(),
                    anchorMillis = now,
                    resolvedBeforeMillis = Long.MAX_VALUE,
                )
            }.orEmpty()
        val missingIds = ids - fromCache.keys
        return fromCache.values.toList() + getEventsByIds(missingIds)
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

    fun getTodayEvents(limit: Int = 50): List<CalendarEventInfo> = getEventsOn(LocalDate.now(), limit)


    /** Events on [day], all-day and timed, including ones that started earlier and run into it; earliest first. */
    fun getEventsOn(
        day: LocalDate,
        limit: Int = 50,
    ): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfTomorrow = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val utcStartOfDay = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val utcStartOfTomorrow = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        // CalendarContract stores all-day instances at UTC midnight. Query both the local-day
        // and UTC-day boundaries, then use the normalized local timestamps for final filtering.
        val queryStart = minOf(startOfDay, utcStartOfDay)
        val queryEnd = maxOf(startOfTomorrow, utcStartOfTomorrow) - 1
        return queryInstancesInWindow(queryStart, queryEnd, limit, null)
            .filter { event ->
                shouldShowTodayEvent(
                    event = event,
                    startOfDay = startOfDay,
                    startOfTomorrow = startOfTomorrow,
                )
            }
            .sortedBy { it.startMillis }
    }

    fun getEventInstancesAroundNow(limit: Int = 2000): List<CalendarEventInfo> {
        if (limit <= 0 || !hasPermission()) return emptyList()
        return queryEventInstancesAroundNow(now = System.currentTimeMillis(), limit = limit)
    }

    private fun getEventRecurrenceRules(eventIds: Set<Long>): Map<Long, String?> {
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

    private fun nearestEventsAroundNow(
        instancesByEvent: Map<Long, List<CalendarEventInfo>>,
        now: Long,
        limit: Int,
        nearestInstanceAnchorMillis: Long,
    ): List<CalendarEventInfo> {
        if (instancesByEvent.isEmpty()) return emptyList()

        val nearestInstances =
            selectNearestInstancePerEvent(
                instancesByEvent = instancesByEvent,
                anchorMillis = nearestInstanceAnchorMillis,
            )
        return sortByDistanceToNow(nearestInstances.values, now).take(limit)
    }

    private fun sortByDistanceToNow(
        events: Collection<CalendarEventInfo>,
        now: Long,
    ): List<CalendarEventInfo> =
        events.sortedWith(
            compareBy<CalendarEventInfo> { eventDistanceToNow(it.startMillis, now) }
                .thenByDescending { it.startMillis >= now }
                .thenBy { it.startMillis },
        )

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

        val rawLimit = if (eventIds.isNullOrEmpty()) limit else Int.MAX_VALUE
        if (instancesRruleColumnSupported) {
            val rows =
                try {
                    readInstances(uri, INSTANCE_PROJECTION_WITH_RRULE, selection, rawLimit)
                } catch (e: SecurityException) {
                    throw e
                } catch (e: RuntimeException) {
                    // A provider without the column: use the separate Events query from now on.
                    Log.w(TAG, "Instances query with RRULE failed; reading recurrence separately", e)
                    instancesRruleColumnSupported = false
                    null
                }
            if (rows != null) return rows
        }

        val rows = readInstances(uri, INSTANCE_PROJECTION, selection, rawLimit) ?: return emptyList()
        if (rows.isEmpty()) return emptyList()

        val recurrenceByEventId = getEventRecurrenceRules(rows.map { it.eventId }.toSet())
        return rows.map { event -> event.copy(recurrenceRule = recurrenceByEventId[event.eventId]) }
    }

    /**
     * Reads instance rows, with each event's recurrence rule when [projection] has the RRULE column.
     * Returns null when the projection asks for RRULE and the cursor lacks it.
     */
    private fun readInstances(
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        rawLimit: Int,
    ): List<CalendarEventInfo>? {
        val wantsRrule = CalendarContract.Instances.RRULE in projection
        val rows = mutableListOf<CalendarEventInfo>()
        // Instances of one event share their title and rule; keep one copy of each string.
        val sharedStrings = HashMap<String, String>()
        val cursor =
            contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            ) ?: return rows
        cursor.use {
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val rruleIndex = if (wantsRrule) cursor.getColumnIndex(CalendarContract.Instances.RRULE) else -1
            if (wantsRrule && rruleIndex < 0) {
                instancesRruleColumnSupported = false
                return null
            }

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
                    val recurrenceRule =
                        if (rruleIndex >= 0) {
                            cursor.getString(rruleIndex)?.trim()?.takeIf(String::isNotBlank)
                        } else {
                            null
                        }
                    rows.add(
                        CalendarEventInfo(
                            eventId = id,
                            title = sharedStrings.getOrPut(title) { title },
                            startMillis = startMillis,
                            endMillis = endMillis,
                            allDay = allDay,
                            recurrenceRule = recurrenceRule?.let { rule -> sharedStrings.getOrPut(rule) { rule } },
                        ),
                    )
                }
            }
        }
        return rows
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

/**
 * Whether [event] belongs to the day from [startOfDay] to [startOfTomorrow]: it starts that day, or
 * it started earlier and is still running then, like a multi-day trip or an event past midnight.
 */
internal fun shouldShowTodayEvent(
    event: CalendarEventInfo,
    startOfDay: Long,
    startOfTomorrow: Long,
): Boolean {
    if (event.startMillis >= startOfTomorrow) return false
    return event.startMillis >= startOfDay || event.endMillis > startOfDay
}

/** Each event's instance nearest [anchorMillis]: its first at or after it, else its latest. */
internal fun selectNearestInstancePerEvent(
    instancesByEvent: Map<Long, List<CalendarEventInfo>>,
    anchorMillis: Long,
): Map<Long, CalendarEventInfo> =
    instancesByEvent
        .filterValues { it.isNotEmpty() }
        .mapValues { (_, candidates) ->
            val nearestFuture =
                candidates.filter { it.startMillis >= anchorMillis }.minByOrNull { it.startMillis }
            nearestFuture ?: candidates.maxByOrNull { it.startMillis } ?: candidates.first()
        }

/**
 * Each event's first instance at or after [anchorMillis] that starts before [resolvedBeforeMillis];
 * events without one are left out, for the caller to read from a wider window.
 */
internal fun resolvedUpcomingInstances(
    instancesByEvent: Map<Long, List<CalendarEventInfo>>,
    anchorMillis: Long,
    resolvedBeforeMillis: Long,
): Map<Long, CalendarEventInfo> =
    buildMap {
        instancesByEvent.forEach { (eventId, instances) ->
            val next = instances.filter { it.startMillis >= anchorMillis }.minByOrNull { it.startMillis }
            if (next != null && next.startMillis < resolvedBeforeMillis) put(eventId, next)
        }
    }
