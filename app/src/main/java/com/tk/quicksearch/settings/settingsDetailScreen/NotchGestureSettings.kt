package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
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
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Gestures page card for the system-wide notch taps; editing them needs the accessibility service. */
@Composable
internal fun NotchGesturesCard(
    state: GestureSettingsState,
    searchState: SearchUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SettingsCard(
        modifier = modifier,
        title = stringResource(R.string.settings_notch_gestures_title),
        description = stringResource(R.string.settings_notch_gestures_desc),
    ) {
        Column {
            listOf(
                HomeGesture.NOTCH_SINGLE_TAP,
                HomeGesture.NOTCH_DOUBLE_TAP,
                HomeGesture.NOTCH_LONG_PRESS,
            ).forEachIndexed { index, gesture ->
                if (index > 0) HorizontalDivider(color = AppColors.SettingsDivider)
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(gesture.titleResId),
                            icon = gesture.icon(),
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
                            actionOnPress = { state.openNotchGesture(gesture, context) },
                        ),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
                )
            }
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
        mutableStateOf(preferences.getNotchSingleTapAction() == HomeSwipeGestureAction.OPEN_QUICK_SEARCH)
    }
    var showDisclosure by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context)
                    opensQuickSearch =
                        preferences.getNotchSingleTapAction() == HomeSwipeGestureAction.OPEN_QUICK_SEARCH
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestAccessibility = {
        if (shouldShowAccessibilityDisclosure) {
            showDisclosure = true
        } else {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    if (showDisclosure) {
        LockScreenAccessibilityDisclosureDialog(
            onAgree = {
                showDisclosure = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showDisclosure = false },
        )
    }

    SettingsToggleRow(
        title = stringResource(R.string.settings_notch_title),
        subtitle =
            stringResource(
                if (isAccessibilityEnabled) {
                    R.string.settings_notch_launch_desc
                } else {
                    R.string.settings_notch_accessibility_needed
                },
            ),
        checked = isAccessibilityEnabled && opensQuickSearch,
        enabled = isAccessibilityEnabled,
        onDisabledClick = requestAccessibility,
        onCheckedChange = { enabled ->
            preferences.setNotchSingleTapAction(
                if (enabled) HomeSwipeGestureAction.OPEN_QUICK_SEARCH else HomeSwipeGestureAction.NONE,
            )
            preferences.setNotchSingleTapCustomAction(null)
            preferences.setNotchSingleTapAliasTarget(null)
            opensQuickSearch = enabled
        },
        isLastItem = true,
        showDivider = false,
        modifier = modifier,
    )
}
