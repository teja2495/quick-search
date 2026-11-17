package com.tk.quicksearch.search.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.utils.SearchTextCache
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide cache of the calendar instance window that event search scores. Shared across
 * repository instances so a new search screen doesn't re-expand every recurring event, and loaded
 * by one caller at a time. After a calendar change the previous rows keep serving while a debounced
 * background reload runs, because syncs notify the provider many times in a row. Reloads after a
 * change wait until a search surface is visible, so syncs in the background cost nothing.
 */
internal object CalendarSearchRowsCache {
    private const val FRESH_MILLIS = 5L * 60L * 1000L

    // With the calendar observer registered and no change seen, rows stay current this long.
    private const val MAX_UNCHANGED_AGE_MILLIS = 60L * 60L * 1000L
    private const val CHANGE_RELOAD_DELAY_MILLIS = 3_000L

    /** The day and past-events setting the rows were loaded for; either change needs a new window. */
    data class Key(
        val todayStartMillis: Long,
        val includePastEvents: Boolean,
    )

    class Entry(
        val rows: List<CalendarEventInfo>,
        internal val fetchedAtElapsedMillis: Long,
        internal val changeGeneration: Long,
        val key: Key,
        internal val load: () -> List<CalendarEventInfo>,
    ) {
        /** [rows] grouped by event in row order, built once and reused by every query. */
        val instancesByEvent: Map<Long, List<CalendarEventInfo>> by lazy { rows.groupBy { it.eventId } }

        /** Normalized titles, so typing doesn't re-normalize every candidate per query. */
        val titleTexts = SearchTextCache()
    }

    private val changeGeneration = AtomicLong()
    private val loadLock = Any()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val reloadExecutor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "CalendarSearchRowsLoad").apply { isDaemon = true }
        }
    }
    private val reloadAfterChange =
        Runnable {
            val entry = cached ?: return@Runnable
            reloadExecutor.execute { loadLocked(entry.key, entry.load, reloadIfStale = true) }
        }

    @Volatile private var cached: Entry? = null

    /** Set once the calendar ContentObserver is registered; without it only [FRESH_MILLIS] bounds staleness. */
    @Volatile var isObserverRegistered: Boolean = false

    @Volatile private var isSurfaceVisible: Boolean = false

    fun get(
        key: Key,
        load: () -> List<CalendarEventInfo>,
    ): Entry {
        usableOrNull(key)?.let { entry ->
            if (entry.changeGeneration != changeGeneration.get()) scheduleReloadAfterChange()
            return entry
        }
        return loadLocked(key, load, reloadIfStale = false)
    }

    /** Usable rows for [key] without loading, or null. */
    fun peek(key: Key): Entry? = usableOrNull(key)

    fun markChanged() {
        changeGeneration.incrementAndGet()
        if (cached != null && isSurfaceVisible) scheduleReloadAfterChange()
    }

    /** Paired with the UI surfaces' start and stop; a change seen while hidden reloads on return. */
    fun onSurfaceVisibilityChanged(visible: Boolean) {
        isSurfaceVisible = visible
        if (!visible) {
            mainHandler.removeCallbacks(reloadAfterChange)
            return
        }
        val entry = cached ?: return
        if (entry.changeGeneration != changeGeneration.get()) scheduleReloadAfterChange()
    }

    fun release() {
        cached = null
        mainHandler.removeCallbacks(reloadAfterChange)
    }

    private fun loadLocked(
        key: Key,
        load: () -> List<CalendarEventInfo>,
        reloadIfStale: Boolean,
    ): Entry =
        synchronized(loadLock) {
            usableOrNull(key)?.let { entry ->
                val isCurrent = entry.changeGeneration == changeGeneration.get()
                if (isCurrent || !reloadIfStale) return entry
            }
            val generation = changeGeneration.get()
            val startElapsed = SystemClock.elapsedRealtime()
            val rows = load()
            SnapshotLoadLog.loaded("Calendar search rows", rows.size, startElapsed)
            Entry(rows, startElapsed, generation, key, load).also { cached = it }
        }

    private fun scheduleReloadAfterChange() {
        mainHandler.removeCallbacks(reloadAfterChange)
        mainHandler.postDelayed(reloadAfterChange, CHANGE_RELOAD_DELAY_MILLIS)
    }

    /**
     * Rows for [key] that are fresh, even when the calendar changed since (a reload is then due),
     * or older but unchanged while the observer is watching.
     */
    private fun usableOrNull(key: Key): Entry? {
        val entry = cached ?: return null
        if (entry.key != key) return null
        val age = SystemClock.elapsedRealtime() - entry.fetchedAtElapsedMillis
        if (age !in 0 until MAX_UNCHANGED_AGE_MILLIS) return null
        if (age < FRESH_MILLIS) return entry
        return entry.takeIf { isObserverRegistered && it.changeGeneration == changeGeneration.get() }
    }
}
