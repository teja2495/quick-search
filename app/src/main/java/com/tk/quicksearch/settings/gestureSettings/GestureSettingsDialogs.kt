package com.tk.quicksearch.settings.gestureSettings

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.preferences.HomeGesture
import com.tk.quicksearch.search.data.preferences.SwipeDirection
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog

/** Gesture assignments plus the dialog chain used to edit them, shared by Settings and search results. */
internal class GestureSettingsState(
    private val preferences: UserAppPreferences,
    val isDefaultLauncher: Boolean,
    isLockScreenAccessibilityEnabled: Boolean,
    private val onSwipeLeftMovedOffSettings: () -> Unit,
) {
    var actions by mutableStateOf(SwipeDirection.entries.associateWith(preferences::actionFor))
    var customActions by mutableStateOf(SwipeDirection.entries.associateWith(preferences::customActionFor))
    var aliasTargets by mutableStateOf(SwipeDirection.entries.associateWith(preferences::aliasTargetFor))
    var isLauncherSwipeRightEnabled by mutableStateOf(preferences.isLauncherSwipeRightEnabled())
        private set
    var homeActions by mutableStateOf(
        HomeGesture.entries.associateWith { gesture -> preferences.actionFor(gesture, isDefaultLauncher) },
    )
    var homeCustomActions by mutableStateOf(HomeGesture.entries.associateWith(preferences::customActionFor))
    var homeAliasTargets by mutableStateOf(HomeGesture.entries.associateWith(preferences::aliasTargetFor))
    var isLockScreenAccessibilityEnabled by mutableStateOf(isLockScreenAccessibilityEnabled)

    var selectedDirection by mutableStateOf<SwipeDirection?>(null)
    var customPickerDirection by mutableStateOf<SwipeDirection?>(null)
    var aliasPickerDirection by mutableStateOf<SwipeDirection?>(null)
    var aliasPickerHomeGesture by mutableStateOf<HomeGesture?>(null)
    var aliasPickerKind by mutableStateOf<AliasPickerKind?>(null)
    var homeCustomPickerGesture by mutableStateOf<HomeGesture?>(null)
    var selectedKeyboardAction by mutableStateOf<SwipeGestureAction?>(null)
    var selectedHomeGesture by mutableStateOf<HomeGesture?>(null)
    var showLockScreenAccessibilityDisclosure by mutableStateOf(false)
    var pendingLockScreenGesture by mutableStateOf<HomeGesture?>(null)

    /** True while any dialog in the chain is open or a lock-screen grant is pending. */
    val isEditing: Boolean
        get() =
            selectedDirection != null ||
                customPickerDirection != null ||
                aliasPickerKind != null ||
                homeCustomPickerGesture != null ||
                selectedKeyboardAction != null ||
                selectedHomeGesture != null ||
                showLockScreenAccessibilityDisclosure ||
                pendingLockScreenGesture != null

    fun open(target: GestureSettingTarget, context: Context) {
        when (target) {
            is GestureSettingTarget.Swipe -> selectedDirection = target.direction
            is GestureSettingTarget.Home -> {
                if (target.gesture.isNotch) {
                    openNotchGesture(target.gesture, context)
                } else {
                    selectedHomeGesture = target.gesture
                }
            }
            is GestureSettingTarget.Keyboard -> selectedKeyboardAction = target.action
        }
    }

    /** Notch taps only work through the accessibility service, so ask for it before editing them. */
    fun openNotchGesture(gesture: HomeGesture, context: Context) {
        if (isLockScreenAccessibilityEnabled) {
            selectedHomeGesture = gesture
        } else {
            requestAccessibility(context)
        }
    }

    fun requestAccessibility(context: Context) {
        if (shouldShowAccessibilityDisclosure) {
            showLockScreenAccessibilityDisclosure = true
        } else {
            LockScreenAccessibilityService.openAccessibilitySettings(context)
        }
    }

    fun onResume(context: Context) {
        isLockScreenAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context)
        pendingLockScreenGesture?.let { gesture ->
            if (isLockScreenAccessibilityEnabled) {
                saveHome(gesture, HomeSwipeGestureAction.LOCK_SCREEN)
            }
            pendingLockScreenGesture = null
        }
        homeActions = homeActions + (HomeGesture.DOUBLE_TAP to preferences.actionFor(HomeGesture.DOUBLE_TAP))
    }

    fun save(
        direction: SwipeDirection,
        action: SwipeGestureAction,
        customAction: CustomWidgetButtonAction? = null,
        aliasTarget: String? = null,
    ) {
        preferences.setActionFor(direction, action)
        preferences.setCustomActionFor(direction, customAction?.toJson())
        preferences.setAliasTargetFor(direction, aliasTarget)
        if (direction == SwipeDirection.LEFT && action != SwipeGestureAction.SETTINGS) {
            onSwipeLeftMovedOffSettings()
        }
        actions = actions + (direction to action)
        customActions = customActions + (direction to customAction?.toJson())
        aliasTargets = aliasTargets + (direction to aliasTarget)
    }

    fun saveHome(
        gesture: HomeGesture,
        action: HomeSwipeGestureAction,
        customActionJson: String? = null,
        aliasTarget: String? = null,
    ) {
        preferences.setActionFor(gesture, action)
        preferences.setCustomActionFor(gesture, customActionJson)
        preferences.setAliasTargetFor(gesture, aliasTarget)
        homeActions = homeActions + (gesture to action)
        homeCustomActions = homeCustomActions + (gesture to customActionJson)
        homeAliasTargets = homeAliasTargets + (gesture to aliasTarget)
    }

    /** As the default launcher, swipe right opens the widget panel or All Apps, or does nothing. */
    val launcherSwipeRightAction: SwipeGestureAction
        get() =
            when {
                !isLauncherSwipeRightEnabled -> SwipeGestureAction.NONE
                actions[SwipeDirection.RIGHT] == SwipeGestureAction.ALL_APPS -> SwipeGestureAction.ALL_APPS
                else -> SwipeGestureAction.WIDGETS_PANEL
            }

    fun selectLauncherSwipeRight(action: SwipeGestureAction) {
        val enabled = action != SwipeGestureAction.NONE
        preferences.setLauncherSwipeRightEnabled(enabled)
        isLauncherSwipeRightEnabled = enabled
        if (action == SwipeGestureAction.ALL_APPS) {
            save(SwipeDirection.RIGHT, SwipeGestureAction.ALL_APPS)
        } else if (enabled && actions[SwipeDirection.RIGHT] == SwipeGestureAction.ALL_APPS) {
            save(SwipeDirection.RIGHT, SwipeGestureAction.WIDGETS_PANEL)
        }
    }

    fun deleteCustomAction(actionJson: String) {
        SwipeDirection.entries.forEach { direction ->
            if (customActions[direction] == actionJson) save(direction, direction.defaultAction)
        }
        HomeGesture.entries.forEach { gesture ->
            if (homeCustomActions[gesture] == actionJson) {
                preferences.setActionFor(gesture, gesture.defaultAction)
                preferences.setCustomActionFor(gesture, null)
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
            if (homeAliasTargets[gesture] == targetId) saveHome(gesture, gesture.defaultAction)
        }
    }

    fun setKeyboardGesture(action: SwipeGestureAction, targetDirection: SwipeDirection?) {
        val updatedActions = actions.toMutableMap()
        val updatedCustomActions = customActions.toMutableMap()
        KEYBOARD_DIRECTIONS.forEach { direction ->
            if (updatedActions[direction] == action || direction == targetDirection) {
                updatedActions[direction] = if (direction == targetDirection) action else SwipeGestureAction.NONE
                updatedCustomActions[direction] = null
            }
        }
        KEYBOARD_DIRECTIONS.forEach { direction ->
            preferences.setActionFor(direction, updatedActions.getValue(direction))
            preferences.setCustomActionFor(direction, updatedCustomActions[direction])
        }
        actions = updatedActions
        customActions = updatedCustomActions
        selectedKeyboardAction = null
    }

    fun keyboardGestureDirection(action: SwipeGestureAction): SwipeDirection? =
        KEYBOARD_DIRECTIONS.firstOrNull { actions[it] == action }

    fun closeAliasPicker(reopenPrevious: Boolean) {
        if (reopenPrevious) {
            selectedDirection = aliasPickerDirection
            selectedHomeGesture = aliasPickerHomeGesture
        }
        aliasPickerDirection = null
        aliasPickerHomeGesture = null
        aliasPickerKind = null
    }

    private companion object {
        val KEYBOARD_DIRECTIONS = listOf(SwipeDirection.UP, SwipeDirection.DOWN)
    }
}

@Composable
internal fun rememberGestureSettingsState(
    searchViewModel: SearchViewModel,
    initialTarget: GestureSettingTarget? = null,
): GestureSettingsState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDefaultLauncher = context.isDefaultHomeApp()
    val state =
        remember(context) {
            GestureSettingsState(
                preferences = UserAppPreferences(context.applicationContext),
                isDefaultLauncher = isDefaultLauncher,
                isLockScreenAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context),
                onSwipeLeftMovedOffSettings = { searchViewModel.setSettingsIconEnabled(true) },
            ).also { state -> initialTarget?.let { target -> state.open(target, context) } }
        }
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) state.onResume(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

/** Renders whichever gesture dialog [state] currently has open. */
@Composable
internal fun GestureSettingsDialogs(
    state: GestureSettingsState,
    searchViewModel: SearchViewModel,
) {
    val context = LocalContext.current
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()

    state.selectedDirection?.let { direction ->
        if (direction == SwipeDirection.RIGHT && state.isDefaultLauncher) {
            LauncherSwipeRightActionDialog(
                selectedAction = state.launcherSwipeRightAction,
                onSelect = { action ->
                    state.selectLauncherSwipeRight(action)
                    state.selectedDirection = null
                },
                onDismiss = { state.selectedDirection = null },
            )
        } else {
            GestureActionDialog(
                direction = direction,
                allowsCloseQuickSearch = !state.isDefaultLauncher,
                selectedAction = state.actions.getValue(direction),
                selectedCustomActionJson = state.customActions[direction],
                selectedAliasTarget = state.aliasTargets[direction],
                customActions = allCustomActions(state.customActions, state.homeCustomActions),
                aliasItems = allGestureAliasItems(searchState, state.aliasTargets, state.homeAliasTargets),
                onSelectDefault = { action ->
                    state.save(direction, action)
                    state.selectedDirection = null
                },
                onPickCustom = {
                    state.selectedDirection = null
                    state.customPickerDirection = direction
                },
                onPickSearchEngine = {
                    state.selectedDirection = null
                    state.aliasPickerDirection = direction
                    state.aliasPickerKind = AliasPickerKind.SEARCH_ENGINE
                },
                onPickTool = {
                    state.selectedDirection = null
                    state.aliasPickerDirection = direction
                    state.aliasPickerKind = AliasPickerKind.TOOL
                },
                onSelectCustom = { action ->
                    state.save(direction, SwipeGestureAction.CUSTOM, action)
                    state.selectedDirection = null
                },
                onSelectAlias = { action, targetId ->
                    state.save(direction, action, aliasTarget = targetId)
                    state.selectedDirection = null
                },
                onDeleteCustom = state::deleteCustomAction,
                onDeleteAlias = state::deleteAliasTarget,
                onDismiss = { state.selectedDirection = null },
            )
        }
    }

    when (state.aliasPickerKind) {
        AliasPickerKind.SEARCH_ENGINE ->
            GestureSearchEnginePickerDialog(
                targets = searchState.searchTargetsOrder,
                onDismiss = { state.closeAliasPicker(reopenPrevious = true) },
                onSelect = { target ->
                    val targetId = target.getId()
                    state.aliasPickerDirection?.let {
                        state.save(it, SwipeGestureAction.SEARCH_ENGINE, aliasTarget = targetId)
                    }
                    state.aliasPickerHomeGesture?.let { gesture ->
                        state.saveHome(gesture, HomeSwipeGestureAction.SEARCH_ENGINE, aliasTarget = targetId)
                    }
                    state.closeAliasPicker(reopenPrevious = false)
                },
            )
        AliasPickerKind.TOOL ->
            GestureToolPickerDialog(
                state = searchState,
                onDismiss = { state.closeAliasPicker(reopenPrevious = true) },
                onSelect = { targetId ->
                    state.aliasPickerDirection?.let { direction ->
                        state.save(direction, SwipeGestureAction.TOOL, aliasTarget = targetId)
                    }
                    state.aliasPickerHomeGesture?.let { gesture ->
                        state.saveHome(gesture, HomeSwipeGestureAction.TOOL, aliasTarget = targetId)
                    }
                    state.closeAliasPicker(reopenPrevious = false)
                },
            )
        null -> Unit
    }

    state.customPickerDirection?.let { direction ->
        // The picker searches through the shared query; put back whatever was typed before.
        val queryBeforePicker = remember(direction) { searchState.query }
        CustomWidgetButtonPickerDialog(
            currentAction = state.customActions[direction]?.let(CustomWidgetButtonAction::fromJson),
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = {
                searchViewModel.onQueryChange(queryBeforePicker)
                state.customPickerDirection = null
                state.selectedDirection = direction
            },
            onSelect = { action ->
                state.save(direction, SwipeGestureAction.CUSTOM, action)
                searchViewModel.onQueryChange(queryBeforePicker)
                state.customPickerDirection = null
            },
        )
    }

    state.homeCustomPickerGesture?.let { gesture ->
        val queryBeforePicker = remember(gesture) { searchState.query }
        CustomWidgetButtonPickerDialog(
            currentAction = state.homeCustomActions[gesture]?.let(CustomWidgetButtonAction::fromJson),
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = {
                searchViewModel.onQueryChange(queryBeforePicker)
                state.homeCustomPickerGesture = null
                state.selectedHomeGesture = gesture
            },
            onSelect = { action ->
                state.saveHome(
                    gesture,
                    HomeSwipeGestureAction.CUSTOM,
                    customActionJson = action.toJson(),
                    aliasTarget = state.homeAliasTargets[gesture],
                )
                searchViewModel.onQueryChange(queryBeforePicker)
                state.homeCustomPickerGesture = null
            },
        )
    }

    state.selectedKeyboardAction?.let { action ->
        KeyboardGestureDialog(
            action = action,
            selectedDirection = state.keyboardGestureDirection(action),
            onSelect = { direction -> state.setKeyboardGesture(action, direction) },
            onDismiss = { state.selectedKeyboardAction = null },
        )
    }

    state.selectedHomeGesture?.let { gesture ->
        HomeVerticalGestureDialog(
            titleResId = gesture.titleResId,
            availableActions = gesture.availableActions(state.isDefaultLauncher),
            hasLockScreenAccessibilityPermission = state.isLockScreenAccessibilityEnabled,
            selectedAction = state.homeActions.getValue(gesture),
            selectedCustomActionJson = state.homeCustomActions[gesture],
            selectedAliasTarget = state.homeAliasTargets[gesture],
            customActions = allCustomActions(state.customActions, state.homeCustomActions),
            aliasItems = allGestureAliasItems(searchState, state.aliasTargets, state.homeAliasTargets),
            onSelectDefault = { action ->
                state.saveHome(gesture, action, aliasTarget = state.homeAliasTargets[gesture])
                state.selectedHomeGesture = null
            },
            onSelectLockScreen = {
                state.selectedHomeGesture = null
                if (LockScreenAccessibilityService.isEnabled(context)) {
                    state.saveHome(gesture, HomeSwipeGestureAction.LOCK_SCREEN)
                } else {
                    state.pendingLockScreenGesture = gesture
                    if (shouldShowAccessibilityDisclosure) {
                        state.showLockScreenAccessibilityDisclosure = true
                    } else {
                        LockScreenAccessibilityService.openAccessibilitySettings(context)
                    }
                }
            },
            onPickCustom = {
                state.selectedHomeGesture = null
                state.homeCustomPickerGesture = gesture
            },
            onPickSearchEngine = {
                state.selectedHomeGesture = null
                state.aliasPickerHomeGesture = gesture
                state.aliasPickerKind = AliasPickerKind.SEARCH_ENGINE
            },
            onPickTool = {
                state.selectedHomeGesture = null
                state.aliasPickerHomeGesture = gesture
                state.aliasPickerKind = AliasPickerKind.TOOL
            },
            onSelectCustom = { action ->
                state.saveHome(
                    gesture,
                    HomeSwipeGestureAction.CUSTOM,
                    customActionJson = action.toJson(),
                    aliasTarget = state.homeAliasTargets[gesture],
                )
                state.selectedHomeGesture = null
            },
            onSelectAlias = { action, targetId ->
                state.saveHome(gesture, action, aliasTarget = targetId)
                state.selectedHomeGesture = null
            },
            onDeleteCustom = state::deleteCustomAction,
            onDeleteAlias = state::deleteAliasTarget,
            onDismiss = { state.selectedHomeGesture = null },
        )
    }

    if (state.showLockScreenAccessibilityDisclosure) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                state.showLockScreenAccessibilityDisclosure = false
                LockScreenAccessibilityService.openAccessibilitySettings(context)
            },
            onDismiss = {
                state.showLockScreenAccessibilityDisclosure = false
                state.pendingLockScreenGesture = null
            },
        )
    }
}
