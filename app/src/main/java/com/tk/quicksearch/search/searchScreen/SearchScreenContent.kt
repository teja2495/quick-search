package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.search.core.ActiveInfoCard
import com.tk.quicksearch.search.core.DirectSearchStatus
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.WordClockStatus
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.searchEngines.inline.SearchEngineIconsSection
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.components.CurrencyConverterSearchCard
import com.tk.quicksearch.search.searchScreen.components.DictionarySearchCard
import com.tk.quicksearch.search.searchScreen.components.WorldClockSearchCard
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.WordClockIntentParser
import kotlinx.coroutines.delay

private const val OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS = 500L

@Composable
internal fun SearchScreenContent(
        state: SearchUiState,
        renderingState: SectionRenderingState,
        contactsParams: ContactsSectionParams,
        filesParams: FilesSectionParams,
        appShortcutsParams: AppShortcutsSectionParams,
        settingsParams: SettingsSectionParams,
        calendarParams: CalendarSectionParams,
        appsParams: AppsSectionParams,
        onQueryChanged: (String) -> Unit,
        onSelectRetainedQueryHandled: () -> Unit,
        onClearQuery: () -> Unit,
        onSettingsClick: () -> Unit,
        onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        onRequestUsagePermission: () -> Unit,
        onSearchTargetClick: (String, SearchTarget) -> Unit,
        onSearchEngineLongPress: () -> Unit,
        onDirectSearchEmailClick: (String) -> Unit,
        onPhoneNumberClick: (String) -> Unit,
        onWebSuggestionClick: (String) -> Unit,
        onOpenPersonalContextDialog: () -> Unit,
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onOpenDirectSearchConfigure: () -> Unit = {},
        onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
        onOpenSearchHistorySettings: () -> Unit = {},
        onDismissSearchHistoryTip: () -> Unit = {},
        onGeminiModelInfoClick: () -> Unit = {},
        onDictionarySearchClick: () -> Unit = {},
        onCurrencyConverterSearchClick: () -> Unit = {},
        onWorldClockSearchClick: () -> Unit = {},
        onKeyboardSwitchToggle: () -> Unit,
        onOverlayNumberKeyboardUiChanged: ((Boolean, Boolean) -> Unit)? = null,
        onOverlayExpandRequest: () -> Unit = {},
        isOverlayExpanded: Boolean = false,
        onWelcomeAnimationCompleted: (() -> Unit)? = null,
        expandedSection: ExpandedSection,
        manuallySwitchedToNumberKeyboard: Boolean,
        scrollState: androidx.compose.foundation.ScrollState,
        onClearDetectedShortcut: () -> Unit,
        onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
        modifier: Modifier = Modifier,
        isOverlayPresentation: Boolean = false,
        showSearchField: Boolean = true,
        onOpenPermissionsSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var canShowOpenKeyboardPill by
            remember(isOverlayPresentation) { mutableStateOf(!isOverlayPresentation) }
    var delayedOpenKeyboardActionVisible by remember { mutableStateOf(false) }
    var isSearchHistoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isOverlayPresentation) {
        if (!isOverlayPresentation) {
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
    val isCalculatorMode = state.calculatorState.isCalculatorMode
    val isToolMode = state.calculatorState.isToolMode
    val isUnitConverterMode = state.calculatorState.isUnitConverterMode
    val activeToolType = if (isToolMode) state.calculatorState.toolType else null
    val isCurrencyConverterAliasMode = state.isCurrencyConverterAliasMode
    val isWordClockAliasMode = state.isWordClockAliasMode
    val isDictionaryAliasMode = state.isDictionaryAliasMode
    val searchHintText =
            when {
                isCalculatorMode -> stringResource(R.string.calculator_enter_math_expression_hint)
                isUnitConverterMode -> stringResource(R.string.unit_converter_enter_conversion_hint)
                isCurrencyConverterAliasMode ->
                        stringResource(R.string.search_hint_currency_converter)
                isWordClockAliasMode -> stringResource(R.string.search_hint_word_clock)
                isDictionaryAliasMode -> stringResource(R.string.search_hint_dictionary)
                state.detectedAliasSearchSection != null -> when (state.detectedAliasSearchSection) {
                    SearchSection.APPS -> stringResource(R.string.search_hint_apps)
                    SearchSection.APP_SHORTCUTS -> stringResource(R.string.search_hint_app_shortcuts)
                    SearchSection.CONTACTS -> stringResource(R.string.search_hint_contacts)
                    SearchSection.FILES -> stringResource(R.string.search_hint_files)
                    SearchSection.SETTINGS -> stringResource(R.string.search_hint_settings)
                    SearchSection.CALENDAR -> stringResource(R.string.search_hint_calendar)
                    SearchSection.APP_SETTINGS -> stringResource(R.string.search_hint_app_settings)
                }
                else -> stringResource(R.string.search_hint)
            }
    val activeInfoCard = state.activeInfoCard
    val showCalculatorResult =
            activeInfoCard == ActiveInfoCard.CALCULATOR &&
                    (state.calculatorState.isToolMode ||
                    state.calculatorState.result != null ||
                    state.calculatorState.parsedDateMillis != null ||
                    state.calculatorState.dateDiffLabel != null ||
                    state.calculatorState.timeResultLabel != null)
    val showDirectSearch =
            activeInfoCard == ActiveInfoCard.DIRECT_SEARCH &&
                    state.DirectSearchState.status != DirectSearchStatus.Idle
    val showCurrencyConverter =
            activeInfoCard == ActiveInfoCard.CURRENCY_CONVERTER &&
                    state.currencyConverterEnabled &&
                    state.currencyConverterState.status != CurrencyConverterStatus.Idle
    val showWordClock =
            activeInfoCard == ActiveInfoCard.WORD_CLOCK &&
                    state.wordClockEnabled &&
                    state.wordClockState.status != WordClockStatus.Idle
    val showDictionary =
            activeInfoCard == ActiveInfoCard.DICTIONARY &&
                    state.dictionaryEnabled &&
                    state.dictionaryState.status != DictionaryStatus.Idle
    val trimmedQuery = state.query.trim()
    val noInfoCardActive = activeInfoCard == ActiveInfoCard.NONE
    val showCurrencyConverterSearchCard =
            noInfoCardActive &&
                    state.currencyConverterEnabled &&
                    state.hasGeminiApiKey &&
                    trimmedQuery.isNotBlank() &&
                    if (isCurrencyConverterAliasMode) {
                        true
                    } else {
                        CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showWorldClockSearchCard =
            noInfoCardActive &&
                    state.wordClockEnabled &&
                    state.hasGeminiApiKey &&
                    trimmedQuery.isNotBlank() &&
                    !showCurrencyConverterSearchCard &&
                    if (isWordClockAliasMode) {
                        true
                    } else {
                        WordClockIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showDictionarySearchCard =
            noInfoCardActive &&
                    state.dictionaryEnabled &&
                    state.hasGeminiApiKey &&
                    trimmedQuery.isNotBlank() &&
                    !showCurrencyConverterSearchCard &&
                    !showWorldClockSearchCard &&
                    if (isDictionaryAliasMode) {
                        true
                    } else {
                        DictionaryIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val hideCompactSearchEnginesInToolMode =
            (isToolMode ||
                    showCurrencyConverter ||
                    showWordClock ||
                    showDictionary ||
                    isCurrencyConverterAliasMode ||
                    isWordClockAliasMode) &&
                    state.isSearchEngineCompactMode
    val shouldShowNumberKeyboardOperators =
            isImeVisible && (manuallySwitchedToNumberKeyboard || isCalculatorMode)
    val showBottomSearchBar = showSearchField && state.bottomSearchBarEnabled
    val useOverlayThemeTints = state.backgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val searchColorTheme =
            resolveSearchColorTheme(
                    theme = state.appTheme,
                    backgroundSource = state.backgroundSource,
                    isDarkMode = isDarkMode,
                    intensity = state.overlayThemeIntensity,
            )
    val overlayCardColor =
            if (useOverlayThemeTints) {
                appThemeResultCardColor(
                        theme = state.appTheme,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
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
                    canShowOpenKeyboardPill &&
                    !isSearchHistoryExpanded

    LaunchedEffect(shouldShowOpenKeyboardAction) {
        if (!shouldShowOpenKeyboardAction) {
            delayedOpenKeyboardActionVisible = false
            return@LaunchedEffect
        }

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
                            !isWordClockAliasMode &&
                            !isDictionaryAliasMode
            ) {
                stringResource(R.string.keyboard_switch_to_number)
            } else {
                null
            }
    val shouldShowPredictedHighlight = isImeVisible
    val predictedTarget =
            remember(
                    shouldShowPredictedHighlight,
                    state.query,
                    renderingState.displayApps,
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
                    resolvePredictedSubmitTarget(
                            query = state.query,
                            renderingState = renderingState,
                            enabledTargets = enabledTargets,
                            detectedShortcutTarget = state.detectedShortcutTarget,
                            searchTargetsOrder = state.searchTargetsOrder,
                            defaultBrowserPackage = resolveDefaultBrowserPackage(context),
                    )
                }
            }
    val predictedTargetForIndicator =
            if (state.topResultIndicatorEnabled) predictedTarget else null

    // Search engine scroll state for auto-scroll during onboarding
    val searchEngineScrollState = rememberLazyListState()

    // Auto-scroll search engines during onboarding
    // Temporarily disabled per request (keep code for quick re-enable later).
//    LaunchedEffect(state.showSearchEngineOnboarding) {
//        if (state.showSearchEngineOnboarding) {
//            // Smooth continuous scroll that loops
//            while (true) {
//                // Check if we can scroll further
//                val layoutInfo = searchEngineScrollState.layoutInfo
//                val canScrollForward =
//                        layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
//                            lastItem.index < layoutInfo.totalItemsCount - 1 ||
//                                    lastItem.offset + lastItem.size > layoutInfo.viewportEndOffset
//                        }
//                                ?: false
//
//                if (!canScrollForward) {
//                    // Reached the end, scroll back to start and continue
//                    delay(500) // Pause briefly at the end
//                    searchEngineScrollState.animateScrollToItem(index = 0, scrollOffset = 0)
//                    delay(500) // Pause briefly at the start
//                    continue
//                }
//
//                // Get current scroll position
//                val currentIndex = searchEngineScrollState.firstVisibleItemIndex
//                val currentOffset = searchEngineScrollState.firstVisibleItemScrollOffset
//
//                // Increment by small amount for smooth scroll
//                val newOffset = currentOffset + 2
//
//                // Smooth scroll
//                delay(30) // Small delay for smooth effect
//
//                searchEngineScrollState.scrollToItem(index = currentIndex, scrollOffset = newOffset)
//            }
//        } else {
//            // When onboarding is dismissed, scroll back to start
//            searchEngineScrollState.animateScrollToItem(index = 0, scrollOffset = 0)
//        }
//    }

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
            if (isOverlayPresentation || shouldRenderInlineNumberKeyboardOperators) {
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

    val searchFieldModifier =
            if (showBottomSearchBar) {
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
                dismissKeyboardBeforeSettingsClick = isOverlayPresentation,
                enabledTargets = enabledTargets,
                shortcutCodes = state.shortcutCodes,
                shortcutEnabled = state.shortcutEnabled,
                isSearchEngineAliasSuffixEnabled = state.isSearchEngineAliasSuffixEnabled,
                shouldUseNumberKeyboard = manuallySwitchedToNumberKeyboard || isCalculatorMode,
                detectedShortcutTarget = state.detectedShortcutTarget,
                detectedAliasSearchSection = state.detectedAliasSearchSection,
                isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
                isWordClockAliasMode = isWordClockAliasMode,
                isDictionaryAliasMode = isDictionaryAliasMode,
                activeToolType = activeToolType,
                isCalculatorMode = isCalculatorMode,
                placeholderText = searchHintText,
                showWelcomeAnimation = state.showSearchBarWelcomeAnimation,
                showWallpaperBackground = state.showWallpaperBackground,
                autoFocusOnStart = state.openKeyboardOnLaunch,
                onClearDetectedShortcut = onClearDetectedShortcut,
                onSectionSelected = onSectionSelected,
                onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
                forceRestingOutline = showBottomSearchBar,
                modifier = searchFieldModifier,
                onSearchAction = {
                    val trimmedQuery = state.query.trim()
                    val isUrlQuery = isLikelyWebUrl(trimmedQuery)
                    val defaultBrowserPackage = resolveDefaultBrowserPackage(context)

                    // If query has trailing/leading spaces, trim it first
                    if (state.query != trimmedQuery) {
                        onQueryChanged(trimmedQuery)
                    }

                    if (isUrlQuery && trimmedQuery.isNotBlank()) {
                        val browserTarget =
                                defaultBrowserTarget(state.searchTargetsOrder, defaultBrowserPackage)
                        if (browserTarget != null) {
                            onSearchTargetClick(trimmedQuery, browserTarget)
                            return@PersistentSearchBar false
                        }
                    }

                    val firstApp = renderingState.displayApps.firstOrNull()
                    if (firstApp != null) {
                        onAppClick(firstApp)
                        return@PersistentSearchBar false
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
                        val primaryTarget = enabledTargets.firstOrNull()
                        if (primaryTarget != null && trimmedQuery.isNotBlank()) {
                            onSearchTargetClick(trimmedQuery, primaryTarget)
                        }
                    }
                    false
                },
        )
    }

    CompositionLocalProvider(LocalSearchColorTheme provides searchColorTheme) {
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
                appsParams = appsParams,
                predictedTarget = predictedTargetForIndicator,
                onRequestUsagePermission = onRequestUsagePermission,
                scrollState = scrollState,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onDirectSearchEmailClick,
                onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                onWebSuggestionClick = onWebSuggestionClick,
                onSearchTargetClick = onSearchTargetClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
                onDeleteRecentItem = onDeleteRecentItem,
                onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                onDismissSearchHistoryTip = onDismissSearchHistoryTip,
                onGeminiModelInfoClick = onGeminiModelInfoClick,
                onSearchHistoryExpandedChange = { isSearchHistoryExpanded = it },
                onOpenPermissionsSettings = onOpenPermissionsSettings,
                showCalculator = showCalculatorResult,
                showCurrencyConverter = showCurrencyConverter,
                showWordClock = showWordClock,
                showDictionary = showDictionary,
                showDirectSearch = showDirectSearch,
                directSearchState = state.DirectSearchState,
                isOverlayPresentation = isOverlayPresentation,
        )

        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded, when search engine section is disabled,
        // or when a shortcut is detected
        // Fixed search engines section at the bottom (above keyboard, not scrollable)
        // Hide when files or contacts are expanded
        if (expandedSection == ExpandedSection.NONE) {
            AnimatedVisibility(
                    visible = keyboardSwitchText != null,
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
                }
            }

            if (!hideCompactSearchEnginesInToolMode && !isSearchHistoryExpanded) {
                AnimatedVisibility(
                        visible = showCurrencyConverterSearchCard,
                        enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 50)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                ) {
                    CurrencyConverterSearchCard(
                            modifier =
                                    Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = DesignTokens.SpacingXSmall),
                            onClick = onCurrencyConverterSearchClick,
                            showWallpaperBackground = state.showWallpaperBackground,
                    )
                }
                AnimatedVisibility(
                        visible = showWorldClockSearchCard,
                        enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 50)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                ) {
                    WorldClockSearchCard(
                            modifier =
                                    Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = DesignTokens.SpacingXSmall),
                            onClick = onWorldClockSearchClick,
                            showWallpaperBackground = state.showWallpaperBackground,
                    )
                }
                AnimatedVisibility(
                        visible = showDictionarySearchCard,
                        enter = fadeIn(animationSpec = tween(durationMillis = 140, delayMillis = 50)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                ) {
                    DictionarySearchCard(
                            modifier =
                                    Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = DesignTokens.SpacingXSmall),
                            onClick = onDictionarySearchClick,
                            showWallpaperBackground = state.showWallpaperBackground,
                    )
                }
                CompositionLocalProvider(
                        LocalOverlayResultCardColor provides overlayCardColor,
                        LocalOverlayDividerColor provides overlayDividerTint,
                        LocalOverlayActionColor provides overlayActionTint,
                ) {
                    SearchEnginesVisibility(
                            enginesState = state.searchEnginesState,
                            modifier = searchEnginesModifier,
                            compactContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        hasAppResults = renderingState.hasAppResults,
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
                                )
                            },
                            fullContent = {
                                SearchEngineIconsSection(
                                        query = state.query,
                                        hasAppResults = renderingState.hasAppResults,
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
                                )
                            },
                            shortcutContent = { target ->
                                SearchEngineIconsSection(
                                        query = state.query,
                                        hasAppResults = renderingState.hasAppResults,
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
                                )
                            },
                            hiddenContent = {
                                // Add padding when search engines are hidden to prevent keyboard from
                                // covering content
                                Spacer(modifier = searchEnginesModifier)
                            },
                    )
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
            val shouldShowCompactBottomBarBackground =
                    expandedSection == ExpandedSection.NONE &&
                    state.isSearchEngineCompactMode &&
                            !hideCompactSearchEnginesInToolMode &&
                            !isSearchHistoryExpanded &&
                            state.detectedShortcutTarget == null &&
                            state.detectedAliasSearchSection == null &&
                            !state.isCurrencyConverterAliasMode &&
                            !state.isWordClockAliasMode

            Box(
                    modifier =
                            Modifier
                                    .fillMaxWidth()
                                    .then(
                                            if (shouldShowCompactBottomBarBackground) {
                                                Modifier
                                                        .extendToScreenEdges()
                                                        .background(
                                                                AppColors.getCompactSectionBackground(
                                                                        state.showWallpaperBackground
                                                                ).copy(alpha = 0.9f)
                                                        )
                                            } else {
                                                Modifier
                                            }
                                    )
            ) {
                Box(
                        modifier =
                                Modifier
                                        .fillMaxWidth()
                                        .then(
                                                if (shouldShowCompactBottomBarBackground) {
                                                    Modifier.padding(horizontal = DesignTokens.SpacingXLarge)
                                                } else {
                                                    Modifier
                                                }
                                        )
                ) {
                    searchFieldContent()
                }
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
                            fadeOut(animationSpec = tween(durationMillis = 130)) +
                                    shrinkVertically(
                                            shrinkTowards = Alignment.Bottom,
                                            animationSpec = tween(durationMillis = 180),
                                    ),
            ) {
                OpenKeyboardAction(
                        text = openKeyboardText,
                        showWallpaperBackground = state.showWallpaperBackground,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            keyboardController?.show()
                        },
                )
            }
        }
    }
    } 
}
