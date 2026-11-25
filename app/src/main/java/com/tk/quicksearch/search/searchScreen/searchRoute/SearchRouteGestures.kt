package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.search.data.preferences.HomeGesture
import com.tk.quicksearch.search.data.preferences.SwipeDirection
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import com.tk.quicksearch.widgetsPanel.HomeWidgetEditGuard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.util.hapticGestureThreshold
import com.tk.quicksearch.app.navigation.LocalPageSwipeController
import com.tk.quicksearch.app.navigation.RootDestination
import com.tk.quicksearch.app.navigation.detectPageSwipe
import com.tk.quicksearch.notchGesture.NotchGestureRequest
import com.tk.quicksearch.notchGesture.NotchGestureRequests
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.apps.AllAppsDrawerController
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.search.searchScreen.shouldCloseSearchSurfaceAfterExternalNavigation
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.search.searchScreen.HomeHorizontalSwipe
import com.tk.quicksearch.search.searchScreen.performHomeGesture
import kotlinx.coroutines.launch

private const val SWIPE_NAVIGATION_THRESHOLD_PX = 140f

internal data class RouteGestures(
    val requestBiometricAuthentication: (String, () -> Unit) -> Unit,
    val requestDeviceCredentialAuthentication: (String, () -> Unit) -> Unit,
    val runAfterAppUnlock: (String, String, () -> Unit) -> Unit,
    val swipeActions: List<SwipeGestureAction>,
    val customSwipeActions: List<String?>,
    val swipeAliasTargets: List<String?>,
    val homeSwipeUpAction: HomeSwipeGestureAction,
    val homeSwipeDownAction: HomeSwipeGestureAction,
    val homeDoubleTapAction: HomeSwipeGestureAction,
    val homeCustomSwipeActions: List<String?>,
    val homeAliasTargets: List<String?>,
    val closeQuickSearch: () -> Unit,
    val handleHomeHorizontalSwipe: (HomeHorizontalSwipe) -> Unit,
    val swipeNavigationModifier: Modifier,
    val allAppsDrawer: AllAppsDrawerController,
    val homeWidgetEditGuard: HomeWidgetEditGuard,
)

@Composable
internal fun rememberRouteGestures(
    viewModel: SearchViewModel,
    uiState: SearchUiState,
    isOverlayPresentation: Boolean,
    onSettingsClick: () -> Unit,
    onOpenWidgetsPanelFromSwipe: (() -> Unit)?,
    onOverlayDismissRequest: (() -> Unit)?,
    onCloseAppRequest: (() -> Unit)?,
): RouteGestures {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gesturePreferences = remember(context.applicationContext) {
        UserAppPreferences(context.applicationContext)
    }
    var isDefaultLauncher by remember { mutableStateOf(context.cachedDefaultHomeAppStatus()) }
    val allAppsDrawer = remember { AllAppsDrawerController() }
    var pendingDeviceCredentialAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deviceCredentialLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val action = pendingDeviceCredentialAction
            pendingDeviceCredentialAction = null
            if (result.resultCode == Activity.RESULT_OK) action?.invoke()
        }
    val requestBiometricAuthentication = remember(context) {
        { promptTitle: String, onAuthenticated: () -> Unit ->
            AppLockGate.authenticate(
                context,
                promptTitle,
                BiometricManager.Authenticators.BIOMETRIC_WEAK,
                onAuthenticated = onAuthenticated,
            )
        }
    }
    val requestDeviceCredentialAuthentication =
        remember(context, deviceCredentialLauncher) {
            { promptTitle: String, onAuthenticated: () -> Unit ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    AppLockGate.authenticate(
                        context,
                        promptTitle,
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                        onAuthenticated = onAuthenticated,
                    )
                } else {
                    val keyguardManager = context.getSystemService(KeyguardManager::class.java)
                    @Suppress("DEPRECATION")
                    val credentialIntent =
                        keyguardManager?.createConfirmDeviceCredentialIntent(promptTitle, null)
                    if (credentialIntent != null) {
                        pendingDeviceCredentialAction = onAuthenticated
                        deviceCredentialLauncher.launch(credentialIntent)
                    }
                }
                Unit
            }
        }
    val runAfterAppUnlock: (String, String, () -> Unit) -> Unit = { packageName, appName, action ->
        AppLockGate.runAfterUnlock(context, packageName, appName, action = action)
    }
    var swipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.actionFor(SwipeDirection.RIGHT),
                gesturePreferences.actionFor(SwipeDirection.LEFT),
                gesturePreferences.actionFor(SwipeDirection.UP),
                gesturePreferences.actionFor(SwipeDirection.DOWN),
            ),
        )
    }
    var customSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.customActionFor(SwipeDirection.RIGHT),
                gesturePreferences.customActionFor(SwipeDirection.LEFT),
                gesturePreferences.customActionFor(SwipeDirection.UP),
                gesturePreferences.customActionFor(SwipeDirection.DOWN),
            ),
        )
    }
    var swipeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.aliasTargetFor(SwipeDirection.RIGHT), gesturePreferences.aliasTargetFor(SwipeDirection.LEFT), gesturePreferences.aliasTargetFor(SwipeDirection.UP), gesturePreferences.aliasTargetFor(SwipeDirection.DOWN))) }
    var homeSwipeUpAction by remember {
        mutableStateOf(gesturePreferences.actionFor(HomeGesture.SWIPE_UP))
    }
    var homeSwipeDownAction by remember {
        mutableStateOf(gesturePreferences.actionFor(HomeGesture.SWIPE_DOWN, isDefaultLauncher))
    }
    var homeDoubleTapAction by remember {
        mutableStateOf(gesturePreferences.actionFor(HomeGesture.DOUBLE_TAP))
    }
    var homeCustomSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.customActionFor(HomeGesture.SWIPE_UP),
                gesturePreferences.customActionFor(HomeGesture.SWIPE_DOWN),
                gesturePreferences.customActionFor(HomeGesture.DOUBLE_TAP),
            ),
        )
    }
    var homeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.aliasTargetFor(HomeGesture.SWIPE_UP), gesturePreferences.aliasTargetFor(HomeGesture.SWIPE_DOWN), gesturePreferences.aliasTargetFor(HomeGesture.DOUBLE_TAP))) }
    var isLauncherSwipeRightEnabled by remember { mutableStateOf(gesturePreferences.isLauncherSwipeRightEnabled()) }
    var twoFingerSwipeUp by remember { mutableStateOf(gesturePreferences.twoFingerSwipeUpGesture()) }
    var twoFingerSwipeDown by remember { mutableStateOf(gesturePreferences.twoFingerSwipeDownGesture()) }
    DisposableEffect(gesturePreferences) {
        val preferences =
            context.applicationContext.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                Context.MODE_PRIVATE,
            )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (!com.tk.quicksearch.search.data.preferences.GesturesPreferences.isRouteGestureKey(key)) {
                return@OnSharedPreferenceChangeListener
            }
            swipeActions =
                listOf(
                    gesturePreferences.actionFor(SwipeDirection.RIGHT),
                    gesturePreferences.actionFor(SwipeDirection.LEFT),
                    gesturePreferences.actionFor(SwipeDirection.UP),
                    gesturePreferences.actionFor(SwipeDirection.DOWN),
                )
            customSwipeActions =
                listOf(
                    gesturePreferences.customActionFor(SwipeDirection.RIGHT),
                    gesturePreferences.customActionFor(SwipeDirection.LEFT),
                    gesturePreferences.customActionFor(SwipeDirection.UP),
                    gesturePreferences.customActionFor(SwipeDirection.DOWN),
                )
            swipeAliasTargets = listOf(gesturePreferences.aliasTargetFor(SwipeDirection.RIGHT), gesturePreferences.aliasTargetFor(SwipeDirection.LEFT), gesturePreferences.aliasTargetFor(SwipeDirection.UP), gesturePreferences.aliasTargetFor(SwipeDirection.DOWN))
            homeSwipeUpAction = gesturePreferences.actionFor(HomeGesture.SWIPE_UP)
            homeSwipeDownAction = gesturePreferences.actionFor(HomeGesture.SWIPE_DOWN, isDefaultLauncher)
            homeDoubleTapAction = gesturePreferences.actionFor(HomeGesture.DOUBLE_TAP)
            homeCustomSwipeActions =
                listOf(
                    gesturePreferences.customActionFor(HomeGesture.SWIPE_UP),
                    gesturePreferences.customActionFor(HomeGesture.SWIPE_DOWN),
                    gesturePreferences.customActionFor(HomeGesture.DOUBLE_TAP),
                )
            homeAliasTargets = listOf(gesturePreferences.aliasTargetFor(HomeGesture.SWIPE_UP), gesturePreferences.aliasTargetFor(HomeGesture.SWIPE_DOWN), gesturePreferences.aliasTargetFor(HomeGesture.DOUBLE_TAP))
            isLauncherSwipeRightEnabled = gesturePreferences.isLauncherSwipeRightEnabled()
            twoFingerSwipeUp = gesturePreferences.twoFingerSwipeUpGesture()
            twoFingerSwipeDown = gesturePreferences.twoFingerSwipeDownGesture()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = context.isDefaultHomeApp()
                homeSwipeDownAction = gesturePreferences.actionFor(HomeGesture.SWIPE_DOWN, isDefaultLauncher)
                // Clear the lock action only when the user turned the service off. A service that is
                // on but not running yet (or stopped by Android) must not wipe the user's choice.
                // Check the saved action first so the accessibility lookups only run when it matters.
                if (
                    gesturePreferences.actionFor(HomeGesture.DOUBLE_TAP) == HomeSwipeGestureAction.LOCK_SCREEN &&
                    !LockScreenAccessibilityService.isEnabled(context) &&
                    !LockScreenAccessibilityService.isTurnedOnInSettings(context)
                ) {
                    gesturePreferences.setActionFor(HomeGesture.DOUBLE_TAP, HomeSwipeGestureAction.NONE)
                }
                homeDoubleTapAction = gesturePreferences.actionFor(HomeGesture.DOUBLE_TAP)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val closeQuickSearch: () -> Unit = {
        if (!isDefaultLauncher) {
            if (isOverlayPresentation) onOverlayDismissRequest?.invoke() else onCloseAppRequest?.invoke()
        }
    }
    val handleHomeHorizontalSwipe: (HomeHorizontalSwipe) -> Unit = { swipe ->
        when (swipe) {
            HomeHorizontalSwipe.RIGHT -> {
                if (isDefaultLauncher) {
                    when {
                        !isLauncherSwipeRightEnabled -> Unit
                        swipeActions[0] == SwipeGestureAction.ALL_APPS -> allAppsDrawer.open()
                        else -> onOpenWidgetsPanelFromSwipe?.invoke()
                    }
                } else {
                    when (swipeActions[0]) {
                        SwipeGestureAction.CLOSE_QUICK_SEARCH -> closeQuickSearch()
                        SwipeGestureAction.WIDGETS_PANEL -> onOpenWidgetsPanelFromSwipe?.invoke()
                        SwipeGestureAction.ALL_APPS -> allAppsDrawer.open()
                        SwipeGestureAction.CUSTOM -> {
                            com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
                                .fromJson(customSwipeActions[0])
                                ?.let { action -> com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity.launch(context, action) }
                        }
                        SwipeGestureAction.SEARCH_ENGINE -> swipeAliasTargets[0]?.let(viewModel::activateGestureSearchTarget)
                        SwipeGestureAction.TOOL -> swipeAliasTargets[0]?.let(viewModel::activateGestureTool)
                        else -> Unit
                    }
                }
            }
            HomeHorizontalSwipe.LEFT -> {
                when (swipeActions[1]) {
                    SwipeGestureAction.CLOSE_QUICK_SEARCH -> closeQuickSearch()
                    SwipeGestureAction.SETTINGS -> onSettingsClick()
                    SwipeGestureAction.ALL_APPS -> allAppsDrawer.open()
                    SwipeGestureAction.CUSTOM -> {
                        com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
                            .fromJson(customSwipeActions[1])
                            ?.let { action -> com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity.launch(context, action) }
                    }
                    SwipeGestureAction.SEARCH_ENGINE -> swipeAliasTargets[1]?.let(viewModel::activateGestureSearchTarget)
                    SwipeGestureAction.TOOL -> swipeAliasTargets[1]?.let(viewModel::activateGestureTool)
                    else -> Unit
                }
            }
        }
    }
    val performTwoFingerSwipe: (TwoFingerSwipeGesture) -> Unit = { gesture ->
        gesture.action.performHomeGesture(
            gesture.customActionJson,
            gesture.aliasTarget,
            context,
            { action, targetId ->
                if (action == HomeSwipeGestureAction.SEARCH_ENGINE) {
                    viewModel.activateGestureSearchTarget(targetId)
                } else {
                    viewModel.activateGestureTool(targetId)
                }
            },
            closeQuickSearch,
            allAppsDrawer::open,
        )
    }
    val homeWidgetEditGuard = remember { HomeWidgetEditGuard() }
    val twoFingerSwipeModifier =
        Modifier.twoFingerVerticalSwipe(
            enabled =
                uiState.query.isBlank() &&
                    (twoFingerSwipeUp.action != HomeSwipeGestureAction.NONE ||
                        twoFingerSwipeDown.action != HomeSwipeGestureAction.NONE),
            onSwipeUp = { if (!homeWidgetEditGuard.isEditing) performTwoFingerSwipe(twoFingerSwipeUp) },
            onSwipeDown = { if (!homeWidgetEditGuard.isEditing) performTwoFingerSwipe(twoFingerSwipeDown) },
        )
    val pageSwipeController = LocalPageSwipeController.current
    val view = LocalView.current
    val latestHomeSwipe by rememberUpdatedState(handleHomeHorizontalSwipe)
    // No page or two-finger swipes while a Home widget is in edit mode. Checked when the gesture
    // runs rather than by swapping modifiers, so entering edit mode never disturbs the touch that
    // long-pressed the widget and is now dragging it.
    val swipeNavigationModifier =
        twoFingerSwipeModifier.pointerInput(isDefaultLauncher, isLauncherSwipeRightEnabled, swipeActions, customSwipeActions, pageSwipeController) {
            detectPageSwipe(
                controller = pageSwipeController,
                thresholdPx = SWIPE_NAVIGATION_THRESHOLD_PX,
                targetFor = { direction ->
                    when {
                        homeWidgetEditGuard.isEditing -> null
                        direction > 0 && isDefaultLauncher ->
                            RootDestination.WidgetsPanel.takeIf {
                                isLauncherSwipeRightEnabled && swipeActions[0] != SwipeGestureAction.ALL_APPS
                            }
                        direction > 0 ->
                            RootDestination.WidgetsPanel.takeIf { swipeActions[0] == SwipeGestureAction.WIDGETS_PANEL }
                        else -> RootDestination.Settings.takeIf { swipeActions[1] == SwipeGestureAction.SETTINGS }
                    }
                },
                onSwipe = { direction ->
                    if (!homeWidgetEditGuard.isEditing) latestHomeSwipe(
                        if (direction > 0) HomeHorizontalSwipe.RIGHT else HomeHorizontalSwipe.LEFT,
                    )
                },
                onThresholdCrossed = { hapticGestureThreshold(view, it) },
            )
        }
    val shouldAutoCloseSearchSurface =
        shouldCloseSearchSurfaceAfterExternalNavigation(
            autoCloseEnabled = uiState.autoCloseOverlay,
            isOverlayPresentation = isOverlayPresentation,
            isDefaultLauncher = isDefaultLauncher,
        )
    LaunchedEffect(shouldAutoCloseSearchSurface, isOverlayPresentation) {
        viewModel.externalNavigationEvent.collect {
            if (!shouldAutoCloseSearchSurface) return@collect
            if (isOverlayPresentation) {
                onOverlayDismissRequest?.invoke()
            } else {
                onCloseAppRequest?.invoke()
            }
        }
    }

    // A notch tap that needs this screen (All Apps, a search engine or tool) waits here until resumed.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            NotchGestureRequests.pending.collect { pending ->
                if (pending == null) return@collect
                when (val request = NotchGestureRequests.consume()) {
                    NotchGestureRequest.AllApps -> allAppsDrawer.open()
                    is NotchGestureRequest.SearchEngine -> viewModel.activateGestureSearchTarget(request.targetId)
                    is NotchGestureRequest.Tool -> viewModel.activateGestureTool(request.featureId)
                    null -> Unit
                }
            }
        }
    }

    return RouteGestures(
        requestBiometricAuthentication = requestBiometricAuthentication,
        requestDeviceCredentialAuthentication = requestDeviceCredentialAuthentication,
        runAfterAppUnlock = runAfterAppUnlock,
        swipeActions = swipeActions,
        customSwipeActions = customSwipeActions,
        swipeAliasTargets = swipeAliasTargets,
        homeSwipeUpAction = homeSwipeUpAction,
        homeSwipeDownAction = homeSwipeDownAction,
        homeDoubleTapAction = homeDoubleTapAction,
        homeCustomSwipeActions = homeCustomSwipeActions,
        homeAliasTargets = homeAliasTargets,
        closeQuickSearch = closeQuickSearch,
        handleHomeHorizontalSwipe = handleHomeHorizontalSwipe,
        swipeNavigationModifier = swipeNavigationModifier,
        allAppsDrawer = allAppsDrawer,
        homeWidgetEditGuard = homeWidgetEditGuard,
    )
}

internal data class TwoFingerSwipeGesture(
    val action: HomeSwipeGestureAction,
    val customActionJson: String?,
    val aliasTarget: String?,
)

private fun UserAppPreferences.twoFingerSwipeUpGesture() =
    TwoFingerSwipeGesture(
        actionFor(HomeGesture.TWO_FINGER_SWIPE_UP),
        customActionFor(HomeGesture.TWO_FINGER_SWIPE_UP),
        aliasTargetFor(HomeGesture.TWO_FINGER_SWIPE_UP),
    )

private fun UserAppPreferences.twoFingerSwipeDownGesture() =
    TwoFingerSwipeGesture(
        actionFor(HomeGesture.TWO_FINGER_SWIPE_DOWN),
        customActionFor(HomeGesture.TWO_FINGER_SWIPE_DOWN),
        aliasTargetFor(HomeGesture.TWO_FINGER_SWIPE_DOWN),
    )
