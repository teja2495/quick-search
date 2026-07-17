package com.tk.quicksearch.app

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.tk.quicksearch.search.data.UserAppPreferences

/**
 * Utility class to handle in-app updates using Google Play's AppUpdateManager API.
 */
object UpdateHelper {
    private const val TAG = "UpdateHelper"

    /**
     * Checks for available app updates and shows a flexible update prompt if available.
     */
    fun checkForUpdates(
        activity: Activity,
        userPreferences: UserAppPreferences,
        onUpdateAvailable: () -> Unit,
    ) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    userPreferences.setUpdateCheckShownThisSession()

                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        Log.d(TAG, "Downloaded update found, completing update")
                        appUpdateManager.completeUpdate()
                        return@addOnSuccessListener
                    }

                    val updateAvailable =
                        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                            appUpdateInfo.updateAvailability() ==
                                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    val isFlexibleUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                    if (updateAvailable && isFlexibleUpdateAllowed) {
                        Log.d(TAG, "Update available")
                        onUpdateAvailable()
                    } else {
                        Log.d(TAG, "No update available or flexible update not allowed")
                    }
                }.addOnFailureListener { exception ->
                    userPreferences.setUpdateCheckShownThisSession()
                    Log.w(TAG, "Failed to check for updates", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            userPreferences.setUpdateCheckShownThisSession()
        }
    }

    fun startUpdate(activity: Activity) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                appUpdateManager.startUpdateFlow(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                )
            }
        }
    }
}
