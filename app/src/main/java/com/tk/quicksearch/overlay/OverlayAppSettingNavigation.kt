package com.tk.quicksearch.overlay

import android.content.Context
import com.tk.quicksearch.app.navigation.AppSettingsDestinationHandlers
import com.tk.quicksearch.app.navigation.handleAppSettingsDestination
import com.tk.quicksearch.app.navigation.launchDevelopmentPage
import com.tk.quicksearch.app.navigation.launchRateQuickSearch
import com.tk.quicksearch.app.navigation.openDefaultAssistantSettings
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.util.FeedbackUtils

internal fun handleOverlayAppSettingDestination(
    context: Context,
    destination: AppSettingsDestination,
    viewModel: SearchViewModel,
    autoCloseOverlay: Boolean,
    onCloseRequested: () -> Unit,
) {
    val closeIfNeeded = {
        if (autoCloseOverlay) onCloseRequested()
    }

    handleAppSettingsDestination(
        destination = destination,
        handlers =
            AppSettingsDestinationHandlers(
                onOpenSettingsDetail = { detailType ->
                    openOverlaySettingsDetail(context, detailType, onCloseRequested)
                },
                onReloadApps = { viewModel.refreshApps(showToast = true) },
                onReloadContacts = { viewModel.refreshContacts(showToast = true) },
                onReloadFiles = { viewModel.refreshFiles(showToast = true) },
                onSendFeedback = {
                    FeedbackUtils.launchFeedbackEmail(context = context, feedbackText = null)
                    closeIfNeeded()
                },
                onRateQuickSearch = {
                    launchRateQuickSearch(context)
                    closeIfNeeded()
                },
                onOpenDevelopmentPage = {
                    launchDevelopmentPage(context)
                    closeIfNeeded()
                },
                onSetDefaultAssistant = {
                    openDefaultAssistantSettings(context)
                    closeIfNeeded()
                },
                onAddHomeScreenWidget = {
                    com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget(context)
                    closeIfNeeded()
                },
                onAddQuickSettingsTile = {
                    com.tk.quicksearch.tile.requestAddQuickSearchTile(context)
                    closeIfNeeded()
                },
            ),
    )
}

private fun openOverlaySettingsDetail(
    context: Context,
    detailType: SettingsDetailType,
    onCloseRequested: () -> Unit,
) {
    OverlayModeController.openMainActivity(
        context = context,
        openSettings = true,
        settingsDetailType = detailType,
    )
    onCloseRequested()
}
