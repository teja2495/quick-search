package com.tk.quicksearch.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Root background behind every screen. With the launcher's System wallpaper source, the main
 * pages leave it transparent so the system wallpaper shows (the navigation host covers Settings in
 * step with its slide); onboarding screens cover it.
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

internal enum class SwipeAnimationDirection {
    LEFT,
    RIGHT,
}

internal fun directionalNavigationTransition(
    direction: SwipeAnimationDirection,
) = when (direction) {
    SwipeAnimationDirection.LEFT ->
        slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
            initialOffsetX = { it },
        ) togetherWith
            slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
                targetOffsetX = { -it },
            )
    SwipeAnimationDirection.RIGHT ->
        slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
            initialOffsetX = { -it },
        ) togetherWith
            slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
                targetOffsetX = { it },
            )
}
