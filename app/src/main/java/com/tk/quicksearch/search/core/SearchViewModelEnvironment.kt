package com.tk.quicksearch.search.core

import android.app.Application
import android.content.Context
import com.tk.quicksearch.search.appSettings.AppSettingsRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsRepository
import com.tk.quicksearch.search.startup.StartupSurfaceStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

internal data class SearchViewModelEnvironment(
    val application: Application,
    val appContext: Context,
    val scope: CoroutineScope,
    val startupSurfaceStore: StartupSurfaceStore,
    val userPreferences: UserAppPreferences,
    val repository: AppsRepository,
    val appShortcutRepository: AppShortcutRepository,
    val calendarRepository: CalendarRepository,
    val contactRepository: ContactRepository,
    val fileRepository: FileSearchRepository,
    val settingsShortcutRepository: DeviceSettingsRepository,
    val appSettingsRepository: AppSettingsRepository,
    val launcherIconManager: LauncherIconManager,
    val contactPreferences: com.tk.quicksearch.search.data.preferences.ContactPreferences,
    val permissionManager: PermissionManager,
    val searchOperations: SearchOperations,
    val startupDispatcher: CoroutineDispatcher,
    val getHandlers: () -> SearchHandlerContainer,
    val getUiState: () -> SearchUiState,
    val getResultsState: () -> SearchResultsState,
    val getPermissionState: () -> SearchPermissionState,
    val getFeatureState: () -> SearchFeatureState,
    val getConfigState: () -> SearchUiConfigState,
    val updateUiState: (((SearchUiState) -> SearchUiState) -> Unit),
    val updateResultsState: (((SearchResultsState) -> SearchResultsState) -> Unit),
    val updatePermissionState: (((SearchPermissionState) -> SearchPermissionState) -> Unit),
    val updateFeatureState: (((SearchFeatureState) -> SearchFeatureState) -> Unit),
    val updateConfigState: (((SearchUiConfigState) -> SearchUiConfigState) -> Unit),
    val showToastRes: (Int) -> Unit,
    val showToastText: (String) -> Unit,
    val onNavigationTriggered: () -> Unit,
)
