package com.tk.quicksearch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Icon with text in a row pattern - commonly used throughout the app.
 *
 * @param icon The icon to display
 * @param text The text to display next to the icon
 * @param modifier Modifier to be applied to the row
 * @param iconTint Color to tint the icon
 * @param textStyle Text style to apply
 */
@Composable
fun IconWithText(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(DesignTokens.IconSize)
        )
        Text(
            text = text,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}