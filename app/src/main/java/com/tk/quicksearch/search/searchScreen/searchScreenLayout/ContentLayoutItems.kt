package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.tools.aiSearch.CurrencyConverterResult
import com.tk.quicksearch.tools.aiSearch.ColorVisualizerResult
import com.tk.quicksearch.tools.aiSearch.CalculatorResult
import com.tk.quicksearch.tools.aiSearch.DictionaryResult
import com.tk.quicksearch.tools.aiSearch.AiSearchResult
import com.tk.quicksearch.tools.aiSearch.WorldClockResult
import com.tk.quicksearch.tools.aiSearch.WeatherResult
import com.tk.quicksearch.search.searchScreen.InfoBanner
import com.tk.quicksearch.search.searchScreen.renderSection
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchResults

@Composable
internal fun NonSectionLayoutItem(
    itemType: ItemPriorityConfig.ItemType,
    state: SearchUiState,
    hasQuery: Boolean,
    isHomeCalendarExpanded: Boolean,
    hidePinnedAndAppsWhenSearchHistoryExpanded: Boolean,
    hasAtAGlanceSection: Boolean,
    atAGlanceRendered: Boolean,
    onAtAGlanceRendered: () -> Unit,
    isReversed: Boolean,
    shouldDeferSearchHistoryUntilAtAGlance: Boolean,
    deferredSearchHistoryRendered: Boolean,
    onDeferredSearchHistoryRendered: () -> Unit,
    renderSearchHistoryBlock: @Composable () -> Unit,
    hideHomeSectionTitleRows: Boolean,
    nowPlaying: NowPlayingGlance?,
    effectiveShowWallpaperBackground: Boolean,
    hasStandaloneTodayCalendarSection: Boolean,
    regularSectionParams: SectionRenderParams,
    appearedHomeContentKeys: MutableSet<String>,
    sectionContextForRecentHistoryExpansion: SectionRenderContext,
    atAGlanceContent: (@Composable (Boolean, Boolean) -> Unit)?,
    atAGlanceItems: List<AtAGlanceItem>,
    showCalculator: Boolean,
    showCurrencyConverter: Boolean,
    showWorldClock: Boolean,
    showDictionary: Boolean,
    showWeather: Boolean,
    onGeminiModelInfoClick: () -> Unit,
    onToggleOtherSearchItemPin: (OtherSearchItemId) -> Unit,
    showAiSearch: Boolean,
    aiSearchState: AiSearchState?,
    onOpenAiSearchConfigure: () -> Unit,
    onPhoneNumberClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    hideResults: Boolean,
    showWebSuggestions: Boolean,
    onWebSuggestionClick: (String) -> Unit,
    showRecentItems: Boolean,
    isUrlQuery: Boolean,
    queryLength: Int,
    inlineTargets: List<SearchTarget>,
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    onCustomizeSearchEnginesClick: () -> Unit,
    onSearchEngineLongPress: () -> Unit,
    predictedTarget: PredictedSubmitTarget?,
) {
        when (itemType) {
            ItemPriorityConfig.ItemType.UPCOMING_ALARM -> {
                if (
                    !hasQuery &&
                    !isHomeCalendarExpanded &&
                    !hidePinnedAndAppsWhenSearchHistoryExpanded &&
                    hasAtAGlanceSection &&
                    !atAGlanceRendered
                ) {
                    // Search history sits between the apps and At a Glance, so it renders on the
                    // far side of At a Glance from the apps grid in either layout direction.
                    if (isReversed && shouldDeferSearchHistoryUntilAtAGlance && !deferredSearchHistoryRendered) {
                        renderSearchHistoryBlock()
                        onDeferredSearchHistoryRendered()
                    }
                    // Media gets its own card on the search-bar side of the other At a Glance
                    // rows, and carries the section title whenever it comes first.
                    val showGlanceTitle = !hideHomeSectionTitleRows
                    val mediaCardFirst = nowPlaying != null && !isReversed
                    if (mediaCardFirst && nowPlaying != null) {
                        if (showGlanceTitle) AtAGlanceTitle()
                        NowPlayingCard(
                            glance = nowPlaying,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                        )
                    }
                    if (hasStandaloneTodayCalendarSection && regularSectionParams.calendarParams != null) {
                        HomeLoadingAnimatedContent(
                            animationKey = "home-today-calendar",
                            enabled = true,
                            appearedKeys = appearedHomeContentKeys,
                        ) {
                            renderSection(
                                section = SearchSection.CALENDAR,
                                params = regularSectionParams,
                                sectionContext = sectionContextForRecentHistoryExpansion.copy(
                                    shouldRenderCalendar = false,
                                    calendarEventsList = emptyList(),
                                    atAGlanceContent = atAGlanceContent,
                                    atAGlanceContentFirst = !isReversed,
                                    hideHomeSectionTitleRows = !showGlanceTitle || mediaCardFirst,
                                ),
                            )
                        }
                    } else {
                        AtAGlanceCard(
                            items = atAGlanceItems,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            showTitle = showGlanceTitle && !mediaCardFirst,
                        )
                    }
                    if (nowPlaying != null && isReversed) {
                        val hasOtherGlanceCard = hasStandaloneTodayCalendarSection || atAGlanceItems.isNotEmpty()
                        if (showGlanceTitle && !hasOtherGlanceCard) AtAGlanceTitle()
                        NowPlayingCard(
                            glance = nowPlaying,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                        )
                    }
                    onAtAGlanceRendered()
                    if (!isReversed && shouldDeferSearchHistoryUntilAtAGlance && !deferredSearchHistoryRendered) {
                        renderSearchHistoryBlock()
                        onDeferredSearchHistoryRendered()
                    }
                }
            }

            ItemPriorityConfig.ItemType.ERROR_BANNER -> {
                if (state.screenState is ScreenVisibilityState.Error) {
                    InfoBanner(
                        message =
                            (
                                state.screenState as
                                        ScreenVisibilityState.Error
                            ).message,
                    )
                }
            }

            ItemPriorityConfig.ItemType.CALCULATOR_RESULT -> {
                if (showCalculator) {
                    if (state.calculatorState.toolType == com.tk.quicksearch.search.core.SearchToolType.COLOR_VISUALIZER) {
                        ColorVisualizerResult(
                            calculatorState = state.calculatorState,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                        )
                    } else {
                        CalculatorResult(
                            calculatorState = state.calculatorState,
                            showWallpaperBackground =
                                effectiveShowWallpaperBackground,
                        )
                    }
                }
            }

            ItemPriorityConfig.ItemType.CURRENCY_CONVERTER_RESULT -> {
                if (showCurrencyConverter) {
                    CurrencyConverterResult(
                            currencyConverterState = state.currencyConverterState,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                    )
                }
            }

            ItemPriorityConfig.ItemType.WORD_CLOCK_RESULT -> {
                if (showWorldClock) {
                    WorldClockResult(
                            worldClockState = state.worldClockState,
                            llmProviderId = state.worldClockState.llmProviderId
                                    ?: state.aiSearchLlmProviderId,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            onGeminiModelInfoClick = onGeminiModelInfoClick,
                    )
                }
            }

            ItemPriorityConfig.ItemType.DICTIONARY_RESULT -> {
                if (showDictionary) {
                    DictionaryResult(
                            dictionaryState = state.dictionaryState,
                            llmProviderId = state.dictionaryState.llmProviderId
                                    ?: state.aiSearchLlmProviderId,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            onGeminiModelInfoClick = onGeminiModelInfoClick,
                    )
                }
            }

            ItemPriorityConfig.ItemType.WEATHER_RESULT -> {
                if (showWeather) {
                    WeatherResult(
                        weatherState = state.weatherState,
                        llmProviderId = state.weatherState.llmProviderId
                            ?: state.aiSearchLlmProviderId,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        onGeminiModelInfoClick = onGeminiModelInfoClick,
                    )
                }
            }

            ItemPriorityConfig.ItemType.OTHER_RESULTS -> {
                if (!hasQuery || !state.topMatchesEnabled) {
                    OtherSearchResults(
                        query = state.query,
                        pinnedItemOrder = state.pinnedNonAppItemOrder,
                        state = state.screenTimeState,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        iconPackPackage = state.selectedIconPackPackage,
                        onTogglePin = onToggleOtherSearchItemPin,
                    )
                }
            }

            ItemPriorityConfig.ItemType.AI_SEARCH_RESULT -> {
                if (showAiSearch && aiSearchState != null) {
                    AiSearchResult(
                        aiSearchState = aiSearchState,
                        aiSearchLlmProviderId = state.aiSearchLlmProviderId,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        onGeminiModelInfoClick = onGeminiModelInfoClick,
                        onOpenAiSearchConfigure = onOpenAiSearchConfigure,
                        onPhoneNumberClick = onPhoneNumberClick,
                        onEmailClick = onEmailClick,
                    )
                }
            }

            // --- Suggestions & Engines ---
            ItemPriorityConfig.ItemType.WEB_SUGGESTIONS -> {
                val allowWebSuggestions =
                    !hideResults || state.detectedShortcutTarget != null
                val isVisible = allowWebSuggestions && hasQuery && showWebSuggestions

                if (isVisible) {
                    WebSuggestionsSection(
                        suggestions = state.webSuggestions,
                        onSuggestionClick = onWebSuggestionClick,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        reverseOrder = isReversed,
                        isShortcutDetected = state.detectedShortcutTarget != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ItemPriorityConfig.ItemType.RECENT_QUERIES -> {
                val isVisible = !hideResults && showRecentItems
                if (isVisible) {
                    if (deferredSearchHistoryRendered) {
                        return
                    }
                    if (shouldDeferSearchHistoryUntilAtAGlance && !atAGlanceRendered) {
                        return
                    }
                    renderSearchHistoryBlock()
                    onDeferredSearchHistoryRendered()
                }
            }

            ItemPriorityConfig.ItemType.SEARCH_ENGINES_INLINE -> {
                // Inline search engines.
                // Condition: Not compact mode.
                val showInlineSearchEngines =
                    !hideResults &&
                        hasQuery &&
                        (isUrlQuery || queryLength > 1) &&
                        (!state.isSearchEngineCompactMode || isUrlQuery)

                if (showInlineSearchEngines) {
                    NoResultsSearchEngineCards(
                        query = state.query,
                        enabledEngines = inlineTargets,
                        onSearchEngineClick = onSearchTargetClick,
                        onCustomizeClick =
                        onCustomizeSearchEnginesClick,
                        onSearchEngineLongPress =
                        onSearchEngineLongPress,
                        showCustomizeCard = false,
                        isReversed = isReversed,
                        showWallpaperBackground =
                            effectiveShowWallpaperBackground,
                        predictedTarget = predictedTarget,
                        appIconShape = state.appIconShape,
                        iconPackPackage = state.selectedIconPackPackage,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ItemPriorityConfig.ItemType.SEARCH_ENGINES_COMPACT -> {
                // If we ever need to render compact engines in the list, do
                // it here.
                // Currently checking isSearchEngineCompactMode to HIDE
                // inline ones.
                // If compact engines are intended to be in the list, add
                // logic here.
                // For now, config doesn't use this in
                // SEARCHING_STATE_LAYOUT, but
                // we handle it for completeness.
            }

            ItemPriorityConfig.ItemType.NO_RESULTS_MESSAGE -> {
                if (!hideResults) {
                    NoResultsMessage(state)
                }
            }

            ItemPriorityConfig.ItemType.APPS_SECTION,
            ItemPriorityConfig.ItemType.APP_SHORTCUTS_SECTION,
            ItemPriorityConfig.ItemType.FILES_SECTION,
            ItemPriorityConfig.ItemType.CONTACTS_SECTION,
            ItemPriorityConfig.ItemType.SETTINGS_SECTION,
            ItemPriorityConfig.ItemType.CALENDAR_SECTION,
            ItemPriorityConfig.ItemType.REMINDERS_SECTION,
            ItemPriorityConfig.ItemType.NOTES_SECTION,
            ItemPriorityConfig.ItemType.APP_SETTINGS_SECTION,
            -> Unit
        }
}
