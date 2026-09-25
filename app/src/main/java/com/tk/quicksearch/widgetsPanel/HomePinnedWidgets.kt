package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.core.ItemPriorityConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToInt


// Touches this close to the widget being edited still count as touching it, so the resize pills
// that straddle its border stay usable.
private val HomeWidgetEditTouchMargin = 12.dp

// Step the empty space a widget can hold above it (below it in one-handed mode) snaps to while
// it is dragged, so a dropped widget keeps the room the user left around it.
internal val HomeWidgetGapStep = 8.dp

internal const val HOME_STACK_LEADING_KEY = "home-stack-leading"
internal const val HOME_STACK_TRAILING_KEY = "home-stack-trailing"

/**
 * A widget only holds empty space at the open end of Home, where nothing renders past it: the
 * bottom normally, and the top in one-handed mode, where the layout is reversed and bottom
 * aligned. Sections with nothing to show take no room, so they do not close the end. Anywhere
 * else the widget takes a slot between items and Home reflows around it as usual.
 */
internal fun List<HomeLayoutEntry>.isOpenEndIndex(
    index: Int,
    isReversed: Boolean,
    heightOf: (HomeLayoutEntry) -> Int,
): Boolean {
    val beyond = if (isReversed) take(index) else drop(index)
    return beyond.none { heightOf(it) > 0 }
}

/**
 * Screen-wide guard for Home widget edit mode. While a widget is selected, a touch anywhere
 * outside it only leaves edit mode: the whole gesture is consumed before Compose content or
 * Android views (other widgets, the search bar, scrolling) can act on it.
 */
internal class HomeWidgetEditGuard {
    internal var editingBoundsInWindow: () -> Rect? = { null }
    internal var onDismiss: (() -> Unit)? = null
    internal var originInWindow: Offset = Offset.Zero
}

internal val LocalHomeWidgetEditGuard = staticCompositionLocalOf<HomeWidgetEditGuard?> { null }

/** Apply at the root of the screen that hosts Home widgets. */
internal fun Modifier.homeWidgetEditTapGuard(guard: HomeWidgetEditGuard): Modifier =
    onGloballyPositioned { coordinates -> guard.originInWindow = coordinates.positionInWindow() }
        .pointerInput(guard) {
            val marginPx = HomeWidgetEditTouchMargin.toPx()
            awaitEachGesture {
                // Initial pass runs parent-first, so this sees the touch before any child, and
                // consuming it here also keeps it from being dispatched to Android views.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val dismiss = guard.onDismiss ?: return@awaitEachGesture
                val widgetBounds = guard.editingBoundsInWindow()
                val touchInWindow = guard.originInWindow + down.position
                if (widgetBounds != null && widgetBounds.inflate(marginPx).contains(touchInWindow)) {
                    return@awaitEachGesture
                }
                down.consume()
                dismiss()
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }

/** Process-wide source of the widgets pinned to Home, shared by the widgets panel and Home. */
internal object HomePinnedWidgetsStore {
    private val state = MutableStateFlow<List<PanelWidgetInfo>>(emptyList())
    private val loaded = AtomicBoolean(false)

    // Single-threaded so placement writes land in the order they were made.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    val widgets: StateFlow<List<PanelWidgetInfo>> = state.asStateFlow()

    suspend fun load(context: Context) {
        if (loaded.get()) return
        val appContext = context.applicationContext
        val stored = withContext(Dispatchers.IO) { WidgetsPanelPreferences(appContext).getWidgets() }
        // A publish that landed while reading is newer than what was read.
        if (loaded.compareAndSet(false, true)) state.value = stored.homePinned()
    }

    /** Publishes the full panel widget list after it was persisted. */
    fun publish(widgets: List<PanelWidgetInfo>) {
        loaded.set(true)
        state.value = widgets.homePinned()
    }

    /** Applies the placements in memory right away and persists them off the main thread. */
    fun updatePlacements(
        context: Context,
        placements: Map<Int, HomeWidgetPlacement?>,
    ) {
        if (placements.isEmpty()) return
        state.value =
            state.value.mapNotNull { widget ->
                if (widget.appWidgetId in placements) {
                    placements[widget.appWidgetId]?.let { widget.copy(home = it) }
                } else {
                    widget
                }
            }
        val appContext = context.applicationContext
        persistScope.launch {
            val orphanedIds = WidgetsPanelPreferences(appContext).setHomePlacements(placements)
            if (orphanedIds.isNotEmpty()) {
                val host = AppWidgetHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID)
                orphanedIds.forEach { runCatching { host.deleteAppWidgetId(it) } }
            }
        }
    }

    private fun List<PanelWidgetInfo>.homePinned(): List<PanelWidgetInfo> =
        filter { it.home != null && !it.isQuickNoteWidget() }
}

/** New Home widgets land right below the app grid (above it in one-handed mode). */
internal fun defaultHomePlacement(
    widget: PanelWidgetInfo,
    allWidgets: List<PanelWidgetInfo>,
): HomeWidgetPlacement {
    val anchor = ItemPriorityConfig.ItemType.UPCOMING_ALARM.name
    val firstOrderAtAnchor =
        allWidgets.mapNotNull { it.home?.takeIf { home -> home.anchor == anchor }?.order }.minOrNull()
    val columnSpan =
        (widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN).coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
    return HomeWidgetPlacement(
        anchor = anchor,
        order = (firstOrderAtAnchor ?: 1) - 1,
        column = (widget.column ?: 0).coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan),
        columnSpan = columnSpan,
        rowSpan = (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN).coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN),
    )
}

internal sealed interface HomeLayoutEntry {
    val key: Any

    data class Item(
        val itemType: ItemPriorityConfig.ItemType,
    ) : HomeLayoutEntry {
        override val key: Any get() = itemType
    }

    data class Widget(
        val widget: PanelWidgetInfo,
    ) : HomeLayoutEntry {
        override val key: Any get() = widget.appWidgetId
    }
}

/**
 * Interleaves pinned widgets with the Home layout. [layoutOrder] is the rendered (visual) order;
 * widget anchors refer to logical order, so each widget sits before its anchor item and the whole
 * sequence flips with the layout in one-handed mode. Unknown anchors fall back to the end.
 */
internal fun homeLayoutEntries(
    layoutOrder: List<ItemPriorityConfig.ItemType>,
    isReversed: Boolean,
    widgets: List<PanelWidgetInfo>,
): List<HomeLayoutEntry> {
    val logicalOrder = if (isReversed) layoutOrder.reversed() else layoutOrder
    val anchors = logicalOrder.mapTo(HashSet()) { it.name }
    val widgetsByAnchor =
        widgets
            .filter { it.home != null }
            .sortedBy { it.home?.order ?: 0 }
            .groupBy { widget ->
                widget.home?.anchor?.takeIf { it in anchors } ?: HOME_WIDGET_ANCHOR_END
            }
    val logicalEntries =
        buildList {
            logicalOrder.forEach { itemType ->
                widgetsByAnchor[itemType.name]?.forEach { add(HomeLayoutEntry.Widget(it)) }
                add(HomeLayoutEntry.Item(itemType))
            }
            widgetsByAnchor[HOME_WIDGET_ANCHOR_END]?.forEach { add(HomeLayoutEntry.Widget(it)) }
        }
    return if (isReversed) logicalEntries.reversed() else logicalEntries
}

/**
 * Moves [widgetId] to [targetIndex] of the visual entries that remain once it is taken out, and
 * returns every Home widget with anchors and orders rewritten to match the new sequence.
 */
internal fun moveHomeWidget(
    visualEntries: List<HomeLayoutEntry>,
    widgetId: Int,
    targetIndex: Int,
    isReversed: Boolean,
): List<PanelWidgetInfo> {
    val dragged =
        visualEntries.firstOrNull { it is HomeLayoutEntry.Widget && it.widget.appWidgetId == widgetId }
            ?: return visualEntries.filterIsInstance<HomeLayoutEntry.Widget>().map { it.widget }
    val nextVisual =
        visualEntries.filterNot { it === dragged }.toMutableList().apply {
            add(targetIndex.coerceIn(0, size), dragged)
        }
    val logical = if (isReversed) nextVisual.reversed() else nextVisual
    val result = mutableListOf<PanelWidgetInfo>()
    val pending = mutableListOf<PanelWidgetInfo>()

    fun flush(anchor: String) {
        pending.forEachIndexed { index, widget ->
            val home = widget.home ?: return@forEachIndexed
            result += widget.copy(home = home.copy(anchor = anchor, order = index))
        }
        pending.clear()
    }

    logical.forEach { entry ->
        when (entry) {
            is HomeLayoutEntry.Widget -> pending += entry.widget
            is HomeLayoutEntry.Item -> flush(entry.itemType.name)
        }
    }
    flush(HOME_WIDGET_ANCHOR_END)
    return result
}

@Composable
internal fun rememberHomePinnedWidgets(enabled: Boolean): List<PanelWidgetInfo> {
    val context = LocalContext.current
    LaunchedEffect(Unit) { HomePinnedWidgetsStore.load(context) }
    val widgets by HomePinnedWidgetsStore.widgets.collectAsState()
    return if (enabled) widgets else emptyList()
}

/** Widget host for Home; only composed while Home has pinned widgets. */
@Composable
internal fun rememberHomeWidgetHost(): WidgetPanelHost {
    val appContext = LocalContext.current.applicationContext
    val host = remember(appContext) { WidgetPanelHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID) }
    DisposableEffect(host) {
        host.startListeningShared()
        onDispose { host.release() }
    }
    return host
}

@Composable
internal fun HomeStackColumn(
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        content()
    }
}

@Composable
internal fun HomePinnedWidgetItem(
    widget: PanelWidgetInfo,
    showHostedWidget: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    packageManager: PackageManager,
    isEditing: Boolean,
    onWidgetBoundsChanged: (Rect) -> Unit,
    modifier: Modifier,
    onMoveStart: () -> Unit,
    onMove: (totalDx: Float, totalDy: Float) -> Unit,
    onInteractionEnd: () -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onRemove: () -> Unit,
    onConfigure: (Intent) -> Unit,
) {
    val home = widget.home ?: return
    val providerInfo =
        remember(widget.appWidgetId, appWidgetManager) {
            appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
        } ?: return
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
    val density = LocalDensity.current
    val rowHeight = WidgetPanelGridRowHeight
    val gap = WidgetPanelGridGap

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - gap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        val minColumnSpan =
            remember(providerInfo, cellWidth) {
                val minWidthPx = providerInfo.minResizeWidth.takeIf { it > 0 } ?: providerInfo.minWidth
                calculateGridColumnSpan(
                    minWidthDp = with(density) { minWidthPx.toDp().value },
                    cellWidthDp = cellWidth.value,
                    gapDp = gap.value,
                )
            }
        val minRowSpan =
            remember(providerInfo) {
                val minHeightPx = providerInfo.minResizeHeight.takeIf { it > 0 } ?: providerInfo.minHeight
                calculateGridRowSpan(
                    minHeightDp = with(density) { minHeightPx.toDp().value },
                    rowHeightDp = rowHeight.value,
                    gapDp = gap.value,
                )
            }
        val width = cellWidth * home.columnSpan + gap * (home.columnSpan - 1)
        val height = rowHeight * home.rowSpan + gap * (home.rowSpan - 1)
        val x = (cellWidth + gap) * home.column
        val animatedX by animateDpAsState(x, HomeWidgetLayoutMotion, label = "homeWidgetX")
        val animatedWidth by animateDpAsState(width, HomeWidgetLayoutMotion, label = "homeWidgetWidth")
        val animatedHeight by animateDpAsState(height, HomeWidgetLayoutMotion, label = "homeWidgetHeight")

        // Home clips its content at its side edges. A selected widget touching an edge shrinks
        // just enough, away from that edge, for what overhangs its border there to stay visible:
        // the resize pill on the start side, the Remove badge on the end side. One uniform factor
        // keeps the widget content undistorted; widgets clear of both edges keep their size.
        val startInset = if (home.column == 0) WidgetResizeVisualShort / 2 else 0.dp
        val endInset =
            if (home.column + home.columnSpan >= WIDGET_PANEL_GRID_COLUMNS) WidgetActionButtonCornerOffset else 0.dp
        val horizontalInset = startInset + endInset
        val editScale by animateFloatAsState(
            targetValue =
                if (isEditing && horizontalInset > 0.dp && width > horizontalInset) {
                    (width - horizontalInset) / width
                } else {
                    1f
                },
            label = "homeWidgetEditScale",
        )
        val scalePivotX = if (horizontalInset > 0.dp) startInset / horizontalInset else 0.5f
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(x = animatedX.roundToPx(), y = 0) }
                    .size(width = animatedWidth, height = animatedHeight)
                    .graphicsLayer {
                        scaleX = editScale
                        scaleY = editScale
                        transformOrigin = TransformOrigin(scalePivotX, 0.5f)
                    }
                    .onGloballyPositioned { coordinates -> onWidgetBoundsChanged(coordinates.boundsInWindow()) }
                    .pointerInput(Unit) {
                        // Runs after the widget view in the main pass; consuming the down keeps
                        // Home's background long-press menu and double-tap from also firing.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false).consume()
                        }
                    },
        ) {
            if (showHostedWidget) {
                HostedWidget(
                    appWidgetId = widget.appWidgetId,
                    providerInfo = providerInfo,
                    appWidgetManager = appWidgetManager,
                    appWidgetHost = appWidgetHost,
                    width = animatedWidth,
                    height = animatedHeight,
                    columnSpan = home.columnSpan,
                    rowSpan = home.rowSpan,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isEditing) {
                HomeWidgetEditOverlay(
                    column = home.column,
                    columnSpan = home.columnSpan,
                    rowSpan = home.rowSpan,
                    gridUnitWidthPx = with(density) { (cellWidth + gap).toPx() },
                    gridUnitHeightPx = with(density) { (rowHeight + gap).toPx() },
                    minColumnSpan = minColumnSpan,
                    minRowSpan = minRowSpan,
                    onMoveStart = onMoveStart,
                    onMove = onMove,
                    onInteractionEnd = onInteractionEnd,
                    onResizePreview = onResizePreview,
                    onRemove = onRemove,
                    onConfigure = configureIntent?.let { intent -> { onConfigure(intent) } },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.HomeWidgetEditOverlay(
    column: Int,
    columnSpan: Int,
    rowSpan: Int,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    onMoveStart: () -> Unit,
    onMove: (totalDx: Float, totalDy: Float) -> Unit,
    onInteractionEnd: () -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onRemove: () -> Unit,
    onConfigure: (() -> Unit)?,
) {
    val currentOnMoveStart by rememberUpdatedState(onMoveStart)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnInteractionEnd by rememberUpdatedState(onInteractionEnd)

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    // Absorb taps on the selected widget so they don't reach the widget content.
                    detectTapGestures(onTap = {})
                },
    ) {
        WidgetEditBorder()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                                currentOnMoveStart()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                currentOnMove(totalDragX, totalDragY)
                            },
                            onDragEnd = { currentOnInteractionEnd() },
                            onDragCancel = { currentOnInteractionEnd() },
                        )
                    },
        )
        // Home is a vertical list, so height grows from the bottom edge only.
        ResizeEdge.entries.forEach { edge ->
            EdgeResizeHandle(
                edge = edge,
                startColumn = column,
                // Home has no grid rows, so a widget can always grow upward from its top edge;
                // only the maximum row span limits it (clamped in the resize preview).
                startRow = WIDGET_PANEL_MAX_ROW_SPAN,
                startColumnSpan = columnSpan,
                startRowSpan = rowSpan,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = minColumnSpan,
                minRowSpan = minRowSpan,
                onResizePreview = onResizePreview,
                onInteractionEnd = onInteractionEnd,
                modifier = Modifier.align(edge.alignment),
            )
        }
        WidgetEditActionButtons(onRemove = onRemove, onConfigure = onConfigure)
    }
}
