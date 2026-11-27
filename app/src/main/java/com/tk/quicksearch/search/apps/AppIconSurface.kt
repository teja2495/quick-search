package com.tk.quicksearch.search.apps

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationBadge
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.shared.ui.theme.LocalIsSystemWallpaperActive
import com.tk.quicksearch.shared.ui.theme.LocalWallpaperDynamicAccentActive
import com.tk.quicksearch.search.folders.AppFolderMember
import com.tk.quicksearch.search.folders.FolderPreviewIcon
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.ui.theme.AppMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppIconSurface(
        iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
        iconIsLegacy: Boolean,
        iconHasProfileBadge: Boolean = false,
        monochromeData: androidx.compose.ui.graphics.ImageBitmap? = null,
        appName: String,
        isSuspended: Boolean = false,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?,
        gestureModifier: Modifier = Modifier,
        clickGesturesEnabled: Boolean = true,
        appIconSurfaceSize: Dp = DesignTokens.AppIconSize,
        appIconSize: Dp,
        appIconShape: AppIconShape = AppIconShape.DEFAULT,
        hasCustomIconPack: Boolean = false,
        iconPackPackage: String? = null,
        oneHandedMode: Boolean = false,
        themedIconsEnabled: Boolean = true,
        showWallpaperBackground: Boolean = false,
        notificationCount: Int = 0,
        showNotificationCount: Boolean = false,
        folderPreviewMember: AppFolderMember? = null,
        isHeld: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed || isHeld) AppGridPressedScale else 1f,
        animationSpec = AppMotion.fastSpatial(),
        label = "appIconPressScale",
    )
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkTheme = LocalAppIsDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val useLightWallpaperShadow = showWallpaperBackground && !isDarkTheme
    val showThemedIcon = themedIconsEnabled && !hasCustomIconPack &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    val useSystemMaterialYouTones =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    (LocalDeviceDynamicColorsActive.current || LocalIsSystemWallpaperActive.current)
    val useWallpaperDerivedAccent = LocalWallpaperDynamicAccentActive.current
    val useCustomThemeDarkThemedIconColors =
            isDarkTheme && !useSystemMaterialYouTones && !useWallpaperDerivedAccent
    val themedIconBackground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent2_800
                                } else {
                                    android.R.color.system_accent1_100
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.secondaryContainer
            } else {
                colorScheme.primaryContainer
            }
    val themedIconForeground =
            if (useSystemMaterialYouTones) {
                Color(
                        androidx.core.content.ContextCompat.getColor(
                                context,
                                if (isDarkTheme) {
                                    android.R.color.system_accent1_200
                                } else {
                                    android.R.color.system_accent1_600
                                },
                        ),
                )
            } else if (useCustomThemeDarkThemedIconColors) {
                colorScheme.onSecondaryContainer
            } else {
                colorScheme.primary
            }
    val themedIconContainerShape = AppShapes.full
    val appIconMaskShape = rememberAppIconMaskShape()
    val badgeInset = (appIconSurfaceSize - appIconSize) / 2

    Surface(
            modifier = Modifier.requiredSize(appIconSurfaceSize).then(gestureModifier),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = AppShapes.large,
    ) {
        val clickModifier =
                if (!clickGesturesEnabled) {
                    Modifier
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                            role = Role.Button,
                            interactionSource = interactionSource,
                            indication = ripple(color = homeTextColor()),
                            onClick = {
                                hapticConfirm(view)()
                                onClick()
                            },
                            onLongClickLabel = stringResource(R.string.desc_show_options),
                            onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(color = homeTextColor()),
                    ) {
                        hapticConfirm(view)()
                        onClick()
                    }
                }
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .then(if (isSuspended) Modifier.greyscale() else Modifier)
                                .then(clickModifier)
                                .graphicsLayer {
                                    scaleX = pressScale
                                    scaleY = pressScale
                                },
                contentAlignment = Alignment.Center,
        ) {
            if (folderPreviewMember != null) {
                FolderPreviewIcon(
                        members = listOf(folderPreviewMember),
                        iconSize = appIconSize,
                        iconPackPackage = iconPackPackage,
                        appIconShape = appIconShape,
                        showWallpaperBackground = showWallpaperBackground,
                )
            } else if (showThemedIcon && monochromeData != null) {
                Box(
                        modifier = Modifier
                                .then(
                                        if (useLightWallpaperShadow) {
                                            Modifier.shadow(
                                                    elevation = DesignTokens.ElevationLevel2,
                                                    shape = themedIconContainerShape,
                                                    ambientColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                            ),
                                                    spotColor =
                                                            Color.Black.copy(
                                                                    alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                            ),
                                            )
                                        } else {
                                            Modifier
                                        },
                                )
                                .size(appIconSize)
                                .clip(themedIconContainerShape)
                                .background(themedIconBackground),
                        contentAlignment = Alignment.Center,
                ) {
                    Image(
                            bitmap = monochromeData,
                            contentDescription = stringResource(R.string.desc_launch_app, appName),
                            modifier = Modifier.requiredSize(appIconSize * ThemedMonochromeGlyphScale),
                            colorFilter = ColorFilter.tint(themedIconForeground),
                    )
                }
            } else if (iconBitmap != null) {
                val isUnsupportedThemedIcon = showThemedIcon && monochromeData == null
                if (isUnsupportedThemedIcon) {
                    Box(
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = themedIconContainerShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha = LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                                            .size(appIconSize)
                                            .clip(themedIconContainerShape)
                                            .background(themedIconBackground),
                            contentAlignment = Alignment.Center,
                    ) {
                        Image(
                                bitmap = iconBitmap,
                                contentDescription = stringResource(R.string.desc_launch_app, appName),
                                modifier = Modifier.requiredSize(appIconSize * UnsupportedThemedIconGlyphScale),
                                colorFilter = ColorFilter.tint(
                                        themedIconForeground.copy(alpha = UnsupportedThemedIconGlyphAlpha),
                                        BlendMode.SrcAtop,
                                ),
                        )
                    }
                } else {
                    val clipModifier =
                            when {
                                // Badged work profile icons are already circle-masked; clipping
                                // again would cut off the badge in the corner.
                                iconHasProfileBadge -> Modifier
                                appIconShape == AppIconShape.CIRCLE ->
                                        Modifier.clip(appIconMaskShape)
                                iconIsLegacy -> Modifier.clip(AppShapes.large)
                                else -> Modifier
                            }
                    val bitmapShadowShape =
                            when {
                                appIconShape == AppIconShape.CIRCLE -> appIconMaskShape
                                iconIsLegacy -> AppShapes.large
                                else -> AppShapes.large
                            }
                    Image(
                            bitmap = iconBitmap,
                            contentDescription =
                                    stringResource(
                                            R.string.desc_launch_app,
                                            appName,
                                    ),
                            modifier =
                                    Modifier.then(
                                                    if (useLightWallpaperShadow) {
                                                        Modifier.shadow(
                                                                elevation = DesignTokens.ElevationLevel2,
                                                                shape = bitmapShadowShape,
                                                                ambientColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowAmbientAlpha,
                                                                        ),
                                                                spotColor =
                                                                        Color.Black.copy(
                                                                                alpha =
                                                                                        LightWallpaperAppIconShadowSpotAlpha,
                                                                        ),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                            )
                            .size(appIconSize)
                            .then(clipModifier),
                    )
                }
            } else {
                val placeholderShape =
                        if (appIconShape == AppIconShape.CIRCLE) {
                            appIconMaskShape
                        } else {
                            AppShapes.large
                        }
                Box(
                        modifier =
                                Modifier.size(appIconSize)
                                        .clip(placeholderShape)
                                        .background(
                                                if (showWallpaperBackground) {
                                                    colorScheme.surface.copy(alpha = AppColors.TintStrongAlpha)
                                                } else {
                                                    colorScheme.surfaceVariant.copy(alpha = AppColors.ContentHighAlpha)
                                                },
                                        ),
                )
            }
            AppNotificationBadge(
                    count = notificationCount,
                    showCount = showNotificationCount,
                    modifier = Modifier.padding(top = badgeInset, end = badgeInset),
            )
        }
    }
}

/** Whether app search results wrap long app names instead of cutting them off. */
internal val LocalShowFullAppNames = compositionLocalOf { false }

@Composable
internal fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
        showFullName: Boolean = false,
) {
    val labelColor = homeTextColor()
    Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))
    Text(
            text = rememberQueryHighlightedText(appName),
            style = if (isOverlayPresentation) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = labelColor,
            minLines = 1,
            maxLines = if (showFullName) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}

/** Draws the content with all colour saturation removed. */
private fun Modifier.greyscale(): Modifier =
        drawWithContent {
            val paint =
                    Paint().apply {
                        colorFilter =
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(size.toRect(), paint)
                drawContent()
                canvas.restore()
            }
        }
