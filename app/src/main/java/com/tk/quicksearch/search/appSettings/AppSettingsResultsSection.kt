package com.tk.quicksearch.search.appSettings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.AppSwitch
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonSegment
import com.tk.quicksearch.search.searchScreen.shared.segmentedRowsActive
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerPadding
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerVerticalPadding
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.ResultRowDivider
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.settings.appearanceSettings.homeTextColorTitleRes
import com.tk.quicksearch.search.searchScreen.shared.compactEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
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
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonDivider
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt

private const val QUICK_SEARCH_PACKAGE_NAME = "com.tk.quicksearch"
private const val ROW_MIN_HEIGHT = 52
private const val ICON_SIZE = 24
private const val EXPAND_BUTTON_HORIZONTAL_PADDING = 12

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
        segmented = true,
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
            val segmentCount = displayRows.size + if (cardState.shouldShowExpandButton) 1 else 0
            val segmented = segmentedRowsActive()
            Column(
                modifier =
                    Modifier.padding(
                        start = resultRowsContainerPadding(DesignTokens.SpacingLarge),
                        top = resultRowsContainerVerticalPadding(4.dp),
                        end = resultRowsContainerPadding(DesignTokens.SpacingMedium),
                        bottom = resultRowsContainerVerticalPadding(4.dp),
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
                    key(setting.id) {
                    val showPredictedOnRow =
                        predictedAppSettingId == setting.id && !useCardLevelPrediction

                    ResultSegment(index = index, count = segmentCount) {
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
                    }

                    if (index != displayRows.lastIndex && !showPredictedOnRow) {
                        ResultRowDivider(showWallpaperBackground, color = overlayDividerColor)
                    }
                    }
                }

                if (cardState.shouldShowExpandButton && segmented) {
                    ExpandButtonSegment(index = segmentCount - 1, count = segmentCount, onClick = onExpandClick)
                } else if (cardState.shouldShowExpandButton) {
                    ExpandButtonDivider(showWallpaperBackground)
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = if (LocalSearchCardLayoutEnabled.current) DesignTokens.SpacingXSmall else 0.dp),
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
    val isNotificationDotsSetting = setting.toggleKey == AppSettingsToggleKey.NOTIFICATION_DOTS
    val context = LocalContext.current
    // Cached status: the live check queries RoleManager and writes a preference on every recomposition.
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
    val isOverlayBlockedByLauncher =
        setting.toggleKey == AppSettingsToggleKey.OVERLAY_MODE && isDefaultLauncher
    val isAutoCloseBlockedByLauncher =
        setting.toggleKey == AppSettingsToggleKey.AUTO_CLOSE_OVERLAY && isDefaultLauncher
    val isBlockedByLauncher = isOverlayBlockedByLauncher || isAutoCloseBlockedByLauncher
    val effectiveDescription =
        if (isBlockedByLauncher) {
            stringResource(R.string.settings_overlay_mode_desc_launcher_blocked)
        } else if (setting.destination == AppSettingsRouteAction.APP_LANGUAGE) {
            val selectedLanguageLabel =
                remember(context) { AppLanguageManager.getSelectedLanguageLabel(context) }
            stringResource(R.string.settings_app_language_desc, selectedLanguageLabel)
        } else if (
            setting.toggleKey == AppSettingsToggleKey.HOME_TEXT_COLOR &&
                !LocalSearchCardLayoutEnabled.current
        ) {
            null
        } else if (isNotificationDotsSetting && checked) {
            // The Dots/Count choice replaces the description once enabled.
            null
        } else {
            setting.description
        }

    val rowActionModifier =
        when {
            setting.isNavigateAction || setting.isNavigationToggle ->
                Modifier.clickable(enabled = !isBlockedByLauncher, role = Role.Button) {
                    hapticConfirm(view)()
                    onClick(setting)
                }
            !isAppsPerRowSetting && !isAppResultRowsSetting && !setting.hasInlineControl ->
                Modifier.toggleable(value = checked, enabled = !isBlockedByLauncher, role = Role.Switch) { enabled ->
                    hapticToggle(view)()
                    onToggle(setting, enabled)
                }
            else -> Modifier
        }
    val rowModifier =
        Modifier.fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT.dp)
            .topPredictedRowContainer(isTopPredicted = isPredicted)
            .padding(vertical = resultRowVerticalPadding(DesignTokens.SpacingLarge))
            .then(rowActionModifier)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(ICON_SIZE.dp).padding(start = DesignTokens.SpacingXSmall)
                .then(if (isBlockedByLauncher) Modifier.alpha(AppColors.ContentMutedAlpha) else Modifier),
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
            verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing),
        ) {
            Text(
                text =
                    rememberQueryHighlightedText(
                        if (setting.toggleKey == AppSettingsToggleKey.HOME_TEXT_COLOR) {
                            stringResource(homeTextColorTitleRes(!LocalSearchCardLayoutEnabled.current))
                        } else {
                            setting.title
                        },
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isBlockedByLauncher) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.ContentMutedAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            effectiveDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = rememberQueryHighlightedText(description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isBlockedByLauncher) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppColors.ContentSecondaryAlpha)
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
            } else if (checked && setting.toggleKey == AppSettingsToggleKey.RECENT_QUERIES) {
                SearchHistoryCountSlider()
            } else if (checked && isNotificationDotsSetting) {
                NotificationBadgeStyleInlineChoice()
            } else if (checked && setting.toggleKey == AppSettingsToggleKey.CIRCULAR_APP_ICONS) {
                AppIconCornerInlineSlider()
            }
        }

        if (isAppsPerRowSetting) {
            ConnectedChoiceGroup(
                options =
                    listOf(
                        ChoiceOption(4, stringResource(R.string.settings_app_columns_4)),
                        ChoiceOption(5, stringResource(R.string.settings_app_columns_5)),
                    ),
                selected = if (appSettingPhoneAppGridColumns == 4) 4 else 5,
                onSelect = { columns ->
                    if (columns != appSettingPhoneAppGridColumns) onAppSettingPhoneAppGridColumnsChange(columns)
                },
                fillWidth = false,
                modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
            )
        } else if (isAppResultRowsSetting) {
            AppResultRowsChips(
                selectedRowCount = appSettingAppResultRowCount,
                onSelectRowCount = onAppSettingAppResultRowCountChange,
            )
        } else if (setting.destination == AppSettingsRouteAction.AI_MODEL) {
            Icon(
                imageVector = MaterialSymbols.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = DesignTokens.SpacingSmall),
            )
        } else if (setting.hasInlineControl) {
            // The inline control is rendered above.
        } else if (setting.isToggleAction) {
            if (setting.isNavigationToggle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MaterialSymbols.ChevronRight,
                        contentDescription = stringResource(R.string.desc_navigate_forward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp).padding(start = DesignTokens.SpacingSmall),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            AppSwitch(
                checked = checked,
                onCheckedChange = { enabled ->
                    hapticToggle(view)()
                    onToggle(setting, enabled)
                },
                enabled = !isBlockedByLauncher,
                // Material's disabled colors are composited over the theme surface, which turns the
                // thumb into an opaque blob on result cards; keep them translucent instead.
                colors = SwitchDefaults.colors(
                    uncheckedTrackColor = Color.Transparent,
                    disabledUncheckedTrackColor = Color.Transparent,
                    disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintStrongAlpha),
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintStrongAlpha),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintLightAlpha),
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.DisabledContentAlpha),
                ),
            )
        } else {
            val isReloadDestination = setting.destination == AppSettingsAction.RELOAD_APPS ||
                setting.destination == AppSettingsAction.RELOAD_CONTACTS ||
                setting.destination == AppSettingsAction.RELOAD_FILES
            Icon(
                imageVector = if (isReloadDestination) MaterialSymbols.Refresh else MaterialSymbols.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = DesignTokens.SpacingSmall),
            )
        }
    }
}

@Composable
private fun AppResultRowsChips(
    selectedRowCount: Int,
    onSelectRowCount: (Int) -> Unit,
) {
    ConnectedChoiceGroup(
        options = listOf(1, 2).map { rowCount -> ChoiceOption(rowCount, rowCount.toString()) },
        selected = if (selectedRowCount == 2) 2 else 1,
        onSelect = { rowCount -> if (rowCount != selectedRowCount) onSelectRowCount(rowCount) },
        fillWidth = false,
        modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
    )
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
