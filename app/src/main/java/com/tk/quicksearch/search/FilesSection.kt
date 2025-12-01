package com.tk.quicksearch.search

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.model.DeviceFile

private const val INITIAL_RESULT_COUNT = 1

@Composable
fun FileResultsSection(
    modifier: Modifier = Modifier,
    hasPermission: Boolean,
    files: List<DeviceFile>,
    isExpanded: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    onRequestPermission: () -> Unit,
    pinnedFileUris: Set<String> = emptySet(),
    onTogglePin: (DeviceFile) -> Unit = {},
    onExclude: (DeviceFile) -> Unit = {},
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit,
    resultSectionTitle: @Composable (String) -> Unit,
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit
) {
    val hasVisibleContent = (hasPermission && files.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return
    val orderedFiles = files

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        resultSectionTitle(stringResource(R.string.files_section_title))
        when {
            hasPermission && files.isNotEmpty() -> {
                val displayAsExpanded = isExpanded || showAllResults
                val canShowExpand = showExpandControls && orderedFiles.size > INITIAL_RESULT_COUNT
                val expandHandler = if (!displayAsExpanded && canShowExpand) onExpandClick else null
                val collapseHandler = if (isExpanded && showExpandControls) onExpandClick else null
                val displayFiles = if (displayAsExpanded) {
                    orderedFiles
                } else {
                    orderedFiles.take(INITIAL_RESULT_COUNT)
                }
                FilesResultCard(
                    files = displayFiles,
                    allFiles = orderedFiles,
                    isExpanded = displayAsExpanded,
                    onFileClick = onFileClick,
                    pinnedFileUris = pinnedFileUris,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExpandClick = expandHandler,
                    onCollapseClick = collapseHandler
                )
            }

            !hasPermission -> {
                permissionDisabledCard(
                    stringResource(R.string.files_permission_title),
                    stringResource(R.string.files_permission_subtitle),
                    stringResource(R.string.permission_action_manage_android),
                    onRequestPermission
                )
            }
        }
    }
}

@Composable
private fun FilesResultCard(
    files: List<DeviceFile>,
    allFiles: List<DeviceFile>,
    isExpanded: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    pinnedFileUris: Set<String>,
    onTogglePin: (DeviceFile) -> Unit,
    onExclude: (DeviceFile) -> Unit,
    onExpandClick: (() -> Unit)?,
    onCollapseClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                files.forEachIndexed { index, file ->
                    FileResultRow(
                        deviceFile = file,
                        onClick = onFileClick,
                        isExpanded = isExpanded,
                        isPinned = pinnedFileUris.contains(file.uri.toString()),
                        onTogglePin = onTogglePin,
                        onExclude = onExclude
                    )
                    if (index != files.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                if (onExpandClick != null && !isExpanded) {
                    TextButton(
                        onClick = { onExpandClick() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(28.dp)
                            .padding(top = 2.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (onCollapseClick != null && isExpanded) {
            TextButton(
                onClick = { onCollapseClick() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = "Collapse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FileResultRow(
    deviceFile: DeviceFile,
    onClick: (DeviceFile) -> Unit,
    isExpanded: Boolean = false,
    isPinned: Boolean = false,
    onTogglePin: (DeviceFile) -> Unit = {},
    onExclude: (DeviceFile) -> Unit = {}
) {
    val (showOptions, setShowOptions) = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isExpanded) 0.dp else 52.dp)
            .combinedClickable(
                onClick = { onClick(deviceFile) },
                onLongClick = { setShowOptions(true) }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(24.dp)
                .padding(start = 4.dp)
        )
        Text(
            text = deviceFile.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { setShowOptions(false) },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    setShowOptions(false)
                    onTogglePin(deviceFile)
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_exclude_generic)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    setShowOptions(false)
                    onExclude(deviceFile)
                }
            )
        }
    }
}

