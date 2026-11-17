package com.tk.quicksearch.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.tk.quicksearch.onboarding.WelcomeMorphState
import com.tk.quicksearch.onboarding.WelcomeMorphPhase

private val appScreenOrder =
    listOf(
        AppScreen.Permissions,
        AppScreen.ImportSettings,
        AppScreen.SearchEngineSetup,
        AppScreen.FinalSetup,
        AppScreen.Main,
    )

/**
 * Onboarding steps slide horizontally. Finishing setup swaps straight to the search screen under
 * the welcome morph's curtain, which then carries the setup button into the search bar.
 */
internal fun AnimatedContentTransitionScope<AppScreen>.appScreenTransform(
    welcomeMorph: WelcomeMorphState,
): ContentTransform {
    if (targetState == AppScreen.Main && welcomeMorph.phase == WelcomeMorphPhase.Landing) {
        // Swap at once: the welcome overlay's curtain covers the search screen while it settles.
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val spec = tween<androidx.compose.ui.unit.IntOffset>(NAVIGATION_ANIMATION_DURATION_MS)
    return if (appScreenOrder.indexOf(targetState) > appScreenOrder.indexOf(initialState)) {
        slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it }
    } else {
        slideInHorizontally(spec) { -it } togetherWith slideOutHorizontally(spec) { it }
    }
}
