package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.search.searchScreen.components.CreateReminderPill
import com.tk.quicksearch.search.searchScreen.components.KeyboardSwitchPill
import com.tk.quicksearch.search.searchScreen.components.PhoneCallPill
import com.tk.quicksearch.search.searchScreen.components.SetAlarmPill
import com.tk.quicksearch.search.searchScreen.components.StartTimerPill
import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.setAlarm.SetAlarmHandler
import com.tk.quicksearch.tools.setAlarm.StartTimerHandler
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser
import java.util.Locale

@Composable
internal fun SearchQuickActionPills(
    query: String,
    keyboardSwitchText: String?,
    shouldShowPhoneCallAction: Boolean,
    detectedAlarmTime: java.time.LocalTime?,
    detectedTimerSeconds: Int?,
    detectedReminderSchedule: ReminderNaturalLanguageParser.Schedule?,
    onKeyboardSwitchToggle: () -> Unit,
    /** Clears the query once an alarm, timer, or reminder has actually been set. */
    onQuickActionCompleted: () -> Unit,
    topPadding: Dp = DesignTokens.SpacingMedium,
    bottomPadding: Dp = DesignTokens.SpacingMedium,
    /** Pulls the row up into padding the content above already provides, tightening the gap. */
    topOverlap: Dp = 0.dp,
) {
    val context = LocalContext.current
    val visible =
            keyboardSwitchText != null ||
                    shouldShowPhoneCallAction ||
                    detectedAlarmTime != null ||
                    detectedTimerSeconds != null ||
                    detectedReminderSchedule != null
    // While the row animates out, keep drawing the pills it last showed instead of an empty padded row.
    val current =
            QuickActionPillContent(
                    keyboardSwitchText,
                    shouldShowPhoneCallAction,
                    detectedAlarmTime,
                    detectedTimerSeconds,
                    detectedReminderSchedule,
            )
    val lastShown = remember { LastShownPills(current) }
    if (visible) lastShown.content = current
    val shown = lastShown.content
    val keyboardSwitchText = shown.keyboardSwitchText
    val shouldShowPhoneCallAction = shown.shouldShowPhoneCallAction
    val detectedAlarmTime = shown.detectedAlarmTime
    val detectedTimerSeconds = shown.detectedTimerSeconds
    val detectedReminderSchedule = shown.detectedReminderSchedule
    val buttonCount =
            listOf(
                    keyboardSwitchText != null,
                    shouldShowPhoneCallAction,
                    detectedAlarmTime != null,
                    detectedAlarmTime != null,
                    detectedTimerSeconds != null,
                    detectedTimerSeconds != null,
                    detectedTimerSeconds != null,
                    detectedReminderSchedule != null,
            ).count { it }
    // Three or more buttons drop their icons so the labels keep a readable size on one row.
    val showIcons = buttonCount < 3
            // The row's height (padding included) animates with the fade so the space it leaves
            // closes in step with the pills rather than snapping shut after them.
            AnimatedVisibility(
                    visible = visible,
                    // Outside the visibility clip so the overlap is not cut off while it animates.
                    modifier =
                            Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val overlap = topOverlap.roundToPx().coerceIn(0, placeable.height)
                                layout(placeable.width, placeable.height - overlap) {
                                    placeable.place(0, -overlap)
                                }
                            },
                    enter =
                            fadeIn(animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) +
                                    expandVertically(
                                            expandFrom = Alignment.Top,
                                            animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing),
                                    ),
                    exit =
                            fadeOut(animationSpec = tween(AppMotion.DurationExit, easing = AppMotion.EmphasizedAccelerateEasing)) +
                                    shrinkVertically(
                                            shrinkTowards = Alignment.Top,
                                            animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing),
                                    ),
            ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = DesignTokens.ContentHorizontalPadding,
                                                end = DesignTokens.ContentHorizontalPadding,
                                                top = topPadding,
                                                bottom = bottomPadding,
                                        ),
                        horizontalArrangement =
                                Arrangement.spacedBy(DesignTokens.SpacingSmall, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (keyboardSwitchText != null) {
                        KeyboardSwitchPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
text = keyboardSwitchText,
                                onClick = onKeyboardSwitchToggle,
                        )
                    }
                    if (shouldShowPhoneCallAction) {
                        PhoneCallPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
onClick = {
                                    context.startActivity(
                                            Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${Uri.encode(query)}")
                                            },
                                    )
                                },
                        )
                    }
                    if (detectedAlarmTime != null) {
                        SetAlarmPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
onClick = {
                                    if (SetAlarmHandler.launchSetAlarm(context, detectedAlarmTime)) {
                                        onQuickActionCompleted()
                                    } else {
                                        AppMessages.show(context, context.getString(R.string.set_alarm_no_clock_app))
                                    }
                                },
                        )
                        CreateReminderPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
useShortLabel = true,
                                onClick = {
                                    val now = java.time.LocalDateTime.now()
                                    val today = now.toLocalDate().atTime(detectedAlarmTime)
                                    val next = if (today.isAfter(now)) today else today.plusDays(1)
                                    ReminderEditorRequests.openNew(
                                            initialDateTimeMillis =
                                                    next.atZone(java.time.ZoneId.systemDefault())
                                                            .toInstant()
                                                            .toEpochMilli(),
                                            initialAllDay = false,
                                            // A bare time has no reminder title; start in the title field.
                                            autoFocusTitle = true,
                                            onCreated = onQuickActionCompleted,
                                    )
                                },
                        )
                    }
                    if (detectedTimerSeconds != null) {
                        StartTimerPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
onClick = {
                                    val started =
                                            StartTimerHandler.launchStartTimer(
                                                    context,
                                                    detectedTimerSeconds,
                                            )
                                    AppMessages.show(context, context.getString(
                                                            if (started) {
                                                                R.string.start_timer_started
                                                            } else {
                                                                R.string.set_alarm_no_clock_app
                                                            }
                                                    ))
                                    if (started) onQuickActionCompleted()
                                },
                        )
                        SetAlarmPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
onClick = {
                                    val alarmTime =
                                            StartTimerHandler.alarmTimeFor(detectedTimerSeconds)
                                    if (SetAlarmHandler.launchSetAlarm(context, alarmTime)) {
                                        onQuickActionCompleted()
                                    } else {
                                        AppMessages.show(context, context.getString(R.string.set_alarm_no_clock_app))
                                    }
                                },
                        )
                        CreateReminderPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
useShortLabel = true,
                                onClick = {
                                    ReminderEditorRequests.openNew(
                                            initialDateTimeMillis =
                                                    System.currentTimeMillis() +
                                                            detectedTimerSeconds * 1000L,
                                            initialAllDay = false,
                                            // Duration-only queries have no reminder title, so open directly
                                            // into the title field for immediate typing.
                                            autoFocusTitle = true,
                                            onCreated = onQuickActionCompleted,
                                    )
                                },
                        )
                    }
                    if (detectedReminderSchedule != null) {
                        CreateReminderPill(
                                modifier = Modifier.weight(1f, fill = false),
                                showIcon = showIcons,
onClick = {
                                    val schedule = detectedReminderSchedule
                                    val dateTime = schedule.date.atTime(
                                            schedule.time ?: java.time.LocalTime.MIDNIGHT,
                                    )
                                    ReminderEditorRequests.openNew(
                                            initialTitle = schedule.title.replaceFirstChar { first ->
                                                if (first.isLowerCase()) {
                                                    first.titlecase(Locale.getDefault())
                                                } else {
                                                    first.toString()
                                                }
                                            },
                                            initialDateTimeMillis = dateTime
                                                    .atZone(java.time.ZoneId.systemDefault())
                                                    .toInstant()
                                                    .toEpochMilli(),
                                            initialAllDay = schedule.time == null,
                                            autoFocusTitle = false,
                                            initialRepeat = schedule.repeat,
                                            onCreated = onQuickActionCompleted,
                                    )
                                },
                        )
                    }
                }
            }

}

private data class QuickActionPillContent(
    val keyboardSwitchText: String?,
    val shouldShowPhoneCallAction: Boolean,
    val detectedAlarmTime: java.time.LocalTime?,
    val detectedTimerSeconds: Int?,
    val detectedReminderSchedule: ReminderNaturalLanguageParser.Schedule?,
)

private class LastShownPills(var content: QuickActionPillContent)
