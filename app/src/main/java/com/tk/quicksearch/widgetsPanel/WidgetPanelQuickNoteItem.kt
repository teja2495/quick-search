package com.tk.quicksearch.widgetsPanel

import androidx.compose.material3.minimumInteractiveComponentSize
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.theme.AppMotion

@Composable
internal fun BoxScope.QuickNotePanelGridItem(
    widget: PanelWidgetInfo,
    isEditing: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitHeightPx: Float,
    minRowSpan: Int,
    onDragStart: (() -> Unit)?,
    onDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
    onAddToStack: () -> Unit,
) {
    val density = LocalDensity.current
    val column = widget.column ?: 0
    val row = widget.row ?: 0
    val columnSpan = widget.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS
    val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val width = cellWidth * columnSpan + gap * (columnSpan - 1)
    val height = rowHeight * rowSpan + gap * (rowSpan - 1)
    val x = (cellWidth + gap) * column
    val y = widgetPanelPageOffset(widget) + (rowHeight + gap) * row
    val animatedY by
        animateDpAsState(targetValue = y, animationSpec = WidgetLayoutMotion, label = "quickNoteWidgetY")
    val animatedHeight by
        animateDpAsState(
            targetValue = height,
            animationSpec = WidgetLayoutMotion,
            label = "quickNoteWidgetHeight",
        )
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val editScale by animateFloatAsState(
        targetValue = if (isEditing) WIDGET_EDIT_LIFT_SCALE else 1f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "quickNoteEditScale",
    )

    Box(
        modifier =
            Modifier
                .size(width = width, height = animatedHeight)
                .offset {
                    IntOffset(
                        x = with(density) { x.roundToPx() },
                        y = with(density) { animatedY.roundToPx() },
                    )
                }
                .zIndex(if (isEditing) 1f else 0f)
                .graphicsLayer {
                    scaleX = editScale
                    scaleY = editScale
                },
    ) {
        CompactQuickNoteWidget(
            modifier = Modifier.fillMaxSize().widgetEditAction(
                onDragStart?.takeUnless { isEditing }?.let { start ->
                    { start(); onDragEnd() }
                },
            ),
            onFocusChanged = onFocusChanged,
            onDragStart = onDragStart?.let { start ->
                {
                    totalDragX = 0f
                    totalDragY = 0f
                    start()
                }
            },
            onDrag = { dragX, dragY ->
                totalDragX += dragX
                totalDragY += dragY
                onDrag(totalDragX, totalDragY)
            },
            onDragEnd = onDragEnd,
        )
        AnimatedWidgetEditOverlay(visible = isEditing) {
            if (onDragStart == null) return@AnimatedWidgetEditOverlay
            QuickNoteEditOverlay(
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                gridUnitHeightPx = gridUnitHeightPx,
                minRowSpan = minRowSpan,
                onMoveBy = onMoveBy,
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = onRemove,
                onAddToStack = onAddToStack,
            )
        }
    }
}

@Composable
internal fun BoxScope.QuickNoteEditOverlay(
    column: Int = 0,
    row: Int = 0,
    columnSpan: Int = WIDGET_PANEL_GRID_COLUMNS,
    rowSpan: Int = WIDGET_PANEL_DEFAULT_ROW_SPAN,
    gridUnitHeightPx: Float = 0f,
    minRowSpan: Int = 1,
    onMoveBy: ((Int, Int) -> Boolean)? = null,
    onMovePreview: ((column: Int, row: Int) -> Unit)? = null,
    onResizePreview: ((WidgetGridResize) -> Unit)? = null,
    onInteractionEnd: () -> Unit = {},
    onRemove: () -> Unit,
    onAddToStack: (() -> Unit)? = null,
) {
    val currentColumn by rememberUpdatedState(column)
    val currentRow by rememberUpdatedState(row)
    val currentOnMovePreview by rememberUpdatedState(onMovePreview)
    val currentOnInteractionEnd by rememberUpdatedState(onInteractionEnd)
    val currentGridUnitHeightPx by rememberUpdatedState(gridUnitHeightPx)
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .widgetLayoutActions(
                    layout = WidgetGridResize(column, row, columnSpan, rowSpan),
                    minColumnSpan = WIDGET_PANEL_GRID_COLUMNS,
                    minRowSpan = minRowSpan,
                    canMoveUp = row > 0,
                    canMoveDown = true,
                    onMove = onMoveBy,
                    onResize = onResizePreview?.let { resize ->
                        { resized ->
                            resize(resized)
                            onInteractionEnd()
                        }
                    },
                )
                .pointerInput(Unit) {
                    // Keep taps on the selected Quick Note from being treated as outside taps.
                    detectTapGestures(onTap = {})
                },
    ) {
        WidgetEditBorder()
        // The overlay covers the note's own long-press drag handle, so once the note is selected
        // it moves by dragging anywhere on it, like other widgets. Lives below handles & buttons.
        if (onMovePreview != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var totalDragY = 0f
                            var startColumn = currentColumn
                            var startRow = currentRow
                            detectDragGestures(
                                onDragStart = {
                                    totalDragY = 0f
                                    startColumn = currentColumn
                                    startRow = currentRow
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragY += dragAmount.y
                                    val unitHeight = currentGridUnitHeightPx
                                    if (unitHeight <= 0f) return@detectDragGestures
                                    // Left unclamped: a row past the page edge asks to change page.
                                    val nextRow = startRow + (totalDragY / unitHeight).roundToInt()
                                    currentOnMovePreview?.invoke(startColumn, nextRow)
                                },
                                onDragEnd = { currentOnInteractionEnd() },
                                onDragCancel = { currentOnInteractionEnd() },
                            )
                        },
            )
        }
        onResizePreview?.let { resize ->
            listOf(ResizeEdge.Top, ResizeEdge.Bottom).forEach { edge ->
                EdgeResizeHandle(
                    edge = edge,
                    startColumn = column,
                    startRow = row,
                    startColumnSpan = columnSpan,
                    startRowSpan = rowSpan,
                    gridUnitWidthPx = 0f,
                    gridUnitHeightPx = gridUnitHeightPx,
                    minColumnSpan = WIDGET_PANEL_GRID_COLUMNS,
                    minRowSpan = minRowSpan,
                    onResizePreview = resize,
                    onInteractionEnd = onInteractionEnd,
                    modifier = Modifier.align(edge.alignment),
                )
            }
        }
        WidgetEditActionButtons(
            onRemove = onRemove,
            onConfigure = null,
            onAddToStack = onAddToStack,
        )
    }
}
