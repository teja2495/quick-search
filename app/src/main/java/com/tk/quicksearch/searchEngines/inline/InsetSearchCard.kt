package com.tk.quicksearch.searchEngines.inline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.search.searchScreen.components.backgroundColor
import com.tk.quicksearch.search.searchScreen.components.borderColor
import com.tk.quicksearch.search.searchScreen.components.borderWidth
import com.tk.quicksearch.search.searchScreen.components.cornerRadius
import com.tk.quicksearch.search.searchScreen.insetCardBackdrop
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * How far the full-bleed card reaches past the content padding. The overlay provides its own wider
 * content padding so its card meets the overlay's edges instead of the screen's.
 */
val LocalInsetFullBleedHorizontalExtension =
    staticCompositionLocalOf { InsetSearchBarGeometry.FullBleedHorizontalExtension }

/**
 * Extra content drawn at the bottom of the engine area (the inset card or the full-bleed engine
 * strip), below its engine row, tool action, or AI follow-up input, so it shares that surface's
 * background (the search screen's quick-action pills).
 */
val LocalSearchEngineAreaFooter =
    staticCompositionLocalOf<(@Composable ColumnScope.() -> Unit)?> { null }

/**
 * Geometry shared by the bottom search bar and the card it sits in when compact engines are shown.
 * The bar sits at the top of the card and the engine row (or AI follow-up input) fills the rest
 * below a divider. The card is painted by the content below the bar and reaches up behind it, so the
 * bar's insets and that reach are derived from the same numbers instead of being tuned separately.
 */
object InsetSearchBarGeometry {
    /**
     * Matches `TextFieldDefaults.MinHeight`. Only a starting point: a large font scale grows the
     * bar past it, and a reach that falls short leaves the card's top edge cutting through the bar,
     * so callers pass a measured overlap once the bar has been laid out.
     */
    private val BarHeight = 56.dp

    /** Lets the card sit slightly outside the regular content inset. */
    val ContainerHorizontalExtension = DesignTokens.SpacingXSmall

    /**
     * Cancels the search screen's horizontal content padding so the card reaches the screen edges.
     * Used while the keyboard is open: nothing reserves space for the gesture handle then, so the
     * resting inset would only read as an empty gap around the card.
     */
    val FullBleedHorizontalExtension = DesignTokens.SpacingLarge

    /** Horizontal extension of the card, [fullBleedFraction] of the way from resting to full bleed. */
    @Composable
    fun containerHorizontalExtension(fullBleedFraction: Float): Dp =
        lerp(
            ContainerHorizontalExtension,
            LocalInsetFullBleedHorizontalExtension.current,
            fullBleedFraction,
        )

    /** Pulls the bar in from the card's side edges. */
    val BarHorizontalInset = DesignTokens.SpacingSmall

    /**
     * Bar inset while the card is full bleed. The card grows outward then, so the bar widens a
     * little too rather than leaving a wider margin than the resting card has.
     */
    private val FullBleedBarHorizontalInset = 0.dp

    /** Bar inset [fullBleedFraction] of the way from resting to full bleed. */
    fun barHorizontalInset(fullBleedFraction: Float): Dp =
        lerp(BarHorizontalInset, FullBleedBarHorizontalInset, fullBleedFraction)

    /** Gap between the top of the card and the top of the bar. */
    val BarTopSpacing = DesignTokens.SpacingSmall

    /** Gap between the bottom of the bar and the divider above the engine row. */
    val BarBottomSpacing = DesignTokens.SpacingXSmall

    val ContainerOverlap = overlapFor(BarHeight)

    /** The distance the card has to reach up to cover a bar of [barHeight]. */
    fun overlapFor(barHeight: Dp): Dp =
        BarTopSpacing + barHeight.coerceAtLeast(BarHeight) + BarBottomSpacing
}

/**
 * The user's search bar colors, applied to the whole card instead of the bar inside it. The card's
 * border width and corner radius are fixed (see [SearchBarStyle.hasFixedShape]).
 */
@Immutable
data class InsetCardStyle(
    val cornerRadius: Dp,
    val containerColor: Color,
    val borderWidth: Dp,
    val borderColor: Color,
) {
    /** Squares off the bottom corners as the card goes full bleed against the keyboard. */
    fun shape(fullBleedFraction: Float): RoundedCornerShape {
        if (fullBleedFraction <= 0f) return RoundedCornerShape(cornerRadius)
        val bottomRadius = lerp(cornerRadius, 0.dp, fullBleedFraction)
        return RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius,
        )
    }

    /** The outline stays visible while the card is full bleed against the keyboard too. */
    val border: BorderStroke?
        get() = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null
}

@Composable
internal fun rememberInsetCardStyle(
    searchBarStyle: SearchBarStyle,
    showWallpaperBackground: Boolean,
): InsetCardStyle {
    val cardStyle = searchBarStyle.withFixedShape()
    return InsetCardStyle(
        cornerRadius = cardStyle.cornerRadius(),
        containerColor =
            cardStyle.backgroundColor(AppColors.getSearchBarBackground(showWallpaperBackground)),
        borderWidth = cardStyle.borderWidth(),
        borderColor = cardStyle.borderColor(AppColors.Accent),
    )
}

/**
 * Draws the card wider than its slot and reaching up behind the search bar laid out above it,
 * without changing sibling placement. The bar must be drawn above the card (higher z-index).
 */
internal fun Modifier.attachBelowBottomSearchBar(
    overlap: Dp,
    horizontalExtension: Dp,
): Modifier =
    layout { measurable, constraints ->
        val horizontalExtensionPx = horizontalExtension.roundToPx()
        val attachedWidth = constraints.maxWidth + (horizontalExtensionPx * 2)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = attachedWidth,
                    maxWidth = attachedWidth,
                ),
            )
        val overlapPx = overlap.roundToPx()
        layout(constraints.maxWidth, (placeable.height - overlapPx).coerceAtLeast(0)) {
            placeable.placeRelative(-horizontalExtensionPx, -overlapPx)
        }
    }

/** How far the accent glow spills past the card's edge. */
private val InsetCardGlowExtent = DesignTokens.SpacingMedium

/** Glow opacity right at the card's edge, eased out to nothing at [InsetCardGlowExtent]. */

/** Concentric strokes that approximate the blur; enough that no single ring reads as a line. */
private const val InsetCardGlowSteps = 20

/** Glow opacity [distance] (0 at the edge, 1 at the extent) away from the card. */
private fun insetCardGlowAlphaAt(distance: Float): Float {
    val remaining = 1f - distance
    return AppColors.TintFaintAlpha * remaining * remaining
}

/** Hairline between the search bar and the engine row (or AI follow-up input). */

/**
 * A soft halo in the border color outside [shape] that separates the card (or the standalone search bar) from
 * whatever scrolls beneath it.
 * Stacked strokes centred on the edge fake a blur, and the inside of the shape is clipped out so the
 * translucent card is not tinted.
 */
internal fun Modifier.insetCardGlow(color: Color, shape: Shape): Modifier =
    drawWithCache {
        val outlinePath = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val extentPx = InsetCardGlowExtent.toPx()
        // Each ring covers everything inside its reach, so the rings' opacities sum to the eased
        // falloff: full strength at the edge, tapering so neither end of the glow shows a line.
        val rings =
            (1..InsetCardGlowSteps).map { step ->
                val inner = (step - 1).toFloat() / InsetCardGlowSteps
                val outer = step.toFloat() / InsetCardGlowSteps
                Stroke(width = 2f * extentPx * outer) to
                    (insetCardGlowAlphaAt(inner) - insetCardGlowAlphaAt(outer))
            }
        // Only the hue is taken: the default outline color is a translucent accent, and its alpha
        // would otherwise scale the whole glow down.
        val glowColor = color.copy(alpha = 1f)
        onDrawBehind {
            clipPath(outlinePath, ClipOp.Difference) {
                rings.forEach { (stroke, alpha) ->
                    drawPath(path = outlinePath, color = glowColor, alpha = alpha, style = stroke)
                }
            }
        }
    }

/**
 * The card the bottom search bar sits in: space reserved for the bar on top, then [content].
 */
@Composable
internal fun InsetSearchCard(
    style: InsetCardStyle,
    overlap: Dp,
    fullBleedFraction: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Read here, not by the caller, so the full-bleed animation only recomposes this card.
    val fullBleedFraction = fullBleedFraction()
    val shape = style.shape(fullBleedFraction)
    Surface(
        modifier =
            modifier
                .attachBelowBottomSearchBar(
                    overlap = overlap,
                    horizontalExtension =
                        InsetSearchBarGeometry.containerHorizontalExtension(fullBleedFraction),
                ).insetCardGlow(style.borderColor, shape)
                .insetCardBackdrop(true, shape),
        color = insetCardBackground(true, style.containerColor),
        shape = shape,
        border = style.border,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // The bar is drawn above the card here; keep its slot clear.
            Spacer(modifier = Modifier.height(overlap))
            // Runs edge to edge across the card, like the full-bleed compact engine strip's hairline.
            HorizontalDivider(
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintSubtleAlpha),
            )
            content()
            LocalSearchEngineAreaFooter.current?.invoke(this)
        }
    }
}
