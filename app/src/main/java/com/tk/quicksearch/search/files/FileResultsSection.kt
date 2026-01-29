package com.tk.quicksearch.search.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticConfirm

// ============================================================================
// Constants
// ============================================================================

private const val FILE_ICON_SIZE = 28
private const val EXPAND_BUTTON_TOP_PADDING = 2
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
    showWallpaperBackground: Boolean = false,
) {
    val hasVisibleContent = (hasPermission && files.isNotEmpty()) || !hasPermission
    if (!hasVisibleContent) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
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
                    showWallpaperBackground = showWallpaperBackground,
                )
            }

            !hasPermission -> {
                permissionDisabledCard(
                    stringResource(R.string.files_permission_title),
                    stringResource(R.string.files_permission_subtitle),
                    stringResource(R.string.permission_action_manage_android),
                    onRequestPermission,
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
    showWallpaperBackground: Boolean = false,
) {
    val displayAsExpanded = isExpanded || showAllResults
    val canShowExpand =
        showExpandControls && files.size > SearchScreenConstants.INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !displayAsExpanded && canShowExpand
    val shouldUseLazyList =
        isExpanded && files.size > SearchScreenConstants.INITIAL_RESULT_COUNT

    val displayFiles =
        if (displayAsExpanded) {
            files
        } else {
            files.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        val cardModifier = Modifier.fillMaxWidth()
        val contentModifier =
            if (isExpanded) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        max = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
                    )
            } else {
                Modifier.fillMaxWidth()
            }

        val cardContent =
            @Composable
            {
                FileCardContent(
                    displayFiles = displayFiles,
                    shouldShowExpandButton = shouldShowExpandButton,
                    onFileClick = onFileClick,
                    pinnedFileUris = pinnedFileUris,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExcludeExtension = onExcludeExtension,
                    onNicknameClick = onNicknameClick,
                    getFileNickname = getFileNickname,
                    onExpandClick = onExpandClick,
                    modifier = contentModifier,
                    useLazyList = shouldUseLazyList,
                )
            }

        if (showWallpaperBackground) {
            Card(
                modifier = cardModifier,
                colors = AppColors.getCardColors(showWallpaperBackground = true),
                shape = MaterialTheme.shapes.extraLarge,
                elevation =
                    AppColors.getCardElevation(showWallpaperBackground = true),
            ) { cardContent() }
        } else {
            ElevatedCard(
                modifier = cardModifier,
                colors = AppColors.getCardColors(showWallpaperBackground = false),
                shape = MaterialTheme.shapes.extraLarge,
                elevation =
                    AppColors.getCardElevation(showWallpaperBackground = false),
            ) { cardContent() }
        }
    }
}

// ============================================================================
// Card Content
// ============================================================================

@Composable
private fun FileCardContent(
    modifier: Modifier = Modifier,
    displayFiles: List<DeviceFile>,
    shouldShowExpandButton: Boolean,
    onFileClick: (DeviceFile) -> Unit,
    pinnedFileUris: Set<String>,
    onTogglePin: (DeviceFile) -> Unit,
    onExclude: (DeviceFile) -> Unit,
    onExcludeExtension: (DeviceFile) -> Unit,
    onNicknameClick: (DeviceFile) -> Unit,
    getFileNickname: (String) -> String?,
    onExpandClick: () -> Unit,
    useLazyList: Boolean = false,
) {
    if (useLazyList) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = DesignTokens.SpacingMedium),
        ) {
            itemsIndexed(
                items = displayFiles,
                key = { _, file -> file.uri.toString() },
            ) { index, file ->
                FileResultRow(
                    deviceFile = file,
                    onClick = onFileClick,
                    isPinned = pinnedFileUris.contains(file.uri.toString()),
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExcludeExtension = onExcludeExtension,
                    onNicknameClick = onNicknameClick,
                    hasNickname =
                        !getFileNickname(file.uri.toString())
                            .isNullOrBlank(),
                )
                if (index != displayFiles.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (shouldShowExpandButton) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExpandButton(
                            onClick = onExpandClick,
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .height(
                                        ContactUiConstants
                                            .EXPAND_BUTTON_HEIGHT
                                            .dp,
                                    ).padding(
                                        top =
                                            EXPAND_BUTTON_TOP_PADDING
                                                .dp,
                                    ),
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.padding(horizontal = DesignTokens.SpacingMedium)) {
            displayFiles.forEachIndexed { index, file ->
                FileResultRow(
                    deviceFile = file,
                    onClick = onFileClick,
                    isPinned = pinnedFileUris.contains(file.uri.toString()),
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExcludeExtension = onExcludeExtension,
                    onNicknameClick = onNicknameClick,
                    hasNickname =
                        !getFileNickname(file.uri.toString())
                            .isNullOrBlank(),
                )
                if (index != displayFiles.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (shouldShowExpandButton) {
                ExpandButton(
                    onClick = onExpandClick,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(
                                ContactUiConstants
                                    .EXPAND_BUTTON_HEIGHT
                                    .dp,
                            ).padding(top = EXPAND_BUTTON_TOP_PADDING.dp),
                )
            }
        }
    }
}

// ============================================================================
// File Row
// ============================================================================

private fun fileResultIcon(deviceFile: DeviceFile) =
    when {
        deviceFile.isDirectory -> {
            Icons.Rounded.Folder
        }

        else -> {
            when (FileTypeUtils.getFileType(deviceFile)) {
                FileType.AUDIO -> Icons.Rounded.AudioFile
                FileType.PICTURES -> Icons.Rounded.Image
                FileType.VIDEOS -> Icons.Rounded.VideoLibrary
                FileType.APKS -> Icons.Rounded.Android
                else -> Icons.AutoMirrored.Rounded.InsertDriveFile
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileResultRow(
    deviceFile: DeviceFile,
    onClick: (DeviceFile) -> Unit,
    isPinned: Boolean = false,
    onTogglePin: (DeviceFile) -> Unit = {},
    onExclude: (DeviceFile) -> Unit = {},
    onExcludeExtension: (DeviceFile) -> Unit = {},
    onNicknameClick: (DeviceFile) -> Unit = {},
    hasNickname: Boolean = false,
    enableLongPress: Boolean = true,
    onLongPressOverride: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
) {
    var showOptions by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(DesignTokens.CardShape)
                    .combinedClickable(
                        onClick = {
                            hapticConfirm(view)()
                            onClick(deviceFile)
                        },
                        onLongClick =
                            onLongPressOverride
                                ?: if (enableLongPress) {
                                    { showOptions = true }
                                } else {
                                    null
                                },
                    ).padding(vertical = DesignTokens.SpacingLarge),
        ) {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon ?: fileResultIcon(deviceFile),
                    contentDescription = null,
                    tint = iconTint,
                    modifier =
                        Modifier
                            .size(
                                if (icon != null) {
                                    34.dp
                                } else {
                                    FILE_ICON_SIZE.dp
                                },
                            ).padding(start = DesignTokens.SpacingSmall),
                )

                Text(
                    text = deviceFile.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (enableLongPress && onLongPressOverride == null) {
            FileDropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false },
                deviceFile = deviceFile,
                isPinned = isPinned,
                hasNickname = hasNickname,
                onTogglePin = { onTogglePin(deviceFile) },
                onExclude = { onExclude(deviceFile) },
                onExcludeExtension = { onExcludeExtension(deviceFile) },
                onNicknameClick = { onNicknameClick(deviceFile) },
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
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding =
            PaddingValues(
                horizontal = EXPAND_BUTTON_HORIZONTAL_PADDING.dp,
                vertical = 0.dp,
            ),
    ) {
        Text(
            text = stringResource(R.string.action_expand_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp),
        )
    }
}
