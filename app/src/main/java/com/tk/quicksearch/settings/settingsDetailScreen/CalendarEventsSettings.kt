package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppFabTone
import com.tk.quicksearch.shared.ui.components.AppExtendedFloatingActionButton
import com.tk.quicksearch.shared.ui.components.AppFloatingActionButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.height
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerIconBadge
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerAppIcon
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.search.calendar.calendarRecurrenceLabel
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.calendar.formatCalendarEventDate
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.settings.appShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

private data class CalendarEventGroup(
    val eventId: Long,
    val title: String,
    val nearestInstance: CalendarEventInfo,
    val instances: List<CalendarEventInfo>,
)

private data class CalendarAppTarget(
    val packageName: String,
    val label: String,
)

internal val CalendarSettingsBarCornerShape = AppShapes.extraLarge

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarEventsSettingsSection(
    onEventClick: (CalendarEventInfo) -> Unit,
    searchQuery: String = "",
    onHideEvent: (CalendarEventInfo) -> Unit = {},
    onUnhideEvent: (CalendarEventInfo) -> Unit = {},
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
    var hasPermission by remember { mutableStateOf(calendarRepository.hasPermission()) }
    var selectedEventGroupForSheet by remember { mutableStateOf<CalendarEventGroup?>(null) }
    var includePastEvents by remember { mutableStateOf(calendarPreferences.getIncludePastEvents()) }
    var defaultCalendarPackage by remember { mutableStateOf(calendarPreferences.getDefaultCalendarPackage()) }
    var showDefaultCalendarDialog by remember { mutableStateOf(false) }
    // Hidden events reuse the calendar exclusion list, so search, history and At a Glance skip them too.
    var hiddenEventIds by remember { mutableStateOf(calendarPreferences.getExcludedEventIds()) }
    val showHiddenEvents by CalendarHiddenEventsState.showHiddenEvents.collectAsState()
    DisposableEffect(Unit) { onDispose { CalendarHiddenEventsState.reset() } }

    val calendarApps by produceState(initialValue = emptyList(), context) {
        value = withContext(Dispatchers.IO) { discoverCalendarApps(context) }
    }
    val defaultCalendarLabel =
        calendarApps.firstOrNull { it.packageName == defaultCalendarPackage }?.label
            ?: defaultCalendarPackage
            ?: stringResource(R.string.common_theme_system)

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

    val events by produceState(initialValue = emptyList(), hasPermission) {
        value =
            if (!hasPermission) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { calendarRepository.getEventInstancesAroundNow(limit = 2000) }
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

    val hasHiddenEvents = remember(eventGroups, hiddenEventIds) { eventGroups.any { it.eventId in hiddenEventIds } }
    LaunchedEffect(hasHiddenEvents) {
        CalendarHiddenEventsState.setHasHiddenEvents(hasHiddenEvents)
        if (!hasHiddenEvents) CalendarHiddenEventsState.setShowHiddenEvents(false)
    }
    val visibleEventGroups =
        remember(eventGroups, hiddenEventIds, showHiddenEvents) {
            if (showHiddenEvents) eventGroups else eventGroups.filterNot { it.eventId in hiddenEventIds }
        }

    val filteredEventGroups =
        remember(visibleEventGroups, normalizedSearchQuery, locale) {
            if (normalizedSearchQuery.isBlank()) {
                visibleEventGroups
            } else {
                visibleEventGroups
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

    // Auto-scroll to today or the next future event on load
    val todayIndex = remember(sortedEventGroups, todayStartMillis) {
        sortedEventGroups.indexOfFirst { it.nearestInstance.startMillis >= todayStartMillis }
    }
    // Keyed on what reloads or re-filters the list, not its size, so hiding an event keeps the scroll position.
    LaunchedEffect(eventGroups, normalizedSearchQuery, showHiddenEvents) {
        if (todayIndex >= 0 && sortedEventGroups.isNotEmpty()) {
            listState.scrollToItem(todayIndex)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = modifier) {
        SegmentedGroup(
            modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding),
        ) {
            SettingsSegmentNavigationRow(
                item = SettingsCardItem(
                    title = stringResource(R.string.settings_calendar_default_title),
                    description = stringResource(R.string.settings_app_language_desc, defaultCalendarLabel),
                    icon = MaterialSymbols.CalendarMonth,
                    actionOnPress = { showDefaultCalendarDialog = true },
                ),
                index = 0,
                count = 2,
            )
            SegmentedToggleTile(
                title = stringResource(R.string.settings_calendar_include_past_events_title),
                description = stringResource(R.string.settings_calendar_include_past_events_desc),
                checked = includePastEvents,
                onCheckedChange = { enabled ->
                    includePastEvents = enabled
                    calendarPreferences.setIncludePastEvents(enabled)
                },
                icon = MaterialSymbols.History,
                index = 1,
                count = 2,
            )
        }
        if (!hasPermission || sortedEventGroups.isEmpty()) {
            SegmentedTile(index = 0, count = 1) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (!hasPermission) R.string.calendar_section_permission_subtitle
                            else R.string.settings_calendar_events_empty,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeight)
                    .clip(AppShapes.extraLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
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
                    SegmentedTile(index = index, count = sortedEventGroups.size) {
                        CalendarEventManagementRow(
                            title = eventGroup.title,
                            dateLabel = formatCalendarEventDate(eventGroup.nearestInstance),
                            relativeLabel = calendarRelativeDateLabel(eventGroup.nearestInstance),
                            recurrenceLabel = recurrenceLabel,
                            isPast = eventGroup.nearestInstance.endMillis < nowMillis,
                            isHidden = eventGroup.eventId in hiddenEventIds,
                            onToggleHidden = {
                                val event = eventGroup.nearestInstance
                                if (eventGroup.eventId in hiddenEventIds) {
                                    hiddenEventIds = hiddenEventIds - eventGroup.eventId
                                    onUnhideEvent(event)
                                } else {
                                    hiddenEventIds = hiddenEventIds + eventGroup.eventId
                                    onHideEvent(event)
                                }
                            },
                            onClick = {
                                if (eventGroup.isRecurring()) {
                                    selectedEventGroupForSheet = eventGroup
                                } else {
                                    onEventClick(eventGroup.nearestInstance)
                                }
                            },
                        )
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

    if (showDefaultCalendarDialog) {
        DefaultCalendarDialog(
            selectedPackageName = defaultCalendarPackage,
            onCalendarSelected = { packageName ->
                defaultCalendarPackage = packageName
                calendarPreferences.setDefaultCalendarPackage(packageName)
                showDefaultCalendarDialog = false
            },
            onDismiss = { showDefaultCalendarDialog = false },
        )
    }
}

@Composable
internal fun DefaultCalendarDialog(
    selectedPackageName: String?,
    onCalendarSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val calendarApps by produceState(initialValue = emptyList(), context) {
        value = withContext(Dispatchers.IO) { discoverCalendarApps(context) }
    }
    AppPickerDrawer(
        title = stringResource(R.string.settings_calendar_default_title),
        onDismiss = onDismiss,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
            item(key = "system") {
                AppPickerDrawerRow(
                    title = stringResource(R.string.common_theme_system),
                    selected = selectedPackageName == null,
                    leading = { AppPickerDrawerIconBadge(icon = MaterialSymbols.Android) },
                    onClick = { onCalendarSelected(null) },
                )
            }
            items(calendarApps, key = CalendarAppTarget::packageName) { app ->
                AppPickerDrawerRow(
                    title = app.label,
                    selected = selectedPackageName == app.packageName,
                    leading = { AppPickerDrawerAppIcon(packageName = app.packageName) },
                    onClick = { onCalendarSelected(app.packageName) },
                )
            }
        }
    }
}

private fun discoverCalendarApps(context: android.content.Context): List<CalendarAppTarget> {
    val intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(
            CalendarContract.Events.CONTENT_URI,
            "vnd.android.cursor.item/event",
        )
    val packageManager = context.packageManager
    val activities =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        }.getOrDefault(emptyList())
    return activities
        .mapNotNull { activity ->
            activity.activityInfo?.packageName?.let { packageName ->
                CalendarAppTarget(
                    packageName = packageName,
                    label = calendarAppDisplayLabel(
                        packageName = packageName,
                        fallbackLabel = activity.loadLabel(packageManager).toString(),
                    ),
                )
            }
        }
        .distinctBy(CalendarAppTarget::packageName)
        .sortedBy { it.label.lowercase() }
}

private fun calendarAppDisplayLabel(
    packageName: String,
    fallbackLabel: String,
): String =
    when (packageName) {
        "com.google.android.calendar" -> "Google Calendar"
        "com.samsung.android.calendar" -> "Samsung Calendar"
        else -> fallbackLabel.ifBlank { packageName }
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarEventsBottomBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onNewEvent: () -> Unit,
    modifier: Modifier = Modifier,
    newItemLabelResId: Int = R.string.reminder_new_title,
    showNewItem: Boolean = true,
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
            .padding(start = DesignTokens.SpacingLarge, end = DesignTokens.SpacingLarge, bottom = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showNewItem) Arrangement.spacedBy(DesignTokens.SpacingMedium) else Arrangement.End,
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
            if (showNewItem) {
                AppFloatingActionButton(
                    onClick = onNewEvent,
                    icon = MaterialSymbols.Add,
                    contentDescription = stringResource(newItemLabelResId),
                )
            }
        } else {
            // Same height and round shape as the new-item button beside it, in the accent's tonal tint.
            AppFloatingActionButton(
                onClick = { isSearchExpanded = true },
                icon = MaterialSymbols.Search,
                contentDescription = stringResource(R.string.common_search),
                tone = AppFabTone.Tonal,
            )
            if (showNewItem) {
                AppExtendedFloatingActionButton(
                    onClick = onNewEvent,
                    text = stringResource(newItemLabelResId),
                    icon = MaterialSymbols.Add,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ============================================================================
// Create / Edit dialogs
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CalendarEventManagementRow(
    title: String,
    dateLabel: String,
    relativeLabel: String,
    recurrenceLabel: String?,
    isPast: Boolean,
    isHidden: Boolean,
    onToggleHidden: () -> Unit,
    onClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (isHidden) AppColors.ContentMutedAlpha else if (isPast) AppColors.ContentHighAlpha else 1f)
                    .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = { showMenu = true })
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingLarge,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            TonalIconBadge(icon = MaterialSymbols.CalendarMonth)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                recurrenceLabel?.let { recurrence ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = recurrence,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = relativeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AppDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            AppDropdownMenuItem(
                text = stringResource(if (isHidden) R.string.action_include_generic else R.string.action_exclude_generic),
                icon = if (isHidden) MaterialSymbols.Visibility else MaterialSymbols.VisibilityOff,
                onClick = {
                    showMenu = false
                    onToggleHidden()
                },
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
