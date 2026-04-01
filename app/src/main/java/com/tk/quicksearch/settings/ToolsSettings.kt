package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
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
        hasGeminiApiKey: Boolean,
        currencyConverterFeatureFlagEnabled: Boolean,
        currencyConverterEnabled: Boolean,
        currencyConverterAlias: String,
        worldClockFeatureFlagEnabled: Boolean,
        wordClockEnabled: Boolean,
        wordClockAlias: String,
        dictionaryEnabled: Boolean,
        dictionaryAlias: String,
        existingShortcuts: Map<String, String>,
        onSetCalculatorAlias: (String) -> Unit,
        onSetUnitConverterAlias: (String) -> Unit,
        onSetDateCalculatorAlias: (String) -> Unit,
        onSetCurrencyConverterAlias: (String) -> Unit,
        onSetWordClockAlias: (String) -> Unit,
        onSetDictionaryAlias: (String) -> Unit,
        onCalculatorToggle: (Boolean) -> Unit,
        onUnitConverterToggle: (Boolean) -> Unit,
        onDateCalculatorToggle: (Boolean) -> Unit,
        onCurrencyConverterToggle: (Boolean) -> Unit,
        onWordClockToggle: (Boolean) -> Unit,
        onDictionaryToggle: (Boolean) -> Unit,
        onNavigateToGeminiApiSetup: () -> Unit = {},
        onNavigateToUnitConverterInfo: () -> Unit = {},
        onNavigateToDateCalculatorInfo: () -> Unit = {},
        modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = DesignTokens.SpacingSmall),
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = DesignTokens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            if (!hasGeminiApiKey) {
                SettingsCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                            item =
                                    SettingsCardItem(
                                            title = stringResource(R.string.settings_tools_gemini_api_title),
                                            description =
                                                    stringResource(
                                                            R.string.settings_tools_gemini_api_desc
                                                    ),
                                            iconResId = R.drawable.direct_search,
                                            actionOnPress = onNavigateToGeminiApiSetup,
                                    ),
                            contentPadding =
                                    PaddingValues(
                                            horizontal = DesignTokens.CardHorizontalPadding,
                                            vertical = DesignTokens.CardVerticalPadding,
                                    ),
                    )
                }
            }
            ToolToggleRows(
                    tools =
                            buildList {
                                add(
                                        ToolToggleCardModel(
                                                title =
                                                        stringResource(
                                                                R.string.calculator_toggle_title
                                                        ),
                                                subtitle =
                                                        stringResource(
                                                                R.string.calculator_toggle_desc
                                                        ),
                                                checked = calculatorEnabled,
                                                onCheckedChange = onCalculatorToggle,
                                                leadingIcon = Icons.Rounded.Calculate,
                                                aliasCode = calculatorAlias,
                                                onAliasCodeChange = onSetCalculatorAlias,
                                                existingShortcuts = existingShortcuts,
                                                aliasFeatureId =
                                                        AliasHandler.CALCULATOR_ALIAS_FEATURE_ID,
                                        ),
                                )
                                add(
                                        ToolToggleCardModel(
                                                title =
                                                        stringResource(
                                                                R.string.unit_converter_toggle_title
                                                        ),
                                                subtitle =
                                                        stringResource(
                                                                R.string.unit_converter_toggle_desc
                                                        ),
                                                checked = unitConverterEnabled,
                                                onCheckedChange = onUnitConverterToggle,
                                                leadingIcon = Icons.Rounded.Straighten,
                                                aliasCode = unitConverterAlias,
                                                onAliasCodeChange = onSetUnitConverterAlias,
                                                existingShortcuts = existingShortcuts,
                                                aliasFeatureId =
                                                        AliasHandler.UNIT_CONVERTER_ALIAS_FEATURE_ID,
                                                onRowClick = onNavigateToUnitConverterInfo,
                                        ),
                                )
                                add(
                                        ToolToggleCardModel(
                                                title =
                                                        stringResource(
                                                                R.string.date_calculator_toggle_title
                                                        ),
                                                subtitle =
                                                        stringResource(
                                                                R.string.date_calculator_toggle_desc
                                                        ),
                                                checked = dateCalculatorEnabled,
                                                onCheckedChange = onDateCalculatorToggle,
                                                leadingIcon = Icons.Rounded.CalendarMonth,
                                                aliasCode = dateCalculatorAlias,
                                                onAliasCodeChange = onSetDateCalculatorAlias,
                                                existingShortcuts = existingShortcuts,
                                                aliasFeatureId =
                                                        AliasHandler.DATE_CALCULATOR_ALIAS_FEATURE_ID,
                                                onRowClick = onNavigateToDateCalculatorInfo,
                                        ),
                                )
                                if (currencyConverterFeatureFlagEnabled) {
                                    add(
                                            ToolToggleCardModel(
                                                    title =
                                                            stringResource(
                                                                    R.string.currency_converter_toggle_title
                                                            ),
                                                    subtitle =
                                                            stringResource(
                                                                    if (hasGeminiApiKey) {
                                                                        R.string.currency_converter_toggle_desc
                                                                    } else {
                                                                        R.string.currency_converter_requires_gemini_key
                                                                    }
                                                            ),
                                                    enabled = hasGeminiApiKey,
                                                    checked = currencyConverterEnabled && hasGeminiApiKey,
                                                    onCheckedChange = onCurrencyConverterToggle,
                                                    leadingIcon = Icons.Rounded.CurrencyExchange,
                                                    aliasCode =
                                                            if (hasGeminiApiKey) {
                                                                currencyConverterAlias
                                                            } else {
                                                                null
                                                            },
                                                    onAliasCodeChange = onSetCurrencyConverterAlias,
                                                    existingShortcuts = existingShortcuts,
                                                    aliasFeatureId =
                                                            AliasHandler
                                                                    .CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                                            ),
                                    )
                                }
                                if (worldClockFeatureFlagEnabled) {
                                    add(
                                            ToolToggleCardModel(
                                                    title =
                                                            stringResource(
                                                                    R.string.word_clock_toggle_title
                                                            ),
                                                    subtitle =
                                                            stringResource(
                                                                    if (hasGeminiApiKey) {
                                                                        R.string.word_clock_toggle_desc
                                                                    } else {
                                                                        R.string.word_clock_requires_gemini_key
                                                                    }
                                                            ),
                                                    enabled = hasGeminiApiKey,
                                                    checked = wordClockEnabled && hasGeminiApiKey,
                                                    onCheckedChange = onWordClockToggle,
                                                    leadingIcon = Icons.Rounded.AccessTime,
                                                    aliasCode =
                                                            if (hasGeminiApiKey) {
                                                                wordClockAlias
                                                            } else {
                                                                null
                                                            },
                                                    onAliasCodeChange = onSetWordClockAlias,
                                                    existingShortcuts = existingShortcuts,
                                                    aliasFeatureId =
                                                            AliasHandler.WORD_CLOCK_ALIAS_FEATURE_ID,
                                            ),
                                    )
                                }
                                add(
                                        ToolToggleCardModel(
                                                title = stringResource(R.string.dictionary_toggle_title),
                                                subtitle =
                                                        stringResource(
                                                                if (hasGeminiApiKey) {
                                                                    R.string.dictionary_toggle_desc
                                                                } else {
                                                                    R.string.dictionary_requires_gemini_key
                                                                }
                                                        ),
                                                enabled = hasGeminiApiKey,
                                                checked = dictionaryEnabled && hasGeminiApiKey,
                                                onCheckedChange = onDictionaryToggle,
                                                leadingIcon = Icons.AutoMirrored.Rounded.MenuBook,
                                                aliasCode =
                                                        if (hasGeminiApiKey) {
                                                            dictionaryAlias
                                                        } else {
                                                            null
                                                        },
                                                onAliasCodeChange = onSetDictionaryAlias,
                                                existingShortcuts = existingShortcuts,
                                                aliasFeatureId = AliasHandler.DICTIONARY_ALIAS_FEATURE_ID,
                                        ),
                                )
                            },
            )
        }
    }
}
