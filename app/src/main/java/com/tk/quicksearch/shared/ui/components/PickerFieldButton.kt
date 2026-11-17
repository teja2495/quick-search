package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Outlined field that opens a picker (date, time, repeat): a leading [icon] and the current
 * [value], shaped like a text field rather than a pill button.
 */
@Composable
fun PickerFieldButton(
    icon: ImageVector,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.field,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(DesignTokens.ButtonIconSizeSmall),
        )
        Spacer(Modifier.width(DesignTokens.SpacingSmall))
        Text(text = value, modifier = Modifier.weight(1f))
    }
}
