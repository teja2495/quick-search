package com.tk.quicksearch.search.data

import android.app.usage.UsageStats
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.res.Configuration
import android.content.res.Resources
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.tk.quicksearch.search.utils.UserHandleUtils
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class TodayAppUsage(
    val openedCount: Int,
    val foregroundTimeMillis: Long,
)

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
    private val currentUserHandleId = UserHandleUtils.getIdentifier(Process.myUserHandle())
    @Volatile private var appCatalogInvalidated = false
    private var launcherAppsCallback: LauncherApps.Callback? = null
    private var packageChangeReceiver: BroadcastReceiver? = null

    // ==================== Public API ====================

    fun hasUsageAccess(): Boolean = PermissionUtils.hasUsageStatsPermission(context)

    /** Returns today's launches and foreground time for an app, or null without Usage Access. */
    fun getTodayAppUsage(packageName: String): TodayAppUsage? {
        if (!hasUsageAccess()) return null
        val manager = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val startOfToday =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        return runCatching {
            val foregroundTime =
                manager
                    .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfToday, now)
                    .firstOrNull { it.packageName == packageName }
                    ?.totalTimeInForeground
                    ?: 0L
            val events = manager.queryEvents(startOfToday, now)
            val event = UsageEvents.Event()
            var openedCount = 0
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (
                    event.packageName == packageName &&
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    openedCount++
                }
            }
            TodayAppUsage(openedCount, foregroundTime)
        }.getOrNull()
    }

    /**
     * Loads app list from cache if available.
     * This is synchronous for instant loading during ViewModel initialization.
     *
     * @return Cached list of apps, or null if no cache exists
     */
    fun loadCachedApps(includeNonLaunchableApps: Boolean = false): List<AppInfo>? =
        appCache.loadCachedApps()?.filter { includeNonLaunchableApps || it.hasLaunchIntent }

    fun cacheLastUpdatedMillis(): Long = appCache.getLastUpdateTime()

    internal fun currentUserHandleId(): Int = currentUserHandleId

    fun clearCache() {
        appCache.clearCache()
        appCatalogInvalidated = true
    }

    fun isAppCatalogInvalidated(): Boolean =
        appCatalogInvalidated || appCache.isCatalogInvalidated()

    internal fun startPackageChangeMonitoring(onCatalogInvalidated: (AppCatalogChange) -> Unit) {
        if (packageChangeReceiver != null || launcherAppsCallback != null) return
        fun invalidate(change: AppCatalogChange) {
            if (!change.requiresCatalogReconciliation) return
            appCatalogInvalidated = true
            appCache.recordCatalogChange(change, currentUserHandleId)
            onCatalogInvalidated(change)
        }

        // LauncherApps callbacks are not available on every device/profile combination. A
        // dynamically registered package receiver is the primary trigger so the in-memory app
        // list refreshes even when this app is not the default launcher.
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    invalidate(AppCatalogChange.fromIntent(intent, currentUserHandleId))
                }
            }
        val packageFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                packageFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.onSuccess {
            packageChangeReceiver = receiver
        }

        val service = launcherApps ?: return
        val callback =
            object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) =
                    invalidate(
                        AppCatalogChange.forPackage(
                            packageName,
                            user,
                            isRemoval = false,
                            isInstallation = true,
                        ),
                    )

                override fun onPackageRemoved(packageName: String, user: UserHandle) =
                    invalidate(AppCatalogChange.forPackage(packageName, user, isRemoval = true))

                override fun onPackageChanged(packageName: String, user: UserHandle) =
                    invalidate(
                        AppCatalogChange.forPackage(
                            packageName,
                            user,
                            isRemoval = false,
                            isAvailabilityChange = true,
                        ),
                    )

                override fun onPackagesAvailable(
                    packageNames: Array<out String>,
                    user: UserHandle,
                    replacing: Boolean,
                ) {
                    packageNames.forEach { packageName ->
                        invalidate(
                            AppCatalogChange.forPackage(
                                packageName,
                                user,
                                isRemoval = false,
                                isAvailabilityChange = true,
                            ),
                        )
                    }
                }

                override fun onPackagesUnavailable(
                    packageNames: Array<out String>,
                    user: UserHandle,
                    replacing: Boolean,
                ) {
                    packageNames.forEach { packageName ->
                        invalidate(AppCatalogChange.forPackage(packageName, user, isRemoval = true))
                    }
                }
            }
        launcherAppsCallback = callback
        runCatching { service.registerCallback(callback, Handler(Looper.getMainLooper())) }
            .onFailure { launcherAppsCallback = null }
    }

    fun stopPackageChangeMonitoring() {
        packageChangeReceiver?.let { receiver ->
            packageChangeReceiver = null
            runCatching { context.unregisterReceiver(receiver) }
        }
        launcherAppsCallback?.let { callback ->
            launcherAppsCallback = null
            runCatching { launcherApps?.unregisterCallback(callback) }
        }
    }

    /**
     * Reads launchable apps on the device alongside their last used timestamp.
     * When requested, it also includes installed packages that do not expose a launcher activity.
     * Results are sorted by last used time (most recent first), then alphabetically by name.
     * Also saves the result to cache for instant loading next time.
     *
     * @param includeNonLaunchableApps Whether to include packages without a launch activity
     * @param launchCounts Map of package name to local launch count
     * @return Apps sorted by usage and name
     */
    suspend fun loadLaunchableApps(
        includeNonLaunchableApps: Boolean = false,
        launchCounts: Map<String, Int> = emptyMap(),
    ): List<AppInfo> {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        val usageMap = queryUsageStatsMap()
        val usageLoadedAtElapsedMs = SystemClock.elapsedRealtime()
        val profileApps = queryLaunchableAppsFromAllProfiles()
        val launcherAppsQueriedAtElapsedMs = SystemClock.elapsedRealtime()
        val launchableApps =
            if (profileApps.isNotEmpty()) {
                profileApps
                    .distinctBy { "${it.applicationInfo.packageName}_${UserHandleUtils.getIdentifier(it.user)}" }
                    .map { createAppInfo(it, usageMap, launchCounts) }
            } else {
                queryLaunchableAppsLegacy()
                    .distinctBy { it.activityInfo.packageName }
                    .map { createAppInfo(it, usageMap, launchCounts) }
            }
        val appInfoCreatedAtElapsedMs = SystemClock.elapsedRealtime()
        val nonLaunchableApps =
            if (includeNonLaunchableApps) {
                val launchablePackageNames = launchableApps.map { it.packageName }.toSet()
                queryInstalledApplications()
                    .asSequence()
                    .filter { it.packageName !in launchablePackageNames }
                    .map { createNonLaunchableAppInfo(it, usageMap, launchCounts) }
                    .toList()
            } else {
                emptyList()
            }
        val nonLaunchableAppsLoadedAtElapsedMs = SystemClock.elapsedRealtime()
        val apps = launchableApps + nonLaunchableApps

        // LauncherApps can briefly return a removed package immediately after an uninstall.
        // Keep removal tombstones authoritative for this reconciliation so the stale entry is
        // never written back into the cache or returned to the UI.
        val removedAppKeys = appCache.removedAppKeys()
        val sortedApps = filterRemovedApps(apps.sortedWith(AppInfoComparator), removedAppKeys)
        if (appCache.loadCachedApps() != sortedApps || removedAppKeys.isNotEmpty()) {
            appCache.saveApps(sortedApps)
        }
        AppSearchPerformanceLogger.logTiming(
            event = "catalogRepositoryLoad",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 500L,
        ) {
            "usageStatsMs=${usageLoadedAtElapsedMs - startedAtElapsedMs} " +
                "launcherQueryMs=${launcherAppsQueriedAtElapsedMs - usageLoadedAtElapsedMs} " +
                "appInfoMs=${appInfoCreatedAtElapsedMs - launcherAppsQueriedAtElapsedMs} " +
                "nonLaunchableMs=${nonLaunchableAppsLoadedAtElapsedMs - appInfoCreatedAtElapsedMs} " +
                "cacheAndSortMs=${SystemClock.elapsedRealtime() - nonLaunchableAppsLoadedAtElapsedMs} " +
                "source=${if (profileApps.isNotEmpty()) "launcherApps" else "legacyIntent"} " +
                "launchable=${launchableApps.size} nonLaunchable=${nonLaunchableApps.size} " +
                "removed=${removedAppKeys.size}"
        }
        appCatalogInvalidated = false
        appCache.clearCatalogInvalidation()
        return sortedApps
    }

    /**
     * Extracts the most recently opened apps from a list, sorted by last used timestamp.
     *
     * @param apps List of apps to extract from
     * @param limit Maximum number of apps to return
     * @return List of apps sorted by last used time (descending)
     */
    /**
     * Returns all recently opened apps sorted by last used timestamp.
     */
    fun getRecentlyOpenedApps(apps: List<AppInfo>): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        return apps.sortedByDescending { it.lastUsedTime }
    }

    fun refreshUsageMetadata(
        apps: List<AppInfo>,
        launchCounts: Map<String, Int>,
    ): List<AppInfo> {
        if (apps.isEmpty()) return apps

        val usageMap = queryUsageStatsMap()
        if (usageMap.isEmpty()) return apps

        val refreshedApps =
            apps.map { app ->
                val stats = usageMap[app.packageName]
                app.copy(
                    lastUsedTime = stats?.lastTimeUsed ?: 0L,
                    totalTimeInForeground = stats?.totalTimeInForeground ?: 0L,
                    launchCount =
                        resolveLaunchCount(
                            usageStats = stats,
                            localLaunchCount = launchCounts[app.launchCountKey()] ?: app.launchCount,
                        ),
                )
            }
        val removedAppKeys = appCache.removedAppKeys()
        val availableApps = filterRemovedApps(refreshedApps, removedAppKeys)
        if (availableApps != apps || removedAppKeys.isNotEmpty()) {
            appCache.saveApps(availableApps, catalogReconciled = false)
        }
        return availableApps
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

    private fun queryInstalledApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                ApplicationInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(PackageManager.MATCH_ALL)
        }

    fun getCurrentPackageName(): String = context.packageName

    fun getDefaultLauncherPackageName(): String? {
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
        val launchCount = resolveLaunchCount(stats, launchCounts[launchCountKey] ?: 0)
        val firstInstallTime = info.firstInstallTime
        val lastUpdateTime = getLastUpdateTime(packageName)
        val appInfo = info.applicationInfo
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val activityInfo =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getActivityInfo(
                        info.componentName,
                        PackageManager.ComponentInfoFlags.of(0),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getActivityInfo(info.componentName, 0)
                }
            }.getOrNull()

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = totalTimeInForeground,
            launchCount = launchCount,
            firstInstallTime = firstInstallTime,
            isSystemApp = isSystemApp,
            searchAliases =
                resolveSearchAliases(
                    displayLabel = label,
                    packageName = packageName,
                    labelResourceIds = listOf(activityInfo?.labelRes ?: 0, appInfo.labelRes),
                    nonLocalizedLabels =
                        listOf(
                            activityInfo?.nonLocalizedLabel,
                            appInfo.nonLocalizedLabel,
                        ),
                ),
            hasLaunchIntent = true,
            userHandleId = userHandleId,
            componentName = info.componentName.flattenToString(),
            lastUpdateTime = lastUpdateTime,
        )
    }

    private fun createAppInfo(
        resolveInfo: ResolveInfo,
        usageMap: Map<String, UsageStats>,
        launchCounts: Map<String, Int>,
    ): AppInfo {
        val packageName = resolveInfo.activityInfo.packageName
        val launchCount = resolveLaunchCount(usageMap[packageName], launchCounts[packageName] ?: 0)
        val label = extractAppLabel(resolveInfo, packageName)
        val stats = usageMap[packageName]
        val lastUsedTime = stats?.lastTimeUsed ?: 0L
        val totalTimeInForeground = stats?.totalTimeInForeground ?: 0L
        val firstInstallTime = getFirstInstallTime(packageName)
        val lastUpdateTime = getLastUpdateTime(packageName)
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
            searchAliases =
                resolveSearchAliases(
                    displayLabel = label,
                    packageName = packageName,
                    labelResourceIds =
                        listOf(
                            resolveInfo.activityInfo.labelRes,
                            resolveInfo.activityInfo.applicationInfo.labelRes,
                        ),
                    nonLocalizedLabels =
                        listOf(
                            resolveInfo.activityInfo.nonLocalizedLabel,
                            resolveInfo.activityInfo.applicationInfo.nonLocalizedLabel,
                        ),
                ),
            hasLaunchIntent = true,
            userHandleId = null,
            componentName = "${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}",
            lastUpdateTime = lastUpdateTime,
        )
    }

    private fun createNonLaunchableAppInfo(
        applicationInfo: ApplicationInfo,
        usageMap: Map<String, UsageStats>,
        launchCounts: Map<String, Int>,
    ): AppInfo {
        val packageName = applicationInfo.packageName
        val label =
            runCatching { packageManager.getApplicationLabel(applicationInfo)?.toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: applicationInfo.nonLocalizedLabel
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: formatPackageNameAsLabel(packageName)
        val stats = usageMap[packageName]
        val lastUsedTime = stats?.lastTimeUsed ?: 0L
        val totalTimeInForeground = stats?.totalTimeInForeground ?: 0L
        val launchCount = resolveLaunchCount(stats, launchCounts[packageName] ?: 0)
        val firstInstallTime = getFirstInstallTime(packageName)
        val lastUpdateTime = getLastUpdateTime(packageName)
        val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        return AppInfo(
            appName = label,
            packageName = packageName,
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = totalTimeInForeground,
            launchCount = launchCount,
            firstInstallTime = firstInstallTime,
            isSystemApp = isSystemApp,
            searchAliases =
                resolveSearchAliases(
                    displayLabel = label,
                    packageName = packageName,
                    labelResourceIds = listOf(applicationInfo.labelRes),
                    nonLocalizedLabels = listOf(applicationInfo.nonLocalizedLabel),
                ),
            hasLaunchIntent = false,
            userHandleId = null,
            componentName = null,
            lastUpdateTime = lastUpdateTime,
        )
    }

    private fun resolveLaunchCount(
        usageStats: UsageStats?,
        localLaunchCount: Int,
    ): Int {
        if (usageStats == null) return localLaunchCount
        val usageLaunchCount =
            runCatching {
                (AppLaunchCountGetter?.invoke(usageStats) as? Int) ?: 0
            }.getOrDefault(0)
        return if (usageLaunchCount > 0) usageLaunchCount else localLaunchCount
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

    /**
     * Resolves a bounded set of alternate package-provided labels while rebuilding the catalog.
     * This keeps PackageManager and Resources work off the query path.
     */
    private fun resolveSearchAliases(
        displayLabel: String,
        packageName: String,
        labelResourceIds: List<Int>,
        nonLocalizedLabels: List<CharSequence?>,
    ): List<String> {
        val displayKey = SearchTextNormalizer.normalizeForSearch(displayLabel)
        return buildList {
            // The visible label already comes from the current package resources. On an
            // English device, resolving the same resources again cannot add a useful alias.
            // Skipping it also avoids expensive package-resource work during catalog startup.
            if (!isCurrentLocaleEnglish()) {
                labelResourceIds
                    .asSequence()
                    .filter { it != 0 }
                    .distinct()
                    .mapNotNull { resolveEnglishLabel(packageName, it) }
                    .forEach(::add)
            }
            nonLocalizedLabels.forEach { label -> label?.toString()?.let(::add) }
        }
            .asSequence()
            .map(String::trim)
            .filter { it.isNotBlank() }
            .distinctBy(SearchTextNormalizer::normalizeForSearch)
            .filter { SearchTextNormalizer.normalizeForSearch(it) != displayKey }
            .take(MAX_SEARCH_ALIASES)
            .toList()
    }

    private fun isCurrentLocaleEnglish(): Boolean =
        context.resources.configuration.locales[0]?.language == Locale.ENGLISH.language

    private fun resolveEnglishLabel(
        packageName: String,
        labelResourceId: Int,
    ): String? =
        runCatching {
            val packageResources = packageManager.getResourcesForApplication(packageName)
            val englishConfiguration =
                Configuration(packageResources.configuration).apply {
                    setLocale(Locale.ENGLISH)
                }
            // Do not create a package Context here. Android parses an app's locale-config while
            // doing so, and malformed third-party manifests can make every label lookup slow.
            @Suppress("DEPRECATION")
            Resources(
                packageResources.assets,
                packageResources.displayMetrics,
                englishConfiguration,
            )
                .getText(labelResourceId)
                .toString()
                .takeIf { it.isNotBlank() }
        }.getOrNull()

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

    private fun getLastUpdateTime(packageName: String): Long =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager
                    .getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0),
                    ).lastUpdateTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            }
        }.getOrDefault(getFirstInstallTime(packageName))

    companion object {
        private const val MAX_SEARCH_ALIASES = 2

        private val AppLaunchCountGetter by lazy(LazyThreadSafetyMode.PUBLICATION) {
            runCatching { UsageStats::class.java.getMethod("getAppLaunchCount") }.getOrNull()
        }

        /**
         * Comparator for sorting apps by launch count (descending), then by name (ascending).
         */
        private val AppInfoComparator =
            compareByDescending<AppInfo> { it.launchCount }
                .thenBy { it.appName.lowercase(Locale.getDefault()) }
    }
}
