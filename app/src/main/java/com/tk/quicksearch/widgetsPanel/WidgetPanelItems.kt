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
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = onRemove,
                onAddToStack = onAddToStack,
            )
        }
    }
}

/**
 * Shows a widget's edit overlay (border, handles, action buttons) with a quick fade and a springy
 * scale-in when the widget enters edit mode, instead of popping in.
 *
 * The fade uses [CompositingStrategy.ModulateAlpha]: AnimatedVisibility's own fade draws through an
 * offscreen buffer the size of the widget, which cuts off the badges and handles that overhang its
 * edges until the fade finishes.
 */
@Composable
internal fun BoxScope.AnimatedWidgetEditOverlay(
    visible: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.matchParentSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        val alpha by
            transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        tween(WIDGET_EDIT_OVERLAY_FADE_IN_MS, easing = AppMotion.EmphasizedEasing)
                    } else {
                        tween(WIDGET_EDIT_OVERLAY_FADE_OUT_MS, easing = AppMotion.EmphasizedEasing)
                    }
                },
                label = "widgetEditOverlayAlpha",
            ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
        val scale by
            transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        AppMotion.fastSpatial()
                    } else {
                        snap()
                    }
                },
                label = "widgetEditOverlayScale",
            ) { state -> if (state == EnterExitState.PreEnter) WIDGET_EDIT_OVERLAY_INITIAL_SCALE else 1f }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    },
            content = content,
        )
    }
}

/** Slight lift applied to the widget being edited. */
internal const val WIDGET_EDIT_LIFT_SCALE = 1.02f
private const val WIDGET_EDIT_OVERLAY_INITIAL_SCALE = 0.94f
private const val WIDGET_EDIT_OVERLAY_FADE_IN_MS = AppMotion.DurationExit
private const val WIDGET_EDIT_OVERLAY_FADE_OUT_MS = AppMotion.DurationQuick

@Composable
internal fun BoxScope.QuickNoteEditOverlay(
    column: Int = 0,
    row: Int = 0,
    columnSpan: Int = WIDGET_PANEL_GRID_COLUMNS,
    rowSpan: Int = WIDGET_PANEL_DEFAULT_ROW_SPAN,
    gridUnitHeightPx: Float = 0f,
    minRowSpan: Int = 1,
    onMoveBy: ((Int, Int) -> Boolean)? = null,
    onResizePreview: ((WidgetGridResize) -> Unit)? = null,
    onInteractionEnd: () -> Unit = {},
    onRemove: () -> Unit,
    onAddToStack: (() -> Unit)? = null,
) {
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
    val graphicsContext = LocalGraphicsContext.current
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

    // Lay the widget out at its target grid size, not the animated frame size: a per-frame size
    // re-measures the RemoteViews and re-sends options every frame, making providers like
    // Calendar rebuild mid-resize. The container clips while its frame animates to the target.
    // AndroidView.update also runs for unrelated recompositions (focus, editing, content updates).
    // Re-sending options there asks the provider to rebuild its RemoteViews again.
    val appliedOptions = remember(appWidgetId, appWidgetHost) { arrayOfNulls<Bundle>(1) }
    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { ctx ->
                // createView already binds the id/provider and loads the system's cached RemoteViews;
                // a view kept from an earlier showing is reused instead.
                appWidgetHost.obtainView(ctx, appWidgetId, providerInfo, graphicsContext).apply {
                    layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                }
            },
            update = { hostView ->
                if (hostView.layoutParams.width != widthPx || hostView.layoutParams.height != heightPx) {
                    hostView.layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                }
                if (appliedOptions[0] !== displayOptions) {
                    val currentOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    if (!widgetOptionsMatch(currentOptions.asWidgetOptionValues(), displayOptions.asWidgetOptionValues())) {
                        // The bundle already contains exact bounds and responsive sizes. Calling the
                        // legacy updateAppWidgetSize afterward clears SIZES and triggers another update.
                        appWidgetManager.updateAppWidgetOptions(appWidgetId, displayOptions)
                    }
                    appliedOptions[0] = displayOptions
                }
            },
            modifier =
                Modifier
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .size(width = width, height = height),
        )
    }
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
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
    onConfigure: () -> Unit,
    onPinToHome: () -> Unit,
    onAddToStack: () -> Unit,
    onReorderStack: (() -> Unit)? = null,
) {
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val currentColumn by rememberUpdatedState(column)
    val currentRow by rememberUpdatedState(row)
    val currentColumnSpan by rememberUpdatedState(columnSpan)
    val currentRowSpan by rememberUpdatedState(rowSpan)

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .widgetLayoutActions(
                    layout = WidgetGridResize(column, row, columnSpan, rowSpan),
                    minColumnSpan = minColumnSpan,
                    minRowSpan = minRowSpan,
                    canMoveUp = row > 0,
                    canMoveDown = true,
                    onMove = onMoveBy,
                    onResize = { resize ->
                        onResizePreview(resize)
                        onInteractionEnd()
                    },
                )
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
                    .pointerInput(horizontalSign) {
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
                                totalDragX += dragAmount.x * horizontalSign
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
            onAddToStack = onAddToStack,
            onReorderStack = onReorderStack,
        )
    }
}

/**
 * Edit badges along the widget's top edge: Add to stack, Reorder stack and Add to Home packed at the top-start
 * corner, then Settings and Remove packed at the top-end corner. Unavailable actions are omitted.
 */
@Composable
internal fun BoxScope.WidgetEditActionButtons(
    onRemove: () -> Unit,
    onConfigure: (() -> Unit)?,
    onPinToHome: (() -> Unit)? = null,
    onAddToStack: (() -> Unit)? = null,
    onReorderStack: (() -> Unit)? = null,
) {
    WidgetEditBadgeScrim()
    // Each badge lays out at its 48dp touch target, so pull them together until the visible
    // badges sit SpacingSmall apart; their touch targets overlap slightly at the seams.
    val badgeArrangement =
        Arrangement.spacedBy(
            DesignTokens.SpacingSmall - (DesignTokens.MinTouchTarget - WidgetActionButtonSize),
        )
    Row(
        modifier = Modifier.align(Alignment.TopStart),
        horizontalArrangement = badgeArrangement,
    ) {
        onAddToStack?.let { onClick ->
            WidgetActionButton(
                icon = MaterialSymbols.Add,
                contentDescription = stringResource(R.string.widget_add_to_stack),
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
        onReorderStack?.let { onClick ->
            WidgetActionButton(
                icon = MaterialSymbols.Reorder,
                contentDescription = stringResource(R.string.widget_reorder_stack),
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
        onPinToHome?.let { onClick ->
            WidgetActionButton(
                icon = MaterialSymbols.Home,
                contentDescription = stringResource(R.string.action_add_to_home),
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
    }
    Row(
        modifier = Modifier.align(Alignment.TopEnd),
        horizontalArrangement = badgeArrangement,
    ) {
        onConfigure?.let { onClick ->
            WidgetActionButton(
                icon = MaterialSymbols.Settings,
                contentDescription = stringResource(R.string.settings_gesture_settings),
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onClick,
            )
        }
        WidgetActionButton(
            icon = MaterialSymbols.Close,
            contentDescription = stringResource(R.string.action_remove),
            tint = MaterialTheme.colorScheme.onError,
            background = MaterialTheme.colorScheme.error,
            onClick = onRemove,
        )
    }
}

/**
 * Dark band fading down from the widget's top edge, behind the edit badges, so they stay legible
 * over any widget content. Clipped to the card shape.
 */
@Composable
internal fun BoxScope.WidgetEditBadgeScrim() {
    val scrim = MaterialTheme.colorScheme.scrim
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WidgetEditBadgeScrimHeight)
                .clip(AppShapes.widgetPanelCard)
                .background(
                    Brush.verticalGradient(
                        listOf(scrim.copy(alpha = AppColors.ContentSecondaryAlpha), Color.Transparent),
                    ),
                ),
    )
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = AppColors.ContentSecondaryAlpha),
                    shape = AppShapes.widgetPanelCard,
                ),
    )
}

/**
 * A 30dp edit badge. The click lives on the badge itself, before its clip, so Material's minimum
 * interactive size gives it a 48dp touch target without changing how tightly the badges pack.
 */
@Composable
internal fun WidgetActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize().size(WidgetActionButtonSize),
        shape = AppShapes.full,
        color = background,
        contentColor = tint,
        // Ring in the panel background color separates the badge from widget content beneath it.
        border = BorderStroke(WidgetEditRingWidth, MaterialTheme.colorScheme.background),
        shadowElevation = DesignTokens.ElevationLevel2,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
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
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val capturedStartColumn by rememberUpdatedState(startColumn)
    val capturedStartRow by rememberUpdatedState(startRow)
    val capturedStartColumnSpan by rememberUpdatedState(startColumnSpan)
    val capturedStartRowSpan by rememberUpdatedState(startRowSpan)
    val capturedGridUnitWidthPx by rememberUpdatedState(gridUnitWidthPx)
    val capturedGridUnitHeightPx by rememberUpdatedState(gridUnitHeightPx)
    val capturedMinColumnSpan by rememberUpdatedState(minColumnSpan)
    val capturedMinRowSpan by rememberUpdatedState(minRowSpan)

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

    // Center the touch target on the border line, where the pill is drawn, so touches on the
    // pill's outer half (and just outside the widget) still grab the handle instead of falling
    // through to the page swipe or the tap that ends editing.
    val edgeOffset = WidgetResizeEdgeHitShort / 2
    Box(
        modifier =
            modifier
                .offset(x = edgeOffset * edge.xSign, y = edgeOffset * edge.ySign)
                .size(width = hitWidth, height = hitHeight)
                .pointerInput(edge, horizontalSign) {
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
                            totalX += dragAmount.x * horizontalSign
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
                                    gridUnitWidthPx = capturedGridUnitWidthPx,
                                    gridUnitHeightPx = capturedGridUnitHeightPx,
                                    minColumnSpan = capturedMinColumnSpan,
                                    minRowSpan = capturedMinRowSpan,
                                ),
                            )
                        },
                        onDragEnd = { onInteractionEnd() },
                        onDragCancel = { onInteractionEnd() },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = visualWidth, height = visualHeight)
                    .border(
                        width = WidgetEditRingWidth,
                        color = MaterialTheme.colorScheme.background,
                        shape = AppShapes.full,
                    )
                    .padding(WidgetEditRingWidth)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = AppShapes.full,
                    ),
        )
    }
}

@Suppress("DEPRECATION")
private fun Bundle.asWidgetOptionValues(): Map<String, Any?> = keySet().associateWith { get(it) }
