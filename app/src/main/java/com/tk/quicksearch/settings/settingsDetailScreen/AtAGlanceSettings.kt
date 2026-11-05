package com.tk.quicksearch.settings.settingsDetailScreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.appNotifications.AppNotificationsSettingsRow
import com.tk.quicksearch.appNotifications.WidgetAppNotificationsSettingsRow
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.search.data.AppNotificationFilters
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.BatteryPreferences
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.data.preferences.GlanceItemSettings
import com.tk.quicksearch.customInfo.CustomInfoActivity
import com.tk.quicksearch.customInfo.CustomInfoItem
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.customInfo.customInfoRepeatLabel
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.data.preferences.UpcomingAlarmPreferences
import com.tk.quicksearch.search.data.preferences.WidgetGlanceItemSettings
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.AliasPill
import com.tk.quicksearch.settings.shared.SettingsNestedCheckbox
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** One At a Glance toggle; [searchText] is what the page's search bar matches against. */
private class GlanceToggle(
    val searchText: String,
    val row: @Composable (isFirstItem: Boolean, isLastItem: Boolean) -> Unit,
)

/** The toggles that need notification access, each with its own permission request. */
private val NOTIFICATION_ITEMS =
    listOf(
        GlanceItem.NOW_PLAYING,
        GlanceItem.ONGOING_CALL,
        GlanceItem.OTP_CODES,
        GlanceItem.MISSED_CALLS,
        GlanceItem.WEATHER,
        GlanceItem.WORKOUTS,
        GlanceItem.TIMERS,
        GlanceItem.PROGRESS,
    )

/** A runtime permission behind a toggle: whether it is granted, and a request that turns the toggle on once it is. */
private class PermissionGate(
    val hasAccess: Boolean,
    val request: () -> Unit,
)

/**
 * Tracks [permission] across resumes. While a request is in flight (the dialog or a trip to app
 * settings), [onGranted] runs once the user comes back with the permission granted.
 */
@Composable
private fun rememberPermissionGate(
    permission: String,
    onGranted: () -> Unit,
): PermissionGate {
    val context = LocalContext.current
    val onGrantedState = rememberUpdatedState(onGranted)
    val isGranted = {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    var hasAccess by remember { mutableStateOf(isGranted()) }
    var pendingEnable by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasAccess = isGranted()
                    if (pendingEnable && hasAccess) {
                        pendingEnable = false
                        onGrantedState.value()
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            hasAccess = results[permission] == true
            if (hasAccess) {
                pendingEnable = false
                onGrantedState.value()
            } else {
                // Keep waiting only if the denial sent the user to app settings to grant it there.
                pendingEnable =
                    PermissionHelper.handleDeniedRuntimePermission(
                        context = context,
                        permission = permission,
                        wasPreviouslyDenied = true,
                    )
            }
        }
    return PermissionGate(hasAccess = hasAccess) {
        pendingEnable = true
        PermissionHelper.requestRuntimePermissionOrOpenSettings(
            context = context,
            permission = permission,
            wasPreviouslyDenied = false,
            runtimeLauncher = launcher,
        )
    }
}

/**
 * Toggles for the glanceable rows shown on the home screen, filtered by [searchQuery] from the
 * page's bottom search bar. Give [modifier] a bounded height; the toggles card scrolls within it.
 *
 * With [widgetItems], the toggles edit an At a Glance widget's own items instead: changes go to
 * [onWidgetItemsChange], and items the widget hasn't changed show the app's setting. Media, which
 * has its own widget, and the app-wide actions (editing Custom Info, App notifications' apps,
 * hidden alarm apps) are left out.
 */
@Composable
fun AtAGlanceSettingsSection(
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    widgetItems: Map<String, Boolean>? = null,
    onWidgetItemsChange: (Map<String, Boolean>) -> Unit = {},
    widgetAppNotificationFilters: AppNotificationFilters? = null,
    onWidgetAppNotificationFiltersChange: (AppNotificationFilters) -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val isWidget = widgetItems != null
    val appSettings = remember(context) { AppGlanceItemSettings(appContext) }
    val settings: GlanceItemSettings =
        if (widgetItems != null) WidgetGlanceItemSettings(widgetItems, appSettings, onWidgetItemsChange) else appSettings
    val alarmPreferences = remember(context) { UpcomingAlarmPreferences(appContext) }
    val customInfoRepository = remember(context) { CustomInfoRepository(appContext) }
    val customInfoChange by CustomInfoRepository.changes.collectAsState()
    var customInfoItems by remember { mutableStateOf(emptyList<CustomInfoItem>()) }
    var itemToDelete by remember { mutableStateOf<CustomInfoItem?>(null) }
    LaunchedEffect(customInfoRepository, customInfoChange) {
        customInfoItems = withContext(Dispatchers.IO) { customInfoRepository.all() }
    }
    var shown by remember(widgetItems) { mutableStateOf(GlanceItem.entries.associateWith(settings::isShown)) }
    val settingsState = rememberUpdatedState(settings)

    fun isShown(item: GlanceItem) = shown.getValue(item)

    fun setShown(
        item: GlanceItem,
        value: Boolean,
    ) {
        shown = shown + (item to value)
        settingsState.value.setShown(item, value)
    }
    var hiddenAlarmPackages by remember { mutableStateOf(alarmPreferences.getHiddenPackages()) }
    var showHiddenAlarmAppsDialog by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember {
        mutableStateOf(NotificationDotsPermission.hasNotificationListenerAccess(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasNotificationAccess = NotificationDotsPermission.hasNotificationListenerAccess(context)
                    hiddenAlarmPackages = alarmPreferences.getHiddenPackages()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val calendarGate = rememberPermissionGate(Manifest.permission.READ_CALENDAR) { setShown(GlanceItem.TODAY_EVENTS, true) }
    val contactsGate = rememberPermissionGate(Manifest.permission.READ_CONTACTS) { setShown(GlanceItem.BIRTHDAYS, true) }
    val needsPermissionText = stringResource(R.string.settings_overlay_source_needs_permission)

    // These reuse the notification-listener grant already used for notification dots, so the
    // request dialog and permission bookkeeping are not duplicated for each feature.
    val notificationCheckedChanges =
        NOTIFICATION_ITEMS.associateWith { item -> rememberNotificationDotsCheckedChange { setShown(item, it) } }

    /** A toggle that needs notification access; its description gives way to the permission hint. */
    fun notificationToggle(
        item: GlanceItem,
        title: String,
        description: String,
    ) = GlanceToggle("$title $description") { isFirst, isLast ->
        val onCheckedChange = notificationCheckedChanges.getValue(item)
        SettingsToggleRow(
            title = title,
            subtitle = if (hasNotificationAccess) description else needsPermissionText,
            checked = isShown(item) && hasNotificationAccess,
            onCheckedChange = onCheckedChange,
            enabled = hasNotificationAccess,
            onDisabledClick = { onCheckedChange(true) },
            isFirstItem = isFirst,
            isLastItem = isLast,
        )
    }

    /** A toggle that needs a runtime permission tracked by [gate]. */
    fun permissionToggle(
        item: GlanceItem,
        title: String,
        description: String,
        gate: PermissionGate,
        subtitleContent: (@Composable () -> Unit)? = null,
    ) = GlanceToggle("$title $description") { isFirst, isLast ->
        SettingsToggleRow(
            title = title,
            subtitle = if (gate.hasAccess) description else needsPermissionText,
            subtitleContent = subtitleContent?.takeIf { gate.hasAccess && isShown(item) },
            checked = isShown(item) && gate.hasAccess,
            onCheckedChange = { setShown(item, it) },
            enabled = gate.hasAccess,
            onDisabledClick = gate.request,
            isFirstItem = isFirst,
            isLastItem = isLast,
        )
    }

    /** A toggle with no permission requirement. */
    fun simpleToggle(
        item: GlanceItem,
        title: String,
        description: String,
        subtitleContent: (@Composable () -> Unit)? = null,
    ) = GlanceToggle("$title $description") { isFirst, isLast ->
        SettingsToggleRow(
            title = title,
            subtitle = description,
            subtitleContent = subtitleContent?.takeIf { isShown(item) },
            checked = isShown(item),
            onCheckedChange = { setShown(item, it) },
            isFirstItem = isFirst,
            isLastItem = isLast,
        )
    }

    /** A nested checkbox under a toggle for [item]. */
    @Composable
    fun nestedCheckbox(
        item: GlanceItem,
        label: String,
    ) = SettingsNestedCheckbox(
        label = label,
        checked = isShown(item),
        onCheckedChange = { setShown(item, it) },
        modifier = Modifier.padding(top = DesignTokens.SpacingXSmall),
    )

    val alarmsTitle = stringResource(R.string.settings_at_a_glance_alarms_title)
    val appNotificationsTitle = stringResource(R.string.settings_at_a_glance_app_notifications_title)
    val appNotificationsDescription = stringResource(R.string.settings_at_a_glance_app_notifications_desc)
    val alarmsDescription = stringResource(R.string.settings_upcoming_alarm_desc)
    val hiddenAlarmAppsLabel = stringResource(R.string.settings_hidden_alarm_apps_title)
    // A Custom Info item turned off in the app never runs, so a widget can only hide those that do.
    val customInfoToggles = customInfoItems.asReversed().filter { !isWidget || it.enabled }.map { item ->
        // Next run, or the last one once a one-time item has run.
        val scheduleText = listOfNotNull(
            (item.dueMillis ?: item.lastRunMillis)?.let { millis ->
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
            },
            customInfoRepeatLabel(item.repeat),
        ).joinToString(" · ").ifBlank { null }
        GlanceToggle("${item.title} ${scheduleText.orEmpty()}") { isFirst, isLast ->
            SettingsToggleRow(
                title = item.title,
                subtitle = scheduleText,
                checked = settings.isCustomInfoShown(item),
                onCheckedChange = { enabled -> settings.setCustomInfoShown(item, enabled) },
                subtitleContent = if (isWidget) null else {
                    {
                        CustomInfoActions(
                            onEdit = {
                                context.startActivity(
                                    Intent(context, CustomInfoActivity::class.java)
                                        .putExtra(CustomInfoActivity.EXTRA_ITEM_ID, item.id),
                                )
                                @Suppress("DEPRECATION")
                                (context as? Activity)?.overridePendingTransition(
                                    R.anim.custom_info_slide_in_right,
                                    R.anim.custom_info_slide_out_left,
                                )
                            },
                            onDelete = { itemToDelete = item },
                        )
                    }
                },
                isFirstItem = isFirst,
                isLastItem = isLast,
            )
        }
    }
    val appNotificationsToggle =
        GlanceToggle("$appNotificationsTitle $appNotificationsDescription") { isFirst, isLast ->
            if (isWidget) {
                WidgetAppNotificationsSettingsRow(
                    hasNotificationAccess = hasNotificationAccess,
                    checked = isShown(GlanceItem.APP_NOTIFICATIONS),
                    onCheckedChange = { setShown(GlanceItem.APP_NOTIFICATIONS, it) },
                    widgetFilters = widgetAppNotificationFilters,
                    onFiltersChange = onWidgetAppNotificationFiltersChange,
                    isFirstItem = isFirst,
                    isLastItem = isLast,
                )
            } else {
                AppNotificationsSettingsRow(hasNotificationAccess, isFirst, isLast)
            }
        }
    val toggles =
        customInfoToggles + listOfNotNull(
            notificationToggle(
                item = GlanceItem.NOW_PLAYING,
                title = stringResource(R.string.section_media),
                description = stringResource(R.string.settings_now_playing_desc),
            ),
            permissionToggle(
                item = GlanceItem.TODAY_EVENTS,
                title = stringResource(R.string.section_calendar),
                description = stringResource(R.string.settings_at_a_glance_events_desc),
                gate = calendarGate,
                subtitleContent = {
                    nestedCheckbox(GlanceItem.TOMORROW_EVENTS, stringResource(R.string.settings_at_a_glance_tomorrow_events))
                },
            ),
            notificationToggle(
                item = GlanceItem.ONGOING_CALL,
                title = stringResource(R.string.settings_at_a_glance_ongoing_call_title),
                description = stringResource(R.string.settings_at_a_glance_ongoing_call_desc),
            ),
            notificationToggle(
                item = GlanceItem.OTP_CODES,
                title = stringResource(R.string.settings_at_a_glance_otp_title),
                description = stringResource(R.string.settings_at_a_glance_otp_desc),
            ),
            appNotificationsToggle,
            notificationToggle(
                item = GlanceItem.MISSED_CALLS,
                title = stringResource(R.string.settings_at_a_glance_missed_calls_title),
                description = stringResource(R.string.settings_at_a_glance_missed_calls_desc),
            ),
            simpleToggle(
                item = GlanceItem.DO_NOT_DISTURB,
                title = stringResource(R.string.settings_shortcut_do_not_disturb),
                description = stringResource(R.string.settings_at_a_glance_dnd_desc),
            ),
            simpleToggle(
                item = GlanceItem.AIRPLANE_MODE,
                title = stringResource(R.string.settings_shortcut_airplane),
                description = stringResource(R.string.settings_at_a_glance_airplane_desc),
            ),
            simpleToggle(
                item = GlanceItem.HOTSPOT,
                title = stringResource(R.string.settings_shortcut_hotspot),
                description = stringResource(R.string.settings_at_a_glance_hotspot_desc),
            ),
            simpleToggle(
                item = GlanceItem.WIFI_SIGN_IN,
                title = stringResource(R.string.settings_at_a_glance_wifi_sign_in_title),
                description = stringResource(R.string.settings_at_a_glance_wifi_sign_in_desc),
            ),
            simpleToggle(
                item = GlanceItem.FLASHLIGHT,
                title = stringResource(R.string.settings_at_a_glance_flashlight_title),
                description = stringResource(R.string.settings_at_a_glance_flashlight_desc),
            ),
            GlanceToggle("$alarmsTitle $alarmsDescription") { isFirst, isLast ->
                // Hidden alarm apps are shared with the home card, so a widget doesn't offer them.
                val showHiddenAppsLink = !isWidget && hiddenAlarmPackages.isNotEmpty()
                SettingsToggleRow(
                    title = alarmsTitle,
                    subtitle = if (showHiddenAppsLink) null else alarmsDescription,
                    subtitleContent = {
                        if (showHiddenAppsLink) {
                            Text(
                                text = hiddenAlarmAppsLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showHiddenAlarmAppsDialog = true },
                            )
                        }
                        if (isShown(GlanceItem.UPCOMING_ALARM)) {
                            nestedCheckbox(GlanceItem.TOMORROW_ALARM, stringResource(R.string.settings_at_a_glance_tomorrow_alarm))
                        }
                    },
                    checked = isShown(GlanceItem.UPCOMING_ALARM),
                    onCheckedChange = { setShown(GlanceItem.UPCOMING_ALARM, it) },
                    isFirstItem = isFirst,
                    isLastItem = isLast,
                )
            },
            notificationToggle(
                item = GlanceItem.WEATHER,
                title = stringResource(R.string.weather_toggle_title),
                description = stringResource(R.string.settings_at_a_glance_weather_desc),
            ),
            notificationToggle(
                item = GlanceItem.WORKOUTS,
                title = stringResource(R.string.settings_at_a_glance_workouts_title),
                description = stringResource(R.string.settings_at_a_glance_workouts_desc),
            ),
            notificationToggle(
                item = GlanceItem.TIMERS,
                title = stringResource(R.string.settings_at_a_glance_timers_title),
                description = stringResource(R.string.settings_at_a_glance_timers_desc),
            ),
            simpleToggle(
                item = GlanceItem.REMINDERS,
                title = stringResource(R.string.section_reminders),
                description = stringResource(R.string.settings_upcoming_reminders_desc),
                subtitleContent = {
                    nestedCheckbox(GlanceItem.TOMORROW_REMINDERS, stringResource(R.string.settings_at_a_glance_tomorrow_reminders))
                },
            ),
            permissionToggle(
                item = GlanceItem.BIRTHDAYS,
                title = stringResource(R.string.settings_at_a_glance_birthdays_title),
                description = stringResource(R.string.settings_at_a_glance_birthdays_desc),
                gate = contactsGate,
            ),
            notificationToggle(
                item = GlanceItem.PROGRESS,
                title = stringResource(R.string.settings_at_a_glance_progress_title),
                description = stringResource(R.string.settings_at_a_glance_progress_desc),
            ),
            simpleToggle(
                item = GlanceItem.LOW_BATTERY,
                title = stringResource(R.string.settings_at_a_glance_low_battery_title),
                description =
                    stringResource(R.string.settings_low_battery_desc, BatteryPreferences.LOW_BATTERY_THRESHOLD_PERCENT),
            ),
            simpleToggle(
                item = GlanceItem.CHARGING,
                title = stringResource(R.string.settings_at_a_glance_charging_title),
                description = stringResource(R.string.settings_at_a_glance_charging_desc),
            ),
            simpleToggle(
                item = GlanceItem.LOW_STORAGE,
                title = stringResource(R.string.settings_at_a_glance_storage_title),
                description =
                    stringResource(
                        R.string.settings_at_a_glance_storage_desc,
                        GlancePreferences.LOW_STORAGE_THRESHOLD_PERCENT,
                    ),
            ),
        )
    val query = searchQuery.trim()
    val visibleToggles =
        if (query.isEmpty()) toggles else toggles.filter { it.searchText.contains(query, ignoreCase = true) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        // A widget's drawer says what it is for in its own title.
        if (!isWidget) {
            Text(
                text = stringResource(R.string.settings_at_a_glance_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
            )
        }
        if (visibleToggles.isEmpty()) {
            Text(
                text = stringResource(R.string.widget_custom_buttons_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.SpacingLarge),
            )
        } else {
            // The page itself doesn't scroll: the card takes the height left above the search bar
            // and scrolls its rows inside, shrinking to fit when they don't fill it.
            SettingsCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    visibleToggles.forEachIndexed { index, toggle ->
                        toggle.row(index == 0, index == visibleToggles.lastIndex)
                    }
                }
            }
        }
    }

    if (showHiddenAlarmAppsDialog) {
        HiddenAlarmAppsDialog(
            packageNames = hiddenAlarmPackages,
            onUnhide = { packageName ->
                hiddenAlarmPackages = alarmPreferences.unhidePackage(packageName)
                AtAGlanceWidgetUpdater.startIfPlaced(appContext)
                if (hiddenAlarmPackages.isEmpty()) showHiddenAlarmAppsDialog = false
            },
            onDismiss = { showHiddenAlarmAppsDialog = false },
        )
    }
    itemToDelete?.let { item ->
        AppAlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.custom_info_delete_confirm_title, item.title)) },
            text = { Text(stringResource(R.string.custom_info_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    CustomInfoScheduler.cancel(context, item.id)
                    customInfoRepository.delete(item.id)
                    itemToDelete = null
                }) {
                    Text(stringResource(R.string.dialog_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

/** Uses the same compact search and add action layout as Notes settings. */
@Composable
fun AtAGlanceSettingsBottomBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onNavigateToApiKeySetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAiKey by remember { mutableStateOf(false) }
    var showApiKeyRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(preferences) {
        hasAiKey = withContext(Dispatchers.IO) { preferences.hasAnyLlmApiKey() }
    }
    DisposableEffect(lifecycleOwner, preferences) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    hasAiKey = withContext(Dispatchers.IO) { preferences.hasAnyLlmApiKey() }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CalendarEventsBottomBar(
        query = query,
        onQueryChange = onQueryChange,
        onClear = onClear,
        onNewEvent = {
            if (hasAiKey) {
                context.startActivity(Intent(context, CustomInfoActivity::class.java))
                @Suppress("DEPRECATION")
                (context as? Activity)?.overridePendingTransition(R.anim.custom_info_slide_in_right, R.anim.custom_info_slide_out_left)
            } else {
                showApiKeyRequiredDialog = true
            }
        },
        newItemLabelResId = R.string.custom_info_title,
        modifier = modifier,
    )

    if (showApiKeyRequiredDialog) {
        AiApiKeyRequiredDialog(
            onDismiss = { showApiKeyRequiredDialog = false },
            onSetupKey = onNavigateToApiKeySetup,
        )
    }
}

/** Edit and Delete pills under a Custom Info toggle. */
@Composable
private fun CustomInfoActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = DesignTokens.SpacingXSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        AliasPill(
            text = AnnotatedString(stringResource(R.string.settings_edit_label)),
            textColor = AppColors.LinkColor,
            leadingIcon = MaterialSymbols.Edit,
            onClick = onEdit,
        )
        AliasPill(
            text = AnnotatedString(stringResource(R.string.dialog_delete)),
            textColor = MaterialTheme.colorScheme.error,
            leadingIcon = MaterialSymbols.Delete,
            onClick = onDelete,
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
