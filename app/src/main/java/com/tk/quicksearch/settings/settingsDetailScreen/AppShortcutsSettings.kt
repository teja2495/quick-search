package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.isUserCreatedShortcut
import com.tk.quicksearch.search.data.rememberShortcutIcon
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.settings.shared.AppShortcutSource
import com.tk.quicksearch.settings.shared.filterAppShortcutSources
import com.tk.quicksearch.search.searchEngines.getContentDescription
import com.tk.quicksearch.search.searchEngines.getSearchTargetShortcutPackageName
import com.tk.quicksearch.search.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.search.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

private data class AppShortcutGroup(
    val packageName: String,
    val appLabel: String,
    val shortcuts: List<StaticShortcut>,
    val sources: List<AppShortcutSource>,
    val querySource: SearchTargetQuerySource? = null,
)

private data class SearchTargetQuerySource(
    val target: SearchTarget,
    val packageName: String,
    val label: String,
)

private enum class ShortcutSearchMatchPriority {
    EXACT,
    STARTS_WITH,
    WORD_STARTS_WITH,
    CONTAINS,
}

private enum class ShortcutFilterOption(val labelResId: Int) {
    ALL(R.string.settings_app_shortcuts_filter_all_apps_with_shortcuts),
    CUSTOM_SHORTCUTS(R.string.settings_app_shortcuts_filter_apps_with_custom_shortcuts),
    SEARCH_ENGINES(R.string.settings_app_shortcuts_filter_search_engines),
}

private fun shortcutMatchPriority(name: String, query: String, locale: Locale): ShortcutSearchMatchPriority? {
    val normalizedName = name.lowercase(locale)
    return when {
        normalizedName == query -> ShortcutSearchMatchPriority.EXACT
        normalizedName.startsWith(query) -> ShortcutSearchMatchPriority.STARTS_WITH
        normalizedName
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .any { it.isNotBlank() && it.startsWith(query) } ->
            ShortcutSearchMatchPriority.WORD_STARTS_WITH
        normalizedName.contains(query) -> ShortcutSearchMatchPriority.CONTAINS
        else -> null
    }
}

private fun bestShortcutMatchPriority(
    group: AppShortcutGroup,
    query: String,
    locale: Locale,
): ShortcutSearchMatchPriority? {
    val allCandidates =
        buildList {
            add(group.appLabel)
            addAll(group.shortcuts.map(::shortcutDisplayName))
            addAll(group.sources.map { it.label })
            group.querySource?.let { add(it.label) }
        }
    return allCandidates.mapNotNull { shortcutMatchPriority(it, query, locale) }.minOrNull()
}

private fun resolveAppLabel(
    context: android.content.Context,
    packageName: String,
    locale: Locale,
): String {
    return runCatching {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager
            .getApplicationLabel(appInfo)
            .toString()
            .takeIf { it.isNotBlank() }
            ?: packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(locale) }
    }.getOrElse {
        packageName.substringAfterLast(".").replaceFirstChar { it.titlecase(locale) }
    }
}

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
    val queryShortcutSources =
        remember(searchTargets) {
            searchTargets
                .filterNot { target -> target is SearchTarget.Browser }
                .map { target ->
                    SearchTargetQuerySource(
                        target = target,
                        packageName = getSearchTargetShortcutPackageName(target),
                        label = target.getContentDescription(),
                    )
                }
        }
    val allShortcutGroups =
        remember(displayShortcuts, filteredShortcutSources, queryShortcutSources, context, locale) {
            val shortcutsByPackage = displayShortcuts.groupBy { it.packageName }
            val sourcesByPackage = filteredShortcutSources.groupBy { it.packageName }
            val querySourcesByPackage = queryShortcutSources.associateBy { it.packageName }
            (shortcutsByPackage.keys + sourcesByPackage.keys + querySourcesByPackage.keys)
                .mapNotNull { packageName ->
                    val appShortcuts = shortcutsByPackage[packageName].orEmpty()
                    val appSources = sourcesByPackage[packageName].orEmpty()
                    val querySource = querySourcesByPackage[packageName]
                    if (appShortcuts.isEmpty() && appSources.isEmpty() && querySource == null) {
                        null
                    } else {
                        val appLabel =
                            appShortcuts.firstOrNull()?.appLabel?.takeIf { it.isNotBlank() }
                                ?: querySource?.label?.takeIf { it.isNotBlank() }
                                ?: resolveAppLabel(context, packageName, locale)
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
                            querySource = querySource,
                        )
                    }
                }.sortedBy { it.appLabel.lowercase(locale) }
        }
    val shortcutGroups =
        remember(allShortcutGroups, normalizedSearchQuery, locale) {
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
                        val matchesQuerySource =
                            group.querySource?.let {
                                shortcutMatchPriority(it.label, normalizedSearchQuery, locale) != null
                            } == true
                        val filteredGroup =
                            when {
                                appMatchPriority != null -> group
                                matchingShortcuts.isNotEmpty() || matchingSources.isNotEmpty() || matchesQuerySource ->
                                    group.copy(
                                        shortcuts = matchingShortcuts,
                                        sources = matchingSources,
                                        querySource = if (matchesQuerySource) group.querySource else null,
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
    var selectedFilterOption by remember { mutableStateOf(ShortcutFilterOption.ALL) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }
    val visibleShortcutGroups =
        remember(shortcutGroups, selectedFilterOption) {
            when (selectedFilterOption) {
                ShortcutFilterOption.ALL -> shortcutGroups
                ShortcutFilterOption.CUSTOM_SHORTCUTS ->
                    shortcutGroups.filter { it.sources.isNotEmpty() || it.querySource != null }
                ShortcutFilterOption.SEARCH_ENGINES ->
                    shortcutGroups.filter { it.querySource != null }
            }
        }
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }
    var queryShortcutDialogSource by remember { mutableStateOf<SearchTargetQuerySource?>(null) }

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

    queryShortcutDialogSource?.let { source ->
        AddSearchTargetQueryShortcutDialog(
            targetLabel = source.label,
            onDismiss = { queryShortcutDialogSource = null },
            onSave = { shortcutName, shortcutQuery ->
                onAddQueryShortcut(source.target, shortcutName, shortcutQuery)
                queryShortcutDialogSource = null
            },
        )
    }

    val allExpanded =
        visibleShortcutGroups.isNotEmpty() &&
            visibleShortcutGroups.all { expandedCards[it.packageName] == true }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        if (normalizedSearchQuery.isBlank()) {
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

        if (normalizedSearchQuery.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
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
                        shape = RoundedCornerShape(24.dp),
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

        visibleShortcutGroups.forEach { group ->
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
                    searchTarget = group.querySource?.target,
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
                        val appShortcuts = group.shortcuts
                        if (appShortcuts.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                                    onDeleteClick =
                                        if (isCustomShortcut) {
                                            { onDeleteCustomShortcut(shortcut) }
                                        } else {
                                            null
                                        },
                                    iconPackPackage = iconPackPackage,
                                )

                                if (index < appShortcuts.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }

                        val appSources = group.sources
                        if (appSources.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            appSources.forEachIndexed { index, source ->
                                ShortcutSourceRow(
                                    source = source,
                                    onClick = { onAddShortcutFromSource(source) },
                                )
                                if (index < appSources.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }

                        val querySource = group.querySource
                        if (querySource != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SearchTargetQuerySourceRow(
                                source = querySource,
                                onClick = { queryShortcutDialogSource = querySource },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutSourceRow(
    source: AppShortcutSource,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (source.icon != null) {
            Image(
                bitmap = source.icon,
                contentDescription = source.label,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(DesignTokens.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = source.label.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = source.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.settings_app_shortcuts_add_button),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchTargetQuerySourceRow(
    source: SearchTargetQuerySource,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        SearchTargetIcon(
            target = source.target,
            iconSize = DesignTokens.IconSize,
            style = IconRenderStyle.ADVANCED,
        )

        Text(
            text = stringResource(R.string.settings_app_shortcuts_add_query_shortcut),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.settings_app_shortcuts_add_query_shortcut),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AddSearchTargetQueryShortcutDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var shortcutName by remember(targetLabel) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var shortcutQuery by remember {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }

    val trimmedName = shortcutName.text.trim()
    val trimmedQuery = shortcutQuery.text.trim()
    val canSave = trimmedName.isNotBlank() && trimmedQuery.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_app_shortcuts_add_query_dialog_title, targetLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(stringResource(R.string.settings_app_shortcuts_shortcut_name_label)) },
                )
                OutlinedTextField(
                    value = shortcutQuery,
                    onValueChange = { shortcutQuery = it },
                    singleLine = false,
                    maxLines = 3,
                    label = { Text(stringResource(R.string.settings_app_shortcuts_query_label)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmedName, trimmedQuery) },
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun AppShortcutCardHeader(
    packageName: String,
    appLabel: String,
    shortcutCount: Int,
    searchTarget: SearchTarget?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    iconPackPackage: String?,
) {
    val iconResult = rememberAppIcon(packageName = packageName, iconPackPackage = iconPackPackage)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (searchTarget != null) {
            SearchTargetIcon(
                target = searchTarget,
                iconSize = DesignTokens.IconSize,
                style = IconRenderStyle.ADVANCED,
            )
        } else if (iconResult.bitmap != null) {
            Image(
                bitmap = iconResult.bitmap,
                contentDescription = appLabel,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(DesignTokens.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = appLabel.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.settings_app_shortcuts_card_count,
                        shortcutCount,
                        shortcutCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}

@Composable
private fun ShortcutToggleRow(
    shortcut: StaticShortcut,
    checked: Boolean,
    showToggle: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onShortcutNameClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    iconPackPackage: String?,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val shortcutName = shortcutDisplayName(shortcut)
    val iconSize = DesignTokens.IconSize
    val density = LocalDensity.current
    val iconSizePx =
        remember(iconSize, density) {
            with(density) { iconSize.roundToPx().coerceAtLeast(1) }
        }
    val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
    val appIconResult = rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)
    val displayIcon = iconBitmap ?: appIconResult.bitmap

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (displayIcon != null) {
            Image(
                bitmap = displayIcon,
                contentDescription = shortcutName,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortcutName.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = shortcutName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onShortcutNameClick,
                    ),
        )

        if (onDeleteClick != null) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_app_shortcuts_delete_action),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (showToggle) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    hapticToggle(view)()
                    onCheckedChange(it)
                },
                modifier = Modifier.scale(0.85f),
            )
        }
    }
}
