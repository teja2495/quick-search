package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.preferences.HomeGesture
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

/** Gestures page card for the system-wide notch taps; editing them needs the accessibility service. */
@Composable
internal fun NotchGesturesCard(
    state: GestureSettingsState,
    searchState: SearchUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SegmentedGroup(
        modifier = modifier,
        title = stringResource(R.string.settings_notch_gestures_title),
        description = stringResource(R.string.settings_notch_gestures_desc),
    ) {
        val rows = GestureSettingTarget.notchTargets
        rows.forEachIndexed { index, target ->
            val gesture = target.gesture
            SettingsSegmentNavigationRow(
                item =
                    SettingsCardItem(
                        title = stringResource(target.titleResId),
                        icon = target.icon,
                        description =
                            if (state.isLockScreenAccessibilityEnabled) {
                                homeGestureDescription(
                                    state.homeActions.getValue(gesture),
                                    state.homeCustomActions[gesture],
                                    state.homeAliasTargets[gesture],
                                    searchState,
                                )
                            } else {
                                stringResource(R.string.settings_notch_accessibility_needed)
                            },
                        actionOnPress = { state.open(target, context) },
                    ),
                index = index,
                count = rows.size,
            )
        }
    }
}

/**
 * Launch Options toggle that points the notch single tap at Quick Search. It reads as on only while
 * that is the single-tap action, so choosing anything else for the notch turns it off.
 */
@Composable
internal fun NotchLaunchOptionRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    var isAccessibilityEnabled by remember { mutableStateOf(LockScreenAccessibilityService.isEnabled(context)) }
    var opensQuickSearch by remember {
        mutableStateOf(preferences.actionFor(HomeGesture.NOTCH_SINGLE_TAP) == HomeSwipeGestureAction.OPEN_QUICK_SEARCH)
    }
    var showDisclosure by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context)
                    opensQuickSearch =
                        preferences.actionFor(HomeGesture.NOTCH_SINGLE_TAP) == HomeSwipeGestureAction.OPEN_QUICK_SEARCH
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestAccessibility = {
        if (shouldShowAccessibilityDisclosure) {
            showDisclosure = true
        } else {
            LockScreenAccessibilityService.openAccessibilitySettings(context)
        }
    }

    if (showDisclosure) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                showDisclosure = false
                LockScreenAccessibilityService.openAccessibilitySettings(context)
            },
            onDismiss = { showDisclosure = false },
        )
    }

    val view = LocalView.current
    SegmentedTileRow(
        title = stringResource(R.string.settings_notch_title),
        description = stringResource(
            if (isAccessibilityEnabled) R.string.settings_notch_launch_desc
            else R.string.settings_notch_accessibility_needed,
        ),
        modifier = modifier.then(
            if (isAccessibilityEnabled) {
                Modifier.toggleable(
                    value = opensQuickSearch,
                    role = Role.Switch,
                    onValueChange = { enabled ->
                        hapticToggle(view)()
                        preferences.setActionFor(
                            HomeGesture.NOTCH_SINGLE_TAP,
                            if (enabled) HomeSwipeGestureAction.OPEN_QUICK_SEARCH else HomeSwipeGestureAction.NONE,
                        )
                        preferences.setCustomActionFor(HomeGesture.NOTCH_SINGLE_TAP, null)
                        preferences.setAliasTargetFor(HomeGesture.NOTCH_SINGLE_TAP, null)
                        opensQuickSearch = enabled
                    },
                )
            } else {
                Modifier.clickable(role = Role.Button, onClick = requestAccessibility)
            },
        ),
        leading = { TonalIconBadge(MaterialSymbols.Swipe) },
        trailing = {
            AppSwitch(
                checked = isAccessibilityEnabled && opensQuickSearch,
                enabled = isAccessibilityEnabled,
                onCheckedChange = null,
            )
        },
    )
}
