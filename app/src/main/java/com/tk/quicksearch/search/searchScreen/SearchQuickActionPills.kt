package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.R
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
) {
    val context = LocalContext.current
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
            AnimatedVisibility(
                    visible =
                            keyboardSwitchText != null ||
                                    shouldShowPhoneCallAction ||
                                    detectedAlarmTime != null ||
                                    detectedTimerSeconds != null ||
                                    detectedReminderSchedule != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
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
