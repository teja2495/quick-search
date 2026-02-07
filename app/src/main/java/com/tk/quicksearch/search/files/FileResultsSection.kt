package com.tk.quicksearch.search.files

import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
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

private const val FILE_ICON_SIZE = 24
private const val PDF_ICON_SIZE = 20
private const val THUMBNAIL_SIZE_DP = 60
private const val THUMBNAIL_LOAD_SIZE_PX = 160
private const val THUMBNAIL_CACHE_MAX_SIZE = 60
private const val EXPAND_BUTTON_TOP_PADDING = 2
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12
private const val DROPDOWN_CORNER_RADIUS = 24

private object FileThumbnailCache {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(THUMBNAIL_CACHE_MAX_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>) = size > THUMBNAIL_CACHE_MAX_SIZE
    }

    @Synchronized
    fun get(uri: String): ImageBitmap? = cache[uri]

    @Synchronized
    fun put(uri: String, bitmap: ImageBitmap) {
        cache[uri] = bitmap
    }
}

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
    onOpenFolder: (DeviceFile) -> Unit = {},
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
                    onOpenFolder = onOpenFolder,
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
    onOpenFolder: (DeviceFile) -> Unit,
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
                    onOpenFolder = onOpenFolder,
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
    onOpenFolder: (DeviceFile) -> Unit,
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
                    onOpenFolder = onOpenFolder,
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
                    onOpenFolder = onOpenFolder,
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

@Composable
private fun fileResultIcon(deviceFile: DeviceFile): ImageVector =
    when {
        deviceFile.isDirectory -> Icons.Rounded.Folder
        FileTypeUtils.isPdf(deviceFile) -> ImageVector.vectorResource(R.drawable.ic_pdf)
        else -> when (FileTypeUtils.getFileType(deviceFile)) {
            FileType.AUDIO -> Icons.Rounded.AudioFile
            FileType.PICTURES -> Icons.Rounded.Image
            FileType.VIDEOS -> Icons.Rounded.VideoLibrary
            FileType.APKS -> Icons.Rounded.Android
            else -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }
    }

@Composable
private fun FileResultThumbnailOrIcon(
    deviceFile: DeviceFile,
    iconOverride: ImageVector?,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fileType = FileTypeUtils.getFileType(deviceFile)
    val showThumbnail = !deviceFile.isDirectory && (fileType == FileType.PICTURES || fileType == FileType.VIDEOS)
    val uriString = deviceFile.uri.toString()
    var thumbnailBitmap by remember(uriString) {
        mutableStateOf<ImageBitmap?>(FileThumbnailCache.get(uriString))
    }

    if (showThumbnail && iconOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LaunchedEffect(uriString) {
            if (thumbnailBitmap != null) return@LaunchedEffect
            val signal = CancellationSignal()
            currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
                if (cause is CancellationException) signal.cancel()
            }
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.loadThumbnail(
                        deviceFile.uri,
                        Size(THUMBNAIL_LOAD_SIZE_PX, THUMBNAIL_LOAD_SIZE_PX),
                        signal,
                    )
                } catch (_: Exception) {
                    null
                }
            }
            val imageBitmap = bitmap?.asImageBitmap()
            if (imageBitmap != null) {
                FileThumbnailCache.put(uriString, imageBitmap)
                thumbnailBitmap = imageBitmap
            }
        }
    }

    val isPdf = iconOverride == null && FileTypeUtils.isPdf(deviceFile)
    val iconSize = when {
        iconOverride != null -> 34.dp
        isPdf -> PDF_ICON_SIZE.dp
        else -> FILE_ICON_SIZE.dp
    }

    if (thumbnailBitmap != null && iconOverride == null) {
        val thumbnailAlpha = remember(uriString) { Animatable(0f) }
        LaunchedEffect(uriString) {
            thumbnailAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200),
            )
        }
        Box(modifier = modifier.size(THUMBNAIL_SIZE_DP.dp)) {
            Icon(
                imageVector = fileResultIcon(deviceFile),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(THUMBNAIL_SIZE_DP.dp)
                    .alpha(1f - thumbnailAlpha.value),
            )
            Image(
                bitmap = thumbnailBitmap!!,
                contentDescription = null,
                modifier = Modifier
                    .size(THUMBNAIL_SIZE_DP.dp)
                    .clip(DesignTokens.CardShape)
                    .alpha(thumbnailAlpha.value),
                contentScale = ContentScale.Crop,
            )
        }
    } else {
        Icon(
            imageVector = iconOverride ?: fileResultIcon(deviceFile),
            contentDescription = null,
            tint = if (isPdf) Color.Unspecified else iconTint,
            modifier = modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileResultRow(
    deviceFile: DeviceFile,
    onClick: (DeviceFile) -> Unit,
    onOpenFolder: (DeviceFile) -> Unit = {},
    isPinned: Boolean = false,
    onTogglePin: (DeviceFile) -> Unit = {},
    onExclude: (DeviceFile) -> Unit = {},
    onExcludeExtension: (DeviceFile) -> Unit = {},
    onNicknameClick: (DeviceFile) -> Unit = {},
    hasNickname: Boolean = false,
    enableLongPress: Boolean = true,
    onLongPressOverride: (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
) {
    var showOptions by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
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
                FileResultThumbnailOrIcon(
                    deviceFile = deviceFile,
                    iconOverride = icon,
                    iconTint = iconTint,
                    modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
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
                onOpenFolderClick = { onOpenFolder(deviceFile) },
                onFileInfoClick = { showFileInfoDialog = true },
            )
        }
        if (showFileInfoDialog) {
            FileInfoDialog(
                deviceFile = deviceFile,
                onDismiss = { showFileInfoDialog = false },
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
