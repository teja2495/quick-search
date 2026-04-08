package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.settings.AppShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CalendarEventGroup(
    val eventId: Long,
    val title: String,
    val nearestInstance: CalendarEventInfo,
    val instances: List<CalendarEventInfo>,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarEventsSettingsSection(
    onEventClick: (CalendarEventInfo) -> Unit,
    searchQuery: String = "",
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
                withContext(Dispatchers.IO) {
                    calendarRepository.getEventInstancesAroundNow(limit = 2000)
                }
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
            if (!hasPermission) {
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
                                if (eventGroup.isRecurring()) {
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
