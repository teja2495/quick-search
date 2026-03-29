package com.tk.quicksearch.searchEngines.inline

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.util.hapticConfirm

/**
 * Composable for displaying a single search engine icon item.
 *
 * @param engine The search engine to display
 * @param query The search query to pass when clicked
 * @param iconSize The size of the icon
 * @param itemWidth The width of the clickable area
 * @param onSearchEngineClick Callback when the engine is clicked
 */
@Composable
fun SearchEngineIconItem(
    engine: SearchTarget,
    query: String,
    iconSize: Dp,
    itemWidth: Dp,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: (() -> Unit)? = null,
    isPredicted: Boolean = false,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val compactVisualIconSize = (iconSize - 2.dp).coerceAtLeast(18.dp)
    val isLightMode = !LocalAppIsDarkTheme.current
    val iconShadowModifier = if (isLightMode) {
        Modifier.shadow(
            elevation = 8.dp,
            shape = CircleShape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.3f),
            spotColor = Color.Black.copy(alpha = 0.5f),
        )
    } else {
        Modifier
    }
    val highlightExtraWidth = 8.dp
    val highlightExtraHeight = 12.dp
    val highlightShape = RoundedCornerShape(18.dp)
    val highlightBackgroundColor = AppColors.InlineEngineHighlightBackground
    val highlightBorderColor = AppColors.InlineEngineHighlightBorder
    Box(
        modifier =
            modifier
                .width(itemWidth)
                .then(
                    if (!isPredicted) {
                        Modifier
                    } else {
                        Modifier.drawBehind {
                            val extraWidthPx = highlightExtraWidth.toPx()
                            val extraHeightPx = highlightExtraHeight.toPx()
                            val left = -extraWidthPx / 2f
                            val top = -extraHeightPx / 2f
                            val outlineWidth = size.width + extraWidthPx
                            val outlineHeight = size.height + extraHeightPx
                            val strokeWidth = 1.dp.toPx()
                            val cornerRadius = highlightShape.topStart.toPx(size, this)

                            drawRoundRect(
                                color = highlightBackgroundColor,
                                topLeft = Offset(left, top),
                                size = Size(outlineWidth, outlineHeight),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                            )
                            drawRoundRect(
                                color = highlightBorderColor,
                                topLeft = Offset(left, top),
                                size = Size(outlineWidth, outlineHeight),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                style = Stroke(width = strokeWidth),
                            )
                        }
                    },
                )
                .combinedClickable(
                    onClick = {
                        hapticConfirm(view)()
                        onSearchEngineClick(query, engine)
                    },
                    onLongClick = onSearchEngineLongPress,
                ),
        contentAlignment = Alignment.Center,
    ) {
        SearchTargetIcon(
            target = engine,
            iconSize = compactVisualIconSize,
            style = IconRenderStyle.ADVANCED,
            appIconShape = appIconShape,
            modifier = iconShadowModifier,
        )
    }
}
