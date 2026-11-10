package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.search.calendar.calendarRelativeTimeLabel
import com.tk.quicksearch.search.data.GlanceTiming
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.noted.NotedGlanceTask
import com.tk.quicksearch.search.noted.NotedGlanceTasks
import com.tk.quicksearch.search.noted.NotedAppIcon
import com.tk.quicksearch.search.noted.NotedLauncher
import com.tk.quicksearch.search.noted.NotedRepository
import com.tk.quicksearch.search.reminders.reminderOverdueColor
import com.tk.quicksearch.search.reminders.reminderScheduleLabel
import com.tk.quicksearch.shared.ui.components.LocalShowUndoSnackbar
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val UpcomingReminderDismissSize = 28.dp
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val REMINDER_NOW_WINDOW_MILLIS = 60L * 1000L
private const val DONE_UNDO_WINDOW_MILLIS = 4_000L

/**
 * Reminders for the home At a Glance card, as [ReminderRepository.getHomeCardReminders] picks them.
 * More than 30 minutes before it's due (or the evening before, for tomorrow's), a reminder can be
 * dismissed until then. After that it stays until it is marked done, snoozed (hidden here and from
 * notifications for 30 minutes, due time unchanged) or deleted; those actions offer Undo, which
 * restores the reminder as it was. Marking done keeps the row in place for a few seconds with its
 * own Undo button; snooze and delete use the snackbar.
 */
internal class UpcomingRemindersGlance(
    val reminders: List<ReminderInfo>,
    /** Reminders in [reminders] just marked done, still shown so their row can offer Undo. */
    val pendingDoneIds: Set<Long>,
    val nowMillis: Long,
    val markDone: (ReminderInfo) -> Unit,
    val undoDone: (ReminderInfo) -> Unit,
    val snooze: (ReminderInfo) -> Unit,
    val dismiss: (ReminderInfo) -> Unit,
    val delete: (ReminderInfo) -> Unit,
    /** Noted tasks due now or soon, shown like reminders while "Use Noted" is on. */
    val notedTasks: List<NotedGlanceTask> = emptyList(),
    val pendingDoneNotedKeys: Set<String> = emptySet(),
    val markNotedDone: (NotedGlanceTask) -> Unit = {},
    val undoNotedDone: (NotedGlanceTask) -> Unit = {},
    val dismissNoted: (NotedGlanceTask) -> Unit = {},
)

/** Polls the home-card reminders while [enabled]; refreshes on resume, on edits and every 10 seconds. */
@Composable
internal fun rememberUpcomingRemindersGlance(enabled: Boolean): UpcomingRemindersGlance {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { ReminderRepository(context) }
    val reminderPreferences = remember(context) { ReminderPreferences(context) }
    val changeCount by ReminderRepository.changes.collectAsState()
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reminders by remember { mutableStateOf(emptyList<ReminderInfo>()) }
    var notedTasks by remember { mutableStateOf(emptyList<NotedGlanceTask>()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(repository, refreshKey, changeCount, enabled) {
        if (!enabled) {
            reminders = emptyList()
            notedTasks = emptyList()
            return@LaunchedEffect
        }
        // Home stays composed while another app is in front; only poll while it can be seen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                reminders = withContext(Dispatchers.IO) { repository.getHomeCardReminders(nowMillis) }
                notedTasks =
                    withContext(Dispatchers.IO) {
                        NotedGlanceTasks.load(
                            context,
                            nowMillis,
                            isShown = reminderPreferences.isShowUpcomingRemindersEnabled(),
                            isTomorrowShown = reminderPreferences.isShowTomorrowRemindersEnabled(),
                        )
                    }
                delay(10_000)
            }
        }
    }

    // Reminders just marked done, with the index they had, so their row stays put while it offers Undo.
    var pendingDone by remember { mutableStateOf(emptyMap<Long, Pair<Int, ReminderInfo>>()) }
    val pendingDoneJobs = remember { mutableMapOf<Long, Job>() }
    val displayedReminders =
        if (pendingDone.isEmpty()) {
            reminders
        } else {
            val live = reminders.filterNot { it.reminderId in pendingDone }.toMutableList()
            pendingDone.values.sortedBy { it.first }.forEach { (index, reminder) ->
                live.add(index.coerceIn(0, live.size), reminder)
            }
            live
        }

    // Noted tasks just marked done, kept in place like reminders so their row can offer Undo.
    var pendingNotedDone by remember { mutableStateOf(emptyMap<String, Pair<Int, NotedGlanceTask>>()) }
    val pendingNotedDoneJobs = remember { mutableMapOf<String, Job>() }
    val displayedNotedTasks =
        if (pendingNotedDone.isEmpty()) {
            notedTasks
        } else {
            val live = notedTasks.filterNot { it.key in pendingNotedDone }.toMutableList()
            pendingNotedDone.values.sortedBy { it.first }.forEach { (index, task) ->
                live.add(index.coerceIn(0, live.size), task)
            }
            live
        }
    val notedRepository = remember(context) { NotedRepository(context) }

    val showUndoSnackbar = LocalShowUndoSnackbar.current
    val snoozedMessage = stringResource(R.string.home_reminder_snoozed)
    val deletedMessage = stringResource(R.string.home_reminder_deleted)

    /**
     * Hides [reminder] right away, runs [action] off the main thread, then offers Undo. [readBefore]
     * captures extra state the undo needs (the pinned order before a delete).
     */
    fun <T> act(
        reminder: ReminderInfo,
        message: String,
        icon: ImageVector,
        readBefore: ReminderRepository.() -> T,
        action: ReminderRepository.() -> Unit,
        undo: ReminderRepository.(T) -> Unit,
    ) {
        reminders = reminders.filterNot { it.reminderId == reminder.reminderId }
        scope.launch {
            val before = withContext(Dispatchers.IO) { repository.readBefore().also { repository.action() } }
            showUndoSnackbar?.invoke(message, icon) {
                scope.launch(Dispatchers.IO) { repository.undo(before) }
            }
        }
    }
    return UpcomingRemindersGlance(
        reminders = displayedReminders,
        pendingDoneIds = pendingDone.keys,
        nowMillis = nowMillis,
        markDone = { reminder ->
            val index = displayedReminders.indexOfFirst { it.reminderId == reminder.reminderId }.coerceAtLeast(0)
            pendingDone = pendingDone + (reminder.reminderId to (index to reminder))
            pendingDoneJobs.remove(reminder.reminderId)?.cancel()
            pendingDoneJobs[reminder.reminderId] =
                scope.launch {
                    withContext(Dispatchers.IO) { repository.setDone(reminder.reminderId, true) }
                    delay(DONE_UNDO_WINDOW_MILLIS)
                    pendingDone = pendingDone - reminder.reminderId
                    pendingDoneJobs.remove(reminder.reminderId)
                }
        },
        undoDone = { reminder ->
            pendingDoneJobs.remove(reminder.reminderId)?.cancel()
            scope.launch {
                withContext(Dispatchers.IO) { repository.restore(reminder) }
                // Drop the placeholder only after the restore, so the row doesn't flicker out and back.
                pendingDone = pendingDone - reminder.reminderId
            }
        },
        snooze = { reminder ->
            act(
                reminder, snoozedMessage, MaterialSymbols.Snooze,
                readBefore = { isNotificationShowing(reminder.reminderId) },
                action = { snooze(reminder.reminderId) },
                undo = { notificationWasShowing -> unsnooze(reminder.reminderId, notificationWasShowing) },
            )
        },
        // Only offered while it hides the reminder for a while (see [UpcomingReminderRow]); it comes
        // back by itself, with its notification, so there is nothing to undo.
        dismiss = { reminder ->
            reminders = reminders.filterNot { it.reminderId == reminder.reminderId }
            scope.launch(Dispatchers.IO) { repository.dismissFromHome(reminder.reminderId) }
        },
        delete = { reminder ->
            act(
                reminder, deletedMessage, MaterialSymbols.Delete,
                readBefore = { getPinnedReminderOrder() },
                action = { deleteReminder(reminder.reminderId) },
                undo = { pinnedOrder -> restore(reminder, pinnedOrder) },
            )
        },
        notedTasks = displayedNotedTasks,
        pendingDoneNotedKeys = pendingNotedDone.keys,
        markNotedDone = { task ->
            val index = displayedNotedTasks.indexOfFirst { it.key == task.key }.coerceAtLeast(0)
            pendingNotedDone = pendingNotedDone + (task.key to (index to task))
            pendingNotedDoneJobs.remove(task.key)?.cancel()
            pendingNotedDoneJobs[task.key] =
                scope.launch {
                    withContext(Dispatchers.IO) { notedRepository.setTaskCompletedBlocking(task.item.id, true) }
                    ReminderRepository.notifyChanged()
                    delay(DONE_UNDO_WINDOW_MILLIS)
                    pendingNotedDone = pendingNotedDone - task.key
                    pendingNotedDoneJobs.remove(task.key)
                }
        },
        dismissNoted = { task ->
            notedTasks = notedTasks.filterNot { it.key == task.key }
            scope.launch(Dispatchers.IO) { NotedGlanceTasks.dismiss(context, task) }
        },
        undoNotedDone = { task ->
            pendingNotedDoneJobs.remove(task.key)?.cancel()
            scope.launch {
                withContext(Dispatchers.IO) { notedRepository.setTaskCompletedBlocking(task.item.id, false) }
                ReminderRepository.notifyChanged()
                pendingNotedDone = pendingNotedDone - task.key
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UpcomingReminderRow(
    reminder: ReminderInfo,
    nowMillis: Long,
    isPendingDone: Boolean,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onUndoDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    /** A Noted task's menu offers only Done and, while it hides the task for a while, Dismiss. */
    isNotedTask: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    // Keep a timed reminder at its due time in a brief "Now" state before it becomes overdue.
    // This also avoids showing "1 minute ago" during the first minute after it is due.
    val elapsedSinceDueMillis = nowMillis - reminder.dueMillis
    val isNow =
        !reminder.isDone &&
            reminder.hasTime &&
            elapsedSinceDueMillis in 0 until REMINDER_NOW_WINDOW_MILLIS
    // Overdue reminders show their time (or date once a day late) with a red "Overdue" in place of
    // the relative label; upcoming ones read best relative to now.
    val isOverdue = reminder.isOverdue(nowMillis) && !isNow
    val time = remember(reminder.dueMillis, context) {
        DateFormat.getTimeFormat(context).format(Date(reminder.dueMillis))
    }
    val dayLabel =
        when (reminder.date) {
            GlanceTiming.localDate(nowMillis) -> stringResource(R.string.calendar_relative_today)
            GlanceTiming.localDate(nowMillis).plusDays(1) -> stringResource(R.string.calendar_relative_tomorrow)
            else -> null
        }
    val scheduleText =
        when {
            // Undated reminders read as "Today" or "Tomorrow" rather than their 9:00 AM notification time.
            !isOverdue && !reminder.hasTime && dayLabel != null -> dayLabel
            !isOverdue && !isNow && reminder.date != GlanceTiming.localDate(nowMillis) && dayLabel != null ->
                "$time • $dayLabel"
            !isOverdue && !isNow -> "$time • ${calendarRelativeTimeLabel(reminder.dueMillis, nowMillis)}"
            nowMillis - reminder.dueMillis < DAY_MILLIS -> time
            else -> reminderScheduleLabel(reminder)
        }
    val statusText =
        when {
            isNow -> stringResource(R.string.calendar_relative_now)
            isOverdue -> stringResource(R.string.reminder_status_overdue)
            else -> null
        }
    val statusColor = if (isNow) AppColors.Accent else reminderOverdueColor()
    val scheduleLabel =
        buildAnnotatedString {
            append(scheduleText)
            if (statusText != null) {
                append(" • ")
                withStyle(SpanStyle(color = statusColor)) { append(statusText) }
            }
        }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(
            start = 7.dp,
            top = DesignTokens.SpacingMedium,
            bottom = DesignTokens.SpacingMedium,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).combinedClickable(
                enabled = !isPendingDone,
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke() ?: Icon(
                painter = painterResource(R.drawable.ic_reminder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isPendingDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isPendingDone) TextDecoration.LineThrough else null,
                    maxLines = GlanceTextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        if (isPendingDone) {
                            buildAnnotatedString { append(stringResource(R.string.home_reminder_marked_done)) }
                        } else {
                            scheduleLabel
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = GlanceTextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_mark_as_done)) },
                    leadingIcon = { Icon(imageVector = MaterialSymbols.Check, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDone()
                    },
                )
                // Dismiss hides a reminder until it's due (or tomorrow's until the morning), or an overdue one
                // for the rest of the day. Within 30 minutes of its time that would be Done, so only Snooze shows.
                val canDismiss =
                    ReminderRepository.canDismissUntilDue(reminder, nowMillis) ||
                        (GlanceTiming.isEvening(nowMillis) && ReminderRepository.isTomorrowEveningItem(reminder, nowMillis))
                if (isNotedTask) {
                    // Snooze and Delete act on Quick Search reminders; a Noted task is changed in Noted.
                    if (canDismiss || isOverdue) {
                        HorizontalDivider()
                        DismissForNowMenuItem {
                            showMenu = false
                            onDismiss()
                        }
                    }
                } else {
                    HorizontalDivider()
                    if (!canDismiss) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.reminder_notification_action_snooze)) },
                            leadingIcon = { Icon(imageVector = MaterialSymbols.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze()
                            },
                        )
                    }
                    if (canDismiss || isOverdue) {
                        DismissForNowMenuItem {
                            showMenu = false
                            onDismiss()
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.dialog_delete)) },
                        leadingIcon = { Icon(imageVector = MaterialSymbols.Delete, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
        if (isPendingDone) {
            // Counts down the undo window; the row is removed when the ring runs out.
            val remaining = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                remaining.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(DONE_UNDO_WINDOW_MILLIS.toInt(), easing = LinearEasing),
                )
            }
            val trackColor = MaterialTheme.colorScheme.outlineVariant
            val progressColor = AppColors.Accent
            // Drop the 48dp touch-target padding so the ring hugs the compact pill.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                TextButton(
                    onClick = onUndoDone,
                    contentPadding = PaddingValues(horizontal = DesignTokens.SpacingMedium),
                    modifier =
                        Modifier
                            // Overrides TextButton's 40dp default minimum height so the pill stays compact.
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .height(UndoPillHeight)
                            .drawWithContent {
                                drawContent()
                                drawUndoCountdownRing(remaining.value, trackColor, progressColor)
                            },
                ) {
                    Text(
                        text = stringResource(R.string.action_undo),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Accent,
                    )
                }
            }
        } else {
            IconButton(
                onClick = onDone,
                modifier = Modifier.size(UpcomingReminderDismissSize),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Check,
                    contentDescription = stringResource(R.string.action_mark_as_done),
                    tint = AppColors.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DismissForNowMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(R.string.reminders_home_card_dismiss_for_now)) },
        leadingIcon = { Icon(imageVector = MaterialSymbols.Close, contentDescription = null) },
        onClick = onClick,
    )
}

private val UndoCountdownStroke = 2.dp
private val UndoPillHeight = 24.dp

/** A pill outline around the Undo button: a faint track, and an accent arc for the [remaining] fraction. */
private fun DrawScope.drawUndoCountdownRing(remaining: Float, trackColor: Color, progressColor: Color) {
    val strokePx = UndoCountdownStroke.toPx()
    val inset = strokePx / 2f
    val width = size.width - strokePx
    val height = size.height - strokePx
    val radius = height / 2f
    val outline =
        Path().apply {
            addRoundRect(RoundRect(inset, inset, inset + width, inset + height, CornerRadius(radius, radius)))
        }
    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
    drawPath(outline, color = trackColor, style = stroke)
    if (remaining <= 0f) return
    val measure = PathMeasure().apply { setPath(outline, forceClosed = true) }
    val arc = Path()
    measure.getSegment(0f, measure.length * remaining, arc, startWithMoveTo = true)
    drawPath(arc, color = progressColor, style = stroke)
}

/** A Noted task as a reminder row: tapping opens it in Noted, and the check completes it there. */
internal fun notedTaskGlanceItem(task: NotedGlanceTask, glance: UpcomingRemindersGlance): AtAGlanceItem =
    AtAGlanceItem(key = task.key) {
        val context = LocalContext.current
        UpcomingReminderRow(
            reminder = task.reminder,
            nowMillis = glance.nowMillis,
            isPendingDone = task.key in glance.pendingDoneNotedKeys,
            onClick = { NotedLauncher.open(context, task.item) },
            onDone = { glance.markNotedDone(task) },
            onUndoDone = { glance.undoNotedDone(task) },
            onSnooze = {},
            onDismiss = { glance.dismissNoted(task) },
            onDelete = {},
            isNotedTask = true,
            leadingIcon = { NotedAppIcon(iconPackPackage = null, modifier = Modifier.size(24.dp)) },
        )
    }
