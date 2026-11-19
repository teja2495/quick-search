package com.tk.quicksearch.search.apps.privateSpace

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager

/**
 * Identifies the Android 15+ Private Space profile.
 *
 * The system only lists that profile for the default launcher holding ACCESS_HIDDEN_PROFILES.
 * Its apps must never reach the regular app catalog, persisted caches, launch counts, or search
 * history, so profile enumerations that feed those paths skip it via [isPrivateProfile].
 */
internal object PrivateSpaceProfile {
    fun isPrivateProfile(
        context: Context,
        userHandle: UserHandle,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return runCatching {
            launcherApps.getLauncherUserInfo(userHandle)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
        }.getOrDefault(false)
    }

    /** Returns the Private Space profile, or null when it does not exist or is hidden from us. */
    fun find(context: Context): UserHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val profiles = runCatching { launcherApps.profiles }.getOrNull().orEmpty()
        return profiles.firstOrNull { isPrivateProfile(context, it) }
    }
}
