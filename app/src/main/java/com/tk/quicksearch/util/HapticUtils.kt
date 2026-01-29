package com.tk.quicksearch.util

import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * Returns a lambda that performs haptic feedback for primary actions (buttons, launches, etc.)
 * Uses CONFIRM feedback type for clear action confirmation.
 */
inline fun hapticConfirm(view: android.view.View): () -> Unit =
    {
        ViewCompat.performHapticFeedback(
            view,
            HapticFeedbackConstantsCompat.CONFIRM,
        )
    }

/**
 * Returns a lambda that performs haptic feedback for toggle switches and state changes.
 * Uses VIRTUAL_KEY feedback type for subtle state change feedback.
 */
inline fun hapticToggle(view: android.view.View): () -> Unit =
    {
        ViewCompat.performHapticFeedback(
            view,
            HapticFeedbackConstantsCompat.VIRTUAL_KEY,
        )
    }

/**
 * Returns a lambda that performs stronger haptic feedback for important actions.
 * Uses LONG_PRESS feedback type for stronger feedback.
 */
inline fun hapticStrong(view: android.view.View): () -> Unit =
    {
        ViewCompat.performHapticFeedback(
            view,
            HapticFeedbackConstantsCompat.LONG_PRESS,
        )
    }
