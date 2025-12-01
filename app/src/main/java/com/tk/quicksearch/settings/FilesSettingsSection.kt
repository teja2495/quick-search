package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.FileType

@Composable
fun FileTypesSection(
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    filesSectionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!filesSectionEnabled) {
        return
    }
    
    Text(
        text = stringResource(R.string.settings_file_types_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_file_types_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            FileType.values().forEachIndexed { index, fileType ->
                FileTypeRow(
                    fileType = fileType,
                    isEnabled = fileType in enabledFileTypes,
                    onToggle = { enabled -> onToggleFileType(fileType, enabled) }
                )
                if (index != FileType.values().lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTypeRow(
    fileType: FileType,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val fileTypeName = when (fileType) {
        FileType.PHOTOS_AND_VIDEOS -> stringResource(R.string.file_type_photos_and_videos)
        FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
        FileType.APK -> stringResource(R.string.file_type_apk)
        FileType.OTHER -> stringResource(R.string.file_type_other)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fileTypeName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

