package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.renderSection
import com.tk.quicksearch.search.searchScreen.rememberSettledRegularSearchRenderingState
import com.tk.quicksearch.search.searchScreen.rememberSettledTopMatches
import com.tk.quicksearch.search.searchScreen.rememberTopMatches
import com.tk.quicksearch.search.searchScreen.shouldDeferTopMatchesForLocalSearch
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.RemindersSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.PinnedNonAppItemsSection
import com.tk.quicksearch.search.searchScreen.components.SectionPermissionResultCard
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.R
import com.tk.quicksearch.widgetsPanel.HomeWidgetStack
import com.tk.quicksearch.widgetsPanel.rememberHomePinnedWidgets
import com.tk.quicksearch.widgetsPanel.rememberHomeWidgetHost

/** Unified content layout that handles both one-handed mode and top-aligned layouts. */
@Composable
fun ContentLayout(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    renderingState: SectionRenderingState,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    remindersParams: RemindersSectionParams? = null,
    appsParams: AppsSectionParams,
    predictedTarget: PredictedSubmitTarget? = null,
    isPhysicalKeyboardConnected: Boolean,
    onRequestUsagePermission: () -> Unit,
    onOtherSearchItemAction: OtherSearchItemActionHandler,
    minContentHeight: Dp,
    expandedCardMaxHeight: Dp,
    isReversed: Boolean,
    hideResults: Boolean,
    showCalculator: Boolean = false,
    showCurrencyConverter: Boolean = false,
    showWorldClock: Boolean = false,
    showDictionary: Boolean = false,
    showWeather: Boolean = false,
    showAiSearch: Boolean = false,
    aiSearchState: AiSearchState? = null,
    isOverlayPresentation: Boolean = false,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenAiSearchConfigure: () -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onClearRecentItems: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
    isSearchHistoryExpanded: Boolean = false,
    onSearchHistoryExpandedChange: (Boolean) -> Unit = {},
    searchHistoryCollapseRequestKey: Int = 0,
    searchHistorySelectedTab: SearchHistoryTab = SearchHistoryTab.SEARCHES,
    onSearchHistorySelectedTabChange: (SearchHistoryTab) -> Unit = {},
    onOpenPermissionsSettings: () -> Unit = {},
    onHomePinnedSectionOrderChange: (List<SearchSection>) -> Unit = {},
    selectedTopMatchIndex: Int? = null,
    isScrollInProgress: () -> Boolean = { false },
) {
    val context = LocalContext.current
    val userPreferences = remember(context) { UserAppPreferences(context) }
    val effectiveContactsParams =
        contactsParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveFilesParams =
        filesParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveAppShortcutsParams =
        appShortcutsParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveSettingsParams =
        settingsParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveCalendarParams =
        calendarParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveNotesParams =
        notesParams.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val effectiveRemindersParams =
        remindersParams?.copy(
            predictedTarget = predictedTarget,
            expandedCardMaxHeight = expandedCardMaxHeight,
        )
    val hasQuery = state.query.isNotBlank()
    val isLocalSearchRefreshing =
        shouldDeferTopMatchesForLocalSearch(
            query = state.query,
            isAppSearchInProgress = state.isAppSearchInProgress,
            isSecondarySearchInProgress = state.isSecondarySearchInProgress,
        )
    val regularRenderingState =
        rememberSettledRegularSearchRenderingState(
            query = state.query,
            currentState = renderingState,
            isAppSearchRefreshing = state.isAppSearchInProgress,
            secondarySectionsRefreshing = state.secondarySearchSectionsInProgress,
        )
    // Home sections reserve their full height as soon as their data arrives and only fade in, so
    // nothing above them reflows while the rest of the agenda is still loading.
    var suggestionsAppGridHasAppeared by remember { mutableStateOf(false) }
    val appearedHomeContentKeys = remember { mutableSetOf<String>() }
    val effectiveAppsParams = appsParams.copy(
        predictedTarget = predictedTarget,
        onGridAppeared = {
            if (!hasQuery) {
                suggestionsAppGridHasAppeared = true
            }
        },
        // The suggestions are already in their final order when this grid is rendered. Animate
        // only its first appearance so the late section joins the agenda transition smoothly;
        // subsequent recompositions keep the settled grid fully visible.
        suppressSuggestionsEnterAnimation = suggestionsAppGridHasAppeared,
    )
    val regularAppsParams =
        if (hasQuery) {
            effectiveAppsParams.copy(
                apps = regularRenderingState.displayApps,
                hasAppResults = regularRenderingState.hasAppResults,
            )
        } else {
            effectiveAppsParams
        }

    // 1. Determine Layout Order based on ItemPriorityConfig
    val queryLength = state.query.trim().length
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
    val baseLayoutOrder = ItemPriorityConfig.getLayoutOrder(hasQuery)
    // reverseScrolling anchors content to the bottom but does not reverse child placement.
    val finalLayoutOrder =
        if (!hasQuery) {
            homeLayoutOrder(baseLayoutOrder, isReversed, state.homePinnedSectionOrder)
        } else if (isReversed) {
            baseLayoutOrder.reversed()
        } else {
            baseLayoutOrder
        }

    // 3. Prepare Shared Rendering Context and Params
    // We reuse the extracted logic to determine visibility and expansion states
    val sectionContext =
        rememberSectionRenderContext(
            state = state,
            renderingState = regularRenderingState,
            filesParams = effectiveFilesParams,
            contactsParams = effectiveContactsParams,
            settingsParams = effectiveSettingsParams,
            calendarParams = effectiveCalendarParams,
            notesParams = effectiveNotesParams,
            remindersParams = effectiveRemindersParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            appsParams = regularAppsParams,
            isSearching = hasQuery,
            oneHandedMode =
                state.oneHandedMode, // This affects list reversal inside helpers
        )
    val sectionParams =
        SectionRenderParams(
            renderingState = regularRenderingState,
            contactsParams = effectiveContactsParams,
            filesParams = effectiveFilesParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            settingsParams = effectiveSettingsParams,
            calendarParams = effectiveCalendarParams,
            notesParams = effectiveNotesParams,
            remindersParams = effectiveRemindersParams,
            appsParams = regularAppsParams,
            isReversed = isReversed,
        )

    val effectiveShowWallpaperBackground = state.showWallpaperBackground
    val inlineTargets =
        remember(
            state.searchTargetsOrder,
            state.disabledSearchTargetIds,
            isUrlQuery,
            context,
        ) {
            if (isUrlQuery) {
                orderedBrowserTargets(
                    targets = state.searchTargetsOrder,
                    defaultBrowserPackage = resolveDefaultBrowserPackage(context),
                )
            } else {
                state.searchTargetsOrder.filter { it.getId() !in state.disabledSearchTargetIds }
            }
        }

    // Pre-calculate common states
    val isExpanded = renderingState.expandedSection != ExpandedSection.NONE
    val hasAnySearchResults = hasAnySearchResults(state)
    // Web Suggestions Logic
    val suggestionsNotEmpty = state.webSuggestions.isNotEmpty()
    val suggestionsEnabled = state.webSuggestionsEnabled
    val suggestionWasSelected = state.webSuggestionWasSelected

    val showWebSuggestions =
        hasQuery &&
                !isUrlQuery &&
                !showAiSearch &&
                !showCalculator &&
                !showCurrencyConverter &&
                !showWorldClock &&
                !showDictionary &&
                !showWeather &&
                suggestionsNotEmpty &&
                suggestionsEnabled &&
                !suggestionWasSelected

    // Recent Queries Logic (for App Open State mainly, but CONFIG has RECENT_QUERIES item)
    // Suppress regular history in alias mode — alias recent items are shown in the section slot instead.
    val showRecentItems =
            !hasQuery &&
            state.detectedAliasSearchSection == null &&
            !state.isCurrencyConverterAliasMode &&
            !state.isWorldClockAliasMode &&
            !state.isDictionaryAliasMode &&
            !state.isWeatherAliasMode &&
            state.recentQueriesEnabled &&
            // The collapsed home history starts on the Searches tab. Recently opened
            // results alone must not create an empty Search History section.
            state.recentItems.any { it is RecentSearchItem.Query }

    // Hoisted to the screen so the screen-level layout (bottom alignment, one-handed mode) flips
    // in the same frame as this content. Mirroring a local flag upward through an effect lagged
    // by a frame, which flashed the app grid at the top on collapse before it dropped down.
    val searchHistoryExpanded = isSearchHistoryExpanded
    val currentOnSearchHistoryExpandedChange by rememberUpdatedState(onSearchHistoryExpandedChange)
    LaunchedEffect(showRecentItems) {
        if (!showRecentItems) currentOnSearchHistoryExpandedChange(false)
    }
    DisposableEffect(Unit) {
        onDispose { currentOnSearchHistoryExpandedChange(false) }
    }

    val hidePinnedAndAppsWhenSearchHistoryExpanded = showRecentItems && searchHistoryExpanded
    val isHomeCalendarExpanded =
        !hasQuery &&
            state.detectedAliasSearchSection == null &&
            renderingState.expandedSection == ExpandedSection.CALENDAR
    val hideHomeSectionTitleRows =
        !hasQuery && (searchHistoryExpanded || renderingState.expandedSection != ExpandedSection.NONE)
    val sectionContextForRecentHistoryExpansion =
        if (hidePinnedAndAppsWhenSearchHistoryExpanded) {
            sectionContext.copy(
                shouldRenderFiles = false,
                shouldRenderContacts = false,
                shouldRenderApps = false,
                shouldRenderAppShortcuts = false,
                shouldRenderSettings = false,
                shouldRenderCalendar = false,
                todayCalendarEventsList = emptyList(),
                shouldRenderNotes = false,
                hideHomeSectionTitleRows = true,
            )
        } else if (isHomeCalendarExpanded) {
            sectionContext.copy(
                shouldRenderFiles = false,
                shouldRenderContacts = false,
                shouldRenderApps = false,
                shouldRenderAppShortcuts = false,
                shouldRenderSettings = false,
                shouldRenderNotes = false,
                calendarEventsList = emptyList(),
                hideHomeSectionTitleRows = true,
            )
        } else if (hideHomeSectionTitleRows) {
            sectionContext.copy(hideHomeSectionTitleRows = true)
        } else {
            sectionContext
        }
    val activeAliasSection = state.detectedAliasSearchSection
    val isSectionAliasMode = activeAliasSection != null
    val showAliasRecentItems = isSectionAliasMode && !hasQuery && state.aliasRecentItems.isNotEmpty()
    val canDeferOtherContentForSuggestions =
        state.appSuggestionsEnabled &&
            !hasQuery &&
            !hideResults &&
            !isSectionAliasMode &&
            renderingState.shouldShowApps &&
            renderingState.expandedSection == ExpandedSection.NONE
    val waitingForSuggestions =
        canDeferOtherContentForSuggestions &&
            state.isInitializing &&
            state.recentApps.isEmpty() &&
            state.pinnedApps.isEmpty()
    // Keep pinned home content in the layout while the suggestions grid expands. Removing it
    // until the grid has appeared makes cards below the grid (such as Screen Time) pop back in
    // at their final position instead of being smoothly pushed down by the expanding grid.
    val hideOtherContent = waitingForSuggestions
    val topMatches =
        rememberTopMatches(
            query = state.query,
            renderingState = regularRenderingState,
            context = sectionContextForRecentHistoryExpansion,
            params = sectionParams,
            limit = state.topMatchesLimit,
            topMatchesSectionOrder = state.topMatchesSectionOrder,
            disabledTopMatchesSections = state.disabledTopMatchesSections,
            secondaryRankingSignal = state.secondaryRankingSignal,
            filterStaleCandidates = isLocalSearchRefreshing,
            otherSearchItemIds =
                OtherSearchItemRegistry.visibleSearchItemIds(
                    query = state.query,
                    pinnedItemOrder = state.pinnedNonAppItemOrder,
                    screenTimeState = state.screenTimeState,
                ),
        )
    val settledTopMatches =
        rememberSettledTopMatches(
            query = state.query,
            currentMatches = topMatches,
            isSearchRefreshing = isLocalSearchRefreshing,
            limit = state.topMatchesLimit,
        )
    val displayedTopMatches = settledTopMatches.matches
    val canShowTopMatches =
        state.topMatchesEnabled &&
            hasQuery &&
            !hideResults &&
            !isExpanded &&
            !isSectionAliasMode
    val showTopMatches = canShowTopMatches && displayedTopMatches.isNotEmpty()
    // Until a fresh query's top matches settle, the regular sections would render alone and then
    // get shoved aside (and the app grid swapped for the top matches grid) a few frames later.
    // Holding them back for that short window makes the whole result set appear at once.
    val holdRegularSectionsForTopMatches =
        canShowTopMatches && !settledTopMatches.isReady && displayedTopMatches.isEmpty()
    val showTopMatchesSection =
        canShowTopMatches && (showTopMatches || isLocalSearchRefreshing)
    val hasMoreResults =
        hasMoreResults(
            renderingState = regularRenderingState,
            sectionContext = sectionContextForRecentHistoryExpansion,
        )
    val regularSectionParams = regularSectionParams(sectionParams, showTopMatches)

    fun shouldRenderSection(section: SearchSection): Boolean {
        return if (isSectionAliasMode) {
            activeAliasSection == section
        } else {
            !hideResults
        }
    }

    val standaloneTodayEventIds =
        sectionContextForRecentHistoryExpansion.todayCalendarEventsList
            .map { it.eventId }
            .toSet()
    val hasStandaloneTodayCalendarSection = standaloneTodayEventIds.isNotEmpty()
    // Low battery, alarm, reminders and future At a Glance sources share the today's events card on
    // home; media controls get a card of their own.
    val atAGlanceItems =
        rememberAtAGlanceItems(
            enabled = !hasQuery,
            reversed = isReversed,
        )
    val atAGlanceContent: (@Composable (dividerBefore: Boolean, dividerAfter: Boolean) -> Unit)? =
        if (atAGlanceItems.isNotEmpty()) {
            { dividerBefore, dividerAfter ->
                AtAGlanceRows(
                    items = atAGlanceItems,
                    showWallpaperBackground = effectiveShowWallpaperBackground,
                    dividerBefore = dividerBefore,
                    dividerAfter = dividerAfter,
                )
            }
        } else {
            null
        }
    val nowPlaying = rememberNowPlayingGlance(enabled = !hasQuery)
    val hasAtAGlanceSection =
        hasStandaloneTodayCalendarSection || atAGlanceItems.isNotEmpty() || nowPlaying != null
    val pinnedCalendarEventsForPinnedBlock =
        if (!hasQuery && standaloneTodayEventIds.isNotEmpty()) {
            renderingState.pinnedCalendarEvents.filterNot { it.eventId in standaloneTodayEventIds }
        } else {
            renderingState.pinnedCalendarEvents
        }
    val showPinnedNonAppItems =
        !hasQuery &&
            state.unifiedPinnedItemsEnabled &&
            !hideResults &&
            !isSectionAliasMode &&
            !hidePinnedAndAppsWhenSearchHistoryExpanded &&
            !isHomeCalendarExpanded &&
            (
                renderingState.hasPinnedAppShortcuts ||
                    renderingState.hasPinnedContacts ||
                    renderingState.hasPinnedFiles ||
                    renderingState.hasPinnedSettings ||
                    pinnedCalendarEventsForPinnedBlock.isNotEmpty() ||
                    renderingState.hasPinnedNotes ||
                    renderingState.hasPinnedReminders
            )
    var pinnedNonAppItemsRendered = false
    var atAGlanceRendered = false
    var deferredSearchHistoryRendered = false
    val shouldDeferSearchHistoryUntilAtAGlance =
        showRecentItems && hasAtAGlanceSection
    val showSectionedPinnedHeaders =
        !hasQuery &&
            !state.unifiedPinnedItemsEnabled &&
            !hideResults &&
            !isSectionAliasMode &&
            !hideHomeSectionTitleRows
    // Same home states as the sectioned headers above, minus the pinned-mode split: an item-less
    // home section renders nothing either way, yet still takes a slot in the content Column's
    // arrangement spacing.
    val skipItemlessHomeSections =
        !hasQuery &&
            !hideResults &&
            !isSectionAliasMode &&
            !hideHomeSectionTitleRows

    var showPinnedSectionOrderDialog by rememberSaveable { mutableStateOf(false) }

    @Composable
    fun renderHomePinnedSection(section: SearchSection, content: @Composable () -> Unit) {
        HomePinnedSection(
            section = section,
            showSectionedPinnedHeaders = showSectionedPinnedHeaders,
            userPreferences = userPreferences,
            effectiveShowWallpaperBackground = effectiveShowWallpaperBackground,
            onShowOrderDialog = { showPinnedSectionOrderDialog = true },
            content = content,
        )
    }
    HomePinnedSectionOrderDialog(
        showPinnedSectionOrderDialog = showPinnedSectionOrderDialog,
        state = state,
        onHomePinnedSectionOrderChange = onHomePinnedSectionOrderChange,
        onDismiss = { showPinnedSectionOrderDialog = false },
    )

    @Composable
    fun renderSearchHistoryBlock() {
        HomeSearchHistoryBlock(
            state = state,
            renderingState = renderingState,
            isHomeCalendarExpanded = isHomeCalendarExpanded,
            hasQuery = hasQuery,
            appearedHomeContentKeys = appearedHomeContentKeys,
            hasAtAGlanceSection = hasAtAGlanceSection,
            effectiveContactsParams = effectiveContactsParams,
            effectiveFilesParams = effectiveFilesParams,
            effectiveSettingsParams = effectiveSettingsParams,
            effectiveAppShortcutsParams = effectiveAppShortcutsParams,
            effectiveCalendarParams = effectiveCalendarParams,
            effectiveNotesParams = effectiveNotesParams,
            effectiveRemindersParams = effectiveRemindersParams,
            notesParams = notesParams,
            onRecentQueryClick = onRecentQueryClick,
            onDeleteRecentItem = onDeleteRecentItem,
            onClearRecentItems = onClearRecentItems,
            searchHistoryExpanded = searchHistoryExpanded,
            onSearchHistoryExpandedChange = onSearchHistoryExpandedChange,
            searchHistoryCollapseRequestKey = searchHistoryCollapseRequestKey,
            expandedCardMaxHeight = expandedCardMaxHeight,
            effectiveShowWallpaperBackground = effectiveShowWallpaperBackground,
            isOverlayPresentation = isOverlayPresentation,
            searchHistorySelectedTab = searchHistorySelectedTab,
            onSearchHistorySelectedTabChange = onSearchHistorySelectedTabChange,
            showPinnedNonAppItems = showPinnedNonAppItems,
            pinnedNonAppItemsRendered = pinnedNonAppItemsRendered,
            onPinnedNonAppItemsRendered = { pinnedNonAppItemsRendered = true },
            userPreferences = userPreferences,
            pinnedCalendarEventsForPinnedBlock = pinnedCalendarEventsForPinnedBlock,
        )
    }

    @Composable
    fun renderTopMatches() {
        ContentLayoutTopMatches(
            showTopMatchesSection = showTopMatchesSection,
            displayedTopMatches = displayedTopMatches,
            sectionParams = sectionParams,
            effectiveShowWallpaperBackground = effectiveShowWallpaperBackground,
            state = state,
            isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
            isLocalSearchRefreshing = isLocalSearchRefreshing,
            selectedTopMatchIndex = selectedTopMatchIndex,
            isReversed = isReversed,
            onOtherSearchItemAction = onOtherSearchItemAction,
            showTopMatches = showTopMatches,
            hasMoreResults = hasMoreResults,
        )
    }

    @Composable
    fun renderLayoutItem(itemType: ItemPriorityConfig.ItemType) {
        val section = itemType.toSearchSectionOrNull()
        val isSectionItem = section != null

        // If a section is expanded, we hide all OTHER non-section items.
        if (isExpanded && !isSectionItem) return

        if (section != null) {
            // The expanded search history owns the whole content area. Skipping the sections
            // outright keeps them from emitting empty layout nodes, which would still take a
            // slot in this Column's arrangement spacing and push the card down.
            if (hidePinnedAndAppsWhenSearchHistoryExpanded) return
            if (isHomeCalendarExpanded && section != SearchSection.CALENDAR) return
            if (searchHistoryExpanded && section == SearchSection.NOTES) return
            if (!shouldRenderSection(section)) return
            if (holdRegularSectionsForTopMatches) return
            if (section == SearchSection.APPS && isUrlQuery) return
            if (hideOtherContent && section != SearchSection.APPS) return
            if (
                !hasQuery &&
                    shouldSkipRegularCalendarSectionForStandaloneTodayEvents(
                        section = section,
                        todayCalendarEventsCount = standaloneTodayEventIds.size,
                        pinnedCalendarEventsCount =
                            sectionContextForRecentHistoryExpansion.calendarEventsList.size,
                    ) && !isHomeCalendarExpanded
            ) {
                // Today's events are injected after the app grid. Rendering this otherwise empty
                // calendar slot as well produces a duplicate home-screen calendar card.
                return
            }

            if (!hasQuery && section != SearchSection.APPS && showPinnedNonAppItems && !showRecentItems) {
                if (!pinnedNonAppItemsRendered) {
                    UnifiedPinnedItemsBlock(
                        userPreferences = userPreferences,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                    ) {
                        PinnedNonAppItemsSection(
                            pinnedItemOrder = state.pinnedNonAppItemOrder,
                            contacts = renderingState.pinnedContacts,
                            files = renderingState.pinnedFiles,
                            appShortcuts = renderingState.pinnedAppShortcuts,
                            settings = renderingState.pinnedSettings,
                            calendarEvents = pinnedCalendarEventsForPinnedBlock,
                            notes = renderingState.pinnedNotes,
                            reminders = renderingState.pinnedReminders,
                            contactsParams = effectiveContactsParams,
                            filesParams = effectiveFilesParams,
                            appShortcutsParams = effectiveAppShortcutsParams,
                            settingsParams = effectiveSettingsParams,
                            calendarParams = effectiveCalendarParams,
                            notesParams = effectiveNotesParams,
                            remindersParams = effectiveRemindersParams,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    pinnedNonAppItemsRendered = true
                }
                return
            }

            val showAliasRecentForSection =
                showAliasRecentItems && section in ALIAS_RECENT_ELIGIBLE_SECTIONS

            if (showAliasRecentForSection) {
                AliasRecentItemsSection(
                    items = state.aliasRecentItems,
                    contactsParams = effectiveContactsParams,
                    filesParams = effectiveFilesParams,
                    settingsParams = effectiveSettingsParams,
                    appShortcutsParams = effectiveAppShortcutsParams,
                    notesParams = notesParams,
                    remindersParams = remindersParams,
                    onRecentQueryClick = onRecentQueryClick,
                    onDeleteRecentItem = onDeleteRecentItem,
                    expandedCardMaxHeight = expandedCardMaxHeight,
                    showWallpaperBackground = effectiveShowWallpaperBackground,
                    isOverlayPresentation = isOverlayPresentation,
                )
            }
            if (showAliasRecentForSection) return

            val permissionMessageRes =
                sectionAliasPermissionMessageRes(
                    state = state,
                    section = section,
                    isSectionAliasMode = isSectionAliasMode,
                )
            if (permissionMessageRes != null) {
                SectionPermissionResultCard(
                    title = stringResource(R.string.permission_required_title),
                    message = stringResource(permissionMessageRes),
                    showWallpaperBackground = effectiveShowWallpaperBackground,
                    onActionClick = onOpenPermissionsSettings,
                )
                return
            }

            val sectionContext =
                if (
                    section == SearchSection.CALENDAR &&
                    !hasQuery &&
                    !state.unifiedPinnedItemsEnabled &&
                    !isHomeCalendarExpanded &&
                    hasStandaloneTodayCalendarSection
                ) {
                    sectionContextForRecentHistoryExpansion.copy(
                        todayCalendarEventsList = emptyList(),
                    )
                } else {
                    sectionContextForRecentHistoryExpansion
                }
            if (
                skipItemlessHomeSections &&
                section.supportsPinnedHomeCollapse() &&
                !homePinnedSectionHasItems(section, sectionContext)
            ) {
                return
            }
            val homeSectionContentReady =
                section != SearchSection.APPS ||
                    (
                        sectionContext.shouldRenderApps &&
                            (
                                effectiveAppsParams.hasAppResults && effectiveAppsParams.apps.isNotEmpty() ||
                                    effectiveAppsParams.showAllAppsButton && effectiveAppsParams.allApps.isNotEmpty()
                            )
                    )
            if (homeSectionContentReady) {
                HomeLoadingAnimatedContent(
                    animationKey = "home-section-${section.name}",
                    enabled = !hasQuery && !isHomeCalendarExpanded,
                    appearedKeys = appearedHomeContentKeys,
                ) {
                    renderHomePinnedSection(section) {
                        renderSection(section, regularSectionParams, sectionContext)
                    }
                }
            }
            return
        }

        if (hideOtherContent) return

        NonSectionLayoutItem(
            itemType = itemType,
            state = state,
            hasQuery = hasQuery,
            isHomeCalendarExpanded = isHomeCalendarExpanded,
            hidePinnedAndAppsWhenSearchHistoryExpanded = hidePinnedAndAppsWhenSearchHistoryExpanded,
            hasAtAGlanceSection = hasAtAGlanceSection,
            atAGlanceRendered = atAGlanceRendered,
            onAtAGlanceRendered = { atAGlanceRendered = true },
            isReversed = isReversed,
            shouldDeferSearchHistoryUntilAtAGlance = shouldDeferSearchHistoryUntilAtAGlance,
            deferredSearchHistoryRendered = deferredSearchHistoryRendered,
            onDeferredSearchHistoryRendered = { deferredSearchHistoryRendered = true },
            renderSearchHistoryBlock = { renderSearchHistoryBlock() },
            hideHomeSectionTitleRows = hideHomeSectionTitleRows,
            nowPlaying = nowPlaying,
            effectiveShowWallpaperBackground = effectiveShowWallpaperBackground,
            hasStandaloneTodayCalendarSection = hasStandaloneTodayCalendarSection,
            regularSectionParams = regularSectionParams,
            appearedHomeContentKeys = appearedHomeContentKeys,
            sectionContextForRecentHistoryExpansion = sectionContextForRecentHistoryExpansion,
            atAGlanceContent = atAGlanceContent,
            atAGlanceItems = atAGlanceItems,
            showCalculator = showCalculator,
            showCurrencyConverter = showCurrencyConverter,
            showWorldClock = showWorldClock,
            showDictionary = showDictionary,
            showWeather = showWeather,
            onGeminiModelInfoClick = onGeminiModelInfoClick,
            onOtherSearchItemAction = onOtherSearchItemAction,
            showAiSearch = showAiSearch,
            aiSearchState = aiSearchState,
            onOpenAiSearchConfigure = onOpenAiSearchConfigure,
            onPhoneNumberClick = onPhoneNumberClick,
            onEmailClick = onEmailClick,
            hideResults = hideResults,
            showWebSuggestions = showWebSuggestions,
            onWebSuggestionClick = onWebSuggestionClick,
            showRecentItems = showRecentItems,
            isUrlQuery = isUrlQuery,
            queryLength = queryLength,
            inlineTargets = inlineTargets,
            onSearchTargetClick = onSearchTargetClick,
            onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            predictedTarget = predictedTarget,
        )
    }

    @Composable
    fun renderDeferredSearchHistory() {
        if (shouldDeferSearchHistoryUntilAtAGlance && !deferredSearchHistoryRendered && showRecentItems) {
            renderSearchHistoryBlock()
            deferredSearchHistoryRendered = true
        }
    }

    val homeWidgets = rememberHomePinnedWidgets(enabled = !isOverlayPresentation)
    // The container only depends on whether any widget is pinned, so typing a query keeps the same
    // layout tree instead of rebuilding every section.
    val homeWidgetHost = if (homeWidgets.isNotEmpty()) rememberHomeWidgetHost() else null
    val showHomeWidgets =
        !hasQuery &&
            !hideResults &&
            !isSectionAliasMode &&
            !isExpanded &&
            !hideOtherContent &&
            !hidePinnedAndAppsWhenSearchHistoryExpanded &&
            !isHomeCalendarExpanded

    if (homeWidgetHost != null) {
        HomeWidgetStack(
            layoutOrder = finalLayoutOrder,
            isReversed = isReversed,
            widgets = homeWidgets,
            showWidgets = showHomeWidgets,
            host = homeWidgetHost,
            spacing = 14.dp,
            isScrollInProgress = isScrollInProgress,
            modifier = modifier,
            leadingContent = {
                if (showTopMatchesSection && !isReversed) renderTopMatches()
            },
            trailingContent = {
                renderDeferredSearchHistory()
                if (showTopMatchesSection && isReversed) renderTopMatches()
            },
            itemContent = { itemType -> renderLayoutItem(itemType) },
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showTopMatchesSection && !isReversed) {
            renderTopMatches()
        }
        finalLayoutOrder.forEach { itemType -> renderLayoutItem(itemType) }

        renderDeferredSearchHistory()

        if (showTopMatchesSection && isReversed) {
            renderTopMatches()
        }
    }
}
