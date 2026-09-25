package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.core.os.bundleOf
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.search.searchScreen.SearchScreenWallpaperLogic
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.ImageAppearanceUtils
import android.util.SizeF
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.QuickNotePanelGridItem(
    widget: PanelWidgetInfo,
    isEditing: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitHeightPx: Float,
    minRowSpan: Int,
    onDragStart: () -> Unit,
    onDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val column = widget.column ?: 0
    val row = widget.row ?: 0
    val columnSpan = widget.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS
    val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val width = cellWidth * columnSpan + gap * (columnSpan - 1)
    val height = rowHeight * rowSpan + gap * (rowSpan - 1)
    val x = (cellWidth + gap) * column
    val y = (rowHeight + gap) * row
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
                .zIndex(if (isEditing) 1f else 0f),
    ) {
        CompactQuickNoteWidget(
            modifier = Modifier.fillMaxSize(),
            onFocusChanged = onFocusChanged,
            onDragStart = {
                totalDragX = 0f
                totalDragY = 0f
                onDragStart()
            },
            onDrag = { dragX, dragY ->
                totalDragX += dragX
                totalDragY += dragY
                onDrag(totalDragX, totalDragY)
            },
            onDragEnd = onDragEnd,
        )
        if (isEditing) {
            QuickNoteEditOverlay(
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                gridUnitHeightPx = gridUnitHeightPx,
                minRowSpan = minRowSpan,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = onRemove,
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
    onResizePreview: ((WidgetGridResize) -> Unit)? = null,
    onInteractionEnd: () -> Unit = {},
    onRemove: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    // Keep taps on the selected Quick Note from being treated as outside taps.
                    detectTapGestures(onTap = {})
                },
    ) {
        WidgetEditBorder()
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
        WidgetActionButton(
            icon = Icons.Rounded.Close,
            tint = MaterialTheme.colorScheme.onError,
            background = MaterialTheme.colorScheme.error,
            onClick = onRemove,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = WidgetActionButtonCornerOffset, y = -WidgetActionButtonCornerOffset),
        )
    }
}

@Composable
internal fun HostedWidget(
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    width: Dp,
    height: Dp,
    columnSpan: Int,
    rowSpan: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }
    val widthDp = width.value.toInt()
    val heightDp = height.value.toInt()
    val displayDensity = context.resources.displayMetrics.density
    val orientation = configuration.orientation
    val displayOptions =
        remember(widthDp, heightDp, columnSpan, rowSpan, displayDensity, orientation) {
            createDisplayedWidgetOptions(
                widthDp = widthDp,
                heightDp = heightDp,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                density = displayDensity,
                orientation = orientation,
            )
        }

    AndroidView(
        factory = { ctx ->
            appWidgetHost.createView(ctx, appWidgetId, providerInfo).apply {
                setAppWidget(appWidgetId, providerInfo)
                layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                appWidgetManager.updateAppWidgetOptions(appWidgetId, displayOptions)
                updateAppWidgetSize(displayOptions, widthDp, heightDp, widthDp, heightDp)
            }
        },
        update = { hostView ->
            hostView.layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            appWidgetManager.updateAppWidgetOptions(appWidgetId, displayOptions)
            hostView.updateAppWidgetSize(displayOptions, widthDp, heightDp, widthDp, heightDp)
        },
        modifier = modifier,
    )
}

@Composable
internal fun BoxScope.WidgetEditOverlay(
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    hasConfigure: Boolean,
    canPinToHome: Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
    onConfigure: () -> Unit,
    onPinToHome: () -> Unit,
) {
    val currentColumn by rememberUpdatedState(column)
    val currentRow by rememberUpdatedState(row)
    val currentColumnSpan by rememberUpdatedState(columnSpan)
    val currentRowSpan by rememberUpdatedState(rowSpan)

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    // Absorb taps anywhere on the widget so the screen-level dismiss doesn't fire.
                    detectTapGestures(onTap = {})
                },
    ) {
        WidgetEditBorder()
        // Drag-to-move handler covering the widget. Lives below handles & buttons so they get
        // their gestures first.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var startColumn = currentColumn
                        var startRow = currentRow
                        var widgetColumnSpan = currentColumnSpan
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                                startColumn = currentColumn
                                startRow = currentRow
                                widgetColumnSpan = currentColumnSpan
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                val nextColumn =
                                    (startColumn + (totalDragX / gridUnitWidthPx).roundToInt())
                                        .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - widgetColumnSpan)
                                val nextRow =
                                    (startRow + (totalDragY / gridUnitHeightPx).roundToInt())
                                        .coerceAtLeast(0)
                                onMovePreview(nextColumn, nextRow)
                            },
                            onDragEnd = { onInteractionEnd() },
                            onDragCancel = { onInteractionEnd() },
                        )
                    },
        )

        ResizeEdge.entries.forEach { edge ->
            EdgeResizeHandle(
                edge = edge,
                startColumn = currentColumn,
                startRow = currentRow,
                startColumnSpan = currentColumnSpan,
                startRowSpan = currentRowSpan,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = minColumnSpan,
                minRowSpan = minRowSpan,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                modifier = Modifier.align(edge.alignment),
            )
        }

        WidgetEditActionButtons(
            onRemove = onRemove,
            onConfigure = onConfigure.takeIf { hasConfigure },
            onPinToHome = onPinToHome.takeIf { canPinToHome },
        )
    }
}

/**
 * Edit badges grouped at the widget's top-end corner, ordered Home, Settings, Remove. Unavailable
 * actions are omitted so the remaining badges stay packed against the corner.
 */
@Composable
internal fun BoxScope.WidgetEditActionButtons(
    onRemove: () -> Unit,
    onConfigure: (() -> Unit)?,
    onPinToHome: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = WidgetActionButtonCornerOffset, y = -WidgetActionButtonCornerOffset),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        onPinToHome?.let { onClick ->
            WidgetActionButton(
                icon = Icons.Rounded.Home,
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
        onConfigure?.let { onClick ->
            WidgetActionButton(
                icon = Icons.Rounded.Settings,
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
        WidgetActionButton(
            icon = Icons.Rounded.Close,
            tint = MaterialTheme.colorScheme.onError,
            background = MaterialTheme.colorScheme.error,
            onClick = onRemove,
        )
    }
}

/**
 * Edit-mode outline drawn as the first child of an edit overlay. A `border` modifier on the
 * overlay itself would draw after its children and paint over the action buttons and handles.
 */
@Composable
internal fun BoxScope.WidgetEditBorder() {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .border(
                    width = WidgetEditBorderWidth,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    shape = DesignTokens.WidgetPanelCardShape,
                ),
    )
}

@Composable
internal fun WidgetActionButton(
    icon: ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(WidgetActionButtonSize),
        shape = CircleShape,
        color = background,
        // Ring in the panel background color separates the badge from widget content beneath it.
        border = BorderStroke(WidgetEditRingWidth, MaterialTheme.colorScheme.background),
        shadowElevation = DesignTokens.ElevationLevel2,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = tint),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }
}

@Composable
internal fun EdgeResizeHandle(
    edge: ResizeEdge,
    startColumn: Int,
    startRow: Int,
    startColumnSpan: Int,
    startRowSpan: Int,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capturedStartColumn by rememberUpdatedState(startColumn)
    val capturedStartRow by rememberUpdatedState(startRow)
    val capturedStartColumnSpan by rememberUpdatedState(startColumnSpan)
    val capturedStartRowSpan by rememberUpdatedState(startRowSpan)

    val (hitWidth, hitHeight) =
        if (edge.isHorizontalAxis) {
            WidgetResizeEdgeHitShort to WidgetResizeEdgeHitLong
        } else {
            WidgetResizeEdgeHitLong to WidgetResizeEdgeHitShort
        }
    val (visualWidth, visualHeight) =
        if (edge.isHorizontalAxis) {
            WidgetResizeVisualShort to WidgetResizeVisualLong
        } else {
            WidgetResizeVisualLong to WidgetResizeVisualShort
        }

    Box(
        modifier =
            modifier
                .size(width = hitWidth, height = hitHeight)
                .pointerInput(edge) {
                    var totalX = 0f
                    var totalY = 0f
                    var startCol = capturedStartColumn
                    var startRowLocal = capturedStartRow
                    var startColSpan = capturedStartColumnSpan
                    var startRowSpanLocal = capturedStartRowSpan
                    detectDragGestures(
                        onDragStart = {
                            totalX = 0f
                            totalY = 0f
                            startCol = capturedStartColumn
                            startRowLocal = capturedStartRow
                            startColSpan = capturedStartColumnSpan
                            startRowSpanLocal = capturedStartRowSpan
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalX += dragAmount.x
                            totalY += dragAmount.y
                            onResizePreview(
                                calculateGridResize(
                                    startColumn = startCol,
                                    startRow = startRowLocal,
                                    startColumnSpan = startColSpan,
                                    startRowSpan = startRowSpanLocal,
                                    horizontalDirection = edge.xSign,
                                    verticalDirection = edge.ySign,
                                    horizontalDeltaPx = totalX,
                                    verticalDeltaPx = totalY,
                                    gridUnitWidthPx = gridUnitWidthPx,
                                    gridUnitHeightPx = gridUnitHeightPx,
                                    minColumnSpan = minColumnSpan,
                                    minRowSpan = minRowSpan,
                                ),
                            )
                        },
                        onDragEnd = { onInteractionEnd() },
                        onDragCancel = { onInteractionEnd() },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        // Shift the pill from the hit area's center onto the border line so it sits on the edge
        // instead of covering widget content.
        val edgeOffset = WidgetResizeEdgeHitShort / 2
        Box(
            modifier =
                Modifier
                    .offset(x = edgeOffset * edge.xSign, y = edgeOffset * edge.ySign)
                    .size(width = visualWidth, height = visualHeight)
                    .border(
                        width = WidgetEditRingWidth,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape,
                    )
                    .padding(WidgetEditRingWidth)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
        )
    }
}
