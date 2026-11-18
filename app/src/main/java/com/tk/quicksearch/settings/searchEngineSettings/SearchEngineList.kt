package com.tk.quicksearch.settings.searchEngineSettings

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.components.AppSwitch
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.AddSearchEngineCard
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.EditAmazonDomainDialog
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.EditCustomSearchEngineDialog
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
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
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    showAddSearchEngineButton: Boolean = true,
    onAddCustomSearchEngine: ((String, String, String, String?) -> Unit)? = null,
    onUpdateCustomSearchEngine: ((String, String, String, String?, String?) -> Unit)? = null,
    onDeleteCustomSearchEngine: ((String) -> Unit)? = null,
) {
    val availableBrowsers = remember(searchEngineOrder) {
        searchEngineOrder.filterIsInstance<SearchTarget.Browser>().map { it.app }
    }
    var customEngineToEdit by remember { mutableStateOf<CustomSearchEngine?>(null) }
    var localDisabledSectionExpanded by remember { mutableStateOf(disabledSearchEnginesExpanded) }
    val isDisabledSectionExpanded =
        if (onToggleDisabledSearchEnginesExpanded != null) {
            disabledSearchEnginesExpanded
        } else {
            localDisabledSectionExpanded
        }

    if (customEngineToEdit != null &&
        onUpdateCustomSearchEngine != null &&
        onDeleteCustomSearchEngine != null
    ) {
        val editingEngine = customEngineToEdit!!
        EditCustomSearchEngineDialog(
            customEngine = editingEngine,
            existingShortcuts = shortcutCodes,
            currentShortcutCode = shortcutCodes["custom:${editingEngine.id}"].orEmpty(),
            availableBrowsers = availableBrowsers,
            onSave = { name, normalizedTemplate, shortcutCode, iconBase64, browserPackage ->
                onUpdateCustomSearchEngine(
                    editingEngine.id,
                    name,
                    normalizedTemplate,
                    iconBase64,
                    browserPackage,
                )
                setAliasCode?.invoke(SearchTarget.Custom(editingEngine), shortcutCode)
                customEngineToEdit = null
            },
            onDelete = {
                onDeleteCustomSearchEngine(editingEngine.id)
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
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
            ) { index, engine, isDragging ->
                key(engine.getId()) {
                    ReorderableItem {
                        // A dragged tile lifts out of the list: fully round, accent-tinted.
                        SegmentedTile(
                            index = if (isDragging) 0 else index,
                            count = if (isDragging) 1 else enabledEngines.size,
                            containerColor =
                                if (isDragging) {
                                    AppColors.AccentTintStrong
                                        .compositeOver(AppColors.getSettingsCardContainerColor())
                                } else {
                                    AppColors.getSettingsCardContainerColor()
                                },
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
                                    isDragging = isDragging,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (disabledEngines.isNotEmpty()) {
            val disabledHeaderModifier =
                Modifier.clip(AppShapes.small).clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ) {
                    if (onToggleDisabledSearchEnginesExpanded != null) {
                        onToggleDisabledSearchEnginesExpanded()
                    } else {
                        localDisabledSectionExpanded = !localDisabledSectionExpanded
                    }
                }
            val collapsedHeaderTextRes =
                if (isDisabledSectionExpanded) {
                    R.string.action_expand_more
                } else {
                    R.string.settings_search_engines_more_search_engines
                }

            if (isDisabledSectionExpanded) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = SegmentedTileContentPadding,
                                top = if (enabledEngines.isNotEmpty()) 24.dp else 0.dp,
                                end = SegmentedTileContentPadding,
                                bottom = DesignTokens.SpacingSmall,
                            ).then(disabledHeaderModifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Section header, as on other segmented settings pages.
                    Text(
                        text = stringResource(collapsedHeaderTextRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.Accent,
                        modifier = Modifier.semantics { heading() },
                    )
                    Icon(
                        imageVector = MaterialSymbols.ExpandLess,
                        contentDescription = stringResource(R.string.desc_collapse),
                        tint = AppColors.Accent,
                        modifier = Modifier.size(DesignTokens.IconSize),
                    )
                }
            } else {
                Box(
                    modifier =
                        Modifier.padding(top = if (enabledEngines.isNotEmpty()) DesignTokens.SpacingXXLarge else 0.dp, bottom = DesignTokens.SpacingSmall),
                ) {
                    SegmentedTile(
                        index = 0,
                        count = 1,
                        onClick = {
                            if (onToggleDisabledSearchEnginesExpanded != null) {
                                onToggleDisabledSearchEnginesExpanded()
                            } else {
                                localDisabledSectionExpanded = true
                            }
                        },
                    ) {
                        SegmentedTileRow(
                            title = stringResource(collapsedHeaderTextRes),
                            trailing = {
                                Icon(
                                    imageVector = MaterialSymbols.ExpandMore,
                                    contentDescription = stringResource(R.string.desc_expand),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(DesignTokens.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isDisabledSectionExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SegmentedGroup {
                    disabledEngines.forEachIndexed { index, engine ->
                        val engineInfo = (engine as? SearchTarget.Engine)?.engine
                        val targetId = engine.getId()
                        val shortcutCode = shortcutCodes[targetId].orEmpty()
                        val isShortcutEnabled =
                            shortcutEnabled[targetId] ?: shortcutCode.isNotEmpty()
                        SegmentedTile(index = index, count = disabledEngines.size) {
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
                        }
                    }
                }
            }
        }

        if (showAddSearchEngineButton && onAddCustomSearchEngine != null) {
            SegmentedTile(
                index = 0,
                count = 1,
                modifier = Modifier.padding(top = DesignTokens.SpacingXXLarge, bottom = DesignTokens.SpacingXXLarge),
            ) {
                AddSearchEngineCard(
                    availableBrowsers = availableBrowsers,
                    onSave = onAddCustomSearchEngine,
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
    engine: SearchTarget,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    dragHandleModifier: Modifier?,
    allowDrag: Boolean,
    shortcutCode: String = "",
    shortcutEnabled: Boolean = true,
    onShortcutCodeChange: ((String) -> Unit)? = null,
    showToggle: Boolean = true,
    switchEnabled: Boolean = true,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveToBottom: (() -> Unit)? = null,
    onCustomEngineClick: (() -> Unit)? = null,
    existingShortcuts: Map<String, String> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    isDragging: Boolean = false,
) {
    val view = LocalView.current
    val engineInfo = (engine as? SearchTarget.Engine)?.engine
    val engineName = engine.getDisplayName()
    var showMenu by remember { mutableStateOf(false) }
    val hasMoveMenu = onMoveToTop != null || onMoveToBottom != null
    val openMoveMenu: (() -> Unit)? = if (hasMoveMenu) ({ showMenu = true }) else null
    val moveUpLabel = stringResource(R.string.settings_action_move_up)
    val moveDownLabel = stringResource(R.string.settings_action_move_down)
    val rowClick = onCustomEngineClick ?: openMoveMenu
    val dragHandle = dragHandleModifier?.takeIf { showToggle && allowDrag }
    val rowSpacing = DesignTokens.SpacingSmall
    // The drag handle and switch occupy their own columns, so their 48dp targets need
    // not set the gap between the engine name and its actions.
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    // Tap edits a custom engine; otherwise it opens the move menu. Long-press always
                    // opens the move menu, so reordering works without the drag handle.
                    if (rowClick != null) {
                        Modifier.combinedClickable(
                            role = Role.Button,
                            onClick = rowClick,
                            onLongClick = if (onCustomEngineClick != null) openMoveMenu else null,
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics {
                    customActions =
                        listOfNotNull(
                            onMoveToTop?.let { move -> CustomAccessibilityAction(moveUpLabel) { move(); true } },
                            onMoveToBottom?.let { move -> CustomAccessibilityAction(moveDownLabel) { move(); true } },
                        )
                }
                .padding(
                    start = if (dragHandle == null) SegmentedTileContentPadding else DesignTokens.SpacingSmall,
                    end = SegmentedTileContentPadding,
                    top = DesignTokens.SpacingXSmall,
                    bottom = DesignTokens.SpacingXSmall,
                ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        if (dragHandle != null) {
            Box(
                modifier = Modifier.size(DesignTokens.MinTouchTarget).then(dragHandle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.DragHandle,
                    contentDescription = stringResource(R.string.settings_action_reorder),
                    tint = if (isDragging) AppColors.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = (DesignTokens.MinTouchTarget - DesignTokens.IconSize) / 2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            ) {
                Box(
                    modifier = Modifier.heightIn(min = DesignTokens.IconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    SearchTargetIcon(
                        target = engine,
                        iconSize = DesignTokens.IconSize,
                        style = IconRenderStyle.ADVANCED,
                    )
                }

                Text(
                    text = engineName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
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

        Box {
            if (hasMoveMenu) {
                AppDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    focusable = true,
                ) {
                    if (onMoveToTop != null) {
                        AppDropdownMenuItem(
                            text = stringResource(R.string.settings_action_move_up),
                            icon = MaterialSymbols.ArrowUpward,
                            onClick = {
                                onMoveToTop.invoke()
                                showMenu = false
                            },
                        )
                    }

                    if (onMoveToTop != null && onMoveToBottom != null) {
                        AppDropdownMenuDivider()
                    }

                    if (onMoveToBottom != null) {
                        AppDropdownMenuItem(
                            text = stringResource(R.string.settings_action_move_down),
                            icon = MaterialSymbols.ArrowDownward,
                            onClick = {
                                onMoveToBottom.invoke()
                                showMenu = false
                            },
                        )
                    }
                }
            }

            if (showToggle) {
                AppSwitch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        hapticToggle(view)()
                        onToggle(enabled)
                    },
                    enabled = switchEnabled,
                )
            }
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

    val label = stringResource(R.string.settings_amazon_domain_label)
    val domain = amazonDomain ?: stringResource(R.string.settings_amazon_domain_default)
    val domainColor = AppColors.LinkColor
    Box(
        modifier = Modifier
            .clickable(role = Role.Button) { showDialog = true }
            .heightIn(min = DesignTokens.MinTouchTarget),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = buildAnnotatedString {
                append(label)
                append(' ')
                withStyle(SpanStyle(color = domainColor)) { append(domain) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
