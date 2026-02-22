package com.tk.quicksearch.search.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import android.os.UserManager
import com.tk.quicksearch.search.common.UserHandleUtils
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.PermissionUtils
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Central data source that surfaces launchable apps together with their latest usage metadata.
 *
 * Responsibilities:
 * - Loading launchable apps from the device
 * - Querying usage statistics for apps
 * - Managing app cache for faster startup
 * - Checking usage access permissions
 */
class AppsRepository(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val launcherApps: LauncherApps? =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager: UserManager? =
        context.getSystemService(Context.USER_SERVICE) as? UserManager
    private val appCache = AppCache(context)

    // ==================== Public API ====================

    fun hasUsageAccess(): Boolean = PermissionUtils.hasUsageStatsPermission(context)

    /**
     * Loads app list from cache if available.
     * This is synchronous for instant loading during ViewModel initialization.
     *
     * @return Cached list of apps, or null if no cache exists
     */
    fun loadCachedApps(): List<AppInfo>? = appCache.loadCachedApps()

    fun cacheLastUpdatedMillis(): Long = appCache.getLastUpdateTime()

    fun clearCache() {
        appCache.clearCache()
    }

    /**
     * Reads all launchable apps on the device alongside their last used timestamp.
     * Results are sorted by last used time (most recent first), then alphabetically by name.
     * Also saves the result to cache for instant loading next time.
     *
     * @param launchCounts Map of package name to local launch count
     * @return List of launchable apps sorted by usage and name
     */
    suspend fun loadLaunchableApps(launchCounts: Map<String, Int> = emptyMap()): List<AppInfo> {
        val usageMap = queryUsageStatsMap()
        val currentPackageName = context.packageName
        val defaultLauncherPackageName = getDefaultLauncherPackageName()

        val activityInfos = queryLaunchableAppsFromAllProfiles()
        val apps =
            if (activityInfos.isNotEmpty()) {
                activityInfos
                    .distinctBy { "${it.applicationInfo.packageName}_${UserHandleUtils.getIdentifier(it.user)}" }
                    .filter {
                        it.applicationInfo.packageName != currentPackageName &&
                            it.applicationInfo.packageName != defaultLauncherPackageName
                    }
                    .map { createAppInfo(it, usageMap, launchCounts) }
            } else {
                val resolveInfos = queryLaunchableAppsLegacy()
                resolveInfos
                    .distinctBy { it.activityInfo.packageName }
                    .filter {
                        val pkg = it.activityInfo.packageName
                        pkg != currentPackageName && pkg != defaultLauncherPackageName
                    }
                    .map { createAppInfo(it, usageMap, launchCounts) }
            }

        appCache.saveApps(apps.sortedWith(AppInfoComparator))
        return apps.sortedWith(AppInfoComparator)
    }

    /**
     * Extracts the most recently opened apps from a list, sorted by last used timestamp.
     *
     * @param apps List of apps to extract from
     * @param limit Maximum number of apps to return
     * @return List of apps sorted by last used time (descending)
     */
    fun extractRecentlyOpenedApps(
        apps: List<AppInfo>,
        limit: Int,
    ): List<AppInfo> {
        if (apps.isEmpty() || limit <= 0) return emptyList()
        return apps.sortedByDescending { it.lastUsedTime }.take(limit)
    }

    /**
     * Returns all recently opened apps sorted by last used timestamp.
     */
    fun getRecentlyOpenedApps(apps: List<AppInfo>): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.sortedByDescending { it.lastUsedTime }
    }

    /**
     * Returns apps installed within the provided time window, sorted by install time (newest first).
     */
    fun extractRecentlyInstalledApps(
        apps: List<AppInfo>,
        windowStartMillis: Long,
        windowEndMillis: Long,
    ): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps
            .filter { it.firstInstallTime in windowStartMillis until windowEndMillis }
            .sortedByDescending { it.firstInstallTime }
    }

    // ==================== Private Helpers ====================

    private fun queryLaunchableAppsFromAllProfiles(): List<LauncherActivityInfo> {
        val launcherApps = this.launcherApps ?: return emptyList()
        val userManager = this.userManager ?: return emptyList()

        val profiles = runCatching { userManager.userProfiles }.getOrNull() ?: return emptyList()

        return profiles.flatMap { userHandle ->
            runCatching {
                launcherApps.getActivityList(null, userHandle)
            }.getOrNull().orEmpty()
        }
    }

    private fun queryLaunchableAppsLegacy(): List<ResolveInfo> {
        val launcherIntent =
            Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }
    }

    private fun getDefaultLauncherPackageName(): String? {
        val homeIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

        val resolveInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

        val packageName = resolveInfo?.activityInfo?.packageName
        return packageName?.takeIf { it.isNotBlank() && it != "android" }
    }

    private fun createAppInfo(
        info: LauncherActivityInfo,
        usageMap: Map<String, UsageStats>,
        launchCounts: Map<String, Int>,
    ): AppInfo {
        val packageName = info.applicationInfo.packageName
        val userHandleId =
            runCatching {
                val id = UserHandleUtils.getIdentifier(info.user)
                if (id == UserHandleUtils.getIdentifier(Process.myUserHandle())) null else id
            }.getOrNull()
        val launchCountKey = if (userHandleId == null) packageName else "$packageName:$userHandleId"
        val label =
            runCatching { info.label?.toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: info.applicationInfo.nonLocalizedLabel
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: formatPackageNameAsLabel(packageName)
        val stats = usageMap[packageName]
        val lastUsedTime = stats?.lastTimeUsed ?: 0L
        val totalTimeInForeground = stats?.totalTimeInForeground ?: 0L
        val launchCount = launchCounts[launchCountKey] ?: 0
        val firstInstallTime = info.firstInstallTime
        val appInfo = info.applicationInfo
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = totalTimeInForeground,
            launchCount = launchCount,
            firstInstallTime = firstInstallTime,
            isSystemApp = isSystemApp,
            userHandleId = userHandleId,
            componentName = info.componentName.flattenToString(),
        )
    }

    private fun createAppInfo(
        resolveInfo: ResolveInfo,
        usageMap: Map<String, UsageStats>,
        launchCounts: Map<String, Int>,
    ): AppInfo {
        val packageName = resolveInfo.activityInfo.packageName
        val launchCount = launchCounts[packageName] ?: 0
        val label = extractAppLabel(resolveInfo, packageName)
        val stats = usageMap[packageName]
        val lastUsedTime = stats?.lastTimeUsed ?: 0L
        val totalTimeInForeground = stats?.totalTimeInForeground ?: 0L
        val firstInstallTime = getFirstInstallTime(packageName)
        val isSystemApp =
            (
                resolveInfo.activityInfo.applicationInfo.flags
                    and ApplicationInfo.FLAG_SYSTEM
            ) != 0

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = totalTimeInForeground,
            launchCount = launchCount,
            firstInstallTime = firstInstallTime,
            isSystemApp = isSystemApp,
            userHandleId = null,
            componentName = "${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}",
        )
    }

    private fun extractAppLabel(
        resolveInfo: ResolveInfo,
        packageName: String,
    ): String =
        runCatching { resolveInfo.loadLabel(packageManager)?.toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: resolveInfo.activityInfo.nonLocalizedLabel
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: resolveInfo.activityInfo.applicationInfo.nonLocalizedLabel
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: formatPackageNameAsLabel(packageName)

    private fun formatPackageNameAsLabel(packageName: String): String =
        packageName
            .substringAfterLast(".")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    private fun queryUsageStatsMap(): Map<String, UsageStats> {
        val manager = usageStatsManager ?: return emptyMap()

        return runCatching {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(30)

            manager.queryAndAggregateUsageStats(
                startTime,
                endTime,
            )
        }.getOrDefault(emptyMap())
    }

    private fun getFirstInstallTime(packageName: String): Long =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager
                    .getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0),
                    ).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }
        }.getOrDefault(0L)

    companion object {
        /**
         * Comparator for sorting apps by launch count (descending), then by name (ascending).
         */
        private val AppInfoComparator =
            compareByDescending<AppInfo> { it.launchCount }
                .thenBy { it.appName.lowercase(Locale.getDefault()) }
    }
}
