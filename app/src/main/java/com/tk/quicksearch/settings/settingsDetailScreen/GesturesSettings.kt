package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.notchGesture.NotchGestureSupport
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal enum class SwipeDirection(val titleResId: Int, val defaultAction: SwipeGestureAction) {
    RIGHT(R.string.settings_gesture_swipe_right, SwipeGestureAction.WIDGETS_PANEL),
    LEFT(R.string.settings_gesture_swipe_left, SwipeGestureAction.SETTINGS),
    UP(R.string.settings_gesture_swipe_up, SwipeGestureAction.OPEN_KEYBOARD),
    DOWN(R.string.settings_gesture_swipe_down, SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS),
}

internal enum class HomeGesture(val titleResId: Int, val defaultAction: HomeSwipeGestureAction) {
    SWIPE_UP(R.string.settings_gesture_swipe_up, HomeSwipeGestureAction.NONE),
    SWIPE_DOWN(R.string.settings_gesture_swipe_down, HomeSwipeGestureAction.NOTIFICATION_PANEL),
    DOUBLE_TAP(R.string.settings_gesture_double_tap_home, HomeSwipeGestureAction.NONE),
    TWO_FINGER_SWIPE_UP(R.string.settings_gesture_two_finger_swipe_up_home, HomeSwipeGestureAction.NONE),
    TWO_FINGER_SWIPE_DOWN(R.string.settings_gesture_two_finger_swipe_down_home, HomeSwipeGestureAction.NONE),
    NOTCH_SINGLE_TAP(R.string.settings_gesture_single_tap, HomeSwipeGestureAction.NONE),
    NOTCH_DOUBLE_TAP(R.string.settings_gesture_double_tap_home, HomeSwipeGestureAction.NONE),
    NOTCH_LONG_PRESS(R.string.settings_gesture_long_press, HomeSwipeGestureAction.NONE),
}

internal val HomeGesture.isTwoFinger: Boolean
    get() = this == HomeGesture.TWO_FINGER_SWIPE_UP || this == HomeGesture.TWO_FINGER_SWIPE_DOWN

/** Notch taps run system-wide from the accessibility service rather than on the Home screen. */
internal val HomeGesture.isNotch: Boolean
    get() =
        this == HomeGesture.NOTCH_SINGLE_TAP ||
            this == HomeGesture.NOTCH_DOUBLE_TAP ||
            this == HomeGesture.NOTCH_LONG_PRESS

internal enum class AliasPickerKind { SEARCH_ENGINE, TOOL }

@Composable
fun GesturesSettingsSection(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
) {
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberGestureSettingsState(searchViewModel)
    GestureSettingsDialogs(state, searchViewModel)
    val context = LocalContext.current
    val notchGesturesSupported = remember(context) { NotchGestureSupport.isSupported(context) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        SegmentedGroup(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.settings_home_gestures_title),
            description = stringResource(R.string.settings_home_gestures_desc),
        ) {
            val gestureDirections = SwipeDirection.entries
            val extraHomeGestures =
                listOf(HomeGesture.DOUBLE_TAP, HomeGesture.TWO_FINGER_SWIPE_UP, HomeGesture.TWO_FINGER_SWIPE_DOWN)
            val gestureCount = gestureDirections.size + extraHomeGestures.size
            gestureDirections.forEachIndexed { index, direction ->
                SettingsSegmentNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(direction.titleResId),
                            icon = direction.gestureIcon(),
                            description =
                                if (direction == SwipeDirection.RIGHT && state.isDefaultLauncher) {
                                    stringResource(state.launcherSwipeRightAction.labelResId())
                                } else if (direction == SwipeDirection.UP || direction == SwipeDirection.DOWN) {
                                    val gesture = if (direction == SwipeDirection.UP) HomeGesture.SWIPE_UP else HomeGesture.SWIPE_DOWN
                                    homeGestureDescription(state.homeActions.getValue(gesture), state.homeCustomActions[gesture], state.homeAliasTargets[gesture], searchState)
                                } else {
                                    gestureDescription(state.actions.getValue(direction), state.customActions[direction], state.aliasTargets[direction], searchState)
                                },
                            actionOnPress = {
                                when (direction) {
                                    SwipeDirection.UP -> state.selectedHomeGesture = HomeGesture.SWIPE_UP
                                    SwipeDirection.DOWN -> state.selectedHomeGesture = HomeGesture.SWIPE_DOWN
                                    else -> state.selectedDirection = direction
                                }
                            },
                        ),
                    index = index,
                    count = gestureCount,
                )
            }
            extraHomeGestures.forEachIndexed { index, gesture ->
                SettingsSegmentNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(gesture.titleResId),
                            icon = gesture.icon(),
                            description = homeGestureDescription(
                                state.homeActions.getValue(gesture),
                                state.homeCustomActions[gesture],
                                state.homeAliasTargets[gesture],
                                searchState,
                            ),
                            actionOnPress = { state.selectedHomeGesture = gesture },
                        ),
                    index = gestureDirections.size + index,
                    count = gestureCount,
                )
            }
        }

        if (notchGesturesSupported) {
            NotchGesturesCard(
                state = state,
                searchState = searchState,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SegmentedGroup(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.settings_keyboard_gestures_title),
            description = stringResource(R.string.settings_keyboard_gestures_desc),
        ) {
            SettingsSegmentNavigationRow(
                item = SettingsCardItem(
                    title = stringResource(R.string.action_open_keyboard),
                    description = stringResource(state.keyboardGestureDirection(SwipeGestureAction.OPEN_KEYBOARD)?.titleResId ?: R.string.settings_gesture_none),
                    icon = MaterialSymbols.Keyboard,
                    actionOnPress = { state.selectedKeyboardAction = SwipeGestureAction.OPEN_KEYBOARD },
                ),
                index = 0,
                count = 2,
            )
            SettingsSegmentNavigationRow(
                item = SettingsCardItem(
                    title = stringResource(R.string.settings_gesture_close_keyboard),
                    description = stringResource(state.keyboardGestureDirection(SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS)?.titleResId ?: R.string.settings_gesture_none),
                    icon = MaterialSymbols.KeyboardHide,
                    actionOnPress = { state.selectedKeyboardAction = SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS },
                ),
                index = 1,
                count = 2,
            )
        }
    }
}
