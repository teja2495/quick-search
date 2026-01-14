package com.tk.quicksearch.settings.excludedItemsScreen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.ui.theme.DesignTokens

// Constants
private val DEFAULT_ICON_SIZE = DesignTokens.IconSize
private val ITEM_ROW_PADDING_HORIZONTAL = DesignTokens.CardHorizontalPadding
private val ITEM_ROW_PADDING_VERTICAL = DesignTokens.CardVerticalPadding
private val SECTION_SPACER_HEIGHT = 4.dp
private val LIST_BOTTOM_PADDING = DesignTokens.VersionBottomPadding
private val ITEM_ROW_ICON_TEXT_SPACING = DesignTokens.ItemRowSpacing
private val ITEM_ROW_TEXT_VERTICAL_PADDING = 2.dp

@Composable
fun ExcludedItemScreen(
    suggestionExcludedApps: List<AppInfo>,
    resultExcludedApps: List<AppInfo>,
    excludedContacts: List<ContactInfo>,
    excludedFiles: List<DeviceFile>,
    excludedFileExtensions: Set<String>,
    onRemoveSuggestionExcludedApp: (AppInfo) -> Unit,
    onRemoveResultExcludedApp: (AppInfo) -> Unit,
    onRemoveExcludedContact: (ContactInfo) -> Unit,
    onRemoveExcludedFile: (DeviceFile) -> Unit,
    onRemoveExcludedFileExtension: (String) -> Unit,
    excludedSettings: List<DeviceSetting>,
    onRemoveExcludedSetting: (DeviceSetting) -> Unit,
    onClearAll: () -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
    iconPackPackage: String? = null
) {
    val allItems = remember(suggestionExcludedApps, resultExcludedApps, excludedContacts, excludedFiles, excludedFileExtensions, excludedSettings) {
        (suggestionExcludedApps.map { ExcludedItem.SuggestionApp(it) } +
         resultExcludedApps.map { ExcludedItem.ResultApp(it) } +
         excludedContacts.map { ExcludedItem.Contact(it) } +
         excludedFiles.map { ExcludedItem.File(it) } +
         excludedFileExtensions.map { ExcludedItem.FileExtension(it) } +
         excludedSettings.map { ExcludedItem.Setting(it) })
            .sortedBy { it.displayName.lowercase() }
    }
    
    if (allItems.isEmpty()) {
        return
    }

    Column {
        // Header with title and description
        if (showTitle) {
            Text(
                text = stringResource(R.string.settings_excluded_items_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
            )

            Text(
                text = stringResource(R.string.settings_excluded_items_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
            )
        } else {
            Text(
                text = stringResource(R.string.settings_excluded_items_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
            )
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
                iconPackPackage = iconPackPackage,
                onRemoveItem = { item ->
                    when (item) {
                        is ExcludedItem.SuggestionApp -> onRemoveSuggestionExcludedApp(item.appInfo)
                        is ExcludedItem.ResultApp -> onRemoveResultExcludedApp(item.appInfo)
                        is ExcludedItem.Contact -> onRemoveExcludedContact(item.contactInfo)
                        is ExcludedItem.File -> onRemoveExcludedFile(item.deviceFile)
                        is ExcludedItem.FileExtension -> onRemoveExcludedFileExtension(item.extension)
                        is ExcludedItem.Setting -> onRemoveExcludedSetting(item.setting)
                    }
                }
            )
        }
    }
}

@Composable
private fun ExcludedItemsList(
    allItems: List<ExcludedItem>,
    iconPackPackage: String?,
    onRemoveItem: (ExcludedItem) -> Unit
) {
    Column {
        allItems.forEachIndexed { index, item ->
            ExcludedItemRow(
                item = item,
                iconPackPackage = iconPackPackage,
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
    iconPackPackage: String?,
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
            horizontalArrangement = Arrangement.spacedBy(ITEM_ROW_ICON_TEXT_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExcludedItemIcon(
                item = item,
                iconPackPackage = iconPackPackage
            )
            
            Column(modifier = Modifier.padding(vertical = ITEM_ROW_TEXT_VERTICAL_PADDING)) {
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
private fun ExcludedItemIcon(
    item: ExcludedItem,
    iconPackPackage: String?
) {
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
        is ExcludedItem.FileExtension -> {
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
        is ExcludedItem.SuggestionApp -> {
            AppIconPlaceholder(appInfo = item.appInfo, iconPackPackage = iconPackPackage)
        }
        is ExcludedItem.ResultApp -> {
            AppIconPlaceholder(appInfo = item.appInfo, iconPackPackage = iconPackPackage)
        }
    }
}

@Composable
private fun AppIconPlaceholder(
    appInfo: AppInfo,
    iconPackPackage: String?
) {
    val iconBitmap = rememberAppIcon(
        packageName = appInfo.packageName,
        iconPackPackage = iconPackPackage
    )
    
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

    data class SuggestionApp(val appInfo: AppInfo) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabelRes: Int = R.string.excluded_item_type_app_suggestions
    }

    data class ResultApp(val appInfo: AppInfo) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabelRes: Int = R.string.excluded_item_type_app_results
    }

    data class Contact(val contactInfo: ContactInfo) : ExcludedItem() {
        override val displayName: String = contactInfo.displayName
        override val typeLabelRes: Int = R.string.excluded_item_type_contact
    }

    data class File(val deviceFile: DeviceFile) : ExcludedItem() {
        override val displayName: String = deviceFile.displayName
        override val typeLabelRes: Int = R.string.excluded_item_type_file
    }

    data class FileExtension(val extension: String) : ExcludedItem() {
        override val displayName: String = ".$extension"
        override val typeLabelRes: Int = R.string.excluded_item_type_file_extension
    }

    data class Setting(val setting: DeviceSetting) : ExcludedItem() {
        override val displayName: String = setting.title
        override val typeLabelRes: Int = R.string.excluded_item_type_setting
    }
}
