package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.shared.ToolToggleCardModel
import com.tk.quicksearch.settings.shared.ToolToggleRows
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun ToolsSettingsSection(
    calculatorEnabled: Boolean,
    calculatorAlias: String,
    unitConverterEnabled: Boolean,
    unitConverterAlias: String,
    dateCalculatorEnabled: Boolean,
    dateCalculatorAlias: String,
    existingShortcuts: Map<String, String>,
    onSetCalculatorAlias: (String) -> Unit,
    onSetUnitConverterAlias: (String) -> Unit,
    onSetDateCalculatorAlias: (String) -> Unit,
    onCalculatorToggle: (Boolean) -> Unit,
    onUnitConverterToggle: (Boolean) -> Unit,
    onDateCalculatorToggle: (Boolean) -> Unit,
    onNavigateToUnitConverterInfo: () -> Unit = {},
    onNavigateToDateCalculatorInfo: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        ToolToggleRows(
            tools =
                buildList {
                    add(
                        ToolToggleCardModel(
                            title = stringResource(R.string.calculator_toggle_title),
                            subtitle = stringResource(R.string.calculator_toggle_desc),
                            checked = calculatorEnabled,
                            onCheckedChange = onCalculatorToggle,
                            leadingIcon = Icons.Rounded.Calculate,
                            aliasCode = calculatorAlias,
                            onAliasCodeChange = onSetCalculatorAlias,
                            existingShortcuts = existingShortcuts,
                            aliasFeatureId = AliasHandler.CALCULATOR_ALIAS_FEATURE_ID,
                        ),
                    )
                    add(
                        ToolToggleCardModel(
                            title = stringResource(R.string.unit_converter_toggle_title),
                            subtitle = stringResource(R.string.unit_converter_toggle_desc),
                            checked = unitConverterEnabled,
                            onCheckedChange = onUnitConverterToggle,
                            leadingIcon = Icons.Rounded.Straighten,
                            aliasCode = unitConverterAlias,
                            onAliasCodeChange = onSetUnitConverterAlias,
                            existingShortcuts = existingShortcuts,
                            aliasFeatureId = AliasHandler.UNIT_CONVERTER_ALIAS_FEATURE_ID,
                            onRowClick = onNavigateToUnitConverterInfo,
                        )
                    )
                    add(
                        ToolToggleCardModel(
                            title = stringResource(R.string.date_calculator_toggle_title),
                            subtitle = stringResource(R.string.date_calculator_toggle_desc),
                            checked = dateCalculatorEnabled,
                            onCheckedChange = onDateCalculatorToggle,
                            leadingIcon = Icons.Rounded.CalendarMonth,
                            aliasCode = dateCalculatorAlias,
                            onAliasCodeChange = onSetDateCalculatorAlias,
                            existingShortcuts = existingShortcuts,
                            aliasFeatureId = AliasHandler.DATE_CALCULATOR_ALIAS_FEATURE_ID,
                            onRowClick = onNavigateToDateCalculatorInfo,
                        )
                    )
                },
        )

        Text(
            text = stringResource(R.string.settings_tools_more_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = DesignTokens.SpacingMedium,
                        start = DesignTokens.SpacingSmall,
                        end = DesignTokens.SpacingSmall,
                    ),
        )
    }
}
