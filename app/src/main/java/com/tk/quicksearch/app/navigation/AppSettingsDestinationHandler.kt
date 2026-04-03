package com.tk.quicksearch.app.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

private const val QUICK_SEARCH_DEVELOPMENT_URL = "https://github.com/teja2495/quick-search"

internal data class AppSettingsDestinationHandlers(
    val onOpenSettingsDetail: (SettingsDetailType) -> Unit,
    val onReloadApps: () -> Unit = {},
    val onReloadContacts: () -> Unit = {},
    val onReloadFiles: () -> Unit = {},
    val onSendFeedback: () -> Unit = {},
    val onRateQuickSearch: () -> Unit = {},
    val onOpenDevelopmentPage: () -> Unit = {},
    val onSetDefaultAssistant: () -> Unit = {},
    val onAddHomeScreenWidget: () -> Unit = {},
    val onAddQuickSettingsTile: () -> Unit = {},
)

internal fun handleAppSettingsDestination(
    destination: AppSettingsDestination,
    handlers: AppSettingsDestinationHandlers,
) {
    destination.toSettingsDetailTypeOrNull()?.let { detailType ->
        handlers.onOpenSettingsDetail(detailType)
        return
    }

    when (destination) {
        AppSettingsDestination.RELOAD_APPS -> handlers.onReloadApps()
        AppSettingsDestination.RELOAD_CONTACTS -> handlers.onReloadContacts()
        AppSettingsDestination.RELOAD_FILES -> handlers.onReloadFiles()
        AppSettingsDestination.SEND_FEEDBACK -> handlers.onSendFeedback()
        AppSettingsDestination.RATE_QUICK_SEARCH -> handlers.onRateQuickSearch()
        AppSettingsDestination.DEVELOPMENT -> handlers.onOpenDevelopmentPage()
        AppSettingsDestination.SET_DEFAULT_ASSISTANT -> handlers.onSetDefaultAssistant()
        AppSettingsDestination.ADD_HOME_SCREEN_WIDGET -> handlers.onAddHomeScreenWidget()
        AppSettingsDestination.ADD_QUICK_SETTINGS_TILE -> handlers.onAddQuickSettingsTile()
        else -> Unit
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
        context.startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_unable_to_open_settings),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
