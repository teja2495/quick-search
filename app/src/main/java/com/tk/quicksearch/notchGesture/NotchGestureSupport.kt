package com.tk.quicksearch.notchGesture

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

/**
 * Notch gestures need the display cutout API from Android 10 and a screen that actually has a
 * notch or camera cutout; the feature is hidden otherwise.
 */
object NotchGestureSupport {
    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasDisplayCutout(context)

    /**
     * Whether the default display has a cutout on any edge. Checking every edge keeps the answer
     * the same in landscape, where the overlay itself only binds to a cutout on the top edge.
     */
    private fun hasDisplayCutout(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val display =
            context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
                ?: return false
        val cutout = runCatching { display.cutout }.getOrNull() ?: return false
        return cutout.boundingRects.any { rect -> !rect.isEmpty }
    }
}
