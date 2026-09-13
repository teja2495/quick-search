package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.filterAvailableStartupApps
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.util.isLowRamDevice
import com.tk.quicksearch.app.startup.StartupTrace

internal data class SearchViewModelInitialState(
    val instantStartupSurfaceEnabled: Boolean,
    val startupSnapshot: StartupSurfaceSnapshot?,
    val resultsState: SearchResultsState,
    val permissionState: SearchPermissionState,
    val featureState: SearchFeatureState,
    val configState: SearchUiConfigState,
)

internal object SearchViewModelInitialStateFactory {
    fun create(
        appContext: Context,
        startupPreferencesReader: UserAppPreferences,
        startupSurfaceStore: StartupSurfaceStore,
        inMemoryRetainedQuery: String,
    ): SearchViewModelInitialState {
        val instantStartupSurfaceEnabled = startupPreferencesReader.isInstantStartupSurfaceEnabled()
        val startupSnapshot =
            if (instantStartupSurfaceEnabled) {
                startupSurfaceStore.loadSnapshot()?.let { snapshot ->
                    val cachedHome = snapshot.homeSurface
                    val availableStartupApps =
                        filterAvailableStartupApps(
                            context = appContext,
                            apps =
                                (snapshot.suggestedApps + cachedHome.pinnedApps + cachedHome.recentApps)
                                    .distinctBy { it.launchCountKey() },
                        )
                    val availableAppKeys =
                        availableStartupApps.mapTo(mutableSetOf()) { it.launchCountKey() }
                    snapshot.copy(
                        suggestedApps =
                            snapshot.suggestedApps.filter { it.launchCountKey() in availableAppKeys },
                        homeSurface =
                            cachedHome.copy(
                                pinnedApps =
                                    cachedHome.pinnedApps.filter {
                                        it.launchCountKey() in availableAppKeys
                                    },
                                recentApps =
                                    cachedHome.recentApps.filter {
                                        it.launchCountKey() in availableAppKeys
                                    },
                            ),
                    )
                }
            } else {
                null
            }
        if (startupSnapshot != null) {
            StartupTrace.mark("QS.Home.StartupSnapshotAvailable")
        }

        val initialBackgroundSource = startupPreferencesReader.getBackgroundSource()
        val initialCustomImageUri = startupPreferencesReader.getCustomImageUri()
        val initialAppThemeMode = startupPreferencesReader.getAppThemeMode()
        val initialIsDarkMode =
            when (initialAppThemeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> {
                    val nightModeFlags =
                        appContext.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }

        val initialPreviewPath =
            startupSnapshot?.startupBackgroundPreviewPath?.takeIf { snapshotPath ->
                startupSnapshot.backgroundSource == initialBackgroundSource &&
                    (initialBackgroundSource != BackgroundSource.CUSTOM_IMAGE ||
                        startupSnapshot.customImageUri == initialCustomImageUri) &&
                    !snapshotPath.isNullOrBlank()
            }

        val clearQueryOnLaunch = startupPreferencesReader.isClearQueryOnLaunchEnabled()
        val hasContactPermission = PermissionUtils.hasContactsPermission(appContext)
        val hasFilePermission = PermissionUtils.hasFileAccessPermission(appContext)
        val hasCalendarPermission = PermissionUtils.hasCalendarPermission(appContext)
        val cachedHome = startupSnapshot?.homeSurface
        val pinnedAppKeys = startupPreferencesReader.getPinnedPackages()
        val pinnedContactIds = startupPreferencesReader.getPinnedContactIds()
        val pinnedFileUris = startupPreferencesReader.getPinnedFileUris()
        val pinnedSettingIds = startupPreferencesReader.getPinnedSettingIds()
        val pinnedCalendarEventIds = startupPreferencesReader.getPinnedCalendarEventIds()
        val pinnedNoteIds = startupPreferencesReader.getPinnedNoteIds()
        val pinnedAppShortcutIds = startupPreferencesReader.getPinnedAppShortcutIds()
        val currentRecentItemKeys =
            if (startupPreferencesReader.areRecentQueriesEnabled()) {
                buildSet {
                    startupPreferencesReader.getRecentItems()
                        .filterIsInstance<RecentSearchEntry.Query>()
                        .forEach { add(it.stableKey) }
                    startupPreferencesReader.getRecentResultOpens()
                        .forEach { add(it.stableKey) }
                }
            } else {
                emptySet()
            }
        val cachedRecentItems =
            if (startupPreferencesReader.areRecentQueriesEnabled()) {
                cachedHome?.recentItems.orEmpty().filter { item ->
                    item.entry.stableKey in currentRecentItemKeys &&
                        when (item) {
                            is RecentSearchItem.Contact ->
                                hasContactPermission && item.entry.contactId !in pinnedContactIds
                            is RecentSearchItem.File ->
                                hasFilePermission && item.entry.uri !in pinnedFileUris
                            is RecentSearchItem.Setting -> item.entry.id !in pinnedSettingIds
                            is RecentSearchItem.AppShortcut ->
                                item.entry.shortcutKey !in pinnedAppShortcutIds
                            is RecentSearchItem.Note -> item.entry.noteId !in pinnedNoteIds
                            else -> true
                        }
                }
            } else {
                emptyList()
            }
        val hasCachedEnabledSearchTargets =
            startupSnapshot?.let { snapshot ->
                snapshot.searchTargetsOrder.any { target ->
                    target.getId() !in snapshot.disabledSearchTargetIds
                }
            } == true

        val initialResultsState =
            SearchResultsState(
                query = if (clearQueryOnLaunch) "" else inMemoryRetainedQuery,
                recentApps = cachedHome?.recentApps.orEmpty(),
                pinnedApps =
                    cachedHome?.pinnedApps.orEmpty().filter {
                        it.launchCountKey() in pinnedAppKeys
                    },
                pinnedNonAppItemOrder = startupPreferencesReader.getPinnedNonAppItemOrder(),
                pinnedContacts =
                    if (hasContactPermission) {
                        cachedHome?.pinnedContacts.orEmpty().filter { it.contactId in pinnedContactIds }
                    } else {
                        emptyList()
                    },
                pinnedFiles =
                    if (hasFilePermission) {
                        cachedHome?.pinnedFiles.orEmpty().filter {
                            it.uri.toString() in pinnedFileUris
                        }
                    } else {
                        emptyList()
                    },
                pinnedSettings =
                    cachedHome?.pinnedSettings.orEmpty().filter { it.id in pinnedSettingIds },
                pinnedCalendarEvents =
                    if (hasCalendarPermission) {
                        cachedHome?.pinnedCalendarEvents.orEmpty().filter {
                            it.eventId in pinnedCalendarEventIds
                        }
                    } else {
                        emptyList()
                    },
                pinnedNotes =
                    cachedHome?.pinnedNotes.orEmpty().filter { it.noteId in pinnedNoteIds },
                pinnedAppShortcuts =
                    cachedHome?.pinnedAppShortcuts.orEmpty().filter {
                        "${it.packageName}:${it.id}" in pinnedAppShortcutIds
                    },
                recentItems = cachedRecentItems,
                indexedAppCount = startupSnapshot?.suggestedApps?.size ?: 0,
                searchEnginesState =
                    if (startupSnapshot?.isSearchEngineCompactMode == true && hasCachedEnabledSearchTargets) {
                        SearchEnginesVisibility.Compact
                    } else {
                        SearchEnginesVisibility.Hidden
                    },
            )

        // Seeded from preferences so fuzzy matching never runs against the enabled-by-default
        // value during the window before startup phase 2 hydrates the feature state.
        val isLowRamDevice = isLowRamDevice(appContext)

        val initialFeatureState =
            SearchFeatureState(
                searchTargetsOrder = startupSnapshot?.searchTargetsOrder.orEmpty(),
                disabledSearchTargetIds = startupSnapshot?.disabledSearchTargetIds.orEmpty(),
                isSearchEngineCompactMode =
                    startupSnapshot?.isSearchEngineCompactMode == true && hasCachedEnabledSearchTargets,
                searchEngineCompactRowCount =
                    startupSnapshot?.searchEngineCompactRowCount?.coerceIn(1, 2) ?: 1,
                fuzzySearchEnabled =
                    !isLowRamDevice && startupPreferencesReader.isFuzzySearchEnabled(),
                fuzzySearchAvailable = !isLowRamDevice,
                secondaryRankingSignal = startupPreferencesReader.getSecondaryRankingSignal(),
                isSearchEngineAliasSuffixEnabled =
                    startupPreferencesReader.isSearchEngineAliasSuffixEnabled(),
                isAliasTriggerAfterSpaceEnabled =
                    startupPreferencesReader.isAliasTriggerAfterSpaceEnabled(),
                showTodayEvents = startupPreferencesReader.getShowTodayEvents(),
                topMatchesEnabled = startupPreferencesReader.isTopMatchesEnabled(),
                topMatchesLimit = startupPreferencesReader.getTopMatchesLimit(),
                topMatchesSectionOrder = startupPreferencesReader.getTopMatchesSectionOrder(),
                disabledTopMatchesSections = startupPreferencesReader.getDisabledTopMatchesSections(),
                showRateQuickSearchCard = startupPreferencesReader.shouldShowRateQuickSearchCard(),
                recentQueriesEnabled = startupPreferencesReader.areRecentQueriesEnabled(),
                hasDismissedSearchHistoryTip =
                    startupPreferencesReader.hasDismissedSearchHistoryTip(),
            )

        val initialPermissionState =
            SearchPermissionState(
                hasContactPermission = hasContactPermission,
                hasFilePermission = hasFilePermission,
                hasCalendarPermission = hasCalendarPermission,
            )

        val initialConfigState =
            SearchUiConfigState(
                startupPhase = StartupPhase.PHASE_1_CACHE_PREFS,
                isInitializing = true,
                isLoading = true,
                isStartupCoreSurfaceReady = startupSnapshot != null,
                showWallpaperBackground =
                    startupSnapshot?.showWallpaperBackground
                        ?: initialBackgroundSource != BackgroundSource.THEME,
                wallpaperBackgroundAlpha =
                    startupSnapshot?.wallpaperBackgroundAlpha
                        ?: startupPreferencesReader.getWallpaperBackgroundAlpha(initialIsDarkMode),
                wallpaperBlurRadius =
                    startupSnapshot?.wallpaperBlurRadius
                        ?: startupPreferencesReader.getWallpaperBlurRadius(initialIsDarkMode),
                appTheme = startupSnapshot?.appTheme ?: startupPreferencesReader.getAppTheme(),
                overlayThemeIntensity =
                    (startupSnapshot?.overlayThemeIntensity
                            ?: startupPreferencesReader.getOverlayThemeIntensity())
                        .coerceIn(
                            UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                            UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
                        ),
                appThemeMode = initialAppThemeMode,
                backgroundSource = initialBackgroundSource,
                customImageUri = initialCustomImageUri,
                startupBackgroundPreviewPath = initialPreviewPath,
                selectedIconPackPackage = startupPreferencesReader.getSelectedIconPackPackage(),
                oneHandedMode =
                    startupSnapshot?.oneHandedMode ?: startupPreferencesReader.isOneHandedMode(),
                bottomSearchBarEnabled = startupPreferencesReader.isBottomSearchBarEnabled(),
                unifiedPinnedItemsEnabled = startupPreferencesReader.isUnifiedPinnedItemsEnabled(),
                searchHintsEnabled = startupPreferencesReader.isSearchHintsEnabled(),
                settingsIconEnabled = startupPreferencesReader.isSettingsIconEnabled(),
                topResultIndicatorEnabled =
                    startupSnapshot?.topResultIndicatorEnabled
                        ?: startupPreferencesReader.isTopResultIndicatorEnabled(),
                openTopResultUsingKeyboardEnabled =
                    startupPreferencesReader.isOpenTopResultUsingKeyboardEnabled(),
                openKeyboardOnLaunch = startupPreferencesReader.isOpenKeyboardOnLaunchEnabled(),
                clearQueryOnLaunch = clearQueryOnLaunch,
                autoCloseOverlay = startupPreferencesReader.isAutoCloseOverlayEnabled(),
                fontScaleMultiplier =
                    (startupSnapshot?.fontScaleMultiplier
                            ?: startupPreferencesReader.getFontScaleMultiplier())
                        .coerceIn(
                            UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                            UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
                        ),
                useSystemFont =
                    startupSnapshot?.useSystemFont ?: startupPreferencesReader.shouldUseSystemFont(),
                homeTextColorOverride = startupPreferencesReader.getHomeTextColorOverride(),
                launcherAppIcon = startupPreferencesReader.getLauncherAppIcon(),
                showAppLabels =
                    startupSnapshot?.showAppLabels ?: startupPreferencesReader.shouldShowAppLabels(),
                appIconSizeStep =
                    startupSnapshot?.appIconSizeStep
                        ?: startupPreferencesReader.getAppIconSizeStep(),
                appIconShape = startupPreferencesReader.getAppIconShape(),
                themedIconsEnabled = startupPreferencesReader.isThemedIconsEnabled(),
                deviceThemeEnabled = startupPreferencesReader.isDeviceThemeEnabled(),
                amoledThemeEnabled = startupPreferencesReader.isAmoledThemeEnabled(),
                maskUnsupportedIconPackIcons =
                    startupPreferencesReader.isIconPackUnsupportedIconMaskEnabled(),
                appSuggestionsEnabled =
                    startupSnapshot?.appSuggestionsEnabled
                        ?: startupPreferencesReader.areAppSuggestionsEnabled(),
                showAllAppsButton = startupPreferencesReader.shouldShowAllAppsButton(),
                includeNonLaunchableAppsInSearch =
                    startupPreferencesReader.shouldIncludeNonLaunchableAppsInSearch(),
                showInRecents = startupPreferencesReader.shouldShowInRecents(),
                selectedAppSuggestionTab = startupPreferencesReader.getSelectedAppSuggestionTab(),
                enabledAppSuggestionTabs = startupPreferencesReader.getEnabledAppSuggestionTabs(),
                selectRetainedQuery = !clearQueryOnLaunch && inMemoryRetainedQuery.isNotEmpty(),
            )

        return SearchViewModelInitialState(
            instantStartupSurfaceEnabled = instantStartupSurfaceEnabled,
            startupSnapshot = startupSnapshot,
            resultsState = initialResultsState,
            permissionState = initialPermissionState,
            featureState = initialFeatureState,
            configState = initialConfigState,
        )
    }

}
