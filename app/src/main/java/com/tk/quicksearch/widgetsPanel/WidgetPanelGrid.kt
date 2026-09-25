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
import androidx.compose.material.icons.rounded.Add
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
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperLogic
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.ImageAppearanceUtils
import android.util.SizeF
import kotlin.math.roundToInt

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
internal fun WidgetsPanelHeader(
    inEditMode: Boolean,
    onAddWidget: () -> Unit,
    onExitEditMode: () -> Unit,
) {
    val mutedColor = homeTextColor().copy(alpha = 0.7f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.widgets_panel_title),
            style = MaterialTheme.typography.titleMedium,
            color = mutedColor,
            modifier = Modifier.weight(1f),
        )
        if (inEditMode) {
            TextButton(
                onClick = onExitEditMode,
                colors = ButtonDefaults.textButtonColors(contentColor = homeTextColor()),
            ) {
                Text(
                    text = stringResource(R.string.dialog_done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            IconButton(
                onClick = onAddWidget,
                colors = IconButtonDefaults.iconButtonColors(contentColor = mutedColor),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.common_action_add),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
internal fun WidgetPanelGrid(
    widgets: List<PanelWidgetInfo>,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    widgetContentAvailable: Map<Int, Boolean>,
    editingWidgetId: Int?,
    density: Density,
    panelScrollState: ScrollState,
    viewportTopPx: Float,
    viewportHeightPx: Int,
    onPersist: (List<PanelWidgetInfo>) -> Unit,
    onSetEditingWidgetId: (Int?) -> Unit,
    onWidgetTouch: (Int) -> Boolean,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
    onRemoveWidget: (PanelWidgetInfo) -> Unit,
    onConfigureWidget: (PanelWidgetInfo, Intent) -> Unit,
    onPinWidgetToHome: (PanelWidgetInfo) -> Unit,
    packageManager: PackageManager,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val cellWidth =
            (maxWidth - WidgetPanelGridGap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        val rowHeight = WidgetPanelGridRowHeight
        val gap = WidgetPanelGridGap
        val gridUnitWidthPx = with(density) { (cellWidth + gap).toPx() }
        val gridUnitHeightPx = with(density) { (rowHeight + gap).toPx() }

        val specs =
            remember(widgets, appWidgetManager, cellWidth) {
                widgets.associate { widget ->
                    widget.appWidgetId to
                        if (widget.isQuickNoteWidget()) {
                            WidgetGridSpec(
                                minColumnSpan = WIDGET_PANEL_GRID_COLUMNS,
                                minRowSpan = 1,
                            )
                        } else {
                            val info = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                            val minWidthDp =
                                (info?.minResizeWidth?.takeIf { it > 0 } ?: info?.minWidth ?: 0)
                                    .let { with(density) { it.toDp().value } }
                            val minHeightDp =
                                (info?.minResizeHeight?.takeIf { it > 0 } ?: info?.minHeight ?: 0)
                                    .let { with(density) { it.toDp().value } }
                            WidgetGridSpec(
                                minColumnSpan =
                                    calculateGridColumnSpan(
                                        minWidthDp = minWidthDp,
                                        cellWidthDp = cellWidth.value,
                                        gapDp = gap.value,
                                    ),
                                minRowSpan =
                                    calculateGridRowSpan(
                                        minHeightDp = minHeightDp,
                                        rowHeightDp = rowHeight.value,
                                        gapDp = gap.value,
                                    ),
                            )
                        }
                }
            }

        val laidOut =
            remember(widgets, specs) { resolveWidgetGridLayout(widgets, specs) }

        LaunchedEffect(laidOut) {
            if (laidOut != widgets) onPersist(laidOut)
        }

        var liveLayout by remember { mutableStateOf<List<PanelWidgetInfo>?>(null) }
        val displayLayout = liveLayout ?: laidOut
        var hostDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        var quickNoteDragStart by remember { mutableStateOf<PanelWidgetInfo?>(null) }
        // Scroll offset captured when a host drag begins, so the drag target can compensate for any
        // auto-scroll and keep the widget under the finger (the host path works in screen coords).
        var scrollAtDragStart by remember { mutableIntStateOf(0) }
        // Window Y of the grid content box, used to locate the dragged widget for edge auto-scroll.
        var gridTopPx by remember { mutableFloatStateOf(0f) }

        val currentLaidOut by rememberUpdatedState(laidOut)
        val currentGridUnitWidthPx by rememberUpdatedState(gridUnitWidthPx)
        val currentGridUnitHeightPx by rememberUpdatedState(gridUnitHeightPx)
        val currentOnPersist by rememberUpdatedState(onPersist)
        val currentOnSetEditing by rememberUpdatedState(onSetEditingWidgetId)
        val currentOnWidgetTouch by rememberUpdatedState(onWidgetTouch)

        fun moveQuickNoteBy(totalDragX: Float, totalDragY: Float) {
            val start = quickNoteDragStart ?: return
            val columnSpan = start.columnSpan ?: WIDGET_PANEL_GRID_COLUMNS
            val nextColumn =
                ((start.column ?: 0) + (totalDragX / gridUnitWidthPx).roundToInt())
                    .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
            val nextRow =
                ((start.row ?: 0) + (totalDragY / gridUnitHeightPx).roundToInt())
                    .coerceAtLeast(0)
            liveLayout =
                moveWidgetToCell(
                    widgets = laidOut,
                    appWidgetId = QUICK_NOTE_PANEL_WIDGET_ID,
                    targetColumn = nextColumn,
                    targetRow = nextRow,
                )
        }

        SideEffect {
            appWidgetHost.onWidgetLongPress = { id ->
                val widget = currentLaidOut.firstOrNull { it.appWidgetId == id }
                if (widget != null) {
                    hostDragStart = widget
                    scrollAtDragStart = panelScrollState.value
                    currentOnSetEditing(id)
                }
            }
            appWidgetHost.onWidgetDragMove = { id, dx, dy ->
                val start = hostDragStart
                if (start != null && start.appWidgetId == id) {
                    val columnSpan = start.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                    // Fold the auto-scroll distance into the vertical delta so the widget keeps
                    // tracking the finger as the page scrolls under it.
                    val scrollDelta = (panelScrollState.value - scrollAtDragStart).toFloat()
                    val nextColumn =
                        ((start.column ?: 0) + (dx / currentGridUnitWidthPx).roundToInt())
                            .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
                    val nextRow =
                        ((start.row ?: 0) + ((dy + scrollDelta) / currentGridUnitHeightPx).roundToInt())
                            .coerceAtLeast(0)
                    liveLayout =
                        moveWidgetToCell(
                            widgets = currentLaidOut,
                            appWidgetId = id,
                            targetColumn = nextColumn,
                            targetRow = nextRow,
                        )
                }
            }
            appWidgetHost.onWidgetDragEnd = { _ ->
                val final = liveLayout
                liveLayout = null
                hostDragStart = null
                if (final != null && final != currentLaidOut) currentOnPersist(final)
            }
            appWidgetHost.onWidgetTouch = { widgetId -> currentOnWidgetTouch(widgetId) }
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
                if (viewportHeight <= 0) continue
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
                        gridTopPx = coordinates.positionInWindow().y
                    },
        ) {
            displayLayout.forEach { widget ->
                key(widget.appWidgetId) {
                    WidgetPanelGridItem(
                        widget = widget,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        hasWidgetContent = widgetContentAvailable[widget.appWidgetId] == true,
                        isEditing = editingWidgetId == widget.appWidgetId,
                        cellWidth = cellWidth,
                        rowHeight = rowHeight,
                        gap = gap,
                        gridUnitWidthPx = gridUnitWidthPx,
                        gridUnitHeightPx = gridUnitHeightPx,
                        spec = specs[widget.appWidgetId],
                        onMovePreview = { targetColumn, targetRow ->
                            liveLayout =
                                moveWidgetToCell(
                                    widgets = laidOut,
                                    appWidgetId = widget.appWidgetId,
                                    targetColumn = targetColumn,
                                    targetRow = targetRow,
                                )
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
                            if (final != null && final != laidOut) onPersist(final)
                        },
                        onRemove = { onRemoveWidget(widget) },
                        onConfigure = { intent -> onConfigureWidget(widget, intent) },
                        onPinToHome = { onPinWidgetToHome(widget) },
                        packageManager = packageManager,
                        onQuickNoteDragStart = {
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
                            if (final != null && final != laidOut) onPersist(final)
                        },
                        onQuickNoteFocusChanged = onQuickNoteFocusChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.WidgetPanelGridItem(
    widget: PanelWidgetInfo,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    hasWidgetContent: Boolean,
    isEditing: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    spec: WidgetGridSpec?,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
    onConfigure: (Intent) -> Unit,
    onPinToHome: () -> Unit,
    packageManager: PackageManager,
    onQuickNoteDragStart: () -> Unit,
    onQuickNoteDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onQuickNoteDragEnd: () -> Unit,
    onQuickNoteFocusChanged: (Boolean) -> Unit,
) {
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
        targetValue = if (isEditing) 1.02f else 1f,
        label = "widgetEditScale",
    )

    if (widget.isQuickNoteWidget()) {
        QuickNotePanelGridItem(
            widget = widget,
            isEditing = isEditing,
            cellWidth = cellWidth,
            rowHeight = rowHeight,
            gap = gap,
            gridUnitHeightPx = gridUnitHeightPx,
            minRowSpan = spec?.minRowSpan ?: 1,
            onDragStart = onQuickNoteDragStart,
            onDrag = onQuickNoteDrag,
            onDragEnd = onQuickNoteDragEnd,
            onFocusChanged = onQuickNoteFocusChanged,
            onResizePreview = onResizePreview,
            onInteractionEnd = onInteractionEnd,
            onRemove = onRemove,
        )
        return
    }

    val providerInfo = remember(widget.appWidgetId, appWidgetManager) {
        appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
    }
    if (providerInfo == null) return

    val configureIntent =
        remember(providerInfo, widget.appWidgetId, packageManager) {
            providerInfo.configure
                ?.takeIf { isWidgetConfigureActivityAccessible(packageManager, it) }
                ?.let { configure ->
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)
                }
        }

    Box(
        modifier =
            Modifier
                .size(width = animatedWidth, height = animatedHeight)
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
        HostedWidget(
            appWidgetId = widget.appWidgetId,
            providerInfo = providerInfo,
            appWidgetManager = appWidgetManager,
            appWidgetHost = appWidgetHost,
            width = animatedWidth,
            height = animatedHeight,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            modifier = Modifier.fillMaxSize(),
        )
        if (!hasWidgetContent) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = DesignTokens.WidgetPanelCardShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.widget_loading_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (isEditing) {
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
                canPinToHome = widget.home == null,
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = onRemove,
                onConfigure = { configureIntent?.let(onConfigure) },
                onPinToHome = onPinToHome,
            )
        }
    }
}
