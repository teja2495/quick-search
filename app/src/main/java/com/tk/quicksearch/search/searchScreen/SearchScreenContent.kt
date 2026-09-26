package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.util.rememberPhysicalKeyboardConnected
import com.tk.quicksearch.tools.setAlarm.SetAlarmHandler
import com.tk.quicksearch.tools.setAlarm.StartTimerHandler
import com.tk.quicksearch.reminders.ReminderNaturalLanguageParser
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.openNotificationShade
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
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
    val openKeyboardOnLaunchOnStartup = remember { state.openKeyboardOnLaunch }
    var canShowOpenKeyboardPill by
            remember(isOverlayPresentation) { mutableStateOf(!isOverlayPresentation && !openKeyboardOnLaunchOnStartup) }
    var delayedOpenKeyboardActionVisible by remember { mutableStateOf(false) }
    var hideOpenKeyboardActionInstantly by remember { mutableStateOf(false) }
    var hasSeenStartupKeyboardVisible by
            remember(isOverlayPresentation) { mutableStateOf(false) }
    var hasClosedStartupKeyboardAfterLaunch by
            remember(isOverlayPresentation) { mutableStateOf(!openKeyboardOnLaunchOnStartup) }
    var isSearchHistoryExpanded by remember { mutableStateOf(false) }
    var isAiFollowUpInputVisible by remember { mutableStateOf(false) }
    var aiFollowUpText by remember { mutableStateOf("") }
    var searchHistoryCollapseRequestKey by remember { mutableStateOf(0) }
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
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
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

    val searchHintText = rememberSearchHint(state, isDefaultLauncher)
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
    val shouldShowNumberKeyboardOperators =
            isImeVisible && (manuallySwitchedToNumberKeyboard || isCalculatorMode)
    val showBottomSearchBar = showSearchField && state.bottomSearchBarEnabled
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
                    !isImeVisible &&
                    !isPhysicalKeyboardConnected &&
                    canShowOpenKeyboardPill &&
                    hasClosedStartupKeyboardAfterLaunch &&
                    !isSearchHistoryExpanded

    LaunchedEffect(shouldShowOpenKeyboardAction) {
        if (!shouldShowOpenKeyboardAction) {
            delayedOpenKeyboardActionVisible = false
            return@LaunchedEffect
        }

        hideOpenKeyboardActionInstantly = false
        delayedOpenKeyboardActionVisible = false
        delay(OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS)
        delayedOpenKeyboardActionVisible = true
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
    val detectedAlarmTime =
            remember(state.query, isToolAliasMode) {
                if (isToolAliasMode) {
                    null
                } else {
                    SetAlarmHandler.detectAlarmTime(state.query)
                }
            }
    val detectedTimerSeconds =
            remember(state.query, isToolAliasMode) {
                if (isToolAliasMode) {
                    null
                } else {
                    StartTimerHandler.detectTimerSeconds(state.query)
                }
            }
    val detectedReminderSchedule =
            remember(state.query, isToolAliasMode) {
                if (isToolAliasMode) {
                    null
                } else {
                    ReminderNaturalLanguageParser.parse(state.query)
                        ?.takeIf { it.title.isNotBlank() }
                }
            }
    val predictedState = rememberPredictedSearchTargetState(
        state = state,
        renderingState = renderingState,
        appsParams = appsParams,
        enabledTargets = enabledTargets,
        isImeVisible = isImeVisible,
        isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
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
                        .safeDrawingPadding()
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
            onContactActionTrigger = onContactActionTrigger,
            onAppClick = onAppClick,
        )
    }

    var lastTriggeredQuery by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.query,
        state.allApps,
        renderingState.displayApps,
        state.allAppShortcuts,
        renderingState.appShortcutResults,
        renderingState.contactResults,
        state.pinnedContacts,
        renderingState.fileResults,
        state.pinnedFiles,
        renderingState.settingResults,
        state.allDeviceSettings,
        renderingState.calendarEvents,
        renderingState.noteResults,
        state.pinnedNotes,
        state.nicknameUpdateVersion,
    ) {
        if (state.query == lastTriggeredQuery) return@LaunchedEffect
        lastTriggeredQuery = null
        if (state.query.isBlank()) return@LaunchedEffect
        if (openMatchingTrigger(state.query)) {
            lastTriggeredQuery = state.query
        }
    }

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
        PersistentSearchBar(
                query = state.query,
                selectRetainedQuery = state.selectRetainedQuery,
                onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
                onQueryChange = onQueryChanged,
                onClearQuery = onClearQuery,
                onSettingsClick = onSettingsClick,
                showSettingsIcon = state.settingsIconEnabled,
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
                autoFocusOnStart = state.openKeyboardOnLaunch,
                releaseFocusOnLeave = !isOverlayPresentation && state.clearQueryOnLaunch,
                restoreKeyboardOnEnter = !isOverlayPresentation && state.restoreSearchKeyboard,
                onRestoreKeyboardHandled = onRestoreSearchKeyboardHandled,
                startupSurfaceReady = state.isStartupCoreSurfaceReady,
                onClearDetectedShortcut = onClearDetectedShortcut,
                onSectionSelected = onSectionSelected,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                onPressWhileKeyboardClosed = {
                    if (!isImeVisible) {
                        hideOpenKeyboardActionInstantly = true
                        delayedOpenKeyboardActionVisible = false
                    }
                },
                focusRequester = searchFocusRequester,
                forceRestingOutline = showBottomSearchBar,
                transparentBackground = useInsetEngineStrip,
                cornerRadius =
                        if (useInsetEngineStrip) {
                            InsetSearchBarGeometry.BarCornerRadius
                        } else {
                            DesignTokens.Spacing28
                        },
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

    val swipeActions = rememberSearchScreenSwipeActions(
        state = state,
        expandedSection = expandedSection,
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
    )
    val onLauncherSwipeUp = swipeActions.onLauncherSwipeUp
    val onLauncherSwipeDown = swipeActions.onLauncherSwipeDown
    val bottomBarSwipeModifier = swipeActions.bottomBarSwipeModifier

    CompositionLocalProvider(
        LocalSearchColorTheme provides searchColorTheme,
        LocalAmoledThemeActive provides amoledSurfacesActive,
        LocalSearchResultQuery provides state.query.trim(),
    ) {
    Column(modifier = contentModifier, verticalArrangement = Arrangement.Top) {
        if (showSearchField && !showBottomSearchBar) {
            searchFieldContent()
        }

        if (showSearchField && !showBottomSearchBar && !state.oneHandedMode) {
            Spacer(modifier = Modifier.padding(top = DesignTokens.SpacingXSmall))
        }

        SearchContentArea(
                modifier =
                        run {
                            val base =
                                    if (isOverlayPresentation) {
                                        Modifier.fillMaxWidth().weight(1f)
                                    } else {
                                        Modifier.weight(1f)
                                    }
                            if (showSearchField && showBottomSearchBar) {
                                base.padding(top = DesignTokens.SpacingXXSmall)
                            } else {
                                base
                            }
                        },
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
                onSearchHistoryCollapseRequested = { searchHistoryCollapseRequestKey += 1 },
                onOpenPermissionsSettings = onOpenPermissionsSettings,
                onHomePinnedSectionOrderChange = onHomePinnedSectionOrderChange,
                onChangeWallpaperClick = onChangeWallpaperClick,
                onOpenGesturesSettingsClick = onOpenGesturesSettingsClick,
                onOpenSettingsClick = onSettingsClick,
                showOpenSettingsOption = !state.settingsIconEnabled,
                showCalculator = state.calculatorState.isToolMode || state.calculatorState.result != null || state.calculatorState.parsedDateMillis != null || state.calculatorState.dateDiffLabel != null || state.calculatorState.timeResultLabel != null,
                showCurrencyConverter = showCurrencyConverter,
                showWorldClock = showWorldClock,
                showDictionary = showDictionary,
                showWeather = showWeather,
                showAiSearch = state.AiSearchState.status != AiSearchStatus.Idle,
                aiSearchState = state.AiSearchState,
                isOverlayPresentation = isOverlayPresentation,
                onBottomOneHandedOverscrollUp = {
                    searchFocusRequester.requestFocus()
                    keyboardController?.show()
                },
                onLauncherOverscrollUp = onLauncherSwipeUp,
                onLauncherOverscrollDown = onLauncherSwipeDown,
                onHomeDoubleTap = {
                    homeDoubleTapAction.performHomeGesture(
                        homeDoubleTapCustomActionJson,
                        homeDoubleTapAliasTarget,
                        context,
                        { action, target -> onGestureAliasTarget(action, target) },
                        onCloseQuickSearch,
                    )
                },
                selectedTopMatchIndex = selectedTopMatchIndex,
        )

        // Pinned above the keyboard, outside the scrollable content.
        SearchScreenBottomChrome(
            state = state,
            expandedSection = expandedSection,
            keyboardSwitchText = keyboardSwitchText,
            shouldShowPhoneCallAction = shouldShowPhoneCallAction,
            detectedAlarmTime = detectedAlarmTime,
            detectedTimerSeconds = detectedTimerSeconds,
            detectedReminderSchedule = detectedReminderSchedule,
            onKeyboardSwitchToggle = onKeyboardSwitchToggle,
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
            hideOpenKeyboardActionInstantly = hideOpenKeyboardActionInstantly,
            openKeyboardText = openKeyboardText,
            onVoiceClick = onVoiceClick,
            onOpenKeyboardActionClicked = {
                hideOpenKeyboardActionInstantly = true
                delayedOpenKeyboardActionVisible = false
                searchFocusRequester.requestFocus()
                openKeyboardActionScope.launch {
                    withFrameNanos { }
                    keyboardController?.show()
                }
            },
        )
    }
    } 
}
