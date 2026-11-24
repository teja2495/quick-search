package com.tk.quicksearch.search.searchScreen.components

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.app.navigation.PageSwipeController
import com.tk.quicksearch.app.navigation.RootDestination

// TEMPORARY (debug builds only): traces where search-bar taps go while the "search bar ignores
// taps" bug is being diagnosed. Filter with `adb logcat -s QSTouch`. Remove once the cause is found.
internal const val SEARCH_TOUCH_TAG = "QSTouch"

internal fun pageSwipeDebugState(
    destination: RootDestination,
    controller: PageSwipeController,
): String =
    "dest=$destination settled=${controller.settled} target=${controller.target} progress=${controller.progress}"

internal fun searchTouchLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(SEARCH_TOUCH_TAG, message)
}

/**
 * Logs each press/release that reaches this node: once on arrival (Initial pass) and once after
 * every handler has seen it (Final pass), with whether something consumed it. Never consumes.
 */
internal fun Modifier.searchTouchDebugLog(
    name: String,
    key: Any? = null,
    extra: () -> String = { "" },
): Modifier {
    if (!BuildConfig.DEBUG) return this
    return pointerInput(name, key) {
        awaitPointerEventScope {
            while (true) {
                val arrived = awaitPointerEvent(PointerEventPass.Initial)
                val tracked = arrived.type == PointerEventType.Press || arrived.type == PointerEventType.Release
                if (tracked) {
                    val change = arrived.changes.firstOrNull()
                    Log.d(
                        SEARCH_TOUCH_TAG,
                        "$name ${arrived.type} arrive pos=${change?.position} size=$size " +
                            "consumed=${change?.isConsumed} ${extra()}",
                    )
                }
                val final = awaitPointerEvent(PointerEventPass.Final)
                if (tracked) {
                    Log.d(
                        SEARCH_TOUCH_TAG,
                        "$name ${final.type} final consumed=${final.changes.map { it.isConsumed }}",
                    )
                }
            }
        }
    }
}
