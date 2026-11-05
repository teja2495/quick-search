package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.customInfo.CustomInfoItem
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.search.data.AppNotificationsSettings
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetUpdater

/** An At a Glance toggle; [key] names it in the At a Glance widget's stored overrides. */
enum class GlanceItem(
    val key: String,
) {
    NOW_PLAYING("now_playing"),
    TODAY_EVENTS("today_events"),
    TOMORROW_EVENTS("tomorrow_events"),
    ONGOING_CALL("ongoing_call"),
    OTP_CODES("otp_codes"),
    APP_NOTIFICATIONS("app_notifications"),
    MISSED_CALLS("missed_calls"),
    DO_NOT_DISTURB("do_not_disturb"),
    AIRPLANE_MODE("airplane_mode"),
    HOTSPOT("hotspot"),
    WIFI_SIGN_IN("wifi_sign_in"),
    FLASHLIGHT("flashlight"),
    UPCOMING_ALARM("upcoming_alarm"),
    TOMORROW_ALARM("tomorrow_alarm"),
    WEATHER("weather"),
    WORKOUTS("workouts"),
    TIMERS("timers"),
    REMINDERS("reminders"),
    TOMORROW_REMINDERS("tomorrow_reminders"),
    BIRTHDAYS("birthdays"),
    PROGRESS("progress"),
    LOW_BATTERY("low_battery"),
    CHARGING("charging"),
    LOW_STORAGE("low_storage"),
}

/** Which At a Glance items show, for the home card or for an At a Glance widget. */
interface GlanceItemSettings {
    fun isShown(item: GlanceItem): Boolean

    fun setShown(
        item: GlanceItem,
        shown: Boolean,
    )

    fun isCustomInfoShown(item: CustomInfoItem): Boolean

    fun setCustomInfoShown(
        item: CustomInfoItem,
        shown: Boolean,
    )
}

/** The home card's At a Glance settings, kept in the existing per-feature preferences. */
class AppGlanceItemSettings(
    context: Context,
) : GlanceItemSettings {
    private val appContext = context.applicationContext
    private val glance by lazy { GlancePreferences(appContext) }
    private val calendar by lazy { CalendarPreferences(appContext) }
    private val alarms by lazy { UpcomingAlarmPreferences(appContext) }
    private val reminders by lazy { ReminderPreferences(appContext) }
    private val media by lazy { MediaPreferences(appContext) }
    private val battery by lazy { BatteryPreferences(appContext) }

    override fun isShown(item: GlanceItem): Boolean =
        when (item) {
            GlanceItem.NOW_PLAYING -> media.isShowNowPlayingEnabled()
            GlanceItem.TODAY_EVENTS -> calendar.getShowTodayEvents()
            GlanceItem.TOMORROW_EVENTS -> glance.isShowTomorrowEventsEnabled()
            GlanceItem.ONGOING_CALL -> glance.isShowOngoingCallEnabled()
            GlanceItem.OTP_CODES -> glance.isShowOtpCodesEnabled()
            GlanceItem.APP_NOTIFICATIONS -> glance.isShowAppNotificationsEnabled()
            GlanceItem.MISSED_CALLS -> glance.isShowMissedCallsEnabled()
            GlanceItem.DO_NOT_DISTURB -> glance.isShowDoNotDisturbEnabled()
            GlanceItem.AIRPLANE_MODE -> glance.isShowAirplaneModeEnabled()
            GlanceItem.HOTSPOT -> glance.isShowHotspotEnabled()
            GlanceItem.WIFI_SIGN_IN -> glance.isShowWifiSignInEnabled()
            GlanceItem.FLASHLIGHT -> glance.isShowFlashlightEnabled()
            GlanceItem.UPCOMING_ALARM -> alarms.isShowUpcomingAlarmEnabled()
            GlanceItem.TOMORROW_ALARM -> alarms.isShowTomorrowAlarmEnabled()
            GlanceItem.WEATHER -> glance.isShowWeatherEnabled()
            GlanceItem.WORKOUTS -> glance.isShowWorkoutsEnabled()
            GlanceItem.TIMERS -> glance.isShowTimersEnabled()
            GlanceItem.REMINDERS -> reminders.isShowUpcomingRemindersEnabled()
            GlanceItem.TOMORROW_REMINDERS -> reminders.isShowTomorrowRemindersEnabled()
            GlanceItem.BIRTHDAYS -> glance.isShowBirthdaysEnabled()
            GlanceItem.PROGRESS -> glance.isShowProgressNotificationsEnabled()
            GlanceItem.LOW_BATTERY -> battery.isShowLowBatteryEnabled()
            GlanceItem.CHARGING -> battery.isShowChargingEnabled()
            GlanceItem.LOW_STORAGE -> glance.isShowLowStorageEnabled()
        }

    override fun setShown(
        item: GlanceItem,
        shown: Boolean,
    ) {
        when (item) {
            GlanceItem.NOW_PLAYING -> media.setShowNowPlayingEnabled(shown)
            GlanceItem.TODAY_EVENTS -> calendar.setShowTodayEvents(shown)
            GlanceItem.TOMORROW_EVENTS -> glance.setShowTomorrowEventsEnabled(shown)
            GlanceItem.ONGOING_CALL -> glance.setShowOngoingCallEnabled(shown)
            GlanceItem.OTP_CODES -> glance.setShowOtpCodesEnabled(shown)
            GlanceItem.APP_NOTIFICATIONS -> AppNotificationsSettings.setEnabled(appContext, shown)
            GlanceItem.MISSED_CALLS -> glance.setShowMissedCallsEnabled(shown)
            GlanceItem.DO_NOT_DISTURB -> glance.setShowDoNotDisturbEnabled(shown)
            GlanceItem.AIRPLANE_MODE -> glance.setShowAirplaneModeEnabled(shown)
            GlanceItem.HOTSPOT -> glance.setShowHotspotEnabled(shown)
            GlanceItem.WIFI_SIGN_IN -> glance.setShowWifiSignInEnabled(shown)
            GlanceItem.FLASHLIGHT -> glance.setShowFlashlightEnabled(shown)
            GlanceItem.UPCOMING_ALARM -> alarms.setShowUpcomingAlarmEnabled(shown)
            GlanceItem.TOMORROW_ALARM -> alarms.setShowTomorrowAlarmEnabled(shown)
            GlanceItem.WEATHER -> glance.setShowWeatherEnabled(shown)
            GlanceItem.WORKOUTS -> glance.setShowWorkoutsEnabled(shown)
            GlanceItem.TIMERS -> glance.setShowTimersEnabled(shown)
            GlanceItem.REMINDERS -> reminders.setShowUpcomingRemindersEnabled(shown)
            GlanceItem.TOMORROW_REMINDERS -> reminders.setShowTomorrowRemindersEnabled(shown)
            GlanceItem.BIRTHDAYS -> glance.setShowBirthdaysEnabled(shown)
            GlanceItem.PROGRESS -> glance.setShowProgressNotificationsEnabled(shown)
            GlanceItem.LOW_BATTERY -> battery.setShowLowBatteryEnabled(shown)
            GlanceItem.CHARGING -> battery.setShowChargingEnabled(shown)
            GlanceItem.LOW_STORAGE -> glance.setShowLowStorageEnabled(shown)
        }
        // Placed widgets follow these settings unless overridden, and nothing else reports the change.
        AtAGlanceWidgetUpdater.startIfPlaced(appContext)
    }

    override fun isCustomInfoShown(item: CustomInfoItem): Boolean = item.enabled

    override fun setCustomInfoShown(
        item: CustomInfoItem,
        shown: Boolean,
    ) {
        CustomInfoScheduler.setEnabled(appContext, item.id, shown)
        AtAGlanceWidgetUpdater.startIfPlaced(appContext)
    }
}

/**
 * A new At a Glance widget's overrides: none, so it follows [app], unless [app] shows no items at all,
 * in which case every item starts on so the widget has something to show until the user configures it.
 */
fun newWidgetGlanceItems(app: GlanceItemSettings): Map<String, Boolean> =
    if (GlanceItem.entries.any(app::isShown)) emptyMap() else GlanceItem.entries.associate { it.key to true }

/**
 * An At a Glance widget's settings: [overrides] holds what the user changed for the widget, and
 * everything else follows [app]. A Custom Info item the app turned off never runs, so the widget
 * can only hide one the app shows. [onChange] receives the updated overrides.
 */
class WidgetGlanceItemSettings(
    private val overrides: Map<String, Boolean>,
    private val app: GlanceItemSettings,
    private val onChange: (Map<String, Boolean>) -> Unit = {},
) : GlanceItemSettings {
    override fun isShown(item: GlanceItem): Boolean = overrides[item.key] ?: app.isShown(item)

    override fun setShown(
        item: GlanceItem,
        shown: Boolean,
    ) = onChange(overrides + (item.key to shown))

    override fun isCustomInfoShown(item: CustomInfoItem): Boolean =
        app.isCustomInfoShown(item) && (overrides[customInfoKey(item.id)] ?: true)

    override fun setCustomInfoShown(
        item: CustomInfoItem,
        shown: Boolean,
    ) = onChange(overrides + (customInfoKey(item.id) to shown))

    private fun customInfoKey(id: Int) = "$CUSTOM_INFO_KEY_PREFIX$id"

    private companion object {
        const val CUSTOM_INFO_KEY_PREFIX = "custom_info_"
    }
}
