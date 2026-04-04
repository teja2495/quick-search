package com.tk.quicksearch.search.core

internal class SearchViewModelStartupState {
    var pendingNavigationClear: Boolean = false
    var isStartupComplete: Boolean = false
    var resumeNeedsStaticDataRefresh: Boolean = false
    var lastBrowserTargetRefreshMs: Long = 0L
    var wallpaperAvailable: Boolean = false
}

internal class SearchViewModelPreferencesStateAccess(
    private val state: SearchViewModelLegacyPreferenceState,
    private val computeEffectiveIsDarkMode: () -> Boolean,
    private val applyLauncherIconSelection: (LauncherAppIcon?) -> Unit,
    private val saveStartupSurfaceSnapshotCallback: (
        forcePreviewRefresh: Boolean,
        allowDuringQuery: Boolean,
    ) -> Unit,
) : SearchPreferencesStateAccess {
    override var enabledFileTypes: Set<com.tk.quicksearch.search.models.FileType>
        get() = state.enabledFileTypes
        set(value) {
            state.enabledFileTypes = value
        }

    override var showFolders: Boolean
        get() = state.showFolders
        set(value) {
            state.showFolders = value
        }

    override var showSystemFiles: Boolean
        get() = state.showSystemFiles
        set(value) {
            state.showSystemFiles = value
        }

    override var folderWhitelistPatterns: Set<String>
        get() = state.folderWhitelistPatterns
        set(value) {
            state.folderWhitelistPatterns = value
        }

    override var folderBlacklistPatterns: Set<String>
        get() = state.folderBlacklistPatterns
        set(value) {
            state.folderBlacklistPatterns = value
        }

    override var oneHandedMode: Boolean
        get() = state.oneHandedMode
        set(value) {
            state.oneHandedMode = value
        }

    override var bottomSearchBarEnabled: Boolean
        get() = state.bottomSearchBarEnabled
        set(value) {
            state.bottomSearchBarEnabled = value
        }

    override var topResultIndicatorEnabled: Boolean
        get() = state.topResultIndicatorEnabled
        set(value) {
            state.topResultIndicatorEnabled = value
        }

    override var wallpaperAccentEnabled: Boolean
        get() = state.wallpaperAccentEnabled
        set(value) {
            state.wallpaperAccentEnabled = value
        }

    override var openKeyboardOnLaunch: Boolean
        get() = state.openKeyboardOnLaunch
        set(value) {
            state.openKeyboardOnLaunch = value
        }

    override var overlayModeEnabled: Boolean
        get() = state.overlayModeEnabled
        set(value) {
            state.overlayModeEnabled = value
        }

    override var autoCloseOverlay: Boolean
        get() = state.autoCloseOverlay
        set(value) {
            state.autoCloseOverlay = value
        }

    override var appSuggestionsEnabled: Boolean
        get() = state.appSuggestionsEnabled
        set(value) {
            state.appSuggestionsEnabled = value
        }

    override var showAppLabels: Boolean
        get() = state.showAppLabels
        set(value) {
            state.showAppLabels = value
        }

    override var phoneAppGridColumns: Int
        get() = state.phoneAppGridColumns
        set(value) {
            state.phoneAppGridColumns = value
        }

    override var appIconShape: AppIconShape
        get() = state.appIconShape
        set(value) {
            state.appIconShape = value
        }

    override var launcherAppIcon: LauncherAppIcon
        get() = state.launcherAppIcon
        set(value) {
            state.launcherAppIcon = value
        }

    override var themedIconsEnabled: Boolean
        get() = state.themedIconsEnabled
        set(value) {
            state.themedIconsEnabled = value
        }

    override var maskUnsupportedIconPackIcons: Boolean
        get() = state.maskUnsupportedIconPackIcons
        set(value) {
            state.maskUnsupportedIconPackIcons = value
        }

    override var wallpaperBackgroundAlpha: Float
        get() = state.wallpaperBackgroundAlpha
        set(value) {
            state.wallpaperBackgroundAlpha = value
        }

    override var wallpaperBlurRadius: Float
        get() = state.wallpaperBlurRadius
        set(value) {
            state.wallpaperBlurRadius = value
        }

    override var appTheme: AppTheme
        get() = state.appTheme
        set(value) {
            state.appTheme = value
        }

    override var overlayThemeIntensity: Float
        get() = state.overlayThemeIntensity
        set(value) {
            state.overlayThemeIntensity = value
        }

    override var fontScaleMultiplier: Float
        get() = state.fontScaleMultiplier
        set(value) {
            state.fontScaleMultiplier = value
        }

    override var backgroundSource: BackgroundSource
        get() = state.backgroundSource
        set(value) {
            state.backgroundSource = value
        }

    override var customImageUri: String?
        get() = state.customImageUri
        set(value) {
            state.customImageUri = value
        }

    override var clearQueryOnLaunch: Boolean
        get() = state.clearQueryOnLaunch
        set(value) {
            state.clearQueryOnLaunch = value
        }

    override var amazonDomain: String?
        get() = state.amazonDomain
        set(value) {
            state.amazonDomain = value
        }

    override fun computeEffectiveIsDarkMode(): Boolean = computeEffectiveIsDarkMode.invoke()

    override fun applyLauncherIconSelection(selection: LauncherAppIcon?) {
        applyLauncherIconSelection.invoke(selection)
    }

    override fun saveStartupSurfaceSnapshotAsync(forcePreviewRefresh: Boolean, allowDuringQuery: Boolean) {
        saveStartupSurfaceSnapshotCallback.invoke(forcePreviewRefresh, allowDuringQuery)
    }
}

internal class SearchViewModelStartupLifecycleStateAccess(
    private val startupState: SearchViewModelStartupState,
    private val directDialEnabledProvider: () -> Boolean,
    private val setDirectDialEnabled: (Boolean) -> Unit,
) : SearchStartupLifecycleStateAccess {
    override var pendingNavigationClear: Boolean
        get() = startupState.pendingNavigationClear
        set(value) {
            startupState.pendingNavigationClear = value
        }

    override var isStartupComplete: Boolean
        get() = startupState.isStartupComplete
        set(value) {
            startupState.isStartupComplete = value
        }

    override var resumeNeedsStaticDataRefresh: Boolean
        get() = startupState.resumeNeedsStaticDataRefresh
        set(value) {
            startupState.resumeNeedsStaticDataRefresh = value
        }

    override var lastBrowserTargetRefreshMs: Long
        get() = startupState.lastBrowserTargetRefreshMs
        set(value) {
            startupState.lastBrowserTargetRefreshMs = value
        }

    override var wallpaperAvailable: Boolean
        get() = startupState.wallpaperAvailable
        set(value) {
            startupState.wallpaperAvailable = value
        }

    override var directDialEnabled: Boolean
        get() = directDialEnabledProvider.invoke()
        set(value) {
            setDirectDialEnabled.invoke(value)
        }
}
