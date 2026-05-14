package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlin.math.roundToInt

private const val WIDGET_PANEL_HOST_ID = 8291
private const val WIDGET_PANEL_SWIPE_THRESHOLD_PX = 140f
private val WidgetPanelGridRowHeight = 80.dp
private val WidgetPanelGridGap = 8.dp
private val WidgetResizeEdgeHitLong = 64.dp
private val WidgetResizeEdgeHitShort = 32.dp
private val WidgetResizeVisualLong = 28.dp
private val WidgetResizeVisualShort = 6.dp
private val WidgetActionButtonSize = 32.dp
private val WidgetActionButtonInset = 6.dp
private val WidgetEditBorderWidth = 2.dp

private data class PendingWidgetRequest(
    val appWidgetId: Int,
    val provider: AppWidgetProviderInfo,
)

private enum class ResizeEdge(
    val xSign: Int,
    val ySign: Int,
    val alignment: Alignment,
    val isHorizontalAxis: Boolean,
) {
    Top(0, -1, Alignment.TopCenter, isHorizontalAxis = false),
    Bottom(0, 1, Alignment.BottomCenter, isHorizontalAxis = false),
    Start(-1, 0, Alignment.CenterStart, isHorizontalAxis = true),
    End(1, 0, Alignment.CenterEnd, isHorizontalAxis = true),
}

@Composable
fun WidgetsPanelScreen(
    onNavigateToSearch: () -> Unit,
    appTheme: AppTheme,
    overlayThemeIntensity: Float,
    deviceThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val density = LocalDensity.current
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val appWidgetHost = remember(appContext) { WidgetPanelHost(appContext, WIDGET_PANEL_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }

    var widgets by remember { mutableStateOf(preferences.getWidgets()) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<PendingWidgetRequest?>(null) }

    fun persistWidgets(next: List<PanelWidgetInfo>) {
        if (next == widgets) return
        widgets = next
        preferences.setWidgets(next)
    }

    SideEffect {
        appWidgetHost.onWidgetLongPress = { id ->
            if (widgets.any { it.appWidgetId == id }) {
                editingWidgetId = id
            }
        }
    }

    LaunchedEffect(widgets) {
        val available =
            widgets.filter { appWidgetManager.getAppWidgetInfo(it.appWidgetId) != null }
        if (available.size != widgets.size) {
            widgets
                .filterNot { available.any { kept -> kept.appWidgetId == it.appWidgetId } }
                .forEach { stale -> appWidgetHost.deleteAppWidgetId(stale.appWidgetId) }
            persistWidgets(available)
            if (editingWidgetId != null && available.none { it.appWidgetId == editingWidgetId }) {
                editingWidgetId = null
            }
        }
    }

    fun finalizeAddWidget(request: PendingWidgetRequest) {
        val provider = request.provider
        val (columnSpan, rowSpan) = initialSpanFor(provider)
        widgets =
            preferences.addWidget(
                appWidgetId = request.appWidgetId,
                provider = provider.provider,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
            )
        editingWidgetId = null
        showPicker = false
        pendingRequest = null
    }

    val configureLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                finalizeAddWidget(request)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    fun launchConfigureIfNeeded(request: PendingWidgetRequest) {
        val configure = request.provider.configure
        if (configure == null) {
            finalizeAddWidget(request)
            return
        }
        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, request.appWidgetId)
        pendingRequest = request
        configureLauncher.launch(intent)
    }

    val bindLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                launchConfigureIfNeeded(request)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    val configureExistingLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { /* result intentionally ignored */ }

    fun requestAddWidget(provider: AppWidgetProviderInfo) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val request = PendingWidgetRequest(appWidgetId, provider)
        val canBind =
            runCatching {
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
            }.getOrDefault(false)

        if (canBind) {
            launchConfigureIfNeeded(request)
        } else {
            pendingRequest = request
            val intent =
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            bindLauncher.launch(intent)
        }
    }

    DisposableEffect(appWidgetHost) {
        appWidgetHost.startListening()
        onDispose { appWidgetHost.stopListening() }
    }

    BackHandler {
        when {
            editingWidgetId != null -> editingWidgetId = null
            showPicker -> showPicker = false
            else -> onNavigateToSearch()
        }
    }

    val swipeBackModifier =
        Modifier.pointerInput(onNavigateToSearch) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount -> totalHorizontalDrag += dragAmount },
                onDragEnd = {
                    if (totalHorizontalDrag <= -WIDGET_PANEL_SWIPE_THRESHOLD_PX) {
                        onNavigateToSearch()
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
            )
        }

    SettingsScreenBackground(
        appTheme = appTheme,
        overlayThemeIntensity = overlayThemeIntensity,
        deviceThemeEnabled = deviceThemeEnabled,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(swipeBackModifier)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .padding(top = DesignTokens.SpacingXXLarge, bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                WidgetsPanelHeader(
                    inEditMode = editingWidgetId != null,
                    onAddWidget = { showPicker = true },
                    onExitEditMode = { editingWidgetId = null },
                )

                CompactQuickNoteWidget(modifier = Modifier.fillMaxWidth())

                if (widgets.isNotEmpty()) {
                    WidgetPanelGrid(
                        widgets = widgets,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        editingWidgetId = editingWidgetId,
                        density = density,
                        onPersist = ::persistWidgets,
                        onRemoveWidget = { widget ->
                            appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                            persistWidgets(
                                preferences.removeWidget(widget.appWidgetId),
                            )
                            editingWidgetId = null
                        },
                        onConfigureWidget = { _, configureIntent ->
                            configureExistingLauncher.launch(configureIntent)
                            editingWidgetId = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (showPicker) {
                WidgetPickerSheet(
                    appWidgetManager = appWidgetManager,
                    onDismiss = { showPicker = false },
                    onSelectWidget = ::requestAddWidget,
                )
            }
        }
    }
}

@Composable
private fun WidgetsPanelHeader(
    inEditMode: Boolean,
    onAddWidget: () -> Unit,
    onExitEditMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.widgets_panel_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (inEditMode) {
            TextButton(onClick = onExitEditMode) {
                Text(text = stringResource(R.string.dialog_done))
            }
        } else {
            Button(
                onClick = onAddWidget,
                shape = DesignTokens.ShapeXXLarge,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
                Spacer(modifier = Modifier.size(DesignTokens.TextButtonIconSpacing))
                Text(text = stringResource(R.string.common_action_add))
            }
        }
    }
}

@Composable
private fun WidgetPanelGrid(
    widgets: List<PanelWidgetInfo>,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    editingWidgetId: Int?,
    density: Density,
    onPersist: (List<PanelWidgetInfo>) -> Unit,
    onRemoveWidget: (PanelWidgetInfo) -> Unit,
    onConfigureWidget: (PanelWidgetInfo, Intent) -> Unit,
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
                    val info = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                    val minWidthDp =
                        (info?.minResizeWidth?.takeIf { it > 0 } ?: info?.minWidth ?: 0)
                            .let { with(density) { it.toDp().value } }
                    val minHeightDp =
                        (info?.minResizeHeight?.takeIf { it > 0 } ?: info?.minHeight ?: 0)
                            .let { with(density) { it.toDp().value } }
                    widget.appWidgetId to
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

        val laidOut =
            remember(widgets, specs) { resolveWidgetGridLayout(widgets, specs) }

        LaunchedEffect(laidOut) {
            if (laidOut != widgets) onPersist(laidOut)
        }

        var liveLayout by remember { mutableStateOf<List<PanelWidgetInfo>?>(null) }
        val displayLayout = liveLayout ?: laidOut

        val rows =
            displayLayout.maxOfOrNull { widget ->
                (widget.row ?: 0) + (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
            } ?: 0
        val panelHeight =
            if (rows <= 0) 0.dp else rowHeight * rows + gap * (rows - 1)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(panelHeight),
        ) {
            displayLayout.forEach { widget ->
                key(widget.appWidgetId) {
                    WidgetPanelGridItem(
                        widget = widget,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
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
                                    widgets = liveLayout ?: laidOut,
                                    appWidgetId = widget.appWidgetId,
                                    targetColumn = targetColumn,
                                    targetRow = targetRow,
                                    specs = specs,
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

    val animatedX by animateDpAsState(targetValue = x, label = "widgetX")
    val animatedY by animateDpAsState(targetValue = y, label = "widgetY")
    val animatedWidth by animateDpAsState(targetValue = width, label = "widgetWidth")
    val animatedHeight by animateDpAsState(targetValue = height, label = "widgetHeight")
    val editScale by animateFloatAsState(
        targetValue = if (isEditing) 1.02f else 1f,
        label = "widgetEditScale",
    )

    val providerInfo = remember(widget.appWidgetId, appWidgetManager) {
        appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
    }
    if (providerInfo == null) return

    val configureIntent =
        remember(providerInfo, widget.appWidgetId) {
            providerInfo.configure?.let { configure ->
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
            widget = widget,
            providerInfo = providerInfo,
            appWidgetHost = appWidgetHost,
            width = width,
            height = height,
            modifier = Modifier.fillMaxSize(),
        )

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
                onMovePreview = onMovePreview,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                onRemove = onRemove,
                onConfigure = { configureIntent?.let(onConfigure) },
            )
        }
    }
}

@Composable
private fun HostedWidget(
    widget: PanelWidgetInfo,
    providerInfo: AppWidgetProviderInfo,
    appWidgetHost: WidgetPanelHost,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }
    val widthDp = width.value.toInt()
    val heightDp = height.value.toInt()

    AndroidView(
        factory = { ctx ->
            appWidgetHost.createView(ctx, widget.appWidgetId, providerInfo).apply {
                setAppWidget(widget.appWidgetId, providerInfo)
                layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp)
            }
        },
        update = { hostView ->
            hostView.layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            hostView.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp)
        },
        modifier = modifier,
    )
}

@Composable
private fun BoxScope.WidgetEditOverlay(
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    hasConfigure: Boolean,
    onMovePreview: (column: Int, row: Int) -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onInteractionEnd: () -> Unit,
    onRemove: () -> Unit,
    onConfigure: () -> Unit,
) {
    val currentColumn by rememberUpdatedState(column)
    val currentRow by rememberUpdatedState(row)
    val currentColumnSpan by rememberUpdatedState(columnSpan)
    val currentRowSpan by rememberUpdatedState(rowSpan)

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .border(
                    width = WidgetEditBorderWidth,
                    color = MaterialTheme.colorScheme.primary,
                    shape = DesignTokens.ShapeMedium,
                ),
    ) {
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

        WidgetActionButton(
            icon = Icons.Rounded.Delete,
            tint = MaterialTheme.colorScheme.onError,
            background = MaterialTheme.colorScheme.error,
            onClick = onRemove,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(WidgetActionButtonInset),
        )
        if (hasConfigure) {
            WidgetActionButton(
                icon = Icons.Rounded.Settings,
                tint = MaterialTheme.colorScheme.onPrimary,
                background = MaterialTheme.colorScheme.primary,
                onClick = onConfigure,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(WidgetActionButtonInset),
            )
        }
    }
}

@Composable
private fun WidgetActionButton(
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
private fun EdgeResizeHandle(
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
        Box(
            modifier =
                Modifier
                    .size(width = visualWidth, height = visualHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
        )
    }
}

private fun initialSpanFor(provider: AppWidgetProviderInfo): Pair<Int, Int> {
    val targetW =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellWidth else 0
    val targetH =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellHeight else 0
    val columnSpan =
        if (targetW > 0) targetW.coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
        else WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    val rowSpan =
        if (targetH > 0) targetH.coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN)
        else WIDGET_PANEL_DEFAULT_ROW_SPAN
    return columnSpan to rowSpan
}
