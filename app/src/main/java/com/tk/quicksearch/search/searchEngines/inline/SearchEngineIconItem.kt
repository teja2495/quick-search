package com.tk.quicksearch.search.searchEngines.inline

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.util.hapticConfirm

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
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val highlightExtraWidth = 8.dp
    val highlightExtraHeight = 12.dp
    val highlightShape = RoundedCornerShape(18.dp)
    val highlightBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val highlightBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    Box(
        modifier =
            modifier
                .width(if (isPredicted) itemWidth + highlightExtraWidth else itemWidth)
                .then(
                    if (!isPredicted) {
                        Modifier
                    } else {
                        Modifier.drawBehind {
                            val extraHeightPx = highlightExtraHeight.toPx()
                            val top = -extraHeightPx / 2f
                            val outlineHeight = size.height + extraHeightPx
                            val strokeWidth = 1.dp.toPx()
                            val cornerRadius = highlightShape.topStart.toPx(size, this)

                            drawRoundRect(
                                color = highlightBackgroundColor,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, outlineHeight),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                            )
                            drawRoundRect(
                                color = highlightBorderColor,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, outlineHeight),
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
            iconSize = iconSize,
            style = IconRenderStyle.ADVANCED,
        )
    }
}
