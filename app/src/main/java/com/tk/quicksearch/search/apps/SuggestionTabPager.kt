package com.tk.quicksearch.search.apps

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.AppSuggestionTabType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Finger-tracked paging between app suggestion tabs, like the Home / Widgets panel swipe: the
 * current tab follows the finger while the neighbouring tab slides in beside it, and release
 * settles by velocity. [offset] is the current tab's horizontal offset in px; the [peer] tab sits
 * one width away on [peerSide] (-1 left, +1 right).
 */
@Stable
internal class SuggestionTabPagerState(
    initial: AppSuggestionTabType,
    private val scope: CoroutineScope,
) {
    var displayed by mutableStateOf(initial)
        internal set
    var peer by mutableStateOf<AppSuggestionTabType?>(null)
        private set
    var peerSide by mutableStateOf(0)
        private set
    private val offsetState = mutableFloatStateOf(0f)
    val offset: Float get() = offsetState.floatValue

    /** Last measured page width; read only by gestures and animations. */
    internal var widthPx = 0
    private var settleJob: Job? = null

    /** Persists the current tab; runs once a slide has settled so it never recomposes mid-motion. */
    internal var onSelect: (AppSuggestionTabType) -> Unit = {}
    private var reported = initial

    /** Records [tab] as already persisted (it came from the saved selection). */
    internal fun markReported(tab: AppSuggestionTabType) {
        reported = tab
    }

    internal fun stop() {
        settleJob?.cancel()
        settleJob = null
    }

    /** Moves the current tab by [delta], revealing the neighbour on the side it uncovers. */
    internal fun dragBy(
        delta: Float,
        tabs: List<AppSuggestionTabType>,
    ) {
        val index = tabs.indexOf(displayed)
        if (index < 0) return
        val next = offset + delta
        // Dragging right uncovers the tab on the left.
        val side = if (next > 0f) -1 else if (next < 0f) 1 else 0
        if (side == 0) {
            offsetState.floatValue = 0f
            return
        }
        val existingPeer = peer
        val neighbour =
            if (existingPeer != null && peerSide == side) existingPeer else tabs.getOrNull(index + side)
        if (neighbour == null) {
            offsetState.floatValue = 0f
            peer = null
            return
        }
        peer = neighbour
        peerSide = side
        val width = widthPx.toFloat()
        offsetState.floatValue = if (width > 0f) next.coerceIn(-width, width) else next
    }

    /** Ends a drag. [velocity] is the finger's horizontal velocity in px/s. */
    internal fun release(
        velocity: Float,
        thresholdPx: Float,
        flingVelocityPx: Float,
    ) {
        val target = peer
        val distance = abs(offset)
        // Positive when the finger moves toward the revealed tab.
        val towardPeer = -peerSide * velocity
        val commit =
            target != null &&
                when {
                    towardPeer >= flingVelocityPx -> distance >= thresholdPx * MinFlingDistanceFraction
                    towardPeer <= -flingVelocityPx -> false
                    else -> distance >= thresholdPx
                }
        if (commit) {
            swapTo(target, peerSide)
        }
        settle(velocity)
    }

    /** Slides to [tab] (selected elsewhere, e.g. a tab-strip tap) from the side it lies on. */
    internal fun animateTo(
        tab: AppSuggestionTabType,
        tabs: List<AppSuggestionTabType>,
    ) {
        if (tab == displayed) return
        val from = tabs.indexOf(displayed)
        val to = tabs.indexOf(tab)
        if (from < 0 || to < 0 || widthPx <= 0) {
            stop()
            displayed = tab
            peer = null
            offsetState.floatValue = 0f
            return
        }
        val side = if (to > from) 1 else -1
        // Keep the visible position: if the old peer is the target, continue from where it is.
        if (peer == tab && peerSide == side) {
            swapTo(tab, side)
        } else {
            stop()
            peer = displayed
            peerSide = -side
            displayed = tab
            offsetState.floatValue = side * widthPx.toFloat()
        }
        settle(0f)
    }

    /** Makes the peer current without moving anything on screen. */
    private fun swapTo(
        target: AppSuggestionTabType,
        side: Int,
    ) {
        stop()
        val previous = displayed
        offsetState.floatValue = offset + side * widthPx.toFloat()
        displayed = target
        peer = previous
        peerSide = -side
    }

    private fun settle(velocity: Float) {
        stop()
        if (offset == 0f) {
            peer = null
            reportDisplayed()
            return
        }
        settleJob =
            scope.launch {
                animate(
                    initialValue = offset,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    // Ends at half a pixel instead of creeping on for sub-pixel frames.
                    animationSpec = AppMotion.settle(visibilityThreshold = SettleVisibilityThresholdPx),
                ) { value, _ -> offsetState.floatValue = value }
                peer = null
                reportDisplayed()
            }
    }

    /** Ends a running slide at its destination right away. */
    internal fun finishSettle() {
        if (peer == null && offset == 0f) return
        stop()
        offsetState.floatValue = 0f
        peer = null
        reportDisplayed()
    }

    private fun reportDisplayed() {
        if (displayed == reported) return
        reported = displayed
        onSelect(displayed)
    }

    internal fun snapTo(tab: AppSuggestionTabType) {
        stop()
        displayed = tab
        reported = tab
        peer = null
        offsetState.floatValue = 0f
    }

    private companion object {
        const val MinFlingDistanceFraction = 0.25f
        const val SettleVisibilityThresholdPx = 0.5f
    }
}

@Composable
internal fun rememberSuggestionTabPagerState(
    selected: AppSuggestionTabType?,
    tabs: List<AppSuggestionTabType>,
    onSelect: (AppSuggestionTabType) -> Unit,
): SuggestionTabPagerState? {
    if (selected == null) return null
    val scope = rememberCoroutineScope()
    val state = remember(scope) { SuggestionTabPagerState(selected, scope) }
    state.onSelect = onSelect
    val latestTabs by rememberUpdatedState(tabs)
    LaunchedEffect(selected) {
        state.markReported(selected)
        if (state.displayed !in latestTabs) {
            state.snapTo(selected)
        } else {
            state.animateTo(selected, latestTabs)
        }
    }
    LaunchedEffect(tabs) {
        if (state.displayed !in tabs) state.snapTo(selected)
    }
    return state
}

private val TabFlingVelocity = 400.dp

/**
 * Tracks horizontal drags across the suggestions. A drag toward a side with no tab is left
 * unconsumed so the root page swipe (Widgets panel, Settings, …) takes it with its own live reveal.
 */
internal fun Modifier.suggestionTabSwipe(
    state: SuggestionTabPagerState,
    tabs: List<AppSuggestionTabType>,
): Modifier =
    pointerInput(state, tabs) {
        val flingVelocityPx = TabFlingVelocity.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            var total = Offset.Zero
            var direction = 0
            var slopChange: PointerInputChange? = null
            while (direction == 0) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                total += change.positionChange()
                // Same test as the root page swipe, so on any drag this child decides first.
                when {
                    abs(total.x) > slop -> {
                        direction = if (total.x > 0f) 1 else -1
                        slopChange = change
                    }
                    abs(total.y) > slop -> return@awaitEachGesture
                }
            }
            val index = tabs.indexOf(state.displayed)
            // Dragging right (+1) reveals the previous tab.
            val hasNeighbour = index >= 0 && tabs.getOrNull(index - direction) != null
            if (!hasNeighbour) {
                // Toward a side with no tab: finish any slide still running and leave the drag
                // to the root page swipe, even right after arriving on this tab.
                state.finishSettle()
                return@awaitEachGesture
            }
            // Claim the drag on the event that crossed the slop; otherwise the root page swipe
            // also starts on it and nudges the whole page before cancelling.
            slopChange?.consume()

            state.stop()
            val velocityTracker = VelocityTracker()
            var completed = false
            try {
                horizontalDrag(down.id) { change ->
                    velocityTracker.addPointerInputChange(change)
                    state.dragBy(change.positionChange().x, tabs)
                    change.consume()
                }
                completed = true
                val threshold = state.widthPx * SwipeCommitFraction
                state.release(velocityTracker.calculateVelocity().x, threshold, flingVelocityPx)
            } finally {
                if (!completed) state.release(0f, Float.MAX_VALUE, Float.MAX_VALUE)
            }
        }
    }

private const val SwipeCommitFraction = 0.3f

/**
 * Lays out the current tab and, while one is revealed, its neighbour beside it. Once the home
 * screen has settled every tab stays composed and measured (but unplaced and undrawn when off
 * screen), in a fixed order, so starting or finishing a swipe never composes or moves a grid.
 */
@Composable
internal fun <T> SuggestionTabPager(
    state: SuggestionTabPagerState,
    tabs: List<T>,
    tabType: (T) -> AppSuggestionTabType,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    if (tabs.none { tabType(it) == state.displayed }) return
    var warmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(TabWarmUpDelayMillis)
        warmed = true
    }
    val peerType = state.peer
    Layout(
        modifier = if (peerType != null) modifier.clipToBounds() else modifier,
        content = {
            for (tab in tabs) {
                val type = tabType(tab)
                if (warmed || type == state.displayed || type == peerType) {
                    key(type) {
                        Box(Modifier.layoutId(type)) { content(tab) }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val displayedType = state.displayed
        val revealed = state.peer
        var current: Placeable? = null
        var peer: Placeable? = null
        for (measurable in measurables) {
            val placeable = measurable.measure(constraints)
            when (measurable.layoutId) {
                displayedType -> current = placeable
                revealed -> peer = placeable
            }
        }
        val base = current ?: return@Layout layout(0, 0) {}
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else base.width
        state.widthPx = width
        val fraction = if (peer != null && width > 0) (abs(state.offset) / width).coerceIn(0f, 1f) else 0f
        // The height eases between the two tabs so rows appear or disappear with the swipe.
        val height =
            if (peer != null) {
                (base.height + (peer.height - base.height) * fraction).roundToInt()
            } else {
                base.height
            }
        layout(width, height) {
            base.placeRelativeWithLayer(0, 0) { translationX = state.offset }
            peer?.placeRelativeWithLayer(0, 0) {
                translationX = state.offset + state.peerSide * width
            }
        }
    }
}

/** Keeps composing the other tabs out of the startup path. */
private const val TabWarmUpDelayMillis = 1_000L

/** Tab strip that follows the pager's current tab, so a swipe updates only the strip. */
@Composable
internal fun PagerTabStrip(
    state: SuggestionTabPagerState,
    tabs: List<AppSuggestionTab>,
    onSelectedIndexChange: (Int) -> Unit,
) {
    AppSuggestionTabStrip(
        tabs = tabs,
        selectedIndex = tabs.indexOfFirst { it.type == state.displayed }.coerceAtLeast(0),
        onSelectedIndexChange = onSelectedIndexChange,
    )
}
