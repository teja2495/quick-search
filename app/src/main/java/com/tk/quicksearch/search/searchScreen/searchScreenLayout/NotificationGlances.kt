package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.FinishedProgressNotification
import com.tk.quicksearch.search.data.GlanceNotificationAction
import com.tk.quicksearch.search.data.GlanceNotificationsStore
import com.tk.quicksearch.search.data.MissedCallNotification
import com.tk.quicksearch.search.data.OngoingCallNotification
import com.tk.quicksearch.search.data.OtpNotification
import com.tk.quicksearch.search.data.OtpNotifications
import com.tk.quicksearch.search.data.ProgressNotification
import com.tk.quicksearch.search.data.TimerNotification
import com.tk.quicksearch.search.data.WeatherNotification
import com.tk.quicksearch.search.data.WorkoutNotification
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.sendFromUserTap
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** At most this many ongoing (progress and Live Update) and finished ones show on home, running ones first. */
private const val MAX_PROGRESS_ROWS = 3

/** At most this many weather notifications show on home, current conditions first. */
private const val MAX_WEATHER_ROWS = 2

/** Running clock-app timers, live and finished progress notifications, missed and ongoing calls, workouts, one-time codes and weather for the home At a Glance card. */
internal class NotificationGlances(
    val timers: List<TimerNotification>,
    val progress: List<ProgressNotification>,
    /** What progress notifications left when they finished, until the app removes them or they're dismissed. */
    val finishedProgress: List<FinishedProgressNotification>,
    val dismissFinishedProgress: (FinishedProgressNotification) -> Unit,
    /** Newest first; shown as a single summary row. */
    val missedCalls: List<MissedCallNotification>,
    /** Hides the missed calls row until a newer missed call comes in, and clears their notifications. */
    val dismissMissedCalls: () -> Unit,
    val ongoingCalls: List<OngoingCallNotification>,
    val workouts: List<WorkoutNotification>,
    /** The newest one-time code, until dismissed or it is [OtpNotifications.LIFETIME_MILLIS] old. */
    val otp: OtpNotification?,
    val dismissOtp: (OtpNotification) -> Unit,
    /** Read from weather apps' own notifications; no weather service is queried. */
    val weather: List<WeatherNotification>,
    /** Hides the row and clears the notification from the shade when the app allows. */
    val dismissWeather: (WeatherNotification) -> Unit,
    /** Wall clock the timer and call rows count from; ticks every second while one shows. */
    val nowMillis: Long,
)

/**
 * Mirrors [GlanceNotificationsStore] while [enabled], each source behind its own toggle and the
 * notification access shared with notification dots and media.
 */
@Composable
internal fun rememberNotificationGlances(enabled: Boolean): NotificationGlances {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val refreshKey = rememberResumeRefreshKey()
    val allTimers by GlanceNotificationsStore.timers.collectAsState()
    val allProgress by GlanceNotificationsStore.progress.collectAsState()
    val allFinishedProgress by GlanceNotificationsStore.finishedProgress.collectAsState()
    val allMissedCalls by GlanceNotificationsStore.missedCalls.collectAsState()
    val allOngoingCalls by GlanceNotificationsStore.ongoingCalls.collectAsState()
    val allWorkouts by GlanceNotificationsStore.workouts.collectAsState()
    val allOtps by GlanceNotificationsStore.otps.collectAsState()
    val allWeather by GlanceNotificationsStore.weather.collectAsState()
    val hasAccess = remember(refreshKey) { NotificationDotsPermission.hasNotificationListenerAccess(context) }
    val showTimers = remember(refreshKey) { preferences.isShowTimersEnabled() }
    val showProgress = remember(refreshKey) { preferences.isShowProgressNotificationsEnabled() }
    val showMissedCalls = remember(refreshKey) { preferences.isShowMissedCallsEnabled() }
    val showOngoingCalls = remember(refreshKey) { preferences.isShowOngoingCallEnabled() }
    val showWorkouts = remember(refreshKey) { preferences.isShowWorkoutsEnabled() }
    val showOtpCodes = remember(refreshKey) { preferences.isShowOtpCodesEnabled() }
    val showWeather = remember(refreshKey) { preferences.isShowWeatherEnabled() }
    val available = enabled && hasAccess
    val timers = if (available && showTimers) allTimers else emptyList()
    val progress = if (available && showProgress) allProgress.take(MAX_PROGRESS_ROWS) else emptyList()
    val finishedProgress =
        if (available && showProgress) allFinishedProgress.take(MAX_PROGRESS_ROWS - progress.size) else emptyList()
    var missedCallsDismissedAt by remember { mutableLongStateOf(preferences.getMissedCallsDismissedAt()) }
    val missedCalls =
        if (available && showMissedCalls) {
            allMissedCalls.filter { it.callTime > missedCallsDismissedAt }
        } else {
            emptyList()
        }

    val ongoingCalls = if (available && showOngoingCalls) allOngoingCalls else emptyList()
    val workouts = if (available && showWorkouts) allWorkouts else emptyList()
    val weather = if (available && showWeather) allWeather.take(MAX_WEATHER_ROWS) else emptyList()
    var otpClock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val otp =
        if (available && showOtpCodes) {
            allOtps.firstOrNull().takeIf { it != null && it.postTime > otpClock - OtpNotifications.LIFETIME_MILLIS }
        } else {
            null
        }
    // The store only changes when notifications do, so the code is aged out here.
    LaunchedEffect(otp?.key, otp?.postTime, refreshKey) {
        otpClock = System.currentTimeMillis()
        val expiresAt = (otp ?: return@LaunchedEffect).postTime + OtpNotifications.LIFETIME_MILLIS
        delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(0L))
        otpClock = System.currentTimeMillis()
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val ticking = timers.isNotEmpty() || ongoingCalls.any { it.startTime != null }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(ticking) {
        if (!ticking) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1_000L - nowMillis % 1_000L)
            }
        }
    }
    return NotificationGlances(
        timers = timers,
        progress = progress,
        finishedProgress = finishedProgress,
        dismissFinishedProgress = { GlanceNotificationsStore.dismissFinishedProgress(it.key) },
        missedCalls = missedCalls,
        dismissMissedCalls = {
            GlanceNotificationsStore.dismissMissedCalls(missedCalls.map { it.key })
            missedCalls.maxOfOrNull { it.callTime }?.let { newest ->
                preferences.setMissedCallsDismissedAt(newest)
                missedCallsDismissedAt = newest
            }
        },
        ongoingCalls = ongoingCalls,
        workouts = workouts,
        otp = otp,
        dismissOtp = GlanceNotificationsStore::dismissOtp,
        weather = weather,
        dismissWeather = { GlanceNotificationsStore.dismissWeather(it.key) },
        nowMillis = nowMillis,
    )
}

/** Opens the notification's own target, behind the app lock, falling back to launching the app. */
internal fun openNotificationTarget(
    context: Context,
    packageName: String,
    contentIntent: PendingIntent?,
) {
    AppLockGate.runAfterUnlock(context, packageName) {
        launchNotificationTarget(context, packageName, contentIntent)
    }
}

/** [openNotificationTarget] for callers already past the app lock. */
internal fun launchNotificationTarget(
    context: Context,
    packageName: String,
    contentIntent: PendingIntent?,
) {
    if (contentIntent?.sendFromUserTap() == true) return
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    runCatching { context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
internal fun rememberAppLabel(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) {
        val packageManager = context.packageManager
        runCatching {
            packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
        }.getOrDefault(packageName)
    }
}

/**
 * A running or paused timer or stopwatch: its time as the title, "Timer • <app>" (or "• Paused")
 * below, and the clock app's own buttons (such as Pause and Stop) as round icon buttons on the right.
 * A button without an icon (Google Clock's) gets a matching Material icon by its label, and only
 * an unrecognized one falls back to a small text pill.
 */
@Composable
internal fun TimerRow(
    timer: TimerNotification,
    nowMillis: Long,
) {
    val context = LocalContext.current
    val elapsedMillis =
        timer.pausedMillis ?: if (timer.isCountDown) {
            (timer.chronometerBase - nowMillis).coerceAtLeast(0L)
        } else {
            (nowMillis - timer.chronometerBase).coerceAtLeast(0L)
        }
    // Round a countdown up so it reads 0:01 until the last second has fully passed, like the clock app.
    val seconds = if (timer.isCountDown) (elapsedMillis + 999L) / 1_000L else elapsedMillis / 1_000L
    val kind = stringResource(if (timer.isCountDown) R.string.home_timer else R.string.home_stopwatch)
    val state =
        if (timer.pausedMillis != null) {
            stringResource(R.string.home_timer_paused)
        } else {
            rememberAppLabel(timer.packageName)
        }
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = if (timer.isCountDown) MaterialSymbols.Timer else MaterialSymbols.AvTimer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = DateUtils.formatElapsedTime(seconds),
        titleStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        subtitle = "$kind • $state",
        onClick = { openNotificationTarget(context, timer.packageName, timer.contentIntent) },
        trailing =
            timer.actions.takeIf { it.isNotEmpty() }?.let { actions ->
                {
                    Row(
                        modifier = Modifier.animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Each slot crossfades when its button changes, such as Pause turning into Resume.
                        actions.forEachIndexed { index, action ->
                            key(index) {
                                AnimatedContent(
                                    targetState = action,
                                    contentKey = { it.title },
                                    transitionSpec = {
                                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.8f)) togetherWith
                                            (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)) using
                                            SizeTransform(clip = false)
                                    },
                                    label = "timerAction",
                                ) { TimerActionButton(it) }
                            }
                        }
                    }
                }
            },
    )
}

private val TimerActionIconSize = 20.dp

@Composable
private fun TimerActionButton(action: GlanceNotificationAction) {
    val context = LocalContext.current
    // Rasterize at the exact on-screen size so the icon stays sharp.
    val iconPx = with(LocalDensity.current) { TimerActionIconSize.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, action.icon, iconPx) {
        value =
            withContext(Dispatchers.IO) {
                runCatching { action.icon?.loadDrawable(context)?.toBitmap(iconPx, iconPx)?.asImageBitmap() }.getOrNull()
            }
    }
    val bitmap = icon
    val fallbackIcon = remember(action.title) { timerActionIcon(action.title) }
    if (bitmap == null && fallbackIcon == null) {
        GlanceActionChip(label = action.title, onClick = { action.intent.sendFromUserTap() })
        return
    }
    FilledTonalIconButton(
        onClick = { action.intent.sendFromUserTap() },
        modifier = Modifier.size(36.dp),
    ) {
        if (bitmap != null) {
            Icon(bitmap = bitmap, contentDescription = action.title, modifier = Modifier.size(TimerActionIconSize))
        } else if (fallbackIcon != null) {
            Icon(imageVector = fallbackIcon, contentDescription = action.title, modifier = Modifier.size(TimerActionIconSize))
        }
    }
}

/** A Material icon for a clock app button that has none, matched by its (English) label. */
internal fun timerActionIcon(title: String): ImageVector? =
    when (title.trim().lowercase()) {
        "pause" -> MaterialSymbols.Pause
        "resume", "start", "play", "continue" -> MaterialSymbols.PlayArrow
        "reset", "restart" -> MaterialSymbols.RestartAlt
        "stop" -> MaterialSymbols.Stop
        "delete", "remove" -> MaterialSymbols.Delete
        "lap" -> MaterialSymbols.Flag
        "dismiss", "cancel" -> MaterialSymbols.Close
        else -> null
    }

/**
 * A connected call with the caller (or the calling app when it doesn't say), its duration, and a
 * hang up button when the notification offers one. When that hang up only opens the call screen,
 * the button ends the call through Telecom, asking for the permission on first use.
 */
@Composable
internal fun OngoingCallRow(
    call: OngoingCallNotification,
    nowMillis: Long,
) {
    val context = LocalContext.current
    val subtitle = call.caller ?: rememberAppLabel(call.packageName)
    val openCallScreen = { openNotificationTarget(context, call.packageName, call.contentIntent) }
    val endCallPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted || !endCallThroughTelecom(context)) openCallScreen()
        }
    val onHangUp: (() -> Unit)? =
        when {
            call.hangUpIntent != null -> {
                { call.hangUpIntent.sendFromUserTap() }
            }

            call.endsThroughTelecom -> {
                {
                    val hasPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) ==
                            PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        endCallPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                    } else if (!endCallThroughTelecom(context)) {
                        openCallScreen()
                    }
                }
            }

            else -> null
        }
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_ongoing_call),
        subtitle = subtitle,
        pillText =
            call.startTime
                ?.let { start -> (nowMillis - start) / 1_000L }
                // Hidden for the first minute so a just-connected call doesn't show 00:00.
                ?.takeIf { elapsedSeconds -> elapsedSeconds >= 60L }
                ?.let(DateUtils::formatElapsedTime),
        onClick = openCallScreen,
        trailing =
            onHangUp?.let { hangUp ->
                {
                    FilledIconButton(
                        onClick = hangUp,
                        modifier = Modifier.size(32.dp),
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.CallEnd,
                            contentDescription = stringResource(R.string.home_ongoing_call_hang_up),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
    )
}

/** Ends the current phone call; false when it can't, such as without the permission. */
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal fun endCallThroughTelecom(context: Context): Boolean =
    runCatching { context.getSystemService(TelecomManager::class.java)?.endCall() == true }.getOrDefault(false)

/** A workout in progress with its live stats and the duration the app last posted. */
@Composable
internal fun WorkoutRow(workout: WorkoutNotification) {
    val context = LocalContext.current
    val appLabel = rememberAppLabel(workout.packageName)
    GlanceStatusRow(
        icon = { NotificationAppIcon(workout.packageName) },
        title = workout.title ?: appLabel,
        subtitle = workout.stats ?: appLabel.takeIf { workout.title != null },
        titleMaxLines = GlanceTextMaxLines,
        subtitleStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
        pillText = workout.duration,
        onClick = { openNotificationTarget(context, workout.packageName, workout.contentIntent) },
    )
}

/** A weather app's current conditions or alert, with a button at the top right to dismiss it. */
@Composable
internal fun WeatherRow(
    weather: WeatherNotification,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val appLabel = rememberAppLabel(weather.packageName)
    GlanceStatusRow(
        icon = { NotificationAppIcon(weather.packageName) },
        title = weather.title ?: appLabel,
        subtitle = weather.text ?: appLabel.takeIf { weather.title != null },
        titleMaxLines = GlanceTextMaxLines,
        onClick = { openNotificationTarget(context, weather.packageName, weather.contentIntent) },
        onDismiss = onDismiss,
        dismissAtTop = true,
    )
}

@Composable
internal fun ProgressNotificationRow(notification: ProgressNotification) {
    val context = LocalContext.current
    val appLabel = rememberAppLabel(notification.packageName)
    if (notification.progressMax == 0) {
        // A Live Update without a bar, such as a ride's "Driver 3 min away".
        GlanceStatusRow(
            icon = { NotificationAppIcon(notification.packageName) },
            title = notification.title ?: appLabel,
            subtitle = notification.text ?: appLabel.takeIf { notification.title != null },
            titleMaxLines = GlanceTextMaxLines,
            pillText = notification.shortCriticalText,
            onClick = {
                openNotificationTarget(context, notification.packageName, notification.contentIntent)
            },
        )
        return
    }
    val fraction = notification.progress.toFloat() / notification.progressMax
    val percentLabel = remember(fraction) { NumberFormat.getPercentInstance().format(fraction.toDouble()) }
    val fullLabel = remember { NumberFormat.getPercentInstance().format(1.0) }
    GlanceStatusRow(
        icon = { NotificationAppIcon(notification.packageName) },
        title = notification.title ?: appLabel,
        subtitle = notification.text ?: appLabel.takeIf { notification.title != null },
        titleMaxLines = GlanceTextMaxLines,
        pillText = percentLabel,
        pillWidthText = fullLabel,
        onClick = {
            openNotificationTarget(context, notification.packageName, notification.contentIntent)
        },
        belowText = {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
            )
        },
    )
}

/** The notification an app left when its progress finished, with a button to hide it from home. */
@Composable
internal fun FinishedProgressNotificationRow(
    notification: FinishedProgressNotification,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val appLabel = rememberAppLabel(notification.packageName)
    GlanceStatusRow(
        icon = { NotificationAppIcon(notification.packageName) },
        title = notification.title ?: appLabel,
        subtitle = notification.text ?: appLabel.takeIf { notification.title != null },
        titleMaxLines = GlanceTextMaxLines,
        pillText = stringResource(R.string.reminder_status_done),
        onClick = {
            openNotificationTarget(context, notification.packageName, notification.contentIntent)
        },
        onDismiss = onDismiss,
    )
}

/** The posting app's icon for a notification row, or a generic app icon while it loads. */
@Composable
internal fun NotificationAppIcon(packageName: String) {
    val appIcon = rememberAppIcon(packageName = packageName, iconPackPackage = LocalGlanceIconPackPackage.current).bitmap
    if (appIcon != null) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Icon(
            imageVector = MaterialSymbols.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
