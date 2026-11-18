package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.tools.aiTools.ConfirmedDictionaryQuery
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockHandler
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockNotRecognizedException
import com.tk.quicksearch.tools.aiTools.ConfirmedWeatherQuery
import com.tk.quicksearch.tools.aiTools.WeatherHandler
import com.tk.quicksearch.tools.aiTools.WeatherIntentParser
import com.tk.quicksearch.tools.inlineSearch.SearchToolType
import com.tk.quicksearch.tools.inlineSearch.detectInlineSearchTool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class ToolAliasState(
    val lockedToolMode: SearchToolType?,
    val lockedCurrencyConverterAlias: Boolean,
    val lockedWorldClockAlias: Boolean,
    val lockedDictionaryAlias: Boolean,
    val lockedWeatherAlias: Boolean,
    val lockedCustomToolId: String? = null,
)

internal class SearchToolCoordinator(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher,
    private val userPreferences: UserAppPreferences,
    private val currencyConverterHandler: CurrencyConverterHandler,
    private val worldClockHandler: WorldClockHandler,
    private val dictionaryHandler: DictionaryHandler,
    private val weatherHandler: WeatherHandler,
    private val toolAliasStateProvider: () -> ToolAliasState,
    private val hasApiKeyProvider: () -> Boolean,
    private val currentResultsProvider: () -> SearchResultsState,
    private val currentQueryProvider: () -> String,
    private val clearInformationCardsExcept: (SearchViewModel.ActiveInformationCard) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val showToast: (Int) -> Unit,
) {
    private var currencyConversionJob: Job? = null
    private var currencyConversionQueryVersion: Long = 0L
    private var worldClockJob: Job? = null
    private var worldClockQueryVersion: Long = 0L
    private var dictionaryJob: Job? = null
    private var dictionaryQueryVersion: Long = 0L
    private var weatherJob: Job? = null
    private var weatherQueryVersion: Long = 0L

    fun cancelInactive(activeCard: SearchViewModel.ActiveInformationCard) {
        if (activeCard != SearchViewModel.ActiveInformationCard.CURRENCY_CONVERTER) {
            currencyConversionJob?.cancel()
        }
        if (activeCard != SearchViewModel.ActiveInformationCard.WORD_CLOCK) {
            worldClockJob?.cancel()
        }
        if (activeCard != SearchViewModel.ActiveInformationCard.DICTIONARY) {
            dictionaryJob?.cancel()
        }
        if (activeCard != SearchViewModel.ActiveInformationCard.WEATHER) {
            weatherJob?.cancel()
        }
    }

    fun resolveToolState(
        trimmedQuery: String,
        detectedTarget: SearchTarget?,
        detectedAliasSearchSection: SearchSection?,
        skipLocalTools: Boolean,
    ): CalculatorState {
        if (skipLocalTools || toolAliasStateProvider().lockedCustomToolId != null) {
            return CalculatorState()
        }

        val toolAliasState = toolAliasStateProvider()
        toolAliasState.lockedToolMode?.let { tool ->
            return tool.evaluateInMode(trimmedQuery)
        }

        if (detectedTarget != null || detectedAliasSearchSection != null) {
            return CalculatorState()
        }

        return detectInlineSearchTool(trimmedQuery, userPreferences::isBooleanSettingEnabled)
            ?: CalculatorState()
    }

    fun scheduleCurrencyConversion(trimmedQuery: String, showingTool: Boolean) {
        currencyConversionJob?.cancel()
        if (currentResultsProvider().currencyConverterState.status == CurrencyConverterStatus.Idle) return
        val aliasState = toolAliasStateProvider()
        val lockedCurrencyConverterAlias = aliasState.lockedCurrencyConverterAlias
        if (!userPreferences.isCurrencyConverterEnabled() && !lockedCurrencyConverterAlias) {
            updateResultsState { s ->
                if (s.currencyConverterState.status == CurrencyConverterStatus.Idle) {
                    s
                } else {
                    s.copy(currencyConverterState = CurrencyConverterState())
                }
            }
            return
        }
        if (showingTool || trimmedQuery.isBlank()) {
            updateResultsState { s ->
                if (s.currencyConverterState.status == CurrencyConverterStatus.Idle) {
                    s
                } else {
                    s.copy(currencyConverterState = CurrencyConverterState())
                }
            }
            return
        }
        val matchesCandidate =
            if (lockedCurrencyConverterAlias) {
                trimmedQuery.isNotBlank()
            } else {
                CurrencyConversionIntentParser.isCandidate(trimmedQuery)
            }
        if (!matchesCandidate) {
            updateResultsState { s ->
                if (s.currencyConverterState.status == CurrencyConverterStatus.Idle) {
                    s
                } else {
                    s.copy(currencyConverterState = CurrencyConverterState())
                }
            }
            return
        }
    }

    fun executeCurrencyConversion() {
        val trimmedQuery = currentQueryProvider().trim()
        if (trimmedQuery.isBlank()) return

        val aliasState = toolAliasStateProvider()
        if (!userPreferences.isCurrencyConverterEnabled() && !aliasState.lockedCurrencyConverterAlias) {
            return
        }

        val confirmed = CurrencyConversionIntentParser.parseConfirmed(trimmedQuery)
        if (confirmed == null) {
            updateResultsState { s -> s.copy(currencyConverterState = CurrencyConverterState()) }
            showToast(R.string.currency_converter_invalid_input)
            return
        }

        clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.CURRENCY_CONVERTER)
        val version = ++currencyConversionQueryVersion
        currencyConversionJob?.cancel()
        currencyConversionJob =
            scope.launch(workerDispatcher) {
                updateResultsState { s ->
                    s.copy(
                        currencyConverterState =
                            CurrencyConverterState(
                                status = CurrencyConverterStatus.Loading,
                                activeQuery = trimmedQuery,
                            ),
                    )
                }
                val apiResult = currencyConverterHandler.convert(confirmed)
                if (version != currencyConversionQueryVersion) return@launch
                if (currentQueryProvider().trim() != trimmedQuery) return@launch
                apiResult.fold(
                    onSuccess = { parsed ->
                        updateResultsState { s ->
                            s.copy(
                                currencyConverterState =
                                    CurrencyConverterState(
                                        status = CurrencyConverterStatus.Success,
                                        convertedAmount = parsed.convertedAmount,
                                        targetCurrencyCode = parsed.targetCurrencyCode,
                                        targetCurrencyName = parsed.targetCurrencyName,
                                        sourceAmount = parsed.sourceAmount,
                                        sourceCurrencyCode = parsed.sourceCurrencyCode,
                                        ratesFetchedAtMillis = parsed.ratesFetchedAtMillis,
                                        activeQuery = trimmedQuery,
                                    ),
                            )
                        }
                    },
                    onFailure = { e ->
                        val msg = e.message ?: appContext.getString(R.string.direct_search_error_generic)
                        updateResultsState { s ->
                            s.copy(
                                currencyConverterState =
                                    CurrencyConverterState(
                                        status = CurrencyConverterStatus.Error,
                                        errorMessage = msg,
                                        activeQuery = trimmedQuery,
                                    ),
                            )
                        }
                    },
                )
            }
    }

    fun scheduleWorldClock(trimmedQuery: String, showingTool: Boolean) {
        worldClockJob?.cancel()
        if (currentResultsProvider().worldClockState.status == WorldClockStatus.Idle) return
        val aliasState = toolAliasStateProvider()
        val lockedWorldClockAlias = aliasState.lockedWorldClockAlias
        val hasApiKey = hasApiKeyProvider()

        if ((!userPreferences.isWorldClockEnabled() && !lockedWorldClockAlias) || !hasApiKey) {
            updateResultsState { s ->
                if (s.worldClockState.status == WorldClockStatus.Idle) {
                    s
                } else {
                    s.copy(worldClockState = WorldClockState())
                }
            }
            return
        }
        if (showingTool || trimmedQuery.isBlank()) {
            updateResultsState { s ->
                if (s.worldClockState.status == WorldClockStatus.Idle) {
                    s
                } else {
                    s.copy(worldClockState = WorldClockState())
                }
            }
            return
        }
        val matchesCandidate =
            if (lockedWorldClockAlias) {
                trimmedQuery.isNotBlank()
            } else {
                WorldClockIntentParser.isCandidate(trimmedQuery)
            }
        if (!matchesCandidate) {
            updateResultsState { s ->
                if (s.worldClockState.status == WorldClockStatus.Idle) {
                    s
                } else {
                    s.copy(worldClockState = WorldClockState())
                }
            }
            return
        }
    }

    fun executeWorldClockLookup() {
        val trimmedQuery = currentQueryProvider().trim()
        if (trimmedQuery.isBlank()) return

        val aliasState = toolAliasStateProvider()
        if ((!userPreferences.isWorldClockEnabled() && !aliasState.lockedWorldClockAlias) ||
            !hasApiKeyProvider()
        ) {
            return
        }

        val confirmed =
            if (aliasState.lockedWorldClockAlias) {
                WorldClockIntentParser.parseAliasConfirmed(trimmedQuery)
            } else {
                WorldClockIntentParser.parseConfirmed(trimmedQuery)
            }
                ?: run {
                    updateResultsState { s -> s.copy(worldClockState = WorldClockState()) }
                    return
                }

        clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.WORD_CLOCK)
        val version = ++worldClockQueryVersion
        worldClockJob?.cancel()
        worldClockJob =
            scope.launch(workerDispatcher) {
                updateResultsState { s ->
                    s.copy(
                        worldClockState =
                            WorldClockState(
                                status = WorldClockStatus.Loading,
                                activeQuery = trimmedQuery,
                            ),
                    )
                }
                val apiResult = worldClockHandler.convert(confirmed)
                if (version != worldClockQueryVersion) return@launch
                if (currentQueryProvider().trim() != trimmedQuery) return@launch
                apiResult.fold(
                    onSuccess = { (parsed, modelId) ->
                        updateResultsState { s ->
                            s.copy(
                                worldClockState =
                                    WorldClockState(
                                        status = WorldClockStatus.Success,
                                        worldClockText = parsed.worldClockText,
                                        sourceTimeText = parsed.sourceTimeText,
                                        placeText = parsed.placeText,
                                        timeZoneText = parsed.timeZoneText,
                                        activeQuery = trimmedQuery,
                                        usedModelId = modelId,
                                        llmProviderId = userPreferences.getWorldClockProviderId(),
                                    ),
                            )
                        }
                    },
                    onFailure = { e ->
                        if (e is WorldClockNotRecognizedException) {
                            val msg = appContext.getString(R.string.world_clock_error_not_recognized)
                            updateResultsState { s ->
                                s.copy(
                                    worldClockState =
                                        WorldClockState(
                                            status = WorldClockStatus.Error,
                                            errorMessage = msg,
                                            activeQuery = trimmedQuery,
                                        ),
                                )
                            }
                            return@fold
                        }
                        val msg = e.message ?: appContext.getString(R.string.direct_search_error_generic)
                        updateResultsState { s ->
                            s.copy(
                                worldClockState =
                                    WorldClockState(
                                        status = WorldClockStatus.Error,
                                        errorMessage = msg,
                                        activeQuery = trimmedQuery,
                                    ),
                            )
                        }
                    },
                )
            }
    }

    fun scheduleDictionaryLookup(trimmedQuery: String, showingTool: Boolean) {
        dictionaryJob?.cancel()
        if (currentResultsProvider().dictionaryState.status == DictionaryStatus.Idle) return
        val aliasState = toolAliasStateProvider()
        val lockedDictionaryAlias = aliasState.lockedDictionaryAlias

        if (!userPreferences.isDictionaryEnabled() && !lockedDictionaryAlias) {
            updateResultsState { s ->
                if (s.dictionaryState.status == DictionaryStatus.Idle) {
                    s
                } else {
                    s.copy(dictionaryState = DictionaryState())
                }
            }
            return
        }
        if (showingTool || trimmedQuery.isBlank()) {
            updateResultsState { s ->
                if (s.dictionaryState.status == DictionaryStatus.Idle) {
                    s
                } else {
                    s.copy(dictionaryState = DictionaryState())
                }
            }
            return
        }
        val matchesCandidate =
            if (lockedDictionaryAlias) {
                trimmedQuery.isNotBlank()
            } else {
                DictionaryIntentParser.isCandidate(trimmedQuery)
            }
        if (!matchesCandidate) {
            updateResultsState { s ->
                if (s.dictionaryState.status == DictionaryStatus.Idle) {
                    s
                } else {
                    s.copy(dictionaryState = DictionaryState())
                }
            }
            return
        }
    }

    fun executeDictionaryLookup() {
        val trimmedQuery = currentQueryProvider().trim()
        if (trimmedQuery.isBlank()) return

        val aliasState = toolAliasStateProvider()
        if (!userPreferences.isDictionaryEnabled() && !aliasState.lockedDictionaryAlias) {
            return
        }

        val confirmed =
            if (aliasState.lockedDictionaryAlias) {
                ConfirmedDictionaryQuery(
                    term = trimmedQuery,
                    originalQuery = trimmedQuery,
                )
            } else {
                DictionaryIntentParser.parseConfirmed(trimmedQuery)
            }
                ?: run {
                    updateResultsState { s -> s.copy(dictionaryState = DictionaryState()) }
                    return
                }

        clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.DICTIONARY)
        val version = ++dictionaryQueryVersion
        dictionaryJob?.cancel()
        dictionaryJob =
            scope.launch(workerDispatcher) {
                updateResultsState { s ->
                    s.copy(
                        dictionaryState =
                            DictionaryState(
                                status = DictionaryStatus.Loading,
                                activeQuery = trimmedQuery,
                            ),
                    )
                }
                val apiResult = dictionaryHandler.define(confirmed)
                if (version != dictionaryQueryVersion) return@launch
                if (currentQueryProvider().trim() != trimmedQuery) return@launch
                apiResult.fold(
                    onSuccess = { parsed ->
                        updateResultsState { s ->
                            s.copy(
                                dictionaryState =
                                    DictionaryState(
                                        status = DictionaryStatus.Success,
                                        word = parsed.word,
                                        senses = parsed.senses,
                                        activeQuery = trimmedQuery,
                                    ),
                            )
                        }
                    },
                    onFailure = { e ->
                        val msg = e.message ?: appContext.getString(R.string.direct_search_error_generic)
                        updateResultsState { s ->
                            s.copy(
                                dictionaryState =
                                    DictionaryState(
                                        status = DictionaryStatus.Error,
                                        errorMessage = msg,
                                        activeQuery = trimmedQuery,
                                    ),
                            )
                        }
                    },
                )
            }
    }

    fun scheduleWeatherLookup(trimmedQuery: String, showingTool: Boolean) {
        weatherJob?.cancel()
        if (currentResultsProvider().weatherState.status == WeatherStatus.Idle) return
        val aliasState = toolAliasStateProvider()
        val canUseWeather =
            (userPreferences.isWeatherEnabled() || aliasState.lockedWeatherAlias) &&
                hasApiKeyProvider()
        val isCandidate =
            aliasState.lockedWeatherAlias || WeatherIntentParser.isCandidate(trimmedQuery)
        if (!canUseWeather || showingTool || !isCandidate) {
            updateResultsState { state ->
                if (state.weatherState.status == WeatherStatus.Idle) state
                else state.copy(weatherState = WeatherState())
            }
        }
    }

    fun executeWeatherLookup() {
        val trimmedQuery = currentQueryProvider().trim()
        val aliasState = toolAliasStateProvider()
        if ((!userPreferences.isWeatherEnabled() && !aliasState.lockedWeatherAlias) ||
            !hasApiKeyProvider()
        ) {
            return
        }
        val confirmed =
            if (aliasState.lockedWeatherAlias) {
                ConfirmedWeatherQuery(
                    requestedLocation = trimmedQuery.takeIf { it.isNotBlank() },
                    originalQuery = trimmedQuery,
                )
            } else {
                WeatherIntentParser.parseConfirmed(trimmedQuery)
            } ?: run {
                updateResultsState { it.copy(weatherState = WeatherState()) }
                return
            }

        clearInformationCardsExcept(SearchViewModel.ActiveInformationCard.WEATHER)
        val version = ++weatherQueryVersion
        weatherJob?.cancel()
        weatherJob =
            scope.launch(workerDispatcher) {
                updateResultsState { state ->
                    state.copy(
                        weatherState = WeatherState(
                            status = WeatherStatus.Loading,
                            activeQuery = trimmedQuery,
                        ),
                    )
                }
                val apiResult = weatherHandler.getWeather(confirmed)
                if (version != weatherQueryVersion) return@launch
                if (currentQueryProvider().trim() != trimmedQuery) return@launch
                apiResult.fold(
                    onSuccess = { (parsed, modelId) ->
                        updateResultsState { state ->
                            state.copy(
                                weatherState = WeatherState(
                                    status = WeatherStatus.Success,
                                    location = parsed.location,
                                    summary = parsed.summary,
                                    activeQuery = trimmedQuery,
                                    usedModelId = modelId,
                                    llmProviderId = userPreferences.getWeatherProviderId(),
                                ),
                            )
                        }
                    },
                    onFailure = { error ->
                        updateResultsState { state ->
                            state.copy(
                                weatherState = WeatherState(
                                    status = WeatherStatus.Error,
                                    errorMessage = error.message
                                        ?: appContext.getString(R.string.direct_search_error_generic),
                                    activeQuery = trimmedQuery,
                                ),
                            )
                        }
                    },
                )
            }
    }
}
