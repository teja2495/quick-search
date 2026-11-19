package com.tk.quicksearch.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Counts the menus, sheets, and dialogs open over a surface, so its own chrome (such as the Open
 * Keyboard pill) can step aside while one covers it. The shared popup wrappers report themselves.
 */
@Stable
class OpenPopupTracker {
    private var openCount by mutableIntStateOf(0)

    val hasOpenPopup: Boolean
        get() = openCount > 0

    internal fun opened() {
        openCount++
    }

    internal fun closed() {
        openCount = (openCount - 1).coerceAtLeast(0)
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
