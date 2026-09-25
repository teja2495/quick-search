package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.search.data.preferences.BatteryPreferences
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.preferences.MediaPreferences
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.data.preferences.UpcomingAlarmPreferences
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Toggles for the glanceable cards shown on the home screen: media, today's events, alarm, reminders and low battery. */
@Composable
fun AtAGlanceSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val calendarPreferences = remember(context) { CalendarPreferences(context.applicationContext) }
    val alarmPreferences = remember(context) { UpcomingAlarmPreferences(context.applicationContext) }
    val reminderPreferences = remember(context) { ReminderPreferences(context.applicationContext) }
    val mediaPreferences = remember(context) { MediaPreferences(context.applicationContext) }
    val batteryPreferences = remember(context) { BatteryPreferences(context.applicationContext) }
    var showTodayEvents by remember { mutableStateOf(calendarPreferences.getShowTodayEvents()) }
    var showUpcomingAlarm by remember { mutableStateOf(alarmPreferences.isShowUpcomingAlarmEnabled()) }
    var hiddenAlarmPackages by remember { mutableStateOf(alarmPreferences.getHiddenPackages()) }
    var showHiddenAlarmAppsDialog by remember { mutableStateOf(false) }
    var showUpcomingReminders by remember {
        mutableStateOf(reminderPreferences.isShowUpcomingRemindersEnabled())
    }
    var showNowPlaying by remember { mutableStateOf(mediaPreferences.isShowNowPlayingEnabled()) }
    var showLowBattery by remember { mutableStateOf(batteryPreferences.isShowLowBatteryEnabled()) }
    var hasMediaAccess by remember {
        mutableStateOf(NotificationDotsPermission.hasNotificationListenerAccess(context))
    }
    var hasCalendarAccess by remember { mutableStateOf(PermissionHelper.checkCalendarPermission(context)) }
    // Set while the calendar grant is in flight (dialog or system settings), so the toggle turns on
    // once the user comes back with the permission granted.
    var pendingCalendarEnable by remember { mutableStateOf(false) }
    val enableTodayEvents = {
        pendingCalendarEnable = false
        showTodayEvents = true
        calendarPreferences.setShowTodayEvents(true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasMediaAccess = NotificationDotsPermission.hasNotificationListenerAccess(context)
                    hasCalendarAccess = PermissionHelper.checkCalendarPermission(context)
                    hiddenAlarmPackages = alarmPreferences.getHiddenPackages()
                    if (pendingCalendarEnable && hasCalendarAccess) enableTodayEvents()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val calendarPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            hasCalendarAccess = results[Manifest.permission.READ_CALENDAR] == true
            if (hasCalendarAccess) {
                enableTodayEvents()
            } else {
                // Keep waiting only if the denial sent the user to app settings to grant it there.
                pendingCalendarEnable =
                    PermissionHelper.handleDeniedRuntimePermission(
                        context = context,
                        permission = Manifest.permission.READ_CALENDAR,
                        wasPreviouslyDenied = true,
                    )
            }
        }
    val requestCalendarPermission = {
        pendingCalendarEnable = true
        PermissionHelper.requestRuntimePermissionOrOpenSettings(
            context = context,
            permission = Manifest.permission.READ_CALENDAR,
            wasPreviouslyDenied = false,
            runtimeLauncher = calendarPermissionLauncher,
        )
    }
    val needsPermissionText = stringResource(R.string.settings_overlay_source_needs_permission)

    // Reuses the notification-listener grant already used for notification dots, so the request
    // dialog and permission bookkeeping are not duplicated for a second feature.
    val onShowNowPlayingCheckedChange =
        rememberNotificationDotsCheckedChange { enabled ->
            showNowPlaying = enabled
            mediaPreferences.setShowNowPlayingEnabled(enabled)
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Text(
            text = stringResource(R.string.settings_at_a_glance_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
        )
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                title = stringResource(R.string.section_media),
                subtitle =
                    if (hasMediaAccess) stringResource(R.string.settings_now_playing_desc) else needsPermissionText,
                checked = showNowPlaying && hasMediaAccess,
                onCheckedChange = onShowNowPlayingCheckedChange,
                enabled = hasMediaAccess,
                onDisabledClick = { onShowNowPlayingCheckedChange(true) },
                isFirstItem = true,
                isLastItem = false,
            )
            SettingsToggleRow(
                title = stringResource(R.string.section_calendar),
                subtitle =
                    if (hasCalendarAccess) {
                        stringResource(R.string.settings_at_a_glance_events_desc)
                    } else {
                        needsPermissionText
                    },
                checked = showTodayEvents && hasCalendarAccess,
                onCheckedChange = { enabled ->
                    showTodayEvents = enabled
                    calendarPreferences.setShowTodayEvents(enabled)
                },
                enabled = hasCalendarAccess,
                onDisabledClick = requestCalendarPermission,
                isFirstItem = false,
                isLastItem = false,
            )
            SettingsToggleRow(
                title = stringResource(R.string.settings_at_a_glance_alarms_title),
                subtitle =
                    if (hiddenAlarmPackages.isEmpty()) stringResource(R.string.settings_upcoming_alarm_desc) else null,
                subtitleContent =
                    if (hiddenAlarmPackages.isEmpty()) {
                        null
                    } else {
                        {
                            Text(
                                text = stringResource(R.string.settings_hidden_alarm_apps_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showHiddenAlarmAppsDialog = true },
                            )
                        }
                    },
                checked = showUpcomingAlarm,
                onCheckedChange = { enabled ->
                    showUpcomingAlarm = enabled
                    alarmPreferences.setShowUpcomingAlarmEnabled(enabled)
                },
                isFirstItem = false,
                isLastItem = false,
            )
            SettingsToggleRow(
                title = stringResource(R.string.section_reminders),
                subtitle = stringResource(R.string.settings_upcoming_reminders_desc),
                checked = showUpcomingReminders,
                onCheckedChange = { enabled ->
                    showUpcomingReminders = enabled
                    reminderPreferences.setShowUpcomingRemindersEnabled(enabled)
                },
                isFirstItem = false,
                isLastItem = false,
            )
            SettingsToggleRow(
                title = stringResource(R.string.settings_at_a_glance_low_battery_title),
                subtitle = stringResource(R.string.settings_low_battery_desc, BatteryPreferences.LOW_BATTERY_THRESHOLD_PERCENT),
                checked = showLowBattery,
                onCheckedChange = { enabled ->
                    showLowBattery = enabled
                    batteryPreferences.setShowLowBatteryEnabled(enabled)
                },
                isFirstItem = false,
                isLastItem = true,
            )
        }
    }

    if (showHiddenAlarmAppsDialog) {
        HiddenAlarmAppsDialog(
            packageNames = hiddenAlarmPackages,
            onUnhide = { packageName ->
                hiddenAlarmPackages = alarmPreferences.unhidePackage(packageName)
                if (hiddenAlarmPackages.isEmpty()) showHiddenAlarmAppsDialog = false
            },
            onDismiss = { showHiddenAlarmAppsDialog = false },
        )
    }
}

/** Lists the apps whose alarms were hidden from At a Glance, each with an Unhide action. */
@Composable
private fun HiddenAlarmAppsDialog(
    packageNames: Set<String>,
    onUnhide: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val apps =
        remember(packageNames, context) {
            val packageManager = context.packageManager
            packageNames
                .map { packageName ->
                    val label =
                        runCatching {
                            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
                        }.getOrDefault(packageName)
                    packageName to label
                }.sortedBy { (_, label) -> label.lowercase() }
        }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_hidden_alarm_apps_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                apps.forEach { (packageName, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onUnhide(packageName) }) {
                            Text(text = stringResource(R.string.action_include_generic))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        },
    )
}
