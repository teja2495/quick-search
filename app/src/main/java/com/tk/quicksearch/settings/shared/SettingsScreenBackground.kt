package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.searchScreen.SearchScreenBackground
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.ThemeModeFallbackBackgroundAlpha

@Composable
fun SettingsScreenBackground(
    appTheme: AppTheme,
    overlayThemeIntensity: Float,
    deviceThemeEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val searchColorTheme = remember(appTheme, overlayThemeIntensity, isDarkMode, deviceThemeEnabled) {
        if (deviceThemeEnabled) {
            null
        } else {
            resolveSearchColorTheme(
                theme = appTheme,
                backgroundSource = BackgroundSource.THEME,
                isDarkMode = isDarkMode,
                intensity = overlayThemeIntensity,
            )
        }
    }

    CompositionLocalProvider(LocalSearchColorTheme provides searchColorTheme) {
        Box(modifier = modifier.fillMaxSize()) {
            SearchScreenBackground(
                showWallpaperBackground = false,
                wallpaperBitmap = null,
                wallpaperBackgroundAlpha = 0f,
                wallpaperBlurRadius = 0f,
                fallbackBackgroundAlpha = if (deviceThemeEnabled) 1f else ThemeModeFallbackBackgroundAlpha,
                useGradientFallback = !deviceThemeEnabled,
                appTheme = appTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                modifier = Modifier.fillMaxSize(),
            )
            content()
        }
    }
}
