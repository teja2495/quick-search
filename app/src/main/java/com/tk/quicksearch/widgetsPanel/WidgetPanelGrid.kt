package com.tk.quicksearch.widgetsPanel

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalFrostedCardSource
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import kotlin.math.floor
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.components.AppTonalButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
internal fun WidgetsPanelHeader(
    inEditMode: Boolean,
    layoutLocked: Boolean?,
    onLayoutLockChange: (Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onExitEditMode: () -> Unit,
) {
    val foreground = homeTextColor()
    val actionColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = foreground.copy(alpha = AppColors.TintLightAlpha),
        contentColor = foreground,
        disabledContainerColor = foreground.copy(alpha = AppColors.TintFaintAlpha),
        disabledContentColor = foreground.copy(alpha = AppColors.DisabledContentAlpha),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.widgets_panel_title),
            style = MaterialTheme.typography.headlineSmall,
            color = foreground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (!inEditMode && layoutLocked != null) {
            WidgetLayoutLockButton(
                locked = layoutLocked,
                actionColors = actionColors,
                onCheckedChange = onLayoutLockChange,
            )
            Spacer(modifier = Modifier.width(DesignTokens.SpacingXSmall))
        }
        // Swap the add button for Done with a fade and springy scale instead of a hard cut.
        AnimatedContent(
            targetState = inEditMode,
            transitionSpec = {
                (fadeIn(tween(HEADER_ACTION_FADE_MS, easing = AppMotion.EmphasizedDecelerateEasing)) +
                    scaleIn(initialScale = HEADER_ACTION_INITIAL_SCALE, animationSpec = AppMotion.fastSpatial()))
                    .togetherWith(fadeOut(tween(HEADER_ACTION_FADE_MS, easing = AppMotion.EmphasizedAccelerateEasing)))
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.CenterEnd,
            label = "widgetPanelHeaderAction",
        ) { editing ->
            if (editing) {
                AppTonalButton(
                    onClick = onExitEditMode,
                    tone = AppButtonTone.Custom(foreground),
                    icon = MaterialSymbols.Check,
                ) {
                    Text(text = stringResource(R.string.dialog_done))
                }
            } else {
                AppIconButton(
                    onClick = onAddWidget,
                    tooltip = stringResource(R.string.common_action_add),
                    style = AppIconButtonStyle.Tonal,
                    enabled = layoutLocked == false,
                    modifier = Modifier.size(DesignTokens.Spacing40),
                    colors = actionColors,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Add,
                        contentDescription = stringResource(R.string.common_action_add),
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetLayoutLockButton(
    locked: Boolean,
    actionColors: IconButtonColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    val lockLabel = stringResource(R.string.widgets_panel_lock_layout)
    val tooltip = if (locked) stringResource(R.string.widgets_panel_unlock_layout) else lockLabel
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        FilledTonalIconToggleButton(
            checked = locked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.minimumInteractiveComponentSize().size(DesignTokens.Spacing40),
            colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                containerColor = actionColors.containerColor,
                contentColor = actionColors.contentColor,
                checkedContainerColor = actionColors.containerColor,
                checkedContentColor = actionColors.contentColor,
            ),
        ) {
            Icon(
                imageVector = if (locked) MaterialSymbols.Lock else MaterialSymbols.LockOpen,
                contentDescription = tooltip,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        }
    }
}

private const val HostedWidgetOverscanRows = 2

private class WidgetMovePreview {
    private var widgetId: Int? = null
    private var column = 0
    private var row = 0

    fun reset() {
        widgetId = null
    }

    fun update(appWidgetId: Int, targetColumn: Int, targetRow: Int): Boolean {
        if (widgetId == appWidgetId && column == targetColumn && row == targetRow) return false
        widgetId = appWidgetId
        column = targetColumn
        row = targetRow
        return true
    }
}

private const val HEADER_ACTION_FADE_MS = AppMotion.DurationExit
private const val HEADER_ACTION_INITIAL_SCALE = 0.85f

@Composable
internal fun WidgetPanelGrid(
    widgets: List<PanelWidgetInfo>,
    showHostedWidgets: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    widgetContentAvailable: Map<Int, Boolean>,
    editingWidgetId: Int?,
    layoutEditingEnabled: Boolean,
    density: Density,
    panelScrollState: ScrollState,
    viewportTopPx: Float,
    viewportHeightPx: Int,
    onPersist: (List<PanelWidgetInfo>) -> Unit,
    onSetEditingWidgetId: (Int?) -> Unit,
    onWidgetTouch: (Int) -> Boolean,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
    onRemoveWidget: (widget: PanelWidgetInfo, pageId: Int) -> Unit,
    onConfigureWidget: (PanelWidgetInfo, Intent) -> Unit,
    onPinWidgetToHome: (PanelWidgetInfo) -> Unit,
    onAddToStack: (PanelWidgetInfo, WidgetStackFit) -> Unit,
    onReorderStack: (PanelWidgetInfo) -> Unit,
    modifier: Modifier = Modifier,
    /** Bumped to flash every stack's page indicator, such as when the panel comes into view. */
    stackIndicatorRevealKey: Int = 0,
) {
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    BoxWithConstraints(modifier = modifier) {
        val cellWidth =
            (maxWidth - WidgetPanelGridGap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        val rowHeight = WidgetPanelGridRowHeight
        val gap = WidgetPanelGridGap
        val gridUnitWidthPx = with(density) { (cellWidth + gap).toPx() }
        val gridUnitHeightPx = with(density) { (rowHeight + gap).toPx() }

        val specs =
            remember(widgets, appWidgetHost, cellWidth) {
                widgets.associate { widget ->
                    // A stack is as small as its largest page allows.
                    widget.appWidgetId to
                        widget.pageIds
                            .map { id ->
                                if (isQuickNotePage(id)) {
                                    QuickNoteGridSpec
                                } else {
                                    widgetMinGridSpec(appWidgetHost.providerInfo(id), density, cellWidth.value)
                                }
                            }
                            .reduce { a, b ->
                                WidgetGridSpec(
                                    minColumnSpan = maxOf(a.minColumnSpan, b.minColumnSpan),
                                    minRowSpan = maxOf(a.minRowSpan, b.minRowSpan),
                                )
                            }
                }
            }

        val laidOut =
            remember(widgets, specs) { resolveWidgetGridLayout(widgets, specs) }
        // Snapshot at entry: once shown, a kept view is attached and no longer reported as kept.
        val keptViewIds = remember(appWidgetHost) { appWidgetHost.keptViewIds() }

        LaunchedEffect(laidOut) {
            if (laidOut != widgets) onPersist(laidOut)
        }

        var liveLayout by remember { mutableStateOf<List<PanelWidgetInfo>?>(null) }
        val displayLayout = liveLayout.takeIf { layoutEditingEnabled } ?: laidOut
        val movePreview = remember(laidOut) { WidgetMovePreview() }
        var hostDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        var quickNoteDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        // Scroll offset captured when a host drag begins, so the drag target can compensate for any
        // auto-scroll and keep the widget under the finger (the host path works in screen coords).
        var scrollAtDragStart by remember { mutableIntStateOf(0) }
        // Window Y of the grid content box, used to locate the dragged widget for edge auto-scroll.
        var gridTopPx by remember { mutableFloatStateOf(Float.NaN) }

        val currentLaidOut by rememberUpdatedState(laidOut)

        fun previewMove(appWidgetId: Int, column: Int, row: Int) {
            if (movePreview.update(appWidgetId, column, row)) {
                liveLayout = moveWidgetToCell(laidOut, appWidgetId, column, row)
            }
        }

        val currentGridUnitWidthPx by rememberUpdatedState(gridUnitWidthPx)
        val currentHorizontalSign by rememberUpdatedState(horizontalSign)
        val currentGridUnitHeightPx by rememberUpdatedState(gridUnitHeightPx)
        val currentOnPersist by rememberUpdatedState(onPersist)
        val currentOnSetEditing by rememberUpdatedState(onSetEditingWidgetId)
        val currentOnWidgetTouch by rememberUpdatedState(onWidgetTouch)

        fun moveQuickNoteBy(totalDragX: Float, totalDragY: Float) {
            val start = quickNoteDragStart ?: return
            val columnSpan = start.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS
            val nextColumn =
                ((start.column ?: 0) + (totalDragX * horizontalSign / gridUnitWidthPx).roundToInt())
                    .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
            val nextRow =
                ((start.row ?: 0) + (totalDragY / gridUnitHeightPx).roundToInt())
                    .coerceAtLeast(0)
            previewMove(start.appWidgetId, nextColumn, nextRow)
        }

        SideEffect {
            appWidgetHost.onWidgetLongPress = if (layoutEditingEnabled) {
                { pageId ->
                    // A long-press on any page of a stack edits the whole stack.
                    val widget = currentLaidOut.stackOwnerOf(pageId)
                    if (widget != null) {
                        movePreview.reset()
                        hostDragStart = widget
                        scrollAtDragStart = panelScrollState.value
                        currentOnSetEditing(widget.appWidgetId)
                    }
                }
            } else {
                null
            }
            appWidgetHost.onWidgetDragMove = { pageId, dx, dy ->
                val start = hostDragStart
                if (start != null && pageId in start.pageIds) {
                    val id = start.appWidgetId
                    val columnSpan = start.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                    // Fold the auto-scroll distance into the vertical delta so the widget keeps
                    // tracking the finger as the page scrolls under it.
                    val scrollDelta = (panelScrollState.value - scrollAtDragStart).toFloat()
                    val nextColumn =
                        ((start.column ?: 0) + (dx * currentHorizontalSign / currentGridUnitWidthPx).roundToInt())
                            .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
                    val nextRow =
                        ((start.row ?: 0) + ((dy + scrollDelta) / currentGridUnitHeightPx).roundToInt())
                            .coerceAtLeast(0)
                    previewMove(id, nextColumn, nextRow)
                }
            }
            appWidgetHost.onWidgetDragEnd = { _ ->
                val final = liveLayout
                liveLayout = null
                hostDragStart = null
                movePreview.reset()
                if (final != null && final != currentLaidOut) currentOnPersist(final)
            }
            appWidgetHost.onWidgetTouch = { pageId ->
                currentOnWidgetTouch(currentLaidOut.stackOwnerOf(pageId)?.appWidgetId ?: pageId)
            }
        }

        // Auto-scroll the panel while a widget is being dragged near the viewport's top/bottom edge
        // so the dragged widget stays visible. Speed ramps with how far past the edge margin it is.
        val currentDisplayLayout by rememberUpdatedState(displayLayout)
        val currentEditingWidgetId by rememberUpdatedState(editingWidgetId)
        val currentViewportTopPx by rememberUpdatedState(viewportTopPx)
        val currentViewportHeightPx by rememberUpdatedState(viewportHeightPx)
        val isDragging = liveLayout != null
        LaunchedEffect(isDragging) {
            if (!isDragging) return@LaunchedEffect
            val edgeMarginPx = with(density) { rowHeight.toPx() }
            while (true) {
                withFrameNanos {}
                val viewportHeight = currentViewportHeightPx
                if (viewportHeight <= 0 || gridTopPx.isNaN()) continue
                val draggedId = currentEditingWidgetId ?: continue
                val dragged =
                    currentDisplayLayout.firstOrNull { it.appWidgetId == draggedId } ?: continue
                val row = dragged.row ?: 0
                val rowSpan = dragged.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
                val widgetTopPx = gridTopPx + with(density) { ((rowHeight + gap) * row).toPx() }
                val widgetHeightPx =
                    with(density) { (rowHeight * rowSpan + gap * (rowSpan - 1)).toPx() }
                val widgetBottomPx = widgetTopPx + widgetHeightPx
                val viewportTop = currentViewportTopPx
                val viewportBottom = viewportTop + viewportHeight
                val maxStep = viewportHeight / 45f
                val overshootBottom = widgetBottomPx - (viewportBottom - edgeMarginPx)
                val overshootTop = (viewportTop + edgeMarginPx) - widgetTopPx
                val step =
                    when {
                        overshootBottom > 0f ->
                            (overshootBottom / edgeMarginPx).coerceIn(0f, 1f) * maxStep
                        overshootTop > 0f ->
                            -((overshootTop / edgeMarginPx).coerceIn(0f, 1f) * maxStep)
                        else -> 0f
                    }
                if (step != 0f) panelScrollState.scrollBy(step)
            }
        }

        val hostedRows by remember(gridUnitHeightPx) {
            // All slots keep their geometry. Scroll changes only invalidate this reader when
            // the buffered row range changes.
            derivedStateOf(structuralEqualityPolicy()) {
                if (currentViewportHeightPx <= 0 || gridTopPx.isNaN()) {
                    IntRange.EMPTY
                } else {
                    val topRow = floor((currentViewportTopPx - gridTopPx) / gridUnitHeightPx).toInt()
                    val bottomRow =
                        floor(
                            (currentViewportTopPx + currentViewportHeightPx - gridTopPx) / gridUnitHeightPx,
                        ).toInt()
                    (topRow - HostedWidgetOverscanRows)..(bottomRow + HostedWidgetOverscanRows)
                }
            }
        }

        val rows =
            displayLayout.maxOfOrNull { widget ->
                (widget.row ?: 0) + (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
            } ?: 0
        val panelHeight =
            if (rows <= 0) 0.dp else rowHeight * rows + gap * (rows - 1)
        val animatedPanelHeight by
            animateDpAsState(
                targetValue = panelHeight,
                animationSpec = WidgetLayoutMotion,
                label = "widgetPanelHeight",
            )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(animatedPanelHeight)
                    .onGloballyPositioned { coordinates ->
                        gridTopPx = coordinates.localToWindow(Offset.Zero).y
                    },
        ) {
            displayLayout.forEach { widget ->
                key(widget.appWidgetId) {
                    val isEditing = editingWidgetId == widget.appWidgetId
                    val firstRow = widget.row ?: 0
                    val lastRow = firstRow + (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN) - 1
                    val isNearViewport =
                        !hostedRows.isEmpty() && firstRow <= hostedRows.last && lastRow >= hostedRows.first
                    // Views kept from an earlier entry are already inflated, so they show during the
                    // slide; only new views wait for the page to settle.
                    val canHost = showHostedWidgets || widget.appWidgetId in keptViewIds
                    WidgetPanelGridItem(
                        widget = widget,
                        isNearViewport = isNearViewport,
                        showHostedWidget = canHost && (isNearViewport || isEditing),
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        widgetContentAvailable = widgetContentAvailable,
                        isEditing = isEditing,
                        layoutEditingEnabled = layoutEditingEnabled,
                        cellWidth = cellWidth,
                        rowHeight = rowHeight,
                        gap = gap,
                        gridUnitWidthPx = gridUnitWidthPx,
                        gridUnitHeightPx = gridUnitHeightPx,
                        spec = specs[widget.appWidgetId],
                        onEdit = { onSetEditingWidgetId(widget.appWidgetId) },
                        onMoveBy = move@{ columnDelta, rowDelta ->
                            val next = moveWidgetByStep(laidOut, widget.appWidgetId, columnDelta, rowDelta)
                                ?: return@move false
                            onPersist(next)
                            true
                        },
                        onMovePreview = { targetColumn, targetRow ->
                            previewMove(widget.appWidgetId, targetColumn, targetRow)
                        },
                        onResizePreview = { resize ->
                            liveLayout =
                                resizeWidgetInGrid(
                                    widgets = liveLayout ?: laidOut,
                                    appWidgetId = widget.appWidgetId,
                                    resize = resize,
                                    specs = specs,
                                )
                        },
                        onInteractionEnd = {
                            val final = liveLayout
                            liveLayout = null
                            movePreview.reset()
                            if (final != null && final != laidOut) onPersist(final)
                        },
                        onRemove = { pageId -> onRemoveWidget(widget, pageId) },
                        onConfigure = { intent -> onConfigureWidget(widget, intent) },
                        onPinToHome = { onPinWidgetToHome(widget) },
                        onAddToStack = { onAddToStack(widget, it) },
                        onReorderStack = { onReorderStack(widget) },
                        onQuickNoteDragStart = {
                            movePreview.reset()
                            quickNoteDragStart = widget
                            onSetEditingWidgetId(widget.appWidgetId)
                        },
                        onQuickNoteDrag = { totalDragX, totalDragY ->
                            moveQuickNoteBy(totalDragX, totalDragY)
                        },
                        onQuickNoteDragEnd = {
                            val final = liveLayout
                            liveLayout = null
                            quickNoteDragStart = null
                            movePreview.reset()
                            if (final != null && final != laidOut) onPersist(final)
                        },
                        onQuickNoteFocusChanged = onQuickNoteFocusChanged,
                        stackIndicatorRevealKey = stackIndicatorRevealKey,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.WidgetPanelGridItem(
    widget: PanelWidgetInfo,
    isNearViewport: Boolean,
    showHostedWidget: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    widgetContentAvailable: Map<Int, Boolean>,
    isEditing: Boolean,
    layoutEditingEnabled: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    spec: WidgetGridSpec?,
    onEdit: () -> Unit,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: (pageId: Int) -> Unit,
    onConfigure: (Intent) -> Unit,
    onPinToHome: () -> Unit,
    onAddToStack: (WidgetStackFit) -> Unit,
    onReorderStack: () -> Unit,
    onQuickNoteDragStart: () -> Unit,
    onQuickNoteDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onQuickNoteDragEnd: () -> Unit,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
    stackIndicatorRevealKey: Int,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val column = widget.column ?: 0
    val row = widget.row ?: 0
    val columnSpan = widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
    val width = cellWidth * columnSpan + gap * (columnSpan - 1)
    val height = rowHeight * rowSpan + gap * (rowSpan - 1)
    val x = (cellWidth + gap) * column
    val y = (rowHeight + gap) * row

    // Animate each grid-step update, including the widget being edited, so resizing and the
    // resulting reflow feel continuous instead of snapping between row/column boundaries.
    val animatedX by animateDpAsState(targetValue = x, animationSpec = WidgetLayoutMotion, label = "widgetX")
    val animatedY by animateDpAsState(targetValue = y, animationSpec = WidgetLayoutMotion, label = "widgetY")
    val animatedWidth by
        animateDpAsState(targetValue = width, animationSpec = WidgetLayoutMotion, label = "widgetWidth")
    val animatedHeight by
        animateDpAsState(targetValue = height, animationSpec = WidgetLayoutMotion, label = "widgetHeight")
    val editScale by animateFloatAsState(
        targetValue = if (isEditing) WIDGET_EDIT_LIFT_SCALE else 1f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "widgetEditScale",
    )

    if (widget.isQuickNoteWidget() && widget.stack.isEmpty()) {
        QuickNotePanelGridItem(
            widget = widget,
            isEditing = isEditing,
            cellWidth = cellWidth,
            rowHeight = rowHeight,
            gap = gap,
            gridUnitHeightPx = gridUnitHeightPx,
            minRowSpan = spec?.minRowSpan ?: 1,
            onDragStart = onQuickNoteDragStart.takeIf { layoutEditingEnabled },
            onDrag = onQuickNoteDrag,
            onDragEnd = onQuickNoteDragEnd,
            onFocusChanged = onQuickNoteFocusChanged,
            onMoveBy = onMoveBy,
            onResizePreview = onResizePreview,
            onInteractionEnd = onInteractionEnd,
            onRemove = { onRemove(QUICK_NOTE_PANEL_WIDGET_ID) },
            onAddToStack = { onAddToStack(WidgetStackFit(columnSpan, rowSpan, cellWidth.value)) },
        )
        return
    }

    val pageIds = widget.pageIds
    // The Quick Note page has no provider info; every other page needs one to be shown.
    val pageInfos: List<Pair<Int, AppWidgetProviderInfo?>> =
        remember(pageIds, appWidgetHost) {
            pageIds.mapNotNull { id ->
                if (isQuickNotePage(id)) id to null else appWidgetHost.providerInfo(id)?.let { id to it }
            }
        }
    if (pageInfos.isEmpty()) return
    val isStack = pageInfos.size > 1
    val stackState = rememberWidgetStackState()
    SyncWidgetStackPages(pageCount = pageInfos.size, state = stackState)
    LaunchedEffect(stackIndicatorRevealKey) {
        if (stackIndicatorRevealKey > 0) stackState.revealIndicator()
    }
    // Edit actions act on the page in view.
    val currentPageId = pageInfos.getOrNull(stackState.settledPage)?.first ?: pageInfos.first().first
    val currentPageInfo = pageInfos.firstOrNull { it.first == currentPageId }?.second

    val configureIntent =
        remember(currentPageInfo, currentPageId) {
            currentPageInfo?.configure?.let {
                WidgetConfigureTrampolineActivity.intent(context, currentPageId)
            }
        }

    Box(
        modifier =
            Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .widgetEditAction(onEdit.takeIf { layoutEditingEnabled && !isEditing })
                .offset {
                    IntOffset(
                        x = with(density) { animatedX.roundToPx() },
                        y = with(density) { animatedY.roundToPx() },
                    )
                }
                .zIndex(if (isEditing) 1f else 0f)
                .graphicsLayer {
                    scaleX = editScale
                    scaleY = editScale
                },
    ) {
        val pageContent: @Composable BoxScope.(Int) -> Unit = { pageId ->
            if (isQuickNotePage(pageId)) {
                QuickNoteStackPage(
                    layoutEditingEnabled = layoutEditingEnabled,
                    onFocusChanged = onQuickNoteFocusChanged,
                    onDragStart = onQuickNoteDragStart,
                    onDrag = onQuickNoteDrag,
                    onDragEnd = onQuickNoteDragEnd,
                )
            }
            pageInfos.firstOrNull { it.first == pageId }?.second?.let { info ->
                val id = pageId
                WidgetPanelPageContent(
                    appWidgetId = id,
                    providerInfo = info,
                    isNearViewport = isNearViewport,
                    showHostedWidget = showHostedWidget,
                    hasWidgetContent = widgetContentAvailable[id] == true,
                    appWidgetManager = appWidgetManager,
                    appWidgetHost = appWidgetHost,
                    width = width,
                    height = height,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            }
        }
        // A single widget goes through the stack layout too, so its hosted view keeps its place
        // (and its loaded content) when a second page turns it into a stack.
        WidgetStackPager(
            pageIds = pageInfos.map { it.first },
            state = stackState,
            userScrollEnabled = !isEditing,
            pageContent = pageContent,
        )

        AnimatedWidgetEditOverlay(visible = isEditing) {
            if (!layoutEditingEnabled) return@AnimatedWidgetEditOverlay
            WidgetEditOverlay(
                column = column,
                row = row,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = spec?.minColumnSpan ?: 1,
                minRowSpan = spec?.minRowSpan ?: 1,
                hasConfigure = configureIntent != null,
                // Home has no Quick Note, so a stack holding it stays in the panel.
                canPinToHome = pageIds.none(::isQuickNotePage),
                onMoveBy = onMoveBy,
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = { onRemove(currentPageId) },
                onConfigure = { configureIntent?.let(onConfigure) },
                onPinToHome = onPinToHome,
                onAddToStack = {
                    onAddToStack(WidgetStackFit(columnSpan, rowSpan, cellWidth.value))
                },
                onReorderStack = onReorderStack.takeIf { isStack },
            )
        }
    }
}

private val QuickNoteGridSpec = WidgetGridSpec(minColumnSpan = WIDGET_PANEL_GRID_COLUMNS, minRowSpan = 1)

/** The Quick Note as a page of a widget stack; a long press on its title drags the whole stack. */
@Composable
private fun QuickNoteStackPage(
    layoutEditingEnabled: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    CompactQuickNoteWidget(
        modifier = Modifier.fillMaxSize(),
        onFocusChanged = onFocusChanged,
        onDragStart =
            if (layoutEditingEnabled) {
                {
                    totalDragX = 0f
                    totalDragY = 0f
                    onDragStart()
                }
            } else {
                null
            },
        onDrag = { dragX, dragY ->
            totalDragX += dragX
            totalDragY += dragY
            onDrag(totalDragX, totalDragY)
        },
        onDragEnd = onDragEnd,
    )
}
