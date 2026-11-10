package com.tk.quicksearch.app.navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Root page switcher for Home / Widgets panel / Settings.
 *
 * One [progress] value (0 = [settled] page fully shown, 1 = [target] fully shown) drives every
 * visual: page offsets and the shared backdrop read it in the draw phase, so a drag or settle
 * never recomposes the pages per frame. A finger drag writes [progress] directly; on release a
 * spring continues from the finger's velocity, so a flick back to the origin is one continuous
 * motion. Programmatic navigation (taps, back) runs the same machinery with a tween.
 */
@Stable
class PageSwipeController internal constructor(
    initial: RootDestination,
    private val scope: CoroutineScope,
) {
    var settled by mutableStateOf(initial)
        private set
    var target by mutableStateOf<RootDestination?>(null)
        private set

    /** +1 when [target] enters from the right, -1 from the left. */
    private var direction by mutableStateOf(1)
    private val progressState = mutableFloatStateOf(0f)
    val progress: Float get() = progressState.floatValue

    private var animationJob: Job? = null
    private var headingToTarget = false

    internal var onPrepare: (RootDestination) -> Unit = {}
    internal var onCancelled: (RootDestination) -> Unit = {}
    internal var onSettled: (RootDestination) -> Unit = {}

    /** Pages that must be composed: the settled one plus any page sliding in. */
    internal val composedPages: List<RootDestination>
        get() = listOfNotNull(settled, target).sortedBy { it.ordinal }

    /** Horizontal offset of [page] as a fraction of the width. Read only in draw/layout lambdas. */
    internal fun offsetFraction(page: RootDestination): Float =
        when (page) {
            target -> direction * (1f - progress)
            settled -> -direction * progress
            else -> 0f
        }

    /** How much of [page] is on screen (0..1). Read only in draw/layout lambdas. */
    internal fun visibilityOf(page: RootDestination): Float {
        val t = target
        return when {
            t == null -> if (page == settled) 1f else 0f
            page == t -> progress
            page == settled -> 1f - progress
            else -> 0f
        }
    }

    /** Animates to [destination] unless a gesture already brought or is bringing it there. */
    internal fun navigateTo(
        destination: RootDestination,
        directionOverride: Int? = null,
    ) {
        val inFlight = target
        when {
            inFlight == null && destination == settled -> return
            inFlight == destination && headingToTarget -> return
            inFlight == destination -> {
                settle(toTarget = true, velocity = 0f, spec = navigationTween)
                return
            }
            inFlight != null && destination == settled -> {
                settle(toTarget = false, velocity = 0f, spec = navigationTween)
                return
            }
        }
        // A different page: restart from whichever page is mostly on screen.
        animationJob?.cancel()
        if (inFlight != null) {
            if (progress >= 0.5f) settled = inFlight
            target = null
            progressState.floatValue = 0f
        }
        if (destination == settled) return
        direction = directionOverride ?: if (destination.ordinal > settled.ordinal) 1 else -1
        target = destination
        settle(toTarget = true, velocity = 0f, spec = navigationTween)
    }

    /** Starts a finger-tracked reveal of [page]; [fingerDirection] is +1 for a drag to the right. */
    internal fun beginDrag(
        page: RootDestination,
        fingerDirection: Int,
    ): Boolean {
        if (target != null || page == settled) return false
        animationJob?.cancel()
        direction = -fingerDirection
        target = page
        progressState.floatValue = 0f
        onPrepare(page)
        return true
    }

    internal fun dragTo(fraction: Float) {
        if (target == null) return
        progressState.floatValue = fraction.coerceIn(0f, 1f)
    }

    /**
     * Ends a drag. [velocity] is in progress units per second, positive toward the target. When
     * committing, [onCommitted] runs first so the source page can tidy up while still composed.
     */
    internal fun release(
        commit: Boolean,
        velocity: Float,
        onCommitted: () -> Unit = {},
    ) {
        if (target == null) return
        headingToTarget = commit
        if (commit) onCommitted()
        settle(toTarget = commit, velocity = velocity, spec = releaseSpring)
    }

    private fun settle(
        toTarget: Boolean,
        velocity: Float,
        spec: AnimationSpec<Float>,
    ) {
        animationJob?.cancel()
        headingToTarget = toTarget
        animationJob =
            scope.launch {
                animate(
                    initialValue = progress,
                    targetValue = if (toTarget) 1f else 0f,
                    initialVelocity = velocity,
                    animationSpec = spec,
                ) { value, _ -> progressState.floatValue = value.coerceIn(0f, 1f) }
                val page = target ?: return@launch
                if (toTarget) settled = page
                target = null
                progressState.floatValue = 0f
                if (!toTarget) onCancelled(page)
                onSettled(settled)
            }
    }

    private companion object {
        val navigationTween: AnimationSpec<Float> =
            tween(NAVIGATION_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
        val releaseSpring: AnimationSpec<Float> =
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }
}

@Composable
internal fun rememberPageSwipeController(
    destination: RootDestination,
    settingsDetailType: SettingsDetailType?,
    onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
    viewModel: SearchViewModel,
): PageSwipeController {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { PageSwipeController(destination, scope) }
    val latestDestination by rememberUpdatedState(destination)
    val latestSettingsDetailType by rememberUpdatedState(settingsDetailType)
    val latestOnSettingsDetailTypeChange by rememberUpdatedState(onSettingsDetailTypeChange)
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    controller.onPrepare = { target ->
        // Show the page Settings would open on, so the commit does not switch it mid-slide.
        if (target == RootDestination.Settings && latestSettingsDetailType == null) {
            SettingsNavigationMemory.getLastOpenedSettingsDetail()?.let(latestOnSettingsDetailTypeChange)
        }
    }
    controller.onCancelled = { target ->
        // Revealing Search restores its keyboard; a cancelled drag must not leave it over this page.
        if (target == RootDestination.Search && imeInsets.getBottom(density) > 0) {
            keyboardController?.hide()
            viewModel.setSearchKeyboardRestoreRequest(true)
        }
    }
    // The navigation state stays the source of truth: if a committed swipe did not navigate there,
    // return to the page the state says is current.
    controller.onSettled = { page -> if (page != latestDestination) controller.navigateTo(latestDestination) }
    return controller
}

/** Lays out the composed root pages side by side, offset by the controller's progress. */
@Composable
internal fun PageSwipeHost(
    controller: PageSwipeController,
    modifier: Modifier = Modifier,
    content: @Composable (RootDestination) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        for (page in controller.composedPages) {
            key(page) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = controller.offsetFraction(page) * size.width },
                ) {
                    content(page)
                }
            }
        }
    }
}

/** Null when the host cannot reveal pages live (e.g. overlay), so gestures navigate on release. */
val LocalPageSwipeController = staticCompositionLocalOf<PageSwipeController?> { null }

private val FLING_VELOCITY = 400.dp

/** A fling forward still needs this share of the threshold, so a stray flick does not navigate. */
private const val MIN_FLING_DISTANCE_FRACTION = 0.25f

/**
 * Horizontal swipe detection shared by the root pages. [direction] is +1 for a swipe to the right
 * and -1 for a swipe to the left. When [targetFor] returns a page and a [controller] exists, that
 * page tracks the finger and release settles by velocity: a flick forward commits, a flick back
 * returns, a slow release commits past [thresholdPx]. [onSwipe] runs once a navigation commits.
 * Otherwise [onSwipe] runs on release once the drag passes [thresholdPx].
 */
internal suspend fun PointerInputScope.detectPageSwipe(
    controller: PageSwipeController?,
    thresholdPx: Float,
    targetFor: (direction: Int) -> RootDestination?,
    onSwipe: (direction: Int) -> Unit,
) {
    val velocityTracker = VelocityTracker()
    val flingVelocityPx = FLING_VELOCITY.toPx()
    var total = 0f
    var locked = 0
    var live = false
    try {
        detectHorizontalDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
                total = 0f
                locked = 0
                live = false
            },
            onHorizontalDrag = { change, amount ->
                velocityTracker.addPointerInputChange(change)
                total += amount
                if (locked == 0 && total != 0f) {
                    locked = if (total > 0f) 1 else -1
                    val target = controller?.let { targetFor(locked) }
                    live = target != null && controller.beginDrag(target, locked)
                }
                if (live && size.width > 0) {
                    controller?.dragTo(locked * total / size.width)
                }
            },
            onDragEnd = {
                val distance = locked * total
                if (live && controller != null) {
                    val towardTarget = locked * velocityTracker.calculateVelocity().x
                    val commit =
                        when {
                            towardTarget >= flingVelocityPx ->
                                distance >= thresholdPx * MIN_FLING_DISTANCE_FRACTION
                            towardTarget <= -flingVelocityPx -> false
                            else -> distance >= thresholdPx
                        }
                    val committedDirection = locked
                    val width = size.width.coerceAtLeast(1)
                    controller.release(commit, towardTarget / width) { onSwipe(committedDirection) }
                } else if (locked != 0 && distance >= thresholdPx) {
                    onSwipe(locked)
                }
                total = 0f
                locked = 0
                live = false
            },
            onDragCancel = {
                if (live) controller?.release(commit = false, velocity = 0f)
                total = 0f
                locked = 0
                live = false
            },
        )
    } finally {
        // The pointer input can be cancelled mid-drag (restart/disposal); never leave a page half-revealed.
        if (live) controller?.release(commit = false, velocity = 0f)
    }
}
