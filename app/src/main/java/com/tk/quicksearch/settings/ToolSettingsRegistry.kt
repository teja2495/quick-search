package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.searchEngines.AliasHandler

enum class ToolSettingId {
    CALCULATOR,
    UNIT_CONVERTER,
    DATE_CALCULATOR,
    CURRENCY_CONVERTER,
    WORD_CLOCK,
    DICTIONARY,
}

data class ToolSettingDefinition(
    val id: ToolSettingId,
    val aliasFeatureId: String,
    val titleResId: Int,
    val defaultDescriptionResId: Int,
    val requiresGeminiApiKey: Boolean = false,
    val requiresGeminiDescriptionResId: Int? = null,
    val icon: ImageVector,
    val toggleKey: AppSettingsToggleKey? = null,
    val infoDestination: SettingsDetailType? = null,
)

data class ToolSettingUiState(
    val enabled: Boolean,
    val aliasCode: String,
)

object ToolSettingsRegistry {
    val definitions: List<ToolSettingDefinition> =
        listOf(
            ToolSettingDefinition(
                id = ToolSettingId.CALCULATOR,
                aliasFeatureId = AliasHandler.CALCULATOR_ALIAS_FEATURE_ID,
                titleResId = R.string.calculator_toggle_title,
                defaultDescriptionResId = R.string.calculator_toggle_desc,
                icon = Icons.Rounded.Calculate,
                toggleKey = AppSettingsToggleKey.CALCULATOR,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.UNIT_CONVERTER,
                aliasFeatureId = AliasHandler.UNIT_CONVERTER_ALIAS_FEATURE_ID,
                titleResId = R.string.unit_converter_toggle_title,
                defaultDescriptionResId = R.string.unit_converter_toggle_desc,
                icon = Icons.Rounded.Straighten,
                toggleKey = AppSettingsToggleKey.UNIT_CONVERTER,
                infoDestination = SettingsDetailType.UNIT_CONVERTER_INFO,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.DATE_CALCULATOR,
                aliasFeatureId = AliasHandler.DATE_CALCULATOR_ALIAS_FEATURE_ID,
                titleResId = R.string.date_calculator_toggle_title,
                defaultDescriptionResId = R.string.date_calculator_toggle_desc,
                icon = Icons.Rounded.CalendarMonth,
                toggleKey = AppSettingsToggleKey.DATE_CALCULATOR,
                infoDestination = SettingsDetailType.DATE_CALCULATOR_INFO,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.CURRENCY_CONVERTER,
                aliasFeatureId = AliasHandler.CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                titleResId = R.string.currency_converter_toggle_title,
                defaultDescriptionResId = R.string.currency_converter_toggle_desc,
                requiresGeminiApiKey = true,
                requiresGeminiDescriptionResId = R.string.currency_converter_requires_gemini_key,
                icon = Icons.Rounded.CurrencyExchange,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.WORD_CLOCK,
                aliasFeatureId = AliasHandler.WORD_CLOCK_ALIAS_FEATURE_ID,
                titleResId = R.string.word_clock_toggle_title,
                defaultDescriptionResId = R.string.word_clock_toggle_desc,
                requiresGeminiApiKey = true,
                requiresGeminiDescriptionResId = R.string.word_clock_requires_gemini_key,
                icon = Icons.Rounded.AccessTime,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.DICTIONARY,
                aliasFeatureId = AliasHandler.DICTIONARY_ALIAS_FEATURE_ID,
                titleResId = R.string.dictionary_toggle_title,
                defaultDescriptionResId = R.string.dictionary_toggle_desc,
                requiresGeminiApiKey = true,
                requiresGeminiDescriptionResId = R.string.dictionary_requires_gemini_key,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                toggleKey = AppSettingsToggleKey.DICTIONARY,
            ),
        )

    fun definitionFor(id: ToolSettingId): ToolSettingDefinition? =
        definitions.firstOrNull { it.id == id }
}
