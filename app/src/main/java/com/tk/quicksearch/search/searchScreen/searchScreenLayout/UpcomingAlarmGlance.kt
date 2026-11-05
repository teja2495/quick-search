package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import android.app.AlarmManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.calendarRelativeTimeLabel
import com.tk.quicksearch.search.data.UpcomingAlarmRepository
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Date
import kotlinx.coroutines.delay

private val UpcomingAlarmDismissSize = 28.dp

/**
 * The next clock-app alarm shown in the home At a Glance card, with its row actions. [isTomorrow]
 * marks tomorrow's first alarm, shown the evening before.
 */
internal class UpcomingAlarmGlance(
    val alarm: AlarmManager.AlarmClockInfo,
    val nowMillis: Long,
    val isTomorrow: Boolean,
    val open: () -> Boolean,
    val dismiss: () -> Unit,
    /** Label of the app that scheduled the alarm, or null when it cannot be identified. */
    val appLabel: String?,
    val hideAlarmsFromApp: () -> Unit,
)

/**
 * Polls the next alarm within 45 minutes, or else tomorrow's first alarm in the evening, while
 * [enabled]; refreshes on resume and every 10 seconds while visible.
 */
@Composable
internal fun rememberUpcomingAlarmGlance(enabled: Boolean): UpcomingAlarmGlance? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember(context) { UpcomingAlarmRepository(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var alarm by remember { mutableStateOf<AlarmManager.AlarmClockInfo?>(null) }
    var isTomorrow by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(repository, refreshKey, enabled) {
        if (!enabled) {
            alarm = null
            return@LaunchedEffect
        }
        // Home stays composed while another app is in front; only poll while it can be seen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                val soon = repository.nextWithinFortyFiveMinutes(nowMillis)
                alarm = soon ?: repository.tomorrowsFirstAlarm(nowMillis)
                isTomorrow = soon == null && alarm != null
                delay(10_000)
            }
        }
    }

    val nextAlarm = alarm ?: return null
    return UpcomingAlarmGlance(
        alarm = nextAlarm,
        nowMillis = nowMillis,
        isTomorrow = isTomorrow,
        open = {
            val clockPackage = nextAlarm.showIntent?.creatorPackage
            if (clockPackage != null && AppLockGate.isProtected(context, clockPackage)) {
                AppLockGate.runAfterUnlock(context, clockPackage) { repository.open(nextAlarm) }
                true
            } else {
                repository.open(nextAlarm)
            }
        },
        dismiss = {
            if (isTomorrow) repository.dismissTomorrow(nextAlarm) else repository.dismiss(nextAlarm)
            alarm = null
        },
        appLabel = remember(nextAlarm) { repository.appLabel(nextAlarm) },
        hideAlarmsFromApp = {
            repository.hideAlarmsFromApp(nextAlarm)
            alarm = null
        },
    )
}

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
            calendarRelativeTimeLabel(glance.alarm.triggerTime, glance.nowMillis)
        }
    val scheduleLabel = "$alarmTime • $relativeLabel"
    val title = stringResource(R.string.home_upcoming_alarm)
    val failureMessage = stringResource(R.string.common_error_unable_to_open, title)
    val showFailure = { Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show() }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(
            start = 7.dp,
            top = DesignTokens.SpacingMedium,
            bottom = DesignTokens.SpacingMedium,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).combinedClickable(
                onClick = { if (!glance.open()) showFailure() },
                onLongClick = { if (glance.appLabel != null) showMenu = true },
            ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpcomingAlarmAppIcon(glance.alarm.showIntent?.creatorPackage)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = scheduleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(R.string.home_upcoming_alarm_hide_app, glance.appLabel.orEmpty()))
                    },
                    leadingIcon = { Icon(imageVector = MaterialSymbols.VisibilityOff, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        glance.hideAlarmsFromApp()
                    },
                )
            }
        }
        IconButton(
            onClick = glance.dismiss,
            modifier = Modifier.size(UpcomingAlarmDismissSize),
        ) {
            Icon(
                imageVector = MaterialSymbols.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The clock app's icon, or the generic alarm icon when its package is unknown or still loading. */
@Composable
private fun UpcomingAlarmAppIcon(packageName: String?) {
    val appIcon = packageName?.let { rememberAppIcon(packageName = it).bitmap }
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}
