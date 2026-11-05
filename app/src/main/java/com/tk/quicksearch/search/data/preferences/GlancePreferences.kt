package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.search.data.AppNotificationFilters
import com.tk.quicksearch.search.data.AppNotificationFiltersCodec

/**
 * Stores the home At a Glance toggles for contact birthdays, low storage, running timers, live
 * ongoing (progress and Live Update) notifications, missed calls, ongoing calls, workouts, one-time codes, weather, Do Not Disturb, airplane mode, hotspot,
 * Wi-Fi sign-in, flashlight and App notifications (with its apps and keywords), plus the birthdays dismissed for the current day, tomorrow's events dismissed tonight and the low storage row's dismissal.
 */
class GlancePreferences(context: Context) : BasePreferences(context) {
    fun isShowBirthdaysEnabled(): Boolean = getBooleanPref(KEY_SHOW_BIRTHDAYS, true)

    fun setShowBirthdaysEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_BIRTHDAYS, enabled)

    fun isShowLowStorageEnabled(): Boolean = getBooleanPref(KEY_SHOW_LOW_STORAGE, true)

    fun setShowLowStorageEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_LOW_STORAGE, enabled)

    fun isShowTimersEnabled(): Boolean = getBooleanPref(KEY_SHOW_TIMERS, true)

    fun setShowTimersEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_TIMERS, enabled)

    fun isShowProgressNotificationsEnabled(): Boolean = getBooleanPref(KEY_SHOW_PROGRESS_NOTIFICATIONS, true)

    fun setShowProgressNotificationsEnabled(enabled: Boolean) =
        setBooleanPref(KEY_SHOW_PROGRESS_NOTIFICATIONS, enabled)

    fun isShowMissedCallsEnabled(): Boolean = getBooleanPref(KEY_SHOW_MISSED_CALLS, true)

    fun setShowMissedCallsEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_MISSED_CALLS, enabled)

    fun isShowDoNotDisturbEnabled(): Boolean = getBooleanPref(KEY_SHOW_DO_NOT_DISTURB, true)

    fun setShowDoNotDisturbEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_DO_NOT_DISTURB, enabled)

    fun isShowOngoingCallEnabled(): Boolean = getBooleanPref(KEY_SHOW_ONGOING_CALL, true)

    fun setShowOngoingCallEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_ONGOING_CALL, enabled)

    fun isShowWorkoutsEnabled(): Boolean = getBooleanPref(KEY_SHOW_WORKOUTS, true)

    fun setShowWorkoutsEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_WORKOUTS, enabled)

    fun isShowWeatherEnabled(): Boolean = getBooleanPref(KEY_SHOW_WEATHER, true)

    fun setShowWeatherEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_WEATHER, enabled)

    fun isShowOtpCodesEnabled(): Boolean = getBooleanPref(KEY_SHOW_OTP_CODES, true)

    fun setShowOtpCodesEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_OTP_CODES, enabled)

    fun isShowAirplaneModeEnabled(): Boolean = getBooleanPref(KEY_SHOW_AIRPLANE_MODE, true)

    fun setShowAirplaneModeEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_AIRPLANE_MODE, enabled)

    fun isShowHotspotEnabled(): Boolean = getBooleanPref(KEY_SHOW_HOTSPOT, true)

    fun setShowHotspotEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_HOTSPOT, enabled)

    fun isShowWifiSignInEnabled(): Boolean = getBooleanPref(KEY_SHOW_WIFI_SIGN_IN, true)

    fun setShowWifiSignInEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_WIFI_SIGN_IN, enabled)

    fun isShowFlashlightEnabled(): Boolean = getBooleanPref(KEY_SHOW_FLASHLIGHT, true)

    fun setShowFlashlightEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_FLASHLIGHT, enabled)

    fun isShowAppNotificationsEnabled(): Boolean = getBooleanPref(KEY_SHOW_APP_NOTIFICATIONS, false)

    fun setShowAppNotificationsEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_APP_NOTIFICATIONS, enabled)

    /** The apps and keywords App notifications shows, in the order the user added them. */
    fun getAppNotificationFilters(): AppNotificationFilters =
        AppNotificationFiltersCodec.decode(prefs.getString(KEY_APP_NOTIFICATION_FILTERS, null))

    fun setAppNotificationFilters(filters: AppNotificationFilters) =
        prefs.edit().putString(KEY_APP_NOTIFICATION_FILTERS, AppNotificationFiltersCodec.encode(filters)).apply()

    /**
     * Calls [onChange] on the main thread whenever the App notifications toggle, apps or keywords
     * change, including through a settings import. Preferences hold the listener weakly, so keep
     * the returned one referenced.
     */
    fun observeAppNotifications(onChange: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == KEY_SHOW_APP_NOTIFICATIONS || key == KEY_APP_NOTIFICATION_FILTERS) onChange()
        }.also(prefs::registerOnSharedPreferenceChangeListener)

    /** Tomorrow's all-day events and first morning event in the evening, under the calendar toggle. */
    fun isShowTomorrowEventsEnabled(): Boolean = getBooleanPref(KEY_SHOW_TOMORROW_EVENTS, true)

    fun setShowTomorrowEventsEnabled(enabled: Boolean) = setBooleanPref(KEY_SHOW_TOMORROW_EVENTS, enabled)

    /** Call time of the newest missed call when the row was dismissed; only later calls bring it back. */
    fun getMissedCallsDismissedAt(): Long = prefs.getLong(KEY_MISSED_CALLS_DISMISSED_AT, 0L)

    fun setMissedCallsDismissedAt(callTime: Long) = prefs.edit().putLong(KEY_MISSED_CALLS_DISMISSED_AT, callTime).apply()

    /** Contact ids dismissed on [day] (an ISO date); dismissals from earlier days are ignored. */
    fun getDismissedBirthdays(day: String): Set<Long> =
        getDayScopedIds(KEY_DISMISSED_BIRTHDAYS_DAY, KEY_DISMISSED_BIRTHDAYS, day)

    fun dismissBirthday(day: String, contactId: Long) =
        addDayScopedId(KEY_DISMISSED_BIRTHDAYS_DAY, KEY_DISMISSED_BIRTHDAYS, day, contactId)

    private fun getDayScopedIds(dayKey: String, idsKey: String, day: String): Set<Long> =
        if (prefs.getString(dayKey, null) == day) {
            getStringSet(idsKey).mapNotNull { it.toLongOrNull() }.toSet()
        } else {
            emptySet()
        }

    private fun addDayScopedId(dayKey: String, idsKey: String, day: String, id: Long) {
        val ids = getDayScopedIds(dayKey, idsKey, day) + id
        prefs.edit()
            .putString(dayKey, day)
            .putStringSet(idsKey, ids.map { it.toString() }.toSet())
            .apply()
    }

    /**
     * Free space, as a percent of the total, when the low storage row was dismissed (raised if space
     * is freed since), or null when it is not dismissed.
     */
    fun getLowStorageDismissedFreePercent(): Float? =
        if (prefs.contains(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT)) {
            prefs.getFloat(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT, 0f)
        } else {
            null
        }

    fun setLowStorageDismissedFreePercent(freePercent: Float?) {
        prefs.edit().apply {
            if (freePercent == null) {
                remove(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT)
            } else {
                putFloat(KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT, freePercent)
            }
        }.apply()
    }

    companion object {
        /** The low storage row appears once free space drops to this share of the total. */
        const val LOW_STORAGE_THRESHOLD_PERCENT = 10

        private const val KEY_SHOW_BIRTHDAYS = "home_show_birthdays"
        private const val KEY_SHOW_LOW_STORAGE = "home_show_low_storage"
        private const val KEY_SHOW_TIMERS = "home_show_timers"
        private const val KEY_SHOW_PROGRESS_NOTIFICATIONS = "home_show_progress_notifications"
        private const val KEY_DISMISSED_BIRTHDAYS_DAY = "home_dismissed_birthdays_day"
        private const val KEY_DISMISSED_BIRTHDAYS = "home_dismissed_birthdays"
        private const val KEY_SHOW_MISSED_CALLS = "home_show_missed_calls"
        private const val KEY_MISSED_CALLS_DISMISSED_AT = "home_missed_calls_dismissed_at"
        private const val KEY_SHOW_DO_NOT_DISTURB = "home_show_do_not_disturb"
        private const val KEY_SHOW_ONGOING_CALL = "home_show_ongoing_call"
        private const val KEY_SHOW_WORKOUTS = "home_show_workouts"
        private const val KEY_SHOW_OTP_CODES = "home_show_otp_codes"
        private const val KEY_SHOW_WEATHER = "home_show_weather"
        private const val KEY_SHOW_AIRPLANE_MODE = "home_show_airplane_mode"
        private const val KEY_SHOW_HOTSPOT = "home_show_hotspot"
        private const val KEY_SHOW_WIFI_SIGN_IN = "home_show_wifi_sign_in"
        private const val KEY_SHOW_FLASHLIGHT = "home_show_flashlight"
        private const val KEY_SHOW_APP_NOTIFICATIONS = "home_show_app_notifications"
        private const val KEY_APP_NOTIFICATION_FILTERS = "home_app_notification_filters"
        private const val KEY_SHOW_TOMORROW_EVENTS = "home_show_tomorrow_events"
        private const val KEY_LOW_STORAGE_DISMISSED_FREE_PERCENT = "home_low_storage_dismissed_free_percent"
    }
}
