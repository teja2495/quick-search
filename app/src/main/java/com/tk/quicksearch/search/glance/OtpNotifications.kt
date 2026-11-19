package com.tk.quicksearch.search.glance

import android.app.Notification
import android.app.PendingIntent
import android.content.res.Resources
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/** A one-time code read from a message or email notification. */
internal data class OtpNotification(
    val key: String,
    val packageName: String,
    /** Upper case, without spaces or hyphens. */
    val code: String,
    /** The message's sender or the notification's title, when it has one. */
    val sender: String?,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/**
 * Reads one-time codes from posted notifications, following OTP Helper's approach: the message
 * text (not the title, which is the sender and often holds unrelated numbers) goes through
 * [OtpCodeExtractor]. Ongoing notifications and ones Android redacted as sensitive are skipped.
 */
internal object OtpNotifications {
    /** A code stops showing this long after its notification was posted. */
    const val LIFETIME_MILLIS = 5 * 60 * 1000L

    /** Parsed codes by notification key, reused until the notification is posted again. */
    private val cache = mutableMapOf<String, Pair<Long, OtpNotification?>>()

    /** Post time of the last dismissed code; it and every older code stay hidden. */
    private var dismissedThrough = 0L

    /** The newest code seen, kept after its notification is removed until it is dismissed or [LIFETIME_MILLIS] old. */
    private var latest: OtpNotification? = null

    /** At most one code: the newest, replacing an older one when a newer code is posted. */
    fun parse(posted: List<StatusBarNotification>): List<OtpNotification> {
        cache.keys.retainAll(posted.map { it.key }.toSet())
        val now = System.currentTimeMillis()
        val since = maxOf(now - LIFETIME_MILLIS, dismissedThrough)
        val newest =
            posted
                .filter { it.postTime > since && it.canHoldOtp() }
                .mapNotNull { sbn ->
                    val cached = cache[sbn.key]
                    if (cached != null && cached.first == sbn.postTime) {
                        cached.second
                    } else {
                        sbn.toOtp().also { cache[sbn.key] = sbn.postTime to it }
                    }
                }.maxByOrNull { it.postTime }
        val kept = latest?.takeIf { it.postTime > since }
        latest = if (newest != null && (kept == null || newest.postTime > kept.postTime)) newest else kept
        return listOfNotNull(latest)
    }

    fun dismiss(otp: OtpNotification) {
        dismissedThrough = maxOf(dismissedThrough, otp.postTime)
        latest = null
    }

    fun clear() {
        cache.clear()
        latest = null
        dismissedThrough = 0L
    }

    private fun StatusBarNotification.canHoldOtp(): Boolean {
        val flags = notification.flags
        if (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0) return false
        return notification.category !in SKIPPED_CATEGORIES
    }

    private fun StatusBarNotification.toOtp(): OtpNotification? {
        val extras = notification.extras ?: return null
        fun CharSequence?.clean() = this?.toString()?.trim()?.takeIf(String::isNotEmpty)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT).clean()
        if (text != null && text == redactedMessage()) return null
        val lastMessage =
            runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) }
                .getOrNull()
                ?.messages
                ?.lastOrNull()
        val body =
            lastMessage?.text.clean()
                ?: listOfNotNull(
                    extras.getCharSequence(Notification.EXTRA_BIG_TEXT).clean() ?: text,
                    extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n"),
                ).joinToString("\n")
        val code = OtpCodeExtractor.extract(body) ?: return null
        return OtpNotification(
            key = key,
            packageName = packageName,
            code = code,
            sender =
                lastMessage?.person?.name.clean()
                    ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE).clean()
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE).clean(),
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    /**
     * The text Android 15+ puts in place of a notification's content when it hides a one-time code
     * from notification listeners.
     */
    private fun redactedMessage(): String? =
        runCatching {
            val system = Resources.getSystem()
            @Suppress("DiscouragedApi")
            val id = system.getIdentifier("redacted_notification_message", "string", "android")
            if (id != 0) system.getString(id) else null
        }.getOrNull()

    private val SKIPPED_CATEGORIES =
        setOf(
            Notification.CATEGORY_CALL,
            "missed_call",
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SERVICE,
            "navigation",
            Notification.CATEGORY_PROMO,
            Notification.CATEGORY_RECOMMENDATION,
        )
}
