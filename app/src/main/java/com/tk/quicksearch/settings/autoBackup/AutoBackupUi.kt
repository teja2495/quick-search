package com.tk.quicksearch.settings.autoBackup

import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Last backup time with a green check, or a failure line, for a scheduled auto-backup. */
@Composable
fun AutoBackupStatusLine(state: AutoBackupState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val failed = state.lastBackupFailed
    val text =
        when {
            failed -> stringResource(R.string.auto_backup_status_failed)
            state.lastBackupMillis != null ->
                stringResource(
                    R.string.auto_backup_status_last_backup,
                    DateUtils.formatDateTime(
                        context,
                        state.lastBackupMillis,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
                    ),
                )
            else -> stringResource(R.string.auto_backup_status_pending)
        }
    val color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val icon =
        when {
            failed -> MaterialSymbols.ErrorOutline
            state.lastBackupMillis != null -> MaterialSymbols.CheckCircle
            else -> MaterialSymbols.Schedule
        }
    val iconTint = if (!failed && state.lastBackupMillis != null) AppColors.TrustAccent else color
    // The icon trails the text inline, so it follows the last word when the line wraps.
    val annotated =
        buildAnnotatedString {
            append(text)
            append(' ')
            appendInlineContent(StatusIconId)
        }
    val inlineContent =
        mapOf(
            StatusIconId to
                InlineTextContent(
                    Placeholder(StatusIconEm.em, StatusIconEm.em, PlaceholderVerticalAlign.TextCenter),
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.fillMaxSize())
                },
        )
    Text(
        text = annotated,
        inlineContent = inlineContent,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier,
    )
}

/** One detail of a scheduled auto-backup, such as its folder or frequency, after its [icon]. */
@Composable
private fun AutoBackupDetailLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DetailIconSize),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Readable path of the backup folder on device storage, like "Documents/Backups". Folders from
 * other providers, and a storage root, give the folder's display name.
 */
fun AutoBackupState.folderPath(): String = autoBackupFolderPath(folderUri, folderName)

/** [autoBackupFolderPath] for a folder just picked, before its name has been read. */
fun autoBackupFolderPath(folderUri: Uri): String =
    autoBackupFolderPath(folderUri, folderUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/').orEmpty())

private fun autoBackupFolderPath(folderUri: Uri, folderName: String): String {
    if (folderUri.authority != EXTERNAL_STORAGE_AUTHORITY) return folderName
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(folderUri) }.getOrNull() ?: return folderName
    return documentId.substringAfter(':', "").trim('/').ifEmpty { folderName }
}

/** The Export Data drawer's tile that opens the schedule picker, or shows and clears the schedule. */
@Composable
fun ScheduleAutoBackupTile(
    state: AutoBackupState?,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    SegmentedGroup {
        SegmentedTile(index = 0, count = 1, onClick = onClick) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TileMinHeight)
                        .padding(start = DesignTokens.SpacingLarge, end = DesignTokens.SpacingSmall)
                        .padding(vertical = DesignTokens.SpacingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall / 2),
                ) {
                    Text(
                        text =
                            stringResource(
                                if (state == null) R.string.auto_backup_schedule_title else R.string.auto_backup_configured_title,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (state == null) {
                        Text(
                            text = stringResource(R.string.widget_icon_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        AutoBackupDetailLine(MaterialSymbols.Folder, state.folderPath())
                        AutoBackupDetailLine(MaterialSymbols.Repeat, stringResource(state.frequency.labelRes))
                    }
                }
                if (state == null) {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = DesignTokens.SpacingSmall),
                    )
                } else {
                    val clearLabel = stringResource(R.string.common_action_clear)
                    AppIconButton(onClick = onClear, tooltip = clearLabel) {
                        Icon(imageVector = MaterialSymbols.Close, contentDescription = clearLabel)
                    }
                }
            }
        }
    }
}

/**
 * Frequency picker: None and each frequency, then (with a frequency picked) the backup folder and
 * how many backups to keep. Saving without a folder asks for one; None clears the schedule.
 */
@Composable
fun AutoBackupScheduleDrawer(
    current: AutoBackupFrequency?,
    currentKeptBackups: Int,
    folderPath: String?,
    onChangeFolder: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (frequency: AutoBackupFrequency?, keptBackups: Int) -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(current?.name) }
    var keptBackups by rememberSaveable { mutableIntStateOf(currentKeptBackups) }
    val selected = AutoBackupFrequency.entries.firstOrNull { it.name == selectedName }
    val choices = listOf<AutoBackupFrequency?>(null) + AutoBackupFrequency.entries
    AppPickerDrawer(
        title = stringResource(R.string.auto_backup_schedule_title),
        onDismiss = onDismiss,
        footer = {
            AppButton(onClick = { onConfirm(selected, keptBackups) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (selected != null && folderPath == null) R.string.auto_backup_choose_folder else R.string.dialog_save,
                    ),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            SegmentedGroup(modifier = Modifier.selectableGroup()) {
                choices.forEachIndexed { index, frequency ->
                    SegmentedTile(index = index, count = choices.size) {
                        SegmentedTileRow(
                            title = stringResource(frequency?.labelRes ?: R.string.settings_gesture_none),
                            modifier =
                                Modifier.selectable(
                                    selected = frequency == selected,
                                    role = Role.RadioButton,
                                    onClick = { selectedName = frequency?.name },
                                ),
                            trailing = { RadioButton(selected = frequency == selected, onClick = null) },
                        )
                    }
                }
            }
            if (selected != null) {
                SegmentedGroup {
                    SegmentedTile(index = 0, count = 2, onClick = onChangeFolder) {
                        SegmentedTileRow(
                            title = stringResource(R.string.auto_backup_folder_title),
                            description = folderPath ?: stringResource(R.string.auto_backup_choose_folder),
                            trailing = {
                                Icon(
                                    imageVector = MaterialSymbols.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    SegmentedTile(index = 1, count = 2) {
                        SegmentedTileContent {
                            Text(
                                text = stringResource(R.string.auto_backup_kept_backups_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            ConnectedChoiceGroup(
                                options = AutoBackupKeptCounts.map { ChoiceOption(value = it, label = it.toString()) },
                                selected = keptBackups,
                                onSelect = { keptBackups = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val StatusIconId = "status_icon"
private const val StatusIconEm = 1.15f
private val DetailIconSize = 16.dp
private val TileMinHeight = 72.dp
