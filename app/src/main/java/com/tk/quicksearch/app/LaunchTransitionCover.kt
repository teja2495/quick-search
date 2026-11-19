package com.tk.quicksearch.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppThemeColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.ui.theme.AppMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainActivity's window is translucent, so Android keeps the app underneath (another launcher)
 * visible until the opening transition finishes. Without wallpaper access the System wallpaper
 * backdrop leaves the root transparent, and that app showed through the search screen. The root
 * is covered with the Monochrome theme until the enter animation completes, then fades to the backdrop.
 */
internal class LaunchTransitionCover(private val enabled: Boolean) {
    private var entered by mutableStateOf(!enabled)
    private var fallbackJob: Job? = null

    fun onStart(scope: CoroutineScope) {
        if (!enabled) return
        entered = false
        fallbackJob?.cancel()
        // Not every return to the activity reports onEnterAnimationComplete; never hold the cover.
        fallbackJob = scope.launch {
            delay(MAX_COVER_MS)
            entered = true
        }
    }

    fun onEnterAnimationComplete() {
        fallbackJob?.cancel()
        entered = true
    }

    /**
     * Root background while the System wallpaper backdrop is shown: the Monochrome theme
     * gradient covers the launch, then slowly dissolves into the wallpaper.
     */
    @Composable
    fun backdropModifier(): Modifier {
        val isDarkMode = LocalAppIsDarkTheme.current
        val themeBrush =
            remember(isDarkMode) {
                Brush.linearGradient(
                    colors = AppThemeColors(theme = AppTheme.MONOCHROME, isDarkMode = isDarkMode),
                    start = Offset.Zero,
                    end = Offset(GRADIENT_END_X, GRADIENT_END_Y),
                )
            }
        val scrimBrush = Brush.verticalGradient(AppColors.ThemeFallbackGradientScrimColors)
        val alpha = remember { Animatable(if (entered) 0f else 1f) }
        LaunchedEffect(entered) {
            if (entered) {
                alpha.animateTo(0f, tween(AppMotion.DurationExtraLong, easing = AppMotion.EmphasizedEasing))
            } else {
                alpha.snapTo(1f)
            }
        }
        return Modifier.drawBehind {
            // Read the flag directly so a restart is covered from its first frame.
            val coverAlpha = if (entered) alpha.value else 1f
            if (coverAlpha > 0f) {
                drawRect(themeBrush, alpha = coverAlpha)
                drawRect(scrimBrush, alpha = coverAlpha)
            }
        }
    }

    private companion object {
        const val MAX_COVER_MS = 800L
        const val GRADIENT_END_X = 1800f
        const val GRADIENT_END_Y = 2200f
    }
}
