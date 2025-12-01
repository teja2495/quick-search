package com.tk.quicksearch.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.tk.quicksearch.R
import com.tk.quicksearch.search.SearchEngine

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
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_search_engines_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_shortcuts_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    
    // Combined toggle card for enabling/disabling search engine section and shortcuts
    if (onToggleSearchEngineSectionEnabled != null && onToggleShortcutsEnabled != null) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                // Show Search Engines toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_search_engines_show_toggle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = searchEngineSectionEnabled,
                        onCheckedChange = onToggleSearchEngineSectionEnabled
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Shortcuts toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_shortcuts_toggle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = shortcutsEnabled,
                        onCheckedChange = onToggleShortcutsEnabled
                    )
                }
            }
        }
    }
    
    // Hide search engine list when both toggles are disabled
    val bothDisabled = !searchEngineSectionEnabled && !shortcutsEnabled
    if (!bothDisabled) {
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
            LaunchedEffect(searchEngineOrder) {
                if (pendingReorder.value) {
                    // Order has updated, clear the flag
                    pendingReorder.value = false
                }
            }
            
            searchEngineOrder.forEachIndexed { index, engine ->
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
                                        // Threshold is when we've moved halfway past the previous item
                                        // For item at draggedIdx + 1, threshold is 0.5
                                        // For item at draggedIdx + 2, threshold is 1.5, etc.
                                        val threshold = relativeIndex - 0.5f
                                        when {
                                            // We've fully passed this item (moved more than one item height past it)
                                            dragProgress >= relativeIndex -> -itemHeight
                                            // We're crossing the threshold for this item
                                            dragProgress > threshold -> {
                                                val progress = ((dragProgress - threshold) * 2f).coerceIn(0f, 1f)
                                                -itemHeight * progress
                                            }
                                            else -> 0.dp
                                        }
                                    }
                                    // Item is above the dragged item (dragging up)
                                    relativeIndex < 0 -> {
                                        // Threshold is when we've moved halfway past the previous item
                                        // For item at draggedIdx - 1, threshold is -0.5
                                        // For item at draggedIdx - 2, threshold is -1.5, etc.
                                        val threshold = relativeIndex + 0.5f
                                        when {
                                            // We've fully passed this item (moved more than one item height past it)
                                            dragProgress <= relativeIndex -> itemHeight
                                            // We're crossing the threshold for this item
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
                            val newOrder = searchEngineOrder.toMutableList()
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
                                val newIndex = (currentIndex + positionsMoved).coerceIn(0, searchEngineOrder.lastIndex)
                                
                                if (newIndex != currentIndex) {
                                    val item = newOrder.removeAt(currentIndex)
                                    newOrder.add(newIndex, item)
                                    // Immediately reset drag state - items will snap to 0 offset
                                    // The list reorder will put them in their new positions
                                    draggedIndex.intValue = -1
                                    dragOffset.floatValue = 0f
                                    pendingReorder.value = true
                                    onReorderSearchEngines(newOrder)
                                } else {
                                    // No reorder needed, reset immediately
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
                    shortcutsEnabled = true,
                    shortcutCode = shortcutCodes[engine] ?: "",
                    shortcutEnabled = shortcutEnabled[engine] ?: true,
                    onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                    onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                    showToggle = searchEngineSectionEnabled
                )
                if (index != searchEngineOrder.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
    }
}

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
    showToggle: Boolean = true
) {
    val engineName = when (engine) {
        SearchEngine.GOOGLE -> stringResource(R.string.search_engine_google)
        SearchEngine.CHATGPT -> stringResource(R.string.search_engine_chatgpt)
        SearchEngine.PERPLEXITY -> stringResource(R.string.search_engine_perplexity)
        SearchEngine.GROK -> stringResource(R.string.search_engine_grok)
        SearchEngine.GOOGLE_MAPS -> stringResource(R.string.search_engine_google_maps)
        SearchEngine.GOOGLE_PLAY -> stringResource(R.string.search_engine_google_play)
        SearchEngine.REDDIT -> stringResource(R.string.search_engine_reddit)
        SearchEngine.YOUTUBE -> stringResource(R.string.search_engine_youtube)
        SearchEngine.AMAZON -> stringResource(R.string.search_engine_amazon)
        SearchEngine.AI_MODE -> stringResource(R.string.search_engine_ai_mode)
    }
    
    val drawableId = when (engine) {
        SearchEngine.GOOGLE -> R.drawable.google
        SearchEngine.CHATGPT -> R.drawable.chatgpt
        SearchEngine.PERPLEXITY -> R.drawable.perplexity
        SearchEngine.GROK -> R.drawable.grok
        SearchEngine.GOOGLE_MAPS -> R.drawable.google_maps
        SearchEngine.GOOGLE_PLAY -> R.drawable.google_play
        SearchEngine.REDDIT -> R.drawable.reddit
        SearchEngine.YOUTUBE -> R.drawable.youtube
        SearchEngine.AMAZON -> R.drawable.amazon
        SearchEngine.AI_MODE -> R.drawable.ai_mode
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = dragOffset)
            .alpha(if (isDragging) 0.8f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
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
        }
        
        if (showToggle) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

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
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_shortcuts_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Enable/disable shortcuts toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_shortcuts_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_shortcuts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = shortcutsEnabled,
                    onCheckedChange = onToggleShortcutsEnabled
                )
            }
            
            if (shortcutsEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditShortcutDialog(
    engineName: String,
    currentCode: String,
    isEnabled: Boolean,
    onSave: (String) -> Unit,
    onToggle: ((Boolean) -> Unit)?,
    onDismiss: () -> Unit
) {
    var editingCode by remember(currentCode) { mutableStateOf(currentCode) }
    var enabledState by remember(isEnabled) { mutableStateOf(isEnabled) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        if (enabledState) {
            focusRequester.requestFocus()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_edit_shortcut_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_shortcut_message, engineName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextField(
                    value = editingCode,
                    onValueChange = { editingCode = it.lowercase().filter { char -> char.isLetterOrDigit() }.take(5) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = enabledState,
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editingCode.isNotBlank()) {
                                onSave(editingCode)
                            }
                            onDismiss()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                if (onToggle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = enabledState,
                            onCheckedChange = { 
                                enabledState = it
                                onToggle(it)
                            }
                        )
                        Text(
                            text = stringResource(R.string.dialog_enable_shortcut),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editingCode.isNotBlank()) {
                        onSave(editingCode)
                    }
                    onDismiss()
                },
                enabled = editingCode.isNotBlank()
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

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

@Composable
private fun ShortcutRow(
    engine: SearchEngine,
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val engineName = when (engine) {
        SearchEngine.GOOGLE -> stringResource(R.string.search_engine_google)
        SearchEngine.CHATGPT -> stringResource(R.string.search_engine_chatgpt)
        SearchEngine.PERPLEXITY -> stringResource(R.string.search_engine_perplexity)
        SearchEngine.GROK -> stringResource(R.string.search_engine_grok)
        SearchEngine.GOOGLE_MAPS -> stringResource(R.string.search_engine_google_maps)
        SearchEngine.GOOGLE_PLAY -> stringResource(R.string.search_engine_google_play)
        SearchEngine.REDDIT -> stringResource(R.string.search_engine_reddit)
        SearchEngine.YOUTUBE -> stringResource(R.string.search_engine_youtube)
        SearchEngine.AMAZON -> stringResource(R.string.search_engine_amazon)
        SearchEngine.AI_MODE -> stringResource(R.string.search_engine_ai_mode)
    }
    
    val drawableId = when (engine) {
        SearchEngine.GOOGLE -> R.drawable.google
        SearchEngine.CHATGPT -> R.drawable.chatgpt
        SearchEngine.PERPLEXITY -> R.drawable.perplexity
        SearchEngine.GROK -> R.drawable.grok
        SearchEngine.GOOGLE_MAPS -> R.drawable.google_maps
        SearchEngine.GOOGLE_PLAY -> R.drawable.google_play
        SearchEngine.REDDIT -> R.drawable.reddit
        SearchEngine.YOUTUBE -> R.drawable.youtube
        SearchEngine.AMAZON -> R.drawable.amazon
        SearchEngine.AI_MODE -> R.drawable.ai_mode
    }
    
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = engineName,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
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

