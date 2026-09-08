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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Search
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
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
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
    RIGHT(R.string.settings_gesture_swipe_right, SwipeGestureAction.WIDGETS_PANEL),
    LEFT(R.string.settings_gesture_swipe_left, SwipeGestureAction.SETTINGS),
    UP(R.string.settings_gesture_swipe_up, SwipeGestureAction.OPEN_KEYBOARD),
    DOWN(R.string.settings_gesture_swipe_down, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS),
}

private enum class HomeGesture(val titleResId: Int, val defaultAction: HomeSwipeGestureAction) {
    SWIPE_UP(R.string.settings_gesture_swipe_up_home, HomeSwipeGestureAction.NONE),
    SWIPE_DOWN(R.string.settings_gesture_swipe_down_home, HomeSwipeGestureAction.NOTIFICATION_PANEL),
    DOUBLE_TAP(R.string.settings_gesture_double_tap_home, HomeSwipeGestureAction.NONE),
}

private enum class AliasPickerKind { SEARCH_ENGINE, TOOL }

@Composable
fun GesturesSettingsSection(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val isDefaultLauncher = context.isDefaultHomeApp()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    var actions by remember { mutableStateOf(SwipeDirection.entries.associateWith(preferences::actionFor)) }
    var customActions by remember { mutableStateOf(SwipeDirection.entries.associateWith(preferences::customActionFor)) }
    var aliasTargets by remember { mutableStateOf(SwipeDirection.entries.associateWith(preferences::aliasTargetFor)) }
    var selectedDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var isLauncherSwipeRightEnabled by remember { mutableStateOf(preferences.isLauncherSwipeRightEnabled()) }
    var customPickerDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var aliasPickerDirection by remember { mutableStateOf<SwipeDirection?>(null) }
    var aliasPickerHomeGesture by remember { mutableStateOf<HomeGesture?>(null) }
    var aliasPickerKind by remember { mutableStateOf<AliasPickerKind?>(null) }
    var homeCustomPickerGesture by remember { mutableStateOf<HomeGesture?>(null) }
    var selectedKeyboardAction by remember { mutableStateOf<SwipeGestureAction?>(null) }
    var selectedHomeGesture by remember { mutableStateOf<HomeGesture?>(null) }
    var showLockScreenAccessibilityDisclosure by remember { mutableStateOf(false) }
    var homeActions by remember {
        mutableStateOf(
            HomeGesture.entries.associateWith { gesture ->
                preferences.homeActionFor(gesture, isDefaultLauncher)
            },
        )
    }
    var homeCustomActions by remember {
        mutableStateOf(
            HomeGesture.entries.associateWith(preferences::homeCustomActionFor),
        )
    }
    var homeAliasTargets by remember { mutableStateOf(HomeGesture.entries.associateWith(preferences::homeAliasTargetFor)) }
    var isLockScreenAccessibilityEnabled by remember {
        mutableStateOf(LockScreenAccessibilityService.isEnabled(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLockScreenAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun save(direction: SwipeDirection, action: SwipeGestureAction, customAction: CustomWidgetButtonAction? = null, aliasTarget: String? = null) {
        preferences.setActionFor(direction, action)
        preferences.setCustomActionFor(direction, customAction?.toJson())
        preferences.setAliasTargetFor(direction, aliasTarget)
        if (direction == SwipeDirection.LEFT && action != SwipeGestureAction.SETTINGS) {
            searchViewModel.setSettingsIconEnabled(true)
        }
        actions = actions + (direction to action)
        customActions = customActions + (direction to customAction?.toJson())
        aliasTargets = aliasTargets + (direction to aliasTarget)
    }

    fun deleteCustomAction(actionJson: String) {
        SwipeDirection.entries.forEach { direction ->
            if (customActions[direction] == actionJson) {
                save(direction, direction.defaultAction)
            }
        }
        HomeGesture.entries.forEach { gesture ->
            if (homeCustomActions[gesture] == actionJson) {
                preferences.setHomeActionFor(gesture, gesture.defaultAction)
                preferences.setHomeCustomActionFor(gesture, null)
                homeActions = homeActions + (gesture to gesture.defaultAction)
                homeCustomActions = homeCustomActions + (gesture to null)
            }
        }
    }

    fun deleteAliasTarget(targetId: String) {
        SwipeDirection.entries.forEach { direction ->
            if (aliasTargets[direction] == targetId) save(direction, direction.defaultAction)
        }
        HomeGesture.entries.forEach { gesture ->
            if (homeAliasTargets[gesture] == targetId) {
                preferences.setHomeActionFor(gesture, gesture.defaultAction)
                preferences.setHomeCustomActionFor(gesture, null)
                preferences.setHomeAliasTargetFor(gesture, null)
                homeActions = homeActions + (gesture to gesture.defaultAction)
                homeCustomActions = homeCustomActions + (gesture to null)
                homeAliasTargets = homeAliasTargets + (gesture to null)
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
        if (direction == SwipeDirection.RIGHT && isDefaultLauncher) {
            LauncherSwipeRightActionDialog(
                isEnabled = isLauncherSwipeRightEnabled,
                onEnabledChange = { enabled ->
                    preferences.setLauncherSwipeRightEnabled(enabled)
                    isLauncherSwipeRightEnabled = enabled
                    selectedDirection = null
                },
                onDismiss = { selectedDirection = null },
            )
        } else GestureActionDialog(
            direction = direction,
            selectedAction = actions.getValue(direction),
            selectedCustomActionJson = customActions[direction],
            selectedAliasTarget = aliasTargets[direction],
            customActions = allCustomActions(customActions, homeCustomActions),
            aliasItems = allGestureAliasItems(searchState, aliasTargets, homeAliasTargets),
            onSelectDefault = { action ->
                save(direction, action)
                selectedDirection = null
            },
            onPickCustom = {
                selectedDirection = null
                customPickerDirection = direction
            },
            onPickSearchEngine = {
                selectedDirection = null
                aliasPickerDirection = direction
                aliasPickerKind = AliasPickerKind.SEARCH_ENGINE
            },
            onPickTool = {
                selectedDirection = null
                aliasPickerDirection = direction
                aliasPickerKind = AliasPickerKind.TOOL
            },
            onSelectCustom = { action ->
                save(direction, SwipeGestureAction.CUSTOM, action)
                selectedDirection = null
            },
            onSelectAlias = { action, targetId -> save(direction, action, aliasTarget = targetId); selectedDirection = null },
            onDeleteCustom = ::deleteCustomAction,
            onDeleteAlias = ::deleteAliasTarget,
            onDismiss = { selectedDirection = null },
        )
    }

    if (aliasPickerKind == AliasPickerKind.SEARCH_ENGINE) {
        GestureSearchEnginePickerDialog(
            targets = searchState.searchTargetsOrder,
            onDismiss = {
                selectedDirection = aliasPickerDirection
                selectedHomeGesture = aliasPickerHomeGesture
                aliasPickerDirection = null; aliasPickerHomeGesture = null; aliasPickerKind = null
            },
            onSelect = { target ->
                val targetId = target.getId()
                aliasPickerDirection?.let { save(it, SwipeGestureAction.SEARCH_ENGINE, aliasTarget = targetId) }
                aliasPickerHomeGesture?.let { gesture ->
                    preferences.setHomeActionFor(gesture, HomeSwipeGestureAction.SEARCH_ENGINE)
                    preferences.setHomeCustomActionFor(gesture, null)
                    preferences.setHomeAliasTargetFor(gesture, targetId)
                    homeActions = homeActions + (gesture to HomeSwipeGestureAction.SEARCH_ENGINE)
                    homeCustomActions = homeCustomActions + (gesture to null)
                    homeAliasTargets = homeAliasTargets + (gesture to targetId)
                }
                aliasPickerDirection = null; aliasPickerHomeGesture = null; aliasPickerKind = null
            },
        )
    } else if (aliasPickerKind == AliasPickerKind.TOOL) {
        GestureToolPickerDialog(
            state = searchState,
            onDismiss = {
                selectedDirection = aliasPickerDirection
                selectedHomeGesture = aliasPickerHomeGesture
                aliasPickerDirection = null; aliasPickerHomeGesture = null; aliasPickerKind = null
            },
            onSelect = { targetId ->
                aliasPickerDirection?.let { direction ->
                    save(direction, SwipeGestureAction.TOOL, aliasTarget = targetId)
                }
                aliasPickerHomeGesture?.let { gesture ->
                    val action = HomeSwipeGestureAction.TOOL
                    preferences.setHomeActionFor(gesture, action)
                    preferences.setHomeCustomActionFor(gesture, null)
                    preferences.setHomeAliasTargetFor(gesture, targetId)
                    homeActions = homeActions + (gesture to action)
                    homeCustomActions = homeCustomActions + (gesture to null)
                    homeAliasTargets = homeAliasTargets + (gesture to targetId)
                }
                aliasPickerDirection = null; aliasPickerHomeGesture = null; aliasPickerKind = null
            },
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
                selectedDirection = direction
            },
            onSelect = { action ->
                save(direction, SwipeGestureAction.CUSTOM, action)
                searchViewModel.onQueryChange("")
                customPickerDirection = null
            },
        )
    }

    homeCustomPickerGesture?.let { gesture ->
        CustomWidgetButtonPickerDialog(
            currentAction = homeCustomActions[gesture]?.let(CustomWidgetButtonAction::fromJson),
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = {
                searchViewModel.onQueryChange("")
                homeCustomPickerGesture = null
                selectedHomeGesture = gesture
            },
            onSelect = { action ->
                preferences.setHomeActionFor(gesture, HomeSwipeGestureAction.CUSTOM)
                preferences.setHomeCustomActionFor(gesture, action.toJson())
                homeActions = homeActions + (gesture to HomeSwipeGestureAction.CUSTOM)
                homeCustomActions = homeCustomActions + (gesture to action.toJson())
                searchViewModel.onQueryChange("")
                homeCustomPickerGesture = null
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

    selectedHomeGesture?.let { gesture ->
        HomeVerticalGestureDialog(
            titleResId = gesture.titleResId,
            allowsNotificationPanel = gesture == HomeGesture.SWIPE_DOWN || gesture == HomeGesture.DOUBLE_TAP,
            allowsLockScreen = gesture == HomeGesture.DOUBLE_TAP,
            hasLockScreenAccessibilityPermission = isLockScreenAccessibilityEnabled,
            selectedAction = homeActions.getValue(gesture),
            selectedCustomActionJson = homeCustomActions[gesture],
            selectedAliasTarget = homeAliasTargets[gesture],
            customActions = allCustomActions(customActions, homeCustomActions),
            aliasItems = allGestureAliasItems(searchState, aliasTargets, homeAliasTargets),
            onSelectDefault = { action ->
                preferences.setHomeActionFor(gesture, action)
                preferences.setHomeCustomActionFor(gesture, null)
                homeActions = homeActions + (gesture to action)
                homeCustomActions = homeCustomActions + (gesture to null)
                selectedHomeGesture = null
            },
            onSelectLockScreen = {
                if (LockScreenAccessibilityService.isEnabled(context)) {
                    preferences.setHomeActionFor(gesture, HomeSwipeGestureAction.LOCK_SCREEN)
                    preferences.setHomeCustomActionFor(gesture, null)
                    preferences.setHomeAliasTargetFor(gesture, null)
                    homeActions = homeActions + (gesture to HomeSwipeGestureAction.LOCK_SCREEN)
                    homeCustomActions = homeCustomActions + (gesture to null)
                    homeAliasTargets = homeAliasTargets + (gesture to null)
                    selectedHomeGesture = null
                } else {
                    selectedHomeGesture = null
                    showLockScreenAccessibilityDisclosure = true
                }
            },
            onPickCustom = {
                selectedHomeGesture = null
                homeCustomPickerGesture = gesture
            },
            onPickSearchEngine = {
                selectedHomeGesture = null
                aliasPickerHomeGesture = gesture
                aliasPickerKind = AliasPickerKind.SEARCH_ENGINE
            },
            onPickTool = {
                selectedHomeGesture = null
                aliasPickerHomeGesture = gesture
                aliasPickerKind = AliasPickerKind.TOOL
            },
            onSelectCustom = { action ->
                preferences.setHomeActionFor(gesture, HomeSwipeGestureAction.CUSTOM)
                preferences.setHomeCustomActionFor(gesture, action.toJson())
                homeActions = homeActions + (gesture to HomeSwipeGestureAction.CUSTOM)
                homeCustomActions = homeCustomActions + (gesture to action.toJson())
                selectedHomeGesture = null
            },
            onSelectAlias = { action, targetId ->
                preferences.setHomeActionFor(gesture, action)
                preferences.setHomeCustomActionFor(gesture, null)
                preferences.setHomeAliasTargetFor(gesture, targetId)
                homeActions = homeActions + (gesture to action)
                homeCustomActions = homeCustomActions + (gesture to null)
                homeAliasTargets = homeAliasTargets + (gesture to targetId)
                selectedHomeGesture = null
            },
            onDeleteCustom = ::deleteCustomAction,
            onDeleteAlias = ::deleteAliasTarget,
            onDismiss = { selectedHomeGesture = null },
        )
    }

    if (showLockScreenAccessibilityDisclosure) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                showLockScreenAccessibilityDisclosure = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showLockScreenAccessibilityDisclosure = false },
        )
    }

    Column(modifier = modifier) {
        SettingsCard(modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding)) {
            Column {
                val gestureDirections = SwipeDirection.entries
                gestureDirections.forEachIndexed { index, direction ->
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
                                        stringResource(
                                            if (isLauncherSwipeRightEnabled) R.string.settings_gesture_widget_panel
                                            else R.string.settings_gesture_none,
                                        )
                                    } else if (direction == SwipeDirection.UP || direction == SwipeDirection.DOWN) {
                                        val gesture = if (direction == SwipeDirection.UP) HomeGesture.SWIPE_UP else HomeGesture.SWIPE_DOWN
                                        homeGestureDescription(homeActions.getValue(gesture), homeCustomActions[gesture], homeAliasTargets[gesture], searchState)
                                    } else {
                                        gestureDescription(actions.getValue(direction), customActions[direction], aliasTargets[direction], searchState)
                                    },
                                actionOnPress = {
                                    when (direction) {
                                        SwipeDirection.UP -> selectedHomeGesture = HomeGesture.SWIPE_UP
                                        SwipeDirection.DOWN -> selectedHomeGesture = HomeGesture.SWIPE_DOWN
                                        else -> selectedDirection = direction
                                    }
                                },
                            ),
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                    )
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(HomeGesture.DOUBLE_TAP.titleResId),
                            icon = HomeGesture.DOUBLE_TAP.icon(),
                            description = homeGestureDescription(
                                homeActions.getValue(HomeGesture.DOUBLE_TAP),
                                homeCustomActions[HomeGesture.DOUBLE_TAP],
                                homeAliasTargets[HomeGesture.DOUBLE_TAP],
                                searchState,
                            ),
                            actionOnPress = { selectedHomeGesture = HomeGesture.DOUBLE_TAP },
                        ),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
                )
            }
        }

        Text(
            text = stringResource(R.string.settings_keyboard_gestures_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
        )
        Text(
            text = stringResource(R.string.settings_keyboard_gestures_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
        )
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.action_open_keyboard),
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

@Composable
private fun LauncherSwipeRightActionDialog(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_gesture_swipe_right)) },
        text = {
            Column {
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_none),
                    selected = !isEnabled,
                    onClick = { onEnabledChange(false) },
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                GestureActionRow(
                    label = stringResource(R.string.settings_gesture_widget_panel),
                    selected = isEnabled,
                    onClick = { onEnabledChange(true) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

private fun SwipeDirection.gestureIcon() =
    when (this) {
        SwipeDirection.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
        SwipeDirection.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack
        SwipeDirection.UP -> Icons.Rounded.ArrowUpward
        SwipeDirection.DOWN -> Icons.Rounded.ArrowDownward
    }

private fun HomeGesture.icon() =
    when (this) {
        HomeGesture.SWIPE_UP -> Icons.Rounded.ArrowUpward
        HomeGesture.SWIPE_DOWN,
        HomeGesture.DOUBLE_TAP,
        -> Icons.Rounded.ArrowDownward
    }

@Composable
private fun HomeVerticalGestureDialog(
    titleResId: Int,
    allowsNotificationPanel: Boolean,
    allowsLockScreen: Boolean,
    hasLockScreenAccessibilityPermission: Boolean,
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
                if (allowsNotificationPanel) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                    GestureActionRow(
                        label = stringResource(R.string.settings_gesture_notification_panel),
                        selected = selectedAction == HomeSwipeGestureAction.NOTIFICATION_PANEL,
                        onClick = { onSelectDefault(HomeSwipeGestureAction.NOTIFICATION_PANEL) },
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
private fun LockScreenAccessibilityDisclosureDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accessibility_lock_screen_disclosure_title)) },
        text = { Text(stringResource(R.string.accessibility_lock_screen_disclosure_message)) },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(stringResource(R.string.action_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_not_now))
            }
        },
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
private fun GestureActionRow(
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
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_remove))
            }
        }
    }
}

@Composable
private fun GesturePickerActions(
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
            GesturePickerRow(stringResource(R.string.settings_gesture_custom), Icons.Rounded.Add, onPickCustom)
            HorizontalDivider(color = AppColors.SettingsDivider)
            GesturePickerRow(stringResource(R.string.settings_app_shortcuts_filter_search_engines), Icons.Rounded.Search, onPickSearchEngine)
            HorizontalDivider(color = AppColors.SettingsDivider)
            GesturePickerRow(stringResource(R.string.settings_tools_title), Icons.Rounded.Build, onPickTool)
        }
    }
}

@Composable
private fun GesturePickerRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
private fun gestureDescription(action: SwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == SwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == SwipeGestureAction.SEARCH_ENGINE || action == SwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

@Composable
private fun homeGestureDescription(action: HomeSwipeGestureAction, customActionJson: String?, aliasTarget: String?, state: com.tk.quicksearch.search.core.SearchUiState): String =
    customActionJson?.let(CustomWidgetButtonAction::fromJson)?.displayLabel()
        ?.takeIf { action == HomeSwipeGestureAction.CUSTOM }
        ?: aliasTarget?.takeIf { action == HomeSwipeGestureAction.SEARCH_ENGINE || action == HomeSwipeGestureAction.TOOL }?.let { aliasDisplayName(it, state) }
        ?: stringResource(action.labelResId())

private fun allCustomActions(
    swipeCustomActions: Map<*, String?>,
    homeCustomActions: Map<*, String?>,
): List<CustomWidgetButtonAction> =
    (swipeCustomActions.values + homeCustomActions.values)
        .mapNotNull(CustomWidgetButtonAction::fromJson)
        .distinctBy { it.toJson() }

@Composable
private fun aliasDisplayName(
    targetId: String,
    state: com.tk.quicksearch.search.core.SearchUiState,
): String =
    state.searchTargetsOrder.firstOrNull { it.getId() == targetId }?.getDisplayName()
        ?: gestureToolItems(state).firstOrNull { it.first == targetId }?.second
        ?: targetId

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

private fun UserAppPreferences.aliasTargetFor(direction: SwipeDirection): String? =
    when (direction) {
        SwipeDirection.RIGHT -> getSwipeRightAliasTarget()
        SwipeDirection.LEFT -> getSwipeLeftAliasTarget()
        SwipeDirection.UP -> getSwipeUpAliasTarget()
        SwipeDirection.DOWN -> getSwipeDownAliasTarget()
    }

private fun UserAppPreferences.setAliasTargetFor(direction: SwipeDirection, targetId: String?) {
    when (direction) {
        SwipeDirection.RIGHT -> setSwipeRightAliasTarget(targetId)
        SwipeDirection.LEFT -> setSwipeLeftAliasTarget(targetId)
        SwipeDirection.UP -> setSwipeUpAliasTarget(targetId)
        SwipeDirection.DOWN -> setSwipeDownAliasTarget(targetId)
    }
}

private fun UserAppPreferences.homeActionFor(
    gesture: HomeGesture,
    isDefaultLauncher: Boolean,
): HomeSwipeGestureAction =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAction(isDefaultLauncher)
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAction()
    }

private fun UserAppPreferences.setHomeActionFor(gesture: HomeGesture, action: HomeSwipeGestureAction) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAction(action)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAction(action)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAction(action)
    }
}

private fun UserAppPreferences.homeCustomActionFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpCustomAction()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownCustomAction()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapCustomAction()
    }

private fun UserAppPreferences.setHomeCustomActionFor(gesture: HomeGesture, actionJson: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpCustomAction(actionJson)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownCustomAction(actionJson)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapCustomAction(actionJson)
    }
}

private fun UserAppPreferences.homeAliasTargetFor(gesture: HomeGesture): String? =
    when (gesture) {
        HomeGesture.SWIPE_UP -> getHomeSwipeUpAliasTarget()
        HomeGesture.SWIPE_DOWN -> getHomeSwipeDownAliasTarget()
        HomeGesture.DOUBLE_TAP -> getHomeDoubleTapAliasTarget()
    }

private fun UserAppPreferences.setHomeAliasTargetFor(gesture: HomeGesture, targetId: String?) {
    when (gesture) {
        HomeGesture.SWIPE_UP -> setHomeSwipeUpAliasTarget(targetId)
        HomeGesture.SWIPE_DOWN -> setHomeSwipeDownAliasTarget(targetId)
        HomeGesture.DOUBLE_TAP -> setHomeDoubleTapAliasTarget(targetId)
    }
}

@Composable
private fun GestureToolPickerDialog(
    state: com.tk.quicksearch.search.core.SearchUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val tools = ToolSettingsRegistry.definitions.map { GestureToolItem(it.aliasFeatureId, stringResource(it.titleResId), it.icon) } +
        state.customTools.filterNot { it.id in state.disabledCustomToolIds }.map { GestureToolItem(it.id, it.name, Icons.Rounded.Build) } +
        state.taskerIntentTools.map { GestureToolItem(it.id, it.name, Icons.Rounded.Build) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_tools_title)) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(tools, key = { it.id }) { tool ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onSelect(tool.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(start = DesignTokens.SpacingMedium),
                        )
                        Text(
                            text = tool.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingMedium),
                        )
                    }
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

private data class GestureToolItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun GestureSearchEnginePickerDialog(
    targets: List<SearchTarget>,
    onDismiss: () -> Unit,
    onSelect: (SearchTarget) -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_shortcuts_filter_search_engines)) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(targets, key = { it.getId() }) { target ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onSelect(target) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchTargetIcon(
                            target = target,
                            iconSize = 28.dp,
                            modifier = Modifier.padding(start = DesignTokens.SpacingMedium),
                        )
                        Text(
                            text = target.getDisplayName(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = DesignTokens.SpacingMedium),
                        )
                    }
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

private data class GestureAliasItem(
    val id: String,
    val label: String,
    val swipeAction: SwipeGestureAction,
    val homeAction: HomeSwipeGestureAction,
)

@Composable
private fun allGestureAliasItems(
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
private fun gestureToolItems(state: com.tk.quicksearch.search.core.SearchUiState): List<Pair<String, String>> =
    ToolSettingsRegistry.definitions.map { it.aliasFeatureId to stringResource(it.titleResId) } +
        state.customTools
            .filterNot { it.id in state.disabledCustomToolIds }
            .map { it.id to it.name } +
        state.taskerIntentTools.map { it.id to it.name }

private fun SwipeGestureAction.labelResId(): Int =
    when (this) {
        SwipeGestureAction.WIDGETS_PANEL -> R.string.settings_gesture_widget_panel
        SwipeGestureAction.SETTINGS -> R.string.settings_gesture_settings
        SwipeGestureAction.OPEN_KEYBOARD -> R.string.action_open_keyboard
        SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS -> R.string.settings_gesture_close_keyboard_notifications
        SwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        SwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        SwipeGestureAction.TOOL -> R.string.settings_tools_title
        SwipeGestureAction.NONE -> R.string.settings_gesture_none
    }

private fun HomeSwipeGestureAction.labelResId(): Int =
    when (this) {
        HomeSwipeGestureAction.LOCK_SCREEN -> R.string.settings_gesture_lock_screen
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> R.string.settings_gesture_notification_panel
        HomeSwipeGestureAction.CUSTOM -> R.string.settings_gesture_custom
        HomeSwipeGestureAction.SEARCH_ENGINE -> R.string.settings_app_shortcuts_filter_search_engines
        HomeSwipeGestureAction.TOOL -> R.string.settings_tools_title
        HomeSwipeGestureAction.NONE -> R.string.settings_gesture_none
    }
