package com.tk.quicksearch.search.deviceSettings

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.SettingExtra
import com.tk.quicksearch.widgets.customButtonsWidget.SettingExtraType

/** Menu item data class for settings dropdown menu. */
private data class DeviceSettingsMenuItem(
        val textResId: Int,
        val icon: @Composable () -> Unit,
        val onClick: () -> Unit,
)

/** Dropdown menu for settings result rows with actions like pin/unpin, nickname, and exclude. */
@Composable
fun DeviceSettingsDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        isPinned: Boolean,
        hasNickname: Boolean,
        hasTrigger: Boolean,
        onTogglePin: () -> Unit,
        onMoveUp: () -> Unit = {},
        onMoveDown: () -> Unit = {},
        onExclude: () -> Unit,
        onNicknameClick: () -> Unit,
        onTriggerClick: () -> Unit,
        onAddToHome: () -> Unit,
        setting: DeviceSetting,
        showPinnedItemMenu: Boolean = false,
) {
    val context = LocalContext.current
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
    val notificationAction = CustomWidgetButtonAction.Setting(
            id = setting.id, title = setting.title, description = setting.description,
            keywords = setting.keywords, action = setting.action, data = setting.data,
            categories = setting.categories,
            extras = setting.extras.map { (key, value) ->
                    SettingExtra(
                            key,
                            when (value) {
                                is Boolean -> SettingExtraType.BOOLEAN
                                is Int -> SettingExtraType.INT
                                is Long -> SettingExtraType.LONG
                                else -> SettingExtraType.STRING
                            },
                            value.toString(),
                    )
            },
            minSdk = setting.minSdk, maxSdk = setting.maxSdk,
    )
    val isPinnedToNotifications = PinnedNotifications.isPinned(context, notificationAction)
    AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
    ) {
        val menuItems = buildList {
            if (showPinnedItemMenu && isPinned) {
                add(
                        DeviceSettingsMenuItem(
                                textResId = R.string.action_unpin_app,
                                icon = { Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null) },
                                onClick = {
                                    onDismissRequest()
                                    onTogglePin()
                                },
                        ),
                )
                add(
                        DeviceSettingsMenuItem(
                                textResId = R.string.action_move_up,
                                icon = { Icon(imageVector = MaterialSymbols.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    onDismissRequest()
                                    onMoveUp()
                                },
                        ),
                )
                add(
                        DeviceSettingsMenuItem(
                                textResId = R.string.action_move_down,
                                icon = { Icon(imageVector = MaterialSymbols.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    onDismissRequest()
                                    onMoveDown()
                                },
                        ),
                )
                return@buildList
            }

            add(
                    DeviceSettingsMenuItem(
                            textResId =
                                    if (isPinned) R.string.action_unpin_app
                                    else R.string.action_pin_app,
                            icon = {
                                Icon(
                                        painter =
                                                painterResource(
                                                        if (isPinned) R.drawable.ic_unpin
                                                        else R.drawable.ic_pin,
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
                    DeviceSettingsMenuItem(
                            textResId =
                                    if (hasTrigger) R.string.action_edit_trigger
                                    else R.string.action_add_trigger,
                            icon = {
                                Icon(imageVector = MaterialSymbols.Bolt, contentDescription = null)
                            },
                            onClick = {
                                onDismissRequest()
                                onTriggerClick()
                            },
                    ),
            )
            add(
                    DeviceSettingsMenuItem(
                            textResId = if (isPinnedToNotifications) R.string.action_unpin_from_notifications else R.string.action_pin_to_notifications,
                            icon = {
                                if (isPinnedToNotifications) {
                                    Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null)
                                } else {
                                    Icon(imageVector = MaterialSymbols.PinEnd, contentDescription = null)
                                }
                            },
                            onClick = { onDismissRequest(); PinnedNotifications.toggle(context, notificationAction) },
                    ),
            )
            if (!isDefaultLauncher) {
                add(
                        DeviceSettingsMenuItem(
                                textResId = R.string.action_add_to_home,
                                icon = {
                                    Icon(imageVector = MaterialSymbols.Home, contentDescription = null)
                                },
                                onClick = {
                                    onDismissRequest()
                                    onAddToHome()
                                },
                        ),
                )
            }
            add(
                    DeviceSettingsMenuItem(
                            textResId =
                                    if (hasNickname) R.string.action_edit_nickname
                                    else R.string.common_nickname,
                            icon = {
                                Icon(imageVector = MaterialSymbols.Edit, contentDescription = null)
                            },
                            onClick = {
                                onDismissRequest()
                                onNicknameClick()
                            },
                    ),
            )
            add(
                    DeviceSettingsMenuItem(
                            textResId = R.string.action_exclude_generic,
                            icon = {
                                Icon(
                                        imageVector = MaterialSymbols.VisibilityOff,
                                        contentDescription = null
                                )
                            },
                            onClick = {
                                onDismissRequest()
                                onExclude()
                            },
                    ),
            )
        }

        val orderedMenuItems = menuItems.toMutableList().apply {
            val notificationIndex = indexOfFirst { it.textResId == R.string.action_pin_to_notifications || it.textResId == R.string.action_unpin_from_notifications }
            if (notificationIndex >= 0 && indexOfFirst { it.textResId == R.string.common_nickname || it.textResId == R.string.action_edit_nickname } >= 0) {
                val notificationItem = removeAt(notificationIndex)
                add(indexOfFirst { it.textResId == R.string.common_nickname || it.textResId == R.string.action_edit_nickname } + 1, notificationItem)
            }
        }
        orderedMenuItems.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                    text = { Text(text = stringResource(item.textResId)) },
                    leadingIcon = { item.icon() },
                    onClick = item.onClick,
            )
        }
    }
}
