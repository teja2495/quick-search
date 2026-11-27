package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.lazy.rememberLazyListState
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.AppTextButton

private val DEFAULT_ICON_SIZE = DesignTokens.IconSize
private val SECTION_SPACER_HEIGHT = 4.dp

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
    excludedAppShortcuts: List<StaticShortcut>,
    onRemoveExcludedAppShortcut: (StaticShortcut) -> Unit,
    excludedOtherItems: List<OtherSearchItemId> = emptyList(),
    onRemoveExcludedOtherItem: (OtherSearchItemId) -> Unit = {},
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
    iconPackPackage: String? = null,
) {
    val otherItemTitles = excludedOtherItems.map { stringResource(it.titleRes) }
    val allItems =
        remember(
            suggestionExcludedApps,
            resultExcludedApps,
            excludedContacts,
            excludedFiles,
            excludedFileExtensions,
            excludedSettings,
            excludedAppShortcuts,
            excludedOtherItems,
            otherItemTitles,
        ) {
            (
                suggestionExcludedApps.map { ExcludedItem.SuggestionApp(it) } +
                    resultExcludedApps.map { ExcludedItem.ResultApp(it) } +
                    excludedContacts.map { ExcludedItem.Contact(it) } +
                    excludedFiles.map { ExcludedItem.File(it) } +
                    excludedFileExtensions.map { ExcludedItem.FileExtension(it) } +
                    excludedSettings.map { ExcludedItem.Setting(it) } +
                    excludedAppShortcuts.map { ExcludedItem.AppShortcut(it) } +
                    excludedOtherItems.zip(otherItemTitles) { item, title -> ExcludedItem.Other(item, title) }
            ).sortedBy { it.displayName.lowercase() }
        }

    if (allItems.isEmpty()) {
        return
    }

    Column(modifier = modifier) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.settings_excluded_items_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
            )

            Text(
                text = stringResource(R.string.settings_excluded_items_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
            )
        } else {
            Text(
                text = stringResource(R.string.settings_excluded_items_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
            )
        }

        Spacer(modifier = Modifier.height(SECTION_SPACER_HEIGHT))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            ExcludedItemsList(
                allItems = allItems,
                iconPackPackage = iconPackPackage,
                onRemoveItem = { item ->
                    when (item) {
                        is ExcludedItem.SuggestionApp -> {
                            onRemoveSuggestionExcludedApp(item.appInfo)
                        }

                        is ExcludedItem.ResultApp -> {
                            onRemoveResultExcludedApp(item.appInfo)
                        }

                        is ExcludedItem.Contact -> {
                            onRemoveExcludedContact(item.contactInfo)
                        }

                        is ExcludedItem.File -> {
                            onRemoveExcludedFile(item.deviceFile)
                        }

                        is ExcludedItem.FileExtension -> {
                            onRemoveExcludedFileExtension(item.extension)
                        }

                        is ExcludedItem.Setting -> {
                            onRemoveExcludedSetting(item.setting)
                        }

                        is ExcludedItem.AppShortcut -> {
                            onRemoveExcludedAppShortcut(item.shortcut)
                        }

                        is ExcludedItem.Other -> {
                            onRemoveExcludedOtherItem(item.itemId)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ExcludedItemsList(
    allItems: List<ExcludedItem>,
    iconPackPackage: String?,
    onRemoveItem: (ExcludedItem) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().settingsScrollClip(listState),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
    ) {
        itemsIndexed(allItems) { index, item ->
            SegmentedTile(index = index, count = allItems.size) {
                ExcludedItemRow(
                    item = item,
                    iconPackPackage = iconPackPackage,
                    onRemove = { onRemoveItem(item) },
                )
            }
        }
    }
}

@Composable
private fun ExcludedItemRow(
    item: ExcludedItem,
    iconPackPackage: String?,
    onRemove: () -> Unit,
) {
    SegmentedTileRow(
        title = item.displayName,
        description = stringResource(item.typeLabelRes),
        leading = {
            Box(
                modifier = Modifier.size(DesignTokens.Spacing40)
                    .clip(AppShapes.full)
                    .background(AppColors.AccentTintTonal),
                contentAlignment = Alignment.Center,
            ) {
                ExcludedItemIcon(item = item, iconPackPackage = iconPackPackage)
            }
        },
        trailing = {
            SettingsRemoveButton(onClick = onRemove)
        },
    )
}

@Composable
private fun ExcludedItemIcon(
    item: ExcludedItem,
    iconPackPackage: String?,
) {
    when (item) {
        is ExcludedItem.Contact -> {
            Icon(
                imageVector = MaterialSymbols.Person,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(DEFAULT_ICON_SIZE),
            )
        }

        is ExcludedItem.File -> {
            Icon(
                imageVector =
                    if (item.deviceFile.isDirectory) {
                        MaterialSymbols.Folder
                    } else {
                        MaterialSymbols.InsertDriveFile
                    },
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(DEFAULT_ICON_SIZE),
            )
        }

        is ExcludedItem.FileExtension -> {
            Icon(
                imageVector = MaterialSymbols.InsertDriveFile,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(DEFAULT_ICON_SIZE),
            )
        }

        is ExcludedItem.Setting -> {
            Icon(
                imageVector = MaterialSymbols.Settings,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(DEFAULT_ICON_SIZE),
            )
        }

        is ExcludedItem.Other -> {
            Icon(
                imageVector = MaterialSymbols.Timer,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(DEFAULT_ICON_SIZE),
            )
        }

        is ExcludedItem.AppShortcut -> {
            AppShortcutIconPlaceholder(shortcut = item.shortcut, iconPackPackage = iconPackPackage)
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
private fun AppShortcutIconPlaceholder(
    shortcut: StaticShortcut,
    iconPackPackage: String?,
) {
    val iconResult =
        rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)

    if (iconResult.bitmap != null) {
        Image(
            bitmap = iconResult.bitmap,
            contentDescription = null,
            modifier = Modifier.size(DEFAULT_ICON_SIZE),
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Apps,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(DEFAULT_ICON_SIZE),
        )
    }
}

@Composable
private fun AppIconPlaceholder(
    appInfo: AppInfo,
    iconPackPackage: String?,
) {
    val iconResult =
        rememberAppIcon(
            packageName = appInfo.packageName,
            iconPackPackage = iconPackPackage,
            userHandleId = appInfo.userHandleId,
        )

    if (iconResult.bitmap != null) {
        Image(
            bitmap = iconResult.bitmap,
            contentDescription = null,
            modifier = Modifier.size(DEFAULT_ICON_SIZE),
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Apps,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(DEFAULT_ICON_SIZE),
        )
    }
}

@Composable
fun ClearAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_excluded_items_clear_all)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.settings_excluded_items_clear_all_confirmation,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            AppTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_action_clear_all))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

private sealed class ExcludedItem {
    abstract val displayName: String
    abstract val typeLabelRes: Int

    data class SuggestionApp(
        val appInfo: AppInfo,
    ) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabelRes: Int = R.string.excluded_item_type_app_suggestions
    }

    data class ResultApp(
        val appInfo: AppInfo,
    ) : ExcludedItem() {
        override val displayName: String = appInfo.appName
        override val typeLabelRes: Int = R.string.excluded_item_type_app_results
    }

    data class Contact(
        val contactInfo: ContactInfo,
    ) : ExcludedItem() {
        override val displayName: String = contactInfo.displayName
        override val typeLabelRes: Int = R.string.notification_pinned_item_type_contact
    }

    data class File(
        val deviceFile: DeviceFile,
    ) : ExcludedItem() {
        override val displayName: String = deviceFile.displayName
        override val typeLabelRes: Int
            get() =
                if (deviceFile.isDirectory) {
                    R.string.excluded_item_type_folder
                } else {
                    R.string.notification_pinned_item_type_file
                }
    }

    data class FileExtension(
        val extension: String,
    ) : ExcludedItem() {
        override val displayName: String = ".$extension"
        override val typeLabelRes: Int = R.string.excluded_item_type_file_extension
    }

    data class Setting(
        val setting: DeviceSetting,
    ) : ExcludedItem() {
        override val displayName: String = setting.title
        override val typeLabelRes: Int = R.string.excluded_item_type_setting
    }

    data class AppShortcut(
        val shortcut: StaticShortcut,
    ) : ExcludedItem() {
        override val displayName: String = shortcutDisplayName(shortcut)
        override val typeLabelRes: Int = R.string.excluded_item_type_app_shortcut
    }

    data class Other(
        val itemId: OtherSearchItemId,
        override val displayName: String,
    ) : ExcludedItem() {
        override val typeLabelRes: Int = R.string.excluded_item_type_search_result
    }
}
