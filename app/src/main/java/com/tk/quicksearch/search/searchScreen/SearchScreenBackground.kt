package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.ui.theme.DesignTokens

@Composable
internal fun SearchScreenBackground(
    showWallpaperBackground: Boolean,
    wallpaperBitmap: ImageBitmap?,
    wallpaperBackgroundAlpha: Float,
    wallpaperBlurRadius: Float,
    fallbackBackgroundAlpha: Float = 1f,
    useGradientFallback: Boolean = false,
    overlayGradientTheme: OverlayGradientTheme = OverlayGradientTheme.MONOCHROME,
    overlayThemeIntensity: Float = UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
    modifier: Modifier = Modifier,
) {
    // Check if we're in dark mode by checking the background color luminance
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkMode =
        remember(backgroundColor) {
            // Calculate relative luminance (0 = black, 1 = white)
            // Using the standard formula: 0.299*R + 0.587*G + 0.114*B
            val luminance =
                backgroundColor.red * DesignTokens.LuminanceRedCoefficient +
                    backgroundColor.green * DesignTokens.LuminanceGreenCoefficient +
                    backgroundColor.blue * DesignTokens.LuminanceBlueCoefficient
            luminance < DesignTokens.DarkModeLuminanceThreshold
        }

    // Track if the animation has already played (only animate first time)
    var hasAnimated by remember { mutableStateOf(false) }
    val shouldAnimate = showWallpaperBackground && wallpaperBitmap != null && !hasAnimated
    val overlayAlpha = wallpaperBackgroundAlpha.coerceIn(0f, 1f)
    val blurRadius = wallpaperBlurRadius.coerceIn(0f, UiPreferences.MAX_WALLPAPER_BLUR_RADIUS)
    val fallbackAlpha = fallbackBackgroundAlpha.coerceIn(0f, 1f)

    // Animate fade-in only the first time wallpaper background becomes visible
    val wallpaperAlpha =
        animateFloatAsState(
            targetValue =
                if (shouldAnimate) {
                    1f
                } else if (showWallpaperBackground && wallpaperBitmap != null) {
                    1f
                } else {
                    0f
                },
            animationSpec = tween(durationMillis = DesignTokens.WallpaperFadeInDuration),
            label = "wallpaperFadeIn",
        ) { hasAnimated = true }

    Box(modifier = modifier.fillMaxSize()) {
        // Blurred wallpaper background (only if enabled)
        if (showWallpaperBackground) {
            wallpaperBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .blur(radius = blurRadius.dp)
                            .graphicsLayer(alpha = wallpaperAlpha.value),
                    contentScale = ContentScale.Crop,
                )

                // Dark overlay in dark mode
                if (isDarkMode) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = overlayAlpha))
                                .graphicsLayer(alpha = wallpaperAlpha.value),
                    )
                } else {
                    // Light overlay in light mode
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = overlayAlpha))
                                .graphicsLayer(alpha = wallpaperAlpha.value),
                    )
                }
            }
        }

    // Fallback background if wallpaper is disabled or not available
        if (!showWallpaperBackground || wallpaperBitmap == null) {
            val fallbackModifier =
                if (useGradientFallback) {
                    val fallbackGradientBrush =
                        Brush.linearGradient(
                            colors =
                                overlayGradientColors(
                                    theme = overlayGradientTheme,
                                    isDarkMode = isDarkMode,
                                    alpha = fallbackAlpha,
                                    intensity = overlayThemeIntensity,
                                ),
                            start = Offset.Zero,
                            end = Offset(1800f, 2200f),
                        )
                    Modifier
                        .fillMaxSize()
                        .background(fallbackGradientBrush)
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    if (isDarkMode) {
                                        listOf(
                                            Color.Black.copy(alpha = 0.22f),
                                            Color.Black.copy(alpha = 0.12f),
                                            Color.Black.copy(alpha = 0.3f),
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.12f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.06f),
                                        )
                                    },
                            ),
                        )
                } else {
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = fallbackAlpha))
                }

            Box(
                modifier = fallbackModifier,
            )
        }
    }
}
