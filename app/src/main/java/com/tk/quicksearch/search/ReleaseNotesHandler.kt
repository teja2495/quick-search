package com.tk.quicksearch.search

import android.app.Application
import android.os.Build
import com.tk.quicksearch.data.UserAppPreferences
import kotlinx.coroutines.flow.update

class ReleaseNotesHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ( (SearchUiState) -> SearchUiState ) -> Unit
) {

    private fun getCurrentVersionName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, 0).versionName
            }
        } catch (e: Exception) {
            null
        }
    }

    fun checkForReleaseNotes() {
        val currentVersion = getCurrentVersionName() ?: return
        // Skip showing release notes on a fresh install; just record baseline
        if (userPreferences.isFirstLaunch()) {
            userPreferences.setLastSeenVersionName(currentVersion)
            return
        }

        val lastSeenVersion = userPreferences.getLastSeenVersionName()
        if (lastSeenVersion == null) {
            userPreferences.setLastSeenVersionName(currentVersion)
            return
        }

        if (lastSeenVersion != currentVersion) {
            uiStateUpdater {
                it.copy(
                    showReleaseNotesDialog = true,
                    releaseNotesVersionName = currentVersion
                )
            }
        }
    }

    fun acknowledgeReleaseNotes(currentReleaseNotesVersionName: String?) {
        val versionToStore = currentReleaseNotesVersionName ?: getCurrentVersionName()
        if (versionToStore != null) {
            userPreferences.setLastSeenVersionName(versionToStore)
        }
        uiStateUpdater {
            it.copy(
                showReleaseNotesDialog = false,
                releaseNotesVersionName = versionToStore
            )
        }
    }
}
