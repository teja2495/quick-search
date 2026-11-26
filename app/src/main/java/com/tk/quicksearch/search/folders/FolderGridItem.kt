package com.tk.quicksearch.search.folders

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import com.tk.quicksearch.search.apps.AppGridPressedScale
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
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.data.appShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.apps.swipeGestures.AppSwipeDirection
import com.tk.quicksearch.search.apps.swipeGestures.AppSwipeGestures
import com.tk.quicksearch.search.apps.swipeGestures.rememberSwipeActions
import com.tk.quicksearch.search.apps.swipeGestures.swipeGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.components.ItemMenuRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private const val FolderPreviewSlotCount = 4
// Four icons in a 2×2 grid whose corners just fit inside the round backdrop.
private const val FolderPreviewIconScale = 0.34f
private const val FolderPreviewGapScale = 0.03f
private const val FolderMergeTargetScale = 1.12f
private val FolderBorderWidth = 1.dp
// Matches the app icon in an app's long-press menu header.

/** Folders are always round, whatever the app icon shape. */
internal val FolderBackdropShape: Shape = AppShapes.full

@Composable
internal fun folderBackdropColor(showWallpaperBackground: Boolean, color: Int? = null): Color =
        when {
            color != null -> folderColorBackdrop(color, showWallpaperBackground)
            !showWallpaperBackground -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> AppColors.FolderWallpaperBackdrop
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
        mergePreviewMember: AppFolderMember? = null,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: ((Boolean) -> Unit)? = null,
        onMoveToWidgetsPanel: (() -> Unit)? = null,
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
    var isHeld by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val showDraggedPresentation = isDragging || isLocalDragging
    val pressScale by animateFloatAsState(
        targetValue = if ((isPressed || isHeld) && !showDraggedPresentation && !isMergeTarget) AppGridPressedScale else 1f,
        animationSpec = AppMotion.fastSpatial(),
        label = "gridIconPressScale",
    )
    val tileScale by
            animateFloatAsState(
                    targetValue =
                            when {
                                showDraggedPresentation -> DraggedPinnedAppScale
                                isMergeTarget -> FolderMergeTargetScale
                                else -> 1f
                            },
                    animationSpec = AppMotion.dragFeedback(),
                    label = "folderTileScale",
            )
    val dragAlpha by
            animateFloatAsState(
                    targetValue = if (showDraggedPresentation) DraggedPinnedAppAlpha else 1f,
                    animationSpec = AppMotion.dragFeedback(),
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
                    onHoldChange = { isHeld = it },
            )
    val isDraggable = onPinnedDragStart != null && onPinnedDrag != null && onPinnedDragEnd != null
    val clickModifier =
            if (isDraggable) {
                Modifier
            } else {
                Modifier.combinedClickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(color = homeTextColor()),
                        onClick = {
                            if (!showOptions) {
                                hapticConfirm(view)()
                                onClick()
                            }
                        },
                        onLongClickLabel = stringResource(R.string.desc_show_options),
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
                                        scaleX = tileScale * pressScale
                                        scaleY = tileScale * pressScale
                                    }
                                    .clip(AppShapes.large)
                                    .swipeGestures(folder.folder.gridKey)
                                    .then(dragModifier)
                                    .then(clickModifier)
                                    .semantics { contentDescription = folderLabel },
                    contentAlignment = Alignment.Center,
            ) {
                val previewIncomingMember =
                        mergePreviewMember?.takeIf { isMergeTarget && folder.members.size < FolderPreviewSlotCount }
                AnimatedContent(
                        targetState = previewIncomingMember,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "folderMergeMemberPreview",
                ) { incomingMember ->
                    FolderPreviewIcon(
                            members =
                                    if (incomingMember == null) {
                                        folder.members
                                    } else {
                                        folder.members + incomingMember
                                    },
                            iconSize = iconSize,
                            iconPackPackage = iconPackPackage,
                            appIconShape = appIconShape,
                            showWallpaperBackground = showWallpaperBackground,
                            color = folder.color,
                    )
                }
            }
            if (showLabel) {
                AppLabelText(
                        appName = folderLabel,
                        isOverlayPresentation = isOverlayPresentation,
                )
            }
        }

        val dismissOptions = { showOptions = false }
        FolderItemMenu(
                folder = folder,
                expanded = showOptions,
                onDismiss = dismissOptions,
                onDelete = onDelete,
                rows =
                        listOfNotNull(
                                onMoveToWidgetsPanel?.let { move ->
                                    moveFolderToWidgetsPanelRow(onDismiss = dismissOptions, onMove = move)
                                },
                        ),
        )
    }
}

/**
 * Long-press menu for a folder tile: the same compact dropdown as calendar events, anchored to the
 * tile, with the folder's Swipe Up / Swipe Down actions ([showSwipeGestures]), [rows] such as
 * moving the folder, and Delete.
 */
@Composable
internal fun FolderItemMenu(
        folder: ResolvedAppFolder,
        expanded: Boolean,
        onDismiss: () -> Unit,
        onDelete: () -> Unit,
        rows: List<ItemMenuRow>,
        showSwipeGestures: Boolean = true,
) {
    val folderKey = folder.folder.gridKey
    val folderLabel = folder.name.ifBlank { stringResource(R.string.excluded_item_type_folder) }
    val (swipeUpAction, swipeDownAction) = rememberSwipeActions(folderKey)
    val swipeItems =
            if (showSwipeGestures) {
                listOf(
                        Triple(AppSwipeDirection.UP, R.string.settings_gesture_swipe_up, swipeUpAction),
                        Triple(AppSwipeDirection.DOWN, R.string.settings_gesture_swipe_down, swipeDownAction),
                ).map { (direction, labelRes, assignedAction) ->
                    ItemMenuRow(
                            label = stringResource(labelRes),
                            icon = {
                                Icon(
                                        imageVector =
                                                if (direction == AppSwipeDirection.UP) {
                                                    MaterialSymbols.SwipeUp
                                                } else {
                                                    MaterialSymbols.SwipeDown
                                                },
                                        contentDescription = null,
                                        tint =
                                                if (assignedAction != null) {
                                                    AppColors.ItemMenuActiveIconTint
                                                } else {
                                                    LocalContentColor.current
                                                },
                                )
                            },
                            onClick = {
                                onDismiss()
                                AppSwipeGestures.requestPicker(folderKey, folderLabel, direction)
                            },
                    )
                }
            } else {
                emptyList()
            }
    val deleteItem =
            ItemMenuRow(
                    label = stringResource(R.string.dialog_delete),
                    icon = { Icon(imageVector = MaterialSymbols.DeleteOutline, contentDescription = null) },
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    destructive = true,
            )
    AppDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        (swipeItems + rows + deleteItem).forEachIndexed { index, item ->
            if (index > 0) AppDropdownMenuDivider()
            AppDropdownMenuItem(
                    text = item.label,
                    leadingIcon = item.icon,
                    onClick = item.onClick,
                    destructive = item.destructive,
            )
        }
    }
}

/** The folder menu's "Move to Widget Panel" row. */
@Composable
internal fun moveFolderToWidgetsPanelRow(onDismiss: () -> Unit, onMove: () -> Unit): ItemMenuRow =
        ItemMenuRow(
                label = stringResource(R.string.widget_move_to_panel),
                icon = { Icon(imageVector = MaterialSymbols.Widgets, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onMove()
                },
        )

/** Round folder backdrop with a 2×2 preview of the first four member icons. */
@Composable
internal fun FolderPreviewIcon(
        members: List<AppFolderMember>,
        iconSize: Dp,
        iconPackPackage: String?,
        appIconShape: AppIconShape,
        showWallpaperBackground: Boolean,
        color: Int? = null,
) {
    val previewIconSize = iconSize * FolderPreviewIconScale
    val gap = iconSize * FolderPreviewGapScale
    val previewMembers = members.take(FolderPreviewSlotCount)
    Box(
            modifier =
                    Modifier.size(iconSize)
                            .clip(FolderBackdropShape)
                            .background(folderBackdropColor(showWallpaperBackground, color))
                            .border(
                                    width = FolderBorderWidth,
                                    color = AppColors.FolderTileBorder,
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
internal fun FolderPreviewMemberIcon(
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
