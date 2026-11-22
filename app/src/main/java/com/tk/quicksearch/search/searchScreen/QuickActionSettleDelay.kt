package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser
import kotlinx.coroutines.delay

/** How long the query must stay unchanged before quick-action pills first appear. */
private const val QUICK_ACTION_SETTLE_DELAY_MS = 600L

internal data class QuickActionInputs(
    val keyboardSwitchText: String?,
    val shouldShowPhoneCallAction: Boolean,
    val detectedAlarmTime: java.time.LocalTime?,
    val detectedTimerSeconds: Int?,
    val detectedReminderSchedule: ReminderNaturalLanguageParser.Schedule?,
) {
    val hasAny: Boolean
        get() =
            keyboardSwitchText != null ||
                shouldShowPhoneCallAction ||
                detectedAlarmTime != null ||
                detectedTimerSeconds != null ||
                detectedReminderSchedule != null

    companion object {
        val None = QuickActionInputs(null, false, null, null, null)
    }
}

/**
 * Holds quick-action pills back until the user pauses typing, so they don't pop in on every
 * keystroke. Once the row is showing it follows the query immediately, and it hides as soon as
 * nothing applies. [showImmediately] skips the wait (e.g. the user switched keyboards themselves).
 */
@Composable
internal fun rememberSettledQuickActions(
    query: String,
    raw: QuickActionInputs,
    showImmediately: Boolean,
): QuickActionInputs {
    var settled by remember { mutableStateOf(showImmediately && raw.hasAny) }
    val hasAny = raw.hasAny
    LaunchedEffect(query, hasAny, showImmediately) {
        when {
            !hasAny -> settled = false
            showImmediately || settled -> settled = true
            else -> {
                delay(QUICK_ACTION_SETTLE_DELAY_MS)
                settled = true
            }
        }
    }
    return if (settled || (showImmediately && hasAny)) raw else QuickActionInputs.None
}
