package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens

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

private fun parsePathFilterInput(input: String): Set<String> =
    input
        .split(",")
        .mapNotNull(::normalizePathFilterPattern)
        .toCollection(LinkedHashSet())

private fun shouldApplyPathFiltersOnInput(value: String): Boolean =
    value.endsWith(",") || value.endsWith(", ")

private fun trailingDelimiter(value: String): String =
    when {
        value.endsWith(", ") -> ", "
        value.endsWith(",") -> ","
        else -> ""
    }

private fun buildNormalizedInputText(rawValue: String, keepTrailingDelimiter: Boolean): String {
    val normalizedPatterns = parsePathFilterInput(rawValue).joinToString(", ")
    if (!keepTrailingDelimiter) return normalizedPatterns
    return normalizedPatterns + trailingDelimiter(rawValue)
}

/** Gets the display name for a file type. */
@Composable
private fun getFileTypeDisplayName(fileType: FileType): String =
    when (fileType) {
        FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
        FileType.PICTURES -> stringResource(R.string.file_type_pictures)
        FileType.VIDEOS -> stringResource(R.string.file_type_videos)
        FileType.AUDIO -> stringResource(R.string.file_type_audio)
        FileType.APKS -> stringResource(R.string.file_type_apks)
        FileType.OTHER -> stringResource(R.string.file_type_other)
    }

/** Gets the icon for a file type. */
private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector =
    when (fileType) {
        FileType.DOCUMENTS -> Icons.Rounded.InsertDriveFile
        FileType.PICTURES -> Icons.Rounded.Image
        FileType.VIDEOS -> Icons.Rounded.VideoLibrary
        FileType.AUDIO -> Icons.Rounded.AudioFile
        FileType.APKS -> Icons.Rounded.Android
        FileType.OTHER -> Icons.Rounded.InsertDriveFile
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
 * @param showHiddenFiles Whether hidden files are shown in search results
 * @param onToggleHiddenFiles Callback when hidden files toggle is changed
 * @param folderWhitelistPatterns Folder path patterns that should be allowed in results
 * @param onSetFolderWhitelistPatterns Callback when whitelist patterns change
 * @param folderBlacklistPatterns Folder path patterns that should be blocked in results
 * @param onSetFolderBlacklistPatterns Callback when blacklist patterns change
 * @param excludedExtensions Set of excluded file extensions
 * @param onRemoveExcludedExtension Callback when an excluded extension should be removed
 * @param filesSectionEnabled Whether the files section is enabled. If false, this section is not
 * displayed.
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
    showHiddenFiles: Boolean,
    onToggleHiddenFiles: (Boolean) -> Unit,
    folderWhitelistPatterns: Set<String>,
    onSetFolderWhitelistPatterns: (Set<String>) -> Unit,
    folderBlacklistPatterns: Set<String>,
    onSetFolderBlacklistPatterns: (Set<String>) -> Unit,
    excludedExtensions: Set<String>,
    onRemoveExcludedExtension: (String) -> Unit,
    filesSectionEnabled: Boolean = true,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!filesSectionEnabled) {
        return
    }

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
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
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
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
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
                    isLastItem = index == orderedFileTypes.lastIndex,
                )
            }

            // Excluded extensions section
            if (excludedExtensions.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

    // Card 3: System Files & Hidden Files toggles
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.settings_system_files_toggle),
                checked = showSystemFiles,
                onCheckedChange = onToggleSystemFiles,
                leadingIcon = Icons.Rounded.Visibility,
                isFirstItem = true,
                isLastItem = false,
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_hidden_files_toggle),
                checked = showHiddenFiles,
                onCheckedChange = onToggleHiddenFiles,
                leadingIcon = Icons.Rounded.VisibilityOff,
                isFirstItem = false,
                isLastItem = true,
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    var whitelistInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = folderWhitelistPatterns.joinToString(", "),
                selection = TextRange(folderWhitelistPatterns.joinToString(", ").length),
            ),
        )
    }
    var blacklistInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = folderBlacklistPatterns.joinToString(", "),
                selection = TextRange(folderBlacklistPatterns.joinToString(", ").length),
            ),
        )
    }
    var lastAppliedWhitelistPatterns by remember { mutableStateOf(folderWhitelistPatterns) }
    var lastAppliedBlacklistPatterns by remember { mutableStateOf(folderBlacklistPatterns) }
    var whitelistHasFocus by remember { mutableStateOf(false) }
    var blacklistHasFocus by remember { mutableStateOf(false) }

    val applyWhitelistPatterns: (String, Boolean) -> Unit = { rawValue, keepTrailingDelimiter ->
        val patterns = parsePathFilterInput(rawValue)
        val updatedInput = buildNormalizedInputText(rawValue, keepTrailingDelimiter)
        whitelistInput = TextFieldValue(updatedInput, TextRange(updatedInput.length))
        lastAppliedWhitelistPatterns = patterns
        onSetFolderWhitelistPatterns(patterns)
    }

    val applyBlacklistPatterns: (String, Boolean) -> Unit = { rawValue, keepTrailingDelimiter ->
        val patterns = parsePathFilterInput(rawValue)
        val updatedInput = buildNormalizedInputText(rawValue, keepTrailingDelimiter)
        blacklistInput = TextFieldValue(updatedInput, TextRange(updatedInput.length))
        lastAppliedBlacklistPatterns = patterns
        onSetFolderBlacklistPatterns(patterns)
    }

    LaunchedEffect(folderWhitelistPatterns) {
        if (folderWhitelistPatterns != lastAppliedWhitelistPatterns) {
            val updatedText = folderWhitelistPatterns.joinToString(", ")
            whitelistInput = TextFieldValue(updatedText, TextRange(updatedText.length))
            lastAppliedWhitelistPatterns = folderWhitelistPatterns
        }
    }
    LaunchedEffect(folderBlacklistPatterns) {
        if (folderBlacklistPatterns != lastAppliedBlacklistPatterns) {
            val updatedText = folderBlacklistPatterns.joinToString(", ")
            blacklistInput = TextFieldValue(updatedText, TextRange(updatedText.length))
            lastAppliedBlacklistPatterns = folderBlacklistPatterns
        }
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
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

            OutlinedTextField(
                value = whitelistInput,
                onValueChange = { value ->
                    whitelistInput = value
                    if (shouldApplyPathFiltersOnInput(value.text)) {
                        applyWhitelistPatterns(value.text, true)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (whitelistHasFocus && !focusState.isFocused) {
                                applyWhitelistPatterns(whitelistInput.text, true)
                            }
                            whitelistHasFocus = focusState.isFocused
                        },
                minLines = 1,
                maxLines = 3,
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

            OutlinedTextField(
                value = blacklistInput,
                onValueChange = { value ->
                    blacklistInput = value
                    if (shouldApplyPathFiltersOnInput(value.text)) {
                        applyBlacklistPatterns(value.text, true)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (blacklistHasFocus && !focusState.isFocused) {
                                applyBlacklistPatterns(blacklistInput.text, true)
                            }
                            blacklistHasFocus = focusState.isFocused
                        },
                minLines = 1,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
