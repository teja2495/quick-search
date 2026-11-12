package com.tk.quicksearch.search.data

import android.os.SystemClock
import android.util.Log

/**
 * Logs how long a whole-provider snapshot read took. Unlike the debug-only search timing logs this
 * also logs in release builds, so slowdown reports from user devices show which snapshot was slow.
 * Snapshots reload rarely (TTL or provider change), so the volume stays low.
 */
internal object SnapshotLoadLog {
    private const val TAG = "QuickSearchSnapshot"

    fun loaded(
        name: String,
        rowCount: Int,
        startElapsedMillis: Long,
    ) {
        val durationMillis = SystemClock.elapsedRealtime() - startElapsedMillis
        Log.i(TAG, "$name loaded $rowCount rows in ${durationMillis}ms")
    }
}
