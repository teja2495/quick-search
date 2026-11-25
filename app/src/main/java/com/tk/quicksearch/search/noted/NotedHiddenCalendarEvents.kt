package com.tk.quicksearch.search.noted

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tk.quicksearch.search.data.CalendarSearchRowsCache

/**
 * Calendar event names hidden in Noted's Calendar, so Quick Search hides the same events. Noted
 * matches by normalized title only, so every event with a hidden name is left out, not just one
 * instance. Empty when the Noted integration is unavailable.
 *
 * Read once from Noted's provider and kept until Noted broadcasts a change
 * ([NotedTasksChangedReceiver]) or Noted's availability changes.
 */
object NotedHiddenCalendarEvents {
    @Volatile
    private var cachedNames: Set<String>? = null

    /** The hidden names, normalized with [normalize]. Blocking; call off the main thread. */
    fun namesBlocking(context: Context): Set<String> {
        cachedNames?.let { return it }
        val appContext = context.applicationContext
        if (!NotedIntegration.ensureChecked(appContext)) return emptySet()
        return load(appContext).also { cachedNames = it }
    }

    /** True when [title] is hidden; [names] comes from [namesBlocking]. */
    fun isHidden(title: String, names: Set<String>): Boolean = names.isNotEmpty() && normalize(title) in names

    /** Drops the cached names and makes calendar search reload, so the next read sees Noted's current set. */
    fun invalidate() {
        cachedNames = null
        CalendarSearchRowsCache.markChanged()
    }

    /** Mirrors Noted's `DaysTrackPreferences.normalizeHiddenCalendarEventName`; keep the two in sync. */
    internal fun normalize(title: String): String =
        title.trim()
            .lowercase()
            .split(NON_ALPHANUMERIC)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun load(context: Context): Set<String> {
        val uri =
            Uri.Builder()
                .scheme("content")
                .authority(NotedIntegration.AUTHORITY)
                .appendPath(NotedIntegration.HIDDEN_CALENDAR_EVENTS_PATH)
                .build()
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val titleIndex = cursor.getColumnIndex(NotedIntegration.COLUMN_TITLE)
                if (titleIndex < 0) return@use emptySet()
                buildSet {
                    while (cursor.moveToNext()) {
                        cursor.getString(titleIndex)?.let(::normalize)?.takeIf(String::isNotEmpty)?.let(::add)
                    }
                }
            }.orEmpty()
        } catch (e: SecurityException) {
            Log.w(TAG, "Noted hidden calendar events denied", e)
            emptySet()
        } catch (e: RuntimeException) {
            // An older Noted without this path, or Noted is being updated.
            Log.w(TAG, "Noted hidden calendar events failed", e)
            emptySet()
        }
    }

    private val NON_ALPHANUMERIC = Regex("[^\\p{Alnum}]+")
    private const val TAG = "NotedHiddenCalendar"
}
