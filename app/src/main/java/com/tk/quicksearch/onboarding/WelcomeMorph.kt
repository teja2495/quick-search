package com.tk.quicksearch.onboarding

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/** Overall pace of the welcome morph: every duration below is stretched by this factor. */
internal const val WelcomeMorphSlowMotion = 1f

private const val PressMs = AppMotion.DurationShort

/** The setup page's background fades this much faster than the rise, so the search content shows early. */
private const val CurtainLead = 1.6f

/** The wallpaper settles from this slight zoom as it is revealed. */
private const val WallpaperStartScale = 1.06f

/** How far above the top exit line pushed content has fully faded. */
private val PushFadeDistance = 140.dp
private val PredictedTopGap = DesignTokens.SpacingSmall

/** The bar's rise from the setup button onto the real search bar, carrying the search content with it. */
private const val TravelMs = AppMotion.DurationExtraLong

/** Share of the rise over which the search content fades in, hiding its first raw frames. */
private const val ContentFadeTravel = 0.3f
private const val HandoffMs = AppMotion.DurationExit
private const val GlowFadeMs = AppMotion.DurationGlowFade

/** How long the landed search bar keeps glowing. Real time: not slowed by [WelcomeMorphSlowMotion]. */
private const val GlowHoldMs = 3000L
private const val PressedScale = 0.95f
private val BarCorner = DesignTokens.Spacing28

/** Real-time cap on waiting for the search bar's first layout before the rise starts anyway. */
private const val TargetWaitMs = 300L

/** How long the setup page must stay on screen, above the search screen, while everything rises. */
internal val WelcomeExitHoldMs: Int get() = (TargetWaitMs + scaled(TravelMs)).toInt()

/** Material 3 emphasized easing: a quick launch that settles gently, for the bar's long glide. */
private val TravelEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun scaled(ms: Int): Int = (ms * WelcomeMorphSlowMotion).toInt()

private fun lerpRect(
    a: Rect,
    b: Rect,
    t: Float,
): Rect = Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t))

enum class WelcomeMorphPhase { Idle, Exiting, Landing, Glowing }

/**
 * First-run welcome, in four beats:
 * 1. The glowing button that finishes setup ([welcomeGlow]) dips under the tap.
 * 2. At once the page switches to the search screen, which sits under the setup page until the end.
 * 3. In one motion the button rises onto the real search bar ([welcomeMorphTarget]), turning into
 *    one on the way; the setup content slides up off the screen ([welcomePagePush]), each element
 *    fading as it nears the top ([welcomeExitItem]); the page's background ([welcomeBackgroundFade])
 *    fades to the wallpaper underneath; and the apps and sections ride up under the bar
 *    ([welcomeContentReveal]).
 * The keyboard waits for the landing ([welcomeMorphHoldsKeyboard]). [WelcomeMorphOverlay] drives
 * the sequence and draws the travelling bar and its glow.
 */
@Stable
class WelcomeMorphState {
    var phase by mutableStateOf(WelcomeMorphPhase.Idle)
        internal set
    val isActive: Boolean get() = phase != WelcomeMorphPhase.Idle

    internal var origin: Rect? = null

    /** Where the bar heads before the search screen exists. */
    internal var predicted: Rect? = null

    /** Just under the status bar: setup content fades out as it slides up past this line. */
    internal var exitLine: Float? = null

    /** The search bar's bounds when the last welcome finished, so a replay aims exactly. */
    internal var landedTarget: Rect? = null
    internal var originLabel = ""
    internal var originColor = Color.Unspecified
    internal var originContentColor = Color.Unspecified
    internal var originTextStyle = TextStyle.Default
    internal var navigate: (() -> Unit)? = null

    internal var target by mutableStateOf<Rect?>(null)
    internal var targetCornerPx = 0f
    internal var targetColor = Color.Unspecified

    internal var press by mutableFloatStateOf(1f)
    internal var curtain by mutableFloatStateOf(0f)
    internal var push by mutableFloatStateOf(0f)
    internal var travel by mutableFloatStateOf(0f)

    /** The search content's layout, inside and outside its rise, to find the bar without that shift. */
    internal var contentInner: LayoutCoordinates? = null
    internal var contentOuterTopLeft: Offset? = null
    internal var handoff by mutableFloatStateOf(0f)
    internal var glow by mutableFloatStateOf(1f)

    fun start(
        bounds: Rect,
        label: String,
        containerColor: Color,
        contentColor: Color,
        textStyle: TextStyle,
        onNavigate: () -> Unit,
    ) {
        if (isActive) return
        origin = bounds
        originLabel = label
        originColor = containerColor
        originContentColor = contentColor
        originTextStyle = textStyle
        navigate = onNavigate
        resetProgress()
        phase = WelcomeMorphPhase.Exiting
    }

    internal fun finishLanding() {
        if (target != null) landedTarget = target
        curtain = 0f
        phase = WelcomeMorphPhase.Glowing
    }

    /** The bar's bounds in window coordinates right now. */
    internal fun barRect(): Rect? {
        val origin = origin ?: return null
        return lerpRect(origin, target ?: predicted ?: origin, travel)
    }

    /** How far the search content sits below its resting place, riding just under the bar. */
    internal fun contentShift(): Float {
        if (phase != WelcomeMorphPhase.Landing) return 0f
        val origin = origin ?: return 0f
        val target = target ?: return 0f
        return (origin.top - target.top) * (1f - travel)
    }

    /**
     * How far the setup page has slid up, in px (zero or negative). It always leaves through the
     * top, whichever way the bar travels: far enough that everything above the button is gone.
     */
    internal fun pushShift(): Float {
        if (phase != WelcomeMorphPhase.Landing) return 0f
        val origin = origin ?: return 0f
        return -origin.bottom * push
    }

    private fun resetProgress() {
        press = 1f
        curtain = 0f
        push = 0f
        travel = 0f
        handoff = 0f
        glow = 1f
    }
}

/** Available everywhere the search bar may report itself. */
val LocalWelcomeMorph = staticCompositionLocalOf<WelcomeMorphState?> { null }

/** Provided only on onboarding pages, so shared rows elsewhere skip the exit bookkeeping. */
val LocalWelcomeExit = staticCompositionLocalOf<WelcomeMorphState?> { null }

/** Moves an onboarding page's content (not its background) up with the bar, as if pushed off screen. */
fun Modifier.welcomePagePush(): Modifier =
    composed {
        val state = LocalWelcomeExit.current ?: return@composed Modifier
        val pushed = remember { BooleanArray(1) }
        Modifier.graphicsLayer {
            translationY = state.pushShift()
            alpha = if (pushedAway(state, pushed)) 0f else 1f
        }
    }

/**
 * True once this page was pushed and the welcome has moved on (finished or skipped), so a page
 * still held on screen during its exit never pops back over the search screen.
 */
private fun pushedAway(
    state: WelcomeMorphState,
    pushed: BooleanArray,
): Boolean {
    if (state.phase == WelcomeMorphPhase.Landing) pushed[0] = true
    return pushed[0] && state.phase != WelcomeMorphPhase.Landing
}

/** Fades an onboarding page's own background out while the search wallpaper fades in behind it. */
fun Modifier.welcomeBackgroundFade(): Modifier =
    composed {
        val state = LocalWelcomeExit.current ?: return@composed Modifier
        val pushed = remember { BooleanArray(1) }
        Modifier.graphicsLayer {
            // Per draw, not offscreen: this layer covers the whole screen for the entire fade.
            compositingStrategy = CompositingStrategy.ModulateAlpha
            alpha = if (pushedAway(state, pushed)) 0f else 1f - state.curtain
        }
    }

/** Fades this element out as the push carries it up to the bar's landing line. */
fun Modifier.welcomeExitItem(): Modifier =
    composed {
        val state = LocalWelcomeExit.current ?: return@composed Modifier
        val fadePx = with(LocalDensity.current) { PushFadeDistance.toPx() }
        var restBottom by remember { mutableFloatStateOf(Float.NaN) }
        Modifier
            .onGloballyPositioned {
                // Stored without the push, whether or not this callback sees the page's translation.
                restBottom = it.positionInWindow().y + it.size.height - state.pushShift()
            }.graphicsLayer {
                val shift = state.pushShift()
                val line = state.exitLine
                alpha =
                    if (shift >= 0f || line == null || restBottom.isNaN()) {
                        1f
                    } else {
                        ((restBottom + shift - line) / fadePx).coerceIn(0f, 1f)
                    }
            }
    }

/** True while the search screen is built ahead of time under the setup pages, not yet shown. */
val LocalSearchSurfaceHidden = compositionLocalOf { false }

/** True while the search screen is still hidden and its field should not raise the keyboard yet. */
@Composable
fun welcomeMorphHoldsKeyboard(): Boolean {
    if (LocalSearchSurfaceHidden.current) return true
    // Released on the tap, so the keyboard rises together with the bar and the content.
    return LocalWelcomeMorph.current?.phase == WelcomeMorphPhase.Exiting
}

/** True while the welcome animates, when the keyboard is on its way and needs no "Open Keyboard" pill. */
@Composable
fun welcomeMorphRunning(): Boolean {
    val phase = LocalWelcomeMorph.current?.phase
    return phase == WelcomeMorphPhase.Exiting || phase == WelcomeMorphPhase.Landing
}

/** Carries the search screen's content (not its backdrop) up with the travelling bar as it lands. */
fun Modifier.welcomeContentReveal(): Modifier =
    composed {
        val state = LocalWelcomeMorph.current ?: return@composed Modifier
        Modifier
            .onGloballyPositioned { state.contentOuterTopLeft = it.positionInWindow() }
            .graphicsLayer {
                translationY = state.contentShift()
                alpha =
                    if (state.phase == WelcomeMorphPhase.Landing) {
                        AppMotion.EmphasizedEasing.transform((state.travel / ContentFadeTravel).coerceIn(0f, 1f))
                    } else {
                        1f
                    }
            }.onGloballyPositioned { state.contentInner = it }
    }

/** Reports the search bar as the landing spot and keeps it hidden until the travelling bar hands off. */
fun Modifier.welcomeMorphTarget(
    cornerRadius: Dp,
    containerColor: Color,
): Modifier =
    composed {
        val state = LocalWelcomeMorph.current ?: return@composed Modifier
        val cornerPx = with(LocalDensity.current) { cornerRadius.toPx() }
        // Tracked all along, so a search screen built ahead of time is aimed at from the first frame.
        DisposableEffect(state) { onDispose { state.target = null } }
        Modifier
            .onGloballyPositioned {
                // Measured against the content's unshifted place, so its rise never moves the aim.
                val inner = state.contentInner?.takeIf { c -> c.isAttached }
                val outer = state.contentOuterTopLeft
                state.target =
                    if (inner != null && outer != null) {
                        inner.localBoundingBoxOf(it, clipBounds = false).translate(outer)
                    } else {
                        it.boundsInWindow()
                    }
                state.targetCornerPx = cornerPx
                state.targetColor = containerColor
            }.graphicsLayer {
                alpha = if (state.phase == WelcomeMorphPhase.Landing && state.travel < 1f) 0f else 1f
            }
    }

/**
 * The search screen's wallpaper settles from a slight zoom while the welcome fades it in under the
 * setup page. Apply to the backdrop the search surface already draws, not to a second copy.
 */
fun Modifier.welcomeBackdropZoom(): Modifier =
    composed {
        val state = LocalWelcomeMorph.current ?: return@composed Modifier
        Modifier.graphicsLayer {
            val scale = if (state.phase == WelcomeMorphPhase.Landing) lerp(WallpaperStartScale, 1f, state.curtain) else 1f
            scaleX = scale
            scaleY = scale
        }
    }

/**
 * Drives the sequence. [onSearchScreen] is true once the search screen is the current page; taps
 * while the bar is landing jump to the end.
 */
@Composable
fun WelcomeMorphOverlay(
    state: WelcomeMorphState,
    onSearchScreen: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val phase = state.phase
    val density = LocalDensity.current
    val safeTopPx = WindowInsets.safeDrawing.getTop(density)
    val predictedGapPx = with(density) { PredictedTopGap.toPx() }

    LaunchedEffect(phase) {
        if (phase != WelcomeMorphPhase.Exiting) return@LaunchedEffect
        val origin = state.origin
        state.exitLine = safeTopPx + predictedGapPx
        state.predicted =
            state.landedTarget ?: origin?.let {
                val top = safeTopPx + predictedGapPx
                Rect(it.left, top, it.right, top + it.height)
            }
        // Straight to the search screen: it composes under the setup page while everything rises.
        state.phase = WelcomeMorphPhase.Landing
        state.navigate?.invoke()
        state.navigate = null
    }

    val landing = phase == WelcomeMorphPhase.Landing && onSearchScreen
    LaunchedEffect(landing) {
        if (!landing) return@LaunchedEffect
        coroutineScope {
            launch {
                animate(1f, PressedScale, animationSpec = tween(scaled(PressMs) / 2, easing = AppMotion.EmphasizedEasing)) { v, _ -> state.press = v }
                animate(PressedScale, 1f, animationSpec = tween(scaled(PressMs) / 2, easing = AppMotion.EmphasizedEasing)) { v, _ -> state.press = v }
            }
            // The search bar reports itself on the search screen's first layout.
            withTimeoutOrNull(TargetWaitMs) { snapshotFlow { state.target }.first { it != null } }
            state.target?.let { state.predicted = it }
            animate(0f, 1f, animationSpec = tween(scaled(TravelMs), easing = TravelEasing)) { v, _ ->
                state.travel = v
                state.push = v
                state.curtain = (v * CurtainLead).coerceAtMost(1f)
            }
        }
        // The real bar is visible from here; fade the travelling copy off it.
        animate(0f, 1f, animationSpec = tween(scaled(HandoffMs), easing = AppMotion.EmphasizedEasing)) { v, _ -> state.handoff = v }
        state.finishLanding()
        currentOnFinished()
    }

    LaunchedEffect(phase) {
        if (phase != WelcomeMorphPhase.Glowing) return@LaunchedEffect
        delay(GlowHoldMs)
        animate(state.glow, 0f, animationSpec = tween(scaled(GlowFadeMs), easing = AppMotion.EmphasizedEasing)) { v, _ -> state.glow = v }
        state.phase = WelcomeMorphPhase.Idle
    }

    when (phase) {
        WelcomeMorphPhase.Idle -> Unit
        WelcomeMorphPhase.Glowing -> LandedGlow(state, modifier)
        else ->
            Box(
                modifier =
                    modifier.fillMaxSize().pointerInput(landing) {
                        detectTapGestures {
                            if (landing) {
                                state.finishLanding()
                                currentOnFinished()
                            }
                        }
                    },
            ) {
                TravellingSearchBar(state = state)
            }
    }
}

/** Gradient border over the landed search bar; it never takes touches. */
@Composable
private fun LandedGlow(
    state: WelcomeMorphState,
    modifier: Modifier = Modifier,
) {
    val tick = rememberGlowFrameTick()
    val glowPainter = remember { WelcomeGlowPainter() }
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { overlayOffset = it.positionInWindow() }
                .drawBehind {
                    tick()
                    val target = state.target ?: return@drawBehind
                    val rect = target.translate(-overlayOffset)
                    drawWelcomeGlow(glowPainter, rect.topLeft, rect.size, state.targetCornerPx.coerceAtMost(rect.height / 2f), state.glow)
                },
    )
}

@Composable
private fun TravellingSearchBar(
    state: WelcomeMorphState,
    modifier: Modifier = Modifier,
) {
    val barColor = AppColors.getSearchBarBackground(false)
    val barContentColor = AppColors.getSearchBarTextAndIconColor(false)
    val barIconColor = AppColors.getSearchBarSecondaryIconTint(false)
    val barCornerPx = with(LocalDensity.current) { BarCorner.toPx() }
    val hintSlidePx = with(LocalDensity.current) { 12.dp.toPx() }
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    val tick = rememberGlowFrameTick()
    val glowPainter = remember { WelcomeGlowPainter() }

    fun currentRect(): Rect? = state.barRect()?.translate(-overlayOffset)

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { overlayOffset = it.positionInWindow() }) {
        Box(
            modifier =
                Modifier
                    .layout { measurable, _ ->
                        val rect = currentRect() ?: Rect.Zero
                        val w = rect.width.roundToInt().coerceAtLeast(0)
                        val h = rect.height.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(Constraints.fixed(w, h))
                        layout(w, h) { placeable.place(rect.left.roundToInt(), rect.top.roundToInt()) }
                    }.graphicsLayer {
                        scaleX = state.press
                        scaleY = state.press
                    }.drawBehind {
                        val morphColor = lerp(state.originColor, barColor, state.travel)
                        val endColor = state.targetColor.takeIf { it.isSpecified && it.alpha >= 0.5f } ?: barColor
                        val travelT = state.travel.coerceIn(0f, 1f)
                        val corner =
                            lerp(lerp(size.height / 2f, barCornerPx, state.travel), state.targetCornerPx, travelT)
                        tick()
                        val clampedCorner = corner.coerceAtMost(size.height / 2f)
                        // The fill hands off to the real bar; the glow stays until LandedGlow takes over.
                        drawRoundRect(
                            color = lerp(morphColor, endColor, travelT),
                            cornerRadius = CornerRadius(clampedCorner),
                            alpha = 1f - state.handoff,
                        )
                        drawWelcomeGlow(glowPainter, Offset.Zero, size, clampedCorner, alpha = 1f)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.originLabel,
                style = state.originTextStyle,
                color = state.originContentColor,
                maxLines = 1,
                modifier =
                    Modifier
                        .measuredAtWidth(centered = true) { state.origin?.width ?: 0f }
                        .graphicsLayer { alpha = (1f - state.travel * 2.5f).coerceIn(0f, 1f) },
            )
            val hintSlideSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .measuredAtWidth(centered = false) { (state.target ?: state.predicted ?: state.origin)?.width ?: 0f }
                        .padding(horizontal = DesignTokens.SpacingLarge)
                        .graphicsLayer {
                            val a = ((state.travel - 0.4f) / 0.6f).coerceIn(0f, 1f)
                            alpha = a * (1f - state.handoff)
                            translationX = -hintSlidePx * (1f - a) * hintSlideSign
                        },
            ) {
                Icon(
                    imageVector = MaterialSymbols.Search,
                    contentDescription = null,
                    tint = barIconColor,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingMedium))
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = barContentColor.copy(alpha = AppColors.ContentHighAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Measures the content at a steady width instead of the bar's changing one, so its text is laid
 * out once rather than on every frame of the rise. Wider content overflows the bar, centred or
 * from the start.
 */
private fun Modifier.measuredAtWidth(
    centered: Boolean,
    widthPx: () -> Float,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints(maxWidth = widthPx().roundToInt().coerceAtLeast(0)))
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        layout(width, height) {
            placeable.place(if (centered) (width - placeable.width) / 2 else 0, (height - placeable.height) / 2)
        }
    }
