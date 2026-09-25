package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.FloatingButtonConfig
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.shared.SettingsToggleSliderDetails
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlin.math.roundToInt

private const val SizeStepDp = 4

/** Enables the system-wide floating search button and customizes its size and opacity. */
@Composable
fun FloatingButtonSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    var config by remember { mutableStateOf(preferences.getFloatingButtonConfig()) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(LockScreenAccessibilityService.isEnabled(context))
    }
    var showDisclosure by remember { mutableStateOf(false) }

    fun requestAccessibility() {
        if (shouldShowAccessibilityDisclosure) {
            showDisclosure = true
        } else {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    // Keep customization preview mode synchronized with this page's lifecycle.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        isAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context)
                        LockScreenAccessibilityService.setFloatingButtonPreviewVisible(true)
                    }
                    Lifecycle.Event.ON_PAUSE ->
                        LockScreenAccessibilityService.setFloatingButtonPreviewVisible(false)
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            LockScreenAccessibilityService.setFloatingButtonPreviewVisible(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            LockScreenAccessibilityService.setFloatingButtonPreviewVisible(false)
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_floating_button_title),
                subtitle = stringResource(R.string.settings_floating_button_desc),
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    preferences.setFloatingButtonEnabled(enabled)
                    config = config.copy(enabled = enabled)
                    if (enabled && !LockScreenAccessibilityService.isEnabled(context)) {
                        requestAccessibility()
                    }
                },
                isFirstItem = true,
                isLastItem = !(config.enabled && !isAccessibilityEnabled),
            )
            if (config.enabled && !isAccessibilityEnabled) {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.permissions_accessibility_lock_screen_title),
                            description =
                                stringResource(R.string.settings_gesture_lock_screen_requires_accessibility),
                            actionOnPress = ::requestAccessibility,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )
            }
        }

        if (config.enabled && isAccessibilityEnabled) {
            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_floating_button_move_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            start = DesignTokens.SpacingXXLarge,
                            end = DesignTokens.SpacingXXLarge,
                            top = DesignTokens.cardItemTopPadding(isFirstItem = true),
                        ),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_edge_gesture_size),
                    checked = false,
                    onCheckedChange = {},
                    showSwitch = false,
                    showDivider = false,
                    sliderDetails =
                        SettingsToggleSliderDetails(
                            value = config.sizeDp.toFloat(),
                            onValueChange = { value ->
                                val sizeDp = (value / SizeStepDp).roundToInt() * SizeStepDp
                                if (sizeDp != config.sizeDp) {
                                    preferences.setFloatingButtonSizeDp(sizeDp)
                                    config = config.copy(sizeDp = sizeDp)
                                }
                            },
                            valueRange =
                                FloatingButtonConfig.MIN_SIZE_DP.toFloat()..FloatingButtonConfig.MAX_SIZE_DP.toFloat(),
                            steps =
                                (FloatingButtonConfig.MAX_SIZE_DP - FloatingButtonConfig.MIN_SIZE_DP) / SizeStepDp - 1,
                            valueLabel = "${config.sizeDp} dp",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
                        ),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_floating_button_opacity),
                    checked = false,
                    onCheckedChange = {},
                    showSwitch = false,
                    isLastItem = true,
                    showDivider = false,
                    sliderDetails =
                        SettingsToggleSliderDetails(
                            value = config.opacity,
                            onValueChange = { value ->
                                preferences.setFloatingButtonOpacity(value)
                                config = config.copy(opacity = value)
                            },
                            valueRange = FloatingButtonConfig.MIN_OPACITY..FloatingButtonConfig.MAX_OPACITY,
                            valueLabel = "${(config.opacity * 100).roundToInt()}%",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
                        ),
                )
            }
        }
    }
}
