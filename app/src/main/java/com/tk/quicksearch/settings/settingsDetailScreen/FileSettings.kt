package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.components.AppInputChip
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.BooleanSettings
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

private val multiSlashRegex = "/+".toRegex()

internal fun normalizePathFilterPattern(rawPattern: String): String? {
    val trimmed = rawPattern.trim()
    if (trimmed.isBlank()) return null

    val normalizedPath =
        trimmed
            .replace('\\', '/')
            .replace(multiSlashRegex, "/")
            .removePrefix("*/")
            .removePrefix("/")
            .removeSuffix("/*")
            .trim('/')
            .trim()

    if (normalizedPath.isBlank()) return null
    return "*/$normalizedPath/*"
}

private fun patternDisplayPath(pattern: String): String =
    pattern
        .removePrefix("*/")
        .removeSuffix("/*")
        .trim('/')

internal fun displayPathWithLeadingSlash(path: String): String =
    path.trim('/').let { trimmedPath ->
        if (trimmedPath.isBlank()) "/" else "/$trimmedPath"
    }

internal fun folderDisplayPath(folder: DeviceFile): String =
    listOfNotNull(folder.relativePath?.trim('/'), folder.displayName.trim())
        .filter { it.isNotBlank() }
        .joinToString("/")

/** Gets the display name for a file type. */
@Composable
private fun getFileTypeDisplayName(fileType: FileType): String =
    when (fileType) {
        FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
        FileType.PICTURES -> stringResource(R.string.file_type_pictures)
        FileType.VIDEOS -> stringResource(R.string.file_type_videos)
        FileType.AUDIO -> stringResource(R.string.file_type_audio)
        FileType.APKS -> stringResource(R.string.file_type_apks)
        FileType.OTHER -> stringResource(R.string.contact_method_fallback_label)
    }

/** Gets the icon for a file type. */
private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector =
    when (fileType) {
        FileType.DOCUMENTS -> MaterialSymbols.InsertDriveFile
        FileType.PICTURES -> MaterialSymbols.Image
        FileType.VIDEOS -> MaterialSymbols.VideoLibrary
        FileType.AUDIO -> MaterialSymbols.AudioFile
        FileType.APKS -> MaterialSymbols.Android
        FileType.OTHER -> MaterialSymbols.InsertDriveFile
    }

/**
 * Settings section for configuring which files and folders are included in search results.
 *
 * @param enabledFileTypes Set of currently enabled file types
 * @param onToggleFileType Callback when a file type toggle is changed
 * @param showFolders Whether folders are shown in search results
 * @param onToggleFolders Callback when folders toggle is changed
 * @param showSystemFiles Whether system files are shown in search results
 * @param onToggleSystemFiles Callback when system files toggle is changed
 * @param folderWhitelistPatterns Folder path patterns that should be allowed in results
 * @param onSetFolderWhitelistPatterns Callback when whitelist patterns change
 * @param folderBlacklistPatterns Folder path patterns that should be blocked in results
 * @param onSetFolderBlacklistPatterns Callback when blacklist patterns change
 * @param excludedExtensions Set of excluded file extensions
 * @param onRemoveExcludedExtension Callback when an excluded extension should be removed
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun FileTypesSection(
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    showFolders: Boolean,
    onToggleFolders: (Boolean) -> Unit,
    booleanSettings: Map<String, Boolean>,
    onApplySettingsCommand: (SettingsCommand) -> Unit,
    showSystemFiles: Boolean,
    onToggleSystemFiles: (Boolean) -> Unit,
    folderWhitelistPatterns: Set<String>,
    onSetFolderWhitelistPatterns: (Set<String>) -> Unit,
    folderBlacklistPatterns: Set<String>,
    onSetFolderBlacklistPatterns: (Set<String>) -> Unit,
    excludedExtensions: Set<String>,
    onRemoveExcludedExtension: (String) -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val orderedFileTypes = listOf(
        FileType.DOCUMENTS,
        FileType.PICTURES,
        FileType.VIDEOS,
        FileType.AUDIO,
        FileType.APKS,
        FileType.OTHER,
    )
    SegmentedGroup(
        modifier = modifier,
        title = if (showTitle) stringResource(R.string.settings_file_types_title) else null,
    ) {
        BooleanSettingTile(
            setting = BooleanSettings.FILE_PREVIEWS.setting,
            values = booleanSettings,
            onCommand = onApplySettingsCommand,
            icon = MaterialSymbols.Visibility,
            index = 0,
            count = 1,
        )
    }

    Spacer(modifier = Modifier.height(DesignTokens.SpacingMedium))

    SegmentedGroup {
        val count = orderedFileTypes.size + 2
        SegmentedToggleTile(
            title = stringResource(R.string.settings_folders_toggle),
            checked = showFolders,
            onCheckedChange = onToggleFolders,
            icon = MaterialSymbols.Folder,
            index = 0,
            count = count,
        )
        orderedFileTypes.forEachIndexed { index, fileType ->
            SegmentedToggleTile(
                title = getFileTypeDisplayName(fileType),
                checked = fileType in enabledFileTypes,
                onCheckedChange = { onToggleFileType(fileType, it) },
                icon = getFileTypeIcon(fileType),
                index = index + 1,
                count = count,
            )
        }
        SegmentedToggleTile(
            title = stringResource(R.string.settings_system_files_toggle),
            checked = showSystemFiles,
            onCheckedChange = onToggleSystemFiles,
            icon = MaterialSymbols.Visibility,
            index = count - 1,
            count = count,
        )
    }

    if (excludedExtensions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingMedium))
        SegmentedGroup {
            SegmentedTile(index = 0, count = 1) {
                SegmentedTileContent {
                    Text(
                        text = stringResource(R.string.settings_excluded_extensions_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        excludedExtensions.sorted().forEach { extension ->
                            AppInputChip(
                                label = ".$extension",
                                onRemove = { onRemoveExcludedExtension(extension) },
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(DesignTokens.SpacingMedium))

    var activeFolderFilterTarget by remember { mutableStateOf<FolderFilterTarget?>(null) }

    activeFolderFilterTarget?.let { target ->
        FolderSearchSheet(
            title =
                when (target) {
                    FolderFilterTarget.Whitelist -> stringResource(R.string.settings_folder_whitelist_add)
                    FolderFilterTarget.Blacklist -> stringResource(R.string.settings_folder_blacklist_add)
                },
            selectedPatterns =
                when (target) {
                    FolderFilterTarget.Whitelist -> folderWhitelistPatterns
                    FolderFilterTarget.Blacklist -> folderBlacklistPatterns
                },
            onDismiss = { activeFolderFilterTarget = null },
            onSelect = { folder ->
                val pattern = normalizePathFilterPattern(folderDisplayPath(folder)) ?: return@FolderSearchSheet
                when (target) {
                    FolderFilterTarget.Whitelist ->
                        onSetFolderWhitelistPatterns(folderWhitelistPatterns + pattern)
                    FolderFilterTarget.Blacklist ->
                        onSetFolderBlacklistPatterns(folderBlacklistPatterns + pattern)
                }
                activeFolderFilterTarget = null
            },
        )
    }

    SegmentedTile(index = 0, count = 1) {
        Column {
            SegmentedTileContent {
                Text(
                    text = stringResource(R.string.settings_folder_filters_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.settings_folder_filter_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = AppColors.SettingsDivider)
            FolderFilterGroup(
                title = stringResource(R.string.settings_folder_whitelist_title),
                description = stringResource(R.string.settings_folder_whitelist_helper),
                addDescription = stringResource(R.string.settings_folder_whitelist_add),
                patterns = folderWhitelistPatterns,
                onAdd = { activeFolderFilterTarget = FolderFilterTarget.Whitelist },
                onRemove = { onSetFolderWhitelistPatterns(folderWhitelistPatterns - it) },
            )
            HorizontalDivider(color = AppColors.SettingsDivider)
            FolderFilterGroup(
                title = stringResource(R.string.settings_folder_blacklist_title),
                description = stringResource(R.string.settings_folder_blacklist_helper),
                addDescription = stringResource(R.string.settings_folder_blacklist_add),
                patterns = folderBlacklistPatterns,
                onAdd = { activeFolderFilterTarget = FolderFilterTarget.Blacklist },
                onRemove = { onSetFolderBlacklistPatterns(folderBlacklistPatterns - it) },
            )
        }
    }

}

private enum class FolderFilterTarget {
    Whitelist,
    Blacklist,
}

@Composable
private fun FolderFilterGroup(
    title: String,
    description: String,
    addDescription: String,
    patterns: Set<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val sortedPatterns = patterns.sortedBy(::patternDisplayPath)
    Column {
        SegmentedTileRow(
            title = title,
            description = description,
            verticalAlignment = Alignment.CenterVertically,
            leading = { TonalIconBadge(icon = MaterialSymbols.Folder) },
            trailing = {
                AppIconButton(onClick = onAdd, tooltip = addDescription, style = AppIconButtonStyle.Tonal) {
                    Icon(
                        imageVector = MaterialSymbols.Add,
                        contentDescription = addDescription,
                    )
                }
            },
        )
        sortedPatterns.forEach { pattern ->
            val path = patternDisplayPath(pattern)
            SegmentedTileRow(
                title = path.substringAfterLast('/', path),
                description = displayPathWithLeadingSlash(path),
                leading = { TonalIconBadge(icon = MaterialSymbols.Folder) },
                trailing = {
                    AppIconButton(onClick = { onRemove(pattern) }, tooltip = stringResource(R.string.action_remove)) {
                        Icon(
                            imageVector = MaterialSymbols.DeleteOutline,
                            contentDescription = stringResource(R.string.action_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}
