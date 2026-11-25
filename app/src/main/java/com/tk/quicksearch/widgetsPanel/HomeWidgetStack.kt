package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.theme.AppMotion
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.app.navigation.LocalRootPageSettled
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.shared.ui.components.AppMessages
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

private data class HomeEntryBounds(
    val top: Int,
    val height: Int,
)

/**
 * Smooths a Home entry's jump to a new layout position while a widget is being dragged to a new
 * slot: the entry starts drawn at its old position and eases into the new one. Not used while
 * resizing, where the widget's height already animates and neighbours must follow it exactly.
 */
private class HomeReorderShift(
    private val scope: CoroutineScope,
) {
    private var lastTop: Int? = null
    private var lastEpoch = -1
    private var job: Job? = null
    var shift by mutableFloatStateOf(0f)
        private set

    /**
     * Call from `onPlaced` with the entry's layout top, measured outside any translation.
     * [reorderEpoch] changes only when the dragged widget lands in a different slot, so each entry
     * animates once per reorder; the continuous shifts from growing empty space are applied
     * straight away, keeping the layout glued to the finger.
     */
    fun onPlacedTop(
        top: Int,
        reorderEpoch: Int,
        animate: Boolean,
    ) {
        val previous = lastTop
        lastTop = top
        val shouldAnimate = animate && reorderEpoch != lastEpoch
        lastEpoch = reorderEpoch
        if (previous == null || previous == top || !shouldAnimate) return
        // Written during layout so this frame already draws the entry at its old position.
        val start = shift + (previous - top)
        shift = start
        job?.cancel()
        job =
            scope.launch {
                animate(start, 0f, animationSpec = HomeReorderMotion) { value, _ -> shift = value }
            }
    }
}

// Home motion is snappier than the widgets panel's: entries shift out of the way while a widget
// is dragged, and resizes should track the finger closely.
private val HomeReorderMotion = AppMotion.placement<Float>()
internal val HomeWidgetLayoutMotion = AppMotion.placement<Dp>()

private sealed interface HomeStackSheet {
    data class AddTo(
        val ownerId: Int,
        val fit: WidgetStackFit,
    ) : HomeStackSheet

    data class Reorder(
        val ownerId: Int,
    ) : HomeStackSheet
}

private data class HomeWidgetDrag(
    val appWidgetId: Int,
    val startTop: Int,
    val startColumn: Int,
    val startOriginY: Float,
    val startStackHeight: Float,
    // Where every other widget sat when the drag began, measured from Home's anchored end, so
    // widgets in empty space hold still while the dragged one moves around them.
    val startDistances: Map<Int, Float>,
)

/**
 * Home content with pinned widgets interleaved between the regular Home items.
 *
 * Long-press a widget (or drag it while selected) to move it above or below any Home item;
 * while selected it also offers remove (Home only), settings, and resize handles.
 */
@Composable
internal fun HomeWidgetStack(
    layoutOrder: List<ItemPriorityConfig.ItemType>,
    isReversed: Boolean,
    widgets: List<PanelWidgetInfo>,
    showWidgets: Boolean,
    host: WidgetPanelHost,
    spacing: Dp,
    isScrollInProgress: () -> Boolean,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
    itemContent: @Composable (ItemPriorityConfig.ItemType) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val horizontalSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context.applicationContext) }

    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    // Preview of placements while dragging or resizing; persisted when the gesture ends.
    var liveWidgets by remember { mutableStateOf<List<PanelWidgetInfo>?>(null) }
    var isInteracting by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<HomeWidgetDrag?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggedCurrentTop by remember { mutableIntStateOf(0) }
    // Growing the empty space moves the whole stack in one-handed mode, where Home is bottom
    // aligned, so drag math needs the stack's own position on screen.
    var stackOriginY by remember { mutableFloatStateOf(0f) }
    // Bumped only when the dragged widget changes slot; see [HomeReorderShift.onPlacedTop].
    var reorderEpoch by remember { mutableIntStateOf(0) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    // Total stack height, written during measure and read from gesture callbacks.
    val stackHeightPx = remember { floatArrayOf(0f) }
    // Eases a dropped widget from where the finger left it into its slot.
    var settlingWidgetId by remember { mutableStateOf<Int?>(null) }
    var settleOffset by remember { mutableFloatStateOf(0f) }
    val animationScope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    // Written during measure and read only from gesture callbacks, so it isn't snapshot state.
    val gridUnitWidthPx = remember { floatArrayOf(0f) }
    // Let Home finish sliding in before third-party RemoteViews are inflated.
    val pageSettled = LocalRootPageSettled.current
    var showHostedWidgets by remember { mutableStateOf(false) }
    val bounds = remember { mutableMapOf<Any, HomeEntryBounds>() }
    // Each widget's on-screen bounds, for the screen-level edit tap guard.
    val windowBounds = remember { mutableMapOf<Int, Rect>() }

    val displayWidgets = liveWidgets ?: widgets
    val entries = homeLayoutEntries(layoutOrder, isReversed, if (showWidgets) displayWidgets else emptyList())

    val currentWidgets by rememberUpdatedState(widgets)
    val currentDisplayWidgets by rememberUpdatedState(displayWidgets)
    val currentLayoutOrder by rememberUpdatedState(layoutOrder)
    val currentIsReversed by rememberUpdatedState(isReversed)

    LaunchedEffect(pageSettled) {
        if (!pageSettled || showHostedWidgets) return@LaunchedEffect
        withFrameNanos { }
        showHostedWidgets = true
    }

    // Drop the preview once the store reflects the committed placements.
    LaunchedEffect(widgets) {
        if (!isInteracting) liveWidgets = null
    }

    LaunchedEffect(showWidgets) {
        if (!showWidgets) editingWidgetId = null
    }

    // Wait for the incoming page to settle, and suspend updates while a query hides the widgets.
    DisposableEffect(host, showWidgets, showHostedWidgets) {
        host.setSuspended(!showWidgets || !showHostedWidgets)
        onDispose { }
    }

    LaunchedEffect(host, widgets) {
        host.retainViews(widgets.flatMapTo(HashSet()) { it.pageIds })
    }

    // The host outlives Home's page, keeping its widget views for the next time Home is shown.
    DisposableEffect(host) {
        onDispose { host.detachSurface() }
    }
    // Views kept from Home's last showing are already inflated, so they show during the slide in;
    // only new views wait for the page to settle. Snapshot at entry: a shown view is no longer kept.
    val keptViewIds = remember(host) { host.keptViewIds() }

    // Home scrolls under the widgets: a touch that stops a fling, or one the scroll takes over,
    // must not turn into a long-press edit.
    val currentIsScrollInProgress by rememberUpdatedState(isScrollInProgress)
    DisposableEffect(host) {
        host.isScrollInProgressProvider = { currentIsScrollInProgress() }
        onDispose { host.isScrollInProgressProvider = { false } }
    }
    LaunchedEffect(host) {
        snapshotFlow { currentIsScrollInProgress() }
            .collect { inProgress -> if (inProgress) host.cancelAllPendingLongPresses() }
    }

    LaunchedEffect(widgets) {
        val editing = editingWidgetId ?: return@LaunchedEffect
        if (widgets.none { it.appWidgetId == editing }) editingWidgetId = null
    }

    BackHandler(enabled = editingWidgetId != null) { editingWidgetId = null }

    // Add-to-stack picker or reorder drawer for a Home stack, by the stack's owner id.
    var stackSheet by remember { mutableStateOf<HomeStackSheet?>(null) }

    val editGuard = LocalHomeWidgetEditGuard.current
    DisposableEffect(editGuard, editingWidgetId) {
        val guard = editGuard ?: return@DisposableEffect onDispose { }
        val editing = editingWidgetId
        val dismiss = { editingWidgetId = null }
        if (editing != null) {
            guard.onDismiss = dismiss
            guard.editingBoundsInWindow = { windowBounds[editing] }
        }
        onDispose {
            if (guard.onDismiss === dismiss) {
                guard.onDismiss = null
                guard.editingBoundsInWindow = { null }
            }
        }
    }

    fun commit() {
        val final = liveWidgets
        drag?.let { active ->
            val offset = active.startTop + dragOffsetY - (stackOriginY - active.startOriginY) - draggedCurrentTop
            if (offset != 0f) {
                settleJob?.cancel()
                settlingWidgetId = active.appWidgetId
                settleOffset = offset
                settleJob =
                    animationScope.launch {
                        animate(offset, 0f, animationSpec = HomeReorderMotion) { value, _ ->
                            settleOffset = value
                        }
                        settlingWidgetId = null
                    }
            }
        }
        isInteracting = false
        drag = null
        dragOffsetY = 0f
        if (final == null) return
        val committed = currentWidgets.associateBy { it.appWidgetId }
        val changes =
            final
                .filter { committed[it.appWidgetId]?.home != it.home }
                .associate { it.appWidgetId to it.home }
        if (changes.isEmpty()) {
            liveWidgets = null
        } else {
            HomePinnedWidgetsStore.updatePlacements(context, changes)
        }
    }

    fun startDrag(appWidgetId: Int) {
        val widget = currentDisplayWidgets.firstOrNull { it.appWidgetId == appWidgetId } ?: return
        val home = widget.home ?: return
        val startTop = bounds[appWidgetId]?.top ?: 0
        val stackHeight = stackHeightPx[0]
        val startDistances =
            currentDisplayWidgets
                .filter { it.appWidgetId != appWidgetId }
                .mapNotNull { other ->
                    val otherBounds = bounds[other.appWidgetId] ?: return@mapNotNull null
                    val distance =
                        if (currentIsReversed) {
                            stackHeight - otherBounds.top - otherBounds.height
                        } else {
                            otherBounds.top.toFloat()
                        }
                    other.appWidgetId to distance
                }.toMap()
        drag =
            HomeWidgetDrag(
                appWidgetId = appWidgetId,
                startTop = startTop,
                startColumn = home.column,
                startOriginY = stackOriginY,
                startStackHeight = stackHeight,
                startDistances = startDistances,
            )
        dragTargetIndex = -1
        draggedCurrentTop = startTop
        dragOffsetY = 0f
        isInteracting = true
        liveWidgets = currentDisplayWidgets
        editingWidgetId = appWidgetId
    }

    fun dragTo(
        totalDx: Float,
        totalDy: Float,
    ) {
        val active = drag ?: return
        dragOffsetY = totalDy
        val current = liveWidgets ?: currentWidgets
        val visual = homeLayoutEntries(currentLayoutOrder, currentIsReversed, current)
        val draggedHeight = bounds[active.appWidgetId]?.height ?: 0
        // Rounded like the layout rounds it, so widgets holding still don't drift by a pixel per step.
        val stepPx = with(density) { HomeWidgetGapStep.roundToPx().toFloat() }
        // Work in the stack as it was when the drag began. One-handed Home is bottom aligned, so
        // as empty space comes and goes the stack's top moves while its bottom stays put; shifting
        // positions to that start frame keeps entries that hold still on screen still here too.
        val frameShift = if (currentIsReversed) active.startStackHeight - stackHeightPx[0] else 0f
        val fingerTop = active.startTop + totalDy
        val draggedCenter = fingerTop + draggedHeight / 2f
        val others =
            visual.filterNot {
                it is HomeLayoutEntry.Widget && it.widget.appWidgetId == active.appWidgetId
            }
        var targetIndex = others.size
        for ((index, entry) in others.withIndex()) {
            val entryBounds = bounds[entry.key] ?: continue
            if (entryBounds.height <= 0) continue
            if (entryBounds.top + frameShift + entryBounds.height / 2f > draggedCenter) {
                targetIndex = index
                break
            }
        }
        targetIndex = clampHomeWidgetTargetIndex(others, targetIndex, currentIsReversed)
        if (targetIndex != dragTargetIndex) {
            if (dragTargetIndex != -1) reorderEpoch++
            dragTargetIndex = targetIndex
        }

        // Widgets with only widgets beyond them can sit in empty space: the dragged one stays where
        // the finger leaves it, and the others hold their places unless it pushes them outward.
        // Between Home items there is no room to spare, and Home reflows as usual.
        val reordered = moveHomeWidget(visual, active.appWidgetId, targetIndex, currentIsReversed)
        val fingerDistance =
            if (currentIsReversed) active.startStackHeight - fingerTop - draggedHeight else fingerTop
        val gapsPx =
            homeWidgetGapsPx(
                slots =
                    homeGapSlots(
                        homeLayoutEntries(currentLayoutOrder, currentIsReversed, reordered),
                        currentIsReversed,
                    ) { key -> bounds[key]?.height ?: 0 },
                spacingPx = with(density) { spacing.roundToPx() },
            ) { widgetId ->
                if (widgetId == active.appWidgetId) fingerDistance else active.startDistances[widgetId]
            }
        val unitWidth = gridUnitWidthPx[0]
        liveWidgets =
            reordered.map { widget ->
                val home = widget.home ?: return@map widget
                val gapSteps =
                    ((gapsPx[widget.appWidgetId] ?: 0f) / stepPx).roundToInt().coerceIn(0, HOME_WIDGET_GAP_STEPS_MAX)
                val column =
                    if (widget.appWidgetId != active.appWidgetId || unitWidth <= 0f) {
                        home.column
                    } else {
                        (active.startColumn + (totalDx * horizontalSign / unitWidth).roundToInt())
                            .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - home.columnSpan)
                    }
                if (column == home.column && gapSteps == home.gapSteps) {
                    widget
                } else {
                    widget.copy(home = home.copy(column = column, gapSteps = gapSteps))
                }
            }
    }

    fun resizePreview(
        appWidgetId: Int,
        resize: WidgetGridResize,
    ) {
        isInteracting = true
        liveWidgets =
            (liveWidgets ?: currentWidgets).map { widget ->
                val home = widget.home
                if (widget.appWidgetId != appWidgetId || home == null) {
                    widget
                } else {
                    widget.copy(
                        home =
                            home.copy(
                                column = resize.column,
                                columnSpan = resize.columnSpan,
                                rowSpan = resize.rowSpan.coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN),
                            ),
                    )
                }
            }
    }

    val configureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // The host reports the page touched; a stack moves and edits as one.
    fun ownerOf(pageId: Int): Int = currentDisplayWidgets.stackOwnerOf(pageId)?.appWidgetId ?: pageId

    SideEffect {
        host.onWidgetLongPress = { id -> startDrag(ownerOf(id)) }
        host.onWidgetDragMove = { id, dx, dy -> if (drag?.appWidgetId == ownerOf(id)) dragTo(dx, dy) }
        host.onWidgetDragEnd = { _ -> commit() }
        host.onWidgetTouch = { id ->
            // Touching another widget while one is selected only leaves edit mode.
            val editing = editingWidgetId
            if (editing != null && editing != ownerOf(id)) {
                editingWidgetId = null
                true
            } else {
                false
            }
        }
    }

    when (val sheet = stackSheet) {
        is HomeStackSheet.AddTo ->
            HomeAddWidgetSheet(
                onDismiss = { stackSheet = null },
                stackTarget = sheet.ownerId to sheet.fit,
            )
        is HomeStackSheet.Reorder ->
            HomeStackReorderSheet(
                ownerId = sheet.ownerId,
                widgets = widgets,
                host = host,
                onOwnerChange = { ownerId -> stackSheet = ownerId?.let(HomeStackSheet::Reorder) },
            )
        null -> Unit
    }

    Layout(
        content = {
            key(HOME_STACK_LEADING_KEY) {
                HomeStackColumn(
                    spacing = spacing,
                    modifier =
                        Modifier
                            .layoutId(HOME_STACK_LEADING_KEY)
                            .onPlaced { coordinates ->
                                bounds[HOME_STACK_LEADING_KEY] =
                                    HomeEntryBounds(
                                        top = coordinates.positionInParent().y.roundToInt(),
                                        height = coordinates.size.height,
                                    )
                            },
                ) { leadingContent() }
            }
            entries.forEachIndexed { entryIndex, entry ->
                key(entry.key) {
                    when (entry) {
                        is HomeLayoutEntry.Item -> {
                            val reorderShift = remember { HomeReorderShift(animationScope) }
                            HomeStackColumn(
                                spacing = spacing,
                                modifier =
                                    Modifier
                                        .layoutId(entry.key)
                                        .onPlaced { coordinates ->
                                            val top = coordinates.positionInParent().y.roundToInt()
                                            bounds[entry.key] =
                                                HomeEntryBounds(top = top, height = coordinates.size.height)
                                            reorderShift.onPlacedTop(
                                                top,
                                                reorderEpoch = reorderEpoch,
                                                animate = drag != null,
                                            )
                                        }.graphicsLayer { translationY = reorderShift.shift },
                            ) {
                                itemContent(entry.itemType)
                            }
                        }

                        is HomeLayoutEntry.Widget -> {
                            val widget = entry.widget
                            val appWidgetId = widget.appWidgetId
                            val isDragged = drag?.appWidgetId == appWidgetId
                            val reorderShift = remember { HomeReorderShift(animationScope) }
                            HomePinnedWidgetItem(
                                widget = widget,
                                showHostedWidget = showHostedWidgets || widget.pageIds.any { it in keptViewIds },
                                appWidgetManager = appWidgetManager,
                                appWidgetHost = host,
                                isEditing = editingWidgetId == appWidgetId,
                                onWidgetBoundsChanged = { boundsInWindow ->
                                    windowBounds[appWidgetId] = boundsInWindow
                                },
                                modifier =
                                    Modifier
                                        .layoutId(appWidgetId)
                                        .onPlaced { coordinates ->
                                            val top = coordinates.positionInParent().y.roundToInt()
                                            bounds[appWidgetId] =
                                                HomeEntryBounds(top = top, height = coordinates.size.height)
                                            if (drag?.appWidgetId == appWidgetId) {
                                                draggedCurrentTop = top
                                            }
                                            reorderShift.onPlacedTop(
                                                top,
                                                reorderEpoch = reorderEpoch,
                                                animate = drag != null && drag?.appWidgetId != appWidgetId,
                                            )
                                        }.zIndex(if (isDragged) 2f else if (editingWidgetId == appWidgetId) 1f else 0f)
                                        .graphicsLayer {
                                            val active = drag
                                            translationY =
                                                when {
                                                    active?.appWidgetId == appWidgetId ->
                                                        active.startTop + dragOffsetY -
                                                            (stackOriginY - active.startOriginY) -
                                                            draggedCurrentTop
                                                    settlingWidgetId == appWidgetId -> settleOffset
                                                    else -> reorderShift.shift
                                                }
                                        },
                                onEdit = { editingWidgetId = appWidgetId },
                                onMoveStart = { startDrag(appWidgetId) },
                                onMove = { dx, dy -> dragTo(dx, dy) },
                                canMoveUp = canMoveHomeWidgetTo(entries, appWidgetId, entryIndex - 1, isReversed),
                                canMoveDown = canMoveHomeWidgetTo(entries, appWidgetId, entryIndex + 1, isReversed),
                                onMoveBy = { columnDelta, rowDelta ->
                                    liveWidgets =
                                        if (rowDelta != 0) {
                                            moveHomeWidget(entries, appWidgetId, entryIndex + rowDelta, isReversed)
                                        } else {
                                            displayWidgets.map { item ->
                                                val home = item.home
                                                if (item.appWidgetId == appWidgetId && home != null) {
                                                    item.copy(home = home.copy(column = home.column + columnDelta))
                                                } else {
                                                    item
                                                }
                                            }
                                        }
                                    commit()
                                    true
                                },
                                onInteractionEnd = { commit() },
                                onResizePreview = { resize -> resizePreview(appWidgetId, resize) },
                                onRemove = {
                                    editingWidgetId = null
                                    HomePinnedWidgetsStore.updatePlacements(context, mapOf(appWidgetId to null))
                                },
                                onConfigure = { intent ->
                                    editingWidgetId = null
                                    runCatching { configureLauncher.launch(intent) }
                                },
                                onAddToStack = { fit ->
                                    editingWidgetId = null
                                    stackSheet = HomeStackSheet.AddTo(appWidgetId, fit)
                                },
                                onReorderStack = {
                                    editingWidgetId = null
                                    stackSheet = HomeStackSheet.Reorder(appWidgetId)
                                },
                                onMoveToPanel = {
                                    editingWidgetId = null
                                    HomePinnedWidgetsStore.moveToPanel(context, appWidgetId)
                                    AppMessages.show(context, R.string.widget_moved_to_panel)
                                },
                            )
                        }
                    }
                }
            }
            key(HOME_STACK_TRAILING_KEY) {
                HomeStackColumn(
                    spacing = spacing,
                    modifier =
                        Modifier
                            .layoutId(HOME_STACK_TRAILING_KEY)
                            .onPlaced { coordinates ->
                                bounds[HOME_STACK_TRAILING_KEY] =
                                    HomeEntryBounds(
                                        top = coordinates.positionInParent().y.roundToInt(),
                                        height = coordinates.size.height,
                                    )
                            },
                ) { trailingContent() }
            }
        },
        modifier =
            modifier.onPlaced { coordinates -> stackOriginY = coordinates.positionInWindow().y },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val spacingPx = spacing.roundToPx()
        val gapStepPx = HomeWidgetGapStep.roundToPx()
        val placeables = measurables.map { it.measure(childConstraints) }
        // Widgets with only widgets beyond them in logical order hold their empty space; between
        // Home items there is none. Sections with nothing to show take no room, so they don't
        // count; leading and trailing content does. Keyed rather than positional: a widget whose
        // provider is gone composes nothing, so entries and measured children don't line up.
        val gapStepsByKey =
            entries
                .filterIsInstance<HomeLayoutEntry.Widget>()
                .associate { it.key to (it.widget.home?.gapSteps ?: 0) }
        val logicalIndices = if (isReversed) placeables.indices.reversed() else placeables.indices
        val eligible =
            homeGapEligible(
                logicalIndices.map { index ->
                    HomeGapSlot(placeables[index].height, measurables[index].layoutId as? Int)
                },
            )
        val gaps = IntArray(measurables.size)
        logicalIndices.forEachIndexed { logicalIndex, index ->
            if (eligible[logicalIndex]) gaps[index] = (gapStepsByKey[measurables[index].layoutId] ?: 0) * gapStepPx
        }
        val positions = IntArray(placeables.size)
        var y = 0
        var hasVisibleChild = false
        placeables.forEachIndexed { index, placeable ->
            // Empty slots (sections with nothing to show) take no spacing. Growing children ease
            // into the gap so height animations don't jump by the full spacing on their first frame.
            if (placeable.height > 0 && hasVisibleChild) y += min(placeable.height, spacingPx)
            // A widget's empty space sits before it in logical Home order: above it normally,
            // below it once the layout is reversed for one-handed mode.
            val gap = if (placeable.height > 0) gaps[index] else 0
            if (!isReversed) y += gap
            positions[index] = y
            y += placeable.height
            if (isReversed) y += gap
            if (placeable.height > 0) hasVisibleChild = true
        }
        stackHeightPx[0] = y.toFloat()
        val width =
            (placeables.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = y.coerceIn(constraints.minHeight, constraints.maxHeight)
        val cellWidthPx =
            (width - WidgetPanelGridGap.toPx() * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        gridUnitWidthPx[0] = cellWidthPx + WidgetPanelGridGap.toPx()
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.place(0, positions[index]) }
        }
    }
}

/** Column that stacks one Home item's blocks with the same spacing Home uses between items. */
