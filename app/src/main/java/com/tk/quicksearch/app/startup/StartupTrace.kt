package com.tk.quicksearch.app.startup

import android.os.SystemClock
import android.os.Trace
import android.util.Log
import com.tk.quicksearch.BuildConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight trace markers used to measure user-visible startup readiness milestones. Each marker
 * is also timed from the latest surface's onCreate; release builds log the ones that land late so
 * slow-startup reports from user devices show which milestone was delayed.
 */
object StartupTrace {
    private const val TAG = "QuickSearchStartup"
    private const val SLOW_MILESTONE_MILLIS = 1_000L

    @Volatile private var surfaceName: String? = null
    @Volatile private var surfaceStartElapsedMillis = 0L
    private val loggedMarks = ConcurrentHashMap.newKeySet<String>()

    /** Starts timing milestones for a newly created search surface. */
    fun beginSurface(name: String) {
        surfaceName = name
        surfaceStartElapsedMillis = SystemClock.elapsedRealtime()
        loggedMarks.clear()
    }

    fun mark(name: String) {
        Trace.beginSection(name)
        Trace.endSection()
        logMilestone(name)
    }

    private fun logMilestone(name: String) {
        val surface = surfaceName ?: return
        // Only the first time per surface: later marks come from re-renders, not startup.
        if (!loggedMarks.add(name)) return
        val sinceStartMillis = SystemClock.elapsedRealtime() - surfaceStartElapsedMillis
        val message = "$name at +${sinceStartMillis}ms since $surface onCreate"
        if (sinceStartMillis >= SLOW_MILESTONE_MILLIS) {
            Log.w(TAG, "Slow startup milestone: $message")
        } else if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
