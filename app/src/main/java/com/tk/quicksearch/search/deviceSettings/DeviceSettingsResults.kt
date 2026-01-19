package com.tk.quicksearch.search.deviceSettings

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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.R
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.util.hapticConfirm
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants

private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12

@Composable
private fun DeviceSettingsCard(
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
fun DeviceSettingsResultsSection(
    modifier: Modifier = Modifier,
    settings: List<DeviceSetting>,
    isExpanded: Boolean,
    pinnedSettingIds: Set<String>,
    onSettingClick: (DeviceSetting) -> Unit,
    onTogglePin: (DeviceSetting) -> Unit,
    onExclude: (DeviceSetting) -> Unit,
    onNicknameClick: (DeviceSetting) -> Unit,
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
        settings.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
    }

    val canShowExpandControls = showExpandControls && settings.size > SearchScreenConstants.INITIAL_RESULT_COUNT
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

        DeviceSettingsCard(
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
    shortcut: DeviceSetting,
    isPinned: Boolean,
    onClick: (DeviceSetting) -> Unit,
    onTogglePin: (DeviceSetting) -> Unit,
    onExclude: (DeviceSetting) -> Unit,
    onNicknameClick: (DeviceSetting) -> Unit,
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

        DeviceSettingsDropdownMenu(
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
            modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp)
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
            modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp)
        )
    }
}