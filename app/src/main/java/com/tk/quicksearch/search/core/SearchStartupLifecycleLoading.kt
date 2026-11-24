package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.apps.AppSuggestionTabType
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.userAppPreferences.StartupPreferencesFacade
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.apps.AppIconRequest
import com.tk.quicksearch.search.apps.prefetchAppIconRequests
import com.tk.quicksearch.search.apps.setAppIconCornerPercent
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.getAppGridColumns
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.PinnedShortcutRequests
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.search.apps.invalidateAppIcon
import android.os.SystemClock

private const val PACKAGE_REFRESH_DEBOUNCE_MS = 400L
private const val STARTUP_ICON_PREFETCH_PARALLELISM = 4

internal fun SearchStartupLifecycleDelegate.startPackageChangeMonitoring() {
        repository.startPackageChangeMonitoring { change, isRepeat ->
            if (!isRepeat) {
                change.packageName?.let { packageName ->
                    if (change.isRemoval) {
                        if (appShortcutSearchHandler.removeUnavailablePackage(packageName)) {
                            refreshAppShortcutsState()
                        }
                    } else {
                        appShortcutSearchHandler.markPackageAvailable(packageName)
                        invalidateAppIcon(packageName)
                    }
                }

                if (change.isRemoval) {
                    // Package callbacks arrive on the main thread; removing the app refreshes all
                    // derived state, so do it on the catalog's own single-thread dispatcher.
                    scope.launch(startupDispatcher) { appSearchManager.removeUnavailableApp(change) }
                }
            }

            packageRefreshJob?.cancel()
            packageRefreshJob =
                scope.launch(startupDispatcher) {
                    // Bulk changes (e.g. a mode suspending many apps) arrive as a burst of callbacks.
                    // Debounce so only the final refresh reads the settled package state.
                    delay(PACKAGE_REFRESH_DEBOUNCE_MS)
                    appSearchManager.refreshAppsNow()
                    loadAppShortcuts()
                }
        }
    }

internal fun SearchStartupLifecycleDelegate.refreshAppUsageMetadata() {
        if (!permissionStateProvider().hasUsagePermission) {
            refreshAppSuggestions()
            return
        }

        appUsageRefreshJob?.cancel()
        appUsageRefreshJob =
            scope.launch(startupDispatcher) {
                appSearchManager.refreshUsageMetadataNow()
            }
    }

internal fun SearchStartupLifecycleDelegate.shouldReconcileAppsAtStartup(): Boolean {
        if (appSearchManager.cachedApps.isEmpty()) return true
        if (repository.isAppCatalogInvalidated()) return true
        val ageMs = System.currentTimeMillis() - repository.cacheLastUpdatedMillis()
        return ageMs !in 0 until APP_RECONCILIATION_FRESHNESS_MS
    }

internal suspend fun SearchStartupLifecycleDelegate.loadCacheAndMinimalPrefs() {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        // Optional speed-up: a failure here must not fail phase 1, which publishes suggestions anyway.
        val snapshotSuggestions =
            scope.async(Dispatchers.IO) { runCatching { prepareSnapshotSuggestions() }.getOrNull() }
        val startupConfig = userPreferences.loadStartupConfig()
        setPrefCache(
            SearchPreferenceCache.from(
                config = startupConfig,
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            ),
        )
        applyPreferenceCacheToLegacyVars()
        val startupSnapshot = readStartupPreferencesSnapshot()

        val cachedAppsList =
            runCatching {
                repository.loadCachedApps(
                    includeNonLaunchableApps = userPreferences.shouldIncludeNonLaunchableAppsInSearch(),
                    includeArchivedApps = userPreferences.shouldIncludeArchivedAppsInSearch(),
                )
            }.getOrNull()
        val hasUsagePermission = repository.hasUsageAccess()
        val hasContactPermission = hasContactPermission()
        val hasFilePermission = hasFilePermission()
        val hasCalendarPermission = hasCalendarPermission()
        val hasCallPermission = PermissionHelper.checkCallPermission(applicationProvider())
        val hasWallpaperPermission = WallpaperUtils.hasWallpaperAccessPermission(applicationProvider())
        val disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds()

        withContext(Dispatchers.Main) {
            updateConfigState {
                it.copy(
                    oneHandedMode = startupSnapshot.oneHandedMode,
                    bottomSearchBarEnabled = startupSnapshot.bottomSearchBarEnabled,
                    unifiedPinnedItemsEnabled = startupSnapshot.unifiedPinnedItemsEnabled,
                    topResultIndicatorEnabled = startupSnapshot.topResultIndicatorEnabled,
                    openTopResultUsingKeyboardEnabled = startupSnapshot.openTopResultUsingKeyboardEnabled,
                    accentColorMode = startupSnapshot.accentColorMode,
                    customAccentColorArgb = startupSnapshot.customAccentColorArgb,
                    openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                    clearQueryOnLaunch = startupSnapshot.clearQueryOnLaunch,
                    autoCloseOverlay = startupSnapshot.autoCloseOverlay,
                    showWallpaperBackground =
                        startupSnapshot.backgroundSource != BackgroundSource.THEME,
                    wallpaperBackgroundAlpha = startupSnapshot.wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = startupSnapshot.wallpaperBlurRadius,
                    appTheme = startupSnapshot.appTheme,
                    overlayThemeIntensity = startupSnapshot.overlayThemeIntensity,
                    useSystemFont = startupSnapshot.useSystemFont,
                    backgroundSource = startupSnapshot.backgroundSource,
                    customImageUri = startupSnapshot.customImageUri,
                    appIconShape = startupSnapshot.appIconShape,
                    launcherAppIcon = startupSnapshot.launcherAppIcon,
                    themedIconsEnabled = startupSnapshot.themedIconsEnabled,
                    deviceThemeEnabled = startupSnapshot.deviceThemeEnabled,
                    amoledThemeEnabled = startupSnapshot.amoledThemeEnabled,
                    searchChromeBlurEnabled = startupSnapshot.searchChromeBlurEnabled,
                    maskUnsupportedIconPackIcons = startupSnapshot.maskUnsupportedIconPackIcons,
                    isInitializing = true,
                )
            }
            updatePermissionState {
                it.copy(
                    hasUsagePermission = hasUsagePermission,
                    hasContactPermission = hasContactPermission,
                    hasFilePermission = hasFilePermission,
                    hasCalendarPermission = hasCalendarPermission,
                    hasCallPermission = hasCallPermission,
                    hasWallpaperPermission = hasWallpaperPermission,
                )
            }
            updateFeatureState { it.copy(disabledAppShortcutIds = disabledAppShortcutIds) }

            if (!cachedAppsList.isNullOrEmpty()) {
                initializeWithCacheMinimal(cachedAppsList)
                StartupTrace.mark("QS.Home.CachedAppsAvailable")
                // Text typed (or shared in) before the catalog loaded was searched against no apps.
                rerunCurrentQuery()
            }
        }
        markPermissionSnapshotRefreshed()

        setStartupConfig(startupConfig)
        applyLauncherIconSelection()

        if (!cachedAppsList.isNullOrEmpty()) {
            val searchableAppsWarmupJob =
                scope.launch(Dispatchers.Default) {
                    warmSearchableAppsSnapshot(cachedAppsList)
                }
            // Shown when phase 2 starts: composing the grid while the keyboard slides in (an
            // animation the app's main thread drives) measurably delayed the keyboard.
            pendingSnapshotSuggestions = snapshotSuggestions.await()
            if (userPreferences.areAppSuggestionsEnabled()) {
                // Icons for the fresh order the later usage refresh settles on (mostly cache hits).
                val visibleApps =
                    selectSuggestedApps(cachedAppsList, getGridItemCount(), hasUsagePermission)
                prefetchAppIconRequests(
                    context = applicationProvider(),
                    requests = visibleApps.map { AppIconRequest(it.packageName, it.userHandleId) },
                    iconPackPackage = userPreferences.getSelectedIconPackPackage(),
                    forceCircularMask = startupSnapshot.appIconShape == AppIconShape.CIRCLE,
                    parallelism = STARTUP_ICON_PREFETCH_PARALLELISM,
                )
            }
            searchableAppsWarmupJob.join()
        } else {
            snapshotSuggestions.cancel()
        }
        AppSearchPerformanceLogger.logTiming(
            event = "startupCacheAndMinimalPrefsReady",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 250L,
        ) {
            "cachedApps=${cachedAppsList?.size ?: 0} usagePermission=$hasUsagePermission"
        }
    }

internal class SnapshotSuggestions(val recents: List<AppInfo>, val pinned: List<AppInfo>)

/**
 * Last session's suggestions (the startup snapshot), checked for uninstalled apps and with their
 * icons decoded into memory, or null when the visible tab doesn't show them. Runs alongside the
 * rest of phase 1 so the grid can be shown, without placeholder icons, as soon as phase 1 is done.
 */
private suspend fun SearchStartupLifecycleDelegate.prepareSnapshotSuggestions(): SnapshotSuggestions? {
    val config = configStateProvider()
    val showsRecentsTab =
        config.selectedAppSuggestionTab == AppSuggestionTabType.RECENTS &&
            AppSuggestionTabType.RECENTS in config.enabledAppSuggestionTabs
    if (!config.appSuggestionsEnabled || !showsRecentsTab) return null
    if (resultsStateProvider().appsSectionState is AppsSectionVisibility.ShowingResults) return null
    val home = startupSnapshotProvider()?.withAvailableStartupApps(applicationProvider())?.homeSurface ?: return null
    val recents = home.recentApps.takeIf { it.isNotEmpty() } ?: return null
    val pinnedKeys = userPreferences.getPinnedPackages()
    val gridItemCount =
        SearchScreenConstants.ROW_COUNT *
            getAppGridColumns(applicationProvider(), userPreferences.getPhoneAppGridColumns())
    prefetchAppIconRequests(
        context = applicationProvider(),
        requests = recents.take(gridItemCount).map { AppIconRequest(it.packageName, it.userHandleId) },
        iconPackPackage = userPreferences.getSelectedIconPackPackage(),
        forceCircularMask = config.appIconShape == AppIconShape.CIRCLE,
        parallelism = STARTUP_ICON_PREFETCH_PARALLELISM,
    )
    return SnapshotSuggestions(recents, home.pinnedApps.filter { it.launchCountKey() in pinnedKeys })
}

/**
 * Shows the prepared snapshot suggestions instead of holding the grid until fresh usage data is in;
 * the later refresh merges the fresh order in and only reorders when a newer launch changed it.
 */
internal fun SearchStartupLifecycleDelegate.publishPendingSnapshotSuggestions() {
    val suggestions = pendingSnapshotSuggestions ?: return
    pendingSnapshotSuggestions = null
    updateResultsState { state ->
        if (state.query.isNotBlank() || state.appsSectionState is AppsSectionVisibility.ShowingResults) {
            state
        } else {
            state.copy(
                recentApps = suggestions.recents,
                pinnedApps = suggestions.pinned,
                screenState = ScreenVisibilityState.Content,
                appsSectionState = AppsSectionVisibility.ShowingResults(hasPinned = suggestions.pinned.isNotEmpty()),
            )
        }
    }
    StartupTrace.mark("QS.Home.SnapshotSuggestionsPublished")
}

internal suspend fun SearchStartupLifecycleDelegate.loadRemainingStartupPreferences(applyStartupPreferences: (StartupPreferencesFacade.StartupPreferences) -> Unit) {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        val startupPrefs =
            getStartupConfig()?.startupPreferences
                ?: userPreferences.getStartupPreferences()

        withContext(Dispatchers.Main) {
            applyStartupPreferences(startupPrefs)
        }

        val lastUpdated =
            getStartupConfig()?.cachedAppsLastUpdate
                ?: repository.cacheLastUpdatedMillis()
        withContext(Dispatchers.Default) { refreshDerivedState(lastUpdated, false) }
        withContext(Dispatchers.Main) { updateConfigState { it.copy(isInitializing = false) } }
        AppSearchPerformanceLogger.logTiming(
            event = "startupRemainingPreferencesReady",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 250L,
        )
    }

internal fun SearchStartupLifecycleDelegate.applyStartupPreferences(prefs: StartupPreferencesFacade.StartupPreferences) {
        // Also runs after a settings import, so restored icon corners apply without a restart.
        setAppIconCornerPercent(prefs.appIconCornerPercent)
        setPrefCache(
            SearchPreferenceCache.from(
                prefs = prefs,
                assistantLaunchVoiceModeEnabled = userPreferences.isAssistantLaunchVoiceModeEnabled(),
            ),
        )
        applyPreferenceCacheToLegacyVars()
        val snapshot = readLoadedPreferencesSnapshot()

        updateConfigState {
            it.copy(
                enabledFileTypes = snapshot.enabledFileTypes,
                oneHandedMode = snapshot.oneHandedMode,
                bottomSearchBarEnabled = snapshot.bottomSearchBarEnabled,
                unifiedPinnedItemsEnabled = snapshot.unifiedPinnedItemsEnabled,
                searchHintsEnabled = snapshot.searchHintsEnabled,
                cardLayoutEnabled = snapshot.cardLayoutEnabled,
                settingsIconEnabled = snapshot.settingsIconEnabled,
                topResultIndicatorEnabled = snapshot.topResultIndicatorEnabled,
                openTopResultUsingKeyboardEnabled = snapshot.openTopResultUsingKeyboardEnabled,
                accentColorMode = prefs.accentColorMode,
                customAccentColorArgb = prefs.customAccentColorArgb,
                openKeyboardOnLaunch = snapshot.openKeyboardOnLaunch,
                clearQueryOnLaunch = snapshot.clearQueryOnLaunch,
                autoCloseOverlay = snapshot.autoCloseOverlay,
                overlayModeEnabled = snapshot.overlayModeEnabled,
                appSuggestionsEnabled = snapshot.appSuggestionsEnabled,
                showAllAppsButton = snapshot.showAllAppsButton,
                includeNonLaunchableAppsInSearch = snapshot.includeNonLaunchableAppsInSearch,
                selectedAppSuggestionTab = snapshot.selectedAppSuggestionTab,
                enabledAppSuggestionTabs = snapshot.enabledAppSuggestionTabs,
                showAppLabels = snapshot.showAppLabels,
                phoneAppGridColumns = snapshot.phoneAppGridColumns,
                appIconSizeStep = snapshot.appIconSizeStep,
                appIconShape = snapshot.appIconShape,
                launcherAppIcon = snapshot.launcherAppIcon,
                themedIconsEnabled = snapshot.themedIconsEnabled,
                deviceThemeEnabled = snapshot.deviceThemeEnabled,
                amoledThemeEnabled = snapshot.amoledThemeEnabled,
                searchChromeBlurEnabled = snapshot.searchChromeBlurEnabled,
                maskUnsupportedIconPackIcons = snapshot.maskUnsupportedIconPackIcons,
                showWallpaperBackground = snapshot.backgroundSource != BackgroundSource.THEME,
                wallpaperBackgroundAlpha = snapshot.wallpaperBackgroundAlpha,
                wallpaperBlurRadius = snapshot.wallpaperBlurRadius,
                appTheme = snapshot.appTheme,
                overlayThemeIntensity = snapshot.overlayThemeIntensity,
                fontScaleMultiplier = snapshot.fontScaleMultiplier,
                useSystemFont = snapshot.useSystemFont,
                backgroundSource = snapshot.backgroundSource,
                customImageUri = snapshot.customImageUri,
                showFolders = snapshot.showFolders,
                booleanSettings = prefs.booleanSettings,
                showSystemFiles = snapshot.showSystemFiles,
                folderWhitelistPatterns = snapshot.folderWhitelistPatterns,
                folderBlacklistPatterns = snapshot.folderBlacklistPatterns,
                excludedFileExtensions = snapshot.excludedFileExtensions,
                hasSeenOverlayAssistantTip = userPreferences.hasSeenOverlayAssistantTip(),
            )
        }
        updateFeatureState {
            it.copy(
                amazonDomain = snapshot.amazonDomain,
                directDialEnabled = snapshot.directDialEnabled,
                numberSearchEnabled = userPreferences.isNumberSearchEnabled(),
                assistantLaunchVoiceModeEnabled = snapshot.assistantLaunchVoiceModeEnabled,
                disabledAppShortcutIds = userPreferences.getDisabledAppShortcutIds(),
                recentQueriesEnabled = prefs.searchHistoryEnabled,
                recentQueriesDisplayCount = userPreferences.getRecentQueriesDisplayCount(),
                appResultRowCount = userPreferences.getAppResultRowCount(),
                fuzzySearchEnabled =
                    !com.tk.quicksearch.shared.util.isLowRamDevice(applicationProvider()) &&
                        userPreferences.isFuzzySearchEnabled(),
                fuzzySearchAvailable =
                    !com.tk.quicksearch.shared.util.isLowRamDevice(applicationProvider()),
                secondaryRankingSignal = userPreferences.getSecondaryRankingSignal(),
                webSuggestionsCount = userPreferences.getWebSuggestionsCount(),
                topMatchesEnabled = userPreferences.isTopMatchesEnabled(),
                topMatchesLimit = userPreferences.getTopMatchesLimit(),
                topMatchesSectionOrder = userPreferences.getTopMatchesSectionOrder(),
                disabledTopMatchesSections = userPreferences.getDisabledTopMatchesSections(),
                homePinnedSectionOrder = userPreferences.getHomePinnedSectionOrder(),
                pinnedAppShortcutsInAppGrid = userPreferences.isPinnedAppShortcutsInAppGridEnabled(),
                showStatusBar = userPreferences.isStatusBarVisible(),
                pinnedAppGridOrder = userPreferences.getPinnedAppGridOrder(),
                appFolders = userPreferences.getAppFolders(),
            )
        }

        if (!prefs.searchHistoryEnabled) {
            userPreferences.clearRecentQueries()
        }

        applyLauncherIconSelection()
        refreshRecentItems()
        saveStartupSurfaceSnapshotAsync(false, false)
    }
