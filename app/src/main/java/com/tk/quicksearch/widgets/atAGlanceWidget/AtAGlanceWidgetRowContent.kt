package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.md_theme_dark_error
import com.tk.quicksearch.shared.ui.theme.md_theme_dark_onError
import com.tk.quicksearch.shared.ui.theme.md_theme_light_error
import com.tk.quicksearch.shared.ui.theme.md_theme_light_onError
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors

private const val PROGRESS_TRACK_ALPHA = 0.2f

private val LightErrorScheme = lightColorScheme(error = md_theme_light_error, onError = md_theme_light_onError)
private val DarkErrorScheme = darkColorScheme(error = md_theme_dark_error, onError = md_theme_dark_onError)

/** Home's error colors from the theme that suits [colors]' text: light text sits on a dark card or wallpaper. */
internal fun atAGlanceWidgetErrorScheme(colors: NoteWidgetColors) =
    if (colors.title.luminance() > 0.5f) DarkErrorScheme else LightErrorScheme

/** The widget's text colors, plus home's error colors from [atAGlanceWidgetErrorScheme]. */
internal class AtAGlanceWidgetRowColors(
    day: NoteWidgetColors,
    night: NoteWidgetColors,
) {
    val title = DayNightColorProvider(day = day.title, night = night.title)
    val subtitle = DayNightColorProvider(day = day.body, night = night.body)
    val progressTrack =
        DayNightColorProvider(
            day = day.title.copy(alpha = PROGRESS_TRACK_ALPHA),
            night = night.title.copy(alpha = PROGRESS_TRACK_ALPHA),
        )
    val error = errorColor(day, night) { it.error }
    val onError = errorColor(day, night) { it.onError }

    fun tint(tint: AtAGlanceWidgetTint): ColorProvider =
        when (tint) {
            AtAGlanceWidgetTint.TITLE -> title
            AtAGlanceWidgetTint.SUBTITLE -> subtitle
            AtAGlanceWidgetTint.ERROR -> error
        }

    private fun errorColor(
        day: NoteWidgetColors,
        night: NoteWidgetColors,
        color: (androidx.compose.material3.ColorScheme) -> Color,
    ) = DayNightColorProvider(day = color(atAGlanceWidgetErrorScheme(day)), night = color(atAGlanceWidgetErrorScheme(night)))
}

/**
 * One row laid out like home's GlanceStatusRow: a 24dp icon, 12dp gap, the title with its subtitle
 * and extra content 2dp apart, an optional pill, then buttons and the close button.
 */
@Composable
internal fun AtAGlanceWidgetRowContent(
    context: Context,
    row: AtAGlanceWidgetRow,
    colors: AtAGlanceWidgetRowColors,
    textScale: Float,
) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = AtAGlanceWidgetDimens.ROW_VERTICAL_PADDING)
                .let { modifier ->
                    if (!row.tappable) return@let modifier
                    modifier.clickable(
                        onClick = actionStartActivity(AtAGlanceWidgetActionActivity.createIntent(context, row)),
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(context, row.icon, colors)
        Spacer(modifier = GlanceModifier.width(AtAGlanceWidgetDimens.ICON_TEXT_SPACING))
        Column(modifier = GlanceModifier.defaultWeight()) {
            RowText(context, row, colors, textScale)
        }
        row.pill?.let { pill ->
            Spacer(modifier = GlanceModifier.width(AtAGlanceWidgetDimens.ICON_TEXT_SPACING))
            Pill(context, pill, colors, textScale)
        }
        if (row.trailing.isNotEmpty()) {
            row.trailing.forEachIndexed { index, button ->
                // A wider gap after the pill keeps a value like a call's duration clear of its button.
                val gap =
                    if (index == 0 && row.pill != null && button.style == AtAGlanceWidgetButtonStyle.HANG_UP) {
                        16.dp
                    } else if (index == 0 && row.pill != null) {
                        AtAGlanceWidgetDimens.ICON_TEXT_SPACING
                    } else {
                        AtAGlanceWidgetDimens.BUTTON_SPACING
                    }
                Spacer(modifier = GlanceModifier.width(gap))
                RowButton(context, row, button, colors, textScale)
            }
        }
        if (row.dismiss != null) {
            if (row.trailing.isNotEmpty()) Spacer(modifier = GlanceModifier.width(AtAGlanceWidgetDimens.BUTTON_SPACING))
            if (row.dismissAtTop) {
                Column(modifier = GlanceModifier.fillMaxHeight(), verticalAlignment = Alignment.Top) {
                    DismissButton(context, row, colors.subtitle)
                }
            } else {
                DismissButton(context, row, colors.subtitle)
            }
        }
    }
}

@Composable
private fun RowText(
    context: Context,
    row: AtAGlanceWidgetRow,
    colors: AtAGlanceWidgetRowColors,
    textScale: Float,
) {
    val isCritical = row.titleStyle == AtAGlanceWidgetTitleStyle.CRITICAL
    Text(
        text = row.title,
        style =
            TextStyle(
                color = if (isCritical) colors.error else colors.title,
                fontSize =
                    when (row.titleStyle) {
                        AtAGlanceWidgetTitleStyle.SMALL -> AtAGlanceWidgetDimens.SUBTITLE_FONT_SIZE
                        AtAGlanceWidgetTitleStyle.LARGE -> AtAGlanceWidgetDimens.LARGE_TITLE_FONT_SIZE
                        else -> AtAGlanceWidgetDimens.TITLE_FONT_SIZE
                    } * textScale,
                fontWeight = if (isCritical) FontWeight.Medium else FontWeight.Normal,
            ),
        maxLines = row.titleMaxLines,
    )
    row.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
        TextGap()
        Text(
            text = subtitle,
            style =
                TextStyle(
                    color = colors.subtitle,
                    fontSize =
                        (if (row.largeSubtitle) AtAGlanceWidgetDimens.LARGE_SUBTITLE_FONT_SIZE else AtAGlanceWidgetDimens.SUBTITLE_FONT_SIZE) *
                            textScale,
                ),
            maxLines = row.subtitleMaxLines,
        )
    }
    row.code?.let { code ->
        TextGap()
        // The code and its icon copy; the rest of the row opens the message.
        Row(
            modifier =
                GlanceModifier.clickable(
                    onClick =
                        actionStartActivity(
                            AtAGlanceWidgetActionActivity.createIntent(
                                context,
                                row,
                                AtAGlanceWidgetTap(AtAGlanceWidgetTapType.OTP, key = row.tap.key, packageName = row.tap.packageName),
                            ),
                        ),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                style =
                    TextStyle(
                        color = colors.title,
                        fontSize = AtAGlanceWidgetDimens.CODE_FONT_SIZE * textScale,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(AtAGlanceWidgetDimens.BUTTON_SPACING))
            VectorImage(context, MaterialSymbols.ContentCopy, colors.subtitle, AtAGlanceWidgetDimens.BUTTON_ICON_SIZE)
        }
    }
    row.footer?.let { footer ->
        TextGap()
        Text(
            text = footer,
            style = TextStyle(color = colors.subtitle, fontSize = AtAGlanceWidgetDimens.FOOTER_FONT_SIZE * textScale),
            maxLines = 1,
        )
    }
    row.progress?.let { progress ->
        TextGap()
        if (row.progressFlag) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.defaultWeight().height(AtAGlanceWidgetDimens.PROGRESS_HEIGHT),
                    color = colors.title,
                    backgroundColor = colors.progressTrack,
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                VectorImage(context, MaterialSymbols.Flag, colors.title, 16.dp)
            }
        } else {
            Spacer(modifier = GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(AtAGlanceWidgetDimens.PROGRESS_HEIGHT).padding(end = 4.dp),
                color = colors.title,
                backgroundColor = colors.progressTrack,
            )
        }
    }
    if (row.chips.isNotEmpty()) {
        Spacer(modifier = GlanceModifier.height(AtAGlanceWidgetDimens.CHIP_TOP_SPACING))
        Row {
            row.chips.forEachIndexed { index, chip ->
                if (index > 0) Spacer(modifier = GlanceModifier.width(AtAGlanceWidgetDimens.BUTTON_SPACING))
                RowButton(context, row, chip, colors, textScale)
            }
        }
    }
}

@Composable
private fun TextGap() {
    Spacer(modifier = GlanceModifier.height(AtAGlanceWidgetDimens.TEXT_SPACING))
}

@Composable
private fun Pill(
    context: Context,
    pill: AtAGlanceWidgetPill,
    colors: AtAGlanceWidgetRowColors,
    textScale: Float,
) {
    // Home's neutral pill is the title color at 12% alpha, which the faint drawable carries.
    val (background, container, content) =
        when (pill.style) {
            AtAGlanceWidgetPillStyle.NEUTRAL -> Triple(R.drawable.widget_at_a_glance_pill_faint, colors.title, colors.title)
            AtAGlanceWidgetPillStyle.ERROR -> Triple(R.drawable.widget_at_a_glance_pill, colors.error, colors.onError)
        }
    Box(
        modifier =
            GlanceModifier
                .background(ImageProvider(background), colorFilter = ColorFilter.tint(container))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pill.icon == null) {
            LabelText(pill.text, content, textScale)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VectorImage(context, pill.icon, content, 16.dp)
                Spacer(modifier = GlanceModifier.width(4.dp))
                LabelText(pill.text, content, textScale)
            }
        }
    }
}

@Composable
private fun LabelText(
    text: String,
    color: ColorProvider,
    textScale: Float,
) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = AtAGlanceWidgetDimens.LABEL_FONT_SIZE * textScale, fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}

/** Home's row buttons; the nested clickable keeps a button's tap from also opening the row. */
@Composable
private fun RowButton(
    context: Context,
    row: AtAGlanceWidgetRow,
    button: AtAGlanceWidgetButton,
    colors: AtAGlanceWidgetRowColors,
    textScale: Float,
) {
    val click =
        GlanceModifier.clickable(
            onClick = actionStartActivity(AtAGlanceWidgetActionActivity.createIntent(context, row, button.tap)),
        )
    when (button.style) {
        AtAGlanceWidgetButtonStyle.HANG_UP ->
            CircleButton(
                context, button, R.drawable.widget_at_a_glance_circle, colors.error, colors.onError,
                AtAGlanceWidgetDimens.HANG_UP_BUTTON_SIZE,
                AtAGlanceWidgetDimens.BUTTON_ICON_SIZE, click,
            )
        AtAGlanceWidgetButtonStyle.TONAL ->
            CircleButton(
                context, button, R.drawable.widget_at_a_glance_circle_faint, colors.title, colors.title, button.size,
                AtAGlanceWidgetDimens.BUTTON_ICON_SIZE, click,
            )
        AtAGlanceWidgetButtonStyle.ICON ->
            Box(
                modifier = GlanceModifier.size(AtAGlanceWidgetDimens.DISMISS_BUTTON_SIZE).then(click),
                contentAlignment = Alignment.Center,
            ) {
                ButtonIcon(context, button, colors.title, AtAGlanceWidgetDimens.BUTTON_ICON_SIZE)
            }
        AtAGlanceWidgetButtonStyle.CHIP ->
            Row(
                modifier =
                    GlanceModifier
                        .background(ImageProvider(R.drawable.widget_at_a_glance_pill_faint), colorFilter = ColorFilter.tint(colors.title))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .then(click),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (button.icon != null || button.appIcon != null) {
                    ButtonIcon(context, button, colors.title, AtAGlanceWidgetDimens.CHIP_ICON_SIZE)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                LabelText(button.label, colors.title, textScale)
            }
        AtAGlanceWidgetButtonStyle.OUTLINED_CHIP ->
            Row(
                modifier =
                    GlanceModifier
                        .background(ImageProvider(R.drawable.widget_at_a_glance_pill_faint), colorFilter = ColorFilter.tint(colors.title))
                        // A leading app icon sits closer to the edge; a bare label is centered.
                        .padding(start = if (button.appPackage != null) 8.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                        .then(click),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                button.appPackage?.let { packageName ->
                    AppImage(context, packageName, colors, AtAGlanceWidgetDimens.CHIP_ICON_SIZE)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                LabelText(button.label, colors.title, textScale)
            }
    }
}

@Composable
private fun CircleButton(
    context: Context,
    button: AtAGlanceWidgetButton,
    @DrawableRes background: Int,
    container: ColorProvider,
    content: ColorProvider,
    size: Dp,
    iconSize: Dp,
    click: GlanceModifier,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .background(ImageProvider(background), colorFilter = ColorFilter.tint(container))
                .then(click),
        contentAlignment = Alignment.Center,
    ) {
        ButtonIcon(context, button, content, iconSize)
    }
}

@Composable
private fun ButtonIcon(
    context: Context,
    button: AtAGlanceWidgetButton,
    tint: ColorProvider,
    size: Dp,
) {
    val provider =
        button.appIcon?.let(::ImageProvider)
            ?: button.icon?.let { ImageProvider(AtAGlanceWidgetIcons.vector(context, it)) }
            ?: return
    Image(
        provider = provider,
        contentDescription = button.label,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Home's close button; the nested clickable keeps its tap from also opening the row. */
@Composable
private fun DismissButton(
    context: Context,
    row: AtAGlanceWidgetRow,
    tint: ColorProvider,
) {
    Box(
        modifier =
            GlanceModifier
                .size(AtAGlanceWidgetDimens.DISMISS_BUTTON_SIZE)
                .clickable(
                    onClick = actionRunCallback<AtAGlanceWidgetDismissAction>(AtAGlanceWidgetDismissAction.parameters(row)),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(AtAGlanceWidgetIcons.vector(context, MaterialSymbols.Close)),
            contentDescription = context.getString(R.string.common_close),
            modifier = GlanceModifier.size(AtAGlanceWidgetDimens.DISMISS_ICON_SIZE),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
private fun RowIcon(
    context: Context,
    icon: AtAGlanceWidgetIcon,
    colors: AtAGlanceWidgetRowColors,
) {
    val size = AtAGlanceWidgetDimens.ICON_SIZE
    when (icon) {
        is AtAGlanceWidgetIcon.Vector -> VectorImage(context, icon.vector, colors.tint(icon.tint), size)
        is AtAGlanceWidgetIcon.Drawable ->
            Image(
                provider = ImageProvider(icon.resId),
                contentDescription = null,
                modifier = GlanceModifier.size(size),
                colorFilter = ColorFilter.tint(colors.tint(icon.tint)),
            )
        is AtAGlanceWidgetIcon.App -> AppImage(context, icon.packageName, colors, size)
        is AtAGlanceWidgetIcon.Battery ->
            Box(modifier = GlanceModifier.size(size)) {
                Image(
                    provider = ImageProvider(AtAGlanceWidgetIcons.batteryOutline(context)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(size),
                    colorFilter = ColorFilter.tint(if (icon.isCritical) colors.error else colors.subtitle),
                )
                Image(
                    provider = ImageProvider(AtAGlanceWidgetIcons.batteryFill(context, icon.percent)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(size),
                    colorFilter = ColorFilter.tint(colors.error),
                )
            }
    }
}

@Composable
private fun VectorImage(
    context: Context,
    vector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: ColorProvider,
    size: Dp,
) {
    Image(
        provider = ImageProvider(AtAGlanceWidgetIcons.vector(context, vector)),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** The app's icon untinted, or a generic app icon in the subtitle color as on home. */
@Composable
private fun AppImage(
    context: Context,
    packageName: String,
    colors: AtAGlanceWidgetRowColors,
    size: Dp,
) {
    val appIcon = AtAGlanceWidgetIcons.app(context, packageName)
    Image(
        provider = ImageProvider(appIcon ?: AtAGlanceWidgetIcons.fallbackAppIcon(context)),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = if (appIcon == null) ColorFilter.tint(colors.subtitle) else null,
    )
}
