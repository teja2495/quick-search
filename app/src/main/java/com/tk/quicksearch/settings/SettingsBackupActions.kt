package com.tk.quicksearch.settings.settingsScreen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Builds the export dialog's initial selection. Reads encrypted storage, so call off the main thread. */
internal fun loadExportSelectionState(context: Context): ExportSelectionState {
    val userPrefs = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
    val hasPinnedItems =
        listOf(
            BasePreferences.KEY_PINNED,
            BasePreferences.KEY_PINNED_CONTACT_IDS,
            BasePreferences.KEY_PINNED_FILE_URIS,
            BasePreferences.KEY_PINNED_SETTINGS,
            BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS,
            BasePreferences.KEY_PINNED_REMINDER_IDS,
            BasePreferences.KEY_PINNED_APP_SHORTCUTS,
        ).any { key ->
            userPrefs.getStringSet(key, emptySet()).orEmpty().isNotEmpty()
        }
    val hasNotes =
        FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES) &&
            run {
                val notesJson = userPrefs.getString(BasePreferences.KEY_NOTES_DATA, null).orEmpty()
                notesJson.isNotBlank() && notesJson != "[]"
            }
    val hasCustomCalendarEvents =
        run {
            val eventsJson = userPrefs.getString(BasePreferences.KEY_CUSTOM_CALENDAR_EVENTS_DATA, null).orEmpty()
            eventsJson.isNotBlank() && eventsJson != "[]"
        }
    val hasReminders =
        run {
            val remindersJson = userPrefs.getString(BasePreferences.KEY_REMINDERS_DATA, null).orEmpty()
            remindersJson.isNotBlank() && remindersJson != "[]"
        }
    val hasApiKeys = UserAppPreferences(context).hasAnyLlmApiKey()
    return ExportSelectionState(
        includePinnedItems = hasPinnedItems,
        includeShortcuts = true,
        includeNotes = hasNotes,
        includeCalendarEvents = hasCustomCalendarEvents,
        includeReminders = hasReminders,
        includeApiKeys = false,
        showPinnedItemsOption = hasPinnedItems,
        showNotesOption = hasNotes,
        showCalendarEventsOption = hasCustomCalendarEvents,
        showRemindersOption = hasReminders,
        showApiKeysOption = hasApiKeys,
    )
}

internal fun defaultBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "quick-search-settings-$timestamp.quicksearch"
}

internal fun exportSettingsToUri(
    context: Context,
    uri: Uri,
    selectionState: ExportSelectionState,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch(Dispatchers.IO) {
        val isSuccess =
            runCatching {
                SettingsBackupManager.exportToUri(
                    context = context,
                    outputUri = uri,
                    options = selectionState.toExportOptions(),
                )
            }.isSuccess
        withContext(Dispatchers.Main) {
            val messageResId =
                if (isSuccess) {
                    R.string.settings_backup_export_success
                } else {
                    R.string.settings_backup_export_failed
                }
            Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun importSettingsFromUri(
    context: Context,
    uri: Uri,
    onSuccess: () -> Unit,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch(Dispatchers.IO) {
        val isSuccess =
            runCatching {
                SettingsBackupManager.importFromUri(context, uri)
            }.isSuccess
        withContext(Dispatchers.Main) {
            val messageResId =
                if (isSuccess) {
                    R.string.settings_backup_import_success
                } else {
                    R.string.settings_backup_import_failed
                }
            Toast
                .makeText(
                    context,
                    context.getString(messageResId),
                    Toast.LENGTH_SHORT,
                ).show()
            if (isSuccess) {
                onSuccess()
            }
        }
    }
}

/**
 * Self-contained Import / Export buttons with their own file pickers and dialogs,
 * for surfaces outside the Settings screen (e.g. app settings search results).
 */
@Composable
fun SettingsBackupButtons(
    onSettingsImported: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var showExportSelectionDialog by remember { mutableStateOf(false) }
    var exportSelectionState by remember { mutableStateOf(ExportSelectionState()) }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            exportSettingsToUri(context, uri, exportSelectionState, coroutineScope)
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            importSettingsFromUri(
                context = context,
                uri = uri,
                onSuccess = onSettingsImported,
                coroutineScope = coroutineScope,
            )
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        OutlinedButton(
            onClick = { showImportWarningDialog = true },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.settings_backup_import_button))
        }
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    exportSelectionState =
                        withContext(Dispatchers.IO) { loadExportSelectionState(context) }
                    showExportSelectionDialog = true
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.settings_backup_export_button))
        }
    }

    if (showImportWarningDialog) {
        AppAlertDialog(
            onDismissRequest = { showImportWarningDialog = false },
            title = { Text(text = stringResource(R.string.settings_backup_import_warning_title)) },
            text = { Text(text = stringResource(R.string.settings_backup_import_warning_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportWarningDialog = false
                        importLauncher.launch(arrayOf("*/*"))
                    },
                ) {
                    Text(text = stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarningDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showExportSelectionDialog) {
        SettingsExportDialog(
            selectionState = exportSelectionState,
            onSelectionStateChange = { exportSelectionState = it },
            onDismiss = { showExportSelectionDialog = false },
            onExport = {
                showExportSelectionDialog = false
                exportLauncher.launch(defaultBackupFileName())
            },
        )
    }
}
