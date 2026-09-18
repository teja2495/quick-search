package com.tk.quicksearch.search.apps

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.appShortcuts.AppShortcutResultMenu
import com.tk.quicksearch.search.folders.folderMergePreview
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.preferences.ResultTrigger
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import java.util.Locale

private const val ShortcutAppBadgeScale = 0.42f
private const val ShortcutBadgeOffsetScale = 0.2f

/** Long-press actions for pinned app shortcuts shown in the app grid. */
data class AppGridShortcutActions(
        val onTogglePin: (StaticShortcut) -> Unit,
        val onDisable: (StaticShortcut) -> Unit,
        val onDisableAllForApp: (StaticShortcut) -> Unit,
        val onAppInfoClick: (StaticShortcut) -> Unit,
        val onNicknameClick: (StaticShortcut) -> Unit,
        val onTriggerClick: (StaticShortcut) -> Unit,
        val onEditCustomShortcut: (StaticShortcut) -> Unit,
        val onEditShortcutIcon: (StaticShortcut) -> Unit,
        val getNickname: (String) -> String?,
        val getTrigger: (String) -> ResultTrigger?,
)

/** A pinned app shortcut tile in the app grid: the shortcut icon, badged with its app's icon. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppShortcutGridItem(
        shortcut: StaticShortcut,
        onClick: (StaticShortcut) -> Unit,
        actions: AppGridShortcutActions?,
        iconPackPackage: String?,
        showLabel: Boolean,
        isOverlayPresentation: Boolean,
        appIconSizeStep: Int,
        appIconShape: AppIconShape,
        modifier: Modifier = Modifier,
        isDragging: Boolean = false,
        dragOffset: IntOffset? = null,
        onItemMeasured: (Int) -> Unit = {},
        onPinnedDragStart: (() -> Unit)? = null,
        onPinnedDrag: ((Float, Float) -> Unit)? = null,
        onPinnedDragEnd: ((Boolean) -> Unit)? = null,
        isMergeSource: Boolean = false,
        isMergeTarget: Boolean = false,
        showWallpaperBackground: Boolean = false,
        onHoldChange: ((Boolean) -> Unit)? = null,
) {
    val view = LocalView.current
    val displayName = shortcutDisplayName(shortcut)
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
    val badgeSize = iconSize * ShortcutAppBadgeScale
    val badgeOffset = badgeSize * ShortcutBadgeOffsetScale
    val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx().coerceAtLeast(1) }
    val shortcutIcon = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
    val appIcon =
            rememberAppIcon(
                    packageName = shortcut.packageName,
                    iconPackPackage = iconPackPackage,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
            ).bitmap
    var showOptions by remember { mutableStateOf(false) }
    var isLocalDragging by remember { mutableStateOf(false) }
    val showDraggedPresentation = isDragging || isLocalDragging
    val dragScale by animateFloatAsState(
            targetValue =
                    when {
                        showDraggedPresentation && isMergeSource -> MergeSourcePinnedAppScale
                        showDraggedPresentation -> DraggedPinnedAppScale
                        else -> 1f
                    },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedShortcutDragScale",
    )
    val dragAlpha by animateFloatAsState(
            targetValue = if (showDraggedPresentation) DraggedPinnedAppAlpha else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pinnedShortcutDragAlpha",
    )
    val dragModifier =
            rememberPinnedGridDragModifier(
                    key = shortcutKey(shortcut),
                    onClick = { onClick(shortcut) },
                    onShowOptions = { showOptions = true },
                    onLocalDraggingChange = { isLocalDragging = it },
                    onPinnedDragStart = onPinnedDragStart,
                    onPinnedDrag = onPinnedDrag,
                    onPinnedDragEnd = onPinnedDragEnd,
                    onHoldChange = onHoldChange,
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
                                onClick(shortcut)
                            }
                        },
                        onLongClick = { showOptions = true },
                )
            }

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
                                scaleX = dragScale
                                scaleY = dragScale
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
                                    .folderMergePreview(
                                            active = isMergeTarget,
                                            iconSize = iconSize,
                                            appIconShape = appIconShape,
                                            showWallpaperBackground = showWallpaperBackground,
                                    )
                                    .clip(DesignTokens.ShapeLarge)
                                    .then(dragModifier)
                                    .then(clickModifier),
                    contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    val mainIcon = shortcutIcon ?: appIcon
                    if (mainIcon != null) {
                        Image(
                                bitmap = mainIcon,
                                contentDescription = displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                                text =
                                        displayName.trim().take(1).uppercase(Locale.getDefault())
                                                .ifBlank { "?" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (shortcutIcon != null && appIcon != null) {
                        Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .offset(x = badgeOffset, y = badgeOffset)
                                                .size(badgeSize),
                                contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            if (showLabel) {
                AppLabelText(
                        appName = displayName,
                        isOverlayPresentation = isOverlayPresentation,
                )
            }
        }

        if (actions != null) {
            val key = shortcutKey(shortcut)
            AppShortcutResultMenu(
                    shortcut = shortcut,
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    isPinned = true,
                    hasNickname = !actions.getNickname(key).isNullOrBlank(),
                    hasTrigger = actions.getTrigger(key)?.word?.isNotBlank() == true,
                    onTogglePin = actions.onTogglePin,
                    onDisable = actions.onDisable,
                    onDisableAllForApp = actions.onDisableAllForApp,
                    onAppInfoClick = actions.onAppInfoClick,
                    onNicknameClick = actions.onNicknameClick,
                    onTriggerClick = actions.onTriggerClick,
                    onEditCustomShortcut = actions.onEditCustomShortcut,
                    onEditShortcutIcon = actions.onEditShortcutIcon,
                    iconPackPackage = iconPackPackage,
            )
        }
    }
}
