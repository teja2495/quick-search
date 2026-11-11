package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
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
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/** The flashlight while it is on; a tap turns it off. */
internal class FlashlightGlance(
    val onClick: () -> Unit,
)

/**
 * Follows the torch of every camera while [enabled] and the toggle is on. The torch callback needs
 * no permission and reports each camera's current state as soon as it is registered, then every
 * change. A camera that becomes unavailable (opened by a camera app) counts as off.
 */
@Composable
internal fun rememberFlashlightGlance(enabled: Boolean): FlashlightGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val cameraManager =
        remember(context) { runCatching { context.applicationContext.getSystemService(CameraManager::class.java) }.getOrNull() }
    val refreshKey = rememberResumeRefreshKey()
    val show = remember(enabled, refreshKey) { enabled && preferences.isShowFlashlightEnabled() }
    var litCameraIds by remember { mutableStateOf(emptySet<String>()) }

    DisposableEffect(show, refreshKey, cameraManager) {
        litCameraIds = emptySet()
        val callback =
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(
                    cameraId: String,
                    enabled: Boolean,
                ) {
                    litCameraIds = if (enabled) litCameraIds + cameraId else litCameraIds - cameraId
                }

                override fun onTorchModeUnavailable(cameraId: String) {
                    litCameraIds = litCameraIds - cameraId
                }
            }
        // Callbacks arrive on the main thread, where state is written.
        val registered =
            show &&
                cameraManager != null &&
                runCatching { cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }.isSuccess
        onDispose {
            if (registered) runCatching { cameraManager.unregisterTorchCallback(callback) }
        }
    }

    if (cameraManager == null || litCameraIds.isEmpty()) return null
    val cameraIds = litCameraIds
    return FlashlightGlance(
        onClick = {
            cameraIds.forEach { cameraId -> runCatching { cameraManager.setTorchMode(cameraId, false) } }
        },
    )
}

@Composable
internal fun FlashlightRow(glance: FlashlightGlance) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.FlashlightOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(R.string.home_flashlight_on),
        pillText = stringResource(R.string.home_turn_off),
        onClick = glance.onClick,
    )
}
