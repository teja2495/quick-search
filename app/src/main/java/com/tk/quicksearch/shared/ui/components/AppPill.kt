package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun AppPill(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = AppColors.Accent,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.full,
        color = AppColors.KeyboardPillBackground,
        tonalElevation = DesignTokens.ElevationLevel0,
    ) {
        Row(
            modifier =
                if (compact) {
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                } else {
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                },
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = leadingIconTint,
                    modifier = Modifier.size(if (compact) 12.dp else 14.dp),
                )
            }
            Text(
                text = text,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = AppColors.KeyboardPillText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
