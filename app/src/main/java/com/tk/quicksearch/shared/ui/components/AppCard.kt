package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = AppColors.getCardColors(showWallpaperBackground),
        elevation = AppColors.getCardElevation(showWallpaperBackground),
        shape = DesignTokens.ShapeMedium,
    ) {
        content()
    }
}
