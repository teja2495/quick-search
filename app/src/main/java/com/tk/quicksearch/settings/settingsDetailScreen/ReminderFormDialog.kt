package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.calendar.calendarRecurrenceLabel
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.calendar.formatCalendarEventDate
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.settings.appShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
) {
    val context = LocalContext.current
    var reminderTitle by remember { mutableStateOf(initialTitle) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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

    val canSave = reminderTitle.isNotBlank() && selectedDateMillis != null

    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocusTitle) {
        if (autoFocusTitle) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    when {
        showDatePicker -> {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.dialog_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }

        showTimePicker -> {
            AppAlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        hasTime = true
                        showTimePicker = false
                    }) {
                        Text(stringResource(R.string.dialog_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
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
                        OutlinedTextField(
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
                            label = { Text(stringResource(nameHintResId)) },
                            singleLine = titleMaxLines == 1,
                            maxLines = titleMaxLines,
                            keyboardOptions = KeyboardOptions(capitalization = titleKeyboardCapitalization),
                            visualTransformation = titleVisualTransformation,
                            modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
                        )

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (selectedDateMillis != null) {
                                    formatPickedDate(selectedDateMillis)
                                } else {
                                    stringResource(R.string.calendar_create_event_select_date)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (hasTime) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatPickedTime(timePickerState.hour, timePickerState.minute),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { hasTime = false },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.common_time),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val utcDateMillis = selectedDateMillis ?: return@Button
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = if (canSave) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        ),
                    ) {
                        Text(stringResource(confirmResId))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
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
