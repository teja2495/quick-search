package com.tk.quicksearch.search.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
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
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationDot
import com.tk.quicksearch.search.core.AppIconShape
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppIconSurface(
        iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
        iconIsLegacy: Boolean,
        monochromeData: androidx.compose.ui.graphics.ImageBitmap? = null,
        appName: String,
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
        showPinnedIndicator: Boolean = false,
        showNotificationDot: Boolean = false,
        folderPreviewMember: AppFolderMember? = null,
) {
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
    val themedIconContainerShape = CircleShape
    val pinnedIndicatorInset = (appIconSurfaceSize - appIconSize) / 2

    Surface(
            modifier = Modifier.requiredSize(appIconSurfaceSize).then(gestureModifier),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = DesignTokens.ShapeLarge,
    ) {
        val clickModifier =
                if (!clickGesturesEnabled) {
                    Modifier
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                            onClick = {
                                hapticConfirm(view)()
                                onClick()
                            },
                            onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable {
                        hapticConfirm(view)()
                        onClick()
                    }
                }
        Box(
                modifier = Modifier.fillMaxSize().then(clickModifier),
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
                                appIconShape == AppIconShape.CIRCLE ->
                                        Modifier.clip(CircleShape)
                                iconIsLegacy -> Modifier.clip(DesignTokens.ShapeLarge)
                                else -> Modifier
                            }
                    val bitmapShadowShape =
                            when {
                                appIconShape == AppIconShape.CIRCLE -> CircleShape
                                iconIsLegacy -> DesignTokens.ShapeLarge
                                else -> DesignTokens.ShapeLarge
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
                            CircleShape
                        } else {
                            DesignTokens.ShapeLarge
                        }
                Box(
                        modifier =
                                Modifier.size(appIconSize)
                                        .clip(placeholderShape)
                                        .background(
                                                if (showWallpaperBackground) {
                                                    colorScheme.surface.copy(alpha = 0.28f)
                                                } else {
                                                    colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                },
                                        ),
                )
            }
            if (showPinnedIndicator) {
                Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        modifier =
                                Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = pinnedIndicatorInset, end = pinnedIndicatorInset)
                                        .size(12.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }
            AppNotificationDot(
                    visible = showNotificationDot,
                    modifier =
                            if (showPinnedIndicator) {
                                Modifier.padding(top = pinnedIndicatorInset + 12.dp, end = pinnedIndicatorInset)
                            } else {
                                Modifier.padding(top = pinnedIndicatorInset, end = pinnedIndicatorInset)
                            },
            )
        }
    }
}

@Composable
internal fun AppLabelText(
        appName: String,
        isOverlayPresentation: Boolean,
) {
    val labelColor = homeTextColor()
    Spacer(
            modifier =
                    Modifier.height(
                            if (isOverlayPresentation) {
                                4.dp
                            } else {
                                DesignTokens.SpacingXSmall
                            },
                    ),
    )
    Text(
            text = rememberQueryHighlightedText(appName),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
    )
}
