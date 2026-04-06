package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
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
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_backup_export_option_settings_title),
                    description = "",
                    checked = selectionState.includeSettings,
                    onCheckedChange = {
                        onSelectionStateChange(selectionState.copy(includeSettings = it))
                    },
                    icon = Icons.Rounded.Tune,
                    isLastItem = false,
                )
                if (selectionState.showSearchHistoryOption) {
                    SettingsCheckboxRow(
                        title = stringResource(R.string.recent_queries_toggle_title),
                        description = "",
                        checked = selectionState.includeSearchHistory,
                        onCheckedChange = {
                            onSelectionStateChange(selectionState.copy(includeSearchHistory = it))
                        },
                        icon = Icons.Rounded.History,
                        isLastItem = false,
                    )
                }
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
                    isLastItem = false,
                )
                SettingsCheckboxRow(
                    title = stringResource(R.string.section_notes),
                    description = "",
                    checked = selectionState.includeNotes,
                    onCheckedChange = {
                        onSelectionStateChange(selectionState.copy(includeNotes = it))
                    },
                    icon = Icons.Rounded.Description,
                    isLastItem = false,
                )
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
                    description = "",
                    checked = selectionState.includeSearchEngines,
                    onCheckedChange = {
                        onSelectionStateChange(selectionState.copy(includeSearchEngines = it))
                    },
                    icon = Icons.AutoMirrored.Rounded.ManageSearch,
                    isLastItem = false,
                )
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_backup_export_option_gemini_title),
                    description = "",
                    checked = selectionState.includeGeminiApi,
                    onCheckedChange = {
                        onSelectionStateChange(selectionState.copy(includeGeminiApi = it))
                    },
                    iconResId = R.drawable.direct_search,
                    isLastItem = true,
                )
                if (selectionState.includeGeminiApi) {
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
            Button(
                enabled = selectionState.hasAnySelection(),
                onClick = onExport,
            ) {
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
    val includeSettings: Boolean = true,
    val includeSearchHistory: Boolean = true,
    val includePinnedItems: Boolean = true,
    val includeShortcuts: Boolean = true,
    val includeNotes: Boolean = true,
    val includeSearchEngines: Boolean = true,
    val includeGeminiApi: Boolean = false,
    val showSearchHistoryOption: Boolean = true,
    val showPinnedItemsOption: Boolean = true,
) {
    fun hasAnySelection(): Boolean =
        includeSettings ||
            includeSearchHistory ||
            includePinnedItems ||
            includeShortcuts ||
            includeNotes ||
            includeSearchEngines ||
            includeGeminiApi

    fun toExportOptions(): SettingsBackupManager.ExportOptions {
        val items = buildSet {
            if (includeSettings) add(SettingsBackupManager.ExportItem.SETTINGS)
            if (includeSearchHistory) add(SettingsBackupManager.ExportItem.SEARCH_HISTORY)
            if (includePinnedItems) add(SettingsBackupManager.ExportItem.PINNED_ITEMS)
            if (includeShortcuts) add(SettingsBackupManager.ExportItem.SHORTCUTS)
            if (includeNotes) add(SettingsBackupManager.ExportItem.NOTES)
            if (includeSearchEngines) add(SettingsBackupManager.ExportItem.SEARCH_ENGINES)
            if (includeGeminiApi) add(SettingsBackupManager.ExportItem.GEMINI_API)
        }
        return SettingsBackupManager.ExportOptions(selectedItems = items)
    }
}
