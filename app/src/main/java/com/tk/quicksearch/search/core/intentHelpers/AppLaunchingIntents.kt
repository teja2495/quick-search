package com.tk.quicksearch.search.core

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.common.UserHandleUtils
import com.tk.quicksearch.search.models.AppInfo

/** App launching related intents. */
internal object AppLaunchingIntents {
    private const val TAG = "AppLaunchingIntents"

    /** Launches an app by package name. Uses LauncherApps for work profile apps. */
    fun launchApp(
        context: Application,
        appInfo: AppInfo,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        if (tryLaunchWithLauncherApps(context, appInfo)) {
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (launchIntent == null) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, appInfo.appName)
            return
        }
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
        )
        runCatching { context.startActivity(launchIntent) }
            .onFailure { throwable ->
                Log.w(TAG, "Failed to launch ${appInfo.packageName}", throwable)
                onShowToast?.invoke(R.string.common_error_unable_to_open, appInfo.appName)
            }
    }

    private fun tryLaunchWithLauncherApps(
        context: Application,
        appInfo: AppInfo,
    ): Boolean {
        val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
        val userManager = context.getSystemService(android.content.Context.USER_SERVICE) as? UserManager ?: return false
        val profiles = runCatching { userManager.userProfiles }.getOrNull().orEmpty()
        if (profiles.isEmpty()) return false

        val requestedUserHandleId = appInfo.userHandleId
        val requestedComponent = appInfo.componentName?.let(ComponentName::unflattenFromString)
        val targetProfiles =
            if (requestedUserHandleId != null) {
                profiles.filter { UserHandleUtils.getIdentifier(it) == requestedUserHandleId }
            } else {
                profiles
            }

        for (userHandle in targetProfiles) {
            val activityList =
                runCatching { launcherApps.getActivityList(appInfo.packageName, userHandle) }
                    .getOrNull()
                    .orEmpty()
            if (activityList.isEmpty()) continue

            val targetComponent =
                when {
                    requestedComponent != null -> {
                        activityList.firstOrNull { it.componentName == requestedComponent }?.componentName
                    }
                    else -> {
                        activityList.firstOrNull()?.componentName
                    }
                }

            if (targetComponent != null) {
                val started =
                    runCatching {
                        launcherApps.startMainActivity(targetComponent, userHandle, null, null)
                        true
                    }.getOrElse { false }
                if (started) return true
            }
        }

        return false
    }
}