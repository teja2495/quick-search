package com.tk.quicksearch.search.data

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.data.preferences.GlanceItemSettings
import com.tk.quicksearch.search.data.preferences.UpcomingAlarmPreferences
import com.tk.quicksearch.shared.util.sendFromUserTap
import java.time.Instant
import java.time.ZoneId

/** Android exposes the next scheduled alarm clock, rather than a list of upcoming alarms. */
/** [glanceItems] decides whether alarms and tomorrow's alarm show; the home card's by default. */
class UpcomingAlarmRepository(
    private val context: Context,
    private val glanceItems: GlanceItemSettings = AppGlanceItemSettings(context),
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val preferences = UpcomingAlarmPreferences(context)
    private val clockAppCache = HashMap<String, Boolean>()

    fun nextWithinFortyFiveMinutes(nowMillis: Long = System.currentTimeMillis()): AlarmManager.AlarmClockInfo? {
        val alarm = nextClockAlarm() ?: return null
        val timeUntilAlarm = alarm.triggerTime - nowMillis
        return alarm.takeIf {
            timeUntilAlarm in 1..FORTY_FIVE_MINUTES_MILLIS &&
                !preferences.isDismissed(it.triggerTime)
        }
    }

    /**
     * From [TOMORROW_ALARM_START_HOUR] until midnight, the next clock alarm when it rings tomorrow.
     * Android only exposes the next alarm, so an alarm still to ring tonight hides tomorrow's.
     */
    fun tomorrowsFirstAlarm(nowMillis: Long = System.currentTimeMillis()): AlarmManager.AlarmClockInfo? {
        if (!glanceItems.isShown(GlanceItem.TOMORROW_ALARM)) return null
        val zoneId = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        if (now.hour < TOMORROW_ALARM_START_HOUR) return null
        val alarm = nextClockAlarm() ?: return null
        val alarmDay = Instant.ofEpochMilli(alarm.triggerTime).atZone(zoneId).toLocalDate()
        return alarm.takeIf {
            alarmDay == now.toLocalDate().plusDays(1) && !preferences.isTomorrowDismissed(it.triggerTime)
        }
    }

    /** The next alarm when the home card shows alarms and a clock app not hidden by the user set it. */
    private fun nextClockAlarm(): AlarmManager.AlarmClockInfo? {
        if (!glanceItems.isShown(GlanceItem.UPCOMING_ALARM)) return null
        val alarm = alarmManager?.nextAlarmClock ?: return null
        val creatorPackage = alarm.showIntent?.creatorPackage ?: return null
        if (!isClockApp(creatorPackage)) return null
        if (preferences.isHiddenPackage(creatorPackage)) return null
        return alarm
    }

    /**
     * Any app can schedule an alarm clock (e.g. reminder or sleep-tracking apps), so only surface
     * alarms created by apps that handle the standard clock-app intents. Cached per package for
     * this repository's lifetime, since the home card re-reads the next alarm every minute.
     */
    private fun isClockApp(creatorPackage: String): Boolean =
        synchronized(clockAppCache) {
            clockAppCache.getOrPut(creatorPackage) {
                CLOCK_APP_ACTIONS.any { action ->
                    queryActivityPackages(Intent(action).setPackage(creatorPackage)).isNotEmpty()
                }
            }
        }

    private fun queryActivityPackages(intent: Intent): Set<String> =
        runCatching {
            val packageManager = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        }.getOrDefault(emptyList())
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

    fun dismiss(alarm: AlarmManager.AlarmClockInfo) {
        preferences.dismiss(alarm.triggerTime)
    }

    fun dismissTomorrow(alarm: AlarmManager.AlarmClockInfo) {
        preferences.dismissTomorrow(alarm.triggerTime)
    }

    /** Hides this and future alarms scheduled by the app that created [alarm]. */
    fun hideAlarmsFromApp(alarm: AlarmManager.AlarmClockInfo) {
        alarm.showIntent?.creatorPackage?.let(preferences::hidePackage)
    }

    fun appLabel(alarm: AlarmManager.AlarmClockInfo): String? {
        val creatorPackage = alarm.showIntent?.creatorPackage ?: return null
        return runCatching {
            val packageManager = context.packageManager
            packageManager.getApplicationInfo(creatorPackage, 0).loadLabel(packageManager).toString()
        }.getOrNull()
    }

    fun open(alarm: AlarmManager.AlarmClockInfo): Boolean {
        val alarmsPage = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            .setPackage(alarm.showIntent.creatorPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(alarmsPage)
            return true
        } catch (_: Exception) {
            // Some clock apps do not handle the standard alarms-page action.
        }
        val launchIntent = alarm.showIntent.creatorPackage?.let(context.packageManager::getLaunchIntentForPackage)
        if (launchIntent != null) {
            try {
                context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) {
                // Fall back to the alarm-specific show intent.
            }
        }
        return alarm.showIntent.sendFromUserTap()
    }

    companion object {
        private val CLOCK_APP_ACTIONS =
            listOf(AlarmClock.ACTION_SHOW_ALARMS, AlarmClock.ACTION_SET_ALARM)
        private const val FORTY_FIVE_MINUTES_MILLIS = 45 * 60 * 1000L

        /** Matches when tomorrow's calendar events start showing. */
        const val TOMORROW_ALARM_START_HOUR = 21
    }
}
