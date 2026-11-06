package com.tk.quicksearch.app

import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.tk.quicksearch.shared.util.isDefaultHomeApp

/**
 * Default-launcher entry point for the home gesture.
 *
 * This is a thin subclass of [MainActivity] that lets the manifest bind a
 * Home-specific theme to the `CATEGORY_HOME` intent filter. That theme and
 * [WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER] let Android composite the
 * system wallpaper behind transparent app content.
 *
 * [MainActivity] itself keeps its translucent theme for non-launcher launches
 * (share intents, web search, ASSIST, app-icon taps from another launcher,
 * etc.) where seeing through to the previous app is desirable.
 *
 * The app only exposes the transparent backdrop when this activity is the
 * active default Home app and System wallpaper is the selected source.
 */
class HomeActivity : MainActivity() {
    final override var canShowSystemWallpaperBackdrop: Boolean = false
        private set

    private var suppressNextResumeAutoFocus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        canShowSystemWallpaperBackdrop = isDefaultHomeApp()
        if (canShowSystemWallpaperBackdrop) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.setFormat(PixelFormat.TRANSPARENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.decorView.setBackgroundColor(Color.TRANSPARENT)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        // A Home gesture while Home is already on screen arrives here with the activity only
        // paused (not stopped). Treat it as "dismiss the keyboard" instead of a no-op. Returning
        // Home from another app finds the activity stopped and keeps its normal keyboard behavior.
        val isHomeGestureWhileVisible =
            intent.action == Intent.ACTION_MAIN &&
                intent.hasCategory(Intent.CATEGORY_HOME) &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        val isKeyboardVisible =
            ViewCompat.getRootWindowInsets(window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        super.onNewIntent(intent)
        if (isHomeGestureWhileVisible && isKeyboardVisible) {
            // The search field re-requests focus and the keyboard on ON_RESUME; skip that once so
            // the keyboard and the focus go away together.
            suppressNextResumeAutoFocus = true
            dismissKeyboard()
        }
    }

    /** True once per Home gesture that dismissed the keyboard; the caller skips its resume focus. */
    fun consumeResumeAutoFocusSuppression(): Boolean {
        val suppress = suppressNextResumeAutoFocus
        suppressNextResumeAutoFocus = false
        return suppress
    }

    override fun onPause() {
        super.onPause()
        // Drop an unconsumed suppression (auto-focus off) so it can't swallow a later resume.
        suppressNextResumeAutoFocus = false
    }

    private fun dismissKeyboard() {
        // The window is stateAlwaysVisible, so the field must lose focus or the IME comes back.
        currentFocus?.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.ime())
    }

    /** Back at the launcher root must not reveal the previously used app. */
    override fun handleSearchBackPressed() = Unit
}
