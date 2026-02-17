package com.tk.quicksearch.util

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
    @Volatile
    private var cachedOverlayCustomUri: String? = null
    @Volatile
    private var cachedOverlayCustomBitmap: Bitmap? = null

    sealed class WallpaperLoadResult {
        data class Success(
            val bitmap: Bitmap,
        ) : WallpaperLoadResult()

        object PermissionRequired : WallpaperLoadResult()

        object SecurityError : WallpaperLoadResult()

        object Unavailable : WallpaperLoadResult()
    }

    /**
     * Checks if the app has permission to access wallpapers on Android 13+.
     */
    fun hasWallpaperPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older Android versions, wallpaper access doesn't require special permissions
            true
        }

    /**
     * Checks if wallpaper access would require special permission on this device.
     * This is used to determine if we should show permission prompts to the user.
     */
    fun wallpaperRequiresPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasWallpaperPermission(context)

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
    suspend fun getWallpaperBitmap(context: Context): Bitmap? =
        when (val result = getWallpaperBitmapResult(context)) {
            is WallpaperLoadResult.Success -> result.bitmap
            else -> null
        }

    suspend fun getWallpaperBitmapResult(context: Context): WallpaperLoadResult {
        cachedBitmap?.let { return WallpaperLoadResult.Success(it) }

        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadWallpaperBitmap(context)
                if (bitmap != null) {
                    cachedBitmap = bitmap
                    WallpaperLoadResult.Success(bitmap)
                } else {
                    WallpaperLoadResult.Unavailable
                }
            } catch (e: SecurityException) {
                WallpaperLoadResult.PermissionRequired
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

    suspend fun getOverlayCustomImageBitmap(
        context: Context,
        uriString: String?,
    ): ImageBitmap? {
        val normalized = uriString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (cachedOverlayCustomUri == normalized && cachedOverlayCustomBitmap != null) {
            return cachedOverlayCustomBitmap?.asImageBitmap()
        }

        val bitmap =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(normalized)
                    decodeBitmapWithOrientation(context, uri)
                }.getOrNull()
            } ?: return null

        cachedOverlayCustomUri = normalized
        cachedOverlayCustomBitmap = bitmap
        return bitmap.asImageBitmap()
    }

    private fun loadWallpaperBitmap(context: Context): Bitmap? {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable = wallpaperManager.drawable
        return wallpaperDrawable?.toBitmap()
    }

    private fun decodeBitmapWithOrientation(
        context: Context,
        uri: Uri,
    ): Bitmap? {
        val bytes =
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val exifOrientation =
            runCatching {
                ExifInterface(bytes.inputStream()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            else -> return original
        }

        val transformed =
            runCatching {
                Bitmap.createBitmap(
                    original,
                    0,
                    0,
                    original.width,
                    original.height,
                    matrix,
                    true,
                )
            }.getOrNull() ?: return original

        if (transformed != original) {
            original.recycle()
        }
        return transformed
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
