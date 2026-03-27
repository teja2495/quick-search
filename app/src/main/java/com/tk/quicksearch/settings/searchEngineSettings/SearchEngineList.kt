package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import sh.calvin.reorderable.ReorderableColumn

/**
 * Card containing the list of search engines with drag-and-drop reordering.
 * Uses the Reorderable library for smooth, reliable drag-to-reorder functionality.
 */
@Composable
fun SearchEngineListCard(
    searchEngineOrder: List<SearchTarget>,
    disabledSearchEngines: Set<String>,
    disabledSearchEnginesExpanded: Boolean = true,
    onToggleDisabledSearchEnginesExpanded: (() -> Unit)? = null,
    onToggleSearchEngine: (SearchTarget, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchTarget>) -> Unit,
    shortcutCodes: Map<String, String>,
    setAliasCode: ((SearchTarget, String) -> Unit)?,
    shortcutEnabled: Map<String, Boolean>,
    setAliasEnabled: ((SearchTarget, Boolean) -> Unit)?,
    isSearchEngineCompactMode: Boolean,
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    showAddSearchEngineButton: Boolean = true,
    onAddCustomSearchEngine: ((String, String, String) -> Unit)? = null,
    onUpdateCustomSearchEngine: ((String, String, String, String?) -> Unit)? = null,
    onDeleteCustomSearchEngine: ((String) -> Unit)? = null,
) {
    var showAddSearchEngineDialog by remember { mutableStateOf(false) }
    var customEngineToEdit by remember { mutableStateOf<CustomSearchEngine?>(null) }
    var localDisabledSectionExpanded by remember { mutableStateOf(disabledSearchEnginesExpanded) }
    val isDisabledSectionExpanded =
        if (onToggleDisabledSearchEnginesExpanded != null) {
            disabledSearchEnginesExpanded
        } else {
            localDisabledSectionExpanded
        }

    if (showAddSearchEngineDialog && onAddCustomSearchEngine != null) {
        AddSearchEngineDialog(
            onSave = { name, normalizedTemplate, faviconBase64 ->
                onAddCustomSearchEngine(name, normalizedTemplate, faviconBase64)
                showAddSearchEngineDialog = false
            },
            onDismiss = { showAddSearchEngineDialog = false },
        )
    }

    if (customEngineToEdit != null &&
        onUpdateCustomSearchEngine != null &&
        onDeleteCustomSearchEngine != null
    ) {
        EditCustomSearchEngineDialog(
            customEngine = customEngineToEdit!!,
            existingShortcuts = shortcutCodes,
            currentShortcutCode = shortcutCodes["custom:${customEngineToEdit!!.id}"].orEmpty(),
            onSave = { name, normalizedTemplate, shortcutCode, iconBase64 ->
                val editingEngine = customEngineToEdit!!
                onUpdateCustomSearchEngine(editingEngine.id, name, normalizedTemplate, iconBase64)
                setAliasCode?.invoke(SearchTarget.Custom(editingEngine), shortcutCode)
                setAliasEnabled?.invoke(SearchTarget.Custom(editingEngine), true)
                customEngineToEdit = null
            },
            onDelete = {
                onDeleteCustomSearchEngine(customEngineToEdit!!.id)
                customEngineToEdit = null
            },
            onDismiss = { customEngineToEdit = null },
        )
    }

    Column {
        val enabledEngines =
            searchEngineOrder.filter { it.getId() !in disabledSearchEngines }
        val disabledEngines =
            searchEngineOrder.filter { it.getId() in disabledSearchEngines }

        if (enabledEngines.isNotEmpty()) {
            val view = LocalView.current

            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReorderableColumn(
                    list = enabledEngines,
                    onSettle = { fromIndex, toIndex ->
                        if (fromIndex != toIndex) {
                            val newOrder =
                                enabledEngines.toMutableList().apply {
                                    add(toIndex, removeAt(fromIndex))
                                }
                            onReorderSearchEngines(newOrder + disabledEngines)
                        }
                    },
                    onMove = {
                        performHapticFeedbackSafely(
                            view,
                            HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { index, engine, isDragging ->
                    key(engine.getId()) {
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 4.dp else 0.dp,
                            label = "elevation",
                        )

                        Surface(
                            shadowElevation = elevation,
                            color = if (isDragging) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                val engineInfo = (engine as? SearchTarget.Engine)?.engine
                                val targetId = engine.getId()
                                val shortcutCode = shortcutCodes[targetId].orEmpty()
                                val isShortcutEnabled =
                                    shortcutEnabled[targetId] ?: shortcutCode.isNotEmpty()
                                SearchEngineRowContent(
                                    engine = engine,
                                    isEnabled = true,
                                    onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                                    dragHandleModifier =
                                        Modifier.longPressDraggableHandle(
                                            onDragStarted = {
                                                performHapticFeedbackSafely(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_START,
                                                )
                                            },
                                            onDragStopped = {
                                                performHapticFeedbackSafely(
                                                    view,
                                                    HapticFeedbackConstantsCompat.GESTURE_END,
                                                )
                                            },
                                        ),
                                    allowDrag = true,
                                    shortcutCode = shortcutCode,
                                    shortcutEnabled = isShortcutEnabled,
                                    onShortcutCodeChange =
                                        setAliasCode?.let { { code -> it(engine, code) } },
                                    onShortcutToggle =
                                        engineInfo?.let {
                                            setAliasEnabled?.let { { enabled -> it(engine, enabled) } }
                                        },
                                    showToggle = true,
                                    switchEnabled = true,
                                    amazonDomain =
                                        if (engineInfo == SearchEngine.AMAZON) {
                                            amazonDomain
                                        } else {
                                            null
                                        },
                                    onSetAmazonDomain =
                                        if (engineInfo == SearchEngine.AMAZON) {
                                            onSetAmazonDomain
                                        } else {
                                            null
                                        },
                                    onMoveToTop =
                                        if (index > 0) {
                                            {
                                                val newOrder =
                                                    enabledEngines.toMutableList().apply {
                                                        add(0, removeAt(index))
                                                    }
                                                onReorderSearchEngines(newOrder + disabledEngines)
                                            }
                                        } else {
                                            null
                                        },
                                    onMoveToBottom =
                                        if (index < enabledEngines.lastIndex) {
                                            {
                                                val newOrder =
                                                    enabledEngines.toMutableList().apply {
                                                        add(enabledEngines.lastIndex, removeAt(index))
                                                    }
                                                onReorderSearchEngines(newOrder + disabledEngines)
                                            }
                                        } else {
                                            null
                                        },
                                    onCustomEngineClick =
                                        if (engine is SearchTarget.Custom) {
                                            { customEngineToEdit = engine.custom }
                                        } else {
                                            null
                                        },
                                    existingShortcuts = shortcutCodes,
                                    isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
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
            val disabledHeaderModifier =
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (onToggleDisabledSearchEnginesExpanded != null) {
                        onToggleDisabledSearchEnginesExpanded()
                    } else {
                        localDisabledSectionExpanded = !localDisabledSectionExpanded
                    }
                }
            val collapsedHeaderTextRes =
                if (isDisabledSectionExpanded) {
                    R.string.settings_search_engines_more_title
                } else {
                    R.string.settings_search_engines_more_search_engines
                }

            if (isDisabledSectionExpanded) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = DesignTokens.CardHorizontalPadding,
                                top = if (enabledEngines.isNotEmpty()) 24.dp else 0.dp,
                                end = DesignTokens.CardHorizontalPadding,
                                bottom = 8.dp,
                            ).then(disabledHeaderModifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(collapsedHeaderTextRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandLess,
                        contentDescription = stringResource(R.string.desc_collapse),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                SettingsCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = if (enabledEngines.isNotEmpty()) 24.dp else 0.dp, bottom = 8.dp)
                            .then(disabledHeaderModifier),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = DesignTokens.CardHorizontalPadding,
                                    vertical = 16.dp,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(collapsedHeaderTextRes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = stringResource(R.string.desc_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isDisabledSectionExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        disabledEngines.forEachIndexed { index, engine ->
                            val engineInfo = (engine as? SearchTarget.Engine)?.engine
                            val targetId = engine.getId()
                            val shortcutCode = shortcutCodes[targetId].orEmpty()
                            val isShortcutEnabled =
                                shortcutEnabled[targetId] ?: shortcutCode.isNotEmpty()
                            SearchEngineRowContent(
                                engine = engine,
                                isEnabled = false,
                                onToggle = { enabled -> onToggleSearchEngine(engine, enabled) },
                                dragHandleModifier = null,
                                allowDrag = false,
                                shortcutCode = shortcutCode,
                                shortcutEnabled = isShortcutEnabled,
                                onShortcutCodeChange =
                                    setAliasCode?.let { { code -> it(engine, code) } },
                                onShortcutToggle =
                                    engineInfo?.let {
                                        setAliasEnabled?.let { { enabled -> it(engine, enabled) } }
                                    },
                                showToggle = true,
                                switchEnabled = true,
                                amazonDomain =
                                    if (engineInfo == SearchEngine.AMAZON) {
                                        amazonDomain
                                    } else {
                                        null
                                    },
                                onSetAmazonDomain =
                                    if (engineInfo == SearchEngine.AMAZON) {
                                        onSetAmazonDomain
                                    } else {
                                        null
                                    },
                                onMoveToTop = null,
                                onMoveToBottom = null,
                                onCustomEngineClick =
                                    if (engine is SearchTarget.Custom) {
                                        { customEngineToEdit = engine.custom }
                                    } else {
                                        null
                                    },
                                existingShortcuts = shortcutCodes,
                                isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
                            )

                            if (index != disabledEngines.lastIndex) {
                                SearchEngineDivider()
                            }
                        }
                    }
                }
            }
        }

        if (showAddSearchEngineButton && onAddCustomSearchEngine != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = { showAddSearchEngineDialog = true }) {
                    Text(text = stringResource(R.string.settings_add_search_engine_button))
                }
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
    engine: SearchTarget,
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
    onMoveToBottom: (() -> Unit)? = null,
    onCustomEngineClick: (() -> Unit)? = null,
    existingShortcuts: Map<String, String> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
) {
    val view = LocalView.current
    val engineInfo = (engine as? SearchTarget.Engine)?.engine
    val engineName = engine.getDisplayName()
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onCustomEngineClick != null) {
                        Modifier.clickable(onClick = onCustomEngineClick)
                    } else {
                        Modifier
                    },
                )
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        // Drag handle - long press to drag
        if (showToggle && allowDrag && dragHandleModifier != null) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.settings_action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .size(DesignTokens.IconSize)
                        .then(dragHandleModifier),
            )
        }

        SearchTargetIcon(
            target = engine,
            iconSize = DesignTokens.IconSize,
            style = IconRenderStyle.ADVANCED,
        )

        // Engine name and details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
        ) {
            Text(
                text = engineName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (onShortcutCodeChange != null) {
                val isSearchEngineAlias =
                    engine is SearchTarget.Engine || engine is SearchTarget.Browser
                AliasCodeDisplay(
                    shortcutCode = shortcutCode,
                    isEnabled = shortcutEnabled,
                    onCodeChange = onShortcutCodeChange,
                    engineName = engineName,
                    existingShortcuts = existingShortcuts,
                    currentShortcutId = engine.getId(),
                    validateCode = { input -> isValidGeneralAliasCode(input) },
                    validateConflict =
                        if (isSearchEngineAlias) {
                            { input, existing -> !hasExactAliasConflict(input, existing) }
                        } else {
                            { input, existing ->
                                !hasExactAliasConflict(input, existing)
                            }
                        },
                    isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
                )
            }
            if (engineInfo == SearchEngine.AMAZON && onSetAmazonDomain != null) {
                AmazonDomainLink(
                    amazonDomain = amazonDomain,
                    onSetAmazonDomain = onSetAmazonDomain,
                )
            }
        }

        // Context menu for move actions
        if (onMoveToTop != null || onMoveToBottom != null) {
            Box {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = MaterialTheme.shapes.large,
                    containerColor = AppColors.DialogBackground,
                ) {
                    if (onMoveToTop != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_action_move_up)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onMoveToTop.invoke()
                                showMenu = false
                            },
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
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onMoveToBottom.invoke()
                                showMenu = false
                            },
                        )
                    }
                }
            }
        }

        // Toggle switch
        if (showToggle) {
            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(enabled)
                },
                enabled = switchEnabled,
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = Color.Transparent,
                ),
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
    onSetAmazonDomain: (String?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        EditAmazonDomainDialog(
            currentDomain = amazonDomain,
            onSave = onSetAmazonDomain,
            onDismiss = { showDialog = false },
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_amazon_domain_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amazonDomain ?: stringResource(R.string.settings_amazon_domain_default),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.LinkColor,
            modifier = Modifier.clickable { showDialog = true },
        )
    }
}
