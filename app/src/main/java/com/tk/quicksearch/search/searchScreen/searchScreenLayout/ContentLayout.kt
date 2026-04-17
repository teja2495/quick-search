package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.SearchHistorySection
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.searchEngines.compact.NoResultsSearchEngineCards
import com.tk.quicksearch.search.webSuggestions.WebSuggestionsSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.directSearch.CurrencyConverterResult
import com.tk.quicksearch.tools.directSearch.CalculatorResult
import com.tk.quicksearch.tools.directSearch.DictionaryResult
import com.tk.quicksearch.tools.directSearch.DirectSearchResult
import com.tk.quicksearch.tools.directSearch.WordClockResult
import com.tk.quicksearch.search.searchScreen.ExpandedSection
import com.tk.quicksearch.search.searchScreen.InfoBanner
import com.tk.quicksearch.search.searchScreen.hasAnySearchResults
import com.tk.quicksearch.search.searchScreen.renderSection
import com.tk.quicksearch.search.searchScreen.ContactsSectionParams
import com.tk.quicksearch.search.searchScreen.FilesSectionParams
import com.tk.quicksearch.search.searchScreen.AppShortcutsSectionParams
import com.tk.quicksearch.search.searchScreen.SettingsSectionParams
import com.tk.quicksearch.search.searchScreen.AppsSectionParams
import com.tk.quicksearch.search.searchScreen.CalendarSectionParams
import com.tk.quicksearch.search.searchScreen.NotesSectionParams
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.components.SectionPermissionResultCard
import com.tk.quicksearch.R
import androidx.compose.ui.res.stringResource

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
    onRequestUsagePermission: () -> Unit,
    minContentHeight: Dp,
    expandedCardMaxHeight: Dp,
    isReversed: Boolean,
    hideResults: Boolean,
    showCalculator: Boolean = false,
    showCurrencyConverter: Boolean = false,
    showWordClock: Boolean = false,
    showDictionary: Boolean = false,
    showDirectSearch: Boolean = false,
    directSearchState: DirectSearchState? = null,
    isOverlayPresentation: Boolean = false,
    onPhoneNumberClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onOpenPersonalContextDialog: () -> Unit = {},
    onWebSuggestionClick: (String) -> Unit = {},
    onSearchEngineLongPress: () -> Unit = {},
    onCustomizeSearchEnginesClick: () -> Unit = {},
    onOpenDirectSearchConfigure: () -> Unit = {},
    onSearchTargetClick: (String, SearchTarget) -> Unit = { _, _ -> },
    onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
    onOpenSearchHistorySettings: () -> Unit = {},
    onDismissSearchHistoryTip: () -> Unit = {},
    onGeminiModelInfoClick: () -> Unit = {},
    onSearchHistoryExpandedChange: (Boolean) -> Unit = {},
    onOpenPermissionsSettings: () -> Unit = {},
) {
    val context = LocalContext.current
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
    val effectiveAppsParams = appsParams.copy(predictedTarget = predictedTarget)

    // 1. Determine Layout Order based on ItemPriorityConfig
    val hasQuery = state.query.isNotBlank()
    val isUrlQuery = remember(state.query) { isLikelyWebUrl(state.query) }
    val baseLayoutOrder = ItemPriorityConfig.getLayoutOrder(hasQuery)

    // 2. Apply One-Handed Mode Reversal if needed
    // User Requirement: "When one handed mode is enabled the same order is reversed."
    // isReversed flag passed here reflects one-handed mode state.
    val finalLayoutOrder = if (isReversed) baseLayoutOrder.reversed() else baseLayoutOrder

    // 3. Prepare Shared Rendering Context and Params
    // We reuse the extracted logic to determine visibility and expansion states
    val sectionContext =
        rememberSectionRenderContext(
            state = state,
            renderingState = renderingState,
            filesParams = effectiveFilesParams,
            contactsParams = effectiveContactsParams,
            settingsParams = effectiveSettingsParams,
            calendarParams = effectiveCalendarParams,
            notesParams = effectiveNotesParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            appsParams = effectiveAppsParams,
            isSearching = hasQuery,
            oneHandedMode =
                state.oneHandedMode, // This affects list reversal inside helpers
        )

    val sectionParams =
        SectionRenderParams(
            renderingState = renderingState,
            contactsParams = effectiveContactsParams,
            filesParams = effectiveFilesParams,
            appShortcutsParams = effectiveAppShortcutsParams,
            settingsParams = effectiveSettingsParams,
            calendarParams = effectiveCalendarParams,
            notesParams = effectiveNotesParams,
            appsParams = effectiveAppsParams,
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
                !showDirectSearch &&
                !showCalculator &&
                !showCurrencyConverter &&
                !showWordClock &&
                !showDictionary &&
                suggestionsNotEmpty &&
                suggestionsEnabled &&
                !suggestionWasSelected

    // Recent Queries Logic (for App Open State mainly, but CONFIG has RECENT_QUERIES item)
    // Suppress regular history in alias mode — alias recent items are shown in the section slot instead.
    val showRecentItems =
            !hasQuery &&
            state.detectedAliasSearchSection == null &&
            !state.isCurrencyConverterAliasMode &&
            !state.isWordClockAliasMode &&
            !state.isDictionaryAliasMode &&
            state.recentQueriesEnabled &&
            state.recentItems.isNotEmpty()

    var searchHistoryExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(showRecentItems) {
        if (!showRecentItems) searchHistoryExpanded = false
    }
    LaunchedEffect(searchHistoryExpanded) {
        onSearchHistoryExpandedChange(searchHistoryExpanded)
    }

    val hidePinnedAndAppsWhenSearchHistoryExpanded =
        showRecentItems && searchHistoryExpanded
    val sectionContextForRecentHistoryExpansion =
        if (hidePinnedAndAppsWhenSearchHistoryExpanded) {
            sectionContext.copy(
                shouldRenderFiles = false,
                shouldRenderContacts = false,
                shouldRenderApps = false,
                shouldRenderAppShortcuts = false,
                shouldRenderSettings = false,
                shouldRenderCalendar = false,
                shouldRenderNotes = false,
            )
        } else {
            sectionContext
        }
    val activeAliasSection = state.detectedAliasSearchSection
    val isSectionAliasMode = activeAliasSection != null
    val showAliasRecentItems = isSectionAliasMode && !hasQuery && state.aliasRecentItems.isNotEmpty()

    fun shouldRenderSection(section: SearchSection): Boolean {
        return if (isSectionAliasMode) {
            activeAliasSection == section
        } else {
            !hideResults
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        finalLayoutOrder.forEach { itemType ->
            val section = itemType.toSearchSectionOrNull()
            val isSectionItem = section != null

            // If a section is expanded, we hide all OTHER non-section items.
            if (isExpanded && !isSectionItem) return@forEach

            if (section != null) {
                if (!shouldRenderSection(section)) return@forEach
                if (section == SearchSection.APPS && isUrlQuery) return@forEach

                val showAliasRecentForSection =
                    showAliasRecentItems && section in ALIAS_RECENT_ELIGIBLE_SECTIONS
                if (showAliasRecentForSection) {
                    AliasRecentItemsSection(
                        items = state.aliasRecentItems,
                        contactsParams = effectiveContactsParams,
                        filesParams = effectiveFilesParams,
                        settingsParams = effectiveSettingsParams,
                        appShortcutsParams = effectiveAppShortcutsParams,
                        onWebSuggestionClick = onWebSuggestionClick,
                        onDeleteRecentItem = onDeleteRecentItem,
                        expandedCardMaxHeight = expandedCardMaxHeight,
                        showWallpaperBackground = effectiveShowWallpaperBackground,
                        isOverlayPresentation = isOverlayPresentation,
                    )
                    return@forEach
                }

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

                renderSection(section, sectionParams, sectionContextForRecentHistoryExpansion)
                return@forEach
            }

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
                        CalculatorResult(
                            calculatorState = state.calculatorState,
                            showWallpaperBackground =
                                effectiveShowWallpaperBackground,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.CURRENCY_CONVERTER_RESULT -> {
                    AnimatedVisibility(
                        visible = showCurrencyConverter,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 80)) +
                                expandVertically(animationSpec = tween(durationMillis = 250, delayMillis = 60), expandFrom = Alignment.Top),
                        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 160), shrinkTowards = Alignment.Top),
                    ) {
                        CurrencyConverterResult(
                                currencyConverterState = state.currencyConverterState,
                                showWallpaperBackground = effectiveShowWallpaperBackground,
                                onGeminiModelInfoClick = onGeminiModelInfoClick,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.WORD_CLOCK_RESULT -> {
                    AnimatedVisibility(
                        visible = showWordClock,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 80)) +
                                expandVertically(animationSpec = tween(durationMillis = 250, delayMillis = 60), expandFrom = Alignment.Top),
                        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 160), shrinkTowards = Alignment.Top),
                    ) {
                        WordClockResult(
                                wordClockState = state.wordClockState,
                                showWallpaperBackground = effectiveShowWallpaperBackground,
                                onGeminiModelInfoClick = onGeminiModelInfoClick,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.DICTIONARY_RESULT -> {
                    AnimatedVisibility(
                        visible = showDictionary,
                        enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 80)) +
                                expandVertically(animationSpec = tween(durationMillis = 250, delayMillis = 60), expandFrom = Alignment.Top),
                        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 160), shrinkTowards = Alignment.Top),
                    ) {
                        DictionaryResult(
                                dictionaryState = state.dictionaryState,
                                showWallpaperBackground = effectiveShowWallpaperBackground,
                                onGeminiModelInfoClick = onGeminiModelInfoClick,
                        )
                    }
                }

                ItemPriorityConfig.ItemType.DIRECT_SEARCH_RESULT -> {
                    if (showDirectSearch && directSearchState != null) {
                        DirectSearchResult(
                            directSearchState = directSearchState,
                            directSearchLlmProviderId = state.directSearchLlmProviderId,
                            showWallpaperBackground = effectiveShowWallpaperBackground,
                            onGeminiModelInfoClick = onGeminiModelInfoClick,
                            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
                            onPhoneNumberClick = onPhoneNumberClick,
                            onEmailClick = onEmailClick,
                        )
                    }
                }

                // --- Suggestions & Engines ---
                ItemPriorityConfig.ItemType.WEB_SUGGESTIONS -> {
                    val allowWebSuggestions =
                        !hideResults || state.detectedShortcutTarget != null
                    AnimatedVisibility(
                        visible = allowWebSuggestions && hasQuery && showWebSuggestions,
                        enter =
                            fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 80)) +
                                expandVertically(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 80),
                                    expandFrom = Alignment.Top,
                                ),
                        exit =
                            fadeOut(animationSpec = tween(durationMillis = 90)) +
                                shrinkVertically(
                                    animationSpec = tween(durationMillis = 150),
                                    shrinkTowards = Alignment.Top,
                                ),
                    ) {
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
                    if (!hideResults && showRecentItems) {
                        val orderedRecentItems = state.recentItems
                        AnimatedVisibility(
                            visible = true, // showRecentItems is
                            // already checked
                            enter = fadeIn(),
                            exit = shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                            ) {
                                SearchHistorySection(
                                    items = orderedRecentItems,
                                    callingApp =
                                        effectiveContactsParams.callingApp
                                            ?: CallingApp.CALL,
                                    messagingApp =
                                        effectiveContactsParams.messagingApp
                                            ?: MessagingApp
                                                .MESSAGES,
                                    onRecentQueryClick =
                                    onWebSuggestionClick,
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
                                    onDeleteRecentItem =
                                    onDeleteRecentItem,
                                    showSearchHistoryTip = !state.hasDismissedSearchHistoryTip,
                                    onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                                    onDismissSearchHistoryTip = onDismissSearchHistoryTip,
                                    onExpandedChange = { searchHistoryExpanded = it },
                                    expandedCardMaxHeight = expandedCardMaxHeight,
                                    showWallpaperBackground =
                                        effectiveShowWallpaperBackground,
                                    isOverlayPresentation = isOverlayPresentation,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                ItemPriorityConfig.ItemType.SEARCH_ENGINES_INLINE -> {
                    // Inline search engines.
                    // Condition: Not compact mode.
                    val showInlineSearchEngines =
                        !hideResults &&
                            hasQuery &&
                            !state.isSecondarySearchInProgress &&
                            (!state.isSearchEngineCompactMode || isUrlQuery)
                    AnimatedVisibility(
                        visible = showInlineSearchEngines,
                        enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 70)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                    ) {
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
    }
}

private val ALIAS_RECENT_ELIGIBLE_SECTIONS =
    setOf(
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS,
        SearchSection.APP_SETTINGS,
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
    onWebSuggestionClick: (String) -> Unit,
    onDeleteRecentItem: (RecentSearchEntry) -> Unit,
    expandedCardMaxHeight: Dp,
    showWallpaperBackground: Boolean,
    isOverlayPresentation: Boolean,
) {
    SearchHistorySection(
        items = items,
        callingApp = contactsParams.callingApp ?: CallingApp.CALL,
        messagingApp = contactsParams.messagingApp ?: MessagingApp.MESSAGES,
        onRecentQueryClick = onWebSuggestionClick,
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
        onAppSettingClick = settingsParams.onAppSettingClick,
        onAppSettingToggle = settingsParams.onAppSettingToggle,
        isAppSettingToggleChecked = settingsParams.isAppSettingToggleChecked,
        appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
        onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
        onDeleteRecentItem = onDeleteRecentItem,
        expandedCardMaxHeight = expandedCardMaxHeight,
        showWallpaperBackground = showWallpaperBackground,
        isOverlayPresentation = isOverlayPresentation,
        alwaysExpanded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
