package com.tk.quicksearch.widgets.atAGlanceWidget

import android.app.AlarmManager
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.tk.quicksearch.R
import com.tk.quicksearch.customInfo.CustomInfoItem
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.calendar.calendarRelativeTimeLabel
import com.tk.quicksearch.search.data.AppNotificationFilters
import com.tk.quicksearch.search.data.AppNotificationsSettings
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactBirthday
import com.tk.quicksearch.search.data.ContactBirthdaysRepository
import com.tk.quicksearch.search.data.GlanceNotificationsStore
import com.tk.quicksearch.search.data.MissedCallNotification
import com.tk.quicksearch.search.data.OtpNotifications
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.UpcomingAlarmRepository
import com.tk.quicksearch.search.data.loadTodayGlanceEvents
import com.tk.quicksearch.search.data.loadTomorrowGlanceEvents
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.data.preferences.GlanceItemSettings
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.englishOrdinal
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.timerActionIcon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Matches the home card: at most this many ongoing and finished progress notifications. */
private const val MAX_PROGRESS_ROWS = 3

/** Matches the home card: at most this many weather notifications. */
private const val MAX_WEATHER_ROWS = 2

/** Tomorrow's events show from this hour of the evening, as on home. */
private const val TOMORROW_EVENTS_START_HOUR = 21

/** The upcoming alarm row shows from this long before the alarm, as on home. */
private const val ALARM_LEAD_MILLIS = 45L * 60L * 1000L

/** A reminder reads "Now" for this long after it is due before turning overdue, as on home. */
private const val REMINDER_NOW_WINDOW_MILLIS = 60L * 1000L

private const val MINUTE_MILLIS = 60L * 1000L
private const val HOUR_MILLIS = 60L * MINUTE_MILLIS
private const val DAY_MILLIS = 24L * HOUR_MILLIS

private fun ceilMinutes(millis: Long): Long = (millis + MINUTE_MILLIS - 1L) / MINUTE_MILLIS

/** A notification reads "Now" for this long after it is posted, as on home. */
private const val JUST_NOW_MILLIS = 60L * 1000L

/** RemoteViews collections are sent in one parcel, so the widget stops well short of its size limit. */
private const val MAX_ROWS = 30

/**
 * Reads the home At a Glance items without Compose, in the home card's order: the time-critical
 * items, then things due soon, what needs a look, today's and tomorrow's context, passive device
 * states, and today's calendar events last. Now Playing is read apart, by
 * [loadAtAGlanceWidgetNowPlaying]. Relative times such as "in 5 min" schedule a refresh for when
 * they next change. [items] decides which items the widget shows. Blocking; call off the main thread.
 */
internal fun loadAtAGlanceWidgetSnapshot(
    context: Context,
    items: GlanceItemSettings,
    appNotificationFilters: AppNotificationFilters? = null,
    nowMillis: Long = System.currentTimeMillis(),
): AtAGlanceWidgetSnapshot {
    val loader = AtAGlanceWidgetRowLoader(context.applicationContext, nowMillis, items, appNotificationFilters)
    val rows =
        with(loader) {
            buildList {
                addAll(notificationRows())
                addAll(alarmRows())
                addAll(reminderRows())
                addAll(missedCallRows())
                addAll(appNotificationRows())
                addAll(lowBatteryRows())
                addAll(wifiSignInRows())
                addAll(progressRows())
                addAll(workoutAndWeatherRows())
                addAll(birthdayRows())
                addAll(tomorrowEventRows())
                addAll(customInfoRows())
                addAll(deviceStateRows())
                addAll(todayEventRows())
            }
        }
    return AtAGlanceWidgetSnapshot(rows = rows.take(MAX_ROWS), nextChangeMillis = loader.nextChangeMillis)
}

internal class AtAGlanceWidgetRowLoader(
    val context: Context,
    val nowMillis: Long,
    val items: GlanceItemSettings = AppGlanceItemSettings(context),
    /** The widget's own App notifications apps and keywords; null to show what Home's match. */
    private val appNotificationFilters: AppNotificationFilters? = null,
) {
    val glancePreferences = GlancePreferences(context)
    private val calendarPreferences = CalendarPreferences(context)
    private val hasNotificationAccess = NotificationDotsPermission.hasNotificationListenerAccess(context)
    private val timeFormat = DateFormat.getTimeFormat(context)
    private val narrowDurationFormat = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.NARROW)
    private val appLabels = mutableMapOf<String, String>()

    /** Earliest time a row appears or disappears without any broadcast saying so. */
    var nextChangeMillis: Long? = null
        private set

    init {
        // Today's and tomorrow's rows change when the evening starts and at midnight.
        val today = LocalDate.now()
        changesAt(today.atTime(LocalTime.of(TOMORROW_EVENTS_START_HOUR, 0)).toEpochMillis())
        changesAt(today.plusDays(1).atStartOfDay().toEpochMillis())
    }

    fun changesAt(millis: Long) {
        if (millis <= nowMillis) return
        nextChangeMillis = nextChangeMillis?.coerceAtMost(millis) ?: millis
    }

    fun string(
        resId: Int,
        vararg args: Any,
    ): String = context.getString(resId, *args)

    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

    /** Home's "in 5 min" or "2 hours ago" label, refreshed when its count of minutes or hours changes. */
    fun relativeTime(targetMillis: Long): String {
        val delta = targetMillis - nowMillis
        val step = if (abs(delta) >= HOUR_MILLIS) HOUR_MILLIS else MINUTE_MILLIS
        changesAt(if (delta >= 0) nowMillis + delta % step + 1 else nowMillis + step - (-delta) % step)
        return calendarRelativeTimeLabel(context.resources, targetMillis, nowMillis)
    }

    /**
     * A call's or stopwatch's running time as "2m", "2h" or "2h 3m", refreshed when its
     * minute count changes. Seconds would need a widget redraw every second, so they're left out.
     */
    fun elapsedDuration(sinceMillis: Long): String {
        val elapsed = (nowMillis - sinceMillis).coerceAtLeast(0L)
        changesAt(nowMillis + MINUTE_MILLIS - elapsed % MINUTE_MILLIS)
        return durationLabel(elapsed / MINUTE_MILLIS)
    }

    /** A countdown's time left as [elapsedDuration] does, rounded up so "1m" stays until it ends. */
    fun remainingDuration(untilMillis: Long): String {
        val remaining = (untilMillis - nowMillis).coerceAtLeast(0L)
        if (remaining > 0L) changesAt(nowMillis + (remaining - 1L) % MINUTE_MILLIS + 1L)
        return durationLabel(ceilMinutes(remaining))
    }

    /** Compact "2h 3m", "2h" or "45m" in the locale's narrow unit style. */
    fun durationLabel(totalMinutes: Long): String {
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        val measures =
            buildList {
                if (hours > 0L) add(Measure(hours, MeasureUnit.HOUR))
                if (minutes > 0L || hours == 0L) add(Measure(minutes, MeasureUnit.MINUTE))
            }
        return narrowDurationFormat.formatMeasures(*measures.toTypedArray())
    }

    fun appLabel(packageName: String): String =
        appLabels.getOrPut(packageName) {
            val packageManager = context.packageManager
            runCatching {
                packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
            }.getOrDefault(packageName)
        }

    /** Home's notification age: "Now" for a minute, then "5 min ago", refreshed when it changes. */
    private fun notificationAge(postTime: Long): String {
        val age = nowMillis - postTime
        if (age < JUST_NOW_MILLIS) {
            changesAt(postTime + JUST_NOW_MILLIS)
            return string(R.string.calendar_relative_now)
        }
        changesAt(nowMillis + MINUTE_MILLIS - age % MINUTE_MILLIS)
        return DateUtils.getRelativeTimeSpanString(
            postTime,
            nowMillis,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString().replace(".", "")
    }

    /** A notification's row, which wraps its title in full as on home. */
    private fun notificationRow(
        prefix: String,
        key: String,
        packageName: String,
        title: String,
        subtitle: String?,
        icon: AtAGlanceWidgetIcon = AtAGlanceWidgetIcon.App(packageName),
        dismiss: AtAGlanceWidgetDismissType? = null,
        titleStyle: AtAGlanceWidgetTitleStyle = AtAGlanceWidgetTitleStyle.BODY,
        titleMaxLines: Int = Int.MAX_VALUE,
        largeSubtitle: Boolean = false,
        pill: String? = null,
        footer: String? = null,
        progress: Float? = null,
        trailing: List<AtAGlanceWidgetButton> = emptyList(),
        dismissAtTop: Boolean = false,
    ) = AtAGlanceWidgetRow(
        key = "$prefix-$key",
        icon = icon,
        title = title,
        subtitle = subtitle,
        tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.NOTIFICATION, key = key, packageName = packageName),
        dismiss = dismiss,
        titleStyle = titleStyle,
        titleMaxLines = titleMaxLines,
        largeSubtitle = largeSubtitle,
        pill = pill?.let { AtAGlanceWidgetPill(it) },
        footer = footer,
        progress = progress,
        trailing = trailing,
        dismissAtTop = dismissAtTop,
    )

    /** Ongoing calls, the newest one-time code and running timers. */
    fun notificationRows(): List<AtAGlanceWidgetRow> {
        if (!hasNotificationAccess) return emptyList()
        val calls =
            if (items.isShown(GlanceItem.ONGOING_CALL)) {
                GlanceNotificationsStore.ongoingCalls.value.map { call ->
                    AtAGlanceWidgetRow(
                        key = "ongoing-call-${call.key}",
                        icon = AtAGlanceWidgetIcon.Vector(MaterialSymbols.Call),
                        title = string(R.string.home_ongoing_call),
                        subtitle = call.caller ?: appLabel(call.packageName),
                        tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.NOTIFICATION, key = call.key, packageName = call.packageName),
                        pill = call.startTime?.let { AtAGlanceWidgetPill(elapsedDuration(it)) },
                        trailing =
                            listOfNotNull(
                                AtAGlanceWidgetButton(
                                    tap =
                                        AtAGlanceWidgetTap(
                                            AtAGlanceWidgetTapType.HANG_UP_CALL,
                                            key = call.key,
                                            packageName = call.packageName,
                                        ),
                                    style = AtAGlanceWidgetButtonStyle.HANG_UP,
                                    label = string(R.string.home_ongoing_call_hang_up),
                                    icon = MaterialSymbols.CallEnd,
                                ).takeIf { call.hangUpIntent != null || call.endsThroughTelecom },
                            ),
                    )
                }
            } else {
                emptyList()
            }
        val otp =
            GlanceNotificationsStore.otps.value
                .firstOrNull()
                ?.takeIf { items.isShown(GlanceItem.OTP_CODES) && it.postTime > nowMillis - OtpNotifications.LIFETIME_MILLIS }
        otp?.let { changesAt(it.postTime + OtpNotifications.LIFETIME_MILLIS) }
        val otpRows =
            listOfNotNull(
                otp?.let {
                    // As on home, the row opens the message and the code below copies.
                    AtAGlanceWidgetRow(
                        key = "otp-${it.key}",
                        icon = AtAGlanceWidgetIcon.App(it.packageName),
                        title = "${string(R.string.home_otp_from, it.sender ?: appLabel(it.packageName))} • ${notificationAge(it.postTime)}",
                        tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.NOTIFICATION, key = it.key, packageName = it.packageName),
                        dismiss = AtAGlanceWidgetDismissType.OTP,
                        titleStyle = AtAGlanceWidgetTitleStyle.SMALL,
                        titleMaxLines = Int.MAX_VALUE,
                        code = it.code,
                    )
                },
            )
        val timers =
            if (items.isShown(GlanceItem.TIMERS)) {
                GlanceNotificationsStore.timers.value.map { timer ->
                    val kind = string(if (timer.isCountDown) R.string.home_timer else R.string.home_stopwatch)
                    val paused = timer.pausedMillis
                    // Home's time without seconds, so the widget only redraws once a minute.
                    val title =
                        when {
                            paused != null ->
                                durationLabel(if (timer.isCountDown) ceilMinutes(paused) else paused / MINUTE_MILLIS)
                            timer.isCountDown -> remainingDuration(timer.chronometerBase)
                            else -> elapsedDuration(timer.chronometerBase)
                        }
                    val detail =
                        when {
                            paused != null -> "$kind • ${string(R.string.home_timer_paused)}"
                            timer.isCountDown && timer.chronometerBase > nowMillis -> "$kind • ${formatTime(timer.chronometerBase)}"
                            else -> "$kind • ${appLabel(timer.packageName)}"
                        }
                    notificationRow(
                        prefix = "timer",
                        key = timer.key,
                        packageName = timer.packageName,
                        title = title,
                        subtitle = detail,
                        icon = AtAGlanceWidgetIcon.Vector(if (timer.isCountDown) MaterialSymbols.Timer else MaterialSymbols.AvTimer),
                        titleStyle = AtAGlanceWidgetTitleStyle.LARGE,
                        titleMaxLines = 1,
                        trailing =
                            timer.actions.mapIndexed { index, action ->
                                val tap =
                                    AtAGlanceWidgetTap(
                                        AtAGlanceWidgetTapType.TIMER_ACTION,
                                        key = timer.key,
                                        packageName = timer.packageName,
                                        id = index.toLong(),
                                    )
                                val fallbackIcon = timerActionIcon(action.title)
                                // As on home, a button with no icon of either kind shows its label.
                                AtAGlanceWidgetButton(
                                    tap = tap,
                                    style =
                                        if (action.icon == null && fallbackIcon == null) {
                                            AtAGlanceWidgetButtonStyle.OUTLINED_CHIP
                                        } else {
                                            AtAGlanceWidgetButtonStyle.TONAL
                                        },
                                    label = action.title,
                                    icon = fallbackIcon,
                                    appIcon = action.icon,
                                )
                            },
                    )
                }
            } else {
                emptyList()
            }
        return calls + otpRows + timers
    }

    /** The next alarm within 45 minutes, or else tomorrow's first alarm in the evening. */
    fun alarmRows(): List<AtAGlanceWidgetRow> {
        val repository = UpcomingAlarmRepository(context, items)
        val soon = repository.nextWithinFortyFiveMinutes(nowMillis)
        val alarm = soon ?: repository.tomorrowsFirstAlarm(nowMillis)
        // The row appears 45 minutes before the next alarm and leaves once it rings.
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.let { next ->
            changesAt(next.triggerTime - ALARM_LEAD_MILLIS)
            changesAt(next.triggerTime + 1_000L)
        }
        alarm ?: return emptyList()
        val time = formatTime(alarm.triggerTime)
        val packageName = alarm.showIntent?.creatorPackage
        return listOf(
            AtAGlanceWidgetRow(
                key = "alarm",
                icon = packageName?.let { AtAGlanceWidgetIcon.App(it) } ?: AtAGlanceWidgetIcon.Vector(MaterialSymbols.Alarm),
                title = string(R.string.home_upcoming_alarm),
                subtitle = "$time • ${if (soon == null) string(R.string.calendar_relative_tomorrow) else relativeTime(alarm.triggerTime)}",
                tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.ALARM, packageName = packageName),
                dismiss = AtAGlanceWidgetDismissType.ALARM,
            ),
        )
    }

    /** Reminders shown on home's At a Glance card; see [ReminderRepository.getHomeCardReminders]. */
    fun reminderRows(): List<AtAGlanceWidgetRow> {
        val repository = ReminderRepository(context)
        val reminders =
            repository.getHomeCardReminders(
                nowMillis,
                items.isShown(GlanceItem.REMINDERS),
                items.isShown(GlanceItem.TOMORROW_REMINDERS),
            )
        // Upcoming reminders join the card ahead of time, and turn overdue at their due time.
        repository.getUpcomingReminders(limit = 5).forEach { reminder ->
            changesAt(ReminderRepository.glanceVisibleFromMillis(reminder))
            changesAt(reminder.dueMillis)
        }
        val reminderRows = reminders.map { reminder ->
            reminder.dueMillis to AtAGlanceWidgetRow(
                key = "reminder-${reminder.reminderId}",
                icon = AtAGlanceWidgetIcon.Drawable(R.drawable.ic_reminder),
                title = reminder.title,
                subtitle = reminderSubtitle(reminder),
                tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.REMINDER, id = reminder.reminderId),
                titleMaxLines = Int.MAX_VALUE,
                dismiss = AtAGlanceWidgetDismissType.REMINDER,
                trailing =
                    listOf(
                        AtAGlanceWidgetButton(
                            tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.REMINDER_DONE, id = reminder.reminderId),
                            style = AtAGlanceWidgetButtonStyle.ICON,
                            label = string(R.string.action_mark_as_done),
                            icon = MaterialSymbols.Check,
                        ),
                    ),
            )
        }
        return (reminderRows + notedTaskRows()).sortedBy { it.first }.map { it.second }
    }

    /**
     * An upcoming reminder's time (home also says how soon it is due), then "Now" for a minute, then
     * "Overdue" with its time, or its date once a day late.
     */
    internal fun reminderSubtitle(reminder: ReminderInfo): String {
        val sinceDue = nowMillis - reminder.dueMillis
        val isNow = reminder.hasTime && sinceDue in 0 until REMINDER_NOW_WINDOW_MILLIS
        val isOverdue = reminder.isOverdue(nowMillis) && !isNow
        if (isNow) changesAt(reminder.dueMillis + REMINDER_NOW_WINDOW_MILLIS)
        if (isOverdue && reminder.hasTime) changesAt(reminder.dueMillis + DAY_MILLIS)
        val time = formatTime(reminder.dueMillis)
        val isTomorrow = reminder.date == LocalDate.now().plusDays(1)
        val tomorrowLabel = string(R.string.calendar_relative_tomorrow)
        return when {
            isTomorrow && !reminder.hasTime -> tomorrowLabel
            isTomorrow -> "$time • $tomorrowLabel"
            !reminder.hasTime -> reminderSchedule(reminder)
            sinceDue < DAY_MILLIS -> time
            else -> reminderSchedule(reminder)
        } + when {
            isNow -> " • ${string(R.string.calendar_relative_now)}"
            isOverdue -> " • ${string(R.string.reminder_status_overdue)}"
            else -> ""
        }
    }

    internal fun reminderSchedule(reminder: ReminderInfo): String {
        val isToday = reminder.date == LocalDate.now()
        val time = if (reminder.hasTime) formatTime(reminder.dueMillis) else null
        return when {
            isToday -> time ?: string(R.string.calendar_relative_today)
            else -> {
                val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(reminder.dayStartMillis))
                listOfNotNull(date, time).joinToString(" • ")
            }
        }
    }

    /** One summary row for the missed calls since they were last dismissed on home. */
    fun missedCallRows(): List<AtAGlanceWidgetRow> {
        if (!hasNotificationAccess || !items.isShown(GlanceItem.MISSED_CALLS)) return emptyList()
        val dismissedAt = glancePreferences.getMissedCallsDismissedAt()
        val calls = GlanceNotificationsStore.missedCalls.value.filter { it.callTime > dismissedAt }
        val latest = calls.firstOrNull() ?: return emptyList()
        val title =
            if (calls.size == 1) {
                string(R.string.home_missed_call)
            } else {
                string(R.string.home_missed_calls_count, NumberFormat.getIntegerInstance().format(calls.size))
            }
        // As on home: with calls from several apps, a chip per app opens its newest call, phone app first.
        val appCalls = calls.distinctBy { it.packageName }.sortedByDescending { it.fromPhoneApp }
        val chips =
            appCalls.takeIf { it.size > 1 }.orEmpty().map { call ->
                AtAGlanceWidgetButton(
                    tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.MISSED_CALLS, key = call.key, packageName = call.packageName),
                    style = AtAGlanceWidgetButtonStyle.OUTLINED_CHIP,
                    label = appLabel(call.packageName),
                    appPackage = call.packageName,
                )
            }
        // A single phone call gets the notification's call back button.
        val callBack =
            calls.singleOrNull()?.takeIf { it.fromPhoneApp && it.callBackIntent != null }?.let { call ->
                AtAGlanceWidgetButton(
                    tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.MISSED_CALL_BACK, key = call.key, packageName = call.packageName),
                    style = AtAGlanceWidgetButtonStyle.CHIP,
                    label = string(R.string.contact_method_call_label),
                    icon = MaterialSymbols.Call,
                )
            }
        return listOf(
            AtAGlanceWidgetRow(
                key = "missed-calls",
                icon = AtAGlanceWidgetIcon.Vector(MaterialSymbols.PhoneMissed, AtAGlanceWidgetTint.ERROR),
                title = title,
                subtitle = calls.mapNotNull(::callerLabel).distinct().joinToString(", ").ifBlank { null },
                tap =
                    AtAGlanceWidgetTap(AtAGlanceWidgetTapType.MISSED_CALLS, key = latest.key, packageName = latest.packageName),
                dismiss = AtAGlanceWidgetDismissType.MISSED_CALLS,
                chips = chips,
                trailing = listOfNotNull(callBack),
                dismissAtTop = chips.isNotEmpty(),
            ),
        )
    }

    private fun callerLabel(call: MissedCallNotification): String? {
        if (call.fromPhoneApp) return call.caller
        val appLabel = appLabel(call.packageName)
        return call.caller?.let { string(R.string.home_missed_call_from_app, it, appLabel) } ?: appLabel
    }

    /**
     * Every notification matching the widget's App notifications apps and keywords, or the app's when it
     * has none of its own; the widget scrolls instead of folding them. The widget's toggle, not Home's, decides.
     */
    fun appNotificationRows(): List<AtAGlanceWidgetRow> {
        if (!hasNotificationAccess || !items.isShown(GlanceItem.APP_NOTIFICATIONS)) return emptyList()
        val filters =
            appNotificationFilters
                ?: (AppNotificationsSettings.config.value ?: AppNotificationsSettings.load(context)).filters
        val otpKey = GlanceNotificationsStore.otps.value.firstOrNull()?.key
        return GlanceNotificationsStore.appNotificationsMatching(filters)
            .filter { it.key != otpKey }
            .sortedByDescending { it.postTime }
            .map { notification ->
                val appLabel = appLabel(notification.packageName)
                notificationRow(
                    prefix = "app-notification",
                    key = notification.key,
                    packageName = notification.packageName,
                    title = notification.title ?: notification.text ?: appLabel,
                    subtitle = notification.text.takeIf { notification.title != null },
                    dismiss = AtAGlanceWidgetDismissType.APP_NOTIFICATION,
                    footer = "$appLabel • ${notificationAge(notification.postTime)}",
                    dismissAtTop = true,
                )
            }
    }

    /** Running progress and Live Updates first, then those that finished. */
    fun progressRows(): List<AtAGlanceWidgetRow> {
        if (!hasNotificationAccess || !items.isShown(GlanceItem.PROGRESS)) return emptyList()
        val progress = GlanceNotificationsStore.progress.value.take(MAX_PROGRESS_ROWS)
        val finished = GlanceNotificationsStore.finishedProgress.value.take(MAX_PROGRESS_ROWS - progress.size)
        val percentFormat = NumberFormat.getPercentInstance()
        return progress.map { notification ->
            val appLabel = appLabel(notification.packageName)
            // A Live Update without a bar, such as a ride's "Driver 3 min away", shows its short text instead.
            val fraction =
                notification.progress.toFloat().div(notification.progressMax).takeIf { notification.progressMax > 0 }
            notificationRow(
                prefix = "progress",
                key = notification.key,
                packageName = notification.packageName,
                title = notification.title ?: appLabel,
                subtitle = notification.text ?: appLabel.takeIf { notification.title != null },
                pill = fraction?.let { percentFormat.format(it.toDouble()) } ?: notification.shortCriticalText,
                progress = fraction,
            )
        } +
            finished.map { notification ->
                val appLabel = appLabel(notification.packageName)
                notificationRow(
                    prefix = "finished-progress",
                    key = notification.key,
                    packageName = notification.packageName,
                    title = notification.title ?: appLabel,
                    subtitle = notification.text ?: appLabel.takeIf { notification.title != null },
                    pill = string(R.string.reminder_status_done),
                    dismiss = AtAGlanceWidgetDismissType.FINISHED_PROGRESS,
                )
            }
    }

    fun workoutAndWeatherRows(): List<AtAGlanceWidgetRow> {
        if (!hasNotificationAccess) return emptyList()
        val workouts =
            if (items.isShown(GlanceItem.WORKOUTS)) {
                GlanceNotificationsStore.workouts.value.map { workout ->
                    val appLabel = appLabel(workout.packageName)
                    notificationRow(
                        prefix = "workout",
                        key = workout.key,
                        packageName = workout.packageName,
                        title = workout.title ?: appLabel,
                        subtitle = workout.stats ?: appLabel.takeIf { workout.title != null },
                        largeSubtitle = true,
                        pill = workout.duration,
                    )
                }
            } else {
                emptyList()
            }
        val weather =
            if (items.isShown(GlanceItem.WEATHER)) {
                GlanceNotificationsStore.weather.value.take(MAX_WEATHER_ROWS).map { weather ->
                    val appLabel = appLabel(weather.packageName)
                    notificationRow(
                        prefix = "weather",
                        key = weather.key,
                        packageName = weather.packageName,
                        title = weather.title ?: appLabel,
                        subtitle = weather.text ?: appLabel.takeIf { weather.title != null },
                        dismiss = AtAGlanceWidgetDismissType.WEATHER,
                        dismissAtTop = true,
                    )
                }
            } else {
                emptyList()
            }
        return workouts + weather
    }

    /** Today's birthdays and anniversaries not dismissed on home. */
    fun birthdayRows(): List<AtAGlanceWidgetRow> {
        val repository = ContactBirthdaysRepository(context)
        if (!items.isShown(GlanceItem.BIRTHDAYS) || !repository.hasPermission()) return emptyList()
        val today = LocalDate.now()
        val dismissed = glancePreferences.getDismissedBirthdays(today.toString())
        return repository.birthdaysOn(today).filterNot { it.contactId in dismissed }.map { birthday ->
            AtAGlanceWidgetRow(
                key = "birthday-${birthday.contactId}-${birthday.isAnniversary}",
                icon = AtAGlanceWidgetIcon.Vector(MaterialSymbols.Cake),
                title = birthdayTitle(birthday),
                subtitle = string(R.string.home_birthday_wish),
                tap =
                    AtAGlanceWidgetTap(
                        AtAGlanceWidgetTapType.BIRTHDAY,
                        id = birthday.contactId,
                        extra = birthday.lookupKey,
                    ),
                dismiss = AtAGlanceWidgetDismissType.BIRTHDAY,
            )
        }
    }

    private fun birthdayTitle(birthday: ContactBirthday): String {
        val years =
            birthday.years
                ?: return string(
                    if (birthday.isAnniversary) R.string.home_anniversary_title else R.string.home_birthday_title,
                    birthday.name,
                )
        return string(
            if (birthday.isAnniversary) R.string.home_anniversary_title_ordinal else R.string.home_birthday_title_ordinal,
            birthday.name,
            englishOrdinal(years),
        )
    }

    /** In the evening, tomorrow's all-day events and its first timed event when that is in the morning. */
    fun tomorrowEventRows(): List<AtAGlanceWidgetRow> {
        val repository = CalendarRepository(context)
        val now = LocalDateTime.now()
        if (
            now.hour < TOMORROW_EVENTS_START_HOUR ||
            !items.isShown(GlanceItem.TODAY_EVENTS) ||
            !items.isShown(GlanceItem.TOMORROW_EVENTS) ||
            !repository.hasPermission()
        ) {
            return emptyList()
        }
        val tomorrowLabel = string(R.string.calendar_relative_tomorrow)
        return loadTomorrowGlanceEvents(repository, calendarPreferences, nowMillis)
            .map { event ->
                calendarEventRow(
                    prefix = "tomorrow-event",
                    event = event,
                    subtitle = if (event.allDay) tomorrowLabel else "${formatTime(event.startMillis)} • $tomorrowLabel",
                    dismiss = AtAGlanceWidgetDismissType.TOMORROW_EVENT,
                )
            }
    }

    /** The latest Custom Info answers and failures still shown on home. */
    fun customInfoRows(): List<AtAGlanceWidgetRow> =
        CustomInfoRepository(context)
            .all()
            .filter { item ->
                items.isCustomInfoShown(item) && item.showOnHome &&
                    (item.status == CustomInfoItem.ERROR ||
                        (item.status == CustomInfoItem.COMPLETE && item.answer.isNotBlank()))
            }.map { item ->
                val isError = item.status == CustomInfoItem.ERROR
                AtAGlanceWidgetRow(
                    key = "custom-info-${item.id}",
                    icon =
                        if (isError) {
                            AtAGlanceWidgetIcon.Vector(MaterialSymbols.ErrorOutline, AtAGlanceWidgetTint.ERROR)
                        } else {
                            AtAGlanceWidgetIcon.Vector(MaterialSymbols.AutoAwesome)
                        },
                    title = item.title,
                    subtitle =
                        if (isError) {
                            string(R.string.direct_search_error_generic)
                        } else {
                            item.answer.trimEnd()
                        },
                    tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.CUSTOM_INFO, id = item.id.toLong()),
                    dismiss = AtAGlanceWidgetDismissType.CUSTOM_INFO,
                    trailing =
                        listOfNotNull(
                            AtAGlanceWidgetButton(
                                tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.CUSTOM_INFO_RETRY, id = item.id.toLong()),
                                style = AtAGlanceWidgetButtonStyle.TONAL,
                                label = string(R.string.action_retry),
                                icon = MaterialSymbols.Refresh,
                                size = 40.dp,
                            ).takeIf { isError },
                        ),
                )
            }

    /** Today's calendar events that haven't ended, soonest first, like the events in the home card. */
    fun todayEventRows(): List<AtAGlanceWidgetRow> {
        val repository = CalendarRepository(context)
        if (!items.isShown(GlanceItem.TODAY_EVENTS) || !repository.hasPermission()) return emptyList()
        val todayEvents = loadTodayGlanceEvents(repository, calendarPreferences, nowMillis)
        todayEvents.nextVisibleAtMillis?.let(::changesAt)
        val events = todayEvents.visible
        events.forEach { if (!it.allDay) changesAt(it.endMillis + 1_000L) }
        val todayLabel = string(R.string.calendar_relative_today)
        return events.map { event ->
            calendarEventRow(
                prefix = "today-event",
                event = event,
                subtitle = if (event.allDay) todayLabel else todayEventSchedule(event),
                dismiss = AtAGlanceWidgetDismissType.TODAY_EVENT,
                titleMaxLines = 2,
                subtitleMaxLines = 1,
            )
        }
    }

    /** The start time, then "Now" while the event is on; unlike home, no "in 5 min". */
    private fun todayEventSchedule(event: CalendarEventInfo): String {
        val start = formatTime(event.startMillis)
        if (nowMillis in event.startMillis..event.endMillis) return "$start • ${string(R.string.calendar_relative_now)}"
        changesAt(event.startMillis)
        return start
    }

    private fun calendarEventRow(
        prefix: String,
        event: CalendarEventInfo,
        subtitle: String,
        dismiss: AtAGlanceWidgetDismissType? = null,
        titleMaxLines: Int = 1,
        subtitleMaxLines: Int = Int.MAX_VALUE,
    ) = AtAGlanceWidgetRow(
        key = "$prefix-${event.eventId}",
        icon = AtAGlanceWidgetIcon.Vector(MaterialSymbols.CalendarMonth),
        title = event.title,
        subtitle = subtitle,
        tap =
            AtAGlanceWidgetTap(
                AtAGlanceWidgetTapType.CALENDAR_EVENT,
                id = event.eventId,
                packageName = calendarPreferences.getDefaultCalendarPackage(),
            ),
        dismiss = dismiss,
        titleMaxLines = titleMaxLines,
        subtitleMaxLines = subtitleMaxLines,
    )
}

internal fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
