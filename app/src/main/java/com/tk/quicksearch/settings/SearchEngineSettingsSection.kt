package com.tk.quicksearch.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tk.quicksearch.R
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.getContentDescription
import com.tk.quicksearch.search.getDrawableResId
import com.tk.quicksearch.search.getDisplayName
import com.tk.quicksearch.settings.EditShortcutDialog
import com.tk.quicksearch.settings.EditAmazonDomainDialog
import com.tk.quicksearch.settings.SettingsSpacing

/**
 * Helper function to get color filter for search engine icons based on theme.
 * Applies filters to convert white icons to black in light mode.
 */
@Composable
private fun getSearchEngineIconColorFilter(engine: SearchEngine): ColorFilter? {
    val needsColorChange = engine in setOf(
        SearchEngine.CHATGPT,
        SearchEngine.GROK,
        SearchEngine.AMAZON
    )
    
    // Check if we're in light mode by checking the background color brightness
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightMode = backgroundColor.red > 0.9f && backgroundColor.green > 0.9f && backgroundColor.blue > 0.9f
    
    return if (needsColorChange && isLightMode) {
        if (engine == SearchEngine.AMAZON) {
            // For Amazon: Use a darker scaling that preserves orange better
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.3f, 0f, 0f, 0f, 0f,     // Red: scale to 30% (preserves orange better)
                        0f, 0.3f, 0f, 0f, 0f,     // Green: scale to 30%
                        0f, 0f, 0.3f, 0f, 0f,     // Blue: scale to 30%
                        0f, 0f, 0f, 1f, 0f        // Alpha: keep
                    )
                )
            )
        } else {
            // For ChatGPT and Grok: simple inversion (all white → all black)
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,  // Red: invert
                        0f, -1f, 0f, 0f, 255f,   // Green: invert
                        0f, 0f, -1f, 0f, 255f,   // Blue: invert
                        0f, 0f, 0f, 1f, 0f       // Alpha: keep
                    )
                )
            )
        }
    } else {
        null
    }
}

/**
 * Main section for configuring search engines.
 * Allows reordering, enabling/disabling, and managing shortcuts.
 */
@Composable
fun SearchEnginesSection(
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    shortcutCodes: Map<SearchEngine, String> = emptyMap(),
    setShortcutCode: ((SearchEngine, String) -> Unit)? = null,
    shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap(),
    setShortcutEnabled: ((SearchEngine, Boolean) -> Unit)? = null,
    searchEngineSectionEnabled: Boolean = true,
    onToggleSearchEngineSectionEnabled: ((Boolean) -> Unit)? = null,
    shortcutsEnabled: Boolean = true,
    onToggleShortcutsEnabled: ((Boolean) -> Unit)? = null,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_search_engines_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
        )
        
        Text(
            text = stringResource(R.string.settings_search_engines_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
        )
    } else {
        Text(
            text = stringResource(R.string.settings_search_engines_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
        )
    }
    
    // Combined toggle card for enabling/disabling search engine section and shortcuts
    if (onToggleSearchEngineSectionEnabled != null && onToggleShortcutsEnabled != null) {
        SearchEngineToggleCard(
            searchEngineSectionEnabled = searchEngineSectionEnabled,
            onToggleSearchEngineSectionEnabled = onToggleSearchEngineSectionEnabled,
            shortcutsEnabled = shortcutsEnabled,
            onToggleShortcutsEnabled = onToggleShortcutsEnabled
        )
    }
    
    // Hide search engine list when both toggles are disabled
    val bothDisabled = !searchEngineSectionEnabled && !shortcutsEnabled
    if (!bothDisabled) {
        SearchEngineListCard(
            searchEngineOrder = searchEngineOrder,
            disabledSearchEngines = disabledSearchEngines,
            onToggleSearchEngine = onToggleSearchEngine,
            onReorderSearchEngines = onReorderSearchEngines,
            shortcutCodes = shortcutCodes,
            setShortcutCode = setShortcutCode,
            shortcutEnabled = shortcutEnabled,
            setShortcutEnabled = setShortcutEnabled,
            shortcutsEnabled = shortcutsEnabled,
            searchEngineSectionEnabled = searchEngineSectionEnabled,
            amazonDomain = amazonDomain,
            onSetAmazonDomain = onSetAmazonDomain
        )
    }
}

/**
 * Toggle card for enabling/disabling search engine section and shortcuts.
 */
@Composable
private fun SearchEngineToggleCard(
    searchEngineSectionEnabled: Boolean,
    onToggleSearchEngineSectionEnabled: (Boolean) -> Unit,
    shortcutsEnabled: Boolean,
    onToggleShortcutsEnabled: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            SearchEngineToggleRow(
                title = stringResource(R.string.settings_search_engines_show_toggle),
                checked = searchEngineSectionEnabled,
                onCheckedChange = onToggleSearchEngineSectionEnabled,
                isFirstItem = true
            )
            
            SearchEngineDivider()
            
            SearchEngineToggleRow(
                title = stringResource(R.string.settings_shortcuts_toggle),
                checked = shortcutsEnabled,
                onCheckedChange = onToggleShortcutsEnabled,
                subtitle = stringResource(R.string.settings_shortcuts_hint),
                isLastItem = true
            )
        }
    }
}

/**
 * Card containing the list of search engines with drag-and-drop reordering.
 */
@Composable
private fun SearchEngineListCard(
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: ((SearchEngine, String) -> Unit)?,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: ((SearchEngine, Boolean) -> Unit)?,
    shortcutsEnabled: Boolean,
    searchEngineSectionEnabled: Boolean,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null
) {
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
            
            searchEngineOrder.forEachIndexed { index, engine ->
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
                        snap()
                    } else {
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )
                    },
                    label = "rowOffset"
                )
                
                SearchEngineRow(
                    engine = engine,
                    isEnabled = engine !in disabledSearchEngines,
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
                                val positionsMoved = when {
                                    dragProgress > 0.5f -> dragProgress.roundToInt()
                                    dragProgress < -0.5f -> dragProgress.roundToInt()
                                    else -> 0
                                }
                                val newIndex = (currentIndex + positionsMoved)
                                    .coerceIn(0, searchEngineOrder.lastIndex)
                                
                                if (newIndex != currentIndex) {
                                    val newOrder = searchEngineOrder.toMutableList()
                                    val item = newOrder.removeAt(currentIndex)
                                    newOrder.add(newIndex, item)
                                    draggedIndex.intValue = -1
                                    dragOffset.floatValue = 0f
                                    pendingReorder.value = true
                                    onReorderSearchEngines(newOrder)
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
                    shortcutsEnabled = shortcutsEnabled,
                    shortcutCode = shortcutCodes[engine] ?: "",
                    shortcutEnabled = shortcutEnabled[engine] ?: true,
                    onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                    onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                    showToggle = searchEngineSectionEnabled,
                    amazonDomain = if (engine == SearchEngine.AMAZON) amazonDomain else null,
                    onSetAmazonDomain = if (engine == SearchEngine.AMAZON) onSetAmazonDomain else null
                )
                
                if (index != searchEngineOrder.lastIndex) {
                    SearchEngineDivider()
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
    shortcutsEnabled: Boolean = false,
    shortcutCode: String = "",
    shortcutEnabled: Boolean = true,
    onShortcutCodeChange: ((String) -> Unit)? = null,
    onShortcutToggle: ((Boolean) -> Unit)? = null,
    showToggle: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null
) {
    val engineName = engine.getDisplayName()
    val drawableId = engine.getDrawableResId()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset)
            .alpha(if (isDragging) 0.8f else 1f)
            .padding(
                horizontal = SearchEngineSettingsSpacing.rowHorizontalPadding,
                vertical = SearchEngineSettingsSpacing.rowVerticalPadding
            )
            .then(
                if (showToggle) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = onDrag,
                            onDragEnd = { onDragEnd() }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showToggle) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.settings_action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
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
            if (shortcutsEnabled && shortcutCode.isNotEmpty()) {
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
        
        if (showToggle) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

/**
 * Section for managing shortcuts separately.
 */
@Composable
fun ShortcutsSection(
    shortcutsEnabled: Boolean,
    onToggleShortcutsEnabled: (Boolean) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: (SearchEngine, String) -> Unit,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_shortcuts_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = SettingsSpacing.sectionTopPadding, bottom = SettingsSpacing.sectionTitleBottomPadding)
    )
    Text(
        text = stringResource(R.string.settings_shortcuts_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Enable/disable shortcuts toggle
            SearchEngineToggleRow(
                title = stringResource(R.string.settings_shortcuts_toggle),
                checked = shortcutsEnabled,
                onCheckedChange = onToggleShortcutsEnabled,
                subtitle = stringResource(R.string.settings_shortcuts_desc),
                isFirstItem = true,
                isLastItem = !shortcutsEnabled
            )
            
            if (shortcutsEnabled) {
                SearchEngineDivider()
                
                // List of shortcuts
                SearchEngine.values().forEachIndexed { index, engine ->
                    ShortcutRow(
                        engine = engine,
                        shortcutCode = shortcutCodes[engine] ?: "",
                        isEnabled = shortcutEnabled[engine] ?: true,
                        onCodeChange = { code -> setShortcutCode(engine, code) },
                        onToggle = { enabled -> setShortcutEnabled(engine, enabled) }
                    )
                    if (index != SearchEngine.values().lastIndex) {
                        SearchEngineDivider()
                    }
                }
            }
        }
    }
}

/**
 * Display component for shortcut code with edit dialog.
 */
@Composable
private fun ShortcutCodeDisplay(
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: ((String) -> Unit)?,
    onToggle: ((Boolean) -> Unit)?,
    engineName: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog && onCodeChange != null) {
        EditShortcutDialog(
            engineName = engineName,
            currentCode = shortcutCode,
            isEnabled = isEnabled,
            onSave = { code -> onCodeChange(code) },
            onToggle = onToggle,
            onDismiss = { showDialog = false }
        )
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isEnabled) {
            Text(
                text = stringResource(R.string.settings_shortcut_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shortcutCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDialog = true }
            )
        } else {
            Text(
                text = stringResource(R.string.settings_add_shortcut),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDialog = true }
            )
        }
    }
}

/**
 * Row component for displaying and editing a shortcut.
 */
@Composable
private fun ShortcutRow(
    engine: SearchEngine,
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val engineName = engine.getDisplayName()
    val drawableId = engine.getDrawableResId()
    
    if (showDialog) {
        EditShortcutDialog(
            engineName = engineName,
            currentCode = shortcutCode,
            isEnabled = isEnabled,
            onSave = { code -> onCodeChange(code) },
            onToggle = { enabled -> onToggle(enabled) },
            onDismiss = { showDialog = false }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SearchEngineSettingsSpacing.rowHorizontalPadding,
                vertical = SearchEngineSettingsSpacing.rowVerticalPadding
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isEnabled) {
                    Text(
                        text = shortcutCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showDialog = true }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_add_shortcut),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showDialog = true }
                    )
                }
            }
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Calculates the offset for an item relative to the dragged item.
 */
private fun calculateOffsetForRelativeItem(
    relativeIndex: Int,
    dragProgress: Float,
    itemHeight: Dp
): Dp {
    return when {
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
