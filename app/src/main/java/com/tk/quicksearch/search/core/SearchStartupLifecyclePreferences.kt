package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.apps.notificationDots.NotificationDotsPermission
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.userAppPreferences.StartupPreferencesFacade
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.apps.prefetchAppIcons
import com.tk.quicksearch.searchEngines.getAppPackageCandidates
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.PackageConstants
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.PinnedShortcutRequests
import com.tk.quicksearch.app.startup.StartupTrace
import android.os.SystemClock

internal fun SearchStartupLifecycleDelegate.onSettingsImported(
        applyStartupPreferences: (StartupPreferencesFacade.StartupPreferences) -> Unit,
        handleOnResume: () -> Unit,
        onAfterSettingsImportMain: () -> Unit = {},
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.reloadNicknameCaches()
            val startupPrefs = userPreferences.getStartupPreferences()

            searchEngineManager.reloadFromPreferences()
            val shortcutsState = aliasHandler.reloadFromPreferences()
            aiSearchHandler.reloadFromPreferences()
            val webSuggestionsEnabled = webSuggestionHandler.reloadFromPreferences()

            val activeLlmApiKey = aiSearchHandler.getLlmApiKey()
            val personalContext = aiSearchHandler.getPersonalContext()
            val activeLlmModel = aiSearchHandler.getSelectedModelId()
            val activeLlmGroundingEnabled = aiSearchHandler.isGroundingEnabled()
            val activeLlmThinkingEnabled = aiSearchHandler.isThinkingEnabled()
            val activeLlmAvailableModels = aiSearchHandler.getAvailableModels()
            val hasApiKey = userPreferences.hasAnyLlmApiKey()
            val customTools = normalizeCustomToolModels(userPreferences.getCustomTools())

            withContext(Dispatchers.Main) {
                applyStartupPreferences(startupPrefs)
                updateFeatureState { state ->
                    state.copy(
                        searchTargetsOrder = searchEngineManager.searchTargetsOrder,
                        disabledSearchTargetIds = searchEngineManager.disabledSearchTargetIds,
                        isSearchEngineCompactMode = searchEngineManager.isSearchEngineCompactMode,
                        searchEngineCompactRowCount = searchEngineManager.searchEngineCompactRowCount,
                        isSearchEngineAliasSuffixEnabled = userPreferences.isSearchEngineAliasSuffixEnabled(),
                        isAliasTriggerAfterSpaceEnabled = userPreferences.isAliasTriggerAfterSpaceEnabled(),
                        shortcutsEnabled = shortcutsState.shortcutsEnabled,
                        shortcutCodes = shortcutsState.shortcutCodes,
                        shortcutEnabled = shortcutsState.shortcutEnabled,
                        webSuggestionsEnabled = webSuggestionsEnabled,
                        calculatorEnabled = userPreferences.isCalculatorEnabled(),
                        unitConverterEnabled = userPreferences.isUnitConverterEnabled(),
                        dateCalculatorEnabled = userPreferences.isDateCalculatorEnabled(),
                        colorVisualizerEnabled = userPreferences.isColorVisualizerEnabled(),
                        currencyConverterEnabled = userPreferences.isCurrencyConverterEnabled(),
                        worldClockEnabled = userPreferences.isWorldClockEnabled(),
                        dictionaryEnabled = userPreferences.isDictionaryEnabled(),
                        weatherEnabled = userPreferences.isWeatherEnabled(),
                        weatherLocationConfigured = userPreferences.getWeatherLocation().isNotBlank(),
                        weatherLocation = userPreferences.getWeatherLocation(),
                        customTools = customTools,
                        disabledCustomToolIds = userPreferences.getDisabledCustomTools(),
                        taskerIntentTools = userPreferences.getTaskerIntentTools(),
                        hasApiKey = hasApiKey,
                        activeLlmApiKeyLast4 = activeLlmApiKey?.takeLast(4),
                        llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                        customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                        customLlmAdvancedPayloadByProvider = userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                        aiSearchLlmProviderId = aiSearchHandler.getAiSearchProviderId(),
                        personalContext = personalContext,
                        activeLlmModel = activeLlmModel,
                        activeLlmGroundingEnabled = activeLlmGroundingEnabled,
                        activeLlmThinkingEnabled = activeLlmThinkingEnabled,
                        activeLlmAvailableModels = activeLlmAvailableModels,
                        availableLlmModelsByProvider = emptyMap(),
                    )
                }
                updateConfigState { state ->
                    state.copy(
                        showSearchEngineOnboarding =
                            searchEngineManager.isSearchEngineCompactMode &&
                                !userPreferences.hasSeenSearchEngineOnboarding(),
                    )
                }
                handleOnResume()
                loadAppSettings()
                updateUiState { applyVisibilityStates(it) }
                onAfterSettingsImportMain()
            }
        }
    }

internal fun SearchStartupLifecycleDelegate.handleOptionalPermissionChangeInternal(allowAppRefresh: Boolean) {
        val previousUsagePermission = permissionStateProvider().hasUsagePermission
        val latestUsagePermission = repository.hasUsageAccess()
        val usagePermissionChanged = previousUsagePermission != latestUsagePermission

        if (usagePermissionChanged) {
            updatePermissionState { it.copy(hasUsagePermission = latestUsagePermission) }
            if (allowAppRefresh && latestUsagePermission) {
                refreshApps()
            } else if (allowAppRefresh) {
                refreshAppSuggestions()
            }
        }

        val optionalChanged = refreshOptionalPermissions()
        val query = resultsStateProvider().query
        if ((optionalChanged || usagePermissionChanged) && query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

internal fun SearchStartupLifecycleDelegate.shouldRetainDirectOrGeminiQueryOnStop(): Boolean {
        val state = resultsStateProvider()
        if (state.query.isBlank()) return false
        return state.AiSearchState.status != AiSearchStatus.Idle ||
            state.currencyConverterState.status != CurrencyConverterStatus.Idle ||
            state.worldClockState.status != WorldClockStatus.Idle ||
            state.dictionaryState.status != DictionaryStatus.Idle
    }

internal fun SearchStartupLifecycleDelegate.refreshAppsUsageAndPermissions() {
        updatePermissionState { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
        refreshOptionalPermissions()
    }

internal fun SearchStartupLifecycleDelegate.initializeWithCacheMinimal(
        cachedAppsList: List<AppInfo>,
    ) {
        val startupSnapshot = readStartupPreferencesSnapshot()
        appSearchManager.initCache(cachedAppsList)
        val lastUpdated = repository.cacheLastUpdatedMillis()
        val suggestionsEnabled = userPreferences.areAppSuggestionsEnabled()
        val startupPrefs = getStartupConfig()?.startupPreferences
        val labelsEnabled = startupPrefs?.showAppLabels ?: userPreferences.shouldShowAppLabels()
        val columnsForPhone =
            startupPrefs?.phoneAppGridColumns ?: userPreferences.getPhoneAppGridColumns()
        val appIconSizeStep =
            startupPrefs?.appIconSizeStep ?: userPreferences.getAppIconSizeStep()

        updateResultsState {
            it.copy(
                cacheLastUpdatedMillis = lastUpdated,
                // The cached order is kept off-screen until usage metadata and any required app
                // catalog reconciliation are complete.
                recentApps = emptyList(),
                indexedAppCount = cachedAppsList.size,
            )
        }
        updateConfigState {
            it.copy(
                oneHandedMode = startupSnapshot.oneHandedMode,
                bottomSearchBarEnabled = startupSnapshot.bottomSearchBarEnabled,
                openKeyboardOnLaunch = startupSnapshot.openKeyboardOnLaunch,
                appSuggestionsEnabled = suggestionsEnabled,
                showAllAppsButton = userPreferences.shouldShowAllAppsButton(),
                includeNonLaunchableAppsInSearch =
                    userPreferences.shouldIncludeNonLaunchableAppsInSearch(),
                includeArchivedAppsInSearch = userPreferences.shouldIncludeArchivedAppsInSearch(),
                selectedAppSuggestionTab = userPreferences.getSelectedAppSuggestionTab(),
                enabledAppSuggestionTabs = userPreferences.getEnabledAppSuggestionTabs(),
                showAppLabels = labelsEnabled,
                phoneAppGridColumns = columnsForPhone,
                appIconSizeStep = appIconSizeStep,
                isStartupCoreSurfaceReady = true,
            )
        }
        saveStartupSurfaceSnapshotAsync(false, false)
    }

internal suspend fun SearchStartupLifecycleDelegate.publishCurrentStartupAppSuggestions() {
        val startedAtElapsedMs = SystemClock.elapsedRealtime()
        refreshAppSuggestions()
        publishStartupAppSuggestions()
        AppSearchPerformanceLogger.logTiming(
            event = "startupSuggestionsPublished",
            elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs,
            slowThresholdMs = 100L,
        ) {
            "recents=${resultsStateProvider().recentApps.size} pinned=${resultsStateProvider().pinnedApps.size}"
        }
    }

internal suspend fun SearchStartupLifecycleDelegate.publishStartupAppSuggestions() {
        withContext(Dispatchers.Main.immediate) {
            updateResultsState { state ->
                if (state.query.isNotBlank() || state.recentApps.isEmpty()) {
                    state
                } else {
                    state.copy(
                        screenState = ScreenVisibilityState.Content,
                        appsSectionState =
                            AppsSectionVisibility.ShowingResults(
                                hasPinned = state.pinnedApps.isNotEmpty(),
                            ),
                    )
                }
            }
            StartupTrace.mark("QS.Home.AppSuggestionsPublished")
        }
    }

internal fun SearchStartupLifecycleDelegate.getMessagingAppInfo(packageNames: Set<String>): MessagingAppInfo {
        val isWhatsAppInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.WHATSAPP_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.WHATSAPP_PACKAGE)
            }
        val isWhatsAppBusinessInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.WHATSAPP_BUSINESS_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.WHATSAPP_BUSINESS_PACKAGE)
            }
        val isTelegramInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.TELEGRAM_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.TELEGRAM_PACKAGE)
            }
        val isSignalInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.SIGNAL_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.SIGNAL_PACKAGE)
            }
        val isGoogleMeetInstalled =
            if (packageNames.isNotEmpty()) {
                packageNames.contains(PackageConstants.GOOGLE_MEET_PACKAGE)
            } else {
                messagingHandler.isPackageInstalled(PackageConstants.GOOGLE_MEET_PACKAGE)
            }
        val resolvedMessagingApp =
            messagingHandler.updateMessagingAvailability(
                whatsappInstalled = isWhatsAppInstalled,
                whatsappBusinessInstalled = isWhatsAppBusinessInstalled,
                telegramInstalled = isTelegramInstalled,
                signalInstalled = isSignalInstalled,
                updateState = false,
            )
        val selectedCallingApp = userPreferences.getCallingApp()
        val resolvedCallingApp =
            resolveCallingApp(
                app = selectedCallingApp,
                isWhatsAppInstalled = isWhatsAppInstalled,
                isWhatsAppBusinessInstalled = isWhatsAppBusinessInstalled,
                isTelegramInstalled = isTelegramInstalled,
                isSignalInstalled = isSignalInstalled,
                isGoogleMeetInstalled = isGoogleMeetInstalled,
            )
        if (resolvedCallingApp != selectedCallingApp) {
            userPreferences.setCallingApp(resolvedCallingApp)
        }

        return MessagingAppInfo(
            isWhatsAppInstalled,
            isWhatsAppBusinessInstalled,
            isTelegramInstalled,
            isSignalInstalled,
            resolvedMessagingApp,
            isGoogleMeetInstalled,
            resolvedCallingApp,
        )
    }

internal fun SearchStartupLifecycleDelegate.resolveCallingApp(
        app: CallingApp,
        isWhatsAppInstalled: Boolean,
        isWhatsAppBusinessInstalled: Boolean,
        isTelegramInstalled: Boolean,
        isSignalInstalled: Boolean,
        isGoogleMeetInstalled: Boolean,
    ): CallingApp =
        when (app) {
            CallingApp.WHATSAPP -> if (isWhatsAppInstalled) CallingApp.WHATSAPP else CallingApp.CALL
            CallingApp.WHATSAPP_BUSINESS -> if (isWhatsAppBusinessInstalled) CallingApp.WHATSAPP_BUSINESS else CallingApp.CALL
            CallingApp.TELEGRAM -> if (isTelegramInstalled) CallingApp.TELEGRAM else CallingApp.CALL
            CallingApp.SIGNAL -> if (isSignalInstalled) CallingApp.SIGNAL else CallingApp.CALL
            CallingApp.GOOGLE_MEET ->
                if (isGoogleMeetInstalled) CallingApp.GOOGLE_MEET else CallingApp.CALL
            CallingApp.CALL -> CallingApp.CALL
        }

internal fun SearchStartupLifecycleDelegate.applyAppShortcutIconOverrides(
        shortcuts: List<StaticShortcut>,
        overrides: Map<String, String>,
    ): List<StaticShortcut> {
        if (overrides.isEmpty()) return shortcuts
        return shortcuts.map { shortcut ->
            val key = shortcutKey(shortcut)
            val overrideIcon = overrides[key] ?: return@map shortcut
            if (isUserCreatedShortcut(shortcut)) shortcut else shortcut.copy(iconBase64 = overrideIcon)
        }
    }

internal fun SearchStartupLifecycleDelegate.normalizeCustomToolModels(tools: List<CustomTool>): List<CustomTool> {
        val normalizedTools =
            tools.map { tool ->
                if (tool.modelId.isNotBlank()) {
                    tool
                } else {
                    tool.copy(
                        modelId =
                            AiSearchLlmProviderRegistry
                                .get(tool.providerId, applicationProvider())
                                .defaultModelId,
                    )
                }
            }

        if (normalizedTools != tools) {
            userPreferences.setCustomTools(normalizedTools)
        }
        return normalizedTools
    }

internal data class MessagingAppInfo(
        val isWhatsAppInstalled: Boolean,
        val isWhatsAppBusinessInstalled: Boolean,
        val isTelegramInstalled: Boolean,
        val isSignalInstalled: Boolean,
        val messagingApp: MessagingApp,
        val isGoogleMeetInstalled: Boolean,
        val callingApp: CallingApp,
    )

internal const val BROWSER_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L
internal const val DEFERRED_AI_SEARCH_MODELS_DELAY_MS = 15_000L
internal const val OPTIONAL_STARTUP_DELAY_MS = 10_000L
internal const val OPTIONAL_STARTUP_QUERY_RECHECK_MS = 1_000L
internal const val APP_RECONCILIATION_FRESHNESS_MS = 24L * 60L * 60L * 1_000L
internal const val PERMISSION_SNAPSHOT_DEDUP_WINDOW_MS = 1_500L
internal const val MAX_STARTUP_SEARCH_TARGETS_TO_PREFETCH = 14
internal const val MAX_STARTUP_SEARCH_TARGET_ICON_PACKAGES = 30
