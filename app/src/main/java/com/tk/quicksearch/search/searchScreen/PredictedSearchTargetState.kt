package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.other.OtherSearchItemRegistry

internal data class PredictedSearchTargetState(
    val isNonSubmittableSuggestionsTab: Boolean,
    val firstSubmittableGridApp: com.tk.quicksearch.search.models.AppInfo?,
    val suffixAliasMatchIgnoringTrailingSpace: Pair<String, SearchTarget>?,
    val isOtherSearchResultVisible: Boolean,
    val predictedTargetForIndicator: PredictedSubmitTarget?,
    val hideResultsForTopMatchSubmit: Boolean,
)

@Composable
internal fun rememberPredictedSearchTargetState(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    appsParams: AppsSectionParams,
    enabledTargets: List<SearchTarget>,
    isImeVisible: Boolean,
    isPhysicalKeyboardConnected: Boolean,
    showCurrencyConverterSearchCard: Boolean,
    showDictionarySearchCard: Boolean,
    showWeatherSearchCard: Boolean,
    showWorldClockSearchCard: Boolean,
    showCurrencyConverter: Boolean,
    showWorldClock: Boolean,
    showDictionary: Boolean,
    showWeather: Boolean,
): PredictedSearchTargetState {
    val context = LocalContext.current
    val shouldShowPredictedHighlight = isImeVisible
    val isNonSubmittableSuggestionsTab = appsParams.isNonSubmittableSuggestionsTab()
    val firstSubmittableGridApp =
            remember(
                    appsParams.isSearching,
                    appsParams.selectedSuggestionTab,
                    appsParams.apps,
                    appsParams.pinnedApps,
                    appsParams.pinnedAndRecentApps,
                    appsParams.mostUsedApps,
            ) {
                appsParams.firstSubmittableGridApp()
            }
    val predictedTarget =
            remember(
                    shouldShowPredictedHighlight,
                    state.query,
                    firstSubmittableGridApp,
                    renderingState.appShortcutResults,
                    renderingState.contactResults,
                    renderingState.fileResults,
                    renderingState.settingResults,
                    renderingState.calendarEvents,
                    renderingState.appSettingResults,
                    state.detectedShortcutTarget,
                    state.searchTargetsOrder,
                    enabledTargets,
            ) {
                if (!shouldShowPredictedHighlight) {
                    null
                } else {
                    val defaultBrowserPackage =
                            if (isLikelyWebUrl(state.query.trim())) {
                                resolveDefaultBrowserPackage(context)
                            } else {
                                null
                            }
                    resolvePredictedSubmitTarget(
                            query = state.query,
                            firstApp = firstSubmittableGridApp,
                            renderingState = renderingState,
                            enabledTargets = enabledTargets,
                            detectedShortcutTarget = state.detectedShortcutTarget,
                            searchTargetsOrder = state.searchTargetsOrder,
                            defaultBrowserPackage = defaultBrowserPackage,
                    )
                }
            }
    val suffixAliasMatchIgnoringTrailingSpace =
            remember(
                    state.query,
                    state.isSearchEngineAliasSuffixEnabled,
                    enabledTargets,
                    state.shortcutCodes,
                    state.shortcutEnabled,
            ) {
                if (!state.isSearchEngineAliasSuffixEnabled) {
                    null
                } else {
                    detectSuffixSearchTargetAlias(
                            query = state.query,
                            enabledTargets = enabledTargets,
                            shortcutCodes = state.shortcutCodes,
                            shortcutEnabled = state.shortcutEnabled,
                            requireTrailingSpace = false,
                    )
                }
            }
    val hasSuffixAliasKeywordAtQueryEnd = suffixAliasMatchIgnoringTrailingSpace != null
    val isOtherSearchResultVisible =
        OtherSearchItemRegistry.hasVisibleResult(
            query = state.query,
            pinnedItemOrder = state.pinnedNonAppItemOrder,
            screenTimeState = state.screenTimeState,
        )
    val shouldShowTopResultIndicator =
            state.openTopResultUsingKeyboardEnabled &&
                (state.topResultIndicatorEnabled || isPhysicalKeyboardConnected)
    val predictedTargetForIndicator =
            if (shouldShowTopResultIndicator &&
                    !isNonSubmittableSuggestionsTab &&
                    !isOtherSearchResultVisible &&
                    !showCurrencyConverterSearchCard &&
                    !showDictionarySearchCard &&
                    !showWeatherSearchCard &&
                    !showWorldClockSearchCard &&
                    !hasSuffixAliasKeywordAtQueryEnd) {
                predictedTarget
            } else null
    val hideResultsForTopMatchSubmit =
            state.AiSearchState.status != AiSearchStatus.Idle ||
                    state.calculatorState.isToolMode ||
                    state.calculatorState.result != null ||
                    state.calculatorState.parsedDateMillis != null ||
                    state.calculatorState.dateDiffLabel != null ||
                    state.calculatorState.timeResultLabel != null ||
                    showCurrencyConverter ||
                    showWorldClock ||
                    showDictionary ||
                    showWeather ||
                    state.detectedShortcutTarget != null ||
                    state.detectedAliasSearchSection != null ||
                    state.isCurrencyConverterAliasMode ||
                    state.isWorldClockAliasMode ||
                    state.isDictionaryAliasMode ||
                    state.isWeatherAliasMode ||
                    state.detectedCustomToolId != null
                    || state.detectedTaskerIntentId != null
    return PredictedSearchTargetState(
        isNonSubmittableSuggestionsTab,
        firstSubmittableGridApp,
        suffixAliasMatchIgnoringTrailingSpace,
        isOtherSearchResultVisible,
        predictedTargetForIndicator,
        hideResultsForTopMatchSubmit,
    )
}
