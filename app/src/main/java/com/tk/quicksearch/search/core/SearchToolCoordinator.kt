package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.tools.aiTools.ConfirmedDictionaryQuery
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.CurrencyNotRecognizedException
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryNotRecognizedException
import com.tk.quicksearch.tools.aiTools.WorldClockHandler
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser
import com.tk.quicksearch.tools.aiTools.WorldClockNotRecognizedException
import com.tk.quicksearch.tools.calculator.CalculatorHandler
import com.tk.quicksearch.tools.dateCalculator.DateCalculatorHandler
import com.tk.quicksearch.tools.unitConverter.UnitConverterHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class ToolAliasState(
    val lockedToolMode: SearchToolType?,
    val lockedCurrencyConverterAlias: Boolean,
    val lockedWorldClockAlias: Boolean,
    val lockedDictionaryAlias: Boolean,
    val lockedCustomToolId: String? = null,
)

internal class SearchToolCoordinator(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher,
    private val userPreferences: UserAppPreferences,
    private val calculatorHandler: CalculatorHandler,
    private val unitConverterHandler: UnitConverterHandler,
    private val dateCalculatorHandler: DateCalculatorHandler,
    private val currencyConverterHandler: CurrencyConverterHandler,
    private val worldClockHandler: WorldClockHandler,
    private val dictionaryHandler: DictionaryHandler,
    private val toolAliasStateProvider: () -> ToolAliasState,
    private val hasApiKeyProvider: () -> Boolean,
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

    fun cancelAll() {
        currencyConversionJob?.cancel()
        worldClockJob?.cancel()
        dictionaryJob?.cancel()
    }

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
        val toolMode = toolAliasState.lockedToolMode
        if (toolMode != null) {
            return when (toolMode) {
                SearchToolType.CALCULATOR ->
                    calculatorHandler.processQuery(
                        query = trimmedQuery,
                        forceCalculatorMode = true,
                    )

                SearchToolType.UNIT_CONVERTER ->
                    unitConverterHandler.processQuery(
                        query = trimmedQuery,
                        forceUnitConverterMode = true,
                    )

                SearchToolType.DATE_CALCULATOR ->
                    dateCalculatorHandler.processQuery(
                        query = trimmedQuery,
                        forceDateCalculatorMode = true,
                    )
            }
        }

        if (detectedTarget != null || detectedAliasSearchSection != null) {
            return CalculatorState()
        }

        val calculatorResult =
            calculatorHandler.processQuery(
                query = trimmedQuery,
                forceCalculatorMode = false,
            )
        if (calculatorResult.result != null) {
            return calculatorResult
        }

        val unitConverterResult =
            unitConverterHandler.processQuery(
                query = trimmedQuery,
                forceUnitConverterMode = false,
            )
        if (unitConverterResult.result != null) {
            return unitConverterResult
        }

        return dateCalculatorHandler.processQuery(
            query = trimmedQuery,
            forceDateCalculatorMode = false,
        )
    }

    fun scheduleCurrencyConversion(trimmedQuery: String, showingTool: Boolean) {
        currencyConversionJob?.cancel()
        val aliasState = toolAliasStateProvider()
        val lockedCurrencyConverterAlias = aliasState.lockedCurrencyConverterAlias
        val hasApiKey = hasApiKeyProvider()

        if ((!userPreferences.isCurrencyConverterEnabled() && !lockedCurrencyConverterAlias) ||
            !hasApiKey
        ) {
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
        if ((!userPreferences.isCurrencyConverterEnabled() && !aliasState.lockedCurrencyConverterAlias) ||
            !hasApiKeyProvider()
        ) {
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
                    onSuccess = { (parsed, modelId) ->
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
                                        activeQuery = trimmedQuery,
                                        usedModelId = modelId,
                                    ),
                            )
                        }
                    },
                    onFailure = { e ->
                        if (e is CurrencyNotRecognizedException) {
                            updateResultsState { s ->
                                s.copy(currencyConverterState = CurrencyConverterState())
                            }
                            return@launch
                        }
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
        val aliasState = toolAliasStateProvider()
        val lockedDictionaryAlias = aliasState.lockedDictionaryAlias
        val hasApiKey = hasApiKeyProvider()

        if ((!userPreferences.isDictionaryEnabled() && !lockedDictionaryAlias) || !hasApiKey) {
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
        if ((!userPreferences.isDictionaryEnabled() && !aliasState.lockedDictionaryAlias) ||
            !hasApiKeyProvider()
        ) {
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
                    onSuccess = { (parsed, modelId) ->
                        updateResultsState { s ->
                            s.copy(
                                dictionaryState =
                                    DictionaryState(
                                        status = DictionaryStatus.Success,
                                        word = parsed.word,
                                        partOfSpeech = parsed.partOfSpeech,
                                        meaning = parsed.meaning,
                                        example = parsed.example,
                                        synonyms = parsed.synonyms,
                                        activeQuery = trimmedQuery,
                                        usedModelId = modelId,
                                    ),
                            )
                        }
                    },
                    onFailure = { e ->
                        if (e is DictionaryNotRecognizedException) {
                            updateResultsState { s ->
                                s.copy(dictionaryState = DictionaryState())
                            }
                            return@fold
                        }
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
}
