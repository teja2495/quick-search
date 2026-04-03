package com.tk.quicksearch.search.core

import android.content.Intent
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutRepository
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.apps.invalidateAppIconCache
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val isAppShortcutsLoadInFlight: AtomicBoolean,
    private val hasCalendarPermission: () -> Boolean,
    private val updateUiState: ((SearchUiState) -> SearchUiState) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val updatePermissionState: ((SearchPermissionState) -> SearchPermissionState) -> Unit,
    private val showToastRes: (Int) -> Unit,
    private val refreshRecentItems: () -> Unit,
) {
    private val pinningHandler get() = handlersProvider().pinningHandler
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

    fun loadSettingsShortcuts() {
        scope.launch(Dispatchers.IO) {
            settingsSearchHandler.loadShortcuts()
            withContext(Dispatchers.Main) { refreshSettingsState(updateResults = false) }
        }
    }

    fun loadAppShortcuts() {
        if (!isAppShortcutsLoadInFlight.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            try {
                val loadedCached = appShortcutSearchHandler.loadCachedShortcutsOnly()
                if (loadedCached) {
                    withContext(Dispatchers.Main) { refreshAppShortcutsState() }
                }

                val loadedFresh = appShortcutSearchHandler.refreshShortcutsFromSystem()
                if (loadedFresh || !loadedCached) {
                    withContext(Dispatchers.Main) { refreshAppShortcutsState() }
                }
            } finally {
                isAppShortcutsLoadInFlight.set(false)
            }
        }
    }

    fun refreshAppShortcutsCacheFirst() {
        loadAppShortcuts()
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

    private fun applyAppShortcutIconOverrides(
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

    fun refreshAppShortcutsState(updateResults: Boolean = true) {
        val query = if (updateResults) resultsStateProvider().query else ""
        val disabledShortcutIds = userPreferences.getDisabledAppShortcutIds()
        val iconOverrides = userPreferences.getAllAppShortcutIconOverrides()
        val currentState =
            appShortcutSearchHandler.getShortcutsState(
                query = query,
                isSectionEnabled = SearchSection.APP_SHORTCUTS !in sectionManager.disabledSections,
            )

        val pinned = applyAppShortcutIconOverrides(currentState.pinned, iconOverrides)
        val excluded = applyAppShortcutIconOverrides(currentState.excluded, iconOverrides)
        val allShortcuts =
            applyAppShortcutIconOverrides(
                appShortcutSearchHandler.getAvailableShortcuts(),
                iconOverrides,
            )

        updateUiState { state ->
            state.copy(
                allAppShortcuts = allShortcuts,
                disabledAppShortcutIds = disabledShortcutIds,
                pinnedAppShortcuts = pinned,
                excludedAppShortcuts = excluded,
                appShortcutResults =
                    if (updateResults) {
                        applyAppShortcutIconOverrides(currentState.results, iconOverrides)
                    } else {
                        state.appShortcutResults
                    },
            )
        }
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
                )
            }
            return
        }

        val excludedIds = userPreferences.getExcludedCalendarEventIds()
        val pinnedIds =
            userPreferences.getPinnedCalendarEventIds().filterNot { excludedIds.contains(it) }.toSet()
        val pinned = calendarRepository.getEventsByIds(pinnedIds)
        val excluded = calendarRepository.getEventsByIds(excludedIds)

        updateResultsState {
            it.copy(
                pinnedCalendarEvents = pinned,
                excludedCalendarEvents = excluded,
            )
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
        loadPinnedAndExcludedCalendarEvents()
        refreshSettingsState(updateResults = false)
        refreshAppShortcutsState(updateResults = false)
    }
}
