package com.tk.quicksearch.search.other

import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import com.tk.quicksearch.search.core.ScreenTimeAppUsage
import java.util.Calendar
import java.util.TimeZone

class ScreenTimeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager
    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun getTodayScreenTime(nowMillis: Long = System.currentTimeMillis()): ScreenTimeUsageSummary {
        val startOfToday = startOfCurrentDayMillis(nowMillis)
        val queryStart = startOfPreviousDayMillis(startOfToday)
        val usageEvents = usageStatsManager?.queryEvents(queryStart, nowMillis)
            ?: return ScreenTimeUsageSummary(0L, emptyList())
        val deviceUsageEvents = buildList {
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE ->
                        add(DeviceUsageEvent.ScreenState(event.timeStamp, isInteractive = true))
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                        add(DeviceUsageEvent.ScreenState(event.timeStamp, isInteractive = false))
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        add(
                            DeviceUsageEvent.ActivityState(
                                timestampMillis = event.timeStamp,
                                packageName = event.packageName.orEmpty(),
                                instanceToken = event.instanceToken(),
                                isForeground = true,
                            ),
                        )
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED,
                    ->
                        add(
                            DeviceUsageEvent.ActivityState(
                                timestampMillis = event.timeStamp,
                                packageName = event.packageName.orEmpty(),
                                instanceToken = event.instanceToken(),
                                isForeground = false,
                            ),
                        )
                }
            }
        }
        val usage = calculateForegroundAppUsage(
            startMillis = startOfToday,
            endMillis = nowMillis,
            events = deviceUsageEvents,
            excludedPackages = excludedScreenTimePackages(),
            currentInteractiveFallback = powerManager?.isInteractive == true,
        )
        val topApps =
            usage.durationByPackage.entries
                .asSequence()
                .filter { it.value > 0L }
                .sortedByDescending { it.value }
                .take(3)
                .map { (packageName, durationMillis) ->
                    ScreenTimeAppUsage(
                        packageName = packageName,
                        appName = resolveAppName(packageName),
                        durationMillis = durationMillis,
                    )
                }
                .toList()
        return ScreenTimeUsageSummary(
            durationMillis = usage.totalDurationMillis,
            topApps = topApps,
        )
    }

    private fun resolveAppName(packageName: String): String {
        val packageManager = appContext.packageManager
        return runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0),
            ).toString()
        }.getOrElse { packageName.substringAfterLast('.') }
    }

    private fun UsageEvents.Event.instanceToken(): String =
        "$packageName#${className.orEmpty()}"

    private fun excludedScreenTimePackages(): Set<String> {
        val homeIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
        val homePackages =
            setOfNotNull(
                resolveDefaultHomePackage(homeIntent),
            )
        return (homePackages +
            setOf(
                "android",
                "com.android.systemui",
                "com.samsung.android.app.aodservice",
            )).toSet()
    }

    private fun resolveDefaultHomePackage(homeIntent: Intent): String? {
        val packageManager = appContext.packageManager
        val resolveInfo =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(
                    homeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        return resolveInfo?.activityInfo?.packageName
    }
}

data class ScreenTimeUsageSummary(
    val durationMillis: Long,
    val topApps: List<ScreenTimeAppUsage>,
)

internal sealed interface DeviceUsageEvent {
    val timestampMillis: Long

    data class ScreenState(
        override val timestampMillis: Long,
        val isInteractive: Boolean,
    ) : DeviceUsageEvent

    data class ActivityState(
        override val timestampMillis: Long,
        val packageName: String,
        val instanceToken: String,
        val isForeground: Boolean,
    ) : DeviceUsageEvent
}

internal fun calculateForegroundAppUsageDurationMillis(
    startMillis: Long,
    endMillis: Long,
    events: List<DeviceUsageEvent>,
    excludedPackages: Set<String> = emptySet(),
    currentInteractiveFallback: Boolean = false,
): Long =
    calculateForegroundAppUsage(
        startMillis = startMillis,
        endMillis = endMillis,
        events = events,
        excludedPackages = excludedPackages,
        currentInteractiveFallback = currentInteractiveFallback,
    ).totalDurationMillis

internal data class ForegroundAppUsage(
    val totalDurationMillis: Long,
    val durationByPackage: Map<String, Long>,
)

internal fun calculateForegroundAppUsage(
    startMillis: Long,
    endMillis: Long,
    events: List<DeviceUsageEvent>,
    excludedPackages: Set<String> = emptySet(),
    currentInteractiveFallback: Boolean = false,
): ForegroundAppUsage {
    if (endMillis <= startMillis) return ForegroundAppUsage(0L, emptyMap())

    val orderedEvents = events.sortedBy { it.timestampMillis }
    var isScreenInteractive: Boolean? = null
    val activeActivities = mutableMapOf<String, String>()
    val seenActivityTokens = mutableSetOf<String>()
    var cursorMillis = startMillis
    val durationByPackage = mutableMapOf<String, Long>()

    orderedEvents.forEach { event ->
        if (event.timestampMillis < startMillis) {
            when (event) {
                is DeviceUsageEvent.ScreenState -> isScreenInteractive = event.isInteractive
                is DeviceUsageEvent.ActivityState -> Unit
            }
            return@forEach
        }
        if (event.timestampMillis > endMillis) return@forEach

        if (
            event is DeviceUsageEvent.ScreenState &&
                isScreenInteractive == null
        ) {
            isScreenInteractive = !event.isInteractive
        }
        if (
            event is DeviceUsageEvent.ActivityState &&
                !event.isForeground &&
                event.instanceToken !in seenActivityTokens
        ) {
            // A first pause/stop means this activity was already foreground at midnight.
            activeActivities[event.instanceToken] = event.packageName
        }

        val foregroundPackage = activeActivities.values.lastOrNull { it !in excludedPackages }
        val screenIsInteractive = isScreenInteractive ?: currentInteractiveFallback
        if (screenIsInteractive && foregroundPackage != null) {
            durationByPackage[foregroundPackage] =
                durationByPackage.getOrDefault(foregroundPackage, 0L) +
                    (event.timestampMillis - cursorMillis).coerceAtLeast(0L)
        }
        cursorMillis = event.timestampMillis.coerceIn(startMillis, endMillis)
        when (event) {
            is DeviceUsageEvent.ScreenState -> {
                isScreenInteractive = event.isInteractive
                if (!event.isInteractive) {
                    activeActivities.clear()
                    seenActivityTokens.clear()
                }
            }
            is DeviceUsageEvent.ActivityState -> {
                seenActivityTokens += event.instanceToken
                if (event.isForeground) {
                    // On a phone, the newly resumed activity becomes the foreground surface.
                    // Replacing the previous entry prevents missing pause events from making
                    // foreground time continue indefinitely.
                    activeActivities.clear()
                    activeActivities[event.instanceToken] = event.packageName
                } else {
                    activeActivities.remove(event.instanceToken)
                }
            }
        }
    }

    val foregroundPackage = activeActivities.values.lastOrNull { it !in excludedPackages }
    val screenIsInteractive = isScreenInteractive ?: currentInteractiveFallback
    if (screenIsInteractive && foregroundPackage != null) {
        durationByPackage[foregroundPackage] =
            durationByPackage.getOrDefault(foregroundPackage, 0L) +
                (endMillis - cursorMillis).coerceAtLeast(0L)
    }
    val totalMillis = durationByPackage.values.sum().coerceIn(0L, endMillis - startMillis)
    return ForegroundAppUsage(
        totalDurationMillis = totalMillis,
        durationByPackage = durationByPackage.toMap(),
    )
}

internal fun startOfCurrentDayMillis(
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun startOfPreviousDayMillis(
    startOfTodayMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = startOfTodayMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
