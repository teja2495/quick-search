package com.tk.quicksearch.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.AppInfo
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
class AppUsageRepository(
    private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val appCache = AppCache(context)

    // ==================== Public API ====================

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false

        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )

        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.PACKAGE_USAGE_STATS
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> false
        }
    }

    /**
     * Loads app list from cache if available.
     * This is synchronous for instant loading during ViewModel initialization.
     *
     * @return Cached list of apps, or null if no cache exists
     */
    fun loadCachedApps(): List<AppInfo>? {
        return appCache.loadCachedApps()
    }

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
        val resolveInfos = queryLaunchableApps()
        val usageMap = queryUsageStatsMap()
        val currentPackageName = context.packageName

        val apps = resolveInfos
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != currentPackageName }
            .map { resolveInfo ->
                createAppInfo(resolveInfo, usageMap, launchCounts)
            }
            .sortedWith(AppInfoComparator)

        appCache.saveApps(apps)
        return apps
    }

    /**
     * Extracts the most recently opened apps from a list, sorted by last used timestamp.
     *
     * @param apps List of apps to extract from
     * @param limit Maximum number of apps to return
     * @return List of apps sorted by last used time (descending)
     */
    fun extractRecentlyOpenedApps(apps: List<AppInfo>, limit: Int): List<AppInfo> {
        if (apps.isEmpty() || limit <= 0) return emptyList()
        return apps.sortedByDescending { it.lastUsedTime }.take(limit)
    }

    // ==================== Private Helpers ====================

    private fun queryLaunchableApps(): List<ResolveInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }
    }

    private fun createAppInfo(
        resolveInfo: ResolveInfo,
        usageMap: Map<String, UsageStats>,
        launchCounts: Map<String, Int>
    ): AppInfo {
        val packageName = resolveInfo.activityInfo.packageName
        val label = extractAppLabel(resolveInfo, packageName)
        val stats = usageMap[packageName]
        val lastUsedTime = stats?.lastTimeUsed ?: 0L
        val totalTimeInForeground = stats?.totalTimeInForeground ?: 0L
        val launchCount = launchCounts[packageName] ?: 0
        val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags 
            and ApplicationInfo.FLAG_SYSTEM) != 0

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = totalTimeInForeground,
            launchCount = launchCount,
            isSystemApp = isSystemApp
        )
    }

    /**
     * Extracts the display label for an app, falling back to a formatted package name if needed.
     */
    private fun extractAppLabel(resolveInfo: ResolveInfo, packageName: String): String {
        return resolveInfo.loadLabel(packageManager)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: formatPackageNameAsLabel(packageName)
    }

    private fun formatPackageNameAsLabel(packageName: String): String {
        return packageName
            .substringAfterLast(".")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun queryUsageStatsMap(): Map<String, UsageStats> {
        val manager = usageStatsManager ?: return emptyMap()

        return runCatching {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(30)

            manager.queryAndAggregateUsageStats(
                startTime,
                endTime
            )
        }.getOrDefault(emptyMap())
    }

    companion object {
        /**
         * Comparator for sorting apps by launch count (descending), then by name (ascending).
         */
        private val AppInfoComparator = compareByDescending<AppInfo> { it.launchCount }
            .thenBy { it.appName.lowercase(Locale.getDefault()) }
    }
}

