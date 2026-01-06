package com.tk.quicksearch.settings.searchEngines

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.searchengines.*
import com.tk.quicksearch.ui.theme.DesignTokens
import sh.calvin.reorderable.ReorderableColumn

/**
 * Card containing the list of search engines with drag-and-drop reordering.
 * Uses the Reorderable library for smooth, reliable drag-to-reorder functionality.
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
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    showRequestSearchEngine: Boolean = true
) {
    Column {
        val enabledEngines = searchEngineOrder.filter { it !in disabledSearchEngines }
        val disabledEngines = searchEngineOrder.filter { it in disabledSearchEngines }

        if (enabledEngines.isNotEmpty()) {
            val view = LocalView.current

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                ReorderableColumn(
                    list = enabledEngines,
                    onSettle = { fromIndex, toIndex ->
                        if (fromIndex != toIndex) {
                            val newOrder = enabledEngines.toMutableList().apply {
                                add(toIndex, removeAt(fromIndex))
                            }
                            onReorderSearchEngines(newOrder + disabledEngines)
                        }
                    },
                    onMove = {
                        ViewCompat.performHapticFeedback(
                            view,
                            HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { index, engine, isDragging ->
                    key(engine) {
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 4.dp else 0.dp,
                            label = "elevation"
                        )

                        Surface(
                            shadowElevation = elevation,
                            color = if (isDragging) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                SearchEngineRowContent(
                                    engine = engine,
                                    isEnabled = true,
                                    onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                                    dragHandleModifier = if (searchEngineSectionEnabled) {
                                        Modifier.longPressDraggableHandle(
                                            onDragStarted = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_START
                                                )
                                            },
                                            onDragStopped = {
                                                ViewCompat.performHapticFeedback(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_END
                                                )
                                            }
                                        )
                                    } else null,
                                    allowDrag = searchEngineSectionEnabled,
                                    shortcutCode = shortcutCodes[engine] ?: "",
                                    shortcutEnabled = shortcutEnabled[engine] ?: true,
                                    onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                                    onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                                    showToggle = searchEngineSectionEnabled,
                                    switchEnabled = searchEngineSectionEnabled,
                                    amazonDomain = if (engine == SearchEngine.AMAZON) amazonDomain else null,
                                    onSetAmazonDomain = if (engine == SearchEngine.AMAZON) onSetAmazonDomain else null,
                                    onMoveToTop = if (searchEngineSectionEnabled && index > 0) {
                                        {
                                            val newOrder = enabledEngines.toMutableList().apply {
                                                add(0, removeAt(index))
                                            }
                                            onReorderSearchEngines(newOrder + disabledEngines)
                                        }
                                    } else null,
                                    onMoveToBottom = if (searchEngineSectionEnabled && index < enabledEngines.lastIndex) {
                                        {
                                            val newOrder = enabledEngines.toMutableList().apply {
                                                add(enabledEngines.lastIndex, removeAt(index))
                                            }
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
            }
        }

        if (disabledEngines.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_engines_more_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = DesignTokens.CardHorizontalPadding,
                    top = 24.dp,
                    bottom = 8.dp
                )
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    disabledEngines.forEachIndexed { index, engine ->
                        SearchEngineRowContent(
                            engine = engine,
                            isEnabled = false,
                            onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                            dragHandleModifier = null,
                            allowDrag = false,
                            shortcutCode = shortcutCodes[engine] ?: "",
                            shortcutEnabled = shortcutEnabled[engine] ?: true,
                            onShortcutCodeChange = setShortcutCode?.let { { code -> it(engine, code) } },
                            onShortcutToggle = setShortcutEnabled?.let { { enabled -> it(engine, enabled) } },
                            showToggle = searchEngineSectionEnabled,
                            switchEnabled = searchEngineSectionEnabled,
                            amazonDomain = if (engine == SearchEngine.AMAZON) amazonDomain else null,
                            onSetAmazonDomain = if (engine == SearchEngine.AMAZON) onSetAmazonDomain else null,
                            onMoveToTop = null,
                            onMoveToBottom = null
                        )

                        if (index != disabledEngines.lastIndex) {
                            SearchEngineDivider()
                        }
                    }
                }
            }
        }

        if (showRequestSearchEngine) {
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.request_search_engine_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val subject = "Search Engine Request"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:tejakarlapudi.apps@gmail.com?subject=${Uri.encode(subject)}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual row for a search engine in the list.
 * Accepts an optional dragHandleModifier for reorderable rows.
 */
@Composable
private fun SearchEngineRowContent(
    engine: SearchEngine,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    dragHandleModifier: Modifier?,
    allowDrag: Boolean,
    shortcutCode: String = "",
    shortcutEnabled: Boolean = true,
    onShortcutCodeChange: ((String) -> Unit)? = null,
    onShortcutToggle: ((Boolean) -> Unit)? = null,
    showToggle: Boolean = true,
    switchEnabled: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null
) {
    val engineName = engine.getDisplayName()
    val drawableId = engine.getDrawableResId()
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DesignTokens.CardHorizontalPadding,
                vertical = DesignTokens.CardVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing)
    ) {
        // Drag handle - long press to drag
        if (showToggle && allowDrag && dragHandleModifier != null) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.settings_action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(DesignTokens.IconSize)
                    .then(dragHandleModifier)
            )
        }

        // Search engine icon
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = engineName,
            modifier = Modifier.size(DesignTokens.IconSize),
            contentScale = ContentScale.Fit,
            colorFilter = getSearchEngineIconColorFilter(engine)
        )

        // Engine name and details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)
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
            if (engine == SearchEngine.AMAZON && onSetAmazonDomain != null) {
                AmazonDomainLink(
                    amazonDomain = amazonDomain,
                    onSetAmazonDomain = onSetAmazonDomain
                )
            }
        }

        // Context menu for move actions
        if (onMoveToTop != null || onMoveToBottom != null) {
            Box {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = MaterialTheme.shapes.large
                ) {
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

                    if (onMoveToTop != null && onMoveToBottom != null) {
                        HorizontalDivider()
                    }

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
        }

        // Toggle switch
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
