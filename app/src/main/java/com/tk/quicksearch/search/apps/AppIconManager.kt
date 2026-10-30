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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.utils.UserHandleUtils
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private data class AppIconEntry(
    val bitmap: ImageBitmap,
    val isLegacy: Boolean,
    val monochromeData: ImageBitmap? = null,
    val hasProfileBadge: Boolean = false,
)

// This process-wide value can be initialized by any surface while another surface is already
// recomposing. Keep it outside Compose's snapshot system so its first access cannot leak state
// created in one snapshot into another composition.
private val appIconCacheEpoch = MutableStateFlow(0L)

// Per-package epochs so a single app changing its icon (e.g. switching launcher activity-aliases)
// refreshes only that app's icons instead of every icon on screen.
private val packageIconEpochs = MutableStateFlow<Map<String, Long>>(emptyMap())

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
 * Icons are rasterized at the size the app grid draws them (the 64dp app icon surface scaled by
 * the user's icon size step) instead of their intrinsic size, so far more of them fit in
 * [AppIconCache] and long lists like the all apps dialog don't reload while scrolling. Smaller
 * steps keep the default size so the 64dp surfaces elsewhere stay sharp.
 */
private fun appIconBitmapSize(
    context: Context,
    userPreferences: UserAppPreferences,
): Int {
    val sizeScale = UiPreferences.appIconSizeScale(userPreferences.getAppIconSizeStep()).coerceAtLeast(1f)
    return kotlin.math.ceil(DesignTokens.AppIconSize.value * sizeScale * context.resources.displayMetrics.density)
        .toInt()
        .coerceIn(1, MaxAppIconBitmapSize)
}

/**
 * In-memory cache for app icons to avoid repeated loading.
 */
private object AppIconCache {
    private const val MIN_CACHE_SIZE_BYTES = 16L * 1024 * 1024
    private const val MAX_CACHE_SIZE_BYTES = 64L * 1024 * 1024
    private const val BYTES_PER_PIXEL = 4

    // Sized from the heap so a full app list stays cached on most devices without risking OOM.
    // Bitmap pixels live outside the Java heap from Android 8, so a larger share is safe there.
    private val cacheSizeBytes =
        (Runtime.getRuntime().maxMemory() / if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 4 else 8)
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

    fun remove(cacheKey: String) {
        cache.remove(cacheKey)
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

/** Refreshes [packageName]'s icons after the app changes its components, such as its launcher icon. */
fun invalidateAppIcon(packageName: String) {
    packageIconEpochs.update { epochs -> epochs + (packageName to (epochs[packageName] ?: 0L) + 1) }
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
    /** True when a work profile badge is drawn in the bitmap corner; any circle mask is pre-applied. */
    val hasProfileBadge: Boolean = false,
)

/**
 * Loads an app icon from cache or package manager.
 * When [userHandleId] is set (work profile), the icon (icon pack or system) gets the work badge
 * drawn after any circular mask, so the badge is not clipped by the circle.
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
    // One instance per icon; building UserAppPreferences on every recomposition showed up in typing profiles.
    val userPreferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val maskUnsupportedIconPackIcons =
        if (iconPackPackage == null) false
        else userPreferences.isIconPackUnsupportedIconMaskEnabled()
    val iconOverride = userPreferences.getAppIconOverride(packageName)
    val bitmapSize = appIconBitmapSize(context, userPreferences)
    val includeMonochrome = userPreferences.isThemedIconsEnabled()
    val cacheEpoch by appIconCacheEpoch.collectAsState()
    val packageEpochFlow =
        remember(packageName) {
            packageIconEpochs.map { it[packageName] ?: 0L }.distinctUntilChanged()
        }
    val packageEpoch by packageEpochFlow.collectAsState(initial = packageIconEpochs.value[packageName] ?: 0L)
    fun cacheKeyFor(monochrome: Boolean) =
        buildCacheKey(
            packageName = packageName,
            iconPackPackage = iconPackPackage,
            iconOverride = iconOverride,
            maskUnsupportedIconPackIcons = maskUnsupportedIconPackIcons,
            userHandleId = userHandleId,
            bitmapSize = bitmapSize,
            cacheEpoch = cacheEpoch,
            packageEpoch = packageEpoch,
            forceCircularMask = forceCircularMask,
            includeArchived = includeArchived,
            includeMonochrome = monochrome,
        )
    val cacheKey = cacheKeyFor(includeMonochrome)
    val cachedInitial = AppIconCache.get(cacheKey)
    // Toggling themed icons switches cache keys; the other variant keeps the icon on screen
    // instead of a placeholder, and already holds everything the non-themed variant needs.
    val alternateKey = cacheKeyFor(!includeMonochrome)
    val alternateEntry =
        if (cachedInitial == null) {
            AppIconCache.get(alternateKey)?.let { entry ->
                if (includeMonochrome) entry else entry.copy(monochromeData = null)
            }
        } else {
            null
        }

    val iconState =
        produceState<AppIconResult>(
            initialValue =
                (cachedInitial ?: alternateEntry)?.toResult()
                    ?: AppIconResult(null, false),
            key1 = cacheKey,
        ) {
            if (cachedInitial != null) {
                value = cachedInitial.toResult()
                return@produceState
            }

            val cached = AppIconCache.get(cacheKey)
            if (cached != null) {
                value = cached.toResult()
                return@produceState
            }

            if (!includeMonochrome && alternateEntry != null) {
                AppIconCache.put(cacheKey, alternateEntry)
                AppIconCache.remove(alternateKey)
                value = alternateEntry.toResult()
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
                        targetSize = bitmapSize,
                        forceCircularMask = forceCircularMask,
                        includeArchived = includeArchived,
                        includeMonochrome = includeMonochrome,
                    )
                }

            if (entry != null) {
                AppIconCache.put(cacheKey, entry)
                value = entry.toResult()
            }
        }

    return iconState.value
}

private fun AppIconEntry.toResult(): AppIconResult = AppIconResult(bitmap, isLegacy, monochromeData, hasProfileBadge)

private fun loadAppIconEntry(
    context: Context,
    packageName: String,
    iconPackPackage: String?,
    iconOverride: com.tk.quicksearch.search.data.preferences.AppIconOverride?,
    userHandleId: Int?,
    densityDpi: Int,
    targetSize: Int,
    forceCircularMask: Boolean,
    includeArchived: Boolean = true,
    includeMonochrome: Boolean = false,
): AppIconEntry? {
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
                loadBaseIcon =
                    userHandleId?.let { handleId ->
                        { loadWorkProfileActivityIcon(context, packageName, handleId, densityDpi) }
                    },
            )
        }

    return when {
        iconPackBitmap != null -> {
            val boundedIcon = iconPackBitmap.boundedTo(targetSize)
            if (userHandleId == null) {
                AppIconEntry(bitmap = boundedIcon, isLegacy = false)
            } else {
                workProfileIconEntry(
                    context = context,
                    icon = boundedIcon.asAndroidBitmap(),
                    userHandleId = userHandleId,
                    isLegacy = false,
                    applyCircleMask = forceCircularMask,
                )
            }
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
                includeMonochrome = includeMonochrome,
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

/**
 * Builds a work profile icon entry: [icon] is circle-cropped first when [applyCircleMask] is set,
 * then the work badge is drawn on top so it sits in the corner outside the circle.
 */
private fun workProfileIconEntry(
    context: Context,
    icon: Bitmap,
    userHandleId: Int,
    isLegacy: Boolean,
    applyCircleMask: Boolean,
): AppIconEntry {
    val shaped = if (applyCircleMask) icon.circleCropped() else icon
    return AppIconEntry(
        bitmap = addWorkProfileBadge(context, shaped, userHandleId).asImageBitmap(),
        isLegacy = isLegacy && !applyCircleMask,
        hasProfileBadge = true,
    )
}

private fun Bitmap.circleCropped(): Bitmap {
    val size = maxOf(width, height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, (size - width) / 2f, (size - height) / 2f, paint)
    return output
}

private fun addWorkProfileBadge(
    context: Context,
    icon: Bitmap,
    userHandleId: Int,
): Bitmap {
    val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager ?: return icon
    val userHandle =
        UserHandleUtils.of(userHandleId)
            ?: userManager.userProfiles.find { UserHandleUtils.getIdentifier(it) == userHandleId }
            ?: return icon
    return runCatching {
        val drawable = BitmapDrawable(context.resources, icon)
        context.packageManager
            .getUserBadgedIcon(drawable, userHandle)
            .toBitmap(width = icon.width, height = icon.height)
    }.getOrDefault(icon)
}

private fun loadSystemAppIcon(
    context: Context,
    packageName: String,
    densityDpi: Int,
    targetSize: Int,
    forceCircularMask: Boolean,
    includeArchived: Boolean,
    includeMonochrome: Boolean,
): AppIconEntry? =
    runCatching {
        // Prefer the launcher activity's icon: apps with switchable icons (Telegram, Reddit,
        // Discord) enable an activity-alias whose icon differs from the application icon.
        // Archived apps (Android 15+) are invisible to getApplicationIcon, but their launcher
        // activity still loads the icon the system kept at archive time.
        val drawable =
            loadLauncherActivityIcon(context, packageName, densityDpi, includeArchived)
                ?: context.packageManager.getApplicationIcon(packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val bitmap =
                adaptiveToBitmap(
                    drawable = drawable,
                    targetSize = targetSize,
                    forceCircularMask = forceCircularMask,
                ).asImageBitmap()
            // Only themed icons draw the monochrome layer, so skip rasterizing it otherwise.
            val monochromeData = if (includeMonochrome && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    includeArchived: Boolean,
): android.graphics.drawable.Drawable? {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
    return runCatching {
        launcherApps
            .getActivityList(packageName, android.os.Process.myUserHandle())
            .firstOrNull()
            ?.takeIf { includeArchived || !it.applicationInfo.isArchivedApp() }
            ?.getIcon(densityDpi)
    }.getOrNull()
}

private fun android.content.pm.ApplicationInfo.isArchivedApp(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && isArchived

private fun loadWorkProfileBadgedIcon(
    context: Context,
    packageName: String,
    userHandleId: Int,
    densityDpi: Int,
    targetSize: Int,
    forceCircularMask: Boolean,
): AppIconEntry? {
    // Load the unbadged icon so the circle mask is applied before the work badge is drawn.
    val drawable = loadWorkProfileActivityIcon(context, packageName, userHandleId, densityDpi) ?: return null
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            workProfileIconEntry(
                context = context,
                icon =
                    adaptiveToBitmap(
                        drawable = drawable,
                        targetSize = targetSize,
                        forceCircularMask = forceCircularMask,
                    ),
                userHandleId = userHandleId,
                isLegacy = false,
                applyCircleMask = false,
            )
        } else {
            workProfileIconEntry(
                context = context,
                icon = drawable.toBoundedBitmap(targetSize),
                userHandleId = userHandleId,
                isLegacy = true,
                applyCircleMask = forceCircularMask,
            )
        }
    }.getOrNull()
}

/** Loads the unbadged launcher icon of [packageName] as installed in the given work profile. */
private fun loadWorkProfileActivityIcon(
    context: Context,
    packageName: String,
    userHandleId: Int,
    densityDpi: Int,
): android.graphics.drawable.Drawable? {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
    val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager ?: return null
    val userHandle =
        UserHandleUtils.of(userHandleId)
            ?: userManager.userProfiles.find { UserHandleUtils.getIdentifier(it) == userHandleId }
            ?: return null
    return runCatching {
        launcherApps.getActivityList(packageName, userHandle).firstOrNull()?.getIcon(densityDpi)
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
    val bitmapSize = appIconBitmapSize(context, userPreferences)
    val includeMonochrome = userPreferences.isThemedIconsEnabled()

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
                        bitmapSize = bitmapSize,
                        forceCircularMask = forceCircularMask,
                        includeArchived = includeArchived,
                        includeMonochrome = includeMonochrome,
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
                                targetSize = bitmapSize,
                                forceCircularMask = forceCircularMask,
                                includeArchived = includeArchived,
                                includeMonochrome = includeMonochrome,
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
    maskUnsupportedIconPackIcons: Boolean = true,
    userHandleId: Int? = null,
    bitmapSize: Int,
    cacheEpoch: Long = appIconCacheEpoch.value,
    packageEpoch: Long = packageIconEpochs.value[packageName] ?: 0L,
    forceCircularMask: Boolean = false,
    includeArchived: Boolean = true,
    includeMonochrome: Boolean = false,
): String {
    val prefix = iconPackPackage ?: "system"
    val maskSuffix = if (iconPackPackage != null) ":mask:$maskUnsupportedIconPackIcons" else ""
    val suffix = userHandleId?.let { ":work:$it" } ?: ""
    val shapeSuffix = if (forceCircularMask) ":circle" else ""
    val overrideSuffix = iconOverride?.let { ":override:${it.iconPackPackage}:${it.drawableName}" }.orEmpty()
    val archivedSuffix = if (includeArchived) "" else ":installedOnly"
    val monochromeSuffix = if (includeMonochrome) ":mono" else ""
    return "$cacheEpoch:$packageEpoch:$bitmapSize:$prefix$maskSuffix:$packageName$suffix$shapeSuffix$overrideSuffix$archivedSuffix$monochromeSuffix"
}
