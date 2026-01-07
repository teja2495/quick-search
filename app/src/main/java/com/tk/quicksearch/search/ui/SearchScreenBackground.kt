package com.tk.quicksearch.search.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchScreenBackground(
    showWallpaperBackground: Boolean,
    wallpaperBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    // Check if we're in dark mode by checking the background color luminance
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkMode = remember(backgroundColor) {
        // Calculate relative luminance (0 = black, 1 = white)
        // Using the standard formula: 0.299*R + 0.587*G + 0.114*B
        val luminance = backgroundColor.red * 0.299f + backgroundColor.green * 0.587f + backgroundColor.blue * 0.114f
        luminance < 0.5f
    }

    // Track if the animation has already played (only animate first time)
    var hasAnimated by remember { mutableStateOf(false) }
    val shouldAnimate = showWallpaperBackground && wallpaperBitmap != null && !hasAnimated

    // Animate fade-in only the first time wallpaper background becomes visible
    val wallpaperAlpha = animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else if (showWallpaperBackground && wallpaperBitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "wallpaperFadeIn"
    ) { hasAnimated = true }

    Box(modifier = modifier.fillMaxSize()) {
        // Blurred wallpaper background (only if enabled)
        if (showWallpaperBackground) {
            wallpaperBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 20.dp)
                        .graphicsLayer(alpha = wallpaperAlpha.value),
                    contentScale = ContentScale.FillBounds
                )

                // Dark overlay in dark mode
                if (isDarkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .graphicsLayer(alpha = wallpaperAlpha.value)
                    )
                } else {
                    // Light overlay in light mode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                            .graphicsLayer(alpha = wallpaperAlpha.value)
                    )
                }
            }
        }

        // Fallback background if wallpaper is disabled or not available
        if (!showWallpaperBackground || wallpaperBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}
