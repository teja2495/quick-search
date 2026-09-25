package com.tk.quicksearch.search.searchScreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.isAmoledSurfaceTheme
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme

internal data class SearchSurfaceColors(
    val amoledSurfacesActive: Boolean,
    val searchColorTheme: com.tk.quicksearch.shared.ui.theme.SearchColorTheme?,
    val overlayCardColor: androidx.compose.ui.graphics.Color?,
    val overlayDividerTint: androidx.compose.ui.graphics.Color?,
    val overlayActionTint: androidx.compose.ui.graphics.Color?,
)

@Composable
internal fun rememberSearchSurfaceColors(state: SearchUiState): SearchSurfaceColors {
    val useOverlayThemeTints = !state.deviceThemeEnabled && state.backgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val amoledSurfacesActive =
            isAmoledSurfaceTheme(
                    amoledThemeEnabled = state.amoledThemeEnabled,
                    theme = state.appTheme,
                    isDarkMode = isDarkMode,
                    deviceThemeEnabled = state.deviceThemeEnabled,
                    backgroundSource = state.backgroundSource,
            )
    val searchColorTheme =
            if (state.deviceThemeEnabled) {
                null
            } else {
                resolveSearchColorTheme(
                        theme = state.appTheme,
                        backgroundSource = state.backgroundSource,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                        amoledThemeEnabled = state.amoledThemeEnabled,
                )
            }
    val overlayCardColor =
            if (useOverlayThemeTints) {
                appThemeResultCardColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                        amoledThemeEnabled = state.amoledThemeEnabled,
                )
            } else {
                null
            }
    val overlayDividerTint =
            if (useOverlayThemeTints) {
                appThemeDividerColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                )
            } else {
                null
            }
    val overlayActionTint =
            if (useOverlayThemeTints) {
                appThemeActionColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                )
            } else {
                null
            }
    return SearchSurfaceColors(
        amoledSurfacesActive,
        searchColorTheme,
        overlayCardColor,
        overlayDividerTint,
        overlayActionTint,
    )
}
