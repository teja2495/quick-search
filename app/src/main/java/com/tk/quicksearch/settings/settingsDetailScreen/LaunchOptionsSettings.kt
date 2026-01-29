package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow

/**
 * Launch Options settings screen with default assistant and quick settings tile settings.
 */
@Composable
fun LaunchOptionsSettings(
    isDefaultAssistant: Boolean,
    onSetDefaultAssistant: () -> Unit,
    onAddHomeScreenWidget: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            // Default Assistant Section
            SettingsNavigationRow(
                item =
                    SettingsCardItem(
                        title = stringResource(R.string.settings_default_assistant_title),
                        description =
                            stringResource(
                                if (isDefaultAssistant) {
                                    R.string.settings_default_assistant_desc_change
                                } else {
                                    R.string.settings_default_assistant_desc
                                },
                            ),
                        actionOnPress = onSetDefaultAssistant,
                    ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Home Screen Widget Section
            SettingsNavigationRow(
                item =
                    SettingsCardItem(
                        title = stringResource(R.string.settings_home_screen_widget_title),
                        description = stringResource(R.string.settings_home_screen_widget_desc),
                        actionOnPress = onAddHomeScreenWidget,
                    ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            )

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Settings Tile Section
            SettingsNavigationRow(
                item =
                    SettingsCardItem(
                        title = stringResource(R.string.settings_quick_settings_tile_title),
                        description = stringResource(R.string.settings_quick_settings_tile_desc),
                        actionOnPress = onAddQuickSettingsTile,
                    ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}
