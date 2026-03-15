package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.calendar.calendarRecurrenceLabel
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.calendar.formatCalendarEventDate
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.settings.AppShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CalendarEventSortOption(
    @StringRes val labelResId: Int,
) {
    NAME(R.string.settings_app_sort_name),
    DATE(R.string.settings_calendar_sort_date),
}

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
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val calendarRepository = remember(context) { CalendarRepository(context) }
    var hasPermission by remember { mutableStateOf(calendarRepository.hasPermission()) }
    var selectedSortOption by rememberSaveable { mutableStateOf(CalendarEventSortOption.NAME) }
    var isSortAscending by rememberSaveable { mutableStateOf(true) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var selectedEventGroupForSheet by remember { mutableStateOf<CalendarEventGroup?>(null) }

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
        remember(eventGroups, normalizedSearchQuery, locale) {
            if (normalizedSearchQuery.isBlank()) {
                eventGroups
            } else {
                eventGroups
                    .mapNotNull { eventGroup ->
                        val priority =
                            shortcutMatchPriority(
                                name = eventGroup.title,
                                query = normalizedSearchQuery,
                                locale = locale,
                            ) ?: return@mapNotNull null
                        eventGroup to priority
                    }.sortedWith(
                        compareBy<Pair<CalendarEventGroup, com.tk.quicksearch.settings.AppShortcutsSettings.ShortcutSearchMatchPriority>> { it.second.ordinal }
                            .thenBy { it.first.title.lowercase(locale) }
                            .thenBy { it.first.nearestInstance.startMillis },
                    )
                    .map { it.first }
            }
        }

    val sortedEventGroups =
        remember(filteredEventGroups, selectedSortOption, isSortAscending, locale) {
            val baseComparator =
                when (selectedSortOption) {
                    CalendarEventSortOption.NAME -> {
                        compareBy<CalendarEventGroup> { it.title.lowercase(locale) }
                            .thenBy { it.nearestInstance.startMillis }
                    }

                    CalendarEventSortOption.DATE -> {
                        compareBy<CalendarEventGroup> { it.nearestInstance.startMillis }
                            .thenBy { it.title.lowercase(locale) }
                    }
                }

            val comparator = if (isSortAscending) baseComparator else baseComparator.reversed()
            filteredEventGroups.sortedWith(comparator)
        }

    LaunchedEffect(selectedSortOption, isSortAscending) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Row(
                    modifier =
                        Modifier
                            .clickable { isSortMenuExpanded = true }
                            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.settings_app_sort_by_format,
                                stringResource(selectedSortOption.labelResId),
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                    shape = RoundedCornerShape(24.dp),
                    properties = PopupProperties(focusable = false),
                    containerColor = AppColors.DialogBackground,
                ) {
                    CalendarEventSortOption.entries.forEachIndexed { index, option ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(text = stringResource(option.labelResId)) },
                            onClick = {
                                selectedSortOption = option
                                isSortMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.offset(y = (-3).dp),
                onClick = { isSortAscending = !isSortAscending },
            ) {
                Icon(
                    imageVector =
                        if (isSortAscending) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                    contentDescription =
                        if (isSortAscending) {
                            stringResource(R.string.settings_app_sort_ascending)
                        } else {
                            stringResource(R.string.settings_app_sort_descending)
                        },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            if (!hasPermission) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.calendar_permission_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.SpacingLarge),
                    )
                }
            } else if (sortedEventGroups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.fillMaxSize(),
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
                                selectedEventGroupForSheet = eventGroup
                            },
                        )
                        if (index < sortedEventGroups.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    selectedEventGroupForSheet?.let { eventGroup ->
        ModalBottomSheet(
            onDismissRequest = { selectedEventGroupForSheet = null },
            containerColor = AppColors.DialogBackground,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.SpacingLarge),
            ) {
                Text(
                    text = eventGroup.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.padding(
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding,
                            bottom = DesignTokens.SpacingSmall,
                        ),
                )
                calendarRecurrenceLabel(
                    recurrenceRule = eventGroup.nearestInstance.recurrenceRule,
                    instanceCount = eventGroup.instances.size,
                )?.let { recurrenceText ->
                    Text(
                        text = recurrenceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = DesignTokens.SpacingMedium,
                            ),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                ) {
                    itemsIndexed(
                        items = eventGroup.instances,
                        key = { _, instance -> "${instance.eventId}_${instance.startMillis}" },
                    ) { index, instance ->
                        CalendarEventInstanceRow(
                            dateLabel = formatCalendarEventDate(instance),
                            relativeLabel = calendarRelativeDateLabel(instance.startMillis),
                            onClick = {
                                selectedEventGroupForSheet = null
                                onEventClick(instance)
                            },
                        )
                        if (index < eventGroup.instances.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
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

@Composable
private fun CalendarEventInstanceRow(
    dateLabel: String,
    relativeLabel: String,
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
                text = dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
