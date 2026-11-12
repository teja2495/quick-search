package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** Airplane mode while it is on; apps can't turn it off, so a tap opens its settings. */
internal class AirplaneModeGlance(
    val onClick: () -> Unit,
)

/** The Wi-Fi hotspot while it is on; apps can't turn it off, so a tap opens its settings. */
internal class HotspotGlance(
    val onClick: () -> Unit,
)

/**
 * The default Wi-Fi network has a sign-in page (hotel, airport, café). Apps can't open the system
 * captive portal sign-in, so a tap opens the Wi-Fi panel, where the system offers it.
 */
internal class WifiSignInGlance(
    val onClick: () -> Unit,
    /** Hides the row until the network no longer needs a sign-in. */
    val dismiss: () -> Unit,
)

/** Process-wide so a dismissal survives home being left and recomposed; cleared once signed in. */
internal object WifiSignInDismissal {
    var dismissed by mutableStateOf(false)
}

/** Follows airplane mode while [enabled] and the toggle is on. Reading it needs no permission. */
@Composable
internal fun rememberAirplaneModeGlance(enabled: Boolean): AirplaneModeGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val refreshKey = rememberResumeRefreshKey()
    val show = remember(enabled, refreshKey) { enabled && preferences.isShowAirplaneModeEnabled() }
    var isOn by remember { mutableStateOf(false) }

    DisposableEffect(show, refreshKey) {
        val readState = { isOn = show && isAirplaneModeOn(context) }
        readState()
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = readState()
            }
        val registered = show && registerSystemReceiver(context, receiver, Intent.ACTION_AIRPLANE_MODE_CHANGED)
        onDispose {
            if (registered) runCatching { context.unregisterReceiver(receiver) }
        }
    }

    if (!isOn) return null
    return AirplaneModeGlance(onClick = { openAirplaneModeSettings(context) })
}

internal fun openAirplaneModeSettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
    )
}

internal fun isAirplaneModeOn(context: Context): Boolean =
    runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }.getOrDefault(false)

/**
 * Follows the Wi-Fi hotspot while [enabled] and the toggle is on. There is no public API for its
 * state, so this listens to the system's hotspot state broadcast and reads the current state from
 * the hidden [WifiManager] getter, as the broadcast is not sticky on every device (Samsung). Both
 * need ACCESS_WIFI_STATE, an install-time permission.
 */
@Composable
internal fun rememberHotspotGlance(enabled: Boolean): HotspotGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val refreshKey = rememberResumeRefreshKey()
    val show = remember(enabled, refreshKey) { enabled && preferences.isShowHotspotEnabled() }
    var isOn by remember { mutableStateOf(false) }

    DisposableEffect(show, refreshKey) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    isOn = show && isHotspotOn(context, intent)
                }
            }
        var registered = false
        if (show) {
            val sticky =
                runCatching {
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(ACTION_WIFI_AP_STATE_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    ).also { registered = true }
                }.getOrNull()
            isOn = isHotspotOn(context, sticky)
        } else {
            isOn = false
        }
        onDispose {
            if (registered) runCatching { context.unregisterReceiver(receiver) }
        }
    }

    if (!isOn) return null
    return HotspotGlance(onClick = { openHotspotSettings(context) })
}

internal fun openHotspotSettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(ACTION_TETHER_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
    )
}

/**
 * Follows whether the default network is Wi-Fi behind a captive portal while [enabled] and the
 * toggle is on. Reads the current state up front, then follows the default network callback, which
 * reports capability changes, loss and switches of the default network. Needs only
 * ACCESS_NETWORK_STATE, an install-time permission; the network name would need location, so it is
 * not shown.
 */
@Composable
internal fun rememberWifiSignInGlance(enabled: Boolean): WifiSignInGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val refreshKey = rememberResumeRefreshKey()
    val show = remember(enabled, refreshKey) { enabled && preferences.isShowWifiSignInEnabled() }
    var needsSignIn by remember { mutableStateOf(false) }

    DisposableEffect(show, refreshKey) {
        val connectivityManager =
            if (show) {
                runCatching { context.applicationContext.getSystemService(ConnectivityManager::class.java) }.getOrNull()
            } else {
                null
            }
        needsSignIn = connectivityManager != null && hasCaptivePortalWifi(connectivityManager)
        if (show && !needsSignIn) WifiSignInDismissal.dismissed = false

        // Callbacks arrive on a system thread; state is only written on the main thread.
        val mainHandler = Handler(Looper.getMainLooper())
        var disposed = false
        val callback =
            connectivityManager?.let {
                CaptivePortalWifiCallback(it) { value ->
                    mainHandler.post {
                        if (!disposed) {
                            needsSignIn = value
                            if (!value) WifiSignInDismissal.dismissed = false
                        }
                    }
                }
            }
        val registered = callback != null && registerWifiNetworkCallback(connectivityManager, callback)
        onDispose {
            disposed = true
            mainHandler.removeCallbacksAndMessages(null)
            if (registered) runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    if (!needsSignIn || WifiSignInDismissal.dismissed) return null
    return WifiSignInGlance(
        onClick = { openWifiPanel(context) },
        dismiss = { WifiSignInDismissal.dismissed = true },
    )
}

/** The Wi-Fi panel, where the system offers a network's sign-in page. */
internal fun openWifiPanel(context: Context) {
    val wifiIntents =
        listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI) else null,
            Intent(Settings.ACTION_WIFI_SETTINGS),
        )
    openFirstAvailable(context, *wifiIntents.toTypedArray())
}

internal fun needsCaptivePortalSignIn(capabilities: NetworkCapabilities?): Boolean =
    capabilities != null &&
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

/**
 * Whether any connected Wi-Fi network needs a sign-in. Not only the default network: while Wi-Fi is
 * behind a portal it is unvalidated, so mobile data stays the default network.
 */
internal fun hasCaptivePortalWifi(connectivityManager: ConnectivityManager): Boolean =
    runCatching {
        @Suppress("DEPRECATION")
        connectivityManager.allNetworks.any { needsCaptivePortalSignIn(connectivityManager.getNetworkCapabilities(it)) }
    }.getOrDefault(false)

/** Follows every Wi-Fi network, as [hasCaptivePortalWifi] reads it; register with [registerWifiNetworkCallback]. */
internal fun registerWifiNetworkCallback(
    connectivityManager: ConnectivityManager,
    callback: ConnectivityManager.NetworkCallback,
): Boolean =
    runCatching {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        connectivityManager.registerNetworkCallback(request, callback)
    }.isSuccess

/**
 * Tracks which Wi-Fi networks need a sign-in and reports whether any does after each change, on the
 * system callback thread.
 */
internal class CaptivePortalWifiCallback(
    private val connectivityManager: ConnectivityManager,
    private val onChange: (needsSignIn: Boolean) -> Unit,
) : ConnectivityManager.NetworkCallback() {
    private val captiveNetworks = mutableSetOf<Network>()

    // Before API 26 capabilities are not always reported right after a network becomes available.
    override fun onAvailable(network: Network) =
        update(network, runCatching { connectivityManager.getNetworkCapabilities(network) }.getOrNull())

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
    ) = update(network, networkCapabilities)

    override fun onLost(network: Network) = update(network, null)

    private fun update(
        network: Network,
        capabilities: NetworkCapabilities?,
    ) {
        val value =
            synchronized(captiveNetworks) {
                if (needsCaptivePortalSignIn(capabilities)) captiveNetworks.add(network) else captiveNetworks.remove(network)
                captiveNetworks.isNotEmpty()
            }
        onChange(value)
    }
}

internal fun isHotspotOn(
    context: Context,
    stateIntent: Intent?,
): Boolean {
    val state = stateIntent?.getIntExtra(EXTRA_WIFI_AP_STATE, -1) ?: -1
    if (state != -1) return state == WIFI_AP_STATE_ENABLED
    return runCatching {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        WifiManager::class.java.getMethod("isWifiApEnabled").invoke(wifiManager) as Boolean
    }.getOrDefault(false)
}

private fun registerSystemReceiver(
    context: Context,
    receiver: BroadcastReceiver,
    action: String,
): Boolean =
    runCatching {
        ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
    }.isSuccess

/** The hotspot state broadcast, its state extra and enabled value; hidden [WifiManager] constants. */
internal const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
private const val EXTRA_WIFI_AP_STATE = "wifi_state"
private const val WIFI_AP_STATE_ENABLED = 13

/** The system Hotspot & tethering page; not a public Settings constant. */
private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"

@Composable
internal fun AirplaneModeRow(glance: AirplaneModeGlance) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.AirplanemodeActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_airplane_mode_on),
        pillText = stringResource(R.string.home_turn_off),
        onClick = glance.onClick,
    )
}

@Composable
internal fun HotspotRow(glance: HotspotGlance) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.WifiTethering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_hotspot_on),
        pillText = stringResource(R.string.home_turn_off),
        onClick = glance.onClick,
    )
}

@Composable
internal fun WifiSignInRow(glance: WifiSignInGlance) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.WifiPassword,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_wifi_sign_in),
        onClick = glance.onClick,
        onDismiss = glance.dismiss,
    )
}
