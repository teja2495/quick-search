package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchScreenWallpaperLogic
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.ThemeModeFallbackBackgroundAlpha

private const val STARTUP_BACKGROUND_TRANSITION_DURATION_MS = 90

/**
 * True while the navigation host draws one wallpaper behind Home and the widgets panel, so those
 * pages skip their own background and only their content slides during the page switch.
 */
internal val LocalPageBackdropDrawnByHost = compositionLocalOf { false }

/** The wallpaper, custom image, or theme background shared by Home and the widgets panel. */
@Composable
internal fun SharedPageBackdrop(
    state: SearchUiState,
    modifier: Modifier = Modifier,
) {
    val wallpaperState = SearchScreenWallpaperLogic(state = state)
    SearchPageBackdrop(
        state = state,
        imageBitmap = wallpaperState.imageBitmap,
        useImageBackground = wallpaperState.usesWallpaperBackground,
        useSystemWallpaperBackdrop = wallpaperState.usesSystemWallpaperBackdrop,
        useMonoThemeFallback = wallpaperState.usesMonoThemeFallback,
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
                DesignTokens.WallpaperFadeInDuration + 120
            },
        fallbackBackgroundAlpha =
            if (!state.deviceThemeEnabled && effectiveBackgroundSource == BackgroundSource.THEME) {
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
        modifier = modifier,
    )
}
