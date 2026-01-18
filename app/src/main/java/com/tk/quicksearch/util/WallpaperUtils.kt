package com.tk.quicksearch.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility functions for working with wallpapers.
 */
object WallpaperUtils {
    
    // In-memory cache for the wallpaper bitmap
    @Volatile
    private var cachedBitmap: Bitmap? = null
    
    /**
     * Gets the cached wallpaper bitmap synchronously.
     * Returns null if not cached yet.
     */
    fun getCachedWallpaperBitmap(): Bitmap? = cachedBitmap
    
    /**
     * Gets the current wallpaper as a Bitmap.
     * Returns cached bitmap if available, otherwise loads from system.
     * Returns null if wallpaper cannot be retrieved.
     */
    suspend fun getWallpaperBitmap(context: Context): Bitmap? {
        // Return cached bitmap immediately if available
        cachedBitmap?.let { return it }
        
        // Load from system if not cached
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val wallpaperDrawable = wallpaperManager.drawable
                
                if (wallpaperDrawable != null) {
                    val bitmap = wallpaperDrawable.toBitmap()
                    // Cache the bitmap for future use
                    cachedBitmap = bitmap
                    bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Preloads the wallpaper bitmap in the background.
     * This should be called early in the app lifecycle to ensure
     * the wallpaper is ready when needed.
     */
    fun preloadWallpaper(context: Context) {
        // Only preload if not already cached
        if (cachedBitmap == null) {
            CoroutineScope(Dispatchers.IO).launch {
                getWallpaperBitmap(context)
            }
        }
    }
    
}

/**
 * Converts a Drawable to a Bitmap.
 * Uses the drawable's intrinsic dimensions if available, otherwise falls back to standard HD resolution.
 */
private fun Drawable.toBitmap(): Bitmap {
    // Use drawable's intrinsic dimensions if available, otherwise use standard HD resolution
    val width = intrinsicWidth.takeIf { it > 0 } ?: 1920
    val height = intrinsicHeight.takeIf { it > 0 } ?: 1080

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Set bounds to fill the entire bitmap
    setBounds(0, 0, width, height)
    draw(canvas)

    return bitmap
}

