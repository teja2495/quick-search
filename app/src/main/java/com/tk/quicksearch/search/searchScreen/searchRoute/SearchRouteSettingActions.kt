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
import com.tk.quicksearch.search.appSettings.AppSettingsRouteAction
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.search.apps.appLock.AppLockOutsideQuickSearchToggleState
import com.tk.quicksearch.search.apps.appLock.rememberAppLockOutsideQuickSearchToggleState
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpOutsideQuickSearchToggleState
import com.tk.quicksearch.search.apps.speedBump.rememberSpeedBumpOutsideQuickSearchToggleState
import com.tk.quicksearch.appNotifications.AppNotificationsSettings
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberAppNotificationsConfig
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberResumeRefreshKey
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.gestureSettings.GestureSettingTarget
import com.tk.quicksearch.settings.toolsSettings.ToolSettingsRegistry
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
    val speedBumpOn = rememberSpeedBumpOutsideQuickSearchToggleState().isOn
    val appLockOn = rememberAppLockOutsideQuickSearchToggleState().isOn
    // Keyed on the toggle values rather than the whole state, so the lambda (and the result rows
    // that take it) stays the same while unrelated state, such as other sections' results, changes.
    val toggleStates =
        remember(uiState) {
            AppSettingsToggleKey.entries.associateWith { uiState.isAppSettingToggleEnabled(it) }
        }
    val booleanSettings = uiState.booleanSettings
    return remember(toggleStates, booleanSettings, appNotificationsChecked, speedBumpOn, appLockOn) {
        { setting ->
            when (val toggleKey = setting.toggleKey) {
                null -> setting.booleanSetting?.valueIn(booleanSettings) == true
                AppSettingsToggleKey.APP_NOTIFICATIONS -> appNotificationsChecked
                AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH -> speedBumpOn
                AppSettingsToggleKey.APP_LOCK_OUTSIDE_QUICK_SEARCH -> appLockOn
                else -> toggleStates[toggleKey] == true
            }
        }
    }
}

internal data class RouteSettingActions(
    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    val onAppSettingClick: (AppSettingResult) -> Unit,
    val activeDialog: MutableState<AppSettingRouteDialog?>,
    val activeGestureDialog: MutableState<GestureSettingTarget?>,
    val onOpenAppSettingDestination: (AppSettingsTarget.Host) -> Unit,
    val speedBumpToggle: SpeedBumpOutsideQuickSearchToggleState,
    val appLockToggle: AppLockOutsideQuickSearchToggleState,
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
    onOpenAppSettingDestination: (AppSettingsTarget.Host) -> Unit,
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

    val speedBumpToggle = rememberSpeedBumpOutsideQuickSearchToggleState()
    val appLockToggle = rememberAppLockOutsideQuickSearchToggleState()

    val onAppNotificationsCheckedChange =
        rememberNotificationDotsCheckedChange { enabled -> AppNotificationsSettings.setEnabled(context, enabled) }

    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = onAppSettingToggle@{ setting, enabled ->
        viewModel.trackRecentAppSettingTap(setting.id)
        setting.booleanSetting?.let {
            viewModel.applySettingsCommand(SettingsCommand.SetBoolean(it, enabled))
            return@onAppSettingToggle
        }
        val requiresLlmApiKey =
            ToolSettingsRegistry.definitions.any { it.toggleKey == setting.toggleKey && it.requiresLlmApiKey }
        if (requiresLlmApiKey && !uiState.hasApiKey) {
            activeDialog.value = AppSettingRouteDialog.API_KEY_REQUIRED
            return@onAppSettingToggle
        }
        when (val toggleKey = setting.toggleKey) {
            AppSettingsToggleKey.NOTIFICATION_DOTS -> onNotificationDotsCheckedChange(enabled)
            AppSettingsToggleKey.APP_NOTIFICATIONS -> onAppNotificationsCheckedChange(enabled)
            AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH -> speedBumpToggle.setEnabled(enabled)
            AppSettingsToggleKey.APP_LOCK_OUTSIDE_QUICK_SEARCH -> appLockToggle.setEnabled(enabled)
            AppSettingsToggleKey.OVERLAY_MODE -> {
                val isDefaultHomeApp = context.isDefaultHomeApp()
                val shouldEnableOverlay = enabled && !isDefaultHomeApp
                if (shouldEnableOverlay) {
                    viewModel.setOverlayModeEnabled(true) {
                        OverlayModeController.startOverlay(
                            context = context,
                            initialQuery = uiState.query.takeIf { it.isNotBlank() },
                        )
                        (context as? android.app.Activity)?.finish()
                    }
                } else if (!enabled) {
                    viewModel.setOverlayModeEnabled(false) {
                        if (isOverlayPresentation) {
                            OverlayModeController.openMainActivity(context, returnToOverlay = false)
                        }
                    }
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
        when (val destination = setting.destination) {
            is AppSettingsTarget.Host -> onOpenAppSettingDestination(destination)
            is AppSettingsTarget.Gesture -> activeGestureDialog.value = destination.gesture
            is AppSettingsRouteAction -> when (destination) {
                AppSettingsRouteAction.SEARCH_RESULT_RANKING -> onShowSecondaryRankingDialog()
                AppSettingsRouteAction.ICON_PACKS -> {
                    viewModel.refreshIconPacks()
                    onShowIconPackDialog()
                }
                AppSettingsRouteAction.TOP_MATCHES_PRIORITY -> activeDialog.value = AppSettingRouteDialog.TOP_MATCHES_PRIORITY
                AppSettingsRouteAction.APP_SUGGESTION_TABS -> activeDialog.value = AppSettingRouteDialog.APP_SUGGESTION_TABS
                AppSettingsRouteAction.LAUNCHER_ICON -> activeDialog.value = AppSettingRouteDialog.LAUNCHER_ICON
                AppSettingsRouteAction.PINNED_SECTIONS_ORDER -> activeDialog.value = AppSettingRouteDialog.PINNED_SECTIONS_ORDER
                AppSettingsRouteAction.PINNED_NOTIFICATION_ITEMS -> activeDialog.value = AppSettingRouteDialog.PINNED_NOTIFICATION_ITEMS
                AppSettingsRouteAction.AI_MODEL -> activeDialog.value = AppSettingRouteDialog.AI_MODEL
                AppSettingsRouteAction.EXPORT_SETTINGS -> activeDialog.value = AppSettingRouteDialog.EXPORT_SETTINGS
                AppSettingsRouteAction.IMPORT_SETTINGS -> activeDialog.value = AppSettingRouteDialog.IMPORT_SETTINGS
                AppSettingsRouteAction.APP_LANGUAGE -> activeDialog.value = AppSettingRouteDialog.APP_LANGUAGE
                AppSettingsRouteAction.APP_NOTIFICATIONS -> {
                    if (NotificationDotsPermission.hasNotificationListenerAccess(context)) {
                        openAppNotificationsSettings(context)
                    } else {
                        onAppNotificationsCheckedChange(true)
                    }
                }
                AppSettingsRouteAction.RELEASE_NOTES -> viewModel.showReleaseNotes()
                AppSettingsRouteAction.OPEN_EVENTS_IN -> onShowDefaultCalendarDialog()
            }
            null -> Unit
        }
    }

    return RouteSettingActions(
        onAppSettingToggle,
        onAppSettingClick,
        activeDialog,
        activeGestureDialog,
        onOpenAppSettingDestination,
        speedBumpToggle,
        appLockToggle,
    )
}
