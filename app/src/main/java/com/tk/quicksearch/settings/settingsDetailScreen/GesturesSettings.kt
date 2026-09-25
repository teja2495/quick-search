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
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog

internal enum class SwipeDirection(val titleResId: Int, val defaultAction: SwipeGestureAction) {
    RIGHT(R.string.settings_gesture_swipe_right, SwipeGestureAction.WIDGETS_PANEL),
    LEFT(R.string.settings_gesture_swipe_left, SwipeGestureAction.SETTINGS),
    UP(R.string.settings_gesture_swipe_up, SwipeGestureAction.OPEN_KEYBOARD),
    DOWN(R.string.settings_gesture_swipe_down, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS),
}

internal enum class HomeGesture(val titleResId: Int, val defaultAction: HomeSwipeGestureAction) {
    SWIPE_UP(R.string.settings_gesture_swipe_up_home, HomeSwipeGestureAction.NONE),
    SWIPE_DOWN(R.string.settings_gesture_swipe_down_home, HomeSwipeGestureAction.NOTIFICATION_PANEL),
    DOUBLE_TAP(R.string.settings_gesture_double_tap_home, HomeSwipeGestureAction.NONE),
}

internal enum class AliasPickerKind { SEARCH_ENGINE, TOOL }

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
    var pendingLockScreenGesture by remember { mutableStateOf<HomeGesture?>(null) }
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
                pendingLockScreenGesture?.let { gesture ->
                    if (isLockScreenAccessibilityEnabled) {
                        preferences.setHomeActionFor(gesture, HomeSwipeGestureAction.LOCK_SCREEN)
                        preferences.setHomeCustomActionFor(gesture, null)
                        preferences.setHomeAliasTargetFor(gesture, null)
                        homeCustomActions = homeCustomActions + (gesture to null)
                        homeAliasTargets = homeAliasTargets + (gesture to null)
                    }
                    pendingLockScreenGesture = null
                }
                homeActions =
                    homeActions + (
                        HomeGesture.DOUBLE_TAP to
                            preferences.getHomeDoubleTapAction()
                    )
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
            allowsCloseQuickSearch = !isDefaultLauncher,
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
            allowsCloseQuickSearch = !isDefaultLauncher,
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
                    pendingLockScreenGesture = gesture
                    if (shouldShowAccessibilityDisclosure) {
                        showLockScreenAccessibilityDisclosure = true
                    } else {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
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
            onDismiss = {
                showLockScreenAccessibilityDisclosure = false
                pendingLockScreenGesture = null
            },
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
