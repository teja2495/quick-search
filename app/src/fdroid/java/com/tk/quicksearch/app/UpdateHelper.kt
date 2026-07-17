package com.tk.quicksearch.app

import android.app.Activity
import com.tk.quicksearch.search.data.UserAppPreferences

/** No-op for F-Droid builds (Google Play in-app updates are unavailable). */
object UpdateHelper {
    fun checkForUpdates(
        activity: Activity,
        userPreferences: UserAppPreferences,
        onUpdateAvailable: () -> Unit,
    ) = Unit

    fun startUpdate(activity: Activity) = Unit
}
