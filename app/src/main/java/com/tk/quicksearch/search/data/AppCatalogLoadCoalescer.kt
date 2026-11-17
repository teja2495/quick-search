package com.tk.quicksearch.search.data

import android.os.SystemClock
import com.tk.quicksearch.search.models.AppInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Each UI surface (launcher/main and overlay) has its own ViewModel and [AppsRepository], and both
 * rebuild the catalog after the same package change. This lets the second rebuild reuse the
 * first one's result instead of repeating every PackageManager call.
 *
 * A result is reused only when it was built with the same inputs, started after the latest
 * catalog change in this process, and finished within [REUSE_WINDOW_MS].
 */
internal object AppCatalogLoadCoalescer {
    private const val REUSE_WINDOW_MS = 2_000L

    private val mutex = Mutex()

    @Volatile
    private var lastCatalogChangeAtElapsedMs = 0L
    private var lastLoad: CompletedLoad? = null

    private class CompletedLoad(
        val key: LoadKey,
        val startedAtElapsedMs: Long,
        val completedAtElapsedMs: Long,
        val apps: List<AppInfo>,
    )

    data class LoadKey(
        val includeNonLaunchableApps: Boolean,
        val includeArchivedApps: Boolean,
        val launchCounts: Map<String, Int>,
    )

    /** Marks earlier results stale; called for every recorded catalog change or cache clear. */
    fun onCatalogChanged() {
        lastCatalogChangeAtElapsedMs = SystemClock.elapsedRealtime()
    }

    suspend fun load(
        key: LoadKey,
        build: suspend () -> List<AppInfo>,
    ): List<AppInfo> =
        mutex.withLock {
            val nowElapsedMs = SystemClock.elapsedRealtime()
            lastLoad?.let { last ->
                if (
                    last.key == key &&
                    last.startedAtElapsedMs > lastCatalogChangeAtElapsedMs &&
                    nowElapsedMs - last.completedAtElapsedMs < REUSE_WINDOW_MS
                ) {
                    return@withLock last.apps
                }
            }
            val apps = build()
            lastLoad = CompletedLoad(key, nowElapsedMs, SystemClock.elapsedRealtime(), apps)
            apps
        }
}
