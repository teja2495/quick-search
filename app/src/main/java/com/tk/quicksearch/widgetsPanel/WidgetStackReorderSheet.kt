package com.tk.quicksearch.widgetsPanel

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.segmentedTileContainerColor
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.coroutines.coroutineContext

private val StackThumbnailWidth = 72.dp
private val StackThumbnailHeight = 48.dp

private class StackPageRow(
    val label: String,
    val appLabel: String,
    val thumbnail: ImageBitmap?,
    /** Drawn instead of a thumbnail, for the Quick Note page. */
    val icon: ImageVector? = null,
)

/**
 * Drawer listing a stack's pages top to bottom in page order. Rows are dragged by their handle,
 * or anywhere after a long press; the new order is saved when a drag ends.
 */
@Composable
internal fun WidgetStackReorderSheet(
    pageIds: List<Int>,
    appWidgetHost: WidgetPanelHost,
    onReorder: (List<Int>) -> Unit,
    onRemove: (pageId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val currentOnReorder by rememberUpdatedState(onReorder)
    val orderedIds = remember(pageIds) { pageIds.toMutableStateList() }
    val moveUp = stringResource(R.string.action_move_up)
    val moveDown = stringResource(R.string.action_move_down)
    val removeLabel = stringResource(R.string.action_remove)
    val thumbWidthPx = with(density) { StackThumbnailWidth.roundToPx() }
    val thumbHeightPx = with(density) { StackThumbnailHeight.roundToPx() }
    val quickNoteLabel = stringResource(R.string.notes_quick_note_title)
    val quickSearchLabel = stringResource(R.string.app_name)

    // Keyed by the set of pages, not their order, so reordering doesn't reload the rows.
    val pageKey = remember(pageIds) { pageIds.toSet() }
    val rows by produceState(emptyMap<Int, StackPageRow>(), pageKey, quickNoteLabel, quickSearchLabel) {
        value = withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            pageKey.mapNotNull { id ->
                coroutineContext.ensureActive()
                if (isQuickNotePage(id)) {
                    return@mapNotNull id to
                        StackPageRow(quickNoteLabel, quickSearchLabel, thumbnail = null, icon = MaterialSymbols.EditNote)
                }
                val info = appWidgetHost.providerInfo(id) ?: return@mapNotNull null
                val label = runCatching { info.loadLabel(packageManager) }.getOrNull().orEmpty()
                val appLabel =
                    runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(info.provider.packageName, 0),
                        ).toString()
                    }.getOrDefault(info.provider.packageName)
                val thumbnail =
                    WidgetPanelSnapshots.read(context, id, info.provider)
                        ?.let { fitThumbnail(it, thumbWidthPx, thumbHeightPx) }
                        ?: runCatching { info.loadIcon(context, density.density.times(160).toInt()) }
                            .getOrNull()
                            ?.let { rasterize(it, thumbHeightPx, thumbHeightPx) }
                id to StackPageRow(label.ifBlank { appLabel }, appLabel, thumbnail)
            }.toMap()
        }
    }

    fun commit() {
        if (orderedIds.toList() != pageIds) currentOnReorder(orderedIds.toList())
    }

    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            if (from.index != to.index) {
                orderedIds.add(to.index, orderedIds.removeAt(from.index))
                performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK)
            }
        }
    val dragStarted: (Offset) -> Unit = {
        performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.GESTURE_START)
    }
    val dragStopped: () -> Unit = {
        commit()
        performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.GESTURE_END)
    }

    AppPickerDrawer(
        title = stringResource(R.string.widget_reorder_stack),
        onDismiss = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.widget_reorder_stack_info),
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
            itemsIndexed(orderedIds, key = { _, id -> id }) { index, id ->
                ReorderableItem(state = reorderState, key = id) { isDragging ->
                    fun moveBy(delta: Int): Boolean {
                        val current = orderedIds.indexOf(id)
                        val target = current + delta
                        if (current < 0 || target !in orderedIds.indices) return false
                        orderedIds.add(target, orderedIds.removeAt(current))
                        commit()
                        return true
                    }
                    val row = rows[id]
                    SegmentedTile(
                        index = if (isDragging) 0 else index,
                        count = if (isDragging) 1 else orderedIds.size,
                        containerColor =
                            if (isDragging) {
                                AppColors.AccentTintStrong.compositeOver(segmentedTileContainerColor())
                            } else {
                                segmentedTileContainerColor()
                            },
                        modifier =
                            Modifier.longPressDraggableHandle(
                                onDragStarted = dragStarted,
                                onDragStopped = dragStopped,
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().padding(
                                    start = DesignTokens.SpacingXSmall,
                                    top = DesignTokens.SpacingMedium,
                                    end = DesignTokens.SpacingXSmall,
                                    bottom = DesignTokens.SpacingMedium,
                                ).semantics {
                                    customActions =
                                        buildList {
                                            if (index > 0) add(CustomAccessibilityAction(moveUp) { moveBy(-1) })
                                            if (index < orderedIds.lastIndex) {
                                                add(CustomAccessibilityAction(moveDown) { moveBy(1) })
                                            }
                                        }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.DragHandle,
                                contentDescription = stringResource(R.string.settings_action_reorder),
                                tint = if (isDragging) AppColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier =
                                    Modifier.size(DesignTokens.Spacing48)
                                        .draggableHandle(
                                            onDragStarted = dragStarted,
                                            onDragStopped = dragStopped,
                                        ).padding(DesignTokens.SpacingMedium),
                            )
                            StackPageThumbnail(row?.thumbnail, row?.icon)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row?.label.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = row?.appLabel.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            AppIconButton(
                                onClick = { onRemove(id) },
                                tooltip = removeLabel,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.DeleteOutline,
                                    contentDescription = removeLabel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StackPageThumbnail(
    thumbnail: ImageBitmap?,
    icon: ImageVector?,
) {
    Box(
        modifier =
            Modifier.size(StackThumbnailWidth, StackThumbnailHeight)
                .clip(AppShapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Scales a full widget snapshot down to the thumbnail it is drawn at, keeping its aspect ratio. */
private fun fitThumbnail(bitmap: Bitmap, maxWidthPx: Int, maxHeightPx: Int): ImageBitmap {
    val scale = minOf(1f, maxWidthPx / bitmap.width.toFloat(), maxHeightPx / bitmap.height.toFloat())
    if (scale >= 1f) return bitmap.asImageBitmap()
    val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return bitmap.scale(width, height).also { if (it !== bitmap) bitmap.recycle() }.asImageBitmap()
}
