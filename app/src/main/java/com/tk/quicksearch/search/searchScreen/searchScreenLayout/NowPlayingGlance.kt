package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.media.MediaAppSeek
import com.tk.quicksearch.media.MediaSeek
import com.tk.quicksearch.media.openMediaSessionPlayer
import com.tk.quicksearch.search.data.MediaPlaybackRepository
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val NowPlayingAlbumArtSize = 56.dp
private val NowPlayingControlButtonSize = 44.dp
private val NowPlayingControlIconSize = 21.dp
private val NowPlayingProminentCircleSize = 33.dp
private val NowPlayingProminentCircleBorder = 1.dp
private const val NowPlayingProminentCircleBorderAlpha = 0.35f
private val NowPlayingPlayPauseCircleSize = NowPlayingProminentCircleSize
private val NowPlayingPlayPauseIconSize = NowPlayingControlIconSize
private val NowPlayingDismissButtonSize = 28.dp
private val NowPlayingSecondaryControlIconSize = 18.dp
private val NowPlayingRowStartPadding = 7.dp

/** How far the title/controls column starts from the card content's start edge. */
private val NowPlayingColumnLeadingOffset = NowPlayingRowStartPadding + NowPlayingAlbumArtSize + DesignTokens.SpacingMedium

/** The active now-playing media session shown in the home At a Glance card, with transport controls. */
internal class NowPlayingGlance(
    val title: String,
    val albumArt: android.graphics.Bitmap?,
    val isPlaying: Boolean,
    /** Whether the session accepts seeks; the rewind/forward buttons are hidden otherwise. */
    val canSeek: Boolean,
    /** Long media: rewind/forward are emphasized over previous/next and sit beside play/pause. */
    val isSeekMode: Boolean,
    val playPause: () -> Unit,
    val previous: () -> Unit,
    val next: () -> Unit,
    val seekBack: () -> Unit,
    val seekForward: () -> Unit,
    /** The app's own seek buttons; when set they replace rewind/forward and previous/next is hidden. */
    val appSeek: NowPlayingAppSeek?,
    val dismiss: () -> Unit,
    val open: () -> Unit,
)

internal class NowPlayingAppSeekButton(
    /** Null when the app's drawable could not be loaded; our rewind/forward glyph stands in. */
    val icon: ImageBitmap?,
    val label: String?,
    val onClick: () -> Unit,
)

/** A missing direction falls back to the 15 second seek button (see [com.tk.quicksearch.media.AppSeekActions]). */
internal class NowPlayingAppSeek(
    val back: NowPlayingAppSeekButton?,
    val forward: NowPlayingAppSeekButton?,
)

/**
 * A dismissal outlives the card's composition, which is torn down whenever home is left (for
 * example to open the widget panel), so coming back does not bring the card back on its own.
 */
private object NowPlayingDismissal {
    var sessionToken by mutableStateOf<Any?>(null)
    var dismissedWhilePlaying by mutableStateOf(false)
    var sawNonPlayingState by mutableStateOf(false)
}

/**
 * The card only shows while media plays, except after the user pauses it from the card itself:
 * then it stays (paused) so they can resume from it, until that session plays again from anywhere.
 */
private object NowPlayingPausedFromCard {
    var sessionToken by mutableStateOf<Any?>(null)
    var sawPausedState by mutableStateOf(false)
}

/**
 * Tracks the system's active media session while [enabled], refreshing whenever the set of
 * sessions changes and mirroring the selected session's playback state and metadata live (rather
 * than polling, since play/pause must flip instantly).
 */
@Composable
internal fun rememberNowPlayingGlance(enabled: Boolean): NowPlayingGlance? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { MediaPlaybackRepository(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var metadata by remember { mutableStateOf<MediaMetadata?>(null) }
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }
    val dismissal = NowPlayingDismissal
    val pausedFromCard = NowPlayingPausedFromCard

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(repository, enabled, refreshKey) {
        if (!enabled || !repository.hasAccess()) {
            controller = null
            return@DisposableEffect onDispose {}
        }
        fun refreshController() {
            controller = repository.activeController()
        }
        refreshController()
        val listener = repository.registerSessionsListener(::refreshController)
        onDispose { repository.unregisterSessionsListener(listener) }
    }

    DisposableEffect(controller?.sessionToken) {
        val current = controller
        if (current == null) {
            metadata = null
            playbackState = null
            return@DisposableEffect onDispose {}
        }
        metadata = runCatching { current.metadata }.getOrNull()
        playbackState = runCatching { current.playbackState }.getOrNull()
        val callback =
            object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    playbackState = state
                }

                override fun onMetadataChanged(newMetadata: MediaMetadata?) {
                    metadata = newMetadata
                }

                override fun onSessionDestroyed() {
                    controller = null
                }
            }
        runCatching { current.registerCallback(callback) }
        onDispose { runCatching { current.unregisterCallback(callback) } }
    }

    val activeController = controller ?: return null
    val activeMetadata = metadata ?: return null
    val title =
        activeMetadata
            .getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val albumArt =
        activeMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: activeMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
    val isActivelyPlaying = isPlaying || playbackState?.state == PlaybackState.STATE_BUFFERING
    val isPausedFromCard = pausedFromCard.sessionToken == activeController.sessionToken
    val isSeekMode = MediaSeek.isSeekMode(activeMetadata, playbackState)
    val dismissedForCurrentSession = dismissal.sessionToken == activeController.sessionToken
    val shouldRemainDismissed =
        dismissedForCurrentSession &&
            !(isPlaying && (!dismissal.dismissedWhilePlaying || dismissal.sawNonPlayingState))

    androidx.compose.runtime.LaunchedEffect(
        dismissal.sessionToken,
        activeController.sessionToken,
        isPlaying,
    ) {
        when {
            dismissal.sessionToken == null -> Unit
            !dismissedForCurrentSession -> {
                dismissal.sessionToken = null
                dismissal.sawNonPlayingState = false
            }
            !isPlaying -> dismissal.sawNonPlayingState = true
            !dismissal.dismissedWhilePlaying || dismissal.sawNonPlayingState -> {
                dismissal.sessionToken = null
                dismissal.sawNonPlayingState = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(pausedFromCard.sessionToken, activeController.sessionToken, isPlaying) {
        // Pausing from the card sets the token while the state still reads playing, so only a
        // resume seen after the pause landed (or a switch to another session) clears it.
        when {
            pausedFromCard.sessionToken == null -> Unit
            !isPausedFromCard || (isPlaying && pausedFromCard.sawPausedState) -> {
                pausedFromCard.sessionToken = null
                pausedFromCard.sawPausedState = false
            }
            !isPlaying -> pausedFromCard.sawPausedState = true
        }
    }

    val appSeekActions = MediaAppSeek.find(playbackState)
    val appSeekIconSizePx = with(LocalDensity.current) { NowPlayingControlIconSize.roundToPx() }.coerceAtLeast(1)
    val packageName = activeController.packageName
    val appSeekIcons =
        remember(packageName, appSeekActions, appSeekIconSizePx) {
            appSeekActions?.let { actions ->
                listOf(actions.back, actions.forward).map { action ->
                    action?.let {
                        MediaAppSeek.loadIcon(context, packageName, it.iconRes, appSeekIconSizePx)?.asImageBitmap()
                    }
                }
            }
        }

    if (shouldRemainDismissed) return null
    if (!isActivelyPlaying && !isPausedFromCard) return null

    return NowPlayingGlance(
        title = title,
        albumArt = albumArt,
        isPlaying = isPlaying,
        canSeek = MediaSeek.supportsSeek(playbackState),
        isSeekMode = isSeekMode,
        playPause = {
            runCatching {
                if (isPlaying) {
                    pausedFromCard.sessionToken = activeController.sessionToken
                    pausedFromCard.sawPausedState = false
                    activeController.transportControls.pause()
                } else {
                    activeController.transportControls.play()
                }
            }
        },
        previous = { runCatching { activeController.transportControls.skipToPrevious() } },
        next = { runCatching { activeController.transportControls.skipToNext() } },
        seekBack = { MediaSeek.seekBy(activeController, -MediaSeek.STEP_MS) },
        seekForward = { MediaSeek.seekBy(activeController, MediaSeek.STEP_MS) },
        appSeek =
            appSeekActions?.let { actions ->
                fun button(
                    action: AppSeekAction?,
                    icon: ImageBitmap?,
                    fallback: () -> Unit,
                ) = action?.let {
                    NowPlayingAppSeekButton(
                        icon = icon,
                        label = it.label.takeIf { label -> label.isNotBlank() },
                        onClick = { if (!MediaAppSeek.send(activeController, it.action)) fallback() },
                    )
                }
                NowPlayingAppSeek(
                    back = button(actions.back, appSeekIcons?.getOrNull(0)) { MediaSeek.seekBy(activeController, -MediaSeek.STEP_MS) },
                    forward =
                        button(actions.forward, appSeekIcons?.getOrNull(1)) { MediaSeek.seekBy(activeController, MediaSeek.STEP_MS) },
                )
            },
        dismiss = {
            dismissal.sessionToken = activeController.sessionToken
            dismissal.dismissedWhilePlaying = isPlaying
            dismissal.sawNonPlayingState = false
        },
        open = {
            AppLockGate.runAfterUnlock(context, activeController.packageName) {
                openMediaSessionPlayer(context, activeController)
            }
        },
    )
}

@Composable
internal fun NowPlayingRow(glance: NowPlayingGlance) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = NowPlayingRowStartPadding,
                    top = DesignTokens.SpacingMedium,
                    bottom = DesignTokens.SpacingSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(DesignTokens.SpacingSmall))
                    .clickable(onClick = glance.open),
        ) {
            NowPlayingAlbumArt(glance)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Scrolls when the title doesn't fit; stays still when it does.
                Text(
                    text = glance.title,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(onClick = glance.open)
                            .basicMarquee(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                IconButton(onClick = glance.dismiss, modifier = Modifier.size(NowPlayingDismissButtonSize)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            // The emphasized pair (see NowPlayingGlance.isSeekMode) sits beside play/pause; the
            // other pair moves to the outer slots, each centered in the space left over there.
            // The app's own seek buttons take the inner slots alone, leaving the outer ones empty.
            val appSeek = glance.appSeek
            val outerBack: NowPlayingControl?
            val innerBack: NowPlayingControl?
            val outerForward: NowPlayingControl?
            val innerForward: NowPlayingControl?
            if (appSeek != null) {
                outerBack = null
                outerForward = null
                innerBack =
                    seekControl(appSeek.back, Icons.Rounded.FastRewind, R.string.media_seek_back, glance.seekBack, glance.canSeek)
                innerForward =
                    seekControl(
                        appSeek.forward,
                        Icons.Rounded.FastForward,
                        R.string.media_seek_forward,
                        glance.seekForward,
                        glance.canSeek,
                    )
            } else {
                val skipBack = NowPlayingControl(Icons.Rounded.SkipPrevious, R.string.media_command_previous, glance.previous)
                val skipForward = NowPlayingControl(Icons.Rounded.SkipNext, R.string.media_command_next, glance.next)
                val seekBack =
                    NowPlayingControl(Icons.Rounded.FastRewind, R.string.media_seek_back, glance.seekBack)
                        .takeIf { glance.canSeek }
                val seekForward =
                    NowPlayingControl(Icons.Rounded.FastForward, R.string.media_seek_forward, glance.seekForward)
                        .takeIf { glance.canSeek }
                if (glance.isSeekMode) {
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
            val hasOuterControls = outerBack != null || outerForward != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasOuterControls) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        outerBack?.let { NowPlayingSkipButton(it, isProminent = false) }
                    }
                }
                Row(
                    // Without outer controls the group alone would sit right of the card's center,
                    // since this column starts after the album art; center it on the card instead.
                    modifier =
                        if (hasOuterControls) {
                            Modifier
                        } else {
                            Modifier.weight(1f).centeredOnCard(NowPlayingColumnLeadingOffset)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    innerBack?.let { NowPlayingSkipButton(it, isProminent = true) }
                    IconButton(onClick = glance.playPause, modifier = Modifier.size(NowPlayingControlButtonSize)) {
                        Box(
                            modifier =
                                Modifier
                                    .size(NowPlayingPlayPauseCircleSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (glance.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.media_command_play_pause),
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(NowPlayingPlayPauseIconSize),
                            )
                        }
                    }
                    innerForward?.let { NowPlayingSkipButton(it, isProminent = true) }
                }
                if (hasOuterControls) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        outerForward?.let { NowPlayingSkipButton(it, isProminent = false) }
                    }
                }
            }
        }
    }
}

/**
 * Nudge right of the card's exact center: the album art weighs down the start side, so the
 * controls read as centered a little past the true middle.
 */
internal val CenteredOnCardNudge = 20.dp

/**
 * Lays the content out across the full width it is given, horizontally centered (plus
 * [CenteredOnCardNudge]) on a card whose content starts [leadingOffset] before this column (with
 * matching padding on both card edges), but never starting before the column itself.
 */
internal fun Modifier.centeredOnCard(leadingOffset: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0))
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val x =
            ((width - leadingOffset.roundToPx() - placeable.width) / 2 + CenteredOnCardNudge.roundToPx())
                .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
        layout(width, placeable.height) { placeable.place(x, 0) }
    }

private class NowPlayingControl(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    val onClick: () -> Unit,
    /** An app seek button's own icon and label, shown in place of [icon] and [labelRes]. */
    val appIcon: ImageBitmap? = null,
    val appLabel: String? = null,
)

/** The app's seek button for one direction, or the 15 second one when the app has none for it. */
private fun seekControl(
    appButton: NowPlayingAppSeekButton?,
    icon: ImageVector,
    @StringRes labelRes: Int,
    fallback: () -> Unit,
    canSeek: Boolean,
): NowPlayingControl? =
    if (appButton != null) {
        NowPlayingControl(icon, labelRes, appButton.onClick, appButton.icon, appButton.label)
    } else {
        NowPlayingControl(icon, labelRes, fallback).takeIf { canSeek }
    }

/**
 * A skip or seek control. Whichever pair suits the media is [isProminent], drawn in an outlined
 * circle next to play/pause: seeking for long media (see [MediaSeek.isSeekMode]), track skipping otherwise.
 */
@Composable
private fun NowPlayingSkipButton(
    control: NowPlayingControl,
    isProminent: Boolean,
) {
    IconButton(onClick = control.onClick, modifier = Modifier.size(NowPlayingControlButtonSize)) {
        val icon =
            @Composable {
                val contentDescription = control.appLabel ?: stringResource(control.labelRes)
                val modifier = Modifier.size(if (isProminent) NowPlayingControlIconSize else NowPlayingSecondaryControlIconSize)
                val appIcon = control.appIcon
                if (appIcon != null) {
                    Icon(
                        bitmap = appIcon,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = modifier,
                    )
                } else {
                    Icon(
                        imageVector = control.icon,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = modifier,
                    )
                }
            }
        if (isProminent) {
            // Outlined, and a step smaller than the filled play/pause circle, so it reads as secondary to it.
            Box(
                modifier =
                    Modifier
                        .size(NowPlayingProminentCircleSize)
                        .border(
                            NowPlayingProminentCircleBorder,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = NowPlayingProminentCircleBorderAlpha),
                            CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        } else {
            icon()
        }
    }
}

@Composable
private fun NowPlayingAlbumArt(glance: NowPlayingGlance) {
    val bitmap = glance.albumArt
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier =
                Modifier
                    .size(NowPlayingAlbumArtSize)
                    .clip(RoundedCornerShape(DesignTokens.SpacingSmall)),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(NowPlayingAlbumArtSize)
                    .clip(RoundedCornerShape(DesignTokens.SpacingSmall))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
