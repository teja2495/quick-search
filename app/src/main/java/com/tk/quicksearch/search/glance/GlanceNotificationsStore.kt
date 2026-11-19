package com.tk.quicksearch.search.glance

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import com.tk.quicksearch.appNotifications.AppNotification
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.appNotifications.AppNotifications
import com.tk.quicksearch.appNotifications.AppNotificationsConfig
import com.tk.quicksearch.appNotifications.AppNotificationsSettings
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** A notification's own button, such as a timer's Pause or Stop. */
internal data class GlanceNotificationAction(
    val title: String,
    val intent: PendingIntent,
    /** The button's icon from the posting app, when it has one. */
    val icon: Icon? = null,
)

/** A running timer or stopwatch posted by a clock app, counted from [chronometerBase]. */
internal data class TimerNotification(
    val key: String,
    val packageName: String,
    val isCountDown: Boolean,
    /** Wall-clock time the timer ends, or the stopwatch started. */
    val chronometerBase: Long,
    val contentIntent: PendingIntent?,
    /** The frozen time left (or elapsed) while paused; null while it runs. */
    val pausedMillis: Long? = null,
    /** The notification's buttons, such as Pause, Resume, Stop or +1:00. */
    val actions: List<GlanceNotificationAction> = emptyList(),
)

/** A missed call notification; [caller] is usually the name or number. */
internal data class MissedCallNotification(
    val key: String,
    val packageName: String,
    /** Posted by the default phone app rather than a calling app such as WhatsApp. */
    val fromPhoneApp: Boolean,
    val caller: String?,
    /** When the call came in, or when the notification was posted if the app does not say. */
    val callTime: Long,
    val contentIntent: PendingIntent?,
    /** The notification's call back button, if it has one. */
    val callBackIntent: PendingIntent? = null,
)

/** A call in progress; [caller] is usually the name or number. */
internal data class OngoingCallNotification(
    val key: String,
    val packageName: String,
    val caller: String?,
    /** Wall-clock time the call connected, when the notification shows its duration. */
    val startTime: Long?,
    val contentIntent: PendingIntent?,
    /** The call style notification's own hang up action, if it has one. */
    val hangUpIntent: PendingIntent?,
    /**
     * The notification offers a hang up that only opens the call screen (Samsung's phone app), so
     * the call is ended through [android.telecom.TelecomManager] instead.
     */
    val endsThroughTelecom: Boolean = false,
)

/**
 * The At a Glance view of the posted notifications, fed by
 * [com.tk.quicksearch.search.apps.notificationDots.NotificationDotsListenerService] so it shares
 * the notification access already granted for notification dots and media.
 */
internal object GlanceNotificationsStore {
    private val timersState = MutableStateFlow<List<TimerNotification>>(emptyList())
    private val progressState = MutableStateFlow<List<ProgressNotification>>(emptyList())
    private val finishedProgressState = MutableStateFlow<List<FinishedProgressNotification>>(emptyList())
    private val progressTracker = ProgressNotificationTracker()
    private val missedCallsState = MutableStateFlow<List<MissedCallNotification>>(emptyList())
    private val ongoingCallsState = MutableStateFlow<List<OngoingCallNotification>>(emptyList())
    private val workoutsState = MutableStateFlow<List<WorkoutNotification>>(emptyList())
    private val otpsState = MutableStateFlow<List<OtpNotification>>(emptyList())
    private val weatherState = MutableStateFlow<List<WeatherNotification>>(emptyList())
    private val appNotificationsState = MutableStateFlow<List<AppNotification>>(emptyList())
    private val appNotificationCandidatesState = MutableStateFlow<List<AppNotification>>(emptyList())
    private var clockPackages: Set<String>? = null

    /** The keys other rows show. Written with [appNotificationsState] under [appNotificationsLock]; read alone by [isAppNotificationCandidate]. */
    @Volatile
    private var coveredKeys = emptySet<String>()
    private val appNotificationsLock = Any()

    /** Timers read from custom chronometer views, by notification key, reused until the notification changes. */
    private val remoteTimerCache = mutableMapOf<String, Pair<Long, TimerNotification?>>()

    /** Call start times read from custom chronometer views, by notification key, reused until the notification changes. */
    private val remoteCallStartCache = mutableMapOf<String, Pair<Long, Long?>>()

    val timers: StateFlow<List<TimerNotification>> = timersState.asStateFlow()
    val progress: StateFlow<List<ProgressNotification>> = progressState.asStateFlow()
    val finishedProgress: StateFlow<List<FinishedProgressNotification>> = finishedProgressState.asStateFlow()
    val missedCalls: StateFlow<List<MissedCallNotification>> = missedCallsState.asStateFlow()
    val ongoingCalls: StateFlow<List<OngoingCallNotification>> = ongoingCallsState.asStateFlow()
    val workouts: StateFlow<List<WorkoutNotification>> = workoutsState.asStateFlow()
    val otps: StateFlow<List<OtpNotification>> = otpsState.asStateFlow()
    val weather: StateFlow<List<WeatherNotification>> = weatherState.asStateFlow()

    /** What Home's App notifications shows, newest first; the rows other sources show are left out. */
    val appNotifications: StateFlow<List<AppNotification>> = appNotificationsState.asStateFlow()

    /**
     * Every notification App notifications could show, whatever the apps and keywords, for At a Glance
     * widgets with their own and for Recent matches; the rows other sources show are left out.
     */
    val appNotificationCandidates: StateFlow<List<AppNotification>> = appNotificationCandidatesState.asStateFlow()

    /** The notifications [filters] matches, newest first, for an At a Glance widget with its own apps and keywords. */
    fun appNotificationsMatching(filters: AppNotificationFilters): List<AppNotification> =
        AppNotifications.select(appNotificationCandidatesState.value, emptySet(), AppNotificationsConfig(enabled = true, filters))

    // The listener updates from its rescan thread while dismissals come from the UI, so everything
    // that touches the parsers' and trackers' state holds this object's lock.
    @Synchronized
    fun update(
        context: Context,
        notifications: Array<StatusBarNotification>?,
    ) {
        val clocks = clockPackages ?: resolveClockPackages(context).also { clockPackages = it }
        val posted =
            notifications.orEmpty().filter { sbn ->
                sbn.packageName != context.packageName &&
                    sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
            }
        // Clock apps need not mark a running timer ongoing (Google Clock does not), so any of their
        // notifications with a chronometer counts.
        val postedKeys = posted.map { it.key }.toSet()
        remoteTimerCache.keys.retainAll(postedKeys)
        remoteCallStartCache.keys.retainAll(postedKeys)
        val timerNotifications = posted.mapNotNull { it.toTimer(context, clocks) }
        val timerKeys = timerNotifications.map { it.key }.toSet()
        timersState.value = timerNotifications.sortedBy { it.chronometerBase }
        val dialerPackage = defaultDialerPackage(context)
        val missedCalls = posted.mapNotNull { it.toMissedCall(dialerPackage) }
        missedCallsState.value = missedCalls.sortedByDescending { it.callTime }
        val ongoingCalls = posted.mapNotNull { it.toOngoingCall(context) }
        ongoingCallsState.value = ongoingCalls.sortedByDescending { it.startTime ?: 0L }
        val workouts = WorkoutNotifications.parse(context, posted)
        workoutsState.value = workouts
        // Workouts can be Live Updates too; they keep their own row.
        val workoutKeys = workouts.map { it.key }.toSet()
        progressTracker.update(posted.filter { it.key !in timerKeys && it.key !in workoutKeys })
        progressState.value = progressTracker.progress
        finishedProgressState.value = progressTracker.finished
        otpsState.value = OtpNotifications.parse(posted)
        val weather = WeatherNotifications.parse(context, posted)
        weatherState.value = weather
        val coveredKeys =
            timerKeys + workoutKeys + missedCalls.map { it.key } + ongoingCalls.map { it.key } +
                progressTracker.progress.map { it.key } + progressTracker.finished.map { it.key } + weather.map { it.key }
        val config = AppNotificationsSettings.load(context)
        synchronized(appNotificationsLock) {
            this.coveredKeys = coveredKeys
            val candidates = AppNotifications.candidates(posted, coveredKeys)
            appNotificationCandidatesState.value = candidates
            appNotificationsState.value = AppNotifications.select(candidates, emptySet(), config)
        }
    }

    /** Re-matches the posted notifications after App notifications' settings change, from any thread. */
    fun refreshAppNotifications() {
        val config = AppNotificationsSettings.config.value ?: return
        synchronized(appNotificationsLock) {
            appNotificationsState.value = AppNotifications.select(appNotificationCandidatesState.value, emptySet(), config)
        }
    }

    /**
     * Whether App notifications could ever show [sbn], whatever the apps and keywords, for Recent
     * matches in its settings. Call after [update] so rows that other sources show are known.
     */
    fun isAppNotificationCandidate(
        context: Context,
        sbn: StatusBarNotification,
    ): Boolean =
        sbn.packageName != context.packageName &&
            sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            sbn.key !in coveredKeys &&
            AppNotifications.isCandidate(sbn)

    /** Drops cached state when notification access is lost; clock apps are resolved again on reconnect. */
    @Synchronized
    fun clear() {
        clockPackages = null
        dialerPackageReadAtMillis = 0L
        remoteTimerCache.clear()
        remoteCallStartCache.clear()
        WorkoutNotifications.clear()
        OtpNotifications.clear()
        WeatherNotifications.clear()
        AppNotifications.clear()
        synchronized(appNotificationsLock) {
            coveredKeys = emptySet()
            appNotificationCandidatesState.value = emptyList()
            appNotificationsState.value = emptyList()
        }
        timersState.value = emptyList()
        progressTracker.clear()
        progressState.value = emptyList()
        finishedProgressState.value = emptyList()
        missedCallsState.value = emptyList()
        ongoingCallsState.value = emptyList()
        workoutsState.value = emptyList()
        otpsState.value = emptyList()
        weatherState.value = emptyList()
    }

    /** Hides a one-time code and any older ones from At a Glance, leaving their notifications posted. */
    @Synchronized
    fun dismissOtp(otp: OtpNotification) {
        OtpNotifications.dismiss(otp)
        otpsState.value = otpsState.value.filter { it.postTime > otp.postTime }
    }

    /**
     * Clears one App notifications notification from the shade and hides it from At a Glance; the
     * app's other matches stay. Hiding covers notifications the app won't let us clear.
     */
    @Synchronized
    fun dismissAppNotification(notification: AppNotification) {
        NotificationDotsListenerService.cancelNotification(notification.key)
        synchronized(appNotificationsLock) {
            AppNotifications.dismiss(notification)
            appNotificationCandidatesState.value = appNotificationCandidatesState.value.filter { it.key != notification.key }
            appNotificationsState.value = appNotificationsState.value.filter { it.key != notification.key }
        }
    }

    /** Clears the missed call notifications from the shade when the app allows. */
    fun dismissMissedCalls(keys: List<String>) {
        keys.forEach(NotificationDotsListenerService::cancelNotification)
    }

    /** Hides a weather notification from At a Glance and clears it from the shade when the app allows. */
    @Synchronized
    fun dismissWeather(key: String) {
        WeatherNotifications.dismiss(key)
        weatherState.value = weatherState.value.filter { it.key != key }
        NotificationDotsListenerService.cancelNotification(key)
    }

    /** Hides a finished progress notification from At a Glance, leaving it posted. */
    @Synchronized
    fun dismissFinishedProgress(key: String) {
        progressTracker.dismiss(key)
        finishedProgressState.value = progressTracker.finished
    }

    // Asked on every notification change, so the binder call is made at most once per interval.
    private var dialerPackage: String? = null
    private var dialerPackageReadAtMillis = 0L

    private fun defaultDialerPackage(context: Context): String? {
        val now = SystemClock.elapsedRealtime()
        if (dialerPackageReadAtMillis == 0L || now - dialerPackageReadAtMillis > DIALER_PACKAGE_REFRESH_MILLIS) {
            dialerPackage =
                runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()
            dialerPackageReadAtMillis = now
        }
        return dialerPackage
    }

    /**
     * Phone apps mark missed calls with the missed call category (Android 10+), and older ones at
     * least post them to a channel named for it, so the default dialer's channel id is checked too.
     */
    private fun StatusBarNotification.toMissedCall(dialerPackage: String?): MissedCallNotification? {
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        val isMissedCall =
            notification.category == CATEGORY_MISSED_CALL ||
                (
                    packageName == dialerPackage &&
                        notification.channelIdOrNull()?.contains("missed", ignoreCase = true) == true
                )
        if (!isMissedCall) return null
        return MissedCallNotification(
            key = key,
            packageName = packageName,
            fromPhoneApp = packageName == dialerPackage,
            caller = missedCallCaller(),
            callTime = notification.`when`.takeIf { it > 0L } ?: postTime,
            contentIntent = notification.contentIntent,
            callBackIntent = callBackIntent(),
        )
    }

    /**
     * The caller's name or number. Phone apps put it in the title (Google) or the text (Samsung),
     * with a generic "Missed call" label in the other. The lock screen version leaves the caller
     * out, so a field that also appears there is the label, not the caller.
     */
    private fun StatusBarNotification.missedCallCaller(): String? {
        val extras = notification.extras ?: return null
        val publicExtras = notification.publicVersion?.extras
        val labels =
            listOfNotNull(
                publicExtras?.getCharSequence(Notification.EXTRA_TITLE),
                publicExtras?.getCharSequence(Notification.EXTRA_TEXT),
            ).map { it.toString().trim() }.toSet()
        return listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT)
            .mapNotNull { extras.getCharSequence(it)?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull { it !in labels }
    }

    /**
     * A connected call. Call style notifications (Android 12+) say whether the call is ringing or
     * connected; otherwise a ringing call is told apart by its full-screen intent, which connected
     * calls don't use.
     */
    private fun StatusBarNotification.toOngoingCall(context: Context): OngoingCallNotification? {
        if (notification.flags and Notification.FLAG_ONGOING_EVENT == 0) return null
        val extras = notification.extras ?: return null
        // Google Meet posts a call style notification without the call category.
        val isCallStyle = extras.getString(Notification.EXTRA_TEMPLATE) == CALL_STYLE_TEMPLATE
        if (notification.category != Notification.CATEGORY_CALL && !isCallStyle) return toTelegramCall()
        val callType = extras.getInt(EXTRA_CALL_TYPE, CALL_TYPE_UNKNOWN)
        val isConnected =
            if (callType != CALL_TYPE_UNKNOWN) callType == CALL_TYPE_ONGOING else notification.fullScreenIntent == null
        if (!isConnected) return null
        val offeredHangUp = offeredHangUpIntents(extras)
        val hangUpIntent = offeredHangUp.firstOrNull { !opensCallScreen(it) }
        return OngoingCallNotification(
            key = key,
            packageName = packageName,
            caller =
                (callPersonName(extras) ?: extras.getCharSequence(Notification.EXTRA_TITLE))
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
            startTime = callStartTime(context, extras),
            contentIntent = notification.contentIntent,
            hangUpIntent = hangUpIntent,
            endsThroughTelecom = hangUpIntent == null && offeredHangUp.isNotEmpty(),
        )
    }

    /**
     * Telegram (and its forks) posts its ongoing call as a plain foreground service notification with
     * no call category, always under the same id; the title is a localized "Ongoing Telegram call",
     * the text the caller, `when` the time the call started, and the only action ends the call.
     */
    private fun StatusBarNotification.toTelegramCall(): OngoingCallNotification? {
        if (!packageName.startsWith(TELEGRAM_PACKAGE_PREFIX) || id != TELEGRAM_ONGOING_CALL_ID) return null
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE == 0) return null
        val extras = notification.extras ?: return null
        return OngoingCallNotification(
            key = key,
            packageName = packageName,
            caller = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()?.takeIf(String::isNotEmpty),
            startTime = notification.`when`.takeIf { it > 0L },
            contentIntent = notification.contentIntent,
            hangUpIntent = notification.actions?.singleOrNull()?.actionIntent,
        )
    }

    /**
     * When the call connected: the standard chronometer's `when`, or else the base of a running
     * chronometer in a custom layout, as Samsung's in-call screen uses. Null while it doesn't count,
     * such as while dialing.
     */
    private fun StatusBarNotification.callStartTime(
        context: Context,
        extras: Bundle,
    ): Long? {
        if (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)) return notification.`when`.takeIf { it > 0L }
        val cached = remoteCallStartCache[key]
        if (cached != null && cached.first == postTime) return cached.second
        val chronometer = findRemoteChronometer(context, extras)
        val start =
            chronometer
                ?.takeIf { !it.isCountDown && it.isStarted() != false && it.isShownCounting() }
                ?.let { System.currentTimeMillis() - (SystemClock.elapsedRealtime() - it.base) }
        remoteCallStartCache[key] = postTime to start
        return start
    }

    /** The call style's hang up intent, then the hang up action button, when the notification has them. */
    @Suppress("DEPRECATION")
    private fun StatusBarNotification.offeredHangUpIntents(extras: Bundle): List<PendingIntent> =
        listOfNotNull(
            runCatching { extras.getParcelable<PendingIntent>(EXTRA_HANG_UP_INTENT) }.getOrNull(),
            hangUpActionIntent(),
        )

    /**
     * Whether [intent] is the same intent as the notification's tap target: Samsung's phone app sets
     * its hang up to that too, which only opens its call screen.
     */
    private fun StatusBarNotification.opensCallScreen(intent: PendingIntent): Boolean =
        intent == notification.contentIntent || intent == notification.fullScreenIntent

    /**
     * Dialers without the call style (Samsung's, for one) offer the hang up only as a notification
     * action: the one titled like it, or else the only button.
     */
    private fun StatusBarNotification.hangUpActionIntent(): PendingIntent? {
        val actions = notification.actions.orEmpty().filter { it.remoteInputs.isNullOrEmpty() && it.actionIntent != null }
        val byTitle =
            actions.firstOrNull { action ->
                val title = action.title?.toString()?.lowercase().orEmpty()
                HANG_UP_TITLES.any { it in title }
            }
        return (byTitle ?: actions.singleOrNull())?.actionIntent
    }

    private val HANG_UP_TITLES = listOf("end", "hang", "disconnect", "hangup")

    @Suppress("DEPRECATION")
    private fun callPersonName(extras: Bundle): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { extras.getParcelable<Person>(EXTRA_CALL_PERSON)?.name }.getOrNull()
        } else {
            null
        }

    /**
     * [Notification.EXTRA_CALL_TYPE] with its values, [Notification.EXTRA_CALL_PERSON] and
     * [Notification.EXTRA_HANG_UP_INTENT]; only defined from Android 12.
     */
    private const val DIALER_PACKAGE_REFRESH_MILLIS = 10 * 60 * 1000L
    private const val EXTRA_CALL_TYPE = "android.callType"
    private const val EXTRA_CALL_PERSON = "android.callPerson"
    private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"
    private const val CALL_STYLE_TEMPLATE = "android.app.Notification\$CallStyle"

    /** Telegram's package (and its official builds' prefix) and its VoIPService ongoing call notification id. */
    private const val TELEGRAM_PACKAGE_PREFIX = "org.telegram.messenger"
    private const val TELEGRAM_ONGOING_CALL_ID = 201
    private const val CALL_TYPE_UNKNOWN = 0
    private const val CALL_TYPE_ONGOING = 2

    private fun Notification.channelIdOrNull(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) channelId else null


    /**
     * Up to two of the timer's buttons, such as Pause and Stop, leaving out reply fields and
     * time-adjust buttons like "+1:00" (the only ones with a digit in their label).
     */
    private fun StatusBarNotification.buttonActions(): List<GlanceNotificationAction> =
        notification.actions.orEmpty()
            .filter { it.remoteInputs.isNullOrEmpty() }
            .mapNotNull { action ->
                val title = action.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (title.any { it.isDigit() }) return@mapNotNull null
                val intent = action.actionIntent ?: return@mapNotNull null
                GlanceNotificationAction(title, intent, action.getIcon())
            }.take(MAX_BUTTON_ACTIONS)

    /**
     * The missed call's call back button: the action marked as a call (Android 10+), or else the
     * first plain button that isn't the notification's tap target (Google's Call back precedes Message).
     */
    private fun StatusBarNotification.callBackIntent(): PendingIntent? {
        val actions = notification.actions.orEmpty().filter { it.remoteInputs.isNullOrEmpty() && it.actionIntent != null }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            actions.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_CALL }?.let { return it.actionIntent }
        }
        return actions.firstOrNull { it.actionIntent != notification.contentIntent }?.actionIntent
    }

    private const val MAX_BUTTON_ACTIONS = 2

    /** [Notification.CATEGORY_MISSED_CALL], which is only defined from Android 10. */
    private const val CATEGORY_MISSED_CALL = "missed_call"

    /** Apps that handle the standard timer or alarm intents; only their chronometers count as timers. */
    private fun resolveClockPackages(context: Context): Set<String> {
        val packageManager = context.packageManager
        return listOf(AlarmClock.ACTION_SET_TIMER, AlarmClock.ACTION_SHOW_ALARMS)
            .flatMap { action ->
                runCatching { packageManager.queryIntentActivities(Intent(action), 0) }.getOrNull().orEmpty()
            }.mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private fun StatusBarNotification.toTimer(
        context: Context,
        clockPackages: Set<String>,
    ): TimerNotification? {
        if (packageName !in clockPackages) return null
        val extras = notification.extras ?: return null
        if (!extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)) {
            val cached = remoteTimerCache[key]
            if (cached != null && cached.first == postTime) return cached.second
            return toRemoteViewTimer(context, extras, previous = cached?.second)
                .also { remoteTimerCache[key] = postTime to it }
        }
        if (notification.`when` <= 0L) return null
        return TimerNotification(
            key = key,
            packageName = packageName,
            isCountDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN),
            chronometerBase = notification.`when`,
            contentIntent = notification.contentIntent,
            actions = buttonActions(),
        )
    }

    /**
     * Many clock apps leave the standard chronometer extras unset and show the live count in a
     * chronometer view instead: Samsung Clock in an extra of its own (its `when` is only the post
     * time), Google Clock and other AOSP-based clocks in a custom notification layout. Inflating the
     * first view that has one gives the chronometer's base and direction.
     */
    private fun StatusBarNotification.toRemoteViewTimer(
        context: Context,
        extras: Bundle,
        previous: TimerNotification?,
    ): TimerNotification? {
        val chronometer =
            findRemoteChronometer(context, extras) ?: return toFrozenTextTimer(context, previous)
        val elapsedNow = SystemClock.elapsedRealtime()
        val frozenMillis = chronometer.frozenMillis(elapsedNow)
        val isPaused = frozenMillis != null
        val isCountDown =
            if (isPaused) {
                previous?.isCountDown ?: channelCountDown() ?: chronometer.isCountDown
            } else {
                chronometer.isCountDown
            }
        val base = System.currentTimeMillis() - (elapsedNow - chronometer.base)
        return TimerNotification(
            key = key,
            packageName = packageName,
            isCountDown = isCountDown,
            chronometerBase = base,
            contentIntent = notification.contentIntent,
            pausedMillis = frozenMillis,
            actions = buttonActions(),
        )
    }

    /**
     * A paused timer or stopwatch whose custom layout shows its time as plain text in a view named
     * "chronometer" rather than in a chronometer view, as Google Clock's does.
     */
    // Other apps' custom notification layouts are only exposed through the deprecated fields.
    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toFrozenTextTimer(
        context: Context,
        previous: TimerNotification?,
    ): TimerNotification? {
        val shownSeconds =
            listOfNotNull(notification.contentView, notification.bigContentView)
                .firstNotNullOfOrNull { remoteViews ->
                    runCatching {
                        val view = remoteViews.apply(context, FrameLayout(context))
                        view.findClockText()?.toString()?.parseClockSeconds()
                    }.getOrNull()
                } ?: return null
        return TimerNotification(
            key = key,
            packageName = packageName,
            isCountDown = previous?.isCountDown ?: channelCountDown() ?: false,
            chronometerBase = previous?.chronometerBase ?: System.currentTimeMillis(),
            contentIntent = notification.contentIntent,
            pausedMillis = shownSeconds * 1_000L,
            actions = buttonActions(),
        )
    }

    /** The text of the first text view whose resource name contains "chronometer". */
    private fun View.findClockText(): CharSequence? {
        if (this is TextView && id != View.NO_ID) {
            val name = runCatching { resources.getResourceEntryName(id) }.getOrNull()
            if (name != null && "chronometer" in name.lowercase()) return text
        }
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) {
            getChildAt(index).findClockText()?.let { return it }
        }
        return null
    }

    /**
     * The time a paused chronometer shows, or null while it counts. Where the started flag cannot be
     * read, a paused one is told apart by text that its base would not show: Samsung Clock sets a
     * paused timer's base to now and writes the time left into it as text.
     */
    private fun Chronometer.frozenMillis(elapsedNow: Long): Long? {
        val baseMillis = abs(base - elapsedNow)
        return when (isStarted()) {
            true -> null
            false -> baseMillis
            null -> {
                val shownSeconds = text?.toString()?.parseClockSeconds() ?: return null
                if (abs(shownSeconds - baseMillis / 1_000L) > 1L) shownSeconds * 1_000L else null
            }
        }
    }

    /** Seconds in "M:SS" or "H:MM:SS" text such as a chronometer shows, or null when it is not a time. */
    private fun String.parseClockSeconds(): Long? {
        val parts = CLOCK_TEXT.find(this)?.value?.split(':') ?: return null
        return parts.fold(0L) { total, part -> total * 60L + part.toLong() }
    }

    private val CLOCK_TEXT = Regex("""\d+(?::\d{2}){1,2}""")

    /** Whether the notification's channel names a timer or a stopwatch, or null when it names neither. */
    private fun StatusBarNotification.channelCountDown(): Boolean? {
        val channel = notification.channelId?.lowercase() ?: return null
        return when {
            "stopwatch" in channel -> false
            "timer" in channel -> true
            else -> null
        }
    }

    /**
     * The first chronometer view in the notification's Samsung chronometer extra or its custom
     * layouts. Runs on the listener's main thread, as view inflation needs.
     */
    @Suppress("DEPRECATION")
    private fun StatusBarNotification.findRemoteChronometer(
        context: Context,
        extras: Bundle,
    ): Chronometer? {
        val tag = extras.getString(EXTRA_SAMSUNG_CHRONOMETER_TAG)
        return listOfNotNull(
            extras.getParcelable<RemoteViews>(EXTRA_SAMSUNG_CHRONOMETER_VIEW),
            notification.contentView,
            notification.bigContentView,
            notification.headsUpContentView,
        ).firstNotNullOfOrNull { remoteViews ->
            runCatching {
                val view = remoteViews.apply(context, FrameLayout(context))
                (tag?.let { view.findViewWithTag<View>(it) } as? Chronometer) ?: view.findChronometer()
            }.getOrNull()
        }
    }

    private fun View.findChronometer(): Chronometer? {
        if (this is Chronometer) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) {
            getChildAt(index).findChronometer()?.let { return it }
        }
        return null
    }

    /**
     * Whether the chronometer is actually on screen. In-call layouts keep a chronometer view while
     * dialing, hidden or blank, with its base at the time the notification was posted; only a
     * visible one with text is a connected call's timer.
     */
    private fun Chronometer.isShownCounting(): Boolean {
        var view: View? = this
        while (view != null) {
            if (view.visibility != View.VISIBLE) return false
            view = view.parent as? View
        }
        return !text.isNullOrBlank()
    }

    /** Whether the chronometer is counting, or null when that cannot be read (it is not public API). */
    private fun Chronometer.isStarted(): Boolean? =
        runCatching {
            Chronometer::class.java.getDeclaredField("mStarted").apply { isAccessible = true }.getBoolean(this)
        }.getOrNull()

    private const val EXTRA_SAMSUNG_CHRONOMETER_VIEW = "android.ongoingActivityNoti.chronometerRemoteView"
    private const val EXTRA_SAMSUNG_CHRONOMETER_TAG = "android.ongoingActivityNoti.chronometerRemoteViewTag"
}
