package com.tk.quicksearch.search.searchScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.search.core.WeatherStatus
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.search.core.SearchEnginesVisibility
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.ScreenTimeState
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.core.WorldClockStatus
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.core.rememberSectionRenderContext
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.searchEngines.inline.SearchEngineIconsSection
import com.tk.quicksearch.searchEngines.inline.AiFollowUpInputSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.isAmoledSurfaceTheme
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.LocalAmoledThemeActive
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.util.rememberPhysicalKeyboardConnected
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.ConfirmedWeatherQuery
import com.tk.quicksearch.tools.aiTools.WeatherIntentParser
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.openNotificationShade
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.HomeSwipeGestureAction
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import com.tk.quicksearch.app.startup.StartupTrace
import com.tk.quicksearch.tools.aiTools.WorldClockIntentParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS = 200L
private const val OPEN_KEYBOARD_COLD_START_SUPPRESS_MS = 1000L
private const val SEARCH_HINT_ROTATION_INTERVAL_MS = 5000L
private const val ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS = 280
private const val ONE_HANDED_COMPACT_ENGINES_FADE_IN_DURATION_MS = 180
private const val ONE_HANDED_COMPACT_ENGINES_FADE_IN_DELAY_MS = 40
private const val ONE_HANDED_COMPACT_ENGINES_FADE_OUT_DURATION_MS = 130

private fun HomeSwipeGestureAction.performHomeGesture(actionJson: String?, aliasTarget: String?, context: android.content.Context, onAliasTarget: (HomeSwipeGestureAction, String) -> Unit) {
    when (this) {
        HomeSwipeGestureAction.LOCK_SCREEN -> LockScreenAccessibilityService.lockScreen()
        HomeSwipeGestureAction.NOTIFICATION_PANEL -> context.openNotificationShade()
        HomeSwipeGestureAction.CUSTOM -> {
            CustomWidgetButtonAction.fromJson(actionJson)?.let { action ->
                context.startActivity(WidgetActionActivity.createIntent(context, action))
            }
        }
        HomeSwipeGestureAction.SEARCH_ENGINE,
        HomeSwipeGestureAction.TOOL -> aliasTarget?.let { onAliasTarget(this, it) }
        HomeSwipeGestureAction.NONE -> Unit
    }
}

private data class ToolCardConfig(
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
        appsParams: AppsSectionParams,
        onQueryChanged: (String) -> Unit,
        onSelectRetainedQueryHandled: () -> Unit,
        onRestoreSearchKeyboardHandled: () -> Unit = {},
        onStartupKeyboardVisible: () -> Unit = {},
        onClearQuery: () -> Unit,
        onSettingsClick: () -> Unit,
        onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        onRequestUsagePermission: () -> Unit,
        onToggleOtherSearchItemPin: (OtherSearchItemId) -> Unit,
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
        onChangeWallpaperClick: () -> Unit = {},
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

    // Calculate enabled engines
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

    val hintSearchAnything = stringResource(R.string.search_hint)
    val staticSearchHint =
        stringResource(
            if (isDefaultLauncher) {
                R.string.common_search
            } else {
                R.string.search_hint_static
            },
        )
    val cycleHints = stringArrayResource(R.array.search_hints_cycle)
    // Indices must stay in sync with R.array.search_hints_cycle order in strings.xml
    val defaultHints = remember(
        hintSearchAnything,
        cycleHints,
        state.disabledSections,
        state.hasContactPermission,
        state.hasCalendarPermission,
        state.hasFilePermission,
        state.hasApiKey,
        state.calculatorEnabled,
        state.unitConverterEnabled,
        state.dateCalculatorEnabled,
        state.currencyConverterEnabled,
        state.worldClockEnabled,
        state.dictionaryEnabled,
        state.weatherEnabled,
        state.weatherLocationConfigured,
    ) {
        val gated = listOf(
            cycleHints[0] to (SearchSection.CONTACTS !in state.disabledSections && state.hasContactPermission),
            cycleHints[1] to (SearchSection.FILES !in state.disabledSections && state.hasFilePermission),
            cycleHints[2] to (SearchSection.CALENDAR !in state.disabledSections && state.hasCalendarPermission),
            cycleHints[3] to (SearchSection.APPS !in state.disabledSections),
            cycleHints[4] to (SearchSection.APP_SHORTCUTS !in state.disabledSections),
            cycleHints[5] to (SearchSection.SETTINGS !in state.disabledSections),
            cycleHints[6] to state.currencyConverterEnabled,
            cycleHints[7] to state.unitConverterEnabled,
            cycleHints[8] to state.dateCalculatorEnabled,
            cycleHints[9] to state.calculatorEnabled,
            cycleHints[10] to (state.worldClockEnabled && state.hasApiKey),
            cycleHints[11] to (state.dictionaryEnabled && state.hasApiKey),
            cycleHints[12] to (state.weatherEnabled && state.hasApiKey),
        )
        listOf(hintSearchAnything) + gated.filter { it.second }.map { it.first }.shuffled()
    }

    val isDefaultHintMode =
            !isCalculatorMode &&
                    !isUnitConverterMode &&
                    !isCurrencyConverterAliasMode &&
                    !isWorldClockAliasMode &&
                    !isDictionaryAliasMode &&
                    !isWeatherAliasMode &&
                    activeCustomTool == null &&
                    state.detectedAliasSearchSection == null

    var hintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isDefaultHintMode, state.searchHintsEnabled) {
        if (!isDefaultHintMode || !state.searchHintsEnabled) {
            hintIndex = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(SEARCH_HINT_ROTATION_INTERVAL_MS)
            hintIndex = (hintIndex + 1) % defaultHints.size
        }
    }

    val searchHintText =
            when {
                isCalculatorMode -> stringResource(R.string.calculator_enter_math_expression_hint)
                isUnitConverterMode -> stringResource(R.string.unit_converter_enter_conversion_hint)
                isCurrencyConverterAliasMode ->
                        stringResource(R.string.search_hint_currency_converter)
                isWorldClockAliasMode -> stringResource(R.string.search_hint_world_clock)
                isDictionaryAliasMode -> stringResource(R.string.search_hint_dictionary)
                isWeatherAliasMode -> stringResource(R.string.search_hint_weather)
                activeCustomTool != null -> activeCustomTool.name
                state.detectedAliasSearchSection != null ->
                    stringResource(
                        SearchSectionUiMetadataRegistry
                            .metadataFor(state.detectedAliasSearchSection)
                            .searchHintRes,
                    )
                !state.searchHintsEnabled -> staticSearchHint
                else -> defaultHints[hintIndex % defaultHints.size]
            }
    val showCurrencyConverter =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    state.currencyConverterState.status != CurrencyConverterStatus.Idle
    val showWorldClock =
            (state.worldClockEnabled || isWorldClockAliasMode) &&
                    state.worldClockState.status != WorldClockStatus.Idle
    val showDictionary =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.dictionaryState.status != DictionaryStatus.Idle
    val showWeather =
            (state.weatherEnabled || isWeatherAliasMode) &&
                    state.weatherState.status != WeatherStatus.Idle
    val showCalculatorResult =
            state.calculatorState.isToolMode ||
                    state.calculatorState.result != null ||
                    state.calculatorState.parsedDateMillis != null ||
                    state.calculatorState.dateDiffLabel != null ||
                    state.calculatorState.timeResultLabel != null
    val trimmedQuery = state.query.trim()
    val showCurrencyConverterSearchCard =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isCurrencyConverterAliasMode) {
                        true // always show when alias mode is active
                    } else {
                        trimmedQuery.isNotBlank() &&
                                CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showDictionarySearchCard =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isDictionaryAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                DictionaryIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showWorldClockSearchCard =
            (state.worldClockEnabled || isWorldClockAliasMode) &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isWorldClockAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                WorldClockIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val confirmedWeatherQuery =
            if (isWeatherAliasMode) {
                ConfirmedWeatherQuery(
                    requestedLocation = trimmedQuery.takeIf { it.isNotBlank() },
                    originalQuery = trimmedQuery,
                )
            } else {
                WeatherIntentParser.parseConfirmed(trimmedQuery)
            }
    val weatherLocationAvailable =
            state.weatherLocationConfigured ||
                    confirmedWeatherQuery?.requestedLocation?.isNotBlank() == true
    val showWeatherSearchCard =
            (state.weatherEnabled || isWeatherAliasMode) &&
                    state.hasApiKey &&
                    weatherLocationAvailable &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWorldClock &&
                    !showDictionary &&
                    !showWeather &&
                    if (isWeatherAliasMode) {
                        true
                    } else {
                        confirmedWeatherQuery != null
                    }
    val showCustomToolSearchCard =
            activeCustomTool != null &&
                    state.hasApiKey &&
                    !showCalculatorResult &&
                    state.AiSearchState.status == AiSearchStatus.Idle
    val showAiFollowUpAction =
            state.AiSearchState.status == AiSearchStatus.Success &&
                    !state.AiSearchState.answer.isNullOrBlank() &&
                    state.detectedCustomToolId == null

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
    val useOverlayThemeTints = !state.deviceThemeEnabled && state.backgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val amoledSurfacesActive =
            isAmoledSurfaceTheme(
                    amoledThemeEnabled = state.amoledThemeEnabled,
                    theme = state.appTheme,
                    isDarkMode = isDarkMode,
                    deviceThemeEnabled = state.deviceThemeEnabled,
                    backgroundSource = state.backgroundSource,
            )
    val searchColorTheme =
            if (state.deviceThemeEnabled) {
                null
            } else {
                resolveSearchColorTheme(
                        theme = state.appTheme,
                        backgroundSource = state.backgroundSource,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                        amoledThemeEnabled = state.amoledThemeEnabled,
                )
            }
    val overlayCardColor =
            if (useOverlayThemeTints) {
                appThemeResultCardColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                        amoledThemeEnabled = state.amoledThemeEnabled,
                )
            } else {
                null
            }
    val overlayDividerTint =
            if (useOverlayThemeTints) {
                appThemeDividerColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                )
            } else {
                null
            }
    val overlayActionTint =
            if (useOverlayThemeTints) {
                appThemeActionColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                )
            } else {
                null
            }
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
            } else if (state.query.isNotEmpty() &&
                            state.query.none { it.isLetter() } &&
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
    val isLocalSearchRefreshing =
            shouldDeferTopMatchesForLocalSearch(
                    query = state.query,
                    isAppSearchInProgress = state.isAppSearchInProgress,
                    isSecondarySearchInProgress = state.isSecondarySearchInProgress,
            )
    val topMatchSubmitContext =
            rememberSectionRenderContext(
                    state = state,
                    renderingState = renderingState,
                    filesParams = filesParams,
                    contactsParams = contactsParams,
                    settingsParams = settingsParams,
                    calendarParams = calendarParams,
                    notesParams = notesParams,
                    appShortcutsParams = appShortcutsParams,
                    appsParams = appsParams,
                    isSearching = state.query.isNotBlank(),
                    oneHandedMode = state.oneHandedMode,
            )
    val topMatchSubmitParams =
            SectionRenderParams(
                    renderingState = renderingState,
                    contactsParams = contactsParams,
                    filesParams = filesParams,
                    appShortcutsParams = appShortcutsParams,
                    settingsParams = settingsParams,
                    calendarParams = calendarParams,
                    notesParams = notesParams,
                    appsParams = appsParams,
                    isReversed = state.oneHandedMode,
            )
    val currentTopMatchesForSubmit =
            rememberTopMatches(
                    query = state.query,
                    renderingState = renderingState,
                    context = topMatchSubmitContext,
                    params = topMatchSubmitParams,
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
    val settledTopMatchesForSubmit =
            rememberSettledTopMatches(
                    query = state.query,
                    currentMatches = currentTopMatchesForSubmit,
                    isSearchRefreshing = isLocalSearchRefreshing,
                    limit = state.topMatchesLimit,
            )
    val topMatchesForSubmit = settledTopMatchesForSubmit.matches
    val shouldSubmitTopMatch =
            state.topMatchesEnabled &&
                    state.query.isNotBlank() &&
                    !hideResultsForTopMatchSubmit &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    settledTopMatchesForSubmit.isReady &&
                    topMatchesForSubmit.isNotEmpty()
    val keyboardNavigableTopMatches =
            if (state.oneHandedMode) {
                topMatchesForSubmit.asReversed()
            } else {
                topMatchesForSubmit
            }
    var selectedTopMatchIndex by remember { mutableStateOf<Int?>(null) }
    var previousTopMatchQuery by remember { mutableStateOf(state.query) }

    LaunchedEffect(state.query, shouldSubmitTopMatch, topMatchesForSubmit) {
        if (!shouldSubmitTopMatch) {
            selectedTopMatchIndex = null
            previousTopMatchQuery = state.query
            return@LaunchedEffect
        }

        val queryChanged = state.query != previousTopMatchQuery
        selectedTopMatchIndex =
                when {
                    queryChanged -> 0
                    selectedTopMatchIndex in topMatchesForSubmit.indices -> selectedTopMatchIndex
                    else -> 0
                }
        previousTopMatchQuery = state.query
    }

    fun moveSelectedTopMatch(delta: Int): Boolean {
        if (!shouldSubmitTopMatch || keyboardNavigableTopMatches.isEmpty()) return false
        val displayedIndexByActualIndex =
                keyboardNavigableTopMatches
                        .mapIndexed { displayedIndex, item ->
                            topMatchesForSubmit.indexOf(item) to displayedIndex
                        }
                        .toMap()
        val currentDisplayedIndex =
                selectedTopMatchIndex
                        ?.let(displayedIndexByActualIndex::get)
                        ?: 0
        val nextDisplayedIndex =
                (currentDisplayedIndex + delta).coerceIn(0, keyboardNavigableTopMatches.lastIndex)
        val selectedItem = keyboardNavigableTopMatches[nextDisplayedIndex]
        selectedTopMatchIndex = topMatchesForSubmit.indexOf(selectedItem).takeIf { it >= 0 }
        return true
    }
    val activeToolCardConfig =
            if (isSearchHistoryExpanded) {
                null
            } else {
                when {
                    showAiFollowUpAction ->
                            ToolCardConfig(
                                    label = stringResource(R.string.direct_search_ask_follow_up),
                                    icon = Icons.Rounded.QuestionAnswer,
                                    onClick = { isAiFollowUpInputVisible = true },
                            )
                    showCurrencyConverterSearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.get_currency_value),
                                    icon = Icons.Rounded.CurrencyExchange,
                                    onClick = onCurrencyConversionClick,
                            )
                    showDictionarySearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.search_in_dictionary),
                                    icon = Icons.Rounded.Search,
                                    onClick = onDictionarySearchClick,
                            )
                    showWeatherSearchCard ->
                            ToolCardConfig(
                                    label =
                                        if (isWeatherAliasMode && trimmedQuery.isBlank()) {
                                            stringResource(
                                                R.string.weather_in_location,
                                                state.weatherLocation,
                                            )
                                        } else {
                                            stringResource(R.string.get_weather)
                                        },
                                    icon = Icons.Rounded.Cloud,
                                    onClick = onWeatherSearchClick,
                            )
                    showCustomToolSearchCard ->
                            ToolCardConfig(
                                    label = activeCustomTool?.name.orEmpty(),
                                    icon = Icons.Rounded.Construction,
                                    onClick = onCustomToolSearchClick,
                            )
                    showTaskerIntentCard ->
                            ToolCardConfig(
                                    label = stringResource(
                                            R.string.tasker_intent_action,
                                            activeTaskerIntent?.name.orEmpty(),
                                    ),
                                    appIconPackage = com.tk.quicksearch.tools.tasker.TaskerIntegration.PACKAGE_NAME,
                                    onClick = onTaskerIntentClick,
                            )
                    showWorldClockSearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.get_time),
                                    icon = Icons.Rounded.AccessTime,
                                    onClick = onWorldClockSearchClick,
                            )
                    else -> null
                }
            }
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

    fun matchesTrigger(
        query: String,
        word: String,
        triggerAfterSpace: Boolean,
    ): Boolean {
        val normalizedWord = word.trim().lowercase()
        if (normalizedWord.isBlank()) return false
        val normalizedQuery = query.lowercase()
        return if (triggerAfterSpace) {
            normalizedQuery == "$normalizedWord "
        } else {
            normalizedQuery == normalizedWord
        }
    }

    fun openMatchingTrigger(query: String): Boolean {
        state.allApps.firstOrNull { app ->
            appsParams.getAppTrigger(app.packageName)?.let { trigger ->
                matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
            } == true
        }?.let { app ->
            onAppClick(app)
            return true
        }

        state.allAppShortcuts.firstOrNull { shortcut ->
            appShortcutsParams.getShortcutTrigger(
                com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey(shortcut),
            )?.let { trigger ->
                matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
            } == true
        }?.let { shortcut ->
            appShortcutsParams.onShortcutClick(shortcut)
            return true
        }

        (renderingState.contactResults + state.pinnedContacts)
            .distinctBy { it.contactId }
            .firstOrNull { contact ->
                contactsParams.getContactTrigger(contact.contactId)?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { contact ->
                if (contact.hasContactMethods) {
                    contactsParams.onShowContactMethods(contact)
                } else {
                    contactsParams.onContactClick(contact)
                }
                return true
            }

        getAllContactActionTriggers().firstNotNullOfOrNull { (key, trigger) ->
            if (matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)) {
                key
            } else {
                null
            }
        }?.let { key ->
            onContactActionTrigger(key.contactId, key.action)
            return true
        }

        (renderingState.fileResults + state.pinnedFiles)
            .distinctBy { it.uri }
            .firstOrNull { file ->
                filesParams.getFileTrigger(file.uri.toString())?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { file ->
                filesParams.onFileClick(file)
                return true
            }

        state.allDeviceSettings.firstOrNull { setting ->
            settingsParams.getSettingTrigger(setting.id)?.let { trigger ->
                matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
            } == true
        }?.let { setting ->
            settingsParams.onSettingClick(setting)
            return true
        }

        (renderingState.noteResults + state.pinnedNotes)
            .distinctBy { it.noteId }
            .firstOrNull { note ->
                notesParams.getNoteTrigger(note.noteId)?.let { trigger ->
                    matchesTrigger(query, trigger.word, trigger.triggerAfterSpace)
                } == true
            }?.let { note ->
                notesParams.onNoteClick(note)
                return true
            }

        return false
    }

    var lastTriggeredQuery by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        state.query,
        renderingState.contactResults,
        renderingState.fileResults,
        renderingState.settingResults,
        renderingState.calendarEvents,
        renderingState.noteResults,
    ) {
        if (state.query.isBlank() || state.query == lastTriggeredQuery) return@LaunchedEffect
        if (openMatchingTrigger(state.query)) {
            lastTriggeredQuery = state.query
        }
    }

    // With the bottom search bar, the compact engine strip becomes a rounded container attached to
    // the top of the search bar instead of a full-bleed band behind it. It tucks behind the bar so
    // the two read as one shape.
    val useInsetEngineStrip =
            showBottomSearchBar &&
                    state.isSearchEngineCompactMode &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    state.detectedShortcutTarget == null &&
                    state.detectedAliasSearchSection == null

    // The strip paints the card behind a transparent bar, so it has to reach past the bar's bottom
    // edge; falling short would draw its own outline inside the bar. The derived overlap assumes a
    // default-height field, so track the measured height and keep whichever is taller.
    var measuredSearchBarHeight by remember { mutableStateOf(0.dp) }
    val insetEngineStripOverlap = InsetSearchBarGeometry.overlapFor(measuredSearchBarHeight)

    val searchFieldModifier =
            if (useInsetEngineStrip) {
                // Inset on every side by the same amount so the bar sits centred inside the card
                // the strip paints; the strip reaches down by exactly these spacings plus the bar.
                Modifier.padding(
                        start = InsetSearchBarGeometry.BarHorizontalInset,
                        end = InsetSearchBarGeometry.BarHorizontalInset,
                        top = InsetSearchBarGeometry.BarTopSpacing,
                        bottom = InsetSearchBarGeometry.BarBottomSpacing,
                ).onSizeChanged { size ->
                    val measured = with(density) { size.height.toDp() }
                    if (measured > 0.dp && measured != measuredSearchBarHeight) {
                        measuredSearchBarHeight = measured
                    }
                }
            } else if (showBottomSearchBar) {
                Modifier.padding(
                        top =
                                if (state.oneHandedMode) {
                                    DesignTokens.SpacingXSmall
                                } else {
                                    0.dp
                                },
                        bottom = DesignTokens.SpacingMedium,
                )
            } else {
                Modifier.padding(
                        bottom =
                                (if (state.oneHandedMode) {
                                    DesignTokens.SpacingMedium
                                } else {
                                    DesignTokens.SpacingXSmall
                                }) + DesignTokens.SpacingXXSmall,
                )
            }

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
                    if (isNonSubmittableSuggestionsTab) {
                        return@PersistentSearchBar true
                    }
                    if (isOtherSearchResultVisible && !state.topMatchesEnabled) {
                        return@PersistentSearchBar true
                    }

                    if (!state.openTopResultUsingKeyboardEnabled) {
                        val query = state.query.trim()
                        enabledTargets.firstOrNull()?.let { target ->
                            if (query.isNotBlank()) onSearchTargetClick(query, target)
                        }
                        return@PersistentSearchBar false
                    }

                    // Tool prompt cards take priority: Done triggers the card action.
                    // When no card is visible, fall through to the search engine.
                    if (showCurrencyConverterSearchCard) {
                        onCurrencyConversionClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }
                    if (showDictionarySearchCard) {
                        onDictionarySearchClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }
                    if (showWeatherSearchCard) {
                        onWeatherSearchClick()
                        return@PersistentSearchBar true
                    }
                    if (showCustomToolSearchCard) {
                        onCustomToolSearchClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }
                    if (showTaskerIntentCard) {
                        onTaskerIntentClick()
                        return@PersistentSearchBar true
                    }
                    if (showWorldClockSearchCard) {
                        onWorldClockSearchClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }

                    // The app grid is the primary result surface. Done follows its visible
                    // ordering before considering the cross-section Top Matches fallback.
                    val firstApp = firstSubmittableGridApp
                    if (firstApp != null) {
                        onAppClick(firstApp)
                        return@PersistentSearchBar false
                    }

                    if (shouldSubmitTopMatch) {
                        openTopMatch(
                                item = topMatchesForSubmit.getOrNull(selectedTopMatchIndex ?: 0) ?: topMatchesForSubmit.first(),
                                params = topMatchSubmitParams,
                        )?.let { keepKeyboardOpen ->
                            return@PersistentSearchBar keepKeyboardOpen
                        }
                    }

                    val trimmedQuery = state.query.trim()
                    if (openMatchingTrigger(state.query)) {
                        return@PersistentSearchBar false
                    }

                    val isUrlQuery = isLikelyWebUrl(trimmedQuery)

                    // If query has trailing/leading spaces, trim it first
                    if (state.query != trimmedQuery) {
                        onQueryChanged(trimmedQuery)
                    }

                    if (isUrlQuery && trimmedQuery.isNotBlank()) {
                        val defaultBrowserPackage = resolveDefaultBrowserPackage(context)
                        val browserTarget =
                                defaultBrowserTarget(state.searchTargetsOrder, defaultBrowserPackage)
                        if (browserTarget != null) {
                            onSearchTargetClick(trimmedQuery, browserTarget)
                            return@PersistentSearchBar false
                        }
                    }

                    val firstAppShortcut = renderingState.appShortcutResults.firstOrNull()
                    if (firstAppShortcut != null) {
                        appShortcutsParams.onShortcutClick(firstAppShortcut)
                        return@PersistentSearchBar false
                    }

                    val firstContact = renderingState.contactResults.firstOrNull()
                    if (firstContact != null) {
                        if (firstContact.hasContactMethods) {
                            contactsParams.onShowContactMethods(firstContact)
                        } else {
                            contactsParams.onContactClick(firstContact)
                        }
                        return@PersistentSearchBar false
                    }

                    val firstFile = renderingState.fileResults.firstOrNull()
                    if (firstFile != null) {
                        filesParams.onFileClick(firstFile)
                        return@PersistentSearchBar false
                    }

                    val firstSetting = renderingState.settingResults.firstOrNull()
                    if (firstSetting != null) {
                        settingsParams.onSettingClick(firstSetting)
                        return@PersistentSearchBar false
                    }

                    val firstCalendarEvent = renderingState.calendarEvents.firstOrNull()
                    if (firstCalendarEvent != null) {
                        calendarParams.onEventClick(firstCalendarEvent)
                        return@PersistentSearchBar false
                    }

                    val firstNote =
                        if (FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES)) {
                            renderingState.noteResults.firstOrNull()
                        } else {
                            null
                        }
                    if (firstNote != null) {
                        notesParams.onNoteClick(firstNote)
                        return@PersistentSearchBar false
                    }

                    val firstAppSetting = renderingState.appSettingResults.firstOrNull()
                    if (firstAppSetting != null) {
                        if (firstAppSetting.isToggleAction) {
                            val currentValue = settingsParams.isAppSettingToggleChecked(firstAppSetting)
                            settingsParams.onAppSettingToggle(firstAppSetting, !currentValue)
                            return@PersistentSearchBar true // keep keyboard open for toggles
                        } else {
                            settingsParams.onAppSettingClick(firstAppSetting)
                            return@PersistentSearchBar false
                        }
                    }

                    // Check if a shortcut is detected
                    if (isCalculatorMode) {
                        return@PersistentSearchBar false
                    } else if (state.detectedShortcutTarget != null) {
                        // Query already has shortcut stripped by ViewModel when
                        // shortcut-at-start is detected
                        onSearchTargetClick(trimmedQuery, state.detectedShortcutTarget)
                    } else {
                        val shouldResolveSuffixAliasOnDone =
                                state.isSearchEngineAliasSuffixEnabled &&
                                        state.isAliasTriggerAfterSpaceEnabled &&
                                        state.query.lastOrNull()?.isWhitespace() == false
                        if (shouldResolveSuffixAliasOnDone) {
                            val suffixAliasMatch = suffixAliasMatchIgnoringTrailingSpace
                            if (suffixAliasMatch != null) {
                                val aliasQuery = suffixAliasMatch.first.trim()
                                if (aliasQuery.isNotBlank()) {
                                    onQueryChanged(aliasQuery)
                                    onSearchTargetClick(aliasQuery, suffixAliasMatch.second)
                                    return@PersistentSearchBar false
                                }
                            }
                        }
                        val primaryTarget = enabledTargets.firstOrNull()
                        if (primaryTarget != null && trimmedQuery.isNotBlank()) {
                            onSearchTargetClick(trimmedQuery, primaryTarget)
                        }
                    }
                    false
                },
        )
    }

    CompositionLocalProvider(
        LocalSearchColorTheme provides searchColorTheme,
        LocalAmoledThemeActive provides amoledSurfacesActive,
        LocalSearchResultQuery provides state.query.trim(),
    ) {
    Column(modifier = contentModifier, verticalArrangement = Arrangement.Top) {
        if (showSearchField && !showBottomSearchBar) {
            // Fixed search bar at the top
            searchFieldContent()
        }

        // Add spacing between search bar and scrollable content when bottom aligned setting is off
        if (showSearchField && !showBottomSearchBar && !state.oneHandedMode) {
            Spacer(modifier = Modifier.padding(top = DesignTokens.SpacingXSmall))
        }

        // Scrollable content between search bar and search engines
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
                appsParams = appsParams,
                predictedTarget = predictedTargetForIndicator,
                isPhysicalKeyboardConnected = isPhysicalKeyboardConnected,
                onRequestUsagePermission = onRequestUsagePermission,
                onToggleOtherSearchItemPin = onToggleOtherSearchItemPin,
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
                onChangeWallpaperClick = onChangeWallpaperClick,
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
                onLauncherOverscrollUp = {
                    when {
                        swipeUpAction == SwipeGestureAction.OPEN_KEYBOARD && !isImeVisible -> {
                            searchFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        swipeUpAction == SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS && isImeVisible -> {
                            keyboardController?.hide()
                        }
                        swipeUpAction == SwipeGestureAction.SEARCH_ENGINE || swipeUpAction == SwipeGestureAction.TOOL ->
                            swipeUpAliasTarget?.let { onGestureAliasTarget(swipeUpAction, it) }
                        else ->
                            homeSwipeUpAction.performHomeGesture(homeSwipeUpCustomActionJson, homeSwipeUpAliasTarget, context) { action, target -> onGestureAliasTarget(action, target) }
                    }
                },
                onLauncherOverscrollDown = {
                    when {
                        swipeDownAction == SwipeGestureAction.OPEN_KEYBOARD && !isImeVisible -> {
                            searchFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        swipeDownAction == SwipeGestureAction.CLOSE_KEYBOARD_OR_NOTIFICATIONS && isImeVisible -> {
                            keyboardController?.hide()
                        }
                        swipeDownAction == SwipeGestureAction.SEARCH_ENGINE || swipeDownAction == SwipeGestureAction.TOOL ->
                            swipeDownAliasTarget?.let { onGestureAliasTarget(swipeDownAction, it) }
                        else ->
                            homeSwipeDownAction.performHomeGesture(homeSwipeDownCustomActionJson, homeSwipeDownAliasTarget, context) { action, target -> onGestureAliasTarget(action, target) }
                    }
                },
                onHomeDoubleTap = {
                    homeDoubleTapAction.performHomeGesture(homeDoubleTapCustomActionJson, homeDoubleTapAliasTarget, context) { action, target -> onGestureAliasTarget(action, target) }
                },
                selectedTopMatchIndex = selectedTopMatchIndex,
        )

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, when search engine section is disabled,
        // or when a shortcut is detected
        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded
        if (expandedSection == ExpandedSection.NONE) {
            AnimatedVisibility(
                    visible = keyboardSwitchText != null || shouldShowPhoneCallAction,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                top = DesignTokens.SpacingSmall,
                                                bottom = DesignTokens.SpacingSmall,
                                        ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (keyboardSwitchText != null) {
                        KeyboardSwitchPill(
                                text = keyboardSwitchText,
                                onClick = onKeyboardSwitchToggle,
                        )
                    }
                    if (shouldShowPhoneCallAction) {
                        Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
                        PhoneCallPill(
                                onClick = {
                                    context.startActivity(
                                            Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${Uri.encode(state.query)}")
                                            },
                                    )
                                },
                        )
                    }
                }
            }

            if (!isSearchHistoryExpanded) {
                CompositionLocalProvider(
                        LocalOverlayResultCardColor provides overlayCardColor,
                        LocalOverlayDividerColor provides overlayDividerTint,
                        LocalOverlayActionColor provides overlayActionTint,
                ) {
                    if (isAiFollowUpInputVisible) {
                        AiFollowUpInputSection(
                                value = aiFollowUpText,
                                onValueChange = { aiFollowUpText = it },
                                onSend = {
                                    val followUp = aiFollowUpText.trim()
                                    if (followUp.isNotEmpty()) {
                                        isAiFollowUpInputVisible = false
                                        aiFollowUpText = ""
                                        onAiFollowUpSubmit(followUp)
                                    }
                                },
                                showWallpaperBackground = state.showWallpaperBackground,
                                useInsetContainer = useInsetEngineStrip,
                                insetOverlap = insetEngineStripOverlap,
                                modifier = searchEnginesModifier,
                        )
                    } else {
                        AnimatedContent(
                            targetState = state.searchEnginesState,
                            modifier = Modifier.fillMaxWidth(),
                            contentKey = { it::class },
                            transitionSpec = {
                                if (
                                    state.oneHandedMode &&
                                        (initialState is SearchEnginesVisibility.Compact ||
                                            targetState is SearchEnginesVisibility.Compact)
                                ) {
                                    val enterTransition =
                                        if (targetState is SearchEnginesVisibility.Compact) {
                                            fadeIn(
                                                animationSpec =
                                                    tween(
                                                        durationMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_IN_DURATION_MS,
                                                        delayMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_IN_DELAY_MS,
                                                    ),
                                            ) +
                                                expandVertically(
                                                    expandFrom = Alignment.Bottom,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                            easing = FastOutSlowInEasing,
                                                        ),
                                                )
                                        } else {
                                            EnterTransition.None
                                        }
                                    val exitTransition =
                                        if (initialState is SearchEnginesVisibility.Compact) {
                                            fadeOut(
                                                animationSpec =
                                                    tween(
                                                        durationMillis =
                                                            ONE_HANDED_COMPACT_ENGINES_FADE_OUT_DURATION_MS,
                                                    ),
                                            ) +
                                                shrinkVertically(
                                                    shrinkTowards = Alignment.Bottom,
                                                    animationSpec =
                                                        tween(
                                                            durationMillis =
                                                                ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                            easing = FastOutSlowInEasing,
                                                        ),
                                                )
                                        } else {
                                            ExitTransition.None
                                        }
                                    enterTransition
                                        .togetherWith(exitTransition)
                                        .using(
                                            SizeTransform(clip = false) { _, _ ->
                                                tween(
                                                    durationMillis =
                                                        ONE_HANDED_COMPACT_ENGINES_REFLOW_DURATION_MS,
                                                    easing = FastOutSlowInEasing,
                                                )
                                            },
                                        )
                                } else {
                                    (EnterTransition.None togetherWith ExitTransition.None)
                                        .using(null)
                                }
                            },
                            label = "oneHandedCompactSearchEnginesReflow",
                        ) { animatedEnginesState ->
                            SearchEnginesVisibility(
                                enginesState = animatedEnginesState,
                            compactContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = state.detectedShortcutTarget,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = state.searchEngineCompactRowCount,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                        toolActionLabel = activeToolCardConfig?.label,
                                        toolActionIcon = activeToolCardConfig?.icon,
                                        toolActionAppIconPackage = activeToolCardConfig?.appIconPackage,
                                        onToolActionClick = activeToolCardConfig?.onClick,
                                        showOnlyToolAction = showOnlyToolActionInCompactSection,
                                        useInsetContainer = useInsetEngineStrip,
                                        insetOverlap = insetEngineStripOverlap,
                                )
                            },
                            fullContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = state.detectedShortcutTarget,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = 1,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                )
                            },
                            shortcutContent = { target ->
                                SearchEngineIconsSection(
                                        query = state.query,
                                        enabledEngines = enabledTargets,
                                        onSearchEngineClick = onSearchTargetClick,
                                        onSearchEngineLongPress = onSearchEngineLongPress,
                                        externalScrollState = searchEngineScrollState,
                                        detectedShortcutTarget = target,
                                        onClearDetectedShortcut = onClearDetectedShortcut,
                                        showWallpaperBackground = state.showWallpaperBackground,
                                        compactRowCount = 1,
                                        predictedTarget = predictedTargetForIndicator,
                                        appIconShape = state.appIconShape,
                                        iconPackPackage = state.selectedIconPackPackage,
                                )
                            },
                            hiddenContent = {
                                if (activeToolCardConfig != null) {
                                    SearchEngineIconsSection(
                                            modifier = searchEnginesModifier,
                                            query = state.query,
                                            enabledEngines = enabledTargets,
                                            onSearchEngineClick = onSearchTargetClick,
                                            onSearchEngineLongPress = onSearchEngineLongPress,
                                            externalScrollState = searchEngineScrollState,
                                            detectedShortcutTarget = state.detectedShortcutTarget,
                                            onClearDetectedShortcut = onClearDetectedShortcut,
                                            showWallpaperBackground = state.showWallpaperBackground,
                                            compactRowCount = state.searchEngineCompactRowCount,
                                            predictedTarget = predictedTargetForIndicator,
                                            appIconShape = state.appIconShape,
                                            iconPackPackage = state.selectedIconPackPackage,
                                            toolActionLabel = activeToolCardConfig.label,
                                            toolActionIcon = activeToolCardConfig.icon,
                                            toolActionAppIconPackage = activeToolCardConfig.appIconPackage,
                                            onToolActionClick = activeToolCardConfig.onClick,
                                            showOnlyToolAction = true,
                                            useInsetContainer = useInsetEngineStrip,
                                            insetOverlap = insetEngineStripOverlap,
                                    )
                                } else {
                                    // Add padding when search engines are hidden to prevent keyboard from
                                    // covering content
                                    Spacer(modifier = searchEnginesModifier)
                                }
                            },
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                androidx.compose.animation.AnimatedVisibility(
                        visible = shouldRenderInlineNumberKeyboardOperators && !showBottomSearchBar,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(durationMillis = 180)) +
                                        expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 220),
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
                                        ),
                ) {
                    NumberKeyboardOperatorPills(
                            modifier = Modifier.imePadding(),
                            isOverlayPresentation = isOverlayPresentation,
                            extendToScreenEdges = false,
                            showWallpaperBackground = state.showWallpaperBackground,
                            onOperatorClick = { operator ->
                                onQueryChanged(state.query + operator)
                            },
                    )
                }
            }
        }

        if (showSearchField && showBottomSearchBar) {
            // The compact engine strip carries its own rounded background, so the search bar
            // stays transparent over the wallpaper instead of sitting on a full-bleed band.
            Box(modifier = Modifier.fillMaxWidth()) {
                searchFieldContent()
            }
            if (useInsetEngineStrip) {
                // Keeps the pills below from sitting flush against the card's bottom edge.
                Spacer(modifier = Modifier.size(DesignTokens.SpacingSmall))
            }

            Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
                androidx.compose.animation.AnimatedVisibility(
                        visible =
                                expandedSection == ExpandedSection.NONE &&
                                        shouldRenderInlineNumberKeyboardOperators,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                                fadeIn(animationSpec = tween(durationMillis = 180)) +
                                        expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 220),
                                        ),
                        exit =
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
                                        ),
                ) {
                    NumberKeyboardOperatorPills(
                            modifier = Modifier.imePadding(),
                            isOverlayPresentation = isOverlayPresentation,
                            extendToScreenEdges = false,
                            showWallpaperBackground = state.showWallpaperBackground,
                            onOperatorClick = { operator ->
                                onQueryChanged(state.query + operator)
                            },
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().extendToScreenEdges()) {
            androidx.compose.animation.AnimatedVisibility(
                    visible = shouldShowOpenKeyboardAction && delayedOpenKeyboardActionVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter =
                            fadeIn(animationSpec = tween(durationMillis = 180)) +
                                    expandVertically(
                                            expandFrom = Alignment.Bottom,
                                            animationSpec = tween(durationMillis = 220),
                                    ),
                    exit =
                            if (hideOpenKeyboardActionInstantly) {
                                ExitTransition.None
                            } else {
                                fadeOut(animationSpec = tween(durationMillis = 130)) +
                                        shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec = tween(durationMillis = 180),
                                        )
                            },
            ) {
                OpenKeyboardAction(
                        text = openKeyboardText,
                        showWallpaperBackground = state.showWallpaperBackground,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
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
    } 
}

private fun String.isPhoneNumberQuery(): Boolean =
        isNotEmpty() &&
                if (first() == '+') {
                    length > 1 && drop(1).all(Char::isDigit)
                } else {
                    all(Char::isDigit)
                }
