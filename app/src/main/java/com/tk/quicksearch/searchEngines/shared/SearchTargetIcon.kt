package com.tk.quicksearch.searchEngines.shared

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.searchEngines.getContentDescription
import com.tk.quicksearch.searchEngines.isInAppBrowserPackage
import com.tk.quicksearch.searchEngines.getContentDescriptionResId
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.getDrawableResId
import com.tk.quicksearch.searchEngines.getIconColorPolicy
import com.tk.quicksearch.searchEngines.isInstallOnlyEngine
import com.tk.quicksearch.searchEngines.SearchEngineIconColorPolicy

/**
 * Configuration for rendering search target icons with different styles.
 */
enum class IconRenderStyle {
    /** Simple icon rendering with no special effects */
    SIMPLE,

    /** Advanced icon rendering with color filtering for light/dark themes */
    ADVANCED,
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
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    modifier: Modifier = Modifier,
) {
    when (target) {
        is SearchTarget.Engine -> {
            val targetEngine = target.engine
            val iconCandidates =
                targetEngine.getAppPackageCandidates().map { packageName ->
                    packageName to
                        rememberAppIcon(
                            packageName = packageName,
                            forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                        )
                }
            val appIconBitmap = iconCandidates.firstOrNull { it.second.bitmap != null }?.second?.bitmap

            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = targetEngine.getContentDescription(),
                    modifier =
                        modifier.size(iconSize).then(
                            if (appIconShape == AppIconShape.CIRCLE) {
                                Modifier.clip(CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentScale = ContentScale.Fit,
                )
            } else if (!targetEngine.isInstallOnlyEngine()) {
                val backgroundColor = MaterialTheme.colorScheme.background
                val isLightMode =
                    backgroundColor.red > 0.9f &&
                        backgroundColor.green > 0.9f &&
                        backgroundColor.blue > 0.9f

                if (targetEngine == SearchEngine.WIKIPEDIA) {
                    Icon(
                        painter = painterResource(id = targetEngine.getDrawableResId()),
                        contentDescription = targetEngine.getContentDescription(),
                        modifier = modifier.size(iconSize),
                        tint = if (isLightMode) Color.Black else Color.White,
                    )
                } else {
                    when (style) {
                        IconRenderStyle.SIMPLE -> {
                            Icon(
                                painter = painterResource(id = targetEngine.getDrawableResId()),
                                contentDescription = targetEngine.getContentDescription(),
                                modifier = modifier.size(iconSize),
                                tint = Color.Unspecified,
                            )
                        }

                        IconRenderStyle.ADVANCED -> {
                            val colorPolicy = targetEngine.getIconColorPolicy()
                            val colorFilter =
                                if (isLightMode) {
                                    when (colorPolicy) {
                                        SearchEngineIconColorPolicy.DARKEN_ON_LIGHT ->
                                            ColorFilter.colorMatrix(
                                                ColorMatrix(
                                                    floatArrayOf(
                                                        0.3f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0.3f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0.3f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        1f,
                                                        0f,
                                                    ),
                                                ),
                                            )
                                        SearchEngineIconColorPolicy.INVERT_ON_LIGHT ->
                                            ColorFilter.colorMatrix(
                                                ColorMatrix(
                                                    floatArrayOf(
                                                        -1f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        255f,
                                                        0f,
                                                        -1f,
                                                        0f,
                                                        0f,
                                                        255f,
                                                        0f,
                                                        0f,
                                                        -1f,
                                                        0f,
                                                        255f,
                                                        0f,
                                                        0f,
                                                        0f,
                                                        1f,
                                                        0f,
                                                    ),
                                                ),
                                            )
                                        SearchEngineIconColorPolicy.NONE -> null
                                    }
                                } else null

                            Image(
                                painter = painterResource(id = targetEngine.getDrawableResId()),
                                contentDescription = targetEngine.getContentDescription(),
                                modifier = modifier.size(iconSize),
                                contentScale = ContentScale.Fit,
                                colorFilter = colorFilter,
                            )
                        }
                    }
                }
            }
        }

        is SearchTarget.Browser -> {
            if (isInAppBrowserPackage(target.app.packageName)) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return
            }
            val iconResult =
                rememberAppIcon(
                    packageName = target.app.packageName,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                )
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap!!,
                    contentDescription = target.app.label,
                    modifier =
                        modifier.size(iconSize).then(
                            if (appIconShape == AppIconShape.CIRCLE) {
                                Modifier.clip(CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is SearchTarget.Custom -> {
            val faviconBitmap =
                remember(target.custom.faviconBase64) {
                    val encoded = target.custom.faviconBase64 ?: return@remember null
                    val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                        ?: return@remember null
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            if (faviconBitmap != null) {
                Image(
                    bitmap = faviconBitmap,
                    contentDescription = target.custom.name,
                    modifier =
                        modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(iconSize * 0.2f)),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = target.custom.name,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
