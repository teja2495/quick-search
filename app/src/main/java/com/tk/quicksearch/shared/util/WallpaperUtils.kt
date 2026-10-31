package com.tk.quicksearch.shared.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.shared.permissions.PermissionHelper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_BACKGROUND_BITMAP_DIMENSION = 2400
private const val MAX_BACKGROUND_BITMAP_PIXELS = 4_000_000
private const val STARTUP_PREVIEW_MAX_DIMENSION = 720
private const val STARTUP_PREVIEW_QUALITY = 82
// TODO: mock for previewing the live wallpaper label; remove before release.
private const val MOCK_LIVE_WALLPAPER = true

/**
 * Utility functions for working with wallpapers.
 */
object WallpaperUtils {
    // In-memory cache for the wallpaper bitmap
    @Volatile
    private var cachedBitmap: Bitmap? = null
    /** How much [cachedBitmap] was scaled down from the full wallpaper; see [wallpaperDivisorForBlur]. */
    @Volatile
    private var cachedBitmapDivisor: Int = 1
    @Volatile
    private var cachedSystemWallpaperId: Int? = null
    @Volatile
    private var cachedOverlayCustomUri: String? = null
    @Volatile
    private var cachedOverlayCustomBitmap: Bitmap? = null
    /** How much [cachedOverlayCustomBitmap] was scaled down from its bounded decode; see [wallpaperDivisorForBlur]. */
    @Volatile
    private var cachedOverlayCustomDivisor: Int = 1
    @Volatile
    private var cachedWallpaperAppearance: ImageAppearance? = null
    @Volatile
    private var cachedOverlayCustomAppearanceUri: String? = null
    @Volatile
    private var cachedOverlayCustomAppearance: ImageAppearance? = null
    private val wallpaperBitmapMutex = Mutex()
    private val customImageBitmapMutex = Mutex()

    sealed class WallpaperLoadResult {
        data class Success(
            val bitmap: Bitmap,
        ) : WallpaperLoadResult()

        object PermissionRequired : WallpaperLoadResult()

        object SecurityError : WallpaperLoadResult()

        object Unavailable : WallpaperLoadResult()
    }

    data class WallpaperAccessState(
        /** True only when the app can read wallpaper bitmap pixels. */
        val wallpaperBitmapAvailable: Boolean,
        val needsPermission: Boolean,
        val securityError: Boolean,
        val shouldSelectSystemWallpaper: Boolean,
    )

    fun hasWallpaperAccessPermission(context: Context): Boolean =
        PermissionHelper.checkFilesPermission(context)

    fun resolveWallpaperAccessState(result: WallpaperLoadResult): WallpaperAccessState =
        when (result) {
            is WallpaperLoadResult.Success ->
                WallpaperAccessState(
                    wallpaperBitmapAvailable = true,
                    needsPermission = false,
                    securityError = false,
                    shouldSelectSystemWallpaper = true,
                )

            WallpaperLoadResult.PermissionRequired ->
                WallpaperAccessState(
                    wallpaperBitmapAvailable = false,
                    needsPermission = true,
                    securityError = false,
                    shouldSelectSystemWallpaper = false,
                )

            WallpaperLoadResult.SecurityError ->
                WallpaperAccessState(
                    wallpaperBitmapAvailable = false,
                    needsPermission = false,
                    securityError = true,
                    shouldSelectSystemWallpaper = false,
                )

            WallpaperLoadResult.Unavailable ->
                WallpaperAccessState(
                    wallpaperBitmapAvailable = false,
                    needsPermission = false,
                    securityError = false,
                    shouldSelectSystemWallpaper = false,
                )
        }

    fun shouldUseImageBackground(
        backgroundSource: BackgroundSource,
        hasImageBitmap: Boolean,
        wallpaperAvailable: Boolean,
        requireWallpaperAvailableForSystemSource: Boolean = true,
    ): Boolean {
        if (backgroundSource == BackgroundSource.THEME || !hasImageBitmap) return false
        if (
            requireWallpaperAvailableForSystemSource &&
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
            !wallpaperAvailable
        ) {
            return false
        }
        return true
    }

    /**
     * Only enough pixels to read the wallpaper's colors, for surfaces where Android composites the
     * wallpaper itself and the bitmap is never drawn.
     */
    const val WALLPAPER_THUMBNAIL_DIVISOR = 8

    /**
     * How far the wallpaper can be scaled down before it is drawn under [blurRadiusDp] of blur
     * without the loss showing: a blurred image needs far fewer pixels than a sharp one.
     */
    fun wallpaperDivisorForBlur(blurRadiusDp: Float): Int =
        when {
            // Compose blur only renders from Android 12; before that the image is drawn sharp.
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> 1
            blurRadiusDp >= 16f -> 4
            blurRadiusDp >= 8f -> 2
            else -> 1
        }

    /**
     * Gets the cached wallpaper bitmap synchronously, or null when nothing is cached yet or the
     * cached one was scaled down further than [maxDivisor].
     */
    fun getCachedWallpaperBitmap(maxDivisor: Int = 1): Bitmap? =
        cachedBitmap?.takeIf { cachedBitmapDivisor <= maxDivisor }

    /** Gets the cached custom background synchronously, or null when [uriString] isn't the cached one. */
    fun getCachedOverlayCustomImageBitmap(uriString: String?): ImageBitmap? {
        val normalized = uriString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cached = cachedOverlayCustomBitmap ?: return null
        return cached.takeIf { cachedOverlayCustomUri == normalized }?.asImageBitmap()
    }

    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? =
        runCatching {
            val dir = File(context.filesDir, "backgrounds")
            if (!dir.exists()) dir.mkdirs()
            val extension = resolveImageExtension(context, sourceUri)
            val dest = File(dir, "custom_background_${System.currentTimeMillis()}.$extension")
            val writeSucceeded =
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                    true
                } ?: false
            if (!writeSucceeded) return@runCatching null
            if (!dest.exists() || dest.length() == 0L) return@runCatching null
            dir.listFiles { f -> f.name.startsWith("custom_background_") && f != dest }
                ?.forEach { it.delete() }
            Uri.fromFile(dest).toString()
        }
            .onFailure { Log.w("WallpaperUtils", "Failed to save custom background image", it) }
            .getOrNull()

    /** Deletes the saved custom background and forgets its cached bitmap. */
    fun deleteCustomImages(context: Context) {
        cachedOverlayCustomUri = null
        cachedOverlayCustomBitmap = null
        cachedOverlayCustomDivisor = 1
        cachedOverlayCustomAppearance = null
        File(context.filesDir, "backgrounds")
            .listFiles { f -> f.name.startsWith("custom_background_") }
            ?.forEach { it.delete() }
    }

    /**
     * True when the home screen runs a live wallpaper and the system keeps no image of it. Android
     * then hands apps its built-in default image instead. Some OEMs keep an image alongside a live
     * wallpaper, and that one still reads fine.
     */
    fun isLiveWallpaperActive(context: Context): Boolean {
        if (MOCK_LIVE_WALLPAPER) return true
        return runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (wallpaperManager.wallpaperInfo == null) return@runCatching false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return@runCatching true
            val imageFile = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            imageFile?.close()
            imageFile == null
        }.getOrDefault(false)
    }

    fun invalidateWallpaperCache() {
        cachedBitmap = null
        cachedBitmapDivisor = 1
        cachedSystemWallpaperId = null
        cachedWallpaperAppearance = null
    }

    fun clearMemoryCaches() {
        invalidateWallpaperCache()
        cachedOverlayCustomUri = null
        cachedOverlayCustomBitmap = null
        cachedOverlayCustomDivisor = 1
        cachedOverlayCustomAppearanceUri = null
        cachedOverlayCustomAppearance = null
    }

    /**
     * Gets the current wallpaper as a Bitmap.
     * Returns cached bitmap if available, otherwise loads from system.
     * Returns null if wallpaper cannot be retrieved.
     */
    suspend fun getWallpaperBitmap(
        context: Context,
        divisor: Int = 1,
    ): Bitmap? =
        when (val result = getWallpaperBitmapResult(context, divisor)) {
            is WallpaperLoadResult.Success -> result.bitmap
            else -> null
        }

    /**
     * Loads the wallpaper scaled down by [divisor] and keeps only that one in memory, so a blurred or
     * system-composited wallpaper does not hold a full-size bitmap for the life of the process.
     */
    suspend fun getWallpaperBitmapResult(
        context: Context,
        divisor: Int = 1,
    ): WallpaperLoadResult =
        withContext(Dispatchers.IO) {
            wallpaperBitmapMutex.withLock {
                getWallpaperBitmapResultLocked(context, divisor.coerceAtLeast(1))
            }
        }

    private suspend fun getWallpaperBitmapResultLocked(
        context: Context,
        divisor: Int,
    ): WallpaperLoadResult {
        // System wallpaper compositing does not require file access, but reading its
        // pixels does. Never call WallpaperManager.drawable without that permission.
        if (!hasWallpaperAccessPermission(context)) {
            return WallpaperLoadResult.PermissionRequired
        }

        val currentWallpaperId = getCurrentSystemWallpaperId(context)
        val cachedWallpaperId = cachedSystemWallpaperId
        cachedBitmap?.let { cached ->
            val shouldReuseCached =
                when {
                    currentWallpaperId == null -> true
                    cachedWallpaperId == null -> false
                    else -> currentWallpaperId == cachedWallpaperId
                }
            if (shouldReuseCached) {
                val cachedDivisor = cachedBitmapDivisor
                if (cachedDivisor == divisor) return WallpaperLoadResult.Success(cached)
                // A finer bitmap only needs scaling down; a coarser one is reloaded below.
                if (cachedDivisor < divisor) {
                    val scale = cachedDivisor.toFloat() / divisor
                    val scaled =
                        runCatching {
                            Bitmap.createScaledBitmap(
                                cached,
                                (cached.width * scale).toInt().coerceAtLeast(1),
                                (cached.height * scale).toInt().coerceAtLeast(1),
                                true,
                            )
                        }.getOrNull()
                    if (scaled != null) {
                        cachedBitmap = scaled
                        cachedBitmapDivisor = divisor
                        return WallpaperLoadResult.Success(scaled)
                    }
                }
            } else {
                invalidateWallpaperCache()
            }
        }

        return try {
            val bitmap = loadWallpaperBitmap(context, divisor)
            if (bitmap != null) {
                cachedBitmap = bitmap
                cachedBitmapDivisor = divisor
                cachedSystemWallpaperId = currentWallpaperId ?: getCurrentSystemWallpaperId(context)
                WallpaperLoadResult.Success(bitmap)
            } else {
                WallpaperLoadResult.Unavailable
            }
        } catch (e: SecurityException) {
            WallpaperLoadResult.SecurityError
        } catch (e: Exception) {
            WallpaperLoadResult.Unavailable
        }
    }

    private fun getCurrentSystemWallpaperId(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return runCatching {
            WallpaperManager.getInstance(context).getWallpaperId(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()
    }

    /**
     * Preloads the wallpaper bitmap in the background.
     * This should be called early in the app lifecycle to ensure
     * the wallpaper is ready when needed.
     */
    suspend fun preloadWallpaper(
        context: Context,
        divisor: Int = 1,
    ) {
        if (cachedBitmap == null) {
            getWallpaperBitmap(context, divisor)
        }
    }

    suspend fun preloadCustomImage(
        context: Context,
        uriString: String?,
        divisor: Int = 1,
    ) {
        val normalized = uriString?.trim()?.takeIf { it.isNotEmpty() } ?: return
        if (cachedOverlayCustomUri == normalized && cachedOverlayCustomBitmap != null) return
        getOverlayCustomImageBitmap(context, normalized, divisor)
    }

    /**
     * Loads the custom background scaled down by [divisor] and keeps only that one in memory, the
     * same way [getWallpaperBitmapResult] does for the system wallpaper.
     */
    suspend fun getOverlayCustomImageBitmap(
        context: Context,
        uriString: String?,
        divisor: Int = 1,
    ): ImageBitmap? =
        customImageBitmapMutex.withLock {
            getOverlayCustomImageBitmapLocked(context, uriString, divisor.coerceAtLeast(1))
        }

    private suspend fun getOverlayCustomImageBitmapLocked(
        context: Context,
        uriString: String?,
        divisor: Int,
    ): ImageBitmap? {
        val normalized = uriString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cached = cachedOverlayCustomBitmap
        if (cachedOverlayCustomUri == normalized && cached != null) {
            val cachedDivisor = cachedOverlayCustomDivisor
            if (cachedDivisor == divisor) return cached.asImageBitmap()
            // A sharper cached image can be scaled down; a blurrier one has to be decoded again.
            if (cachedDivisor < divisor) {
                val scale = cachedDivisor.toFloat() / divisor
                val scaled =
                    withContext(Dispatchers.Default) {
                        runCatching {
                            Bitmap.createScaledBitmap(
                                cached,
                                (cached.width * scale).toInt().coerceAtLeast(1),
                                (cached.height * scale).toInt().coerceAtLeast(1),
                                true,
                            )
                        }.getOrNull()
                    }
                if (scaled != null) {
                    cachedOverlayCustomBitmap = scaled
                    cachedOverlayCustomDivisor = divisor
                    return scaled.asImageBitmap()
                }
            }
        }

        val bitmap =
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(normalized)
                    decodeBitmapWithOrientation(context, uri, divisor)
                }.getOrNull()
            } ?: return null

        if (cachedOverlayCustomUri != normalized) {
            cachedOverlayCustomAppearanceUri = null
            cachedOverlayCustomAppearance = null
        }
        cachedOverlayCustomUri = normalized
        cachedOverlayCustomBitmap = bitmap
        cachedOverlayCustomDivisor = divisor
        return bitmap.asImageBitmap()
    }

    suspend fun getBackgroundAppearance(
        context: Context,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
    ): ImageAppearance? {
        return when (backgroundSource) {
            BackgroundSource.SYSTEM_WALLPAPER -> getWallpaperAppearance(context)
            BackgroundSource.CUSTOM_IMAGE -> {
                val normalized = customImageUri?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                getCustomImageAppearance(context, normalized)
            }
            BackgroundSource.THEME -> null
        }
    }

    suspend fun loadStartupBackgroundPreviewBitmap(
        previewPath: String?,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val normalized = previewPath?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext null
            val file = File(normalized)
            if (!file.exists() || !file.isFile) return@withContext null
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }

    suspend fun saveStartupBackgroundPreview(
        context: Context,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
    ): String? =
        withContext(Dispatchers.IO) {
            val sourceBitmap =
                when (backgroundSource) {
                    // The cached bitmap may be a thumbnail; then a preview-sized one is read just for this.
                    BackgroundSource.SYSTEM_WALLPAPER ->
                        cachedBitmap?.takeIf { maxOf(it.width, it.height) >= STARTUP_PREVIEW_MAX_DIMENSION }
                            ?: loadUncachedWallpaperBitmap(context, maxDimension = STARTUP_PREVIEW_MAX_DIMENSION)
                    BackgroundSource.CUSTOM_IMAGE -> {
                        val normalized = customImageUri?.trim()?.takeIf { it.isNotEmpty() }
                        if (normalized == null) {
                            null
                        } else {
                            cachedOverlayCustomBitmap?.takeIf {
                                cachedOverlayCustomUri == normalized &&
                                    maxOf(it.width, it.height) >= STARTUP_PREVIEW_MAX_DIMENSION
                            } ?: runCatching {
                                decodeBitmapWithOrientation(context, Uri.parse(normalized))
                            }.getOrNull()
                        }
                    }
                    BackgroundSource.THEME -> null
                } ?: return@withContext null

            val preview = buildStartupPreview(sourceBitmap)
            val directory = File(context.filesDir, "startup")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "startup_background_preview.jpg")
            val writeSucceeded =
                runCatching {
                    FileOutputStream(file).use { output ->
                        preview.compress(Bitmap.CompressFormat.JPEG, STARTUP_PREVIEW_QUALITY, output)
                    }
                }.isSuccess

            if (preview != sourceBitmap) {
                preview.recycle()
            }

            if (writeSucceeded) file.absolutePath else null
        }

    private fun loadWallpaperBitmap(
        context: Context,
        divisor: Int = 1,
        maxDimension: Int = MAX_BACKGROUND_BITMAP_DIMENSION,
    ): Bitmap? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = wallpaperManager.drawable
            try {
                wallpaperDrawable?.toBitmap(divisor, maxDimension)
            } finally {
                // WallpaperManager keeps the full-size decode in a process-wide cache until told to
                // forget it, doubling the memory of the copy kept here.
                runCatching { wallpaperManager.forgetLoadedWallpaper() }
            }
        } catch (exception: SecurityException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    /** Reads the wallpaper without replacing the cached one, for one-off uses like previews. */
    private fun loadUncachedWallpaperBitmap(
        context: Context,
        divisor: Int = 1,
        maxDimension: Int = MAX_BACKGROUND_BITMAP_DIMENSION,
    ): Bitmap? {
        if (!hasWallpaperAccessPermission(context)) return null
        return runCatching { loadWallpaperBitmap(context, divisor, maxDimension) }.getOrNull()
    }

    private suspend fun getWallpaperAppearance(context: Context): ImageAppearance? {
        cachedWallpaperAppearance?.let { return it }
        // Analysis samples the image down to a few dozen pixels, so a thumbnail is enough.
        val bitmap =
            cachedBitmap
                ?: withContext(Dispatchers.IO) {
                    loadUncachedWallpaperBitmap(context, divisor = WALLPAPER_THUMBNAIL_DIVISOR)
                }
                ?: return null
        return withContext(Dispatchers.Default) {
            ImageAppearanceUtils.analyze(bitmap)
        }.also { appearance ->
            cachedWallpaperAppearance = appearance
        }
    }

    private suspend fun getCustomImageAppearance(
        context: Context,
        normalizedUri: String,
    ): ImageAppearance? {
        if (cachedOverlayCustomAppearanceUri == normalizedUri && cachedOverlayCustomAppearance != null) {
            return cachedOverlayCustomAppearance
        }
        val bitmap =
            customImageBitmapMutex.withLock {
                if (cachedOverlayCustomUri == normalizedUri && cachedOverlayCustomBitmap != null) {
                    cachedOverlayCustomBitmap
                } else {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            decodeBitmapWithOrientation(context, Uri.parse(normalizedUri))
                        }.getOrNull()
                    }?.also { decoded ->
                        if (cachedOverlayCustomUri != normalizedUri) {
                            cachedOverlayCustomAppearanceUri = null
                            cachedOverlayCustomAppearance = null
                        }
                        cachedOverlayCustomUri = normalizedUri
                        cachedOverlayCustomBitmap = decoded
                        cachedOverlayCustomDivisor = 1
                    }
                }
            } ?: return null

        return withContext(Dispatchers.Default) {
            ImageAppearanceUtils.analyze(bitmap)
        }.also { appearance ->
            cachedOverlayCustomAppearanceUri = normalizedUri
            cachedOverlayCustomAppearance = appearance
        }
    }

    /** Decodes within the background bounds, then [divisor] times smaller still for blurred backgrounds. */
    private fun decodeBitmapWithOrientation(
        context: Context,
        uri: Uri,
        divisor: Int = 1,
    ): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeBitmapWithImageDecoder(context, uri, divisor)
                ?: decodeBitmapWithBitmapFactory(context, uri, divisor)
        } else {
            decodeBitmapWithBitmapFactory(context, uri, divisor)
        }

    private fun decodeBitmapWithImageDecoder(
        context: Context,
        uri: Uri,
        divisor: Int,
    ): Bitmap? =
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded =
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    if (width > 0 && height > 0) {
                        decoder.setTargetSampleSize(computeSampleSize(width, height) * divisor)
                    }
                }
            clampBitmapToBounds(decoded)
        }
            .onFailure { Log.w("WallpaperUtils", "ImageDecoder failed for custom background", it) }
            .getOrNull()

    private fun decodeBitmapWithBitmapFactory(
        context: Context,
        uri: Uri,
        divisor: Int,
    ): Bitmap? {
        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        val sourceWidth = boundsOptions.outWidth
        val sourceHeight = boundsOptions.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(sourceWidth, sourceHeight) * divisor
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        val decoded =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

        val original = clampBitmapToBounds(decoded)
        val exifOrientation =
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
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

    private fun buildStartupPreview(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val scale =
            minOf(
                1f,
                STARTUP_PREVIEW_MAX_DIMENSION.toFloat() / width,
                STARTUP_PREVIEW_MAX_DIMENSION.toFloat() / height,
            )
        if (scale >= 1f) return bitmap

        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun resolveImageExtension(
        context: Context,
        sourceUri: Uri,
    ): String {
        val mimeExtension =
            context.contentResolver.getType(sourceUri)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        if (mimeExtension != null) {
            return mimeExtension.lowercase()
        }

        val uriPathExtension = sourceUri.lastPathSegment?.substringAfterLast('.', "")
        return uriPathExtension?.takeIf { it.isNotBlank() }?.lowercase() ?: "jpg"
    }
}

/**
 * Converts a Drawable to a Bitmap.
 * Uses the drawable's intrinsic dimensions if available, otherwise falls back to standard HD resolution.
 */
private fun Drawable.toBitmap(
    divisor: Int = 1,
    maxDimension: Int = MAX_BACKGROUND_BITMAP_DIMENSION,
): Bitmap? =
    runCatching {
        // Use drawable's intrinsic dimensions if available, otherwise use standard HD resolution.
        val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: 1920
        val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: 1080
        val scale =
            minOf(
                1f,
                maxDimension.toFloat() / sourceWidth,
                maxDimension.toFloat() / sourceHeight,
                kotlin.math.sqrt(
                    MAX_BACKGROUND_BITMAP_PIXELS.toFloat() /
                        (sourceWidth.toFloat() * sourceHeight.toFloat()),
                ).coerceAtMost(1f),
            ) / divisor.coerceAtLeast(1)
        val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val height = (sourceHeight * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        bitmap
    }.onFailure {
        Log.w("WallpaperUtils", "Failed to rasterize wallpaper drawable", it)
    }.getOrNull()
