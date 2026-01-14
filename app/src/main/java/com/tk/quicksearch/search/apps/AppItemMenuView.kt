package com.tk.quicksearch.search.apps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Menu item data class for app dropdown menu.
 */
private data class AppMenuItem(
    val textResId: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

/**
 * Dropdown menu for app grid items with actions like info, hide, pin/unpin, and uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItemDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isPinned: Boolean,
    showUninstall: Boolean,
    hasNickname: Boolean,
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
        properties = PopupProperties(focusable = false)
    ) {
        val menuItems = buildList {
            add(
                AppMenuItem(
                    textResId = R.string.action_app_info,
                    icon = Icons.Rounded.Info,
                    onClick = {
                        onDismiss()
                        onAppInfoClick()
                    }
                )
            )
            add(
                AppMenuItem(
                    textResId = R.string.action_hide_app,
                    icon = Icons.Rounded.VisibilityOff,
                    onClick = {
                        onDismiss()
                        onHideApp()
                    }
                )
            )
            add(
                AppMenuItem(
                    textResId = if (isPinned) R.string.action_unpin_app else R.string.action_pin_app,
                    icon = if (isPinned) Icons.Rounded.Close else Icons.Rounded.PushPin,
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
                    textResId = if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname,
                    icon = Icons.Rounded.Edit,
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
                        icon = Icons.Rounded.Delete,
                        onClick = {
                            onDismiss()
                            onUninstallClick()
                        }
                    )
                )
            }
        }

        menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(item.textResId)) },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null
                    )
                },
                onClick = item.onClick
            )
        }
    }
}
