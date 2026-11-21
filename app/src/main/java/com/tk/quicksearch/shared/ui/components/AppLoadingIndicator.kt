package com.tk.quicksearch.shared.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * Material 3 Expressive loading indicator: a morphing, rotating shape for indeterminate waits.
 * Every indeterminate wait uses this instead of `CircularProgressIndicator`. Size it with
 * [modifier] (48dp by default; inline spinners use `DesignTokens.InlineProgressSize`).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
) {
    LoadingIndicator(modifier = modifier, color = color)
}
