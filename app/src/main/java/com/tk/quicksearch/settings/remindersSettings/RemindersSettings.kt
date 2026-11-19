package com.tk.quicksearch.settings.remindersSettings

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.settings.gestureSettings.labelResId
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.reminders.ReminderPermissions
import com.tk.quicksearch.reminders.rememberMissingReminderPermissionRequester
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.models.NotedItem
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.noted.NotedAppIcon
import com.tk.quicksearch.search.noted.NotedLauncher
import com.tk.quicksearch.search.noted.NotedRepository
import com.tk.quicksearch.search.noted.NotedTaskRelativeDateText
import com.tk.quicksearch.search.noted.notedSubtitle
import com.tk.quicksearch.search.reminders.ReminderRelativeDateText
import com.tk.quicksearch.search.reminders.reminderScheduleLabel
import com.tk.quicksearch.settings.appShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppTextButton

private enum class ReminderFilterOption(val labelResId: Int) {
    ALL(R.string.settings_reminders_filter_all),
    OVERDUE(R.string.reminder_status_overdue),
    PAST(R.string.settings_reminders_filter_past),
    FUTURE(R.string.settings_reminders_filter_future),
}

/** A row on this page: a Quick Search reminder, or an open, dated task from the Noted app. */
private sealed interface ReminderEntry {
    val key: String
    val day: LocalDate
    val sortMillis: Long

    data class Local(val reminder: ReminderInfo) : ReminderEntry {
        override val key get() = "reminder:${reminder.reminderId}"
        override val day get() = reminder.date
        override val sortMillis get() = reminder.dueMillis
    }

    data class Noted(val item: NotedItem, override val day: LocalDate) : ReminderEntry {
        override val key get() = "noted:${item.key}"
        override val sortMillis: Long
            get() = item.dueAtMillis?.takeIf { item.isDueTimeSet }
                ?: day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

@Composable
fun RemindersSettingsSection(
    searchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { ReminderRepository(context) }
    val notedRepository = remember(context) { NotedRepository(context) }
    // Noted changes outside this app, so its tasks reload whenever the page resumes.
    var resumeCount by remember { mutableStateOf(0) }
    val reminderPreferences = remember(context) { ReminderPreferences(context) }
    val changeCount by ReminderRepository.changes.collectAsState()
    var includePastReminders by remember { mutableStateOf(reminderPreferences.getIncludePastReminders()) }
    var hasPermissions by remember { mutableStateOf(ReminderPermissions.hasAllPermissions(context)) }
    var permissionHintDismissed by remember { mutableStateOf(false) }
    val refreshPermissions = { hasPermissions = ReminderPermissions.hasAllPermissions(context) }
    val requestMissingPermission = rememberMissingReminderPermissionRequester(onResult = refreshPermissions)

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshPermissions()
                    resumeCount++
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val reminders by produceState(initialValue = emptyList<ReminderInfo>(), changeCount) {
        value = withContext(Dispatchers.IO) { repository.getAllReminders() }
    }
    val notedTasks by produceState(initialValue = emptyList<ReminderEntry.Noted>(), changeCount, resumeCount) {
        // Only while "Noted Integration" is on; toggling it bumps changeCount, which reloads this.
        value =
            if (!reminderPreferences.isUseNotedEnabled()) {
                emptyList()
            } else {
                notedRepository.dueTasks().mapNotNull { item -> item.dueDate()?.let { ReminderEntry.Noted(item, it) } }
            }
    }
    val locale = Locale.getDefault()
    val normalizedQuery = remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val today = LocalDate.now()
    val nowMillis = System.currentTimeMillis()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var selectedFilterOption by remember { mutableStateOf(ReminderFilterOption.ALL) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }
    val sortedReminders =
        remember(reminders, notedTasks, normalizedQuery, locale, nowMillis) {
            (reminders.map(ReminderEntry::Local) + notedTasks)
                .filter { entry ->
                    val title = when (entry) {
                        is ReminderEntry.Local -> entry.reminder.title
                        is ReminderEntry.Noted -> entry.item.title
                    }
                    normalizedQuery.isBlank() ||
                        shortcutMatchPriority(name = title, query = normalizedQuery, locale = locale) != null
                }
                .sortedBy { it.sortMillis }
        }
    val displayedReminders =
        remember(sortedReminders, selectedFilterOption, normalizedQuery, nowMillis) {
            if (normalizedQuery.isNotBlank()) {
                sortedReminders
            } else {
                sortedReminders.filter { entry ->
                    // Noted only shares open tasks, so they are never in Past.
                    val isDone = entry is ReminderEntry.Local && entry.reminder.isDone
                    val isOverdue = when (entry) {
                        is ReminderEntry.Local -> entry.reminder.isOverdue(nowMillis)
                        is ReminderEntry.Noted -> entry.item.isOverdue(nowMillis)
                    }
                    when (selectedFilterOption) {
                        ReminderFilterOption.ALL -> true
                        ReminderFilterOption.OVERDUE -> !isDone && isOverdue
                        ReminderFilterOption.PAST -> isDone
                        ReminderFilterOption.FUTURE -> !isDone && !isOverdue
                    }
                }
            }
        }
    val todayIndex = remember(displayedReminders, today) {
        displayedReminders.indexOfFirst { entry -> entry.day >= today }
    }
    LaunchedEffect(selectedFilterOption, normalizedQuery, todayIndex, displayedReminders.size) {
        val targetIndex =
            if (normalizedQuery.isBlank() && selectedFilterOption == ReminderFilterOption.ALL && todayIndex >= 0) {
                todayIndex
            } else {
                0
            }
        if (displayedReminders.isNotEmpty()) listState.scrollToItem(targetIndex)
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(modifier = modifier) {
        if (!hasPermissions && !permissionHintDismissed) {
            SegmentedTile(
                index = 0,
                count = 1,
                modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(role = Role.Button, onClick = requestMissingPermission)
                            .padding(
                                horizontal = DesignTokens.SpacingLarge,
                                vertical = DesignTokens.SpacingLarge,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                    Text(
                        text = stringResource(R.string.reminders_permission_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    AppIconButton(onClick = { permissionHintDismissed = true }, tooltip = stringResource(R.string.common_close)) {
                        Icon(
                            imageVector = MaterialSymbols.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = AppColors.Accent,
                        )
                    }
                }
            }
        }
        SegmentedGroup(
            modifier =
                Modifier.fillMaxWidth().padding(
                    bottom = if (normalizedQuery.isBlank()) DesignTokens.SpacingXSmall else DesignTokens.SpacingMedium,
                ),
        ) {
            SegmentedToggleTile(
                title = stringResource(R.string.settings_include_past_reminders_title),
                description = stringResource(R.string.settings_include_past_reminders_desc),
                checked = includePastReminders,
                onCheckedChange = { enabled ->
                    includePastReminders = enabled
                    reminderPreferences.setIncludePastReminders(enabled)
                    ReminderRepository.notifyChanged()
                },
                icon = MaterialSymbols.History,
                index = 0,
                count = 1,
            )
        }
        if (normalizedQuery.isBlank()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            start = DesignTokens.SpacingXSmall,
                        ),
            ) {
                AppTextButton(onClick = { isFilterMenuExpanded = true }) {
                    Text(
                        text = stringResource(selectedFilterOption.labelResId),
                        color = AppColors.Accent,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(
                        imageVector = MaterialSymbols.ExpandMore,
                        contentDescription = null,
                        tint = AppColors.Accent,
                    )
                }
                AppDropdownMenu(
                    expanded = isFilterMenuExpanded,
                    onDismissRequest = { isFilterMenuExpanded = false },
                ) {
                    ReminderFilterOption.entries.forEachIndexed { index, option ->
                        if (index > 0) AppDropdownMenuDivider()
                        AppDropdownMenuItem(
                            text = stringResource(option.labelResId),
                            onClick = {
                                selectedFilterOption = option
                                isFilterMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (displayedReminders.isEmpty()) {
                SegmentedTile(index = 0, count = 1) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.reminders_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight).clip(AppShapes.extraLarge),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                ) {
                    itemsIndexed(items = displayedReminders, key = { _, entry -> entry.key }) { index, entry ->
                        SegmentedTile(index = index, count = displayedReminders.size) {
                            when (entry) {
                                is ReminderEntry.Local -> {
                                    val reminder = entry.reminder
                                    ReminderManagementRow(
                                        reminder = reminder,
                                        isPast = reminder.isDone,
                                        isOverdue = !reminder.isDone && reminder.isOverdue(nowMillis),
                                        onClick = { ReminderEditorRequests.openEdit(reminder) },
                                        onMarkDone = {
                                            scope.launch(Dispatchers.IO) { repository.setDone(reminder.reminderId, true) }
                                        },
                                        onDelete = {
                                            scope.launch(Dispatchers.IO) { repository.deleteReminder(reminder.reminderId) }
                                        },
                                    )
                                }
                                is ReminderEntry.Noted ->
                                    NotedTaskManagementRow(
                                        item = entry.item,
                                        onClick = { NotedLauncher.open(context, entry.item) },
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderManagementRow(
    reminder: ReminderInfo,
    isPast: Boolean,
    isOverdue: Boolean,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .alpha(if (isPast) AppColors.ContentHighAlpha else 1f)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        TonalIconBadge(icon = MaterialSymbols.Alarm)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = reminderScheduleLabel(reminder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ReminderRelativeDateText(reminder)
        }
        when {
            isPast ->
                AppIconButton(onClick = onDelete, tooltip = stringResource(R.string.dialog_delete)) {
                    Icon(
                        imageVector = MaterialSymbols.DeleteOutline,
                        contentDescription = stringResource(R.string.dialog_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            isOverdue ->
                AppIconButton(onClick = onMarkDone, tooltip = stringResource(R.string.action_mark_as_done)) {
                    Icon(
                        imageVector = MaterialSymbols.Check,
                        contentDescription = stringResource(R.string.action_mark_as_done),
                        tint = AppColors.Accent,
                    )
                }
        }
    }
}

/** A Noted task styled like [ReminderManagementRow]; it opens in Noted, which owns its actions. */
@Composable
private fun NotedTaskManagementRow(
    item: NotedItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        NotedAppIcon(iconPackPackage = null, modifier = Modifier.size(DesignTokens.Spacing40))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            notedSubtitle(item)?.let { schedule ->
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NotedTaskRelativeDateText(item)
        }
    }
}
