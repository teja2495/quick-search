package com.tk.quicksearch.search.deviceSettings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R

/**
 * Menu item data class for settings dropdown menu.
 */
private data class DeviceSettingsMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

/**
 * Dropdown menu for settings result rows with actions like pin/unpin, nickname, and exclude.
 */
@Composable
fun DeviceSettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isPinned: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onNicknameClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false)
    ) {
        val menuItems = buildList {
            add(
                DeviceSettingsMenuItem(
                    textResId = if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic,
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin
                            ),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onDismissRequest()
                        onTogglePin()
                    }
                )
            )
            add(
                DeviceSettingsMenuItem(
                    textResId = if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname,
                    icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        onDismissRequest()
                        onNicknameClick()
                    }
                )
            )
            add(
                DeviceSettingsMenuItem(
                    textResId = R.string.action_exclude_generic,
                    icon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
                    onClick = {
                        onDismissRequest()
                        onExclude()
                    }
                )
            )
        }

        menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(item.textResId)) },
                leadingIcon = {
                    item.icon()
                },
                onClick = item.onClick
            )
        }
    }
}