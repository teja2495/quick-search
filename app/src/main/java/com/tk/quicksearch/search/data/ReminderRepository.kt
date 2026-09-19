package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.reminders.ReminderScheduler
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.preferences.NicknamePreferences
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.time.Instant
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

    init {
        // TODO: Remove this migration once most users have updated past the release that added reminders.
        migrateCustomCalendarEvents()
    }

    /**
     * Calendar events created inside Quick Search are replaced by reminders. Moves them, with their pinned
     * state, into reminders and clears the old store. Hidden events come back visible, since reminders
     * can't be hidden. A no-op once that store is empty, and it
     * also picks up events restored from an older backup.
     */
    private fun migrateCustomCalendarEvents() {
        val calendarPreferences = CalendarPreferences(appContext)
        if (calendarPreferences.getCustomEventsJson().isBlank()) return
        // Several repositories can be created at once; the lock keeps events from migrating twice.
        synchronized(lock) {
            val raw = calendarPreferences.getCustomEventsJson()
            if (raw.isBlank()) return
            migrateCustomCalendarEvents(calendarPreferences, raw)
        }
    }

    private fun migrateCustomCalendarEvents(calendarPreferences: CalendarPreferences, raw: String) {
        val events = parseLegacyCustomEvents(raw)
        val pinnedEventIds = calendarPreferences.getPinnedEventIds()
        val nicknamePreferences = NicknamePreferences(appContext)
        val nowMillis = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()

        val migrated =
            events.map { event ->
                val reminder =
                    legacyCustomEventToReminder(event, preferences.nextReminderId(), nowMillis, zoneId)
                if (event.eventId in pinnedEventIds) preferences.pinReminder(reminder.reminderId)
                calendarPreferences.unpinEvent(event.eventId)
                calendarPreferences.removeExcludedEvent(event.eventId)
                nicknamePreferences.setCalendarEventNickname(event.eventId, null)
                reminder
            }
        if (migrated.isNotEmpty()) {
            mutate { reminders -> reminders + migrated }
            migrated.forEach { ReminderScheduler.schedule(appContext, it, nowMillis) }
        }
        calendarPreferences.setCustomEventsJson("")
    }

    fun getAllReminders(): List<ReminderInfo> = readReminders()

    fun getReminderById(reminderId: Long): ReminderInfo? =
        readReminders().firstOrNull { it.reminderId == reminderId }

    fun getRemindersByIds(reminderIds: Collection<Long>): List<ReminderInfo> {
        if (reminderIds.isEmpty()) return emptyList()
        val ids = reminderIds.toSet()
        return readReminders().filter { it.reminderId in ids }
    }

    fun searchReminders(
        query: String,
        includePastReminders: Boolean,
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
            .map { it.first }
            .toList()
    }

    /** Reminders to surface on Home: due within 30 minutes or overdue, and not done or dismissed. */
    fun getHomeCardReminders(nowMillis: Long = System.currentTimeMillis()): List<ReminderInfo> =
        readReminders()
            .filter { reminder ->
                !reminder.isDone &&
                    !reminder.isDismissedFromHome &&
                    reminder.dueMillis - HOME_CARD_LEAD_MILLIS <= nowMillis
            }
            .sortedBy { it.dueMillis }

    /** Undone reminders from today onward, soonest first; shown for the Reminders alias with no query. */
    fun getUpcomingReminders(limit: Int): List<ReminderInfo> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return readReminders()
            .filter { !it.isDone && !it.date.isBefore(today) }
            .sortedBy { it.dueMillis }
            .take(limit)
    }

    fun createReminder(title: String, date: LocalDate, timeMinutes: Int?): ReminderInfo {
        lateinit var reminder: ReminderInfo
        mutate { reminders ->
            reminder =
                ReminderInfo(
                    reminderId = preferences.nextReminderId(),
                    title = title.trim(),
                    date = date,
                    timeMinutes = timeMinutes,
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
    ): ReminderInfo? {
        val updated =
            updateOne(reminderId) { reminder ->
                val scheduleChanged = reminder.date != date || reminder.timeMinutes != timeMinutes
                reminder.copy(
                    title = title.trim(),
                    date = date,
                    timeMinutes = timeMinutes,
                    // A new time is a new occurrence, so it may surface on Home again.
                    isDismissedFromHome = reminder.isDismissedFromHome && !scheduleChanged,
                )
            } ?: return null
        ReminderScheduler.cancelNotification(appContext, reminderId)
        ReminderScheduler.schedule(appContext, updated)
        return updated
    }

    fun deleteReminder(reminderId: Long) {
        mutate { reminders -> reminders.filterNot { it.reminderId == reminderId } }
        ReminderScheduler.cancel(appContext, reminderId)
        ReminderScheduler.cancelNotification(appContext, reminderId)
        preferences.unpinReminder(reminderId)
    }

    fun setDone(reminderId: Long, isDone: Boolean): ReminderInfo? {
        val updated = updateOne(reminderId) { it.copy(isDone = isDone) } ?: return null
        if (isDone) {
            ReminderScheduler.cancel(appContext, reminderId)
            ReminderScheduler.cancelNotification(appContext, reminderId)
        } else {
            ReminderScheduler.schedule(appContext, updated)
        }
        return updated
    }

    fun dismissFromHome(reminderId: Long): ReminderInfo? =
        updateOne(reminderId) { it.copy(isDismissedFromHome = true) }

    /** Moves the reminder 30 minutes past its due time, or past now if it is already overdue. */
    fun snooze(reminderId: Long, nowMillis: Long = System.currentTimeMillis()): ReminderInfo? {
        val updated =
            updateOne(reminderId) { reminder ->
                val zoneId = ZoneId.systemDefault()
                val snoozedUntil =
                    Instant.ofEpochMilli(maxOf(reminder.dueMillis, nowMillis) + SNOOZE_MILLIS)
                        .atZone(zoneId)
                        .toLocalDateTime()
                reminder.copy(
                    date = snoozedUntil.toLocalDate(),
                    timeMinutes = snoozedUntil.hour * 60 + snoozedUntil.minute,
                    isDone = false,
                )
            } ?: return null
        ReminderScheduler.cancelNotification(appContext, reminderId)
        ReminderScheduler.schedule(appContext, updated)
        return updated
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

    private fun readReminders(): List<ReminderInfo> = parseReminders(preferences.getRemindersJson())

    private fun writeReminders(reminders: List<ReminderInfo>) {
        preferences.setRemindersJson(serializeReminders(reminders))
    }

    companion object {
        const val SNOOZE_MILLIS = 30L * 60L * 1000L
        const val HOME_CARD_LEAD_MILLIS = 30L * 60L * 1000L

        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_DATE = "date"
        private const val FIELD_TIME_MINUTES = "timeMinutes"
        private const val FIELD_DONE = "done"
        private const val FIELD_DISMISSED_FROM_HOME = "dismissedFromHome"

        private const val LEGACY_FIELD_EVENT_ID = "eventId"
        private const val LEGACY_FIELD_TITLE = "title"
        private const val LEGACY_FIELD_DATE_TIME_MILLIS = "dateTimeMillis"
        private const val LEGACY_FIELD_ALL_DAY = "allDay"

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
                        .put(FIELD_DISMISSED_FROM_HOME, reminder.isDismissedFromHome),
                )
            }
            return array.toString()
        }

        internal data class LegacyCustomEvent(
            val eventId: Long,
            val title: String,
            val dateTimeMillis: Long,
            val allDay: Boolean,
        )

        internal fun parseLegacyCustomEvents(raw: String): List<LegacyCustomEvent> {
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val eventId = obj.optLong(LEGACY_FIELD_EVENT_ID, 0L)
                val title = obj.optString(LEGACY_FIELD_TITLE).orEmpty()
                val dateTimeMillis = obj.optLong(LEGACY_FIELD_DATE_TIME_MILLIS, 0L)
                if (eventId == 0L || title.isBlank() || dateTimeMillis <= 0L) return@mapNotNull null
                LegacyCustomEvent(
                    eventId = eventId,
                    title = title,
                    dateTimeMillis = dateTimeMillis,
                    allDay = obj.optBoolean(LEGACY_FIELD_ALL_DAY, true),
                )
            }
        }

        /**
         * All-day events become reminders without a time. Events already in the past are marked done and
         * kept off Home, so the migration doesn't surface a pile of overdue reminders.
         */
        internal fun legacyCustomEventToReminder(
            event: LegacyCustomEvent,
            reminderId: Long,
            nowMillis: Long,
            zoneId: ZoneId,
        ): ReminderInfo {
            val dateTime = Instant.ofEpochMilli(event.dateTimeMillis).atZone(zoneId)
            val reminder =
                ReminderInfo(
                    reminderId = reminderId,
                    title = event.title.trim(),
                    date = dateTime.toLocalDate(),
                    timeMinutes = if (event.allDay) null else dateTime.hour * 60 + dateTime.minute,
                )
            val isPast =
                if (event.allDay) {
                    reminder.date.isBefore(Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate())
                } else {
                    reminder.dueMillis(zoneId) <= nowMillis
                }
            return if (isPast) reminder.copy(isDone = true, isDismissedFromHome = true) else reminder
        }
    }
}
