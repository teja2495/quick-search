package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry

internal interface SearchViewModelNavigationApi {
    val navigationApiDelegate: SearchViewModelNavigationApiDelegate

    fun openUsageAccessSettings() = navigationApiDelegate.openUsageAccessSettings()

    fun openAppSettings() = navigationApiDelegate.openAppSettings()

    fun openAllFilesAccessSettings() = navigationApiDelegate.openAllFilesAccessSettings()

    fun openFilesPermissionSettings() = navigationApiDelegate.openFilesPermissionSettings()

    fun openContactPermissionSettings() = navigationApiDelegate.openContactPermissionSettings()

    fun openCalendarPermissionSettings() = navigationApiDelegate.openCalendarPermissionSettings()

    fun launchApp(appInfo: AppInfo) = navigationApiDelegate.launchApp(appInfo)

    fun openAppInfo(appInfo: AppInfo) = navigationApiDelegate.openAppInfo(appInfo)

    fun openAppInfo(packageName: String) = navigationApiDelegate.openAppInfo(packageName)

    fun requestUninstall(appInfo: AppInfo) = navigationApiDelegate.requestUninstall(appInfo)

    fun openSearchUrl(
        query: String,
        searchEngine: SearchEngine,
        addToSearchHistory: Boolean = true,
    ) = navigationApiDelegate.openSearchUrl(query, searchEngine, addToSearchHistory)

    fun openSearchTarget(
        query: String,
        target: SearchTarget,
        addToSearchHistory: Boolean = true,
    ) = navigationApiDelegate.openSearchTarget(query, target, addToSearchHistory)

    fun searchIconPacks() = navigationApiDelegate.searchIconPacks()

    fun openFile(deviceFile: DeviceFile) = navigationApiDelegate.openFile(deviceFile)

    fun openContainingFolder(deviceFile: DeviceFile) =
        navigationApiDelegate.openContainingFolder(deviceFile)

    fun openContact(contactInfo: ContactInfo) = navigationApiDelegate.openContact(contactInfo)

    fun openEmail(email: String) = navigationApiDelegate.openEmail(email)

    fun openSetting(setting: DeviceSetting) = navigationApiDelegate.openSetting(setting)

    fun openCalendarEvent(event: CalendarEventInfo) = navigationApiDelegate.openCalendarEvent(event)

    fun launchAppShortcut(shortcut: StaticShortcut) = navigationApiDelegate.launchAppShortcut(shortcut)

    fun onWebSuggestionTap(suggestion: String) = navigationApiDelegate.onWebSuggestionTap(suggestion)
}

class SearchViewModelNavigationApiDelegate internal constructor(
    private val applicationProvider: () -> android.app.Application,
    private val navigationHandler: () -> com.tk.quicksearch.app.navigation.NavigationHandler,
    private val userPreferences: com.tk.quicksearch.search.data.UserAppPreferences,
    private val permissionStateProvider: () -> SearchPermissionState,
    private val resultsStateProvider: () -> SearchResultsState,
    private val onQueryChange: (String) -> Unit,
    private val updateResultsState: ((SearchResultsState) -> SearchResultsState) -> Unit,
    private val onNavigationTriggered: () -> Unit,
    private val showToastText: (String) -> Unit,
) {
    fun openUsageAccessSettings() = navigationHandler().openUsageAccessSettings()

    fun openAppSettings() = navigationHandler().openAppSettings()

    fun openAllFilesAccessSettings() = navigationHandler().openAllFilesAccessSettings()

    fun openFilesPermissionSettings() = navigationHandler().openFilesPermissionSettings()

    fun openContactPermissionSettings() = navigationHandler().openContactPermissionSettings()

    fun openCalendarPermissionSettings() = navigationHandler().openCalendarPermissionSettings()

    fun launchApp(appInfo: AppInfo) {
        navigationHandler().launchApp(
            appInfo,
            shouldTrackRecentFallback = !permissionStateProvider().hasUsagePermission,
        )
    }

    fun openAppInfo(appInfo: AppInfo) = navigationHandler().openAppInfo(appInfo)

    fun openAppInfo(packageName: String) = navigationHandler().openAppInfo(packageName)

    fun requestUninstall(appInfo: AppInfo) = navigationHandler().requestUninstall(appInfo)

    fun openSearchUrl(query: String, searchEngine: SearchEngine, addToSearchHistory: Boolean) {
        navigationHandler().openSearchUrl(query, searchEngine, addToSearchHistory)
    }

    fun openSearchTarget(query: String, target: SearchTarget, addToSearchHistory: Boolean) {
        navigationHandler().openSearchTarget(query, target, addToSearchHistory)
    }

    fun searchIconPacks() = navigationHandler().searchIconPacks()

    fun openFile(deviceFile: DeviceFile) = navigationHandler().openFile(deviceFile)

    fun openContainingFolder(deviceFile: DeviceFile) = navigationHandler().openContainingFolder(deviceFile)

    fun openContact(contactInfo: ContactInfo) = navigationHandler().openContact(contactInfo)

    fun openEmail(email: String) = navigationHandler().openEmail(email)

    fun openSetting(setting: DeviceSetting) = navigationHandler().openSetting(setting)

    fun openCalendarEvent(event: CalendarEventInfo) = navigationHandler().openCalendarEvent(event)

    fun launchAppShortcut(shortcut: StaticShortcut) {
        val error =
            launchStaticShortcut(
                context = applicationProvider(),
                shortcut = shortcut,
                skipSearchTargetQueryHistory = true,
            )
        if (error != null) {
            showToastText(error)
        } else {
            userPreferences.addRecentItem(RecentSearchEntry.AppShortcut(shortcutKey(shortcut)))
            onNavigationTriggered()
        }
    }

    fun onWebSuggestionTap(suggestion: String) {
        val detectedTarget = resultsStateProvider().detectedShortcutTarget
        if (detectedTarget != null) {
            navigationHandler().openSearchTarget(suggestion.trim(), detectedTarget)
        } else {
            onQueryChange(suggestion)
        }
        updateResultsState { it.copy(webSuggestionWasSelected = true) }
    }
}
