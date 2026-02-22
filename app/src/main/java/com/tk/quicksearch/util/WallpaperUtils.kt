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

private const val MAX_BACKGROUND_BITMAP_DIMENSION = 4096
private const val MAX_BACKGROUND_BITMAP_PIXELS = 12_000_000

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
        val original = decodeBoundedBitmap(bytes) ?: return null
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

    private fun decodeBoundedBitmap(bytes: ByteArray): Bitmap? {
        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val sourceWidth = boundsOptions.outWidth
        val sourceHeight = boundsOptions.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(sourceWidth, sourceHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        return clampBitmapToBounds(sampled)
    }

    private fun computeSampleSize(
        width: Int,
        height: Int,
    ): Int {
        var sampleSize = 1
        while (true) {
            val sampledWidth = width / sampleSize
            val sampledHeight = height / sampleSize
            val exceedsDimension =
                sampledWidth > MAX_BACKGROUND_BITMAP_DIMENSION ||
                    sampledHeight > MAX_BACKGROUND_BITMAP_DIMENSION
            val exceedsPixels =
                sampledWidth.toLong() * sampledHeight.toLong() > MAX_BACKGROUND_BITMAP_PIXELS
            if (!exceedsDimension && !exceedsPixels) break
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun clampBitmapToBounds(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val dimensionScale =
            minOf(
                1f,
                MAX_BACKGROUND_BITMAP_DIMENSION.toFloat() / width,
                MAX_BACKGROUND_BITMAP_DIMENSION.toFloat() / height,
            )
        val pixelScale =
            kotlin.math.sqrt(
                MAX_BACKGROUND_BITMAP_PIXELS.toFloat() / (width.toFloat() * height.toFloat()),
            ).coerceAtMost(1f)
        val finalScale = minOf(dimensionScale, pixelScale)
        if (finalScale >= 1f) return bitmap

        val targetWidth = (width * finalScale).toInt().coerceAtLeast(1)
        val targetHeight = (height * finalScale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
}

/**
 * Converts a Drawable to a Bitmap.
 * Uses the drawable's intrinsic dimensions if available, otherwise falls back to standard HD resolution.
 */
private fun Drawable.toBitmap(): Bitmap {
    // Use drawable's intrinsic dimensions if available, otherwise use standard HD resolution
    val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: 1920
    val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: 1080
    val scale =
        minOf(
            1f,
            MAX_BACKGROUND_BITMAP_DIMENSION.toFloat() / sourceWidth,
            MAX_BACKGROUND_BITMAP_DIMENSION.toFloat() / sourceHeight,
            kotlin.math.sqrt(
                MAX_BACKGROUND_BITMAP_PIXELS.toFloat() /
                    (sourceWidth.toFloat() * sourceHeight.toFloat()),
            ).coerceAtMost(1f),
        )
    val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
    val height = (sourceHeight * scale).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Set bounds to fill the entire bitmap
    setBounds(0, 0, width, height)
    draw(canvas)

    return bitmap
}
