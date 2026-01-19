package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R

/**
 * Launch Options settings screen with default assistant and quick settings tile settings.
 */
@Composable
fun LaunchOptionsSettings(
    isDefaultAssistant: Boolean,
    onSetDefaultAssistant: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Default Assistant Section
            NavigationSection(
                title = stringResource(R.string.settings_default_assistant_title),
                description = stringResource(
                    if (isDefaultAssistant) {
                        R.string.settings_default_assistant_desc_change
                    } else {
                        R.string.settings_default_assistant_desc
                    }
                ),
                onClick = onSetDefaultAssistant
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Settings Tile Section
            NavigationSection(
                title = stringResource(R.string.settings_quick_settings_tile_title),
                description = stringResource(R.string.settings_quick_settings_tile_desc),
                onClick = onAddQuickSettingsTile
            )
        }
    }
}