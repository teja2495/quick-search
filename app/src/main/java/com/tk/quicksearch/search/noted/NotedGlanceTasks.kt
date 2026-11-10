package com.tk.quicksearch.search.noted

import android.content.Context
import com.tk.quicksearch.search.data.GlanceTiming
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.models.NotedItem
import com.tk.quicksearch.search.models.ReminderInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A Noted task on At a Glance. [reminder] holds its due date and time as a Quick Search reminder,
 * so the row reads exactly like one; its id is unused.
 */
internal data class NotedGlanceTask(
    val item: NotedItem,
    val reminder: ReminderInfo,
) {
    val key: String get() = "noted-${item.id}"
}

/** Noted's dated open tasks, shown on At a Glance beside reminders while "Use Noted" is on. */
internal object NotedGlanceTasks {
    /**
     * The tasks At a Glance shows now, by the same rules as [ReminderRepository.getHomeCardReminders]:
     * from 30 minutes before a timed task (or the morning of a date-only one), tomorrow's the evening
     * before, and until it is completed. Days Track items leave once due, since they are never overdue.
     * Blocking; call off the main thread.
     */
    fun load(
        context: Context,
        nowMillis: Long,
        isShown: Boolean,
        isTomorrowShown: Boolean,
    ): List<NotedGlanceTask> {
        if (!isShown || !isEnabled(context)) return emptyList()
        val showTomorrow = isTomorrowShown && GlanceTiming.isEvening(nowMillis)
        val hiddenUntil = ReminderPreferences(context).getNotedHiddenUntil()
        return all(context).filter { task ->
            val reminder = task.reminder
            (hiddenUntil[task.item.id] ?: 0L) <= nowMillis &&
                (!task.item.isDaysTrack || !reminder.isOverdue(nowMillis)) &&
                (
                    ReminderRepository.glanceVisibleFromMillis(reminder) <= nowMillis ||
                        showTomorrow && ReminderRepository.isTomorrowEveningItem(reminder, nowMillis)
                )
        }
    }

    /**
     * Dismisses [task] from At a Glance as [ReminderRepository.dismissFromHome] does a reminder:
     * tomorrow's task dismissed the evening before comes back the next morning (or when due, if
     * earlier), one dismissed more than 30 minutes before it's due comes back when due, an overdue one
     * is hidden for the rest of the day, and one due within 30 minutes is completed in Noted.
     * Blocking; call off the main thread.
     */
    fun dismiss(context: Context, task: NotedGlanceTask, nowMillis: Long = System.currentTimeMillis()) {
        val reminder = task.reminder
        val until =
            when {
                GlanceTiming.isEvening(nowMillis) && ReminderRepository.isTomorrowEveningItem(reminder, nowMillis) ->
                    minOf(GlanceTiming.morningStartMillis(reminder.date), reminder.dueMillis)
                ReminderRepository.canDismissUntilDue(reminder, nowMillis) -> reminder.dueMillis
                reminder.isOverdue(nowMillis) -> GlanceTiming.startOfDayMillis(GlanceTiming.localDate(nowMillis).plusDays(1))
                else -> null
            }
        if (until != null) {
            ReminderPreferences(context).setNotedHiddenUntil(task.item.id, until, nowMillis)
        } else {
            NotedRepository(context).setTaskCompletedBlocking(task.item.id, completed = true)
        }
        ReminderRepository.notifyChanged()
    }

    /** The shown or upcoming task with Noted id [taskId], if it is still open. Blocking. */
    fun find(context: Context, taskId: String): NotedGlanceTask? = all(context).firstOrNull { it.item.id == taskId }

    /**
     * Open tasks due today or later, as reminders for the reminders list, each carrying its
     * [ReminderInfo.notedItem] and the id from [reminderIdFor]. Blocking.
     */
    fun upcomingAsReminders(context: Context, limit: Int): List<ReminderInfo> {
        val today = LocalDate.now()
        return all(context)
            .filter { !it.reminder.date.isBefore(today) }
            .take(limit)
            .map { it.asReminder() }
    }

    /**
     * Pinned Noted tasks whose [reminderIdFor] id is in [reminderIds], as reminders for the Home
     * pinned section. Tasks deleted, locked, completed, or left without a due date in Noted are left
     * out. Blocking.
     */
    fun byReminderIds(context: Context, reminderIds: Collection<Long>): List<ReminderInfo> {
        val ids = reminderIds.filter { it < 0L }.toSet()
        if (ids.isEmpty()) return emptyList()
        val taskPrefix = "${NotedItem.Kind.TASK.raw}:"
        val keys =
            ReminderPreferences(context).getPinnedNotedKeys().filter { key ->
                key.startsWith(taskPrefix) && reminderIdFor(key.removePrefix(taskPrefix)) in ids
            }
        return NotedRepository(context).itemsBlocking(keys).mapNotNull(::asPinnedReminder)
    }

    /**
     * A stable negative reminder id for Noted task [taskId], so it can be pinned and ordered beside
     * reminders, whose ids are positive.
     */
    fun reminderIdFor(taskId: String): Long {
        val bits =
            runCatching { UUID.fromString(taskId).let { it.mostSignificantBits xor it.leastSignificantBits } }
                .getOrElse { taskId.hashCode().toLong() }
        return -(bits and Long.MAX_VALUE) - 1L
    }

    /**
     * [item] as the reminder Home pins it as, or null when it can't be pinned. Only dated tasks pin:
     * they read as reminders, while notes, lists, and undated tasks have no reminder to stand in for.
     */
    fun asPinnedReminder(item: NotedItem): ReminderInfo? = toGlanceTask(item)?.asReminder()

    private fun NotedGlanceTask.asReminder(): ReminderInfo =
        reminder.copy(reminderId = reminderIdFor(item.id), notedItem = item)

    /** Completes Noted task [taskId] off the main thread, then refreshes reminders on home and the widget. */
    fun completeInBackground(context: Context, taskId: String) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (NotedRepository(appContext).setTaskCompletedBlocking(taskId, completed = true)) {
                ReminderRepository.notifyChanged()
            }
        }
    }

    /** Every dated open task, soonest first, for scheduling when rows appear. Blocking. */
    fun all(context: Context): List<NotedGlanceTask> {
        if (!isEnabled(context)) return emptyList()
        return NotedRepository(context).dueTasksBlocking().mapNotNull(::toGlanceTask).sortedBy { it.reminder.dueMillis }
    }

    private fun isEnabled(context: Context): Boolean =
        ReminderPreferences(context).isUseNotedEnabled() && NotedIntegration.ensureChecked(context)

    private fun toGlanceTask(item: NotedItem): NotedGlanceTask? {
        if (item.kind != NotedItem.Kind.TASK) return null
        val zoneId = ZoneId.systemDefault()
        val dueAt = item.dueAtMillis
        val reminder =
            if (item.isDueTimeSet && dueAt != null) {
                val local = Instant.ofEpochMilli(dueAt).atZone(zoneId)
                ReminderInfo(0L, item.title, local.toLocalDate(), local.hour * 60 + local.minute)
            } else {
                ReminderInfo(0L, item.title, item.dueDate(zoneId) ?: return null, timeMinutes = null)
            }
        return NotedGlanceTask(item, reminder)
    }
}
