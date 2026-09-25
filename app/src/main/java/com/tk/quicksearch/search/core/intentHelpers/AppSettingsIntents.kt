package com.tk.quicksearch.search.core.intentHelpers

import android.app.Application
import android.content.Intent
import android.provider.Settings

/** App settings related intents. */
internal object AppSettingsIntents {
    /** Opens usage access settings for the app. */
    fun openUsageAccessSettings(context: Application) {
        val intent = IntentUtils.createPackageIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }

    /** Opens app settings for the app. */
    fun openAppSettings(context: Application) {
        val intent =
            IntentUtils.createPackageIntent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                context.packageName,
            )
        context.startActivity(intent)
    }

    /** Opens app info settings for a specific package. */
    fun openAppInfo(
        context: Application,
        packageName: String,
    ) {
        val intent = IntentUtils.createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageName)
        context.startActivity(intent)
    }

    /** Opens all files access settings with fallback. */
    fun openAllFilesAccessSettings(context: Application) {
        val manageIntent =
            IntentUtils.createPackageIntent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                context.packageName,
            )
        runCatching { context.startActivity(manageIntent) }.onFailure {
            val fallback =
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(fallback)
        }
    }
}