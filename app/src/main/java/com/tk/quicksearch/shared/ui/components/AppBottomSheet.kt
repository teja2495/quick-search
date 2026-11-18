package com.tk.quicksearch.shared.ui.components

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * @param swipeToDismissEnabled When false, the sheet can't be dragged at all and the drag handle
 *   is hidden; it can still be closed with the back gesture or the `dismiss` passed to [content].
 * @param dismissOnClickOutside Whether tapping the scrim closes the sheet.
 * @param onFullyExpanded Called once the open animation has settled.
 * @param onDismissStarted Called as soon as the sheet starts closing (back gesture, `dismiss`,
 *   scrim tap or swipe), before the close animation finishes and [onDismissRequest] is called.
 * @param scrimColor Dims the screen behind the sheet; pass [Color.Transparent] to keep the screen
 *   behind fully visible (as when previewing a change live).
 * @param background Drawn behind the whole sheet, drag handle included, in place of the dialog
 *   color.
 * @param content Receives `dismiss`, which animates the sheet closed and then calls
 *   [onDismissRequest].
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    swipeToDismissEnabled: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    onFullyExpanded: (() -> Unit)? = null,
    onDismissStarted: (() -> Unit)? = null,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    background: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnFullyExpanded by rememberUpdatedState(onFullyExpanded)
    val currentOnDismissStarted by rememberUpdatedState(onDismissStarted)
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentOnDismissRequest() }
    }

    LaunchedEffect(Unit) {
        launch {
            snapshotFlow {
                sheetState.currentValue == SheetValue.Expanded && !sheetState.isAnimationRunning
            }.first { it }
            currentOnFullyExpanded?.invoke()
        }
        // The sheet starts out hidden, so wait for it to begin opening before watching for close.
        snapshotFlow { sheetState.targetValue }.first { it != SheetValue.Hidden }
        snapshotFlow { sheetState.targetValue }.first { it == SheetValue.Hidden }
        currentOnDismissStarted?.invoke()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = swipeToDismissEnabled,
        scrimColor = scrimColor,
        properties =
            ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = dismissOnClickOutside,
            ),
        shape = AppShapes.drawerSheet,
        containerColor = if (background == null) AppColors.DialogBackground else Color.Transparent,
        tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle =
            if (swipeToDismissEnabled && background == null) {
                { DragHandle() }
            } else {
                null
            },
        // With a background, the handle and the bottom inset move inside it so it covers them too.
        contentWindowInsets =
            if (background == null) {
                { BottomSheetDefaults.windowInsets }
            } else {
                { WindowInsets(0) }
            },
        content = {
            if (background == null) {
                content(dismiss)
            } else {
                Box {
                    Box(modifier = Modifier.matchParentSize()) { background() }
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(BottomSheetDefaults.windowInsets),
                    ) {
                        if (swipeToDismissEnabled) {
                            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) { DragHandle() }
                        }
                        content(dismiss)
                    }
                }
            }
        },
    )
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = DesignTokens.SpacingMedium)
            .size(width = 32.dp, height = 4.dp)
            .clip(AppShapes.full)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppColors.ContentFaintAlpha))
    )
}
