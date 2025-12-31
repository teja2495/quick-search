package com.tk.quicksearch.settings.searchEngines

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.searchEngines.EditAmazonDomainDialog
import com.tk.quicksearch.settings.searchEngines.SearchEngineDivider
import com.tk.quicksearch.settings.searchEngines.SearchEngineSettingsSpacing
import com.tk.quicksearch.settings.searchEngines.getSearchEngineIconColorFilter
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.searchengines.*
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Card containing the list of search engines with drag-and-drop reordering.
 */
@Composable
fun SearchEngineListCard(
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: ((SearchEngine, String) -> Unit)?,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: ((SearchEngine, Boolean) -> Unit)?,
    searchEngineSectionEnabled: Boolean,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null
) {
    Column {
        // Separate enabled and disabled engines
        val enabledEngines = searchEngineOrder.filter { it !in disabledSearchEngines }
        val disabledEngines = searchEngineOrder.filter { it in disabledSearchEngines }

        // Enabled engines card (with drag-to-reorder)
        if (enabledEngines.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    val draggedIndex = remember { mutableIntStateOf(-1) }
                    val dragOffset = remember { mutableFloatStateOf(0f) }
                    val pendingReorder = remember { mutableStateOf(false) }
                    val density = LocalDensity.current
                    val itemHeight = SearchEngineSettingsSpacing.itemHeight

                    // Clear pending reorder flag after order updates
                    LaunchedEffect(searchEngineOrder) {
                        if (pendingReorder.value) {
                            pendingReorder.value = false
                        }
                    }

                    enabledEngines.forEachIndexed { index, engine ->
                        val isDragging = draggedIndex.intValue == index

                        // Calculate the target offset for this item
                        val targetOffset = remember(draggedIndex.intValue, dragOffset.floatValue, index) {
                            if (draggedIndex.intValue < 0) {
                                0.dp
                            } else {
                                val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                                val dragProgress = offsetInDp.value / itemHeight.value

                                when {
                                    index == draggedIndex.intValue -> {
                                        // Dragged item follows the drag directly
                                        offsetInDp
                                    }
                                    else -> {
                                        // Calculate the relative position of this item to the dragged item
                                        val relativeIndex = index - draggedIndex.intValue
                                        calculateOffsetForRelativeItem(relativeIndex, dragProgress, itemHeight)
                                    }
                                }
                            }
                        }

                        // Animate the offset smoothly, but snap when pending reorder
                        val animatedOffset by animateDpAsState(
                            targetValue = targetOffset,
                            animationSpec = if (pendingReorder.value) {
                                snap(delayMillis = 50) // Small delay for visual feedback before snap
                            } else {
                                spring(
                                    dampingRatio = 0.75f, // Slightly more responsive
                                    stiffness = 400f       // Higher stiffness for snappier feel
                                )
                            },
                            label = "rowOffset"
                        )

                            // Calculate if this item is a potential drop target
                            val isDraggedItem = draggedIndex.intValue == index
                            val isPotentialDropTarget = remember(draggedIndex.intValue, dragOffset.floatValue, index) {
                                if (draggedIndex.intValue < 0 || isDraggedItem) {
                                    false
                                } else {
                                    val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                                    val dragProgress = offsetInDp.value / itemHeight.value
                                    val relativeIndex = index - draggedIndex.intValue
                                    val threshold = 0.3f // Lower threshold for visual feedback

                                    when {
                                        relativeIndex > 0 -> dragProgress >= relativeIndex - threshold
                                        relativeIndex < 0 -> dragProgress <= relativeIndex + threshold
                                        else -> false
                                    }
                                }
                            }

                            SearchEngineRow(
                            engine = engine,
                            isEnabled = true,
                            onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                            onDragStart = if (searchEngineSectionEnabled) {
                                {
                                    draggedIndex.intValue = index
                                    dragOffset.floatValue = 0f
                                }
                            } else {
                                {}
                            },
                            onDrag = if (searchEngineSectionEnabled) {
                                { change, dragAmount ->
                                    dragOffset.floatValue += dragAmount.y
                                    change.consume()
                                }
                            } else {
                                { _, _ -> }
                            },
                            onDragEnd = if (searchEngineSectionEnabled) {
                                {
                                    val currentIndex = draggedIndex.intValue
                                    if (currentIndex >= 0) {
                                        val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                                        val dragProgress = offsetInDp.value / itemHeight.value
                                        // More sensitive threshold for better responsiveness
                                        val positionsMoved = when {
                                            dragProgress > 0.4f -> (dragProgress + 0.1f).roundToInt()
                                            dragProgress < -0.4f -> (dragProgress - 0.1f).roundToInt()
                                            else -> 0
                                        }
                                        val newIndex = (currentIndex + positionsMoved)
                                            .coerceIn(0, enabledEngines.lastIndex)

                                        if (newIndex != currentIndex) {
                                            val newOrder = enabledEngines.toMutableList()
                                            val item = newOrder.removeAt(currentIndex)
                                            newOrder.add(newIndex, item)
                                            draggedIndex.intValue = -1
                                            dragOffset.floatValue = 0f
                                            pendingReorder.value = true
                                            // Reconstruct full order with enabled engines first, then disabled
                                            onReorderSearchEngines(newOrder + disabledEngines)
                                        } else {
                                            draggedIndex.intValue = -1
                                            dragOffset.floatValue = 0f
                                            pendingReorder.value = false
                                        }
                                    }
                                }
                            } else {
                                {}
                            },
                            isDragging = isDragging,
                            dragOffset = animatedOffset,
                            isPotentialDropTarget = isPotentialDropTarget,
                            shortcutCode = shortcutCodes[engine] ?: "",
                            shortcutEnabled = shortcutEnabled[engine] ?: true,
                            onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                            onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                            showToggle = searchEngineSectionEnabled,
                            allowDrag = searchEngineSectionEnabled,
                            switchEnabled = searchEngineSectionEnabled,
                            amazonDomain = if (engine == SearchEngine.AMAZON) amazonDomain else null,
                            onSetAmazonDomain = if (engine == SearchEngine.AMAZON) onSetAmazonDomain else null,
                            onMoveToTop = if (searchEngineSectionEnabled && index > 0) {
                                {
                                    val newOrder = enabledEngines.toMutableList()
                                    val item = newOrder.removeAt(index)
                                    newOrder.add(0, item)
                                    onReorderSearchEngines(newOrder + disabledEngines)
                                }
                            } else null,
                            onMoveToBottom = if (searchEngineSectionEnabled && index < enabledEngines.lastIndex) {
                                {
                                    val newOrder = enabledEngines.toMutableList()
                                    val item = newOrder.removeAt(index)
                                    newOrder.add(item)
                                    onReorderSearchEngines(newOrder + disabledEngines)
                                }
                            } else null
                        )

                        if (index != enabledEngines.lastIndex) {
                            SearchEngineDivider()
                        }
                    }
                }
            }
        }

        // More search engines title (outside the card)
        if (disabledEngines.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_engines_more_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SearchEngineSettingsSpacing.rowHorizontalPadding,
                    top = 24.dp,
                    bottom = 8.dp
                )
            )
        }

        // More search engines card (disabled engines, no drag)
        if (disabledEngines.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    disabledEngines.forEachIndexed { index, engine ->
                        SearchEngineRow(
                            engine = engine,
                            isEnabled = false,
                            onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                            onDragStart = {}, // No drag functionality for disabled engines
                            onDrag = { _, _ -> },
                            onDragEnd = {},
                            isDragging = false,
                            dragOffset = 0.dp,
                            isPotentialDropTarget = false, // Disabled engines can't be drop targets
                            shortcutCode = shortcutCodes[engine] ?: "",
                            shortcutEnabled = shortcutEnabled[engine] ?: true,
                            onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                            onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                            showToggle = searchEngineSectionEnabled,
                            allowDrag = false, // No drag for disabled engines
                            switchEnabled = searchEngineSectionEnabled,
                            amazonDomain = if (engine == SearchEngine.AMAZON) amazonDomain else null,
                            onSetAmazonDomain = if (engine == SearchEngine.AMAZON) onSetAmazonDomain else null,
                            onMoveToTop = null, // No move actions for disabled engines
                            onMoveToBottom = null
                        )

                        if (index != disabledEngines.lastIndex) {
                            SearchEngineDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual row for a search engine in the list.
 */
@Composable
private fun SearchEngineRow(
    engine: SearchEngine,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    isDragging: Boolean = false,
    dragOffset: androidx.compose.ui.unit.Dp = 0.dp,
    isPotentialDropTarget: Boolean = false,
    shortcutCode: String = "",
    shortcutEnabled: Boolean = true,
    onShortcutCodeChange: ((String) -> Unit)? = null,
    onShortcutToggle: ((Boolean) -> Unit)? = null,
    showToggle: Boolean = true,
    allowDrag: Boolean = true,
    switchEnabled: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    val engineName = engine.getDisplayName()
    val drawableId = engine.getDrawableResId()
    var showMenu by remember { mutableStateOf(false) }

    // Enhanced animation states for better drag feedback
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "dragScale"
    )

    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "dragElevation"
    )

    // Background highlight for potential drop targets
    val dropTargetAlpha by animateFloatAsState(
        targetValue = if (isPotentialDropTarget && !isDragging) 0.1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dropTargetAlpha"
    )

    // Get the primary color for drop target highlighting
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset)
            .scale(dragScale)
            .shadow(
                elevation = dragElevation,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .then(
                if (dropTargetAlpha > 0f) {
                    Modifier.drawBehind {
                        drawRect(
                            color = primaryColor.copy(alpha = dropTargetAlpha),
                            size = size
                        )
                    }
                } else Modifier
            )
            .padding(
                horizontal = SearchEngineSettingsSpacing.rowHorizontalPadding,
                vertical = SearchEngineSettingsSpacing.rowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showToggle && allowDrag) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.settings_action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (allowDrag) {
                            Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        onDragStart()
                                    },
                                    onDrag = { change, dragAmount ->
                                        // More responsive drag handling
                                        onDrag(change, dragAmount)
                                    },
                                    onDragEnd = { onDragEnd() }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // Content area with long press for menu
        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        if (onMoveToTop != null || onMoveToBottom != null) {
                            showMenu = true
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = engineName,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                colorFilter = getSearchEngineIconColorFilter(engine)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = engineName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (shortcutCode.isNotEmpty()) {
                    ShortcutCodeDisplay(
                        shortcutCode = shortcutCode,
                        isEnabled = shortcutEnabled,
                        onCodeChange = onShortcutCodeChange,
                        onToggle = onShortcutToggle,
                        engineName = engineName
                    )
                }
                // Show Amazon domain configuration link
                if (engine == SearchEngine.AMAZON && onSetAmazonDomain != null) {
                    AmazonDomainLink(
                        amazonDomain = amazonDomain,
                        onSetAmazonDomain = onSetAmazonDomain
                    )
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(24.dp),
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
            ) {
                // Show "Move to top" if available
                if (onMoveToTop != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_action_move_up)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onMoveToTop.invoke()
                            showMenu = false
                        }
                    )
                }

                // Show divider if both options are available
                if (onMoveToTop != null && onMoveToBottom != null) {
                    HorizontalDivider()
                }

                // Show "Move to bottom" if available
                if (onMoveToBottom != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_action_move_down)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onMoveToBottom.invoke()
                            showMenu = false
                        }
                    )
                }
            }
        }

        if (showToggle) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = switchEnabled
            )
        }
    }
}

/**
 * Calculates the offset for an item relative to the dragged item.
 * Items move completely into position once they start moving.
 */
private fun calculateOffsetForRelativeItem(
    relativeIndex: Int,
    dragProgress: Float,
    itemHeight: Dp
): Dp {
    return when {
        // Item is below the dragged item (dragging down) - move up to fill space
        relativeIndex > 0 -> {
            val threshold = relativeIndex - 0.3f  // Wait longer before moving up
            if (dragProgress >= threshold) {
                -itemHeight * 1.4f  // Move more than item height for better effect
            } else {
                0.dp
            }
        }
        // Item is above the dragged item (dragging up) - move down to fill space
        relativeIndex < 0 -> {
            val threshold = relativeIndex + 0.3f  // Wait longer before moving down
            if (dragProgress <= threshold) {
                itemHeight * 1.3f  // Move more than item height for better effect
            } else {
                0.dp
            }
        }
        else -> 0.dp
    }
}

/**
 * Easing function for smooth transitions (quadratic ease in/out).
 */
private fun easeInOutQuad(t: Float): Float {
    return if (t < 0.5f) {
        2 * t * t
    } else {
        1 - (-2 * t + 2).pow(2) / 2
    }
}

/**
 * Clickable link for configuring Amazon domain, shown in the Amazon search engine row.
 */
@Composable
private fun AmazonDomainLink(
    amazonDomain: String?,
    onSetAmazonDomain: (String?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        EditAmazonDomainDialog(
            currentDomain = amazonDomain,
            onSave = onSetAmazonDomain,
            onDismiss = { showDialog = false }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_amazon_domain_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amazonDomain ?: stringResource(R.string.settings_amazon_domain_default),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showDialog = true }
        )
    }
}
