package com.tk.quicksearch.search.deviceSettings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12
private const val QUICK_SEARCH_PACKAGE_NAME = "com.tk.quicksearch"

private sealed interface SettingsRowModel {
        val stableId: String
}

private data class DeviceSettingRowModel(
        val setting: DeviceSetting,
) : SettingsRowModel {
        override val stableId: String = setting.id
}

private data class AppSettingRowModel(
        val setting: AppSettingResult,
) : SettingsRowModel {
        override val stableId: String = setting.id
}

@Composable
fun DeviceSettingsResultsSection(
        modifier: Modifier = Modifier,
        settings: List<DeviceSetting>,
        appSettings: List<AppSettingResult>,
        isExpanded: Boolean,
        pinnedSettingIds: Set<String>,
        onSettingClick: (DeviceSetting) -> Unit,
        onAppSettingClick: (AppSettingResult) -> Unit,
        onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
        isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
        onTogglePin: (DeviceSetting) -> Unit,
        onExclude: (DeviceSetting) -> Unit,
        onNicknameClick: (DeviceSetting) -> Unit,
        getSettingNickname: (String) -> String?,
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
        val allRows =
                remember(settings, appSettings) {
                        buildList {
                                addAll(settings.map(::DeviceSettingRowModel))
                                addAll(appSettings.map(::AppSettingRowModel))
                        }
                }
        if (allRows.isEmpty()) return

        val predictedSettingId = (predictedTarget as? PredictedSubmitTarget.Setting)?.id
        val predictedAppSettingId = (predictedTarget as? PredictedSubmitTarget.AppSetting)?.id
        val hasPredictedRow =
                allRows.any { row ->
                        when (row) {
                                is DeviceSettingRowModel ->
                                        predictedSettingId != null &&
                                                row.setting.id == predictedSettingId
                                is AppSettingRowModel ->
                                        predictedAppSettingId != null &&
                                                row.setting.id == predictedAppSettingId
                        }
                }
        val displayAsExpanded = isExpanded || showAllResults
        val useCardLevelPrediction =
                hasPredictedRow && (!displayAsExpanded || allRows.size == 1)

        val scrollState = rememberScrollState()

        Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
                ExpandableResultsCard(
                        resultCount = allRows.size,
                        isExpanded = isExpanded,
                        showAllResults = showAllResults,
                        isTopPredicted = useCardLevelPrediction,
                        showExpandControls = showExpandControls,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        hasScrollableContent = scrollState.maxValue > 0,
                        fillExpandedHeight = fillExpandedHeight,
                        showWallpaperBackground = showWallpaperBackground,
                        overlayCardColor = overlayCardColor,
                ) { contentModifier, cardState ->
                        val displayRows =
                                if (cardState.displayAsExpanded) {
                                        allRows
                                } else {
                                        allRows.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
                                }
                        Column(
                                modifier =
                                        contentModifier.then(
                                                if (isExpanded) {
                                                        Modifier.verticalScroll(scrollState)
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
                                        displayRows.forEachIndexed { index, row ->
                                                val isPredictedRow =
                                                        when (row) {
                                                                is DeviceSettingRowModel -> {
                                                                        predictedSettingId !=
                                                                                null &&
                                                                                row.setting.id ==
                                                                                        predictedSettingId
                                                                }
                                                                is AppSettingRowModel -> {
                                                                        predictedAppSettingId !=
                                                                                null &&
                                                                                row.setting.id ==
                                                                                        predictedAppSettingId
                                                                }
                                                }
                                                val showPredictedOnRow =
                                                        isPredictedRow && !useCardLevelPrediction

                                                when (row) {
                                                        is DeviceSettingRowModel -> {
                                                                val shortcut = row.setting
                                                                SettingResultRow(
                                                                        shortcut = shortcut,
                                                                        isPinned =
                                                                                pinnedSettingIds.contains(
                                                                                        shortcut.id,
                                                                                ),
                                                                        onClick = onSettingClick,
                                                                        onTogglePin = onTogglePin,
                                                                        onExclude = onExclude,
                                                                        onNicknameClick =
                                                                                onNicknameClick,
                                                                        hasNickname =
                                                                                !getSettingNickname(
                                                                                                shortcut
                                                                                                        .id,
                                                                                        )
                                                                                        .isNullOrBlank(),
                                                                        isPredicted =
                                                                                showPredictedOnRow,
                                                                )
                                                        }
                                                        is AppSettingRowModel -> {
                                                                AppSettingResultRow(
                                                                        setting = row.setting,
                                                                        checked =
                                                                                isAppSettingToggleChecked(
                                                                                        row.setting,
                                                                                ),
                                                                        onToggle =
                                                                                onAppSettingToggle,
                                                                        onClick =
                                                                                onAppSettingClick,
                                                                        isPredicted =
                                                                                showPredictedOnRow,
                                                                )
                                                        }
                                                }

                                                if (index != displayRows.lastIndex &&
                                                        !showPredictedOnRow) {
                                                        HorizontalDivider(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = overlayDividerColor
                                                                                ?: MaterialTheme
                                                                                        .colorScheme
                                                                                        .outlineVariant,
                                                        )
                                                }
                                        }

                                        if (cardState.shouldShowExpandButton) {
                                                ExpandButton(
                                                        onClick = onExpandClick,
                                                        modifier =
                                                                Modifier.align(
                                                                                Alignment
                                                                                        .CenterHorizontally,
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
                                onTogglePin = { onTogglePin(shortcut) },
                                onExclude = { onExclude(shortcut) },
                                onNicknameClick = { onNicknameClick(shortcut) },
                                onAddToHome = { addToHomeHandler.addDeviceSettingToHome(shortcut) },
                        )
                }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppSettingResultRow(
        setting: AppSettingResult,
        checked: Boolean,
        onToggle: (AppSettingResult, Boolean) -> Unit,
        onClick: (AppSettingResult) -> Unit,
        isPredicted: Boolean = false,
) {
        val view = LocalView.current
        val appIconResult =
                rememberAppIcon(
                        packageName = QUICK_SEARCH_PACKAGE_NAME,
                        iconPackPackage = null,
                )

        val rowModifier =
                Modifier.fillMaxWidth()
                        .heightIn(min = ROW_MIN_HEIGHT.dp)
                        .topPredictedRowContainer(isTopPredicted = isPredicted)
                        .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                        .padding(vertical = 12.dp)
                        .then(
                                if (setting.isNavigateAction) {
                                        Modifier.combinedClickable(
                                                onClick = {
                                                        hapticConfirm(view)()
                                                        onClick(setting)
                                                },
                                        )
                                } else {
                                        Modifier
                                },
                        )

        Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
                Box(
                        modifier =
                                Modifier.size(ICON_SIZE.dp).padding(start = DesignTokens.SpacingXSmall),
                ) {
                        val iconBitmap = appIconResult.bitmap
                        if (iconBitmap != null) {
                                Image(
                                        bitmap = iconBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(ICON_SIZE.dp),
                                )
                        } else {
                                Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(ICON_SIZE.dp),
                                )
                        }
                }

                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                        Text(
                                text = setting.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                        )
                        setting.description?.takeIf { it.isNotBlank() }?.let { description ->
                                Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                )
                        }
                }

                if (setting.isToggleAction) {
                        Switch(
                                checked = checked,
                                onCheckedChange = { enabled ->
                                        onToggle(setting, enabled)
                                },
                        )
                } else {
                        Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = stringResource(R.string.desc_navigate_forward),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
        }
}

@Composable
private fun ExpandButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
        val overlayActionColor = LocalOverlayActionColor.current
        val moreActionColor =
                if (overlayActionColor != null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                }

        TextButton(
                onClick = onClick,
                modifier = modifier,
                contentPadding =
                        PaddingValues(
                                horizontal = EXPAND_BUTTON_HORIZONTAL_PADDING.dp,
                                vertical = 0.dp,
                        ),
        ) {
                Text(
                        text = stringResource(R.string.action_expand_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = moreActionColor,
                )
                Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(R.string.desc_expand),
                        tint = moreActionColor,
                        modifier = Modifier.size(ContactUiConstants.EXPAND_ICON_SIZE.dp),
                )
        }
}
