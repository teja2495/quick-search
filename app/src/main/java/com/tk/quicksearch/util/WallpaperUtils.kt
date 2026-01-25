package com.tk.quicksearch.util

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
     * Checks if the app has permission to access wallpapers on Android 13+.
     */
    fun hasWallpaperPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older Android versions, wallpaper access doesn't require special permissions
            true
        }
    }

    /**
     * Checks if wallpaper access would require special permission on this device.
     * This is used to determine if we should show permission prompts to the user.
     */
    fun wallpaperRequiresPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasWallpaperPermission(context)
    }
    
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
            } catch (e: SecurityException) {
                // On Android 13+, wallpaper access may require READ_MEDIA_IMAGES permission
                // Check if we have the permission - if not, permission is needed for wallpaper access
                if (!hasWallpaperPermission(context)) {
                    // Permission not granted, cannot access wallpaper
                    null
                } else {
                    // We have permission but still got SecurityException
                    // This might be a device-specific enforcement, try once more
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
                    } catch (e2: SecurityException) {
                        // Still failing even with permission, give up
                        null
                    }
                }
            } catch (e: Exception) {
                // Other exceptions (not permission-related)
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

