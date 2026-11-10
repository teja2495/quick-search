package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.notchGesture.NotchGestureSupport
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.permissions.shouldShowAccessibilityDisclosure
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog

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

    Column(modifier = modifier) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding),
            title = stringResource(R.string.settings_home_gestures_title),
            description = stringResource(R.string.settings_home_gestures_desc),
        ) {
            Column {
                val gestureDirections = SwipeDirection.entries
                gestureDirections.forEachIndexed { index, direction ->
                    SettingsNavigationRow(
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
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                    )
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
                val extraHomeGestures =
                    listOf(HomeGesture.DOUBLE_TAP, HomeGesture.TWO_FINGER_SWIPE_UP, HomeGesture.TWO_FINGER_SWIPE_DOWN)
                extraHomeGestures.forEachIndexed { index, gesture ->
                    if (index > 0) HorizontalDivider(color = AppColors.SettingsDivider)
                    SettingsNavigationRow(
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
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                    )
                }
            }
        }

        if (notchGesturesSupported) {
            NotchGesturesCard(
                state = state,
                searchState = searchState,
                modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SectionTopPadding),
            )
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.settings_keyboard_gestures_title),
            description = stringResource(R.string.settings_keyboard_gestures_desc),
        ) {
            Column {
                SettingsNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.action_open_keyboard),
                        description = stringResource(state.keyboardGestureDirection(SwipeGestureAction.OPEN_KEYBOARD)?.titleResId ?: R.string.settings_gesture_none),
                        icon = MaterialSymbols.Keyboard,
                        actionOnPress = { state.selectedKeyboardAction = SwipeGestureAction.OPEN_KEYBOARD },
                    ),
                    contentPadding = PaddingValues(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
                )
                HorizontalDivider(color = AppColors.SettingsDivider)
                SettingsNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(R.string.settings_gesture_close_keyboard),
                        description = stringResource(state.keyboardGestureDirection(SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS)?.titleResId ?: R.string.settings_gesture_none),
                        icon = MaterialSymbols.KeyboardHide,
                        actionOnPress = { state.selectedKeyboardAction = SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS },
                    ),
                    contentPadding = PaddingValues(horizontal = DesignTokens.CardHorizontalPadding, vertical = DesignTokens.CardVerticalPadding),
                )
            }
        }
    }
}
