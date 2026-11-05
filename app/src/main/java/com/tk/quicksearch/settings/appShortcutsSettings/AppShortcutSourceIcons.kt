package com.tk.quicksearch.settings.appShortcutsSettings

import android.content.ComponentName
import android.content.pm.PackageManager
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

/** Icon of an [AppShortcutSource]; [bitmap] is null when loading finished without an icon. */
internal class ShortcutSourceIcon(
    val bitmap: ImageBitmap?,
)

private const val SOURCE_ICON_SIZE_PX = 96
private const val SOURCE_ICON_CACHE_ENTRIES = 160

// Icons are decoded only when a row is composed, so opening the page doesn't decode one per installed app.
private val sourceIconCache = LruCache<String, ShortcutSourceIcon>(SOURCE_ICON_CACHE_ENTRIES)

fun clearShortcutSourceIconMemoryCache() {
    sourceIconCache.evictAll()
}

/** Returns the source's icon, or null while it is still loading. */
@Composable
internal fun rememberShortcutSourceIcon(source: AppShortcutSource): ShortcutSourceIcon? {
    val packageManager = LocalContext.current.packageManager
    val cacheKey = source.iconCacheKey()
    val icon by
        produceState(initialValue = sourceIconCache.get(cacheKey), cacheKey) {
            if (value != null) return@produceState
            value =
                withContext(Dispatchers.IO) {
                    loadShortcutSourceIcon(packageManager, source).also { sourceIconCache.put(cacheKey, it) }
                }
        }
    return icon
}

private fun AppShortcutSource.iconCacheKey(): String = "$packageName:$className:${sourceType.name}"

private fun loadShortcutSourceIcon(
    packageManager: PackageManager,
    source: AppShortcutSource,
): ShortcutSourceIcon {
    val drawable =
        runCatching {
            when (source.sourceType) {
                AppShortcutSourceType.SHORTCUT_PROVIDER ->
                    packageManager.getActivityIcon(ComponentName(source.packageName, source.className))
                AppShortcutSourceType.APP_ACTIVITY -> packageManager.getApplicationIcon(source.packageName)
            }
        }.getOrNull()
    val bitmap =
        drawable
            ?.let { runCatching { it.toBitmap(width = SOURCE_ICON_SIZE_PX, height = SOURCE_ICON_SIZE_PX) }.getOrNull() }
            ?.asImageBitmap()
    return ShortcutSourceIcon(bitmap)
}
