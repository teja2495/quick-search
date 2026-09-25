package com.tk.quicksearch.search.searchScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R

@Composable
internal fun toolCardConfig(
    isSearchHistoryExpanded: Boolean,
    showAiFollowUpAction: Boolean,
    onShowAiFollowUpInput: () -> Unit,
    showCurrencyConverterSearchCard: Boolean,
    onCurrencyConversionClick: () -> Unit,
    showDictionarySearchCard: Boolean,
    onDictionarySearchClick: () -> Unit,
    showWeatherSearchCard: Boolean,
    isWeatherAliasMode: Boolean,
    trimmedQuery: String,
    weatherLocation: String,
    onWeatherSearchClick: () -> Unit,
    showCustomToolSearchCard: Boolean,
    customToolName: String?,
    onCustomToolSearchClick: () -> Unit,
    showTaskerIntentCard: Boolean,
    taskerIntentName: String?,
    onTaskerIntentClick: () -> Unit,
    showWorldClockSearchCard: Boolean,
    onWorldClockSearchClick: () -> Unit,
): ToolCardConfig? {
    return if (isSearchHistoryExpanded) {
                null
            } else {
                when {
                    showAiFollowUpAction ->
                            ToolCardConfig(
                                    label = stringResource(R.string.direct_search_ask_follow_up),
                                    icon = Icons.Rounded.QuestionAnswer,
                                    onClick = { onShowAiFollowUpInput() },
                            )
                    showCurrencyConverterSearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.get_currency_value),
                                    icon = Icons.Rounded.CurrencyExchange,
                                    onClick = onCurrencyConversionClick,
                            )
                    showDictionarySearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.search_in_dictionary),
                                    icon = Icons.Rounded.Search,
                                    onClick = onDictionarySearchClick,
                            )
                    showWeatherSearchCard ->
                            ToolCardConfig(
                                    label =
                                        if (isWeatherAliasMode && trimmedQuery.isBlank()) {
                                            stringResource(
                                                R.string.weather_in_location,
                                                weatherLocation,
                                            )
                                        } else {
                                            stringResource(R.string.get_weather)
                                        },
                                    icon = Icons.Rounded.Cloud,
                                    onClick = onWeatherSearchClick,
                            )
                    showCustomToolSearchCard ->
                            ToolCardConfig(
                                    label = customToolName.orEmpty(),
                                    icon = Icons.Rounded.Construction,
                                    onClick = onCustomToolSearchClick,
                            )
                    showTaskerIntentCard ->
                            ToolCardConfig(
                                    label = stringResource(
                                            R.string.tasker_intent_action,
                                            taskerIntentName.orEmpty(),
                                    ),
                                    appIconPackage = com.tk.quicksearch.tools.tasker.TaskerIntegration.PACKAGE_NAME,
                                    onClick = onTaskerIntentClick,
                            )
                    showWorldClockSearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.get_time),
                                    icon = Icons.Rounded.AccessTime,
                                    onClick = onWorldClockSearchClick,
                            )
                    else -> null
                }
            }
}
