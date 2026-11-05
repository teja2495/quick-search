package com.tk.quicksearch.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color

/**
 * Opacity of the Home/widgets wallpaper drawn behind the page switch. It fades out while Settings
 * slides in (and back in while Settings slides out) so the translucent Settings background does not
 * show the wallpaper during the slide and then jump when the wallpaper is removed.
 */
@Composable
internal fun Transition<RootDestination>.sharedBackdropAlpha(): State<Float> =
    animateFloat(
        transitionSpec = { tween(NAVIGATION_ANIMATION_DURATION_MS) },
        label = "SharedBackdropAlpha",
    ) { destination -> if (destination == RootDestination.Settings) 0f else 1f }

/**
 * Root background behind every page. With the launcher's System wallpaper source, Home leaves it
 * transparent so the system wallpaper shows; other pages cover it. Animated so the wallpaper fades
 * along with the page slide instead of being covered at its first frame.
 */
@Composable
internal fun animatedRootBackgroundColor(
    allowSystemWallpaperBackdrop: Boolean,
    showsSystemWallpaper: Boolean,
): Color {
    val background = MaterialTheme.colorScheme.background
    // Transparent stays the background hue so the fade does not pass through black.
    val target =
        if (allowSystemWallpaperBackdrop && !showsSystemWallpaper) background else background.copy(alpha = 0f)
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(NAVIGATION_ANIMATION_DURATION_MS),
        label = "RootBackgroundColor",
    ).value
}
