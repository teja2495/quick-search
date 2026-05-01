package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore

internal data class SearchViewModelInitialState(
    val instantStartupSurfaceEnabled: Boolean,
    val startupSnapshot: StartupSurfaceSnapshot?,
    val resultsState: SearchResultsState,
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
                startupSurfaceStore.loadSnapshot()
            } else {
                null
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

        val initialResultsState =
            SearchResultsState(
                query = if (clearQueryOnLaunch) "" else inMemoryRetainedQuery,
                recentApps = startupSnapshot?.suggestedApps.orEmpty(),
                indexedAppCount = startupSnapshot?.suggestedApps?.size ?: 0,
            )

        val initialFeatureState =
            SearchFeatureState(
                isSearchEngineAliasSuffixEnabled =
                    startupPreferencesReader.isSearchEngineAliasSuffixEnabled(),
                isAliasTriggerAfterSpaceEnabled =
                    startupPreferencesReader.isAliasTriggerAfterSpaceEnabled(),
                showTodayEvents = startupPreferencesReader.getShowTodayEvents(),
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
                oneHandedMode =
                    startupSnapshot?.oneHandedMode ?: startupPreferencesReader.isOneHandedMode(),
                bottomSearchBarEnabled =
                    startupSnapshot?.bottomSearchBarEnabled
                        ?: startupPreferencesReader.isBottomSearchBarEnabled(),
                topResultIndicatorEnabled =
                    startupSnapshot?.topResultIndicatorEnabled
                        ?: startupPreferencesReader.isTopResultIndicatorEnabled(),
                openKeyboardOnLaunch =
                    startupSnapshot?.openKeyboardOnLaunch
                        ?: startupPreferencesReader.isOpenKeyboardOnLaunchEnabled(),
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
                launcherAppIcon = startupPreferencesReader.getLauncherAppIcon(),
                showAppLabels =
                    startupSnapshot?.showAppLabels ?: startupPreferencesReader.shouldShowAppLabels(),
                appSuggestionsEnabled =
                    startupSnapshot?.appSuggestionsEnabled
                        ?: startupPreferencesReader.areAppSuggestionsEnabled(),
                selectRetainedQuery = !clearQueryOnLaunch && inMemoryRetainedQuery.isNotEmpty(),
            )

        return SearchViewModelInitialState(
            instantStartupSurfaceEnabled = instantStartupSurfaceEnabled,
            startupSnapshot = startupSnapshot,
            resultsState = initialResultsState,
            featureState = initialFeatureState,
            configState = initialConfigState,
        )
    }
}
