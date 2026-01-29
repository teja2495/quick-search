package com.tk.quicksearch.search.files

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
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.ui.theme.AppColors

/**
 * Menu item data class for file dropdown menu.
 */
private data class FileMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

/**
 * Dropdown menu for file result rows with actions like pin/unpin, nickname, exclude, and exclude extension.
 */
@Composable
fun FileDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    deviceFile: DeviceFile,
    isPinned: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onExcludeExtension: () -> Unit,
    onNicknameClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false),
        containerColor = AppColors.DialogBackground,
    ) {
        val menuItems =
            buildList {
                add(
                    FileMenuItem(
                        textResId = if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic,
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin,
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
                    FileMenuItem(
                        textResId = if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname,
                        icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            onDismissRequest()
                            onNicknameClick()
                        },
                    ),
                )
                add(
                    FileMenuItem(
                        textResId = R.string.action_exclude_generic,
                        icon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
                        onClick = {
                            onDismissRequest()
                            onExclude()
                        },
                    ),
                )

                val fileExtension = FileUtils.getFileExtension(deviceFile.displayName)
                if (fileExtension != null) {
                    add(
                        FileMenuItem(
                            textResId = R.string.action_exclude_extension,
                            icon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
                            onClick = {
                                onDismissRequest()
                                onExcludeExtension()
                            },
                        ),
                    )
                }
            }

        menuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(item.textResId, FileUtils.getFileExtension(deviceFile.displayName) ?: "")) },
                leadingIcon = {
                    item.icon()
                },
                onClick = item.onClick,
            )
        }
    }
}
