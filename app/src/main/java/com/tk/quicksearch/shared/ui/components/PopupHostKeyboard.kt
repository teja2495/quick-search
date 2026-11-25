package com.tk.quicksearch.shared.ui.components

import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/*
 * A popup window takes focus, which closes a keyboard the host had open. Popups placed relative to
 * the keyboard would open above it and then ride it down as it closes. These helpers place a popup
 * where it ends up from its first frame instead, and the keyboard slides away below it. Once that
 * keyboard is gone, or the popup's own text field takes it, the popup follows the keyboard as usual.
 */

/** Whether the host's keyboard is open as the popup opens. Call from the host composition. */
@Composable
internal fun rememberHostKeyboardOpen(): Boolean {
    val hostView = LocalView.current
    return remember {
        ViewCompat.getRootWindowInsets(hostView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }
}

/**
 * Safe-drawing padding for a popup's own content in an edge-to-edge dialog window
 * (`decorFitsSystemWindows = false`), leaving out the host's keyboard while it closes.
 */
@Composable
internal fun Modifier.safeDrawingPaddingIgnoringHostKeyboard(hostKeyboardOpen: Boolean): Modifier {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var ignoreKeyboard by remember { mutableStateOf(hostKeyboardOpen) }
    if (ignoreKeyboard) {
        LaunchedEffect(ime, density) {
            // The dialog window may get its first insets after it is shown: wait to see the host's
            // keyboard, unless it is already gone by then, and then for it to finish closing.
            withTimeoutOrNull(HostKeyboardArrivalTimeoutMillis) {
                snapshotFlow { ime.getBottom(density) > 0 }.first { it }
            }
            snapshotFlow { ime.getBottom(density) == 0 }.first { it }
            ignoreKeyboard = false
        }
        return windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
    }
    return safeDrawingPadding()
}

/**
 * For a dialog window the platform resizes for the keyboard (`decorFitsSystemWindows = true`):
 * keeps it from resizing while the host's keyboard closes, then restores its soft input mode. Call
 * from inside the dialog's content.
 */
@Composable
internal fun DialogWindowIgnoresHostKeyboardEffect(hostKeyboardOpen: Boolean) {
    if (!hostKeyboardOpen || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val dialogView = LocalView.current
    val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return
    var restored by remember { mutableStateOf(false) }
    if (restored) return
    DisposableEffect(window) {
        val originalMode = window.attributes.softInputMode
        window.setSoftInputMode(
            (originalMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
        )
        onDispose { window.setSoftInputMode(originalMode) }
    }
    LaunchedEffect(window) {
        val imm = dialogView.context.getSystemService(InputMethodManager::class.java)
        fun imeVisible() =
            window.decorView.rootWindowInsets?.isVisible(android.view.WindowInsets.Type.ime()) == true
        fun ownFieldHasKeyboard() = imm?.isActive(dialogView) == true
        // Checked once a frame: the platform resizes this window itself, so Compose sees no insets.
        val start = SystemClock.uptimeMillis()
        while (!imeVisible() && SystemClock.uptimeMillis() - start < HostKeyboardArrivalTimeoutMillis) {
            withFrameNanos {}
        }
        while (imeVisible() && !ownFieldHasKeyboard() &&
            SystemClock.uptimeMillis() - start < HostKeyboardCloseTimeoutMillis
        ) {
            withFrameNanos {}
        }
        restored = true
    }
}

private const val HostKeyboardArrivalTimeoutMillis = 500L

/** Upper bound in case the keyboard stays up for something other than the popup's own field. */
private const val HostKeyboardCloseTimeoutMillis = 1_500L
