package com.tk.quicksearch.search.deviceSettings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12

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
        onTriggerClick: (DeviceSetting) -> Unit,
        getSettingNickname: (String) -> String?,
        getSettingTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        showAllResults: Boolean,
        showExpandControls: Boolean,
        onExpandClick: () -> Unit,
        expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
        showWallpaperBackground: Boolean = false,
        predictedTarget: PredictedSubmitTarget? = null,
        fillExpandedHeight: Boolean = false,
) {
        val overlayCardColor = LocalOverlayResultCardColor.current
        val overlayDividerColor = LocalOverlayDividerColor.current
        if (settings.isEmpty()) return

        val predictedSettingId = (predictedTarget as? PredictedSubmitTarget.Setting)?.id
        val deviceSettingsScrollState = rememberScrollState()
        val displayAsExpanded = isExpanded || showAllResults

        Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
                val hasPredictedRow =
                        predictedSettingId != null &&
                                settings.any { it.id == predictedSettingId }
                val useCardLevelPrediction =
                        hasPredictedRow && (!displayAsExpanded || settings.size == 1)

                ExpandableResultsCard(
                        resultCount = settings.size,
                        isExpanded = displayAsExpanded,
                        showAllResults = showAllResults,
                        isTopPredicted = useCardLevelPrediction,
                        showExpandControls = showExpandControls,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        hasScrollableContent = deviceSettingsScrollState.maxValue > 0,
                        fillExpandedHeight = fillExpandedHeight,
                        showWallpaperBackground = showWallpaperBackground,
                        overlayCardColor = overlayCardColor,
                ) { contentModifier, cardState ->
                        val displayRows =
                                if (cardState.displayAsExpanded) {
                                        settings
                                } else {
                                        settings.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
                                }

                        Column(
                                modifier =
                                        contentModifier.then(
                                                if (isExpanded) {
                                                        Modifier.verticalScroll(deviceSettingsScrollState)
                                                } else {
                                                        Modifier
                                                },
                                        ),
                        ) {
                                Column(
                                        modifier =
                                                Modifier.padding(
                                                        start = DesignTokens.SpacingLarge,
                                                        top = 4.dp,
                                                        end = DesignTokens.SpacingMedium,
                                                        bottom = 4.dp,
                                                )
                                                .padding(
                                                        bottom =
                                                                if (cardState.shouldFillExpandedHeight) {
                                                                        DesignTokens.SpacingSmall
                                                                } else {
                                                                        0.dp
                                                                },
                                                ),
                                ) {
                                        displayRows.forEachIndexed { index, shortcut ->
                                                val showPredictedOnRow =
                                                        predictedSettingId == shortcut.id &&
                                                                !useCardLevelPrediction

                                                SettingResultRow(
                                                        shortcut = shortcut,
                                                        isPinned =
                                                                pinnedSettingIds.contains(
                                                                        shortcut.id,
                                                                ),
                                                        onClick = onSettingClick,
                                                        onTogglePin = onTogglePin,
                                                        onExclude = onExclude,
                                                        onNicknameClick = onNicknameClick,
                                                        onTriggerClick = onTriggerClick,
                                                        hasNickname =
                                                                !getSettingNickname(shortcut.id)
                                                                        .isNullOrBlank(),
                                                        hasTrigger =
                                                                getSettingTrigger(shortcut.id)?.word?.isNotBlank() == true,
                                                        isPredicted = showPredictedOnRow,
                                                )

                                                if (index != displayRows.lastIndex &&
                                                        !showPredictedOnRow) {
                                                        HorizontalDivider(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = overlayDividerColor
                                                                                ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant,
                                                        )
                                                }
                                        }

                                        if (cardState.shouldShowExpandButton) {
                                                ExpandButton(
                                                        onClick = onExpandClick,
                                                        modifier =
                                                                Modifier.align(
                                                                                Alignment.CenterHorizontally,
                                                                        )
                                                                        .padding(top = 4.dp),
                                                )
                                        }
                                }
                        }
                }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingResultRow(
        shortcut: DeviceSetting,
        isPinned: Boolean,
        onClick: (DeviceSetting) -> Unit,
        onTogglePin: (DeviceSetting) -> Unit,
        onExclude: (DeviceSetting) -> Unit,
        onNicknameClick: (DeviceSetting) -> Unit,
        hasNickname: Boolean,
        onTriggerClick: (DeviceSetting) -> Unit = {},
        hasTrigger: Boolean = false,
        showDescription: Boolean = true,
        enableLongPress: Boolean = true,
        onLongPressOverride: (() -> Unit)? = null,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        isPredicted: Boolean = false,
) {
        val context = LocalContext.current
        val addToHomeHandler =
                remember(context) { com.tk.quicksearch.search.common.AddToHomeHandler(context) }
        var showOptions by remember { mutableStateOf(false) }
        val view = LocalView.current
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .heightIn(min = ROW_MIN_HEIGHT.dp)
                                .topPredictedRowContainer(isTopPredicted = isPredicted)
                                .combinedClickable(
                                        onClick = {
                                                hapticConfirm(view)()
                                                onClick(shortcut)
                                        },
                                        onLongClick = onLongPressOverride
                                                        ?: if (enableLongPress) {
                                                                { showOptions = true }
                                                        } else {
                                                                null
                                                        },
                                )
                                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                                .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
                Icon(
                        imageVector = icon ?: Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = iconTint,
                        modifier =
                                Modifier.size(if (icon != null) 30.dp else ICON_SIZE.dp)
                                        .padding(start = DesignTokens.SpacingXSmall),
                )

                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                        Text(
                                text = shortcut.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                        )
                        if (showDescription) {
                                shortcut.description?.takeIf { it.isNotBlank() }?.let { description
                                        ->
                                        Text(
                                                text = description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                        )
                                }
                        }
                }

                if (enableLongPress && onLongPressOverride == null) {
                        DeviceSettingsDropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                isPinned = isPinned,
                                hasNickname = hasNickname,
                                hasTrigger = hasTrigger,
                                onTogglePin = { onTogglePin(shortcut) },
                                onExclude = { onExclude(shortcut) },
                                onNicknameClick = { onNicknameClick(shortcut) },
                                onTriggerClick = { onTriggerClick(shortcut) },
                                onAddToHome = { addToHomeHandler.addDeviceSettingToHome(shortcut) },
                        )
                }
        }
}

@Composable
private fun ExpandButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
        com.tk.quicksearch.search.searchScreen.components.ExpandButton(
                onClick = onClick,
                modifier = modifier,
                textResId = R.string.action_expand_more_device_settings,
        )
}
