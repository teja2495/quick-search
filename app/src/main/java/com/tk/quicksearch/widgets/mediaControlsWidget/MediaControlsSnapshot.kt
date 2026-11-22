package com.tk.quicksearch.widgets.mediaControlsWidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.media.AppSeekActions
import com.tk.quicksearch.media.MediaAppSeek
import com.tk.quicksearch.media.MediaSeek
import com.tk.quicksearch.search.data.MediaPlaybackRepository
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * What the Media Controls widget shows: the system-priority session, i.e. the same one its media
 * key buttons reach. Unlike the At a Glance card this ignores the "show now playing" toggle, so
 * turning that card off does not blank a widget the user placed on purpose.
 */
internal data class MediaControlsSnapshot(
    /** Without notification access nothing can be read, but the buttons still send media keys. */
    val hasAccess: Boolean,
    /** Null when nothing is playing. */
    val title: String?,
    /** Artist (or album, or the player's own subtitle) under the title; null when the session has none. */
    val subtitle: String? = null,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val canSeek: Boolean,
    val isSeekMode: Boolean,
    /** The app's own seek buttons; when set they replace rewind/forward and previous/next is hidden. */
    val appSeek: AppSeekActions? = null,
    /** [appSeek]'s icons from the media app's resources, rasterized at the control icon size. */
    val appSeekBackIcon: Bitmap? = null,
    val appSeekForwardIcon: Bitmap? = null,
    /** Identifies the session, so a dismissal or a pause from a widget applies to it alone. */
    val sessionToken: MediaSession.Token? = null,
    /** The session's [PlaybackState] code, or null when it reports none. */
    val playbackState: Int? = null,
) {
    val hasSession: Boolean
        get() = title != null
}

internal fun MediaPlaybackRepository.mediaControlsHasAccess(): Boolean = hasAccess()

internal fun readMediaControlsSnapshot(context: Context): MediaControlsSnapshot {
    val repository = MediaPlaybackRepository(context)
    val hasAccess = repository.mediaControlsHasAccess()
    if (!hasAccess) {
        // Nothing can be read without access; the buttons still send media keys.
        return MediaControlsSnapshot(
            hasAccess = false,
            title = null,
            albumArt = null,
            isPlaying = false,
            canSeek = true,
            isSeekMode = false,
        )
    }
    val controller =
        repository.priorityController()
            ?: return MediaControlsSnapshot(
                hasAccess = hasAccess,
                title = null,
                albumArt = null,
                isPlaying = false,
                // With no session to ask, keep the full row: seeks fall back to rewind/fast-forward keys.
                canSeek = true,
                isSeekMode = false,
            )
    val metadata = runCatching { controller.metadata }.getOrNull()
    val playbackState = runCatching { controller.playbackState }.getOrNull()
    val title =
        metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: runCatching {
                val packageManager = context.packageManager
                packageManager.getApplicationInfo(controller.packageName, 0).loadLabel(packageManager).toString()
            }.getOrNull()
            ?: controller.packageName
    // Same fallbacks as the At a Glance Now Playing card.
    val subtitle =
        listOf(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            MediaMetadata.METADATA_KEY_ALBUM,
        ).firstNotNullOfOrNull { key -> metadata?.getString(key)?.trim()?.takeIf { it.isNotEmpty() && it != title } }
    val appSeek = MediaAppSeek.find(playbackState)
    val iconSizePx =
        (MediaControlsWidgetDimens.CONTROL_ICON_SIZE.value * context.resources.displayMetrics.density)
            .roundToInt()
            .coerceAtLeast(1)
    fun appSeekIcon(action: AppSeekAction?): Bitmap? =
        action?.let { MediaAppSeek.loadIcon(context, controller.packageName, it.iconRes, iconSizePx) }
    return MediaControlsSnapshot(
        hasAccess = hasAccess,
        title = title,
        subtitle = subtitle,
        albumArt =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
        isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
        canSeek = MediaSeek.supportsSeek(playbackState),
        isSeekMode = MediaSeek.isSeekMode(metadata, playbackState),
        appSeek = appSeek,
        appSeekBackIcon = appSeekIcon(appSeek?.back),
        appSeekForwardIcon = appSeekIcon(appSeek?.forward),
        sessionToken = controller.sessionToken,
        playbackState = playbackState?.state,
    )
}

/**
 * Center-crops [source] into a [sizePx] square with rounded corners. Session artwork is often
 * 1000px or more, which would blow the widget host's bitmap budget, and Glance can only round
 * corners itself on Android 12+.
 */
internal fun roundedAlbumArt(
    source: Bitmap,
    sizePx: Int,
    cornerRadiusPx: Float,
): Bitmap? =
    runCatching {
        val software =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }
        val scale = max(sizePx / software.width.toFloat(), sizePx / software.height.toFloat())
        val shader =
            BitmapShader(software, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(
                    Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(
                            (sizePx - software.width * scale) / 2f,
                            (sizePx - software.height * scale) / 2f,
                        )
                    },
                )
            }
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        Canvas(output).drawRoundRect(
            RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
            cornerRadiusPx,
            cornerRadiusPx,
            paint,
        )
        output
    }.getOrNull()
