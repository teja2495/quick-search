package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Constants for consistent spacing and styling in search engine settings.
 * @deprecated Use DesignTokens instead for new components.
 */
object SearchEngineSettingsSpacing {
    val cardHorizontalPadding = DesignTokens.CardHorizontalPadding
    val cardVerticalPadding = DesignTokens.CardVerticalPadding
    val cardTopPadding = DesignTokens.CardTopPadding
    val cardBottomPadding = DesignTokens.CardBottomPadding
    val apiKeyButtonBottomPadding = 8.dp
    val rowHorizontalPadding = DesignTokens.CardHorizontalPadding
    val rowVerticalPadding = DesignTokens.CardVerticalPadding
    val itemHeight = DesignTokens.DraggableItemHeight
}

private const val AMAZON_ICON_SCALE_FACTOR = 0.3f
private const val INVERSION_OFFSET = 255f
private const val BRIGHTNESS_THRESHOLD = 0.7f

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

    // Check if we're in light mode by calculating perceived brightness
    val backgroundColor = MaterialTheme.colorScheme.background
    val brightness = (backgroundColor.red * 0.299f + backgroundColor.green * 0.587f + backgroundColor.blue * 0.114f)
    val isLightMode = brightness > BRIGHTNESS_THRESHOLD

    return if (needsColorChange && isLightMode) {
        if (engine == SearchEngine.AMAZON) {
            // For Amazon: Use a darker scaling that preserves orange better
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        AMAZON_ICON_SCALE_FACTOR, 0f, 0f, 0f, 0f,     // Red: scale to preserve orange
                        0f, AMAZON_ICON_SCALE_FACTOR, 0f, 0f, 0f,     // Green: scale to preserve orange
                        0f, 0f, AMAZON_ICON_SCALE_FACTOR, 0f, 0f,     // Blue: scale to preserve orange
                        0f, 0f, 0f, 1f, 0f        // Alpha: keep
                    )
                )
            )
        } else {
            // For ChatGPT and Grok: simple inversion (all white → all black)
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, INVERSION_OFFSET,  // Red: invert
                        0f, -1f, 0f, 0f, INVERSION_OFFSET,   // Green: invert
                        0f, 0f, -1f, 0f, INVERSION_OFFSET,   // Blue: invert
                        0f, 0f, 0f, 1f, 0f       // Alpha: keep
                    )
                )
            )
        }
    } else {
        null
    }
}

/**
 * Reusable divider component with consistent styling.
 */
@Composable
fun SearchEngineDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}