package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes

/**
 * Material 3 input chip for a value the user entered, such as an excluded file extension. Tapping
 * the chip removes it, and the trailing close icon says so. It is a pill like [AppFilterChip].
 */
@Composable
fun AppInputChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = false,
        onClick = onRemove,
        label = { Text(text = label) },
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = MaterialSymbols.Close,
                contentDescription = stringResource(R.string.action_remove),
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        shape = AppShapes.full,
        colors =
            InputChipDefaults.inputChipColors(
                labelColor = MaterialTheme.colorScheme.onSurface,
                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        border =
            InputChipDefaults.inputChipBorder(
                enabled = true,
                selected = false,
                borderColor = AppColors.SettingsDivider,
            ),
    )
}
