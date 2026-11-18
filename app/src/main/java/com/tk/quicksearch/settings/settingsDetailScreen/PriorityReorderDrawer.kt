package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Section ordering and optional enable switches in the shared settings drawer. */
@Composable
internal fun PriorityReorderDialog(
    items: List<SearchSection>,
    onItemsChange: (List<SearchSection>) -> Unit,
    onDismiss: () -> Unit,
    disabledSections: Set<SearchSection> = emptySet(),
    onItemEnabledChange: ((SearchSection, Boolean) -> Unit)? = null,
    titleRes: Int = R.string.top_matches_priority_title,
    infoRes: Int = R.string.top_matches_priority_dialog_info,
) {
    val view = LocalView.current
    val orderedItems = remember(items) { items.toMutableStateList() }
    val moveUp = stringResource(R.string.action_move_up)
    val moveDown = stringResource(R.string.action_move_down)
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index != to.index) {
            orderedItems.add(to.index, orderedItems.removeAt(from.index))
            performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK)
        }
    }
    AppPickerDrawer(
        title = stringResource(titleRes),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(infoRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().clip(AppShapes.extraLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
        ) {
            itemsIndexed(orderedItems, key = { _, section -> section.name }) { index, section ->
                ReorderableItem(state = reorderState, key = section.name) { isDragging ->
                    fun moveBy(delta: Int): Boolean {
                        val current = orderedItems.indexOf(section)
                        val target = current + delta
                        if (current < 0 || target !in orderedItems.indices) return false
                        orderedItems.add(target, orderedItems.removeAt(current))
                        onItemsChange(orderedItems.toList())
                        return true
                    }
                    SegmentedTile(
                        index = if (isDragging) 0 else index,
                        count = if (isDragging) 1 else orderedItems.size,
                        containerColor = if (isDragging) {
                            AppColors.AccentTintStrong.compositeOver(AppColors.getSettingsCardContainerColor())
                        } else {
                            AppColors.getSettingsCardContainerColor()
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(
                                end = DesignTokens.SpacingLarge,
                                top = DesignTokens.SpacingXSmall,
                                bottom = DesignTokens.SpacingXSmall,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.DragHandle,
                                contentDescription = stringResource(R.string.settings_action_reorder),
                                tint = if (isDragging) AppColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DesignTokens.Spacing48)
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.GESTURE_START)
                                        },
                                        onDragStopped = {
                                            if (orderedItems != items) onItemsChange(orderedItems.toList())
                                            performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.GESTURE_END)
                                        },
                                    ).padding(DesignTokens.SpacingMedium),
                            )
                            Text(
                                text = stringResource(SearchSectionUiMetadataRegistry.metadataFor(section).sectionLabelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f).semantics {
                                    customActions = buildList {
                                        if (index > 0) add(CustomAccessibilityAction(moveUp) { moveBy(-1) })
                                        if (index < orderedItems.lastIndex) add(CustomAccessibilityAction(moveDown) { moveBy(1) })
                                    }
                                },
                            )
                            if (onItemEnabledChange != null) {
                                val checked = section !in disabledSections
                                AppSwitch(
                                    checked = checked,
                                    onCheckedChange = { enabled ->
                                        if (!enabled && orderedItems.count { it !in disabledSections } <= 1 && checked) {
                                            AppMessages.show(view.context, view.context.getString(R.string.settings_sections_at_least_one_required))
                                        } else {
                                            onItemEnabledChange(section, enabled)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
