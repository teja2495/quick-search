package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutsSettingsSection
import com.tk.quicksearch.settings.searchEnginesScreen.DirectSearchSetupCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun SettingsDetailLevel2Screen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    hasUsagePermission: Boolean,
    appShortcutFocusShortcut: StaticShortcut? = null,
    appShortcutFocusPackageName: String? = null,
    appShortcutSources: List<AppShortcutSource> = emptyList(),
    searchTargets: List<SearchTarget> = emptyList(),
    onAppShortcutFocusHandled: () -> Unit = {},
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
) {
    if (!detailType.isLevel2()) return

    BackHandler(onBack = callbacks.onBack)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    var appShortcutsSearchQuery by remember { mutableStateOf("") }
    var appManagementSearchQuery by remember { mutableStateOf("") }
    var calendarEventsSearchQuery by remember { mutableStateOf("") }
    var appShortcutsCollapseAllTrigger by remember { mutableIntStateOf(0) }

    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty()

    LaunchedEffect(detailType, hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }
    SettingsScreenBackground(
        appTheme = state.appTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        modifier = modifier,
    ) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailHeader(
                title = stringResource(detailType.titleResId()),
                onBack = callbacks.onBack,
            )

            if (detailType == SettingsDetailType.APP_MANAGEMENT) {
                AppManagementSettingsSection(
                    apps = state.allApps,
                    hasUsagePermission = hasUsagePermission,
                    iconPackPackage = state.selectedIconPackPackage,
                    searchQuery = appManagementSearchQuery,
                    onRequestAppUninstall = callbacks.onRequestAppUninstall,
                    onOpenAppInfo = callbacks.onOpenAppInfo,
                    onRefreshApps = callbacks.onRefreshApps,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.APP_SHORTCUTS) {
                AppShortcutsSettingsSection(
                    shortcuts = state.allAppShortcuts,
                    disabledShortcutIds = state.disabledAppShortcutIds,
                    iconPackPackage = state.selectedIconPackPackage,
                    searchQuery = appShortcutsSearchQuery,
                    collapseAllTrigger = appShortcutsCollapseAllTrigger,
                    onShortcutEnabledChange = callbacks.onToggleAppShortcutEnabled,
                    onShortcutNameClick = callbacks.onLaunchAppShortcut,
                    shortcutSources = appShortcutSources,
                    onAddShortcutFromSource = callbacks.onAddAppShortcutFromSource,
                    onAddAppDeepLinkShortcut = callbacks.onAddAppDeepLinkShortcut,
                    searchTargets = searchTargets,
                    onAddQueryShortcut = callbacks.onAddSearchTargetQueryShortcut,
                    onUpdateCustomShortcut = callbacks.onUpdateCustomAppShortcut,
                    onDeleteCustomShortcut = callbacks.onDeleteCustomAppShortcut,
                    focusShortcut = appShortcutFocusShortcut,
                    focusPackageName = appShortcutFocusPackageName,
                    onFocusHandled = onAppShortcutFocusHandled,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else if (detailType == SettingsDetailType.TOOLS) {
                Column(
                        modifier =
                                Modifier.settingsContentWidth()
                                        .fillMaxHeight()
                                        .align(Alignment.CenterHorizontally)
                                        .padding(
                                                start = DesignTokens.ContentHorizontalPadding,
                                                end = DesignTokens.ContentHorizontalPadding,
                                                bottom = DesignTokens.SectionTopPadding,
                                        ),
                ) {
                    ToolsSettingsSection(
                            calculatorEnabled = state.calculatorEnabled,
                            calculatorAlias =
                                    state.shortcutCodes[
                                            com.tk.quicksearch.searchEngines.AliasHandler
                                                    .CALCULATOR_ALIAS_FEATURE_ID
                                    ].orEmpty(),
                            unitConverterEnabled = state.unitConverterEnabled,
                            unitConverterAlias =
                                    state.shortcutCodes[
                                            com.tk.quicksearch.searchEngines.AliasHandler
                                                    .UNIT_CONVERTER_ALIAS_FEATURE_ID
                                    ].orEmpty(),
                            dateCalculatorEnabled = state.dateCalculatorEnabled,
                            dateCalculatorAlias =
                                    state.shortcutCodes[
                                            com.tk.quicksearch.searchEngines.AliasHandler
                                                    .DATE_CALCULATOR_ALIAS_FEATURE_ID
                                    ].orEmpty(),
                            hasGeminiApiKey = state.hasGeminiApiKey,
                            currencyConverterEnabled = state.currencyConverterEnabled,
                            currencyConverterAlias =
                                    state.shortcutCodes[
                                            com.tk.quicksearch.searchEngines.AliasHandler
                                                    .CURRENCY_CONVERTER_ALIAS_FEATURE_ID
                                    ].orEmpty(),
                            existingShortcuts = state.shortcutCodes,
                            onSetCalculatorAlias = callbacks.onSetCalculatorAlias,
                            onSetUnitConverterAlias = callbacks.onSetUnitConverterAlias,
                            onSetDateCalculatorAlias = callbacks.onSetDateCalculatorAlias,
                            onSetCurrencyConverterAlias = callbacks.onSetCurrencyConverterAlias,
                            onCalculatorToggle = callbacks.onToggleCalculator,
                            onUnitConverterToggle = callbacks.onToggleUnitConverter,
                            onDateCalculatorToggle = callbacks.onToggleDateCalculator,
                            onCurrencyConverterToggle = callbacks.onToggleCurrencyConverter,
                            onNavigateToGeminiApiSetup = {
                                onNavigateToDetail(SettingsDetailType.GEMINI_API_CONFIG)
                            },
                            onNavigateToUnitConverterInfo = {
                                onNavigateToDetail(SettingsDetailType.UNIT_CONVERTER_INFO)
                            },
                            onNavigateToDateCalculatorInfo = {
                                onNavigateToDetail(SettingsDetailType.DATE_CALCULATOR_INFO)
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            } else if (detailType == SettingsDetailType.CALENDAR_EVENTS) {
                CalendarEventsSettingsSection(
                    onEventClick = callbacks.onLaunchCalendarEvent,
                    searchQuery = calendarEventsSearchQuery,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .verticalScroll(scrollState)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom =
                                    if (detailType == SettingsDetailType.APP_SHORTCUTS) {
                                        96.dp
                                    } else {
                                        DesignTokens.SectionTopPadding
                                    },
                            ),
                ) {
                    when (detailType) {
                        SettingsDetailType.EXCLUDED_ITEMS -> {
                            ExcludedItemScreen(
                                suggestionExcludedApps = state.suggestionExcludedApps,
                                resultExcludedApps = state.resultExcludedApps,
                                excludedContacts = state.excludedContacts,
                                excludedFiles = state.excludedFiles,
                                excludedFileExtensions = state.excludedFileExtensions,
                                excludedSettings = state.excludedSettings,
                                excludedAppShortcuts = state.excludedAppShortcuts,
                                onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                                onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                                onRemoveExcludedFileExtension = callbacks.onRemoveExcludedFileExtension,
                                onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                                onRemoveExcludedAppShortcut = callbacks.onRemoveExcludedAppShortcut,
                                showTitle = false,
                                iconPackPackage = state.selectedIconPackPackage,
                            )
                        }

                        SettingsDetailType.APP_SHORTCUTS -> Unit
                        SettingsDetailType.DEVICE_SETTINGS -> {
                            DeviceSettingsSettingsSection(
                                settings = state.allDeviceSettings,
                                onSettingClick = callbacks.onLaunchDeviceSetting,
                            )
                        }

                        SettingsDetailType.CALLS_TEXTS -> {
                            CallsTextsSettingsSection(
                                messagingApp = state.messagingApp,
                                callingApp = state.callingApp,
                                onSetMessagingApp = callbacks.onSetMessagingApp,
                                onSetCallingApp = callbacks.onSetCallingApp,
                                directDialEnabled = state.directDialEnabled,
                                onToggleDirectDial = callbacks.onToggleDirectDial,
                                hasCallPermission = PermissionHelper.checkCallPermission(context),
                                contactsSectionEnabled = true,
                                isWhatsAppInstalled = state.isWhatsAppInstalled,
                                isTelegramInstalled = state.isTelegramInstalled,
                                isSignalInstalled = state.isSignalInstalled,
                                isGoogleMeetInstalled = state.isGoogleMeetInstalled,
                                modifier = Modifier,
                            )
                        }

                        SettingsDetailType.FILES -> {
                            FileTypesSection(
                                enabledFileTypes = state.enabledFileTypes,
                                onToggleFileType = callbacks.onToggleFileType,
                                showFolders = state.showFolders,
                                onToggleFolders = callbacks.onToggleFolders,
                                showSystemFiles = state.showSystemFiles,
                                onToggleSystemFiles = callbacks.onToggleSystemFiles,
                                folderWhitelistPatterns = state.folderWhitelistPatterns,
                                onSetFolderWhitelistPatterns = callbacks.onSetFolderWhitelistPatterns,
                                folderBlacklistPatterns = state.folderBlacklistPatterns,
                                onSetFolderBlacklistPatterns = callbacks.onSetFolderBlacklistPatterns,
                                excludedExtensions = state.excludedFileExtensions,
                                onRemoveExcludedExtension = callbacks.onRemoveExcludedFileExtension,
                                showTitle = false,
                                modifier = Modifier,
                            )
                        }

                        SettingsDetailType.GEMINI_API_CONFIG -> {
                            DirectSearchSetupCard(
                                directSearchEnabled = state.hasGeminiApiKey,
                                onSetGeminiApiKey = callbacks.onSetGeminiApiKey,
                                geminiApiKeyLast4 = state.geminiApiKeyLast4,
                                isSavingGeminiApiKey = state.isSavingGeminiApiKey,
                                onOpenDirectSearchConfigure = null,
                            )
                            if (state.hasGeminiApiKey) {
                                APIKeySettingsSection(
                                    personalContext = state.personalContext,
                                    geminiModel = state.geminiModel,
                                    geminiGroundingEnabled = state.geminiGroundingEnabled,
                                    availableGeminiModels = state.availableGeminiModels,
                                    onSetPersonalContext = callbacks.onSetPersonalContext,
                                    onSetGeminiModel = callbacks.onSetGeminiModel,
                                    onSetGeminiGroundingEnabled = callbacks.onSetGeminiGroundingEnabled,
                                    onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        SettingsDetailType.TOOLS -> Unit

                        SettingsDetailType.UNIT_CONVERTER_INFO -> {
                            UnitConverterInfoSection(modifier = Modifier.fillMaxWidth())
                        }

                        SettingsDetailType.DATE_CALCULATOR_INFO -> {
                            DateCalculatorInfoSection(modifier = Modifier.fillMaxWidth())
                        }

                        else -> Unit
                    }
                }
            }
        }

        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            FloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_action_clear_all),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (
            detailType == SettingsDetailType.APP_SHORTCUTS ||
                detailType == SettingsDetailType.APP_MANAGEMENT ||
                detailType == SettingsDetailType.CALENDAR_EVENTS
        ) {
            val query =
                when (detailType) {
                    SettingsDetailType.APP_SHORTCUTS -> appShortcutsSearchQuery
                    SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery
                    SettingsDetailType.CALENDAR_EVENTS -> calendarEventsSearchQuery
                    else -> ""
                }
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = { updatedQuery ->
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> appShortcutsSearchQuery = updatedQuery
                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = updatedQuery
                        SettingsDetailType.CALENDAR_EVENTS -> {
                            calendarEventsSearchQuery = updatedQuery
                        }

                        else -> Unit
                    }
                },
                onClear = {
                    when (detailType) {
                        SettingsDetailType.APP_SHORTCUTS -> {
                            appShortcutsCollapseAllTrigger++
                            appShortcutsSearchQuery = ""
                        }

                        SettingsDetailType.APP_MANAGEMENT -> appManagementSearchQuery = ""
                        SettingsDetailType.CALENDAR_EVENTS -> calendarEventsSearchQuery = ""
                        else -> Unit
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd),
            )
        }

        if (showClearAllConfirmation && detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    callbacks.onClearAllExclusions()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false },
            )
        }
    }
    }
}
