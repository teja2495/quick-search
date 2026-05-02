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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.ShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

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
        onTriggerClick: (StaticShortcut) -> Unit,
        onEditCustomShortcut: (StaticShortcut) -> Unit = {},
        onEditShortcutIcon: (StaticShortcut) -> Unit = {},
        getShortcutNickname: (String) -> String?,
        getShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        showAllResults: Boolean,
        showExpandControls: Boolean,
        onExpandClick: () -> Unit,
        expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
        iconPackPackage: String?,
        showWallpaperBackground: Boolean,
        predictedTarget: PredictedSubmitTarget? = null,
        fillExpandedHeight: Boolean = false,
) {
        val overlayCardColor = LocalOverlayResultCardColor.current
        val overlayDividerColor = LocalOverlayDividerColor.current
        if (shortcuts.isEmpty()) return

        val predictedShortcutId = (predictedTarget as? PredictedSubmitTarget.AppShortcut)?.id
        val hasPredictedShortcut =
                predictedShortcutId != null &&
                        shortcuts.any { shortcut -> shortcutKey(shortcut) == predictedShortcutId }
        val displayAsExpanded = isExpanded || showAllResults
        val useCardLevelPrediction =
                hasPredictedShortcut && (!displayAsExpanded || shortcuts.size == 1)

        val scrollState = rememberScrollState()

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
                ExpandableResultsCard(
                        resultCount = shortcuts.size,
                        isExpanded = isExpanded,
                        showAllResults = showAllResults,
                        isTopPredicted = useCardLevelPrediction,
                        showExpandControls = showExpandControls,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        hasScrollableContent = scrollState.maxValue > 0,
                        fillExpandedHeight = fillExpandedHeight,
                        showWallpaperBackground = showWallpaperBackground,
                        overlayCardColor = overlayCardColor,
                ) { contentModifier, cardState ->
                        val displayShortcuts =
                                if (cardState.displayAsExpanded) {
                                        shortcuts
                                } else {
                                        shortcuts.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
                                }
                        Column(
                                modifier =
                                        contentModifier.then(
                                                if (isExpanded) {
                                                        Modifier.verticalScroll(scrollState)
                                                } else {
                                                        Modifier
                                                },
                                        ),
                        ) {
                                AppShortcutsCardContent(
                                        displayShortcuts = displayShortcuts,
                                        overlayDividerColor = overlayDividerColor,
                                        showWallpaperBackground = showWallpaperBackground,
                                        pinnedShortcutIds = pinnedShortcutIds,
                                        excludedShortcutIds = excludedShortcutIds,
                                        onShortcutClick = onShortcutClick,
                                        onTogglePin = onTogglePin,
                                        onExclude = onExclude,
                                        onInclude = onInclude,
                                        onAppInfoClick = onAppInfoClick,
                                        onNicknameClick = onNicknameClick,
                                        onTriggerClick = onTriggerClick,
                                        onEditCustomShortcut = onEditCustomShortcut,
                                        onEditShortcutIcon = onEditShortcutIcon,
                                        getShortcutNickname = getShortcutNickname,
                                        getShortcutTrigger = getShortcutTrigger,
                                        iconPackPackage = iconPackPackage,
                                        shouldShowExpandButton = cardState.shouldShowExpandButton,
                                        onExpandClick = onExpandClick,
                                        predictedShortcutId = predictedShortcutId,
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
}

@Composable
private fun AppShortcutsCardContent(
        displayShortcuts: List<StaticShortcut>,
        overlayDividerColor: Color?,
        showWallpaperBackground: Boolean = false,
        pinnedShortcutIds: Set<String>,
        excludedShortcutIds: Set<String>,
        onShortcutClick: (StaticShortcut) -> Unit,
        onTogglePin: (StaticShortcut) -> Unit,
        onExclude: (StaticShortcut) -> Unit,
        onInclude: (StaticShortcut) -> Unit,
        onAppInfoClick: (StaticShortcut) -> Unit,
        onNicknameClick: (StaticShortcut) -> Unit,
        onTriggerClick: (StaticShortcut) -> Unit,
        onEditCustomShortcut: (StaticShortcut) -> Unit,
        onEditShortcutIcon: (StaticShortcut) -> Unit,
        getShortcutNickname: (String) -> String?,
        getShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        iconPackPackage: String?,
        shouldShowExpandButton: Boolean,
        onExpandClick: () -> Unit,
        predictedShortcutId: String?,
        useCardLevelPrediction: Boolean,
        bottomContentPadding: Dp,
) {
        Column(
                modifier =
                        Modifier.padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp)
                                .padding(bottom = bottomContentPadding),
        ) {
                displayShortcuts.forEachIndexed { index, shortcut ->
                        key(shortcutKey(shortcut)) {
                                val shortcutId = shortcutKey(shortcut)
                                val isPredictedShortcut =
                                        predictedShortcutId != null && shortcutId == predictedShortcutId
                                val showPredictedOnRow =
                                        isPredictedShortcut && !useCardLevelPrediction
                                AppShortcutRow(
                                        shortcut = shortcut,
                                        isPinned = pinnedShortcutIds.contains(shortcutId),
                                        isExcluded = excludedShortcutIds.contains(shortcutId),
                                        hasNickname = !getShortcutNickname(shortcutId).isNullOrBlank(),
                                        hasTrigger = getShortcutTrigger(shortcutId)?.word?.isNotBlank() == true,
                                        onShortcutClick = onShortcutClick,
                                        onTogglePin = onTogglePin,
                                        onExclude = onExclude,
                                        onInclude = onInclude,
                                        onAppInfoClick = onAppInfoClick,
                                        onNicknameClick = onNicknameClick,
                                        onTriggerClick = onTriggerClick,
                                        onEditCustomShortcut = onEditCustomShortcut,
                                        onEditShortcutIcon = onEditShortcutIcon,
                                        iconPackPackage = iconPackPackage,
                                        isPredicted = showPredictedOnRow,
                                )
                                if (index < displayShortcuts.lastIndex && !showPredictedOnRow) {
                                        HorizontalDivider(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = overlayDividerColor
                                                                ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                                        )
                                }
                        }
                }

                if (shouldShowExpandButton) {
                        ExpandButton(
                                onClick = onExpandClick,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                textResId = R.string.action_expand_more_shortcuts,
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
        hasTrigger: Boolean = false,
        onShortcutClick: (StaticShortcut) -> Unit,
        onTogglePin: (StaticShortcut) -> Unit,
        onExclude: (StaticShortcut) -> Unit,
        onInclude: (StaticShortcut) -> Unit,
        onAppInfoClick: (StaticShortcut) -> Unit,
        onNicknameClick: (StaticShortcut) -> Unit,
        onTriggerClick: (StaticShortcut) -> Unit = {},
        onEditCustomShortcut: (StaticShortcut) -> Unit,
        onEditShortcutIcon: (StaticShortcut) -> Unit,
        iconPackPackage: String?,
        showAppLabel: Boolean = true,
        subtitleText: String? = null,
        enableLongPress: Boolean = true,
        onLongPressOverride: (() -> Unit)? = null,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        iconTint: Color = MaterialTheme.colorScheme.secondary,
        isPredicted: Boolean = false,
) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val addToHomeHandler =
                remember(context) { com.tk.quicksearch.search.common.AddToHomeHandler(context) }
        var showOptions by remember { mutableStateOf(false) }
        val view = LocalView.current
        val displayName = shortcutDisplayName(shortcut)
        val iconSizePx = with(LocalDensity.current) { ICON_SIZE.dp.roundToPx() }
        val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
        val appIconResult =
                rememberAppIcon(
                        packageName = shortcut.packageName,
                        iconPackPackage = iconPackPackage
                )
        val hasEmbeddedOrOverrideIcon = !shortcut.iconBase64.isNullOrBlank()
        val displayIcon =
                if (hasEmbeddedOrOverrideIcon) {
                    iconBitmap
                } else {
                    iconBitmap ?: appIconResult.bitmap
                }
        if (displayIcon == null && icon == null && !hasEmbeddedOrOverrideIcon) return

        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .heightIn(min = ROW_MIN_HEIGHT.dp)
                                .topPredictedRowContainer(isTopPredicted = isPredicted)
                                .combinedClickable(
                                        onClick = {
                                                hapticConfirm(view)()
                                                onShortcutClick(shortcut)
                                        },
                                        onLongClick = onLongPressOverride
                                                        ?: if (enableLongPress) {
                                                                { showOptions = true }
                                                        } else {
                                                                null
                                                        },
                                )
                                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                                .padding(vertical = 12.dp),
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
                        val secondaryText =
                                if (showAppLabel) {
                                        subtitleText?.takeIf { it.isNotBlank() } ?: shortcut.appLabel
                                } else {
                                        null
                                }
                        Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                        )
                        if (!secondaryText.isNullOrBlank()) {
                                Text(
                                        text = secondaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                )
                        }
                }

                if (enableLongPress && onLongPressOverride == null) {
                        AppShortcutDropdownMenu(
                                shortcut = shortcut,
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                isPinned = isPinned,
                                isExcluded = isExcluded,
                                hasNickname = hasNickname,
                                hasTrigger = hasTrigger,
                                onTogglePin = { onTogglePin(shortcut) },
                                onExclude = { onExclude(shortcut) },
                                onInclude = { onInclude(shortcut) },
                                onAppInfoClick = { onAppInfoClick(shortcut) },
                                onNicknameClick = { onNicknameClick(shortcut) },
                                onTriggerClick = { onTriggerClick(shortcut) },
                                onEditCustomShortcut = onEditCustomShortcut,
                                onEditShortcutIcon = onEditShortcutIcon,
                                onAddToHome = { addToHomeHandler.addAppShortcutToHome(shortcut) },
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
        shortcut: StaticShortcut,
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        isPinned: Boolean,
        isExcluded: Boolean,
        hasNickname: Boolean,
        hasTrigger: Boolean,
        onTogglePin: () -> Unit,
        onExclude: () -> Unit,
        onInclude: () -> Unit,
        onAppInfoClick: () -> Unit,
        onNicknameClick: () -> Unit,
        onTriggerClick: () -> Unit,
        onEditCustomShortcut: (StaticShortcut) -> Unit,
        onEditShortcutIcon: (StaticShortcut) -> Unit,
        onAddToHome: () -> Unit,
) {
        androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                properties = PopupProperties(focusable = false),
                containerColor = AppColors.DialogBackground,
        ) {
                val menuItems = buildList {
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
                                                if (hasTrigger) {
                                                        R.string.action_edit_trigger
                                                } else {
                                                        R.string.action_add_trigger
                                                },
                                        icon = {
                                                Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = null,
                                                )
                                        },
                                        onClick = {
                                                onDismissRequest()
                                                onTriggerClick()
                                        },
                                ),
                        )
                        add(
                                AppShortcutMenuItem(
                                        textResId = R.string.action_add_to_home,
                                        icon = {
                                                Icon(
                                                        imageVector = Icons.Rounded.Home,
                                                        contentDescription = null,
                                                )
                                        },
                                        onClick = {
                                                onDismissRequest()
                                                onAddToHome()
                                        },
                                ),
                        )
                        add(
                                AppShortcutMenuItem(
                                        textResId =
                                                if (isPinned) {
                                                        R.string.action_unpin_app
                                                } else {
                                                        R.string.action_pin_app
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
                        if (isUserCreatedShortcut(shortcut)) {
                                add(
                                        AppShortcutMenuItem(
                                                textResId = R.string.settings_edit_label,
                                                icon = {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Edit,
                                                                contentDescription = null,
                                                        )
                                                },
                                                onClick = {
                                                        onDismissRequest()
                                                        onEditCustomShortcut(shortcut)
                                                },
                                        ),
                                )
                        } else {
                                add(
                                        AppShortcutMenuItem(
                                                textResId = R.string.action_edit_icon,
                                                icon = {
                                                        Icon(
                                                                imageVector = Icons.Rounded.Edit,
                                                                contentDescription = null,
                                                        )
                                                },
                                                onClick = {
                                                        onDismissRequest()
                                                        onEditShortcutIcon(shortcut)
                                                },
                                        ),
                                )
                        }
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
