package com.tk.quicksearch.search.glance

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification

/** Current conditions or an alert posted by a weather app. */
internal data class WeatherNotification(
    val key: String,
    val packageName: String,
    /** Such as "72° Partly cloudy" or "Rain starting in 15 min". */
    val title: String?,
    val text: String?,
    /** A persistent current conditions notification rather than a one-off alert. */
    val isOngoing: Boolean,
    val postTime: Long,
    val contentIntent: PendingIntent?,
)

/**
 * Reads weather from the notifications weather apps already post (no weather service is queried):
 * persistent current conditions notifications and alerts such as rain or severe weather. A
 * notification counts when it comes from a known weather app, an app whose package names weather,
 * or a channel named for weather. Apps that draw their notification in a custom layout, such as
 * Samsung Weather or Breezy Weather, are read from its text views.
 */
internal object WeatherNotifications {
    /** Parsed notifications by key, reused until the notification is posted again. */
    private val cache = mutableMapOf<String, Pair<Long, WeatherNotification?>>()

    /** Keys hidden from At a Glance, kept while the notification stays posted (ongoing ones can't be cleared). */
    private val dismissedKeys = mutableSetOf<String>()

    /** Current conditions first, then alerts, newest first within each. */
    fun parse(
        context: Context,
        posted: List<StatusBarNotification>,
    ): List<WeatherNotification> {
        val postedKeys = posted.map { it.key }.toSet()
        cache.keys.retainAll(postedKeys)
        dismissedKeys.retainAll(postedKeys)
        return posted
            .filter { it.key !in dismissedKeys && it.isWeather() }
            .mapNotNull { sbn ->
                val cached = cache[sbn.key]
                if (cached != null && cached.first == sbn.postTime) {
                    cached.second
                } else {
                    sbn.toWeather(context).also { cache[sbn.key] = sbn.postTime to it }
                }
            }.sortedWith(compareByDescending<WeatherNotification> { it.isOngoing }.thenByDescending { it.postTime })
    }

    fun dismiss(key: String) {
        dismissedKeys += key
    }

    fun clear() {
        cache.clear()
        dismissedKeys.clear()
    }

    /** The first temperature in [text], such as "72°", "-3 °C" or "18.5°F", with its spacing removed. */
    fun temperatureIn(text: String): String? =
        TEMPERATURE_PATTERN.find(text)?.value?.replace(" ", "")?.replace('−', '-')

    private fun StatusBarNotification.isWeather(): Boolean {
        val extras = notification.extras
        // Skips "Updating weather…" style notifications shown while an app refreshes.
        if (extras != null &&
            (extras.getInt(Notification.EXTRA_PROGRESS_MAX) > 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        ) {
            return false
        }
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId else null
        return packageName in WEATHER_PACKAGES ||
            packageName.contains(WEATHER, ignoreCase = true) ||
            channelId?.contains(WEATHER, ignoreCase = true) == true
    }

    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toWeather(context: Context): WeatherNotification? {
        val extras = notification.extras
        fun text(name: String) = extras?.getCharSequence(name)?.toString()?.trim()?.takeIf(String::isNotEmpty)
        var texts =
            listOfNotNull(
                text(Notification.EXTRA_TITLE),
                text(Notification.EXTRA_BIG_TEXT) ?: text(Notification.EXTRA_TEXT),
            )
        if (texts.isEmpty()) {
            texts =
                listOfNotNull(notification.contentView, notification.bigContentView)
                    .firstNotNullOfOrNull { remoteViews ->
                        remoteViews.readLayoutTexts(context)?.map { it.text }?.takeIf { it.isNotEmpty() }
                    }.orEmpty()
        }
        if (texts.isEmpty()) return null
        val hasTemperature = texts.any { temperatureIn(it) != null }
        val isOngoing = notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR) != 0
        // An ongoing notification without a temperature is a service notice, such as location updates.
        if (isOngoing && !hasTemperature) return null
        val lines = texts.distinct()
        return WeatherNotification(
            key = key,
            packageName = packageName,
            title = lines.getOrNull(0),
            text = lines.getOrNull(1),
            isOngoing = isOngoing,
            postTime = postTime,
            contentIntent = notification.contentIntent,
        )
    }

    private const val WEATHER = "weather"

    /** Weather apps whose package doesn't say weather: Samsung Weather. */
    private val WEATHER_PACKAGES = setOf("com.sec.android.daemonapp")

    private val TEMPERATURE_PATTERN = Regex("""(?<![\d.,])[-−]?\d{1,3}(?:[.,]\d)?\s?°\s?[CFcf]?(?![A-Za-z])""")
}
