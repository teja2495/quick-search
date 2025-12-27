package com.tk.quicksearch.search

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.search.IconPackManager

/**
 * In-memory cache for app icons to avoid repeated loading.
 */
private object AppIconCache {
    private const val MAX_CACHE_SIZE_BYTES = 8 * 1024 * 1024 // 8 MB cap to avoid OOM
    private const val BYTES_PER_PIXEL = 4

    private val cache = object : LruCache<String, ImageBitmap>(MAX_CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            val bytes = value.width.toLong() * value.height.toLong() * BYTES_PER_PIXEL
            // LruCache uses Int units; guard against overflow and zero-sized bitmaps.
            return bytes.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
        }
    }

    fun get(cacheKey: String): ImageBitmap? = cache.get(cacheKey)

    fun put(cacheKey: String, bitmap: ImageBitmap?) {
        if (bitmap != null) {
            cache.put(cacheKey, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}

/**
 * Loads an app icon from cache or package manager.
 * Returns a State that holds the ImageBitmap, or null if the icon cannot be loaded.
 */
@Composable
fun rememberAppIcon(
    packageName: String,
    iconPackPackage: String? = null
): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = buildCacheKey(packageName, iconPackPackage)
    
    val iconState = produceState<ImageBitmap?>(
        initialValue = AppIconCache.get(cacheKey),
        key1 = packageName,
        key2 = iconPackPackage
    ) {
        // Check cache first
        val cached = AppIconCache.get(cacheKey)
        if (cached != null) {
            value = cached
            return@produceState
        }

        // Load from package manager on IO thread
        val bitmap = withContext(Dispatchers.IO) {
            // Try icon pack first if selected; fall back to system icon if not found.
            val iconPackBitmap = iconPackPackage?.let { pack ->
                IconPackManager.loadIconBitmap(
                    context = context,
                    iconPackPackage = pack,
                    targetPackage = packageName
                )
            }

            iconPackBitmap ?: runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }

        // Cache and return the result
        if (bitmap != null) {
            AppIconCache.put(cacheKey, bitmap)
        }
        value = bitmap
    }
    
    return iconState.value
}

/**
 * Clears all cached icons, including icon pack resources.
 */
fun clearAppIconCaches() {
    AppIconCache.clear()
    IconPackManager.clearAllCaches()
}

/**
 * Warms the in-memory icon cache for the provided package list.
 * Useful when an icon pack is applied so icons are ready before Compose draws them.
 */
suspend fun prefetchAppIcons(
    context: Context,
    packageNames: Collection<String>,
    iconPackPackage: String?,
    maxCount: Int = 30
) {
    if (packageNames.isEmpty()) return

    val packagesToLoad = packageNames
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
            val iconPackBitmap = iconPackPackage?.let { pack ->
                IconPackManager.loadIconBitmap(
                    context = context,
                    iconPackPackage = pack,
                    targetPackage = packageName
                )
            }

            val bitmap = iconPackBitmap ?: runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()

            if (bitmap != null) {
                AppIconCache.put(cacheKey, bitmap)
            }
        }
    }
}

private fun buildCacheKey(packageName: String, iconPackPackage: String?): String {
    val prefix = iconPackPackage ?: "system"
    return "$prefix:$packageName"
}
