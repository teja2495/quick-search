package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = AppColors.getSettingsCardContainerColor(),
            ),
        elevation = AppColors.getCardElevation(false),
        shape = MaterialTheme.shapes.extraLarge,
        content = content,
    )
}
