package com.tk.quicksearch.shared.util

import android.view.View
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * Performs haptic feedback without crashing if a device/OEM enforces vibrator permission checks.
 * Haptics are treated as best-effort UI feedback, so failures are ignored.
 */
fun performHapticFeedbackSafely(
    view: View,
    feedbackConstant: Int,
): Boolean =
    try {
        ViewCompat.performHapticFeedback(view, feedbackConstant)
    } catch (_: SecurityException) {
        false
    }

/**
 * Returns a lambda that performs haptic feedback for primary actions (buttons, launches, etc.)
 * Uses CONFIRM feedback type for clear action confirmation.
 */
fun hapticConfirm(view: View): () -> Unit =
    {
        performHapticFeedbackSafely(
            view,
            HapticFeedbackConstantsCompat.CONFIRM,
        )
    }

/**
 * Returns a lambda that performs haptic feedback for toggle switches and state changes.
 * Uses VIRTUAL_KEY feedback type for subtle state change feedback.
 */
fun hapticToggle(view: View): () -> Unit =
    {
        performHapticFeedbackSafely(
            view,
            HapticFeedbackConstantsCompat.VIRTUAL_KEY,
        )
    }

/**
 * Returns a lambda that performs stronger haptic feedback for important actions.
 * Uses LONG_PRESS feedback type for stronger feedback.
 */
fun hapticStrong(view: View): () -> Unit =
    {
        performHapticFeedbackSafely(
            view,
            HapticFeedbackConstantsCompat.LONG_PRESS,
        )
    }

/**
 * Feedback for a switch flipping, distinct for on and off (Android 14+ has dedicated patterns;
 * older versions fall back to a light click).
 */
fun hapticToggleState(
    view: View,
    checked: Boolean,
) {
    performHapticFeedbackSafely(
        view,
        if (checked) HapticFeedbackConstantsCompat.TOGGLE_ON else HapticFeedbackConstantsCompat.TOGGLE_OFF,
    )
}

/** Feedback for a drag crossing the point where releasing would commit (or crossing back). */
fun hapticGestureThreshold(
    view: View,
    activated: Boolean,
) {
    performHapticFeedbackSafely(
        view,
        if (activated) {
            HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE
        },
    )
}
