package com.tk.quicksearch.search.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Color as AndroidColor
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.common.UserHandleUtils
import com.tk.quicksearch.search.managers.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

private data class AppIconEntry(
    val bitmap: ImageBitmap,
    val isLegacy: Boolean,
    val monochromeData: ImageBitmap? = null,
)

private val appIconCacheEpoch = AtomicLong(0L)

/**
 * Zoom factor when rasterizing adaptive icons for a circular mask. The foreground/background
 * safe zone is smaller than the full canvas, so without scaling the glyph looks small in the circle.
 */
private const val CircularAdaptiveIconContentScale = 1.2f

/**
 * In-memory cache for app icons to avoid repeated loading.
 */
private object AppIconCache {
    private const val MAX_CACHE_SIZE_BYTES = 8 * 1024 * 1024 // 8 MB cap to avoid OOM
    private const val BYTES_PER_PIXEL = 4

    private val cache =
        object : LruCache<String, AppIconEntry>(MAX_CACHE_SIZE_BYTES) {
            override fun sizeOf(
                key: String,
                value: AppIconEntry,
            ): Int {
                val b = value.bitmap
                val bytes = b.width.toLong() * b.height.toLong() * BYTES_PER_PIXEL
                return bytes.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
            }
        }

    fun get(cacheKey: String): AppIconEntry? = cache.get(cacheKey)

    fun put(
        cacheKey: String,
        entry: AppIconEntry,
    ) {
        cache.put(cacheKey, entry)
    }

    fun clear() {
        cache.evictAll()
    }
}

fun invalidateAppIconCache() {
    AppIconCache.clear()
    appIconCacheEpoch.incrementAndGet()
}

data class AppIconResult(
    val bitmap: ImageBitmap?,
    val isLegacy: Boolean,
    val monochromeData: ImageBitmap? = null,
)

/**
 * Loads an app icon from cache or package manager.
 * When [userHandleId] is set (work profile), loads the badged icon via LauncherApps.
 * Returns bitmap and whether the icon is legacy (non-adaptive); legacy icons may need circular clip.
 */
@Composable
fun rememberAppIcon(
    packageName: String,
    iconPackPackage: String? = null,
    userHandleId: Int? = null,
    forceCircularMask: Boolean = false,
): AppIconResult {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val cacheEpoch = appIconCacheEpoch.get()
    val cacheKey =
        buildCacheKey(
            packageName = packageName,
            iconPackPackage = iconPackPackage,
            userHandleId = userHandleId,
            cacheEpoch = cacheEpoch,
            forceCircularMask = forceCircularMask,
        )
    val cachedInitial = AppIconCache.get(cacheKey)

    val iconState =
        produceState<AppIconResult>(
            initialValue =
                cachedInitial?.let { AppIconResult(it.bitmap, it.isLegacy, it.monochromeData) }
                    ?: AppIconResult(null, false),
            key1 = cacheKey,
        ) {
            if (cachedInitial != null) {
                value = AppIconResult(cachedInitial.bitmap, cachedInitial.isLegacy, cachedInitial.monochromeData)
                return@produceState
            }

            val cached = AppIconCache.get(cacheKey)
            if (cached != null) {
                value = AppIconResult(cached.bitmap, cached.isLegacy, cached.monochromeData)
                return@produceState
            }

            val entry =
                withContext(Dispatchers.IO) {
                    if (userHandleId != null) {
                        loadWorkProfileBadgedIcon(
                            context = context,
                            packageName = packageName,
                            userHandleId = userHandleId,
                            densityDpi = densityDpi,
                            forceCircularMask = forceCircularMask,
                        )
                    } else {
                        val iconPackBitmap =
                            iconPackPackage?.let { pack ->
                                IconPackManager.loadIconBitmap(
                                    context = context,
                                    iconPackPackage = pack,
                                    targetPackage = packageName,
                                )
                            }

                        if (iconPackBitmap != null) {
                            AppIconEntry(iconPackBitmap, isLegacy = false)
                        } else {
                            runCatching {
                                val drawable = context.packageManager.getApplicationIcon(packageName)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                                    val bitmap =
                                        adaptiveToBitmap(
                                            drawable = drawable,
                                            forceCircularMask = forceCircularMask,
                                        ).asImageBitmap()
                                    val monochromeData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        extractMonochromeBitmap(drawable)?.asImageBitmap()
                                    } else null
                                    AppIconEntry(bitmap, isLegacy = false, monochromeData = monochromeData)
                                } else {
                                    val bitmap = drawable.toBitmap().asImageBitmap()
                                    AppIconEntry(bitmap, isLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                                }
                            }.getOrNull()
                        }
                    }
                }

            if (entry != null) {
                AppIconCache.put(cacheKey, entry)
                value = AppIconResult(entry.bitmap, entry.isLegacy, entry.monochromeData)
            }
        }

    return iconState.value
}

private fun loadWorkProfileBadgedIcon(
    context: Context,
    packageName: String,
    userHandleId: Int,
    densityDpi: Int,
    forceCircularMask: Boolean,
): AppIconEntry? {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
    val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager ?: return null
    val userHandle =
        UserHandleUtils.of(userHandleId)
            ?: userManager.userProfiles.find { UserHandleUtils.getIdentifier(it) == userHandleId }
    if (userHandle == null) return null
    val activityInfo = runCatching {
        launcherApps.getActivityList(packageName, userHandle).firstOrNull()
    }.getOrNull() ?: return null
    return runCatching {
        val drawable = activityInfo.getBadgedIcon(densityDpi)
        val isLegacy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            drawable !is AdaptiveIconDrawable
        } else {
            true // All icons are legacy on API < 26
        }
        val bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                adaptiveToBitmap(drawable = drawable, forceCircularMask = forceCircularMask).asImageBitmap()
            } else {
                drawable.toBitmap().asImageBitmap()
            }
        AppIconEntry(bitmap, isLegacy)
    }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun adaptiveToBitmap(
    drawable: AdaptiveIconDrawable,
    forceCircularMask: Boolean,
): Bitmap {
    val targetSize = maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight).coerceAtLeast(1)
    if (!forceCircularMask) {
        return drawable.toBitmap(width = targetSize, height = targetSize)
    }

    val composed = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val composedCanvas = Canvas(composed)
    drawable.background?.setBounds(0, 0, targetSize, targetSize)
    drawable.background?.draw(composedCanvas)
    drawable.foreground?.setBounds(0, 0, targetSize, targetSize)
    drawable.foreground?.draw(composedCanvas)

    val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val outputCanvas = Canvas(output)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.WHITE
        }
    val radius = targetSize / 2f
    outputCanvas.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val matrix =
        Matrix().apply {
            postScale(
                CircularAdaptiveIconContentScale,
                CircularAdaptiveIconContentScale,
                radius,
                radius,
            )
        }
    outputCanvas.drawBitmap(composed, matrix, paint)
    composed.recycle()

    return output
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun extractMonochromeBitmap(drawable: AdaptiveIconDrawable): Bitmap? {
    val monochrome = drawable.monochrome ?: return null
    val size = 108 // Standard adaptive icon grid size in dp units
    return runCatching {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        monochrome.setBounds(0, 0, size, size)
        monochrome.draw(canvas)
        bitmap
    }.getOrNull()
}

/**
 * Warms the in-memory icon cache for the provided package list.
 * Useful when an icon pack is applied so icons are ready before Compose draws them.
 */
suspend fun prefetchAppIcons(
    context: Context,
    packageNames: Collection<String>,
    iconPackPackage: String?,
    maxCount: Int = 30,
) {
    if (packageNames.isEmpty()) return

    val packagesToLoad =
        packageNames
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { pkg -> pkg to buildCacheKey(pkg, iconPackPackage) }
            .filter { (_, cacheKey) -> AppIconCache.get(cacheKey) == null }
            .take(maxCount)
            .toList()

    if (packagesToLoad.isEmpty()) return

    withContext(Dispatchers.IO) {
        packagesToLoad.forEach { (packageName, cacheKey) ->
            val iconPackBitmap =
                iconPackPackage?.let { pack ->
                    IconPackManager.loadIconBitmap(
                        context = context,
                        iconPackPackage = pack,
                        targetPackage = packageName,
                    )
                }

            val entry =
                if (iconPackBitmap != null) {
                    AppIconEntry(iconPackBitmap, isLegacy = false)
                } else {
                    runCatching {
                        val drawable = context.packageManager.getApplicationIcon(packageName)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                            val bitmap = drawable.toBitmap().asImageBitmap()
                            val monochromeData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                extractMonochromeBitmap(drawable)?.asImageBitmap()
                            } else null
                            AppIconEntry(bitmap, isLegacy = false, monochromeData = monochromeData)
                        } else {
                            val bitmap = drawable.toBitmap().asImageBitmap()
                            AppIconEntry(bitmap, isLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                        }
                    }.getOrNull()
                }

            if (entry != null) {
                AppIconCache.put(cacheKey, entry)
            }
        }
    }
}

private fun buildCacheKey(
    packageName: String,
    iconPackPackage: String?,
    userHandleId: Int? = null,
    cacheEpoch: Long = appIconCacheEpoch.get(),
    forceCircularMask: Boolean = false,
): String {
    val prefix = iconPackPackage ?: "system"
    val suffix = userHandleId?.let { ":work:$it" } ?: ""
    val shapeSuffix = if (forceCircularMask) ":circle" else ""
    return "$cacheEpoch:$prefix:$packageName$suffix$shapeSuffix"
}
