package com.tk.quicksearch.search.glance

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification

/** A workout being tracked by a fitness app, with the live stats its notification shows. */
internal data class WorkoutNotification(
    val key: String,
    val packageName: String,
    /** Such as "Walk in progress". */
    val title: String?,
    /** The stats before the duration, such as "0.4 mi • 79 bpm". */
    val stats: String?,
    /** The elapsed time as the app formats it, such as "01:13". */
    val duration: String?,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/**
 * Reads workout notifications. Google Health (still the Fitbit package) posts its exercise tracking
 * as a foreground service notification with a custom layout and no title or text extras, so the
 * layout is inflated and its title and content text views are read. The content is the stats joined
 * by " • ", ending with the duration. Samsung Health posts a standard ongoing notification with the
 * exercise as the title, the duration as the text and the stats (joined by " | ", or a status such
 * as "Auto paused") in Samsung's ongoing activity extras.
 */
internal object WorkoutNotifications {
    /** Parsed workouts by notification key, reused until the notification is posted again. */
    private val cache = mutableMapOf<String, Pair<Long, WorkoutNotification?>>()

    fun parse(
        context: Context,
        posted: List<StatusBarNotification>,
    ): List<WorkoutNotification> {
        cache.keys.retainAll(posted.map { it.key }.toSet())
        return posted
            .filter { it.isWorkout() }
            .mapNotNull { sbn ->
                val cached = cache[sbn.key]
                if (cached != null && cached.first == sbn.postTime) {
                    cached.second
                } else {
                    sbn.toWorkout(context).also { cache[sbn.key] = sbn.postTime to it }
                }
            }.sortedByDescending { it.postTime }
    }

    fun clear() {
        cache.clear()
    }

    private fun StatusBarNotification.isWorkout(): Boolean =
        when (packageName) {
            GOOGLE_HEALTH_PACKAGE -> isGoogleHealthWorkout()
            SAMSUNG_HEALTH_PACKAGE ->
                notification.flags and Notification.FLAG_ONGOING_EVENT != 0 &&
                    notification.extras?.containsKey(EXTRA_SAMSUNG_SECONDARY_INFO) == true
            else -> false
        }

    private fun StatusBarNotification.isGoogleHealthWorkout(): Boolean {
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE == 0) return false
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId else null
        return channelId?.startsWith(GOOGLE_HEALTH_WORKOUT_CHANNEL_PREFIX) == true
    }

    private fun StatusBarNotification.toWorkout(context: Context): WorkoutNotification? =
        if (packageName == SAMSUNG_HEALTH_PACKAGE) toSamsungHealthWorkout() else toGoogleHealthWorkout(context)

    private fun StatusBarNotification.toSamsungHealthWorkout(): WorkoutNotification? {
        val extras = notification.extras ?: return null
        fun text(name: String) = extras.getCharSequence(name)?.toString()?.trim()?.takeIf(String::isNotEmpty)
        val title = text(Notification.EXTRA_TITLE)
        val text = text(Notification.EXTRA_TEXT)
        val duration = text?.takeIf { DURATION_PATTERN.matches(it) }
        val stats =
            text(EXTRA_SAMSUNG_SECONDARY_INFO)
                ?.split(SAMSUNG_STAT_SEPARATOR)
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.joinToString(" • ")
                ?.takeIf(String::isNotEmpty)
                ?: text.takeIf { duration == null }
        if (title == null && stats == null && duration == null) return null
        return WorkoutNotification(
            key = key,
            packageName = packageName,
            title = title,
            stats = stats,
            duration = duration,
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    /** Runs on the listener's main thread, as view inflation needs. */
    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toGoogleHealthWorkout(context: Context): WorkoutNotification? {
        val extras = notification.extras
        val texts =
            listOfNotNull(notification.contentView, notification.bigContentView).firstNotNullOfOrNull { remoteViews ->
                remoteViews.readLayoutTexts(context)?.toWorkoutTexts()?.takeIf { it.title != null || it.content != null }
            }
        val title =
            texts?.title ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()?.takeIf(String::isNotEmpty)
        val content =
            texts?.content ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()?.takeIf(String::isNotEmpty)
        if (title == null && content == null) return null
        val parts = content?.split(STAT_SEPARATOR)?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
        val duration = parts.lastOrNull()?.takeIf { DURATION_PATTERN.matches(it) }
        val stats = (if (duration != null) parts.dropLast(1) else parts).takeIf { it.isNotEmpty() }?.joinToString(" • ")
        return WorkoutNotification(
            key = key,
            packageName = packageName,
            title = title,
            stats = stats,
            duration = duration,
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    private class LayoutTexts(
        val title: String?,
        val content: String?,
    )

    /** The views named title and content, or else the first two text views in order. */
    private fun List<LayoutText>.toWorkoutTexts(): LayoutTexts {
        fun named(name: String) = firstOrNull { it.idName == name }?.text
        val named = LayoutTexts(named("title"), named("content"))
        if (named.title != null || named.content != null) return named
        return LayoutTexts(getOrNull(0)?.text, getOrNull(1)?.text)
    }

    private const val GOOGLE_HEALTH_PACKAGE = "com.fitbit.FitbitMobile"

    /** Google Health's exercise tracking channel, "MobileRunNotificationChannel2" as of writing. */
    private const val GOOGLE_HEALTH_WORKOUT_CHANNEL_PREFIX = "MobileRun"
    private const val STAT_SEPARATOR = "•"

    private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

    /** The stats line of Samsung's ongoing activity (Now bar) notifications. */
    private const val EXTRA_SAMSUNG_SECONDARY_INFO = "android.ongoingActivityNoti.secondaryInfo"
    private const val SAMSUNG_STAT_SEPARATOR = "|"
    private val DURATION_PATTERN = Regex("""\d{1,2}(:\d{2}){1,2}""")
}
