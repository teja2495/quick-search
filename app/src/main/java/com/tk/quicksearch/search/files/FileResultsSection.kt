package com.tk.quicksearch.search.files

import android.os.Build
import android.os.SystemClock
import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.FileIntents
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppTheme
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

// ============================================================================
// Constants
// ============================================================================

private const val FILE_ICON_SIZE = 24
private const val CUSTOM_FILE_ICON_SIZE = 20
private const val THUMBNAIL_SIZE_DP = 60
private const val THUMBNAIL_LOAD_SIZE_PX = 160
private const val THUMBNAIL_CACHE_MAX_SIZE = 60
private const val THUMBNAIL_FAILURE_RETRY_DELAY_MS = 30_000L
private const val EXPAND_BUTTON_TOP_PADDING = 2
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12
private val FILE_CARD_CONTENT_VERTICAL_PADDING = 4.dp
private val WORD_EXTENSIONS = setOf("doc", "docx")
private val SHEET_EXTENSIONS = setOf("xls", "xlsx")
private val SLIDES_EXTENSIONS = setOf("ppt", "pptx")
private val TEXT_EXTENSIONS = setOf("txt")
private val EPUB_EXTENSIONS = setOf("epub")
private val ZIP_EXTENSIONS = setOf("zip")

private data class CustomFileIconSpec(
        val drawableRes: Int,
        val aspectRatio: Float,
)

private object FileThumbnailCache {
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache =
            object : LinkedHashMap<String, ImageBitmap>(THUMBNAIL_CACHE_MAX_SIZE, 0.75f, true) {
                override fun removeEldestEntry(
                        eldest: MutableMap.MutableEntry<String, ImageBitmap>
                ) = size > THUMBNAIL_CACHE_MAX_SIZE
            }
    private val inFlightLoads = mutableMapOf<String, Deferred<ImageBitmap?>>()
    private val failureTimestamps = mutableMapOf<String, Long>()

    @Synchronized fun get(uri: String): ImageBitmap? = cache[uri]

    @Synchronized
    fun put(uri: String, bitmap: ImageBitmap) {
        cache[uri] = bitmap
        failureTimestamps.remove(uri)
    }

    suspend fun getOrLoad(uri: String, loader: suspend () -> ImageBitmap?): ImageBitmap? {
        get(uri)?.let {
            return it
        }

        val deferred =
                synchronized(this) {
                    cache[uri]?.let {
                        return it
                    }

                    inFlightLoads[uri]?.let {
                        return@synchronized it
                    }

                    val now = SystemClock.elapsedRealtime()
                    val lastFailure = failureTimestamps[uri]
                    if (lastFailure != null && now - lastFailure < THUMBNAIL_FAILURE_RETRY_DELAY_MS
                    ) {
                        return@synchronized null
                    }

                    loadScope
                            .async(start = CoroutineStart.LAZY) {
                                var loadedBitmap: ImageBitmap? = null
                                try {
                                    loadedBitmap = loader()
                                } catch (_: Exception) {
                                    loadedBitmap = null
                                } finally {
                                    synchronized(this@FileThumbnailCache) {
                                        inFlightLoads.remove(uri)
                                        if (loadedBitmap != null) {
                                            cache[uri] = loadedBitmap!!
                                            failureTimestamps.remove(uri)
                                        } else {
                                            failureTimestamps[uri] = SystemClock.elapsedRealtime()
                                        }
                                    }
                                }
                                loadedBitmap
                            }
                            .also { inFlightLoads[uri] = it }
                }
                        ?: return null

        if (!deferred.isActive && !deferred.isCompleted) deferred.start()
        return try {
            deferred.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
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
        onTriggerClick: (DeviceFile) -> Unit = {},
        getFileNickname: (String) -> String? = { null },
        getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger? = { null },
        showAllResults: Boolean = false,
        showExpandControls: Boolean = false,
        onExpandClick: () -> Unit,
        expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
        permissionDisabledCard: @Composable (String, String, String, () -> Unit) -> Unit,
        showWallpaperBackground: Boolean = false,
        predictedTarget: PredictedSubmitTarget? = null,
        fillExpandedHeight: Boolean = false,
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
                        onTriggerClick = onTriggerClick,
                        getFileNickname = getFileNickname,
                        getFileTrigger = getFileTrigger,
                        onExpandClick = onExpandClick,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        showWallpaperBackground = showWallpaperBackground,
                        predictedTarget = predictedTarget,
                        fillExpandedHeight = fillExpandedHeight,
                )
            }
            !hasPermission -> {
                permissionDisabledCard(
                        stringResource(R.string.permission_required_title),
                        stringResource(R.string.files_section_permission_subtitle),
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
        onTriggerClick: (DeviceFile) -> Unit,
        getFileNickname: (String) -> String?,
        getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        onExpandClick: () -> Unit,
        expandedCardMaxHeight: Dp,
        showWallpaperBackground: Boolean = false,
        predictedTarget: PredictedSubmitTarget?,
        fillExpandedHeight: Boolean,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val lazyListState = rememberLazyListState()
    val hasLazyListOverflow = lazyListState.canScrollForward || lazyListState.canScrollBackward
    val predictedFileUri = (predictedTarget as? PredictedSubmitTarget.File)?.uri
    val hasPredictedFile =
            predictedFileUri != null && files.any { it.uri.toString() == predictedFileUri }
    val displayAsExpanded = isExpanded || showAllResults
    val useCardLevelPrediction =
            hasPredictedFile && (!displayAsExpanded || files.size == 1)
    val shouldUseLazyList = isExpanded && files.size > SearchScreenConstants.INITIAL_RESULT_COUNT

    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        ExpandableResultsCard(
                resultCount = files.size,
                isExpanded = isExpanded,
                showAllResults = showAllResults,
                isTopPredicted = useCardLevelPrediction,
                showExpandControls = showExpandControls,
                expandedCardMaxHeight = expandedCardMaxHeight,
                hasScrollableContent = shouldUseLazyList && hasLazyListOverflow,
                fillExpandedHeight = fillExpandedHeight,
                showWallpaperBackground = showWallpaperBackground,
                overlayCardColor = overlayCardColor,
        ) { contentModifier, cardState ->
            val displayFiles =
                    if (cardState.displayAsExpanded) {
                        files
                    } else {
                        files.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
                    }
            FileCardContent(
                    displayFiles = displayFiles,
                    overlayDividerColor = overlayDividerColor,
                    showWallpaperBackground = showWallpaperBackground,
                    shouldShowExpandButton = cardState.shouldShowExpandButton,
                    onFileClick = onFileClick,
                    onOpenFolder = onOpenFolder,
                    pinnedFileUris = pinnedFileUris,
                    onTogglePin = onTogglePin,
                    onExclude = onExclude,
                    onExcludeExtension = onExcludeExtension,
                    onNicknameClick = onNicknameClick,
                    onTriggerClick = onTriggerClick,
                    getFileNickname = getFileNickname,
                    getFileTrigger = getFileTrigger,
                    onExpandClick = onExpandClick,
                    modifier = contentModifier,
                    useLazyList = shouldUseLazyList,
                    lazyListState = lazyListState,
                    predictedFileUri = predictedFileUri,
                    useCardLevelPrediction = useCardLevelPrediction,
                    bottomContentPadding =
                            if (cardState.shouldFillExpandedHeight) {
                                    DesignTokens.SpacingSmall
                            } else {
                                    0.dp
                            },
            )
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
        overlayDividerColor: Color?,
        showWallpaperBackground: Boolean = false,
        shouldShowExpandButton: Boolean,
        onFileClick: (DeviceFile) -> Unit,
        onOpenFolder: (DeviceFile) -> Unit,
        pinnedFileUris: Set<String>,
        onTogglePin: (DeviceFile) -> Unit,
        onExclude: (DeviceFile) -> Unit,
        onExcludeExtension: (DeviceFile) -> Unit,
        onNicknameClick: (DeviceFile) -> Unit,
        onTriggerClick: (DeviceFile) -> Unit,
        getFileNickname: (String) -> String?,
        getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        onExpandClick: () -> Unit,
        useLazyList: Boolean = false,
        lazyListState: androidx.compose.foundation.lazy.LazyListState,
        predictedFileUri: String?,
        useCardLevelPrediction: Boolean,
        bottomContentPadding: Dp,
) {
    if (useLazyList) {
        LazyColumn(
                modifier = modifier,
                state = lazyListState,
                contentPadding =
                        PaddingValues(
                                start = DesignTokens.SpacingMedium,
                                top = FILE_CARD_CONTENT_VERTICAL_PADDING,
                                end = DesignTokens.SpacingMedium,
                                bottom = bottomContentPadding,
                        ),
        ) {
            itemsIndexed(
                    items = displayFiles,
                    key = { _, file -> file.uri.toString() },
            ) { index, file ->
                val isPredictedFile =
                        predictedFileUri != null &&
                                file.uri.toString() == predictedFileUri
                val showPredictedOnRow = isPredictedFile && !useCardLevelPrediction
                FileResultRow(
                        deviceFile = file,
                        onClick = onFileClick,
                        onOpenFolder = onOpenFolder,
                        isPinned = pinnedFileUris.contains(file.uri.toString()),
                        onTogglePin = onTogglePin,
                        onExclude = onExclude,
                        onExcludeExtension = onExcludeExtension,
                        onNicknameClick = onNicknameClick,
                        onTriggerClick = onTriggerClick,
                        hasNickname = !getFileNickname(file.uri.toString()).isNullOrBlank(),
                        hasTrigger = getFileTrigger(file.uri.toString())?.word?.isNotBlank() == true,
                        isPredicted = showPredictedOnRow,
                )
                if (index != displayFiles.lastIndex && !showPredictedOnRow) {
                    HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = overlayDividerColor ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (shouldShowExpandButton) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        ExpandButton(
                                onClick = onExpandClick,
                                modifier =
                                        Modifier.align(Alignment.Center)
                                                .height(
                                                        ContactUiConstants.EXPAND_BUTTON_HEIGHT.dp,
                                                )
                                                .padding(
                                                        top = EXPAND_BUTTON_TOP_PADDING.dp,
                                                ),
                        )
                    }
                }
            }
        }
    } else {
        Column(
                modifier =
                        modifier
                                .padding(
                                        horizontal = DesignTokens.SpacingMedium,
                                        vertical = FILE_CARD_CONTENT_VERTICAL_PADDING,
                                )
                                .padding(bottom = bottomContentPadding)
        ) {
            displayFiles.forEachIndexed { index, file ->
                val isPredictedFile =
                        predictedFileUri != null &&
                                file.uri.toString() == predictedFileUri
                val showPredictedOnRow = isPredictedFile && !useCardLevelPrediction
                FileResultRow(
                        deviceFile = file,
                        onClick = onFileClick,
                        onOpenFolder = onOpenFolder,
                        isPinned = pinnedFileUris.contains(file.uri.toString()),
                        onTogglePin = onTogglePin,
                        onExclude = onExclude,
                        onExcludeExtension = onExcludeExtension,
                        onNicknameClick = onNicknameClick,
                        onTriggerClick = onTriggerClick,
                        hasNickname = !getFileNickname(file.uri.toString()).isNullOrBlank(),
                        hasTrigger = getFileTrigger(file.uri.toString())?.word?.isNotBlank() == true,
                        isPredicted = showPredictedOnRow,
                )
                if (index != displayFiles.lastIndex && !showPredictedOnRow) {
                    HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = overlayDividerColor ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            if (shouldShowExpandButton) {
                ExpandButton(
                        onClick = onExpandClick,
                        modifier =
                                Modifier.align(Alignment.CenterHorizontally)
                                        .height(
                                                ContactUiConstants.EXPAND_BUTTON_HEIGHT.dp,
                                        )
                                        .padding(top = EXPAND_BUTTON_TOP_PADDING.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ============================================================================
// File Row
// ============================================================================

private fun customFileIconSpec(deviceFile: DeviceFile): CustomFileIconSpec? {
    if (deviceFile.isDirectory) return null

    val extension = deviceFile.displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (extension.isBlank()) return null

    return when {
        FileTypeUtils.isPdf(deviceFile) -> CustomFileIconSpec(R.drawable.ic_pdf, 1f)
        extension in WORD_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_doc, 3800f / 4800f)
        extension in SHEET_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_sheet, 47.333332f / 65.083336f)
        extension in SLIDES_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_slides, 421f / 511.605f)
        extension in TEXT_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_txt, 1f)
        extension in EPUB_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_epub, 508.893f / 471.466f)
        extension in ZIP_EXTENSIONS -> CustomFileIconSpec(R.drawable.ic_zip, 500f / 511.56f)
        else -> null
    }
}

@Composable
private fun fileResultIcon(deviceFile: DeviceFile): ImageVector {
    val customIcon = customFileIconSpec(deviceFile)
    return when {
        deviceFile.isDirectory -> Icons.Rounded.Folder
        customIcon != null -> ImageVector.vectorResource(customIcon.drawableRes)
        else ->
                when (FileTypeUtils.getFileType(deviceFile)) {
                    FileType.AUDIO -> Icons.Rounded.AudioFile
                    FileType.PICTURES -> Icons.Rounded.Image
                    FileType.VIDEOS -> Icons.Rounded.VideoLibrary
                    FileType.APKS -> Icons.Rounded.Android
                    else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                }
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
    val customIconSpec = if (iconOverride == null) customFileIconSpec(deviceFile) else null
    val customIcon = customIconSpec?.let { ImageVector.vectorResource(it.drawableRes) }
    val showThumbnail =
            !deviceFile.isDirectory &&
                    (fileType == FileType.PICTURES || fileType == FileType.VIDEOS)
    val uriString = deviceFile.uri.toString()
    var thumbnailBitmap by
            remember(uriString) { mutableStateOf<ImageBitmap?>(FileThumbnailCache.get(uriString)) }

    if (showThumbnail && iconOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LaunchedEffect(uriString) {
            if (thumbnailBitmap != null) return@LaunchedEffect
            val imageBitmap =
                    FileThumbnailCache.getOrLoad(uriString) {
                        try {
                            context.contentResolver
                                    .loadThumbnail(
                                            deviceFile.uri,
                                            Size(THUMBNAIL_LOAD_SIZE_PX, THUMBNAIL_LOAD_SIZE_PX),
                                            null,
                                    )
                                    ?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
            if (imageBitmap != null) {
                thumbnailBitmap = imageBitmap
            }
        }
    }

    val hasCustomFileIcon = customIconSpec != null && customIcon != null
    val iconSize =
            when {
                iconOverride != null -> 34.dp
                hasCustomFileIcon -> CUSTOM_FILE_ICON_SIZE.dp
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
                    imageVector = customIcon ?: fileResultIcon(deviceFile),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(THUMBNAIL_SIZE_DP.dp).alpha(1f - thumbnailAlpha.value),
            )
            Image(
                    bitmap = thumbnailBitmap!!,
                    contentDescription = null,
                    modifier =
                            Modifier.size(THUMBNAIL_SIZE_DP.dp)
                                    .clip(DesignTokens.CardShape)
                                    .alpha(thumbnailAlpha.value),
                    contentScale = ContentScale.Crop,
            )
        }
    } else {
        if (hasCustomFileIcon) {
            val ratio = customIconSpec!!.aspectRatio
            val (iconWidth, iconHeight) =
                    if (ratio >= 1f) {
                        Pair(iconSize, iconSize / ratio)
                    } else {
                        Pair(iconSize * ratio, iconSize)
                    }
            Box(
                    modifier = modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
            ) {
                Icon(
                        imageVector = customIcon!!,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = iconWidth, height = iconHeight),
                )
            }
        } else {
            Icon(
                    imageVector = iconOverride ?: fileResultIcon(deviceFile),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = modifier.size(iconSize),
            )
        }
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
        onTriggerClick: (DeviceFile) -> Unit = {},
        hasTrigger: Boolean = false,
        enableLongPress: Boolean = true,
        onLongPressOverride: (() -> Unit)? = null,
        icon: ImageVector? = null,
        iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        isPredicted: Boolean = false,
) {
    val context = LocalContext.current
    val addToHomeHandler =
            remember(context) { com.tk.quicksearch.search.common.AddToHomeHandler(context) }
    var showOptions by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .topPredictedRowContainer(isTopPredicted = isPredicted)
                                .combinedClickable(
                                        onClick = {
                                            hapticConfirm(view)()
                                            onClick(deviceFile)
                                        },
                                        onLongClick = onLongPressOverride
                                                        ?: if (enableLongPress) {
                                                            { showOptions = true }
                                                        } else {
                                                            null
                                                        },
                                )
                                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                                .padding(vertical = DesignTokens.SpacingLarge),
        ) {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
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
                    hasTrigger = hasTrigger,
                    onTogglePin = { onTogglePin(deviceFile) },
                    onExclude = { onExclude(deviceFile) },
                    onExcludeExtension = { onExcludeExtension(deviceFile) },
                    onNicknameClick = { onNicknameClick(deviceFile) },
                    onTriggerClick = { onTriggerClick(deviceFile) },
                    onOpenFolderClick = { onOpenFolder(deviceFile) },
                    onFileInfoClick = { showFileInfoDialog = true },
                    onShareClick = { FileIntents.shareFile(context, deviceFile) },
                    onAddToHome = { addToHomeHandler.addFileToHome(deviceFile) },
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
    val moreActionColor = MaterialTheme.colorScheme.primary
    val moreTextStyle =
            if (LocalAppTheme.current == AppTheme.MONOCHROME) {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodySmall
            }

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
                text = stringResource(R.string.action_expand_more_files),
                style = moreTextStyle,
                color = moreActionColor,
        )
        Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.desc_expand),
                tint = moreActionColor,
                modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp),
        )
    }
}
