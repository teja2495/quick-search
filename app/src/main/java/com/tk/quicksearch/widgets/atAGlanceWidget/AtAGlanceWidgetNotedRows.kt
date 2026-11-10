package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.noted.NotedGlanceTasks
import com.tk.quicksearch.search.models.NotedItem
import com.tk.quicksearch.search.noted.NotedIntegration
import com.tk.quicksearch.search.noted.NotedLauncher
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/**
 * Noted tasks shown like reminders while "Use Noted" is on, each with its due time to sort it among
 * them. They show only their scheduled time, without "Now" or "Overdue". Tapping opens the task in
 * Noted; the check completes it there, and closing dismisses it as a reminder would be.
 */
internal fun AtAGlanceWidgetRowLoader.notedTaskRows(): List<Pair<Long, AtAGlanceWidgetRow>> {
    val isShown = items.isShown(GlanceItem.REMINDERS)
    if (!isShown) return emptyList()
    // Upcoming tasks join the card ahead of time, and turn overdue at their due time.
    NotedGlanceTasks.all(context).take(5).forEach { task ->
        changesAt(ReminderRepository.glanceVisibleFromMillis(task.reminder))
        changesAt(task.reminder.dueMillis)
    }
    val tasks = NotedGlanceTasks.load(context, nowMillis, isShown, items.isShown(GlanceItem.TOMORROW_REMINDERS))
    val notedPackage = NotedIntegration.packageName
    return tasks.map { task ->
        task.reminder.dueMillis to
            AtAGlanceWidgetRow(
                key = task.key,
                icon =
                    notedPackage?.let { AtAGlanceWidgetIcon.App(it) }
                        ?: AtAGlanceWidgetIcon.Drawable(R.drawable.ic_reminder),
                title = task.item.title,
                subtitle = reminderSchedule(task.reminder),
                tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.NOTED_TASK, key = task.item.id),
                titleMaxLines = Int.MAX_VALUE,
                dismiss = AtAGlanceWidgetDismissType.NOTED_TASK,
                trailing =
                    listOf(
                        AtAGlanceWidgetButton(
                            tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.NOTED_TASK_DONE, key = task.item.id),
                            style = AtAGlanceWidgetButtonStyle.ICON,
                            label = string(R.string.action_mark_as_done),
                            icon = MaterialSymbols.Check,
                        ),
                    ),
            )
    }
}

internal fun openNotedTask(context: Context, taskId: String) {
    NotedLauncher.open(context, NotedItem.Kind.TASK, taskId)
}
