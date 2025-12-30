package com.tk.quicksearch.search

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.util.FileUtils

// ============================================================================
// Constants
// ============================================================================

private const val INITIAL_RESULT_COUNT = 1
private const val FILE_ROW_MIN_HEIGHT = 52
private const val FILE_ICON_SIZE = 24
private const val FILE_ICON_START_PADDING = 4
private const val EXPAND_BUTTON_HEIGHT = 28
private const val EXPAND_BUTTON_TOP_PADDING = 2
private const val EXPAND_ICON_SIZE = 18
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12
private const val DROPDOWN_CORNER_RADIUS = 24

// ============================================================================
// Public API
// ============================================================================

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
    onExcludeExtension: (DeviceFile) -> Unit = {},
    onNicknameClick: (DeviceFile) -> Unit = {},
    getFileNickname: (String) -> String? = { null },
    showAllResults: Boolean = false,
    showExpandControls: Boolean = false,
    onExpandClick: () -> Unit,
    permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
    showWallpaperBackground: Boolean = false
) {
    val hasVisibleContent = (hasPermission && files.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            hasPermission && files.isNotEmpty() -> {
                FilesResultCard(
                    files = files,
                    isExpanded = isExpanded,
                    showAllResults = showAllResults,
                    showExpandControls = showExpandControls,
                    onFileClick = onFileClick,
                    pinnedFileUris = pinnedFileUris,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExcludeExtension = onExcludeExtension,
                    onNicknameClick = onNicknameClick,
                    getFileNickname = getFileNickname,
                    onExpandClick = onExpandClick,
                    showWallpaperBackground = showWallpaperBackground
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

// ============================================================================
// Result Card
// ============================================================================

@Composable
private fun FilesResultCard(
    files: List<DeviceFile>,
    isExpanded: Boolean,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    pinnedFileUris: Set<String>,
    onTogglePin: (DeviceFile) -> Unit,
    onExclude: (DeviceFile) -> Unit,
    onExcludeExtension: (DeviceFile) -> Unit,
    onNicknameClick: (DeviceFile) -> Unit,
    getFileNickname: (String) -> String?,
    onExpandClick: () -> Unit,
    showWallpaperBackground: Boolean = false
) {
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand = showExpandControls && files.size > INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !displayAsExpanded && canShowExpand
    val shouldShowCollapseButton = isExpanded && showExpandControls
    
    val displayFiles = if (displayAsExpanded) {
        files
    } else {
        files.take(INITIAL_RESULT_COUNT)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showWallpaperBackground) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    displayFiles.forEachIndexed { index, file ->
                        FileResultRow(
                            deviceFile = file,
                            onClick = onFileClick,
                            isExpanded = displayAsExpanded,
                            isPinned = pinnedFileUris.contains(file.uri.toString()),
                            onTogglePin = onTogglePin,
                            onExclude = onExclude,
                            onExcludeExtension = onExcludeExtension,
                            onNicknameClick = onNicknameClick,
                            hasNickname = !getFileNickname(file.uri.toString()).isNullOrBlank()
                        )
                        if (index != displayFiles.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    
                    if (shouldShowExpandButton) {
                        ExpandButton(
                            onClick = onExpandClick,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(EXPAND_BUTTON_HEIGHT.dp)
                                .padding(top = EXPAND_BUTTON_TOP_PADDING.dp)
                        )
                    }
                }
            }
        } else {
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
                    displayFiles.forEachIndexed { index, file ->
                        FileResultRow(
                            deviceFile = file,
                            onClick = onFileClick,
                            isExpanded = displayAsExpanded,
                            isPinned = pinnedFileUris.contains(file.uri.toString()),
                            onTogglePin = onTogglePin,
                            onExclude = onExclude,
                            onExcludeExtension = onExcludeExtension,
                            onNicknameClick = onNicknameClick,
                            hasNickname = !getFileNickname(file.uri.toString()).isNullOrBlank()
                        )
                        if (index != displayFiles.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                    
                    if (shouldShowExpandButton) {
                        ExpandButton(
                            onClick = onExpandClick,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(EXPAND_BUTTON_HEIGHT.dp)
                                .padding(top = EXPAND_BUTTON_TOP_PADDING.dp)
                        )
                    }
                }
            }
        }
        
        if (shouldShowCollapseButton) {
            CollapseButton(
                onClick = onExpandClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// File Row
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileResultRow(
    deviceFile: DeviceFile,
    onClick: (DeviceFile) -> Unit,
    isExpanded: Boolean = false,
    isPinned: Boolean = false,
    onTogglePin: (DeviceFile) -> Unit = {},
    onExclude: (DeviceFile) -> Unit = {},
    onExcludeExtension: (DeviceFile) -> Unit = {},
    onNicknameClick: (DeviceFile) -> Unit = {},
    hasNickname: Boolean = false
) {
    var showOptions by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isExpanded) 0.dp else FILE_ROW_MIN_HEIGHT.dp)
            .combinedClickable(
                onClick = { onClick(deviceFile) },
                onLongClick = { showOptions = true }
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
                .size(FILE_ICON_SIZE.dp)
                .padding(start = FILE_ICON_START_PADDING.dp)
        )
        
        Text(
            text = deviceFile.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
        
        FileDropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            deviceFile = deviceFile,
            isPinned = isPinned,
            hasNickname = hasNickname,
            onTogglePin = { onTogglePin(deviceFile) },
            onExclude = { onExclude(deviceFile) },
            onExcludeExtension = { onExcludeExtension(deviceFile) },
            onNicknameClick = { onNicknameClick(deviceFile) }
        )
    }
}

// ============================================================================
// Dropdown Menu
// ============================================================================

@Composable
private fun FileDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    deviceFile: DeviceFile,
    isPinned: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onExcludeExtension: () -> Unit,
    onNicknameClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(DROPDOWN_CORNER_RADIUS.dp),
        properties = PopupProperties(focusable = false)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isPinned) Icons.Rounded.Close else Icons.Rounded.PushPin,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onTogglePin()
            }
        )
        
        HorizontalDivider()
        
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onNicknameClick()
            }
        )
        
        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.action_exclude_generic))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onExclude()
            }
        )

        val fileExtension = FileUtils.getFileExtension(deviceFile.displayName)
        if (fileExtension != null) {
            HorizontalDivider()

            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.action_exclude_extension, fileExtension))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    onDismissRequest()
                    onExcludeExtension()
                }
            )
        }
    }
}

// ============================================================================
// Expand/Collapse Buttons
// ============================================================================

@Composable
private fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = EXPAND_BUTTON_HORIZONTAL_PADDING.dp,
            vertical = 0.dp
        )
    ) {
        Text(
            text = stringResource(R.string.action_expand_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}

@Composable
private fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.action_collapse),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}
