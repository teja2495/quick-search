package com.tk.quicksearch.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Contacts
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tk.quicksearch.R
import com.tk.quicksearch.search.SearchSection

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
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_sections_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            val draggedIndex = remember { mutableIntStateOf(-1) }
            val dragOffset = remember { mutableFloatStateOf(0f) }
            val pendingReorder = remember { mutableStateOf(false) }
            val density = LocalDensity.current
            val itemHeight = 60.dp // Approximate row height with padding
            
            // Clear pending reorder flag after order updates
            LaunchedEffect(sectionOrder) {
                if (pendingReorder.value) {
                    // Order has updated, clear the flag
                    pendingReorder.value = false
                }
            }
            
            sectionOrder.forEachIndexed { index, section ->
                val isDragging = draggedIndex.intValue == index
                
                // Calculate the target offset for this item - handles multi-position drags
                val targetOffset = remember(draggedIndex.intValue, dragOffset.floatValue, index) {
                    if (draggedIndex.intValue < 0) {
                        0.dp
                    } else {
                        val draggedIdx = draggedIndex.intValue
                        val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                        val dragProgress = offsetInDp.value / itemHeight.value
                        
                        when {
                            isDragging -> {
                                // Dragged item follows the drag directly
                                offsetInDp
                            }
                            else -> {
                                // Calculate the relative position of this item to the dragged item
                                val relativeIndex = index - draggedIdx
                                
                                when {
                                    // Item is below the dragged item (dragging down)
                                    relativeIndex > 0 -> {
                                        val threshold = relativeIndex - 0.5f
                                        when {
                                            dragProgress >= relativeIndex -> -itemHeight
                                            dragProgress > threshold -> {
                                                val progress = ((dragProgress - threshold) * 2f).coerceIn(0f, 1f)
                                                -itemHeight * progress
                                            }
                                            else -> 0.dp
                                        }
                                    }
                                    // Item is above the dragged item (dragging up)
                                    relativeIndex < 0 -> {
                                        val threshold = relativeIndex + 0.5f
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
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )
                    },
                    label = "rowOffset"
                )
                
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
                        val newOrder = sectionOrder.toMutableList()
                        val currentIndex = draggedIndex.intValue
                        if (currentIndex >= 0) {
                            val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                            val dragProgress = offsetInDp.value / itemHeight.value
                            // Calculate how many positions to move based on total drag distance
                            val positionsMoved = when {
                                dragProgress > 0.5f -> dragProgress.roundToInt()
                                dragProgress < -0.5f -> dragProgress.roundToInt()
                                else -> 0
                            }
                            val newIndex = (currentIndex + positionsMoved).coerceIn(0, sectionOrder.lastIndex)
                            
                            if (newIndex != currentIndex) {
                                val item = newOrder.removeAt(currentIndex)
                                newOrder.add(newIndex, item)
                                // Immediately reset drag state - items will snap to 0 offset
                                // The list reorder will put them in their new positions
                                draggedIndex.intValue = -1
                                dragOffset.floatValue = 0f
                                pendingReorder.value = true
                                onReorderSections(newOrder)
                            } else {
                                // No reorder needed, reset immediately
                                draggedIndex.intValue = -1
                                dragOffset.floatValue = 0f
                                pendingReorder.value = false
                            }
                        }
                    },
                    isDragging = isDragging,
                    dragOffset = animatedOffset
                )
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
    dragOffset: androidx.compose.ui.unit.Dp = 0.dp
) {
    val sectionName = when (section) {
        SearchSection.APPS -> stringResource(R.string.section_apps)
        SearchSection.CONTACTS -> stringResource(R.string.section_contacts)
        SearchSection.FILES -> stringResource(R.string.section_files)
    }
    
    val icon = when (section) {
        SearchSection.APPS -> Icons.Rounded.Apps
        SearchSection.CONTACTS -> Icons.Rounded.Contacts
        SearchSection.FILES -> Icons.Rounded.InsertDriveFile
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset)
            .alpha(if (isDragging) 0.8f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = onDrag,
                    onDragEnd = { onDragEnd() }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = stringResource(R.string.settings_action_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Icon(
            imageVector = icon,
            contentDescription = sectionName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        
        Text(
            text = sectionName,
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

