package com.tk.quicksearch.search.apps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val LazyGridScrollbarTouchWidth = DesignTokens.SpacingLarge
private val ThumbWidth = DesignTokens.SpacingXSmall
private val DraggedThumbWidth = 6.dp
private const val ThumbHideDelayMillis = 1200L
private val ThumbEndInset = 2.dp
private val MinThumbHeight = DesignTokens.Spacing48

/**
 * Vertical scrollbar for a [LazyVerticalGrid] with a draggable thumb. There is no track; the
 * thumb fades in while the grid scrolls or the thumb is dragged and fades out shortly after.
 * While dragged it widens and takes the accent color.
 *
 * While dragging, the thumb follows the finger directly and the grid is scrolled to the matching
 * pixel position. [onDragFractionChange] reports the thumb's scroll fraction during a drag and
 * `null` once the drag ends.
 */
@Composable
internal fun LazyGridVerticalScrollbar(
        state: LazyGridState,
        modifier: Modifier = Modifier,
        onDragFractionChange: (Float?) -> Unit = {},
) {
    val metrics by remember {
        derivedStateOf { scrollbarMetrics(state) }
    }
    if (metrics == null) return

    val currentMetrics = rememberUpdatedState(metrics)
    val currentOnDragFractionChange = rememberUpdatedState(onDragFractionChange)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val minThumbPx = with(density) { MinThumbHeight.toPx() }
    val isDragging = dragFraction != null
    val isActive = isDragging || state.isScrollInProgress
    var thumbVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isActive) {
        if (isActive) {
            thumbVisible = true
        } else {
            delay(ThumbHideDelayMillis)
            thumbVisible = false
        }
    }
    val thumbAlpha by animateFloatAsState(targetValue = if (thumbVisible) 1f else 0f, label = "thumbAlpha")
    val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) DraggedThumbWidth else ThumbWidth,
            label = "thumbWidth",
    )
    val thumbColor by animateColorAsState(
            targetValue =
                    if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    },
            label = "thumbColor",
    )

    // Scroll requests are conflated so a fast drag only ever applies the latest thumb position.
    LaunchedEffect(state) {
        snapshotFlow { dragFraction }
                .filterNotNull()
                .collectLatest { fraction ->
                    val snapshot = currentMetrics.value ?: return@collectLatest
                    val (index, offset) = scrollPositionForFraction(fraction, snapshot)
                    state.scrollToItem(index, offset)
                }
    }

    BoxWithConstraints(
            modifier =
                    modifier
                            .fillMaxHeight()
                            .width(LazyGridScrollbarTouchWidth)
                            .padding(end = ThumbEndInset)
                            .pointerInput(state) {
                                detectTapGestures { offset ->
                                    val snapshot = currentMetrics.value ?: return@detectTapGestures
                                    val trackHeightPx = size.height.toFloat()
                                    val thumbHeightPx = thumbHeightPx(trackHeightPx, snapshot, minThumbPx)
                                    val fraction =
                                            thumbFractionForTop(
                                                    thumbTopPx = offset.y - thumbHeightPx / 2f,
                                                    trackHeightPx = trackHeightPx,
                                                    thumbHeightPx = thumbHeightPx,
                                            )
                                    val (index, itemOffset) = scrollPositionForFraction(fraction, snapshot)
                                    coroutineScope.launch { state.scrollToItem(index, itemOffset) }
                                }
                            }
                            .pointerInput(state) {
                                var grabOffsetPx = 0f
                                fun updateDrag(y: Float) {
                                    val snapshot = currentMetrics.value ?: return
                                    val trackHeightPx = size.height.toFloat()
                                    val fraction =
                                            thumbFractionForTop(
                                                    thumbTopPx = y - grabOffsetPx,
                                                    trackHeightPx = trackHeightPx,
                                                    thumbHeightPx = thumbHeightPx(trackHeightPx, snapshot, minThumbPx),
                                            )
                                    dragFraction = fraction
                                    currentOnDragFractionChange.value(fraction)
                                }
                                fun endDrag() {
                                    dragFraction = null
                                    currentOnDragFractionChange.value(null)
                                }
                                detectVerticalDragGestures(
                                        onDragStart = { start ->
                                            val snapshot = currentMetrics.value
                                            val trackHeightPx = size.height.toFloat()
                                            val thumbHeightPx =
                                                    snapshot?.let { thumbHeightPx(trackHeightPx, it, minThumbPx) } ?: 0f
                                            val thumbTopPx =
                                                    (trackHeightPx - thumbHeightPx).coerceAtLeast(0f) *
                                                            (snapshot?.scrollFraction ?: 0f)
                                            // Keep the grab point when the thumb itself is dragged;
                                            // otherwise center the thumb under the finger.
                                            grabOffsetPx =
                                                    if (start.y in thumbTopPx..(thumbTopPx + thumbHeightPx)) {
                                                        start.y - thumbTopPx
                                                    } else {
                                                        thumbHeightPx / 2f
                                                    }
                                            updateDrag(start.y)
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { endDrag() },
                                ) { change, _ ->
                                    change.consume()
                                    updateDrag(change.position.y)
                                }
                            },
            contentAlignment = Alignment.TopEnd,
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat()
        if (trackHeightPx <= 0f) return@BoxWithConstraints
        val resolved = metrics ?: return@BoxWithConstraints
        val thumbHeightPx = thumbHeightPx(trackHeightPx, resolved, minThumbPx)
        val maxOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

        Box(
                modifier =
                        Modifier
                                .offset {
                                    val fraction = dragFraction ?: resolved.scrollFraction
                                    IntOffset(0, (maxOffsetPx * fraction).roundToInt())
                                }
                                .alpha(thumbAlpha)
                                .width(thumbWidth)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .clip(RoundedCornerShape(percent = 50))
                                .background(thumbColor),
        )
    }
}

internal data class LazyGridScrollbarMetrics(
        val totalItems: Int,
        val scrollFraction: Float,
        val thumbSizeFraction: Float,
        val columns: Int = 1,
        val rowHeightPx: Float = 0f,
        val scrollRangePx: Float = 0f,
)

/**
 * Pixel-based scroll metrics estimated from the visible rows, so the thumb moves continuously
 * instead of jumping a row at a time.
 */
internal fun scrollbarMetrics(state: LazyGridState): LazyGridScrollbarMetrics? {
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems <= 0 || visibleItems.isEmpty()) return null
    if (!state.canScrollForward && !state.canScrollBackward) return null

    val columns =
            visibleItems
                    .groupingBy { it.row }
                    .eachCount()
                    .values
                    .maxOrNull()
                    ?.coerceAtLeast(1) ?: 1
    val rowHeightPx =
            (visibleItems.maxOf { it.size.height } + layoutInfo.mainAxisItemSpacing).toFloat()
    if (rowHeightPx <= 0f) return null
    val totalRows = (totalItems + columns - 1) / columns
    val contentHeightPx =
            totalRows * rowHeightPx - layoutInfo.mainAxisItemSpacing +
                    layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
    val viewportHeightPx = layoutInfo.viewportSize.height.toFloat()
    val scrollRangePx = (contentHeightPx - viewportHeightPx).coerceAtLeast(1f)
    val scrolledPx =
            (state.firstVisibleItemIndex / columns) * rowHeightPx +
                    state.firstVisibleItemScrollOffset
    val scrollFraction =
            when {
                !state.canScrollBackward -> 0f
                !state.canScrollForward -> 1f
                else -> (scrolledPx / scrollRangePx).coerceIn(0f, 1f)
            }
    val thumbSizeFraction = (viewportHeightPx / contentHeightPx).coerceIn(0.08f, 1f)
    return LazyGridScrollbarMetrics(
            totalItems = totalItems,
            scrollFraction = scrollFraction,
            thumbSizeFraction = thumbSizeFraction,
            columns = columns,
            rowHeightPx = rowHeightPx,
            scrollRangePx = scrollRangePx,
    )
}

internal fun thumbHeightPx(
        trackHeightPx: Float,
        metrics: LazyGridScrollbarMetrics,
        minThumbPx: Float,
): Float = (trackHeightPx * metrics.thumbSizeFraction).coerceIn(minThumbPx.coerceAtMost(trackHeightPx), trackHeightPx)

internal fun thumbFractionForTop(
        thumbTopPx: Float,
        trackHeightPx: Float,
        thumbHeightPx: Float,
): Float {
    val maxOffsetPx = trackHeightPx - thumbHeightPx
    if (maxOffsetPx <= 0f) return 0f
    return (thumbTopPx / maxOffsetPx).coerceIn(0f, 1f)
}

/** Maps a scroll fraction to the grid item index and pixel offset to pass to `scrollToItem`. */
internal fun scrollPositionForFraction(
        fraction: Float,
        metrics: LazyGridScrollbarMetrics,
): Pair<Int, Int> {
    if (metrics.totalItems <= 0 || metrics.rowHeightPx <= 0f) return 0 to 0
    val targetPx = fraction.coerceIn(0f, 1f) * metrics.scrollRangePx
    val lastRow = (metrics.totalItems - 1) / metrics.columns
    val row = (targetPx / metrics.rowHeightPx).toInt().coerceIn(0, lastRow)
    val offsetPx = (targetPx - row * metrics.rowHeightPx).roundToInt().coerceAtLeast(0)
    return (row * metrics.columns) to offsetPx
}
