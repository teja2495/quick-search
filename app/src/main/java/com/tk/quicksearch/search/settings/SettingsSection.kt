package com.tk.quicksearch.search.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.util.hapticConfirm

private const val INITIAL_RESULT_COUNT = 1
private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HEIGHT = 28
private const val EXPAND_ICON_SIZE = 18
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12

@Composable
private fun SettingsCard(
    showWallpaperBackground: Boolean,
    cardColors: androidx.compose.material3.CardColors,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardElevation: androidx.compose.material3.CardElevation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (showWallpaperBackground) {
        Card(
            modifier = modifier,
            colors = cardColors,
            shape = cardShape,
            elevation = cardElevation
        ) { content() }
    } else {
        ElevatedCard(
            modifier = modifier,
            colors = cardColors,
            shape = cardShape,
            elevation = cardElevation
        ) { content() }
    }
}

@Composable
fun SettingsResultsSection(
    modifier: Modifier = Modifier,
    settings: List<SettingShortcut>,
    isExpanded: Boolean,
    pinnedSettingIds: Set<String>,
    onSettingClick: (SettingShortcut) -> Unit,
    onTogglePin: (SettingShortcut) -> Unit,
    onExclude: (SettingShortcut) -> Unit,
    onNicknameClick: (SettingShortcut) -> Unit,
    getSettingNickname: (String) -> String?,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    showWallpaperBackground: Boolean = false
) {
    if (settings.isEmpty()) return

    val displaySettings = if (isExpanded || showAllResults) {
        settings
    } else {
        settings.take(INITIAL_RESULT_COUNT)
    }

    val canShowExpandControls = showExpandControls && settings.size > INITIAL_RESULT_COUNT
    val shouldShowExpandButton = !isExpanded && !showAllResults && canShowExpandControls
    val shouldShowCollapseButton = isExpanded && showExpandControls

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val cardColors = CardDefaults.cardColors(
            containerColor = if (showWallpaperBackground) {
                Color.Black.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )

        val cardShape = MaterialTheme.shapes.extraLarge
        val cardElevation = if (showWallpaperBackground) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.cardElevation()
        }

        SettingsCard(
            showWallpaperBackground = showWallpaperBackground,
            cardColors = cardColors,
            cardShape = cardShape,
            cardElevation = cardElevation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                displaySettings.forEachIndexed { index, shortcut ->
                    SettingResultRow(
                        shortcut = shortcut,
                        isPinned = pinnedSettingIds.contains(shortcut.id),
                        onClick = onSettingClick,
                        onTogglePin = onTogglePin,
                        onExclude = onExclude,
                        onNicknameClick = onNicknameClick,
                        hasNickname = !getSettingNickname(shortcut.id).isNullOrBlank()
                    )
                    if (index != displaySettings.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                if (shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        if (shouldShowCollapseButton) {
            CollapseButton(
                onClick = onExpandClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingResultRow(
    shortcut: SettingShortcut,
    isPinned: Boolean,
    onClick: (SettingShortcut) -> Unit,
    onTogglePin: (SettingShortcut) -> Unit,
    onExclude: (SettingShortcut) -> Unit,
    onNicknameClick: (SettingShortcut) -> Unit,
    hasNickname: Boolean
) {
    var showOptions by remember { mutableStateOf(false) }
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT.dp)
            .combinedClickable(
                onClick = {
                    hapticConfirm(view)()
                    onClick(shortcut)
                },
                onLongClick = { showOptions = true }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(ICON_SIZE.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            shortcut.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        SettingsDropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            isPinned = isPinned,
            hasNickname = hasNickname,
            onTogglePin = { onTogglePin(shortcut) },
            onExclude = { onExclude(shortcut) },
            onNicknameClick = { onNicknameClick(shortcut) }
        )
    }
}

@Composable
private fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isPinned: Boolean,
    hasNickname: Boolean,
    onTogglePin: () -> Unit,
    onExclude: () -> Unit,
    onNicknameClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        properties = PopupProperties(focusable = false)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isPinned) R.string.action_unpin_generic else R.string.action_pin_generic
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isPinned) Icons.Rounded.Close else Icons.Rounded.PushPin,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onTogglePin()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (hasNickname) R.string.action_edit_nickname else R.string.action_add_nickname
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onNicknameClick()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.action_exclude_generic)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null
                )
            },
            onClick = {
                onDismissRequest()
                onExclude()
            }
        )
    }
}

@Composable
private fun ExpandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = EXPAND_BUTTON_HORIZONTAL_PADDING.dp,
            vertical = 0.dp
        )
    ) {
        Text(
            text = stringResource(R.string.action_expand_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = stringResource(R.string.desc_expand),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}

@Composable
private fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.action_collapse),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.ExpandLess,
            contentDescription = stringResource(R.string.desc_collapse),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EXPAND_ICON_SIZE.dp)
        )
    }
}

