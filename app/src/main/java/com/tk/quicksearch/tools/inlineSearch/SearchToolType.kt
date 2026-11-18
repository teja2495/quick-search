package com.tk.quicksearch.tools.inlineSearch

import com.tk.quicksearch.search.appSettings.AppSettingsTarget
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.BooleanSetting
import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.tools.calculator.CalculatorResult
import com.tk.quicksearch.tools.calculator.evaluateCalculator
import com.tk.quicksearch.tools.colorVisualizer.ColorVisualizerResult
import com.tk.quicksearch.tools.colorVisualizer.evaluateColorVisualizer
import com.tk.quicksearch.tools.dateCalculator.evaluateDateCalculator
import com.tk.quicksearch.tools.unitConverter.evaluateUnitConverter

/** Built-in synchronous tools in automatic detection order. */
enum class SearchToolType(
    val setting: BooleanSetting,
    val settingsOrder: Int = Int.MAX_VALUE,
    val searchSettingsOrder: Int = settingsOrder,
    val aliasFeatureId: String? = null,
    val infoDestination: SettingsDetailType? = null,
    val searchDestination: AppSettingsTarget? = null,
    val autoDetect: Boolean = true,
    val replacesSearchResults: Boolean = true,
    val settingsIcon: () -> ImageVector,
    val attributionIcon: () -> ImageVector = settingsIcon,
    private val evaluate: (query: String, explicit: Boolean) -> CalculatorState?,
    val renderResult: @Composable (result: CalculatorState, showWallpaperBackground: Boolean) -> Unit,
) {
    CALCULATOR(
        setting = BooleanSetting(
            preferenceKey = "calculator_enabled",
            defaultValue = true,
            searchId = "app_toggle_calculator",
            titleRes = R.string.calculator_toggle_title,
            descriptionRes = R.string.calculator_toggle_desc,
        ),
        settingsOrder = 0,
        aliasFeatureId = "calculator_mode",
        settingsIcon = { MaterialSymbols.Calculate },
        attributionIcon = { MaterialSymbols.CalculateOutlined },
        evaluate = ::evaluateCalculator,
        renderResult = { result, wallpaper -> CalculatorResult(result, wallpaper) },
    ),
    UNIT_CONVERTER(
        setting = BooleanSetting(
            preferenceKey = "unit_converter_enabled",
            defaultValue = true,
            searchId = "app_toggle_unit_converter",
            titleRes = R.string.unit_converter_info_title,
            descriptionRes = R.string.date_calculator_toggle_desc,
            keywords = listOf("conversion", "examples"),
        ),
        settingsOrder = 1,
        aliasFeatureId = "unit_converter_mode",
        infoDestination = SettingsDetailType.UNIT_CONVERTER_INFO,
        searchDestination = AppSettingsTarget.Page(SettingsDetailType.UNIT_CONVERTER_INFO),
        settingsIcon = { MaterialSymbols.Straighten },
        attributionIcon = { MaterialSymbols.StraightenOutlined },
        evaluate = { query, _ -> evaluateUnitConverter(query) },
        renderResult = { result, wallpaper -> CalculatorResult(result, wallpaper) },
    ),
    COLOR_VISUALIZER(
        setting = BooleanSetting(
            preferenceKey = "color_visualizer_enabled",
            defaultValue = true,
            searchId = "app_toggle_color_visualizer",
            titleRes = R.string.color_visualizer_toggle_title,
            descriptionRes = R.string.color_visualizer_toggle_desc,
            keywords = listOf("color", "colour", "hex", "rgb"),
        ),
        settingsOrder = 5,
        searchSettingsOrder = 4,
        settingsIcon = { MaterialSymbols.Palette },
        attributionIcon = { MaterialSymbols.PaletteOutlined },
        evaluate = ::evaluateColorVisualizer,
        renderResult = { result, wallpaper -> ColorVisualizerResult(result, wallpaper) },
    ),
    DATE_CALCULATOR(
        setting = BooleanSetting(
            preferenceKey = "date_calculator_enabled",
            defaultValue = true,
            searchId = "app_toggle_date_calculator",
            titleRes = R.string.date_calculator_info_title,
            descriptionRes = R.string.date_calculator_toggle_desc,
            keywords = listOf("examples"),
        ),
        settingsOrder = 2,
        aliasFeatureId = "date_calculator_mode",
        infoDestination = SettingsDetailType.DATE_CALCULATOR_INFO,
        searchDestination = AppSettingsTarget.Page(SettingsDetailType.DATE_CALCULATOR_INFO),
        replacesSearchResults = false,
        settingsIcon = { MaterialSymbols.CalendarMonth },
        attributionIcon = { MaterialSymbols.CalendarMonthOutlined },
        evaluate = ::evaluateDateCalculator,
        renderResult = { result, wallpaper -> CalculatorResult(result, wallpaper) },
    ),
    ;

    internal fun matchQuery(query: String): CalculatorState? =
        evaluate(query, false)?.copy(toolType = this, isToolMode = false)

    internal fun evaluateInMode(query: String): CalculatorState {
        if (query.isEmpty()) return emptyMode()
        val result = evaluate(query, true) ?: return emptyMode().copy(showInvalidExpression = true)
        return result.copy(toolType = this, isToolMode = true)
    }

    internal fun emptyMode(): CalculatorState = CalculatorState(toolType = this, isToolMode = true)
}

internal fun detectInlineSearchTool(
    query: String,
    isEnabled: (BooleanSetting) -> Boolean,
): CalculatorState? {
    for (tool in SearchToolType.entries) {
        if (!tool.autoDetect || !isEnabled(tool.setting)) continue
        tool.matchQuery(query)?.let { return it }
    }
    return null
}

/** An optional feature-specific payload rendered by the tool that supplied it. */
interface InlineToolData
