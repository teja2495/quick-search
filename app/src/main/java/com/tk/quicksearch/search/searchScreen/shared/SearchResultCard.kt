package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

/** Subtle border shown on result cards when a wallpaper/custom image is active in dark mode. */
private val WallpaperDarkCardBorder = BorderStroke(DesignTokens.BorderWidth, Color.White.copy(alpha = 0.12f))
private const val RESULT_CARD_FADE_DURATION_MS = 140

/**
 * Search-screen card wrapper (counterpart to [com.tk.quicksearch.settings.shared.SettingsCard]).
 * Used only on the search result surface: sections, suggestions, engine cards, direct search, etc.
 * Styling is centralized via [DesignTokens.SearchResultCardShape] and [AppColors.getSearchResultCardColors].
 */
@Composable
fun SearchResultCard(
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean,
    overlayContainerColor: Color? = LocalOverlayResultCardColor.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDarkTheme = LocalAppIsDarkTheme.current
    val colors = AppColors.getSearchResultCardColors(showWallpaperBackground, overlayContainerColor)
    val shape = DesignTokens.SearchResultCardShape
    val border = if (showWallpaperBackground && isDarkTheme) WallpaperDarkCardBorder else null
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
