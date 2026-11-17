package com.tk.quicksearch.app.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.onboarding.LocalSearchSurfaceHidden
import com.tk.quicksearch.onboarding.WelcomeMorphPhase
import com.tk.quicksearch.onboarding.WelcomeMorphState

/**
 * Drawn at about 1/255: invisible under the setup pages, yet still drawn, so icons and wallpaper
 * are already on the GPU when the welcome reveals the screen.
 */
private const val HiddenAlpha = 1f / 255f

/**
 * Hosts the search surface under the onboarding pages. While not [visible] it stays composed but
 * unseen, untouchable, silent to accessibility, deaf to system back and unable to raise the
 * keyboard, so the final setup steps warm it up and finishing setup only has to reveal it. While
 * the welcome runs it fades in under the setup page's fading background.
 */
@Composable
internal fun SearchSurfaceLayer(
    composed: Boolean,
    visible: Boolean,
    welcomeMorph: WelcomeMorphState,
    content: @Composable () -> Unit,
) {
    if (!composed) return
    val hidden = !visible && !welcomeMorph.isActive
    val lifecycleOwner = LocalLifecycleOwner.current
    val inertBack =
        remember(lifecycleOwner) {
            object : OnBackPressedDispatcherOwner {
                override val onBackPressedDispatcher = OnBackPressedDispatcher()
                override val lifecycle: Lifecycle get() = lifecycleOwner.lifecycle
            }
        }
    val backOwner = LocalOnBackPressedDispatcherOwner.current?.takeUnless { hidden } ?: inertBack
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Per draw, not offscreen: a full-screen offscreen layer would cost every frame.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        alpha =
                            when {
                                welcomeMorph.phase == WelcomeMorphPhase.Exiting ||
                                    welcomeMorph.phase == WelcomeMorphPhase.Landing -> welcomeMorph.curtain
                                hidden -> HiddenAlpha
                                else -> 1f
                            }
                    }.then(if (hidden) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            CompositionLocalProvider(
                LocalOnBackPressedDispatcherOwner provides backOwner,
                LocalSearchSurfaceHidden provides hidden,
            ) {
                content()
            }
        }
        if (hidden) {
            // Pages above leave gaps; nothing may reach the hidden search screen through them.
            Box(
                modifier =
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
            )
        }
    }
}
