package com.tk.quicksearch.util

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
inline fun hapticConfirm(view: View): () -> Unit =
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
inline fun hapticToggle(view: View): () -> Unit =
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
inline fun hapticStrong(view: View): () -> Unit =
    {
        performHapticFeedbackSafely(
            view,
            HapticFeedbackConstantsCompat.LONG_PRESS,
        )
    }
