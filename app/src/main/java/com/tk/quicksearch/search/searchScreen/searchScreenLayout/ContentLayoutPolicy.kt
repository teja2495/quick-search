package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.ScreenVisibilityState
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SectionRenderParams

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.ItemPriorityConfig
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.searchScreen.TopMatchItem

internal fun homeLayoutOrder(
    baseLayoutOrder: List<ItemPriorityConfig.ItemType>,
    isReversed: Boolean,
    pinnedSectionOrder: List<SearchSection> = emptyList(),
): List<ItemPriorityConfig.ItemType> {
    val pinnedSectionRank = pinnedSectionOrder.withIndex().associate { (index, section) -> section to index }
    val logicalOrder =
        buildList {
            add(ItemPriorityConfig.ItemType.ERROR_BANNER)
            add(ItemPriorityConfig.ItemType.APPS_SECTION)
            add(ItemPriorityConfig.ItemType.UPCOMING_ALARM)
            add(ItemPriorityConfig.ItemType.RECENT_QUERIES)
            addAll(baseLayoutOrder.filter { it == ItemPriorityConfig.ItemType.OTHER_RESULTS })
            addAll(
                baseLayoutOrder
                    .filter { itemType ->
                        // Bookmarks and Noted items can't be pinned, so they have nothing to show on Home.
                        SearchSectionRegistry.sectionForItemType(itemType)
                            ?.let {
                                it != SearchSection.APPS &&
                                    it != SearchSection.BOOKMARKS &&
                                    it != SearchSection.NOTED
                            } == true
                    }.sortedBy { itemType ->
                        pinnedSectionRank[SearchSectionRegistry.sectionForItemType(itemType)]
                            ?: Int.MAX_VALUE
                    },
            )
        }
    return if (isReversed) logicalOrder.reversed() else logicalOrder
}

/**
 * Space under bottom-anchored search results. The app grid's labels already clear the search bar,
 * so only a result card in the bottom slot gets [resultCardGap].
 */
internal fun bottomAnchoredResultsGap(
    resultCardGap: Dp,
    state: SearchUiState,
    showTopMatchesSection: Boolean,
    topMatches: List<TopMatchItem>,
    rendersAppsSection: Boolean,
): Dp {
    if (resultCardGap == 0.dp) return 0.dp
    val endsWithAppGrid =
        if (showTopMatchesSection) {
            // Reversed top matches place their first match last.
            topMatches.firstOrNull() is TopMatchItem.AppGrid
        } else {
            // The error banner and other-result cards are the only items below the apps section.
            rendersAppsSection &&
                state.screenState !is ScreenVisibilityState.Error &&
                (
                    state.topMatchesEnabled ||
                        !OtherSearchItemRegistry.hasVisibleResult(
                            query = state.query,
                            pinnedItemOrder = state.pinnedNonAppItemOrder,
                            screenTimeState = state.screenTimeState,
                        )
                )
        }
    return if (endsWithAppGrid) 0.dp else resultCardGap
}

/**
 * Whether Home shows its Search History section (the RECENT_QUERIES layout item). Alias modes
 * suppress it because their recent items render in the section slot instead.
 */
internal fun shouldShowHomeSearchHistory(state: SearchUiState): Boolean =
    state.query.isBlank() &&
        state.detectedAliasSearchSection == null &&
        !state.isCurrencyConverterAliasMode &&
        !state.isWorldClockAliasMode &&
        !state.isDictionaryAliasMode &&
        !state.isWeatherAliasMode &&
        state.recentQueriesEnabled &&
        // The collapsed home history starts on the Searches tab. Recently opened
        // results alone must not create an empty Search History section.
        state.recentItems.any { it is RecentSearchItem.Query }

internal fun shouldRenderStandaloneTodayAgendaBeforeApps(isReversed: Boolean): Boolean = isReversed

internal fun shouldShowSearchHistoryTitle(hasAtAGlanceSection: Boolean): Boolean =
    hasAtAGlanceSection

internal fun shouldSkipRegularCalendarSectionForStandaloneTodayEvents(
    section: SearchSection,
    todayCalendarEventsCount: Int,
    pinnedCalendarEventsCount: Int,
): Boolean =
    section == SearchSection.CALENDAR &&
        todayCalendarEventsCount > 0 &&
        pinnedCalendarEventsCount == 0

/** Whether Top Matches already shows the regular app results, making the Apps section a duplicate. */
internal fun topMatchesRepeatApps(topMatches: List<TopMatchItem>, displayApps: List<AppInfo>): Boolean =
    topMatches.any { it is TopMatchItem.AppGrid && it.apps == displayApps }

internal fun regularSectionParams(
    sectionParams: SectionRenderParams,
    showTopMatches: Boolean,
): SectionRenderParams =
        if (showTopMatches) {
            sectionParams.copy(
                contactsParams = sectionParams.contactsParams.copy(predictedTarget = null),
                filesParams = sectionParams.filesParams.copy(predictedTarget = null),
                appShortcutsParams = sectionParams.appShortcutsParams?.copy(predictedTarget = null),
                settingsParams = sectionParams.settingsParams?.copy(predictedTarget = null),
                calendarParams = sectionParams.calendarParams?.copy(predictedTarget = null),
                notesParams = sectionParams.notesParams?.copy(predictedTarget = null),
                remindersParams = sectionParams.remindersParams,
                appsParams =
                    sectionParams.appsParams?.copy(
                        predictedTarget = null,
                        suppressTopResultIndicator = true,
                    ),
            )
        } else {
            sectionParams
        }
