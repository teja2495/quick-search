package com.tk.quicksearch.shared.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Shape builders for every component. Components ask here for their shape instead of assembling
 * corners themselves, so the segmented-list, connected-group and press-morph rules live in one
 * place. Values come from the corner tokens in [DesignTokens].
 */
object AppShapes {
    // Every token-based shape in the app comes from here. Call sites never build
    // RoundedCornerShape from tokens; only user-configured or animated radii (widget border
    // radius, search bar style, predictive-back preview) construct shapes directly.

    // Corner scale -------------------------------------------------------------------------

    val none: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerNone)
    val extraSmall: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerExtraSmall)
    val small: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerSmall)
    val medium: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerMedium)
    val large: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerLarge)
    val largeIncreased: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerLargeIncreased)
    val extraLarge: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerExtraLarge)
    val full: RoundedCornerShape = CircleShape

    /** The Material shape scale handed to `MaterialTheme`, so stock components match. */
    val material: Shapes =
        Shapes(
            extraSmall = RoundedCornerShape(DesignTokens.CornerExtraSmall),
            small = RoundedCornerShape(DesignTokens.CornerSmall),
            medium = RoundedCornerShape(DesignTokens.CornerMedium),
            large = RoundedCornerShape(DesignTokens.CornerLarge),
            extraLarge = RoundedCornerShape(DesignTokens.CornerExtraLarge),
        )

    // Roles ---------------------------------------------------------------------------------

    /** Standard card. */
    val card: RoundedCornerShape = medium

    /** A modal drawer meets the bottom edge of the window. */
    val drawerSheet: RoundedCornerShape = stacked(top = DesignTokens.CornerExtraLarge, bottom = DesignTokens.CornerNone)

    /** Search result surfaces: history, suggestions, section cards, engine and tool cards. */
    val resultCard: RoundedCornerShape = extraLarge

    /** Widgets and cards in the widgets panel. */
    val widgetPanelCard: RoundedCornerShape = RoundedCornerShape(DesignTokens.WidgetPanelCornerRadius)

    /** Outline of a whole segmented group (its outer corners). */
    val segmentGroup: RoundedCornerShape = RoundedCornerShape(DesignTokens.SegmentedListOuterCorner)

    /** Small app/shortcut icon tiles clipped inside rows. */
    val iconTile: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerIconTile)

    /** Editable icon frames (custom engine and bookmark icons). */
    val iconFrame: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerIconFrame)

    /** Website favicons. */
    val favicon: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerFavicon)

    /** Favicons drawn at varying sizes; the corner scales with the icon (20% of its size). */
    val faviconScaled: RoundedCornerShape = RoundedCornerShape(percent = 20)

    /** Highlight behind the selected search engine icon. */
    val engineHighlight: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerEngineHighlight)

    /** Single-line inline text inputs (AI follow-up). */
    val inlineInput: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerInlineInput)

    /** Outlined input fields and picker fields. */
    val field: RoundedCornerShape = RoundedCornerShape(DesignTokens.CornerMedium)

    /** Nine-sided cookie for decorative badges (Material 3 Expressive "Cookie 9"). */
    val cookie9: Shape = PolygonShape(AppPolygons.cookie9)

    /** Four-sided cookie for decorative badges (Material 3 Expressive "Cookie 4"). */
    val cookie4: Shape = PolygonShape(AppPolygons.cookie4)

    /** Shape with each corner set separately, for bento layouts. Corners follow reading order. */
    fun corners(
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
    ): RoundedCornerShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

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
