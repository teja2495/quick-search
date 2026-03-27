package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun AppPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.ShapeFull,
        color = AppColors.KeyboardPillBackground,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.KeyboardPillText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
