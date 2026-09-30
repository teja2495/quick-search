package com.tk.quicksearch.search.appSettings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactUiConstants
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt

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
    appSettingPhoneAppGridColumns: Int,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit,
    appSettingAppResultRowCount: Int,
    onAppSettingAppResultRowCountChange: (Int) -> Unit,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showWallpaperBackground: Boolean = false,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
) {
    if (appSettings.isEmpty()) return

    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
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
                        appSettingPhoneAppGridColumns = appSettingPhoneAppGridColumns,
                        onAppSettingPhoneAppGridColumnsChange = onAppSettingPhoneAppGridColumnsChange,
                        appSettingAppResultRowCount = appSettingAppResultRowCount,
                        onAppSettingAppResultRowCountChange = onAppSettingAppResultRowCountChange,
                        isPredicted = showPredictedOnRow,
                    )

                    if (index != displayRows.lastIndex && !showPredictedOnRow) {
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
                            Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppSettingResultRow(
    setting: AppSettingResult,
    checked: Boolean,
    onToggle: (AppSettingResult, Boolean) -> Unit,
    onWebSuggestionsCountChange: (Int) -> Unit,
    onClick: (AppSettingResult) -> Unit,
    webSuggestionsCount: Int,
    appSettingPhoneAppGridColumns: Int = 4,
    onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit = {},
    appSettingAppResultRowCount: Int = 1,
    onAppSettingAppResultRowCountChange: (Int) -> Unit = {},
    isPredicted: Boolean = false,
) {
    val view = LocalView.current
    val appIconResult =
        rememberAppIcon(
            packageName = QUICK_SEARCH_PACKAGE_NAME,
            iconPackPackage = null,
        )
    val isWebSuggestionsToggle = setting.toggleKey == AppSettingsToggleKey.WEB_SUGGESTIONS
    val isAppsPerRowSetting = setting.toggleKey == AppSettingsToggleKey.APPS_PER_ROW
    val isAppResultRowsSetting = setting.toggleKey == AppSettingsToggleKey.APP_RESULT_ROWS
    val context = LocalContext.current
    val isDefaultLauncher = context.isDefaultHomeApp()
    val isOverlayBlockedByLauncher =
        setting.toggleKey == AppSettingsToggleKey.OVERLAY_MODE && isDefaultLauncher
    val isAutoCloseBlockedByLauncher =
        setting.toggleKey == AppSettingsToggleKey.AUTO_CLOSE_OVERLAY && isDefaultLauncher
    val isBlockedByLauncher = isOverlayBlockedByLauncher || isAutoCloseBlockedByLauncher
    val effectiveDescription =
        if (isBlockedByLauncher) {
            stringResource(R.string.settings_overlay_mode_desc_launcher_blocked)
        } else if (setting.destination == AppSettingsDestination.APP_LANGUAGE) {
            val selectedLanguageLabel =
                remember(context) { AppLanguageManager.getSelectedLanguageLabel(context) }
            stringResource(R.string.settings_app_language_desc, selectedLanguageLabel)
        } else {
            setting.description
        }

    val rowModifier =
        Modifier.fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT.dp)
            .topPredictedRowContainer(isTopPredicted = isPredicted)
            .topPredictedRowContentPadding()
            .padding(vertical = resultRowVerticalPadding(DesignTokens.SpacingLarge))
            .combinedClickable(
                interactionSource = null,
                indication = null,
                enabled = !isBlockedByLauncher,
                onClick = {
                    if (setting.isNavigateAction || setting.isNavigationToggle) {
                        hapticConfirm(view)()
                        onClick(setting)
                    } else if (!isAppsPerRowSetting && !isAppResultRowsSetting && !setting.hasInlineControl) {
                        hapticToggle(view)()
                        onToggle(setting, !checked)
                    }
                },
                role = if (setting.isToggleAction && !setting.isNavigationToggle && !isAppsPerRowSetting && !isAppResultRowsSetting && !setting.hasInlineControl) Role.Switch else null,
            )

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(ICON_SIZE.dp).padding(start = DesignTokens.SpacingXSmall)
                .then(if (isBlockedByLauncher) Modifier.alpha(0.5f) else Modifier),
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
                text = rememberQueryHighlightedText(setting.title),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isBlockedByLauncher) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            effectiveDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = rememberQueryHighlightedText(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isBlockedByLauncher) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (setting.hasInlineControl) {
                AppSettingInlineControlContent(setting = setting)
            }

            if (isWebSuggestionsToggle && checked) {
                InlineSliderRow(
                    value = webSuggestionsCount.toFloat(),
                    onValueChange = { value -> onWebSuggestionsCountChange(value.roundToInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    label = webSuggestionsCount.toString(),
                )
            }
        }

        if (isAppsPerRowSetting) {
            val fourSelected = appSettingPhoneAppGridColumns == 4
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                modifier = Modifier.padding(end = 6.dp),
            ) {
                AppSettingChoiceChip(
                    label = stringResource(R.string.settings_app_columns_4),
                    selected = fourSelected,
                    onClick = { onAppSettingPhoneAppGridColumnsChange(4) },
                    showCheck = false,
                )
                AppSettingChoiceChip(
                    label = stringResource(R.string.settings_app_columns_5),
                    selected = !fourSelected,
                    onClick = { onAppSettingPhoneAppGridColumnsChange(5) },
                    showCheck = false,
                )
            }
        } else if (isAppResultRowsSetting) {
            AppResultRowsChips(
                selectedRowCount = appSettingAppResultRowCount,
                onSelectRowCount = onAppSettingAppResultRowCountChange,
            )
        } else if (setting.destination == AppSettingsDestination.AI_MODEL) {
            Icon(
                imageVector = Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        } else if (setting.id == THEME_MODE_SETTING_ID) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        } else if (setting.hasInlineControl) {
            Unit
        } else if (setting.isToggleAction) {
            if (setting.isNavigationToggle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(setting, enabled)
                },
                enabled = !isBlockedByLauncher,
                modifier = Modifier.scale(TOGGLE_SCALE),
                // Material's disabled colors are composited over the theme surface, which turns the
                // thumb into an opaque blob on result cards; keep them translucent instead.
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = Color.Transparent,
                    disabledUncheckedTrackColor = Color.Transparent,
                    disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            )
        } else {
            val isReloadDestination = setting.destination == AppSettingsDestination.RELOAD_APPS ||
                setting.destination == AppSettingsDestination.RELOAD_CONTACTS ||
                setting.destination == AppSettingsDestination.RELOAD_FILES
            Icon(
                imageVector = if (isReloadDestination) Icons.Rounded.Refresh else Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun AppResultRowsChips(
    selectedRowCount: Int,
    onSelectRowCount: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        modifier = Modifier.padding(end = 6.dp),
    ) {
        listOf(1, 2).forEach { rowCount ->
            AppSettingChoiceChip(
                label = rowCount.toString(),
                selected = selectedRowCount == rowCount,
                onClick = { onSelectRowCount(rowCount) },
                showCheck = false,
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
        textResId = R.string.action_expand_more_app_settings,
    )
}
