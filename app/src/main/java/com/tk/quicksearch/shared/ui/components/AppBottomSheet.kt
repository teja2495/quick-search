package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    swipeToDismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { if (swipeToDismissEnabled) true else it != SheetValue.Hidden },
    )

    // When swipe-to-dismiss is disabled, consume downward scroll leftovers so the sheet
    // never physically moves. onPostScroll/onPostFling run after child scrollables have
    // taken what they need, so verticalScroll inside the sheet still works normally.
    val blockSwipeConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y > 0f) available else Offset.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (available.y > 0f) available else Velocity.Zero
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppColors.DialogBackground,
        tonalElevation = 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        content = {
            if (!swipeToDismissEnabled) {
                Column(modifier = Modifier.nestedScroll(blockSwipeConnection)) {
                    content()
                }
            } else {
                content()
            }
        },
    )
}
