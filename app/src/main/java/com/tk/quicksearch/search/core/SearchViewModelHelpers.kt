package com.tk.quicksearch.search.core
import android.app.Application
import android.os.Trace
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.R
import com.tk.quicksearch.app.ReleaseNotesHandler
import com.tk.quicksearch.app.navigation.NavigationHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutManagementHandler
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsRepository
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppManagementService
import com.tk.quicksearch.search.apps.AppSearchManager
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.calendar.CalendarManagementHandler
import com.tk.quicksearch.search.common.PinningHandler
import com.tk.quicksearch.search.contacts.actions.ContactActionHandler
import com.tk.quicksearch.search.contacts.ContactManagementHandler
import com.tk.quicksearch.search.contacts.MessagingHandler
import com.tk.quicksearch.search.data.appShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.appShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.userAppPreferences.StartupPreferencesFacade
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsManagementHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileManagementHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.ScreenTimeRepository
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.startup.StartupHomeSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceSnapshot
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import com.tk.quicksearch.search.webSuggestions.WebSuggestionHandler
import com.tk.quicksearch.searchEngines.SearchEngineManager
import com.tk.quicksearch.searchEngines.SecondarySearchOrchestrator
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.shared.util.isLowRamDevice
import com.tk.quicksearch.tools.aiTools.CurrencyConverterHandler
import com.tk.quicksearch.tools.aiTools.DictionaryHandler
import com.tk.quicksearch.tools.aiTools.WorldClockHandler
import com.tk.quicksearch.tools.calculator.CalculatorHandler
import com.tk.quicksearch.tools.dateCalculator.DateCalculatorHandler
import com.tk.quicksearch.tools.aiSearch.AiSearchHandler
import com.tk.quicksearch.tools.unitConverter.UnitConverterHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.JvmName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
internal fun SearchViewModel.refreshScreenTimeResult(query: String) {
        val pinnedItemOrder = _resultsState.value.pinnedNonAppItemOrder
        val matchesQuery = OtherSearchItemRegistry.matchesScreenTime(query)
        if (!matchesQuery) {
            hasScreenTimeResultForCurrentSearch = false
        }
        if (
            !OtherSearchItemRegistry.shouldLoad(
                itemId = OtherSearchItemId.SCREEN_TIME,
                query = query,
                pinnedItemOrder = pinnedItemOrder,
            )
        ) {
            screenTimeSearchJob?.cancel()
            updateResultsState { it.copy(screenTimeState = ScreenTimeState.Hidden) }
            return
        }
        if (!com.tk.quicksearch.search.utils.PermissionUtils.hasUsageStatsPermission(appContext)) {
            screenTimeSearchJob?.cancel()
            updateResultsState { it.copy(screenTimeState = ScreenTimeState.Hidden) }
            return
        }
        val currentState = _resultsState.value.screenTimeState
        if (currentState == ScreenTimeState.Loading) return
        if (
            matchesQuery &&
                hasScreenTimeResultForCurrentSearch &&
                currentState is ScreenTimeState.Available
        ) {
            return
        }
        screenTimeSearchJob?.cancel()
        updateResultsState { it.copy(screenTimeState = ScreenTimeState.Loading) }
        screenTimeSearchJob =
            viewModelScope.launch(Dispatchers.IO) {
                val screenTime = screenTimeRepository.getTodayScreenTime()
                if (
                    com.tk.quicksearch.search.utils.PermissionUtils.hasUsageStatsPermission(appContext) &&
                        OtherSearchItemRegistry.shouldLoad(
                            itemId = OtherSearchItemId.SCREEN_TIME,
                            query = _resultsState.value.query,
                            pinnedItemOrder = _resultsState.value.pinnedNonAppItemOrder,
                        )
                ) {
                    updateResultsState {
                        it.copy(
                            screenTimeState =
                                ScreenTimeState.Available(
                                    durationMillis = screenTime.durationMillis,
                                    topApps = screenTime.topApps,
                                ),
                            )
                    }
                    hasScreenTimeResultForCurrentSearch =
                        OtherSearchItemRegistry.matchesScreenTime(_resultsState.value.query)
                }
            }
    }


internal fun SearchViewModel.computeEffectiveIsDarkMode(): Boolean {
        return when (_configState.value.appThemeMode) {
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
            AppThemeMode.SYSTEM -> {
                val nightModeFlags =
                        appContext.resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
internal fun SearchViewModel.applyLauncherIconSelection(selection: LauncherAppIcon = launcherAppIcon) {
        launcherIconManager.applySelection(
                selection = selection,
        )
    }
    // Contact Actions
internal fun SearchViewModel.isAiSearchActive() =
        _resultsState.value.AiSearchState.status != AiSearchStatus.Idle

internal fun SearchViewModel.applyVisibilityStates(state: SearchUiState): SearchUiState =
            visibilityStateResolver.apply(state)

internal fun SearchViewModel.getGridItemCount(): Int =
            derivedStateDelegate.getGridItemCount()
internal fun SearchViewModel.getSearchableAppsSnapshot(): List<AppInfo> = derivedStateDelegate.getSearchableAppsSnapshot()
internal fun SearchViewModel.warmSearchableAppsSnapshot(apps: List<AppInfo>) {
        derivedStateDelegate.warmSearchableAppsSnapshot(apps)
    }
    /**
     * Recomputes only the app-suggestions / app-search part of derived state: nickname cache,
     * pinned apps, recents, search results, hidden-app lists, and icon prefetch. Does NOT touch
     * messaging/calling state and does NOT re-trigger secondary searches.
     *
     * Call this when the apps list or app preferences change but contacts/files/settings are
     * unaffected (e.g. pin/hide an app, toggle suggestions, resume without usage permission).
     */
internal fun SearchViewModel.refreshAppSuggestions(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        derivedStateDelegate.refreshAppSuggestions(lastUpdated = lastUpdated, isLoading = isLoading)
    }
    /**
     * Re-triggers secondary searches (contacts, files, settings) for the current query. Used by
     * management handlers (contact/file/settings pin/exclude operations) so they don't have to
     * touch app-suggestion state at all.
     */
internal fun SearchViewModel.refreshSecondarySearches() = derivedStateDelegate.refreshSecondarySearches()
    /**
     * Full derived-state refresh: recomputes app suggestions, messaging state, and re-triggers
     * secondary searches. Use only when the installed app list changes (e.g. app
     * installed/uninstalled) or during startup, where all three concerns need updating together.
     */
internal fun SearchViewModel.refreshDerivedState(
            lastUpdated: Long? = null,
            isLoading: Boolean? = null,
    ) {
        derivedStateDelegate.refreshDerivedState(lastUpdated = lastUpdated, isLoading = isLoading)
    }

internal fun SearchViewModel.refreshPostStartupState() {
        derivedStateDelegate.refreshMessagingState()
        derivedStateDelegate.refreshSecondarySearches()
    }
internal fun SearchViewModel.saveStartupSurfaceSnapshotAsync(
            forcePreviewRefresh: Boolean = false,
            allowDuringQuery: Boolean = false,
    ) {
        derivedStateDelegate.saveStartupSurfaceSnapshotAsync(
            forcePreviewRefresh = forcePreviewRefresh,
            allowDuringQuery = allowDuringQuery,
        )
    }

internal fun SearchViewModel.extractSuggestedApps(
            apps: List<AppInfo>,
            limit: Int,
            hasUsagePermission: Boolean,
    ): List<AppInfo> =
            appSuggestionSelector.selectSuggestedApps(
                    apps = apps,
                    limit = limit,
                    hasUsagePermission = hasUsagePermission,
            )
