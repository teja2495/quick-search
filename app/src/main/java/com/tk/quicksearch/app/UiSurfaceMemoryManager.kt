package com.tk.quicksearch.app

import android.os.Handler
import android.os.Looper
import com.tk.quicksearch.search.apps.clearAppIconMemoryCache
import com.tk.quicksearch.search.data.appShortcutRepository.clearShortcutIconMemoryCache
import com.tk.quicksearch.search.data.bookmarks.clearBookmarkFaviconMemoryCache
import com.tk.quicksearch.search.files.clearFileThumbnailMemoryCache
import com.tk.quicksearch.settings.appShortcutsSettings.clearShortcutSourceIconMemoryCache
import com.tk.quicksearch.search.apps.IconPackManager
import com.tk.quicksearch.search.contacts.components.ContactImageCache
import com.tk.quicksearch.search.data.CalendarSearchRowsCache
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileNameIndex
import com.tk.quicksearch.shared.util.WallpaperUtils
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** Keeps process-wide bitmap and index cleanup from running while another app UI surface is still active. */
object UiSurfaceMemoryManager {
    // Reopening within this window (the overlay is often reopened right away) reuses decoded icons
    // and the wallpaper instead of decoding them again; after it, an idle process gives them back.
    private const val IDLE_CLEAR_DELAY_MILLIS = 60_000L

    private val activeSurfaceCount = AtomicInteger(0)
    private val visibleSurfaceCount = AtomicInteger(0)
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val clearWhenIdle =
        Runnable {
            if (activeSurfaceCount.get() != 0) return@Runnable
            // The file name index outlives the UI so the next search doesn't rescan MediaStore; its
            // own TTL and memory-pressure callbacks release it.
            clearMemoryCaches(releaseSearchIndexes = false)
            // Off the main thread so the pause can't land on a frame if a surface opens meanwhile.
            thread(name = "UiSurfaceMemoryGc", isDaemon = true) { Runtime.getRuntime().gc() }
        }

    fun onSurfaceCreated() {
        activeSurfaceCount.incrementAndGet()
        mainHandler.removeCallbacks(clearWhenIdle)
    }

    fun onSurfaceDestroyed(isChangingConfigurations: Boolean) {
        val remainingCount =
            activeSurfaceCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        if (remainingCount == 0 && !isChangingConfigurations) {
            mainHandler.removeCallbacks(clearWhenIdle)
            mainHandler.postDelayed(clearWhenIdle, IDLE_CLEAR_DELAY_MILLIS)
        }
    }

    /** Paired with [onSurfaceStopped]; background work such as index rebuilds runs only while visible. */
    fun onSurfaceStarted() {
        if (visibleSurfaceCount.incrementAndGet() == 1) {
            FileNameIndex.onSurfaceVisibilityChanged(visible = true)
            CalendarSearchRowsCache.onSurfaceVisibilityChanged(visible = true)
        }
    }

    fun onSurfaceStopped() {
        val remainingCount =
            visibleSurfaceCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        if (remainingCount == 0) {
            FileNameIndex.onSurfaceVisibilityChanged(visible = false)
            CalendarSearchRowsCache.onSurfaceVisibilityChanged(visible = false)
        }
    }

    fun clearMemoryCaches(releaseSearchIndexes: Boolean = true) {
        WallpaperUtils.clearMemoryCaches()
        clearAppIconMemoryCache()
        clearShortcutIconMemoryCache()
        clearFileThumbnailMemoryCache()
        clearShortcutSourceIconMemoryCache()
        clearBookmarkFaviconMemoryCache()
        IconPackManager.clearAllCaches()
        ContactImageCache.clear()
        if (releaseSearchIndexes) {
            FileNameIndex.release()
            CalendarSearchRowsCache.release()
            ContactRepository.releaseMemoryCaches()
        }
    }
}
