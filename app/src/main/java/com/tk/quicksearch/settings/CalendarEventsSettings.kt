package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.tk.quicksearch.search.data.CustomCalendarEventRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.settings.AppShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
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

private data class CalendarEventGroup(
    val eventId: Long,
    val title: String,
    val nearestInstance: CalendarEventInfo,
    val instances: List<CalendarEventInfo>,
)

private val CalendarSettingsBarCornerShape = RoundedCornerShape(28.dp)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarEventsSettingsSection(
    onEventClick: (CalendarEventInfo) -> Unit,
    searchQuery: String = "",
    refreshSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val normalizedSearchQuery =
        remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val nowMillis = System.currentTimeMillis()
    val todayStartMillis = startOfTodayMillis()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val calendarRepository = remember(context) { CalendarRepository(context) }
    val calendarPreferences = remember(context) { CalendarPreferences(context) }
    val customCalendarRepository = remember(context) { CustomCalendarEventRepository(context) }
    var hasPermission by remember { mutableStateOf(calendarRepository.hasPermission()) }
    var selectedEventGroupForSheet by remember { mutableStateOf<CalendarEventGroup?>(null) }
    var editingCustomEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }
    var includePastEvents by remember { mutableStateOf(calendarPreferences.getIncludePastEvents()) }
    var localRefreshToken by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, calendarRepository) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermission = calendarRepository.hasPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val events by produceState(initialValue = emptyList(), hasPermission, refreshSignal, localRefreshToken) {
        value =
            withContext(Dispatchers.IO) {
                val deviceEvents =
                    if (!hasPermission) {
                        emptyList()
                    } else {
                        calendarRepository.getEventInstancesAroundNow(limit = 2000)
                    }
                val customEvents = customCalendarRepository.getAllCustomEvents()
                deviceEvents + customEvents
            }
    }

    val eventGroups =
        remember(events) {
            events
                .groupBy { it.eventId }
                .mapNotNull { (eventId, groupInstances) ->
                    val instances = groupInstances.sortedBy { it.startMillis }
                    val nearest = nearestInstance(instances, nowMillis) ?: return@mapNotNull null
                    CalendarEventGroup(
                        eventId = eventId,
                        title = nearest.title,
                        nearestInstance = nearest,
                        instances = instances,
                    )
                }
        }

    val filteredEventGroups =
        remember(eventGroups, normalizedSearchQuery, locale, todayStartMillis, includePastEvents) {
            if (normalizedSearchQuery.isBlank()) {
                if (includePastEvents) {
                    eventGroups
                } else {
                    eventGroups.mapNotNull { eventGroup ->
                        val nextInstance =
                            firstInstanceOnOrAfter(
                                instances = eventGroup.instances,
                                thresholdMillis = todayStartMillis,
                            ) ?: return@mapNotNull null
                        eventGroup.copy(nearestInstance = nextInstance)
                    }
                }
            } else {
                eventGroups
                    .mapNotNull { eventGroup ->
                        shortcutMatchPriority(
                            name = eventGroup.title,
                            query = normalizedSearchQuery,
                            locale = locale,
                        ) ?: return@mapNotNull null
                        eventGroup
                    }
            }
        }

    val sortedEventGroups =
        remember(filteredEventGroups, locale) {
            filteredEventGroups.sortedWith(
                compareBy<CalendarEventGroup> { it.nearestInstance.startMillis }
                    .thenBy { it.title.lowercase(locale) },
            )
        }

    // Auto-scroll to top when past events toggle changes
    LaunchedEffect(includePastEvents) {
        listState.scrollToItem(0)
    }

    // Auto-scroll to the first event on or after today
    val todayIndex = remember(sortedEventGroups, todayStartMillis) {
        sortedEventGroups.indexOfFirst { it.nearestInstance.startMillis >= todayStartMillis }
    }
    LaunchedEffect(todayIndex, sortedEventGroups.size) {
        if (todayIndex > 0 && sortedEventGroups.isNotEmpty()) {
            listState.scrollToItem(todayIndex)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = modifier) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding),
        ) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_calendar_include_past_events_title),
                subtitle = stringResource(R.string.settings_calendar_include_past_events_desc),
                checked = includePastEvents,
                onCheckedChange = { enabled ->
                    includePastEvents = enabled
                    calendarPreferences.setIncludePastEvents(enabled)
                },
                isFirstItem = true,
                isLastItem = true,
            )
        }
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!hasPermission && events.none { it.eventId < 0 }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.calendar_section_permission_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (sortedEventGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.settings_calendar_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight),
                ) {
                    itemsIndexed(
                        items = sortedEventGroups,
                        key = { _, eventGroup -> eventGroup.eventId },
                    ) { index, eventGroup ->
                        val recurrenceLabel =
                            calendarRecurrenceLabel(
                                recurrenceRule = eventGroup.nearestInstance.recurrenceRule,
                                instanceCount = eventGroup.instances.size,
                            )
                        CalendarEventManagementRow(
                            title = eventGroup.title,
                            dateLabel = formatCalendarEventDate(eventGroup.nearestInstance),
                            relativeLabel = calendarRelativeDateLabel(eventGroup.nearestInstance.startMillis),
                            recurrenceLabel = recurrenceLabel,
                            onClick = {
                                if (eventGroup.eventId < 0) {
                                    // Custom event — open edit dialog
                                    editingCustomEvent = eventGroup.nearestInstance
                                } else if (eventGroup.isRecurring()) {
                                    selectedEventGroupForSheet = eventGroup
                                } else {
                                    onEventClick(eventGroup.nearestInstance)
                                }
                            },
                        )
                        if (index < sortedEventGroups.lastIndex) {
                            HorizontalDivider(color = AppColors.SettingsDivider)
                        }
                    }
                }
            }
        }
    }

    selectedEventGroupForSheet?.let { eventGroup ->
        CalendarEventInstancesDrawer(
            title = eventGroup.title,
            nearestInstance = eventGroup.nearestInstance,
            instances = eventGroup.instances,
            onDismissRequest = { selectedEventGroupForSheet = null },
            onInstanceClick = { instance ->
                selectedEventGroupForSheet = null
                onEventClick(instance)
            },
        )
    }

    editingCustomEvent?.let { event ->
        CustomEventEditDialog(
            event = event,
            onDismiss = { editingCustomEvent = null },
            onSave = { title, dateTimeMillis, allDay ->
                editingCustomEvent = null
                customCalendarRepository.updateCustomEvent(event.eventId, title, dateTimeMillis, allDay)
                localRefreshToken++
            },
            onDelete = {
                editingCustomEvent = null
                customCalendarRepository.deleteCustomEvent(event.eventId)
                localRefreshToken++
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarEventsBottomBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onNewEvent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isSearchExpanded) {
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onClear = onClear,
                modifier = Modifier.weight(1f),
                applyDefaultPadding = false,
                applyImePadding = false,
                fillMaxWidth = false,
                focusRequester = searchFocusRequester,
            )
            FloatingActionButton(
                onClick = onNewEvent,
                modifier = Modifier.size(48.dp),
                shape = CalendarSettingsBarCornerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.calendar_create_event_cta),
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            FloatingActionButton(
                onClick = { isSearchExpanded = true },
                modifier = Modifier.size(48.dp),
                shape = CalendarSettingsBarCornerShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.desc_search_icon),
                    modifier = Modifier.size(22.dp),
                )
            }
            ExtendedFloatingActionButton(
                onClick = onNewEvent,
                expanded = true,
                modifier = Modifier.weight(1f),
                shape = CalendarSettingsBarCornerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(text = stringResource(R.string.calendar_create_event_cta)) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

// ============================================================================
// Create / Edit dialogs
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCalendarEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateTimeMillis: Long, allDay: Boolean) -> Unit,
) {
    CustomEventFormDialog(
        initialTitle = "",
        initialDateTimeMillis = null,
        initialAllDay = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        titleResId = R.string.calendar_create_event_title,
        confirmResId = R.string.dialog_save,
        extraActions = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomEventEditDialog(
    event: CalendarEventInfo,
    onDismiss: () -> Unit,
    onSave: (title: String, dateTimeMillis: Long, allDay: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    CustomEventFormDialog(
        initialTitle = event.title,
        initialDateTimeMillis = event.startMillis,
        initialAllDay = event.allDay,
        onDismiss = onDismiss,
        onConfirm = onSave,
        titleResId = R.string.calendar_edit_event_title,
        confirmResId = R.string.dialog_save,
        extraActions = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomEventFormDialog(
    initialTitle: String,
    initialDateTimeMillis: Long?,
    initialAllDay: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateTimeMillis: Long, allDay: Boolean) -> Unit,
    titleResId: Int,
    confirmResId: Int,
    extraActions: @Composable () -> Unit,
) {
    var eventTitle by remember { mutableStateOf(initialTitle) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Date picker state: pre-select the initial date if provided
    val initialUtcMillis = remember(initialDateTimeMillis) {
        initialDateTimeMillis?.let { localMidnightToUtcMidnight(it) }
            ?: System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    val selectedDateMillis = datePickerState.selectedDateMillis

    // Track whether the user has added a time
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
        is24Hour = false,
    )
    // hasTime drives allDay: if user added a time, allDay = false
    var hasTime by remember { mutableStateOf(!initialAllDay) }

    val canSave = eventTitle.isNotBlank() && selectedDateMillis != null

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
                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text(stringResource(R.string.calendar_create_event_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Date button
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

                        // Time row
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
                                    text = stringResource(R.string.calendar_create_event_add_time),
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
                            onConfirm(eventTitle.trim(), dateTimeMillis, !hasTime)
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

private fun formatPickedTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
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

@Composable
private fun CalendarEventManagementRow(
    title: String,
    dateLabel: String,
    relativeLabel: String,
    recurrenceLabel: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            recurrenceLabel?.let { recurrence ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = recurrence,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = relativeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun nearestInstance(
    instances: List<CalendarEventInfo>,
    nowMillis: Long,
): CalendarEventInfo? =
    instances.minWithOrNull(
        compareBy<CalendarEventInfo> { eventDistanceToNow(it.startMillis, nowMillis) }
            .thenByDescending { it.startMillis >= nowMillis }
            .thenBy { it.startMillis },
    )

private fun eventDistanceToNow(
    startMillis: Long,
    nowMillis: Long,
): Long = if (startMillis >= nowMillis) startMillis - nowMillis else nowMillis - startMillis

private fun firstInstanceOnOrAfter(
    instances: List<CalendarEventInfo>,
    thresholdMillis: Long,
): CalendarEventInfo? = instances.firstOrNull { it.startMillis >= thresholdMillis }

private fun startOfTodayMillis(): Long =
    Calendar.getInstance()
        .apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

private fun CalendarEventGroup.isRecurring(): Boolean =
    instances.size > 1 || !nearestInstance.recurrenceRule.isNullOrBlank()
