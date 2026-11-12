package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

private val PreviewWallpaperPadding = 16.dp
private const val TICK_MILLIS = 1_000L

/** Configure-screen preview of [WorldClockWidget] at a 4x1 size, drawn over the wallpaper. */
@Composable
fun WorldClockWidgetPreview(
    config: WorldClockWidgetConfig,
    wallpaperBitmap: ImageBitmap?,
) {
    val context = LocalContext.current
    val colors = WorldClockWidgetColors.forWidget(config, isSystemDark = isSystemInDarkTheme())
    val fontScale = LocalConfiguration.current.fontScale
    val is24Hour = remember { WorldClockFormats.is24Hour(context) }
    val names = config.locations.map(WorldClockZones::displayName)
    val widths = remember(is24Hour, fontScale, names) { WorldClockFormats.measure(context, is24Hour, names) }
    val timeFormatter =
        remember(is24Hour) {
            runCatching { DateTimeFormatter.ofPattern(WorldClockFormats.timePattern(is24Hour)) }
                .getOrElse { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
        }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS - System.currentTimeMillis() % TICK_MILLIS)
            nowMillis = System.currentTimeMillis()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Explicit height: paint() without intrinsic sizing expands to the max constraints.
                .height(WorldClockLayout.PREVIEW_HEIGHT + PreviewWallpaperPadding * 2)
                .clip(DesignTokens.ShapeLarge)
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
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background, RoundedCornerShape(config.style.borderRadiusDp.dp))
                    .padding(horizontal = WorldClockLayout.HORIZONTAL_PADDING, vertical = WorldClockLayout.VERTICAL_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            if (config.locations.isEmpty()) {
                Text(
                    text = stringResource(R.string.widget_world_clock_add_location),
                    style =
                        TextStyle(
                            color = colors.secondaryText,
                            fontSize = WorldClockLayout.PLACEHOLDER_TEXT_SIZE_SP.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                )
                return@BoxWithConstraints
            }
            val layout =
                WorldClockLayout.cellLayout(
                    // The constraints are inside the padding; the layout expects the whole widget.
                    widthDp = maxWidth.value + WorldClockLayout.HORIZONTAL_PADDING.value * 2,
                    heightDp = maxHeight.value + WorldClockLayout.VERTICAL_PADDING.value * 2,
                    clockCount = config.locations.size,
                    preferredTimeSizeSp = config.textSizeSp,
                    widths = widths,
                    fontScale = fontScale,
                )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WorldClockLayout.CELL_SPACING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val now = Instant.ofEpochMilli(nowMillis)
                config.locations.forEach { location ->
                    PreviewClockCell(
                        location = location,
                        time = now.atZone(ZoneId.of(location.zoneId)).format(timeFormatter),
                        layout = layout,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewClockCell(
    location: WorldClockLocation,
    time: String,
    layout: WorldClockCellLayout,
    colors: WorldClockWidgetColors,
    modifier: Modifier,
) {
    val subtitlePattern = WorldClockFormats.subtitlePattern()
    val subtitle =
        remember(subtitlePattern, location.zoneId) {
            runCatching {
                Instant.now().atZone(ZoneId.of(location.zoneId)).format(DateTimeFormatter.ofPattern(subtitlePattern))
            }.getOrDefault("")
        }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (layout.showLabel) {
            CellText(
                text = WorldClockZones.displayName(location),
                style =
                    TextStyle(
                        color = colors.secondaryText,
                        fontSize = layout.labelSizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                    ),
            )
        }
        CellText(
            text = time,
            style =
                TextStyle(
                    color = colors.text,
                    fontSize = layout.timeSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    // The font WorldClockFormats measures, as on the placed widget.
                    fontFamily = FontFamily.SansSerif,
                ),
        )
        if (layout.showSubtitle) {
            CellText(
                text = subtitle,
                style = TextStyle(color = colors.secondaryText, fontSize = layout.subtitleSizeSp.sp, fontFamily = FontFamily.SansSerif),
            )
        }
    }
}

@Composable
private fun CellText(
    text: String,
    style: TextStyle,
) {
    Text(
        text = text,
        style = style.copy(textAlign = TextAlign.Center),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}
