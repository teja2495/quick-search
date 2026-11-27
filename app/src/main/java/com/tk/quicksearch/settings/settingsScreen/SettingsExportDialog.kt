package com.tk.quicksearch.settings.settingsScreen

import android.net.Uri
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.settings.autoBackup.AutoBackupFrequency
import com.tk.quicksearch.settings.autoBackup.DEFAULT_KEPT_BACKUPS
import com.tk.quicksearch.settings.autoBackup.autoBackupFolderPath
import com.tk.quicksearch.settings.autoBackup.AutoBackupScheduleDrawer
import com.tk.quicksearch.settings.autoBackup.AutoBackupScheduler
import com.tk.quicksearch.settings.autoBackup.AutoBackupStore
import com.tk.quicksearch.settings.autoBackup.ScheduleAutoBackupTile
import com.tk.quicksearch.settings.autoBackup.folderPath
import kotlinx.coroutines.launch

@Composable
fun SettingsExportDialog(
    selectionState: ExportSelectionState,
    onSelectionStateChange: (ExportSelectionState) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val autoBackup by AutoBackupStore.state.collectAsState()
    var showSchedulePicker by rememberSaveable { mutableStateOf(false) }
    var pendingFrequencyName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingKeptBackups by rememberSaveable { mutableIntStateOf(DEFAULT_KEPT_BACKUPS) }
    // A folder picked from the schedule drawer's folder card, applied when the drawer is saved.
    var pickedFolder by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Saving the schedule closes Export Data too, so the change runs outside this composable.
    val appContext = context.applicationContext
    val enableSchedule: (AutoBackupFrequency, Uri, Int) -> Unit = { frequency, folder, keptBackups ->
        val items = selectionState.scheduledItems()
        AutoBackupScheduler.launchChange {
            runCatching { enable(appContext, frequency, folder, items, keptBackups) }
                .onFailure { AppMessages.show(appContext, R.string.auto_backup_folder_failed) }
        }
    }
    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val frequency = AutoBackupFrequency.entries.firstOrNull { it.name == pendingFrequencyName }
            pendingFrequencyName = null
            if (uri == null) return@rememberLauncherForActivityResult
            // Saving without a folder asked for one, so finish the save and close; otherwise the
            // drawer's folder card asked, and the drawer stays open with the new folder.
            if (frequency != null) {
                enableSchedule(frequency, uri, pendingKeptBackups)
                onDismiss()
            } else {
                pickedFolder = uri
            }
        }
    val launchFolderPicker = {
        runCatching { folderLauncher.launch(null) }
            .onFailure { AppMessages.show(context, R.string.auto_backup_folder_failed) }
    }
    // A scheduled auto-backup saves whatever is checked here.
    val updateSelection: (ExportSelectionState) -> Unit = { next ->
        onSelectionStateChange(next)
        AutoBackupStore.updateSelection(context, next.scheduledItems())
    }

    if (showSchedulePicker) {
        AutoBackupScheduleDrawer(
            current = autoBackup?.frequency,
            currentKeptBackups = autoBackup?.keptBackups ?: DEFAULT_KEPT_BACKUPS,
            folderPath = pickedFolder?.let(::autoBackupFolderPath) ?: autoBackup?.folderPath(),
            onChangeFolder = { launchFolderPicker() },
            onDismiss = {
                showSchedulePicker = false
                pickedFolder = null
            },
            // Saving closes Export Data as well, except while the folder picker is still to answer.
            onConfirm = { frequency, keptBackups ->
                val current = autoBackup
                val newFolder = pickedFolder?.takeIf { it != current?.folderUri }
                when {
                    frequency == null -> {
                        if (current != null) AutoBackupScheduler.launchChange { disable(appContext) }
                        onDismiss()
                    }
                    newFolder != null -> {
                        enableSchedule(frequency, newFolder, keptBackups)
                        onDismiss()
                    }
                    current == null -> {
                        pendingFrequencyName = frequency.name
                        pendingKeptBackups = keptBackups
                        launchFolderPicker()
                    }
                    else -> {
                        AutoBackupScheduler.launchChange { changeSchedule(appContext, frequency, keptBackups) }
                        onDismiss()
                    }
                }
            },
        )
        return
    }

    val options = buildList {
        add(ExportOption(R.string.settings_gesture_settings, MaterialSymbols.Tune, true, enabled = false))
        ExportItem.selectableItems.forEach { section ->
            if (section in selectionState.available) {
                val option = checkNotNull(section.option)
                add(
                    ExportOption(option.titleRes, option.icon(), section in selectionState.selected) {
                        updateSelection(selectionState.withSelection(section, it))
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
            ScheduleAutoBackupTile(
                state = autoBackup,
                onClick = { showSchedulePicker = true },
                onClear = { coroutineScope.launch { AutoBackupScheduler.disable(context) } },
            )
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
    /**
     * What a scheduled auto-backup saves: the checked items, plus the default for items not
     * offered right now (nothing to export yet), so they're included once they have data.
     */
    internal fun scheduledItems(): Set<ExportItem> =
        selected.intersect(available) + (ExportItem.defaultSelection - available)

    internal fun withSelection(item: ExportItem, checked: Boolean): ExportSelectionState =
        copy(selected = if (checked) selected + item else selected - item)

    fun toExportOptions(): SettingsBackupManager.ExportOptions =
        SettingsBackupManager.ExportOptions(ExportItem.requiredItems + selected.intersect(available))

    internal companion object {
        fun forAvailable(
            available: Set<ExportItem>,
            selected: Set<ExportItem> = ExportItem.defaultSelection,
        ): ExportSelectionState = ExportSelectionState(available, available.intersect(selected))
    }
}
