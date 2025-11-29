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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.focus.onFocusChanged
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
    shortcutsEnabled: Boolean = false,
    onToggleShortcutsEnabled: ((Boolean) -> Unit)? = null,
    shortcutCodes: Map<SearchEngine, String> = emptyMap(),
    setShortcutCode: ((SearchEngine, String) -> Unit)? = null,
    shortcutEnabled: Map<SearchEngine, Boolean> = emptyMap(),
    setShortcutEnabled: ((SearchEngine, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_search_engines_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Enable/disable shortcuts toggle (if shortcuts functionality is provided)
            if (onToggleShortcutsEnabled != null) {
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
                }
            }
            
            val draggedIndex = remember { mutableIntStateOf(-1) }
            val dragOffset = remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val itemHeight = 60.dp // Approximate row height with padding
            
            searchEngineOrder.forEachIndexed { index, engine ->
                val isDragging = draggedIndex.intValue == index
                
                // Calculate the target offset for this item - only adjacent items move
                val targetOffset = remember(draggedIndex.intValue, dragOffset.floatValue, index) {
                    if (draggedIndex.intValue < 0) {
                        0.dp
                    } else {
                        val draggedIdx = draggedIndex.intValue
                        val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                        
                        when {
                            isDragging -> {
                                // Dragged item follows the drag directly
                                offsetInDp
                            }
                            index == draggedIdx + 1 -> {
                                // Item is directly below the dragged item
                                val dragProgress = offsetInDp.value / itemHeight.value
                                when {
                                    // Dragged item has moved past this item
                                    dragProgress > 1f -> -itemHeight
                                    // Dragged item is approaching this item - interpolate
                                    dragProgress > 0.5f -> {
                                        val progress = ((dragProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                                        -itemHeight * progress
                                    }
                                    else -> 0.dp
                                }
                            }
                            index == draggedIdx - 1 -> {
                                // Item is directly above the dragged item
                                val dragProgress = offsetInDp.value / itemHeight.value
                                when {
                                    // Dragged item has moved past this item
                                    dragProgress < -1f -> itemHeight
                                    // Dragged item is approaching this item - interpolate
                                    dragProgress < -0.5f -> {
                                        val progress = ((-dragProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                                        itemHeight * progress
                                    }
                                    else -> 0.dp
                                }
                            }
                            else -> {
                                // Not an adjacent item, don't move
                                0.dp
                            }
                        }
                    }
                }
                
                // Animate the offset smoothly
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 300f
                    ),
                    label = "rowOffset"
                )
                
                SearchEngineRow(
                    engine = engine,
                    isEnabled = engine !in disabledSearchEngines,
                    onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                    onDragStart = {
                        draggedIndex.intValue = index
                        dragOffset.floatValue = 0f
                    },
                    onDrag = { change, dragAmount ->
                        dragOffset.floatValue += dragAmount.y
                        change.consume()
                    },
                    onDragEnd = {
                        val newOrder = searchEngineOrder.toMutableList()
                        val currentIndex = draggedIndex.intValue
                        if (currentIndex >= 0) {
                            val offsetInDp = with(density) { dragOffset.floatValue.toDp() }
                            val threshold = itemHeight / 2
                            val newIndex = when {
                                offsetInDp > threshold -> (currentIndex + 1).coerceAtMost(searchEngineOrder.lastIndex)
                                offsetInDp < -threshold -> (currentIndex - 1).coerceAtLeast(0)
                                else -> currentIndex
                            }
                            
                            if (newIndex != currentIndex) {
                                val item = newOrder.removeAt(currentIndex)
                                newOrder.add(newIndex, item)
                                onReorderSearchEngines(newOrder)
                            }
                        }
                        draggedIndex.intValue = -1
                        dragOffset.floatValue = 0f
                    },
                    isDragging = isDragging,
                    dragOffset = animatedOffset,
                    shortcutsEnabled = shortcutsEnabled && onToggleShortcutsEnabled != null,
                    shortcutCode = shortcutCodes[engine] ?: "",
                    shortcutEnabled = shortcutEnabled[engine] ?: true,
                    onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                    onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } }
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
    onShortcutToggle: ((Boolean) -> Unit)? = null
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
            if (shortcutsEnabled && shortcutCode.isNotEmpty() && shortcutEnabled) {
                ShortcutCodeDisplay(
                    shortcutCode = shortcutCode,
                    isEnabled = shortcutEnabled,
                    onCodeChange = onShortcutCodeChange,
                    onToggle = onShortcutToggle
                )
            }
        }
        
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
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
private fun ShortcutCodeDisplay(
    shortcutCode: String,
    isEnabled: Boolean,
    onCodeChange: ((String) -> Unit)?,
    onToggle: ((Boolean) -> Unit)?
) {
    var isEditing by remember { mutableStateOf(false) }
    var editingCode by remember(shortcutCode) { mutableStateOf(shortcutCode) }
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Update editingCode when shortcutCode changes externally (when not editing)
    LaunchedEffect(shortcutCode) {
        if (!isEditing) {
            editingCode = shortcutCode
        }
    }
    
    // Auto-focus when editing starts
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
    
    // Save when focus is lost while editing
    LaunchedEffect(hasFocus, isEditing) {
        if (isEditing && !hasFocus) {
            // Focus was lost, save the value
            if (editingCode.isNotBlank()) {
                onCodeChange?.invoke(editingCode)
            } else {
                editingCode = shortcutCode
            }
            isEditing = false
        }
    }
    
    if (isEditing) {
        TextField(
            value = editingCode,
            onValueChange = { editingCode = it.lowercase().filter { char -> char.isLetterOrDigit() }.take(5) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    hasFocus = focusState.isFocused
                },
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (editingCode.isNotBlank()) {
                        onCodeChange?.invoke(editingCode)
                    } else {
                        editingCode = shortcutCode
                    }
                    isEditing = false
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (editingCode.isNotBlank()) {
                            onCodeChange?.invoke(editingCode)
                        }
                        isEditing = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.settings_action_edit_shortcut),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_shortcut_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = shortcutCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { isEditing = true }
            )
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.settings_action_edit_shortcut),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { isEditing = true }
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
    var isEditing by remember { mutableStateOf(false) }
    var editingCode by remember(shortcutCode) { mutableStateOf(shortcutCode) }
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Update editingCode when shortcutCode changes externally (when not editing)
    LaunchedEffect(shortcutCode) {
        if (!isEditing) {
            editingCode = shortcutCode
        }
    }
    
    // Auto-focus when editing starts
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }
    
    // Save when focus is lost while editing
    LaunchedEffect(hasFocus, isEditing) {
        if (isEditing && !hasFocus) {
            // Focus was lost, save the value
            if (editingCode.isNotBlank()) {
                onCodeChange(editingCode)
            } else {
                editingCode = shortcutCode
            }
            isEditing = false
        }
    }
    
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
            if (isEditing) {
                TextField(
                    value = editingCode,
                    onValueChange = { editingCode = it.lowercase().filter { char -> char.isLetterOrDigit() }.take(5) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            hasFocus = focusState.isFocused
                        },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editingCode.isNotBlank()) {
                                onCodeChange(editingCode)
                            } else {
                                editingCode = shortcutCode
                            }
                            isEditing = false
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (editingCode.isNotBlank()) {
                                    onCodeChange(editingCode)
                                }
                                isEditing = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.settings_action_edit_shortcut),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = shortcutCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { isEditing = true }
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.settings_action_edit_shortcut),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { isEditing = true }
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

