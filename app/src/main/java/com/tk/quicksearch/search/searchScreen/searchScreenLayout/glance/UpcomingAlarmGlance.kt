package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.calendarRelativeTimeLabel
import com.tk.quicksearch.search.data.UpcomingAlarmRepository
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The next clock-app alarm shown in the home At a Glance card, with its row actions. [isTomorrow]
 * marks tomorrow's first alarm, shown the evening before.
 */
internal class UpcomingAlarmGlance(
    val alarm: AlarmManager.AlarmClockInfo,
    /** Read inside the row, so the minute tick recomposes only the alarm row. */
    val nowMillis: () -> Long,
    val isTomorrow: Boolean,
    /** The app that scheduled the alarm, read once rather than on every recomposition. */
    val creatorPackage: String?,
    val open: () -> Boolean,
    val dismiss: () -> Unit,
    /** Label of the app that scheduled the alarm, or null when it cannot be identified. */
    val appLabel: String?,
    val hideAlarmsFromApp: () -> Unit,
)

/** One read of the next alarm; kept across reads while its time and app are unchanged. */
private class UpcomingAlarmRead(
    val alarm: AlarmManager.AlarmClockInfo,
    val isTomorrow: Boolean,
    val creatorPackage: String?,
    val appLabel: String?,
) {
    fun sameAs(other: UpcomingAlarmRead?): Boolean =
        other != null &&
            other.alarm.triggerTime == alarm.triggerTime &&
            other.isTomorrow == isTomorrow &&
            other.creatorPackage == creatorPackage
}

/**
 * Reads the next alarm within 45 minutes, or else tomorrow's first alarm in the evening, off the
 * main thread: on resume, at each minute boundary while visible, and when the system's next alarm
 * changes. Returns null while not [enabled].
 */
@Composable
internal fun rememberUpcomingAlarmGlance(enabled: Boolean): UpcomingAlarmGlance? {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(appContext) { UpcomingAlarmRepository(appContext) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var read by remember { mutableStateOf<UpcomingAlarmRead?>(null) }

    DisposableEffect(lifecycleOwner, appContext) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    refreshKey++
                }
            }
        val registered =
            runCatching {
                ContextCompat.registerReceiver(
                    appContext,
                    receiver,
                    IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            }.isSuccess
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (registered) runCatching { appContext.unregisterReceiver(receiver) }
        }
    }
    LaunchedEffect(repository, refreshKey) {
        // Home stays composed while another app is in front; only read while it can be seen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                val next =
                    withContext(Dispatchers.IO) {
                        val soon = repository.nextWithinFortyFiveMinutes(now)
                        val alarm = soon ?: repository.tomorrowsFirstAlarm(now)
                        alarm?.let {
                            val creatorPackage = it.showIntent?.creatorPackage
                            val candidate = UpcomingAlarmRead(it, soon == null, creatorPackage, null)
                            val current = read
                            if (candidate.sameAs(current)) {
                                current
                            } else {
                                UpcomingAlarmRead(it, soon == null, creatorPackage, repository.appLabel(it))
                            }
                        }
                    }
                nowMillis = now
                if (next !== read) read = next
                // Labels and the 45-minute window move by the minute, as do alarm trigger times.
                delay(MINUTE_MILLIS - now % MINUTE_MILLIS + 50L)
            }
        }
    }

    if (!enabled) return null
    val current = read ?: return null
    val nextAlarm = current.alarm
    return UpcomingAlarmGlance(
        alarm = nextAlarm,
        nowMillis = { nowMillis },
        isTomorrow = current.isTomorrow,
        creatorPackage = current.creatorPackage,
        open = {
            val clockPackage = current.creatorPackage
            if (clockPackage != null && AppLockGate.isProtected(context, clockPackage)) {
                AppLockGate.runAfterUnlock(context, clockPackage) { repository.open(nextAlarm) }
                true
            } else {
                repository.open(nextAlarm)
            }
        },
        dismiss = {
            if (current.isTomorrow) repository.dismissTomorrow(nextAlarm) else repository.dismiss(nextAlarm)
            read = null
        },
        appLabel = current.appLabel,
        hideAlarmsFromApp = {
            repository.hideAlarmsFromApp(nextAlarm)
            read = null
        },
    )
}

private const val MINUTE_MILLIS = 60_000L

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UpcomingAlarmRow(glance: UpcomingAlarmGlance) {
    val context = LocalContext.current
    val alarmTime = remember(glance.alarm.triggerTime, context) {
        DateFormat.getTimeFormat(context).format(Date(glance.alarm.triggerTime))
    }
    val relativeLabel =
        if (glance.isTomorrow) {
            stringResource(R.string.calendar_relative_tomorrow)
        } else {
            calendarRelativeTimeLabel(glance.alarm.triggerTime, glance.nowMillis())
        }
    val scheduleLabel = "$alarmTime • $relativeLabel"
    val title = stringResource(R.string.home_upcoming_alarm)
    val failureMessage = stringResource(R.string.common_error_unable_to_open, title)
    val showFailure = { AppMessages.show(context, failureMessage) }
    var showMenu by remember { mutableStateOf(false) }

    GlanceStatusRow(
        icon = { UpcomingAlarmAppIcon(glance.creatorPackage) },
        title = title,
        onClick = { if (!glance.open()) showFailure() },
        onLongClick = { if (glance.appLabel != null) showMenu = true },
        subtitle = scheduleLabel,
        onDismiss = glance.dismiss,
        belowText = {
            AppDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                AppDropdownMenuItem(
                    text = stringResource(R.string.home_upcoming_alarm_hide_app, glance.appLabel.orEmpty()),
                    icon = MaterialSymbols.VisibilityOff,
                    onClick = {
                        showMenu = false
                        glance.hideAlarmsFromApp()
                    },
                )
            }
        },
    )
}

/** The clock app's icon, or the generic alarm icon when its package is unknown or still loading. */
@Composable
private fun UpcomingAlarmAppIcon(packageName: String?) {
    val iconPackPackage = LocalGlanceIconPackPackage.current
    val appIcon = packageName?.let { rememberAppIcon(packageName = it, iconPackPackage = iconPackPackage).bitmap }
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}
