package com.tk.quicksearch.search.apps

import android.content.Context
import android.os.SystemClock
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppCatalogChange
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.applyCatalogRemoval
import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AppSearchManager(
    private val context: Context,
    private val repository: AppsRepository,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onAppsUpdated: () -> Unit,
    private val onLoadingStateChanged: (Boolean, String?) -> Unit,
    private val showToastCallback: (Int) -> Unit,
    private val isLowRamDevice: Boolean = false,
    initialFuzzyConfig: FuzzySearchConfig = FuzzySearchConfig.DEFAULT_APP_CONFIG,
) {
    private data class AppSearchLabels(
        val displayName: String,
        val aliases: List<String>,
    )

    private val currentPackageName = repository.getCurrentPackageName()
    private val defaultLauncherPackageName by lazy { repository.getDefaultLauncherPackageName() }

    /**
     * Incremented whenever the catalog is replaced. Derived snapshots built from [cachedApps]
     * compare against it so they cannot keep serving apps the catalog has already dropped.
     */
    @Volatile
    var catalogVersion: Long = 0L
        private set

    var cachedApps: List<AppInfo> = emptyList()
        private set(value) {
            field = value
            catalogVersion++
        }

    private var noMatchPrefix: String? = null
    private val searchTextCache = SearchTextCache()
    private val searchMatcher = CachedSearchMatcher(searchTextCache)
    @Volatile private var preparedAppSearchData: Map<String, PreparedAppSearchData> = emptyMap()

    private var fuzzySearchStrategy =
        FuzzyAppSearchStrategy(
            config = initialFuzzyConfig,
            isLowRamDevice = isLowRamDevice,
            isFuzzySearchEnabled = userPreferences::isFuzzySearchEnabled,
            textCache = searchTextCache,
        )

    fun initCache(initialApps: List<AppInfo>) {
        cachedApps = initialApps
        // A query typed before the catalog loaded matched nothing; that miss says nothing about it.
        noMatchPrefix = null
        preparedAppSearchData = emptyMap()
        scope.launch(Dispatchers.Default) {
            rebuildPreparedAppSearchData(initialApps)
        }
    }

    fun loadApps() {
        scope.launch(Dispatchers.IO) {
            refreshAppsNow()
        }
    }

    fun refreshApps(
        showToast: Boolean = false,
        forceUiUpdate: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            refreshAppsNow(showToast, forceUiUpdate)
        }
    }

    /**
     * Structured variant used by startup so app reconciliation remains owned by the startup job
     * instead of escaping into another fire-and-forget coroutine.
     */
    suspend fun refreshAppsNow(
        showToast: Boolean = false,
        forceUiUpdate: Boolean = false,
    ) {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        if (cachedApps.isEmpty()) {
            onLoadingStateChanged(true, null)
        }

        val launchCounts = userPreferences.getAllAppLaunchCounts()
        val launchCountsLoadedAtElapsedMs = SystemClock.elapsedRealtime()
        runCatching {
            repository.loadLaunchableApps(
                includeNonLaunchableApps = userPreferences.shouldIncludeNonLaunchableAppsInSearch(),
                includeArchivedApps = userPreferences.shouldIncludeArchivedAppsInSearch(),
                launchCounts = launchCounts,
            )
        }
            .onSuccess { apps ->
                val appsLoadedAtElapsedMs = SystemClock.elapsedRealtime()
                val currentPackageSet = cachedApps.map { it.launchCountKey() }.toSet()
                val newPackageSet = apps.map { it.launchCountKey() }.toSet()
                val appSetChanged = currentPackageSet != newPackageSet
                val currentUsageMap = cachedApps.associate { it.launchCountKey() to it.launchCount }
                val newUsageMap = apps.associate { it.launchCountKey() to it.launchCount }
                val usageStatsChanged = currentUsageMap != newUsageMap
                val currentSearchLabels =
                    cachedApps.associate { app ->
                        app.launchCountKey() to AppSearchLabels(app.appName, app.searchAliases)
                    }
                val newSearchLabels =
                    apps.associate { app ->
                        app.launchCountKey() to AppSearchLabels(app.appName, app.searchAliases)
                    }
                val searchLabelsChanged = currentSearchLabels != newSearchLabels
                val suspendedChanged =
                    cachedApps.filter { it.isSuspended }.map { it.launchCountKey() }.toSet() !=
                        apps.filter { it.isSuspended }.map { it.launchCountKey() }.toSet()

                val shouldPublish =
                    showToast ||
                        cachedApps.isEmpty() ||
                        appSetChanged ||
                        usageStatsChanged ||
                        searchLabelsChanged ||
                        suspendedChanged ||
                        forceUiUpdate
                if (shouldPublish) {
                    cachedApps = apps
                    if (appSetChanged || searchLabelsChanged || preparedAppSearchData.isEmpty()) {
                        rebuildPreparedAppSearchData(apps)
                    }
                    noMatchPrefix = null
                    onAppsUpdated()
                }

                if (cachedApps.isNotEmpty()) {
                    onLoadingStateChanged(false, null)
                }

                if (showToast) {
                    withContext(Dispatchers.Main) {
                        showToastCallback(R.string.apps_refreshed_successfully)
                    }
                }

                AppSearchPerformanceLogger.logTiming(
                    event = "catalogRefresh",
                    elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
                    slowThresholdMs = 500L,
                ) {
                    "launchCountsMs=${launchCountsLoadedAtElapsedMs - startedAtElapsedMs} " +
                        "repositoryMs=${appsLoadedAtElapsedMs - launchCountsLoadedAtElapsedMs} " +
                        "publishMs=${SystemClock.elapsedRealtime() - appsLoadedAtElapsedMs} " +
                        "apps=${apps.size} cachedApps=${cachedApps.size} " +
                        "includeNonLaunchable=${userPreferences.shouldIncludeNonLaunchableAppsInSearch()} " +
                        "updated=$shouldPublish"
                }
            }.onFailure { error ->
                AppSearchPerformanceLogger.log {
                    "catalogRefresh failed totalMs=${SystemClock.elapsedRealtime() - startedAtElapsedMs} " +
                        "error=${error.javaClass.simpleName}"
                }
                val fallbackMessage = context.getString(R.string.error_loading_user_apps)
                onLoadingStateChanged(false, error.localizedMessage ?: fallbackMessage)

                if (showToast) {
                    withContext(Dispatchers.Main) {
                        showToastCallback(R.string.failed_to_refresh_apps)
                    }
                }
            }
    }

    fun clearCachedApps() {
        scope.launch(Dispatchers.IO) {
            repository.clearCache()
            cachedApps = emptyList()
            preparedAppSearchData = emptyMap()
            searchTextCache.clear()
            noMatchPrefix = null
            // We need to notify VM to clear its state
            onLoadingStateChanged(true, null)
            onAppsUpdated() // VM will see empty cachedApps

            scope.launch(Dispatchers.Main) {
                showToastCallback(R.string.settings_cache_cleared_toast)
            }
            refreshApps()
        }
    }

    internal fun removeUnavailableApp(change: AppCatalogChange): Boolean {
        if (!change.isRemoval || cachedApps.isEmpty()) return false

        val remainingApps =
            applyCatalogRemoval(
                apps = cachedApps,
                change = change,
                currentUserHandleId = repository.currentUserHandleId(),
            )
        if (remainingApps.size == cachedApps.size) return false

        cachedApps = remainingApps
        val remainingKeys = remainingApps.mapTo(mutableSetOf()) { it.launchCountKey() }
        preparedAppSearchData = preparedAppSearchData.filterKeys(remainingKeys::contains)
        fuzzySearchStrategy.clearCaches()
        noMatchPrefix = null
        onAppsUpdated()
        return true
    }

    fun recordAppLaunch(appInfo: AppInfo) {
        val appKey = appInfo.launchCountKey()
        val updatedApps =
            applyRecordedAppLaunch(
                apps = cachedApps,
                appKey = appKey,
                launchTime = System.currentTimeMillis(),
            )
        if (updatedApps != cachedApps) {
            cachedApps = updatedApps
            noMatchPrefix = null
            // Keep the launch metadata current without refreshing the visible suggestions while
            // Android is transitioning to the launched app. The next resume/usage refresh will
            // publish the updated ordering.
        }
    }

    suspend fun refreshUsageMetadataNow() {
        if (cachedApps.isEmpty()) return

        val refreshedApps =
            repository.refreshUsageMetadata(
                apps = cachedApps,
                launchCounts = userPreferences.getAllAppLaunchCounts(),
            )
        if (refreshedApps != cachedApps) {
            cachedApps = refreshedApps
            noMatchPrefix = null
            onAppsUpdated()
        }
    }

    fun resetNoMatchPrefixIfNeeded(normalizedQuery: String) {
        val prefix = noMatchPrefix ?: return
        if (!normalizedQuery.startsWith(prefix)) {
            noMatchPrefix = null
        }
    }

    fun shouldSkipDueToNoMatchPrefix(normalizedQuery: String): Boolean {
        return shouldSkipDueToNoMatchPrefix(normalizedQuery, noMatchPrefix)
    }

    fun setNoMatchPrefix(prefix: String?) {
        noMatchPrefix = prefix
    }

    fun availableApps(
        hidden: Set<String> = userPreferences.getSuggestionHiddenPackages(),
    ): List<AppInfo> {
        if (cachedApps.isEmpty()) return emptyList()
        return cachedApps.filterNot { app ->
            hidden.contains(app.launchCountKey()) ||
                hidden.contains(app.packageName) ||
                !app.hasLaunchIntent ||
                app.isArchived ||
                app.packageName == currentPackageName ||
                app.packageName == defaultLauncherPackageName
        }
    }

    /** Quick Search itself and the default launcher are never shown as pinned home apps. */
    fun isEligibleForPinnedSuggestions(app: AppInfo): Boolean =
        app.packageName != currentPackageName && app.packageName != defaultLauncherPackageName

    fun readPinnedOrder(): Map<String, Int> =
        userPreferences.getPinnedPackageOrder().withIndex().associate { it.value to it.index }

    private var cachedAppNicknames: Map<String, String> = emptyMap()

    init {
        // Initial load of nicknames
        refreshNicknames()
    }

    fun refreshNicknames() {
        cachedAppNicknames = userPreferences.getAllAppNicknames()
        // Entries keyed by old nickname text would otherwise linger until the next catalog rebuild.
        fuzzySearchStrategy.clearCaches()
    }

    fun deriveMatches(
        query: String,
        source: List<AppInfo>,
        limit: Int,
    ): List<AppInfo> =
        AppSearchAlgorithm.findMatches(
            query = query,
            source = source,
            limit = limit,
            fuzzySearchStrategy = fuzzySearchStrategy,
            appNicknames = cachedAppNicknames,
            secondaryRankingSignal = userPreferences.getSecondaryRankingSignal(),
            matcher = searchMatcher,
            preparedAppData = preparedAppSearchDataForQuery(),
        )

    /** Query-path variant used by the debounced keystroke search. */
    fun deriveMatches(
        queryContext: SearchQueryContext,
        source: List<AppInfo>,
        limit: Int,
        ensureActive: () -> Unit = {},
    ): List<AppInfo> =
        AppSearchAlgorithm.findMatches(
            queryContext = queryContext,
            source = source,
            limit = limit,
            fuzzySearchStrategy = fuzzySearchStrategy,
            appNicknames = cachedAppNicknames,
            secondaryRankingSignal = userPreferences.getSecondaryRankingSignal(),
            matcher = searchMatcher,
            preparedAppData = preparedAppSearchDataForQuery(),
            ensureActive = ensureActive,
        )

    /**
     * Until the background rebuild after [initCache] lands, build the prepared data once here
     * instead of letting every keystroke (or derived-state refresh) recompute initials and
     * abbreviations for every app.
     */
    private fun preparedAppSearchDataForQuery(): Map<String, PreparedAppSearchData> {
        val current = preparedAppSearchData
        val apps = cachedApps
        if (current.isNotEmpty() || apps.isEmpty()) return current
        val prepared = apps.associate { app -> app.launchCountKey() to PreparedAppSearchData.from(app) }
        if (cachedApps === apps && preparedAppSearchData.isEmpty()) preparedAppSearchData = prepared
        return prepared
    }

    private fun rebuildPreparedAppSearchData(apps: List<AppInfo>) {
        searchTextCache.clear()
        fuzzySearchStrategy.clearCaches()
        apps.forEach { app ->
            searchTextCache.prepare(app.appName)
            app.searchAliases.forEach(searchTextCache::prepare)
        }
        val prepared = apps.associate { app -> app.launchCountKey() to PreparedAppSearchData.from(app) }
        val preparedLabels = apps.associate { app -> app.launchCountKey() to AppSearchLabels(app.appName, app.searchAliases) }
        val currentLabels =
            cachedApps.associate { app -> app.launchCountKey() to AppSearchLabels(app.appName, app.searchAliases) }
        if (preparedLabels == currentLabels) {
            preparedAppSearchData = prepared
        }
    }

    internal companion object {
        internal fun shouldSkipDueToNoMatchPrefix(
            normalizedQuery: String,
            noMatchPrefix: String?,
        ): Boolean {
            val prefix = noMatchPrefix ?: return false
            // Always evaluate single-character queries to avoid stale no-match prefixes
            // suppressing legitimate first-letter app searches.
            if (normalizedQuery.length <= 1) return false
            return normalizedQuery.length >= prefix.length && normalizedQuery.startsWith(prefix)
        }
    }
}

internal fun applyRecordedAppLaunch(
    apps: List<AppInfo>,
    appKey: String,
    launchTime: Long,
): List<AppInfo> =
    apps.map { app ->
        if (app.launchCountKey() == appKey) {
            app.copy(
                lastUsedTime = launchTime,
                launchCount = app.launchCount + 1,
            )
        } else {
            app
        }
    }
