package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.ui.graphics.Color
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

/** How strongly the album art's color washes the card's start edge; faint so text keeps its contrast. */
private const val AlbumArtTintAlpha = 0.28f

/**
 * A wash of [albumArt]'s main color that fades out across the card, or null when the art is
 * missing or colorless. Computed off the main thread, once per bitmap.
 */
@Composable
internal fun rememberAlbumArtTint(albumArt: Bitmap?): Brush {
    // Keeps the previous color while the next one is computed, so the tint blends rather than blinking out.
    val color by produceState<Color?>(null, albumArt) {
        value = albumArt?.let { withContext(Dispatchers.Default) { albumArtAccent(it) } }
    }
    // The tint follows the art: it eases in once computed and blends into the next track's color.
    val animated by animateColorAsState(
        targetValue = color?.copy(alpha = AlbumArtTintAlpha) ?: Color.Transparent,
        animationSpec = tween(AppMotion.DurationLarge),
        label = "albumArtTint",
    )
    return Brush.horizontalGradient(listOf(animated, Color.Transparent))
}

/** Below this saturation the art reads as grey, and a tint would only muddy the card. */
private const val AlbumArtMinSaturation = 0.15f

/**
 * The saturation-weighted average color of [bitmap], kept to a mid lightness so it tints rather
 * than glares, or null when the art is colorless. Reads pixels, so call it off the main thread.
 */
private fun albumArtAccent(bitmap: Bitmap): Color? =
    runCatching {
        val sample = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val hsv = FloatArray(3)
        var weightSum = 0f
        var x = 0f
        var y = 0f
        var saturation = 0f
        for (px in 0 until sample.width) {
            for (py in 0 until sample.height) {
                android.graphics.Color.colorToHSV(sample.getPixel(px, py), hsv)
                // Vivid, bright pixels decide the hue; dark and grey ones barely count.
                val weight = hsv[1] * hsv[2]
                val radians = Math.toRadians(hsv[0].toDouble())
                x += (kotlin.math.cos(radians) * weight).toFloat()
                y += (kotlin.math.sin(radians) * weight).toFloat()
                saturation += hsv[1] * weight
                weightSum += weight
            }
        }
        if (sample !== bitmap) sample.recycle()
        if (weightSum <= 0f) return@runCatching null
        val meanSaturation = saturation / weightSum
        if (meanSaturation < AlbumArtMinSaturation) return@runCatching null
        val hue = ((Math.toDegrees(kotlin.math.atan2(y, x).toDouble()) + 360.0) % 360.0).toFloat()
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, meanSaturation.coerceIn(0.35f, 0.7f), 0.8f)))
    }.getOrNull()
