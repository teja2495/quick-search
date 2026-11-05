package com.tk.quicksearch.search.contacts.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.ItemMenuLongPressOption
import com.tk.quicksearch.shared.ui.components.ItemMenuPopup
import com.tk.quicksearch.shared.ui.components.ItemMenuRow
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.itemMenuRemoveOption
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus

/** Menu item data class for contact dropdown menu. */
private data class ContactMenuItem(
        val textResId: Int,
        val icon: @Composable () -> Unit,
        val onClick: () -> Unit,
        val isTile: Boolean = false,
        val longPressOption: ItemMenuLongPressOption? = null,
)

/** Long-press menu for contact result rows with actions like pin/unpin, nickname, and exclude. */
@Composable
fun ContactDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        displayName: String,
        photoUri: String?,
        isPinned: Boolean,
        hasNickname: Boolean,
        hasTrigger: Boolean,
        onTogglePin: () -> Unit,
        onMoveUp: () -> Unit = {},
        onMoveDown: () -> Unit = {},
        onExclude: () -> Unit,
        onNicknameClick: () -> Unit,
        onTriggerClick: () -> Unit,
        /** Clears the nickname or trigger from a long press on its tile; null hides that option. */
        onRemoveNickname: (() -> Unit)? = null,
        onRemoveTrigger: (() -> Unit)? = null,
        onAddToHome: () -> Unit,
        onPinToNotifications: () -> Unit,
        isPinnedToNotifications: Boolean,
        showPinnedItemMenu: Boolean = false,
) {
        val context = LocalContext.current
        val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
        val openAppSettingDestination = LocalOpenAppSettingDestination.current
        // Removing keeps the menu open, so the menu tracks it until it's next opened.
        var triggerRemoved by remember(expanded) { mutableStateOf(false) }
        var nicknameRemoved by remember(expanded) { mutableStateOf(false) }
        val triggerSet = hasTrigger && !triggerRemoved
        val nicknameSet = hasNickname && !nicknameRemoved
        val removeTriggerOption = itemMenuRemoveOption(
                onRemoveTrigger?.takeIf { triggerSet }?.let { remove -> { triggerRemoved = true; remove() } },
        )
        val removeNicknameOption = itemMenuRemoveOption(
                onRemoveNickname?.takeIf { nicknameSet }?.let { remove -> { nicknameRemoved = true; remove() } },
        )
        val menuItems = buildList {
            if (showPinnedItemMenu && isPinned) {
                add(
                        ContactMenuItem(
                                textResId = R.string.action_unpin_app,
                                icon = { Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null, tint = AppColors.ItemMenuActiveIconTint) },
                                onClick = {
                                    onDismissRequest()
                                    onTogglePin()
                                },
                        ),
                )
                add(
                        ContactMenuItem(
                                textResId = R.string.action_move_up,
                                icon = { Icon(imageVector = MaterialSymbols.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    onDismissRequest()
                                    onMoveUp()
                                },
                        ),
                )
                add(
                        ContactMenuItem(
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
                    ContactMenuItem(
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
                                        tint = if (isPinned) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                                )
                            },
                            onClick = {
                                onDismissRequest()
                                onTogglePin()
                            },
                            isTile = true,
                    ),
            )
            add(
                    ContactMenuItem(
                            textResId =
                                    R.string.action_add_trigger,
                            icon = {
                                Icon(imageVector = MaterialSymbols.Bolt, contentDescription = null, tint = if (triggerSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current)
                            },
                            onClick = {
                                onDismissRequest()
                                onTriggerClick()
                            },
                            isTile = true,
                            longPressOption = removeTriggerOption,
                    ),
            )
            add(
                    ContactMenuItem(
                            textResId =
                                    R.string.common_nickname,
                            icon = {
                                Icon(imageVector = MaterialSymbols.Edit, contentDescription = null, tint = if (nicknameSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current)
                            },
                            onClick = {
                                onDismissRequest()
                                onNicknameClick()
                            },
                            isTile = true,
                            longPressOption = removeNicknameOption,
                    ),
            )
            add(
                    ContactMenuItem(
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
                            isTile = true,
                    ),
            )
            add(
                    ContactMenuItem(
                            textResId = if (isPinnedToNotifications) R.string.action_unpin_from_notifications else R.string.action_pin_to_notifications,
                            icon = {
                                if (isPinnedToNotifications) {
                                    Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null, tint = AppColors.ItemMenuActiveIconTint)
                                } else {
                                    Icon(imageVector = MaterialSymbols.PinEnd, contentDescription = null)
                                }
                            },
                            onClick = { onDismissRequest(); onPinToNotifications() },
                    ),
            )
            if (!isDefaultLauncher) {
                add(
                        ContactMenuItem(
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
            openAppSettingDestination?.let { openDestination ->
                add(
                        ContactMenuItem(
                                textResId = R.string.action_contacts_settings,
                                icon = {
                                    Icon(imageVector = MaterialSymbols.Settings, contentDescription = null)
                                },
                                onClick = {
                                    onDismissRequest()
                                    openDestination(AppSettingsDestination.CALLS_TEXTS)
                                },
                        ),
                )
            }
        }

        if (showPinnedItemMenu && isPinned) {
            AppDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = onDismissRequest,
            ) {
                menuItems.forEachIndexed { index, item ->
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
            return
        }

        if (expanded) {
            ItemMenuPopup(
                    onDismiss = onDismissRequest,
                    leadingContent = {
                        ContactAvatar(
                                photoUri = photoUri,
                                displayName = displayName,
                                modifier = Modifier.size(40.dp),
                        )
                    },
                    title = {
                        Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shortcutsTitle = stringResource(R.string.app_menu_section_shortcuts),
                    actionsTitle = stringResource(R.string.app_menu_section_actions),
                    actions = menuItems.filter { it.isTile }.map { item ->
                        ItemMenuTile(
                                label = stringResource(item.textResId),
                                icon = item.icon,
                                onClick = item.onClick,
                                longPressOption = item.longPressOption,
                        )
                    },
                    rows = menuItems.filterNot { it.isTile }.map { item ->
                        ItemMenuRow(
                                label = stringResource(item.textResId),
                                icon = item.icon,
                                onClick = item.onClick,
                        )
                    },
            )
        }
}
