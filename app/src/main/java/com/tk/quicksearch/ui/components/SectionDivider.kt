package com.tk.quicksearch.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Standardized horizontal divider with consistent styling.
 *
 * @param modifier Modifier to be applied to the divider
 */
@Composable
fun SectionDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = DesignTokens.DividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}