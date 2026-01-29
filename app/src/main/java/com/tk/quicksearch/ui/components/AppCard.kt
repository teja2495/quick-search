package com.tk.quicksearch.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Standardized card wrapper with consistent styling and wallpaper background support.
 *
 * @param modifier Modifier to be applied to the card
 * @param showWallpaperBackground Whether to use wallpaper-friendly background
 * @param content Content to display inside the card
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    showWallpaperBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = AppColors.getCardColors(showWallpaperBackground),
        shape = DesignTokens.ShapeMedium,
    ) {
        content()
    }
}
