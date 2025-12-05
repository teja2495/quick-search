package com.tk.quicksearch.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
                    val bitmap = drawableToBitmap(wallpaperDrawable)
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
    
    /**
     * Clears the cached wallpaper bitmap.
     * Call this if the wallpaper might have changed.
     */
    fun clearCache() {
        cachedBitmap = null
    }
    
    /**
     * Converts a Drawable to a Bitmap.
     * Uses a reasonable size for wallpaper rendering.
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        // Use a standard wallpaper size for better quality
        val width = 1920
        val height = 1080
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Set bounds to fill the entire bitmap
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        
        return bitmap
    }
    
}

