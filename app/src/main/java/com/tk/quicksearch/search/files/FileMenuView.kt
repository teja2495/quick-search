package com.tk.quicksearch.search.files

import android.provider.OpenableColumns
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.ItemMenuLongPressOption
import com.tk.quicksearch.shared.ui.components.ItemMenuPopup
import com.tk.quicksearch.shared.ui.components.ItemMenuRow
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.itemMenuRemoveOption
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import java.util.concurrent.TimeUnit

private data class FileMenuItem(
        val textResId: Int,
        val textArg: String? = null,
        val icon: @Composable () -> Unit,
        val onClick: () -> Unit,
        val group: FileMenuGroup = FileMenuGroup.ACTIONS,
        val longPressOption: ItemMenuLongPressOption? = null,
)

/** Where an item appears in the long-press [ItemMenuPopup]. */
private enum class FileMenuGroup {
        ACTIONS,
        ROWS,
}

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
    val extension = FileUtils.getFileExtension(deviceFile.displayName)?.uppercase()
    val sdCardLabel = stringResource(R.string.file_path_sd_card)
    val locationText =
            remember(deviceFile, sdCardLabel) {
                FileFolderPathFormatter.format(deviceFile)?.let { path ->
                    val parts = if (path.isSecondaryVolume) listOf(sdCardLabel) + path.segments else path.segments
                    parts.joinToString(" › ")
                }
            }
    // MediaStore DATE_MODIFIED is in seconds.
    val modifiedText =
            remember(deviceFile.lastModified) {
                deviceFile.lastModified.takeIf { it > 0 }?.let {
                    DateUtils.formatDateTime(
                            context,
                            TimeUnit.SECONDS.toMillis(it),
                            DateUtils.FORMAT_SHOW_DATE or
                                    DateUtils.FORMAT_SHOW_TIME or
                                    DateUtils.FORMAT_ABBREV_MONTH,
                    )
                }
            }

    AppAlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FileResultThumbnailOrIcon(
                                deviceFile = deviceFile,
                                iconOverride = null,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                                text = FileUtils.getFileNameWithoutExtension(deviceFile.displayName),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = DesignTokens.SpacingLarge),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge)) {
                        Row {
                            FileInfoSection(
                                    icon = MaterialSymbols.InsertDriveFile,
                                    label = stringResource(R.string.file_info_type),
                                    value = extension ?: "—",
                                    modifier = Modifier.weight(1f),
                            )
                            FileInfoSection(
                                    icon = MaterialSymbols.Storage,
                                    label = stringResource(R.string.file_info_size),
                                    value = fileSizeBytes?.let { formatFileSize(it) } ?: "—",
                                    modifier = Modifier.weight(1f),
                            )
                        }
                        FileInfoSection(
                                icon = MaterialSymbols.Folder,
                                label = stringResource(R.string.file_info_path),
                                value = locationText ?: "—",
                        )
                        FileInfoSection(
                                icon = MaterialSymbols.Schedule,
                                label = stringResource(R.string.file_info_modified),
                                value = modifiedText ?: "—",
                        )
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

@Composable
private fun FileInfoSection(
        icon: ImageVector,
        label: String,
        value: String,
        modifier: Modifier = Modifier,
) {
    Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp).size(20.dp),
        )
        Column {
            Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun FileDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        deviceFile: DeviceFile,
        isPinned: Boolean,
        hasNickname: Boolean,
        hasTrigger: Boolean,
        onTogglePin: () -> Unit,
        onMoveUp: () -> Unit = {},
        onMoveDown: () -> Unit = {},
        onExclude: () -> Unit,
        onExcludeExtension: () -> Unit,
        onNicknameClick: () -> Unit,
        onTriggerClick: () -> Unit,
        /** Clears the nickname or trigger from a long press on its item; null hides that option. */
        onRemoveNickname: (() -> Unit)? = null,
        onRemoveTrigger: (() -> Unit)? = null,
        onOpenFolderClick: () -> Unit = {},
        onFileInfoClick: () -> Unit = {},
        onShareClick: () -> Unit = {},
        onAddToHome: () -> Unit,
        showPinnedItemMenu: Boolean = false,
) {
    val context = LocalContext.current
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
    val fileExtension = FileUtils.getFileExtension(deviceFile.displayName)
    val notificationAction = CustomWidgetButtonAction.File(
            uri = deviceFile.uri.toString(), displayName = deviceFile.displayName, mimeType = deviceFile.mimeType,
            lastModified = deviceFile.lastModified, isDirectory = deviceFile.isDirectory,
            relativePath = deviceFile.relativePath, volumeName = deviceFile.volumeName,
    )
    val isPinnedToNotifications = PinnedNotifications.isPinned(context, notificationAction)
    val openAppSettingDestination = LocalOpenAppSettingDestination.current
    // Removing keeps the menu open, so the menu tracks it until it's next opened.
    var triggerRemoved by remember(expanded) { mutableStateOf(false) }
    var nicknameRemoved by remember(expanded) { mutableStateOf(false) }
    val triggerSet = hasTrigger && !triggerRemoved
    val nicknameSet = hasNickname && !nicknameRemoved
    val removeTriggerOption = itemMenuRemoveOption(
            onRemoveTrigger?.takeIf { triggerSet }?.let { remove -> { triggerRemoved = true; remove() } },
    )
    val removeNicknameOption = itemMenuRemoveOption(
            onRemoveNickname?.takeIf { nicknameSet }?.let { remove -> { nicknameRemoved = true; remove() } },
    )
    val menuItems = buildList {
        if (showPinnedItemMenu && isPinned) {
            add(FileMenuItem(
                    textResId = R.string.action_unpin_app,
                    icon = { Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null, tint = AppColors.ItemMenuActiveIconTint) },
                    onClick = { onDismissRequest(); onTogglePin() },
            ))
            if (!deviceFile.isDirectory) {
                add(FileMenuItem(
                        textResId = R.string.action_share,
                        icon = { Icon(imageVector = MaterialSymbols.Share, contentDescription = null) },
                        onClick = { onDismissRequest(); onShareClick() },
                ))
                add(FileMenuItem(
                        textResId = R.string.action_open_folder,
                        icon = { Icon(imageVector = MaterialSymbols.Folder, contentDescription = null) },
                        onClick = { onDismissRequest(); onOpenFolderClick() },
                ))
            }
            add(FileMenuItem(
                    textResId = R.string.action_move_up,
                    icon = { Icon(imageVector = MaterialSymbols.ArrowUpward, contentDescription = null) },
                    onClick = { onDismissRequest(); onMoveUp() },
            ))
            add(FileMenuItem(
                    textResId = R.string.action_move_down,
                    icon = { Icon(imageVector = MaterialSymbols.ArrowDownward, contentDescription = null) },
                    onClick = { onDismissRequest(); onMoveDown() },
            ))
            return@buildList
        }

        if (!deviceFile.isDirectory) {
            add(FileMenuItem(
                    textResId = R.string.action_share,
                    icon = { Icon(imageVector = MaterialSymbols.Share, contentDescription = null) },
                    onClick = { onDismissRequest(); onShareClick() },
            ))
        }
        add(FileMenuItem(
                textResId = if (isPinned) R.string.action_unpin_app else R.string.action_pin_app,
                icon = {
                    Icon(
                            painter = painterResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin),
                            contentDescription = null,
                            tint = if (isPinned) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                    )
                },
                onClick = { onDismissRequest(); onTogglePin() },
        ))
        add(FileMenuItem(
                textResId = R.string.action_add_trigger,
                icon = { Icon(imageVector = MaterialSymbols.Bolt, contentDescription = null, tint = if (triggerSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current) },
                onClick = { onDismissRequest(); onTriggerClick() },
                longPressOption = removeTriggerOption,
        ))
        add(FileMenuItem(
                textResId = R.string.action_exclude_generic,
                icon = { Icon(imageVector = MaterialSymbols.VisibilityOff, contentDescription = null) },
                onClick = { onDismissRequest(); onExclude() },
        ))

        add(FileMenuItem(
                textResId = R.string.common_nickname,
                icon = { Icon(imageVector = MaterialSymbols.Edit, contentDescription = null, tint = if (nicknameSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current) },
                onClick = { onDismissRequest(); onNicknameClick() },
                group = FileMenuGroup.ROWS,
                longPressOption = removeNicknameOption,
        ))
        if (!deviceFile.isDirectory) {
            add(FileMenuItem(
                    textResId = R.string.action_open_folder,
                    icon = { Icon(imageVector = MaterialSymbols.Folder, contentDescription = null) },
                    onClick = { onDismissRequest(); onOpenFolderClick() },
                    group = FileMenuGroup.ROWS,
            ))
        }
        add(FileMenuItem(
                textResId = if (isPinnedToNotifications) R.string.action_unpin_from_notifications else R.string.action_pin_to_notifications,
                icon = {
                    if (isPinnedToNotifications) {
                        Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null, tint = AppColors.ItemMenuActiveIconTint)
                    } else {
                        Icon(imageVector = MaterialSymbols.PinEnd, contentDescription = null)
                    }
                },
                onClick = {
                    onDismissRequest()
                    PinnedNotifications.toggle(
                        context = context,
                        action = notificationAction,
                    )
                },
                group = FileMenuGroup.ROWS,
        ))
        if (!isDefaultLauncher) {
            add(FileMenuItem(
                    textResId = R.string.action_add_to_home,
                    icon = { Icon(imageVector = MaterialSymbols.Home, contentDescription = null) },
                    onClick = { onDismissRequest(); onAddToHome() },
                    group = FileMenuGroup.ROWS,
            ))
        }
        if (!deviceFile.isDirectory) {
            add(FileMenuItem(
                    textResId = R.string.action_file_info,
                    icon = { Icon(imageVector = MaterialSymbols.Info, contentDescription = null) },
                    onClick = { onDismissRequest(); onFileInfoClick() },
                    group = FileMenuGroup.ROWS,
            ))
        }
        if (fileExtension != null) {
            add(FileMenuItem(
                    textResId = R.string.action_exclude_extension,
                    textArg = fileExtension,
                    icon = { Icon(imageVector = MaterialSymbols.VisibilityOff, contentDescription = null) },
                    onClick = { onDismissRequest(); onExcludeExtension() },
                    group = FileMenuGroup.ROWS,
            ))
        }
        openAppSettingDestination?.let { openDestination ->
            add(FileMenuItem(
                    textResId = R.string.action_files_settings,
                    icon = { Icon(imageVector = MaterialSymbols.Settings, contentDescription = null) },
                    onClick = { onDismissRequest(); openDestination(AppSettingsDestination.FILES) },
                    group = FileMenuGroup.ROWS,
            ))
        }
    }

    if (showPinnedItemMenu && isPinned) {
        AppDropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
        ) {
            menuItems.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                        text = {
                            Text(
                                    text =
                                            if (item.textArg != null) {
                                                stringResource(item.textResId, item.textArg)
                                            } else {
                                                stringResource(item.textResId)
                                            },
                            )
                        },
                        leadingIcon = { item.icon() },
                        onClick = item.onClick,
                )
            }
        }
        return
    }

    val fileIcon = when {
        deviceFile.isDirectory -> MaterialSymbols.Folder
        deviceFile.mimeType?.startsWith("image/") == true -> MaterialSymbols.Image
        deviceFile.mimeType?.startsWith("video/") == true -> MaterialSymbols.VideoLibrary
        deviceFile.mimeType?.startsWith("audio/") == true -> MaterialSymbols.AudioFile
        else -> MaterialSymbols.InsertDriveFile
    }

    if (expanded) {
        val rowsByGroup = menuItems.groupBy { it.group }.mapValues { (_, items) ->
            items.map { item ->
                ItemMenuRow(
                        label = item.textArg?.let { stringResource(item.textResId, it) }
                                ?: stringResource(item.textResId),
                        icon = item.icon,
                        onClick = item.onClick,
                        longPressOption = item.longPressOption,
                )
            }
        }
        ItemMenuPopup(
                onDismiss = onDismissRequest,
                leadingContent = {
                    Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                title = {
                    Text(
                            text = deviceFile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                    )
                },
                shortcutsTitle = stringResource(R.string.app_menu_section_shortcuts),
                actionsTitle = stringResource(R.string.app_menu_section_actions),
                actions = menuItems.filter { it.group == FileMenuGroup.ACTIONS }.map { item ->
                    ItemMenuTile(
                            label = stringResource(item.textResId),
                            icon = item.icon,
                            onClick = item.onClick,
                            longPressOption = item.longPressOption,
                    )
                },
                rows = rowsByGroup[FileMenuGroup.ROWS].orEmpty(),
        )
    }
}
