package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun ShortcutIcon(
    icon: ImageBitmap?,
    displayName: String,
    size: Dp,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            val fallback =
                displayName
                    .trim()
                    .take(1)
                    .uppercase(Locale.getDefault())
                    .ifBlank { "?" }
            Text(
                text = fallback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun rememberShortcutIcon(
    shortcut: StaticShortcut,
    iconSizePx: Int,
): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = remember(shortcut, iconSizePx) { shortcutIconCacheKey(shortcut, iconSizePx) }
    val cachedIcon = remember(cacheKey) { ShortcutIconMemoryCache.get(cacheKey) }
    // Keyed per shortcut: shortcuts of one app can share every icon source field, so a slot
    // reused by a different shortcut (e.g. after one is disabled) must still reload.
    val iconState =
        produceState(initialValue = cachedIcon, key1 = cacheKey) {
            value = cachedIcon
            value =
                withContext(Dispatchers.IO) {
                    loadShortcutIconBitmap(
                        context = context,
                        shortcut = shortcut,
                        iconSizePx = iconSizePx,
                    )
                }
        }
    return iconState.value
}

/** Blocking variant of [rememberShortcutIcon] for non-Compose surfaces such as notifications. */
fun loadShortcutIconAndroidBitmap(
    context: Context,
    shortcut: StaticShortcut,
    iconSizePx: Int,
): Bitmap? = loadShortcutIconBitmap(context, shortcut, iconSizePx)?.asAndroidBitmap()

private fun loadShortcutIconBitmap(
    context: Context,
    shortcut: StaticShortcut,
    iconSizePx: Int,
): ImageBitmap? {
    val cacheKey = shortcutIconCacheKey(shortcut, iconSizePx)
    ShortcutIconMemoryCache.get(cacheKey)?.let { return it }

    val embedded = shortcut.iconBase64?.takeIf { it.isNotBlank() }
    if (embedded != null) {
        val decoded = kotlin.runCatching { Base64.decode(embedded, Base64.DEFAULT) }.getOrNull()
        val bitmap =
            decoded?.let { bytes -> decodeScaledBitmap(bytes, iconSizePx) }
                ?.takeUnless { it.isFullyTransparent() }
        return bitmap?.asImageBitmap()?.also { ShortcutIconMemoryCache.put(cacheKey, it) }
    }

    loadLauncherAppsShortcutIcon(context, shortcut, iconSizePx)?.let { icon ->
        ShortcutIconMemoryCache.put(cacheKey, icon)
        return icon
    }

    val resId = shortcut.iconResId ?: return null
    val targetContext =
        kotlin.runCatching { context.createPackageContext(shortcut.packageName, 0) }.getOrNull()
            ?: return null
    // Shortcut drawables often reference theme attributes, which only resolve against the
    // owning app's theme; the bare package context theme leaves them transparent.
    val appTheme = targetContext.applicationInfo.theme
    if (appTheme != 0) {
        kotlin.runCatching { targetContext.setTheme(appTheme) }
    }

    val drawable =
        kotlin.runCatching { targetContext.resources.getDrawable(resId, targetContext.theme) }
            .getOrNull()
            ?: return null

    val sizePx = iconSizePx.coerceAtLeast(1)
    return kotlin.runCatching { drawable.toBitmap(width = sizePx, height = sizePx) }
        .getOrNull()
        ?.takeUnless { it.isFullyTransparent() }
        ?.asImageBitmap()
        ?.also { ShortcutIconMemoryCache.put(cacheKey, it) }
}

internal fun Bitmap.isFullyTransparent(): Boolean {
    if (!hasAlpha()) return false
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels.all { pixel -> pixel ushr 24 == 0 }
}

private fun shortcutIconCacheKey(shortcut: StaticShortcut, iconSizePx: Int): String {
    val launcherIntent = shortcut.launcherAppsIntent()
    val launcherId =
        launcherIntent?.let { intent ->
            listOf(
                intent.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_PACKAGE),
                intent.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_ID),
                intent.getLongExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_USER_SERIAL, -1L),
            ).joinToString(":")
        }
    return listOf(
        shortcut.packageName,
        shortcut.id,
        launcherId.orEmpty(),
        shortcut.iconResId ?: 0,
        shortcut.iconBase64?.hashCode() ?: 0,
        iconSizePx,
    ).joinToString("|")
}

private fun StaticShortcut.launcherAppsIntent() =
    intents.firstOrNull { intent -> intent.action == ACTION_LAUNCHER_APPS_SHORTCUT }

private fun decodeScaledBitmap(bytes: ByteArray, iconSizePx: Int): Bitmap? {
    val targetSize = iconSizePx.coerceAtLeast(1)
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
        }
    val sampleSize =
        calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetSize = targetSize,
        )
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
    if (width <= targetSize && height <= targetSize) return 1
    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= targetSize && halfHeight / sampleSize >= targetSize) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun loadLauncherAppsShortcutIcon(
    context: Context,
    shortcut: StaticShortcut,
    iconSizePx: Int,
): ImageBitmap? {
    val launcherIntent = shortcut.launcherAppsIntent() ?: return null
    val packageName =
        launcherIntent.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_PACKAGE)
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val shortcutId =
        launcherIntent.getStringExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val userSerial = launcherIntent.getLongExtra(EXTRA_LAUNCHER_APPS_SHORTCUT_USER_SERIAL, -1L)
    val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
    if (!launcherApps.hasShortcutHostPermission()) return null

    val userHandle =
        runCatching {
            val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
            launcherApps.profiles.firstOrNull { profile ->
                userManager.getSerialNumberForUser(profile) == userSerial
            } ?: launcherApps.profiles.firstOrNull()
        }.getOrNull() ?: return null

    val query =
        LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setShortcutIds(listOf(shortcutId))
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        }
    val info: ShortcutInfo =
        runCatching { launcherApps.getShortcuts(query, userHandle) }
            .getOrNull()
            .orEmpty()
            .firstOrNull { it.`package` == packageName && it.id == shortcutId && it.isEnabled }
            ?: return null
    val density = context.resources.displayMetrics.densityDpi
    val drawable =
        runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
            ?: return null
    val sizePx = iconSizePx.coerceAtLeast(1)
    return runCatching { drawable.toBitmap(width = sizePx, height = sizePx) }
        .getOrNull()
        ?.takeUnless { it.isFullyTransparent() }
        ?.asImageBitmap()
}

private object ShortcutIconMemoryCache {
    private const val MAX_CACHE_SIZE_BYTES = 4 * 1024 * 1024
    private const val BYTES_PER_PIXEL = 4

    private val cache =
        object : LruCache<String, ImageBitmap>(MAX_CACHE_SIZE_BYTES) {
            override fun sizeOf(key: String, value: ImageBitmap): Int =
                (value.width.toLong() * value.height.toLong() * BYTES_PER_PIXEL)
                    .coerceIn(1, Int.MAX_VALUE.toLong())
                    .toInt()
        }

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, icon: ImageBitmap) {
        synchronized(cache) { cache.put(key, icon) }
    }

    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }
}

fun clearShortcutIconMemoryCache() {
    ShortcutIconMemoryCache.clear()
}
