package com.tk.quicksearch.shared.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Shape builders for every component. Components ask here for their shape instead of assembling
 * corners themselves, so the segmented-list, connected-group and press-morph rules live in one
 * place. Values come from the corner tokens in [DesignTokens].
 */
object AppShapes {
    /** The Material shape scale handed to `MaterialTheme`, built from the corner tokens. */
    val material: Shapes =
        Shapes(
            extraSmall = RoundedCornerShape(DesignTokens.CornerExtraSmall),
            small = RoundedCornerShape(DesignTokens.CornerSmall),
            medium = RoundedCornerShape(DesignTokens.CornerMedium),
            large = RoundedCornerShape(DesignTokens.CornerLarge),
            extraLarge = RoundedCornerShape(DesignTokens.CornerExtraLarge),
        )

    /** Nine-lobed scalloped shape for decorative badges (Material 3 Expressive "Cookie 9"). */
    val cookie9: Shape = CookieShape(lobes = 9)

    /** Four-leaf clover shape for decorative badges (Material 3 Expressive "Clover 4"). */
    val clover4: Shape = CookieShape(lobes = 4, depth = 0.22f)

    /** A shape with [top] corners above and [bottom] corners below: a tile in a vertical stack. */
    fun stacked(top: Dp, bottom: Dp): RoundedCornerShape =
        RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)

    /**
     * A shape with [start] corners at the leading edge and [end] corners at the trailing edge: a
     * button in a horizontal group. Follows reading order, so round ends stay at the edges in RTL.
     */
    fun connected(start: Dp, end: Dp): RoundedCornerShape =
        RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)

    /**
     * Tile at [index] of [count] in a segmented list: [outer] corners at the ends of the list,
     * [inner] corners between tiles.
     */
    fun segment(
        index: Int,
        count: Int,
        inner: Dp = DesignTokens.SegmentedListInnerCorner,
        outer: Dp = DesignTokens.SegmentedListOuterCorner,
    ): RoundedCornerShape = segment(first = index <= 0, last = index >= count - 1, inner = inner, outer = outer)

    /** Segmented-list tile that is the [first] and/or [last] of its list. */
    fun segment(
        first: Boolean,
        last: Boolean,
        inner: Dp = DesignTokens.SegmentedListInnerCorner,
        outer: Dp = DesignTokens.SegmentedListOuterCorner,
    ): RoundedCornerShape = stacked(top = if (first) outer else inner, bottom = if (last) outer else inner)
}

/**
 * Corner radius that moves from [rest] to [pressed] while [interactionSource] is pressed (or while
 * [held]), on the fast spatial spring: the Material 3 Expressive shape morph.
 */
@Composable
fun animatePressedCorner(
    interactionSource: InteractionSource,
    rest: Dp,
    pressed: Dp,
    held: Boolean = false,
): Dp {
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed || held) pressed else rest,
        animationSpec = AppMotion.fastSpatial(),
        label = "pressedCorner",
    )
    return corner
}

/** Uniform shape that morphs from [rest] to [pressed] corners while pressed. */
@Composable
fun pressMorphShape(
    interactionSource: InteractionSource,
    rest: Dp,
    pressed: Dp,
    held: Boolean = false,
): RoundedCornerShape = RoundedCornerShape(animatePressedCorner(interactionSource, rest, pressed, held))

/**
 * Fully round button of [height] that squares to [pressed] corners while pressed, as Material 3
 * Expressive buttons do.
 */
@Composable
fun pressMorphButtonShape(
    interactionSource: InteractionSource,
    height: Dp,
    pressed: Dp = DesignTokens.ButtonPressedCornerMedium,
): RoundedCornerShape = pressMorphShape(interactionSource, rest = height / 2, pressed = pressed)

/**
 * Floating action (bottom bars, onboarding, save, collapse): fully round at rest, squaring while
 * pressed.
 */
@Composable
fun floatingActionShape(interactionSource: InteractionSource): RoundedCornerShape =
    pressMorphButtonShape(interactionSource, DesignTokens.FloatingActionHeight)

/** Segmented-list tile at [index] of [count] whose inner corners round further while pressed. */
@Composable
fun pressMorphSegmentShape(
    interactionSource: InteractionSource,
    index: Int,
    count: Int,
): RoundedCornerShape {
    val inner =
        animatePressedCorner(
            interactionSource,
            rest = DesignTokens.SegmentedListInnerCorner,
            pressed = DesignTokens.SegmentPressedInnerCorner,
        )
    return AppShapes.segment(index, count, inner = inner)
}

/**
 * Button of a Material 3 Expressive connected button group. [isStart]/[isEnd] buttons keep
 * [outer] corners on the group's edges; corners between buttons are small and tighten while
 * pressed. [pressedOuter] lets the outer corners square too; a [selected] button morphs fully
 * round to [outer].
 */
@Composable
fun connectedButtonShape(
    interactionSource: InteractionSource,
    isStart: Boolean,
    isEnd: Boolean,
    outer: Dp,
    pressedOuter: Dp = outer,
    selected: Boolean = false,
): RoundedCornerShape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val spec = AppMotion.fastSpatial<Dp>()
    val outerCorner by animateDpAsState(
        targetValue = if (isPressed) pressedOuter else outer,
        animationSpec = spec,
        label = "connectedOuterCorner",
    )
    val innerCorner by animateDpAsState(
        targetValue =
            when {
                selected -> outer
                isPressed -> DesignTokens.ConnectedGroupPressedInnerCorner
                else -> DesignTokens.ConnectedGroupInnerCorner
            },
        animationSpec = spec,
        label = "connectedInnerCorner",
    )
    return AppShapes.connected(
        start = if (isStart) outerCorner else innerCorner,
        end = if (isEnd) outerCorner else innerCorner,
    )
}
