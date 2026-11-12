package com.tk.quicksearch.search.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.tk.quicksearch.search.models.CalendarEventInfo
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide cache of the multi-year calendar instance window that event search scores. Shared
 * across repository instances so a new search screen doesn't re-expand every recurring event, and
 * loaded by one caller at a time. After a calendar change the previous rows keep serving while a
 * debounced background reload runs, because syncs notify the provider many times in a row.
 */
internal object CalendarSearchRowsCache {
    private const val MAX_AGE_MILLIS = 5L * 60L * 1000L
    private const val CHANGE_RELOAD_DELAY_MILLIS = 3_000L

    private class Entry(
        val rows: List<CalendarEventInfo>,
        val fetchedAtElapsedMillis: Long,
        val changeGeneration: Long,
        val todayStartMillis: Long,
    )

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
            val todayStart = cached?.todayStartMillis ?: return@Runnable
            val load = lastLoader ?: return@Runnable
            reloadExecutor.execute { loadLocked(todayStart, load, reloadIfStale = true) }
        }

    @Volatile private var cached: Entry? = null
    @Volatile private var lastLoader: (() -> List<CalendarEventInfo>)? = null

    fun get(
        todayStartMillis: Long,
        load: () -> List<CalendarEventInfo>,
    ): List<CalendarEventInfo> {
        lastLoader = load
        usableOrNull(todayStartMillis)?.let { entry ->
            if (entry.changeGeneration != changeGeneration.get()) scheduleReloadAfterChange()
            return entry.rows
        }
        return loadLocked(todayStartMillis, load, reloadIfStale = false)
    }

    fun markChanged() {
        changeGeneration.incrementAndGet()
        if (cached != null) scheduleReloadAfterChange()
    }

    fun release() {
        cached = null
    }

    private fun loadLocked(
        todayStartMillis: Long,
        load: () -> List<CalendarEventInfo>,
        reloadIfStale: Boolean,
    ): List<CalendarEventInfo> =
        synchronized(loadLock) {
            usableOrNull(todayStartMillis)?.let { entry ->
                val isCurrent = entry.changeGeneration == changeGeneration.get()
                if (isCurrent || !reloadIfStale) return entry.rows
            }
            val generation = changeGeneration.get()
            val startElapsed = SystemClock.elapsedRealtime()
            val rows = load()
            SnapshotLoadLog.loaded("Calendar search rows", rows.size, startElapsed)
            cached = Entry(rows, startElapsed, generation, todayStartMillis)
            rows
        }

    private fun scheduleReloadAfterChange() {
        mainHandler.removeCallbacks(reloadAfterChange)
        mainHandler.postDelayed(reloadAfterChange, CHANGE_RELOAD_DELAY_MILLIS)
    }

    /** Rows for [todayStartMillis] within the TTL, even when the calendar changed since. */
    private fun usableOrNull(todayStartMillis: Long): Entry? {
        val entry = cached ?: return null
        val age = SystemClock.elapsedRealtime() - entry.fetchedAtElapsedMillis
        return entry.takeIf { it.todayStartMillis == todayStartMillis && age in 0 until MAX_AGE_MILLIS }
    }
}
