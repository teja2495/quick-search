package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog

private enum class SwipeDirection(val titleResId: Int, val defaultAction: SwipeGestureAction) {
    RIGHT(R.string.settings_gesture_swipe_right, SwipeGestureAction.QUICK_NOTE),
    LEFT(R.string.settings_gesture_swipe_left, SwipeGestureAction.SETTINGS),
    UP(R.string.settings_gesture_swipe_up, SwipeGestureAction.OPEN_KEYBOARD),
    DOWN(R.string.settings_gesture_swipe_down, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS),
}

@Composable
fun GesturesSettingsSection(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
) {
    val context = LocalContext.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val isDefaultLauncher = context.isDefaultHomeApp()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    var actions by remember { mutableStateOf(SwipeDirection.entries.associateWith(preferences::actionFor)) }
    var customActions by remember { mutableStateOf(SwipeDirection.entries.associateWith(preferences::customActionFor)) }
    var selectedDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var customPickerDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var homeCustomPickerDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var selectedKeyboardAction by remember { mutableStateOf<SwipeGestureAction?>(null) }
    var selectedHomeVerticalDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var homeActions by remember {
        mutableStateOf(
            mapOf(
                SwipeDirection.UP to preferences.homeActionFor(SwipeDirection.UP),
                SwipeDirection.DOWN to preferences.homeActionFor(SwipeDirection.DOWN),
            ),
        )
    }
    var homeCustomActions by remember {
        mutableStateOf(
            mapOf(
                SwipeDirection.UP to preferences.homeCustomActionFor(SwipeDirection.UP),
                SwipeDirection.DOWN to preferences.homeCustomActionFor(SwipeDirection.DOWN),
            ),
        )
    }

    fun save(direction: SwipeDirection, action: SwipeGestureAction, customAction: CustomWidgetButtonAction? = null) {
        preferences.setActionFor(direction, action)
        preferences.setCustomActionFor(direction, customAction?.toJson())
        if (direction == SwipeDirection.LEFT && action != SwipeGestureAction.SETTINGS) {
            searchViewModel.setSettingsIconEnabled(true)
        }
        actions = actions + (direction to action)
        customActions = customActions + (direction to customAction?.toJson())
    }

    fun deleteCustomAction(actionJson: String) {
        SwipeDirection.entries.forEach { direction ->
            if (customActions[direction] == actionJson) {
                save(direction, direction.defaultAction)
            }
        }
        listOf(SwipeDirection.UP, SwipeDirection.DOWN).forEach { direction ->
            if (homeCustomActions[direction] == actionJson) {
                preferences.setHomeActionFor(direction, direction.homeDefaultAction)
                preferences.setHomeCustomActionFor(direction, null)
                homeActions = homeActions + (direction to direction.homeDefaultAction)
                homeCustomActions = homeCustomActions + (direction to null)
            }
        }
    }

    fun setKeyboardGesture(action: SwipeGestureAction, targetDirection: SwipeDirection?) {
        val updatedActions = actions.toMutableMap()
        val updatedCustomActions = customActions.toMutableMap()
        listOf(SwipeDirection.UP, SwipeDirection.DOWN).forEach { direction ->
            if (updatedActions[direction] == action || direction == targetDirection) {
                updatedActions[direction] = if (direction == targetDirection) action else SwipeGestureAction.NONE
                updatedCustomActions[direction] = null
            }
        }
        listOf(SwipeDirection.UP, SwipeDirection.DOWN).forEach { direction ->
            preferences.setActionFor(direction, updatedActions.getValue(direction))
            preferences.setCustomActionFor(direction, updatedCustomActions[direction])
        }
        actions = updatedActions
        customActions = updatedCustomActions
        selectedKeyboardAction = null
    }

    fun keyboardGestureDirection(action: SwipeGestureAction): SwipeDirection? =
        listOf(SwipeDirection.UP, SwipeDirection.DOWN).firstOrNull { actions[it] == action }

    selectedDirection?.let { direction ->
        GestureActionDialog(
            direction = direction,
            selectedAction = actions.getValue(direction),
            selectedCustomActionJson = customActions[direction],
            customActions = allCustomActions(customActions, homeCustomActions),
            onSelectDefault = { action ->
                save(direction, action)
                selectedDirection = null
            },
            onPickCustom = {
                selectedDirection = null
                customPickerDirection = direction
            },
            onSelectCustom = { action ->
                save(direction, SwipeGestureAction.CUSTOM, action)
                selectedDirection = null
            },
            onDeleteCustom = ::deleteCustomAction,
            onDismiss = { selectedDirection = null },
        )
    }

    customPickerDirection?.let { direction ->
        CustomWidgetButtonPickerDialog(
            currentAction = customActions[direction]?.let(CustomWidgetButtonAction::fromJson),
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = {
                searchViewModel.onQueryChange("")
                customPickerDirection = null
            },
            onSelect = { action ->
                save(direction, SwipeGestureAction.CUSTOM, action)
                searchViewModel.onQueryChange("")
                customPickerDirection = null
            },
        )
    }

    homeCustomPickerDirection?.let { direction ->
        CustomWidgetButtonPickerDialog(
            currentAction = homeCustomActions[direction]?.let(CustomWidgetButtonAction::fromJson),
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = {
                searchViewModel.onQueryChange("")
                homeCustomPickerDirection = null
            },
            onSelect = { action ->
                preferences.setHomeActionFor(direction, HomeSwipeGestureAction.CUSTOM)
                preferences.setHomeCustomActionFor(direction, action.toJson())
                homeActions = homeActions + (direction to HomeSwipeGestureAction.CUSTOM)
                homeCustomActions = homeCustomActions + (direction to action.toJson())
                searchViewModel.onQueryChange("")
                homeCustomPickerDirection = null
            },
        )
    }

    selectedKeyboardAction?.let { action ->
        KeyboardGestureDialog(
            action = action,
            selectedDirection = keyboardGestureDirection(action),
            onSelect = { direction -> setKeyboardGesture(action, direction) },
            onDismiss = { selectedKeyboardAction = null },
        )
    }

    selectedHomeVerticalDirection?.let { direction ->
        HomeVerticalGestureDialog(
            direction = direction,
            selectedAction = homeActions.getValue(direction),
            selectedCustomActionJson = homeCustomActions[direction],
            customActions = allCustomActions(customActions, homeCustomActions),
            onSelectDefault = { action ->
                preferences.setHomeActionFor(direction, action)
                preferences.setHomeCustomActionFor(direction, null)
                homeActions = homeActions + (direction to action)
                homeCustomActions = homeCustomActions + (direction to null)
                selectedHomeVerticalDirection = null
            },
            onPickCustom = {
                selectedHomeVerticalDirection = null
                homeCustomPickerDirection = direction
            },
            onSelectCustom = { action ->
                preferences.setHomeActionFor(direction, HomeSwipeGestureAction.CUSTOM)
                preferences.setHomeCustomActionFor(direction, action.toJson())
                homeActions = homeActions + (direction to HomeSwipeGestureAction.CUSTOM)
                homeCustomActions = homeCustomActions + (direction to action.toJson())
                selectedHomeVerticalDirection = null
            },
            onDeleteCustom = ::deleteCustomAction,
            onDismiss = { selectedHomeVerticalDirection = null },
        )
    }

    Column(modifier = modifier) {
        SettingsCard(modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding)) {
            Column {
                val homeGestureDirections = SwipeDirection.entries
                homeGestureDirections.forEachIndexed { index, direction ->
                    SettingsNavigationRow(
                        item =
                            SettingsCardItem(
                                title =
                                    stringResource(
                                        when (direction) {
                                            SwipeDirection.UP -> R.string.settings_gesture_swipe_up_home
                                            SwipeDirection.DOWN -> R.string.settings_gesture_swipe_down_home
                                            else -> direction.titleResId
                                        },
                                    ),
                                icon = direction.gestureIcon(),
                                description =
                                    if (direction == SwipeDirection.RIGHT && isDefaultLauncher) {
                                        stringResource(R.string.settings_gesture_widget_panel)
                                    } else if (direction == SwipeDirection.UP || direction == SwipeDirection.DOWN) {
                                        homeGestureDescription(homeActions.getValue(direction), homeCustomActions[direction])
                                    } else {
                                        gestureDescription(actions.getValue(direction), customActions[direction])
                                },
                                isEnabled = direction != SwipeDirection.RIGHT || !isDefaultLauncher,
                                actionOnPress = {
                                    if (direction == SwipeDirection.UP || direction == SwipeDirection.DOWN) {
                                        selectedHomeVerticalDirection = direction
                                    } else {
                                        selectedDirection = direction
                                    }
                                },
                            ),
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                    )
                    if (index != homeGestureDirections.lastIndex) HorizontalDivider(color = AppColors.SettingsDivider)
                }
            }
        }

        Text(
            text = stringResource(R.string.settings_keyboard_gestures_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
        )
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.settings_gesture_open_keyboard),
                        description = stringResource(keyboardGestureDirection(SwipeGestureAction.OPEN_KEYBOARD)?.titleResId ?: R.string.settings_gesture_none),
                        icon = Icons.Rounded.Keyboard,
                        actionOnPress = { selectedKeyboardAction = SwipeGestureAction.OPEN_KEYBOARD },
                    ),
                    contentPadding = PaddingValues(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                SettingsNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.settings_gesture_close_keyboard),
                        description = stringResource(keyboardGestureDirection(SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS)?.titleResId ?: R.string.settings_gesture_none),
                        icon = Icons.Rounded.KeyboardHide,
                        actionOnPress = { selectedKeyboardAction = SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS },
                    ),
                    contentPadding = PaddingValues(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
                )
            }
        }
    }
}

private fun SwipeDirection.gestureIcon() =
    when (this) {
        SwipeDirection.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
        SwipeDirection.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack
        SwipeDirection.UP -> Icons.Rounded.ArrowUpward
        SwipeDirection.DOWN -> Icons.Rounded.ArrowDownward
    }

@Composable
private fun HomeVerticalGestureDialog(
    direction: SwipeDirection,
    selectedAction: HomeSwipeGestureAction,
    selectedCustomActionJson: String?,
    customActions: List<CustomWidgetButtonAction>,
    onSelectDefault: (HomeSwipeGestureAction) -> Unit,
    onPickCustom: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(direction.titleResId)) },
        text = {
            Column {
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = selectedAction == HomeSwipeGestureAction.NONE,
                    onClick = { onSelectDefault(HomeSwipeGestureAction.NONE) },
                )
                if (direction == SwipeDirection.DOWN) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_notification_panel),
                        selected = selectedAction == HomeSwipeGestureAction.NOTIFICATION_PANEL,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.NOTIFICATION_PANEL) },
                    )
                }
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_custom),
                    selected = false,
                    onClick = onPickCustom,
                )
                customActions.forEach { action ->
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    val json = action.toJson()
                    GestureActionRow(
                        label = action.displayLabel(),
                        selected = selectedAction == HomeSwipeGestureAction.CUSTOM && selectedCustomActionJson == json,
                        onClick = { onSelectCustom(action) },
                        onDelete = { onDeleteCustom(json) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun KeyboardGestureDialog(
    action: SwipeGestureAction,
    selectedDirection: SwipeDirection?,
    onSelect: (SwipeDirection?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(action.labelResId())) },
        text = {
            Column {
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_swipe_up),
                    selected = selectedDirection == SwipeDirection.UP,
                    onClick = { onSelect(SwipeDirection.UP) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_swipe_down),
                    selected = selectedDirection == SwipeDirection.DOWN,
                    onClick = { onSelect(SwipeDirection.DOWN) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = selectedDirection == null,
                    onClick = { onSelect(null) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun GestureActionDialog(
    direction: SwipeDirection,
    selectedAction: SwipeGestureAction,
    selectedCustomActionJson: String?,
    customActions: List<CustomWidgetButtonAction>,
    onSelectDefault: (SwipeGestureAction) -> Unit,
    onPickCustom: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(direction.titleResId)) },
        text = {
            Column {
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = selectedAction == SwipeGestureAction.NONE,
                    onClick = { onSelectDefault(SwipeGestureAction.NONE) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(direction.defaultAction.labelResId()),
                    selected = selectedAction == direction.defaultAction,
                    onClick = { onSelectDefault(direction.defaultAction) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_custom),
                    selected = false,
                    onClick = onPickCustom,
                )
                customActions.forEach { action ->
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    val json = action.toJson()
                    GestureActionRow(
                        label = action.displayLabel(),
                        selected = selectedAction == SwipeGestureAction.CUSTOM && selectedCustomActionJson == json,
                        onClick = { onSelectCustom(action) },
                        onDelete = { onDeleteCustom(json) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun GestureActionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingMedium),
        )
        onDelete?.let {
            IconButton(onClick = it) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_remove))
            }
        }
    }
}

@Composable
private fun gestureDescription(action: SwipeGestureAction, customActionJson: String?): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == SwipeGestureAction.CUSTOM }
        ?: stringResource(action.labelResId())

@Composable
private fun homeGestureDescription(action: HomeSwipeGestureAction, customActionJson: String?): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == HomeSwipeGestureAction.CUSTOM }
        ?: stringResource(action.labelResId())

private fun allCustomActions(
    swipeCustomActions: Map<SwipeDirection, String?>,
    homeCustomActions: Map<SwipeDirection, String?>,
): List<CustomWidgetButtonAction> =
    (swipeCustomActions.values + homeCustomActions.values)
        .mapNotNull(CustomWidgetButtonAction::fromJson)
        .distinctBy { it.toJson() }

private fun UserAppPreferences.actionFor(direction: SwipeDirection): SwipeGestureAction =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAction()
        SwipeDirection.LEFT -> getSwipeLeftAction()
        SwipeDirection.UP -> getSwipeUpAction()
        SwipeDirection.DOWN -> getSwipeDownAction()
    }

private fun UserAppPreferences.customActionFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightCustomAction()
        SwipeDirection.LEFT -> getSwipeLeftCustomAction()
        SwipeDirection.UP -> getSwipeUpCustomAction()
        SwipeDirection.DOWN -> getSwipeDownCustomAction()
    }

private fun UserAppPreferences.setActionFor(direction: SwipeDirection, action: SwipeGestureAction) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAction(action)
        SwipeDirection.LEFT -> setSwipeLeftAction(action)
        SwipeDirection.UP -> setSwipeUpAction(action)
        SwipeDirection.DOWN -> setSwipeDownAction(action)
    }
}

private fun UserAppPreferences.setCustomActionFor(direction: SwipeDirection, actionJson: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightCustomAction(actionJson)
        SwipeDirection.LEFT -> setSwipeLeftCustomAction(actionJson)
        SwipeDirection.UP -> setSwipeUpCustomAction(actionJson)
        SwipeDirection.DOWN -> setSwipeDownCustomAction(actionJson)
    }
}

private fun UserAppPreferences.homeActionFor(direction: SwipeDirection): HomeSwipeGestureAction =
    when (direction) {
        SwipeDirection.UP -> getHomeSwipeUpAction()
        SwipeDirection.DOWN -> getHomeSwipeDownAction()
        SwipeDirection.RIGHT,
        SwipeDirection.LEFT,
        -> error("Home vertical gestures only support swipe up and down")
    }

private fun UserAppPreferences.setHomeActionFor(direction: SwipeDirection, action: HomeSwipeGestureAction) {
    when (direction) {
        SwipeDirection.UP -> setHomeSwipeUpAction(action)
        SwipeDirection.DOWN -> setHomeSwipeDownAction(action)
        SwipeDirection.RIGHT,
        SwipeDirection.LEFT,
        -> error("Home vertical gestures only support swipe up and down")
    }
}

private fun UserAppPreferences.homeCustomActionFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.UP -> getHomeSwipeUpCustomAction()
        SwipeDirection.DOWN -> getHomeSwipeDownCustomAction()
        SwipeDirection.RIGHT,
        SwipeDirection.LEFT,
        -> error("Home vertical gestures only support swipe up and down")
    }

private fun UserAppPreferences.setHomeCustomActionFor(direction: SwipeDirection, actionJson: String?) {
    when (direction) {
        SwipeDirection.UP -> setHomeSwipeUpCustomAction(actionJson)
        SwipeDirection.DOWN -> setHomeSwipeDownCustomAction(actionJson)
        SwipeDirection.RIGHT,
        SwipeDirection.LEFT,
        -> error("Home vertical gestures only support swipe up and down")
    }
}

private val SwipeDirection.homeDefaultAction: HomeSwipeGestureAction
    get() =
        when (this) {
            SwipeDirection.UP -> HomeSwipeGestureAction.NONE
            SwipeDirection.DOWN -> HomeSwipeGestureAction.NOTIFICATION_PANEL
            SwipeDirection.RIGHT,
            SwipeDirection.LEFT,
            -> error("Home vertical gestures only support swipe up and down")
        }

private fun SwipeGestureAction.labelResId(): Int =
    when (this) {
        SwipeGestureAction.QUICK_NOTE -> R.string.settings_gesture_quick_note
        SwipeGestureAction.SETTINGS -> R.string.settings_gesture_settings
        SwipeGestureAction.OPEN_KEYBOARD -> R.string.settings_gesture_open_keyboard
        SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS -> R.string.settings_gesture_close_keyboard_notifications
        SwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        SwipeGestureAction.NONE -> R.string.settings_gesture_none
    }

private fun HomeSwipeGestureAction.labelResId(): Int =
    when (this) {
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> R.string.settings_gesture_notification_panel
        HomeSwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        HomeSwipeGestureAction.NONE -> R.string.settings_gesture_none
    }
