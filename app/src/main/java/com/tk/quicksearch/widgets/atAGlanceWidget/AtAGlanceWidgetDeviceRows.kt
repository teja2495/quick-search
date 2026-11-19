package com.tk.quicksearch.widgets.atAGlanceWidget

import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.BatteryPreferences
import com.tk.quicksearch.search.data.preferences.BatteryPreferences.Companion.CRITICAL_BATTERY_THRESHOLD_PERCENT
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.glance.GlanceStatusSurface
import com.tk.quicksearch.search.glance.glanceStatusItems
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.ACTION_WIFI_AP_STATE_CHANGED
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.BatteryStatus
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.WifiSignInDismissal
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.formatShortDuration
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.hasCaptivePortalWifi
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.isAirplaneModeOn
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.isDoNotDisturbOn
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.isHotspotOn
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.text.NumberFormat

/** The sticky battery broadcast, read without registering a receiver. */
internal fun AtAGlanceWidgetRowLoader.batteryStatus(): BatteryStatus? =
    runCatching {
        BatteryStatus.from(
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)),
            context.getSystemService(BatteryManager::class.java),
        )
    }.getOrNull()

private fun formatPercent(percent: Int): String = NumberFormat.getPercentInstance().format(percent / 100.0)

private fun deviceRow(
    type: AtAGlanceWidgetTapType,
    icon: AtAGlanceWidgetIcon,
    title: String,
    subtitle: String? = null,
    dismiss: AtAGlanceWidgetDismissType? = null,
    tappable: Boolean = true,
    titleStyle: AtAGlanceWidgetTitleStyle = AtAGlanceWidgetTitleStyle.BODY,
    pill: AtAGlanceWidgetPill? = null,
) = AtAGlanceWidgetRow(
    key = type.name,
    icon = icon,
    title = title,
    subtitle = subtitle,
    tap = AtAGlanceWidgetTap(type),
    dismiss = dismiss,
    tappable = tappable,
    titleStyle = titleStyle,
    pill = pill,
)

/** A device state that is on, with home's "Turn off" pill. */
private fun AtAGlanceWidgetRowLoader.turnOffRow(
    type: AtAGlanceWidgetTapType,
    icon: ImageVector,
    title: Int,
) = deviceRow(type, AtAGlanceWidgetIcon.Vector(icon), string(title), pill = AtAGlanceWidgetPill(string(R.string.home_turn_off)))

internal fun AtAGlanceWidgetRowLoader.lowBatteryRows(): List<AtAGlanceWidgetRow> {
    if (!items.isShown(GlanceItem.LOW_BATTERY)) return emptyList()
    val battery = batteryStatus()?.takeIf { it.isLow } ?: return emptyList()
    val isCritical = battery.percent <= CRITICAL_BATTERY_THRESHOLD_PERCENT
    val isPowerSaveOn =
        runCatching { context.getSystemService(PowerManager::class.java)?.isPowerSaveMode }.getOrNull() == true
    val detail =
        string(
            when {
                isCritical -> R.string.home_critical_battery_charge_now
                isPowerSaveOn -> R.string.home_low_battery_saver_on
                else -> R.string.home_low_battery_saver_off
            },
        )
    return listOf(
        deviceRow(
            type = AtAGlanceWidgetTapType.LOW_BATTERY,
            icon = AtAGlanceWidgetIcon.Battery(battery.percent, isCritical),
            title =
                string(
                    if (isCritical) R.string.home_critical_battery_title else R.string.settings_at_a_glance_low_battery_title,
                ),
            subtitle = detail,
            titleStyle = if (isCritical) AtAGlanceWidgetTitleStyle.CRITICAL else AtAGlanceWidgetTitleStyle.BODY,
            pill = AtAGlanceWidgetPill(formatPercent(battery.percent), AtAGlanceWidgetPillStyle.ERROR),
        ),
    )
}

internal fun AtAGlanceWidgetRowLoader.wifiSignInRows(): List<AtAGlanceWidgetRow> {
    if (!items.isShown(GlanceItem.WIFI_SIGN_IN)) return emptyList()
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
    val needsSignIn = hasCaptivePortalWifi(connectivityManager)
    if (!needsSignIn || WifiSignInDismissal.dismissed) return emptyList()
    return listOf(
        deviceRow(
            AtAGlanceWidgetTapType.WIFI_SIGN_IN,
            AtAGlanceWidgetIcon.Vector(MaterialSymbols.WifiPassword),
            string(R.string.home_wifi_sign_in),
            dismiss = AtAGlanceWidgetDismissType.WIFI_SIGN_IN,
        ),
    )
}

/** Device states, then charging and sampled status rows, in the home card's order. */
internal fun AtAGlanceWidgetRowLoader.deviceStateRows(): List<AtAGlanceWidgetRow> =
    buildList {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (
            items.isShown(GlanceItem.DO_NOT_DISTURB) &&
            notificationManager != null &&
            isDoNotDisturbOn(notificationManager.currentInterruptionFilter)
        ) {
            add(turnOffRow(AtAGlanceWidgetTapType.DO_NOT_DISTURB, MaterialSymbols.DoNotDisturbOn, R.string.home_dnd_on))
        }
        if (items.isShown(GlanceItem.AIRPLANE_MODE) && isAirplaneModeOn(context)) {
            add(turnOffRow(AtAGlanceWidgetTapType.AIRPLANE_MODE, MaterialSymbols.AirplanemodeActive, R.string.home_airplane_mode_on))
        }
        if (items.isShown(GlanceItem.HOTSPOT)) {
            val stateIntent = runCatching { context.registerReceiver(null, IntentFilter(ACTION_WIFI_AP_STATE_CHANGED)) }.getOrNull()
            if (isHotspotOn(context, stateIntent)) {
                add(turnOffRow(AtAGlanceWidgetTapType.HOTSPOT, MaterialSymbols.WifiTethering, R.string.home_hotspot_on))
            }
        }
        if (items.isShown(GlanceItem.FLASHLIGHT) && AtAGlanceWidgetUpdater.litCameraIds.value.isNotEmpty()) {
            add(turnOffRow(AtAGlanceWidgetTapType.FLASHLIGHT, MaterialSymbols.FlashlightOn, R.string.home_flashlight_on))
        }
        addAll(chargingRows())
        addAll(sampledStatusRows())
    }

private fun AtAGlanceWidgetRowLoader.chargingRows(): List<AtAGlanceWidgetRow> {
    if (!items.isShown(GlanceItem.CHARGING)) return emptyList()
    val preferences = BatteryPreferences(context)
    val battery = batteryStatus()?.takeIf { it.isPlugged } ?: return emptyList()
    // Charging dismissed on home stays hidden until the phone is unplugged, as on home.
    val dismissedPercent = preferences.getChargingDismissedPercent()
    if (dismissedPercent != null && battery.percent >= dismissedPercent) return emptyList()
    val title =
        string(
            when {
                battery.isFull -> R.string.home_charged
                battery.isPaused -> R.string.home_charging_paused
                else -> R.string.settings_at_a_glance_charging_title
            },
        )
    val timeToFull =
        battery.chargeTimeRemainingMillis
            ?.takeIf { !battery.isFull && !battery.isPaused }
            ?.let { string(R.string.home_charging_full_in, formatShortDuration(it)) }
    return listOf(
        deviceRow(
            type = AtAGlanceWidgetTapType.CHARGING,
            icon = AtAGlanceWidgetIcon.Vector(if (battery.isFull) MaterialSymbols.BatteryFull else MaterialSymbols.BatteryChargingFull),
            title = title,
            subtitle = timeToFull,
            dismiss = AtAGlanceWidgetDismissType.CHARGING,
            tappable = false,
            pill = AtAGlanceWidgetPill(formatPercent(battery.percent)),
        ),
    )
}

private fun AtAGlanceWidgetRowLoader.sampledStatusRows(): List<AtAGlanceWidgetRow> =
    glanceStatusItems.mapNotNull { item ->
        if (!items.isShown(item)) return@mapNotNull null
        val status = checkNotNull(item.status)
        val value = status.read(context, GlanceStatusSurface.WIDGET) ?: return@mapNotNull null
        val tint = if (status.isError) AtAGlanceWidgetTint.ERROR else AtAGlanceWidgetTint.TITLE
        AtAGlanceWidgetRow(
            key = item.name,
            icon = AtAGlanceWidgetIcon.Vector(status.icon(), tint),
            title = string(status.rowTitle),
            subtitle = value.subtitle,
            tap = AtAGlanceWidgetTap(AtAGlanceWidgetTapType.STATUS_ITEM, key = item.key),
            dismiss = status.dismissFromWidget?.let { AtAGlanceWidgetDismissType.STATUS_ITEM },
            tappable = status.open != null,
        )
    }
