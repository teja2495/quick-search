package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager.ExportItem
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun SettingsExportDialog(
    selectionState: ExportSelectionState,
    onSelectionStateChange: (ExportSelectionState) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    val options = buildList {
        add(ExportOption(R.string.settings_gesture_settings, MaterialSymbols.Tune, true, enabled = false))
        ExportItem.selectableItems.forEach { section ->
            if (section in selectionState.available) {
                val option = checkNotNull(section.option)
                add(
                    ExportOption(option.titleRes, option.icon(), section in selectionState.selected) {
                        onSelectionStateChange(selectionState.withSelection(section, it))
                    },
                )
            }
        }
    }
    AppPickerDrawer(
        title = stringResource(R.string.settings_backup_export_selection_title),
        onDismiss = onDismiss,
        footer = {
            AppButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_export_button))
            }
            AppTextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            SegmentedGroup {
                options.forEachIndexed { index, option ->
                    SegmentedTile(index = index, count = options.size) {
                        SegmentedTileRow(
                            title = stringResource(option.titleRes),
                            modifier = Modifier.toggleable(
                                value = option.checked,
                                enabled = option.enabled,
                                role = Role.Checkbox,
                                onValueChange = option.onCheckedChange,
                            ),
                            leading = { TonalIconBadge(option.icon) },
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
            if (ExportItem.API_KEYS in selectionState.available && ExportItem.API_KEYS in selectionState.selected) {
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
    val available: Set<ExportItem> = emptySet(),
    val selected: Set<ExportItem> = emptySet(),
) {
    internal fun withSelection(item: ExportItem, checked: Boolean): ExportSelectionState =
        copy(selected = if (checked) selected + item else selected - item)

    fun toExportOptions(): SettingsBackupManager.ExportOptions =
        SettingsBackupManager.ExportOptions(ExportItem.requiredItems + selected.intersect(available))

    internal companion object {
        fun forAvailable(available: Set<ExportItem>): ExportSelectionState =
            ExportSelectionState(available, available.intersect(ExportItem.defaultSelection))
    }
}
