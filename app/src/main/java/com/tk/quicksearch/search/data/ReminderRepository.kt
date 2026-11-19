package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.reminders.ReminderRepeat
import com.tk.quicksearch.reminders.ReminderScheduler
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.glance.GlanceTiming
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.noted.NotedGlanceTasks
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.time.ScheduleRepeat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores reminders as JSON in preferences and keeps their alarms in sync. Every mutation goes
 * through this class so scheduled alarms, notifications, and UI observers stay consistent.
 */
class ReminderRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = ReminderPreferences(appContext)

    fun getAllReminders(): List<ReminderInfo> = readReminders()

    fun getReminderById(reminderId: Long): ReminderInfo? =
        readReminders().firstOrNull { it.reminderId == reminderId }

    fun getRemindersByIds(reminderIds: Collection<Long>): List<ReminderInfo> {
        if (reminderIds.isEmpty()) return emptyList()
        val ids = reminderIds.toSet()
        return readReminders().filter { it.reminderId in ids } + NotedGlanceTasks.byReminderIds(appContext, ids)
    }

    fun searchReminders(
        query: String,
        includePastReminders: Boolean,
        resultLimit: Int = Int.MAX_VALUE,
    ): List<ReminderInfo> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query.trim())
        if (normalizedQuery.isBlank()) return emptyList()
        val today = LocalDate.now(ZoneId.systemDefault())
        return readReminders()
            .asSequence()
            .filter { includePastReminders || isVisibleWhenPastHidden(it, today) }
            .mapNotNull { reminder ->
                val priority = SearchRankingUtils.calculateMatchPriority(reminder.title, normalizedQuery)
                if (SearchRankingUtils.isOtherMatch(priority)) null else reminder to priority
            }
            .sortedWith(compareBy<Pair<ReminderInfo, Int>> { it.second }.thenBy { it.first.dueMillis })
            .take(resultLimit)
            .map { it.first }
            .toList()
    }

    /**
     * Reminders to surface on Home: undated ones from the morning, timed ones from 1.5 hours before
     * they're due, overdue ones, and in the evening tomorrow's undated and morning ones; not done,
     * dismissed or snoozed there.
     */
    fun getHomeCardReminders(
        nowMillis: Long = System.currentTimeMillis(),
        isShown: Boolean = preferences.isShowUpcomingRemindersEnabled(),
        isTomorrowShown: Boolean = preferences.isShowTomorrowRemindersEnabled(),
    ): List<ReminderInfo> {
        if (!isShown) return emptyList()
        val snoozedUntil = preferences.getHomeSnoozedUntil()
        val hiddenUntil = preferences.getHomeHiddenUntil()
        val showTomorrow = isTomorrowShown && GlanceTiming.isEvening(nowMillis)
        return readReminders()
            .filter { reminder ->
                !reminder.isDone &&
                    !reminder.isDismissedFromHome &&
                    !reminder.isDismissedFromHomeToday(nowMillis) &&
                    (snoozedUntil[reminder.reminderId] ?: 0L) <= nowMillis &&
                    (hiddenUntil[reminder.reminderId] ?: 0L) <= nowMillis &&
                    (glanceVisibleFromMillis(reminder) <= nowMillis || showTomorrow && isTomorrowEveningItem(reminder, nowMillis))
            }
            .sortedBy { it.dueMillis }
    }

    /** Undone reminders from today onward, soonest first; shown for the Reminders alias with no query. */
    fun getUpcomingReminders(limit: Int): List<ReminderInfo> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return readReminders()
            .filter { !it.isDone && !it.date.isBefore(today) }
            .sortedBy { it.dueMillis }
            .take(limit)
    }

    /** [getUpcomingReminders] with Noted's upcoming tasks mixed in while "Noted Integration" is on. */
    fun getUpcomingRemindersAndNotedTasks(limit: Int): List<ReminderInfo> =
        (getUpcomingReminders(limit) + NotedGlanceTasks.upcomingAsReminders(appContext, limit))
            .sortedBy { it.dueMillis }
            .take(limit)

    fun createReminder(
        title: String,
        date: LocalDate,
        timeMinutes: Int?,
        repeat: ScheduleRepeat? = null,
    ): ReminderInfo {
        lateinit var reminder: ReminderInfo
        mutate { reminders ->
            reminder =
                ReminderInfo(
                    reminderId = preferences.nextReminderId(),
                    title = title.trim(),
                    date = date,
                    timeMinutes = timeMinutes,
                    repeat = repeat,
                )
            reminders + reminder
        }
        ReminderScheduler.schedule(appContext, reminder)
        return reminder
    }

    fun updateReminder(
        reminderId: Long,
        title: String,
        date: LocalDate,
        timeMinutes: Int?,
        repeat: ScheduleRepeat? = getReminderById(reminderId)?.repeat,
    ): ReminderInfo? {
        val before = getReminderById(reminderId)
        val updated =
            updateOne(reminderId) { reminder ->
                val scheduleChanged = reminder.date != date || reminder.timeMinutes != timeMinutes
                reminder.copy(
                    title = title.trim(),
                    date = date,
                    timeMinutes = timeMinutes,
                    repeat = repeat,
                    // A new time is a new occurrence, so it may surface on Home again.
                    isDismissedFromHome = reminder.isDismissedFromHome && !scheduleChanged,
                    dismissedFromHomeDate = reminder.dismissedFromHomeDate.takeUnless { scheduleChanged },
                )
            } ?: return null
        // A title-only edit keeps the snooze; a new time replaces it.
        if (before?.date != date || before.timeMinutes != timeMinutes) {
            clearSnooze(reminderId)
            preferences.setHomeHiddenUntil(reminderId, null)
        }
        ReminderScheduler.cancelNotification(appContext, reminderId)
        ReminderScheduler.schedule(appContext, updated, snoozedUntilMillis = getSnoozedUntil()[reminderId])
        return updated
    }

    fun deleteReminder(reminderId: Long) {
        mutate { reminders -> reminders.filterNot { it.reminderId == reminderId } }
        ReminderScheduler.cancel(appContext, reminderId)
        ReminderScheduler.cancelNotification(appContext, reminderId)
        clearSnooze(reminderId)
        preferences.unpinReminder(reminderId)
    }

    /**
     * Marks the reminder done or not. A repeating reminder isn't finished by Done: it moves to its
     * next date and stays open.
     */
    fun setDone(reminderId: Long, isDone: Boolean, nowMillis: Long = System.currentTimeMillis()): ReminderInfo? {
        val repeat = getReminderById(reminderId)?.repeat
        if (isDone && repeat != null) return moveToNextOccurrence(reminderId, repeat, nowMillis)
        val updated = updateOne(reminderId) { it.copy(isDone = isDone) } ?: return null
        if (isDone) {
            clearSnooze(reminderId)
            ReminderScheduler.cancel(appContext, reminderId)
            ReminderScheduler.cancelNotification(appContext, reminderId)
        } else {
            ReminderScheduler.schedule(appContext, updated)
        }
        return updated
    }

    private fun moveToNextOccurrence(reminderId: Long, repeat: ScheduleRepeat, nowMillis: Long): ReminderInfo? {
        val updated =
            updateOne(reminderId) { reminder ->
                reminder.copy(
                    date = ReminderRepeat.nextDate(reminder, repeat, nowMillis),
                    isDone = false,
                    isDismissedFromHome = false,
                    dismissedFromHomeDate = null,
                )
            } ?: return null
        clearSnooze(reminderId)
        preferences.setHomeHiddenUntil(reminderId, null)
        ReminderScheduler.cancelNotification(appContext, reminderId)
        ReminderScheduler.schedule(appContext, updated, nowMillis)
        return updated
    }

    fun getSnoozedUntil(): Map<Long, Long> = preferences.getHomeSnoozedUntil()

    fun isNotificationShowing(reminderId: Long): Boolean =
        ReminderScheduler.isNotificationShowing(appContext, reminderId)

    /**
     * Snoozes the reminder for 30 minutes from both the Home card and its notification, without
     * changing its due time: it is hidden on Home and its notification is cleared until then, when it
     * comes back overdue. The Home and notification Snooze buttons both land here.
     */
    fun snooze(reminderId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val reminder = getReminderById(reminderId) ?: return
        val snoozedUntil = nowMillis + SNOOZE_MILLIS
        preferences.setHomeSnoozedUntil(reminderId, snoozedUntil, nowMillis)
        ReminderScheduler.cancelNotification(appContext, reminderId)
        ReminderScheduler.schedule(appContext, reminder, nowMillis, snoozedUntil)
        notifyChanged()
    }

    /**
     * Hides a reminder that isn't due yet from the Home card until its due time, when it comes back
     * with its notification.
     */
    fun hideFromHomeUntilDue(reminderId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val reminder = getReminderById(reminderId) ?: return
        preferences.setHomeSnoozedUntil(reminderId, reminder.dueMillis, nowMillis)
        notifyChanged()
    }

    /**
     * Dismisses a reminder from At a Glance. Tomorrow's reminder dismissed the evening before comes
     * back the next morning (or when due, if earlier); one dismissed more than 30 minutes before it's
     * due comes back when due, as with [hideFromHomeUntilDue]; an overdue one is hidden for the rest
     * of the day, and one due within 30 minutes is marked done.
     */
    fun dismissFromHome(reminderId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val reminder = getReminderById(reminderId) ?: return
        when {
            isTomorrowEveningItem(reminder, nowMillis) && GlanceTiming.isEvening(nowMillis) -> {
                val morning = GlanceTiming.morningStartMillis(reminder.date)
                preferences.setHomeHiddenUntil(reminderId, minOf(morning, reminder.dueMillis), nowMillis)
                notifyChanged()
            }
            canDismissUntilDue(reminder, nowMillis) -> hideFromHomeUntilDue(reminderId, nowMillis)
            reminder.isOverdue(nowMillis) -> {
                preferences.setHomeHiddenUntil(reminderId, GlanceTiming.startOfDayMillis(GlanceTiming.localDate(nowMillis).plusDays(1)), nowMillis)
                notifyChanged()
            }
            else -> setDone(reminderId, true)
        }
    }

    /** Undoes [snooze]; re-posts the notification when [restoreNotification] (it was showing before). */
    fun unsnooze(reminderId: Long, restoreNotification: Boolean) {
        clearSnooze(reminderId)
        val reminder = getReminderById(reminderId) ?: return
        ReminderScheduler.schedule(appContext, reminder)
        if (restoreNotification && !reminder.isDone) ReminderScheduler.showNotification(appContext, reminder)
        notifyChanged()
    }

    private fun clearSnooze(reminderId: Long) {
        if (reminderId in preferences.getHomeSnoozedUntil()) preferences.setHomeSnoozedUntil(reminderId, null)
    }

    fun getPinnedReminderOrder(): List<Long> = preferences.getPinnedReminderOrder()

    /**
     * Puts back a reminder exactly as [snapshot] had it, to undo a Home card action. Re-pins it at its
     * old place when [pinnedOrder] (read before a delete) contains it.
     */
    fun restore(snapshot: ReminderInfo, pinnedOrder: List<Long>? = null) {
        mutate { reminders ->
            if (reminders.any { it.reminderId == snapshot.reminderId }) {
                reminders.map { if (it.reminderId == snapshot.reminderId) snapshot else it }
            } else {
                reminders + snapshot
            }
        }
        ReminderScheduler.schedule(appContext, snapshot)
        if (pinnedOrder != null && snapshot.reminderId in pinnedOrder) {
            preferences.pinReminder(snapshot.reminderId)
            val current = preferences.getPinnedReminderOrder()
            preferences.setPinnedReminderOrder(pinnedOrder.filter { it in current } + current.filterNot { it in pinnedOrder })
        }
    }

    fun getIncludePastReminders(): Boolean = preferences.getIncludePastReminders()

    private fun isVisibleWhenPastHidden(reminder: ReminderInfo, today: LocalDate): Boolean =
        !reminder.date.isBefore(today)

    private fun updateOne(
        reminderId: Long,
        transform: (ReminderInfo) -> ReminderInfo,
    ): ReminderInfo? {
        var updated: ReminderInfo? = null
        mutate { reminders ->
            reminders.map { reminder ->
                if (reminder.reminderId == reminderId) {
                    transform(reminder).also { updated = it }
                } else {
                    reminder
                }
            }
        }
        return updated
    }

    private fun mutate(transform: (List<ReminderInfo>) -> List<ReminderInfo>) {
        synchronized(lock) {
            writeReminders(transform(readReminders()))
        }
        notifyChanged()
    }

    private fun readReminders(): List<ReminderInfo> {
        val raw = preferences.getRemindersJson()
        // Preferences hand back the same string until the next write, so this is usually an
        // identity check; only a changed value is parsed again.
        parsedCache?.let { (cachedRaw, reminders) -> if (cachedRaw === raw || cachedRaw == raw) return reminders }
        return parseReminders(raw).also { parsedCache = raw to it }
    }

    private fun writeReminders(reminders: List<ReminderInfo>) {
        preferences.setRemindersJson(serializeReminders(reminders))
    }

    companion object {
        const val SNOOZE_MILLIS = 30L * 60L * 1000L

        /** Most reminders a regular search returns; the collapsed card shows only the first. */
        const val SEARCH_RESULT_LIMIT = 25

        /** The last reminders JSON read and its parsed reminders, shared by every repository. */
        @Volatile private var parsedCache: Pair<String, List<ReminderInfo>>? = null

        /** When [reminder] joins At a Glance: from the morning for an undated one, else ahead of its time. */
        fun glanceVisibleFromMillis(reminder: ReminderInfo): Long =
            if (reminder.hasTime) {
                reminder.dueMillis - GlanceTiming.LEAD_MILLIS
            } else {
                GlanceTiming.morningStartMillis(reminder.date)
            }

        /** More than 30 minutes before it's due, dismissing hides it until then; later it marks it done. */
        fun canDismissUntilDue(reminder: ReminderInfo, nowMillis: Long): Boolean =
            nowMillis < reminder.dueMillis - GlanceTiming.DISMISS_DONE_WINDOW_MILLIS

        /** Tomorrow's undated or morning reminder, shown the evening before. */
        fun isTomorrowEveningItem(reminder: ReminderInfo, nowMillis: Long): Boolean {
            val tomorrow = GlanceTiming.localDate(nowMillis).plusDays(1)
            return reminder.date == tomorrow &&
                (!reminder.hasTime || GlanceTiming.isTomorrowMorning(reminder.dueMillis, tomorrow))
        }

        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_DATE = "date"
        private const val FIELD_TIME_MINUTES = "timeMinutes"
        private const val FIELD_DONE = "done"
        private const val FIELD_DISMISSED_FROM_HOME = "dismissedFromHome"
        private const val FIELD_DISMISSED_FROM_HOME_DATE = "dismissedFromHomeDate"
        private const val FIELD_REPEAT_INTERVAL = "repeatInterval"
        private const val FIELD_REPEAT_UNIT = "repeatUnit"

        private val lock = Any()
        private val _changes = MutableStateFlow(0)

        /** Increments whenever reminders change, including from notification actions. */
        val changes: StateFlow<Int> = _changes.asStateFlow()

        fun notifyChanged() {
            _changes.update { it + 1 }
        }

        internal fun parseReminders(raw: String): List<ReminderInfo> {
            if (raw.isBlank()) return emptyList()
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val id = obj.optLong(FIELD_ID, 0L)
                val title = obj.optString(FIELD_TITLE).orEmpty()
                val date = runCatching { LocalDate.parse(obj.optString(FIELD_DATE)) }.getOrNull()
                if (id <= 0L || title.isBlank() || date == null) return@mapNotNull null
                ReminderInfo(
                    reminderId = id,
                    title = title,
                    date = date,
                    timeMinutes =
                        if (obj.has(FIELD_TIME_MINUTES)) {
                            obj.optInt(FIELD_TIME_MINUTES).coerceIn(0, 24 * 60 - 1)
                        } else {
                            null
                        },
                    isDone = obj.optBoolean(FIELD_DONE, false),
                    isDismissedFromHome = obj.optBoolean(FIELD_DISMISSED_FROM_HOME, false),
                    dismissedFromHomeDate =
                        obj.optString(FIELD_DISMISSED_FROM_HOME_DATE)
                            .takeIf { it.isNotBlank() }
                            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() },
                    repeat = ReminderRepeat.repeat(obj.optInt(FIELD_REPEAT_INTERVAL, 1), obj.optString(FIELD_REPEAT_UNIT)),
                )
            }
        }

        internal fun serializeReminders(reminders: List<ReminderInfo>): String {
            val array = JSONArray()
            reminders.forEach { reminder ->
                array.put(
                    JSONObject()
                        .put(FIELD_ID, reminder.reminderId)
                        .put(FIELD_TITLE, reminder.title)
                        .put(FIELD_DATE, reminder.date.toString())
                        .apply { reminder.timeMinutes?.let { put(FIELD_TIME_MINUTES, it) } }
                        .put(FIELD_DONE, reminder.isDone)
                        .put(FIELD_DISMISSED_FROM_HOME, reminder.isDismissedFromHome)
                        .apply {
                            reminder.dismissedFromHomeDate?.let {
                                put(FIELD_DISMISSED_FROM_HOME_DATE, it.toString())
                            }
                            reminder.repeat?.let {
                                put(FIELD_REPEAT_INTERVAL, it.interval)
                                put(FIELD_REPEAT_UNIT, ReminderRepeat.unitName(it.unit))
                            }
                        },
                )
            }
            return array.toString()
        }

    }
}
