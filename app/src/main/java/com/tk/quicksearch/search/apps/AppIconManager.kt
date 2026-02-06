package com.tk.quicksearch.search.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
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
)

private val appIconCacheEpoch = AtomicLong(0L)

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
): AppIconResult {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val cacheEpoch = appIconCacheEpoch.get()
    val cacheKey = buildCacheKey(packageName, iconPackPackage, userHandleId, cacheEpoch)
    val cachedInitial = AppIconCache.get(cacheKey)

    val iconState =
        produceState<AppIconResult>(
            initialValue =
                cachedInitial?.let { AppIconResult(it.bitmap, it.isLegacy) }
                    ?: AppIconResult(null, false),
            key1 = cacheKey,
        ) {
            if (cachedInitial != null) {
                value = AppIconResult(cachedInitial.bitmap, cachedInitial.isLegacy)
                return@produceState
            }

            val cached = AppIconCache.get(cacheKey)
            if (cached != null) {
                value = AppIconResult(cached.bitmap, cached.isLegacy)
                return@produceState
            }

            val entry =
                withContext(Dispatchers.IO) {
                    if (userHandleId != null) {
                        loadWorkProfileBadgedIcon(context, packageName, userHandleId, densityDpi)
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
                                val isLegacy = drawable !is AdaptiveIconDrawable
                                val bitmap = drawable.toBitmap().asImageBitmap()
                                AppIconEntry(bitmap, isLegacy)
                            }.getOrNull()
                        }
                    }
                }

            if (entry != null) {
                AppIconCache.put(cacheKey, entry)
                value = AppIconResult(entry.bitmap, entry.isLegacy)
            }
        }

    return iconState.value
}

private fun loadWorkProfileBadgedIcon(
    context: Context,
    packageName: String,
    userHandleId: Int,
    densityDpi: Int,
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
        val isLegacy = drawable !is AdaptiveIconDrawable
        val bitmap = drawable.toBitmap().asImageBitmap()
        AppIconEntry(bitmap, isLegacy)
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
                        val isLegacy = drawable !is AdaptiveIconDrawable
                        val bitmap = drawable.toBitmap().asImageBitmap()
                        AppIconEntry(bitmap, isLegacy)
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
): String {
    val prefix = iconPackPackage ?: "system"
    val suffix = userHandleId?.let { ":work:$it" } ?: ""
    return "$cacheEpoch:$prefix:$packageName$suffix"
}
