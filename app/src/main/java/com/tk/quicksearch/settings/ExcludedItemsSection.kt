package com.tk.quicksearch.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.rememberAppIcon
import com.tk.quicksearch.settings.SettingsSpacing

// Constants
private val DEFAULT_ICON_SIZE = 24.dp
private val ITEM_ROW_PADDING_HORIZONTAL = 16.dp
private val ITEM_ROW_PADDING_VERTICAL = 12.dp
private val SECTION_SPACER_HEIGHT = 4.dp
private val LIST_BOTTOM_PADDING = 80.dp

@Composable
fun ExcludedItemsSection(
    hiddenApps: List<AppInfo>,
    excludedContacts: List<ContactInfo>,
    excludedFiles: List<DeviceFile>,
    onRemoveExcludedApp: (AppInfo) -> Unit,
    onRemoveExcludedContact: (ContactInfo) -> Unit,
    onRemoveExcludedFile: (DeviceFile) -> Unit,
    excludedSettings: List<SettingShortcut>,
    onRemoveExcludedSetting: (SettingShortcut) -> Unit,
    onClearAll: () -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val allItems = remember(hiddenApps, excludedContacts, excludedFiles, excludedSettings) {
        (hiddenApps.map { ExcludedItem.App(it) } +
         excludedContacts.map { ExcludedItem.Contact(it) } +
         excludedFiles.map { ExcludedItem.File(it) } +
         excludedSettings.map { ExcludedItem.Setting(it) })
            .sortedBy { it.displayName.lowercase() }
    }
    
    if (allItems.isEmpty()) {
        return
    }

    var showClearAllConfirmation by remember { mutableStateOf(false) }

    Column {
        // Header with title and description
        Column(modifier = modifier.fillMaxWidth()) {
            if (showTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_excluded_items_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = stringResource(R.string.settings_excluded_items_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(
                            top = SettingsSpacing.sectionTitleBottomPadding,
                            bottom = SettingsSpacing.sectionDescriptionBottomPadding
                        )
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_excluded_items_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(SECTION_SPACER_HEIGHT))
        
        // Items card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LIST_BOTTOM_PADDING),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            ExcludedItemsList(
                allItems = allItems,
                onRemoveItem = { item ->
                    when (item) {
                        is ExcludedItem.App -> onRemoveExcludedApp(item.appInfo)
                        is ExcludedItem.Contact -> onRemoveExcludedContact(item.contactInfo)
                        is ExcludedItem.File -> onRemoveExcludedFile(item.deviceFile)
                        is ExcludedItem.Setting -> onRemoveExcludedSetting(item.setting)
                    }
                }
            )
        }

        // Clear All confirmation dialog
        if (showClearAllConfirmation) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    onClearAll()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false }
            )
        }
    }
}

@Composable
private fun ExcludedItemsList(
    allItems: List<ExcludedItem>,
    onRemoveItem: (ExcludedItem) -> Unit
) {
    Column {
        allItems.forEachIndexed { index, item ->
            ExcludedItemRow(
                item = item,
                onRemove = { onRemoveItem(item) }
            )
            if (index < allItems.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ExcludedItemRow(
    item: ExcludedItem,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ITEM_ROW_PADDING_HORIZONTAL,
                vertical = ITEM_ROW_PADDING_VERTICAL
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExcludedItemIcon(item = item)
            
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
                Text(
                    text = stringResource(item.typeLabelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.settings_action_remove),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ExcludedItemIcon(item: ExcludedItem) {
    when (item) {
        is ExcludedItem.Contact -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DEFAULT_ICON_SIZE)
            )
        }
        is ExcludedItem.File -> {
            Icon(
                imageVector = Icons.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DEFAULT_ICON_SIZE)
            )
        }
        is ExcludedItem.Setting -> {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DEFAULT_ICON_SIZE)
            )
        }
        is ExcludedItem.App -> {
            AppIconPlaceholder(appInfo = item.appInfo)
        }
    }
}

@Composable
private fun AppIconPlaceholder(appInfo: AppInfo) {
    val iconBitmap = rememberAppIcon(appInfo.packageName)
    
    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier.size(DEFAULT_ICON_SIZE),
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback placeholder if icon can't be loaded
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DEFAULT_ICON_SIZE)
        )
    }
}

@Composable
fun ClearAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_excluded_items_clear_all))
        },
        text = {
            Text(
                text = stringResource(R.string.settings_excluded_items_clear_all_confirmation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onConfirm) {
                Text(stringResource(R.string.settings_action_clear_all))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

private sealed class ExcludedItem {
    abstract val displayName: String
    abstract val typeLabelRes: Int

    data class App(val appInfo: AppInfo) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabelRes: Int = R.string.excluded_item_type_app
    }

    data class Contact(val contactInfo: ContactInfo) : ExcludedItem() {
        override val displayName: String = contactInfo.displayName
        override val typeLabelRes: Int = R.string.excluded_item_type_contact
    }

    data class File(val deviceFile: DeviceFile) : ExcludedItem() {
        override val displayName: String = deviceFile.displayName
        override val typeLabelRes: Int = R.string.excluded_item_type_file
    }

    data class Setting(val setting: SettingShortcut) : ExcludedItem() {
        override val displayName: String = setting.title
        override val typeLabelRes: Int = R.string.excluded_item_type_setting
    }
}
