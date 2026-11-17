package com.tk.quicksearch.app.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.onboarding.LocalSearchSurfaceHidden
import com.tk.quicksearch.onboarding.WelcomeMorphPhase
import com.tk.quicksearch.onboarding.WelcomeMorphState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Drawn at about 1/255: invisible under the setup pages, yet still drawn, so icons and wallpaper
 * are already on the GPU when the welcome reveals the screen.
 */
private const val HiddenAlpha = 1f / 255f

/**
 * Two short pulses per setup step and after search initialization warm icons and textures.
 * Between pulses the hidden layer is skipped while the setup page animates.
 */
private const val WarmFrames = 2
private const val WarmPulseIntervalMs = 1000L

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
    warmupStep: AppScreen,
    searchInitializing: Flow<Boolean>,
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
    val hiddenAlpha = remember { mutableFloatStateOf(HiddenAlpha) }
    LaunchedEffect(hidden, lifecycleOwner, warmupStep, searchInitializing) {
        if (!hidden) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            searchInitializing.collectLatest {
                try {
                    repeat(2) { pulse ->
                        if (pulse > 0) delay(WarmPulseIntervalMs)
                        hiddenAlpha.floatValue = HiddenAlpha
                        repeat(WarmFrames) { withFrameNanos {} }
                        hiddenAlpha.floatValue = 0f
                    }
                } finally {
                    hiddenAlpha.floatValue = 0f
                }
            }
        }
    }
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
                                hidden -> hiddenAlpha.floatValue
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
