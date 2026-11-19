package com.tk.quicksearch.search.apps.appLock

import android.content.Context
import android.content.SharedPreferences
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
 * State behind the "Extend App Lock" switch, shared by More options and the
 * searchable settings row. Turning it on without the accessibility service asks for it the same
 * way the other accessibility features do; render [AppLockAccessibilityDisclosureHost] beside it.
 */
@Stable
class AppLockOutsideQuickSearchToggleState internal constructor(
    private val context: Context,
) {
    var isOn by mutableStateOf(AppLock.isOutsideQuickSearchEnabled(context))
        private set

    var hasAccessibility by mutableStateOf(LockScreenAccessibilityService.isEnabled(context))
        private set

    internal var showDisclosure by mutableStateOf(false)

    fun setEnabled(enabled: Boolean) {
        AppLock.setOutsideQuickSearchEnabled(context, enabled)
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
        LockScreenAccessibilityService.openAccessibilitySettings(context)
    }

    internal fun refresh() {
        isOn = AppLock.isOutsideQuickSearchEnabled(context)
        hasAccessibility = LockScreenAccessibilityService.isEnabled(context)
    }
}

@Composable
fun rememberAppLockOutsideQuickSearchToggleState(): AppLockOutsideQuickSearchToggleState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(context) { AppLockOutsideQuickSearchToggleState(context) }
    DisposableEffect(lifecycleOwner, state) {
        // Accessibility is granted in system settings, and the switch can change from another row.
        val lifecycleObserver =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) state.refresh()
            }
        val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> state.refresh() }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        AppLock.registerChangeListener(context, preferenceListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            AppLock.unregisterChangeListener(context, preferenceListener)
        }
    }
    return state
}

@Composable
fun AppLockAccessibilityDisclosureHost(state: AppLockOutsideQuickSearchToggleState) {
    if (!state.showDisclosure) return
    LockScreenAccessibilityDisclosureDialog(
        onAgree = {
            state.showDisclosure = false
            state.openAccessibilitySettings()
        },
        onDismiss = { state.showDisclosure = false },
    )
}
