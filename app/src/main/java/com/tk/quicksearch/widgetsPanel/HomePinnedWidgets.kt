package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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
 * Screen-wide guard for Home widget edit mode. While a widget is selected, a touch anywhere
 * outside it only leaves edit mode: the whole gesture is consumed before Compose content or
 * Android views (other widgets, the search bar, scrolling) can act on it.
 */
internal class HomeWidgetEditGuard {
    internal var editingBoundsInWindow: () -> Rect? = { null }
    // Snapshot state so screen-level gestures can switch off while a widget is in edit mode.
    internal var onDismiss: (() -> Unit)? by mutableStateOf(null)
    internal val isEditing: Boolean get() = onDismiss != null
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

    /** Moves a Home widget to the panel: drops it from Home right away and persists off the main thread. */
    fun moveToPanel(
        context: Context,
        ownerId: Int,
    ) {
        state.value = state.value.filterNot { it.appWidgetId == ownerId }
        val appContext = context.applicationContext
        persistScope.launch { WidgetsPanelPreferences(appContext).moveHomeWidgetToPanel(ownerId) }
    }

    private fun List<PanelWidgetInfo>.homePinned(): List<PanelWidgetInfo> =
        filter { widget -> widget.isOnHome && widget.pageIds.any { !isQuickNotePage(it) } }
}

/**
 * The At a Glance item. It always sits between the app grid and every Home widget: below the
 * widgets' side of Home in regular layout, mirrored with the rest of Home in one-handed mode.
 */
private val HOME_WIDGET_FLOOR_ITEM = ItemPriorityConfig.ItemType.UPCOMING_ALARM

/** New Home widgets land right after At a Glance (below it; above it in one-handed mode). */
internal fun defaultHomePlacement(
    widget: PanelWidgetInfo,
    allWidgets: List<PanelWidgetInfo>,
): HomeWidgetPlacement {
    val anchor = ItemPriorityConfig.ItemType.RECENT_QUERIES.name
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
    val anchorIndex = logicalOrder.withIndex().associate { (index, itemType) -> itemType.name to index }
    // Widgets anchored at or before At a Glance move to just after it, keeping their order.
    val floorIndex = logicalOrder.indexOf(HOME_WIDGET_FLOOR_ITEM)
    val floorAnchor = logicalOrder.getOrNull(floorIndex + 1)?.name ?: HOME_WIDGET_ANCHOR_END
    val widgetsByAnchor =
        widgets
            .filter { it.home != null }
            .sortedWith(
                compareBy<PanelWidgetInfo>({ anchorIndex[it.home?.anchor] ?: Int.MAX_VALUE }, { it.home?.order ?: 0 }),
            ).groupBy { widget ->
                val index = anchorIndex[widget.home?.anchor]
                when {
                    index == null -> HOME_WIDGET_ANCHOR_END
                    floorIndex >= 0 && index <= floorIndex -> floorAnchor
                    else -> widget.home?.anchor ?: HOME_WIDGET_ANCHOR_END
                }
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
            add(clampHomeWidgetTargetIndex(this, targetIndex, isReversed), dragged)
        }
    val logical = if (isReversed) nextVisual.reversed() else nextVisual
    // A widget stays in a row only while it still follows a member of the row it was in. The moved
    // widget leaves its row; a drag that drops it beside another widget joins it afterward.
    val startLogical = if (isReversed) visualEntries.reversed() else visualEntries
    val startLeaders = homeRowLeaders(startLogical.map { (it as? HomeLayoutEntry.Widget)?.widget?.home })
    val startRow =
        startLogical.withIndex().associate { (index, entry) -> entry.key to startLeaders[index] }
    val result = mutableListOf<PanelWidgetInfo>()
    val pending = mutableListOf<PanelWidgetInfo>()

    fun flush(anchor: String) {
        pending.forEachIndexed { index, widget ->
            val home = widget.home ?: return@forEachIndexed
            result += widget.copy(home = home.copy(anchor = anchor, order = index))
        }
        pending.clear()
    }

    var previous: HomeLayoutEntry? = null
    logical.forEach { entry ->
        when (entry) {
            is HomeLayoutEntry.Widget -> {
                val home = entry.widget.home
                val keepsRow =
                    entry !== dragged && previous is HomeLayoutEntry.Widget &&
                        startRow[previous.key] == startRow[entry.key]
                pending +=
                    if (home == null || !home.sharesRow || keepsRow) {
                        entry.widget
                    } else {
                        entry.widget.copy(home = home.copy(sharesRow = false))
                    }
            }
            is HomeLayoutEntry.Item -> flush(entry.itemType.name)
        }
        previous = entry
    }
    flush(HOME_WIDGET_ANCHOR_END)
    return result
}

/**
 * Keeps a widget's slot in [others] (the visual entries without it) on the far side of At a
 * Glance from the app grid.
 */
internal fun clampHomeWidgetTargetIndex(
    others: List<HomeLayoutEntry>,
    targetIndex: Int,
    isReversed: Boolean,
): Int {
    val index = targetIndex.coerceIn(0, others.size)
    val floorIndex = others.indexOfFirst { it is HomeLayoutEntry.Item && it.itemType == HOME_WIDGET_FLOOR_ITEM }
    if (floorIndex < 0) return index
    return if (isReversed) index.coerceAtMost(floorIndex) else index.coerceAtLeast(floorIndex + 1)
}

/** Whether a step move of [widgetId] to [targetIndex] lands where it asked, not clamped away. */
internal fun canMoveHomeWidgetTo(
    visualEntries: List<HomeLayoutEntry>,
    widgetId: Int,
    targetIndex: Int,
    isReversed: Boolean,
): Boolean {
    val others = visualEntries.filterNot { it is HomeLayoutEntry.Widget && it.widget.appWidgetId == widgetId }
    return targetIndex in 0..others.size && clampHomeWidgetTargetIndex(others, targetIndex, isReversed) == targetIndex
}

@Composable
internal fun rememberHomePinnedWidgets(enabled: Boolean): List<PanelWidgetInfo> {
    val context = LocalContext.current
    LaunchedEffect(Unit) { HomePinnedWidgetsStore.load(context) }
    val widgets by HomePinnedWidgetsStore.widgets.collectAsState()
    return if (enabled) widgets else emptyList()
}

/**
 * Home's widget host, owned above the root pages: the Search page is disposed while Widgets or
 * Settings is shown, and keeping the host keeps its widget views, so coming back shows them at
 * once instead of an empty slot followed by a fresh inflation.
 */
internal val LocalHomeWidgetHost = staticCompositionLocalOf<WidgetPanelHost?> { null }

/** Widget host for Home; see [LocalHomeWidgetHost]. */
@Composable
internal fun rememberHomeWidgetHost(): WidgetPanelHost {
    val appContext = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val host = remember(appContext) {
        WidgetPanelHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID).apply { setSuspended(true) }
    }
    DisposableEffect(host, lifecycle) {
        val stopFollowingLifecycle = host.listenWhileStarted(lifecycle)
        onDispose {
            stopFollowingLifecycle()
            host.release()
        }
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
    isEditing: Boolean,
    onWidgetBoundsChanged: (Rect) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
    onMoveStart: () -> Unit,
    onMove: (totalDx: Float, totalDy: Float) -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onInteractionEnd: () -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onRemove: () -> Unit,
    onConfigure: (Intent) -> Unit,
    onAddToStack: (WidgetStackFit) -> Unit,
    onReorderStack: () -> Unit,
    onMoveToPanel: () -> Unit,
) {
    val context = LocalContext.current
    val home = widget.home ?: return
    // Home has no Quick Note, so a stack shows its widget pages only.
    val pageIds = widget.pageIds
    val pageInfos =
        remember(pageIds, appWidgetHost) {
            pageIds.mapNotNull { id ->
                if (isQuickNotePage(id)) null else appWidgetHost.providerInfo(id)?.let { id to it }
            }
        }
    if (pageInfos.isEmpty()) return
    val isStack = pageInfos.size > 1
    val stackState = rememberWidgetStackState()
    SyncWidgetStackPages(pageCount = pageInfos.size, state = stackState)
    // Edit actions act on the page in view.
    val currentPageId = pageInfos.getOrNull(stackState.settledPage)?.first ?: pageInfos.first().first
    val currentPageInfo = pageInfos.firstOrNull { it.first == currentPageId }?.second
    val configureIntent =
        remember(currentPageInfo, currentPageId) {
            currentPageInfo?.configure?.let {
                WidgetConfigureTrampolineActivity.intent(context, currentPageId)
            }
        }
    val density = LocalDensity.current
    val rowHeight = WidgetPanelGridRowHeight
    val gap = WidgetPanelGridGap

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - gap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        // A stack can't shrink below what any of its pages needs.
        val minSpec =
            remember(pageInfos, cellWidth, density) {
                pageInfos
                    .map { (_, info) -> widgetMinGridSpec(info, density, cellWidth.value) }
                    .reduce { a, b ->
                        WidgetGridSpec(
                            minColumnSpan = maxOf(a.minColumnSpan, b.minColumnSpan),
                            minRowSpan = maxOf(a.minRowSpan, b.minRowSpan),
                        )
                    }
            }
        val minColumnSpan = minSpec.minColumnSpan
        val minRowSpan = minSpec.minRowSpan
        val width = cellWidth * home.columnSpan + gap * (home.columnSpan - 1)
        val height = rowHeight * home.rowSpan + gap * (home.rowSpan - 1)
        val x = (cellWidth + gap) * home.column
        val animatedX by animateDpAsState(x, HomeWidgetLayoutMotion, label = "homeWidgetX")
        val animatedWidth by animateDpAsState(width, HomeWidgetLayoutMotion, label = "homeWidgetWidth")
        val animatedHeight by animateDpAsState(height, HomeWidgetLayoutMotion, label = "homeWidgetHeight")

        // Home clips its content at its side edges. A selected widget touching an edge shrinks
        // just enough, away from that edge, for its overhanging resize pill to stay visible. One
        // uniform factor keeps the widget content undistorted; widgets clear of both edges keep
        // their size.
        val startInset = if (home.column == 0) WidgetResizeVisualShort / 2 else 0.dp
        val endInset =
            if (home.column + home.columnSpan >= WIDGET_PANEL_GRID_COLUMNS) WidgetResizeVisualShort / 2 else 0.dp
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
                    .widgetEditAction(onEdit.takeUnless { isEditing })
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
            // A single widget goes through the stack layout too, so its hosted view keeps its
            // place when a second page turns it into a stack.
            WidgetStackPager(
                pageIds = pageInfos.map { it.first },
                state = stackState,
                userScrollEnabled = !isEditing,
            ) { pageId ->
                val pageInfo = pageInfos.firstOrNull { it.first == pageId }?.second
                if (showHostedWidget && pageInfo != null) {
                    HostedWidget(
                        appWidgetId = pageId,
                        providerInfo = pageInfo,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        width = width,
                        height = height,
                        columnSpan = home.columnSpan,
                        rowSpan = home.rowSpan,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            AnimatedWidgetEditOverlay(visible = isEditing) {
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
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveBy = onMoveBy,
                    onInteractionEnd = onInteractionEnd,
                    onResizePreview = onResizePreview,
                    onRemove = onRemove,
                    onConfigure = configureIntent?.let { intent -> { onConfigure(intent) } },
                    onAddToStack = { onAddToStack(WidgetStackFit(home.columnSpan, home.rowSpan, cellWidth.value)) },
                    onReorderStack = onReorderStack.takeIf { isStack },
                    onMoveToPanel = onMoveToPanel,
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
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveBy: (columnDelta: Int, rowDelta: Int) -> Boolean,
    onInteractionEnd: () -> Unit,
    onResizePreview: (WidgetGridResize) -> Unit,
    onRemove: () -> Unit,
    onConfigure: (() -> Unit)?,
    onAddToStack: () -> Unit,
    onReorderStack: (() -> Unit)?,
    onMoveToPanel: () -> Unit,
) {
    val currentOnMoveStart by rememberUpdatedState(onMoveStart)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnInteractionEnd by rememberUpdatedState(onInteractionEnd)

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .widgetLayoutActions(
                    layout = WidgetGridResize(column, 0, columnSpan, rowSpan),
                    minColumnSpan = minColumnSpan,
                    minRowSpan = minRowSpan,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMove = onMoveBy,
                    onResize = { resize ->
                        onResizePreview(resize)
                        onInteractionEnd()
                    },
                )
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
        WidgetEditActionButtons(
            onRemove = onRemove,
            onConfigure = onConfigure,
            onAddToStack = onAddToStack,
            onReorderStack = onReorderStack,
            onMoveToPanel = onMoveToPanel,
        )
    }
}
