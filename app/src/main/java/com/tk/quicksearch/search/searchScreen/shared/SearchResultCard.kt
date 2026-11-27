package com.tk.quicksearch.search.searchScreen.shared

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.ResultCardStyle
import androidx.compose.ui.unit.offset
import kotlin.math.roundToInt
import com.tk.quicksearch.search.searchScreen.LocalFrostedCardSource
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.FrostedResultCardScrimFactor
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

/**
 * False when the user turned on Compact Layout: result and home cards drop their container
 * background and border so content sits directly on the screen background.
 */
internal val LocalSearchCardLayoutEnabled = compositionLocalOf { true }

/**
 * Vertical spacing between sibling cards in the enclosing list. Flat cards (compact layout) overlap
 * each neighbour by half of it, so their bottom dividers sit exactly between two cards' content.
 */
internal val LocalCardlessStackSpacing = compositionLocalOf { DesignTokens.SpacingMedium }

/** The user's corner radius and transparency for result and home cards (Appearance → Cards). */
internal val LocalResultCardStyle = compositionLocalOf { ResultCardStyle() }

/** Outer corner radius of result cards and segmented result groups. */
@Composable
internal fun resultCardOuterCorner(): Dp = LocalResultCardStyle.current.cornerRadiusDp.dp

/** Corner between segmented result tiles: the stock inner corner, never rounder than the outer one. */
@Composable
internal fun resultCardInnerCorner(): Dp = minOf(DesignTokens.SegmentedListInnerCorner, resultCardOuterCorner())

@Immutable
data class SearchResultCardStyleOverrides(
    val shape: Shape? = null,
    val border: BorderStroke? = null,
    val colors: CardColors? = null,
    val containerColor: Color? = null,
    val showBorder: Boolean = true,
)

/**
 * Centralized search-result card visuals for the search surface.
 * Tune shape, border and container colors here; individual cards can tweak via
 * [SearchResultCardStyleOverrides].
 */
object SearchResultCardDefaults {
    /**
     * Base corner shape used by result/history/web/direct-search/search-on cards and their row
     * ripples. Tighter when the card layout is off, where it only shapes press feedback.
     */
    val shape: Shape
        @Composable
        get() =
            if (LocalSearchCardLayoutEnabled.current) {
                val style = LocalResultCardStyle.current
                if (style.cornerRadiusDp == ResultCardStyle.DEFAULT_CORNER_RADIUS_DP) {
                    AppShapes.resultCard
                } else {
                    RoundedCornerShape(style.cornerRadiusDp.dp)
                }
            } else {
                AppShapes.medium
            }

    /** Subtle border shown when wallpaper/custom image is active in dark mode. */
    private val wallpaperDarkBorder =
        BorderStroke(DesignTokens.BorderWidth, AppColors.WallpaperDarkCardBorder)

    @Composable
    fun border(showWallpaperBackground: Boolean): BorderStroke? {
        val isDarkTheme = LocalAppIsDarkTheme.current
        return if (showWallpaperBackground && isDarkTheme) wallpaperDarkBorder else null
    }

    @Composable
    fun colors(
        showWallpaperBackground: Boolean,
        overlayContainerColor: Color?,
        containerColorOverride: Color? = null,
    ): CardColors =
        CardDefaults.cardColors(
            containerColor =
                containerColor(
                    showWallpaperBackground = showWallpaperBackground,
                    overlayContainerColor = containerColorOverride ?: overlayContainerColor,
                ),
        )

    /** Resolved container color for result cards, segment tiles and other home/search cards. */
    @Composable
    fun containerColor(
        showWallpaperBackground: Boolean,
        overlayContainerColor: Color? = LocalOverlayResultCardColor.current,
    ): Color {
        val base =
            AppColors.getSearchResultCardContainerColor(
                showWallpaperBackground = showWallpaperBackground,
                overlayContainerColor = overlayContainerColor,
            )
        val style = LocalResultCardStyle.current
        val resolved =
            if (!showWallpaperBackground || overlayContainerColor != null || !style.blurEnabled) {
                base
            } else {
                readableFrostedCardScrim(base)
            }
        val transparency = style.transparency
        return if (transparency <= 0f) resolved else resolved.copy(alpha = resolved.alpha * (1f - transparency))
    }

    /**
     * Hairline color for dividers between result rows, sections and flat (cardless) cards:
     * overlay theme tint first, then the wallpaper divider, then Material outlineVariant.
     */
    @Composable
    fun dividerColor(
        showWallpaperBackground: Boolean,
        overlayDividerColor: Color? = LocalOverlayDividerColor.current,
    ): Color =
        overlayDividerColor
            ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant
}

/**
 * [base] lightened to the frosted-card scrim while a frosted wallpaper is drawn behind it, raised
 * only as far as card text needs to stay readable. Shared by result cards and the search chrome.
 */
@Composable
internal fun readableFrostedCardScrim(base: Color): Color {
    val frost = LocalFrostedCardSource.current
    // A transparent surface (the bar inside the compact-engine card) stays transparent.
    if (frost?.isActive != true || base.alpha <= 0f) return base
    val tint = frost.tint
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant
    val isDarkTheme = LocalAppIsDarkTheme.current
    return remember(base, tint, foreground, isDarkTheme) {
        val worstWallpaper = tint.compositeOver(if (isDarkTheme) Color.White else Color.Black)
        readableFrostedScrim(base, foreground, worstWallpaper, isDarkTheme)
    }
}

/** Keeps the lightest usable scrim even over a fully white or black custom image. */
private fun readableFrostedScrim(
    scrim: Color,
    foreground: Color,
    wallpaper: Color,
    isDarkTheme: Boolean,
): Color {
    val foregroundLuminance = foreground.luminance() + 0.05f
    fun hasContrast(alpha: Float): Boolean {
        val backgroundLuminance = scrim.copy(alpha = alpha).compositeOver(wallpaper).luminance() + 0.05f
        val contrast = if (isDarkTheme) foregroundLuminance / backgroundLuminance else backgroundLuminance / foregroundLuminance
        return contrast >= 4.5f
    }

    var low = scrim.alpha * FrostedResultCardScrimFactor
    if (hasContrast(low)) return scrim.copy(alpha = low)
    var high = 1f
    repeat(8) {
        val mid = (low + high) / 2f
        if (hasContrast(mid)) high = mid else low = mid
    }
    return scrim.copy(alpha = high)
}

/**
 * Search-screen card wrapper.
 * Used only on the search result surface: sections, suggestions, engine cards, AI search, etc.
 * Styling is centralized via [SearchResultCardDefaults].
 */
@Composable
fun SearchResultCard(
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean,
    overlayContainerColor: Color? = LocalOverlayResultCardColor.current,
    styleOverrides: SearchResultCardStyleOverrides = SearchResultCardStyleOverrides(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardLayoutEnabled = LocalSearchCardLayoutEnabled.current
    val shape = styleOverrides.shape ?: SearchResultCardDefaults.shape
    val border =
        if (cardLayoutEnabled && styleOverrides.showBorder) {
            styleOverrides.border ?: SearchResultCardDefaults.border(showWallpaperBackground)
        } else {
            null
        }
    val colors =
        if (cardLayoutEnabled) {
            styleOverrides.colors ?: SearchResultCardDefaults.colors(
                showWallpaperBackground = showWallpaperBackground,
                overlayContainerColor = overlayContainerColor,
                containerColorOverride = styleOverrides.containerColor,
            )
        } else {
            CardDefaults.cardColors(containerColor = Color.Transparent)
        }

    val cardlessVerticalBleed = LocalCardlessStackSpacing.current / 2
    if (cardLayoutEnabled) {
        Card(
            modifier = modifier.frostedCardBackground(shape),
            colors = colors,
            shape = shape,
            border = border,
            content = content,
        )
        return
    }
    // A flat card is a plain column spanning the screen width, so nothing on it (animateContentSize,
    // the predicted highlight) clips its row dividers short of the edges. Its content is padded back
    // to the usual flat-card bleed.
    val cardlessDividerColor = SearchResultCardDefaults.dividerColor(showWallpaperBackground)
    val slotEdges = remember { EdgeDistances() }
    val rowBleed = DesignTokens.SpacingSmall
    Column(
        modifier =
            Modifier.trackEdgeDistances(slotEdges)
                .cardlessBleed(slotEdges, rowBleed, cardlessVerticalBleed)
                .cardlessSectionDivider(cardlessDividerColor)
                .then(modifier)
                .cardlessContentInset(slotEdges, rowBleed),
        content = content,
    )
}

/** Root-space distances from a node's left and right edges to the edges of the window. */
@Stable
internal class EdgeDistances {
    var left by mutableFloatStateOf(Float.NaN)
    var right by mutableFloatStateOf(Float.NaN)
}

/** Distance to the left screen edge in px, or [fallback] before the first placement. */
internal fun EdgeDistances.leftOr(fallback: Int): Int = if (left.isNaN()) fallback else left.roundToInt().coerceAtLeast(0)

internal fun EdgeDistances.rightOr(fallback: Int): Int = if (right.isNaN()) fallback else right.roundToInt().coerceAtLeast(0)

internal fun Modifier.trackEdgeDistances(distances: EdgeDistances): Modifier =
    onGloballyPositioned { coordinates ->
        val left = coordinates.positionInRoot().x
        distances.left = left
        distances.right = coordinates.findRootCoordinates().size.width - left - coordinates.size.width
    }

/**
 * With the card layout off, stretches this node (a divider) to the screen edges, past the padding
 * of the rows around it. The results column bleeds to the edges, so nothing clips it there.
 */
@Composable
internal fun Modifier.compactEdgeToEdge(): Modifier {
    if (LocalSearchCardLayoutEnabled.current) return this
    val distances = remember { EdgeDistances() }
    return trackEdgeDistances(distances).layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val left = distances.leftOr(0)
        val right = distances.rightOr(0)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = constraints.maxWidth + left + right,
                    maxWidth = constraints.maxWidth + left + right,
                ),
            )
        layout(constraints.maxWidth, placeable.height) { placeable.place(-left, 0) }
    }
}

/**
 * Extends a flat (card layout off) card from its slot out to the screen edges ([slotEdges]; [rowBleed]
 * until they are known), and overlaps neighbours by [vertical] so adjacent flat cards meet edge to
 * edge.
 */
private fun Modifier.cardlessBleed(slotEdges: EdgeDistances, rowBleed: Dp, vertical: Dp): Modifier =
    layout { measurable, constraints ->
        val bounded = constraints.hasBoundedWidth
        val left = if (bounded) slotEdges.leftOr(rowBleed.roundToPx()) else 0
        val right = if (bounded) slotEdges.rightOr(rowBleed.roundToPx()) else 0
        val extraX = left + right
        val extraY = vertical.roundToPx() * 2
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = constraints.minWidth + extraX,
                    maxWidth = constraints.maxWidth + extraX,
                ),
            )
        val width = (placeable.width - extraX).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height - extraY).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) { placeable.place(-left, -(placeable.height - height) / 2) }
    }

/**
 * Pads a flat card's content back in from the screen edges so it sits [rowBleed] past the slot,
 * where row content inset for the card background lines up with section titles and the app grid.
 */
private fun Modifier.cardlessContentInset(slotEdges: EdgeDistances, rowBleed: Dp): Modifier =
    layout { measurable, constraints ->
        val bleed = rowBleed.roundToPx()
        val left = if (constraints.hasBoundedWidth) (slotEdges.leftOr(bleed) - bleed).coerceAtLeast(0) else 0
        val right = if (constraints.hasBoundedWidth) (slotEdges.rightOr(bleed) - bleed).coerceAtLeast(0) else 0
        val placeable = measurable.measure(constraints.offset(horizontal = -(left + right)))
        layout(placeable.width + left + right, placeable.height) { placeable.place(left, 0) }
    }

/**
 * Flat cards have no edges, so each one draws a hairline along its bottom edge (where it meets the
 * next card, or closes the list after the last one), across the full screen width like the row
 * dividers inside sections.
 */
private fun Modifier.cardlessSectionDivider(color: Color): Modifier =
    drawWithContent {
        drawContent()
        val stroke = DesignTokens.BorderWidth.toPx()
        val y = size.height - stroke / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
    }
