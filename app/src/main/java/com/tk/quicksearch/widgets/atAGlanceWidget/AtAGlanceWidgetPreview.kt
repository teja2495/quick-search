package com.tk.quicksearch.widgets.atAGlanceWidget

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors
import com.tk.quicksearch.widgets.utils.WidgetPreferences

private val PreviewWallpaperPadding = 16.dp

/**
 * Settings-screen preview of [AtAGlanceWidget] over the wallpaper, showing sample items (the same
 * ones as the launcher picker's preview layout) so the colors can be judged against typical rows.
 */
@Composable
fun AtAGlanceWidgetPreview(
    state: WidgetPreferences,
    wallpaperBitmap: ImageBitmap?,
    launcherContentScale: Float = 1f,
) {
    val context = LocalContext.current
    val colors = NoteWidgetColors.forWidget(state, isSystemDark = isSystemInDarkTheme())
    // The app's font size setting scales this screen's text, but launchers draw the widget at the
    // system font scale only, so the preview drops the app's multiplier to match. Some launchers
    // also shrink the widget's content to their grid, so the rows are scaled the same way.
    val density = LocalDensity.current
    val widgetDensity = Density(density.density * launcherContentScale, LocalConfiguration.current.fontScale)
    val rows = remember(context) { atAGlanceWidgetSampleRows(context) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.large),
        contentAlignment = Alignment.Center,
    ) {
        // Drawn behind at the card's size, so the preview is only as tall as its rows.
        wallpaperBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        val shape = RoundedCornerShape(state.borderRadiusDp.dp)
        Column(
            modifier =
                Modifier
                    .padding(PreviewWallpaperPadding)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.background, shape),
        ) {
            CompositionLocalProvider(LocalDensity provides widgetDensity) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = AtAGlanceWidgetDimens.rowListHorizontalPadding(state.backgroundAlpha),
                            vertical = AtAGlanceWidgetDimens.LIST_VERTICAL_PADDING,
                        ),
                ) {
                    rows.forEachIndexed { index, row ->
                        PreviewRow(row, colors, state.textScale)
                        if (index < rows.lastIndex) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(AtAGlanceWidgetDimens.DIVIDER_HEIGHT)
                                        .background(colors.title.copy(alpha = AtAGlanceWidgetDimens.DIVIDER_ALPHA)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(
    row: AtAGlanceWidgetRow,
    colors: NoteWidgetColors,
    textScale: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AtAGlanceWidgetDimens.ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewIcon(row.icon, colors)
        Spacer(modifier = Modifier.width(AtAGlanceWidgetDimens.ICON_TEXT_SPACING))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style =
                    TextStyle(
                        color = colors.title,
                        fontSize = AtAGlanceWidgetDimens.TITLE_FONT_SIZE * textScale,
                    ),
            )
            row.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Spacer(modifier = Modifier.height(AtAGlanceWidgetDimens.TEXT_SPACING))
                Text(
                    text = subtitle,
                    style = TextStyle(color = colors.body, fontSize = AtAGlanceWidgetDimens.SUBTITLE_FONT_SIZE * textScale),
                )
            }
        }
        if (row.dismiss != null) {
            Box(modifier = Modifier.size(AtAGlanceWidgetDimens.DISMISS_BUTTON_SIZE), contentAlignment = Alignment.Center) {
                PreviewIcon(
                    AtAGlanceWidgetIcon.Vector(MaterialSymbols.Close, AtAGlanceWidgetTint.SUBTITLE),
                    colors,
                    AtAGlanceWidgetDimens.DISMISS_ICON_SIZE,
                )
            }
        }
    }
}

@Composable
private fun PreviewIcon(
    icon: AtAGlanceWidgetIcon,
    colors: NoteWidgetColors,
    size: Dp = AtAGlanceWidgetDimens.ICON_SIZE,
) {
    val context = LocalContext.current
    val modifier = Modifier.size(size)
    fun tint(tint: AtAGlanceWidgetTint) =
        when (tint) {
            AtAGlanceWidgetTint.TITLE -> colors.title
            AtAGlanceWidgetTint.SUBTITLE -> colors.body
            AtAGlanceWidgetTint.ERROR -> atAGlanceWidgetErrorScheme(colors).error
        }
    when (icon) {
        is AtAGlanceWidgetIcon.Vector ->
            Image(
                bitmap = AtAGlanceWidgetIcons.vector(context, icon.vector).asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                colorFilter = ColorFilter.tint(tint(icon.tint)),
            )
        is AtAGlanceWidgetIcon.Drawable ->
            Image(
                painter = painterResource(icon.resId),
                contentDescription = null,
                modifier = modifier,
                colorFilter = ColorFilter.tint(tint(icon.tint)),
            )
        is AtAGlanceWidgetIcon.App -> {
            val appIcon = AtAGlanceWidgetIcons.app(context, icon.packageName)
            Image(
                bitmap = (appIcon ?: AtAGlanceWidgetIcons.fallbackAppIcon(context)).asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                colorFilter = if (appIcon == null) ColorFilter.tint(colors.body) else null,
            )
        }
        // The sample rows have no battery or navigation row.
        is AtAGlanceWidgetIcon.Battery, is AtAGlanceWidgetIcon.Maneuver -> Unit
    }
}

/**
 * Typical rows, matching widget_at_a_glance_placeholder (the launcher picker's preview), so the
 * preview shows what the widget is for before the user has any items.
 */
private fun atAGlanceWidgetSampleRows(context: Context): List<AtAGlanceWidgetRow> {
    fun row(
        key: String,
        icon: Int,
        title: Int,
        subtitle: Int,
        tap: AtAGlanceWidgetTapType,
        dismiss: AtAGlanceWidgetDismissType,
        tint: AtAGlanceWidgetTint = AtAGlanceWidgetTint.TITLE,
    ) = AtAGlanceWidgetRow(
        key = key,
        icon = AtAGlanceWidgetIcon.Drawable(icon, tint),
        title = context.getString(title),
        subtitle = context.getString(subtitle),
        tap = AtAGlanceWidgetTap(tap),
        dismiss = dismiss,
    )
    return listOf(
        row(
            "alarm",
            R.drawable.ic_widget_glance_alarm,
            R.string.home_upcoming_alarm,
            R.string.widget_at_a_glance_sample_alarm,
            AtAGlanceWidgetTapType.ALARM,
            AtAGlanceWidgetDismissType.ALARM,
        ),
        row(
            "birthday",
            R.drawable.ic_widget_glance_birthday,
            R.string.widget_at_a_glance_sample_birthday,
            R.string.home_birthday_wish,
            AtAGlanceWidgetTapType.BIRTHDAY,
            AtAGlanceWidgetDismissType.BIRTHDAY,
        ),
        row(
            "missed-calls",
            R.drawable.ic_widget_glance_missed_call,
            R.string.home_missed_call,
            R.string.widget_at_a_glance_sample_caller,
            AtAGlanceWidgetTapType.MISSED_CALLS,
            AtAGlanceWidgetDismissType.MISSED_CALLS,
            AtAGlanceWidgetTint.ERROR,
        ),
    )
}
