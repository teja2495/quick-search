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
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.utils.UserHandleUtils
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private data class AppIconEntry(
    val bitmap: ImageBitmap,
    val isLegacy: Boolean,
    val monochromeData: ImageBitmap? = null,
)

// This process-wide value can be initialized by any surface while another surface is already
// recomposing. Keep it outside Compose's snapshot system so its first access cannot leak state
// created in one snapshot into another composition.
private val appIconCacheEpoch = MutableStateFlow(0L)

/**
 * Zoom factor when rasterizing adaptive icons for a circular mask. Keep this slightly above 1.0
 * so circular icons look full, but avoid excessive zoom that can clip edge glyphs on some apps.
 */
private const val CircularAdaptiveIconContentScale = 1.42f

/**
 * App icons are rendered at a small on-screen size. Keeping an APK's original icon bitmap can
 * exceed the hardware canvas texture limit on some devices, causing Compose to crash while it
 * records the draw pass.
 */
private const val MaxAppIconBitmapSize = 512

/**
 * Largest size an app icon is drawn at (64dp app icon surface at the max icon size step).
 * Icons are rasterized at this size instead of their intrinsic size so far more of them fit in
 * [AppIconCache], which keeps long lists like the all apps dialog from reloading while scrolling.
 */
private const val MaxAppIconDisplaySizeDp = 80f

private fun appIconBitmapSize(context: Context): Int =
    kotlin.math.ceil(MaxAppIconDisplaySizeDp * context.resources.displayMetrics.density)
        .toInt()
        .coerceIn(1, MaxAppIconBitmapSize)

/**
 * In-memory cache for app icons to avoid repeated loading.
 */
private object AppIconCache {
    private const val MIN_CACHE_SIZE_BYTES = 16L * 1024 * 1024
    private const val MAX_CACHE_SIZE_BYTES = 64L * 1024 * 1024
    private const val BYTES_PER_PIXEL = 4

    // Sized from the heap so a full app list stays cached on most devices without risking OOM.
    private val cacheSizeBytes =
        (Runtime.getRuntime().maxMemory() / 8)
            .coerceIn(MIN_CACHE_SIZE_BYTES, MAX_CACHE_SIZE_BYTES)
            .toInt()

    private val cache =
        object : LruCache<String, AppIconEntry>(cacheSizeBytes) {
            override fun sizeOf(
                key: String,
                value: AppIconEntry,
            ): Int {
                val bytes =
                    imageBitmapByteCount(value.bitmap) +
                        (value.monochromeData?.let(::imageBitmapByteCount) ?: 0L)
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

    private fun imageBitmapByteCount(bitmap: ImageBitmap): Long =
        bitmap.width.toLong() * bitmap.height.toLong() * BYTES_PER_PIXEL
}

fun invalidateAppIconCache() {
    AppIconCache.clear()
    appIconCacheEpoch.update { it + 1 }
}

/**
 * Evicts cached icon entries without replacing bitmaps already displayed by Compose.
 *
 * Memory callbacks use this path so a retained launcher surface does not briefly replace every
 * visible icon with its loading placeholder. Explicit icon changes should continue to call
 * [invalidateAppIconCache] so active icon states are refreshed.
 */
fun clearAppIconMemoryCache() {
    AppIconCache.clear()
}

data class AppIconResult(
    val bitmap: ImageBitmap?,
    val isLegacy: Boolean,
    val monochromeData: ImageBitmap? = null,
)

/**
 * Loads an app icon from cache or package manager.
 * When [userHandleId] is set (work profile), uses a themed icon only when the pack explicitly
 * supports the app; otherwise it preserves the system-badged fallback icon.
 * Returns bitmap and whether the icon is legacy (non-adaptive); legacy icons may need circular clip.
 * Set [includeArchived] to false to treat an archived app (Android 15+) as not installed.
 */
@Composable
fun rememberAppIcon(
    packageName: String,
    iconPackPackage: String? = null,
    userHandleId: Int? = null,
    forceCircularMask: Boolean = false,
    includeArchived: Boolean = true,
): AppIconResult {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val maskUnsupportedIconPackIcons =
        if (iconPackPackage == null) false
        else UserAppPreferences(context).isIconPackUnsupportedIconMaskEnabled()
    val iconOverride = UserAppPreferences(context).getAppIconOverride(packageName)
    val cacheEpoch by appIconCacheEpoch.collectAsState()
    val cacheKey =
        buildCacheKey(
            packageName = packageName,
            iconPackPackage = iconPackPackage,
            iconOverride = iconOverride,
            maskUnsupportedIconPackIcons = maskUnsupportedIconPackIcons,
            userHandleId = userHandleId,
            cacheEpoch = cacheEpoch,
            forceCircularMask = forceCircularMask,
            includeArchived = includeArchived,
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
                    loadAppIconEntry(
                        context = context,
                        packageName = packageName,
                        iconPackPackage = iconPackPackage,
                        iconOverride = iconOverride,
                        userHandleId = userHandleId,
                        densityDpi = densityDpi,
                        forceCircularMask = forceCircularMask,
                        includeArchived = includeArchived,
                    )
                }

            if (entry != null) {
                AppIconCache.put(cacheKey, entry)
                value = AppIconResult(entry.bitmap, entry.isLegacy, entry.monochromeData)
            }
        }

    return iconState.value
}

private fun loadAppIconEntry(
    context: Context,
    packageName: String,
    iconPackPackage: String?,
    iconOverride: com.tk.quicksearch.search.data.preferences.AppIconOverride?,
    userHandleId: Int?,
    densityDpi: Int,
    forceCircularMask: Boolean,
    includeArchived: Boolean = true,
): AppIconEntry? {
    val targetSize = appIconBitmapSize(context)
    val iconPackBitmap =
        iconOverride?.takeUnless { it.useSystemDefault }?.let { override ->
            IconPackManager.loadDrawableBitmap(
                context = context,
                iconPackPackage = requireNotNull(override.iconPackPackage),
                drawableName = requireNotNull(override.drawableName),
            )
        } ?: iconPackPackage?.takeUnless { iconOverride?.useSystemDefault == true }?.let { pack ->
            IconPackManager.loadIconBitmap(
                context = context,
                iconPackPackage = pack,
                targetPackage = packageName,
            )
        }
    val hasExplicitIconPackIcon =
        iconOverride?.useSystemDefault == false ||
            iconPackPackage?.let { pack ->
                IconPackManager.hasExplicitIcon(context, pack, packageName)
            } == true

    return when {
        iconPackBitmap != null && (userHandleId == null || hasExplicitIconPackIcon) -> {
            val boundedIcon = iconPackBitmap.boundedTo(targetSize)
            AppIconEntry(
                bitmap =
                    userHandleId?.let { handleId ->
                        addWorkProfileBadge(
                            context = context,
                            icon = boundedIcon,
                            userHandleId = handleId,
                        )
                    } ?: boundedIcon,
                isLegacy = false,
            )
        }
        userHandleId != null ->
            loadWorkProfileBadgedIcon(
                context = context,
                packageName = packageName,
                userHandleId = userHandleId,
                densityDpi = densityDpi,
                targetSize = targetSize,
                forceCircularMask = forceCircularMask,
            )
        else ->
            loadSystemAppIcon(
                context = context,
                packageName = packageName,
                densityDpi = densityDpi,
                targetSize = targetSize,
                forceCircularMask = forceCircularMask,
                includeArchived = includeArchived,
            )
    }
}

private fun ImageBitmap.boundedTo(targetSize: Int): ImageBitmap {
    if (maxOf(width, height) <= targetSize) return this
    val scale = targetSize.toFloat() / maxOf(width, height)
    return Bitmap
        .createScaledBitmap(
            asAndroidBitmap(),
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        ).asImageBitmap()
}

private fun addWorkProfileBadge(
    context: Context,
    icon: ImageBitmap,
    userHandleId: Int,
): ImageBitmap {
    val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager ?: return icon
    val userHandle =
        UserHandleUtils.of(userHandleId)
            ?: userManager.userProfiles.find { UserHandleUtils.getIdentifier(it) == userHandleId }
            ?: return icon
    return runCatching {
        val drawable = BitmapDrawable(context.resources, icon.asAndroidBitmap())
        context.packageManager
            .getUserBadgedIcon(drawable, userHandle)
            .toBitmap(width = icon.width, height = icon.height)
            .asImageBitmap()
    }.getOrDefault(icon)
}

private fun loadSystemAppIcon(
    context: Context,
    packageName: String,
    densityDpi: Int,
    targetSize: Int,
    forceCircularMask: Boolean,
    includeArchived: Boolean,
): AppIconEntry? =
    runCatching {
        val drawable =
            runCatching { context.packageManager.getApplicationIcon(packageName) }
                .getOrElse { error ->
                    // Archived apps (Android 15+) are invisible to getApplicationIcon, but their
                    // launcher activity still loads the icon the system kept at archive time.
                    if (!includeArchived) throw error
                    loadLauncherActivityIcon(context, packageName, densityDpi) ?: throw error
                }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val bitmap =
                adaptiveToBitmap(
                    drawable = drawable,
                    targetSize = targetSize,
                    forceCircularMask = forceCircularMask,
                ).asImageBitmap()
            val monochromeData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extractMonochromeBitmap(drawable)?.asImageBitmap()
            } else null
            AppIconEntry(bitmap, isLegacy = false, monochromeData = monochromeData)
        } else {
            val bitmap = drawable.toBoundedBitmap(targetSize).asImageBitmap()
            AppIconEntry(bitmap, isLegacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        }
    }.getOrNull()

private fun loadLauncherActivityIcon(
    context: Context,
    packageName: String,
    densityDpi: Int,
): android.graphics.drawable.Drawable? {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
    return runCatching {
        launcherApps
            .getActivityList(packageName, android.os.Process.myUserHandle())
            .firstOrNull()
            ?.getIcon(densityDpi)
    }.getOrNull()
}

private fun loadWorkProfileBadgedIcon(
    context: Context,
    packageName: String,
    userHandleId: Int,
    densityDpi: Int,
    targetSize: Int,
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
                adaptiveToBitmap(
                    drawable = drawable,
                    targetSize = targetSize,
                    forceCircularMask = forceCircularMask,
                ).asImageBitmap()
            } else {
                drawable.toBoundedBitmap(targetSize).asImageBitmap()
            }
        AppIconEntry(bitmap, isLegacy)
    }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun adaptiveToBitmap(
    drawable: AdaptiveIconDrawable,
    targetSize: Int,
    forceCircularMask: Boolean,
): Bitmap {
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

private fun android.graphics.drawable.Drawable.toBoundedBitmap(maxSize: Int): Bitmap {
    val targetSize =
        maxOf(intrinsicWidth, intrinsicHeight)
            .coerceIn(1, maxSize)
    return toBitmap(width = targetSize, height = targetSize)
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

data class AppIconRequest(
    val packageName: String,
    val userHandleId: Int? = null,
)

/**
 * Warms the in-memory icon cache for the provided package list.
 * Useful when an icon pack is applied so icons are ready before Compose draws them.
 */
suspend fun prefetchAppIcons(
    context: Context,
    packageNames: Collection<String>,
    iconPackPackage: String?,
    maxCount: Int = 30,
    forceCircularMask: Boolean = false,
    includeArchived: Boolean = true,
) {
    prefetchAppIconRequests(
        context = context,
        requests = packageNames.map { AppIconRequest(it.trim()) },
        iconPackPackage = iconPackPackage,
        maxCount = maxCount,
        forceCircularMask = forceCircularMask,
        includeArchived = includeArchived,
    )
}

/**
 * Warms the in-memory icon cache for [requests], including work profile apps, using the same
 * cache keys as [rememberAppIcon].
 */
suspend fun prefetchAppIconRequests(
    context: Context,
    requests: Collection<AppIconRequest>,
    iconPackPackage: String?,
    maxCount: Int = requests.size,
    forceCircularMask: Boolean = false,
    parallelism: Int = 1,
    includeArchived: Boolean = true,
) {
    if (requests.isEmpty()) return
    val userPreferences = UserAppPreferences(context)
    val maskUnsupportedIconPackIcons =
        if (iconPackPackage == null) false
        else userPreferences.isIconPackUnsupportedIconMaskEnabled()
    val densityDpi = context.resources.displayMetrics.densityDpi

    val requestsToLoad =
        requests
            .asSequence()
            .filter { it.packageName.isNotEmpty() }
            .distinct()
            .map { request ->
                val iconOverride = userPreferences.getAppIconOverride(request.packageName)
                Triple(
                    request,
                    buildCacheKey(
                        packageName = request.packageName,
                        iconPackPackage = iconPackPackage,
                        iconOverride = iconOverride,
                        maskUnsupportedIconPackIcons = maskUnsupportedIconPackIcons,
                        userHandleId = request.userHandleId,
                        forceCircularMask = forceCircularMask,
                        includeArchived = includeArchived,
                    ),
                    iconOverride,
                )
            }
            .filter { (_, cacheKey, _) -> AppIconCache.get(cacheKey) == null }
            .take(maxCount)
            .toList()

    if (requestsToLoad.isEmpty()) return

    withContext(Dispatchers.IO) {
        requestsToLoad
            .chunked(((requestsToLoad.size + parallelism - 1) / parallelism).coerceAtLeast(1))
            .map { chunk ->
                async {
                    chunk.forEach { (request, cacheKey, iconOverride) ->
                        ensureActive()
                        if (AppIconCache.get(cacheKey) != null) return@forEach
                        val entry =
                            loadAppIconEntry(
                                context = context,
                                packageName = request.packageName,
                                iconPackPackage = iconPackPackage,
                                iconOverride = iconOverride,
                                userHandleId = request.userHandleId,
                                densityDpi = densityDpi,
                                forceCircularMask = forceCircularMask,
                                includeArchived = includeArchived,
                            )
                        if (entry != null) {
                            AppIconCache.put(cacheKey, entry)
                        }
                    }
                }
            }.awaitAll()
    }
}

private fun buildCacheKey(
    packageName: String,
    iconPackPackage: String?,
    iconOverride: com.tk.quicksearch.search.data.preferences.AppIconOverride? = null,
    maskUnsupportedIconPackIcons: Boolean = false,
    userHandleId: Int? = null,
    cacheEpoch: Long = appIconCacheEpoch.value,
    forceCircularMask: Boolean = false,
    includeArchived: Boolean = true,
): String {
    val prefix = iconPackPackage ?: "system"
    val maskSuffix = if (iconPackPackage != null) ":mask:$maskUnsupportedIconPackIcons" else ""
    val suffix = userHandleId?.let { ":work:$it" } ?: ""
    val shapeSuffix = if (forceCircularMask) ":circle" else ""
    val overrideSuffix = iconOverride?.let { ":override:${it.iconPackPackage}:${it.drawableName}" }.orEmpty()
    val archivedSuffix = if (includeArchived) "" else ":installedOnly"
    return "$cacheEpoch:$prefix$maskSuffix:$packageName$suffix$shapeSuffix$overrideSuffix$archivedSuffix"
}
