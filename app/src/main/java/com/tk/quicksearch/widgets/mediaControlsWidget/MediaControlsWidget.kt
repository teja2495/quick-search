package com.tk.quicksearch.widgets.mediaControlsWidget

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.media.AppSeekActions
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.CenteredOnCardNudge
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetMediaAction
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import com.tk.quicksearch.widgets.utils.WidgetTheme
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlin.math.roundToInt

/** Sizes mirror the At a Glance Now Playing row so the widget reads as the same component. */
internal object MediaControlsWidgetDimens {
    /** Spans the title and controls rows, so the art sits the same inset from the card on all sides. */
    val ALBUM_ART_SIZE = 82.dp
    private val MIN_ALBUM_ART_CORNER_RADIUS = 4.dp
    val CONTROL_BUTTON_SIZE = 54.dp
    val CONTROL_ICON_SIZE = 25.dp
    val SECONDARY_CONTROL_ICON_SIZE = 22.dp
    val PROMINENT_CIRCLE_SIZE = 40.dp
    val TITLE_ROW_HEIGHT = 28.dp
    val TITLE_FONT_SIZE = 17.sp
    val ART_PLACEHOLDER_ICON_SIZE = 24.dp
    val CONTENT_PADDING_HORIZONTAL = 14.dp
    val CONTENT_PADDING_VERTICAL = 14.dp
    val ART_SPACING = 14.dp
    val INNER_CONTROL_SPACING = 10.dp
    val PROMINENT_CIRCLE_STROKE = 1.dp

    /** How far the title/controls column starts from the card content's start edge. */
    val COLUMN_LEADING_OFFSET = ALBUM_ART_SIZE + ART_SPACING
    /** Card height: fits one home-screen row, and stays this compact in taller cells. */
    const val HEIGHT_DP = 110f

    /**
     * Art corners concentric with the card's: the card radius minus the art's inset (equal on the
     * top, bottom and start edges), so both curves share a center and the gap stays even.
     */
    fun albumArtCornerRadius(widgetCornerRadiusDp: Float): Dp =
        (widgetCornerRadiusDp.dp - CONTENT_PADDING_HORIZONTAL)
            .coerceIn(MIN_ALBUM_ART_CORNER_RADIUS, ALBUM_ART_SIZE / 2)
}

class MediaControlsWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        /** Drawn at until the host reports the placed widget's size. */
        val DEFAULT_SIZE = DpSize(WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp, MediaControlsWidgetDimens.HEIGHT_DP.dp)
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent { WidgetBody() }
    }

    @Composable
    private fun WidgetBody() {
        // Reading the state keeps this composition subscribed to refreshWidgets' playback revision
        // bump, so the snapshot below is re-read whenever the session changes.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val storedConfig = prefs.toWidgetPreferences(context).enforceVariantConstraints(WidgetVariant.MEDIA_CONTROLS)
        // Before the first save the state is empty; start dark like the configure screen does.
        val config = if (prefs.asMap().isEmpty()) storedConfig.copy(theme = WidgetTheme.DARK) else storedConfig
        val colors = config.mediaControlsColors()
        val media = readMediaControlsSnapshot(context)
        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp)
        // Compact card centered in the cell; shrinks only if a launcher's row is shorter.
        val heightDp =
            WidgetLayoutUtils
                .resolveOr(size.height, MediaControlsWidgetDimens.HEIGHT_DP.dp)
                .coerceAtMost(MediaControlsWidgetDimens.HEIGHT_DP.dp)
        val density = context.resources.displayMetrics.density

        // Arbitrary corner radii need Android 12+ in Glance, so the surface is drawn as a bitmap.
        val backgroundBitmap =
            if (config.backgroundAlpha > 0f) {
                WidgetBitmapUtils.createWidgetBitmap(
                    widthPx = (widthDp.value * density).roundToInt().coerceAtLeast(1),
                    heightPx = (heightDp.value * density).roundToInt().coerceAtLeast(1),
                    backgroundColor = colors.background.copy(alpha = config.backgroundAlpha),
                    borderColor = null,
                    borderWidthPx = 0,
                    cornerRadiusPx = config.borderRadiusDp * density,
                )
            } else {
                null
            }

        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .height(heightDp)
                        .then(backgroundBitmap?.let { GlanceModifier.background(ImageProvider(it)) } ?: GlanceModifier)
                        .padding(
                            horizontal = MediaControlsWidgetDimens.CONTENT_PADDING_HORIZONTAL,
                            vertical = MediaControlsWidgetDimens.CONTENT_PADDING_VERTICAL,
                        ),
                contentAlignment = Alignment.CenterStart,
            ) {
                MediaControlsContent(
                    context = context,
                    widthDp = widthDp,
                    media = media,
                    colors = colors,
                    artCornerRadius = MediaControlsWidgetDimens.albumArtCornerRadius(config.borderRadiusDp),
                    density = density,
                )
            }
        }
    }
}

@Composable
private fun MediaControlsContent(
    context: Context,
    widthDp: Dp,
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
    artCornerRadius: Dp,
    density: Float,
) {
    // Opening the player goes through an activity: only a foreground app may start another app's
    // activity, which a widget broadcast callback is not.
    val openAction =
        if (media.hasSession || !media.hasAccess) {
            actionStartActivity(MediaControlsOpenPlayerActivity.createIntent(context))
        } else {
            null
        }
    val contentColor = ColorProvider(colors.content)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(media, colors, artCornerRadius, density, openAction)
        Spacer(modifier = GlanceModifier.width(MediaControlsWidgetDimens.ART_SPACING))
        Column(modifier = GlanceModifier.defaultWeight()) {
            val title =
                when {
                    media.title != null -> media.title
                    !media.hasAccess -> context.getString(R.string.widget_media_controls_grant_access)
                    else -> context.getString(R.string.widget_media_controls_nothing_playing)
                }
            Box(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .height(MediaControlsWidgetDimens.TITLE_ROW_HEIGHT)
                        .then(openAction?.let { GlanceModifier.clickable(it, rippleOverride = android.R.color.transparent) } ?: GlanceModifier),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    style = TextStyle(color = contentColor, fontSize = MediaControlsWidgetDimens.TITLE_FONT_SIZE),
                    maxLines = 1,
                )
            }
            TransportRow(context, widthDp, media, colors)
        }
    }
}

@Composable
private fun AlbumArt(
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
    cornerRadius: Dp,
    density: Float,
    openAction: Action?,
) {
    val sizePx = (MediaControlsWidgetDimens.ALBUM_ART_SIZE.value * density).roundToInt().coerceAtLeast(1)
    val art =
        media.albumArt?.let {
            roundedAlbumArt(it, sizePx, cornerRadius.value * density)
        }
    val clickModifier =
        openAction?.let { GlanceModifier.clickable(it, rippleOverride = android.R.color.transparent) } ?: GlanceModifier
    if (art != null) {
        Image(
            provider = ImageProvider(art),
            contentDescription = null,
            modifier = GlanceModifier.size(MediaControlsWidgetDimens.ALBUM_ART_SIZE).then(clickModifier),
        )
    } else {
        Box(
            modifier = GlanceModifier.size(MediaControlsWidgetDimens.ALBUM_ART_SIZE).then(clickModifier),
            contentAlignment = Alignment.Center,
        ) {
            ShapeImage(
                size = MediaControlsWidgetDimens.ALBUM_ART_SIZE,
                cornerRadius = cornerRadius,
                fill = colors.artPlaceholder,
                density = density,
            )
            Image(
                provider = ImageProvider(R.drawable.ic_media_controls_music_note),
                contentDescription = null,
                modifier = GlanceModifier.size(MediaControlsWidgetDimens.ART_PLACEHOLDER_ICON_SIZE),
                colorFilter = ColorFilter.tint(ColorProvider(colors.artPlaceholderIcon)),
            )
        }
    }
}

private enum class ControlStyle { PLAIN, PROMINENT, PLAY_PAUSE }

private class Control(
    @DrawableRes val iconRes: Int,
    val command: MediaCommand,
    /** An app seek button: its own icon, label and custom action, with [command] as the fallback. */
    val appIcon: Bitmap? = null,
    val appLabel: String? = null,
    val customAction: String? = null,
)

/** The app's seek button for one direction, or ours when the app has none for it (see [AppSeekActions]). */
private fun seekControl(
    media: MediaControlsSnapshot,
    action: AppSeekAction?,
    appIcon: Bitmap?,
    @DrawableRes iconRes: Int,
    command: MediaCommand,
): Control? =
    if (action != null) {
        Control(
            iconRes = iconRes,
            command = command,
            appIcon = appIcon,
            appLabel = action.label.takeIf { it.isNotBlank() },
            customAction = action.action,
        )
    } else {
        Control(iconRes, command).takeIf { media.canSeek }
    }

/**
 * Same arrangement as the At a Glance card: whichever pair suits the media (seeking for long
 * media, track skipping otherwise) sits in outlined circles beside play/pause, and the other pair
 * is centered in the outer slots. Rewind/forward are dropped when the session refuses seeks. When
 * the app has its own seek buttons, those sit beside play/pause and the outer slots stay empty.
 */
@Composable
private fun TransportRow(
    context: Context,
    widthDp: Dp,
    media: MediaControlsSnapshot,
    colors: MediaControlsColors,
) {
    val appSeek = media.appSeek
    val outerBack: Control?
    val innerBack: Control?
    val outerForward: Control?
    val innerForward: Control?
    if (appSeek != null) {
        outerBack = null
        outerForward = null
        innerBack =
            seekControl(media, appSeek.back, media.appSeekBackIcon, R.drawable.ic_media_controls_rewind, MediaCommand.SEEK_BACK)
        innerForward =
            seekControl(
                media,
                appSeek.forward,
                media.appSeekForwardIcon,
                R.drawable.ic_media_controls_fast_forward,
                MediaCommand.SEEK_FORWARD,
            )
    } else {
        val skipBack = Control(R.drawable.ic_media_controls_previous, MediaCommand.PREVIOUS)
        val skipForward = Control(R.drawable.ic_media_controls_next, MediaCommand.NEXT)
        val seekBack = Control(R.drawable.ic_media_controls_rewind, MediaCommand.SEEK_BACK).takeIf { media.canSeek }
        val seekForward =
            Control(R.drawable.ic_media_controls_fast_forward, MediaCommand.SEEK_FORWARD).takeIf { media.canSeek }
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
            // Playback state is unknown without access, so show the combined glyph rather than guess.
            !media.hasAccess -> R.drawable.ic_widget_media_play_pause
            media.isPlaying -> R.drawable.ic_media_controls_pause
            else -> R.drawable.ic_media_controls_play
        }

    val hasOuterControls = outerBack != null || outerForward != null
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasOuterControls) {
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                outerBack?.let { ControlButton(context, colors, it, ControlStyle.PLAIN) }
            }
        } else {
            // The group alone would sit right of the card's center, since this column starts
            // after the album art; RemoteViews cannot offset, so pad it onto the card's center.
            val groupCount = 1 + listOfNotNull(innerBack, innerForward).size
            val groupWidth =
                MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE * groupCount +
                    MediaControlsWidgetDimens.INNER_CONTROL_SPACING * (groupCount - 1)
            val columnWidth =
                widthDp - MediaControlsWidgetDimens.CONTENT_PADDING_HORIZONTAL * 2 - MediaControlsWidgetDimens.COLUMN_LEADING_OFFSET
            val startPadding =
                ((columnWidth - MediaControlsWidgetDimens.COLUMN_LEADING_OFFSET - groupWidth) / 2 + CenteredOnCardNudge)
                    .coerceIn(0.dp, (columnWidth - groupWidth).coerceAtLeast(0.dp))
            Spacer(modifier = GlanceModifier.width(startPadding))
        }
        innerBack?.let {
            ControlButton(context, colors, it, ControlStyle.PROMINENT)
            Spacer(modifier = GlanceModifier.width(MediaControlsWidgetDimens.INNER_CONTROL_SPACING))
        }
        ControlButton(context, colors, Control(playPauseIcon, MediaCommand.PLAY_PAUSE), ControlStyle.PLAY_PAUSE)
        innerForward?.let {
            Spacer(modifier = GlanceModifier.width(MediaControlsWidgetDimens.INNER_CONTROL_SPACING))
            ControlButton(context, colors, it, ControlStyle.PROMINENT)
        }
        if (hasOuterControls) {
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                outerForward?.let { ControlButton(context, colors, it, ControlStyle.PLAIN) }
            }
        }
    }
}

@Composable
private fun ControlButton(
    context: Context,
    colors: MediaControlsColors,
    control: Control,
    style: ControlStyle,
) {
    // Circled buttons are only as wide as their circle, and plain ones fill their weighted slot,
    // so the outer seek icons keep room on a 4-cell-wide widget.
    val sizeModifier =
        if (style == ControlStyle.PLAIN) {
            GlanceModifier.fillMaxWidth()
        } else {
            GlanceModifier.width(MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE)
        }
    Box(
        modifier =
            sizeModifier
                .height(MediaControlsWidgetDimens.CONTROL_BUTTON_SIZE)
                .clickable(
                    onClick =
                        actionRunCallback<CustomButtonsWidgetMediaAction>(
                            if (control.customAction != null) {
                                actionParametersOf(
                                    CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY to control.command.value,
                                    CustomButtonsWidgetMediaAction.CUSTOM_ACTION_KEY to control.customAction,
                                )
                            } else {
                                actionParametersOf(CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY to control.command.value)
                            },
                        ),
                    rippleOverride = android.R.color.transparent,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val contentDescription = control.appLabel ?: context.getString(control.command.labelRes)
        val icon = control.appIcon?.let { ImageProvider(it) } ?: ImageProvider(control.iconRes)
        when (style) {
            ControlStyle.PLAIN ->
                ControlIcon(icon, contentDescription, MediaControlsWidgetDimens.SECONDARY_CONTROL_ICON_SIZE, colors.content)
            ControlStyle.PROMINENT ->
                Box(contentAlignment = Alignment.Center) {
                    ShapeImage(
                        size = MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE,
                        cornerRadius = MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE / 2,
                        stroke = colors.prominentCircleBorder,
                        density = context.resources.displayMetrics.density,
                    )
                    ControlIcon(icon, contentDescription, MediaControlsWidgetDimens.CONTROL_ICON_SIZE, colors.content)
                }
            ControlStyle.PLAY_PAUSE ->
                Box(contentAlignment = Alignment.Center) {
                    ShapeImage(
                        size = MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE,
                        cornerRadius = MediaControlsWidgetDimens.PROMINENT_CIRCLE_SIZE / 2,
                        fill = colors.playPauseCircle,
                        density = context.resources.displayMetrics.density,
                    )
                    ControlIcon(icon, contentDescription, MediaControlsWidgetDimens.CONTROL_ICON_SIZE, colors.playPauseIcon)
                }
        }
    }
}

@Composable
private fun ControlIcon(
    provider: ImageProvider,
    contentDescription: String,
    size: Dp,
    color: Color,
) {
    Image(
        provider = provider,
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(ColorProvider(color)),
    )
}

/**
 * A filled or outlined rounded shape drawn as a bitmap. Tinting a shape drawable is unreliable in
 * RemoteViews (some hosts fill the whole bounds), and arbitrary colors need a bitmap anyway.
 */
@Composable
private fun ShapeImage(
    size: Dp,
    cornerRadius: Dp,
    density: Float,
    fill: Color = Color.Transparent,
    stroke: Color? = null,
) {
    val sizePx = (size.value * density).roundToInt().coerceAtLeast(1)
    val bitmap =
        WidgetBitmapUtils.createWidgetBitmap(
            widthPx = sizePx,
            heightPx = sizePx,
            backgroundColor = fill,
            borderColor = stroke,
            borderWidthPx =
                if (stroke != null) {
                    (MediaControlsWidgetDimens.PROMINENT_CIRCLE_STROKE.value * density).roundToInt().coerceAtLeast(1)
                } else {
                    0
                },
            cornerRadiusPx = cornerRadius.value * density,
        )
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
    )
}
