package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.search.core.WeatherStatus
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.WorldClockStatus
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.ConfirmedWeatherQuery
import com.tk.quicksearch.tools.aiTools.WeatherIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser

internal data class SearchCardVisibility(
    val showCurrencyConverter: Boolean,
    val showWorldClock: Boolean,
    val showDictionary: Boolean,
    val showWeather: Boolean,
    val showCurrencyConverterSearchCard: Boolean,
    val showDictionarySearchCard: Boolean,
    val showWorldClockSearchCard: Boolean,
    val showWeatherSearchCard: Boolean,
    val showCustomToolSearchCard: Boolean,
    val showAiFollowUpAction: Boolean,
)

internal fun searchCardVisibility(
    state: SearchUiState,
    activeCustomToolPresent: Boolean,
): SearchCardVisibility {
    val isCurrencyConverterAliasMode = state.isCurrencyConverterAliasMode
    val isWorldClockAliasMode = state.isWorldClockAliasMode
    val isDictionaryAliasMode = state.isDictionaryAliasMode
    val isWeatherAliasMode = state.isWeatherAliasMode
    val showCurrencyConverter =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    state.currencyConverterState.status != CurrencyConverterStatus.Idle
    val showWorldClock =
            (state.worldClockEnabled || isWorldClockAliasMode) &&
                    state.worldClockState.status != WorldClockStatus.Idle
    val showDictionary =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.dictionaryState.status != DictionaryStatus.Idle
    val showWeather =
            (state.weatherEnabled || isWeatherAliasMode) &&
                    state.weatherState.status != WeatherStatus.Idle
    val showCalculatorResult =
            state.calculatorState.isToolMode ||
                    state.calculatorState.result != null ||
                    state.calculatorState.parsedDateMillis != null ||
                    state.calculatorState.dateDiffLabel != null ||
                    state.calculatorState.timeResultLabel != null
    val trimmedQuery = state.query.trim()
    val showCurrencyConverterSearchCard =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isCurrencyConverterAliasMode) {
                        true // always show when alias mode is active
                    } else {
                        trimmedQuery.isNotBlank() &&
                                CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showDictionarySearchCard =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isDictionaryAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                DictionaryIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showWorldClockSearchCard =
            (state.worldClockEnabled || isWorldClockAliasMode) &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isWorldClockAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                WorldClockIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val confirmedWeatherQuery =
            if (isWeatherAliasMode) {
                ConfirmedWeatherQuery(
                    requestedLocation = trimmedQuery.takeIf { it.isNotBlank() },
                    originalQuery = trimmedQuery,
                )
            } else {
                WeatherIntentParser.parseConfirmed(trimmedQuery)
            }
    val weatherLocationAvailable =
            state.weatherLocationConfigured ||
                    confirmedWeatherQuery?.requestedLocation?.isNotBlank() == true
    val showWeatherSearchCard =
            (state.weatherEnabled || isWeatherAliasMode) &&
                    state.hasApiKey &&
                    weatherLocationAvailable &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isWeatherAliasMode) {
                        true
                    } else {
                        confirmedWeatherQuery != null
                    }
    val showCustomToolSearchCard =
            activeCustomToolPresent &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    state.AiSearchState.status == AiSearchStatus.Idle
    val showAiFollowUpAction =
            state.AiSearchState.status == AiSearchStatus.Success &&
                    !state.AiSearchState.answer.isNullOrBlank() &&
                    state.detectedCustomToolId == null

    return SearchCardVisibility(
        showCurrencyConverter = showCurrencyConverter,
        showWorldClock = showWorldClock,
        showDictionary = showDictionary,
        showWeather = showWeather,
        showCurrencyConverterSearchCard = showCurrencyConverterSearchCard,
        showDictionarySearchCard = showDictionarySearchCard,
        showWorldClockSearchCard = showWorldClockSearchCard,
        showWeatherSearchCard = showWeatherSearchCard,
        showCustomToolSearchCard = showCustomToolSearchCard,
        showAiFollowUpAction = showAiFollowUpAction,
    )
}
