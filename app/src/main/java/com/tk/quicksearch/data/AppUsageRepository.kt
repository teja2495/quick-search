package com.tk.quicksearch.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
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

    /**
     * Checks if the app has been granted usage access permission.
     * 
     * @return true if usage access is granted, false otherwise
     */
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
     * 
     * @return Cached list of apps, or null if no cache exists
     */
    suspend fun loadCachedApps(): List<AppInfo>? {
        return appCache.loadCachedApps()
    }

    /**
     * Returns the timestamp when the cache was last updated.
     * 
     * @return Timestamp in milliseconds, or 0L if cache has never been updated
     */
    fun cacheLastUpdatedMillis(): Long = appCache.getLastUpdateTime()

    /**
     * Clears all cached app data.
     */
    fun clearCache() {
        appCache.clearCache()
    }

    /**
     * Reads all launchable apps on the device alongside their last used timestamp.
     * Results are sorted by last used time (most recent first), then alphabetically by name.
     * Also saves the result to cache for instant loading next time.
     * 
     * @return List of launchable apps sorted by usage and name
     */
    suspend fun loadLaunchableApps(): List<AppInfo> {
        val resolveInfos = queryLaunchableApps()
        val usageMap = queryLastUsedMap()
        val currentPackageName = context.packageName

        val apps = resolveInfos
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != currentPackageName }
            .map { resolveInfo ->
                createAppInfo(resolveInfo, usageMap)
            }
            .sortedWith(AppInfoComparator)

        appCache.saveApps(apps)
        return apps
    }

    /**
     * Extracts the most recently used apps from a list, sorted by usage time.
     * 
     * @param apps List of apps to extract from (may be unsorted)
     * @param limit Maximum number of apps to return
     * @return List of most recently used apps, up to the specified limit
     */
    fun extractRecentApps(apps: List<AppInfo>, limit: Int): List<AppInfo> {
        if (apps.isEmpty() || limit <= 0) return emptyList()
        return apps.sortedWith(AppInfoComparator).take(limit)
    }

    // ==================== Private Helpers ====================

    /**
     * Queries the package manager for all launchable apps.
     */
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

    /**
     * Creates an AppInfo object from a ResolveInfo and usage map.
     */
    private fun createAppInfo(
        resolveInfo: ResolveInfo,
        usageMap: Map<String, Long>
    ): AppInfo {
        val packageName = resolveInfo.activityInfo.packageName
        val label = extractAppLabel(resolveInfo, packageName)
        val lastUsedTime = usageMap[packageName] ?: 0L
        val isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags 
            and ApplicationInfo.FLAG_SYSTEM) != 0

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
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

    /**
     * Formats a package name into a readable label by extracting the last component
     * and capitalizing the first letter.
     */
    private fun formatPackageNameAsLabel(packageName: String): String {
        return packageName
            .substringAfterLast(".")
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    /**
     * Queries usage statistics for the last 7 days and returns a map of package names to last used times.
     * 
     * @return Map of package names to last used timestamps, or empty map if unavailable
     */
    private fun queryLastUsedMap(): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()

        return runCatching {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(7)

            manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )?.associate { stats ->
                stats.packageName to stats.lastTimeUsed
            }.orEmpty()
        }.getOrDefault(emptyMap())
    }

    companion object {
        /**
         * Comparator for sorting apps by last used time (descending), then by name (ascending).
         */
        private val AppInfoComparator = compareByDescending<AppInfo> { it.lastUsedTime }
            .thenBy { it.appName.lowercase(Locale.getDefault()) }
    }
}

