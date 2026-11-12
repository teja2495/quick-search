package com.tk.quicksearch.search.core

import android.content.Context
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.launchStaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.bookmarks.BookmarkLauncher
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.tools.aiSearch.QuickSearchHelp

internal interface SearchViewModelNavigationApi {
    val navigationApiDelegate: SearchViewModelNavigationApiDelegate

    fun openUsageAccessSettings() = navigationApiDelegate.openUsageAccessSettings()

    fun openAppSettings() = navigationApiDelegate.openAppSettings()

    fun openAllFilesAccessSettings() = navigationApiDelegate.openAllFilesAccessSettings()

    fun openFilesPermissionSettings() = navigationApiDelegate.openFilesPermissionSettings()

    fun openContactPermissionSettings() = navigationApiDelegate.openContactPermissionSettings()

    fun openCalendarPermissionSettings() = navigationApiDelegate.openCalendarPermissionSettings()

    fun launchApp(
        appInfo: AppInfo,
        launchContext: Context,
    ) = navigationApiDelegate.launchApp(appInfo, launchContext)

    fun launchAppInSplitScreen(
        appInfo: AppInfo,
        launchContext: Context,
    ) = navigationApiDelegate.launchAppInSplitScreen(appInfo, launchContext)

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

    fun recordFileOpen(deviceFile: DeviceFile) = navigationApiDelegate.recordFileOpen(deviceFile)

    fun openContainingFolder(deviceFile: DeviceFile) =
        navigationApiDelegate.openContainingFolder(deviceFile)

    fun openContact(contactInfo: ContactInfo) = navigationApiDelegate.openContact(contactInfo)

    fun openEmail(email: String) = navigationApiDelegate.openEmail(email)

    fun openSetting(setting: DeviceSetting) = navigationApiDelegate.openSetting(setting)

    fun openCalendarEvent(event: CalendarEventInfo) = navigationApiDelegate.openCalendarEvent(event)

    fun launchAppShortcut(shortcut: StaticShortcut) = navigationApiDelegate.launchAppShortcut(shortcut)

    fun openBookmark(bookmark: BookmarkInfo) = navigationApiDelegate.openBookmark(bookmark)

    fun openNotedItem(item: com.tk.quicksearch.search.models.NotedItem) = navigationApiDelegate.openNotedItem(item)

    fun onWebSuggestionTap(suggestion: String) = navigationApiDelegate.onWebSuggestionTap(suggestion)

    fun onRecentQueryTap(entry: RecentSearchEntry.Query) = navigationApiDelegate.onRecentQueryTap(entry)
}

class SearchViewModelNavigationApiDelegate internal constructor(
    private val applicationProvider: () -> android.app.Application,
    private val navigationHandler: () -> com.tk.quicksearch.app.navigation.NavigationHandler,
    private val userPreferences: com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences,
    private val resultsStateProvider: () -> SearchResultsState,
    private val currentQueryProvider: () -> String,
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

    fun launchApp(
        appInfo: AppInfo,
        launchContext: Context,
    ) {
        navigationHandler().launchApp(
            appInfo,
            launchContext,
        )
    }

    fun launchAppInSplitScreen(
        appInfo: AppInfo,
        launchContext: Context,
    ) {
        navigationHandler().launchAppInSplitScreen(
            appInfo,
            launchContext,
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

    fun recordFileOpen(deviceFile: DeviceFile) = navigationHandler().recordFileOpen(deviceFile)

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
            val query = currentQueryProvider().trim()
            if (query.isNotEmpty()) {
                userPreferences.addRecentItem(RecentSearchEntry.Query(query))
            }
            userPreferences.addRecentItem(RecentSearchEntry.AppShortcut(shortcutKey(shortcut)))
            onNavigationTriggered()
        }
    }

    fun openBookmark(bookmark: BookmarkInfo) {
        val app = applicationProvider()
        val opened =
            BookmarkLauncher.open(app, bookmark) { messageRes, arg ->
                showToastText(app.getString(messageRes, arg.orEmpty()))
            }
        if (!opened) return
        val query = currentQueryProvider().trim()
        if (query.isNotEmpty()) {
            userPreferences.addRecentItem(RecentSearchEntry.Query(query))
        }
        onNavigationTriggered()
    }

    fun openNotedItem(item: com.tk.quicksearch.search.models.NotedItem) {
        val app = applicationProvider()
        if (!com.tk.quicksearch.search.noted.NotedLauncher.open(app, item)) {
            showToastText(app.getString(com.tk.quicksearch.R.string.common_error_unable_to_open, app.getString(com.tk.quicksearch.R.string.section_noted)))
            return
        }
        val query = currentQueryProvider().trim()
        if (query.isNotEmpty()) {
            userPreferences.addRecentItem(RecentSearchEntry.Query(query))
        }
        onNavigationTriggered()
    }

    fun onWebSuggestionTap(suggestion: String) {
        val detectedTarget = resultsStateProvider().detectedShortcutTarget
        if (detectedTarget != null) {
            navigationHandler().openSearchTarget(suggestion.trim(), detectedTarget)
        } else {
            onQueryChange(suggestion)
        }
        // Treat a suggestion tap like any other query update so the applied text can
        // immediately fetch and display its own suggestions.
        updateResultsState { it.copy(webSuggestionWasSelected = false) }
    }

    fun onRecentQueryTap(entry: RecentSearchEntry.Query) {
        val trimmedQuery = entry.trimmedQuery
        if (trimmedQuery.isBlank()) return

        onQueryChange(trimmedQuery)
        updateResultsState { state ->
            state.copy(
                AiSearchState =
                    if (entry.hasAiSnapshot) {
                        AiSearchState(
                            status = AiSearchStatus.Success,
                            answer = entry.aiAnswer,
                            activeQuery = trimmedQuery,
                            usedModelId = entry.aiUsedModelId,
                            llmProviderId = entry.aiLlmProviderId,
                            isQuickSearchHelp = QuickSearchHelp.questionOrNull(trimmedQuery) != null,
                        )
                    } else {
                        AiSearchState()
                    },
                webSuggestionWasSelected = false,
            )
        }
    }
}
