package com.tk.quicksearch.search.searchScreen.searchRoute

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
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
import com.tk.quicksearch.R
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
                gesturePreferences.getSwipeRightAction(),
                gesturePreferences.getSwipeLeftAction(),
                gesturePreferences.getSwipeUpAction(),
                gesturePreferences.getSwipeDownAction(),
            ),
        )
    }
    var customSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.getSwipeRightCustomAction(),
                gesturePreferences.getSwipeLeftCustomAction(),
                gesturePreferences.getSwipeUpCustomAction(),
                gesturePreferences.getSwipeDownCustomAction(),
            ),
        )
    }
    var swipeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.getSwipeRightAliasTarget(), gesturePreferences.getSwipeLeftAliasTarget(), gesturePreferences.getSwipeUpAliasTarget(), gesturePreferences.getSwipeDownAliasTarget())) }
    var homeSwipeUpAction by remember {
        mutableStateOf(gesturePreferences.getHomeSwipeUpAction())
    }
    var homeSwipeDownAction by remember {
        mutableStateOf(gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher))
    }
    var homeDoubleTapAction by remember {
        mutableStateOf(gesturePreferences.getHomeDoubleTapAction())
    }
    var homeCustomSwipeActions by remember {
        mutableStateOf(
            listOf(
                gesturePreferences.getHomeSwipeUpCustomAction(),
                gesturePreferences.getHomeSwipeDownCustomAction(),
                gesturePreferences.getHomeDoubleTapCustomAction(),
            ),
        )
    }
    var homeAliasTargets by remember { mutableStateOf(listOf(gesturePreferences.getHomeSwipeUpAliasTarget(), gesturePreferences.getHomeSwipeDownAliasTarget(), gesturePreferences.getHomeDoubleTapAliasTarget())) }
    var isLauncherSwipeRightEnabled by remember { mutableStateOf(gesturePreferences.isLauncherSwipeRightEnabled()) }
    var twoFingerSwipeUp by remember { mutableStateOf(gesturePreferences.twoFingerSwipeUpGesture()) }
    var twoFingerSwipeDown by remember { mutableStateOf(gesturePreferences.twoFingerSwipeDownGesture()) }
    DisposableEffect(gesturePreferences) {
        val preferences =
            context.applicationContext.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                Context.MODE_PRIVATE,
            )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            swipeActions =
                listOf(
                    gesturePreferences.getSwipeRightAction(),
                    gesturePreferences.getSwipeLeftAction(),
                    gesturePreferences.getSwipeUpAction(),
                    gesturePreferences.getSwipeDownAction(),
                )
            customSwipeActions =
                listOf(
                    gesturePreferences.getSwipeRightCustomAction(),
                    gesturePreferences.getSwipeLeftCustomAction(),
                    gesturePreferences.getSwipeUpCustomAction(),
                    gesturePreferences.getSwipeDownCustomAction(),
                )
            swipeAliasTargets = listOf(gesturePreferences.getSwipeRightAliasTarget(), gesturePreferences.getSwipeLeftAliasTarget(), gesturePreferences.getSwipeUpAliasTarget(), gesturePreferences.getSwipeDownAliasTarget())
            homeSwipeUpAction = gesturePreferences.getHomeSwipeUpAction()
            homeSwipeDownAction = gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher)
            homeDoubleTapAction = gesturePreferences.getHomeDoubleTapAction()
            homeCustomSwipeActions =
                listOf(
                    gesturePreferences.getHomeSwipeUpCustomAction(),
                    gesturePreferences.getHomeSwipeDownCustomAction(),
                    gesturePreferences.getHomeDoubleTapCustomAction(),
                )
            homeAliasTargets = listOf(gesturePreferences.getHomeSwipeUpAliasTarget(), gesturePreferences.getHomeSwipeDownAliasTarget(), gesturePreferences.getHomeDoubleTapAliasTarget())
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
                homeSwipeDownAction = gesturePreferences.getHomeSwipeDownAction(isDefaultLauncher)
                val isLockScreenAvailable = LockScreenAccessibilityService.isEnabled(context)
                if (
                    !isLockScreenAvailable &&
                    gesturePreferences.getHomeDoubleTapAction() == HomeSwipeGestureAction.LOCK_SCREEN
                ) {
                    gesturePreferences.setHomeDoubleTapAction(HomeSwipeGestureAction.NONE)
                }
                homeDoubleTapAction = gesturePreferences.getHomeDoubleTapAction()
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
    val twoFingerSwipeModifier =
        Modifier.twoFingerVerticalSwipe(
            enabled =
                uiState.query.isBlank() &&
                    (twoFingerSwipeUp.action != HomeSwipeGestureAction.NONE ||
                        twoFingerSwipeDown.action != HomeSwipeGestureAction.NONE),
            onSwipeUp = { performTwoFingerSwipe(twoFingerSwipeUp) },
            onSwipeDown = { performTwoFingerSwipe(twoFingerSwipeDown) },
        )
    val pageSwipeController = LocalPageSwipeController.current
    val latestHomeSwipe by rememberUpdatedState(handleHomeHorizontalSwipe)
    val swipeNavigationModifier =
        twoFingerSwipeModifier.pointerInput(isDefaultLauncher, isLauncherSwipeRightEnabled, swipeActions, customSwipeActions, uiState.query, pageSwipeController) {
            detectPageSwipe(
                controller = pageSwipeController,
                thresholdPx = SWIPE_NAVIGATION_THRESHOLD_PX,
                targetFor = { direction ->
                    when {
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
                    latestHomeSwipe(
                        if (direction > 0) HomeHorizontalSwipe.RIGHT else HomeHorizontalSwipe.LEFT,
                    )
                },
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
    )
}

internal data class TwoFingerSwipeGesture(
    val action: HomeSwipeGestureAction,
    val customActionJson: String?,
    val aliasTarget: String?,
)

private fun UserAppPreferences.twoFingerSwipeUpGesture() =
    TwoFingerSwipeGesture(
        getHomeTwoFingerSwipeUpAction(),
        getHomeTwoFingerSwipeUpCustomAction(),
        getHomeTwoFingerSwipeUpAliasTarget(),
    )

private fun UserAppPreferences.twoFingerSwipeDownGesture() =
    TwoFingerSwipeGesture(
        getHomeTwoFingerSwipeDownAction(),
        getHomeTwoFingerSwipeDownCustomAction(),
        getHomeTwoFingerSwipeDownAliasTarget(),
    )
