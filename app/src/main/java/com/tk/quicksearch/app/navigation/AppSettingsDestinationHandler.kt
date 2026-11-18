package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsAction
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

private const val QUICK_SEARCH_DEVELOPMENT_URL = "https://github.com/teja2495/quick-search"

internal data class AppSettingsDestinationHandlers(
    val onOpenSettingsDetail: (SettingsDetailType) -> Unit,
    val onReloadApps: () -> Unit,
    val onReloadContacts: () -> Unit,
    val onReloadFiles: () -> Unit,
    val onSendFeedback: () -> Unit,
    val onRateQuickSearch: () -> Unit,
    val onOpenDevelopmentPage: () -> Unit,
    val onSetDefaultAssistant: () -> Unit,
    val onSetDefaultLauncher: () -> Unit,
    val onAddHomeScreenWidget: () -> Unit,
    val onAddQuickSettingsTile: () -> Unit,
)

internal fun handleAppSettingsDestination(
    destination: AppSettingsTarget.Host,
    handlers: AppSettingsDestinationHandlers,
) {
    when (destination) {
        is AppSettingsTarget.Page -> handlers.onOpenSettingsDetail(destination.detailType)
        is AppSettingsAction -> destination.perform(handlers)
    }
}

internal fun launchRateQuickSearch(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            },
        )
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ),
            )
        }
    }
}

internal fun launchDevelopmentPage(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QUICK_SEARCH_DEVELOPMENT_URL)))
    }
}

internal fun openDefaultAssistantSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
    } catch (_: Exception) {
        openSystemSettingsFallback(context)
    }
}

internal fun openDefaultLauncherSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    } catch (_: Exception) {
        openSystemSettingsFallback(context)
    }
}

private fun openSystemSettingsFallback(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    } catch (_: Exception) {
        AppMessages.show(context, context.getString(R.string.settings_unable_to_open_settings))
    }
}
