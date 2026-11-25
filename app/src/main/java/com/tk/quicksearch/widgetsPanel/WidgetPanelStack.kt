package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.os.Process
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.LocalFrostedCardSource
import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.shared.ui.components.AppLoadingIndicator
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Size of the stack a widget is being picked for; the picker lists only widgets that fit it. */
internal data class WidgetStackFit(
    val columnSpan: Int,
    val rowSpan: Int,
    val cellWidthDp: Float,
)

/** Saves the stack the picker adds to (its owner id and size) across activity recreation. */
internal val WidgetStackTargetSaver: Saver<Pair<Int, WidgetStackFit>?, List<Any>> =
    Saver(
        save = { target ->
            target?.let { (ownerId, fit) -> listOf(ownerId, fit.columnSpan, fit.rowSpan, fit.cellWidthDp) }
                ?: emptyList()
        },
        restore = { saved ->
            if (saved.size == 4) {
                (saved[0] as Int) to WidgetStackFit(saved[1] as Int, saved[2] as Int, saved[3] as Float)
            } else {
                null
            }
        },
    )

/** Smallest grid size [info] can be shown at, from its minimum (resize) size. */
internal fun widgetMinGridSpec(
    info: AppWidgetProviderInfo?,
    density: Density,
    cellWidthDp: Float,
): WidgetGridSpec {
    val minWidthDp =
        (info?.minResizeWidth?.takeIf { it > 0 } ?: info?.minWidth ?: 0)
            .let { with(density) { it.toDp().value } }
    val minHeightDp =
        (info?.minResizeHeight?.takeIf { it > 0 } ?: info?.minHeight ?: 0)
            .let { with(density) { it.toDp().value } }
    return WidgetGridSpec(
        minColumnSpan =
            calculateGridColumnSpan(
                minWidthDp = minWidthDp,
                cellWidthDp = cellWidthDp,
                gapDp = WidgetPanelGridGap.value,
            ),
        minRowSpan =
            calculateGridRowSpan(
                minHeightDp = minHeightDp,
                rowHeightDp = WidgetPanelGridRowHeight.value,
                gapDp = WidgetPanelGridGap.value,
            ),
    )
}

internal fun WidgetStackFit.fits(
    info: AppWidgetProviderInfo,
    density: Density,
): Boolean {
    val spec = widgetMinGridSpec(info, density, cellWidthDp)
    return spec.minColumnSpan <= columnSpan && spec.minRowSpan <= rowSpan
}

/**
 * One hosted widget on the panel: its last snapshot while the live view loads, the live view, and
 * a loading card when there is neither. Used for regular widgets and for each page of a stack.
 */
@Composable
internal fun BoxScope.WidgetPanelPageContent(
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    isNearViewport: Boolean,
    showHostedWidget: Boolean,
    hasWidgetContent: Boolean,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: WidgetPanelHost,
    width: Dp,
    height: Dp,
    columnSpan: Int,
    rowSpan: Int,
) {
    val context = LocalContext.current
    val showsLiveContent = showHostedWidget && hasWidgetContent
    val shouldReadSnapshot =
        isNearViewport && !showsLiveContent && providerInfo.profile == Process.myUserHandle()
    var snapshot by remember(appWidgetId, providerInfo.provider, shouldReadSnapshot) {
        mutableStateOf<Bitmap?>(null)
    }
    var snapshotPending by remember(appWidgetId, providerInfo.provider, shouldReadSnapshot) {
        mutableStateOf(shouldReadSnapshot)
    }
    LaunchedEffect(appWidgetId, providerInfo.provider, shouldReadSnapshot) {
        if (shouldReadSnapshot) {
            snapshot = WidgetPanelSnapshots.read(context.applicationContext, appWidgetId, providerInfo.provider)
            snapshotPending = false
        }
    }
    val preview = remember(snapshot) { snapshot?.asImageBitmap() }

    if (preview != null) {
        Image(
            bitmap = preview,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize().clip(AppShapes.widgetPanelCard),
        )
    }
    if (showHostedWidget) {
        HostedWidget(
            appWidgetId = appWidgetId,
            providerInfo = providerInfo,
            appWidgetManager = appWidgetManager,
            appWidgetHost = appWidgetHost,
            width = width,
            height = height,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            modifier = Modifier.fillMaxSize().clip(AppShapes.widgetPanelCard),
        )
    }
    if (isNearViewport && !showsLiveContent && preview == null && !snapshotPending) {
        // A plain Box rather than Surface: Surface consumes touches, which would block the host
        // view's long-press and leave a widget whose provider never sends content impossible
        // to edit or remove.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .frostedCardBackground(AppShapes.widgetPanelCard)
                    .background(
                        color =
                            if (LocalFrostedCardSource.current?.isActive == true) {
                                SearchResultCardDefaults.containerColor(
                                    showWallpaperBackground = true,
                                    overlayContainerColor = null,
                                )
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        shape = AppShapes.widgetPanelCard,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            AppLoadingIndicator()
        }
    }
}

/**
 * Which page of a widget stack is in view. [position] is fractional while a page is dragged or
 * settling; [settledPage] is the page it rests on, or is settling to.
 */
@Stable
internal class WidgetStackState(initialPage: Int) {
    var position by mutableFloatStateOf(initialPage.toFloat())
        private set

    /** Changes only when a swipe picks another page, so a drag doesn't recompose the stack. */
    var settledPage by mutableIntStateOf(initialPage)
        private set

    /** Page nearest to [position], for the page indicator. */
    val nearestPage: Int by derivedStateOf { position.roundToInt().coerceAtLeast(0) }

    /** True while a page is dragged or settling. */
    var isMoving by mutableStateOf(false)
        private set

    /** Bumped to show the page indicator for a moment without moving the pages. */
    var indicatorRevealCount by mutableIntStateOf(0)
        private set

    fun revealIndicator() {
        indicatorRevealCount++
    }

    private var settleJob: Job? = null

    fun stop() {
        settleJob?.cancel()
        settleJob = null
    }

    /**
     * Moves by [delta] pages, at most one page either side of [settledPage] (whose neighbours are
     * the pages composed), resisting past the first and last page.
     */
    fun dragBy(delta: Float, pageCount: Int) {
        stop()
        isMoving = true
        val lastPage = (pageCount - 1).toFloat()
        val next = position + delta
        val resisted = if (next < 0f || next > lastPage) position + delta * WIDGET_STACK_EDGE_RESISTANCE else next
        position =
            resisted
                .coerceIn(-WIDGET_STACK_MAX_OVERSCROLL, lastPage + WIDGET_STACK_MAX_OVERSCROLL)
                .coerceIn(settledPage - 1f, settledPage + 1f)
    }

    fun animateToPage(
        scope: CoroutineScope,
        page: Int,
        initialVelocity: Float = 0f,
    ) {
        stop()
        settledPage = page
        isMoving = true
        settleJob =
            scope.launch {
                animate(
                    initialValue = position,
                    targetValue = page.toFloat(),
                    initialVelocity = initialVelocity,
                    animationSpec = AppMotion.settle(visibilityThreshold = WIDGET_STACK_SETTLE_THRESHOLD),
                ) { value, _ -> position = value }
                isMoving = false
            }
    }

    fun snapToPage(page: Int) {
        stop()
        settledPage = page
        position = page.toFloat()
        isMoving = false
    }

    companion object {
        val Saver: Saver<WidgetStackState, Int> = Saver(save = { it.settledPage }, restore = { WidgetStackState(it) })
    }
}

@Composable
internal fun rememberWidgetStackState(): WidgetStackState =
    rememberSaveable(saver = WidgetStackState.Saver) { WidgetStackState(0) }

/**
 * Keeps [state] on a real page as pages come and go, and slides to a page just added. Call it
 * where the widget is composed even before it becomes a stack, so the slide also plays when the
 * second page turns it into one.
 */
@Composable
internal fun SyncWidgetStackPages(
    pageCount: Int,
    state: WidgetStackState,
) {
    val scope = rememberCoroutineScope()
    // Saved so a page added from a bind or configure screen that recreated the activity still
    // counts as added.
    var lastPageCount by rememberSaveable { mutableIntStateOf(pageCount) }
    LaunchedEffect(pageCount) {
        when {
            pageCount > lastPageCount -> {
                state.animateToPage(scope, pageCount - 1)
                state.revealIndicator()
            }
            state.settledPage > pageCount - 1 -> state.snapToPage((pageCount - 1).coerceAtLeast(0))
        }
        lastPageCount = pageCount
    }
}

/**
 * Pages of a widget stack, swiped sideways. Every page is a plain child, like a regular widget,
 * so its hosted view keeps its place when a widget turns into a stack and back. A horizontal drag
 * is claimed only once it passes touch slop, which cancels the touch in the widget, so taps and
 * long-presses on a page still reach it. The pages follow the finger and settle with a no-bounce
 * spring that carries the release velocity; neighbours shrink slightly as they slide. Only a
 * scale is animated: fading a layer that holds a live widget view would render it offscreen on
 * every frame.
 */
@Composable
internal fun BoxScope.WidgetStackPager(
    pageIds: List<Int>,
    state: WidgetStackState,
    userScrollEnabled: Boolean,
    pageContent: @Composable BoxScope.(appWidgetId: Int) -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageCount = pageIds.size
    val canSwipe = userScrollEnabled && pageCount > 1
    val settledPage = state.settledPage

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .clip(AppShapes.widgetPanelCard)
                .pointerInput(state, pageCount, canSwipe, isRtl) {
                    if (!canSwipe) return@pointerInput
                    // Finger moving towards the start edge shows the next page.
                    val direction = if (isRtl) 1f else -1f
                    val velocityTracker = VelocityTracker()
                    val settle = { velocityPx: Float ->
                        val width = size.width.coerceAtLeast(1).toFloat()
                        val velocity = velocityPx * direction
                        val startPage = state.settledPage
                        val target =
                            when {
                                velocity > WidgetStackFlingVelocity.toPx() -> floor(state.position).toInt() + 1
                                velocity < -WidgetStackFlingVelocity.toPx() -> ceil(state.position).toInt() - 1
                                else -> state.position.roundToInt()
                            }.coerceIn((startPage - 1).coerceAtLeast(0), (startPage + 1).coerceAtMost(pageCount - 1))
                        if (target != startPage) hapticToggle(view)()
                        state.animateToPage(scope, target, initialVelocity = velocity / width)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = {
                            state.stop()
                            velocityTracker.resetTracking()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val width = size.width.coerceAtLeast(1).toFloat()
                            state.dragBy(dragAmount * direction / width, pageCount)
                        },
                        onDragEnd = { settle(velocityTracker.calculateVelocity().x) },
                        onDragCancel = { settle(0f) },
                    )
                },
    ) {
        pageIds.forEachIndexed { index, appWidgetId ->
            // Like a pager's beyond-viewport pages, the settled page's neighbours stay composed so
            // their widgets are already live when swiped in.
            if ((index - settledPage).absoluteValue > 1) return@forEachIndexed
            key(appWidgetId) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val offset = index - state.position
                                translationX = offset * size.width * if (isRtl) -1f else 1f
                                val scale =
                                    lerp(1f, WIDGET_STACK_NEIGHBOUR_SCALE, offset.absoluteValue.coerceIn(0f, 1f))
                                scaleX = scale
                                scaleY = scale
                            },
                ) {
                    pageContent(appWidgetId)
                }
            }
        }
    }

    // Shown while the pages move or when revealed, then faded out once they have rested for a moment.
    var indicatorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        var handledReveals = state.indicatorRevealCount
        snapshotFlow { state.isMoving to state.indicatorRevealCount }
            .collectLatest { (moving, reveals) ->
                if (reveals != handledReveals) {
                    handledReveals = reveals
                    indicatorVisible = true
                }
                if (moving) {
                    indicatorVisible = true
                } else {
                    delay(WIDGET_STACK_INDICATOR_HIDE_DELAY_MS)
                    indicatorVisible = false
                }
            }
    }
    WidgetStackPageIndicator(
        visible = indicatorVisible && pageCount > 1,
        pageCount = pageCount,
        state = state,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = DesignTokens.SpacingSmall),
    )
}

/** Small dots over the stack's bottom edge; the current page's dot stretches into a pill. */
@Composable
private fun WidgetStackPageIndicator(
    visible: Boolean,
    pageCount: Int,
    state: WidgetStackState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) +
                scaleIn(
                    tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing),
                    initialScale = WIDGET_STACK_INDICATOR_HIDDEN_SCALE,
                ),
        exit =
            fadeOut(tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)) +
                scaleOut(
                    tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing),
                    targetScale = WIDGET_STACK_INDICATOR_HIDDEN_SCALE,
                ),
        modifier = modifier,
    ) {
        val currentPage = state.nearestPage.coerceAtMost(pageCount - 1)
        val description = stringResource(R.string.widget_stack_page_indicator, currentPage + 1, pageCount)
        val dotColor = MaterialTheme.colorScheme.inverseOnSurface
        Row(
            modifier =
                Modifier
                    .semantics { contentDescription = description }
                    .background(color = MaterialTheme.colorScheme.inverseSurface, shape = AppShapes.full)
                    .padding(horizontal = WidgetStackIndicatorPadding, vertical = WidgetStackIndicatorPadding),
            horizontalArrangement = Arrangement.spacedBy(WidgetStackIndicatorDotGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val selected = index == currentPage
                val dotWidth by animateDpAsState(
                    targetValue = if (selected) WidgetStackIndicatorActiveWidth else WidgetStackIndicatorDotSize,
                    animationSpec = AppMotion.fastSpatial(),
                    label = "widgetStackDotWidth",
                )
                Box(
                    modifier =
                        Modifier
                            .height(WidgetStackIndicatorDotSize)
                            .width(dotWidth)
                            .background(
                                color = if (selected) dotColor else dotColor.copy(alpha = AppColors.ContentFaintAlpha),
                                shape = AppShapes.full,
                            ),
                )
            }
        }
    }
}

private const val WIDGET_STACK_NEIGHBOUR_SCALE = 0.94f
private const val WIDGET_STACK_SETTLE_THRESHOLD = 0.001f
private const val WIDGET_STACK_INDICATOR_HIDE_DELAY_MS = 2_000L
private const val WIDGET_STACK_INDICATOR_HIDDEN_SCALE = 0.9f
private const val WIDGET_STACK_EDGE_RESISTANCE = 0.3f
private const val WIDGET_STACK_MAX_OVERSCROLL = 0.15f
private val WidgetStackFlingVelocity = 400.dp
private val WidgetStackIndicatorDotSize = 6.dp
private val WidgetStackIndicatorActiveWidth = 14.dp
private val WidgetStackIndicatorDotGap = DesignTokens.SpacingXSmall
private val WidgetStackIndicatorPadding = 5.dp
