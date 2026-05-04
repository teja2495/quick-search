package com.tk.quicksearch.search.searchScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CurrencyConverterStatus
import com.tk.quicksearch.search.core.DictionaryStatus
import com.tk.quicksearch.search.core.AiSearchStatus
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.core.WordClockStatus
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.core.rememberSectionRenderContext
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.extendToScreenEdges
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.searchEngines.inline.SearchEngineIconsSection
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalDeviceDynamicColorsActive
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SearchContentArea
import com.tk.quicksearch.search.searchScreen.appThemeActionColor
import com.tk.quicksearch.search.searchScreen.appThemeDividerColor
import com.tk.quicksearch.search.searchScreen.appThemeResultCardColor
import com.tk.quicksearch.search.searchScreen.resolveSearchColorTheme
import com.tk.quicksearch.shared.ui.theme.LocalSearchColorTheme
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.tools.aiTools.CurrencyConversionIntentParser
import com.tk.quicksearch.tools.aiTools.DictionaryIntentParser
import com.tk.quicksearch.tools.aiTools.WordClockIntentParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val OPEN_KEYBOARD_ACTION_APPEAR_DELAY_MS = 500L
private const val SEARCH_HINT_ROTATION_INTERVAL_MS = 5000L

private data class ToolCardConfig(
        val label: String,
        val icon: ImageVector,
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
        onClearQuery: () -> Unit,
        onSettingsClick: () -> Unit,
        onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
        onRequestUsagePermission: () -> Unit,
        onSearchTargetClick: (String, SearchTarget) -> Unit,
        onSearchEngineLongPress: () -> Unit,
        onAiSearchEmailClick: (String) -> Unit,
        onPhoneNumberClick: (String) -> Unit,
        onWebSuggestionClick: (String) -> Unit,
        onOpenPersonalContextDialog: () -> Unit,
        onCustomizeSearchEnginesClick: () -> Unit = {},
        onOpenAiSearchConfigure: () -> Unit = {},
        onDeleteRecentItem: (RecentSearchEntry) -> Unit = {},
        onClearRecentItems: () -> Unit = {},
        onOpenSearchHistorySettings: () -> Unit = {},
        onDismissSearchHistoryTip: () -> Unit = {},
        onGeminiModelInfoClick: () -> Unit = {},
        onCurrencyConversionClick: () -> Unit = {},
        onDictionarySearchClick: () -> Unit = {},
        onWordClockSearchClick: () -> Unit = {},
        onCustomToolSearchClick: () -> Unit = {},
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
        getAllContactActionTriggers: () -> Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, com.tk.quicksearch.search.data.preferences.ResultTrigger> = { emptyMap() },
        onContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var canShowOpenKeyboardPill by
            remember(isOverlayPresentation) { mutableStateOf(!isOverlayPresentation) }
    var delayedOpenKeyboardActionVisible by remember { mutableStateOf(false) }
    var hideOpenKeyboardActionInstantly by remember { mutableStateOf(false) }
    var isSearchHistoryExpanded by remember { mutableStateOf(false) }
    var searchHistoryCollapseRequestKey by remember { mutableStateOf(0) }
    val openKeyboardActionScope = rememberCoroutineScope()

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
    val activeCustomTool = state.detectedCustomToolId?.let { id -> state.customTools.find { it.id == id } }

    val hintSearchAnything = stringResource(R.string.search_hint)
    val cycleHints = stringArrayResource(R.array.search_hints_cycle)
    // Indices must stay in sync with R.array.search_hints_cycle order in strings.xml
    val defaultHints = remember(
        hintSearchAnything,
        cycleHints,
        state.disabledSections,
        state.hasContactPermission,
        state.hasCalendarPermission,
        state.hasFilePermission,
        state.hasGeminiApiKey,
        state.calculatorEnabled,
        state.unitConverterEnabled,
        state.dateCalculatorEnabled,
        state.currencyConverterEnabled,
        state.wordClockEnabled,
        state.dictionaryEnabled,
    ) {
        val gated = listOf(
            cycleHints[0] to (SearchSection.CONTACTS !in state.disabledSections && state.hasContactPermission),
            cycleHints[1] to (SearchSection.FILES !in state.disabledSections && state.hasFilePermission),
            cycleHints[2] to (SearchSection.CALENDAR !in state.disabledSections && state.hasCalendarPermission),
            cycleHints[3] to (SearchSection.APPS !in state.disabledSections),
            cycleHints[4] to (SearchSection.APP_SHORTCUTS !in state.disabledSections),
            cycleHints[5] to (SearchSection.SETTINGS !in state.disabledSections),
            cycleHints[6] to (state.currencyConverterEnabled && state.hasGeminiApiKey),
            cycleHints[7] to state.unitConverterEnabled,
            cycleHints[8] to state.dateCalculatorEnabled,
            cycleHints[9] to state.calculatorEnabled,
            cycleHints[10] to (state.wordClockEnabled && state.hasGeminiApiKey),
            cycleHints[11] to (state.dictionaryEnabled && state.hasGeminiApiKey),
        )
        listOf(hintSearchAnything) + gated.filter { it.second }.map { it.first }.shuffled()
    }

    val isDefaultHintMode =
            !isCalculatorMode &&
                    !isUnitConverterMode &&
                    !isCurrencyConverterAliasMode &&
                    !isWordClockAliasMode &&
                    !isDictionaryAliasMode &&
                    activeCustomTool == null &&
                    state.detectedAliasSearchSection == null

    var hintIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isDefaultHintMode) {
        if (!isDefaultHintMode) {
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
                isWordClockAliasMode -> stringResource(R.string.search_hint_word_clock)
                isDictionaryAliasMode -> stringResource(R.string.search_hint_dictionary)
                activeCustomTool != null -> activeCustomTool.name
                state.detectedAliasSearchSection != null ->
                    stringResource(
                        SearchSectionUiMetadataRegistry
                            .metadataFor(state.detectedAliasSearchSection)
                            .searchHintRes,
                    )
                else -> defaultHints[hintIndex % defaultHints.size]
            }
    val showCurrencyConverter =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    state.currencyConverterState.status != CurrencyConverterStatus.Idle
    val showWordClock =
            (state.wordClockEnabled || isWordClockAliasMode) &&
                    state.wordClockState.status != WordClockStatus.Idle
    val showDictionary =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.dictionaryState.status != DictionaryStatus.Idle
    val showCalculatorResult =
            state.calculatorState.isToolMode ||
                    state.calculatorState.result != null ||
                    state.calculatorState.parsedDateMillis != null ||
                    state.calculatorState.dateDiffLabel != null ||
                    state.calculatorState.timeResultLabel != null
    val trimmedQuery = state.query.trim()
    val showCurrencyConverterSearchCard =
            (state.currencyConverterEnabled || isCurrencyConverterAliasMode) &&
                    state.hasGeminiApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWordClock &&
                    !showDictionary &&
                    if (isCurrencyConverterAliasMode) {
                        true // always show when alias mode is active
                    } else {
                        trimmedQuery.isNotBlank() &&
                                CurrencyConversionIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showDictionarySearchCard =
            (state.dictionaryEnabled || isDictionaryAliasMode) &&
                    state.hasGeminiApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWordClock &&
                    !showDictionary &&
                    if (isDictionaryAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                DictionaryIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showWordClockSearchCard =
            (state.wordClockEnabled || isWordClockAliasMode) &&
                    state.hasGeminiApiKey &&
                    !showCalculatorResult &&
                    !showCurrencyConverter &&
                    !showWordClock &&
                    !showDictionary &&
                    if (isWordClockAliasMode) {
                        true
                    } else {
                        trimmedQuery.isNotBlank() &&
                                WordClockIntentParser.parseConfirmed(trimmedQuery) != null
                    }
    val showCustomToolSearchCard =
            activeCustomTool != null &&
                    state.hasGeminiApiKey &&
                    !showCalculatorResult &&
                    state.AiSearchState.status == AiSearchStatus.Idle
    val isToolAliasMode =
            isCurrencyConverterAliasMode ||
                    isWordClockAliasMode ||
                    isDictionaryAliasMode ||
                    activeCustomTool != null
    val shouldShowNumberKeyboardOperators =
            isImeVisible && (manuallySwitchedToNumberKeyboard || isCalculatorMode)
    val showBottomSearchBar = showSearchField && state.bottomSearchBarEnabled
    val useOverlayThemeTints = !state.deviceThemeEnabled && state.backgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val searchColorTheme =
            if (state.deviceThemeEnabled) {
                null
            } else {
                resolveSearchColorTheme(
                        theme = state.appTheme,
                        backgroundSource = state.backgroundSource,
                        isDarkMode = isDarkMode,
                        intensity = state.overlayThemeIntensity,
                )
            }
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
                            !isWordClockAliasMode &&
                            !isDictionaryAliasMode &&
                            activeCustomTool == null
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
    val predictedTargetForIndicator =
            if (state.topResultIndicatorEnabled &&
                    !showCurrencyConverterSearchCard &&
                    !showDictionarySearchCard &&
                    !showWordClockSearchCard &&
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
                    showWordClock ||
                    showDictionary ||
                    state.detectedShortcutTarget != null ||
                    state.detectedAliasSearchSection != null ||
                    state.isCurrencyConverterAliasMode ||
                    state.isWordClockAliasMode ||
                    state.isDictionaryAliasMode ||
                    state.detectedCustomToolId != null
    val deferTopMatchSubmitUntilAppsReady =
            state.query.isNotBlank() &&
                    state.isAppSearchInProgress &&
                    !renderingState.hasAppResults
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
    val topMatchesForSubmit =
            rememberTopMatches(
                    query = state.query,
                    renderingState = renderingState,
                    context = topMatchSubmitContext,
                    params = topMatchSubmitParams,
                    limit = state.topMatchesLimit,
                    topMatchesSectionOrder = state.topMatchesSectionOrder,
                    disabledTopMatchesSections = state.disabledTopMatchesSections,
            )
    val shouldSubmitTopMatch =
            state.topMatchesEnabled &&
                    state.query.isNotBlank() &&
                    !hideResultsForTopMatchSubmit &&
                    expandedSection == ExpandedSection.NONE &&
                    !isSearchHistoryExpanded &&
                    !deferTopMatchSubmitUntilAppsReady &&
                    topMatchesForSubmit.isNotEmpty()
    val activeToolCardConfig =
            if (isSearchHistoryExpanded) {
                null
            } else {
                when {
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
                    showCustomToolSearchCard ->
                            ToolCardConfig(
                                    label = activeCustomTool?.name.orEmpty(),
                                    icon = Icons.Rounded.Construction,
                                    onClick = onCustomToolSearchClick,
                            )
                    showWordClockSearchCard ->
                            ToolCardConfig(
                                    label = stringResource(R.string.get_time),
                                    icon = Icons.Rounded.AccessTime,
                                    onClick = onWordClockSearchClick,
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
                detectedCustomToolId = state.detectedCustomToolId,
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
                    if (showCustomToolSearchCard) {
                        onCustomToolSearchClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }
                    if (showWordClockSearchCard) {
                        onWordClockSearchClick()
                        return@PersistentSearchBar true // keep keyboard open
                    }

                    if (shouldSubmitTopMatch) {
                        openTopMatch(
                                item = topMatchesForSubmit.first(),
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
                notesParams = notesParams,
                appsParams = appsParams,
                predictedTarget = predictedTargetForIndicator,
                onRequestUsagePermission = onRequestUsagePermission,
                scrollState = scrollState,
                onPhoneNumberClick = onPhoneNumberClick,
                onEmailClick = onAiSearchEmailClick,
                onOpenPersonalContextDialog = onOpenPersonalContextDialog,
                onWebSuggestionClick = onWebSuggestionClick,
                onSearchTargetClick = onSearchTargetClick,
                onSearchEngineLongPress = onSearchEngineLongPress,
                onCustomizeSearchEnginesClick = onCustomizeSearchEnginesClick,
                onOpenAiSearchConfigure = onOpenAiSearchConfigure,
                onDeleteRecentItem = onDeleteRecentItem,
                onClearRecentItems = onClearRecentItems,
                onOpenSearchHistorySettings = onOpenSearchHistorySettings,
                onDismissSearchHistoryTip = onDismissSearchHistoryTip,
                onGeminiModelInfoClick = onGeminiModelInfoClick,
                onSearchHistoryExpandedChange = { isSearchHistoryExpanded = it },
                searchHistoryCollapseRequestKey = searchHistoryCollapseRequestKey,
                isSearchHistoryExpanded = isSearchHistoryExpanded,
                onSearchHistoryCollapseRequested = { searchHistoryCollapseRequestKey += 1 },
                onOpenPermissionsSettings = onOpenPermissionsSettings,
                showCalculator = state.calculatorState.isToolMode || state.calculatorState.result != null || state.calculatorState.parsedDateMillis != null || state.calculatorState.dateDiffLabel != null || state.calculatorState.timeResultLabel != null,
                showCurrencyConverter = showCurrencyConverter,
                showWordClock = showWordClock,
                showDictionary = showDictionary,
                showAiSearch = state.AiSearchState.status != AiSearchStatus.Idle,
                aiSearchState = state.AiSearchState,
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

            if (!isSearchHistoryExpanded) {
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
                                        toolActionLabel = activeToolCardConfig?.label,
                                        toolActionIcon = activeToolCardConfig?.icon,
                                        onToolActionClick = activeToolCardConfig?.onClick,
                                        showOnlyToolAction = showOnlyToolActionInCompactSection,
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
                                if (activeToolCardConfig != null) {
                                    SearchEngineIconsSection(
                                            modifier = searchEnginesModifier,
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
                                            toolActionLabel = activeToolCardConfig.label,
                                            toolActionIcon = activeToolCardConfig.icon,
                                            onToolActionClick = activeToolCardConfig.onClick,
                                            showOnlyToolAction = true,
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
                            !isSearchHistoryExpanded &&
                            state.detectedShortcutTarget == null &&
                            state.detectedAliasSearchSection == null

            Box(
                    modifier =
                            Modifier
                                    .fillMaxWidth()
                                    .then(
                                            if (shouldShowCompactBottomBarBackground) {
                                                val compactBackground =
                                                        AppColors.getCompactSectionBackground(
                                                                state.showWallpaperBackground
                                                        )
                                                val compactBackgroundColor =
                                                        if (state.showWallpaperBackground && LocalDeviceDynamicColorsActive.current) {
                                                            compactBackground
                                                        } else {
                                                            compactBackground.copy(alpha = 0.9f)
                                                        }
                                                Modifier
                                                        .extendToScreenEdges()
                                                        .background(compactBackgroundColor)
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
