package com.tk.quicksearch.settings.searchengines.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.tk.quicksearch.search.SearchEngine

/**
 * Helper function to get color filter for search engine icons based on theme.
 * Applies filters to convert white icons to black in light mode.
 */
@Composable
fun getSearchEngineIconColorFilter(engine: SearchEngine): ColorFilter? {
    val needsColorChange = engine in setOf(
        SearchEngine.CHATGPT,
        SearchEngine.GROK,
        SearchEngine.AMAZON
    )

    // Check if we're in light mode by checking the background color brightness
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightMode = backgroundColor.red > 0.9f && backgroundColor.green > 0.9f && backgroundColor.blue > 0.9f

    return if (needsColorChange && isLightMode) {
        if (engine == SearchEngine.AMAZON) {
            // For Amazon: Use a darker scaling that preserves orange better
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.3f, 0f, 0f, 0f, 0f,     // Red: scale to 30% (preserves orange better)
                        0f, 0.3f, 0f, 0f, 0f,     // Green: scale to 30%
                        0f, 0f, 0.3f, 0f, 0f,     // Blue: scale to 30%
                        0f, 0f, 0f, 1f, 0f        // Alpha: keep
                    )
                )
            )
        } else {
            // For ChatGPT and Grok: simple inversion (all white → all black)
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,  // Red: invert
                        0f, -1f, 0f, 0f, 255f,   // Green: invert
                        0f, 0f, -1f, 0f, 255f,   // Blue: invert
                        0f, 0f, 0f, 1f, 0f       // Alpha: keep
                    )
                )
            )
        }
    } else {
        null
    }
}
