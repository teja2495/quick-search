package com.tk.quicksearch.widgets.mediaControlsWidget

import androidx.compose.foundation.layout.BoxWithConstraints
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.search.data.MediaPlaybackRepository
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.utils.WidgetPreferences

private val PreviewWallpaperPadding = 16.dp

/** The settings screen shows the card as a miniature so it does not crowd out the settings. */
private const val PreviewScale = 0.8f

/**
 * Settings-screen preview of [MediaControlsWidget], drawn over the wallpaper like the other widget
 * previews. It tracks whatever is playing live so the preview matches the placed widget.
 */
@Composable
fun MediaControlsWidgetPreview(
    state: WidgetPreferences,
    wallpaperBitmap: ImageBitmap?,
) {
    val media = rememberLiveMediaControlsSnapshot()
    val colors = state.mediaControlsColors()
    val shape = RoundedCornerShape(state.borderRadiusDp.dp)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Explicit height: paint() without intrinsic sizing expands to the max constraints.
                .height(MediaControlsWidgetDimens.HEIGHT_DP.dp * PreviewScale + PreviewWallpaperPadding * 2)
                .clip(AppShapes.large)
                .then(
                    wallpaperBitmap?.let { bitmap ->
                        Modifier.paint(
                            painter = BitmapPainter(bitmap),
                            sizeToIntrinsics = false,
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Modifier,
                ).padding(PreviewWallpaperPadding),
        contentAlignment = Alignment.Center,
    ) {
        // Scaling density shrinks every dp and sp of the card uniformly, keeping its proportions.
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * PreviewScale, density.fontScale),
        ) {
            PreviewCard(state, media, colors, shape)
        }
    }
}

@Composable
private fun PreviewCard(
    state: WidgetPreferences,
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
    shape: Shape,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
            .height(MediaControlsWidgetDimens.HEIGHT_DP.dp)
            .background(colors.background.copy(alpha = state.backgroundAlpha), shape),
        contentAlignment = Alignment.Center,
    ) {
        val horizontalPadding = MediaControlsWidgetDimens.horizontalPadding(maxWidth)
        val showExtraControls = MediaControlsWidgetDimens.canFitExtraControls(maxWidth - horizontalPadding * 2)
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = horizontalPadding,
                vertical = MediaControlsWidgetDimens.CONTENT_PADDING_VERTICAL,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(MediaControlsWidgetDimens.TITLE_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MediaControlsWidgetDimens.ART_SPACING),
            ) {
                PreviewAlbumArt(media, colors, MediaControlsWidgetDimens.albumArtCornerRadius(state.borderRadiusDp))
                Text(
                    text = media.title ?: stringResource(
                        if (media.hasAccess) R.string.widget_media_controls_nothing_playing else R.string.widget_media_controls_grant_access,
                    ),
                    color = colors.content,
                    fontSize = MediaControlsWidgetDimens.TITLE_FONT_SIZE,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PreviewTransportRow(media, colors, showExtraControls)
        }
    }
}

/** Re-reads the snapshot whenever the priority session, its playback state or its track changes. */
@Composable
private fun rememberLiveMediaControlsSnapshot(): MediaControlsSnapshot {
    val context = LocalContext.current
    val repository = remember(context) { MediaPlaybackRepository(context) }
    var revision by remember { mutableIntStateOf(0) }
    var controller by remember { mutableStateOf(repository.priorityController()) }
    val hasAccess = rememberMediaControlsAccess()

    // Keyed on access: the sessions listener can only be registered once access is granted.
    DisposableEffect(repository, hasAccess) {
        controller = repository.priorityController()
        revision++
        val listener =
            repository.registerSessionsListener {
                controller = repository.priorityController()
                revision++
            }
        onDispose { repository.unregisterSessionsListener(listener) }
    }

    DisposableEffect(controller?.sessionToken) {
        val current = controller ?: return@DisposableEffect onDispose {}
        val callback =
            object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    revision++
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    revision++
                }

                override fun onSessionDestroyed() {
                    controller = repository.priorityController()
                    revision++
                }
            }
        runCatching { current.registerCallback(callback) }
        onDispose { runCatching { current.unregisterCallback(callback) } }
    }

    return remember(revision) { readMediaControlsSnapshot(context) }
}

@Composable
private fun PreviewAlbumArt(
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
    cornerRadius: Dp,
) {
    val artShape = RoundedCornerShape(cornerRadius)
    val art = remember(media.albumArt) { media.albumArt?.asImageBitmap() }
    if (art != null) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(MediaControlsWidgetDimens.ALBUM_ART_SIZE).clip(artShape),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(MediaControlsWidgetDimens.ALBUM_ART_SIZE)
                    .background(colors.artPlaceholder, artShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_media_controls_music_note),
                contentDescription = null,
                tint = colors.artPlaceholderIcon,
                modifier = Modifier.size(MediaControlsWidgetDimens.ART_PLACEHOLDER_ICON_SIZE),
            )
        }
    }
}

private class PreviewControl(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    /** An app seek button's own icon and label (see [MediaControlsSnapshot.appSeek]). */
    val appIcon: ImageBitmap? = null,
    val appLabel: String? = null,
)

@Composable
private fun rememberSeekControl(
    media: MediaControlsSnapshot,
    action: AppSeekAction?,
    appIcon: android.graphics.Bitmap?,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
): PreviewControl? {
    val icon = remember(appIcon) { appIcon?.asImageBitmap() }
    return if (action != null) {
        PreviewControl(iconRes, labelRes, icon, action.label.takeIf { it.isNotBlank() })
    } else {
        PreviewControl(iconRes, labelRes).takeIf { media.canSeek }
    }
}

@Composable
private fun PreviewTransportRow(
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
    showExtraControls: Boolean,
) {
    val appSeek = media.appSeek
    val outerBack: PreviewControl?
    val innerBack: PreviewControl?
    val outerForward: PreviewControl?
    val innerForward: PreviewControl?
    if (appSeek != null) {
        outerBack = null
        outerForward = null
        innerBack =
            rememberSeekControl(
                media,
                appSeek.back,
                media.appSeekBackIcon,
                R.drawable.ic_media_controls_rewind,
                R.string.media_seek_back,
            )
        innerForward =
            rememberSeekControl(
                media,
                appSeek.forward,
                media.appSeekForwardIcon,
                R.drawable.ic_media_controls_fast_forward,
                R.string.media_seek_forward,
            )
    } else {
        val skipBack = PreviewControl(R.drawable.ic_media_controls_previous, R.string.media_command_previous)
        val skipForward = PreviewControl(R.drawable.ic_media_controls_next, R.string.media_command_next)
        val seekBack =
            PreviewControl(R.drawable.ic_media_controls_rewind, R.string.media_seek_back).takeIf { media.canSeek }
        val seekForward =
            PreviewControl(R.drawable.ic_media_controls_fast_forward, R.string.media_seek_forward).takeIf { media.canSeek }
        if (media.isSeekMode) {
            outerBack = skipBack
            innerBack = seekBack
            outerForward = skipForward
            innerForward = seekForward
        } else {
            outerBack = seekBack
            innerBack = skipBack
            outerForward = seekForward
            innerForward = skipForward
        }
    }
    val playPauseIcon =
        when {
            !media.hasAccess -> R.drawable.ic_widget_media_play_pause
            media.isPlaying -> R.drawable.ic_media_controls_pause
            else -> R.drawable.ic_media_controls_play
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (showExtraControls) outerBack?.let { PlainControl(it, colors) }
        innerBack?.let { ProminentControl(it, colors) }
        ControlSlot {
            Box(
                modifier = Modifier.size(MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE)
                    .background(colors.playPauseCircle, AppShapes.full),
                contentAlignment = Alignment.Center,
            ) {
                PreviewIcon(playPauseIcon, R.string.media_command_play_pause, MediaControlsWidgetDimens.CONTROL_ICON_SIZE, colors.playPauseIcon)
            }
        }
        innerForward?.let { ProminentControl(it, colors) }
        if (showExtraControls) outerForward?.let { PlainControl(it, colors) }
    }
}

@Composable
private fun PlainControl(control: PreviewControl, colors: MediaControlsColors) {
    ControlSlot {
        PreviewIcon(control, MediaControlsWidgetDimens.SECONDARY_CONTROL_ICON_SIZE, colors.content)
    }
}

@Composable
private fun ProminentControl(
    control: PreviewControl,
    colors: MediaControlsColors,
) {
    ControlSlot {
        Box(
            modifier =
                Modifier
                    .size(MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE)
                    .border(MediaControlsWidgetDimens.PROMINENT_CIRCLE_STROKE, colors.prominentCircleBorder, AppShapes.full),
            contentAlignment = Alignment.Center,
        ) {
            PreviewIcon(control, MediaControlsWidgetDimens.CONTROL_ICON_SIZE, colors.content)
        }
    }
}

@Composable
private fun ControlSlot(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.size(MediaControlsWidgetDimens.CONTROL_BUTTON_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PreviewIcon(
    control: PreviewControl,
    size: Dp,
    tint: Color,
) {
    val appIcon = control.appIcon
    if (appIcon != null) {
        Icon(
            bitmap = appIcon,
            contentDescription = control.appLabel ?: stringResource(control.labelRes),
            tint = tint,
            modifier = Modifier.size(size),
        )
    } else {
        PreviewIcon(control.iconRes, control.labelRes, size, tint)
    }
}

@Composable
private fun PreviewIcon(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    size: Dp,
    tint: Color,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(labelRes),
        tint = tint,
        modifier = Modifier.size(size),
    )
}
