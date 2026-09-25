package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.tk.quicksearch.search.data.preferences.EdgeGestureActivation
import com.tk.quicksearch.search.data.preferences.EdgeGestureConfig
import com.tk.quicksearch.search.data.preferences.EdgeGestureSide
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

private const val WidthStepDp = 4
/** Enables the system-wide edge swipe handle and customizes its side, position, size and width. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGestureSettingsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    var config by remember { mutableStateOf(preferences.getEdgeGestureConfig()) }
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
                        LockScreenAccessibilityService.setEdgeGesturePreviewVisible(true)
                    }
                    Lifecycle.Event.ON_PAUSE ->
                        LockScreenAccessibilityService.setEdgeGesturePreviewVisible(false)
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            LockScreenAccessibilityService.setEdgeGesturePreviewVisible(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            LockScreenAccessibilityService.setEdgeGesturePreviewVisible(false)
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
                title = stringResource(R.string.settings_edge_gesture_title),
                subtitle = stringResource(R.string.settings_edge_gesture_desc),
                checked = config.enabled,
                onCheckedChange = { enabled ->
                    preferences.setEdgeGestureEnabled(enabled)
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
                Column(
                    modifier =
                        Modifier.padding(
                            start = DesignTokens.SpacingXXLarge,
                            end = DesignTokens.SpacingXXLarge,
                            top = DesignTokens.cardItemTopPadding(isFirstItem = true),
                            bottom = DesignTokens.SpacingLarge,
                        ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    Text(
                        text = stringResource(R.string.settings_edge_gesture_side),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        EdgeGestureSide.entries.forEachIndexed { index, side ->
                            SegmentedButton(
                                selected = config.side == side,
                                onClick = {
                                    preferences.setEdgeGestureSide(side)
                                    config = config.copy(side = side)
                                },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = EdgeGestureSide.entries.size,
                                    ),
                                icon = {},
                            ) {
                                Text(
                                    stringResource(
                                        when (side) {
                                            EdgeGestureSide.LEFT -> R.string.settings_edge_gesture_side_left
                                            EdgeGestureSide.RIGHT -> R.string.settings_edge_gesture_side_right
                                        },
                                    ),
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_edge_gesture_activation),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        EdgeGestureActivation.entries.forEachIndexed { index, activation ->
                            SegmentedButton(
                                selected = config.activation == activation,
                                onClick = {
                                    preferences.setEdgeGestureActivation(activation)
                                    config = config.copy(activation = activation)
                                },
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = EdgeGestureActivation.entries.size,
                                    ),
                                icon = {},
                            ) {
                                Text(
                                    stringResource(
                                        when (activation) {
                                            EdgeGestureActivation.TAP ->
                                                R.string.settings_edge_gesture_activation_tap
                                            EdgeGestureActivation.SLIDE ->
                                                R.string.settings_edge_gesture_activation_slide
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
                SettingsToggleRow(
                    title = stringResource(R.string.settings_edge_gesture_position),
                    checked = false,
                    onCheckedChange = {},
                    showSwitch = false,
                    showDivider = false,
                    sliderDetails =
                        SettingsToggleSliderDetails(
                            value = config.position,
                            onValueChange = { value ->
                                preferences.setEdgeGesturePosition(value)
                                config = config.copy(position = value)
                            },
                            valueRange = 0f..1f,
                            valueLabel = "${(config.position * 100).roundToInt()}%",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
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
                            value = config.size,
                            onValueChange = { value ->
                                preferences.setEdgeGestureSize(value)
                                config = config.copy(size = value)
                            },
                            valueRange = EdgeGestureConfig.MIN_SIZE..EdgeGestureConfig.MAX_SIZE,
                            valueLabel = "${(config.size * 100).roundToInt()}%",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
                        ),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_edge_gesture_visibility),
                    checked = false,
                    onCheckedChange = {},
                    showSwitch = false,
                    showDivider = false,
                    sliderDetails =
                        SettingsToggleSliderDetails(
                            value = config.opacity,
                            onValueChange = { value ->
                                preferences.setEdgeGestureOpacity(value)
                                config = config.copy(opacity = value)
                            },
                            valueRange = 0f..1f,
                            valueLabel = "${(config.opacity * 100).roundToInt()}%",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
                        ),
                )
                SettingsToggleRow(
                    title = stringResource(R.string.settings_edge_gesture_width),
                    checked = false,
                    onCheckedChange = {},
                    showSwitch = false,
                    isLastItem = true,
                    showDivider = false,
                    sliderDetails =
                        SettingsToggleSliderDetails(
                            value = config.widthDp.toFloat(),
                            onValueChange = { value ->
                                val widthDp = (value / WidthStepDp).roundToInt() * WidthStepDp
                                if (widthDp != config.widthDp) {
                                    preferences.setEdgeGestureWidthDp(widthDp)
                                    config = config.copy(widthDp = widthDp)
                                }
                            },
                            valueRange =
                                EdgeGestureConfig.MIN_WIDTH_DP.toFloat()..EdgeGestureConfig.MAX_WIDTH_DP.toFloat(),
                            steps =
                                (EdgeGestureConfig.MAX_WIDTH_DP - EdgeGestureConfig.MIN_WIDTH_DP) / WidthStepDp - 1,
                            valueLabel = "${config.widthDp} dp",
                            valueLabelWidth = DesignTokens.SpacingXXLarge * 2,
                        ),
                )
            }
        }
    }
}
