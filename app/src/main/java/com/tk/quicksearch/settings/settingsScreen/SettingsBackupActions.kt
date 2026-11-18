package com.tk.quicksearch.settings.settingsScreen

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.AppTextButton

/** Builds the export dialog's initial selection. Reads encrypted storage, so call off the main thread. */
internal fun loadExportSelectionState(context: Context): ExportSelectionState {
    val source = BackupSectionAvailability(context)
    val available = SettingsBackupManager.ExportItem.selectableItems.filterTo(mutableSetOf()) { section ->
        section.option?.isAvailable?.invoke(source) == true
    }
    return ExportSelectionState.forAvailable(available)
}

internal fun defaultBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "quick-search-settings-$timestamp.quicksearch"
}

internal fun exportSettingsToDownloads(
    context: Context,
    selectionState: ExportSelectionState,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch(Dispatchers.IO) {
        val fileName = defaultBackupFileName()
        val result =
            runCatching {
                saveSettingsToDownloads(
                    context,
                    fileName,
                    selectionState.toExportOptions(),
                )
            }
        withContext(Dispatchers.Main) {
            val uri = result.getOrNull()
            if (uri == null) {
                AppMessages.show(context, R.string.settings_backup_export_failed)
                return@withContext
            }
            AppMessages.show(context, R.string.settings_features_saved_to_downloads)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(fileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.action_share)),
            )
        }
    }
}

private fun saveSettingsToDownloads(
    context: Context,
    fileName: String,
    options: SettingsBackupManager.ExportOptions,
): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val uri =
            checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
                "Unable to create the settings backup in Downloads"
            }
        try {
            SettingsBackupManager.exportToUri(context, uri, options)
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    } else {
        val downloadsDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        check(downloadsDirectory.exists() || downloadsDirectory.mkdirs()) {
            "Unable to access Downloads"
        }
        val file = File(downloadsDirectory, fileName)
        SettingsBackupManager.exportToUri(
            context,
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
            options,
        )
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
            AppMessages.show(context, context.getString(messageResId))
            if (isSuccess) {
                onSuccess()
            }
        }
    }
}

enum class SettingsBackupRequest {
    IMPORT,
    EXPORT,
}

/**
 * Self-contained import / export flows with their own file picker and dialogs, for surfaces
 * outside the Settings screen (e.g. app settings search results). Keep it composed while the
 * file picker is open so the import result is still delivered.
 */
@Composable
fun SettingsBackupFlow(
    request: SettingsBackupRequest?,
    onRequestHandled: () -> Unit,
    onSettingsImported: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportSelectionState by remember { mutableStateOf<ExportSelectionState?>(null) }

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

    LaunchedEffect(request) {
        if (request == SettingsBackupRequest.EXPORT) {
            exportSelectionState = withContext(Dispatchers.IO) { loadExportSelectionState(context) }
        }
    }

    when (request) {
        SettingsBackupRequest.IMPORT ->
            AppAlertDialog(
                onDismissRequest = onRequestHandled,
                title = { Text(text = stringResource(R.string.settings_backup_import_warning_title)) },
                text = { Text(text = stringResource(R.string.settings_backup_import_warning_message)) },
                confirmButton = {
                    AppTextButton(
                        onClick = {
                            onRequestHandled()
                            importLauncher.launch(arrayOf("*/*"))
                        },
                    ) {
                        Text(text = stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    AppTextButton(onClick = onRequestHandled) {
                        Text(text = stringResource(R.string.dialog_cancel))
                    }
                },
            )
        SettingsBackupRequest.EXPORT ->
            exportSelectionState?.let { selectionState ->
                val closeExport = {
                    exportSelectionState = null
                    onRequestHandled()
                }
                SettingsExportDialog(
                    selectionState = selectionState,
                    onSelectionStateChange = { exportSelectionState = it },
                    onDismiss = closeExport,
                    onExport = {
                        closeExport()
                        exportSettingsToDownloads(context, selectionState, coroutineScope)
                    },
                )
            }
        null -> Unit
    }
}
