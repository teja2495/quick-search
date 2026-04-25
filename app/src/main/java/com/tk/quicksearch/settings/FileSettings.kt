package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

// Constants for consistent spacing
private object FileTypesSpacing {
    val cardHorizontalPadding = DesignTokens.CardHorizontalPadding
    val cardVerticalPadding = DesignTokens.CardVerticalPadding
    val chipButtonSize = 16.dp
    val chipIconSize = 12.dp
    val excludedExtensionsTitleBottomPadding = DesignTokens.SectionTitleBottomPadding
    val excludedExtensionsChipSpacing = 8.dp
    val pathFilterCardSpacing = 12.dp
    val pathFilterTitleTopSpacing = 4.dp
    val blacklistTitleTopSpacing = 8.dp
    val pathFilterDescriptionBottomSpacing = 6.dp
    val folderDialogMaxHeight = 320.dp
}

private val multiSlashRegex = "/+".toRegex()

private fun normalizePathFilterPattern(rawPattern: String): String? {
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

private fun displayPathWithLeadingSlash(path: String): String =
    path.trim('/').let { trimmedPath ->
        if (trimmedPath.isBlank()) "/" else "/$trimmedPath"
    }

private fun folderDisplayPath(folder: DeviceFile): String =
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
        FileType.DOCUMENTS -> Icons.AutoMirrored.Rounded.InsertDriveFile
        FileType.PICTURES -> Icons.Rounded.Image
        FileType.VIDEOS -> Icons.Rounded.VideoLibrary
        FileType.AUDIO -> Icons.Rounded.AudioFile
        FileType.APKS -> Icons.Rounded.Android
        FileType.OTHER -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }

/** Small chip component for displaying excluded file extensions with remove functionality. */
@Composable
private fun ExcludedExtensionChip(
    extension: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.AssistChip(
        onClick = { /* No click action, only the X button works */ },
        label = {
            Text(
                text = ".$extension",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(FileTypesSpacing.chipButtonSize),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(FileTypesSpacing.chipIconSize),
                )
            }
        },
        modifier = modifier,
        colors =
            androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
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
    // Section title
    if (showTitle) {
        Column(modifier = modifier) {
            Text(
                text = stringResource(R.string.settings_file_types_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        bottom = DesignTokens.SectionTitleBottomPadding,
                    ),
            )
        }
    }

    // Card 1: Folders toggle
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        SettingsToggleRow(
            title = stringResource(R.string.settings_folders_toggle),
            checked = showFolders,
            onCheckedChange = onToggleFolders,
            leadingIcon = Icons.Rounded.Folder,
            isFirstItem = true,
            isLastItem = true,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Card 2: File types with icons in specific order
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Define the order: Documents, Pictures, Videos, Music, APKs, Other
            val orderedFileTypes =
                listOf(
                    FileType.DOCUMENTS,
                    FileType.PICTURES,
                    FileType.VIDEOS,
                    FileType.AUDIO,
                    FileType.APKS,
                    FileType.OTHER,
                )

            orderedFileTypes.forEachIndexed { index, fileType ->
                SettingsToggleRow(
                    title = getFileTypeDisplayName(fileType),
                    checked = fileType in enabledFileTypes,
                    onCheckedChange = { enabled ->
                        onToggleFileType(fileType, enabled)
                    },
                    leadingIcon = getFileTypeIcon(fileType),
                    isFirstItem = index == 0,
                    isLastItem = false,
                )
            }

            SettingsToggleRow(
                title = stringResource(R.string.settings_system_files_toggle),
                checked = showSystemFiles,
                onCheckedChange = onToggleSystemFiles,
                leadingIcon = Icons.Rounded.Visibility,
                isFirstItem = false,
                isLastItem = excludedExtensions.isEmpty(),
            )

            // Excluded extensions section
            if (excludedExtensions.isNotEmpty()) {
                HorizontalDivider(color = AppColors.SettingsDivider)

                Spacer(
                    modifier =
                        Modifier.height(
                            FileTypesSpacing.cardVerticalPadding,
                        ),
                )

                // Excluded extensions header
                Text(
                    text =
                        stringResource(
                            R.string.settings_excluded_extensions_title,
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier.padding(
                            start =
                                FileTypesSpacing
                                    .cardHorizontalPadding,
                            top = FileTypesSpacing.cardVerticalPadding,
                            end =
                                FileTypesSpacing
                                    .cardHorizontalPadding,
                            bottom =
                                FileTypesSpacing
                                    .excludedExtensionsTitleBottomPadding,
                        ),
                )

                // Excluded extensions chips
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start =
                                    FileTypesSpacing
                                        .cardHorizontalPadding,
                                end =
                                    FileTypesSpacing
                                        .cardHorizontalPadding,
                                bottom =
                                    FileTypesSpacing
                                        .cardVerticalPadding,
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            FileTypesSpacing
                                .excludedExtensionsChipSpacing,
                        ),
                ) {
                    excludedExtensions.sorted().forEach { extension ->
                        ExcludedExtensionChip(
                            extension = extension,
                            onRemove = {
                                onRemoveExcludedExtension(extension)
                            },
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    var activeFolderFilterTarget by remember { mutableStateOf<FolderFilterTarget?>(null) }

    activeFolderFilterTarget?.let { target ->
        FolderSearchDialog(
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
                val pattern = normalizePathFilterPattern(folderDisplayPath(folder)) ?: return@FolderSearchDialog
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

    SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(FileTypesSpacing.pathFilterCardSpacing),
        ) {
            Spacer(modifier = Modifier.height(FileTypesSpacing.pathFilterTitleTopSpacing))

            Text(
                text = stringResource(R.string.settings_folder_filters_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.settings_folder_filter_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(FileTypesSpacing.pathFilterDescriptionBottomSpacing))

            Text(
                text = stringResource(R.string.settings_folder_whitelist_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_folder_whitelist_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FolderFilterList(
                patterns = folderWhitelistPatterns,
                onAdd = { activeFolderFilterTarget = FolderFilterTarget.Whitelist },
                onRemove = { pattern ->
                    onSetFolderWhitelistPatterns(folderWhitelistPatterns - pattern)
                },
            )

            Spacer(modifier = Modifier.height(FileTypesSpacing.blacklistTitleTopSpacing))

            Text(
                text = stringResource(R.string.settings_folder_blacklist_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_folder_blacklist_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FolderFilterList(
                patterns = folderBlacklistPatterns,
                onAdd = { activeFolderFilterTarget = FolderFilterTarget.Blacklist },
                onRemove = { pattern ->
                    onSetFolderBlacklistPatterns(folderBlacklistPatterns - pattern)
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private enum class FolderFilterTarget {
    Whitelist,
    Blacklist,
}

@Composable
private fun FolderFilterList(
    patterns: Set<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (patterns.isNotEmpty()) {
            patterns.sortedBy(::patternDisplayPath).forEach { pattern ->
                FolderFilterRow(
                    pattern = pattern,
                    onRemove = { onRemove(pattern) },
                )
            }
        }

        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(50.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(FileTypesSpacing.chipIconSize),
            )
            Text(text = stringResource(R.string.settings_folder_filter_add_button))
        }
    }
}

@Composable
private fun FolderFilterRow(
    pattern: String,
    onRemove: () -> Unit,
) {
    val path = patternDisplayPath(pattern)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DesignTokens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = path.substringAfterLast('/', path),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = displayPathWithLeadingSlash(path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.action_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FolderSearchDialog(
    title: String,
    selectedPatterns: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (DeviceFile) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { FileSearchRepository(context) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var results by remember { mutableStateOf<List<DeviceFile>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(query.text) {
        val trimmedQuery = query.text.trim()
        results =
            if (trimmedQuery.isBlank()) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) {
                    repository.searchFolders(trimmedQuery, limit = 30)
                        .distinctBy { folder ->
                            normalizePathFilterPattern(folderDisplayPath(folder))
                        }
                }
            }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = RoundedCornerShape(50.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.desc_search_icon),
                        )
                    },
                    trailingIcon = {
                        if (query.text.isNotBlank()) {
                            IconButton(onClick = { query = TextFieldValue("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.desc_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = FileTypesSpacing.folderDialogMaxHeight),
                ) {
                    when {
                        query.text.isBlank() -> {
                            Text(
                                text = stringResource(R.string.settings_folder_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        results.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.settings_folder_search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            LazyColumn {
                                itemsIndexed(results) { index, folder ->
                                    val path = folderDisplayPath(folder)
                                    val pattern = normalizePathFilterPattern(path)
                                    FolderSearchResultRow(
                                        folder = folder,
                                        alreadySelected = pattern in selectedPatterns,
                                        onSelect = { onSelect(folder) },
                                    )
                                    if (index < results.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun FolderSearchResultRow(
    folder: DeviceFile,
    alreadySelected: Boolean,
    onSelect: () -> Unit,
) {
    val path = folderDisplayPath(folder)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !alreadySelected,
                    onClick = onSelect,
                )
                .padding(vertical = DesignTokens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = displayPathWithLeadingSlash(path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.settings_folder_filter_add_button),
            tint =
                if (alreadySelected) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}
