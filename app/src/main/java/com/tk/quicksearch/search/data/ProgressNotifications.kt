package com.tk.quicksearch.search.data

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.service.notification.StatusBarNotification

/**
 * An ongoing notification with a determinate progress bar, such as a delivery or a download, or an
 * Android 16 Live Update (a promoted ongoing notification) such as a ride's "Driver 3 min away",
 * which need not have a bar.
 */
internal data class ProgressNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val progress: Int,
    /** 0 when the notification is a Live Update without a progress bar. */
    val progressMax: Int,
    /** A Live Update's short status for its status bar chip, such as "3 min". */
    val shortCriticalText: String?,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/**
 * The notification an app left once a [ProgressNotification] finished, such as "Download complete".
 * It shows until the app removes it or it is dismissed from At a Glance.
 */
internal data class FinishedProgressNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/**
 * Reads progress notifications and follows each one to the notification its app posts when it
 * finishes. Apps either repost the same notification without the ongoing flag or progress bar, or
 * cancel it and post a separate one; the latter arrives as its own listener update, so a progress
 * notification that disappears is watched for [FINISH_GRACE_MILLIS] for a new notification from the
 * same app. One still posted as ongoing (say an indeterminate "Processing…") keeps being watched.
 */
internal class ProgressNotificationTracker {
    /** The progress notifications from the last update, by key. */
    private var lastProgress = emptyMap<String, ProgressNotification>()

    /** Progress notifications that stopped showing progress, with when they did, awaiting their finished notification. */
    private val pending = mutableMapOf<String, Pair<ProgressNotification, Long>>()

    /** Keys of the finished notifications being shown. */
    private val finishedKeys = linkedSetOf<String>()

    var progress: List<ProgressNotification> = emptyList()
        private set
    var finished: List<FinishedProgressNotification> = emptyList()
        private set

    /** [posted] excludes timers, which never count as progress. */
    fun update(posted: List<StatusBarNotification>) {
        val now = System.currentTimeMillis()
        val current =
            posted
                .filter { it.isOngoing() }
                .mapNotNull { it.toProgress() }
                .sortedByDescending { it.postTime }
        val currentKeys = current.map { it.key }.toSet()
        val postedByKey = posted.associateBy { it.key }

        lastProgress.values.filter { it.key !in currentKeys }.forEach { pending[it.key] = it to now }
        pending.keys.removeAll(currentKeys)
        finishedKeys.removeAll(currentKeys)

        val iterator = pending.values.iterator()
        while (iterator.hasNext()) {
            val (last, vanishedAt) = iterator.next()
            val samePosted = postedByKey[last.key]
            val done =
                if (samePosted != null) {
                    samePosted.takeUnless { it.isOngoing() }
                } else {
                    posted
                        .filter { sbn ->
                            sbn.packageName == last.packageName &&
                                sbn.key !in currentKeys &&
                                sbn.key !in finishedKeys &&
                                sbn.postTime >= last.postTime &&
                                sbn.postTime <= vanishedAt + FINISH_GRACE_MILLIS &&
                                !sbn.isOngoing()
                        }.maxByOrNull { it.postTime }
                }
            when {
                done != null && done.hasContent() -> {
                    finishedKeys += done.key
                    iterator.remove()
                }
                samePosted == null && now - vanishedAt > FINISH_GRACE_MILLIS -> iterator.remove()
                done != null -> iterator.remove()
            }
        }

        // A finished notification stays only while the app leaves it posted and not ongoing again.
        finishedKeys.retainAll { key -> postedByKey[key]?.isOngoing() == false }
        lastProgress = current.associateBy { it.key }
        progress = current
        finished =
            finishedKeys
                .mapNotNull { key -> postedByKey[key]?.toFinished() }
                .sortedByDescending { it.postTime }
    }

    /** Hides a finished notification; it comes back only if the app starts and finishes another. */
    fun dismiss(key: String) {
        if (!finishedKeys.remove(key)) return
        finished = finished.filterNot { it.key == key }
    }

    fun clear() {
        lastProgress = emptyMap()
        pending.clear()
        finishedKeys.clear()
        progress = emptyList()
        finished = emptyList()
    }

    private fun StatusBarNotification.text(name: String): String? =
        notification.extras?.getCharSequence(name)?.toString()?.trim()?.takeIf(String::isNotEmpty)

    private fun StatusBarNotification.hasContent(): Boolean =
        text(Notification.EXTRA_TITLE) != null || text(Notification.EXTRA_TEXT) != null

    private fun StatusBarNotification.toFinished(): FinishedProgressNotification? {
        if (!hasContent()) return null
        return FinishedProgressNotification(
            key = key,
            packageName = packageName,
            title = text(Notification.EXTRA_TITLE),
            text = text(Notification.EXTRA_TEXT),
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    private fun StatusBarNotification.toProgress(): ProgressNotification? {
        val extras = notification.extras ?: return null
        if (notification.category == Notification.CATEGORY_TRANSPORT) return null
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return null
        if (extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)) return null
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0).coerceAtLeast(0)
        if (max == 0 && !isBarlessLiveUpdate()) return null
        return ProgressNotification(
            key = key,
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() },
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() },
            progress = extras.getInt(Notification.EXTRA_PROGRESS, 0).coerceIn(0, max),
            progressMax = max,
            shortCriticalText =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    notification.shortCriticalText?.trim()?.takeIf(String::isNotEmpty)
                } else {
                    null
                },
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    /** A Live Update without a progress bar; ongoing calls have their own At a Glance row. */
    private fun StatusBarNotification.isBarlessLiveUpdate(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        if (notification.flags and Notification.FLAG_PROMOTED_ONGOING == 0) return false
        if (notification.category == Notification.CATEGORY_CALL) return false
        if (notification.extras?.getString(Notification.EXTRA_TEMPLATE) == CALL_STYLE_TEMPLATE) return false
        return hasContent()
    }

    private companion object {
        const val CALL_STYLE_TEMPLATE = "android.app.Notification\$CallStyle"

        /** How long after a progress notification disappears a new one from its app counts as its finish. */
        const val FINISH_GRACE_MILLIS = 5_000L
    }
}
