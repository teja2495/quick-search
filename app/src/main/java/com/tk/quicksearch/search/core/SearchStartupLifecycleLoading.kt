package com.tk.quicksearch.search.core

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
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.PinnedShortcutRequests
import com.tk.quicksearch.app.startup.StartupTrace
import android.os.SystemClock

internal fun SearchStartupLifecycleDelegate.startPackageChangeMonitoring() {
        repository.startPackageChangeMonitoring { change ->
            change.packageName?.let { packageName ->
                if (change.isRemoval) {
                    if (appShortcutSearchHandler.removeUnavailablePackage(packageName)) {
                        refreshAppShortcutsState()
                    }
                } else {
                    appShortcutSearchHandler.markPackageAvailable(packageName)
                }
            }

            if (change.isRemoval) {
                appSearchManager.removeUnavailableApp(change)
            }

            packageRefreshJob?.cancel()
            packageRefreshJob =
                scope.launch(startupDispatcher) {
                    loadApps()
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
            if (userPreferences.areAppSuggestionsEnabled()) {
                val visibleApps =
                    selectSuggestedApps(cachedAppsList, getGridItemCount(), hasUsagePermission)
                val iconPack = userPreferences.getSelectedIconPackPackage()
                prefetchAppIcons(
                    context = applicationProvider(),
                    packageNames = visibleApps.map { it.packageName },
                    iconPackPackage = iconPack,
                    forceCircularMask = startupSnapshot.appIconShape == AppIconShape.CIRCLE,
                )
            }
            searchableAppsWarmupJob.join()
        }
        AppSearchPerformanceLogger.logTiming(
            event = "startupCacheAndMinimalPrefsReady",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 250L,
        ) {
            "cachedApps=${cachedAppsList?.size ?: 0} usagePermission=$hasUsagePermission"
        }
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
                settingsIconEnabled = snapshot.settingsIconEnabled,
                topResultIndicatorEnabled = snapshot.topResultIndicatorEnabled,
                openTopResultUsingKeyboardEnabled = snapshot.openTopResultUsingKeyboardEnabled,
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
                filePreviewsEnabled = snapshot.filePreviewsEnabled,
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
                pinnedAppGridOrder = userPreferences.getPinnedAppGridOrder(),
                appFolders = userPreferences.getAppFolders(),
                shouldShowUsagePermissionBanner = userPreferences.shouldShowUsagePermissionBanner(),
            )
        }

        if (!prefs.searchHistoryEnabled) {
            userPreferences.clearRecentQueries()
        }

        applyLauncherIconSelection()
        refreshRecentItems()
        saveStartupSurfaceSnapshotAsync(false, false)
    }
