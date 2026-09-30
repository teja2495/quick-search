package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

private const val RESULT_CARD_FADE_DURATION_MS = 140

/**
 * False when the user turned on Compact Layout: result and home cards drop their container
 * background and border so content sits directly on the screen background.
 */
internal val LocalSearchCardLayoutEnabled = compositionLocalOf { true }

/** False on Home, where flat cards drop the divider below each section and keep only row dividers. */
internal val LocalCardlessSectionDividers = compositionLocalOf { true }

/**
 * Vertical spacing between sibling cards in the enclosing list. Flat cards (compact layout) overlap
 * each neighbour by half of it, so their bottom dividers sit exactly between two cards' content.
 */
internal val LocalCardlessStackSpacing = compositionLocalOf { DesignTokens.SpacingMedium }

@Immutable
data class SearchResultCardStyleOverrides(
    val shape: Shape? = null,
    val border: BorderStroke? = null,
    val colors: CardColors? = null,
    val containerColor: Color? = null,
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
                DesignTokens.SearchResultCardShape
            } else {
                DesignTokens.ShapeMedium
            }

    /** Subtle border shown when wallpaper/custom image is active in dark mode. */
    private val wallpaperDarkBorder =
        BorderStroke(DesignTokens.BorderWidth, Color.White.copy(alpha = 0.12f))

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
    ): CardColors = AppColors.getSearchResultCardColors(
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = containerColorOverride ?: overlayContainerColor,
    )
}

/**
 * Search-screen card wrapper (counterpart to [com.tk.quicksearch.settings.shared.SettingsCard]).
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
        if (cardLayoutEnabled) {
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
    val showCardlessSectionDivider = LocalCardlessSectionDividers.current
    val cardlessDividerColor =
        LocalOverlayDividerColor.current
            ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier =
            if (cardLayoutEnabled) {
                modifier
            } else {
                Modifier.cardlessBleed(DesignTokens.SpacingSmall, cardlessVerticalBleed)
                    .then(if (showCardlessSectionDivider) Modifier.cardlessSectionDivider(cardlessDividerColor) else Modifier)
                    .then(modifier)
            },
        colors = colors,
        shape = shape,
        border = border,
        content = content,
    )
}

/**
 * Extends a flat (card layout off) card [horizontal] past its slot on both sides, so row content
 * that was inset for the card background lines up with section titles and the app grid instead,
 * and overlaps neighbours by [vertical] so adjacent flat cards meet edge to edge.
 */
private fun Modifier.cardlessBleed(horizontal: Dp, vertical: Dp): Modifier =
    layout { measurable, constraints ->
        val extraX = if (constraints.hasBoundedWidth) horizontal.roundToPx() * 2 else 0
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
        layout(width, height) { placeable.placeRelative(-extraX / 2, -(placeable.height - height) / 2) }
    }

/**
 * Flat cards have no edges, so each one draws a hairline along its bottom edge (where it meets the
 * next card), inset like the row dividers inside sections.
 */
private fun Modifier.cardlessSectionDivider(color: Color): Modifier =
    drawWithContent {
        drawContent()
        val stroke = DesignTokens.BorderWidth.toPx()
        val inset = DesignTokens.SpacingMedium.toPx()
        val y = size.height - stroke / 2
        drawLine(color, Offset(inset, y), Offset(size.width - inset, y), strokeWidth = stroke)
    }
