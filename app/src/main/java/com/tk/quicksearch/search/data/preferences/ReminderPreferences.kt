package com.tk.quicksearch.search.data.preferences

import android.content.Context

/** Preferences for reminder storage and pinned reminders. */
class ReminderPreferences(
    context: Context,
) : BasePreferences(context) {
    fun isShowUpcomingRemindersEnabled(): Boolean =
        getBooleanPref(BasePreferences.KEY_HOME_SHOW_UPCOMING_REMINDERS, true)

    fun setShowUpcomingRemindersEnabled(enabled: Boolean) =
        setBooleanPref(BasePreferences.KEY_HOME_SHOW_UPCOMING_REMINDERS, enabled)

    /** Tomorrow's reminders in the evening At a Glance rows; on by default, like tomorrow's events. */
    fun isShowTomorrowRemindersEnabled(): Boolean = getBooleanPref(KEY_SHOW_TOMORROW_REMINDERS, true)

    fun setShowTomorrowRemindersEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_TOMORROW_REMINDERS, enabled)

    /** New reminders go to the Noted app's To Do list instead; only offered while Noted is connected. */
    fun isUseNotedEnabled(): Boolean = getBooleanPref(KEY_USE_NOTED, false)

    fun setUseNotedEnabled(enabled: Boolean) = setBooleanPref(KEY_USE_NOTED, enabled)

    /** Reminder ids snoozed on the Home card, mapped to when they show again. Notifications follow it. */
    fun getHomeSnoozedUntil(): Map<Long, Long> = getReminderTimes(KEY_HOME_SNOOZED_UNTIL)

    /** Sets or, with a null [untilMillis], clears the Home snooze; drops snoozes that have ended. */
    fun setHomeSnoozedUntil(reminderId: Long, untilMillis: Long?, nowMillis: Long = System.currentTimeMillis()) =
        setReminderTime(KEY_HOME_SNOOZED_UNTIL, reminderId, untilMillis, nowMillis)

    /**
     * Reminders dismissed from tomorrow's evening rows, mapped to the next morning when they show
     * again. Unlike a snooze, this leaves the notification alone.
     */
    fun getHomeHiddenUntil(): Map<Long, Long> = getReminderTimes(KEY_HOME_HIDDEN_UNTIL)

    fun setHomeHiddenUntil(reminderId: Long, untilMillis: Long?, nowMillis: Long = System.currentTimeMillis()) =
        setReminderTime(KEY_HOME_HIDDEN_UNTIL, reminderId, untilMillis, nowMillis)

    private fun getReminderTimes(key: String): Map<Long, Long> =
        getStringSet(key).mapNotNull { entry ->
            val id = entry.substringBefore(':').toLongOrNull() ?: return@mapNotNull null
            val until = entry.substringAfter(':').toLongOrNull() ?: return@mapNotNull null
            id to until
        }.toMap()

    private fun setReminderTime(key: String, reminderId: Long, untilMillis: Long?, nowMillis: Long) {
        val entries = getReminderTimes(key).filter { (id, until) -> id != reminderId && until > nowMillis } +
            listOfNotNull(untilMillis?.let { reminderId to it })
        prefs.edit().putStringSet(key, entries.map { (id, until) -> "$id:$until" }.toSet()).apply()
    }

    /** Noted tasks dismissed from At a Glance, by Noted task id, mapped to when they show again. */
    fun getNotedHiddenUntil(): Map<String, Long> =
        getStringSet(KEY_NOTED_HIDDEN_UNTIL).mapNotNull { entry ->
            val until = entry.substringAfterLast(':').toLongOrNull() ?: return@mapNotNull null
            entry.substringBeforeLast(':') to until
        }.toMap()

    /** Hides Noted task [taskId] from At a Glance until [untilMillis]; drops hides that have ended. */
    fun setNotedHiddenUntil(taskId: String, untilMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        val entries = getNotedHiddenUntil().filter { (id, until) -> id != taskId && until > nowMillis } + (taskId to untilMillis)
        prefs.edit().putStringSet(KEY_NOTED_HIDDEN_UNTIL, entries.map { (id, until) -> "$id:$until" }.toSet()).apply()
    }

    /** [NotedItem.key]s of Noted tasks pinned on Home; they are pinned as reminders by [NotedGlanceTasks.reminderIdFor]. */
    fun getPinnedNotedKeys(): Set<String> = getStringSet(KEY_PINNED_NOTED_ITEMS)

    fun setNotedItemPinned(key: String, pinned: Boolean) {
        val keys = getPinnedNotedKeys()
        prefs.edit().putStringSet(KEY_PINNED_NOTED_ITEMS, if (pinned) keys + key else keys - key).apply()
    }

    fun getRemindersJson(): String = prefs.getString(BasePreferences.KEY_REMINDERS_DATA, null).orEmpty()

    fun setRemindersJson(json: String) {
        prefs.edit().putString(BasePreferences.KEY_REMINDERS_DATA, json).apply()
    }

    fun nextReminderId(): Long {
        val next = prefs.getLong(BasePreferences.KEY_REMINDER_ID_COUNTER, 1L)
        prefs.edit().putLong(BasePreferences.KEY_REMINDER_ID_COUNTER, next + 1L).apply()
        return next
    }

    fun getPinnedReminderIds(): Set<Long> = getPinnedLongItems(BasePreferences.KEY_PINNED_REMINDER_IDS)

    fun getPinnedReminderOrder(): List<Long> =
        getStringListPref(BasePreferences.KEY_PINNED_REMINDER_ORDER).mapNotNull { it.toLongOrNull() }

    fun setPinnedReminderOrder(order: List<Long>): List<Long> =
        order.distinct().also {
            setStringListPref(BasePreferences.KEY_PINNED_REMINDER_ORDER, it.map(Long::toString))
        }

    fun pinReminder(reminderId: Long): Set<Long> =
        pinLongItem(BasePreferences.KEY_PINNED_REMINDER_IDS, reminderId).also {
            if (reminderId !in getPinnedReminderOrder()) {
                setPinnedReminderOrder(getPinnedReminderOrder() + reminderId)
            }
        }

    fun unpinReminder(reminderId: Long): Set<Long> =
        unpinLongItem(BasePreferences.KEY_PINNED_REMINDER_IDS, reminderId).also {
            setPinnedReminderOrder(getPinnedReminderOrder().filterNot { it == reminderId })
        }

    fun getIncludePastReminders(): Boolean =
        getBooleanPref(BasePreferences.KEY_INCLUDE_PAST_REMINDERS, defaultValue = true)

    fun setIncludePastReminders(value: Boolean) =
        setBooleanPref(BasePreferences.KEY_INCLUDE_PAST_REMINDERS, value)

    fun hasRequestedReminderPermissions(): Boolean =
        getBooleanPref(BasePreferences.KEY_REMINDER_PERMISSIONS_REQUESTED, defaultValue = false)

    fun setRequestedReminderPermissions() =
        setBooleanPref(BasePreferences.KEY_REMINDER_PERMISSIONS_REQUESTED, true)

    private companion object {
        const val KEY_HOME_SNOOZED_UNTIL = "home_snoozed_reminders"
        const val KEY_HOME_HIDDEN_UNTIL = "home_hidden_reminders"
        const val KEY_SHOW_TOMORROW_REMINDERS = "home_show_tomorrow_reminders"
        const val KEY_USE_NOTED = "reminders_use_noted"
        const val KEY_NOTED_HIDDEN_UNTIL = "home_hidden_noted_tasks"
        const val KEY_PINNED_NOTED_ITEMS = "pinned_noted_items"
    }
}
