package com.tk.quicksearch.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility functions for working with wallpapers.
 */
object WallpaperUtils {
    
    /**
     * Gets the current wallpaper as a Bitmap.
     * Returns null if wallpaper cannot be retrieved.
     */
    suspend fun getWallpaperBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = wallpaperManager.drawable
            
            if (wallpaperDrawable != null) {
                drawableToBitmap(wallpaperDrawable)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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

