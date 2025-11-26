package com.tk.quicksearch.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.tk.quicksearch.model.AppInfo
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Central data source that surfaces launchable apps together with their latest usage metadata.
 */
class AppUsageRepository(
    private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val appCache = AppCache(context)

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
            AppOpsManager.MODE_DEFAULT -> ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.PACKAGE_USAGE_STATS
            ) == PackageManager.PERMISSION_GRANTED

            else -> false
        }
    }

    /**
     * Loads app list from cache if available.
     * Returns null if no cache exists.
     */
    suspend fun loadCachedApps(): List<AppInfo>? {
        return appCache.loadCachedApps()
    }

    fun cacheLastUpdatedMillis(): Long = appCache.getLastUpdateTime()

    fun clearCache() {
        appCache.clearCache()
    }

    /**
     * Reads all launchable apps on the device alongside their last used timestamp.
     * Also saves the result to cache for instant loading next time.
     */
    suspend fun loadLaunchableApps(): List<AppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        val usageMap = lastUsedMap()

        val currentPackageName = context.packageName
        val apps = resolveInfos
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != currentPackageName }
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(Locale.getDefault()) }
                AppInfo(
                    appName = label,
                    packageName = packageName,
                    lastUsedTime = usageMap[packageName] ?: 0L,
                    isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedWith(
                compareByDescending<AppInfo> { it.lastUsedTime }.thenBy { it.appName.lowercase(Locale.getDefault()) }
            )
        
        // Save to cache for next startup
        appCache.saveApps(apps)
        
        return apps
    }

    fun extractRecentApps(apps: List<AppInfo>, limit: Int): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.sortedWith(
            compareByDescending<AppInfo> { it.lastUsedTime }
                .thenBy { it.appName.lowercase(Locale.getDefault()) }
        ).take(limit)
    }

    private fun lastUsedMap(): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()
        return runCatching {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(7)
            manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )?.associate { stats -> stats.packageName to stats.lastTimeUsed }.orEmpty()
        }.getOrDefault(emptyMap())
    }
}

