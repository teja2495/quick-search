package com.tk.quicksearch.searchEngines.shared

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.apps.rememberAppIconMaskShape
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.searchEngines.getContentDescription
import com.tk.quicksearch.searchEngines.isInAppBrowserPackage
import com.tk.quicksearch.searchEngines.getDrawableResId
import com.tk.quicksearch.searchEngines.getIconColorPolicy
import com.tk.quicksearch.searchEngines.isInstallOnlyEngine
import com.tk.quicksearch.searchEngines.SearchEngineIconColorPolicy
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppShapes

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
    iconPackPackage: String? = null,
    modifier: Modifier = Modifier,
) {
    val appIconMaskShape = rememberAppIconMaskShape()
    when (target) {
        is SearchTarget.Engine -> {
            val targetEngine = target.engine
            val iconCandidates =
                targetEngine.getAppPackageCandidates().map { packageName ->
                    packageName to
                        rememberAppIcon(
                            packageName = packageName,
                            iconPackPackage = iconPackPackage,
                            forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                            // An archived engine app can't handle searches, so show the engine icon.
                            includeArchived = false,
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
                                Modifier.clip(appIconMaskShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentScale = ContentScale.Fit,
                )
            } else if (!targetEngine.isInstallOnlyEngine()) {
                val drawableResId = targetEngine.getDrawableResId() ?: return
                val backgroundColor = MaterialTheme.colorScheme.background
                val isLightMode =
                    backgroundColor.red > 0.9f &&
                        backgroundColor.green > 0.9f &&
                        backgroundColor.blue > 0.9f

                if (targetEngine == SearchEngine.WIKIPEDIA) {
                    Icon(
                        painter = painterResource(id = drawableResId),
                        contentDescription = targetEngine.getContentDescription(),
                        modifier = modifier.size(iconSize),
                        tint = AppColors.indicatorBase(lightFill = !isLightMode),
                    )
                } else {
                    when (style) {
                        IconRenderStyle.SIMPLE -> {
                            Icon(
                                painter = painterResource(id = drawableResId),
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
                                painter = painterResource(id = drawableResId),
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
                    imageVector = MaterialSymbols.Public,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return
            }
            val iconResult =
                rememberAppIcon(
                    packageName = target.app.packageName,
                    iconPackPackage = iconPackPackage,
                    forceCircularMask = appIconShape == AppIconShape.CIRCLE,
                )
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap,
                    contentDescription = target.app.label,
                    modifier =
                        modifier.size(iconSize).then(
                            if (appIconShape == AppIconShape.CIRCLE) {
                                Modifier.clip(appIconMaskShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Public,
                    contentDescription = target.app.label,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is SearchTarget.Custom -> {
            val encoded = target.custom.faviconBase64
            // Key the state as well as the producer so an edited engine never flashes its old icon.
            val faviconBitmap by androidx.compose.runtime.key(encoded) {
                produceState<ImageBitmap?>(initialValue = null, key1 = encoded) {
                    value = withContext(Dispatchers.IO) {
                        encoded?.let {
                            runCatching {
                                val bytes = Base64.decode(it, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            }.getOrNull()
                        }
                    }
                }
            }
            if (faviconBitmap != null) {
                Image(
                    bitmap = requireNotNull(faviconBitmap),
                    contentDescription = target.custom.name,
                    modifier =
                        modifier
                            .size(iconSize)
                            .clip(AppShapes.faviconScaled),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Public,
                    contentDescription = target.custom.name,
                    modifier = modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
