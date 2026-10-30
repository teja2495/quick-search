package com.tk.quicksearch.search.searchScreen.searchRoute

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.appNotifications.openAppNotificationsSettings
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.search.data.AppNotificationsSettings
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.rememberAppNotificationsConfig
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.rememberResumeRefreshKey
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.settingsDetailScreen.GestureSettingTarget
import com.tk.quicksearch.settings.settingsDetailScreen.ToolSettingsRegistry
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.applySettingsCommand
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled

/**
 * Whether an app setting toggle row is on. App notifications keeps its state in
 * [AppNotificationsSettings] rather than [SearchUiState], and shows off without notification access,
 * which is checked again each time the screen resumes rather than on every read.
 */
@Composable
internal fun rememberAppSettingToggleChecked(uiState: SearchUiState): (AppSettingResult) -> Boolean {
    val context = LocalContext.current
    val refreshKey = rememberResumeRefreshKey()
    val hasNotificationAccess =
        remember(context, refreshKey) { NotificationDotsPermission.hasNotificationListenerAccess(context) }
    val appNotificationsChecked = rememberAppNotificationsConfig()?.enabled == true && hasNotificationAccess
    return { setting ->
        when (val toggleKey = setting.toggleKey) {
            null -> false
            AppSettingsToggleKey.APP_NOTIFICATIONS -> appNotificationsChecked
            else -> uiState.isAppSettingToggleEnabled(toggleKey)
        }
    }
}

internal data class RouteSettingActions(
    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    val onAppSettingClick: (AppSettingResult) -> Unit,
    val activeDialog: MutableState<AppSettingRouteDialog?>,
    val activeGestureDialog: MutableState<GestureSettingTarget?>,
    val onOpenAppSettingDestination: (AppSettingsDestination) -> Unit,
)

@Composable
internal fun rememberRouteSettingActions(
    viewModel: SearchViewModel,
    uiState: SearchUiState,
    isOverlayPresentation: Boolean,
    onShowToast: (Int) -> Unit,
    onPendingDirectDialToggleChange: (Boolean) -> Unit,
    onCallPermissionRequest: () -> Unit,
    onShowSecondaryRankingDialog: () -> Unit,
    onShowIconPackDialog: () -> Unit,
    onShowDefaultCalendarDialog: () -> Unit,
    onOpenAppSettingDestination: (AppSettingsDestination) -> Unit,
): RouteSettingActions {
    val context = LocalContext.current
    val activeDialog = remember { mutableStateOf<AppSettingRouteDialog?>(null) }
    val activeGestureDialog = remember { mutableStateOf<GestureSettingTarget?>(null) }
    val onNotificationDotsCheckedChange =
        rememberNotificationDotsCheckedChange { enabled ->
            viewModel.applySettingsCommand(
                SettingsCommand.Toggle(
                    key = AppSettingsToggleKey.NOTIFICATION_DOTS,
                    enabled = enabled,
                ),
            )
        }

    val onAppNotificationsCheckedChange =
        rememberNotificationDotsCheckedChange { enabled -> AppNotificationsSettings.setEnabled(context, enabled) }

    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = onAppSettingToggle@{ setting, enabled ->
        viewModel.trackRecentAppSettingTap(setting.id)
        val requiresLlmApiKey =
            ToolSettingsRegistry.definitions.any { it.toggleKey == setting.toggleKey && it.requiresLlmApiKey }
        if (requiresLlmApiKey && !uiState.hasApiKey) {
            activeDialog.value = AppSettingRouteDialog.API_KEY_REQUIRED
            return@onAppSettingToggle
        }
        when (val toggleKey = setting.toggleKey) {
            AppSettingsToggleKey.NOTIFICATION_DOTS -> onNotificationDotsCheckedChange(enabled)
            AppSettingsToggleKey.APP_NOTIFICATIONS -> onAppNotificationsCheckedChange(enabled)
            AppSettingsToggleKey.OVERLAY_MODE -> {
                val isDefaultHomeApp = context.isDefaultHomeApp()
                val shouldEnableOverlay = enabled && !isDefaultHomeApp
                viewModel.setOverlayModeEnabled(shouldEnableOverlay)
                if (shouldEnableOverlay) {
                    OverlayModeController.startOverlay(
                        context = context,
                        initialQuery = uiState.query.takeIf { it.isNotBlank() },
                    )
                    (context as? android.app.Activity)?.finish()
                } else if (isOverlayPresentation) {
                    OverlayModeController.openMainActivity(
                        context = context,
                        initialQuery = uiState.query.takeIf { it.isNotBlank() },
                        returnToOverlay = false,
                    )
                    (context as? android.app.Activity)?.finish()
                }
            }
            AppSettingsToggleKey.DIRECT_DIAL -> {
                if (enabled) {
                    if (uiState.hasCallPermission) {
                        viewModel.setDirectDialEnabled(true)
                    } else if (context is android.app.Activity) {
                        onPendingDirectDialToggleChange(true)
                        onCallPermissionRequest()
                    } else {
                        onShowToast(R.string.error_call_permission_required)
                    }
                } else {
                    onPendingDirectDialToggleChange(false)
                    viewModel.setDirectDialEnabled(false)
                }
            }
            null -> Unit
            else -> viewModel.applySettingsCommand(SettingsCommand.Toggle(toggleKey, enabled))
        }
    }

    val onAppSettingClick: (AppSettingResult) -> Unit = appSettingClick@{ setting ->
        viewModel.trackRecentAppSettingTap(setting.id)
        setting.destination?.let { destination ->
            if (destination == AppSettingsDestination.SEARCH_RESULT_RANKING) {
                onShowSecondaryRankingDialog()
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.ICON_PACKS) {
                viewModel.refreshIconPacks()
                onShowIconPackDialog()
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.TOP_MATCHES_PRIORITY) {
                activeDialog.value = AppSettingRouteDialog.TOP_MATCHES_PRIORITY
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.APP_SUGGESTION_TABS) {
                activeDialog.value = AppSettingRouteDialog.APP_SUGGESTION_TABS
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.PINNED_NOTIFICATION_ITEMS) {
                activeDialog.value = AppSettingRouteDialog.PINNED_NOTIFICATION_ITEMS
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.AI_MODEL) {
                activeDialog.value = AppSettingRouteDialog.AI_MODEL
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.EXPORT_SETTINGS) {
                activeDialog.value = AppSettingRouteDialog.EXPORT_SETTINGS
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.IMPORT_SETTINGS) {
                activeDialog.value = AppSettingRouteDialog.IMPORT_SETTINGS
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.APP_LANGUAGE) {
                activeDialog.value = AppSettingRouteDialog.APP_LANGUAGE
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.APP_NOTIFICATIONS) {
                openAppNotificationsSettings(context)
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.RELEASE_NOTES) {
                viewModel.showReleaseNotes()
                return@appSettingClick
            }
            destination.toGestureSettingTargetOrNull()?.let { target ->
                activeGestureDialog.value = target
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.OPEN_EVENTS_IN) {
                onShowDefaultCalendarDialog()
                return@appSettingClick
            }
            onOpenAppSettingDestination(destination)
        }
    }

    return RouteSettingActions(
        onAppSettingToggle,
        onAppSettingClick,
        activeDialog,
        activeGestureDialog,
        onOpenAppSettingDestination,
    )
}

private fun AppSettingsDestination.toGestureSettingTargetOrNull(): GestureSettingTarget? =
    when (this) {
        AppSettingsDestination.GESTURE_SWIPE_LEFT -> GestureSettingTarget.SWIPE_LEFT
        AppSettingsDestination.GESTURE_SWIPE_RIGHT -> GestureSettingTarget.SWIPE_RIGHT
        AppSettingsDestination.GESTURE_SWIPE_UP -> GestureSettingTarget.SWIPE_UP
        AppSettingsDestination.GESTURE_SWIPE_DOWN -> GestureSettingTarget.SWIPE_DOWN
        AppSettingsDestination.GESTURE_DOUBLE_TAP -> GestureSettingTarget.DOUBLE_TAP
        AppSettingsDestination.GESTURE_TWO_FINGER_SWIPE_UP -> GestureSettingTarget.TWO_FINGER_SWIPE_UP
        AppSettingsDestination.GESTURE_TWO_FINGER_SWIPE_DOWN -> GestureSettingTarget.TWO_FINGER_SWIPE_DOWN
        AppSettingsDestination.GESTURE_OPEN_KEYBOARD -> GestureSettingTarget.OPEN_KEYBOARD
        AppSettingsDestination.GESTURE_CLOSE_KEYBOARD -> GestureSettingTarget.CLOSE_KEYBOARD
        else -> null
    }
