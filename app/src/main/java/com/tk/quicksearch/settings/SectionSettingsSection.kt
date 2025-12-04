package com.tk.quicksearch.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tk.quicksearch.R
import com.tk.quicksearch.search.SearchSection

/**
 * Constants for drag and drop behavior and animations.
 */
private object DragConstants {
    val itemHeight: Dp = 60.dp
    val dragThreshold: Float = 0.5f
    val dragAlpha: Float = 0.8f
    val springDampingRatio: Float = 0.8f
    val springStiffness: Float = 300f
    val rowHorizontalPadding: Dp = 16.dp
    val rowVerticalPadding: Dp = 12.dp
    val iconSize: Dp = 24.dp
    val rowSpacing: Dp = 12.dp
    val titleBottomPadding: Dp = 16.dp
}

/**
 * Data class holding section display metadata.
 */
private data class SectionMetadata(
    val name: String,
    val icon: ImageVector
)

/**
 * Gets the display metadata for a given search section.
 */
@Composable
private fun getSectionMetadata(section: SearchSection): SectionMetadata {
    return when (section) {
        SearchSection.APPS -> SectionMetadata(
            name = stringResource(R.string.section_apps),
            icon = Icons.Rounded.Apps
        )
        SearchSection.CONTACTS -> SectionMetadata(
            name = stringResource(R.string.section_contacts),
            icon = Icons.Rounded.Contacts
        )
        SearchSection.FILES -> SectionMetadata(
            name = stringResource(R.string.section_files),
            icon = Icons.Rounded.InsertDriveFile
        )
    }
}

/**
 * Calculates the target offset for a non-dragged item based on drag progress.
 */
private fun calculateNonDraggedItemOffset(
    relativeIndex: Int,
    dragProgress: Float,
    itemHeight: Dp
): Dp {
    return when {
        relativeIndex > 0 -> {
            // Item is below the dragged item (dragging down)
            val threshold = relativeIndex - DragConstants.dragThreshold
            when {
                dragProgress >= relativeIndex -> -itemHeight
                dragProgress > threshold -> {
                    val progress = ((dragProgress - threshold) * 2f).coerceIn(0f, 1f)
                    -itemHeight * progress
                }
                else -> 0.dp
            }
        }
        relativeIndex < 0 -> {
            // Item is above the dragged item (dragging up)
            val threshold = relativeIndex + DragConstants.dragThreshold
            when {
                dragProgress <= relativeIndex -> itemHeight
                dragProgress < threshold -> {
                    val progress = ((threshold - dragProgress) * 2f).coerceIn(0f, 1f)
                    itemHeight * progress
                }
                else -> 0.dp
            }
        }
        else -> 0.dp
    }
}

/**
 * Calculates the number of positions moved based on drag progress.
 */
private fun calculatePositionsMoved(dragProgress: Float): Int {
    return when {
        dragProgress > DragConstants.dragThreshold -> dragProgress.roundToInt()
        dragProgress < -DragConstants.dragThreshold -> dragProgress.roundToInt()
        else -> 0
    }
}

/**
 * Resets drag state to initial values.
 */
private fun resetDragState(
    draggedIndex: androidx.compose.runtime.MutableIntState,
    dragOffset: androidx.compose.runtime.MutableFloatState
) {
    draggedIndex.intValue = -1
    dragOffset.floatValue = 0f
}

@Composable
fun SectionSettingsSection(
    sectionOrder: List<SearchSection>,
    disabledSections: Set<SearchSection>,
    onToggleSection: (SearchSection, Boolean) -> Unit,
    onReorderSections: (List<SearchSection>) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_sections_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = DragConstants.titleBottomPadding)
    )
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            val draggedIndex = remember { mutableIntStateOf(-1) }
            val dragOffset = remember { mutableFloatStateOf(0f) }
            val pendingReorder = remember { mutableStateOf(false) }
            val density = LocalDensity.current
            
            // Clear pending reorder flag after order updates
            LaunchedEffect(sectionOrder) {
                if (pendingReorder.value) {
                    pendingReorder.value = false
                }
            }
            
            sectionOrder.forEachIndexed { index, section ->
                val isDragging = draggedIndex.intValue == index
                
                // Calculate the target offset for this item
                val targetOffset = remember(draggedIndex.intValue, dragOffset.floatValue, index) {
                    if (draggedIndex.intValue < 0) {
                        0.dp
                    } else {
                        val draggedIdx = draggedIndex.intValue
                        val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                        val dragProgress = offsetInDp.value / DragConstants.itemHeight.value
                        
                        if (isDragging) {
                            // Dragged item follows the drag directly
                            offsetInDp
                        } else {
                            // Calculate offset for non-dragged items
                            val relativeIndex = index - draggedIdx
                            calculateNonDraggedItemOffset(relativeIndex, dragProgress, DragConstants.itemHeight)
                        }
                    }
                }
                
                // Animate the offset smoothly, but snap when pending reorder
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = if (pendingReorder.value) {
                        snap()
                    } else {
                        spring(
                            dampingRatio = DragConstants.springDampingRatio,
                            stiffness = DragConstants.springStiffness
                        )
                    },
                    label = "rowOffset"
                )
                
                Column(
                    modifier = Modifier
                        .offset(y = animatedOffset)
                        .alpha(if (isDragging) DragConstants.dragAlpha else 1f)
                ) {
                    SectionRow(
                        section = section,
                        isEnabled = section !in disabledSections,
                        onToggle = { enabled -> onToggleSection(section, enabled) },
                        onDragStart = {
                            draggedIndex.intValue = index
                            dragOffset.floatValue = 0f
                        },
                        onDrag = { change, dragAmount ->
                            dragOffset.floatValue += dragAmount.y
                            change.consume()
                        },
                        onDragEnd = {
                            val currentIndex = draggedIndex.intValue
                            if (currentIndex >= 0) {
                                val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                                val dragProgress = offsetInDp.value / DragConstants.itemHeight.value
                                val positionsMoved = calculatePositionsMoved(dragProgress)
                                val newIndex = (currentIndex + positionsMoved).coerceIn(0, sectionOrder.lastIndex)
                                
                                if (newIndex != currentIndex) {
                                    val newOrder = sectionOrder.toMutableList()
                                    val item = newOrder.removeAt(currentIndex)
                                    newOrder.add(newIndex, item)
                                    resetDragState(draggedIndex, dragOffset)
                                    pendingReorder.value = true
                                    onReorderSections(newOrder)
                                } else {
                                    resetDragState(draggedIndex, dragOffset)
                                    pendingReorder.value = false
                                }
                            }
                        },
                        isDragging = isDragging,
                        dragOffset = 0.dp
                    )
                }
                
                if (index != sectionOrder.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: SearchSection,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean = false,
    dragOffset: Dp = 0.dp,
    bottomPadding: Dp = DragConstants.rowVerticalPadding
) {
    val metadata = getSectionMetadata(section)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset)
            .alpha(if (isDragging) DragConstants.dragAlpha else 1f)
            .padding(
                start = DragConstants.rowHorizontalPadding,
                end = DragConstants.rowHorizontalPadding,
                top = DragConstants.rowVerticalPadding,
                bottom = bottomPadding
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = onDrag,
                    onDragEnd = { onDragEnd() }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing)
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = stringResource(R.string.settings_action_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DragConstants.iconSize)
        )
        
        Icon(
            imageVector = metadata.icon,
            contentDescription = metadata.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DragConstants.iconSize)
        )
        
        Text(
            text = metadata.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

