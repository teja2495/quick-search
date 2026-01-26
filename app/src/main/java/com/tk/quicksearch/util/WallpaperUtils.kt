package com.tk.quicksearch.util

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
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

    sealed class WallpaperLoadResult {
        data class Success(val bitmap: Bitmap) : WallpaperLoadResult()
        object PermissionRequired : WallpaperLoadResult()
        object SecurityError : WallpaperLoadResult()
        object Unavailable : WallpaperLoadResult()
    }

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
        return when (val result = getWallpaperBitmapResult(context)) {
            is WallpaperLoadResult.Success -> result.bitmap
            else -> null
        }
    }

    suspend fun getWallpaperBitmapResult(context: Context): WallpaperLoadResult {
        cachedBitmap?.let { return WallpaperLoadResult.Success(it) }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadWallpaperBitmap(context)
                if (bitmap != null) {
                    cachedBitmap = bitmap
                    WallpaperLoadResult.Success(bitmap)
                } else if (wallpaperRequiresPermission(context)) {
                    WallpaperLoadResult.PermissionRequired
                } else {
                    WallpaperLoadResult.Unavailable
                }
            } catch (e: SecurityException) {
                if (!hasWallpaperPermission(context)) {
                    WallpaperLoadResult.PermissionRequired
                } else {
                    try {
                        val bitmap = loadWallpaperBitmap(context)
                        if (bitmap != null) {
                            cachedBitmap = bitmap
                            WallpaperLoadResult.Success(bitmap)
                        } else {
                            WallpaperLoadResult.Unavailable
                        }
                    } catch (e2: SecurityException) {
                        WallpaperLoadResult.SecurityError
                    }
                }
            } catch (e: Exception) {
                WallpaperLoadResult.Unavailable
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

    private fun loadWallpaperBitmap(context: Context): Bitmap? {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable = wallpaperManager.drawable
        return wallpaperDrawable?.toBitmap()
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
