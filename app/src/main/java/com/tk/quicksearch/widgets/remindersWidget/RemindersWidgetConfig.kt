package com.tk.quicksearch.widgets.remindersWidget

import android.content.Context
import android.os.Parcelable
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetButton
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetButtonStyle
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetIcon
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetRow
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetRowLoader
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetTap
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetTapType
import com.tk.quicksearch.widgets.noteWidget.withNoteWidgetDefaults
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.applyWidgetPreferences
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import java.time.Instant
import java.time.ZoneId
import kotlinx.parcelize.Parcelize

private object RemindersWidgetKeys {
    /** Set on the first save, so an unsaved widget starts from the Note look instead of the shared defaults. */
    val SAVED = booleanPreferencesKey("reminders_widget_saved")
}

internal object RemindersWidgetDefaults {
    /** Upcoming reminders are listed through this many days after today. */
    const val UPCOMING_DAYS = 3L

    /** RemoteViews collections are sent in one parcel, so the list stops well short of its size limit. */
    const val MAX_ROWS = 30
}

/** A Reminders widget's settings: the Note widget's background, text color, corner radius and transparency. */
@Parcelize
data class RemindersWidgetConfig(
    val style: WidgetPreferences = WidgetPreferences.Default.withNoteWidgetDefaults(),
) : Parcelable

/** Reads a Reminders widget's settings; a widget that was never saved gets the defaults. */
fun Preferences.toRemindersWidgetConfig(context: Context): RemindersWidgetConfig =
    if (this[RemindersWidgetKeys.SAVED] == true) {
        RemindersWidgetConfig(style = toWidgetPreferences(context).coerceToValidRanges())
    } else {
        RemindersWidgetConfig()
    }

fun MutablePreferences.applyRemindersWidgetConfig(
    config: RemindersWidgetConfig,
    context: Context,
) {
    applyWidgetPreferences(config.style.coerceToValidRanges(), context)
    this[RemindersWidgetKeys.SAVED] = true
}

/** The rows a Reminders widget shows, and when they next change on their own (a reminder turning overdue, midnight). */
internal data class RemindersWidgetSnapshot(
    val rows: List<AtAGlanceWidgetRow>,
    val nextChangeMillis: Long?,
)

/**
 * Every undone reminder that is overdue or due from today through [RemindersWidgetDefaults.UPCOMING_DAYS]
 * days ahead, soonest first, worded as on the At a Glance widget. Blocking; call off the main thread.
 */
internal fun loadRemindersWidgetSnapshot(
    context: Context,
    nowMillis: Long = System.currentTimeMillis(),
): RemindersWidgetSnapshot {
    val appContext = context.applicationContext
    val loader = AtAGlanceWidgetRowLoader(appContext, nowMillis)
    val lastDay =
        Instant
            .ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(RemindersWidgetDefaults.UPCOMING_DAYS)
    val reminders =
        ReminderRepository(appContext)
            .getAllReminders()
            // Undone reminders before today are overdue, so they pass this too.
            .filter { !it.isDone && !it.date.isAfter(lastDay) }
            .sortedBy { it.dueMillis }
            .take(RemindersWidgetDefaults.MAX_ROWS)
    // An upcoming reminder turns overdue at its due time.
    reminders.forEach { loader.changesAt(it.dueMillis) }
    val doneLabel = appContext.getString(R.string.action_mark_as_done)
    val rows =
        reminders.map { reminder ->
            AtAGlanceWidgetRow(
                key = "reminder-${reminder.reminderId}",
                icon = AtAGlanceWidgetIcon.Drawable(R.drawable.ic_reminder),
                title = reminder.title,
                subtitle = loader.reminderSubtitle(reminder),
                tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.REMINDER, id = reminder.reminderId),
                titleMaxLines = 2,
                trailing =
                    listOf(
                        AtAGlanceWidgetButton(
                            tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.REMINDER_DONE, id = reminder.reminderId),
                            style = AtAGlanceWidgetButtonStyle.ICON,
                            label = doneLabel,
                            icon = MaterialSymbols.Check,
                        ),
                    ),
            )
        }
    return RemindersWidgetSnapshot(rows = rows, nextChangeMillis = loader.nextChangeMillis)
}
