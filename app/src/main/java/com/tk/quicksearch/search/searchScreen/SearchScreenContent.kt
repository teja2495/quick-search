package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.onboarding.ReportWelcomeBarPlacement
import com.tk.quicksearch.onboarding.welcomeMorphHoldsKeyboard
import com.tk.quicksearch.onboarding.welcomeMorphRunning
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import com.tk.quicksearch.search.searchScreen.components.SearchBarWithSettingsButton
import com.tk.quicksearch.shared.ui.components.LocalOpenPopupTracker
import com.tk.quicksearch.shared.ui.components.undoSnackbarAvoidArea
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchScreen.components.cornerRadius
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.HomeTextContrastTheme
import com.tk.quicksearch.search.searchScreen.shared.LocalSearchCardLayoutEnabled
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.search.searchScreen.components.LocalOpenKeyboardPillMemory
import com.tk.quicksearch.search.searchScreen.components.LocalSearchReturningFromInAppPage
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.util.rememberPhysicalKeyboardConnected
import com.tk.quicksearch.tools.setAlarm.SetAlarmHandler
import com.tk.quicksearch.tools.setAlarm.StartTimerHandler
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import androidx.compose.ui.zIndex
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.app.startup.StartupTrace
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS = 200L
private const val OPEN_KEYBOARD_COLD_START_SUPPRESS_MS = 1000L

internal data class ToolCardConfig(
        val label: String,
        val icon: ImageVector? = null,
        val appIconPackage: String? = null,
        val onClick: () -> Unit,
)

@Composable
internal fun SearchScreenContent(
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
        onQueryChanged: (String) -> Unit,
        onSelectRetainedQueryHandled: () -> Unit,
        onRestoreSearchKeyboardHandled: () -> Unit = {},
        onStartupKeyboardVisible: () -> Unit = {},
        onClearQuery: () -> Unit,
        onVoiceClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        onRequestUsagePermission: () -> Unit,
        onOtherSearchItemAction: OtherSearchItemActionHandler,
        onSearchTargetClick: (String, SearchTarget) -> Unit,
        onSearchEngineLongPress: () -> Unit,
        onAiSearchEmailClick: (String) -> Unit,
        onPhoneNumberClick: (String) -> Unit,
        onWebSuggestionClick: (String) -> Unit,
        onRecentQueryClick: (RecentSearchEntry.Query) -> Unit,
        onOpenPersonalContextDialog: () -> Unit,
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onOpenAiSearchConfigure: () -> Unit = {},
        onAiFollowUpSubmit: (String) -> Unit = {},
        onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
        onClearRecentItems: () -> Unit = {},
        onGeminiModelInfoClick: () -> Unit = {},
        onCurrencyConversionClick: () -> Unit = {},
        onDictionarySearchClick: () -> Unit = {},
        onWeatherSearchClick: () -> Unit = {},
        onWorldClockSearchClick: () -> Unit = {},
        onQuickSearchHelpClick: () -> Unit = {},
        onCustomToolSearchClick: () -> Unit = {},
        onTaskerIntentClick: () -> Unit = {},
        onKeyboardSwitchToggle: () -> Unit,
        onOverlayNumberKeyboardUiChanged: ((Boolean, Boolean) -> Unit)? = null,
        onOverlayExpandRequest: () -> Unit = {},
        isOverlayExpanded: Boolean = false,
        onWelcomeAnimationCompleted: (() -> Unit)? = null,
        expandedSection: ExpandedSection,
        manuallySwitchedToNumberKeyboard: Boolean,
        scrollState: androidx.compose.foundation.ScrollState,
        searchFocusRequester: FocusRequester,
        onClearDetectedShortcut: () -> Unit,
        onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
        modifier: Modifier = Modifier,
        isOverlayPresentation: Boolean = false,
        showSearchField: Boolean = true,
        onOpenPermissionsSettings: () -> Unit = {},
        onHomePinnedSectionOrderChange: (List<SearchSection>) -> Unit = {},
        onChangeWallpaperClick: () -> Unit = {},
        onOpenGesturesSettingsClick: () -> Unit = {},
        swipeUpAction: SwipeGestureAction = SwipeGestureAction.OPEN_KEYBOARD,
        swipeDownAction: SwipeGestureAction = SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS,
        swipeUpCustomActionJson: String? = null,
        swipeDownCustomActionJson: String? = null,
        swipeUpAliasTarget: String? = null,
        swipeDownAliasTarget: String? = null,
        homeSwipeUpAction: com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction = com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction.NONE,
        homeSwipeDownAction: com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction = com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction.NOTIFICATION_PANEL,
        homeSwipeUpCustomActionJson: String? = null,
        homeSwipeDownCustomActionJson: String? = null,
        homeDoubleTapAction: com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction = com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction.NONE,
        homeDoubleTapCustomActionJson: String? = null,
        homeSwipeUpAliasTarget: String? = null,
        homeSwipeDownAliasTarget: String? = null,
        homeDoubleTapAliasTarget: String? = null,
        onGestureAliasTarget: (Enum<*>, String) -> Unit = { _, _ -> },
        onCloseQuickSearch: () -> Unit = {},
        getAllTriggerWordsById: () -> Map<String, String> = { emptyMap() },
        getAllContactActionTriggers: () -> Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, com.tk.quicksearch.search.data.preferences.ResultTrigger> = { emptyMap() },
        onContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isPhysicalKeyboardConnected = rememberPhysicalKeyboardConnected()
    // Returning from Settings/Widgets rebuilds this screen but skips launch auto-focus, so the
    // startup keyboard gating must not wait for a keyboard that will never open.
    val returningFromInAppPage = LocalSearchReturningFromInAppPage.current
    val openKeyboardOnLaunchOnStartup = remember { state.openKeyboardOnLaunch && !returningFromInAppPage }
    var canShowOpenKeyboardPill by
            remember(isOverlayPresentation) { mutableStateOf(!isOverlayPresentation && !openKeyboardOnLaunchOnStartup) }
    val openKeyboardPillMemory = LocalOpenKeyboardPillMemory.current
    // Already on screen before switching pages: show it again as-is, without the enter animation.
    var delayedOpenKeyboardActionVisible by remember {
        mutableStateOf(returningFromInAppPage && openKeyboardPillMemory.visibleWhenLeft)
    }
    var hasSeenStartupKeyboardVisible by
            remember(isOverlayPresentation) { mutableStateOf(false) }
    var hasClosedStartupKeyboardAfterLaunch by
            remember(isOverlayPresentation) { mutableStateOf(!openKeyboardOnLaunchOnStartup) }
    var isSearchHistoryExpanded by remember { mutableStateOf(false) }
    var isAiFollowUpInputVisible by remember { mutableStateOf(false) }
    var aiFollowUpText by remember { mutableStateOf("") }
    var searchHistoryCollapseRequestKey by remember { mutableStateOf(0) }
    var clearRevealKey by remember { mutableStateOf(0) }
    val openKeyboardActionScope = rememberCoroutineScope()

    LaunchedEffect(isOverlayPresentation) {
        if (!isOverlayPresentation) {
            if (openKeyboardOnLaunchOnStartup) {
                delay(OPEN_KEYBOARD_COLD_START_SUPPRESS_MS)
            }
            canShowOpenKeyboardPill = true
            return@LaunchedEffect
        }

        // Overlay auto-focus opens IME shortly after composition; avoid a startup flicker.
        canShowOpenKeyboardPill = false
        delay(850)
        canShowOpenKeyboardPill = true
    }

    val enabledTargets: List<SearchTarget> =
            remember(state.searchTargetsOrder, state.disabledSearchTargetIds) {
                state.searchTargetsOrder.filter { it.getId() !in state.disabledSearchTargetIds }
            }
    // Derived so the keyboard animation does not recompose this whole screen on every frame.
    val imeInsets = WindowInsets.ime
    val isImeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val isImeTargetVisible by rememberImeTargetVisible()
    val isImeOpening by rememberImeOpening()
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            StartupTrace.mark("QS.Home.KeyboardVisible")
            onStartupKeyboardVisible()
        }
    }
    LaunchedEffect(openKeyboardOnLaunchOnStartup, isImeVisible) {
        if (!openKeyboardOnLaunchOnStartup) {
            hasSeenStartupKeyboardVisible = false
            hasClosedStartupKeyboardAfterLaunch = true
            return@LaunchedEffect
        }

        if (isImeVisible) {
            hasSeenStartupKeyboardVisible = true
        } else if (hasSeenStartupKeyboardVisible) {
            hasClosedStartupKeyboardAfterLaunch = true
        }
    }
    val isCalculatorMode = state.calculatorState.isCalculatorMode
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()

    LaunchedEffect(state.hasUsagePermission, state.pinnedNonAppItemOrder) {
        if (
            OtherSearchItemRegistry.shouldLoad(
                itemId = com.tk.quicksearch.search.other.OtherSearchItemId.SCREEN_TIME,
                query = state.query,
                pinnedItemOrder = state.pinnedNonAppItemOrder,
            )
        ) {
            onQueryChanged(state.query)
        }
    }
    val isToolMode = state.calculatorState.isToolMode
    val isUnitConverterMode = state.calculatorState.isUnitConverterMode
    val activeToolType = if (isToolMode) state.calculatorState.toolType else null
    val isCurrencyConverterAliasMode = state.isCurrencyConverterAliasMode
    val isWorldClockAliasMode = state.isWorldClockAliasMode
    val isDictionaryAliasMode = state.isDictionaryAliasMode
    val isWeatherAliasMode = state.isWeatherAliasMode
    val activeCustomTool = state.detectedCustomToolId?.let { id -> state.customTools.find { it.id == id } }
    val activeTaskerIntent = state.detectedTaskerIntentId?.let { id -> state.taskerIntentTools.find { it.id == id } }
    val triggerWords by
            produceState<Collection<String>>(
                    initialValue = emptyList(),
                    getAllTriggerWordsById,
                    state.nicknameUpdateVersion,
            ) {
                value = withContext(Dispatchers.IO) { getAllTriggerWordsById().values.toList() }
            }

    val searchHintText = rememberSearchHint(state)
    val trimmedQuery = state.query.trim()
    val cardVisibility = searchCardVisibility(state, activeCustomTool != null)
    val showCurrencyConverter = cardVisibility.showCurrencyConverter
    val showWorldClock = cardVisibility.showWorldClock
    val showDictionary = cardVisibility.showDictionary
    val showWeather = cardVisibility.showWeather
    val showCurrencyConverterSearchCard = cardVisibility.showCurrencyConverterSearchCard
    val showDictionarySearchCard = cardVisibility.showDictionarySearchCard
    val showWorldClockSearchCard = cardVisibility.showWorldClockSearchCard
    val showWeatherSearchCard = cardVisibility.showWeatherSearchCard
    val showCustomToolSearchCard = cardVisibility.showCustomToolSearchCard
    val showAiFollowUpAction = cardVisibility.showAiFollowUpAction
    LaunchedEffect(showAiFollowUpAction) {
        if (!showAiFollowUpAction) {
            isAiFollowUpInputVisible = false
            aiFollowUpText = ""
        }
    }
    val showTaskerIntentCard = activeTaskerIntent != null
    val isToolAliasMode =
            isCurrencyConverterAliasMode ||
                    isWorldClockAliasMode ||
                    isDictionaryAliasMode ||
                    isWeatherAliasMode ||
                    activeCustomTool != null
                    || activeTaskerIntent != null
                    || cardVisibility.isQuickSearchHelpQuery
    val shouldShowNumberKeyboardOperators =
            isImeVisible && (manuallySwitchedToNumberKeyboard || isCalculatorMode)
    val showBottomSearchBar = showSearchField && state.bottomSearchBarEnabled
    ReportWelcomeBarPlacement(atBottom = showBottomSearchBar)
    val barStyle =
            if (SearchBarStyle.hasFixedShape(state.bottomSearchBarEnabled, state.isSearchEngineCompactMode)) {
                state.searchBarStyle.withFixedShape()
            } else {
                state.searchBarStyle
            }
    val surfaceColors = rememberSearchSurfaceColors(state)
    val amoledSurfacesActive = surfaceColors.amoledSurfacesActive
    val searchColorTheme = surfaceColors.searchColorTheme
    val overlayCardColor = surfaceColors.overlayCardColor
    val overlayDividerTint = surfaceColors.overlayDividerTint
    val overlayActionTint = surfaceColors.overlayActionTint
    val shouldRenderInlineNumberKeyboardOperators =
            shouldShowNumberKeyboardOperators && !isOverlayPresentation
    val openKeyboardText = stringResource(R.string.action_open_keyboard)
    val shouldShowOpenKeyboardAction =
            expandedSection == ExpandedSection.NONE &&
                    !showBottomSearchBar &&
                    // An already visible pill stays while the keyboard opens over it, then goes.
                    (if (isImeTargetVisible) isImeOpening && delayedOpenKeyboardActionVisible else !isImeVisible) &&
                    !isPhysicalKeyboardConnected &&
                    canShowOpenKeyboardPill &&
                    hasClosedStartupKeyboardAfterLaunch &&
                    !state.restoreSearchKeyboard &&
                    !isSearchHistoryExpanded && !welcomeMorphRunning() &&
                    LocalOpenPopupTracker.current?.hasOpenPopup != true

    LaunchedEffect(shouldShowOpenKeyboardAction) {
        if (!shouldShowOpenKeyboardAction) {
            delayedOpenKeyboardActionVisible = false
            return@LaunchedEffect
        }
        if (delayedOpenKeyboardActionVisible) return@LaunchedEffect

        delayedOpenKeyboardActionVisible = false
        delay(OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS)
        delayedOpenKeyboardActionVisible = true
    }
    SideEffect {
        openKeyboardPillMemory.visibleWhenLeft = shouldShowOpenKeyboardAction && delayedOpenKeyboardActionVisible
    }
    val keyboardSwitchText =
            if (isToolMode) {
                null
            } else if (manuallySwitchedToNumberKeyboard) {
                stringResource(R.string.keyboard_switch_back)
            } else if (state.query.isCalculatorStyleQuery() &&
                            state.detectedShortcutTarget == null &&
                            state.detectedAliasSearchSection == null &&
                            !isCurrencyConverterAliasMode &&
                            !isWorldClockAliasMode &&
                            !isDictionaryAliasMode &&
                            !isWeatherAliasMode &&
                            activeCustomTool == null
            ) {
                stringResource(R.string.keyboard_switch_to_number)
            } else {
                null
            }
    val shouldShowPhoneCallAction =
            keyboardSwitchText != null && state.query.isPhoneNumberQuery()
    val detectedAlarmTime = remember(state.query, isToolAliasMode) {
        if (isToolAliasMode) null else SetAlarmHandler.detectAlarmTime(state.query)
    }
    val detectedTimerSeconds = remember(state.query, isToolAliasMode) {
        if (isToolAliasMode) null else StartTimerHandler.detectTimerSeconds(state.query)
    }
    val detectedReminderSchedule = remember(state.query, isToolAliasMode) {
        if (isToolAliasMode) null
        else ReminderNaturalLanguageParser.parse(state.query)?.takeIf { it.title.isNotBlank() }
    }
    val predictedState = rememberPredictedSearchTargetState(
        state = state,
        renderingState = renderingState,
        appsParams = appsParams,
        enabledTargets = enabledTargets,
        isImeVisible = isImeVisible,
        isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
        isQuickSearchHelpQuery = cardVisibility.isQuickSearchHelpQuery,
        showCurrencyConverterSearchCard = showCurrencyConverterSearchCard,
        showDictionarySearchCard = showDictionarySearchCard,
        showWeatherSearchCard = showWeatherSearchCard,
        showWorldClockSearchCard = showWorldClockSearchCard,
        showCurrencyConverter = showCurrencyConverter,
        showWorldClock = showWorldClock,
        showDictionary = showDictionary,
        showWeather = showWeather,
    )
    val isNonSubmittableSuggestionsTab = predictedState.isNonSubmittableSuggestionsTab
    val firstSubmittableGridApp = predictedState.firstSubmittableGridApp
    val suffixAliasMatchIgnoringTrailingSpace = predictedState.suffixAliasMatchIgnoringTrailingSpace
    val isOtherSearchResultVisible = predictedState.isOtherSearchResultVisible
    val predictedTargetForIndicator = predictedState.predictedTargetForIndicator
    val hideResultsForTopMatchSubmit = predictedState.hideResultsForTopMatchSubmit
    val topMatchSubmission = rememberTopMatchSubmission(
        state = state,
        renderingState = renderingState,
        filesParams = filesParams,
        contactsParams = contactsParams,
        settingsParams = settingsParams,
        calendarParams = calendarParams,
        notesParams = notesParams,
        remindersParams = remindersParams,
        appShortcutsParams = appShortcutsParams,
        appsParams = appsParams,
        hideResultsForTopMatchSubmit = hideResultsForTopMatchSubmit,
        expandedSection = expandedSection,
        isSearchHistoryExpanded = isSearchHistoryExpanded,
    )
    val topMatchSubmitParams = topMatchSubmission.params
    val topMatchesForSubmit = topMatchSubmission.matches
    val shouldSubmitTopMatch = topMatchSubmission.shouldSubmit
    val selectedTopMatchIndex = topMatchSubmission.selectedIndex
    val moveSelectedTopMatch = topMatchSubmission.moveSelection
    val activeToolCardConfig = toolCardConfig(
        isSearchHistoryExpanded = isSearchHistoryExpanded,
        showAiFollowUpAction = showAiFollowUpAction,
        onShowAiFollowUpInput = { isAiFollowUpInputVisible = true },
        showQuickSearchHelpCard = cardVisibility.showQuickSearchHelpCard,
        onQuickSearchHelpClick = onQuickSearchHelpClick,
        showCurrencyConverterSearchCard = showCurrencyConverterSearchCard,
        onCurrencyConversionClick = onCurrencyConversionClick,
        showDictionarySearchCard = showDictionarySearchCard,
        onDictionarySearchClick = onDictionarySearchClick,
        showWeatherSearchCard = showWeatherSearchCard,
        isWeatherAliasMode = isWeatherAliasMode,
        trimmedQuery = trimmedQuery,
        weatherLocation = state.weatherLocation,
        onWeatherSearchClick = onWeatherSearchClick,
        showCustomToolSearchCard = showCustomToolSearchCard,
        customToolName = activeCustomTool?.name,
        onCustomToolSearchClick = onCustomToolSearchClick,
        showTaskerIntentCard = showTaskerIntentCard,
        taskerIntentName = activeTaskerIntent?.name,
        onTaskerIntentClick = onTaskerIntentClick,
        showWorldClockSearchCard = showWorldClockSearchCard,
        onWorldClockSearchClick = onWorldClockSearchClick,
    )
    val showOnlyToolActionInCompactSection =
            activeToolCardConfig != null &&
                    (isToolAliasMode || !state.isSearchEngineCompactMode || enabledTargets.isEmpty())

    // Search engine scroll state for auto-scroll during onboarding
    val searchEngineScrollState = rememberLazyListState()

    // Bottom-anchored Home rides on the keyboard as one block, so slide it on a layer.
    val slideWithIme = !isOverlayPresentation && showBottomSearchBar && state.oneHandedMode &&
            state.query.isBlank() && expandedSection == ExpandedSection.NONE && !isSearchHistoryExpanded
    val contentModifier =
            if (isOverlayPresentation) {
                modifier.fillMaxWidth()
                        .padding(
                                start = DesignTokens.SpacingXLarge,
                                top = DesignTokens.Spacing28,
                                end = DesignTokens.SpacingXLarge,
                        )
            } else {
                modifier.fillMaxSize()
                        .safeDrawingPaddingWithImeSlide(enabled = slideWithIme)
                        .padding(
                                start = DesignTokens.SpacingLarge,
                                top = DesignTokens.SpacingSmall,
                                end = DesignTokens.SpacingLarge,
                        )
            }

    val searchEnginesModifier =
            if (
                    isOverlayPresentation ||
                            shouldRenderInlineNumberKeyboardOperators ||
                            showBottomSearchBar
            ) {
                Modifier
            } else {
                Modifier.imePadding()
            }

    LaunchedEffect(isOverlayPresentation, manuallySwitchedToNumberKeyboard, isImeVisible) {
        if (isOverlayPresentation) {
            onOverlayNumberKeyboardUiChanged?.invoke(
                    manuallySwitchedToNumberKeyboard,
                    isImeVisible,
            )
        }
    }

    val openMatchingTrigger: (String) -> Boolean = { query ->
        openMatchingSearchTrigger(
            query = query,
            state = state,
            renderingState = renderingState,
            appsParams = appsParams,
            appShortcutsParams = appShortcutsParams,
            contactsParams = contactsParams,
            filesParams = filesParams,
            settingsParams = settingsParams,
            notesParams = notesParams,
            getAllContactActionTriggers = getAllContactActionTriggers,
            triggerWords = triggerWords,
            onContactActionTrigger = onContactActionTrigger,
            onAppClick = onAppClick,
        )
    }

    AutoOpenMatchingTrigger(state, renderingState, triggerWords, openMatchingTrigger)

    val searchBarGeometry = rememberSearchBarGeometry(
        state = state,
        showBottomSearchBar = showBottomSearchBar,
        expandedSection = expandedSection,
        isSearchHistoryExpanded = isSearchHistoryExpanded,
        isOverlayPresentation = isOverlayPresentation,
    )
    val useInsetEngineStrip = searchBarGeometry.useInsetEngineStrip
    val insetEngineStripOverlap = searchBarGeometry.insetEngineStripOverlap
    val insetEngineStripFullBleedFraction = searchBarGeometry.insetEngineStripFullBleedFraction
    val searchFieldModifier = searchBarGeometry.searchFieldModifier

    val searchFieldContent: @Composable () -> Unit = {
        SearchBarWithSettingsButton(
                query = state.query,
                selectRetainedQuery = state.selectRetainedQuery,
                onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
                onQueryChange = onQueryChanged,
                onClearQuery = { clearRevealKey += 1; onClearQuery() },
                onSettingsClick = onSettingsClick,
                showSettingsIcon = state.settingsIconEnabled,
                onVoiceClick = onVoiceClick,
                dismissKeyboardBeforeSettingsClick = isOverlayPresentation,
                enabledTargets = enabledTargets,
                shortcutCodes = state.shortcutCodes,
                shortcutEnabled = state.shortcutEnabled,
                triggerWords = triggerWords,
                isSearchEngineAliasSuffixEnabled = state.isSearchEngineAliasSuffixEnabled,
                shouldUseNumberKeyboard = manuallySwitchedToNumberKeyboard || isCalculatorMode,
                detectedShortcutTarget = state.detectedShortcutTarget,
                detectedAliasSearchSection = state.detectedAliasSearchSection,
                isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
                isWorldClockAliasMode = isWorldClockAliasMode,
                isDictionaryAliasMode = isDictionaryAliasMode,
                isWeatherAliasMode = isWeatherAliasMode,
                detectedCustomToolId = state.detectedCustomToolId,
                detectedTaskerIntentId = state.detectedTaskerIntentId,
                activeToolType = activeToolType,
                isCalculatorMode = isCalculatorMode,
                placeholderText = searchHintText,
                showWelcomeAnimation = state.showSearchBarWelcomeAnimation,
                showWallpaperBackground = state.showWallpaperBackground,
                autoFocusOnStart = state.openKeyboardOnLaunch && !welcomeMorphHoldsKeyboard(),
                releaseFocusOnLeave = !isOverlayPresentation && state.clearQueryOnLaunch,
                restoreKeyboardOnEnter = !isOverlayPresentation && state.restoreSearchKeyboard,
                onRestoreKeyboardHandled = onRestoreSearchKeyboardHandled,
                startupSurfaceReady = state.isStartupCoreSurfaceReady,
                onClearDetectedShortcut = onClearDetectedShortcut,
                onSectionSelected = onSectionSelected,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                focusRequester = searchFocusRequester,
                forceRestingOutline = showBottomSearchBar,
                transparentBackground = useInsetEngineStrip,
                cornerRadius = barStyle.cornerRadius(),
                // Inside the compact engine card the card carries the outline, not the bar.
                searchBarStyle = if (useInsetEngineStrip) barStyle.copy(borderWidthDp = 0f) else barStyle,
                modifier = searchFieldModifier,
                onMoveTopResultSelectionUp = { moveSelectedTopMatch(-1) },
                onMoveTopResultSelectionDown = { moveSelectedTopMatch(1) },
                onSearchAction = {
                    submitSearchBarAction(
                        state = state,
                        renderingState = renderingState,
                        isNonSubmittableSuggestionsTab = isNonSubmittableSuggestionsTab,
                        isOtherSearchResultVisible = isOtherSearchResultVisible,
                        enabledTargets = enabledTargets,
                        onSearchTargetClick = onSearchTargetClick,
                        isQuickSearchHelpQuery = cardVisibility.isQuickSearchHelpQuery,
                        onQuickSearchHelpClick = onQuickSearchHelpClick,
                        showCurrencyConverterSearchCard = showCurrencyConverterSearchCard,
                        onCurrencyConversionClick = onCurrencyConversionClick,
                        showDictionarySearchCard = showDictionarySearchCard,
                        onDictionarySearchClick = onDictionarySearchClick,
                        showWeatherSearchCard = showWeatherSearchCard,
                        onWeatherSearchClick = onWeatherSearchClick,
                        showCustomToolSearchCard = showCustomToolSearchCard,
                        onCustomToolSearchClick = onCustomToolSearchClick,
                        showTaskerIntentCard = showTaskerIntentCard,
                        onTaskerIntentClick = onTaskerIntentClick,
                        showWorldClockSearchCard = showWorldClockSearchCard,
                        onWorldClockSearchClick = onWorldClockSearchClick,
                        firstSubmittableGridApp = firstSubmittableGridApp,
                        onAppClick = onAppClick,
                        shouldSubmitTopMatch = shouldSubmitTopMatch,
                        topMatchesForSubmit = topMatchesForSubmit,
                        selectedTopMatchIndex = selectedTopMatchIndex,
                        topMatchSubmitParams = topMatchSubmitParams,
                        openMatchingTrigger = openMatchingTrigger,
                        onQueryChanged = onQueryChanged,
                        appShortcutsParams = appShortcutsParams,
                        contactsParams = contactsParams,
                        filesParams = filesParams,
                        settingsParams = settingsParams,
                        calendarParams = calendarParams,
                        notesParams = notesParams,
                        isCalculatorMode = isCalculatorMode,
                        suffixAliasMatchIgnoringTrailingSpace = suffixAliasMatchIgnoringTrailingSpace,
                        context = context,
                    )
                },
        )
    }

    val homeWidgetEditGuard = com.tk.quicksearch.widgetsPanel.LocalHomeWidgetEditGuard.current
    val isHomeWidgetEditing = { homeWidgetEditGuard?.isEditing == true }
    val (onLauncherSwipeUp, onLauncherSwipeDown, bottomBarSwipeModifier) = rememberSearchScreenSwipeActions(
        swipeUpAction = swipeUpAction,
        swipeDownAction = swipeDownAction,
        swipeUpAliasTarget = swipeUpAliasTarget,
        swipeDownAliasTarget = swipeDownAliasTarget,
        homeSwipeUpAction = homeSwipeUpAction,
        homeSwipeDownAction = homeSwipeDownAction,
        homeSwipeUpCustomActionJson = homeSwipeUpCustomActionJson,
        homeSwipeDownCustomActionJson = homeSwipeDownCustomActionJson,
        homeSwipeUpAliasTarget = homeSwipeUpAliasTarget,
        homeSwipeDownAliasTarget = homeSwipeDownAliasTarget,
        onGestureAliasTarget = onGestureAliasTarget,
        onCloseQuickSearch = onCloseQuickSearch,
        isImeVisible = isImeVisible,
        searchFocusRequester = searchFocusRequester,
        keyboardController = keyboardController,
        isGestureBlocked = isHomeWidgetEditing,
        hasQuery = state.query.isNotBlank(),
    )

    CompositionLocalProvider(
        LocalSearchColorTheme provides searchColorTheme,
        LocalAmoledThemeActive provides amoledSurfacesActive,
        LocalSearchResultQuery provides state.query.trim(),
        LocalInsetCardBackdrop provides searchBarGeometry.insetCardBackdrop,
        LocalSearchChromeBlurEnabled provides state.searchChromeBlurEnabled,
        LocalOverlayResultCardColor provides overlayCardColor,
        com.tk.quicksearch.tools.calculator.LocalCalculatorResultQueryHandler provides onQueryChanged,
    ) {
    Column(modifier = contentModifier, verticalArrangement = Arrangement.Top) {
        // Above the results, which extend up behind it and would otherwise take its taps.
        if (showSearchField && !showBottomSearchBar) Box(Modifier.fillMaxWidth().zIndex(1f).then(bottomBarSwipeModifier)) { searchFieldContent() }
        if (!showSearchField) PinnedGlanceFocusTarget(searchFocusRequester)

        if (showSearchField && !showBottomSearchBar && !state.oneHandedMode) {
            Spacer(modifier = Modifier.padding(top = DesignTokens.SpacingXSmall))
        }

        HomeTextContrastTheme(enabled = !isOverlayPresentation && !LocalSearchCardLayoutEnabled.current) {
        SearchContentArea(
                modifier =
                        run {
                            val base =
                                    if (isOverlayPresentation) {
                                        Modifier.fillMaxWidth().let { if (showSearchField) it.weight(1f) else it }
                                    } else {
                                        Modifier.weight(1f)
                                    }
                            if (showSearchField && showBottomSearchBar) {
                                base.padding(top = DesignTokens.SpacingXSmall)
                            } else {
                                base
                            }
                        }.insetCardBackdropSource(searchBarGeometry.insetCardBackdrop),
                state = state,
                renderingState = renderingState,
                contactsParams = contactsParams,
                filesParams = filesParams,
                appShortcutsParams = appShortcutsParams,
                settingsParams = settingsParams,
                calendarParams = calendarParams,
                notesParams = notesParams,
                remindersParams = remindersParams,
                appsParams = appsParams,
                predictedTarget = predictedTargetForIndicator,
                isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
                onRequestUsagePermission = onRequestUsagePermission,
                onOtherSearchItemAction = onOtherSearchItemAction,
                scrollState = scrollState,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onAiSearchEmailClick,
                onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                onWebSuggestionClick = onWebSuggestionClick,
                onRecentQueryClick = onRecentQueryClick,
                onSearchTargetClick = onSearchTargetClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onOpenAiSearchConfigure = onOpenAiSearchConfigure,
                onDeleteRecentItem = onDeleteRecentItem,
                onClearRecentItems = onClearRecentItems,
                onGeminiModelInfoClick = onGeminiModelInfoClick,
                onSearchHistoryExpandedChange = { isSearchHistoryExpanded = it },
                searchHistoryCollapseRequestKey = searchHistoryCollapseRequestKey,
                isSearchHistoryExpanded = isSearchHistoryExpanded,
                clearRevealKey = clearRevealKey,
                onSearchHistoryCollapseRequested = { searchHistoryCollapseRequestKey += 1 },
                onOpenPermissionsSettings = onOpenPermissionsSettings,
                onHomePinnedSectionOrderChange = onHomePinnedSectionOrderChange,
                onChangeWallpaperClick = onChangeWallpaperClick,
                onOpenGesturesSettingsClick = onOpenGesturesSettingsClick,
                onOpenSettingsClick = onSettingsClick,
                showOpenSettingsOption = !state.settingsIconEnabled,
                showCalculator = state.calculatorState.isVisible,
                showCurrencyConverter = showCurrencyConverter,
                showWorldClock = showWorldClock,
                showDictionary = showDictionary,
                showWeather = showWeather,
                showAiSearch = state.AiSearchState.status != AiSearchStatus.Idle,
                aiSearchState = state.AiSearchState,
                isOverlayPresentation = isOverlayPresentation,
                onBottomOneHandedOverscrollUp = {
                    if (!isHomeWidgetEditing()) {
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                onLauncherOverscrollUp = onLauncherSwipeUp,
                onLauncherOverscrollDown = onLauncherSwipeDown,
                onHomeDoubleTap = {
                    if (!isHomeWidgetEditing() && state.query.isBlank()) {
                        homeDoubleTapAction.performHomeGesture(
                            homeDoubleTapCustomActionJson,
                            homeDoubleTapAliasTarget,
                            context,
                            { action, target -> onGestureAliasTarget(action, target) },
                            onCloseQuickSearch,
                        )
                    }
                },
                selectedTopMatchIndex = selectedTopMatchIndex,
                topMatches = topMatchSubmission.topMatches,
                isBottomChromeAnimating = searchBarGeometry.isInsetEngineStripAnimating,
                isImeSliding = slideWithIme,
        )
        }

        // Pinned above the keyboard, outside the scrollable content. Pinned glance mode has none.
        // Undo snackbars lift above it so they never cover the bottom search bar or engine strip.
        if (showSearchField) Column(Modifier.fillMaxWidth().undoSnackbarAvoidArea()) { SearchScreenBottomChrome(
            state = state,
            expandedSection = expandedSection,
            keyboardSwitchText = keyboardSwitchText,
            shouldShowPhoneCallAction = shouldShowPhoneCallAction,
            detectedAlarmTime = detectedAlarmTime,
            detectedTimerSeconds = detectedTimerSeconds,
            detectedReminderSchedule = detectedReminderSchedule,
            onKeyboardSwitchToggle = onKeyboardSwitchToggle,
            onQuickActionCompleted = onClearQuery,
            isSearchHistoryExpanded = isSearchHistoryExpanded,
            overlayCardColor = overlayCardColor,
            overlayDividerTint = overlayDividerTint,
            overlayActionTint = overlayActionTint,
            isAiFollowUpInputVisible = isAiFollowUpInputVisible,
            aiFollowUpText = aiFollowUpText,
            onAiFollowUpTextChange = { aiFollowUpText = it },
            onAiFollowUpInputVisibilityChange = { isAiFollowUpInputVisible = it },
            onAiFollowUpSubmit = onAiFollowUpSubmit,
            useInsetEngineStrip = useInsetEngineStrip,
            insetEngineStripOverlap = insetEngineStripOverlap,
            insetEngineStripFullBleedFraction = insetEngineStripFullBleedFraction,
            searchEnginesModifier = searchEnginesModifier,
            bottomBarSwipeModifier = bottomBarSwipeModifier,
            enabledTargets = enabledTargets,
            onSearchTargetClick = onSearchTargetClick,
            onSearchEngineLongPress = onSearchEngineLongPress,
            searchEngineScrollState = searchEngineScrollState,
            onClearDetectedShortcut = onClearDetectedShortcut,
            predictedTargetForIndicator = predictedTargetForIndicator,
            activeToolCardConfig = activeToolCardConfig,
            showOnlyToolActionInCompactSection = showOnlyToolActionInCompactSection,
            shouldRenderInlineNumberKeyboardOperators = shouldRenderInlineNumberKeyboardOperators,
            showBottomSearchBar = showBottomSearchBar,
            isOverlayPresentation = isOverlayPresentation,
            onQueryChanged = onQueryChanged,
            showSearchField = showSearchField,
            searchFieldContent = searchFieldContent,
            shouldShowOpenKeyboardAction = shouldShowOpenKeyboardAction,
            delayedOpenKeyboardActionVisible = delayedOpenKeyboardActionVisible,
            // The opening keyboard has already covered it; collapsing it again would jump.
            hideOpenKeyboardActionInstantly = isImeTargetVisible,
            openKeyboardText = openKeyboardText,
            onVoiceClick = onVoiceClick,
            onOpenKeyboardActionClicked = {
                if (delayedOpenKeyboardActionVisible) {
                    searchFocusRequester.requestFocus()
                    openKeyboardActionScope.launch {
                        withFrameNanos { }
                        keyboardController?.show()
                    }
                }
            },
        ) }
    }
    } 
}
