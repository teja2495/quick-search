package com.tk.quicksearch.search.searchEngines.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchEngines.getContentDescription
import com.tk.quicksearch.search.searchEngines.getDrawableResId

/**
 * Configuration for rendering search target icons with different styles.
 */
enum class IconRenderStyle {
    /** Simple icon rendering with no special effects */
    SIMPLE,

    /** Advanced icon rendering with color filtering for light/dark themes */
    ADVANCED
}

/**
 * Shared composable for rendering search engine and browser icons.
 *
 * @param target The search target to render
 * @param iconSize Size of the icon
 * @param style Rendering style (simple vs advanced)
 * @param modifier Modifier for the icon
 */
@Composable
fun SearchTargetIcon(
    target: SearchTarget,
    iconSize: Dp,
    style: IconRenderStyle = IconRenderStyle.SIMPLE,
    modifier: Modifier = Modifier
) {
    when (target) {
        is SearchTarget.Engine -> {
            val targetEngine = target.engine

            when (style) {
                IconRenderStyle.SIMPLE -> {
                    Icon(
                        painter = painterResource(id = targetEngine.getDrawableResId()),
                        contentDescription = targetEngine.getContentDescription(),
                        modifier = modifier.size(iconSize),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                }

                IconRenderStyle.ADVANCED -> {
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
                        modifier = modifier.size(iconSize),
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter
                    )
                }
            }
        }

        is SearchTarget.Browser -> {
            val iconBitmap = rememberAppIcon(packageName = target.app.packageName)
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}