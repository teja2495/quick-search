package com.tk.quicksearch.search.searchScreen

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import androidx.compose.runtime.remember
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultAction
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.applySettingsCommand

internal const val RATE_QUICK_SEARCH_SETTING_ID = "app_settings_rate_quick_search"

@Composable
internal fun rememberRateQuickSearchSetting(): AppSettingResult =
    remember {
        AppSettingResult(
            id = RATE_QUICK_SEARCH_SETTING_ID,
            title = "",
            action = AppSettingResultAction.NAVIGATE,
            destination = AppSettingsDestination.RATE_QUICK_SEARCH,
        )
    }

internal data class RouteSettingActions(
    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    val onAppSettingClick: (AppSettingResult) -> Unit,
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
    val onNotificationDotsCheckedChange =
        rememberNotificationDotsCheckedChange { enabled ->
            viewModel.applySettingsCommand(
                SettingsCommand.Toggle(
                    key = AppSettingsToggleKey.NOTIFICATION_DOTS,
                    enabled = enabled,
                ),
            )
        }

    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit = { setting, enabled ->
        viewModel.trackRecentAppSettingTap(setting.id)
        when (val toggleKey = setting.toggleKey) {
            AppSettingsToggleKey.NOTIFICATION_DOTS -> onNotificationDotsCheckedChange(enabled)
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
        if (setting.action != AppSettingResultAction.NAVIGATE) return@appSettingClick
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
            if (destination == AppSettingsDestination.OPEN_EVENTS_IN) {
                onShowDefaultCalendarDialog()
                return@appSettingClick
            }
            if (destination == AppSettingsDestination.RATE_QUICK_SEARCH) {
                viewModel.markRateQuickSearchCompleted()
            }
            onOpenAppSettingDestination(destination)
        }
    }

    return RouteSettingActions(onAppSettingToggle, onAppSettingClick)
}
