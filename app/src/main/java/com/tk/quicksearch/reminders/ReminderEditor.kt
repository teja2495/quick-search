package com.tk.quicksearch.reminders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.settings.settingsDetailScreen.ReminderFormDialog
import com.tk.quicksearch.settings.settingsDetailScreen.FormDateTimeSuggestion
import com.tk.quicksearch.shared.permissions.PermissionHelper
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which reminder dialog is open. Shared so any surface (search, Home, settings) can open it. */
sealed interface ReminderEditorRequest {
    data class New(
        val initialTitle: String = "",
        val initialDateTimeMillis: Long? = null,
        val initialAllDay: Boolean = true,
        val autoFocusTitle: Boolean = true,
    ) : ReminderEditorRequest

    data class Edit(val reminder: ReminderInfo) : ReminderEditorRequest
}

object ReminderEditorRequests {
    private val _request = MutableStateFlow<ReminderEditorRequest?>(null)
    val request: StateFlow<ReminderEditorRequest?> = _request.asStateFlow()

    fun openNew(
        initialTitle: String = "",
        initialDateTimeMillis: Long? = null,
        initialAllDay: Boolean = true,
        autoFocusTitle: Boolean = true,
    ) {
        _request.value = ReminderEditorRequest.New(
            initialTitle,
            initialDateTimeMillis,
            initialAllDay,
            autoFocusTitle,
        )
    }

    fun openEdit(reminder: ReminderInfo) {
        _request.value = ReminderEditorRequest.Edit(reminder)
    }

    fun close() {
        _request.value = null
    }
}

/** Shows the New/Edit Reminder dialog whenever [ReminderEditorRequests] asks for it. */
@Composable
fun ReminderEditorHost() {
    val request by ReminderEditorRequests.request.collectAsState()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val context = LocalContext.current
    val repository = remember(context) { ReminderRepository(context) }
    // Registered before the gate so the permission result still arrives while the prompt pauses us.
    val requestPermissionsIfFirst = rememberFirstReminderPermissionFlow()
    // Both the main and overlay activities host this. The overlay can be translucent, leaving the
    // activity below it STARTED, so only the RESUMED one may show the dialog.
    if (!lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return

    when (val current = request) {
        null -> Unit
        is ReminderEditorRequest.New ->
            ReminderFormDialog(
                reminder = null,
                initialTitle = current.initialTitle,
                initialDateTimeMillis = current.initialDateTimeMillis,
                initialAllDay = current.initialAllDay,
                autoFocusTitle = current.autoFocusTitle,
                onDismiss = ReminderEditorRequests::close,
                onSave = { title, dateTimeMillis, allDay ->
                    ReminderEditorRequests.close()
                    val (date, timeMinutes) = toReminderSchedule(dateTimeMillis, allDay)
                    repository.createReminder(title, date, timeMinutes)
                    requestPermissionsIfFirst()
                },
                onDelete = null,
            )
        is ReminderEditorRequest.Edit -> {
            // Re-read so the dialog shows the latest values (e.g. after a snooze).
            val reminder = remember(current) {
                repository.getReminderById(current.reminder.reminderId) ?: current.reminder
            }
            ReminderFormDialog(
                reminder = reminder,
                initialTitle = reminder.title,
                initialDateTimeMillis = if (reminder.hasTime) reminder.dueMillis else reminder.dayStartMillis,
                initialAllDay = !reminder.hasTime,
                autoFocusTitle = false,
                onDismiss = ReminderEditorRequests::close,
                onSave = { title, dateTimeMillis, allDay ->
                    ReminderEditorRequests.close()
                    val (date, timeMinutes) = toReminderSchedule(dateTimeMillis, allDay)
                    repository.updateReminder(reminder.reminderId, title, date, timeMinutes)
                },
                onDelete = {
                    ReminderEditorRequests.close()
                    repository.deleteReminder(reminder.reminderId)
                },
            )
        }
    }
}

@Composable
private fun ReminderFormDialog(
    reminder: ReminderInfo?,
    initialTitle: String,
    initialDateTimeMillis: Long?,
    initialAllDay: Boolean,
    autoFocusTitle: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, dateTimeMillis: Long, allDay: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val titleVisualTransformation = remember(accentColor) {
        ReminderNaturalLanguageVisualTransformation(accentColor)
    }
    ReminderFormDialog(
        initialTitle = initialTitle,
        initialDateTimeMillis = initialDateTimeMillis,
        initialAllDay = initialAllDay,
        onDismiss = onDismiss,
        onConfirm = { title, dateTimeMillis, allDay ->
            onSave(
                ReminderNaturalLanguageParser.parse(title)?.title ?: title,
                dateTimeMillis,
                allDay,
            )
        },
        titleResId = if (reminder == null) R.string.reminder_new_title else R.string.reminder_edit_title,
        confirmResId = R.string.dialog_save,
        extraActions = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.reminder_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        autoFocusTitle = autoFocusTitle,
        nameHintResId = R.string.reminder_name_hint,
        titleKeyboardCapitalization =
            if (reminder == null) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        titleDateTimeSuggestion = { title ->
            ReminderNaturalLanguageParser.parse(title)?.let { schedule ->
                val dateTime = schedule.date.atTime(schedule.time ?: java.time.LocalTime.MIDNIGHT)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                FormDateTimeSuggestion(dateTimeMillis = dateTime, hasTime = schedule.time != null)
            }
        },
        titleVisualTransformation = titleVisualTransformation,
        titleMaxLines = 2,
    )
}

/** The form reports local-midnight millis plus an optional time; reminders store date + minutes. */
private fun toReminderSchedule(dateTimeMillis: Long, allDay: Boolean) =
    Instant.ofEpochMilli(dateTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().let { dateTime ->
        dateTime.toLocalDate() to if (allDay) null else dateTime.hour * 60 + dateTime.minute
    }

/**
 * After the first reminder is saved, asks for notifications and then opens the "Alarms & reminders"
 * page if exact alarms are off. The reminder is already saved, so denying either is harmless.
 */
@Composable
fun rememberFirstReminderPermissionFlow(): () -> Unit {
    val context = LocalContext.current
    val preferences = remember(context) { ReminderPreferences(context) }
    val openExactAlarmSettingsIfNeeded = {
        if (!ReminderPermissions.canScheduleExactAlarms(context)) {
            ReminderPermissions.openExactAlarmSettings(context)
        }
    }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            openExactAlarmSettingsIfNeeded()
        }
    return {
        if (!preferences.hasRequestedReminderPermissions()) {
            preferences.setRequestedReminderPermissions()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ReminderPermissions.hasPostNotifications(context)
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openExactAlarmSettingsIfNeeded()
            }
        }
    }
}

/** Requests whichever reminder permission is still missing; used by the settings hint. */
@Composable
fun rememberMissingReminderPermissionRequester(onResult: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val preferences = remember(context) { ReminderPreferences(context) }
    var wasDenied by remember { mutableStateOf(false) }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Once Android stops showing the prompt, the only way left is the app's notification settings.
            val wasPreviouslyDenied = wasDenied || preferences.hasRequestedReminderPermissions()
            val shouldShowRationale =
                PermissionHelper.shouldShowRuntimePermissionRationale(
                    context = context,
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                )
            if (!granted && wasPreviouslyDenied && !shouldShowRationale) {
                ReminderPermissions.openNotificationSettings(context)
            }
            wasDenied = !granted
            onResult()
        }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderPermissions.hasPostNotifications(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!ReminderPermissions.canScheduleExactAlarms(context)) {
            ReminderPermissions.openExactAlarmSettings(context)
        }
    }
}
