package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

private const val RESULT_CARD_FADE_DURATION_MS = 140

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
    /** Base corner shape used by result/history/web/direct-search/search-on cards. */
    val shape: Shape
        get() = DesignTokens.SearchResultCardShape

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
    val shape = styleOverrides.shape ?: SearchResultCardDefaults.shape
    val border = styleOverrides.border ?: SearchResultCardDefaults.border(showWallpaperBackground)
    val colors =
        styleOverrides.colors ?: SearchResultCardDefaults.colors(
            showWallpaperBackground = showWallpaperBackground,
            overlayContainerColor = overlayContainerColor,
            containerColorOverride = styleOverrides.containerColor,
        )
    var cardVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(durationMillis = RESULT_CARD_FADE_DURATION_MS),
        label = "searchResultCardAlpha",
    )
    LaunchedEffect(Unit) {
        cardVisible = true
    }

    Card(
        modifier =
            modifier.graphicsLayer {
                alpha = animatedAlpha
            },
        colors = colors,
        shape = shape,
        border = border,
        content = content,
    )
}
