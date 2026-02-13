package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.data.isUserCreatedShortcut
import com.tk.quicksearch.search.data.rememberShortcutIcon
import com.tk.quicksearch.search.data.shortcutDisplayName
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle
import java.util.Locale

private data class AppShortcutGroup(
    val packageName: String,
    val appLabel: String,
    val shortcuts: List<StaticShortcut>,
)

@Composable
fun AppShortcutsSettingsSection(
    shortcuts: List<StaticShortcut>,
    disabledShortcutIds: Set<String>,
    iconPackPackage: String?,
    onShortcutEnabledChange: (StaticShortcut, Boolean) -> Unit,
    onShortcutNameClick: (StaticShortcut) -> Unit,
    onDeleteCustomShortcut: (StaticShortcut) -> Unit,
    focusShortcut: StaticShortcut? = null,
    focusPackageName: String? = null,
    onFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val displayShortcuts =
        remember(shortcuts, focusShortcut) {
            val shortcutToFocus = focusShortcut ?: return@remember shortcuts
            val exists = shortcuts.any { shortcutKey(it) == shortcutKey(shortcutToFocus) }
            if (exists) shortcuts else shortcuts + shortcutToFocus
        }
    val shortcutGroups =
        remember(displayShortcuts) {
            displayShortcuts
                .groupBy { it.packageName }
                .mapNotNull { (packageName, appShortcuts) ->
                    if (appShortcuts.isEmpty()) {
                        null
                    } else {
                        AppShortcutGroup(
                            packageName = packageName,
                            appLabel = appShortcuts.first().appLabel,
                            shortcuts =
                                appShortcuts.sortedWith(
                                    compareBy(
                                        { isUserCreatedShortcut(it) },
                                        { shortcutDisplayName(it).lowercase(Locale.getDefault()) },
                                    ),
                                ),
                        )
                    }
                }.sortedBy { it.appLabel.lowercase(Locale.getDefault()) }
        }
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(shortcutGroups) {
        val currentPackages = shortcutGroups.map { it.packageName }.toSet()
        val existingPackages = expandedCards.keys.toSet()

        shortcutGroups.forEach { group ->
            if (group.packageName !in expandedCards) {
                expandedCards[group.packageName] = false
            }
        }

        (existingPackages - currentPackages).forEach { removedPackage ->
            expandedCards.remove(removedPackage)
        }
    }

    if (shortcutGroups.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_app_shortcuts_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = DesignTokens.SpacingLarge),
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_app_shortcuts_available_count, shortcuts.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = DesignTokens.SpacingXSmall),
        )

        shortcutGroups.forEach { group ->
            val isExpanded = expandedCards[group.packageName] == true
            val bringIntoViewRequester = remember(group.packageName) { BringIntoViewRequester() }

            LaunchedEffect(focusPackageName, group.packageName) {
                if (focusPackageName == group.packageName) {
                    expandedCards[group.packageName] = true
                    bringIntoViewRequester.bringIntoView()
                    onFocusHandled()
                }
            }

            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                AppShortcutCardHeader(
                    packageName = group.packageName,
                    appLabel = group.appLabel,
                    shortcutCount = group.shortcuts.size,
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        group.shortcuts.forEachIndexed { index, shortcut ->
                            val shortcutId = shortcutKey(shortcut)
                            val isCustomShortcut = isUserCreatedShortcut(shortcut)
                            ShortcutToggleRow(
                                shortcut = shortcut,
                                checked = !disabledShortcutIds.contains(shortcutId),
                                showToggle = !isCustomShortcut,
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

                            if (index < group.shortcuts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShortcutCardHeader(
    packageName: String,
    appLabel: String,
    shortcutCount: Int,
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
        if (iconResult.bitmap != null) {
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
            )
        }
    }
}
