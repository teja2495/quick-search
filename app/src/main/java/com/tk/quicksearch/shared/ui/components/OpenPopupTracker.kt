package com.tk.quicksearch.shared.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView

/**
 * Counts the menus, sheets, and dialogs open over a surface, so its own chrome (such as the Open
 * Keyboard pill) can step aside while one covers it. The shared popup wrappers report themselves.
 */
@Stable
class OpenPopupTracker(
    private val onFirstPopupOpened: () -> Unit = {},
    private val onLastPopupClosed: () -> Unit = {},
) {
    private var openCount by mutableIntStateOf(0)

    val hasOpenPopup: Boolean
        get() = openCount > 0

    internal fun opened() {
        if (openCount == 0) onFirstPopupOpened()
        openCount++
    }

    internal fun closed() {
        if (openCount == 0) return
        openCount--
        if (openCount == 0) onLastPopupClosed()
    }
}

/** The tracker for the surface hosting this composition, or null where nothing listens. */
val LocalOpenPopupTracker = staticCompositionLocalOf<OpenPopupTracker?> { null }

/** Reports a popup as open while [active]; call from the host composition, beside the popup. */
@Composable
fun ReportOpenPopupEffect(active: Boolean = true) {
    val tracker = LocalOpenPopupTracker.current ?: return
    DisposableEffect(tracker, active) {
        if (!active) return@DisposableEffect onDispose {}
        tracker.opened()
        onDispose { tracker.closed() }
    }
}

/**
 * A tracker for a search surface that also keeps the keyboard as it was before a popup opened.
 *
 * Menus, sheets, and dialogs take window focus, which closes the keyboard. When the last one
 * closes, the search window is `stateAlwaysVisible`, so a still-focused search field would raise
 * the keyboard again even if it was closed before. When the first popup opens with the keyboard
 * closed, this drops the field's focus so closing the popup leaves it closed; an open keyboard
 * keeps its focus and comes back as before. A hardware keyboard keeps the focus so typing still
 * works. While the keyboard is away for a popup, the surface keeps its keyboard layout
 * ([HostImeInsetsHold]) so it doesn't reflow behind the popup.
 */
@Composable
fun rememberSearchSurfacePopupTracker(): OpenPopupTracker {
    val view = LocalView.current
    val imeInsets = WindowInsets.ime
    val density by rememberUpdatedState(LocalDensity.current)
    val hasHardwareKeyboard by rememberUpdatedState(
        LocalConfiguration.current.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO,
    )
    val imeHold = remember(view) { HostImeInsetsHold(view) }
    DisposableEffect(imeHold) { onDispose { imeHold.dispose() } }
    return remember(view, imeInsets, imeHold) {
        OpenPopupTracker(
            onFirstPopupOpened = {
                // Read now, before the popup's window starts hiding the keyboard.
                val keyboardOpen = imeInsets.getBottom(density) > 0
                imeHold.onPopupOpened()
                if (!keyboardOpen && !hasHardwareKeyboard) {
                    // Outside the effect that reported the popup, so focus isn't changed mid-apply.
                    view.post { view.clearFocus() }
                }
            },
            onLastPopupClosed = imeHold::onPopupsClosed,
        )
    }
}
