package com.tk.quicksearch.app

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences

class ReleaseNotesHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val uiStateUpdater: ((SearchUiState) -> SearchUiState) -> Unit,
) {
    private fun getPackageInfoOrNull(): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }

    private fun getCurrentVersionName(): String? = getPackageInfoOrNull()?.versionName

    private fun getCurrentVersionCode(): Long? {
        val packageInfo = getPackageInfoOrNull() ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    fun checkForReleaseNotes() {
        val currentVersion = getCurrentVersionName() ?: return
        val currentVersionCode = getCurrentVersionCode()

        if (userPreferences.isFirstLaunch()) {
            userPreferences.setAccessibilityPermissionDisclaimerPending(false)
            userPreferences.setHasSeenAccessibilityPermissionDisclaimer(true)
            userPreferences.setLastSeenVersionName(currentVersion)
            currentVersionCode?.let { userPreferences.setLastSeenVersionCode(it) }
            return
        }

        val lastSeenVersion = userPreferences.getLastSeenVersionName()
        if (lastSeenVersion == null) {
            userPreferences.setLastSeenVersionName(currentVersion)
            currentVersionCode?.let { userPreferences.setLastSeenVersionCode(it) }
            return
        }

        val nameChanged = lastSeenVersion != currentVersion
        val lastSeenCode = userPreferences.getLastSeenVersionCode()
        val versionCodeChanged =
            currentVersionCode != null &&
                (lastSeenCode == null || lastSeenCode != currentVersionCode)

        if (!nameChanged && !versionCodeChanged) {
            if (userPreferences.isAccessibilityPermissionDisclaimerPending()) {
                showAccessibilityPermissionDisclaimer()
            }
            return
        }

        if (!userPreferences.hasSeenAccessibilityPermissionDisclaimer()) {
            userPreferences.setAccessibilityPermissionDisclaimerPending(true)
        }

        showReleaseNotes(currentVersion)
    }

    fun showReleaseNotes() {
        getCurrentVersionName()?.let(::showReleaseNotes)
    }

    private fun showReleaseNotes(versionName: String) {
        uiStateUpdater {
            it.copy(
                showReleaseNotesDialog = true,
                releaseNotesVersionName = versionName,
            )
        }
    }

    fun acknowledgeReleaseNotes(currentReleaseNotesVersionName: String?) {
        val versionToStore = currentReleaseNotesVersionName ?: getCurrentVersionName()
        if (versionToStore != null) {
            userPreferences.setLastSeenVersionName(versionToStore)
        }
        getCurrentVersionCode()?.let { userPreferences.setLastSeenVersionCode(it) }
        uiStateUpdater {
            it.copy(
                showReleaseNotesDialog = false,
                releaseNotesVersionName = versionToStore,
                showAccessibilityPermissionDisclaimer =
                    userPreferences.isAccessibilityPermissionDisclaimerPending(),
            )
        }
    }

    fun dismissAccessibilityPermissionDisclaimer() {
        userPreferences.setAccessibilityPermissionDisclaimerPending(false)
        userPreferences.setHasSeenAccessibilityPermissionDisclaimer(true)
        uiStateUpdater { it.copy(showAccessibilityPermissionDisclaimer = false) }
    }

    private fun showAccessibilityPermissionDisclaimer() {
        uiStateUpdater { it.copy(showAccessibilityPermissionDisclaimer = true) }
    }
}
