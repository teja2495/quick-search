package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.notchGesture.NotchGestureSupport
import com.tk.quicksearch.settings.gestureSettings.NotchLaunchOptionRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.shared.ui.components.AppTextButton

/**
 * Launch Options settings screen with default assistant and quick settings tile settings.
 */
@Composable
fun LaunchOptionsSettings(
    isDefaultAssistant: Boolean,
    assistantLaunchVoiceModeEnabled: Boolean,
    onSetDefaultAssistant: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onToggleAssistantLaunchVoiceMode: (Boolean) -> Unit,
    onAddHomeScreenWidget: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    onOpenEdgeGesture: () -> Unit,
    onOpenFloatingButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDefaultLauncher = context.isDefaultHomeApp()
    val notchGesturesSupported = remember(context) { NotchGestureSupport.isSupported(context) }
    var showWidgetConfirmDialog by remember { mutableStateOf(false) }

    if (showWidgetConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showWidgetConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_home_screen_widget_title)) },
            text = { Text(stringResource(R.string.settings_home_screen_widget_confirm_message)) },
            confirmButton = {
                AppTextButton(onClick = {
                    showWidgetConfirmDialog = false
                    onAddHomeScreenWidget()
                }) {
                    Text(stringResource(R.string.dialog_okay))
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showWidgetConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    val options = LaunchOption.entries.filter { option ->
        when (option) {
            LaunchOption.VOICE -> isDefaultAssistant
            LaunchOption.WIDGET -> !isDefaultLauncher
            LaunchOption.NOTCH -> notchGesturesSupported
            else -> true
        }
    }
    SegmentedGroup(modifier = modifier) {
        options.forEachIndexed { index, option ->
            when (option) {
                LaunchOption.VOICE -> SegmentedToggleTile(
                    title = stringResource(R.string.settings_assistant_voice_mode_title),
                    description = stringResource(R.string.settings_assistant_voice_mode_desc),
                    icon = MaterialSymbols.Mic,
                    checked = assistantLaunchVoiceModeEnabled,
                    onCheckedChange = onToggleAssistantLaunchVoiceMode,
                    index = index,
                    count = options.size,
                )
                LaunchOption.NOTCH -> SegmentedTile(index = index, count = options.size) {
                    NotchLaunchOptionRow()
                }
                else -> {
                    val item = when (option) {
                        LaunchOption.LAUNCHER -> SettingsCardItem(
                            title = stringResource(R.string.settings_default_launcher_title),
                            description = stringResource(R.string.settings_default_launcher_desc),
                            icon = MaterialSymbols.Home,
                            actionOnPress = onSetDefaultLauncher,
                        )
                        LaunchOption.ASSISTANT -> SettingsCardItem(
                            title = stringResource(R.string.settings_default_assistant_title),
                            description = stringResource(
                                if (isDefaultAssistant) R.string.settings_default_assistant_desc_change
                                else R.string.settings_default_assistant_desc,
                            ),
                            icon = MaterialSymbols.AutoAwesome,
                            actionOnPress = onSetDefaultAssistant,
                        )
                        LaunchOption.WIDGET -> SettingsCardItem(
                            title = stringResource(R.string.settings_home_screen_widget_title),
                            description = stringResource(R.string.settings_home_screen_widget_desc),
                            icon = MaterialSymbols.Widgets,
                            actionOnPress = { showWidgetConfirmDialog = true },
                        )
                        LaunchOption.TILE -> SettingsCardItem(
                            title = stringResource(R.string.settings_quick_settings_tile_title),
                            description = stringResource(R.string.settings_quick_settings_tile_desc),
                            icon = MaterialSymbols.Tune,
                            actionOnPress = onAddQuickSettingsTile,
                        )
                        LaunchOption.EDGE -> SettingsCardItem(
                            title = stringResource(R.string.settings_edge_gesture_title),
                            description = stringResource(R.string.settings_edge_gesture_desc),
                            icon = MaterialSymbols.Swipe,
                            actionOnPress = onOpenEdgeGesture,
                        )
                        LaunchOption.FLOATING -> SettingsCardItem(
                            title = stringResource(R.string.settings_floating_button_title),
                            description = stringResource(R.string.settings_floating_button_desc),
                            icon = MaterialSymbols.Layers,
                            actionOnPress = onOpenFloatingButton,
                        )
                    }
                    SettingsSegmentNavigationRow(item = item, index = index, count = options.size)
                }
            }
        }
    }
}

private enum class LaunchOption { LAUNCHER, ASSISTANT, VOICE, WIDGET, TILE, EDGE, FLOATING, NOTCH }
