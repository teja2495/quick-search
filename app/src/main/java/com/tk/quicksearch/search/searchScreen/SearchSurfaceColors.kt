package com.tk.quicksearch.search.searchScreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.shared.ui.theme.isAmoledSurfaceTheme
import com.tk.quicksearch.shared.ui.theme.overlayThemeTints
import com.tk.quicksearch.shared.ui.theme.resolveSearchColorTheme

internal data class SearchSurfaceColors(
    val amoledSurfacesActive: Boolean,
    val searchColorTheme: com.tk.quicksearch.shared.ui.theme.SearchColorTheme?,
    val overlayCardColor: androidx.compose.ui.graphics.Color?,
    val overlayDividerTint: androidx.compose.ui.graphics.Color?,
    val overlayActionTint: androidx.compose.ui.graphics.Color?,
)

@Composable
internal fun rememberSearchSurfaceColors(state: SearchUiState): SearchSurfaceColors {
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
    val overlayThemeTints = overlayThemeTints(state, isDarkMode)
    return SearchSurfaceColors(
        amoledSurfacesActive,
        searchColorTheme,
        overlayThemeTints.cardColor,
        overlayThemeTints.dividerTint,
        overlayThemeTints.actionTint,
    )
}
