package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.searchHistory.SearchHistoryTab
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.homeTextColor
import com.tk.quicksearch.tools.aiSearch.CurrencyConverterResult
import com.tk.quicksearch.tools.aiSearch.ColorVisualizerResult
import com.tk.quicksearch.tools.aiSearch.CalculatorResult
import com.tk.quicksearch.tools.aiSearch.DictionaryResult
import com.tk.quicksearch.tools.aiSearch.AiSearchResult
import com.tk.quicksearch.tools.aiSearch.WorldClockResult
import com.tk.quicksearch.tools.aiSearch.WeatherResult
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.InfoBanner
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.renderSection
import com.tk.quicksearch.search.searchScreen.rememberSettledRegularSearchRenderingState
import com.tk.quicksearch.search.searchScreen.rememberSettledTopMatches
import com.tk.quicksearch.search.searchScreen.rememberTopMatches
import com.tk.quicksearch.search.searchScreen.shouldDeferTopMatchesForLocalSearch
import com.tk.quicksearch.search.searchScreen.TopMatchesSection
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.PinnedNonAppItemsSection
import com.tk.quicksearch.search.searchScreen.components.SectionPermissionResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchResults
import com.tk.quicksearch.R
import com.tk.quicksearch.app.startup.StartupTrace

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
    appsParams: AppsSectionParams,
    predictedTarget: PredictedSubmitTarget? = null,
    isPhysicalKeyboardConnected: Boolean,
    onRequestUsagePermission: () -> Unit,
    onToggleOtherSearchItemPin: (OtherSearchItemId) -> Unit,
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
    onSearchHistoryExpandedChange: (Boolean) -> Unit = {},
    searchHistoryCollapseRequestKey: Int = 0,
    searchHistorySelectedTab: SearchHistoryTab = SearchHistoryTab.SEARCHES,
    onSearchHistorySelectedTabChange: (SearchHistoryTab) -> Unit = {},
    onOpenPermissionsSettings: () -> Unit = {},
    selectedTopMatchIndex: Int? = null,
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
    // The overlay already animates its full surface on entry. In one-handed mode, layering every
    // async Home section height animation on top of that bottom-anchored surface makes early
    // content briefly reflow in the opposite direction. App suggestions are the exception: they
    // arrive after the agenda is visible, so their height must expand instead of being inserted in
    // one frame and jumping the agenda to its final position.
    val animateHomeLoadingContent = !(isOverlayPresentation && state.oneHandedMode)
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
            homeLayoutOrder(baseLayoutOrder, isReversed)
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

    var searchHistoryExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(showRecentItems) {
        if (!showRecentItems) searchHistoryExpanded = false
    }
    LaunchedEffect(searchHistoryExpanded) {
        onSearchHistoryExpandedChange(searchHistoryExpanded)
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
    val showTopMatchesSection =
        canShowTopMatches && (showTopMatches || isLocalSearchRefreshing)
    val hasMoreResults =
        hasMoreResults(
            renderingState = regularRenderingState,
            sectionContext = sectionContextForRecentHistoryExpansion,
        )
    val regularSectionParams =
        if (showTopMatches) {
            sectionParams.copy(
                contactsParams = sectionParams.contactsParams.copy(predictedTarget = null),
                filesParams = sectionParams.filesParams.copy(predictedTarget = null),
                appShortcutsParams = sectionParams.appShortcutsParams?.copy(predictedTarget = null),
                settingsParams = sectionParams.settingsParams?.copy(predictedTarget = null),
                calendarParams = sectionParams.calendarParams?.copy(predictedTarget = null),
                notesParams = sectionParams.notesParams?.copy(predictedTarget = null),
                appsParams =
                    sectionParams.appsParams?.copy(
                        predictedTarget = null,
                        suppressTopResultIndicator = true,
                    ),
            )
        } else {
            sectionParams
        }

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
                    renderingState.hasPinnedNotes
            )
    var pinnedNonAppItemsRendered = false
    var standaloneTodayCalendarRendered = false
    var deferredSearchHistoryRendered = false
    val shouldDeferSearchHistoryUntilTodayEvents =
        showRecentItems && hasStandaloneTodayCalendarSection
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

    @Composable
    fun renderHomePinnedSection(
        section: SearchSection,
        content: @Composable () -> Unit,
    ) {
        if (!showSectionedPinnedHeaders || !section.supportsPinnedHomeCollapse()) {
            content()
            return
        }

        var isExpanded by rememberSaveable(section.name) { mutableStateOf(true) }
        LaunchedEffect(section) {
            isExpanded = userPreferences.isHomePinnedSectionExpanded(section)
        }
        val interactionSource = remember { MutableInteractionSource() }
        val metadata = SearchSectionUiMetadataRegistry.metadataFor(section)
        val sectionIcon = metadata.settingsIcon
        val toggleExpanded = {
            val newExpanded = !isExpanded
            isExpanded = newExpanded
            userPreferences.setHomePinnedSectionExpanded(section, newExpanded)
        }
        val headerContent: @Composable (Modifier) -> Unit = { modifier ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = toggleExpanded,
                    )
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingXXSmall,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isExpanded) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = homeTextColor(),
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                    }
                    Text(
                        text = stringResource(metadata.sectionLabelRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = homeTextColor(),
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
        ) {
            if (isExpanded) {
                headerContent(Modifier)
            } else {
                SearchResultCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    showWallpaperBackground = effectiveShowWallpaperBackground,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        headerContent(
                            Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                content()
            }
        }
    }

    fun homePinnedSectionHasItems(
        section: SearchSection,
        sectionContext: SectionRenderContext,
    ): Boolean =
        when (section) {
            SearchSection.APP_SHORTCUTS ->
                sectionContext.shouldRenderAppShortcuts && sectionContext.appShortcutsList.isNotEmpty()
            SearchSection.CONTACTS ->
                sectionContext.shouldRenderContacts && sectionContext.contactsList.isNotEmpty()
            SearchSection.FILES ->
                sectionContext.shouldRenderFiles && sectionContext.filesList.isNotEmpty()
            SearchSection.SETTINGS ->
                sectionContext.shouldRenderSettings &&
                    !sectionContext.isAppSettingsExpanded &&
                    sectionContext.settingsList.isNotEmpty()
            SearchSection.CALENDAR ->
                (sectionContext.shouldRenderCalendar && sectionContext.calendarEventsList.isNotEmpty()) ||
                    (sectionContext.isHomeScreenCalendarMode &&
                        sectionContext.todayCalendarEventsList.isNotEmpty())
            SearchSection.NOTES ->
                sectionContext.shouldRenderNotes && sectionContext.notesList.isNotEmpty()
            SearchSection.APPS, SearchSection.APP_SETTINGS -> true
        }

    @Composable
    fun renderSearchHistoryBlock() {
        if (isHomeCalendarExpanded) return
        LaunchedEffect(Unit) { StartupTrace.mark("QS.Home.SearchHistoryRendered") }
        HomeLoadingAnimatedContent(
            animationKey = "home-search-history",
            enabled = !hasQuery && animateHomeLoadingContent,
            appearedKeys = appearedHomeContentKeys,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                if (
                    shouldShowSearchHistoryTitle(
                        sectionContextForRecentHistoryExpansion.todayCalendarEventsList.isNotEmpty(),
                    )
                ) {
                    Text(
                        text = stringResource(R.string.recent_queries_toggle_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = homeTextColor(),
                        modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                    )
                }
                SearchHistorySection(
                    items = state.recentItems,
                    callingApp =
                        effectiveContactsParams.callingApp
                            ?: CallingApp.CALL,
                    messagingApp =
                        effectiveContactsParams.messagingApp
                            ?: MessagingApp
                                .MESSAGES,
                    onRecentQueryClick =
                    onRecentQueryClick,
                    onContactClick =
                        effectiveContactsParams
                            .onContactClick,
                    onShowContactMethods =
                        effectiveContactsParams
                            .onShowContactMethods,
                    onCallContact =
                        effectiveContactsParams
                            .onCallContact,
                    onSmsContact =
                        effectiveContactsParams.onSmsContact,
                    onContactMethodClick =
                        effectiveContactsParams
                            .onContactMethodClick,
                    getPrimaryContactCardAction =
                        effectiveContactsParams
                            .getPrimaryContactCardAction,
                    getSecondaryContactCardAction =
                        effectiveContactsParams
                            .getSecondaryContactCardAction,
                    onPrimaryActionLongPress =
                        effectiveContactsParams
                            .onPrimaryActionLongPress,
                    onSecondaryActionLongPress =
                        effectiveContactsParams
                            .onSecondaryActionLongPress,
                    onCustomAction =
                        effectiveContactsParams
                            .onCustomAction,
                    onFileClick =
                        effectiveFilesParams.onFileClick,
                    onSettingClick =
                        effectiveSettingsParams
                            .onSettingClick,
                    onAppShortcutClick =
                        effectiveAppShortcutsParams
                            .onShortcutClick,
                    onNoteClick = notesParams.onNoteClick,
                    onDeleteRecentItem =
                    onDeleteRecentItem,
                    onClearRecentItems = onClearRecentItems,
                    isExpanded = searchHistoryExpanded,
                    collapsedItemCount = state.recentQueriesDisplayCount,
                    reverseCollapsedItems = state.oneHandedMode,
                    onExpandedChange = { searchHistoryExpanded = it },
                    collapseRequestKey = searchHistoryCollapseRequestKey,
                    expandedCardMaxHeight = expandedCardMaxHeight,
                    showWallpaperBackground =
                        effectiveShowWallpaperBackground,
                    isOverlayPresentation = isOverlayPresentation,
                    showInlineCollapseButton = false,
                    selectedTab = searchHistorySelectedTab,
                    onSelectedTabChange = onSearchHistorySelectedTabChange,
                    modifier = Modifier.fillMaxWidth(),
                    )
                if (showPinnedNonAppItems && !pinnedNonAppItemsRendered) {
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
                            contactsParams = effectiveContactsParams,
                            filesParams = effectiveFilesParams,
                            appShortcutsParams = effectiveAppShortcutsParams,
                            settingsParams = effectiveSettingsParams,
                            calendarParams = effectiveCalendarParams,
                            notesParams = effectiveNotesParams,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            modifier = Modifier.fillMaxWidth(),
                            )
                    }
                    pinnedNonAppItemsRendered = true
                }
            }
        }
    }

    @Composable
    fun renderTopMatches() {
        if (!showTopMatchesSection) return
        TopMatchesSection(
            matches = displayedTopMatches,
            params = sectionParams,
            showWallpaperBackground = effectiveShowWallpaperBackground,
            showTopResultIndicator =
                state.topResultIndicatorEnabled || isPhysicalKeyboardConnected,
            selectedMatchIndex = selectedTopMatchIndex,
            reverseOrder = isReversed,
            screenTimeState = state.screenTimeState,
            pinnedNonAppItemOrder = state.pinnedNonAppItemOrder,
            iconPackPackage = state.selectedIconPackPackage,
            onToggleOtherSearchItemPin = onToggleOtherSearchItemPin,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showTopMatches && hasMoreResults && !isReversed) {
            Text(
                text = stringResource(R.string.more_results_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingXSmall,
                    ),
            )
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showTopMatchesSection && !isReversed) {
            renderTopMatches()
        }
        finalLayoutOrder.forEach { itemType ->
            val section = itemType.toSearchSectionOrNull()
            val isSectionItem = section != null

            // If a section is expanded, we hide all OTHER non-section items.
            if (isExpanded && !isSectionItem) return@forEach

            if (section != null) {
                // The expanded search history owns the whole content area. Skipping the sections
                // outright keeps them from emitting empty layout nodes, which would still take a
                // slot in this Column's arrangement spacing and push the card down.
                if (hidePinnedAndAppsWhenSearchHistoryExpanded) return@forEach
                if (isHomeCalendarExpanded && section != SearchSection.CALENDAR) return@forEach
                if (searchHistoryExpanded && section == SearchSection.NOTES) return@forEach
                if (!shouldRenderSection(section)) return@forEach
                if (section == SearchSection.APPS && isUrlQuery) return@forEach
                if (hideOtherContent && section != SearchSection.APPS) return@forEach
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
                    return@forEach
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
                                contactsParams = effectiveContactsParams,
                                filesParams = effectiveFilesParams,
                                appShortcutsParams = effectiveAppShortcutsParams,
                                settingsParams = effectiveSettingsParams,
                                calendarParams = effectiveCalendarParams,
                                notesParams = effectiveNotesParams,
                                showWallpaperBackground = effectiveShowWallpaperBackground,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        pinnedNonAppItemsRendered = true
                    }
                    return@forEach
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
                        onRecentQueryClick = onRecentQueryClick,
                        onDeleteRecentItem = onDeleteRecentItem,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        isOverlayPresentation = isOverlayPresentation,
                    )
                }
                if (showAliasRecentForSection) return@forEach

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
                    return@forEach
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
                    return@forEach
                }
                if (
                    shouldRenderStandaloneTodayAgendaBeforeApps(isReversed) &&
                        section == SearchSection.APPS &&
                        hasStandaloneTodayCalendarSection &&
                        !standaloneTodayCalendarRendered
                ) {
                    if (shouldDeferSearchHistoryUntilTodayEvents && !deferredSearchHistoryRendered) {
                        renderSearchHistoryBlock()
                        deferredSearchHistoryRendered = true
                    }
                    HomeLoadingAnimatedContent(
                        animationKey = "home-today-calendar",
                        enabled = animateHomeLoadingContent,
                        appearedKeys = appearedHomeContentKeys,
                    ) {
                        renderSection(
                            section = SearchSection.CALENDAR,
                            params = regularSectionParams,
                            sectionContext =
                                sectionContextForRecentHistoryExpansion.copy(
                                    shouldRenderCalendar = false,
                                    calendarEventsList = emptyList(),
                                ),
                        )
                    }
                    standaloneTodayCalendarRendered = true
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
                        enabled =
                            !hasQuery &&
                                !isHomeCalendarExpanded &&
                                (animateHomeLoadingContent || section == SearchSection.APPS),
                        fadeContent = section != SearchSection.APPS,
                        appearedKeys = appearedHomeContentKeys,
                    ) {
                        renderHomePinnedSection(section) {
                            renderSection(section, regularSectionParams, sectionContext)
                        }
                    }
                }
                if (
                    !isReversed &&
                    section == SearchSection.APPS &&
                        hasStandaloneTodayCalendarSection &&
                        !standaloneTodayCalendarRendered
                ) {
                    HomeLoadingAnimatedContent(
                        animationKey = "home-today-calendar",
                        enabled = animateHomeLoadingContent,
                        appearedKeys = appearedHomeContentKeys,
                    ) {
                        renderSection(
                            section = SearchSection.CALENDAR,
                            params = regularSectionParams,
                            sectionContext =
                                sectionContextForRecentHistoryExpansion.copy(
                                    shouldRenderCalendar = false,
                                    calendarEventsList = emptyList(),
                                ),
                        )
                    }
                    standaloneTodayCalendarRendered = true
                    if (
                        shouldDeferSearchHistoryUntilTodayEvents &&
                            !deferredSearchHistoryRendered
                    ) {
                        renderSearchHistoryBlock()
                        deferredSearchHistoryRendered = true
                    }
                }
                return@forEach
            }

            if (hideOtherContent) return@forEach

            when (itemType) {
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
                            return@forEach
                        }
                        if (shouldDeferSearchHistoryUntilTodayEvents && !standaloneTodayCalendarRendered) {
                            return@forEach
                        }
                        renderSearchHistoryBlock()
                        deferredSearchHistoryRendered = true
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
                ItemPriorityConfig.ItemType.NOTES_SECTION,
                ItemPriorityConfig.ItemType.APP_SETTINGS_SECTION,
                -> Unit
            }
        }

        if (shouldDeferSearchHistoryUntilTodayEvents && !deferredSearchHistoryRendered && showRecentItems) {
            renderSearchHistoryBlock()
            deferredSearchHistoryRendered = true
        }

        if (showTopMatchesSection && isReversed) {
            renderTopMatches()
        }
    }
}

/**
 * Animates home sections from zero height when their asynchronously loaded data first arrives.
 * Expanding the section height also moves every section below it, so late app suggestions push
 * an already-visible agenda down instead of making it jump to its final position.
 */
@Composable
private fun HomeLoadingAnimatedContent(
    animationKey: String,
    enabled: Boolean,
    fadeContent: Boolean = true,
    appearedKeys: MutableSet<String>,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val shouldAnimate = remember(animationKey) { appearedKeys.add(animationKey) }
    if (!shouldAnimate) {
        content()
        return
    }

    var visible by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter =
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = HomeSectionExpandDurationMillis),
            ) +
                if (fadeContent) {
                    fadeIn(animationSpec = tween(durationMillis = HomeSectionFadeDurationMillis))
                } else {
                    androidx.compose.animation.EnterTransition.None
                },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HomeSectionContentSpacing),
        ) {
            content()
        }
    }
}

private const val HomeSectionFadeDurationMillis = 180
private const val HomeSectionExpandDurationMillis = 220
private val HomeSectionContentSpacing = 14.dp

private fun hasMoreResults(
    renderingState: SectionRenderingState,
    sectionContext: SectionRenderContext,
): Boolean =
    (sectionContext.shouldRenderApps && renderingState.displayApps.isNotEmpty()) ||
        (sectionContext.shouldRenderAppShortcuts && sectionContext.appShortcutsList.isNotEmpty()) ||
        (sectionContext.shouldRenderContacts && sectionContext.contactsList.isNotEmpty()) ||
        (sectionContext.shouldRenderFiles && sectionContext.filesList.isNotEmpty()) ||
        (sectionContext.shouldRenderSettings && sectionContext.settingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderAppSettings && sectionContext.appSettingsList.isNotEmpty()) ||
        (sectionContext.shouldRenderCalendar && sectionContext.calendarEventsList.isNotEmpty()) ||
        (sectionContext.shouldRenderNotes && sectionContext.notesList.isNotEmpty())

private fun SearchSection.supportsPinnedHomeCollapse(): Boolean =
    when (this) {
        SearchSection.APPS, SearchSection.APP_SETTINGS -> false
        SearchSection.APP_SHORTCUTS,
        SearchSection.CONTACTS,
        SearchSection.FILES,
        SearchSection.SETTINGS,
        SearchSection.CALENDAR,
        SearchSection.NOTES,
        -> true
    }

@Composable
private fun UnifiedPinnedItemsBlock(
    userPreferences: UserAppPreferences,
    showWallpaperBackground: Boolean,
    content: @Composable () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isExpanded = userPreferences.isUnifiedPinnedItemsExpanded()
    }
    val interactionSource = remember { MutableInteractionSource() }
    val toggleExpanded = {
        val newExpanded = !isExpanded
        isExpanded = newExpanded
        userPreferences.setUnifiedPinnedItemsExpanded(newExpanded)
    }

    val headerContent: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = toggleExpanded,
                )
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingXXSmall,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_suggestions_tab_pinned),
                style = MaterialTheme.typography.titleSmall,
                color = homeTextColor(),
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
    ) {
        if (isExpanded) {
            headerContent(Modifier)
        } else {
            SearchResultCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                showWallpaperBackground = showWallpaperBackground,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    headerContent(
                        Modifier.padding(horizontal = DesignTokens.SpacingLarge),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            content()
        }
    }
}

private val ALIAS_RECENT_ELIGIBLE_SECTIONS =
    setOf(
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS,
        SearchSection.APP_SETTINGS,
        SearchSection.NOTES,
    )

private fun ItemPriorityConfig.ItemType.toSearchSectionOrNull(): SearchSection? =
    SearchSectionRegistry.sectionForItemType(this)

private fun sectionAliasPermissionMessageRes(
    state: SearchUiState,
    section: SearchSection,
    isSectionAliasMode: Boolean,
): Int? {
    if (!isSectionAliasMode) return null
    return when (section) {
        SearchSection.CONTACTS ->
            if (state.contactsSectionState is ContactsSectionVisibility.NoPermission) {
                R.string.contacts_section_permission_subtitle
            } else {
                null
            }
        SearchSection.FILES ->
            if (state.filesSectionState is FilesSectionVisibility.NoPermission) {
                R.string.files_section_permission_subtitle
            } else {
                null
            }
        SearchSection.CALENDAR ->
            if (state.calendarSectionState is CalendarSectionVisibility.NoPermission) {
                R.string.calendar_section_permission_subtitle
            } else {
                null
            }
        else -> null
    }
}

@Composable
private fun AliasRecentItemsSection(
    items: List<com.tk.quicksearch.search.searchHistory.RecentSearchItem>,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    notesParams: NotesSectionParams,
    onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    expandedCardMaxHeight: Dp,
    showWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
) {
    SearchHistorySection(
        items = items,
        callingApp = contactsParams.callingApp ?: CallingApp.CALL,
        messagingApp = contactsParams.messagingApp ?: MessagingApp.MESSAGES,
        onRecentQueryClick = onRecentQueryClick,
        onContactClick = contactsParams.onContactClick,
        onShowContactMethods = contactsParams.onShowContactMethods,
        onCallContact = contactsParams.onCallContact,
        onSmsContact = contactsParams.onSmsContact,
        onContactMethodClick = contactsParams.onContactMethodClick,
        getPrimaryContactCardAction = contactsParams.getPrimaryContactCardAction,
        getSecondaryContactCardAction = contactsParams.getSecondaryContactCardAction,
        onPrimaryActionLongPress = contactsParams.onPrimaryActionLongPress,
        onSecondaryActionLongPress = contactsParams.onSecondaryActionLongPress,
        onCustomAction = contactsParams.onCustomAction,
        onFileClick = filesParams.onFileClick,
        onSettingClick = settingsParams.onSettingClick,
        onAppShortcutClick = appShortcutsParams.onShortcutClick,
        onNoteClick = notesParams.onNoteClick,
        onAppSettingClick = settingsParams.onAppSettingClick,
        onAppSettingToggle = settingsParams.onAppSettingToggle,
        isAppSettingToggleChecked = settingsParams.isAppSettingToggleChecked,
        appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
        onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
        appSettingAppResultRowCount = settingsParams.appSettingAppResultRowCount,
        onAppSettingAppResultRowCountChange = settingsParams.onAppSettingAppResultRowCountChange,
        onDeleteRecentItem = onDeleteRecentItem,
        showInlineCollapseButton = false,
        expandedCardMaxHeight = expandedCardMaxHeight,
        showWallpaperBackground = showWallpaperBackground,
        isOverlayPresentation = isOverlayPresentation,
        alwaysExpanded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
