package com.tk.quicksearch.shared.ui.components

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import androidx.annotation.RequiresApi

/**
 * Holds the keyboard insets a search surface sees at their height from when a popup opened, until
 * the keyboard is back after the popup closes.
 *
 * A popup window takes focus, which hides the keyboard while it is open and shows it again when it
 * closes. The host window isn't the keyboard's target during that time, so it gets the change as a
 * single jump instead of an animation, and the whole search layout reflows from its keyboard layout
 * to its keyboard-closed layout (and back) behind the popup. Holding the insets keeps the content
 * still: the keyboard slides away from and back over a layout that doesn't move.
 *
 * Installed on the parent of the Compose view, so its insets listener and animation callback rewrite
 * the keyboard insets before Compose reads them. Needs API 30 for the platform insets dispatch that
 * passes a parent's rewritten insets to its children; it does nothing on older versions.
 */
internal class HostImeInsetsHold(private val composeView: View) {
    private val impl: Impl? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (composeView.parent as? View)?.let { Impl(it) }
        } else {
            null
        }

    /** Starts holding when the keyboard is open now; call as the first popup opens. */
    fun onPopupOpened() {
        impl?.onPopupOpened()
    }

    /** Releases the hold once the keyboard is back, or soon if it doesn't come back. */
    fun onPopupsClosed() {
        impl?.onPopupsClosed()
    }

    fun dispose() {
        impl?.dispose()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private class Impl(private val target: View) {
        private var heldIme: android.graphics.Insets? = null
        private var popupOpen = false
        private var imeAnimating = false
        private var lastRealInsets: WindowInsets? = null
        private val releaseFallback = Runnable { if (!imeAnimating) release() }

        init {
            target.setOnApplyWindowInsetsListener { view, insets ->
                lastRealInsets = insets
                if (heldIme != null && !popupOpen && !imeAnimating && insets.isVisible(WindowInsets.Type.ime())) {
                    // The keyboard is back without an animation, so there's nothing left to cover.
                    heldIme = null
                    target.removeCallbacks(releaseFallback)
                }
                view.onApplyWindowInsets(withHeldIme(insets))
            }
            target.setWindowInsetsAnimationCallback(
                object : WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onPrepare(animation: WindowInsetsAnimation) {
                        if (animation.isIme) imeAnimating = true
                    }

                    override fun onProgress(
                        insets: WindowInsets,
                        runningAnimations: MutableList<WindowInsetsAnimation>,
                    ): WindowInsets = withHeldIme(insets)

                    override fun onEnd(animation: WindowInsetsAnimation) {
                        if (!animation.isIme) return
                        imeAnimating = false
                        if (heldIme != null && !popupOpen) release()
                    }
                },
            )
        }

        fun onPopupOpened() {
            popupOpen = true
            target.removeCallbacks(releaseFallback)
            if (heldIme != null) return
            val insets = target.rootWindowInsets ?: return
            if (insets.isVisible(WindowInsets.Type.ime())) {
                heldIme = insets.getInsets(WindowInsets.Type.ime())
            }
        }

        fun onPopupsClosed() {
            popupOpen = false
            if (heldIme == null) return
            val realImeVisible = lastRealInsets?.isVisible(WindowInsets.Type.ime()) == true
            if (realImeVisible && !imeAnimating) {
                release()
            } else {
                // The keyboard normally starts coming back right away; if it doesn't, stop holding.
                target.postDelayed(releaseFallback, RELEASE_FALLBACK_MILLIS)
            }
        }

        fun dispose() {
            target.removeCallbacks(releaseFallback)
            target.setOnApplyWindowInsetsListener(null)
            target.setWindowInsetsAnimationCallback(null)
            if (heldIme != null) {
                heldIme = null
                target.requestApplyInsets()
            }
        }

        private fun release() {
            target.removeCallbacks(releaseFallback)
            if (heldIme == null) return
            heldIme = null
            target.requestApplyInsets()
        }

        private fun withHeldIme(insets: WindowInsets): WindowInsets {
            val held = heldIme ?: return insets
            return WindowInsets.Builder(insets)
                .setInsets(WindowInsets.Type.ime(), held)
                .setVisible(WindowInsets.Type.ime(), true)
                .build()
        }

        private val WindowInsetsAnimation.isIme: Boolean
            get() = typeMask and WindowInsets.Type.ime() != 0
    }

    private companion object {
        const val RELEASE_FALLBACK_MILLIS = 700L
    }
}
