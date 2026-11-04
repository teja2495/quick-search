package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.search.searchScreen.isAmoledSurfaceTheme
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.ThemeModeFallbackBackgroundAlpha

@Composable
fun SettingsScreenBackground(
    appTheme: AppTheme,
    overlayThemeIntensity: Float,
    deviceThemeEnabled: Boolean = false,
    amoledThemeEnabled: Boolean = false,
    backgroundSource: BackgroundSource = BackgroundSource.THEME,
    wallpaperBitmap: ImageBitmap? = null,
    wallpaperBackgroundAlpha: Float = 0f,
    wallpaperBlurRadius: Float = 0f,
    useSystemWallpaperBackdrop: Boolean = false,
    /** False when a backdrop behind this screen already draws the background. */
    drawBackground: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ProvideSettingsColors(
        appTheme = appTheme,
        overlayThemeIntensity = overlayThemeIntensity,
        deviceThemeEnabled = deviceThemeEnabled,
        amoledThemeEnabled = amoledThemeEnabled,
        backgroundSource = backgroundSource,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            if (drawBackground) {
                SearchScreenBackground(
                    showWallpaperBackground = !useSystemWallpaperBackdrop,
                    wallpaperBitmap = wallpaperBitmap,
                    wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                    wallpaperBlurRadius = wallpaperBlurRadius,
                    fallbackBackgroundAlpha =
                        if (deviceThemeEnabled || backgroundSource != BackgroundSource.THEME) {
                            1f
                        } else {
                            ThemeModeFallbackBackgroundAlpha
                        },
                    useGradientFallback = !deviceThemeEnabled && backgroundSource == BackgroundSource.THEME,
                    appTheme = appTheme,
                    overlayThemeIntensity = overlayThemeIntensity,
                    amoledThemeEnabled = amoledThemeEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
                if (useSystemWallpaperBackdrop) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    com.tk.quicksearch.shared.ui.theme.AppColors.WallpaperOverlayTint.copy(
                                        alpha = wallpaperBackgroundAlpha.coerceIn(0f, 1f),
                                    ),
                                ),
                    )
                }
            }
            content()
        }
    }
}

/** Provides the settings card colors to [content] without drawing the settings background. */
@Composable
fun ProvideSettingsColors(
    appTheme: AppTheme,
    overlayThemeIntensity: Float,
    deviceThemeEnabled: Boolean = false,
    amoledThemeEnabled: Boolean = false,
    backgroundSource: BackgroundSource = BackgroundSource.THEME,
    content: @Composable () -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val amoledSurfacesActive =
        isAmoledSurfaceTheme(
            amoledThemeEnabled = amoledThemeEnabled,
            theme = appTheme,
            isDarkMode = isDarkMode,
            deviceThemeEnabled = deviceThemeEnabled,
            backgroundSource = backgroundSource,
        )
    val searchColorTheme =
        remember(appTheme, overlayThemeIntensity, isDarkMode, deviceThemeEnabled, amoledThemeEnabled, backgroundSource) {
            if (deviceThemeEnabled) {
                null
            } else {
                resolveSearchColorTheme(
                    theme = appTheme,
                    backgroundSource = backgroundSource,
                    isDarkMode = isDarkMode,
                    intensity = overlayThemeIntensity,
                    amoledThemeEnabled = amoledThemeEnabled,
                )
            }
        }

    CompositionLocalProvider(
        LocalSearchColorTheme provides searchColorTheme,
        LocalAmoledThemeActive provides amoledSurfacesActive,
        content = content,
    )
}
