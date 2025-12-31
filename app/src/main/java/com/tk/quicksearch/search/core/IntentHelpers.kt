package com.tk.quicksearch.search.core

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.searchengines.buildSearchUrl

/**
 * Helper functions for creating and launching intents.
 */
object IntentHelpers {

    /**
     * Creates an intent with package URI and NEW_TASK flag.
     */
    private fun createPackageIntent(action: String, packageName: String): Intent {
        return Intent(action).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Opens usage access settings for the app.
     */
    fun openUsageAccessSettings(context: Application) {
        val intent = createPackageIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }

    /**
     * Opens app settings for the app.
     */
    fun openAppSettings(context: Application) {
        val intent = createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }

    /**
     * Opens app info settings for a specific package.
     */
    fun openAppInfo(context: Application, packageName: String) {
        val intent = createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageName)
        context.startActivity(intent)
    }

    /**
     * Opens all files access settings with fallback.
     */
    fun openAllFilesAccessSettings(context: Application) {
        val manageIntent = createPackageIntent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            context.packageName
        )
        runCatching {
            context.startActivity(manageIntent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Launches an app by package name.
     */
    fun launchApp(context: Application, appInfo: AppInfo) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)

        if (launchIntent == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_launch_app, appInfo.appName),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(launchIntent)
    }

    /**
     * Requests uninstall for an app.
     */
    fun requestUninstall(context: Application, appInfo: AppInfo) {
        val packageName = appInfo.packageName
        if (packageName == context.packageName) {
            Toast.makeText(
                context,
                context.getString(R.string.error_uninstall_self),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val intent = createPackageIntent(Intent.ACTION_DELETE, packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_uninstall_app, appInfo.appName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Opens a search URL with the specified search engine.
     */
    fun openSearchUrl(context: Application, query: String, searchEngine: SearchEngine, amazonDomain: String? = null) {
        val searchUrl = buildSearchUrl(query, searchEngine, amazonDomain)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }



    /**
     * Opens a file with appropriate app.
     */
    fun openFile(context: Application, deviceFile: DeviceFile) {
        val mimeType = deviceFile.mimeType ?: "*/*"

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(deviceFile.uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(viewIntent)
        } catch (exception: ActivityNotFoundException) {
            showFileOpenError(context, deviceFile.displayName)
        } catch (exception: SecurityException) {
            showFileOpenError(context, deviceFile.displayName)
        }
    }

    private fun showFileOpenError(context: Application, fileName: String) {
        Toast.makeText(
            context,
            context.getString(R.string.error_open_file, fileName),
            Toast.LENGTH_SHORT
        ).show()
    }

}
