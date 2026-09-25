package com.tk.quicksearch.search.searchScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
) {
    val context = LocalContext.current
            AnimatedVisibility(
                    visible =
                            keyboardSwitchText != null ||
                                    shouldShowPhoneCallAction ||
                                    detectedAlarmTime != null ||
                                    detectedTimerSeconds != null ||
                                    detectedReminderSchedule != null,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                top = DesignTokens.SpacingSmall,
                                                bottom = DesignTokens.SpacingSmall,
                                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (keyboardSwitchText != null) {
                        KeyboardSwitchPill(
                                text = keyboardSwitchText,
                                onClick = onKeyboardSwitchToggle,
                        )
                    }
                    if (shouldShowPhoneCallAction) {
                        Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        PhoneCallPill(
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
                        if (keyboardSwitchText != null || shouldShowPhoneCallAction) {
                            Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        }
                        SetAlarmPill(
                                onClick = {
                                    if (!SetAlarmHandler.launchSetAlarm(context, detectedAlarmTime)) {
                                        android.widget.Toast.makeText(
                                                        context,
                                                        context.getString(R.string.set_alarm_no_clock_app),
                                                        android.widget.Toast.LENGTH_SHORT,
                                                )
                                                .show()
                                    }
                                },
                        )
                    }
                    if (detectedTimerSeconds != null) {
                        if (keyboardSwitchText != null ||
                                        shouldShowPhoneCallAction ||
                                        detectedAlarmTime != null
                        ) {
                            Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        }
                        StartTimerPill(
                                onClick = {
                                    val started =
                                            StartTimerHandler.launchStartTimer(
                                                    context,
                                                    detectedTimerSeconds,
                                            )
                                    android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(
                                                            if (started) {
                                                                R.string.start_timer_started
                                                            } else {
                                                                R.string.set_alarm_no_clock_app
                                                            }
                                                    ),
                                                    android.widget.Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                },
                        )
                        Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        SetAlarmPill(
                                onClick = {
                                    val alarmTime =
                                            StartTimerHandler.alarmTimeFor(detectedTimerSeconds)
                                    if (!SetAlarmHandler.launchSetAlarm(context, alarmTime)) {
                                        android.widget.Toast.makeText(
                                                        context,
                                                        context.getString(R.string.set_alarm_no_clock_app),
                                                        android.widget.Toast.LENGTH_SHORT,
                                                )
                                                .show()
                                    }
                                },
                        )
                        Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        CreateReminderPill(
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
                                    )
                                },
                        )
                    }
                    if (detectedReminderSchedule != null) {
                        if (keyboardSwitchText != null ||
                                        shouldShowPhoneCallAction ||
                                        detectedAlarmTime != null ||
                                        detectedTimerSeconds != null
                        ) {
                            Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        }
                        CreateReminderPill(
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
                                    )
                                },
                        )
                    }
                }
            }

}
