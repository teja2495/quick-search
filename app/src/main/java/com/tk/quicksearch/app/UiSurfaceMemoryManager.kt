package com.tk.quicksearch.app

import com.tk.quicksearch.search.apps.clearAppIconMemoryCache
import com.tk.quicksearch.search.data.appShortcutRepository.clearShortcutIconMemoryCache
import com.tk.quicksearch.search.files.clearFileThumbnailMemoryCache
import com.tk.quicksearch.search.apps.IconPackManager
import com.tk.quicksearch.shared.util.WallpaperUtils
import java.util.concurrent.atomic.AtomicInteger

/** Keeps process-wide bitmap cleanup from running while another app UI surface is still active. */
object UiSurfaceMemoryManager {
    private val activeSurfaceCount = AtomicInteger(0)

    fun onSurfaceCreated() {
        activeSurfaceCount.incrementAndGet()
    }

    fun onSurfaceDestroyed(isChangingConfigurations: Boolean) {
        val remainingCount =
            activeSurfaceCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        if (remainingCount == 0 && !isChangingConfigurations) {
            clearBitmapMemoryCaches()
            Runtime.getRuntime().gc()
        }
    }

    fun clearBitmapMemoryCaches() {
        WallpaperUtils.clearMemoryCaches()
        clearAppIconMemoryCache()
        clearShortcutIconMemoryCache()
        clearFileThumbnailMemoryCache()
        IconPackManager.clearAllCaches()
    }
}
