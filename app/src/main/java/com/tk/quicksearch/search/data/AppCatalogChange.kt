package com.tk.quicksearch.search.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import com.tk.quicksearch.search.utils.UserHandleUtils
import com.tk.quicksearch.search.models.AppInfo

internal data class AppCatalogChange(
    val packageName: String?,
    val userHandleId: Int?,
    val isRemoval: Boolean,
    val isInstallation: Boolean = false,
    val isAvailabilityChange: Boolean = false,
) {
    val requiresCatalogReconciliation: Boolean
        get() = isRemoval || isInstallation || isAvailabilityChange

    fun appKey(currentUserHandleId: Int): String? {
        val packageName = packageName?.takeIf { it.isNotBlank() } ?: return null
        return if (userHandleId == null || userHandleId == currentUserHandleId) {
            packageName
        } else {
            "$packageName:$userHandleId"
        }
    }

    fun matches(
        app: AppInfo,
        currentUserHandleId: Int,
    ): Boolean = appKey(currentUserHandleId) == app.launchCountKey()

    companion object {
        fun fromIntent(
            intent: Intent?,
            currentUserHandleId: Int,
        ): AppCatalogChange =
            fromPackageEvent(
                action = intent?.action,
                packageName = intent?.data?.schemeSpecificPart,
                userHandleId =
                    intent
                        ?.getIntExtra(EXTRA_USER_HANDLE, currentUserHandleId)
                        ?.takeIf { it >= 0 },
                replacing = intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true,
            )

        internal fun fromPackageEvent(
            action: String?,
            packageName: String?,
            userHandleId: Int?,
            replacing: Boolean,
        ): AppCatalogChange =
            AppCatalogChange(
                packageName = packageName,
                userHandleId = userHandleId,
                isRemoval = action == Intent.ACTION_PACKAGE_REMOVED && !replacing,
                isInstallation = action == Intent.ACTION_PACKAGE_ADDED && !replacing,
                isAvailabilityChange = action == Intent.ACTION_PACKAGE_CHANGED,
            )

        fun forPackage(
            packageName: String,
            userHandle: android.os.UserHandle,
            isRemoval: Boolean,
            isInstallation: Boolean = false,
            isAvailabilityChange: Boolean = false,
        ): AppCatalogChange =
            AppCatalogChange(
                packageName = packageName,
                userHandleId = UserHandleUtils.getIdentifier(userHandle),
                isRemoval = isRemoval,
                isInstallation = isInstallation,
                isAvailabilityChange = isAvailabilityChange,
            )

        private const val EXTRA_USER_HANDLE = "android.intent.extra.user_handle"
    }
}

internal fun filterRemovedApps(
    apps: List<AppInfo>,
    removedAppKeys: Set<String>,
): List<AppInfo> =
    if (removedAppKeys.isEmpty()) {
        apps
    } else {
        apps.filterNot { removedAppKeys.contains(it.launchCountKey()) }
    }

internal fun applyCatalogRemoval(
    apps: List<AppInfo>,
    change: AppCatalogChange,
    currentUserHandleId: Int,
): List<AppInfo> =
    if (!change.isRemoval) {
        apps
    } else {
        apps.filterNot { change.matches(it, currentUserHandleId) }
    }

internal fun filterAvailableStartupApps(
    context: Context,
    apps: List<AppInfo>,
): List<AppInfo> {
    if (apps.isEmpty()) return apps

    val availableApps = apps.filter { app -> isAppAvailable(context, app) }
    if (availableApps.size != apps.size) {
        val availableKeys = availableApps.mapTo(mutableSetOf()) { it.launchCountKey() }
        val unavailableApps = apps.filterNot { availableKeys.contains(it.launchCountKey()) }
        AppCache(context.applicationContext).recordUnavailableApps(unavailableApps)
    }
    return availableApps
}

private fun isAppAvailable(
    context: Context,
    app: AppInfo,
): Boolean {
    val userHandleId = app.userHandleId
    if (userHandleId != null) {
        val userHandle = UserHandleUtils.of(userHandleId) ?: return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
        return runCatching {
            launcherApps.getActivityList(app.packageName, userHandle).isNotEmpty()
        }.getOrDefault(false)
    }

    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                app.packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(app.packageName, 0)
        }
    }.getOrNull()?.let { applicationInfo ->
        applicationInfo.enabled && (applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED) != 0
    } == true
}
