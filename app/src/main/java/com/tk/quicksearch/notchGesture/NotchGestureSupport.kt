package com.tk.quicksearch.notchGesture

import android.os.Build

/** Notch gestures need the display cutout API from Android 10; the feature is hidden below that. */
object NotchGestureSupport {
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
