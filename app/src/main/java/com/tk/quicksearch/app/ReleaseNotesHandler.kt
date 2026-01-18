package com.tk.quicksearch.app

import android.app.Application
import android.os.Build
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.core.SearchUiState
import kotlinx.coroutines.flow.update

class ReleaseNotesHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ( (SearchUiState) -> SearchUiState ) -> Unit
) {

    private fun getCurrentVersionName(): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, 0)
            }
            packageInfo.versionName
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
        if (lastSeenVersion == null || lastSeenVersion == currentVersion) {
            if (lastSeenVersion == null) {
                userPreferences.setLastSeenVersionName(currentVersion)
            }
            return
        }

        // Reset file type preferences to defaults for existing users on version update
        // This ensures Documents, Pictures, Videos, Music, and APKs are enabled by default
        val newEnabledFileTypes = userPreferences.clearEnabledFileTypes()
        uiStateUpdater { currentState ->
            currentState.copy(enabledFileTypes = newEnabledFileTypes)
        }

        uiStateUpdater {
            it.copy(
                showReleaseNotesDialog = true,
                releaseNotesVersionName = currentVersion
            )
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
