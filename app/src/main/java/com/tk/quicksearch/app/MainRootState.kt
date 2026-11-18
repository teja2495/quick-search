package com.tk.quicksearch.app

import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState

/**
 * The slice of [SearchUiState] the activity's root composition reads. Collecting it instead of the
 * full state keeps the root (theme, system bars, startup traces) from recomposing on every
 * keystroke and result update.
 */
internal data class MainRootState(
    val showInRecents: Boolean,
    val showStatusBar: Boolean,
    val appTheme: AppTheme,
    val appThemeMode: AppThemeMode,
    val fontScaleMultiplier: Float,
    val useSystemFont: Boolean,
    val backgroundSource: BackgroundSource,
    val customImageUri: String?,
    val accentColorMode: AccentColorMode,
    val customAccentColorArgb: Int,
    val deviceThemeEnabled: Boolean,
    val startupBackgroundPreviewPath: String?,
    val wallpaperAvailable: Boolean,
    val isStartupCoreSurfaceReady: Boolean,
    val appSuggestionsReady: Boolean,
) {
    companion object {
        fun from(state: SearchUiState): MainRootState =
            MainRootState(
                showInRecents = state.showInRecents,
                showStatusBar = state.showStatusBar,
                appTheme = state.appTheme,
                appThemeMode = state.appThemeMode,
                fontScaleMultiplier = state.fontScaleMultiplier,
                useSystemFont = state.useSystemFont,
                backgroundSource = state.backgroundSource,
                customImageUri = state.customImageUri,
                accentColorMode = state.accentColorMode,
                customAccentColorArgb = state.customAccentColorArgb,
                deviceThemeEnabled = state.deviceThemeEnabled,
                startupBackgroundPreviewPath = state.startupBackgroundPreviewPath,
                wallpaperAvailable = state.wallpaperAvailable,
                isStartupCoreSurfaceReady = state.isStartupCoreSurfaceReady,
                appSuggestionsReady =
                    !state.appSuggestionsEnabled ||
                        state.recentApps.isNotEmpty() ||
                        state.pinnedApps.isNotEmpty(),
            )
    }
}
