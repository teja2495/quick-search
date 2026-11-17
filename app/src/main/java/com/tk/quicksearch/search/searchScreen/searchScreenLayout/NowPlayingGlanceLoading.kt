package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.media.AppSeekActions
import com.tk.quicksearch.media.MediaAppSeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The parts of a playback state the Now Playing card reads: play state, supported actions and the
 * custom seek buttons. Position updates leave these unchanged, so they don't recompose the card;
 * seeking reads the controller directly.
 */
internal fun PlaybackState?.glanceFields(): List<Any?>? =
    this?.let { state ->
        listOf(
            state.state,
            state.actions,
            runCatching { state.customActions }.getOrNull().orEmpty().map {
                Triple(it.action, it.name?.toString(), it.icon)
            },
        )
    }

/** Seek button icons are loaded from the media app's resources, so keep them for the process. */
private val appSeekIconCache = LruCache<String, ImageBitmap>(16)

private fun appSeekIconKey(
    packageName: String,
    iconRes: Int,
    sizePx: Int,
) = "$packageName/$iconRes/$sizePx"

/**
 * The back and forward icons of [actions], loaded off the main thread. Icons seen before show at
 * once; a new app's icons are null for the moment they take to load.
 */
@Composable
internal fun rememberAppSeekIcons(
    packageName: String,
    actions: AppSeekActions?,
    sizePx: Int,
): List<ImageBitmap?>? {
    val context = LocalContext.current.applicationContext
    fun cached() =
        actions?.let {
            listOf(it.back, it.forward).map { action ->
                action?.let { a -> appSeekIconCache.get(appSeekIconKey(packageName, a.iconRes, sizePx)) }
            }
        }
    val icons by produceState(cached(), packageName, actions, sizePx) {
        value = cached()
        if (actions == null) return@produceState
        value =
            withContext(Dispatchers.IO) {
                listOf(actions.back, actions.forward).map { action ->
                    action?.let {
                        val key = appSeekIconKey(packageName, it.iconRes, sizePx)
                        appSeekIconCache.get(key)
                            ?: MediaAppSeek.loadIcon(context, packageName, it.iconRes, sizePx)?.asImageBitmap()
                                ?.also { icon -> appSeekIconCache.put(key, icon) }
                    }
                }
            }
    }
    return icons
}

/** [bitmap] scaled down so its shorter side is [sizePx], or as is when it is already that small. */
internal fun scaledAlbumArt(
    bitmap: Bitmap,
    sizePx: Int,
): ImageBitmap =
    runCatching {
        val shortest = minOf(bitmap.width, bitmap.height)
        if (shortest <= sizePx) return@runCatching bitmap
        val scale = sizePx.toFloat() / shortest
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }.getOrDefault(bitmap).asImageBitmap()
