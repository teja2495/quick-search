package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.data.preferences.NotesPreferences
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import android.util.SizeF
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
private val WidgetPanelBottomScrollSpace = 150.dp

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
    val activity = remember(context) { context.findActivity() }
    val appContext = context.applicationContext
    val packageManager = context.packageManager
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val appWidgetHost = remember(appContext) { WidgetPanelHost(appContext, WIDGET_PANEL_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val notesPreferences = remember(appContext) { NotesPreferences(appContext) }
    val isQuickNoteEnabled = remember(appContext) { notesPreferences.isQuickNoteEnabled() }

    var widgets by remember { mutableStateOf(preferences.getWidgets()) }
    var quickNoteWidget by remember { mutableStateOf(preferences.getQuickNoteWidget()) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<PendingWidgetRequest?>(null) }
    val panelScrollState = rememberScrollState()
    // Window bounds of the scroll viewport, used to auto-scroll a widget back into view while it is
    // being dragged near the top/bottom edge.
    var scrollViewportTopPx by remember { mutableFloatStateOf(0f) }
    var scrollViewportHeightPx by remember { mutableIntStateOf(0) }
    val widgetOptionsFactory =
        remember(configuration) {
            WidgetOptionsFactory(
                screenWidthDp = configuration.screenWidthDp,
                density = context.resources.displayMetrics.density,
                orientation = configuration.orientation,
            )
        }

    fun persistWidgets(next: List<PanelWidgetInfo>) {
        if (next == widgets) return
        widgets = next
        preferences.setWidgets(next)
    }

    fun persistPanelItems(next: List<PanelWidgetInfo>) {
        next.firstOrNull { it.isQuickNoteWidget() }?.let { quickNote ->
            quickNoteWidget = quickNote
            preferences.setQuickNoteWidget(quickNote)
        }
        persistWidgets(next.filterNot { it.isQuickNoteWidget() })
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
        val options = widgetOptionsFactory.create(columnSpan, rowSpan)
        appWidgetManager.updateAppWidgetOptions(request.appWidgetId, options)
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
        val initialOptions =
            widgetOptionsFactory.create(
                initialSpanFor(request.provider).first,
                initialSpanFor(request.provider).second,
            )
        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, request.appWidgetId)
                .putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
                    initialOptions,
                )
        pendingRequest = request
        val launchFailed =
            runCatching { configureLauncher.launch(intent) }
                .exceptionOrNull()
                ?.let { it is SecurityException || it is ActivityNotFoundException }
                ?: false

        if (launchFailed) {
            // Some widgets expose configure components that are not exported to third-party launchers.
            finalizeAddWidget(request)
        }
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
        val (columnSpan, rowSpan) = initialSpanFor(provider)
        val widgetOptions = widgetOptionsFactory.create(columnSpan, rowSpan)
        val canBind =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    appWidgetManager.bindAppWidgetIdIfAllowed(
                        appWidgetId,
                        provider.provider,
                        widgetOptions,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
                }
            }.getOrDefault(false)

        if (canBind) {
            launchConfigureIfNeeded(request)
        } else {
            pendingRequest = request
            val intent =
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, widgetOptions)
            val launchFailed =
                runCatching { bindLauncher.launch(intent) }
                    .exceptionOrNull()
                    ?.let { it is SecurityException || it is ActivityNotFoundException }
                    ?: false
            if (launchFailed) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                pendingRequest = null
            }
        }
    }

    DisposableEffect(appWidgetHost) {
        appWidgetHost.isScrollInProgressProvider = { panelScrollState.isScrollInProgress }
        appWidgetHost.startListening()
        onDispose {
            appWidgetHost.release()
        }
    }

    LaunchedEffect(appWidgetHost, panelScrollState) {
        snapshotFlow { panelScrollState.isScrollInProgress }
            .collect { inProgress ->
                if (inProgress) appWidgetHost.cancelAllPendingLongPresses()
            }
    }

    DisposableEffect(activity, showPicker) {
        val window = activity?.window
        val originalSoftInputMode = window?.attributes?.softInputMode

        if (showPicker && window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        onDispose {
            if (showPicker && window != null && originalSoftInputMode != null) {
                window.setSoftInputMode(originalSoftInputMode)
            }
        }
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

    val editModeDismissModifier =
        if (editingWidgetId != null) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { editingWidgetId = null })
            }
        } else {
            Modifier
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
                    .then(editModeDismissModifier)
                    .navigationBarsPadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .padding(bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {

                WidgetsPanelHeader(
                    inEditMode = editingWidgetId != null,
                    onAddWidget = { showPicker = true },
                    onExitEditMode = { editingWidgetId = null },
                )

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .onGloballyPositioned { coordinates ->
                                scrollViewportTopPx = coordinates.positionInWindow().y
                                scrollViewportHeightPx = coordinates.size.height
                            }
                            .verticalScroll(panelScrollState),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    val panelItems =
                        buildList {
                            if (isQuickNoteEnabled) add(quickNoteWidget)
                            addAll(widgets)
                        }
                    if (panelItems.isNotEmpty()) {
                        WidgetPanelGrid(
                            widgets = panelItems,
                            appWidgetManager = appWidgetManager,
                            appWidgetHost = appWidgetHost,
                            editingWidgetId = editingWidgetId,
                            density = density,
                            panelScrollState = panelScrollState,
                            viewportTopPx = scrollViewportTopPx,
                            viewportHeightPx = scrollViewportHeightPx,
                            onPersist = ::persistPanelItems,
                            onSetEditingWidgetId = { id -> editingWidgetId = id },
                            onRemoveWidget = { widget ->
                                if (widget.isQuickNoteWidget()) return@WidgetPanelGrid
                                appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                                persistWidgets(
                                    preferences.removeWidget(widget.appWidgetId),
                                )
                                editingWidgetId = null
                            },
                            onConfigureWidget = { _, configureIntent ->
                                runCatching { configureExistingLauncher.launch(configureIntent) }
                                editingWidgetId = null
                            },
                            packageManager = packageManager,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
                    }

                    Spacer(modifier = Modifier.height(WidgetPanelBottomScrollSpace))
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

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
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
            TextButton(onClick = onAddWidget) {
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
    panelScrollState: ScrollState,
    viewportTopPx: Float,
    viewportHeightPx: Int,
    onPersist: (List<PanelWidgetInfo>) -> Unit,
    onSetEditingWidgetId: (Int?) -> Unit,
    onRemoveWidget: (PanelWidgetInfo) -> Unit,
    onConfigureWidget: (PanelWidgetInfo, Intent) -> Unit,
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
                                minRowSpan = WIDGET_PANEL_DEFAULT_ROW_SPAN,
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
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
    packageManager: PackageManager,
    onQuickNoteDragStart: () -> Unit,
    onQuickNoteDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onQuickNoteDragEnd: () -> Unit,
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

    // The widget being edited is the one under the finger during a drag/resize: snap it to its
    // target so it tracks the gesture, while the others animate as they slide out of the way.
    val animatedX by animateDpAsState(targetValue = x, label = "widgetX")
    val animatedY by animateDpAsState(targetValue = y, label = "widgetY")
    val animatedWidth by animateDpAsState(targetValue = width, label = "widgetWidth")
    val animatedHeight by animateDpAsState(targetValue = height, label = "widgetHeight")
    val offsetX = if (isEditing) x else animatedX
    val offsetY = if (isEditing) y else animatedY
    val renderWidth = if (isEditing) width else animatedWidth
    val renderHeight = if (isEditing) height else animatedHeight
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
            onDragStart = onQuickNoteDragStart,
            onDrag = onQuickNoteDrag,
            onDragEnd = onQuickNoteDragEnd,
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
                .size(width = renderWidth, height = renderHeight)
                .offset {
                    IntOffset(
                        x = with(density) { offsetX.roundToPx() },
                        y = with(density) { offsetY.roundToPx() },
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
            appWidgetManager = appWidgetManager,
            appWidgetHost = appWidgetHost,
            width = width,
            height = height,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
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
private fun BoxScope.QuickNotePanelGridItem(
    widget: PanelWidgetInfo,
    isEditing: Boolean,
    cellWidth: Dp,
    rowHeight: Dp,
    gap: Dp,
    onDragStart: () -> Unit,
    onDrag: (totalDragX: Float, totalDragY: Float) -> Unit,
    onDragEnd: () -> Unit,
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
    val animatedY by animateDpAsState(targetValue = y, label = "quickNoteWidgetY")
    val offsetY = if (isEditing) y else animatedY
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier =
            Modifier
                .size(width = width, height = height)
                .offset {
                    IntOffset(
                        x = with(density) { x.roundToPx() },
                        y = with(density) { offsetY.roundToPx() },
                    )
                }
                .zIndex(if (isEditing) 1f else 0f),
    ) {
        CompactQuickNoteWidget(
            modifier = Modifier.fillMaxSize(),
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
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .border(
                            width = WidgetEditBorderWidth,
                            color = MaterialTheme.colorScheme.primary,
                            shape = DesignTokens.ExtraLargeCardShape,
                        ),
            )
        }
    }
}

@Composable
private fun HostedWidget(
    widget: PanelWidgetInfo,
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
            appWidgetHost.createView(ctx, widget.appWidgetId, providerInfo).apply {
                setAppWidget(widget.appWidgetId, providerInfo)
                layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                appWidgetManager.updateAppWidgetOptions(widget.appWidgetId, displayOptions)
                updateAppWidgetSize(displayOptions, widthDp, heightDp, widthDp, heightDp)
            }
        },
        update = { hostView ->
            hostView.layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            appWidgetManager.updateAppWidgetOptions(widget.appWidgetId, displayOptions)
            hostView.updateAppWidgetSize(displayOptions, widthDp, heightDp, widthDp, heightDp)
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
                )
                .pointerInput(Unit) {
                    // Absorb taps anywhere on the widget so the screen-level dismiss doesn't fire.
                    detectTapGestures(onTap = {})
                },
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

private class WidgetOptionsFactory(
    private val screenWidthDp: Int,
    private val density: Float,
    private val orientation: Int,
) {
    fun create(
        columnSpan: Int,
        rowSpan: Int,
    ): Bundle {
        val cellWidthDp = estimateGridCellWidthDp()
        val minWidthDp = spanToSizeDp(columnSpan, cellWidthDp, WidgetPanelGridGap.value)
        val minHeightDp =
            spanToSizeDp(rowSpan, WidgetPanelGridRowHeight.value, WidgetPanelGridGap.value)
        val maxWidthDp = minWidthDp
        val maxHeightDp = minHeightDp

        return bundleOf(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to minWidthDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to minHeightDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to maxWidthDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to maxHeightDp.roundToInt(),
            AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY to
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        ).applySamsungHostCompatExtras(
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            density = density,
            orientation = orientation,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(
                        SizeF(minWidthDp, minHeightDp),
                        SizeF(maxWidthDp, maxHeightDp),
                    ),
                )
            }
        }
    }

    private fun estimateGridCellWidthDp(): Float {
        val horizontalPaddingDp = DesignTokens.ContentHorizontalPadding.value
        val gapTotalDp = WidgetPanelGridGap.value * (WIDGET_PANEL_GRID_COLUMNS - 1)
        return (
            screenWidthDp - (horizontalPaddingDp * 2) - gapTotalDp
        ) / WIDGET_PANEL_GRID_COLUMNS.toFloat()
    }
}

private fun spanToSizeDp(
    span: Int,
    cellSizeDp: Float,
    gapDp: Float,
): Float = (cellSizeDp * span) + (gapDp * (span - 1).coerceAtLeast(0))

private fun createDisplayedWidgetOptions(
    widthDp: Int,
    heightDp: Int,
    columnSpan: Int,
    rowSpan: Int,
    density: Float,
    orientation: Int,
): Bundle =
    bundleOf(
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH to widthDp,
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT to heightDp,
        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH to widthDp,
        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT to heightDp,
        AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY to
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
    ).applySamsungHostCompatExtras(
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        density = density,
        orientation = orientation,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
            )
        }
    }

private fun Bundle.applySamsungHostCompatExtras(
    columnSpan: Int,
    rowSpan: Int,
    density: Float,
    orientation: Int,
): Bundle {
    if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) return this

    putInt("semAppWidgetColumnSpan", columnSpan)
    putInt("semAppWidgetRowSpan", rowSpan)
    putInt("semHostType", 1)
    putString("hsMode", "OneUI")
    putInt("hsWidgetDisplayId", 0)
    putFloat("hsResizeRatio", 1f)
    putFloat("semDisplayDensity", density)
    putInt("semWidgetStyle", 1)
    putInt("semWidgetSize", columnSpan * rowSpan)
    putInt("hsCurrentOrientation", if (orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1)
    putInt("hsForcedOrientation", 0)
    return this
}

private fun isWidgetConfigureActivityAccessible(
    packageManager: PackageManager,
    componentName: android.content.ComponentName,
): Boolean {
    val activityInfo =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getActivityInfo(
                    componentName,
                    PackageManager.ComponentInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getActivityInfo(componentName, 0)
            }
        }.getOrNull() ?: return false

    return activityInfo.enabled && activityInfo.exported
}
