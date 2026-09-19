package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCheckboxRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun SettingsExportDialog(
    selectionState: ExportSelectionState,
    onSelectionStateChange: (ExportSelectionState) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_backup_export_selection_title))
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                // Settings are always exported; shown checked and locked so users know they're included.
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_gesture_settings),
                    description = "",
                    checked = true,
                    onCheckedChange = {},
                    icon = Icons.Rounded.Tune,
                    isLastItem = false,
                    enabled = false,
                )
                if (selectionState.showPinnedItemsOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.settings_backup_export_option_pinned_items_title),
                        description = "",
                        checked = selectionState.includePinnedItems,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includePinnedItems = it))
                        },
                        icon = Icons.Rounded.PushPin,
                        isLastItem = false,
                    )
                }
                SettingsCheckboxRow(
                    title = stringResource(R.string.section_app_shortcuts),
                    description = "",
                    checked = selectionState.includeShortcuts,
                    onCheckedChange = {
                        onSelectionStateChange(selectionState.copy(includeShortcuts = it))
                    },
                    icon = Icons.Rounded.Apps,
                    isLastItem = !selectionState.showNotesOption && !selectionState.showCalendarEventsOption && !selectionState.showRemindersOption && !selectionState.showApiKeysOption,
                )
                if (selectionState.showNotesOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.section_notes),
                        description = "",
                        checked = selectionState.includeNotes,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includeNotes = it))
                        },
                        icon = Icons.Rounded.Description,
                        isLastItem = !selectionState.showCalendarEventsOption && !selectionState.showRemindersOption && !selectionState.showApiKeysOption,
                    )
                }
                if (selectionState.showCalendarEventsOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.section_calendar),
                        description = "",
                        checked = selectionState.includeCalendarEvents,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includeCalendarEvents = it))
                        },
                        icon = Icons.Rounded.CalendarMonth,
                        isLastItem = !selectionState.showRemindersOption && !selectionState.showApiKeysOption,
                    )
                }
                if (selectionState.showRemindersOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.section_reminders),
                        description = "",
                        checked = selectionState.includeReminders,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includeReminders = it))
                        },
                        icon = Icons.Rounded.NotificationsActive,
                        isLastItem = !selectionState.showApiKeysOption,
                    )
                }
                if (selectionState.showApiKeysOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.settings_backup_export_option_api_keys),
                        description = "",
                        checked = selectionState.includeApiKeys,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includeApiKeys = it))
                        },
                        icon = Icons.Rounded.Key,
                        isLastItem = true,
                    )
                }
                if (selectionState.showApiKeysOption && selectionState.includeApiKeys) {
                    Text(
                        text = stringResource(R.string.settings_backup_export_api_key_warning_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = DesignTokens.SpacingMedium),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onExport) {
                Text(text = stringResource(R.string.settings_backup_export_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

data class ExportSelectionState(
    val includePinnedItems: Boolean = true,
    val includeShortcuts: Boolean = true,
    val includeNotes: Boolean = true,
    val includeCalendarEvents: Boolean = true,
    val includeReminders: Boolean = true,
    val includeApiKeys: Boolean = false,
    val showPinnedItemsOption: Boolean = true,
    val showNotesOption: Boolean = true,
    val showCalendarEventsOption: Boolean = false,
    val showRemindersOption: Boolean = false,
    val showApiKeysOption: Boolean = false,
) {
    fun toExportOptions(): SettingsBackupManager.ExportOptions {
        val items = buildSet {
            // Settings and search engines are always exported; search history never is.
            add(SettingsBackupManager.ExportItem.SETTINGS)
            add(SettingsBackupManager.ExportItem.SEARCH_ENGINES)
            if (includePinnedItems) add(SettingsBackupManager.ExportItem.PINNED_ITEMS)
            if (includeShortcuts) add(SettingsBackupManager.ExportItem.SHORTCUTS)
            if (includeNotes) add(SettingsBackupManager.ExportItem.NOTES)
            if (includeCalendarEvents) add(SettingsBackupManager.ExportItem.CALENDAR_EVENTS)
            if (includeReminders) add(SettingsBackupManager.ExportItem.REMINDERS)
            if (includeApiKeys && showApiKeysOption) add(SettingsBackupManager.ExportItem.API_KEYS)
        }
        return SettingsBackupManager.ExportOptions(selectedItems = items)
    }
}
