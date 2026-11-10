package com.tk.quicksearch.search.apps.swipeGestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.input.pointer.positionChange
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonPickerDialog
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity

private val AppSwipeTriggerDistance = 24.dp

/** Returns the up and down actions assigned to [app], recomposing when they change. */
@Composable
fun rememberAppSwipeActions(app: AppInfo): Pair<CustomWidgetButtonAction?, CustomWidgetButtonAction?> =
    rememberSwipeActions(AppSwipeGestures.targetKey(app))

/** Returns the up and down actions assigned to [targetKey], recomposing when they change. */
@Composable
fun rememberSwipeActions(targetKey: String): Pair<CustomWidgetButtonAction?, CustomWidgetButtonAction?> {
    val context = LocalContext.current
    val assignments by AppSwipeGestures.assignments(context).collectAsState()
    val upJson = assignments?.get(AppSwipeGestures.key(targetKey, AppSwipeDirection.UP))
    val downJson = assignments?.get(AppSwipeGestures.key(targetKey, AppSwipeDirection.DOWN))
    return remember(upJson, downJson) {
        CustomWidgetButtonAction.fromJson(upJson) to CustomWidgetButtonAction.fromJson(downJson)
    }
}

/**
 * Runs the swipe up / swipe down action assigned to [app] when the icon is swiped vertically.
 * Adds no gesture handling for apps without an assignment, so scrolling is unaffected there.
 */
@Composable
fun Modifier.appSwipeGestures(app: AppInfo): Modifier = swipeGestures(AppSwipeGestures.targetKey(app))

/** Runs the swipe up / swipe down action assigned to [targetKey] (an app or a folder). */
@Composable
fun Modifier.swipeGestures(targetKey: String): Modifier {
    val (upAction, downAction) = rememberSwipeActions(targetKey)
    if (upAction == null && downAction == null) return this
    val context = LocalContext.current
    val view = LocalView.current
    val currentUp by rememberUpdatedState(upAction)
    val currentDown by rememberUpdatedState(downAction)
    return this.pointerInput(targetKey) {
        val triggerDistancePx = AppSwipeTriggerDistance.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var action: CustomWidgetButtonAction? = null
            var isUp = false
            var totalDrag = 0f
            // Only claim the drag in a direction that has an action, so the other direction
            // still scrolls or reaches parent gestures.
            val drag =
                awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                    val candidate = if (overSlop < 0f) currentUp else currentDown
                    if (candidate != null) {
                        change.consume()
                        action = candidate
                        isUp = overSlop < 0f
                        totalDrag = overSlop
                    }
                } ?: return@awaitEachGesture
            val completed =
                verticalDrag(drag.id) { change ->
                    totalDrag += change.positionChange().y
                    change.consume()
                }
            val chosen = action
            val reachedDistance =
                if (isUp) totalDrag <= -triggerDistancePx else totalDrag >= triggerDistancePx
            if (completed && chosen != null && reachedDistance) {
                hapticConfirm(view)()
                WidgetActionActivity.launch(context, chosen)
            }
        }
    }
}

/**
 * Hosts the action picker opened from the app menu's Swipe up / Swipe down buttons. Lives at the
 * search route so it survives the app item leaving composition while the picker drives the query.
 */
@Composable
fun AppSwipeGesturePickerHost(
    searchState: SearchUiState,
    onQueryChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val request by AppSwipeGestures.pickerRequest.collectAsState()
    request?.let { pending ->
        // The picker shares the search query, so put back whatever the user had typed.
        val queryBeforePicker = remember(pending) { searchState.query }
        val close = {
            AppSwipeGestures.clearPickerRequest()
            onQueryChange(queryBeforePicker)
        }
        val isUp = pending.direction == AppSwipeDirection.UP
        // Observed so the picker updates in place when Reset clears the assignment.
        val (assignedUp, assignedDown) = rememberSwipeActions(pending.targetKey)
        val appName = pending.targetName
        val title =
            stringResource(
                if (isUp) R.string.app_swipe_up_picker_title else R.string.app_swipe_down_picker_title,
                appName,
            )
        val setMessage =
            stringResource(
                if (isUp) R.string.app_swipe_up_set_toast else R.string.app_swipe_down_set_toast,
                appName,
            )
        CustomWidgetButtonPickerDialog(
            title = title,
            tipText = stringResource(R.string.app_swipe_picker_tip),
            searchPlaceholder = stringResource(R.string.app_swipe_picker_search_hint),
            currentAction = if (isUp) assignedUp else assignedDown,
            searchState = searchState,
            iconPackPackage = searchState.selectedIconPackPackage,
            onQueryChange = onQueryChange,
            onDismiss = close,
            // Keeps the picker open; the card gives way to the tip once the assignment clears.
            onReset = { AppSwipeGestures.setAction(context, pending.targetKey, pending.direction, null) },
            onSelect = { action ->
                AppSwipeGestures.setAction(context, pending.targetKey, pending.direction, action)
                close()
                Toast.makeText(context, setMessage, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
