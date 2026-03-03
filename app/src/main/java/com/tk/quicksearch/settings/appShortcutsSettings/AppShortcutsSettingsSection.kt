package com.tk.quicksearch.settings.AppShortcutsSettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.getDisplayNameResId
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.searchEngines.getDisplayName
import com.tk.quicksearch.searchEngines.isSearchTargetShortcutPackageName
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AppShortcutsSettingsSection(
    shortcuts: List<StaticShortcut>,
    disabledShortcutIds: Set<String>,
    iconPackPackage: String?,
    searchQuery: String = "",
    collapseAllTrigger: Int = 0,
    onShortcutEnabledChange: (StaticShortcut, Boolean) -> Unit,
    onShortcutNameClick: (StaticShortcut) -> Unit,
    shortcutSources: List<AppShortcutSource>,
    onAddShortcutFromSource: (AppShortcutSource) -> Unit,
    searchTargets: List<SearchTarget>,
    onAddQueryShortcut: (SearchTarget, String, String) -> Unit,
    onUpdateCustomShortcut: (StaticShortcut, String, String?) -> Unit,
    onDeleteCustomShortcut: (StaticShortcut) -> Unit,
    focusShortcut: StaticShortcut? = null,
    focusPackageName: String? = null,
    onFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val normalizedSearchQuery =
        remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val displayShortcuts =
        remember(shortcuts, focusShortcut) {
            val shortcutToFocus = focusShortcut ?: return@remember shortcuts
            val exists = shortcuts.any { shortcutKey(it) == shortcutKey(shortcutToFocus) }
            if (exists) shortcuts else shortcuts + shortcutToFocus
        }
    val filteredShortcutSources =
        remember(shortcutSources, displayShortcuts, context.packageName) {
            filterAppShortcutSources(
                sources = shortcutSources,
                existingShortcuts = displayShortcuts,
                currentPackageName = context.packageName,
            )
        }
    val searchTargetShortcutSources by remember(searchTargets, displayShortcuts, filteredShortcutSources) {
        derivedStateOf {
            val existingPackageNames =
                (
                    displayShortcuts.map { it.packageName } +
                        filteredShortcutSources.map { it.packageName }
                ).toSet()
            searchTargets
                .flatMap { target ->
                    val baseSource =
                        SearchTargetShortcutSource(
                            target = target,
                            packageName =
                                com.tk.quicksearch.searchEngines.resolveSearchTargetShortcutPackageName(
                                    target = target,
                                    existingPackages = existingPackageNames,
                                ),
                            label = when (target) {
                                is SearchTarget.Engine -> context.getString(target.engine.getDisplayNameResId())
                                is SearchTarget.Browser -> target.app.label
                                is SearchTarget.Custom -> target.custom.name
                            },
                            kind = SearchTargetShortcutKind.QUERY,
                        )
                    if (target is SearchTarget.Browser) {
                        listOf(
                            baseSource,
                            baseSource.copy(kind = SearchTargetShortcutKind.URL),
                        )
                    } else {
                        listOf(baseSource)
                    }
                }
        }
    }
    val allPackageNames =
        remember(displayShortcuts, filteredShortcutSources, searchTargetShortcutSources) {
            (
                displayShortcuts.map { it.packageName } +
                    filteredShortcutSources.map { it.packageName } +
                    searchTargetShortcutSources.map { it.packageName }
            ).toSet()
        }
    val appLabelCache = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(allPackageNames, locale) {
        val unresolvedPackages = allPackageNames.filter { it !in appLabelCache }
        if (unresolvedPackages.isEmpty()) return@LaunchedEffect

        val resolvedLabels =
            withContext(Dispatchers.Default) {
                unresolvedPackages.associateWith { packageName ->
                    resolveAppLabel(context, packageName, locale)
                }
            }
        appLabelCache.putAll(resolvedLabels)
    }
    val allShortcutGroups by
        produceState(
            initialValue = emptyList<AppShortcutGroup>(),
            displayShortcuts,
            filteredShortcutSources,
            searchTargetShortcutSources,
            locale,
            appLabelCache.toMap(),
        ) {
            val labelCacheSnapshot = appLabelCache.toMap()
            value =
                withContext(Dispatchers.Default) {
                    val shortcutsByPackage = displayShortcuts.groupBy { it.packageName }
                    val sourcesByPackage = filteredShortcutSources.groupBy { it.packageName }
                    val searchTargetSourcesByPackage = searchTargetShortcutSources.groupBy { it.packageName }
                    (shortcutsByPackage.keys + sourcesByPackage.keys + searchTargetSourcesByPackage.keys)
                        .mapNotNull { packageName ->
                            val appShortcuts = shortcutsByPackage[packageName].orEmpty()
                            val appSources = sourcesByPackage[packageName].orEmpty()
                            val appSearchTargetSources =
                                searchTargetSourcesByPackage[packageName].orEmpty()
                            if (appShortcuts.isEmpty() &&
                                appSources.isEmpty() &&
                                appSearchTargetSources.isEmpty()
                            ) {
                                null
                            } else {
                                val appLabel =
                                    appShortcuts.firstOrNull()?.appLabel?.takeIf { it.isNotBlank() }
                                        ?: appSources.firstOrNull()?.appLabel?.takeIf { it.isNotBlank() }
                                        ?: appSearchTargetSources.firstOrNull()?.label?.takeIf { it.isNotBlank() }
                                        ?: labelCacheSnapshot[packageName]
                                        ?: fallbackAppLabel(packageName, locale)
                                AppShortcutGroup(
                                    packageName = packageName,
                                    appLabel = appLabel,
                                    shortcuts =
                                        appShortcuts.sortedWith(
                                            compareBy(
                                                { isUserCreatedShortcut(it) },
                                                { shortcutDisplayName(it).lowercase(locale) },
                                            ),
                                        ),
                                    sources = appSources,
                                    searchTargetSources = appSearchTargetSources,
                                )
                            }
                        }.sortedBy { it.appLabel.lowercase(locale) }
                }
        }
    val shortcutGroups by
        produceState(
            initialValue = emptyList<AppShortcutGroup>(),
            allShortcutGroups,
            normalizedSearchQuery,
            locale,
        ) {
            value =
                withContext(Dispatchers.Default) {
                    if (normalizedSearchQuery.isBlank()) {
                        allShortcutGroups
                    } else {
                        allShortcutGroups
                            .mapNotNull { group ->
                                val appMatchPriority =
                                    shortcutMatchPriority(group.appLabel, normalizedSearchQuery, locale)
                                val matchingShortcuts =
                                    group.shortcuts.filter { shortcut ->
                                        shortcutMatchPriority(
                                            shortcutDisplayName(shortcut),
                                            normalizedSearchQuery,
                                            locale,
                                        ) != null
                                    }
                                val matchingSources =
                                    group.sources.filter { source ->
                                        shortcutMatchPriority(source.label, normalizedSearchQuery, locale) != null
                                    }
                                val matchingSearchTargetSources =
                                    group.searchTargetSources.filter { source ->
                                        shortcutMatchPriority(source.label, normalizedSearchQuery, locale) != null
                                    }
                                val filteredGroup =
                                    when {
                                        appMatchPriority != null -> group
                                        matchingShortcuts.isNotEmpty() ||
                                            matchingSources.isNotEmpty() ||
                                            matchingSearchTargetSources.isNotEmpty() ->
                                            group.copy(
                                                shortcuts = matchingShortcuts,
                                                sources = matchingSources,
                                                searchTargetSources = matchingSearchTargetSources,
                                            )
                                        else -> null
                                    } ?: return@mapNotNull null

                                val bestPriority =
                                    bestShortcutMatchPriority(filteredGroup, normalizedSearchQuery, locale)
                                        ?: return@mapNotNull null
                                filteredGroup to bestPriority
                            }.sortedWith(compareBy<Pair<AppShortcutGroup, ShortcutSearchMatchPriority>> { it.second.ordinal }
                                .thenBy { it.first.appLabel.lowercase(locale) })
                            .map { it.first }
                    }
                }
        }
    var selectedFilterOption by remember { mutableStateOf(ShortcutFilterOption.ALL) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }
    var shortcutToEdit by remember { mutableStateOf<StaticShortcut?>(null) }
    val visibleShortcutGroups =
        remember(shortcutGroups, selectedFilterOption) {
            when (selectedFilterOption) {
                ShortcutFilterOption.ALL -> shortcutGroups
                ShortcutFilterOption.APPS_WITH_SHORTCUTS -> shortcutGroups.filter { it.shortcuts.isNotEmpty() }
                ShortcutFilterOption.SEARCH_ENGINES ->
                    shortcutGroups.filter { it.searchTargetSources.isNotEmpty() }
                ShortcutFilterOption.BROWSERS ->
                    shortcutGroups.filter { group ->
                        group.searchTargetSources.any { it.target is SearchTarget.Browser }
                    }
            }
        }
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }
    var shortcutDialogSource by remember { mutableStateOf<SearchTargetShortcutSource?>(null) }

    LaunchedEffect(visibleShortcutGroups) {
        val currentPackages = visibleShortcutGroups.map { it.packageName }.toSet()
        val existingPackages = expandedCards.keys.toSet()

        visibleShortcutGroups.forEach { group ->
            if (group.packageName !in expandedCards) {
                expandedCards[group.packageName] = false
            }
        }

        (existingPackages - currentPackages).forEach { removedPackage ->
            expandedCards.remove(removedPackage)
        }
    }
    LaunchedEffect(normalizedSearchQuery, visibleShortcutGroups) {
        if (normalizedSearchQuery.isNotBlank()) {
            visibleShortcutGroups.forEachIndexed { index, group ->
                expandedCards[group.packageName] = index == 0
            }
        }
    }
    LaunchedEffect(collapseAllTrigger) {
        if (collapseAllTrigger > 0) {
            expandedCards.keys.forEach { packageName ->
                expandedCards[packageName] = false
            }
        }
    }

    if (visibleShortcutGroups.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_app_shortcuts_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = DesignTokens.SpacingLarge),
        )
        return
    }

    shortcutDialogSource?.let { source ->
        AddSearchTargetShortcutDialog(
            targetLabel = source.label,
            shortcutKind = source.kind,
            onDismiss = { shortcutDialogSource = null },
            onSave = { shortcutName, shortcutValue ->
                onAddQueryShortcut(source.target, shortcutName, shortcutValue)
                shortcutDialogSource = null
            },
        )
    }
    shortcutToEdit?.let { shortcut ->
        EditCustomShortcutDialog(
            shortcut = shortcut,
            iconPackPackage = iconPackPackage,
            onDismiss = { shortcutToEdit = null },
            onSave = { updatedName, updatedIconBase64 ->
                onUpdateCustomShortcut(shortcut, updatedName, updatedIconBase64)
                shortcutToEdit = null
            },
            onDelete = {
                onDeleteCustomShortcut(shortcut)
                shortcutToEdit = null
            },
        )
    }

    val allExpanded =
        visibleShortcutGroups.isNotEmpty() &&
            visibleShortcutGroups.all { expandedCards[it.packageName] == true }

    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
            if (normalizedSearchQuery.isBlank()) {
                item {
                    val shortcutCount = shortcuts.size.toString()
                    val descriptionText =
                        stringResource(R.string.settings_app_shortcuts_description_with_count, shortcutCount)
                    Text(
                        text =
                            buildAnnotatedString {
                                append(descriptionText)
                                val countStart = descriptionText.indexOf(shortcutCount)
                                if (countStart >= 0) {
                                    addStyle(
                                        style = SpanStyle(fontWeight = FontWeight.Bold),
                                        start = countStart,
                                        end = countStart + shortcutCount.length,
                                    )
                                }
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingXSmall),
                    )
                }
            }

            if (normalizedSearchQuery.isBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Box {
                            Row(
                                modifier =
                                    Modifier
                                        .clickable { isFilterMenuExpanded = true }
                                        .padding(
                                            start = DesignTokens.SpacingXSmall,
                                            end = DesignTokens.SpacingXSmall,
                                        ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(selectedFilterOption.labelResId),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            DropdownMenu(
                                expanded = isFilterMenuExpanded,
                                onDismissRequest = { isFilterMenuExpanded = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                properties = PopupProperties(focusable = false),
                                containerColor = AppColors.DialogBackground,
                            ) {
                                ShortcutFilterOption.entries.forEachIndexed { index, option ->
                                    if (index > 0) {
                                        HorizontalDivider()
                                    }
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(option.labelResId)) },
                                        onClick = {
                                            selectedFilterOption = option
                                            isFilterMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                visibleShortcutGroups.forEach { group ->
                                    expandedCards[group.packageName] = !allExpanded
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (allExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                                contentDescription =
                                    if (allExpanded) {
                                        stringResource(R.string.settings_app_shortcuts_collapse_all)
                                    } else {
                                        stringResource(R.string.settings_app_shortcuts_expand_all)
                                    },
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            items(items = visibleShortcutGroups, key = { it.packageName }) { group ->
                val isExpanded = expandedCards[group.packageName] == true

                LaunchedEffect(focusPackageName, group.packageName) {
                    if (focusPackageName == group.packageName) {
                        expandedCards[group.packageName] = true
                        onFocusHandled()
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    AppShortcutCardHeader(
                        packageName = group.packageName,
                        appLabel = group.appLabel,
                        shortcutCount = group.shortcuts.size,
                        searchTarget =
                            if (isSearchTargetShortcutPackageName(group.packageName)) {
                                group.searchTargetSources.firstOrNull()?.target
                            } else {
                                null
                            },
                        isExpanded = isExpanded,
                        onToggleExpanded = {
                            expandedCards[group.packageName] = !isExpanded
                        },
                        iconPackPackage = iconPackPackage,
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(220)),
                    ) {
                        Column {
                            var hasRenderedSection = false
                            val appShortcuts = group.shortcuts
                            if (appShortcuts.isNotEmpty()) {
                                if (hasRenderedSection) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                appShortcuts.forEachIndexed { index, shortcut ->
                                    val shortcutId = shortcutKey(shortcut)
                                    val isCustomShortcut = isUserCreatedShortcut(shortcut)
                                    ShortcutToggleRow(
                                        shortcut = shortcut,
                                        checked = !disabledShortcutIds.contains(shortcutId),
                                        showToggle = true,
                                        onCheckedChange = { enabled ->
                                            onShortcutEnabledChange(shortcut, enabled)
                                        },
                                        onShortcutNameClick = { onShortcutNameClick(shortcut) },
                                        onEditClick =
                                            if (isCustomShortcut) {
                                                { shortcutToEdit = shortcut }
                                            } else {
                                                null
                                            },
                                        iconPackPackage = iconPackPackage,
                                    )

                                    if (index < appShortcuts.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                                hasRenderedSection = true
                            }

                            val regularAppSources =
                                group.sources.filter { it.sourceType != AppShortcutSourceType.APP_ACTIVITY }
                            if (regularAppSources.isNotEmpty()) {
                                if (hasRenderedSection) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }

                                regularAppSources.forEachIndexed { index, source ->
                                    ShortcutSourceRow(
                                        source = source,
                                        onClick = { onAddShortcutFromSource(source) },
                                    )
                                    if (index < regularAppSources.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                                hasRenderedSection = true
                            }

                            val searchTargetSources = group.searchTargetSources
                            if (searchTargetSources.isNotEmpty()) {
                                if (hasRenderedSection) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                searchTargetSources.forEachIndexed { index, source ->
                                    SearchTargetShortcutSourceRow(
                                        source = source,
                                        onClick = { shortcutDialogSource = source },
                                    )
                                    if (index < searchTargetSources.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                                hasRenderedSection = true
                            }

                            val appActivitySources =
                                group.sources.filter { it.sourceType == AppShortcutSourceType.APP_ACTIVITY }
                            if (appActivitySources.isNotEmpty()) {
                                if (hasRenderedSection) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                appActivitySources.forEachIndexed { index, source ->
                                    ShortcutSourceRow(
                                        source = source,
                                        onClick = { onAddShortcutFromSource(source) },
                                    )
                                    if (index < appActivitySources.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.padding(bottom = DesignTokens.SpacingLarge),
                )
            }
        }
}
