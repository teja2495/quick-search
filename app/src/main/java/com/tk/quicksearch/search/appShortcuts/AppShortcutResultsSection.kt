package com.tk.quicksearch.search.appShortcuts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.ExpandButton
import com.tk.quicksearch.search.data.ShortcutIcon
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.rememberShortcutIcon
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticConfirm

private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 32
private const val OVERRIDE_ICON_SIZE = 26

@Composable
fun AppShortcutResultsSection(
    shortcuts: List<StaticShortcut>,
    isExpanded: Boolean,
    pinnedShortcutIds: Set<String>,
    excludedShortcutIds: Set<String>,
    onShortcutClick: (StaticShortcut) -> Unit,
    onTogglePin: (StaticShortcut) -> Unit,
    onExclude: (StaticShortcut) -> Unit,
    onInclude: (StaticShortcut) -> Unit,
    onAppInfoClick: (StaticShortcut) -> Unit,
    onNicknameClick: (StaticShortcut) -> Unit,
    getShortcutNickname: (String) -> String?,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    iconPackPackage: String?,
    showWallpaperBackground: Boolean,
) {
    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    if (shortcuts.isEmpty()) return

    val displayShortcuts =
        if (isExpanded || showAllResults) {
            shortcuts
        } else {
            shortcuts.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
        }

    val canShowExpandControls =
        showExpandControls && shortcuts.size > SearchScreenConstants.INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !isExpanded && !showAllResults && canShowExpandControls
    val shouldShowCollapseButton = isExpanded && showExpandControls

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        val cardModifier = Modifier.fillMaxWidth()
        val cardColors =
            if (overlayCardColor != null) {
                CardDefaults.cardColors(containerColor = overlayCardColor)
            } else {
                AppColors.getCardColors(showWallpaperBackground = showWallpaperBackground)
            }
        val cardElevation =
            AppColors.getCardElevation(
                showWallpaperBackground = showWallpaperBackground,
            )

        val cardContent =
            @Composable
            {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (isExpanded) {
                                    Modifier
                                        .heightIn(
                                            max =
                                                SearchScreenConstants
                                                    .EXPANDED_CARD_MAX_HEIGHT,
                                        ).verticalScroll(
                                            scrollState,
                                        )
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    AppShortcutsCardContent(
                        displayShortcuts = displayShortcuts,
                        overlayDividerColor = overlayDividerColor,
                        pinnedShortcutIds = pinnedShortcutIds,
                        excludedShortcutIds = excludedShortcutIds,
                        onShortcutClick = onShortcutClick,
                        onTogglePin = onTogglePin,
                        onExclude = onExclude,
                        onInclude = onInclude,
                        onAppInfoClick = onAppInfoClick,
                        onNicknameClick = onNicknameClick,
                        getShortcutNickname = getShortcutNickname,
                        iconPackPackage = iconPackPackage,
                        shouldShowExpandButton = shouldShowExpandButton,
                        onExpandClick = onExpandClick,
                    )
                }
            }

        if (showWallpaperBackground) {
            Card(
                modifier = cardModifier,
                colors = cardColors,
                shape = MaterialTheme.shapes.extraLarge,
                elevation = cardElevation,
            ) { cardContent() }
        } else {
            ElevatedCard(
                modifier = cardModifier,
                colors = cardColors,
                shape = MaterialTheme.shapes.extraLarge,
                elevation = cardElevation,
            ) { cardContent() }
        }
    }
}

@Composable
private fun AppShortcutsCardContent(
    displayShortcuts: List<StaticShortcut>,
    overlayDividerColor: Color?,
    pinnedShortcutIds: Set<String>,
    excludedShortcutIds: Set<String>,
    onShortcutClick: (StaticShortcut) -> Unit,
    onTogglePin: (StaticShortcut) -> Unit,
    onExclude: (StaticShortcut) -> Unit,
    onInclude: (StaticShortcut) -> Unit,
    onAppInfoClick: (StaticShortcut) -> Unit,
    onNicknameClick: (StaticShortcut) -> Unit,
    getShortcutNickname: (String) -> String?,
    iconPackPackage: String?,
    shouldShowExpandButton: Boolean,
    onExpandClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp),
    ) {
        displayShortcuts.forEachIndexed { index, shortcut ->
            val shortcutId = shortcutKey(shortcut)
            AppShortcutRow(
                shortcut = shortcut,
                isPinned = pinnedShortcutIds.contains(shortcutId),
                isExcluded = excludedShortcutIds.contains(shortcutId),
                hasNickname = !getShortcutNickname(shortcutId).isNullOrBlank(),
                onShortcutClick = onShortcutClick,
                onTogglePin = onTogglePin,
                onExclude = onExclude,
                onInclude = onInclude,
                onAppInfoClick = onAppInfoClick,
                onNicknameClick = onNicknameClick,
                iconPackPackage = iconPackPackage,
            )
            if (index < displayShortcuts.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = overlayDividerColor ?: MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        if (shouldShowExpandButton) {
            ExpandButton(
                onClick = onExpandClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppShortcutRow(
    shortcut: StaticShortcut,
    isPinned: Boolean,
    isExcluded: Boolean,
    hasNickname: Boolean,
    onShortcutClick: (StaticShortcut) -> Unit,
    onTogglePin: (StaticShortcut) -> Unit,
    onExclude: (StaticShortcut) -> Unit,
    onInclude: (StaticShortcut) -> Unit,
    onAppInfoClick: (StaticShortcut) -> Unit,
    onNicknameClick: (StaticShortcut) -> Unit,
    iconPackPackage: String?,
    showAppLabel: Boolean = true,
    enableLongPress: Boolean = true,
    onLongPressOverride: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
) {
    var showOptions by remember { mutableStateOf(false) }
    val view = LocalView.current
    val displayName = shortcutDisplayName(shortcut)
    val iconSizePx = with(LocalDensity.current) { ICON_SIZE.dp.roundToPx() }
    val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
    val appIconResult = rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)
    val displayIcon = iconBitmap ?: appIconResult.bitmap

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT.dp)
                .clip(DesignTokens.CardShape)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(view)()
                        onShortcutClick(shortcut)
                    },
                    onLongClick =
                        onLongPressOverride
                            ?: if (enableLongPress) {
                                { showOptions = true }
                            } else {
                                null
                            },
                ).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val leadingIconStartPadding =
            if (icon != null) DesignTokens.SpacingSmall else DesignTokens.SpacingXSmall
        Box(modifier = Modifier.padding(start = leadingIconStartPadding)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(OVERRIDE_ICON_SIZE.dp),
                )
            } else {
                ShortcutIcon(
                    icon = displayIcon,
                    displayName = displayName,
                    size = ICON_SIZE.dp,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showAppLabel) {
                Text(
                    text = shortcut.appLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (enableLongPress && onLongPressOverride == null) {
            AppShortcutDropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false },
                isPinned = isPinned,
                isExcluded = isExcluded,
                hasNickname = hasNickname,
                onTogglePin = { onTogglePin(shortcut) },
                onExclude = { onExclude(shortcut) },
                onInclude = { onInclude(shortcut) },
                onAppInfoClick = { onAppInfoClick(shortcut) },
                onNicknameClick = { onNicknameClick(shortcut) },
            )
        }
    }
}

private data class AppShortcutMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

@Composable
private fun AppShortcutDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isPinned: Boolean,
    isExcluded: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onInclude: () -> Unit,
    onAppInfoClick: () -> Unit,
    onNicknameClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false),
        containerColor = AppColors.DialogBackground,
    ) {
        val menuItems =
            buildList {
                add(
                    AppShortcutMenuItem(
                        textResId = R.string.action_app_info,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            onAppInfoClick()
                        },
                    ),
                )
                add(
                    AppShortcutMenuItem(
                        textResId =
                            if (isPinned) {
                                R.string.action_unpin_generic
                            } else {
                                R.string.action_pin_generic
                            },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (isPinned) {
                                            R.drawable.ic_unpin
                                        } else {
                                            R.drawable.ic_pin
                                        },
                                    ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            onTogglePin()
                        },
                    ),
                )
                add(
                    AppShortcutMenuItem(
                        textResId =
                            if (hasNickname) {
                                R.string.action_edit_nickname
                            } else {
                                R.string.action_add_nickname
                            },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            onNicknameClick()
                        },
                    ),
                )
                add(
                    AppShortcutMenuItem(
                        textResId =
                            if (isExcluded) {
                                R.string.action_include_generic
                            } else {
                                R.string.action_exclude_generic
                            },
                        icon = {
                            Icon(
                                imageVector =
                                    if (isExcluded) {
                                        Icons.Rounded.Visibility
                                    } else {
                                        Icons.Rounded.VisibilityOff
                                    },
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            if (isExcluded) {
                                onInclude()
                            } else {
                                onExclude()
                            }
                        },
                    ),
                )
            }

        menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(item.textResId)) },
                leadingIcon = { item.icon() },
                onClick = item.onClick,
            )
        }
    }
}
