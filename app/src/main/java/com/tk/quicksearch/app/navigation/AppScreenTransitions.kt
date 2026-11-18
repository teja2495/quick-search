package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.tk.quicksearch.onboarding.WelcomeMorphState
import com.tk.quicksearch.onboarding.WelcomeMorphPhase
import com.tk.quicksearch.onboarding.WelcomeExitHoldMs

private val appScreenOrder =
    listOf(
        AppScreen.Permissions,
        AppScreen.ImportSettings,
        AppScreen.SearchEngineSetup,
        AppScreen.FinalSetup,
        AppScreen.Main,
    )

/**
 * Onboarding steps slide horizontally. Finishing setup swaps straight to the search screen, held
 * under the setup page while the welcome morph carries the setup button into the search bar.
 */
internal fun AnimatedContentTransitionScope<AppScreen>.appScreenTransform(
    welcomeMorph: WelcomeMorphState,
): ContentTransform {
    if (targetState == AppScreen.Main && welcomeMorph.phase == WelcomeMorphPhase.Landing) {
        // The setup page stays on top, untouched, while the welcome pushes it away and the search
        // screen rises underneath; it leaves once the rise is done.
        val hold = tween<Float>(WelcomeExitHoldMs, easing = { if (it < 1f) 0f else 1f })
        // No size animation: the empty Main slot would shrink and recentre the page as it leaves.
        return (EnterTransition.None togetherWith fadeOut(hold) using null).apply { targetContentZIndex = -1f }
    }
    val spec = tween<androidx.compose.ui.unit.IntOffset>(NAVIGATION_ANIMATION_DURATION_MS, easing = AppMotion.EmphasizedEasing)
    return if (appScreenOrder.indexOf(targetState) > appScreenOrder.indexOf(initialState)) {
        slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it }
    } else {
        slideInHorizontally(spec) { -it } togetherWith slideOutHorizontally(spec) { it }
    }
}
