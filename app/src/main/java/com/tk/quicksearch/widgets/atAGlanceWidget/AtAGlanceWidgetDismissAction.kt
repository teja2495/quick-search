package com.tk.quicksearch.widgets.atAGlanceWidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.GlanceNotificationsStore
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.noted.NotedGlanceTasks
import com.tk.quicksearch.search.data.UpcomingAlarmRepository
import com.tk.quicksearch.search.data.dismissTodayGlanceEvent
import com.tk.quicksearch.search.data.dismissTomorrowGlanceEvent
import com.tk.quicksearch.search.data.preferences.BatteryPreferences
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.WifiSignInDismissal
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.freePercent
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.readInternalStorage
import com.tk.quicksearch.widgets.utils.refreshAtAGlanceWidgets
import java.time.LocalDate

/**
 * A row's close button. Hides the item the same way home's dismiss does, so it stays hidden on
 * home too, then redraws the widget. Runs in the background without opening an activity.
 */
class AtAGlanceWidgetDismissAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val type =
            parameters[TYPE_KEY]?.let { name -> AtAGlanceWidgetDismissType.entries.find { it.name == name } } ?: return
        val key = parameters[KEY_KEY]
        val id = parameters[ID_KEY] ?: 0L
        runCatching { dismiss(context, type, key, id) }
        refreshAtAGlanceWidgets(context)
    }

    private fun dismiss(
        context: Context,
        type: AtAGlanceWidgetDismissType,
        key: String?,
        id: Long,
    ) {
        val glancePreferences = GlancePreferences(context)
        when (type) {
            AtAGlanceWidgetDismissType.OTP ->
                GlanceNotificationsStore.otps.value.firstOrNull { it.key == key }?.let(GlanceNotificationsStore::dismissOtp)
            AtAGlanceWidgetDismissType.ALARM -> {
                val repository = UpcomingAlarmRepository(context)
                val now = System.currentTimeMillis()
                repository.nextWithinFortyFiveMinutes(now)?.let(repository::dismiss)
                    ?: repository.tomorrowsFirstAlarm(now)?.let(repository::dismissTomorrow)
            }
            AtAGlanceWidgetDismissType.REMINDER -> ReminderRepository(context).dismissFromHome(id)
            AtAGlanceWidgetDismissType.NOTED_TASK ->
                key?.let { NotedGlanceTasks.find(context, it) }?.let { NotedGlanceTasks.dismiss(context, it) }
            AtAGlanceWidgetDismissType.MISSED_CALLS -> {
                val dismissedAt = glancePreferences.getMissedCallsDismissedAt()
                val calls = GlanceNotificationsStore.missedCalls.value.filter { it.callTime > dismissedAt }
                GlanceNotificationsStore.dismissMissedCalls(calls.map { it.key })
                calls.maxOfOrNull { it.callTime }?.let(glancePreferences::setMissedCallsDismissedAt)
            }
            AtAGlanceWidgetDismissType.APP_NOTIFICATION ->
                GlanceNotificationsStore.appNotificationCandidates.value
                    .firstOrNull { it.key == key }
                    ?.let(GlanceNotificationsStore::dismissAppNotification)
            AtAGlanceWidgetDismissType.WEATHER -> key?.let(GlanceNotificationsStore::dismissWeather)
            AtAGlanceWidgetDismissType.FINISHED_PROGRESS -> key?.let(GlanceNotificationsStore::dismissFinishedProgress)
            AtAGlanceWidgetDismissType.BIRTHDAY -> glancePreferences.dismissBirthday(LocalDate.now().toString(), id)
            AtAGlanceWidgetDismissType.TOMORROW_EVENT ->
                dismissTomorrowGlanceEvent(CalendarRepository(context), CalendarPreferences(context), id)
            AtAGlanceWidgetDismissType.TODAY_EVENT -> dismissTodayGlanceEvent(CalendarRepository(context), CalendarPreferences(context), id)
            AtAGlanceWidgetDismissType.CUSTOM_INFO -> {
                CustomInfoScheduler.cancelNotification(context.applicationContext, id.toInt())
                CustomInfoRepository(context).update(id.toInt()) { it.copy(showOnHome = false) }
            }
            AtAGlanceWidgetDismissType.NOW_PLAYING -> dismissAtAGlanceWidgetNowPlaying(context)
            AtAGlanceWidgetDismissType.WIFI_SIGN_IN -> WifiSignInDismissal.dismissed = true
            AtAGlanceWidgetDismissType.CHARGING ->
                AtAGlanceWidgetRowLoader(context, System.currentTimeMillis())
                    .batteryStatus()
                    ?.let { BatteryPreferences(context).dismissCharging(it.percent) }
            AtAGlanceWidgetDismissType.LOW_STORAGE ->
                readInternalStorage()?.let { (free, total) ->
                    glancePreferences.setLowStorageDismissedFreePercent(freePercent(free, total))
                }
        }
    }

    internal companion object {
        private val TYPE_KEY = ActionParameters.Key<String>("dismiss_type")
        private val KEY_KEY = ActionParameters.Key<String>("dismiss_key")
        private val ID_KEY = ActionParameters.Key<Long>("dismiss_id")

        /** For a close button that is not on an [AtAGlanceWidgetRow], such as Now Playing's. */
        fun parameters(type: AtAGlanceWidgetDismissType): ActionParameters = actionParametersOf(TYPE_KEY to type.name)

        fun parameters(row: AtAGlanceWidgetRow): ActionParameters =
            actionParametersOf(
                TYPE_KEY to (row.dismiss?.name ?: ""),
                KEY_KEY to (row.tap.key ?: ""),
                ID_KEY to row.tap.id,
            )
    }
}
