package com.tk.quicksearch.widgets.remindersWidget

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetDimens
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetRow
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PreviewWallpaperPadding = 16.dp

/** Rows the preview shows; enough to judge the colors without pushing the settings off screen. */
private const val PREVIEW_MAX_ROWS = 3

/** Settings-screen preview of [RemindersWidget] over the wallpaper, with the user's own reminders. */
@Composable
fun RemindersWidgetPreview(
    config: RemindersWidgetConfig,
    wallpaperBitmap: ImageBitmap?,
) {
    val context = LocalContext.current
    // Keeps the last rows while the next ones load, so dragging the days slider doesn't flicker.
    val rows by produceState<List<AtAGlanceWidgetRow>?>(initialValue = null, config.upcomingDays) {
        value =
            withContext(Dispatchers.IO) {
                loadRemindersWidgetSnapshot(context, config.upcomingDays).rows.take(PREVIEW_MAX_ROWS)
            }
    }
    val style = config.style
    val colors = NoteWidgetColors.forWidget(style, isSystemDark = isSystemInDarkTheme())
    // Launchers draw the widget at the system font scale only, so the preview drops the app's multiplier.
    val density = LocalDensity.current
    val widgetDensity = Density(density.density, LocalConfiguration.current.fontScale)
    val shape = RoundedCornerShape(style.borderRadiusDp.dp)

    Box(
        modifier = Modifier.fillMaxWidth().clip(AppShapes.large),
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
        CompositionLocalProvider(LocalDensity provides widgetDensity) {
            Column(
                modifier =
                    Modifier
                        .padding(PreviewWallpaperPadding)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(colors.background, shape),
            ) {
                Text(
                    text = stringResource(R.string.section_reminders),
                    style =
                        TextStyle(
                            color = colors.title,
                            fontSize = NoteWidgetDimens.TITLE_FONT_SIZE * style.textScale,
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines = 1,
                    modifier =
                        Modifier.padding(
                            start = NoteWidgetDimens.CONTENT_PADDING,
                            top = NoteWidgetDimens.CONTENT_PADDING,
                            end = NoteWidgetDimens.CONTENT_PADDING,
                        ),
                )
                val loaded = rows
                when {
                    // Still loading.
                    loaded == null -> Spacer(modifier = Modifier.height(NoteWidgetDimens.CONTENT_PADDING))
                    loaded.isEmpty() ->
                        Text(
                            text = stringResource(R.string.widget_reminders_empty),
                            style = TextStyle(color = colors.placeholder, fontSize = NoteWidgetDimens.BODY_FONT_SIZE * style.textScale),
                            modifier =
                                Modifier.padding(
                                    start = NoteWidgetDimens.CONTENT_PADDING,
                                    top = NoteWidgetDimens.TITLE_BODY_SPACING,
                                    end = NoteWidgetDimens.CONTENT_PADDING,
                                    bottom = NoteWidgetDimens.CONTENT_PADDING,
                                ),
                        )
                    else ->
                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = AtAGlanceWidgetDimens.rowListHorizontalPadding(style.backgroundAlpha),
                                    vertical = AtAGlanceWidgetDimens.LIST_VERTICAL_PADDING,
                                ),
                        ) {
                            loaded.forEachIndexed { index, row ->
                                PreviewRow(row, colors, style.textScale)
                                if (index < loaded.lastIndex) {
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
        Image(
            painter = painterResource(R.drawable.ic_reminder),
            contentDescription = null,
            modifier = Modifier.size(AtAGlanceWidgetDimens.ICON_SIZE),
            colorFilter = ColorFilter.tint(colors.title),
        )
        Spacer(modifier = Modifier.width(AtAGlanceWidgetDimens.ICON_TEXT_SPACING))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = TextStyle(color = colors.title, fontSize = AtAGlanceWidgetDimens.TITLE_FONT_SIZE * textScale),
                maxLines = row.titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            row.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Spacer(modifier = Modifier.height(AtAGlanceWidgetDimens.TEXT_SPACING))
                Text(
                    text = subtitle,
                    style = TextStyle(color = colors.body, fontSize = AtAGlanceWidgetDimens.SUBTITLE_FONT_SIZE * textScale),
                )
            }
        }
        row.trailing.firstOrNull()?.icon?.let { icon ->
            Box(modifier = Modifier.size(AtAGlanceWidgetDimens.DISMISS_BUTTON_SIZE), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.title,
                    modifier = Modifier.size(AtAGlanceWidgetDimens.BUTTON_ICON_SIZE),
                )
            }
        }
    }
}
