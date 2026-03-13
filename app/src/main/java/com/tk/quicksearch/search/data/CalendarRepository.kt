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
import java.util.Locale

class CalendarRepository(
    private val context: Context,
) {
    private val contentResolver = context.contentResolver

    companion object {
        private val EVENT_PROJECTION =
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY,
            )

        private const val MAX_EVENT_SEARCH_CANDIDATES = 500
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun searchFutureEventsByTitle(
        query: String,
        limit: Int,
    ): List<CalendarEventInfo> {
        if (query.isBlank() || limit <= 0 || !hasPermission()) return emptyList()

        val now = System.currentTimeMillis()
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank()) return emptyList()

        val candidates = queryFutureEvents(now = now, limit = MAX_EVENT_SEARCH_CANDIDATES)

        return candidates
            .asSequence()
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
                    .thenBy { it.first.startMillis }
                    .thenBy { it.first.title.lowercase(Locale.getDefault()) },
            )
            .map { it.first }
            .take(limit)
            .toList()
    }

    fun getEventsByIds(ids: Set<Long>): List<CalendarEventInfo> {
        if (ids.isEmpty() || !hasPermission()) return emptyList()

        val selection =
            buildString {
                append(CalendarContract.Events._ID)
                append(" IN (")
                append(ids.joinToString(","))
                append(")")
            }

        val rows = mutableListOf<CalendarEventInfo>()
        contentResolver
            .query(
                CalendarContract.Events.CONTENT_URI,
                EVENT_PROJECTION,
                selection,
                null,
                "${CalendarContract.Events.DTSTART} ASC",
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex)?.trim().orEmpty()
                    val startMillis = cursor.getLong(startIndex)
                    val endMillis = cursor.getLong(endIndex)
                    val allDay = cursor.getInt(allDayIndex) == 1

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

        return rows
    }

    fun createViewEventIntent(eventId: Long): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun queryFutureEvents(
        now: Long,
        limit: Int,
    ): List<CalendarEventInfo> {
        val uri: Uri = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
            .appendPath(now.toString())
            .appendPath((now + 1000L * 60L * 60L * 24L * 365L * 2L).toString())
            .build()

        val projection =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )

        val rows = mutableListOf<CalendarEventInfo>()
        contentResolver
            .query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext() && rows.size < limit) {
                    val id = cursor.getLong(idIndex)
                    val title = cursor.getString(titleIndex)?.trim().orEmpty()
                    val startMillis = cursor.getLong(beginIndex)
                    val endMillis = cursor.getLong(endIndex)
                    val allDay = cursor.getInt(allDayIndex) == 1

                    if (title.isNotBlank() && startMillis >= now) {
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

        return rows
    }
}
