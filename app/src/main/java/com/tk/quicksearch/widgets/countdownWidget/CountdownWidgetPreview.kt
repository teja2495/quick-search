package com.tk.quicksearch.widgets.countdownWidget

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val PreviewWallpaperPadding = 16.dp

/** Configure-screen preview of [CountdownWidget] at its default 2x2 size, drawn over the wallpaper. */
@Composable
fun CountdownWidgetPreview(
    config: CountdownWidgetConfig,
    wallpaperBitmap: ImageBitmap?,
) {
    val context = LocalContext.current
    val colors = CountdownWidgetColors.forWidget(config, isSystemDark = isSystemInDarkTheme())
    val content = countdownContent(context, config)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Explicit height: paint() without intrinsic sizing expands to the max constraints.
                .height(CountdownWidgetDimens.PREVIEW_SIZE + PreviewWallpaperPadding * 2)
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
        Column(
            modifier =
                Modifier
                    .size(CountdownWidgetDimens.PREVIEW_SIZE)
                    .background(colors.background, RoundedCornerShape(config.style.borderRadiusDp.dp))
                    .padding(CountdownWidgetDimens.CONTENT_PADDING),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (content.title != null) {
                Text(
                    text = content.title,
                    style =
                        TextStyle(
                            color = colors.secondaryText,
                            fontSize = CountdownWidgetDimens.titleSizeSp(config.textSizeSp).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(modifier = Modifier.height(CountdownWidgetDimens.TITLE_SPACING))
            }
            Text(
                text = content.text,
                style =
                    TextStyle(
                        color = if (content.isPlaceholder) colors.secondaryText else colors.text,
                        fontSize =
                            (
                                if (content.isPlaceholder) {
                                    CountdownWidgetDimens.PLACEHOLDER_TEXT_SIZE_SP
                                } else {
                                    config.textSizeSp
                                }
                            ).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (content.progress != null) {
                Box(modifier = Modifier.height(CountdownWidgetDimens.PROGRESS_SPACING))
                LinearProgressIndicator(
                    progress = { content.progress },
                    modifier = Modifier.fillMaxWidth().height(CountdownWidgetDimens.PROGRESS_HEIGHT),
                    color = colors.text,
                    trackColor = colors.progressTrack,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
