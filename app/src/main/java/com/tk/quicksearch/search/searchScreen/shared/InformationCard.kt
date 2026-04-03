package com.tk.quicksearch.search.searchScreen.shared

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor

/**
 * Card shell for informational tool results on the search surface: calculator, unit converter,
 * date/time calculator, and Gemini / direct-search answers. Uses the same visuals as
 * [SearchResultCard]; prefer this name at call sites for those flows so they stay grouped and
 * easy to restyle independently later.
 */
@Composable
fun InformationCard(
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean,
    overlayContainerColor: Color? = LocalOverlayResultCardColor.current,
    styleOverrides: SearchResultCardStyleOverrides = SearchResultCardStyleOverrides(),
    content: @Composable ColumnScope.() -> Unit,
) {
    SearchResultCard(
        modifier = modifier,
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = overlayContainerColor,
        styleOverrides = styleOverrides,
        content = content,
    )
}
