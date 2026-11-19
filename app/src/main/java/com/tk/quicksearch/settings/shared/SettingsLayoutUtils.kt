package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet

private const val TABLET_SETTINGS_CONTENT_WIDTH_FRACTION = 0.70f
private const val TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION = 0.50f

@Composable
fun Modifier.settingsContentWidth(): Modifier =
    if (isTablet()) {
        if (isLandscape()) {
            fillMaxWidth(TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION)
        } else {
            fillMaxWidth(TABLET_SETTINGS_CONTENT_WIDTH_FRACTION)
        }
    } else {
        fillMaxWidth()
    }

private val SettingsScrollViewportShape = AppShapes.segmentGroup

/**
 * Clips a settings scroll viewport to the settings card corner radius so cards scrolling past
 * the top or bottom edge follow the same rounded outline. Apply after the horizontal content
 * padding and before any scroll modifier.
 */
fun Modifier.settingsScrollClip(): Modifier = clip(SettingsScrollViewportShape)

/**
 * Scroll-aware [settingsScrollClip]: each edge rounds only as content scrolls under it, growing to
 * the full card radius, so a header or text resting at the top or bottom of the viewport is never
 * cut by the corner. Apply to the viewport that owns [scrollState], before its scroll modifier.
 */
fun Modifier.settingsScrollClip(scrollState: ScrollState): Modifier {
    val shapes = ScrollEdgeShapeCache()
    return graphicsLayer {
        val maxRadius = DesignTokens.SegmentedListOuterCorner.toPx()
        shape =
            shapes.shapeFor(
                top = scrollState.value.toFloat().coerceIn(0f, maxRadius),
                bottom = (scrollState.maxValue - scrollState.value).toFloat().coerceIn(0f, maxRadius),
            )
        clip = true
    }
}

/** Scroll-aware clipping for a lazy settings list. Apply to the list that owns [listState]. */
fun Modifier.settingsScrollClip(listState: LazyListState): Modifier {
    val shapes = ScrollEdgeShapeCache()
    return graphicsLayer {
        val maxRadius = DesignTokens.SegmentedListOuterCorner.toPx()
        val top = if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, maxRadius)
        } else {
            maxRadius
        }
        val layout = listState.layoutInfo
        val lastVisible = layout.visibleItemsInfo.lastOrNull()
        val bottom = when {
            !listState.canScrollForward || lastVisible == null -> 0f
            lastVisible.index < layout.totalItemsCount - 1 -> maxRadius
            else -> {
                val contentEnd = lastVisible.offset + lastVisible.size + layout.afterContentPadding
                (contentEnd - layout.viewportEndOffset).toFloat().coerceIn(0f, maxRadius)
            }
        }
        shape = shapes.shapeFor(top = top, bottom = bottom)
        clip = true
    }
}

/** Reuses the clip shape while the corner radii are unchanged, i.e. for most of a scroll. */
private class ScrollEdgeShapeCache {
    private var top = -1f
    private var bottom = -1f
    private var shape: Shape = RectangleShape

    fun shapeFor(top: Float, bottom: Float): Shape {
        if (top != this.top || bottom != this.bottom) {
            this.top = top
            this.bottom = bottom
            shape = RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
        }
        return shape
    }
}
