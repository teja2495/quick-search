package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.tools.inlineSearch.SearchToolType
import kotlinx.coroutines.delay

private const val SEARCH_HINT_ROTATION_INTERVAL_MS = 5000L

@Composable
internal fun rememberSearchHint(state: SearchUiState, isDefaultLauncher: Boolean): String {
    val isCalculatorMode = state.calculatorState.isCalculatorMode
    val isUnitConverterMode = state.calculatorState.isUnitConverterMode
    val isCurrencyConverterAliasMode = state.isCurrencyConverterAliasMode
    val isWorldClockAliasMode = state.isWorldClockAliasMode
    val isDictionaryAliasMode = state.isDictionaryAliasMode
    val isWeatherAliasMode = state.isWeatherAliasMode
    val activeCustomTool = state.detectedCustomToolId?.let { id -> state.customTools.find { it.id == id } }
    val hintSearchAnything = stringResource(R.string.search_hint)
    val staticSearchHint =
        stringResource(
            if (BuildConfig.DEBUG) {
                // Debug builds override app_name to "QS Debug"
                R.string.app_name
            } else if (isDefaultLauncher) {
                R.string.common_search
            } else {
                R.string.search_hint_static
            },
        )
    val calculatorEnabled = state.isBooleanSettingEnabled(SearchToolType.CALCULATOR.setting)
    val unitConverterEnabled = state.isBooleanSettingEnabled(SearchToolType.UNIT_CONVERTER.setting)
    val dateCalculatorEnabled = state.isBooleanSettingEnabled(SearchToolType.DATE_CALCULATOR.setting)
    // stringArrayResource returns a new array each call; arrays compare by identity, so key on a list
    val cycleHints = stringArrayResource(R.array.search_hints_cycle).toList()
    // Indices must stay in sync with R.array.search_hints_cycle order in strings.xml
    val defaultHints = remember(
        hintSearchAnything,
        cycleHints,
        state.disabledSections,
        state.hasContactPermission,
        state.hasCalendarPermission,
        state.hasFilePermission,
        state.hasApiKey,
        calculatorEnabled,
        unitConverterEnabled,
        dateCalculatorEnabled,
        state.currencyConverterEnabled,
        state.worldClockEnabled,
        state.dictionaryEnabled,
        state.weatherEnabled,
        state.weatherLocationConfigured,
    ) {
        val gated = listOf(
            cycleHints[0] to (SearchSection.CONTACTS !in state.disabledSections && state.hasContactPermission),
            cycleHints[1] to (SearchSection.FILES !in state.disabledSections && state.hasFilePermission),
            cycleHints[2] to (SearchSection.CALENDAR !in state.disabledSections && state.hasCalendarPermission),
            cycleHints[3] to (SearchSection.APPS !in state.disabledSections),
            cycleHints[4] to (SearchSection.APP_SHORTCUTS !in state.disabledSections),
            cycleHints[5] to (SearchSection.SETTINGS !in state.disabledSections),
            cycleHints[6] to state.currencyConverterEnabled,
            cycleHints[7] to unitConverterEnabled,
            cycleHints[8] to dateCalculatorEnabled,
            cycleHints[9] to calculatorEnabled,
            cycleHints[10] to (state.worldClockEnabled && state.hasApiKey),
            cycleHints[11] to state.dictionaryEnabled,
            cycleHints[12] to (state.weatherEnabled && state.hasApiKey),
            cycleHints[13] to true,
            cycleHints[14] to true,
            cycleHints[15] to true,
        )
        listOf(hintSearchAnything) + gated.filter { it.second }.map { it.first }.shuffled()
    }

    val isDefaultHintMode =
            !isCalculatorMode &&
                    !isUnitConverterMode &&
                    !isCurrencyConverterAliasMode &&
                    !isWorldClockAliasMode &&
                    !isDictionaryAliasMode &&
                    !isWeatherAliasMode &&
                    activeCustomTool == null &&
                    state.detectedAliasSearchSection == null

    var hintIndex by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isDefaultHintMode, state.searchHintsEnabled) {
        if (!isDefaultHintMode || !state.searchHintsEnabled) {
            hintIndex = 0
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(SEARCH_HINT_ROTATION_INTERVAL_MS)
                hintIndex = (hintIndex + 1) % defaultHints.size
            }
        }
    }

    val searchHintText =
            when {
                isCalculatorMode -> stringResource(R.string.calculator_enter_math_expression_hint)
                isUnitConverterMode -> stringResource(R.string.unit_converter_enter_conversion_hint)
                isCurrencyConverterAliasMode ->
                        stringResource(R.string.search_hint_currency_converter)
                isWorldClockAliasMode -> stringResource(R.string.search_hint_world_clock)
                isDictionaryAliasMode -> stringResource(R.string.search_hint_dictionary)
                isWeatherAliasMode -> stringResource(R.string.search_hint_weather)
                activeCustomTool != null -> activeCustomTool.name
                state.detectedAliasSearchSection != null ->
                    stringResource(
                        SearchSectionUiMetadataRegistry
                            .metadataFor(state.detectedAliasSearchSection)
                            .searchHintRes,
                    )
                !state.searchHintsEnabled -> staticSearchHint
                else -> defaultHints[hintIndex % defaultHints.size]
            }
    return searchHintText
}
