package com.tk.quicksearch.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composable for displaying a single search engine icon item.
 * 
 * @param engine The search engine to display
 * @param query The search query to pass when clicked
 * @param iconSize The size of the icon
 * @param itemWidth The width of the clickable area
 * @param onSearchEngineClick Callback when the engine is clicked
 */
@Composable
fun SearchEngineIconItem(
    engine: SearchEngine,
    query: String,
    iconSize: Dp,
    itemWidth: Dp,
    onSearchEngineClick: (String, SearchEngine) -> Unit,
    onSearchEngineLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(itemWidth)
            .combinedClickable(
                onClick = { onSearchEngineClick(query, engine) },
                onLongClick = onSearchEngineLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        val needsColorChange = engine in setOf(
            SearchEngine.CHATGPT,
            SearchEngine.GROK,
            SearchEngine.AMAZON
        )
        
        // Check if we're in light mode by checking the background color brightness
        val backgroundColor = MaterialTheme.colorScheme.background
        // Light theme background is very bright (close to 1.0), dark theme is very dark (close to 0.0)
        val isLightMode = backgroundColor.red > 0.9f && backgroundColor.green > 0.9f && backgroundColor.blue > 0.9f
        
        val colorFilter = if (needsColorChange && isLightMode) {
            if (engine == SearchEngine.AMAZON) {
                // For Amazon: Use a darker scaling that preserves orange better
                // Scale to 30% instead of 20% to keep orange more visible
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
        
        Image(
            painter = painterResource(id = engine.getDrawableResId()),
            contentDescription = engine.getContentDescription(),
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter
        )
    }
}

