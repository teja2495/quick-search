package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsIconBadge
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsSegmentGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun SettingsExportDialog(
    selectionState: ExportSelectionState,
    onSelectionStateChange: (ExportSelectionState) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    val options = buildList {
        add(ExportOption(R.string.settings_gesture_settings, MaterialSymbols.Tune, true, enabled = false))
        if (selectionState.showPinnedItemsOption) add(ExportOption(
            R.string.settings_backup_export_option_pinned_items_title, MaterialSymbols.PushPin,
            selectionState.includePinnedItems,
        ) { onSelectionStateChange(selectionState.copy(includePinnedItems = it)) })
        if (selectionState.showNotesOption) add(ExportOption(
            R.string.section_notes, MaterialSymbols.Description, selectionState.includeNotes,
        ) { onSelectionStateChange(selectionState.copy(includeNotes = it)) })
        if (selectionState.showCalendarEventsOption) add(ExportOption(
            R.string.section_calendar, MaterialSymbols.CalendarMonth, selectionState.includeCalendarEvents,
        ) { onSelectionStateChange(selectionState.copy(includeCalendarEvents = it)) })
        if (selectionState.showRemindersOption) add(ExportOption(
            R.string.section_reminders, MaterialSymbols.NotificationsActive, selectionState.includeReminders,
        ) { onSelectionStateChange(selectionState.copy(includeReminders = it)) })
        if (selectionState.showBookmarksOption) add(ExportOption(
            R.string.section_bookmarks, MaterialSymbols.Bookmark, selectionState.includeBookmarks,
        ) { onSelectionStateChange(selectionState.copy(includeBookmarks = it)) })
        if (selectionState.showSearchHistoryOption) add(ExportOption(
            R.string.recent_queries_toggle_title, MaterialSymbols.History, selectionState.includeSearchHistory,
        ) { onSelectionStateChange(selectionState.copy(includeSearchHistory = it)) })
        if (selectionState.showApiKeysOption) add(ExportOption(
            R.string.settings_backup_export_option_api_keys, MaterialSymbols.Key, selectionState.includeApiKeys,
        ) { onSelectionStateChange(selectionState.copy(includeApiKeys = it)) })
    }
    AppPickerDrawer(
        title = stringResource(R.string.settings_backup_export_selection_title),
        onDismiss = onDismiss,
        footer = {
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_export_button))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            SettingsSegmentGroup {
                options.forEachIndexed { index, option ->
                    SettingsSegment(index = index, count = options.size) {
                        SettingsSegmentRow(
                            title = stringResource(option.titleRes),
                            modifier = Modifier.toggleable(
                                value = option.checked,
                                enabled = option.enabled,
                                role = Role.Checkbox,
                                onValueChange = option.onCheckedChange,
                            ),
                            leading = { SettingsIconBadge(option.icon) },
                            trailing = {
                                Checkbox(
                                    checked = option.checked,
                                    enabled = option.enabled,
                                    onCheckedChange = null,
                                )
                            },
                        )
                    }
                }
            }
            if (selectionState.showApiKeysOption && selectionState.includeApiKeys) {
                Text(
                    text = stringResource(R.string.settings_backup_export_api_key_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                )
            }
        }
    }
}

private data class ExportOption(
    val titleRes: Int,
    val icon: ImageVector,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit = {},
)

data class ExportSelectionState(
    val includePinnedItems: Boolean = true,
    val includeNotes: Boolean = true,
    val includeCalendarEvents: Boolean = true,
    val includeReminders: Boolean = true,
    val includeBookmarks: Boolean = true,
    val includeSearchHistory: Boolean = true,
    val includeApiKeys: Boolean = false,
    val showPinnedItemsOption: Boolean = true,
    val showNotesOption: Boolean = true,
    val showCalendarEventsOption: Boolean = false,
    val showRemindersOption: Boolean = false,
    val showBookmarksOption: Boolean = false,
    val showSearchHistoryOption: Boolean = false,
    val showApiKeysOption: Boolean = false,
) {
    fun toExportOptions(): SettingsBackupManager.ExportOptions {
        val items = buildSet {
            // Settings and search engines are always exported.
            add(SettingsBackupManager.ExportItem.SETTINGS)
            add(SettingsBackupManager.ExportItem.SEARCH_ENGINES)
            if (includePinnedItems) add(SettingsBackupManager.ExportItem.PINNED_ITEMS)
            add(SettingsBackupManager.ExportItem.SHORTCUTS)
            if (includeNotes) add(SettingsBackupManager.ExportItem.NOTES)
            if (includeCalendarEvents) add(SettingsBackupManager.ExportItem.CALENDAR_EVENTS)
            if (includeReminders) add(SettingsBackupManager.ExportItem.REMINDERS)
            if (includeBookmarks) add(SettingsBackupManager.ExportItem.BOOKMARKS)
            if (includeSearchHistory && showSearchHistoryOption) add(SettingsBackupManager.ExportItem.SEARCH_HISTORY)
            if (includeApiKeys && showApiKeysOption) add(SettingsBackupManager.ExportItem.API_KEYS)
        }
        return SettingsBackupManager.ExportOptions(selectedItems = items)
    }
}
