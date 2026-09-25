package com.tk.quicksearch.search.searchScreen

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.search.searchScreen.HomeHorizontalSwipe
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
                    if (isLauncherSwipeRightEnabled) onOpenWidgetsPanelFromSwipe?.invoke()
                } else {
                    when (swipeActions[0]) {
                        SwipeGestureAction.CLOSE_QUICK_SEARCH -> closeQuickSearch()
                        SwipeGestureAction.WIDGETS_PANEL -> onOpenWidgetsPanelFromSwipe?.invoke()
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
    val swipeNavigationModifier =
        Modifier.pointerInput(isDefaultLauncher, isLauncherSwipeRightEnabled, swipeActions, customSwipeActions, uiState.query) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalHorizontalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalHorizontalDrag >= SWIPE_NAVIGATION_THRESHOLD_PX) {
                        handleHomeHorizontalSwipe(HomeHorizontalSwipe.RIGHT)
                    } else if (totalHorizontalDrag <= -SWIPE_NAVIGATION_THRESHOLD_PX) {
                        handleHomeHorizontalSwipe(HomeHorizontalSwipe.LEFT)
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
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
    )
}
