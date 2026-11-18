package com.tk.quicksearch.widgetsPanel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.tk.quicksearch.R

@Composable
internal fun Modifier.widgetEditAction(onEdit: (() -> Unit)?): Modifier {
    if (onEdit == null) return this
    val label = stringResource(R.string.widget_edit_layout)
    return semantics {
        contentDescription = label
        role = Role.Button
        onClick {
            onEdit()
            true
        }
    }
}

@Composable
internal fun Modifier.widgetLayoutActions(
    layout: WidgetGridResize,
    minColumnSpan: Int,
    minRowSpan: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: ((columnDelta: Int, rowDelta: Int) -> Boolean)?,
    onResize: ((WidgetGridResize) -> Unit)?,
): Modifier {
    val (column, _, columnSpan, rowSpan) = layout
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val label = stringResource(R.string.widget_edit_layout)
    val moveStart = stringResource(if (rtl) R.string.widget_move_right else R.string.widget_move_left)
    val moveEnd = stringResource(if (rtl) R.string.widget_move_left else R.string.widget_move_right)
    val moveUp = stringResource(R.string.action_move_up)
    val moveDown = stringResource(R.string.action_move_down)
    val growWidth = stringResource(R.string.widget_increase_width)
    val shrinkWidth = stringResource(R.string.widget_decrease_width)
    val growHeight = stringResource(R.string.widget_increase_height)
    val shrinkHeight = stringResource(R.string.widget_decrease_height)
    val canGrowAtEnd = column + columnSpan < WIDGET_PANEL_GRID_COLUMNS
    val actions = buildList {
        if (onMove != null) {
            if (column > 0) add(CustomAccessibilityAction(moveStart) { onMove(-1, 0) })
            if (canGrowAtEnd) add(CustomAccessibilityAction(moveEnd) { onMove(1, 0) })
            if (canMoveUp) add(CustomAccessibilityAction(moveUp) { onMove(0, -1) })
            if (canMoveDown) add(CustomAccessibilityAction(moveDown) { onMove(0, 1) })
        }
        if (onResize != null) {
            if (canGrowAtEnd || column > 0) {
                val resized = layout.copy(
                    column = if (canGrowAtEnd) column else column - 1,
                    columnSpan = columnSpan + 1,
                )
                add(CustomAccessibilityAction(growWidth) { onResize(resized); true })
            }
            if (columnSpan > minColumnSpan) {
                add(CustomAccessibilityAction(shrinkWidth) { onResize(layout.copy(columnSpan = columnSpan - 1)); true })
            }
            if (rowSpan < WIDGET_PANEL_MAX_ROW_SPAN) {
                add(CustomAccessibilityAction(growHeight) { onResize(layout.copy(rowSpan = rowSpan + 1)); true })
            }
            if (rowSpan > minRowSpan) {
                add(CustomAccessibilityAction(shrinkHeight) { onResize(layout.copy(rowSpan = rowSpan - 1)); true })
            }
        }
    }
    return if (actions.isEmpty()) this else semantics {
        contentDescription = label
        customActions = actions
    }
}
