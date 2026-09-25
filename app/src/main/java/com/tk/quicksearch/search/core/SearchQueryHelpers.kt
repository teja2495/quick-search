package com.tk.quicksearch.search.core

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class SearchQueryAliasState(
    val lockedShortcutTarget: SearchTarget?,
    val lockedAliasSearchSection: SearchSection?,
    val lockedToolMode: SearchToolType?,
    val lockedCurrencyConverterAlias: Boolean,
    val lockedWorldClockAlias: Boolean,
    val lockedDictionaryAlias: Boolean,
    val lockedWeatherAlias: Boolean,
    val lockedCustomToolId: String? = null,
    val lockedTaskerIntentId: String? = null,
)

internal class LatestSearchJobRunner<T>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val debounceMs: Long,
) {
    private var job: Job? = null
    private val version = AtomicLong(0L)

    fun submit(
        compute: suspend () -> T,
        publish: (T) -> Unit,
    ) {
        cancel()
        val currentVersion = version.incrementAndGet()
        job =
            scope.launch(dispatcher) {
                delay(debounceMs)
                if (currentVersion != version.get()) return@launch
                val result = compute()
                if (currentVersion != version.get()) return@launch
                publish(result)
            }
    }

    fun cancel() {
        job?.cancel()
        version.incrementAndGet()
    }
}

internal fun createToolModeState(toolMode: SearchToolType): CalculatorState =
        when (toolMode) {
            SearchToolType.CALCULATOR ->
                CalculatorState(
                    isCalculatorMode = true,
                    toolType = SearchToolType.CALCULATOR,
                )
            SearchToolType.UNIT_CONVERTER ->
                CalculatorState(
                    isUnitConverterMode = true,
                    toolType = SearchToolType.UNIT_CONVERTER,
                )
            SearchToolType.DATE_CALCULATOR ->
                CalculatorState(
                    isDateCalculatorMode = true,
                    toolType = SearchToolType.DATE_CALCULATOR,
                )
            SearchToolType.COLOR_VISUALIZER ->
                CalculatorState(
                    isColorVisualizerMode = true,
                    toolType = SearchToolType.COLOR_VISUALIZER,
                )
        }

internal sealed interface AliasQueryResolution {
        data object None : AliasQueryResolution

        data class ReprocessQuery(
            val queryWithoutAlias: String,
        ) : AliasQueryResolution

        data class ExecuteSearchTarget(
            val queryWithoutAlias: String,
            val target: SearchTarget,
        ) : AliasQueryResolution
    }
