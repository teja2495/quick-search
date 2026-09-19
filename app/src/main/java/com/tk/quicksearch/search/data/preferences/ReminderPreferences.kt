package com.tk.quicksearch.search.data.preferences

import android.content.Context

/** Preferences for reminder storage and pinned reminders. */
class ReminderPreferences(
    context: Context,
) : BasePreferences(context) {
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
}
