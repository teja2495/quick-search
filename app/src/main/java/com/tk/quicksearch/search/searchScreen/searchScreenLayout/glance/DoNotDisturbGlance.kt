package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** Do Not Disturb while it is on. */
internal class DoNotDisturbGlance(
    val onClick: () -> Unit,
)

/**
 * Follows the interruption filter while [enabled] and the toggle is on. Reading it needs no
 * permission. Turning it off needs Do Not Disturb access, which notification access also grants;
 * from Android 15 an app's filter change only switches a mode of its own, so a tap opens the Do Not
 * Disturb settings there instead.
 */
@Composable
internal fun rememberDoNotDisturbGlance(enabled: Boolean): DoNotDisturbGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val notificationManager = remember(context) { context.getSystemService(NotificationManager::class.java) }
    val refreshKey = rememberResumeRefreshKey()
    val show = remember(refreshKey) { preferences.isShowDoNotDisturbEnabled() }
    var filter by remember { mutableIntStateOf(NotificationManager.INTERRUPTION_FILTER_ALL) }

    DisposableEffect(show, refreshKey, notificationManager) {
        val readFilter = {
            filter =
                if (show) {
                    notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
        }
        readFilter()
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = readFilter()
            }
        val registered =
            show &&
                runCatching {
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                }.isSuccess
        onDispose {
            if (registered) runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // A binder call; the grant only changes in Settings, so re-check it on resume.
    val canTurnOff =
        remember(refreshKey, notificationManager) {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                runCatching { notificationManager?.isNotificationPolicyAccessGranted == true }.getOrDefault(false)
        }
    if (!enabled || notificationManager == null || !isDoNotDisturbOn(filter)) return null
    return DoNotDisturbGlance(
        onClick = {
            if (canTurnOff) {
                runCatching { notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) }
            }
            val current = notificationManager.currentInterruptionFilter
            if (isDoNotDisturbOn(current)) openDoNotDisturbSettings(context) else filter = current
        },
    )
}

internal fun isDoNotDisturbOn(filter: Int) =
    filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
        filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
        filter == NotificationManager.INTERRUPTION_FILTER_NONE

internal fun openDoNotDisturbSettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(ACTION_ZEN_MODE_SETTINGS),
        Intent(Settings.ACTION_SOUND_SETTINGS),
    )
}

/** The system Do Not Disturb (Modes, from Android 15) page; not a public Settings constant. */
private const val ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"

@Composable
internal fun DoNotDisturbRow(glance: DoNotDisturbGlance) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.DoNotDisturbOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_dnd_on),
        pillText = stringResource(R.string.home_turn_off),
        onClick = glance.onClick,
    )
}
