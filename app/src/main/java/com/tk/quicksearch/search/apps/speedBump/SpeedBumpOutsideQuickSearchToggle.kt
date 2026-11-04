package com.tk.quicksearch.search.apps.speedBump

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure

/**
 * State behind the "SpeedBump outside Quick Search" switch, shared by More options and the
 * searchable settings row. Turning it on without the accessibility service asks for it the same
 * way the other accessibility features do; render [SpeedBumpAccessibilityDisclosureHost] beside it.
 */
@Stable
class SpeedBumpOutsideQuickSearchToggleState internal constructor(
    private val context: Context,
) {
    var isOn by mutableStateOf(SpeedBump.isOutsideQuickSearchEnabled(context))
        private set

    var hasAccessibility by mutableStateOf(LockScreenAccessibilityService.isEnabled(context))
        private set

    internal var showDisclosure by mutableStateOf(false)

    fun setEnabled(enabled: Boolean) {
        SpeedBump.setOutsideQuickSearchEnabled(context, enabled)
        isOn = enabled
        if (enabled && !LockScreenAccessibilityService.isEnabled(context)) requestAccessibility()
    }

    fun requestAccessibility() {
        if (shouldShowAccessibilityDisclosure) {
            showDisclosure = true
        } else {
            openAccessibilitySettings()
        }
    }

    internal fun openAccessibilitySettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    internal fun refresh() {
        isOn = SpeedBump.isOutsideQuickSearchEnabled(context)
        hasAccessibility = LockScreenAccessibilityService.isEnabled(context)
    }
}

@Composable
fun rememberSpeedBumpOutsideQuickSearchToggleState(): SpeedBumpOutsideQuickSearchToggleState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(context) { SpeedBumpOutsideQuickSearchToggleState(context) }
    DisposableEffect(lifecycleOwner, state) {
        // Accessibility is granted in system settings, and the switch can change from another row.
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) state.refresh()
            }
        val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> state.refresh() }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        SpeedBump.registerChangeListener(context, preferenceListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            SpeedBump.unregisterChangeListener(context, preferenceListener)
        }
    }
    return state
}

@Composable
fun SpeedBumpAccessibilityDisclosureHost(state: SpeedBumpOutsideQuickSearchToggleState) {
    if (!state.showDisclosure) return
    LockScreenAccessibilityDisclosureDialog(
        onAgree = {
            state.showDisclosure = false
            state.openAccessibilitySettings()
        },
        onDismiss = { state.showDisclosure = false },
    )
}
