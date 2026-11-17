package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ChoiceTile
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.shared.SettingsToggleSliderDetails
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlin.math.roundToInt

private const val WidthStepDp = 4
/** Enables the system-wide edge swipe handle and customizes its side, position, size and width. */
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
            LockScreenAccessibilityService.openAccessibilitySettings(context)
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
                LockScreenAccessibilityService.openAccessibilitySettings(context)
            },
            onDismiss = { showDisclosure = false },
        )
    }

    val showAccessibilityRow = config.enabled && !isAccessibilityEnabled
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        val enableCount = if (showAccessibilityRow) 2 else 1
        SegmentedGroup {
            SegmentedTile(index = 0, count = enableCount) {
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
                    leadingIcon = MaterialSymbols.Swipe,
                    isFirstItem = true,
                    isLastItem = true,
                )
            }
            if (showAccessibilityRow) {
                SettingsSegmentNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.permissions_accessibility_lock_screen_title),
                            description =
                                stringResource(R.string.settings_gesture_lock_screen_requires_accessibility),
                            icon = MaterialSymbols.Lock,
                            actionOnPress = ::requestAccessibility,
                        ),
                    index = 1,
                    count = enableCount,
                )
            }
        }

        if (config.enabled && isAccessibilityEnabled) {
            val count = 6
            SegmentedGroup {
                ChoiceTile(
                    title = stringResource(R.string.settings_edge_gesture_side),
                    options =
                        listOf(
                            ChoiceOption(EdgeGestureSide.LEFT, stringResource(R.string.settings_edge_gesture_side_left)),
                            ChoiceOption(EdgeGestureSide.RIGHT, stringResource(R.string.settings_edge_gesture_side_right)),
                        ),
                    selected = config.side,
                    onSelect = { side ->
                        preferences.setEdgeGestureSide(side)
                        config = config.copy(side = side)
                    },
                    index = 0,
                    count = count,
                )
                ChoiceTile(
                    title = stringResource(R.string.settings_edge_gesture_activation),
                    options =
                        listOf(
                            ChoiceOption(
                                EdgeGestureActivation.TAP,
                                stringResource(R.string.settings_edge_gesture_activation_tap),
                            ),
                            ChoiceOption(
                                EdgeGestureActivation.SLIDE,
                                stringResource(R.string.settings_edge_gesture_activation_slide),
                            ),
                        ),
                    selected = config.activation,
                    onSelect = { activation ->
                        preferences.setEdgeGestureActivation(activation)
                        config = config.copy(activation = activation)
                    },
                    index = 1,
                    count = count,
                )
                SettingsSliderTile(
                    title = stringResource(R.string.settings_edge_gesture_position),
                    index = 2,
                    count = count,
                    details =
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
                SettingsSliderTile(
                    title = stringResource(R.string.settings_edge_gesture_size),
                    index = 3,
                    count = count,
                    details =
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
                SettingsSliderTile(
                    title = stringResource(R.string.settings_edge_gesture_visibility),
                    index = 4,
                    count = count,
                    details =
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
                SettingsSliderTile(
                    title = stringResource(R.string.settings_edge_gesture_width),
                    index = 5,
                    count = count,
                    details =
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

/** A slider-only tile (no switch) in a segmented group. */
@Composable
internal fun SettingsSliderTile(
    title: String,
    details: SettingsToggleSliderDetails,
    index: Int,
    count: Int,
) {
    SegmentedTile(index = index, count = count) {
        SettingsToggleRow(
            title = title,
            checked = false,
            onCheckedChange = {},
            showSwitch = false,
            isFirstItem = true,
            isLastItem = true,
            sliderDetails = details,
        )
    }
}
