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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle

// Constants for consistent spacing
private object FileTypesSpacing {
        val cardHorizontalPadding = DesignTokens.CardHorizontalPadding
        val cardVerticalPadding = DesignTokens.CardVerticalPadding
        val chipButtonSize = 16.dp
        val chipIconSize = 12.dp
        val excludedExtensionsTitleBottomPadding = DesignTokens.SectionTitleBottomPadding
        val excludedExtensionsChipSpacing = 8.dp
}

/** Gets the display name for a file type. */
@Composable
private fun getFileTypeDisplayName(fileType: FileType): String {
        return when (fileType) {
                FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
                FileType.PICTURES -> stringResource(R.string.file_type_pictures)
                FileType.VIDEOS -> stringResource(R.string.file_type_videos)
                FileType.MUSIC -> stringResource(R.string.file_type_music)
                FileType.APKS -> stringResource(R.string.file_type_apks)
                FileType.OTHER -> stringResource(R.string.file_type_other)
        }
}

/** Gets the icon for a file type. */
private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector {
        return when (fileType) {
                FileType.DOCUMENTS -> Icons.Rounded.Folder
                FileType.PICTURES -> Icons.Rounded.Image
                FileType.VIDEOS -> Icons.Rounded.VideoLibrary
                FileType.MUSIC -> Icons.Rounded.MusicNote
                FileType.APKS -> Icons.Rounded.Android
                FileType.OTHER -> Icons.Rounded.Folder
        }
}

/** Small chip component for displaying excluded file extensions with remove functionality. */
@Composable
private fun ExcludedExtensionChip(
        extension: String,
        onRemove: () -> Unit,
        modifier: Modifier = Modifier
) {
        androidx.compose.material3.AssistChip(
                onClick = { /* No click action, only the X button works */},
                label = {
                        Text(
                                text = ".$extension",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                },
                trailingIcon = {
                        IconButton(
                                onClick = onRemove,
                                modifier = Modifier.size(FileTypesSpacing.chipButtonSize)
                        ) {
                                Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.action_remove),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(FileTypesSpacing.chipIconSize)
                                )
                        }
                },
                modifier = modifier,
                colors =
                        androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
        )
}

/** Reusable toggle row component for file type settings. Provides consistent styling and layout. */
@Composable
private fun FileTypeToggleRow(
        text: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
        val view = LocalView.current
        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .padding(
                                        horizontal = FileTypesSpacing.cardHorizontalPadding,
                                        vertical = FileTypesSpacing.cardVerticalPadding
                                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        if (icon != null) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                        Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                }
                Switch(
                        checked = checked,
                        onCheckedChange = { enabled ->
                                hapticToggle(view)()
                                onCheckedChange(enabled)
                        }
                )
        }
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
        excludedExtensions: Set<String>,
        onRemoveExcludedExtension: (String) -> Unit,
        filesSectionEnabled: Boolean = true,
        showTitle: Boolean = true,
        modifier: Modifier = Modifier
) {
        if (!filesSectionEnabled) {
                return
        }

        // Section title
        if (showTitle) {
                SettingsSectionTitle(
                        title = stringResource(R.string.settings_file_types_title),
                        modifier = modifier
                )
        }

        // Card 1: Folders toggle
        SettingsCard {
                FileTypeToggleRow(
                        text = stringResource(R.string.settings_folders_toggle),
                        checked = showFolders,
                        onCheckedChange = onToggleFolders,
                        icon = Icons.Rounded.Folder
                )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 2: File types with icons in specific order
        SettingsCard {
                Column {
                        // Define the order: Documents, Pictures, Videos, Music, APKs, Other
                        val orderedFileTypes =
                                listOf(
                                        FileType.DOCUMENTS,
                                        FileType.PICTURES,
                                        FileType.VIDEOS,
                                        FileType.MUSIC,
                                        FileType.APKS,
                                        FileType.OTHER
                                )

                        orderedFileTypes.forEachIndexed { index, fileType ->
                                FileTypeToggleRow(
                                        text = getFileTypeDisplayName(fileType),
                                        checked = fileType in enabledFileTypes,
                                        onCheckedChange = { enabled ->
                                                onToggleFileType(fileType, enabled)
                                        },
                                        icon = getFileTypeIcon(fileType)
                                )

                                // Add divider between items (not after the last one)
                                if (index < orderedFileTypes.lastIndex) {
                                        HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                }
                        }

                        // Excluded extensions section
                        if (excludedExtensions.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Spacer(
                                        modifier =
                                                Modifier.height(
                                                        FileTypesSpacing.cardVerticalPadding
                                                )
                                )

                                // Excluded extensions header
                                Text(
                                        text =
                                                stringResource(
                                                        R.string.settings_excluded_extensions_title
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
                                                                        .excludedExtensionsTitleBottomPadding
                                                )
                                )

                                // Excluded extensions chips
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                start =
                                                                        FileTypesSpacing
                                                                                .cardHorizontalPadding,
                                                                end =
                                                                        FileTypesSpacing
                                                                                .cardHorizontalPadding,
                                                                bottom =
                                                                        FileTypesSpacing
                                                                                .cardVerticalPadding
                                                        ),
                                        horizontalArrangement =
                                                Arrangement.spacedBy(
                                                        FileTypesSpacing
                                                                .excludedExtensionsChipSpacing
                                                )
                                ) {
                                        excludedExtensions.sorted().forEach { extension ->
                                                ExcludedExtensionChip(
                                                        extension = extension,
                                                        onRemove = {
                                                                onRemoveExcludedExtension(extension)
                                                        }
                                                )
                                        }
                                }
                        }
                }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 3: System Files & Hidden Files toggles
        SettingsCard {
                Column {
                        FileTypeToggleRow(
                                text = stringResource(R.string.settings_system_files_toggle),
                                checked = showSystemFiles,
                                onCheckedChange = onToggleSystemFiles,
                                icon = Icons.Rounded.Visibility
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        FileTypeToggleRow(
                                text = stringResource(R.string.settings_hidden_files_toggle),
                                checked = showHiddenFiles,
                                onCheckedChange = onToggleHiddenFiles,
                                icon = Icons.Rounded.VisibilityOff
                        )
                }
        }
}