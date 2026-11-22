package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.foundation.layout.FlowRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.media.MediaAppSeek
import com.tk.quicksearch.media.MediaSeek
import com.tk.quicksearch.media.openMediaSessionPlayer
import com.tk.quicksearch.search.data.MediaPlaybackRepository
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** States a session passes through while loading or switching tracks; the card stays up through them. */
internal val NowPlayingTransitionStates =
    setOf(
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
    )

/** Tighter than titleSmall's 20sp so a wrapped two-line title reads as one block. */
private val NowPlayingTitleLineHeight = 18.sp
/** Lifts the close button so its glyph lines up with the first title line rather than the row's top. */
private val NowPlayingCloseLift = DesignTokens.SpacingSmall

/** The empty space a control's 48dp touch target leaves under its 32dp circle. */
private val NowPlayingTransportBottomTrim = DesignTokens.SpacingSmall

private val NowPlayingAlbumArtSize = 56.dp
private val NowPlayingControlIconSize = DesignTokens.IconSizeSmall
private val NowPlayingProminentCircleSize = 32.dp
private val NowPlayingProminentCircleBorder = 1.dp
private val NowPlayingPlayPauseCircleSize = NowPlayingProminentCircleSize
private val NowPlayingPlayPauseIconSize = NowPlayingControlIconSize
private val NowPlayingSecondaryControlIconSize = 18.dp
private val NowPlayingRowStartPadding = DesignTokens.GlanceRowHorizontalPadding

/** The active now-playing media session shown in the home At a Glance card, with transport controls. */
internal class NowPlayingGlance(
    val title: String,
    /** Artist (or album, or the player's own subtitle) shown under the title; null when the session has none. */
    val subtitle: String?,
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
    val live = rememberLiveNowPlayingGlance(enabled)
    // Changing tracks briefly reports a paused/stopped state or empty metadata on many players,
    // so keep the last card for a moment instead of hiding it and popping it back in.
    var lastShown by remember { mutableStateOf<NowPlayingGlance?>(null) }
    val canHold = enabled && NowPlayingDismissal.sessionToken == null
    SideEffect { if (live != null) lastShown = live }
    LaunchedEffect(live == null, canHold) {
        if (live != null) return@LaunchedEffect
        if (canHold) delay(NowPlayingTransitionHoldMs)
        lastShown = null
    }
    return live ?: lastShown?.takeIf { canHold }
}

private const val NowPlayingTransitionHoldMs = 2_000L

@Composable
private fun rememberLiveNowPlayingGlance(enabled: Boolean): NowPlayingGlance? {
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

    // Not keyed on [enabled]: typing a query hides the card without re-registering the listener.
    DisposableEffect(repository, refreshKey) {
        if (!repository.hasAccess()) {
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
                    // Players report position updates often; only what the card shows recomposes it.
                    if (state.glanceFields() != playbackState.glanceFields()) playbackState = state
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
    val subtitle =
        listOf(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            MediaMetadata.METADATA_KEY_ALBUM,
        ).firstNotNullOfOrNull { key -> activeMetadata.getString(key)?.trim()?.takeIf { it.isNotEmpty() && it != title } }
    val albumArt =
        activeMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: activeMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
    val isActivelyPlaying = isPlaying || playbackState?.state in NowPlayingTransitionStates
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
    val appSeekIcons = rememberAppSeekIcons(packageName, appSeekActions, appSeekIconSizePx)

    if (!enabled) return null
    if (shouldRemainDismissed) return null
    if (!isActivelyPlaying && !isPausedFromCard) return null

    return NowPlayingGlance(
        title = title,
        subtitle = subtitle,
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
internal fun NowPlayingRow(
    glance: NowPlayingGlance,
    onDismiss: () -> Unit = glance.dismiss,
) {
    val openGlanceSettings = rememberOpenAtAGlanceSettings()
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = NowPlayingRowStartPadding,
            vertical = glanceRowVerticalPadding(),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f).clip(AppShapes.small)
                    .combinedClickable(
                        role = Role.Button,
                        onClick = glance.open,
                        onLongClickLabel = stringResource(R.string.desc_show_options),
                        onLongClick = openGlanceSettings?.let { { showMenu = true } },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                // Track changes swap the art and text with a short motion instead of a cut. Keyed on
                // the track, so a player re-sending the same metadata (or art arriving late) doesn't replay it.
                val trackKey = glance.title to glance.subtitle
                AnimatedContent(
                    targetState = NowPlayingArtFrame(trackKey, glance.albumArt),
                    contentKey = { it.trackKey to (it.albumArt != null) },
                    transitionSpec = {
                        (scaleIn(AppMotion.fastSpatial(), initialScale = 0.85f) + fadeIn(tween(AppMotion.DurationShort))) togetherWith
                            fadeOut(tween(AppMotion.DurationQuick))
                    },
                    label = "nowPlayingArt",
                ) { frame -> NowPlayingAlbumArt(frame.albumArt) }
                // Reserve two title lines plus the artist line (at the user's font scale), so a track
                // with a shorter title or no artist doesn't change the card's height.
                val textBlockHeight =
                    with(LocalDensity.current) {
                        (NowPlayingTitleLineHeight * 2).toDp() + MaterialTheme.typography.bodySmall.lineHeight.toDp()
                    }
                AnimatedContent(
                    targetState = trackKey,
                    modifier = Modifier.weight(1f).heightIn(min = textBlockHeight),
                    transitionSpec = {
                        val enter =
                            slideInVertically(AppMotion.defaultSpatial()) { it / 3 } +
                                fadeIn(tween(AppMotion.DurationShort, delayMillis = AppMotion.DurationMicro))
                        val exit = slideOutVertically(tween(AppMotion.DurationExit)) { -it / 3 } + fadeOut(tween(AppMotion.DurationExit))
                        enter togetherWith exit using SizeTransform(clip = false) { _, _ -> snap() }
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "nowPlayingText",
                ) { (title, subtitle) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(lineHeight = NowPlayingTitleLineHeight),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                }
                if (openGlanceSettings != null) {
                    AppDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        AtAGlanceSettingsMenuItem(openSettings = openGlanceSettings, closeMenu = { showMenu = false })
                    }
                }
            }
            // Pinned to the first title line, so it stays put whether the title wraps or not.
            AppIconButton(
                onClick = onDismiss,
                tooltip = stringResource(R.string.common_close),
                modifier = Modifier.align(Alignment.Top).offset(y = -NowPlayingCloseLift),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
                )
            }
        }
        NowPlayingTransport(glance)
    }
}

@Composable
private fun NowPlayingTransport(glance: NowPlayingGlance) {
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
            seekControl(appSeek.back, MaterialSymbols.FastRewind, R.string.media_seek_back, glance.seekBack, glance.canSeek)
        innerForward =
            seekControl(
                appSeek.forward,
                MaterialSymbols.FastForward,
                R.string.media_seek_forward,
                glance.seekForward,
                glance.canSeek,
            )
    } else {
        val skipBack = NowPlayingControl(MaterialSymbols.SkipPrevious, R.string.media_command_previous, glance.previous)
        val skipForward = NowPlayingControl(MaterialSymbols.SkipNext, R.string.media_command_next, glance.next)
        val seekBack =
            NowPlayingControl(MaterialSymbols.FastRewind, R.string.media_seek_back, glance.seekBack)
                .takeIf { glance.canSeek }
        val seekForward =
            NowPlayingControl(MaterialSymbols.FastForward, R.string.media_seek_forward, glance.seekForward)
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
    FlowRow(
        // The 48dp touch targets pad below their circles; drop that padding so the card's bottom
        // edge sits as close to the controls as its top edge does to the art.
        modifier =
            Modifier.fillMaxWidth().layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val trim = NowPlayingTransportBottomTrim.roundToPx().coerceAtMost(placeable.height)
                layout(placeable.width, placeable.height - trim) { placeable.place(0, 0) }
            },
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall, Alignment.CenterHorizontally),
    ) {
        outerBack?.let { NowPlayingSkipButton(it, isProminent = false) }
        innerBack?.let { NowPlayingSkipButton(it, isProminent = true) }
        AppIconButton(onClick = glance.playPause, tooltip = stringResource(R.string.media_command_play_pause)) {
            // Expressive morph: a circle while paused squares off while playing, and the glyph
            // swaps with a spring scale and rotation instead of cutting.
            val corner by animateDpAsState(
                targetValue = if (glance.isPlaying) DesignTokens.CornerSmall else NowPlayingPlayPauseCircleSize / 2,
                animationSpec = AppMotion.fastSpatial(),
                label = "playPauseCorner",
            )
            Box(
                modifier =
                    Modifier
                        .size(NowPlayingPlayPauseCircleSize)
                        .clip(RoundedCornerShape(corner))
                        .background(MaterialTheme.colorScheme.onSurface),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = glance.isPlaying,
                    transitionSpec = {
                        val enter =
                            scaleIn(AppMotion.fastSpatial(), initialScale = 0.4f) +
                                fadeIn(tween(AppMotion.DurationQuick))
                        val exit =
                            scaleOut(tween(AppMotion.DurationQuick), targetScale = 0.4f) +
                                fadeOut(tween(AppMotion.DurationQuick))
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                    label = "playPauseIcon",
                ) { playing ->
                    val rotation = remember { Animatable(if (playing) -90f else 90f) }
                    LaunchedEffect(Unit) { rotation.animateTo(0f, AppMotion.fastSpatial()) }
                    Icon(
                        imageVector = if (playing) MaterialSymbols.Pause else MaterialSymbols.PlayArrow,
                        contentDescription = stringResource(R.string.media_command_play_pause),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(NowPlayingPlayPauseIconSize).graphicsLayer { rotationZ = rotation.value },
                    )
                }
            }
        }
        innerForward?.let { NowPlayingSkipButton(it, isProminent = true) }
        outerForward?.let { NowPlayingSkipButton(it, isProminent = false) }
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
    AppIconButton(onClick = control.onClick, tooltip = control.appLabel ?: stringResource(control.labelRes)) {
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
                            MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.ContentFaintAlpha),
                            AppShapes.full,
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

/** The art shown for one track; [trackKey] decides when a change animates. */
private class NowPlayingArtFrame(
    val trackKey: Pair<String, String?>,
    val albumArt: android.graphics.Bitmap?,
)

@Composable
private fun NowPlayingAlbumArt(bitmap: android.graphics.Bitmap?) {
    val sizePx = with(LocalDensity.current) { NowPlayingAlbumArtSize.roundToPx() }.coerceAtLeast(1)
    // Players hand over full-size art; draw a copy scaled to the row once it is ready.
    val scaled by produceState<Pair<android.graphics.Bitmap, ImageBitmap>?>(null, bitmap, sizePx) {
        if (bitmap != null) value = bitmap to withContext(Dispatchers.Default) { scaledAlbumArt(bitmap, sizePx) }
    }
    if (bitmap != null) {
        Image(
            bitmap = scaled?.takeIf { it.first === bitmap }?.second ?: bitmap.asImageBitmap(),
            contentDescription = null,
            modifier =
                Modifier
                    .size(NowPlayingAlbumArtSize)
                    .clip(AppShapes.small),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(NowPlayingAlbumArtSize)
                    .clip(AppShapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}
