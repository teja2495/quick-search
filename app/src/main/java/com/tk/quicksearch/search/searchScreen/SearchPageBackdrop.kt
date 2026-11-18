package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperLogic
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperState
import com.tk.quicksearch.search.searchScreen.searchRoute.onWallpaperUnavailable
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.ThemeModeFallbackBackgroundAlpha

private const val STARTUP_BACKGROUND_TRANSITION_DURATION_MS = AppMotion.DurationQuick

/** The host owns the wallpaper for both Home and the widgets panel. */
internal val LocalSharedPageWallpaper = compositionLocalOf<SearchScreenWallpaperState?> { null }

@Composable
internal fun rememberSharedPageWallpaper(
    state: SearchUiState,
    viewModel: SearchViewModel,
    coveredBySettings: Boolean,
): SearchScreenWallpaperState {
    val activity = LocalContext.current as? MainActivity
    val trimVersionWhenCovered = remember(activity, coveredBySettings) { activity?.pageBackdropTrimVersion }
    val releaseBitmap = coveredBySettings && activity?.pageBackdropTrimVersion != trimVersionWhenCovered
    return SearchScreenWallpaperLogic(
        state = state,
        onWallpaperLoaded = { viewModel.setWallpaperAvailable(true) },
        onWallpaperUnavailable = viewModel::onWallpaperUnavailable,
        onSystemWallpaperChanged = viewModel::resetHomeTextColorForNewWallpaper,
        retainBitmap = !releaseBitmap,
    )
}

@Composable
internal fun SharedPageBackdrop(
    state: SearchUiState,
    wallpaperState: SearchScreenWallpaperState,
    modifier: Modifier = Modifier,
) {
    SearchPageBackdrop(
        state = state,
        imageBitmap = wallpaperState.imageBitmap,
        useImageBackground = wallpaperState.usesWallpaperBackground,
        useSystemWallpaperBackdrop = wallpaperState.usesSystemWallpaperBackdrop,
        useMonoThemeFallback = wallpaperState.usesMonoThemeFallback,
        systemWallpaperUnderBitmap = wallpaperState.systemWallpaperUnderBitmap,
        modifier = modifier,
    )
}

@Composable
internal fun SearchPageBackdrop(
    state: SearchUiState,
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    useImageBackground: Boolean,
    useSystemWallpaperBackdrop: Boolean,
    useMonoThemeFallback: Boolean,
    systemWallpaperUnderBitmap: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (useSystemWallpaperBackdrop) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(
                        AppColors.WallpaperOverlayTint.copy(
                            alpha = state.wallpaperBackgroundAlpha.coerceIn(0f, 1f),
                        ),
                    ),
        )
        return
    }
    val effectiveBackgroundSource =
        if (useMonoThemeFallback) BackgroundSource.THEME else state.backgroundSource
    SearchScreenBackground(
        showWallpaperBackground = useImageBackground,
        wallpaperBitmap = imageBitmap,
        wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
        wallpaperBlurRadius = state.wallpaperBlurRadius,
        backgroundTransitionDurationMillis =
            if (state.isInitializing && effectiveBackgroundSource != BackgroundSource.THEME) {
                STARTUP_BACKGROUND_TRANSITION_DURATION_MS
            } else {
                AppMotion.DurationMedium + 120
            },
        fallbackBackgroundAlpha =
            if (systemWallpaperUnderBitmap) {
                // The window's system wallpaper is the fallback; no solid color under the blurred bitmap.
                0f
            } else if (!state.deviceThemeEnabled && effectiveBackgroundSource == BackgroundSource.THEME) {
                ThemeModeFallbackBackgroundAlpha
            } else {
                1f
            },
        useGradientFallback =
            !state.deviceThemeEnabled &&
                (effectiveBackgroundSource == BackgroundSource.THEME || useMonoThemeFallback),
        appTheme = if (useMonoThemeFallback) AppTheme.MONOCHROME else state.appTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        amoledThemeEnabled = state.amoledThemeEnabled,
        publishFrostedCards = true,
        modifier = modifier,
    )
}
