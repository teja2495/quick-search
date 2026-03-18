package com.tk.quicksearch.search.appSettings

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayActionColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.hapticToggle

private const val QUICK_SEARCH_PACKAGE_NAME = "com.tk.quicksearch"
private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12
private const val TOGGLE_SCALE = 0.85f

@Composable
fun AppSettingsResultsSection(
    modifier: Modifier = Modifier,
    appSettings: List<AppSettingResult>,
    isExpanded: Boolean,
    onAppSettingClick: (AppSettingResult) -> Unit,
    onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    onWebSuggestionsCountChange: (Int) -> Unit,
    isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    webSuggestionsCount: Int,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showWallpaperBackground: Boolean = false,
    overlayCardColor: androidx.compose.ui.graphics.Color? = null,
    dividerColor: androidx.compose.ui.graphics.Color? = null,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
) {
    if (appSettings.isEmpty()) return

    val predictedAppSettingId = (predictedTarget as? PredictedSubmitTarget.AppSetting)?.id
    val scrollState = rememberScrollState()
    val displayAsExpanded = isExpanded || showAllResults
    val hasPredictedRow =
        predictedAppSettingId != null && appSettings.any { it.id == predictedAppSettingId }
    val useCardLevelPrediction =
        hasPredictedRow && (!displayAsExpanded || appSettings.size == 1)

    ExpandableResultsCard(
        modifier = modifier,
        resultCount = appSettings.size,
        isExpanded = displayAsExpanded,
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
                appSettings
            } else {
                appSettings.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
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
                displayRows.forEachIndexed { index, setting ->
                    val showPredictedOnRow =
                        predictedAppSettingId == setting.id && !useCardLevelPrediction

                    AppSettingResultRow(
                        setting = setting,
                        checked = isAppSettingToggleChecked(setting),
                        onToggle = onAppSettingToggle,
                        onWebSuggestionsCountChange = onWebSuggestionsCountChange,
                        onClick = onAppSettingClick,
                        webSuggestionsCount = webSuggestionsCount,
                        isPredicted = showPredictedOnRow,
                    )

                    if (index != displayRows.lastIndex && !showPredictedOnRow) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = dividerColor ?: MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }

                if (cardState.shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppSettingResultRow(
    setting: AppSettingResult,
    checked: Boolean,
    onToggle: (AppSettingResult, Boolean) -> Unit,
    onWebSuggestionsCountChange: (Int) -> Unit,
    onClick: (AppSettingResult) -> Unit,
    webSuggestionsCount: Int,
    isPredicted: Boolean = false,
) {
    val view = LocalView.current
    val appIconResult =
        rememberAppIcon(
            packageName = QUICK_SEARCH_PACKAGE_NAME,
            iconPackPackage = null,
        )
    val isWebSuggestionsToggle = setting.toggleKey == AppSettingsToggleKey.WEB_SUGGESTIONS

    val rowModifier =
        Modifier.fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT.dp)
            .topPredictedRowContainer(isTopPredicted = isPredicted)
            .topPredictedRowContentPadding(isTopPredicted = isPredicted)
            .padding(vertical = DesignTokens.SpacingLarge)
            .combinedClickable(
                onClick = {
                    if (setting.isNavigateAction) {
                        hapticConfirm(view)()
                        onClick(setting)
                    } else {
                        hapticToggle(view)()
                        onToggle(setting, !checked)
                    }
                },
                role = if (setting.isToggleAction) Role.Switch else null,
            )

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(ICON_SIZE.dp).padding(start = DesignTokens.SpacingXSmall),
        ) {
            appIconResult.bitmap?.let { iconBitmap ->
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
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
                style = MaterialTheme.typography.titleMedium,
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

            if (isWebSuggestionsToggle && checked) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Slider(
                        value = webSuggestionsCount.toFloat(),
                        onValueChange = { value -> onWebSuggestionsCountChange(value.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.size(width = 140.dp, height = 22.dp),
                    )
                    Text(
                        text = webSuggestionsCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (setting.isToggleAction) {
            Switch(
                checked = checked,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(setting, enabled)
                },
                modifier = Modifier.scale(TOGGLE_SCALE),
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
