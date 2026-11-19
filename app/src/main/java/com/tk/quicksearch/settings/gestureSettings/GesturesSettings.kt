package com.tk.quicksearch.settings.gestureSettings

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
import com.tk.quicksearch.search.data.preferences.SwipeDirection
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.theme.DesignTokens

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
            val rows = GestureSettingTarget.homeRows
            rows.forEachIndexed { index, target ->
                val description =
                    when (target) {
                        is GestureSettingTarget.Swipe -> {
                            val direction = target.direction
                            if (direction == SwipeDirection.RIGHT && state.isDefaultLauncher) {
                                stringResource(state.launcherSwipeRightAction.labelResId())
                            } else {
                                gestureDescription(
                                    state.actions.getValue(direction),
                                    state.customActions[direction],
                                    state.aliasTargets[direction],
                                    searchState,
                                )
                            }
                        }
                        is GestureSettingTarget.Home -> {
                            val gesture = target.gesture
                            homeGestureDescription(
                                state.homeActions.getValue(gesture),
                                state.homeCustomActions[gesture],
                                state.homeAliasTargets[gesture],
                                searchState,
                            )
                        }
                        is GestureSettingTarget.Keyboard -> error("Keyboard gestures have their own settings group")
                    }
                SettingsSegmentNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(target.titleResId),
                            icon = target.icon,
                            description = description,
                            actionOnPress = { state.open(target, context) },
                        ),
                    index = index,
                    count = rows.size,
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
            val rows = GestureSettingTarget.Keyboard.entries
            rows.forEachIndexed { index, target ->
                SettingsSegmentNavigationRow(
                    item = SettingsCardItem(
                        title = stringResource(target.titleResId),
                        description = stringResource(
                            state.keyboardGestureDirection(target.action)?.titleResId ?: R.string.settings_gesture_none,
                        ),
                        icon = target.icon,
                        actionOnPress = { state.open(target, context) },
                    ),
                    index = index,
                    count = rows.size,
                )
            }
        }
    }
}
