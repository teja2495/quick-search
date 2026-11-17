package com.tk.quicksearch.search.searchScreen.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun InfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = AppShapes.large,
    ) {
        Text(
            text = message,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

