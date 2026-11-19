package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.BatteryPreferences
import com.tk.quicksearch.search.data.preferences.BatteryPreferences.Companion.CRITICAL_BATTERY_THRESHOLD_PERCENT
import com.tk.quicksearch.search.data.preferences.BatteryPreferences.Companion.LOW_BATTERY_THRESHOLD_PERCENT
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.text.NumberFormat

/** The low battery warning shown in the home At a Glance card. */
internal class LowBatteryGlance(
    val percent: Int,
    val isPowerSaveOn: Boolean,
    val open: () -> Unit,
) {
    val isCritical: Boolean get() = percent <= CRITICAL_BATTERY_THRESHOLD_PERCENT
}

/** The low battery and charging rows share one battery listener; each is null while it has nothing to show. */
internal class BatteryGlances(
    val lowBattery: LowBatteryGlance?,
    val charging: ChargingGlance?,
)

internal data class BatteryStatus(
    val percent: Int,
    val isCharging: Boolean,
    val isPlugged: Boolean,
    val isFull: Boolean,
    /** Plugged in but held below full, such as by a charge limit or while the battery is hot. */
    val isPaused: Boolean,
    /** Estimated time to a full charge, or null when unknown or not charging. */
    val chargeTimeRemainingMillis: Long?,
) {
    val isLow: Boolean get() = !isCharging && percent <= LOW_BATTERY_THRESHOLD_PERCENT

    companion object {
        fun from(
            intent: Intent?,
            batteryManager: BatteryManager?,
        ): BatteryStatus? {
            intent ?: return null
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return null
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val percent = level * 100 / scale
            val systemEstimate =
                if (plugged != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching { batteryManager?.computeChargeTimeRemaining() }.getOrNull()?.takeIf { it > 0L }
                } else {
                    null
                }
            val observedEstimate = ChargeRateEstimator.update(percent = percent, isPlugged = plugged != 0)
            val remaining = systemEstimate ?: observedEstimate
            return BatteryStatus(
                percent = percent,
                isCharging =
                    plugged != 0 ||
                        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL,
                isPlugged = plugged != 0,
                isFull = status == BatteryManager.BATTERY_STATUS_FULL || percent >= 100,
                isPaused = plugged != 0 && status == BatteryManager.BATTERY_STATUS_NOT_CHARGING && percent < 100,
                chargeTimeRemainingMillis = remaining,
            )
        }
    }
}

/**
 * Estimates time to full from the charge rate seen so far, for devices where
 * [BatteryManager.computeChargeTimeRemaining] never returns an estimate. The rate is measured from
 * the first percent step seen while plugged in, so it needs at least two steps. It lives for the
 * process rather than the composition, which is torn down whenever home is left.
 */
private object ChargeRateEstimator {
    private var lastPercent = -1
    private var anchorPercent = -1
    private var anchorMillis = 0L

    fun update(
        percent: Int,
        isPlugged: Boolean,
    ): Long? {
        if (!isPlugged || percent >= 100) {
            lastPercent = -1
            anchorPercent = -1
            return null
        }
        val now = SystemClock.elapsedRealtime()
        when {
            lastPercent < 0 -> Unit
            percent < lastPercent -> anchorPercent = -1
            percent > lastPercent && anchorPercent < 0 -> {
                anchorPercent = percent
                anchorMillis = now
            }
        }
        lastPercent = percent
        if (anchorPercent < 0 || percent <= anchorPercent) return null
        val millisPerPercent = (now - anchorMillis) / (percent - anchorPercent)
        return millisPerPercent * (100 - percent)
    }
}

/** Listens to the battery broadcast while [enabled], visible, and either battery row is turned on. */
@Composable
internal fun rememberBatteryGlances(enabled: Boolean): BatteryGlances {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(appContext) { BatteryPreferences(appContext) }
    val powerManager = remember(appContext) { appContext.getSystemService(PowerManager::class.java) }
    val batteryManager = remember(appContext) { appContext.getSystemService(BatteryManager::class.java) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var battery by remember { mutableStateOf<BatteryStatus?>(null) }
    var isPowerSaveOn by remember { mutableStateOf(false) }
    var showLowBattery by remember { mutableStateOf(false) }
    var showCharging by remember { mutableStateOf(false) }
    var chargingDismissedPercent by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Battery broadcasts arrive every few seconds while charging, so listen only while home is visible.
    // Not keyed on [enabled]: typing a query hides the rows without re-registering the receiver.
    LifecycleStartEffect(appContext, refreshKey) {
        showLowBattery = preferences.isShowLowBatteryEnabled()
        showCharging = preferences.isShowChargingEnabled()
        if (!showLowBattery && !showCharging) {
            battery = null
            return@LifecycleStartEffect onStopOrDispose {}
        }
        // Broadcasts also fire for temperature and voltage; skip those, and the estimate's binder call.
        // The time-to-full estimate can change at a steady level, so re-read it at most once a minute.
        var lastInputs: List<Int>? = null
        var lastReadMillis = 0L
        fun updateBattery(intent: Intent?) {
            val inputs =
                intent?.let {
                    listOf(
                        it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                        it.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
                        it.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
                        it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                    )
                }
            val now = SystemClock.elapsedRealtime()
            if (inputs != null && inputs == lastInputs && now - lastReadMillis < 60_000L) return
            val status = BatteryStatus.from(intent, batteryManager) ?: return
            lastInputs = inputs
            lastReadMillis = now
            battery = status
            // A dismissed charging row stays hidden until the phone is unplugged. A lower level
            // than at dismissal means it was unplugged while home was not showing.
            val dismissedPercent = preferences.getChargingDismissedPercent()
            chargingDismissedPercent =
                if (dismissedPercent != null && (!status.isPlugged || status.percent < dismissedPercent)) {
                    preferences.clearChargingDismissal()
                    null
                } else {
                    dismissedPercent
                }
        }
        fun updatePowerSave() {
            isPowerSaveOn = runCatching { powerManager?.isPowerSaveMode }.getOrNull() == true
        }
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_BATTERY_CHANGED -> updateBattery(intent)
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> updatePowerSave()
                    }
                }
            }
        val filter =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        // Battery changes are sticky, so registering hands back the current level immediately.
        val sticky =
            runCatching {
                ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            }.getOrNull()
        updateBattery(sticky)
        updatePowerSave()
        onStopOrDispose { runCatching { appContext.unregisterReceiver(receiver) } }
    }

    val current = battery.takeIf { enabled }
    return BatteryGlances(
        lowBattery =
            current?.takeIf { showLowBattery && it.isLow }?.let {
                LowBatteryGlance(
                    percent = it.percent,
                    isPowerSaveOn = isPowerSaveOn,
                    open = { openBatterySettings(context) },
                )
            },
        charging =
            current?.takeIf { showCharging && it.isPlugged && chargingDismissedPercent == null }?.let {
                ChargingGlance(
                    percent = it.percent,
                    isFull = it.isFull,
                    isPaused = it.isPaused,
                    timeToFullMillis = it.chargeTimeRemainingMillis,
                    dismiss = {
                        preferences.dismissCharging(it.percent)
                        chargingDismissedPercent = it.percent
                    },
                )
            },
    )
}

internal fun openBatterySettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        Intent(Settings.ACTION_SETTINGS),
    )
}

/** Starts the first of [intents] that resolves, for settings screens that vary by device. */
internal fun openFirstAvailable(
    context: Context,
    vararg intents: Intent,
) {
    intents.firstOrNull { intent ->
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
}

@Composable
internal fun LowBatteryRow(glance: LowBatteryGlance) {
    val percentLabel =
        remember(glance.percent) { NumberFormat.getPercentInstance().format(glance.percent / 100.0) }
    val title =
        stringResource(
            if (glance.isCritical) R.string.home_critical_battery_title else R.string.settings_at_a_glance_low_battery_title,
        )
    val subtitle =
        stringResource(
            when {
                glance.isCritical -> R.string.home_critical_battery_charge_now
                glance.isPowerSaveOn -> R.string.home_low_battery_saver_on
                else -> R.string.home_low_battery_saver_off
            },
        )

    GlanceStatusRow(
        icon = {
            BatteryLevelIcon(
                percent = glance.percent,
                isCritical = glance.isCritical,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        },
        title = title,
        titleColor = if (glance.isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        onClick = glance.open,
        subtitle = subtitle,
        pillText = percentLabel,
        pillColors =
            GlancePillColors(
                container = MaterialTheme.colorScheme.error,
                content = MaterialTheme.colorScheme.onError,
            ),
    )
}

/** A sideways battery outline whose red fill tracks [percent]; [isCritical] turns the outline red too. */
@Composable
private fun BatteryLevelIcon(
    percent: Int,
    isCritical: Boolean,
    modifier: Modifier = Modifier,
) {
    val outlineColor =
        if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        val stroke = 1.5.dp.toPx()
        val nubWidth = 2.dp.toPx()
        val bodyWidth = size.width - nubWidth - 1.dp.toPx()
        val bodyHeight = size.height * 0.5f
        val top = (size.height - bodyHeight) / 2f

        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(stroke / 2f, top),
            size = Size(bodyWidth - stroke, bodyHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(bodyWidth, top + bodyHeight * 0.3f),
            size = Size(nubWidth, bodyHeight * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )

        val inset = stroke + 1.5.dp.toPx()
        // Keep a sliver visible at 0-1% so the icon never reads as an empty outline.
        val fillWidth = (bodyWidth - inset * 2) * (percent / 100f).coerceIn(0.08f, 1f)
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(inset, top + inset),
            size = Size(fillWidth, bodyHeight - inset * 2),
            cornerRadius = CornerRadius(minOf(1.5.dp.toPx(), fillWidth / 2f)),
        )
    }
}
