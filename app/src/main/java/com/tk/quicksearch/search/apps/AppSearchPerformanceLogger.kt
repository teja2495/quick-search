package com.tk.quicksearch.search.apps

import android.util.Log
import com.tk.quicksearch.BuildConfig

/**
 * Traces for identifying delays in app catalog, suggestions, and result delivery. Debug builds log
 * every timing; release builds log only clearly slow ones (no query text), so reports from user
 * devices show which search or startup step stalled.
 */
internal object AppSearchPerformanceLogger {
    private const val TAG = "AppSearch"

    // Release builds skip the tight debug thresholds so normal typing on slower phones stays quiet.
    private const val RELEASE_MIN_SLOW_MS = 250L

    fun log(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    fun logTiming(
        event: String,
        elapsedMs: Long,
        slowThresholdMs: Long,
        details: () -> String = { "" },
    ) {
        if (!BuildConfig.DEBUG) {
            if (elapsedMs >= maxOf(slowThresholdMs, RELEASE_MIN_SLOW_MS)) {
                Log.w(TAG, "$event elapsedMs=$elapsedMs slow=true ${details()}")
            }
            return
        }

        val message = "$event elapsedMs=$elapsedMs slow=${elapsedMs >= slowThresholdMs} ${details()}"
        if (elapsedMs >= slowThresholdMs) {
            Log.w(TAG, message)
        } else {
            Log.d(TAG, message)
        }
    }
}
