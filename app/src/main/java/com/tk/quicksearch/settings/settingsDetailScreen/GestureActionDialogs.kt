package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.itemsIndexed
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerLeadingSize
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog

@Composable
internal fun LauncherSwipeRightActionDialog(
    selectedAction: SwipeGestureAction,
    onSelect: (SwipeGestureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_gesture_swipe_right)) },
        text = {
            Column {
                listOf(
                    SwipeGestureAction.NONE,
                    SwipeGestureAction.WIDGETS_PANEL,
                    SwipeGestureAction.ALL_APPS,
                ).forEachIndexed { index, action ->
                    if (index > 0) HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(action.labelResId()),
                        selected = selectedAction == action,
                        onClick = { onSelect(action) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

internal fun SwipeDirection.gestureIcon() =
    when (this) {
        SwipeDirection.RIGHT -> MaterialSymbols.ArrowForward
        SwipeDirection.LEFT -> MaterialSymbols.ArrowBack
        SwipeDirection.UP -> MaterialSymbols.ArrowUpward
        SwipeDirection.DOWN -> MaterialSymbols.ArrowDownward
    }

internal fun HomeGesture.icon() =
    when (this) {
        HomeGesture.SWIPE_UP -> MaterialSymbols.ArrowUpward
        HomeGesture.SWIPE_DOWN,
        HomeGesture.DOUBLE_TAP,
        -> MaterialSymbols.ArrowDownward
        HomeGesture.TWO_FINGER_SWIPE_UP -> MaterialSymbols.SwipeUp
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> MaterialSymbols.SwipeDown
        HomeGesture.NOTCH_SINGLE_TAP,
        HomeGesture.NOTCH_DOUBLE_TAP,
        -> MaterialSymbols.Swipe
    }

@Composable
internal fun HomeVerticalGestureDialog(
    titleResId: Int,
    allowsNotificationPanel: Boolean,
    allowsQuickSettings: Boolean,
    allowsCloseQuickSearch: Boolean,
    allowsLockScreen: Boolean,
    allowsAllApps: Boolean,
    hasLockScreenAccessibilityPermission: Boolean,
    allowsAppSurfaces: Boolean = false,
    selectedAction: HomeSwipeGestureAction,
    selectedCustomActionJson: String?,
    selectedAliasTarget: String?,
    customActions: List<CustomWidgetButtonAction>,
    aliasItems: List<GestureAliasItem>,
    onSelectDefault: (HomeSwipeGestureAction) -> Unit,
    onSelectLockScreen: () -> Unit,
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onSelectAlias: (HomeSwipeGestureAction, String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = {
            Column {
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = selectedAction == HomeSwipeGestureAction.NONE,
                    onClick = { onSelectDefault(HomeSwipeGestureAction.NONE) },
                )
                if (allowsAppSurfaces) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_open_quick_search),
                        selected = selectedAction == HomeSwipeGestureAction.OPEN_QUICK_SEARCH,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.OPEN_QUICK_SEARCH) },
                    )
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_pinned_glance),
                        selected = selectedAction == HomeSwipeGestureAction.PINNED_GLANCE,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.PINNED_GLANCE) },
                    )
                }
                if (allowsNotificationPanel) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_notification_panel),
                        selected = selectedAction == HomeSwipeGestureAction.NOTIFICATION_PANEL,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.NOTIFICATION_PANEL) },
                    )
                }
                if (allowsQuickSettings) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_quick_settings),
                        selected = selectedAction == HomeSwipeGestureAction.QUICK_SETTINGS,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.QUICK_SETTINGS) },
                    )
                }
                if (allowsAllApps) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_view_all_apps),
                        selected = selectedAction == HomeSwipeGestureAction.ALL_APPS,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.ALL_APPS) },
                    )
                }
                if (allowsAppSurfaces) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_widget_panel),
                        selected = selectedAction == HomeSwipeGestureAction.WIDGETS_PANEL,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.WIDGETS_PANEL) },
                    )
                }
                if (allowsCloseQuickSearch) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_close_quick_search),
                        selected = selectedAction == HomeSwipeGestureAction.CLOSE_QUICK_SEARCH,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.CLOSE_QUICK_SEARCH) },
                    )
                }
                if (allowsLockScreen) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_lock_screen),
                        supportingText = if (hasLockScreenAccessibilityPermission) {
                            null
                        } else {
                            stringResource(R.string.settings_gesture_lock_screen_requires_accessibility)
                        },
                        selected = selectedAction == HomeSwipeGestureAction.LOCK_SCREEN,
                        onClick = onSelectLockScreen,
                    )
                }
                if (allowsAppSurfaces) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_play_pause_media),
                        selected = selectedAction == HomeSwipeGestureAction.PLAY_PAUSE_MEDIA,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.PLAY_PAUSE_MEDIA) },
                    )
                }
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
                aliasItems.forEach { item ->
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = item.label,
                        selected = selectedAction == item.homeAction && selectedAliasTarget == item.id,
                        onClick = { onSelectAlias(item.homeAction, item.id) },
                        onDelete = { onDeleteAlias(item.id) },
                    )
                }
                GesturePickerActions(
                    onPickCustom = onPickCustom,
                    onPickSearchEngine = onPickSearchEngine,
                    onPickTool = onPickTool,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
internal fun KeyboardGestureDialog(
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
internal fun GestureActionDialog(
    direction: SwipeDirection,
    allowsCloseQuickSearch: Boolean,
    selectedAction: SwipeGestureAction,
    selectedCustomActionJson: String?,
    selectedAliasTarget: String?,
    customActions: List<CustomWidgetButtonAction>,
    aliasItems: List<GestureAliasItem>,
    onSelectDefault: (SwipeGestureAction) -> Unit,
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
    onSelectCustom: (CustomWidgetButtonAction) -> Unit,
    onSelectAlias: (SwipeGestureAction, String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDeleteAlias: (String) -> Unit,
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
                    label = stringResource(R.string.settings_gesture_view_all_apps),
                    selected = selectedAction == SwipeGestureAction.ALL_APPS,
                    onClick = { onSelectDefault(SwipeGestureAction.ALL_APPS) },
                )
                if (allowsCloseQuickSearch) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_close_quick_search),
                        selected = selectedAction == SwipeGestureAction.CLOSE_QUICK_SEARCH,
                        onClick = { onSelectDefault(SwipeGestureAction.CLOSE_QUICK_SEARCH) },
                    )
                }
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
                aliasItems.forEach { item ->
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = item.label,
                        selected = selectedAction == item.swipeAction && selectedAliasTarget == item.id,
                        onClick = { onSelectAlias(item.swipeAction, item.id) },
                        onDelete = { onDeleteAlias(item.id) },
                    )
                }
                GesturePickerActions(
                    onPickCustom = onPickCustom,
                    onPickSearchEngine = onPickSearchEngine,
                    onPickTool = onPickTool,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
internal fun GestureActionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingMedium)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        onDelete?.let {
            IconButton(onClick = it) {
                Icon(MaterialSymbols.Delete, contentDescription = stringResource(R.string.action_remove))
            }
        }
    }
}

@Composable
internal fun GesturePickerActions(
    onPickCustom: () -> Unit,
    onPickSearchEngine: () -> Unit,
    onPickTool: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.SpacingMedium)
                .border(
                    width = DesignTokens.BorderWidth,
                    color = AppColors.OnboardingBubbleBorder.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column {
            GesturePickerRow(stringResource(R.string.settings_gesture_custom), MaterialSymbols.Add, onPickCustom)
            HorizontalDivider(color = AppColors.SettingsDivider)
            GesturePickerRow(stringResource(R.string.settings_app_shortcuts_filter_search_engines), MaterialSymbols.Search, onPickSearchEngine)
            HorizontalDivider(color = AppColors.SettingsDivider)
            GesturePickerRow(stringResource(R.string.settings_tools_title), MaterialSymbols.Build, onPickTool)
        }
    }
}

@Composable
internal fun GesturePickerRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp).padding(start = DesignTokens.SpacingMedium),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = DesignTokens.SpacingMedium),
        )
    }
}

@Composable
internal fun gestureDescription(action: SwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == SwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == SwipeGestureAction.SEARCH_ENGINE || action == SwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

@Composable
internal fun homeGestureDescription(action: HomeSwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == HomeSwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == HomeSwipeGestureAction.SEARCH_ENGINE || action == HomeSwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

internal fun allCustomActions(
    swipeCustomActions: Map<*, String?>,
    homeCustomActions: Map<*, String?>,
): List<CustomWidgetButtonAction> =
    (swipeCustomActions.values + homeCustomActions.values)
        .mapNotNull(CustomWidgetButtonAction::fromJson)
        .distinctBy { it.toJson() }

@Composable
internal fun aliasDisplayName(
    targetId: String,
    state: com.tk.quicksearch.search.core.SearchUiState,
): String =
    state.searchTargetsOrder.firstOrNull { it.getId() == targetId }?.getDisplayName()
        ?: gestureToolItems(state).firstOrNull { it.first == targetId }?.second
        ?: targetId

internal fun UserAppPreferences.actionFor(direction: SwipeDirection): SwipeGestureAction =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAction()
        SwipeDirection.LEFT -> getSwipeLeftAction()
        SwipeDirection.UP -> getSwipeUpAction()
        SwipeDirection.DOWN -> getSwipeDownAction()
    }

internal fun UserAppPreferences.customActionFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightCustomAction()
        SwipeDirection.LEFT -> getSwipeLeftCustomAction()
        SwipeDirection.UP -> getSwipeUpCustomAction()
        SwipeDirection.DOWN -> getSwipeDownCustomAction()
    }

internal fun UserAppPreferences.setActionFor(direction: SwipeDirection, action: SwipeGestureAction) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAction(action)
        SwipeDirection.LEFT -> setSwipeLeftAction(action)
        SwipeDirection.UP -> setSwipeUpAction(action)
        SwipeDirection.DOWN -> setSwipeDownAction(action)
    }
}

internal fun UserAppPreferences.setCustomActionFor(direction: SwipeDirection, actionJson: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightCustomAction(actionJson)
        SwipeDirection.LEFT -> setSwipeLeftCustomAction(actionJson)
        SwipeDirection.UP -> setSwipeUpCustomAction(actionJson)
        SwipeDirection.DOWN -> setSwipeDownCustomAction(actionJson)
    }
}

internal fun UserAppPreferences.aliasTargetFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAliasTarget()
        SwipeDirection.LEFT -> getSwipeLeftAliasTarget()
        SwipeDirection.UP -> getSwipeUpAliasTarget()
        SwipeDirection.DOWN -> getSwipeDownAliasTarget()
    }

internal fun UserAppPreferences.setAliasTargetFor(direction: SwipeDirection, targetId: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAliasTarget(targetId)
        SwipeDirection.LEFT -> setSwipeLeftAliasTarget(targetId)
        SwipeDirection.UP -> setSwipeUpAliasTarget(targetId)
        SwipeDirection.DOWN -> setSwipeDownAliasTarget(targetId)
    }
}

internal fun UserAppPreferences.homeActionFor(
    gesture: HomeGesture,
    isDefaultLauncher: Boolean,
): HomeSwipeGestureAction =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAction(isDefaultLauncher)
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAction()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpAction()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownAction()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapAction()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapAction()
    }

internal fun UserAppPreferences.setHomeActionFor(gesture: HomeGesture, action: HomeSwipeGestureAction) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAction(action)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAction(action)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAction(action)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpAction(action)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownAction(action)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapAction(action)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapAction(action)
    }
}

internal fun UserAppPreferences.homeCustomActionFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpCustomAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownCustomAction()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapCustomAction()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpCustomAction()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownCustomAction()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapCustomAction()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapCustomAction()
    }

internal fun UserAppPreferences.setHomeCustomActionFor(gesture: HomeGesture, actionJson: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpCustomAction(actionJson)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownCustomAction(actionJson)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapCustomAction(actionJson)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpCustomAction(actionJson)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownCustomAction(actionJson)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapCustomAction(actionJson)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapCustomAction(actionJson)
    }
}

internal fun UserAppPreferences.homeAliasTargetFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAliasTarget()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAliasTarget()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAliasTarget()
        HomeGesture.TWO_FINGER_SWIPE_UP -> getHomeTwoFingerSwipeUpAliasTarget()
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> getHomeTwoFingerSwipeDownAliasTarget()
        HomeGesture.NOTCH_SINGLE_TAP -> getNotchSingleTapAliasTarget()
        HomeGesture.NOTCH_DOUBLE_TAP -> getNotchDoubleTapAliasTarget()
    }

internal fun UserAppPreferences.setHomeAliasTargetFor(gesture: HomeGesture, targetId: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAliasTarget(targetId)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAliasTarget(targetId)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAliasTarget(targetId)
        HomeGesture.TWO_FINGER_SWIPE_UP -> setHomeTwoFingerSwipeUpAliasTarget(targetId)
        HomeGesture.TWO_FINGER_SWIPE_DOWN -> setHomeTwoFingerSwipeDownAliasTarget(targetId)
        HomeGesture.NOTCH_SINGLE_TAP -> setNotchSingleTapAliasTarget(targetId)
        HomeGesture.NOTCH_DOUBLE_TAP -> setNotchDoubleTapAliasTarget(targetId)
    }
}

@Composable
internal fun GestureToolPickerDialog(
    state: com.tk.quicksearch.search.core.SearchUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val tools = ToolSettingsRegistry.definitions.mapNotNull { definition ->
        definition.aliasFeatureId?.let { GestureToolItem(it, stringResource(definition.titleResId), definition.icon) }
    } +
        state.customTools.filterNot { it.id in state.disabledCustomToolIds }.map { GestureToolItem(it.id, it.name, MaterialSymbols.Build) } +
        state.taskerIntentTools.map { GestureToolItem(it.id, it.name, MaterialSymbols.Build) }
    AppPickerDrawer(
        title = stringResource(R.string.settings_tools_title),
        onDismiss = onDismiss,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            itemsIndexed(tools, key = { _, tool -> tool.id }) { _, tool ->
                AppPickerDrawerRow(
                    title = tool.label,
                    onClick = { onSelect(tool.id) },
                    leading = { AppPickerDrawerIconBadge(icon = tool.icon) },
                )
            }
        }
    }
}

internal data class GestureToolItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
internal fun GestureSearchEnginePickerDialog(
    targets: List<SearchTarget>,
    onDismiss: () -> Unit,
    onSelect: (SearchTarget) -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
        onDismiss = onDismiss,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            itemsIndexed(targets, key = { _, target -> target.getId() }) { _, target ->
                AppPickerDrawerRow(
                    title = target.getDisplayName(),
                    onClick = { onSelect(target) },
                    leading = { SearchTargetIcon(target = target, iconSize = AppPickerDrawerLeadingSize) },
                )
            }
        }
    }
}

internal data class GestureAliasItem(
    val id: String,
    val label: String,
    val swipeAction: SwipeGestureAction,
    val homeAction: HomeSwipeGestureAction,
)

@Composable
internal fun allGestureAliasItems(
    state: com.tk.quicksearch.search.core.SearchUiState,
    swipeTargets: Map<SwipeDirection, String?>,
    homeTargets: Map<HomeGesture, String?>,
): List<GestureAliasItem> {
    val selectedIds = (swipeTargets.values + homeTargets.values).filterNotNull().toSet()
    val engineNames = state.searchTargetsOrder.associate { it.getId() to it.getDisplayName() }
    val toolNames = gestureToolItems(state).toMap()
    return selectedIds.mapNotNull { id ->
        engineNames[id]?.let { GestureAliasItem(id, it, SwipeGestureAction.SEARCH_ENGINE, HomeSwipeGestureAction.SEARCH_ENGINE) }
            ?: toolNames[id]?.let { GestureAliasItem(id, it, SwipeGestureAction.TOOL, HomeSwipeGestureAction.TOOL) }
    }
}

@Composable
internal fun gestureToolItems(state: com.tk.quicksearch.search.core.SearchUiState): List<Pair<String, String>> =
    ToolSettingsRegistry.definitions.mapNotNull { definition ->
        definition.aliasFeatureId?.let { it to stringResource(definition.titleResId) }
    } +
        state.customTools
            .filterNot { it.id in state.disabledCustomToolIds }
            .map { it.id to it.name } +
        state.taskerIntentTools.map { it.id to it.name }

internal fun SwipeGestureAction.labelResId(): Int =
    when (this) {
        SwipeGestureAction.CLOSE_QUICK_SEARCH -> R.string.settings_gesture_close_quick_search
        SwipeGestureAction.WIDGETS_PANEL -> R.string.settings_gesture_widget_panel
        SwipeGestureAction.SETTINGS -> R.string.settings_gesture_settings
        SwipeGestureAction.ALL_APPS -> R.string.settings_gesture_view_all_apps
        SwipeGestureAction.OPEN_KEYBOARD -> R.string.action_open_keyboard
        SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS -> R.string.settings_gesture_close_keyboard_notifications
        SwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        SwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        SwipeGestureAction.TOOL -> R.string.settings_tools_title
        SwipeGestureAction.NONE -> R.string.settings_gesture_none
    }

internal fun HomeSwipeGestureAction.labelResId(): Int =
    when (this) {
        HomeSwipeGestureAction.CLOSE_QUICK_SEARCH -> R.string.settings_gesture_close_quick_search
        HomeSwipeGestureAction.LOCK_SCREEN -> R.string.settings_gesture_lock_screen
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> R.string.settings_gesture_notification_panel
        HomeSwipeGestureAction.QUICK_SETTINGS -> R.string.settings_gesture_quick_settings
        HomeSwipeGestureAction.ALL_APPS -> R.string.settings_gesture_view_all_apps
        HomeSwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        HomeSwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        HomeSwipeGestureAction.TOOL -> R.string.settings_tools_title
        HomeSwipeGestureAction.OPEN_QUICK_SEARCH -> R.string.settings_gesture_open_quick_search
        HomeSwipeGestureAction.PINNED_GLANCE -> R.string.settings_gesture_pinned_glance
        HomeSwipeGestureAction.WIDGETS_PANEL -> R.string.settings_gesture_widget_panel
        HomeSwipeGestureAction.PLAY_PAUSE_MEDIA -> R.string.settings_gesture_play_pause_media
        HomeSwipeGestureAction.NONE -> R.string.settings_gesture_none
    }
