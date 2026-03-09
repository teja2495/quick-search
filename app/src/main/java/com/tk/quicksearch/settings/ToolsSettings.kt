package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.shared.ToolToggleCardModel
import com.tk.quicksearch.settings.shared.ToolToggleRows

@Composable
fun ToolsSettingsSection(
    calculatorEnabled: Boolean,
    calculatorAlias: String,
    existingShortcuts: Map<String, String>,
    onSetCalculatorAlias: (String) -> Unit,
    onCalculatorToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolToggleRows(
        tools =
            listOf(
                ToolToggleCardModel(
                    title = stringResource(R.string.calculator_toggle_title),
                    checked = calculatorEnabled,
                    onCheckedChange = onCalculatorToggle,
                    leadingIcon = Icons.Rounded.Calculate,
                    aliasCode = calculatorAlias,
                    onAliasCodeChange = onSetCalculatorAlias,
                    existingShortcuts = existingShortcuts,
                    aliasFeatureId = AliasHandler.CALCULATOR_ALIAS_FEATURE_ID,
                ),
            ),
        modifier = modifier,
    )
}
