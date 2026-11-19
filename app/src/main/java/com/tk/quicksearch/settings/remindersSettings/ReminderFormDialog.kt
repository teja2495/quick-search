package com.tk.quicksearch.settings.remindersSettings

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppDatePickerDialog
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.tk.quicksearch.shared.ui.components.PickerFieldButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

internal data class FormDateTimeSuggestion(
    val dateTimeMillis: Long,
    val hasTime: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderFormDialog(
    initialTitle: String,
    initialDateTimeMillis: Long?,
    initialAllDay: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateTimeMillis: Long, allDay: Boolean) -> Unit,
    titleResId: Int,
    confirmResId: Int,
    extraActions: @Composable () -> Unit,
    noticeAboveNameResId: Int? = null,
    autoFocusTitle: Boolean = true,
    nameHintResId: Int = R.string.calendar_create_event_name_hint,
    titleKeyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    titleDateTimeSuggestion: ((String) -> FormDateTimeSuggestion?)? = null,
    titleVisualTransformation: VisualTransformation = VisualTransformation.None,
    titleMaxLines: Int = 1,
    showTitleInput: Boolean = true,
    requireExplicitDateOrTime: Boolean = false,
    /** Extra fields shown below the time button, e.g. a reminder's repeat rule. */
    belowTime: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    var reminderTitle by remember { mutableStateOf(initialTitle) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateOrTimeChosen by remember { mutableStateOf(initialDateTimeMillis != null) }

    val initialUtcMillis = remember(initialDateTimeMillis) {
        initialDateTimeMillis?.let { localMidnightToUtcMidnight(it) }
            ?: System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    val selectedDateMillis = datePickerState.selectedDateMillis

    val initialHour = remember(initialDateTimeMillis, initialAllDay) {
        if (!initialAllDay && initialDateTimeMillis != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
            cal.get(Calendar.HOUR_OF_DAY)
        } else 9
    }
    val initialMinute = remember(initialDateTimeMillis, initialAllDay) {
        if (!initialAllDay && initialDateTimeMillis != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
            cal.get(Calendar.MINUTE)
        } else 0
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    // hasTime drives allDay: if user added a time, allDay = false
    var hasTime by remember { mutableStateOf(!initialAllDay) }

    val canSave = (!showTitleInput || reminderTitle.isNotBlank()) && selectedDateMillis != null &&
        (!requireExplicitDateOrTime || dateOrTimeChosen)

    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocusTitle) {
        if (autoFocusTitle && showTitleInput) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    when {
        showDatePicker -> {
            AppDatePickerDialog(
                state = datePickerState,
                onDismiss = { showDatePicker = false },
                onConfirm = { dateOrTimeChosen = true; showDatePicker = false },
            )
        }

        showTimePicker -> {
            AppAlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    AppTextButton(onClick = {
                        hasTime = true
                        dateOrTimeChosen = true
                        showTimePicker = false
                    }) {
                        Text(stringResource(R.string.dialog_save))
                    }
                },
                dismissButton = {
                    AppTextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
                text = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        TimePicker(state = timePickerState)
                    }
                },
            )
        }

        else -> {
            AppAlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(titleResId),
                            modifier = Modifier.weight(1f),
                        )
                        extraActions()
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                        noticeAboveNameResId?.let { resId ->
                            Text(
                                text = stringResource(resId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (showTitleInput) AppOutlinedTextField(
                            value = reminderTitle,
                            onValueChange = { value ->
                                reminderTitle = value
                                titleDateTimeSuggestion?.invoke(value)?.let { suggestion ->
                                    val dateTime = Instant.ofEpochMilli(suggestion.dateTimeMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime()
                                    datePickerState.selectedDateMillis = localMidnightToUtcMidnight(
                                        dateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    )
                                    if (suggestion.hasTime) {
                                        timePickerState.hour = dateTime.hour
                                        timePickerState.minute = dateTime.minute
                                        hasTime = true
                                    }
                                }
                            },
                            label = stringResource(nameHintResId),
                            singleLine = titleMaxLines == 1,
                            maxLines = titleMaxLines,
                            keyboardOptions = KeyboardOptions(capitalization = titleKeyboardCapitalization),
                            visualTransformation = titleVisualTransformation,
                            modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
                            shape = AppShapes.extraSmall,
                        )

                        PickerFieldButton(
                            icon = MaterialSymbols.CalendarMonth,
                            value =
                                if (selectedDateMillis != null) {
                                    formatPickedDate(selectedDateMillis)
                                } else {
                                    stringResource(R.string.calendar_create_event_select_date)
                                },
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (hasTime) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PickerFieldButton(
                                    icon = MaterialSymbols.Schedule,
                                    value = formatPickedTime(timePickerState.hour, timePickerState.minute),
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.weight(1f),
                                )
                                AppIconButton(onClick = { hasTime = false }, tooltip = stringResource(R.string.common_action_clear)) {
                                    Icon(
                                        imageVector = MaterialSymbols.Close,
                                        contentDescription = stringResource(R.string.common_action_clear),
                                    )
                                }
                            }
                        } else {
                            PickerFieldButton(
                                icon = MaterialSymbols.Schedule,
                                value = stringResource(R.string.common_time),
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        belowTime()
                    }
                },
                confirmButton = {
                    AppTextButton(
                        onClick = {
                            val utcDateMillis = selectedDateMillis ?: return@AppTextButton
                            val localDayMillis = utcMidnightToLocalMidnight(utcDateMillis)
                            val dateTimeMillis = if (hasTime) {
                                localDayMillis +
                                    timePickerState.hour * 60L * 60L * 1000L +
                                    timePickerState.minute * 60L * 1000L
                            } else {
                                localDayMillis
                            }
                            onConfirm(reminderTitle.trim(), dateTimeMillis, !hasTime)
                        },
                        enabled = canSave,
                    ) {
                        Text(stringResource(confirmResId))
                    }
                },
                dismissButton = {
                    AppTextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}

private fun formatPickedDate(utcMillis: Long): String {
    val localMillis = utcMidnightToLocalMidnight(utcMillis)
    return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(localMillis))
}

@Composable
private fun formatPickedTime(hour: Int, minute: Int): String {
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(cal.time)
}

private fun utcMidnightToLocalMidnight(utcMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneId.of("UTC"))
        .toLocalDate()
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun localMidnightToUtcMidnight(localMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(localMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
