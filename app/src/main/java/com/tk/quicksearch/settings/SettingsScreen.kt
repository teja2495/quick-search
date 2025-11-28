package com.tk.quicksearch.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.SearchViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        modifier = modifier,
        onBack = onBack,
        hiddenApps = uiState.hiddenApps,
        onUnhideApp = viewModel::unhideApp,
        showAppLabels = uiState.showAppLabels,
        onToggleAppLabels = viewModel::setShowAppLabels,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        onToggleFileType = viewModel::setFileTypeEnabled,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        shortcutsEnabled = uiState.shortcutsEnabled,
        onToggleShortcutsEnabled = viewModel::setShortcutsEnabled,
        shortcutCodes = uiState.shortcutCodes,
        setShortcutCode = viewModel::setShortcutCode,
        shortcutEnabled = uiState.shortcutEnabled,
        setShortcutEnabled = viewModel::setShortcutEnabled
    )
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    hiddenApps: List<AppInfo>,
    onUnhideApp: (AppInfo) -> Unit,
    showAppLabels: Boolean,
    onToggleAppLabels: (Boolean) -> Unit,
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    shortcutsEnabled: Boolean,
    onToggleShortcutsEnabled: (Boolean) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: (SearchEngine, String) -> Unit,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: (SearchEngine, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.desc_navigate_back),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        AppLabelsSection(
            showAppLabels = showAppLabels,
            onToggleAppLabels = onToggleAppLabels
        )

        LayoutSection(
            keyboardAlignedLayout = keyboardAlignedLayout,
            onToggleKeyboardAlignedLayout = onToggleKeyboardAlignedLayout
        )

        SearchEnginesSection(
            searchEngineOrder = searchEngineOrder,
            disabledSearchEngines = disabledSearchEngines,
            onToggleSearchEngine = onToggleSearchEngine,
            onReorderSearchEngines = onReorderSearchEngines
        )

        ShortcutsSection(
            shortcutsEnabled = shortcutsEnabled,
            onToggleShortcutsEnabled = onToggleShortcutsEnabled,
            shortcutCodes = shortcutCodes,
            setShortcutCode = setShortcutCode,
            shortcutEnabled = shortcutEnabled,
            setShortcutEnabled = setShortcutEnabled
        )

        FileTypesSection(
            enabledFileTypes = enabledFileTypes,
            onToggleFileType = onToggleFileType
        )

        HiddenAppsSection(
            hiddenApps = hiddenApps,
            onUnhideApp = onUnhideApp
        )
    }
}

@Composable
private fun AppLabelsSection(
    showAppLabels: Boolean,
    onToggleAppLabels: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_app_labels_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_app_labels_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
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
                    text = stringResource(R.string.settings_app_labels_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        if (showAppLabels) {
                            R.string.settings_app_labels_toggle_on_desc
                        } else {
                            R.string.settings_app_labels_toggle_off_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showAppLabels,
                onCheckedChange = onToggleAppLabels
            )
        }
    }
}

@Composable
private fun LayoutSection(
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_layout_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_layout_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
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
                    text = stringResource(R.string.settings_layout_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        if (keyboardAlignedLayout) {
                            R.string.settings_layout_toggle_on_desc
                        } else {
                            R.string.settings_layout_toggle_off_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = keyboardAlignedLayout,
                onCheckedChange = onToggleKeyboardAlignedLayout
            )
        }
    }
}

@Composable
private fun SearchEnginesSection(
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_search_engines_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_search_engines_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            searchEngineOrder.forEachIndexed { index, engine ->
                SearchEngineRow(
                    engine = engine,
                    isEnabled = engine !in disabledSearchEngines,
                    canMoveUp = index > 0,
                    canMoveDown = index < searchEngineOrder.lastIndex,
                    onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                    onMoveUp = {
                        val newOrder = searchEngineOrder.toMutableList()
                        newOrder[index] = newOrder[index - 1].also { newOrder[index - 1] = newOrder[index] }
                        onReorderSearchEngines(newOrder)
                    },
                    onMoveDown = {
                        val newOrder = searchEngineOrder.toMutableList()
                        newOrder[index] = newOrder[index + 1].also { newOrder[index + 1] = newOrder[index] }
                        onReorderSearchEngines(newOrder)
                    }
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
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
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
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(R.string.settings_action_move_up),
                    tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(R.string.settings_action_move_down),
                    tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun ShortcutsSection(
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
                        text = stringResource(
                            if (shortcutsEnabled) {
                                R.string.settings_shortcuts_toggle_on_desc
                            } else {
                                R.string.settings_shortcuts_toggle_off_desc
                            }
                        ),
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
                    onValueChange = { editingCode = it.lowercase().filter { char -> char.isLetterOrDigit() } },
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

@Composable
private fun FileTypesSection(
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_file_types_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_file_types_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            FileType.values().forEachIndexed { index, fileType ->
                FileTypeRow(
                    fileType = fileType,
                    isEnabled = fileType in enabledFileTypes,
                    onToggle = { enabled -> onToggleFileType(fileType, enabled) }
                )
                if (index != FileType.values().lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTypeRow(
    fileType: FileType,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val fileTypeName = when (fileType) {
        FileType.PHOTOS_AND_VIDEOS -> stringResource(R.string.file_type_photos_and_videos)
        FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
        FileType.APK -> stringResource(R.string.file_type_apk)
        FileType.OTHER -> stringResource(R.string.file_type_other)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fileTypeName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun HiddenAppsSection(
    hiddenApps: List<AppInfo>,
    onUnhideApp: (AppInfo) -> Unit
) {
    Text(
        text = stringResource(R.string.settings_hidden_apps_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.settings_hidden_apps_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (hiddenApps.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_hidden_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            Column {
                hiddenApps.forEachIndexed { index, appInfo ->
                    HiddenAppRow(
                        appInfo = appInfo,
                        onUnhideApp = onUnhideApp
                    )
                    if (index != hiddenApps.lastIndex) {
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
private fun HiddenAppRow(
    appInfo: AppInfo,
    onUnhideApp: (AppInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { onUnhideApp(appInfo) }) {
            Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.settings_action_unhide_app),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

