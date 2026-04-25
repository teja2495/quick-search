package com.tk.quicksearch.search.core

import android.app.Application
import android.content.Context
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.getAppGridColumns
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchDerivedStateDelegate(
    private val scope: CoroutineScope,
    private val appContext: Context,
    private val applicationProvider: () -> Application,
    private val startupSurfaceStore: StartupSurfaceStore,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val handlersProvider: () -> SearchHandlerContainer,
    private val appSuggestionSelector: AppSuggestionSelector,
    private val instantStartupSurfaceEnabled: Boolean,
    private val cachedAllSearchableAppsProvider: () -> List<AppInfo>,
    private val setCachedAllSearchableApps: (List<AppInfo>) -> Unit,
    private val resultsStateProvider: () -> SearchResultsState,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val configStateProvider: () -> SearchUiConfigState,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
) {
    private val appSearchManager get() = handlersProvider().appSearchManager
    private val iconPackHandler get() = handlersProvider().iconPackHandler
    private val messagingHandler get() = handlersProvider().messagingHandler
    private val secondarySearchOrchestrator get() = handlersProvider().secondarySearchOrchestrator

    fun getGridItemCount(): Int =
        SearchScreenConstants.ROW_COUNT *
            getAppGridColumns(applicationProvider(), configStateProvider().phoneAppGridColumns)

    fun getSearchableAppsSnapshot(): List<AppInfo> {
        return cachedAllSearchableAppsProvider()
    }

    fun warmSearchableAppsSnapshot(apps: List<AppInfo> = appSearchManager.cachedApps) {
        setCachedAllSearchableApps(
            buildSearchableApps(
                apps = apps,
                resultHiddenPackages = userPreferences.getResultHiddenPackages(),
                pinnedPackages = userPreferences.getPinnedPackages(),
            ),
        )
    }

    fun refreshAppSuggestions(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null,
    ) {
        appSearchManager.refreshNicknames()

        val apps = appSearchManager.cachedApps
        val visibleAppList = appSearchManager.availableApps()
        val hasUsagePermission = permissionStateProvider().hasUsagePermission
        val suggestionsEnabled = configStateProvider().appSuggestionsEnabled

        val suggestionHiddenPackages = userPreferences.getSuggestionHiddenPackages()
        val resultHiddenPackages = userPreferences.getResultHiddenPackages()
        val pinnedPackages = userPreferences.getPinnedPackages()

        if (suggestionsEnabled && !hasUsagePermission && userPreferences.getRecentAppLaunches().isEmpty()) {
            val initialRecents =
                visibleAppList
                    .sortedBy { it.appName.lowercase(Locale.getDefault()) }
                    .take(getGridItemCount())
                    .map { it.launchCountKey() }
            userPreferences.setRecentAppLaunches(initialRecents)
        }

        val pinnedAppsForSuggestions = appSearchManager.computePinnedApps(emptySet())
        val pinnedAppsForResults =
            computePinnedApps(
                apps = apps,
                pinnedPackages = pinnedPackages,
                exclusion = resultHiddenPackages,
            )
        val suggestedRecents =
            if (suggestionsEnabled) {
                val recentsSource = visibleAppList.filterNot { pinnedPackages.contains(it.launchCountKey()) }
                extractSuggestedApps(
                    apps = recentsSource,
                    limit = getGridItemCount(),
                    hasUsagePermission = hasUsagePermission,
                )
            } else {
                emptyList()
            }
        val currentResultsState = resultsStateProvider()
        val recents =
            stabilizeVisibleSuggestions(
                currentSuggestions = currentResultsState.recentApps,
                refreshedSuggestions = suggestedRecents,
                visibleApps = visibleAppList,
                pinnedPackages = pinnedPackages,
                limit = getGridItemCount(),
                suggestionsEnabled = suggestionsEnabled,
            )

        val query = currentResultsState.query
        val trimmedQuery = query.trim()

        val allSearchableApps =
            buildSearchableApps(
                apps = apps,
                resultHiddenPackages = resultHiddenPackages,
                pinnedPackages = pinnedPackages,
                pinnedAppsForResults = pinnedAppsForResults,
            )
        setCachedAllSearchableApps(allSearchableApps)

        val searchResults =
            if (trimmedQuery.isBlank()) {
                emptyList()
            } else {
                appSearchManager.deriveMatches(trimmedQuery, allSearchableApps, getGridItemCount())
            }
        val suggestionHiddenAppList =
            apps.filter { suggestionHiddenPackages.contains(it.launchCountKey()) }.sortedBy {
                it.appName.lowercase(Locale.getDefault())
            }
        val resultHiddenAppList =
            apps.filter { resultHiddenPackages.contains(it.launchCountKey()) }.sortedBy {
                it.appName.lowercase(Locale.getDefault())
            }

        updateResultsState { state ->
            state.copy(
                allApps = apps,
                recentApps = recents,
                searchResults = searchResults,
                pinnedApps = pinnedAppsForSuggestions,
                suggestionExcludedApps = suggestionHiddenAppList,
                resultExcludedApps = resultHiddenAppList,
                indexedAppCount = visibleAppList.size,
                cacheLastUpdatedMillis = lastUpdated ?: state.cacheLastUpdatedMillis,
                nicknameUpdateVersion = state.nicknameUpdateVersion + 1,
            )
        }
        if (isLoading != null) {
            updateConfigState { it.copy(isLoading = isLoading) }
        }

        iconPackHandler.prefetchVisibleAppIcons(
            pinnedApps = pinnedAppsForSuggestions,
            recents = recents,
            searchResults = searchResults,
        )

        val hasStartupSuggestions = pinnedAppsForSuggestions.isNotEmpty() || recents.isNotEmpty()
        if (hasStartupSuggestions && !configStateProvider().isStartupCoreSurfaceReady) {
            updateConfigState { it.copy(isStartupCoreSurfaceReady = true) }
        }
        saveStartupSurfaceSnapshotAsync()
    }

    private fun stabilizeVisibleSuggestions(
        currentSuggestions: List<AppInfo>,
        refreshedSuggestions: List<AppInfo>,
        visibleApps: List<AppInfo>,
        pinnedPackages: Set<String>,
        limit: Int,
        suggestionsEnabled: Boolean,
    ): List<AppInfo> {
        if (!suggestionsEnabled || currentSuggestions.isEmpty()) return refreshedSuggestions

        val refreshedByKey = refreshedSuggestions.associateBy { it.launchCountKey() }
        val visibleByKey = visibleApps.associateBy { it.launchCountKey() }
        val stableKeys =
            visibleByKey.keys
                .filterNot { pinnedPackages.contains(it) }
                .toSet()

        val stableExisting =
            currentSuggestions.mapNotNull { current ->
                val key = current.launchCountKey()
                if (!stableKeys.contains(key)) return@mapNotNull null
                refreshedByKey[key] ?: visibleByKey[key]
            }

        val existingKeys = stableExisting.map { it.launchCountKey() }.toSet()
        val appendedSuggestions = refreshedSuggestions.filterNot { existingKeys.contains(it.launchCountKey()) }

        return (stableExisting + appendedSuggestions).take(limit)
    }

    fun refreshMessagingState() {
        val apps = appSearchManager.cachedApps
        val packageNames = apps.map { it.packageName }.toSet()
        val isWhatsAppInstalled = packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
        val isTelegramInstalled = packageNames.contains(PackageConstants.TELEGRAM_PACKAGE)
        val isSignalInstalled = packageNames.contains(PackageConstants.SIGNAL_PACKAGE)
        val isGoogleMeetInstalled = packageNames.contains(PackageConstants.GOOGLE_MEET_PACKAGE)
        val resolvedMessagingApp =
            messagingHandler.updateMessagingAvailability(
                whatsappInstalled = isWhatsAppInstalled,
                telegramInstalled = isTelegramInstalled,
                signalInstalled = isSignalInstalled,
                updateState = false,
            )
        val selectedCallingApp = userPreferences.getCallingApp()
        val resolvedCallingApp =
            resolveCallingApp(
                app = selectedCallingApp,
                isWhatsAppInstalled = isWhatsAppInstalled,
                isTelegramInstalled = isTelegramInstalled,
                isSignalInstalled = isSignalInstalled,
                isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        if (resolvedCallingApp != selectedCallingApp) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }
        updatePermissionState {
            it.copy(
                messagingApp = resolvedMessagingApp,
                callingApp = resolvedCallingApp,
                isWhatsAppInstalled = isWhatsAppInstalled,
                isTelegramInstalled = isTelegramInstalled,
                isSignalInstalled = isSignalInstalled,
                isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        }
    }

    fun refreshSecondarySearches() {
        val query = resultsStateProvider().query
        if (query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

    fun refreshDerivedState(
        lastUpdated: Long? = null,
        isLoading: Boolean? = null,
    ) {
        refreshAppSuggestions(lastUpdated = lastUpdated, isLoading = isLoading)
        refreshMessagingState()
        refreshSecondarySearches()
    }

    fun saveStartupSurfaceSnapshotAsync(
        forcePreviewRefresh: Boolean = false,
        allowDuringQuery: Boolean = false,
    ) {
        if (!instantStartupSurfaceEnabled) return

        scope.launch(Dispatchers.IO) {
            val config = configStateProvider()
            val results = resultsStateProvider()
            val suggestionLimit = getGridItemCount().coerceAtLeast(1)
            val startupSuggestions =
                buildList {
                    addAll(results.pinnedApps)
                    addAll(results.recentApps)
                }
                    .distinctBy { it.launchCountKey() }
                    .take(suggestionLimit)

            if (!allowDuringQuery && results.query.isNotBlank()) {
                return@launch
            }

            val previewPath =
                if (config.backgroundSource == BackgroundSource.THEME) {
                    null
                } else if (forcePreviewRefresh || config.startupBackgroundPreviewPath.isNullOrBlank()) {
                    WallpaperUtils.saveStartupBackgroundPreview(
                        context = appContext,
                        backgroundSource = config.backgroundSource,
                        customImageUri = config.customImageUri,
                    )
                } else {
                    config.startupBackgroundPreviewPath
                }

            val snapshot =
                StartupSurfaceSnapshot(
                    createdAtMillis = System.currentTimeMillis(),
                    backgroundSource = config.backgroundSource,
                    showWallpaperBackground = config.showWallpaperBackground,
                    wallpaperBackgroundAlpha = config.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = config.wallpaperBlurRadius,
                    appTheme = config.appTheme,
                    overlayThemeIntensity = config.overlayThemeIntensity,
                    customImageUri = config.customImageUri,
                    startupBackgroundPreviewPath = previewPath,
                    oneHandedMode = config.oneHandedMode,
                    bottomSearchBarEnabled = config.bottomSearchBarEnabled,
                    topResultIndicatorEnabled = config.topResultIndicatorEnabled,
                    openKeyboardOnLaunch = config.openKeyboardOnLaunch,
                    fontScaleMultiplier = config.fontScaleMultiplier,
                    showAppLabels = config.showAppLabels,
                    appSuggestionsEnabled = config.appSuggestionsEnabled,
                    phoneAppGridColumns = config.phoneAppGridColumns,
                    suggestedApps = startupSuggestions,
                )
            startupSurfaceStore.saveSnapshot(snapshot)

            if (previewPath != config.startupBackgroundPreviewPath) {
                withContext(Dispatchers.Main) {
                    updateConfigState { it.copy(startupBackgroundPreviewPath = previewPath) }
                }
            }
        }
    }

    private fun extractSuggestedApps(
        apps: List<AppInfo>,
        limit: Int,
        hasUsagePermission: Boolean,
    ): List<AppInfo> =
        appSuggestionSelector.selectSuggestedApps(
            apps = apps,
            limit = limit,
            hasUsagePermission = hasUsagePermission,
        )

    private fun buildSearchableApps(
        apps: List<AppInfo>,
        resultHiddenPackages: Set<String>,
        pinnedPackages: Set<String>,
        pinnedAppsForResults: List<AppInfo> =
            computePinnedApps(
                apps = apps,
                pinnedPackages = pinnedPackages,
                exclusion = resultHiddenPackages,
            ),
    ): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()

        val nonPinnedApps =
            apps.filterNot { app ->
                resultHiddenPackages.contains(app.launchCountKey()) ||
                    resultHiddenPackages.contains(app.packageName) ||
                    pinnedPackages.contains(app.launchCountKey())
            }
        return (pinnedAppsForResults + nonPinnedApps).distinctBy { it.launchCountKey() }
    }

    private fun computePinnedApps(
        apps: List<AppInfo>,
        pinnedPackages: Set<String>,
        exclusion: Set<String>,
    ): List<AppInfo> {
        if (apps.isEmpty() || pinnedPackages.isEmpty()) return emptyList()

        return apps
            .asSequence()
            .filter { pinnedPackages.contains(it.launchCountKey()) && !exclusion.contains(it.launchCountKey()) }
            .sortedBy { it.appName.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun resolveCallingApp(
        app: CallingApp,
        isWhatsAppInstalled: Boolean,
        isTelegramInstalled: Boolean,
        isSignalInstalled: Boolean,
        isGoogleMeetInstalled: Boolean,
    ): CallingApp =
        when (app) {
            CallingApp.WHATSAPP -> if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
            CallingApp.TELEGRAM -> if (isTelegramInstalled) CallingApp.TELEGRAM else CallingApp.CALL
            CallingApp.SIGNAL -> if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
            CallingApp.GOOGLE_MEET ->
                if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
            CallingApp.CALL -> CallingApp.CALL
        }
}
