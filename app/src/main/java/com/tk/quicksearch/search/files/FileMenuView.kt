package com.tk.quicksearch.search.files

import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Menu item data class for file dropdown menu. */
private data class FileMenuItem(
        val textResId: Int,
        val icon: @Composable () -> Unit,
        val onClick: () -> Unit,
)

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
fun FileInfoDialog(
        deviceFile: DeviceFile,
        onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var fileSizeBytes by remember(deviceFile.uri) { mutableStateOf<Long?>(null) }
    LaunchedEffect(deviceFile.uri) {
        fileSizeBytes =
                context.contentResolver.query(
                                deviceFile.uri,
                                null,
                                null,
                                null,
                                null,
                        )
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (index != -1) cursor.getLong(index) else null
                            } else null
                        }
    }
    val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
    val modifiedText = dateFormat.format(Date(deviceFile.lastModified))
    val scrollState = rememberScrollState()
    val fileTypeText = FileUtils.getFileExtension(deviceFile.displayName) ?: "—"
    val sizeText = fileSizeBytes?.let { formatFileSize(it) } ?: "—"

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = deviceFile.displayName) },
            text = {
                Column(
                        modifier = Modifier.verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                                    stringResource(R.string.file_info_type) to
                                            (deviceFile.mimeType ?: "—"),
                                    stringResource(R.string.file_info_file_type) to fileTypeText,
                                    stringResource(R.string.file_info_size) to sizeText,
                                    stringResource(R.string.file_info_modified) to modifiedText,
                                    stringResource(R.string.file_info_path) to
                                            (deviceFile.relativePath ?: "—"),
                            )
                            .forEach { (label, value) ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.dialog_okay))
                }
            },
    )
}

/**
 * Dropdown menu for file result rows with actions like pin/unpin, nickname, exclude, and exclude
 * extension.
 */
@Composable
fun FileDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        deviceFile: DeviceFile,
        isPinned: Boolean,
        hasNickname: Boolean,
        onTogglePin: () -> Unit,
        onExclude: () -> Unit,
        onExcludeExtension: () -> Unit,
        onNicknameClick: () -> Unit,
        onOpenFolderClick: () -> Unit = {},
        onFileInfoClick: () -> Unit = {},
        onAddToHome: () -> Unit,
) {
    DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = AppColors.DialogBackground,
    ) {
        val menuItems = buildList {
            add(
                    FileMenuItem(
                            textResId =
                                    if (isPinned) R.string.action_unpin_generic
                                    else R.string.action_pin_generic,
                            icon = {
                                Icon(
                                        painter =
                                                painterResource(
                                                        if (isPinned) R.drawable.ic_unpin
                                                        else R.drawable.ic_pin,
                                                ),
                                        contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismissRequest()
                                onTogglePin()
                            },
                    ),
            )
            add(
                    FileMenuItem(
                            textResId = R.string.action_add_to_home,
                            icon = {
                                Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                            },
                            onClick = {
                                onDismissRequest()
                                onAddToHome()
                            },
                    ),
            )
            add(
                    FileMenuItem(
                            textResId =
                                    if (hasNickname) R.string.action_edit_nickname
                                    else R.string.action_add_nickname,
                            icon = {
                                Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                            },
                            onClick = {
                                onDismissRequest()
                                onNicknameClick()
                            },
                    ),
            )
            add(
                    FileMenuItem(
                            textResId = R.string.action_exclude_generic,
                            icon = {
                                Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null
                                )
                            },
                            onClick = {
                                onDismissRequest()
                                onExclude()
                            },
                    ),
            )

            val fileExtension = FileUtils.getFileExtension(deviceFile.displayName)
            if (fileExtension != null) {
                add(
                        FileMenuItem(
                                textResId = R.string.action_exclude_extension,
                                icon = {
                                    Icon(
                                            imageVector = Icons.Rounded.VisibilityOff,
                                            contentDescription = null
                                    )
                                },
                                onClick = {
                                    onDismissRequest()
                                    onExcludeExtension()
                                },
                        ),
                )
            }
            if (!deviceFile.isDirectory) {
                add(
                        FileMenuItem(
                                textResId = R.string.action_open_folder,
                                icon = {
                                    Icon(
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null
                                    )
                                },
                                onClick = {
                                    onDismissRequest()
                                    onOpenFolderClick()
                                },
                        ),
                )
                add(
                        FileMenuItem(
                                textResId = R.string.action_file_info,
                                icon = {
                                    Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = null
                                    )
                                },
                                onClick = {
                                    onDismissRequest()
                                    onFileInfoClick()
                                },
                        ),
                )
            }
        }

        menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                    text = {
                        Text(
                                text =
                                        stringResource(
                                                item.textResId,
                                                FileUtils.getFileExtension(deviceFile.displayName)
                                                        ?: ""
                                        )
                        )
                    },
                    leadingIcon = { item.icon() },
                    onClick = item.onClick,
            )
        }
    }
}
