package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.media.AppSeekAction
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.CenteredOnCardNudge
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetMediaAction
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsOpenPlayerActivity
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsSnapshot
import com.tk.quicksearch.widgets.mediaControlsWidget.roundedAlbumArt
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import kotlin.math.roundToInt

/** Home's Now Playing row sizes (NowPlayingGlance.kt), with the controls a step larger for a home screen. */
private object NowPlayingRowDimens {
    val ALBUM_ART_SIZE = 56.dp
    val ALBUM_ART_CORNER_RADIUS = 8.dp
    val ART_PLACEHOLDER_ICON_SIZE = 20.dp
    val ROW_START_PADDING = 7.dp
    val ROW_TOP_PADDING = 12.dp
    val ROW_BOTTOM_PADDING = 8.dp
    val ART_SPACING = 12.dp
    val DISMISS_BUTTON_SIZE = 48.dp
    val CONTROL_BUTTON_SIZE = 48.dp
    val CONTROL_ICON_SIZE = 24.dp
    val SECONDARY_CONTROL_ICON_SIZE = 21.dp
    val CIRCLE_SIZE = 38.dp
    val CIRCLE_STROKE = 1.dp
    val INNER_CONTROL_SPACING = 8.dp

    /** How far the title/controls column starts from the row's start edge. */
    val COLUMN_LEADING_OFFSET = ROW_START_PADDING + ALBUM_ART_SIZE + ART_SPACING
}

/** The Media Controls widget's alphas (MediaControlsColors) for the outlined circles and art placeholder. */
private const val PROMINENT_CIRCLE_BORDER_ALPHA = 0.35f
private const val ART_PLACEHOLDER_ALPHA = 0.15f
private const val ART_PLACEHOLDER_ICON_ALPHA = 0.7f

private enum class ControlStyle { PLAIN, PROMINENT }

/**
 * Filled or outlined rounded shapes in [color], drawn as bitmaps as the Media Controls widget does:
 * some hosts fill a tinted shape drawable's whole bounds, so an outline would draw as a solid disc.
 */
private class NowPlayingShapes(
    private val context: Context,
    val color: Color,
) {
    /** Black or white, whichever reads on [color], like the Media Controls widget's play/pause icon. */
    val onColor: Color get() = if (color.luminance() > 0.5f) Color.Black else Color.White

    @Composable
    fun Shape(
        size: Dp,
        cornerRadius: Dp,
        fill: Color = Color.Transparent,
        stroke: Color? = null,
    ) {
        val density = context.resources.displayMetrics.density
        val sizePx = (size.value * density).roundToInt().coerceAtLeast(1)
        val bitmap =
            WidgetBitmapUtils.createWidgetBitmap(
                widthPx = sizePx,
                heightPx = sizePx,
                backgroundColor = fill,
                borderColor = stroke,
                borderWidthPx = if (stroke != null) (NowPlayingRowDimens.CIRCLE_STROKE.value * density).roundToInt().coerceAtLeast(1) else 0,
                cornerRadiusPx = cornerRadius.value * density,
            )
        Image(provider = ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(size))
    }
}

private class Control(
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    val command: MediaCommand,
    /** An app seek button: its own icon, label and custom action, with [command] as the fallback. */
    val appIcon: Bitmap? = null,
    val appLabel: String? = null,
    val customAction: String? = null,
)

/** The app's seek button for one direction, or ours when the app has none for it. */
private fun seekControl(
    media: MediaControlsSnapshot,
    action: AppSeekAction?,
    appIcon: Bitmap?,
    icon: ImageVector,
    @StringRes labelRes: Int,
    command: MediaCommand,
): Control? =
    if (action != null) {
        Control(icon, labelRes, command, appIcon, action.label.takeIf { it.isNotBlank() }, action.action)
    } else {
        Control(icon, labelRes, command).takeIf { media.canSeek }
    }

/**
 * Home's Now Playing row: album art, the title with a close button, and the controls below. The
 * pair that suits the media (seeking for long media, track skipping otherwise) sits in outlined
 * circles beside play/pause, and the other pair is centered in the outer slots. [rowWidth] is the
 * width the row is laid out in, which centers the controls when there are no outer slots.
 * [shapeColor] is the title color in the current theme, for the circles and art placeholder.
 */
@Composable
internal fun AtAGlanceWidgetNowPlayingRow(
    context: Context,
    media: MediaControlsSnapshot,
    colors: AtAGlanceWidgetRowColors,
    textScale: Float,
    rowWidth: Dp,
    shapeColor: Color,
) {
    val shapes = NowPlayingShapes(context, shapeColor)
    // Only a foreground app may start another app's activity, which a widget callback is not.
    val openAction = actionStartActivity(MediaControlsOpenPlayerActivity.createIntent(context))
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(
                    start = NowPlayingRowDimens.ROW_START_PADDING,
                    top = NowPlayingRowDimens.ROW_TOP_PADDING,
                    bottom = NowPlayingRowDimens.ROW_BOTTOM_PADDING,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(context, media, shapes, openAction)
        Spacer(modifier = GlanceModifier.width(NowPlayingRowDimens.ART_SPACING))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = media.title.orEmpty(),
                    modifier = GlanceModifier.defaultWeight().clickable(openAction),
                    style = TextStyle(color = colors.title, fontSize = AtAGlanceWidgetDimens.TITLE_FONT_SIZE * textScale),
                    maxLines = 1,
                )
                Box(
                    modifier =
                        GlanceModifier
                            .size(NowPlayingRowDimens.DISMISS_BUTTON_SIZE)
                            .clickable(
                                onClick =
                                    actionRunCallback<AtAGlanceWidgetDismissAction>(
                                        AtAGlanceWidgetDismissAction.parameters(AtAGlanceWidgetDismissType.NOW_PLAYING),
                                    ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(AtAGlanceWidgetIcons.vector(context, MaterialSymbols.Close)),
                        contentDescription = context.getString(R.string.common_close),
                        modifier = GlanceModifier.size(AtAGlanceWidgetDimens.DISMISS_ICON_SIZE),
                        colorFilter = ColorFilter.tint(colors.subtitle),
                    )
                }
            }
            Controls(context, media, colors, shapes, rowWidth)
        }
    }
}

@Composable
private fun AlbumArt(
    context: Context,
    media: MediaControlsSnapshot,
    shapes: NowPlayingShapes,
    openAction: Action,
) {
    val density = context.resources.displayMetrics.density
    val sizePx = (NowPlayingRowDimens.ALBUM_ART_SIZE.value * density).roundToInt().coerceAtLeast(1)
    val art = media.albumArt?.let { roundedAlbumArt(it, sizePx, NowPlayingRowDimens.ALBUM_ART_CORNER_RADIUS.value * density) }
    val modifier =
        GlanceModifier
            .size(NowPlayingRowDimens.ALBUM_ART_SIZE)
            .clickable(openAction)
    if (art != null) {
        Image(provider = ImageProvider(art), contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            shapes.Shape(
                NowPlayingRowDimens.ALBUM_ART_SIZE,
                NowPlayingRowDimens.ALBUM_ART_CORNER_RADIUS,
                fill = shapes.color.copy(alpha = ART_PLACEHOLDER_ALPHA),
            )
            Image(
                provider = ImageProvider(AtAGlanceWidgetIcons.vector(context, MaterialSymbols.MusicNote)),
                contentDescription = null,
                modifier = GlanceModifier.size(NowPlayingRowDimens.ART_PLACEHOLDER_ICON_SIZE),
                colorFilter = ColorFilter.tint(ColorProvider(shapes.color.copy(alpha = ART_PLACEHOLDER_ICON_ALPHA))),
            )
        }
    }
}

@Composable
private fun Controls(
    context: Context,
    media: MediaControlsSnapshot,
    colors: AtAGlanceWidgetRowColors,
    shapes: NowPlayingShapes,
    rowWidth: Dp,
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
            seekControl(
                media, appSeek.back, media.appSeekBackIcon, MaterialSymbols.FastRewind, R.string.media_seek_back,
                MediaCommand.SEEK_BACK,
            )
        innerForward =
            seekControl(
                media, appSeek.forward, media.appSeekForwardIcon, MaterialSymbols.FastForward, R.string.media_seek_forward,
                MediaCommand.SEEK_FORWARD,
            )
    } else {
        val skipBack = Control(MaterialSymbols.SkipPrevious, R.string.media_command_previous, MediaCommand.PREVIOUS)
        val skipForward = Control(MaterialSymbols.SkipNext, R.string.media_command_next, MediaCommand.NEXT)
        val seekBack =
            Control(MaterialSymbols.FastRewind, R.string.media_seek_back, MediaCommand.SEEK_BACK).takeIf { media.canSeek }
        val seekForward =
            Control(MaterialSymbols.FastForward, R.string.media_seek_forward, MediaCommand.SEEK_FORWARD).takeIf { media.canSeek }
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
    val hasOuterControls = outerBack != null || outerForward != null
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (hasOuterControls) {
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                outerBack?.let { ControlButton(context, it, ControlStyle.PLAIN, colors, shapes) }
            }
        } else {
            // The group alone would sit right of the row's center, since this column starts after
            // the album art; RemoteViews cannot offset, so pad it onto the row's center as home does.
            val groupCount = 1 + listOfNotNull(innerBack, innerForward).size
            val groupWidth =
                NowPlayingRowDimens.CONTROL_BUTTON_SIZE * groupCount + NowPlayingRowDimens.INNER_CONTROL_SPACING * (groupCount - 1)
            val columnWidth = rowWidth - NowPlayingRowDimens.COLUMN_LEADING_OFFSET
            val startPadding =
                ((columnWidth - NowPlayingRowDimens.COLUMN_LEADING_OFFSET - groupWidth) / 2 + CenteredOnCardNudge)
                    .coerceIn(0.dp, (columnWidth - groupWidth).coerceAtLeast(0.dp))
            Spacer(modifier = GlanceModifier.width(startPadding))
        }
        innerBack?.let {
            ControlButton(context, it, ControlStyle.PROMINENT, colors, shapes)
            Spacer(modifier = GlanceModifier.width(NowPlayingRowDimens.INNER_CONTROL_SPACING))
        }
        PlayPauseButton(context, media.isPlaying, colors, shapes)
        innerForward?.let {
            Spacer(modifier = GlanceModifier.width(NowPlayingRowDimens.INNER_CONTROL_SPACING))
            ControlButton(context, it, ControlStyle.PROMINENT, colors, shapes)
        }
        if (hasOuterControls) {
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                outerForward?.let { ControlButton(context, it, ControlStyle.PLAIN, colors, shapes) }
            }
        }
    }
}

private fun commandAction(
    command: MediaCommand,
    customAction: String? = null,
): Action =
    actionRunCallback<AtAGlanceWidgetNowPlayingAction>(
        if (customAction != null) {
            actionParametersOf(
                CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY to command.value,
                CustomButtonsWidgetMediaAction.CUSTOM_ACTION_KEY to customAction,
            )
        } else {
            actionParametersOf(CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY to command.value)
        },
    )

/** Play/pause in a filled circle of the title color. */
@Composable
private fun PlayPauseButton(
    context: Context,
    isPlaying: Boolean,
    colors: AtAGlanceWidgetRowColors,
    shapes: NowPlayingShapes,
) {
    Box(
        modifier =
            GlanceModifier
                .size(NowPlayingRowDimens.CONTROL_BUTTON_SIZE)
                .clickable(commandAction(MediaCommand.PLAY_PAUSE)),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            shapes.Shape(NowPlayingRowDimens.CIRCLE_SIZE, NowPlayingRowDimens.CIRCLE_SIZE / 2, fill = shapes.color)
            ControlIcon(
                ImageProvider(AtAGlanceWidgetIcons.vector(context, if (isPlaying) MaterialSymbols.Pause else MaterialSymbols.PlayArrow)),
                context.getString(R.string.media_command_play_pause),
                NowPlayingRowDimens.CONTROL_ICON_SIZE,
                ColorProvider(shapes.onColor),
            )
        }
    }
}

/** A skip or seek control; [ControlStyle.PROMINENT] ones sit in an outlined circle beside play/pause. */
@Composable
private fun ControlButton(
    context: Context,
    control: Control,
    style: ControlStyle,
    colors: AtAGlanceWidgetRowColors,
    shapes: NowPlayingShapes,
) {
    val provider = control.appIcon?.let(::ImageProvider) ?: ImageProvider(AtAGlanceWidgetIcons.vector(context, control.icon))
    val contentDescription = control.appLabel ?: context.getString(control.labelRes)
    Box(
        modifier =
            GlanceModifier
                .size(NowPlayingRowDimens.CONTROL_BUTTON_SIZE)
                .clickable(commandAction(control.command, control.customAction)),
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            ControlStyle.PLAIN ->
                ControlIcon(provider, contentDescription, NowPlayingRowDimens.SECONDARY_CONTROL_ICON_SIZE, colors.title)
            ControlStyle.PROMINENT ->
                Box(contentAlignment = Alignment.Center) {
                    shapes.Shape(
                        NowPlayingRowDimens.CIRCLE_SIZE,
                        NowPlayingRowDimens.CIRCLE_SIZE / 2,
                        stroke = shapes.color.copy(alpha = PROMINENT_CIRCLE_BORDER_ALPHA),
                    )
                    ControlIcon(provider, contentDescription, NowPlayingRowDimens.CONTROL_ICON_SIZE, colors.title)
                }
        }
    }
}

@Composable
private fun ControlIcon(
    provider: ImageProvider,
    contentDescription: String,
    size: Dp,
    tint: ColorProvider,
) {
    Image(
        provider = provider,
        contentDescription = contentDescription,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
