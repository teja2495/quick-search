package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.util.rememberPhysicalKeyboardConnected
import com.tk.quicksearch.tools.setAlarm.SetAlarmHandler
import com.tk.quicksearch.tools.setAlarm.StartTimerHandler
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.openNotificationShade
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import com.tk.quicksearch.app.startup.StartupTrace
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BOTTOM_BAR_SWIPE_THRESHOLD_PX = 24f

internal data class SearchScreenSwipeActions(
    val onLauncherSwipeUp: () -> Unit,
    val onLauncherSwipeDown: () -> Unit,
    val bottomBarSwipeModifier: Modifier,
)

@Composable
internal fun rememberSearchScreenSwipeActions(
    state: SearchUiState,
    expandedSection: ExpandedSection,
    swipeUpAction: SwipeGestureAction,
    swipeDownAction: SwipeGestureAction,
    swipeUpAliasTarget: String?,
    swipeDownAliasTarget: String?,
    homeSwipeUpAction: HomeSwipeGestureAction,
    homeSwipeDownAction: HomeSwipeGestureAction,
    homeSwipeUpCustomActionJson: String?,
    homeSwipeDownCustomActionJson: String?,
    homeSwipeUpAliasTarget: String?,
    homeSwipeDownAliasTarget: String?,
    onGestureAliasTarget: (Enum<*>, String) -> Unit,
    onCloseQuickSearch: () -> Unit,
    isImeVisible: Boolean,
    searchFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
): SearchScreenSwipeActions {
    val context = LocalContext.current
    val onLauncherSwipeUp: () -> Unit = {
        when {
            swipeUpAction == SwipeGestureAction.OPEN_KEYBOARD && !isImeVisible -> {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
            swipeUpAction == SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS && isImeVisible -> {
                keyboardController?.hide()
            }
            swipeUpAction == SwipeGestureAction.SEARCH_ENGINE || swipeUpAction == SwipeGestureAction.TOOL ->
                swipeUpAliasTarget?.let { onGestureAliasTarget(swipeUpAction, it) }
            else ->
                homeSwipeUpAction.performHomeGesture(
                    homeSwipeUpCustomActionJson,
                    homeSwipeUpAliasTarget,
                    context,
                    { action, target -> onGestureAliasTarget(action, target) },
                    onCloseQuickSearch,
                )
        }
    }
    val onLauncherSwipeDown: () -> Unit = {
        when {
            swipeDownAction == SwipeGestureAction.OPEN_KEYBOARD && !isImeVisible -> {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
            swipeDownAction == SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS && isImeVisible -> {
                keyboardController?.hide()
            }
            swipeDownAction == SwipeGestureAction.SEARCH_ENGINE || swipeDownAction == SwipeGestureAction.TOOL ->
                swipeDownAliasTarget?.let { onGestureAliasTarget(swipeDownAction, it) }
            else ->
                homeSwipeDownAction.performHomeGesture(
                    homeSwipeDownCustomActionJson,
                    homeSwipeDownAliasTarget,
                    context,
                    { action, target -> onGestureAliasTarget(action, target) },
                    onCloseQuickSearch,
                )
        }
    }
    // The engine strip and bottom search bar sit outside the results area, so they need
    // their own vertical drag detector to forward the same swipe gestures. Their inner
    // scrollers are horizontal only, so vertical drags reach this detector unconsumed.
    // Swipe up mirrors the results area: it only fires on an empty query.
    val currentOnLauncherSwipeUp by rememberUpdatedState(onLauncherSwipeUp)
    val currentOnLauncherSwipeDown by rememberUpdatedState(onLauncherSwipeDown)
    val bottomBarSwipeUpEnabled = state.query.isBlank() && expandedSection == ExpandedSection.NONE
    val bottomBarSwipeModifier =
        Modifier.pointerInput(bottomBarSwipeUpEnabled) {
            var accumulatedDragY = 0f
            var gestureHandled = false
            detectVerticalDragGestures(
                onDragStart = {
                    accumulatedDragY = 0f
                    gestureHandled = false
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    if (gestureHandled) return@detectVerticalDragGestures
                    accumulatedDragY += dragAmount
                    when {
                        accumulatedDragY < -BOTTOM_BAR_SWIPE_THRESHOLD_PX -> {
                            if (bottomBarSwipeUpEnabled) currentOnLauncherSwipeUp()
                            gestureHandled = true
                        }
                        accumulatedDragY > BOTTOM_BAR_SWIPE_THRESHOLD_PX -> {
                            currentOnLauncherSwipeDown()
                            gestureHandled = true
                        }
                    }
                },
            )
        }

    return SearchScreenSwipeActions(onLauncherSwipeUp, onLauncherSwipeDown, bottomBarSwipeModifier)
}

internal fun HomeSwipeGestureAction.performHomeGesture(
    actionJson: String?,
    aliasTarget: String?,
    context: android.content.Context,
    onAliasTarget: (HomeSwipeGestureAction, String) -> Unit,
    onCloseQuickSearch: () -> Unit,
) {
    when (this) {
        HomeSwipeGestureAction.CLOSE_QUICK_SEARCH -> onCloseQuickSearch()
        HomeSwipeGestureAction.LOCK_SCREEN -> LockScreenAccessibilityService.lockScreen()
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> context.openNotificationShade()
        HomeSwipeGestureAction.CUSTOM -> {
            CustomWidgetButtonAction.fromJson(actionJson)?.let { action ->
                WidgetActionActivity.launch(context, action)
            }
        }
        HomeSwipeGestureAction.SEARCH_ENGINE,
        HomeSwipeGestureAction.TOOL -> aliasTarget?.let { onAliasTarget(this, it) }
        HomeSwipeGestureAction.NONE -> Unit
    }
}
