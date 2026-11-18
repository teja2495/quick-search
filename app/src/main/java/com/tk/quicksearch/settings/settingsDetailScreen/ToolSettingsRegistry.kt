package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.appSettings.BooleanSetting
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.tools.inlineSearch.SearchToolType

internal object ToolSettingId {
    const val CURRENCY_CONVERTER = "currency_converter"
    const val WORD_CLOCK = "world_clock"
    const val DICTIONARY = "dictionary"
    const val WEATHER = "weather"
}

internal data class ToolSettingDefinition(
    val id: String,
    val settingsOrder: Int,
    val searchSettingsOrder: Int,
    val titleResId: Int,
    val defaultDescriptionResId: Int,
    private val iconProvider: () -> ImageVector,
    val aliasFeatureId: String? = null,
    val requiresLlmApiKey: Boolean = false,
    val requiresLlmApiKeyDescriptionResId: Int? = null,
    val toggleKey: AppSettingsToggleKey? = null,
    val booleanSetting: BooleanSetting? = null,
    val searchId: String = booleanSetting?.searchId ?: "app_toggle_$id",
    val searchDestination: AppSettingsTarget? = null,
    val keywords: List<String> = emptyList(),
    val infoDestination: SettingsDetailType? = null,
    val aiBackedModelConfigurable: Boolean = false,
) {
    val icon: ImageVector get() = iconProvider()
}

internal data class ToolSettingUiState(
    val enabled: Boolean,
    val aliasCode: String,
)

internal object ToolSettingsRegistry {
    val definitions: List<ToolSettingDefinition> by lazy {
        val inlineTools = SearchToolType.entries.map { tool ->
            ToolSettingDefinition(
                id = tool.setting.preferenceKey,
                settingsOrder = tool.settingsOrder,
                searchSettingsOrder = tool.searchSettingsOrder,
                aliasFeatureId = tool.aliasFeatureId,
                titleResId = tool.setting.titleRes,
                defaultDescriptionResId = checkNotNull(tool.setting.descriptionRes),
                iconProvider = tool.settingsIcon,
                booleanSetting = tool.setting,
                searchDestination = tool.searchDestination,
                infoDestination = tool.infoDestination,
            )
        }
        val standaloneTools = listOf(
            ToolSettingDefinition(
                id = ToolSettingId.DICTIONARY,
                settingsOrder = 3,
                searchSettingsOrder = 6,
                aliasFeatureId = AliasHandler.DICTIONARY_ALIAS_FEATURE_ID,
                titleResId = R.string.dictionary_toggle_title,
                defaultDescriptionResId = R.string.dictionary_toggle_desc,
                iconProvider = { MaterialSymbols.MenuBook },
                toggleKey = AppSettingsToggleKey.DICTIONARY,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.CURRENCY_CONVERTER,
                settingsOrder = 4,
                searchSettingsOrder = 3,
                aliasFeatureId = AliasHandler.CURRENCY_CONVERTER_ALIAS_FEATURE_ID,
                titleResId = R.string.currency_converter_toggle_title,
                defaultDescriptionResId = R.string.currency_converter_toggle_desc,
                iconProvider = { MaterialSymbols.CurrencyExchange },
                toggleKey = AppSettingsToggleKey.CURRENCY_CONVERTER,
                keywords = listOf("currency", "exchange rate", "money"),
            ),
            ToolSettingDefinition(
                id = ToolSettingId.WORD_CLOCK,
                settingsOrder = 6,
                searchSettingsOrder = 5,
                aliasFeatureId = AliasHandler.WORD_CLOCK_ALIAS_FEATURE_ID,
                titleResId = R.string.world_clock_toggle_title,
                defaultDescriptionResId = R.string.world_clock_toggle_desc,
                requiresLlmApiKey = true,
                requiresLlmApiKeyDescriptionResId = R.string.currency_converter_requires_gemini_key,
                iconProvider = { MaterialSymbols.AccessTime },
                toggleKey = AppSettingsToggleKey.WORLD_CLOCK,
                keywords = listOf("time zone", "timezone"),
                aiBackedModelConfigurable = true,
            ),
            ToolSettingDefinition(
                id = ToolSettingId.WEATHER,
                settingsOrder = 7,
                searchSettingsOrder = 7,
                aliasFeatureId = AliasHandler.WEATHER_ALIAS_FEATURE_ID,
                titleResId = R.string.weather_toggle_title,
                defaultDescriptionResId = R.string.weather_toggle_desc,
                requiresLlmApiKey = true,
                requiresLlmApiKeyDescriptionResId = R.string.currency_converter_requires_gemini_key,
                iconProvider = { MaterialSymbols.Cloud },
                toggleKey = AppSettingsToggleKey.WEATHER,
                keywords = listOf("forecast", "temperature", "location"),
                aiBackedModelConfigurable = true,
            ),
        )
        (inlineTools + standaloneTools).sortedBy { it.settingsOrder }
    }

    fun definitionFor(id: String): ToolSettingDefinition? =
        definitions.firstOrNull { it.id == id }
}
