package com.tk.quicksearch.widgetsPanel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor

private val PageZoneHeight = 72.dp
private const val PageZoneHoveredScale = 1.12f
private val PageIndicatorDotSize = 6.dp
private val PageIndicatorActiveLength = 14.dp
private val PageIndicatorDotGap = DesignTokens.SpacingXSmall

/**
 * The previous-page and next-page (or new-page) zones along the screen's top and bottom edges,
 * shown while a widget is dragged, like a folder's remove zone. The top zone covers the status bar
 * and the panel header, which hides while dragging. Dropping the widget on a zone moves it there.
 */
@Composable
internal fun BoxScope.WidgetPanelPageZones(pager: WidgetPanelPagerState) {
    val zones = pager.dragZones
    WidgetPanelPageZoneStrip(
        zone = WidgetPanelPageZone.Previous,
        visible = WidgetPanelPageZone.Previous in zones,
        hovered = pager.hoveredZone == WidgetPanelPageZone.Previous,
        modifier = Modifier.align(Alignment.TopCenter),
    )
    val bottomZone = if (WidgetPanelPageZone.NewPage in zones) WidgetPanelPageZone.NewPage else WidgetPanelPageZone.Next
    WidgetPanelPageZoneStrip(
        zone = bottomZone,
        visible = bottomZone in zones,
        hovered = pager.hoveredZone == bottomZone,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

@Composable
private fun WidgetPanelPageZoneStrip(
    zone: WidgetPanelPageZone,
    visible: Boolean,
    hovered: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibility by
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedEasing),
            label = "widgetPanelPageZoneVisibility",
        )
    val glowAlpha by
        animateFloatAsState(
            targetValue = if (hovered) AppColors.ContentMutedAlpha else AppColors.TintEmphasisAlpha,
            label = "widgetPanelPageZoneGlow",
        )
    val contentScale by
        animateFloatAsState(
            targetValue = if (hovered) PageZoneHoveredScale else 1f,
            animationSpec = AppMotion.fastSpatial(),
            label = "widgetPanelPageZoneScale",
        )
    if (visibility <= 0f) return
    val color = homeTextColor()
    val atTop = zone == WidgetPanelPageZone.Previous
    val glow = color.copy(alpha = glowAlpha)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = visibility }
                .background(
                    Brush.verticalGradient(
                        if (atTop) listOf(glow, Color.Transparent) else listOf(Color.Transparent, glow),
                    ),
                )
                .then(if (atTop) Modifier.statusBarsPadding() else Modifier)
                .height(PageZoneHeight),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .height(DesignTokens.MinTouchTarget)
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Icon(
                imageVector =
                    when (zone) {
                        WidgetPanelPageZone.Previous -> MaterialSymbols.ArrowUpward
                        WidgetPanelPageZone.Next -> MaterialSymbols.ArrowDownward
                        WidgetPanelPageZone.NewPage -> MaterialSymbols.Add
                    },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
            Text(
                text =
                    stringResource(
                        when (zone) {
                            WidgetPanelPageZone.Previous -> R.string.widgets_panel_page_zone_previous
                            WidgetPanelPageZone.Next -> R.string.widgets_panel_page_zone_next
                            WidgetPanelPageZone.NewPage -> R.string.widgets_panel_page_zone_new
                        },
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
    }
}

/** Vertical dots for the panel's pages; the current page's dot stretches into a pill. */
@Composable
internal fun WidgetPanelPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    val current = currentPage.coerceIn(0, pageCount - 1)
    val description = stringResource(R.string.widget_stack_page_indicator, current + 1, pageCount)
    val color = homeTextColor()
    Column(
        modifier = modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(PageIndicatorDotGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(pageCount) { index ->
            val selected = index == current
            val length by animateDpAsState(
                targetValue = if (selected) PageIndicatorActiveLength else PageIndicatorDotSize,
                animationSpec = AppMotion.fastSpatial(),
                label = "widgetPanelPageDotLength",
            )
            Box(
                modifier =
                    Modifier
                        .width(PageIndicatorDotSize)
                        .height(length)
                        .background(
                            color = if (selected) color else color.copy(alpha = AppColors.ContentFaintAlpha),
                            shape = AppShapes.full,
                        ),
            )
        }
    }
}
