package com.tk.quicksearch.search.searchEngines.inline

import androidx.compose.foundation.Image
import com.tk.quicksearch.search.core.SearchTarget
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.searchEngines.getContentDescription
import com.tk.quicksearch.search.searchEngines.getDrawableResId
import com.tk.quicksearch.util.hapticConfirm

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
    engine: SearchTarget,
    query: String,
    iconSize: Dp,
    itemWidth: Dp,
    onSearchEngineClick: (String, SearchTarget) -> Unit,
    onSearchEngineLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .width(itemWidth)
            .combinedClickable(
                onClick = {
                    hapticConfirm(view)()
                    onSearchEngineClick(query, engine)
                },
                onLongClick = onSearchEngineLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        when (engine) {
            is SearchTarget.Engine -> {
                val targetEngine = engine.engine
                val needsColorChange = targetEngine in setOf(
                    SearchEngine.CHATGPT,
                    SearchEngine.GROK,
                    SearchEngine.AMAZON
                )

                val backgroundColor = MaterialTheme.colorScheme.background
                val isLightMode =
                        backgroundColor.red > 0.9f &&
                                backgroundColor.green > 0.9f &&
                                backgroundColor.blue > 0.9f

                val colorFilter = if (needsColorChange && isLightMode) {
                    if (targetEngine == SearchEngine.AMAZON) {
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    0.3f, 0f, 0f, 0f, 0f,
                                    0f, 0.3f, 0f, 0f, 0f,
                                    0f, 0f, 0.3f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    } else {
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    }
                } else {
                    null
                }

                Image(
                    painter = painterResource(id = targetEngine.getDrawableResId()),
                    contentDescription = targetEngine.getContentDescription(),
                    modifier = Modifier.size(iconSize),
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter
                )
            }
            is SearchTarget.Browser -> {
                val iconBitmap = rememberAppIcon(packageName = engine.app.packageName)
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = engine.app.label,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = engine.app.label,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

