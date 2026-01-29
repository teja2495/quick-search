package com.tk.quicksearch.app

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.tk.quicksearch.search.data.UserAppPreferences

/**
 * Utility class to handle in-app updates using Google Play's AppUpdateManager API.
 */
object UpdateHelper {
    private const val TAG = "UpdateHelper"

    /**
     * Checks for available app updates and shows a flexible update prompt if available.
     * This will not block the review prompt from showing.
     *
     * @param activity The activity context to show the update flow
     * @param userPreferences User preferences to track update check session
     */
    fun checkForUpdates(
        activity: Activity,
        userPreferences: UserAppPreferences,
    ) {
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    userPreferences.setUpdateCheckShownThisSession()

                    val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    val isFlexibleUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                    if (updateAvailable && isFlexibleUpdateAllowed) {
                        Log.d(TAG, "Update available, showing flexible update prompt")

                        appUpdateManager.startUpdateFlow(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    } else {
                        Log.d(TAG, "No update available or flexible update not allowed")
                    }
                }.addOnFailureListener { exception ->
                    // If update check fails, still mark session to avoid blocking review prompt
                    userPreferences.setUpdateCheckShownThisSession()
                    Log.w(TAG, "Failed to check for updates", exception)
                }
        } catch (e: Exception) {
            // Gracefully handle any errors - update check should never crash the app
            Log.e(TAG, "Error checking for updates", e)
            userPreferences.setUpdateCheckShownThisSession()
        }
    }
}
