package com.tk.quicksearch.search.core

import android.content.Intent
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.appShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.appShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.apps.invalidateAppIconCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.app.startup.StartupTrace

internal class SearchStaticDataDelegate(
    private val scope: CoroutineScope,
    private val userPreferences: UserAppPreferences,
    private val repository: AppsRepository,
    private val appShortcutRepository: AppShortcutRepository,
    private val contactRepository: ContactRepository,
    private val fileRepository: FileSearchRepository,
    private val calendarRepository: CalendarRepository,
    private val handlersProvider: () -> SearchHandlerContainer,
    private val resultsStateProvider: () -> SearchResultsState,
    private val hasCalendarPermission: () -> Boolean,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val showToastRes: (Int) -> Unit,
    private val refreshRecentItems: () -> Unit,
) {
    private val pinningHandler get() = handlersProvider().pinningHandler
    private var todayEventsReloadJob: Job? = null
    private val appSearchManager get() = handlersProvider().appSearchManager
    private val settingsSearchHandler get() = handlersProvider().settingsSearchHandler
    private val appShortcutSearchHandler get() = handlersProvider().appShortcutSearchHandler
    private val secondarySearchOrchestrator get() = handlersProvider().secondarySearchOrchestrator
    private val sectionManager get() = handlersProvider().sectionManager
    private val contactManager get() = handlersProvider().contactManager
    private val fileManager get() = handlersProvider().fileManager
    private val appManager get() = handlersProvider().appManager
    private val settingsManager get() = handlersProvider().settingsManager
    private val calendarManager get() = handlersProvider().calendarManager
    private val appShortcutManager get() = handlersProvider().appShortcutManager

    fun loadApps() {
        appSearchManager.loadApps()
    }

    suspend fun loadAppsForStartup() {
        appSearchManager.refreshAppsNow()
    }

    fun loadSettingsShortcuts() {
        scope.launch(Dispatchers.IO) {
            settingsSearchHandler.loadShortcuts()
            withContext(Dispatchers.Main) { refreshSettingsState(updateResults = false) }
        }
    }

    private val appShortcutsLoadLock = Any()
    private var appShortcutsLoadJob: Job? = null

    private fun appShortcutsLoad(): Job =
        synchronized(appShortcutsLoadLock) {
            appShortcutsLoadJob?.takeUnless { it.isCompleted } ?: scope.launch(
                Dispatchers.IO,
                start = CoroutineStart.LAZY,
            ) {
                val loadedCached = appShortcutSearchHandler.loadCachedShortcutsOnly()
                if (loadedCached) {
                    refreshAppShortcutsStateFromBackground()
                    StartupTrace.mark("QS.Home.ShortcutsCacheAvailable")
                }

                val loadedFresh = appShortcutSearchHandler.refreshShortcutsFromSystem()
                if (loadedFresh || !loadedCached) {
                    refreshAppShortcutsStateFromBackground()
                }
                StartupTrace.mark("QS.Startup.ShortcutsRefreshComplete")
            }.also { appShortcutsLoadJob = it }
        }.also { it.start() }

    fun loadAppShortcuts() {
        appShortcutsLoad()
    }

    suspend fun loadAppShortcutsForStartup() {
        appShortcutsLoad().join()
    }

    /** Refreshes shortcuts from the system and suspends until the fresh result is in UI state. */
    suspend fun refreshAppShortcutsAndAwait() {
        appShortcutsLoad().join()
    }

    fun refreshSettingsState(updateResults: Boolean = true) {
        val currentResults = resultsStateProvider().settingResults
        val currentState =
            settingsSearchHandler.getSettingsState(
                query = resultsStateProvider().query,
                isSettingsSectionEnabled = SearchSection.SETTINGS !in sectionManager.disabledSections,
            )

        updateResultsState { state ->
            state.copy(
                pinnedSettings = currentState.pinned,
                excludedSettings = currentState.excluded,
                settingResults = if (updateResults) currentState.results else currentResults,
                allDeviceSettings = settingsSearchHandler.getAvailableSettings(),
            )
        }
    }

    private class AppShortcutsStateSnapshot(
        val query: String,
        val updateResults: Boolean,
        val disabledIds: Set<String>,
        val all: List<StaticShortcut>,
        val pinned: List<StaticShortcut>,
        val excluded: List<StaticShortcut>,
        val results: List<StaticShortcut>,
    )

    /** Safe off the main thread; the handler already applies icon overrides to every list. */
    private fun computeAppShortcutsState(updateResults: Boolean): AppShortcutsStateSnapshot {
        val query = if (updateResults) resultsStateProvider().query else ""
        val currentState =
            appShortcutSearchHandler.getShortcutsState(
                query = query,
                isSectionEnabled = SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections,
            )
        return AppShortcutsStateSnapshot(
            query = query,
            updateResults = updateResults,
            disabledIds = userPreferences.getDisabledAppShortcutIds(),
            all = appShortcutSearchHandler.getAvailableShortcuts(),
            pinned = currentState.pinned,
            excluded = currentState.excluded,
            results = currentState.results,
        )
    }

    private fun publishAppShortcutsState(snapshot: AppShortcutsStateSnapshot) {
        // A snapshot computed in the background may be for a query the user has since changed;
        // keep the newer results in that case.
        val updateResults =
            snapshot.updateResults && snapshot.query == resultsStateProvider().query
        // A query typed before the shortcuts loaded missed them; let later keystrokes search them.
        secondarySearchOrchestrator.forgetNoResults(SearchSection.APP_SHORTCUTS)
        updateUiState { state ->
            state.copy(
                allAppShortcuts = snapshot.all,
                disabledAppShortcutIds = snapshot.disabledIds,
                pinnedAppShortcuts = snapshot.pinned,
                excludedAppShortcuts = snapshot.excluded,
                appShortcutResults = if (updateResults) snapshot.results else state.appShortcutResults,
            )
        }
    }

    fun refreshAppShortcutsState(updateResults: Boolean = true) {
        publishAppShortcutsState(computeAppShortcutsState(updateResults))
    }

    /** Builds the shortcut state on the calling (background) thread and publishes it on Main. */
    private suspend fun refreshAppShortcutsStateFromBackground() {
        val snapshot = computeAppShortcutsState(updateResults = true)
        withContext(Dispatchers.Main) { publishAppShortcutsState(snapshot) }
    }

    fun refreshUsageAccess() {
        updatePermissionState { it.copy(hasUsagePermission = repository.hasUsageAccess()) }
    }

    fun refreshApps(
        showToast: Boolean = false,
        forceUiUpdate: Boolean = false,
    ) {
        val shouldInvalidateIcons = showToast || forceUiUpdate
        if (shouldInvalidateIcons) {
            invalidateAppIconCache()
        }
        appSearchManager.refreshApps(showToast, forceUiUpdate = forceUiUpdate || showToast)
    }

    fun refreshContacts(showToast: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            contactRepository.refreshContactsProviderSnapshot()
            pinningHandler.loadPinnedAndExcludedContacts()

            val query = resultsStateProvider().query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                updateResultsState { it.copy(contactResults = emptyList()) }
            }

            if (showToast) {
                withContext(Dispatchers.Main) {
                    showToastRes(R.string.contacts_refreshed_successfully)
                }
            }
        }
    }

    fun refreshFiles(showToast: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            fileRepository.refreshFilesProviderSnapshot()
            pinningHandler.loadPinnedAndExcludedFiles()

            val query = resultsStateProvider().query
            if (query.isNotBlank()) {
                secondarySearchOrchestrator.resetNoResultTracking()
                secondarySearchOrchestrator.performSecondarySearches(query)
            } else {
                updateResultsState { it.copy(fileResults = emptyList()) }
            }

            if (showToast) {
                withContext(Dispatchers.Main) { showToastRes(R.string.files_refreshed_successfully) }
            }
        }
    }

    fun loadPinnedAndExcludedCalendarEvents() {
        if (!hasCalendarPermission()) {
            updateResultsState {
                it.copy(
                    pinnedCalendarEvents = emptyList(),
                    excludedCalendarEvents = emptyList(),
                    todayCalendarEvents = emptyList(),
                )
            }
            return
        }

        val excludedIds = userPreferences.getExcludedCalendarEventIds()
        val pinnedIds =
            userPreferences.getPinnedCalendarEventIds().filterNot { excludedIds.contains(it) }.toSet()
        val pinned =
            calendarRepository.getEventsByIds(pinnedIds)
                .sortedByPinnedOrder(userPreferences.getPinnedCalendarEventOrder()) { it.eventId }
        val excluded = calendarRepository.getEventsByIds(excludedIds)
        val todayEvents = if (userPreferences.getShowTodayEvents()) userPreferences.getTodayGlanceEvents(calendarRepository) else null
        val today = todayEvents?.visible.orEmpty()
        scheduleTodayEventsReload(todayEvents?.nextVisibleAtMillis)

        updateResultsState {
            it.copy(
                pinnedCalendarEvents = pinned,
                excludedCalendarEvents = excluded,
                todayCalendarEvents = today,
            )
        }
        StartupTrace.mark("QS.Home.CalendarAvailable")
    }

    /** Reloads when an all-day event reaches its morning start. */
    private fun scheduleTodayEventsReload(atMillis: Long?) {
        todayEventsReloadJob?.cancel()
        if (atMillis == null) return
        todayEventsReloadJob =
            scope.launch(Dispatchers.IO) {
                delay((atMillis - System.currentTimeMillis()).coerceAtLeast(0L) + 1_000L)
                loadPinnedAndExcludedCalendarEvents()
            }
    }

    fun setAppShortcutEnabled(
        shortcut: StaticShortcut,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAppShortcutEnabled(shortcutKey(shortcut), enabled)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun setAppShortcutsEnabled(
        shortcutIds: Collection<String>,
        enabled: Boolean,
    ) {
        if (shortcutIds.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            userPreferences.setAppShortcutsEnabled(shortcutIds, enabled)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun setAllAppShortcutsEnabled(
        packageName: String,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAllAppShortcutsEnabled(packageName, enabled)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun setAppShortcutIconOverride(
        shortcut: StaticShortcut,
        iconBase64: String?,
    ) {
        if (isUserCreatedShortcut(shortcut)) return
        scope.launch(Dispatchers.IO) {
            userPreferences.setAppShortcutIconOverride(shortcutKey(shortcut), iconBase64)
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
            }
        }
    }

    fun getAppShortcutIconOverride(shortcutId: String): String? =
        userPreferences.getAppShortcutIconOverride(shortcutId)

    fun addCustomAppShortcutFromPickerResult(
        resultData: Intent?,
        sourcePackageName: String? = null,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            val addedShortcut =
                appShortcutRepository.addCustomShortcutFromPickerResult(
                    resultData = resultData,
                    sourcePackageName = sourcePackageName,
                )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addSearchTargetQueryShortcut(
        target: SearchTarget,
        shortcutName: String,
        shortcutQuery: String,
        mode: SearchTargetShortcutMode = SearchTargetShortcutMode.AUTO,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            val addedShortcut =
                appShortcutRepository.addSearchTargetQueryShortcut(
                    target = target,
                    shortcutName = shortcutName,
                    shortcutQuery = shortcutQuery,
                    mode = mode,
                )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addCustomAppActivityShortcut(
        packageName: String,
        activityClassName: String,
        activityLabel: String,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            val addedShortcut =
                appShortcutRepository.addCustomShortcutForAppActivity(
                    packageName = packageName,
                    activityClassName = activityClassName,
                    activityLabel = activityLabel,
                )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun addCustomAppDeepLinkShortcut(
        packageName: String,
        shortcutName: String,
        deepLink: String,
        iconBase64: String?,
        showDefaultToast: Boolean = true,
        onShortcutAdded: ((StaticShortcut) -> Unit)? = null,
        onAddFailed: (() -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            val addedShortcut =
                appShortcutRepository.addCustomShortcutForAppDeepLink(
                    packageName = packageName,
                    shortcutName = shortcutName,
                    deepLink = deepLink,
                    iconBase64 = iconBase64,
                )
            if (addedShortcut != null) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
                withContext(Dispatchers.Main) {
                    refreshAppShortcutsState()
                    onShortcutAdded?.invoke(addedShortcut)
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_success)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (showDefaultToast) {
                        showToastRes(R.string.settings_app_shortcuts_add_failed)
                    }
                    onAddFailed?.invoke()
                }
            }
        }
    }

    fun deleteCustomAppShortcut(shortcut: StaticShortcut) {
        if (!isUserCreatedShortcut(shortcut)) return
        scope.launch(Dispatchers.IO) {
            val removed = appShortcutRepository.removeCustomShortcut(shortcut)
            if (!removed) return@launch
            val id = shortcutKey(shortcut)
            userPreferences.unpinAppShortcut(id)
            userPreferences.removeExcludedAppShortcut(id)
            userPreferences.setAppShortcutEnabled(id, true)
            userPreferences.setAppShortcutNickname(id, null)
            userPreferences.setAppShortcutIconOverride(id, null)
            appShortcutSearchHandler.loadCachedShortcutsOnly()
            withContext(Dispatchers.Main) {
                refreshAppShortcutsState()
                refreshRecentItems()
                showToastRes(R.string.settings_app_shortcuts_delete_success)
            }
        }
    }

    fun updateCustomAppShortcut(
        shortcut: StaticShortcut,
        shortcutName: String,
        shortcutValue: String?,
        iconBase64: String?,
    ) {
        if (!isUserCreatedShortcut(shortcut)) return
        scope.launch(Dispatchers.IO) {
            val updated =
                appShortcutRepository.updateCustomShortcut(
                    shortcut = shortcut,
                    shortcutName = shortcutName,
                    shortcutValue = shortcutValue,
                    iconBase64 = iconBase64,
                )
            if (updated) {
                appShortcutSearchHandler.loadCachedShortcutsOnly()
            }
            withContext(Dispatchers.Main) {
                if (!updated) {
                    showToastRes(R.string.settings_app_shortcuts_update_failed)
                    return@withContext
                }
                refreshAppShortcutsState()
                refreshRecentItems()
                showToastRes(R.string.settings_app_shortcuts_update_success)
            }
        }
    }

    fun clearAllExclusions() {
        contactManager.clearAllExcludedContacts()
        fileManager.clearAllExcludedFiles()
        appManager.clearAllHiddenApps()
        settingsManager.clearAllExcludedSettings()
        calendarManager.clearAllExcludedItems()
        appShortcutManager.clearAllExcludedShortcuts()
        pinningHandler.loadExcludedContactsAndFiles()
        // Reloading queries the calendar provider; keep it off the settings screen's main thread.
        scope.launch(Dispatchers.IO) { loadPinnedAndExcludedCalendarEvents() }
        refreshSettingsState(updateResults = false)
        refreshAppShortcutsState(updateResults = false)
    }
}

private fun <T, K> List<T>.sortedByPinnedOrder(
    order: List<K>,
    keySelector: (T) -> K,
): List<T> {
    if (order.isEmpty()) return this
    val orderIndex = order.withIndex().associate { it.value to it.index }
    return sortedBy { orderIndex[keySelector(it)] ?: Int.MAX_VALUE }
}
