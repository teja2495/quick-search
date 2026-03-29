package com.tk.quicksearch.search.core

import android.app.Application
import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.DeviceFile

/** Helper functions for creating and launching intents. */
object IntentHelpers {
    /** Opens usage access settings for the app. */
    fun openUsageAccessSettings(context: Application) {
        AppSettingsIntents.openUsageAccessSettings(context)
    }

    /** Opens app settings for the app. */
    fun openAppSettings(context: Application) {
        AppSettingsIntents.openAppSettings(context)
    }

    /** Opens app info settings for a specific package. */
    fun openAppInfo(
        context: Application,
        packageName: String,
    ) {
        AppSettingsIntents.openAppInfo(context, packageName)
    }

    /** Opens all files access settings with fallback. */
    fun openAllFilesAccessSettings(context: Application) {
        AppSettingsIntents.openAllFilesAccessSettings(context)
    }

    /** Launches an app by package name. Uses LauncherApps for work profile apps. */
    fun launchApp(
        context: Application,
        appInfo: AppInfo,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        AppLaunchingIntents.launchApp(context, appInfo, onShowToast)
    }

    /** Requests uninstall for an app. */
    fun requestUninstall(
        context: Application,
        appInfo: AppInfo,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        AppManagementIntents.requestUninstall(context, appInfo, onShowToast)
    }

    /** Opens a search URL with the specified search engine. */
    fun openSearchUrl(
        context: Application,
        query: String,
        searchEngine: SearchEngine,
        amazonDomain: String? = null,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        SearchIntents.openSearchUrl(context, query, searchEngine, amazonDomain, onShowToast)
    }

    fun openBrowserSearch(
        context: Application,
        query: String,
        browserPackageName: String,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        SearchIntents.openBrowserSearch(context, query, browserPackageName, onShowToast)
    }

    fun openBrowserUrl(
        context: Application,
        url: String,
        browserPackageName: String,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        SearchIntents.openBrowserUrl(context, url, browserPackageName, onShowToast)
    }

    fun openCustomSearchUrl(
        context: Application,
        query: String,
        urlTemplate: String,
        browserPackage: String? = null,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        SearchIntents.openCustomSearchUrl(context, query, urlTemplate, browserPackage, onShowToast)
    }

    /** Opens the folder containing the file, or the folder itself if it is a directory. */
    fun openContainingFolder(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        FileIntents.openContainingFolder(context, deviceFile, onShowToast)
    }

    /** Opens a file with appropriate app. */
    fun openFile(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        FileIntents.openFile(context, deviceFile, onShowToast)
    }

    fun openCalendarEvent(
        context: Application,
        eventId: Long,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        if (!IntentUtils.canResolveIntent(context, intent)) {
            onShowToast?.invoke(R.string.error_open_calendar_event, null)
            return
        }
        runCatching { context.startActivity(intent) }
            .onFailure { onShowToast?.invoke(R.string.error_open_calendar_event, null) }
    }
}
