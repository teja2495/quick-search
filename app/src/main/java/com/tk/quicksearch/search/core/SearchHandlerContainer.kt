package com.tk.quicksearch.search.core

import android.app.Application
import android.content.Context
import com.tk.quicksearch.app.ReleaseNotesHandler
import com.tk.quicksearch.app.navigation.NavigationHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingsRepository
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppManagementService
import com.tk.quicksearch.search.apps.AppSearchManager
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.calendar.CalendarManagementHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.utils.ContactManagementHandler
import com.tk.quicksearch.search.contacts.utils.MessagingHandler
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.SearchEngineManager
import com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.WordClockHandler
import com.tk.quicksearch.tools.calculator.CalculatorHandler
import com.tk.quicksearch.tools.dateCalculator.DateCalculatorHandler
import com.tk.quicksearch.tools.directSearch.DirectSearchHandler
import com.tk.quicksearch.tools.unitConverter.UnitConverterHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

internal class SearchHandlerContainer(
    private val application: Application,
    private val appContext: Context,
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val repository: AppsRepository,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val calendarRepository: CalendarRepository,
    private val appShortcutRepository: AppShortcutRepository,
    private val settingsShortcutRepository: DeviceSettingsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val permissionManager: PermissionManager,
    private val searchOperations: SearchOperations,
    private val startupDispatcher: CoroutineDispatcher,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateConfigState: ((SearchUiConfigState) -> SearchUiConfigState) -> Unit,
    private val refreshSecondarySearches: () -> Unit,
    private val refreshAppShortcutsState: () -> Unit,
    private val refreshAppSuggestions: () -> Unit,
    private val refreshDerivedState: () -> Unit,
    private val showToast: (Int) -> Unit,
    private val currentStateProvider: () -> SearchUiState,
    private val isLowRamDevice: Boolean,
) {
    val appManager by lazy {
        AppManagementService(userPreferences, scope, refreshAppSuggestions)
    }

    val contactManager by lazy {
        ContactManagementHandler(
            userPreferences,
            scope,
            refreshSecondarySearches,
            updateUiState,
        )
    }

    val fileManager by lazy {
        FileManagementHandler(
            userPreferences,
            scope,
            refreshSecondarySearches,
            updateUiState,
        )
    }

    val settingsManager by lazy {
        DeviceSettingsManagementHandler(
            userPreferences,
            scope,
            refreshSecondarySearches,
            updateUiState,
        )
    }

    val calendarManager by lazy {
        CalendarManagementHandler(
            userPreferences,
            scope,
            refreshSecondarySearches,
            updateUiState,
        )
    }

    val appShortcutManager by lazy {
        AppShortcutManagementHandler(
            userPreferences,
            scope,
            refreshAppShortcutsState,
            updateUiState,
        )
    }

    val searchEngineManager by lazy {
        SearchEngineManager(
            appContext,
            userPreferences,
            scope,
            updateUiState,
        )
    }

    val sectionManager by lazy {
        SectionManager(userPreferences, permissionManager, scope, updateUiState)
    }

    val iconPackHandler by lazy {
        IconPackService(application, userPreferences, scope, updateUiState)
    }

    val messagingHandler by lazy {
        MessagingHandler(application, userPreferences, updateUiState)
    }

    val releaseNotesHandler by lazy {
        ReleaseNotesHandler(application, userPreferences, updateUiState)
    }

    val pinningHandler by lazy {
        PinningHandler(
            scope = scope,
            permissionManager = permissionManager,
            contactRepository = contactRepository,
            fileRepository = fileRepository,
            userPreferences = userPreferences,
            uiStateUpdater = updateUiState,
        )
    }

    val webSuggestionHandler by lazy {
        WebSuggestionHandler(
            scope = scope,
            userPreferences = userPreferences,
            uiStateUpdater = updateUiState,
        )
    }

    val calculatorHandler by lazy {
        CalculatorHandler(userPreferences = userPreferences)
    }

    val unitConverterHandler by lazy {
        UnitConverterHandler(userPreferences = userPreferences)
    }

    val dateCalculatorHandler by lazy {
        DateCalculatorHandler(userPreferences = userPreferences)
    }

    val currencyConverterHandler by lazy {
        CurrencyConverterHandler(appContext, userPreferences)
    }

    val wordClockHandler by lazy { WordClockHandler(appContext, userPreferences) }

    val dictionaryHandler by lazy { DictionaryHandler(appContext, userPreferences) }

    val appSearchManager by lazy {
        AppSearchManager(
            context = appContext,
            repository = repository,
            userPreferences = userPreferences,
            scope = scope,
            onAppsUpdated = {
                refreshDerivedState()
            },
            onLoadingStateChanged = { isLoading, error ->
                updateConfigState { it.copy(isLoading = isLoading, errorMessage = error) }
            },
            showToastCallback = showToast,
        )
    }

    val settingsSearchHandler by lazy {
        DeviceSettingsSearchHandler(
            context = appContext,
            repository = settingsShortcutRepository,
            userPreferences = userPreferences,
            showToastCallback = showToast,
        )
    }

    val appShortcutSearchHandler by lazy {
        AppShortcutSearchHandler(
            repository = appShortcutRepository,
            userPreferences = userPreferences,
        )
    }

    val appSettingsSearchHandler by lazy {
        AppSettingsSearchHandler(
            repository = appSettingsRepository,
            userPreferences = userPreferences,
        )
    }

    val fileSearchHandler by lazy {
        FileSearchHandler(fileRepository = fileRepository, userPreferences = userPreferences)
    }

    val directSearchHandler by lazy {
        DirectSearchHandler(
            context = appContext,
            userPreferences = userPreferences,
            scope = scope,
            showToastCallback = showToast,
        )
    }

    val aliasHandler by lazy {
        AliasHandler(
            userPreferences = userPreferences,
            scope = scope,
            uiStateUpdater = updateUiState,
            directSearchHandler = directSearchHandler,
            searchTargetsProvider = { searchEngineManager.searchTargetsOrder },
        )
    }

    val unifiedSearchHandler by lazy {
        UnifiedSearchHandler(
            context = appContext,
            contactRepository = contactRepository,
            calendarRepository = calendarRepository,
            fileRepository = fileRepository,
            userPreferences = userPreferences,
            settingsSearchHandler = settingsSearchHandler,
            appSettingsSearchHandler = appSettingsSearchHandler,
            appShortcutSearchHandler = appShortcutSearchHandler,
            fileSearchHandler = fileSearchHandler,
            searchOperations = searchOperations,
        )
    }

    val secondarySearchOrchestrator by lazy {
        SecondarySearchOrchestrator(
            scope = scope,
            unifiedSearchHandler = unifiedSearchHandler,
            webSuggestionHandler = webSuggestionHandler,
            sectionManager = sectionManager,
            uiStateUpdater = updateUiState,
            currentStateProvider = currentStateProvider,
            isLowRamDevice = isLowRamDevice,
        )
    }

    lateinit var navigationHandler: NavigationHandler
        private set

    lateinit var contactActionHandler: ContactActionHandler
        private set

    fun initializeServices(
        getCallingApp: () -> CallingApp,
        getMessagingApp: () -> MessagingApp,
        getDirectDialEnabled: () -> Boolean,
        getHasSeenDirectDialChoice: () -> Boolean,
        getCurrentState: () -> SearchUiState,
        clearQuery: () -> Unit,
        externalNavigation: () -> Unit,
        onRequestDirectSearch: (query: String, addToSearchHistory: Boolean) -> Unit,
        showToastText: (Int) -> Unit,
    ) {
        navigationHandler =
            NavigationHandler(
                application = application,
                userPreferences = userPreferences,
                settingsSearchHandler = settingsSearchHandler,
                onRequestDirectSearch = onRequestDirectSearch,
                onClearQuery = clearQuery,
                onExternalNavigation = externalNavigation,
                showToastCallback = showToastText,
            )

        contactActionHandler =
            ContactActionHandler(
                context = application,
                userPreferences = userPreferences,
                getCallingApp = getCallingApp,
                getMessagingApp = getMessagingApp,
                getDirectDialEnabled = getDirectDialEnabled,
                getHasSeenDirectDialChoice = getHasSeenDirectDialChoice,
                getCurrentState = getCurrentState,
                uiStateUpdater = updateUiState,
                clearQuery = clearQuery,
                showToastCallback = showToastText,
            )
    }
}
