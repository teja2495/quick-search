package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Sub-option of a settings row: a checkbox and its label on a small accent-tinted panel, so it reads
 * as part of the row above it inside a segmented tile. The whole panel toggles.
 */
@Composable
fun SettingsNestedCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(NestedCheckboxShape)
                .background(AppColors.AccentTintSubtle)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(start = DesignTokens.SpacingSmall, end = DesignTokens.SpacingMedium, top = DesignTokens.SpacingXSmall, bottom = DesignTokens.SpacingXSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        // The panel is the touch target, so the checkbox drops its own 48dp inset.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(DesignTokens.SpacingXSmall),
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = AppColors.Accent,
                        checkmarkColor = AppColors.OnAccent,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val NestedCheckboxShape = AppShapes.large
