package com.tk.quicksearch.settings.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.settings.main.SettingsSpacing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton

// Constants for consistent spacing
private object FileTypesSpacing {
    val cardHorizontalPadding = 20.dp
    val cardVerticalPadding = 12.dp
}

/**
 * Gets the display name for a file type.
 */
@Composable
private fun getFileTypeDisplayName(fileType: FileType): String {
    return when (fileType) {
        FileType.PHOTOS_AND_VIDEOS -> stringResource(R.string.file_type_photos_and_videos)
        FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
        FileType.OTHER -> stringResource(R.string.file_type_other)
    }
}

/**
 * Small chip component for displaying excluded file extensions with remove functionality.
 */
@Composable
private fun ExcludedExtensionChip(
    extension: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.AssistChip(
        onClick = { /* No click action, only the X button works */ },
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
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
        },
        modifier = modifier,
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * Reusable toggle row component for file type settings.
 * Provides consistent styling and layout.
 */
@Composable
private fun FileTypeToggleRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = FileTypesSpacing.cardHorizontalPadding,
                vertical = FileTypesSpacing.cardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Settings section for configuring which file types are included in search results.
 *
 * @param enabledFileTypes Set of currently enabled file types
 * @param onToggleFileType Callback when a file type toggle is changed
 * @param excludedExtensions Set of excluded file extensions
 * @param onRemoveExcludedExtension Callback when an excluded extension should be removed
 * @param filesSectionEnabled Whether the files section is enabled. If false, this section is not displayed.
 * @param modifier Modifier to be applied to the section title
 */
@Composable
fun FileTypesSection(
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    excludedExtensions: Set<String>,
    onRemoveExcludedExtension: (String) -> Unit,
    filesSectionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!filesSectionEnabled) {
        return
    }
    
    // Section title
    Text(
        text = stringResource(R.string.settings_file_types_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
    )
    
    // File types card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Cache FileType.values() to avoid multiple calls
            val fileTypes = FileType.values()
            val lastIndex = fileTypes.lastIndex
            
            fileTypes.forEachIndexed { index, fileType ->
                FileTypeToggleRow(
                    text = getFileTypeDisplayName(fileType),
                    checked = fileType in enabledFileTypes,
                    onCheckedChange = { enabled -> onToggleFileType(fileType, enabled) }
                )
                
                // Add divider between items (not after the last one)
                if (index < lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // Excluded extensions section
            if (excludedExtensions.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(FileTypesSpacing.cardVerticalPadding))

                // Excluded extensions header
                Text(
                    text = stringResource(R.string.settings_excluded_extensions_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = FileTypesSpacing.cardHorizontalPadding,
                        top = FileTypesSpacing.cardVerticalPadding,
                        end = FileTypesSpacing.cardHorizontalPadding,
                        bottom = 8.dp
                    )
                )

                // Excluded extensions chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = FileTypesSpacing.cardHorizontalPadding,
                            end = FileTypesSpacing.cardHorizontalPadding,
                            bottom = FileTypesSpacing.cardVerticalPadding
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    excludedExtensions.sorted().forEach { extension ->
                        ExcludedExtensionChip(
                            extension = extension,
                            onRemove = { onRemoveExcludedExtension(extension) }
                        )
                    }
                }
            }
        }
    }
}


