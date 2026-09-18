package com.tk.quicksearch.search.folders

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppLabelText
import com.tk.quicksearch.search.apps.DraggedPinnedAppAlpha
import com.tk.quicksearch.search.apps.DraggedPinnedAppScale
import com.tk.quicksearch.search.apps.OverlayAppIconSize
import com.tk.quicksearch.search.apps.OverlayAppIconSurfaceSize
import com.tk.quicksearch.search.apps.RegularAppIconSize
import com.tk.quicksearch.search.apps.TopResultIndicatorBottomPadding
import com.tk.quicksearch.search.apps.TopResultIndicatorTopPadding
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.apps.rememberPinnedGridDragModifier
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.ui.components.ItemMenuLongPressDropdown
import com.tk.quicksearch.shared.ui.components.ItemMenuLongPressOption
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticConfirm

private const val FolderPreviewSlotCount = 4
// Four icons in a 2×2 grid whose corners just fit inside the round backdrop.
private const val FolderPreviewIconScale = 0.34f
private const val FolderPreviewGapScale = 0.03f
private const val FolderMergeTargetScale = 1.12f
private val FolderBorderWidth = 1.dp
private const val FolderBorderAlpha = 0.25f
private const val FolderBackdropDarkWallpaperAlpha = 0.55f
private const val FolderBackdropLightWallpaperAlpha = 0.65f
private const val MergePreviewBackdropScale = 1.22f
private const val MergePreviewIconScale = 0.82f

/** Folders are always round, whatever the app icon shape. */
internal val FolderBackdropShape: Shape = CircleShape

@Composable
internal fun folderBackdropColor(showWallpaperBackground: Boolean): Color =
        when {
            !showWallpaperBackground -> MaterialTheme.colorScheme.surfaceContainerHighest
            LocalAppIsDarkTheme.current -> Color.Black.copy(alpha = FolderBackdropDarkWallpaperAlpha)
            else -> Color.White.copy(alpha = FolderBackdropLightWallpaperAlpha)
        }

/**
 * Folder preview shown behind an app or shortcut icon while another item is held over it: a folder
 * backdrop grows in behind the icon, which shrinks slightly as if it were already inside.
 */
@Composable
internal fun Modifier.folderMergePreview(
        active: Boolean,
        iconSize: Dp,
        appIconShape: AppIconShape,
        showWallpaperBackground: Boolean,
): Modifier {
    val progress by
            animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "folderMergePreview",
            )
    val backdropColor = folderBackdropColor(showWallpaperBackground)
    val backdropShape = FolderBackdropShape
    val iconSizePx = with(LocalDensity.current) { iconSize.toPx() }
    return this
            .drawBehind {
                if (progress <= 0f) return@drawBehind
                val backdropSize = iconSizePx * (1f + (MergePreviewBackdropScale - 1f) * progress)
                val outline =
                        backdropShape.createOutline(
                                Size(backdropSize, backdropSize),
                                layoutDirection,
                                this,
                        )
                translate(
                        left = (size.width - backdropSize) / 2f,
                        top = (size.height - backdropSize) / 2f,
                ) {
                    drawOutline(
                            outline = outline,
                            color = backdropColor.copy(alpha = backdropColor.alpha * progress),
                    )
                }
            }
            .graphicsLayer {
                val scale = 1f - (1f - MergePreviewIconScale) * progress
                scaleX = scale
                scaleY = scale
            }
}

/**
 * A Pinned-tab folder tile, the same size as an app cell: a folder backdrop previewing the first
 * four members, and the folder name. Unnamed folders keep an empty label so rows stay aligned.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderGridItem(
        folder: ResolvedAppFolder,
        onClick: () -> Unit,
        onDelete: () -> Unit,
        iconPackPackage: String?,
        showLabel: Boolean,
        isOverlayPresentation: Boolean,
        appIconSizeStep: Int,
        appIconShape: AppIconShape,
        showWallpaperBackground: Boolean,
        modifier: Modifier = Modifier,
        isDragging: Boolean = false,
        isMergeTarget: Boolean = false,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: ((Boolean) -> Unit)? = null,
) {
    val view = LocalView.current
    val sizeScale = UiPreferences.appIconSizeScale(appIconSizeStep)
    val iconSurfaceSize =
            if (isOverlayPresentation) {
                OverlayAppIconSurfaceSize * sizeScale
            } else {
                DesignTokens.AppIconSize * sizeScale
            }
    val iconSize =
            if (isOverlayPresentation) {
                OverlayAppIconSize * sizeScale
            } else {
                RegularAppIconSize * sizeScale
            }
    var showOptions by remember { mutableStateOf(false) }
    var isLocalDragging by remember { mutableStateOf(false) }
    val showDraggedPresentation = isDragging || isLocalDragging
    val tileScale by
            animateFloatAsState(
                    targetValue =
                            when {
                                showDraggedPresentation -> DraggedPinnedAppScale
                                isMergeTarget -> FolderMergeTargetScale
                                else -> 1f
                            },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "folderTileScale",
            )
    val dragAlpha by
            animateFloatAsState(
                    targetValue = if (showDraggedPresentation) DraggedPinnedAppAlpha else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "folderTileDragAlpha",
            )
    val dragModifier =
            rememberPinnedGridDragModifier(
                    key = folder.folder.gridKey,
                    onClick = onClick,
                    onShowOptions = { showOptions = true },
                    onLocalDraggingChange = { isLocalDragging = it },
                    onPinnedDragStart = onPinnedDragStart,
                    onPinnedDrag = onPinnedDrag,
                    onPinnedDragEnd = onPinnedDragEnd,
            )
    val isDraggable = onPinnedDragStart != null && onPinnedDrag != null && onPinnedDragEnd != null
    val clickModifier =
            if (isDraggable) {
                Modifier
            } else {
                Modifier.combinedClickable(
                        onClick = {
                            if (!showOptions) {
                                hapticConfirm(view)()
                                onClick()
                            }
                        },
                        onLongClick = { showOptions = true },
                )
            }
    val folderLabel = folder.name.ifBlank { stringResource(R.string.excluded_item_type_folder) }

    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (coordinates.size.height > 0) {
                                    onItemMeasured(coordinates.size.height)
                                }
                            }
                            .zIndex(if (showDraggedPresentation) 1f else 0f)
                            .graphicsLayer {
                                if (showDraggedPresentation && dragOffset != null) {
                                    translationX = dragOffset.x.toFloat()
                                    translationY = dragOffset.y.toFloat()
                                }
                                alpha = dragAlpha
                            },
            contentAlignment = Alignment.TopCenter,
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(
                                        top = TopResultIndicatorTopPadding,
                                        bottom = TopResultIndicatorBottomPadding,
                                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            Box(
                    modifier =
                            Modifier.requiredSize(iconSurfaceSize)
                                    .graphicsLayer {
                                        scaleX = tileScale
                                        scaleY = tileScale
                                    }
                                    .clip(DesignTokens.ShapeLarge)
                                    .then(dragModifier)
                                    .then(clickModifier)
                                    .semantics { contentDescription = folderLabel },
                    contentAlignment = Alignment.Center,
            ) {
                FolderPreviewIcon(
                        members = folder.members,
                        iconSize = iconSize,
                        iconPackPackage = iconPackPackage,
                        appIconShape = appIconShape,
                        showWallpaperBackground = showWallpaperBackground,
                )
            }
            if (showLabel) {
                AppLabelText(
                        appName = folder.name,
                        isOverlayPresentation = isOverlayPresentation,
                )
            }
        }

        ItemMenuLongPressDropdown(
                option =
                        ItemMenuLongPressOption(
                                label = stringResource(R.string.dialog_delete),
                                icon = Icons.Rounded.Delete,
                                onClick = onDelete,
                        ),
                expanded = showOptions,
                onDismiss = { showOptions = false },
        )
    }
}

/** Round folder backdrop with a 2×2 preview of the first four member icons. */
@Composable
private fun FolderPreviewIcon(
        members: List<AppFolderMember>,
        iconSize: Dp,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        showWallpaperBackground: Boolean,
) {
    val previewIconSize = iconSize * FolderPreviewIconScale
    val gap = iconSize * FolderPreviewGapScale
    val previewMembers = members.take(FolderPreviewSlotCount)
    Box(
            modifier =
                    Modifier.size(iconSize)
                            .clip(FolderBackdropShape)
                            .background(folderBackdropColor(showWallpaperBackground))
                            .border(
                                    width = FolderBorderWidth,
                                    color = Color.White.copy(alpha = FolderBorderAlpha),
                                    shape = FolderBackdropShape,
                            ),
            contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            previewMembers.chunked(2).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowMembers.forEach { member ->
                        FolderPreviewMemberIcon(
                                member = member,
                                size = previewIconSize,
                                iconPackPackage = iconPackPackage,
                                appIconShape = appIconShape,
                        )
                    }
                    if (rowMembers.size == 1) Spacer(Modifier.size(previewIconSize))
                }
            }
        }
    }
}

@Composable
private fun FolderPreviewMemberIcon(
        member: AppFolderMember,
        size: Dp,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
) {
    val iconSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmap =
            when (member) {
                is AppFolderMember.App ->
                        rememberAppIcon(
                                packageName = member.app.packageName,
                                iconPackPackage = iconPackPackage,
                                userHandleId = member.app.userHandleId,
                                forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                        ).bitmap
                is AppFolderMember.Shortcut -> {
                    val shortcutIcon =
                            rememberShortcutIcon(shortcut = member.shortcut, iconSizePx = iconSizePx)
                    val appIcon =
                            rememberAppIcon(
                                    packageName = member.shortcut.packageName,
                                    iconPackPackage = iconPackPackage,
                                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                            ).bitmap
                    shortcutIcon ?: appIcon
                }
            }
    if (bitmap != null) {
        Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit,
        )
    } else {
        Spacer(Modifier.size(size))
    }
}
