package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsToggleRow

/**
 * Launch Options settings screen with default assistant and quick settings tile settings.
 */
@Composable
fun LaunchOptionsSettings(
    isDefaultAssistant: Boolean,
    assistantLaunchVoiceModeEnabled: Boolean,
    onSetDefaultAssistant: () -> Unit,
    onToggleAssistantLaunchVoiceMode: (Boolean) -> Unit,
    onAddHomeScreenWidget: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWidgetConfirmDialog by remember { mutableStateOf(false) }

    if (showWidgetConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showWidgetConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_home_screen_widget_title)) },
            text = { Text(stringResource(R.string.settings_home_screen_widget_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showWidgetConfirmDialog = false
                    onAddHomeScreenWidget()
                }) {
                    Text(stringResource(R.string.dialog_okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
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

                HorizontalDivider(color = AppColors.SettingsDivider)

                SettingsToggleRow(
                    title = stringResource(R.string.settings_assistant_voice_mode_title),
                    subtitle = stringResource(R.string.settings_assistant_voice_mode_desc),
                    checked = assistantLaunchVoiceModeEnabled,
                    onCheckedChange = onToggleAssistantLaunchVoiceMode,
                    isLastItem = true,
                    extraVerticalPadding = 8.dp,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.settings_home_screen_widget_title),
                            description = stringResource(R.string.settings_home_screen_widget_desc),
                            actionOnPress = { showWidgetConfirmDialog = true },
                        ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                )

                HorizontalDivider(color = AppColors.SettingsDivider)

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
}
