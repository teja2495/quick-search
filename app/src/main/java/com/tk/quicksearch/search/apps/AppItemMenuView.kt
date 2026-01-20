package com.tk.quicksearch.search.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.rememberShortcutIcon
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.ui.theme.DesignTokens

/** Menu item data class for app dropdown menu. */
private data class AppMenuItem(
        val textResId: Int,
        val icon: @Composable () -> Unit,
        val onClick: () -> Unit
)

/** Dropdown menu for app grid items with actions like info, hide, pin/unpin, and uninstall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItemDropdownMenu(
        expanded: Boolean,
        onDismiss: () -> Unit,
        isPinned: Boolean,
        showUninstall: Boolean,
        hasNickname: Boolean,
        shortcuts: List<StaticShortcut>,
        onShortcutClick: (StaticShortcut) -> Unit,
        onAppInfoClick: () -> Unit,
        onHideApp: () -> Unit,
        onPinApp: () -> Unit,
        onUnpinApp: () -> Unit,
        onUninstallClick: () -> Unit,
        onNicknameClick: () -> Unit
) {
    DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            modifier = Modifier.padding(vertical = 0.dp)
    ) {
        val menuItems = buildList {
            add(
                    AppMenuItem(
                            textResId = R.string.action_app_info,
                            icon = {
                                Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
                            },
                            onClick = {
                                onDismiss()
                                onAppInfoClick()
                            }
                    )
            )
            add(
                    AppMenuItem(
                            textResId = R.string.action_hide_app,
                            icon = {
                                Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null
                                )
                            },
                            onClick = {
                                onDismiss()
                                onHideApp()
                            }
                    )
            )
            add(
                    AppMenuItem(
                            textResId =
                                    if (isPinned) R.string.action_unpin_app
                                    else R.string.action_pin_app,
                            icon = {
                                Icon(
                                        painter =
                                                painterResource(
                                                        if (isPinned) R.drawable.ic_unpin
                                                        else R.drawable.ic_pin
                                                ),
                                        contentDescription = null
                                )
                            },
                            onClick = {
                                onDismiss()
                                if (isPinned) {
                                    onUnpinApp()
                                } else {
                                    onPinApp()
                                }
                            }
                    )
            )
            add(
                    AppMenuItem(
                            textResId =
                                    if (hasNickname) R.string.action_edit_nickname
                                    else R.string.action_add_nickname,
                            icon = {
                                Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                            },
                            onClick = {
                                onDismiss()
                                onNicknameClick()
                            }
                    )
            )
            if (showUninstall) {
                add(
                        AppMenuItem(
                                textResId = R.string.action_uninstall_app,
                                icon = {
                                    Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = null
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    onUninstallClick()
                                }
                        )
                )
            }
        }

        if (shortcuts.isEmpty()) {
            menuItems.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                        text = { Text(text = stringResource(item.textResId)) },
                        leadingIcon = { item.icon() },
                        onClick = item.onClick
                )
            }
        } else {
            val shortcutIconSize = DesignTokens.IconSize
            val density = LocalDensity.current
            val shortcutIconSizePx =
                    remember(shortcutIconSize, density) {
                        with(density) { shortcutIconSize.roundToPx().coerceAtLeast(1) }
                    }

            shortcuts.forEachIndexed { index, shortcut ->
                val displayName = shortcutDisplayName(shortcut)
                val iconBitmap = rememberShortcutIcon(shortcut, shortcutIconSizePx)
                DropdownMenuItem(
                        text = {
                            Text(text = displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingIcon = {
                            if (iconBitmap != null) {
                                androidx.compose.foundation.Image(
                                        bitmap = iconBitmap,
                                        contentDescription = displayName,
                                        modifier = Modifier.size(shortcutIconSize),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                androidx.compose.foundation.layout.Box(
                                        modifier = Modifier.size(shortcutIconSize),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            text = displayName.trim().take(1).uppercase(),
                                            style =
                                                    androidx.compose.material3.MaterialTheme
                                                            .typography
                                                            .bodyMedium
                                    )
                                }
                            }
                        },
                        onClick = {
                            onDismiss()
                            onShortcutClick(shortcut)
                        },
                        modifier = Modifier.padding(start = DesignTokens.SpacingXSmall)
                )
                if (index < shortcuts.lastIndex) {
                    HorizontalDivider()
                }
            }

            HorizontalDivider()
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(
                                            horizontal = DesignTokens.SpacingSmall,
                                            vertical = DesignTokens.SpacingSmall
                                    ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                menuItems.forEach { item ->
                    val contentDescription = stringResource(item.textResId)
                    IconButton(
                            onClick = item.onClick,
                            modifier =
                                    Modifier.size(40.dp).semantics {
                                        this.contentDescription = contentDescription
                                    }
                    ) { item.icon() }
                }
            }
        }
    }
}
