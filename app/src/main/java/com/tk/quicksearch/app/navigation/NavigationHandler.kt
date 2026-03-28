package com.tk.quicksearch.app.navigation

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.search.core.normalizeToBrowsableUrl
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry

class NavigationHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: DeviceSettingsSearchHandler,
    private val onRequestDirectSearch: (String, Boolean) -> Unit,
    private val onClearQuery: () -> Unit,
    private val onExternalNavigation: () -> Unit,
    private val showToastCallback: (Int) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun openUsageAccessSettings() {
        IntentHelpers.openUsageAccessSettings(application)
        onExternalNavigation()
    }

    fun openAppSettings() {
        IntentHelpers.openAppSettings(application)
        onExternalNavigation()
    }

    fun openAllFilesAccessSettings() {
        IntentHelpers.openAllFilesAccessSettings(application)
        onExternalNavigation()
    }

    fun openFilesPermissionSettings() {
        val targetMethod =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                IntentHelpers::openAllFilesAccessSettings
            } else {
                IntentHelpers::openAppSettings
            }
        targetMethod(application)
        onExternalNavigation()
    }

    fun openContactPermissionSettings() {
        IntentHelpers.openAppSettings(application)
        onExternalNavigation()
    }

    fun openCalendarPermissionSettings() {
        IntentHelpers.openAppSettings(application)
        onExternalNavigation()
    }

    fun launchApp(
        appInfo: AppInfo,
        shouldTrackRecentFallback: Boolean,
    ) {
        IntentHelpers.launchApp(application, appInfo) { stringResId, _ ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
        userPreferences.incrementAppLaunchCount(appInfo.packageName, appInfo.userHandleId)
        if (shouldTrackRecentFallback) {
            userPreferences.addRecentAppLaunch(appInfo.launchCountKey())
        }
        onClearQuery()
    }

    fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(application, appInfo.packageName)
        onExternalNavigation()
    }

    fun openAppInfo(packageName: String) {
        IntentHelpers.openAppInfo(application, packageName)
        onExternalNavigation()
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(application, appInfo) { stringResId, _ ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
        onExternalNavigation()
    }

    fun openSearchUrl(
        query: String,
        searchEngine: SearchEngine,
        addToSearchHistory: Boolean = true,
    ) {
        val trimmedQuery = query.trim()

        if (searchEngine == SearchEngine.DIRECT_SEARCH) {
            onRequestDirectSearch(trimmedQuery, addToSearchHistory)
            return
        }

        // Save the query to recent queries
        if (addToSearchHistory && trimmedQuery.isNotEmpty()) {
            userPreferences.addRecentItem(RecentSearchEntry.Query(trimmedQuery))
        }

        val amazonDomain =
            if (searchEngine == SearchEngine.AMAZON) {
                userPreferences.getAmazonDomain()
            } else {
                null
            }
        IntentHelpers.openSearchUrl(application, trimmedQuery, searchEngine, amazonDomain) {
            stringResId,
            _,
            ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }

        // Always clear query after search
        onClearQuery()
    }

    fun openSearchTarget(
        query: String,
        target: SearchTarget,
        addToSearchHistory: Boolean = true,
    ) {
        val trimmedQuery = query.trim()
        when (target) {
            is SearchTarget.Engine -> {
                openSearchUrl(trimmedQuery, target.engine, addToSearchHistory)
            }

            is SearchTarget.Browser -> {
                if (addToSearchHistory && trimmedQuery.isNotEmpty()) {
                    userPreferences.addRecentItem(RecentSearchEntry.Query(trimmedQuery))
                }

                if (isLikelyWebUrl(trimmedQuery)) {
                    IntentHelpers.openBrowserUrl(
                        application,
                        normalizeToBrowsableUrl(trimmedQuery) ?: trimmedQuery,
                        target.app.packageName,
                    ) { stringResId, _ -> showToastCallback(stringResId) }
                } else {
                    IntentHelpers.openBrowserSearch(
                        application,
                        trimmedQuery,
                        target.app.packageName,
                    ) { stringResId, _ -> showToastCallback(stringResId) }
                }

                onClearQuery()
            }

            is SearchTarget.Custom -> {
                if (addToSearchHistory && trimmedQuery.isNotEmpty()) {
                    userPreferences.addRecentItem(RecentSearchEntry.Query(trimmedQuery))
                }

                IntentHelpers.openCustomSearchUrl(
                    application,
                    trimmedQuery,
                    target.custom.urlTemplate,
                ) { stringResId, _ -> showToastCallback(stringResId) }

                onClearQuery()
            }
        }
    }

    fun searchIconPacks() {
        val query = application.getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY, addToSearchHistory = false)
    }

    fun openFile(deviceFile: DeviceFile) {
        userPreferences.addRecentItem(RecentSearchEntry.File(deviceFile.uri.toString()))
        onClearQuery()
        mainHandler.post {
            IntentHelpers.openFile(application, deviceFile) { stringResId, _ ->
                showToastCallback(stringResId)
            }
        }
    }

    fun openContainingFolder(deviceFile: DeviceFile) {
        onClearQuery()
        mainHandler.post {
            IntentHelpers.openContainingFolder(application, deviceFile) { stringResId, _ ->
                showToastCallback(stringResId)
            }
        }
    }

    fun openSetting(setting: DeviceSetting) {
        settingsSearchHandler.openSetting(setting)
        onClearQuery()
    }

    fun openContact(contactInfo: ContactInfo) {
        val success = ContactIntentHelpers.openContact(application, contactInfo) { stringResId ->
            showToastCallback(stringResId)
        }
        if (!success) {
            return
        }
        userPreferences.addRecentItem(RecentSearchEntry.Contact(contactInfo.contactId))
        // Always clear query after opening contact
        onClearQuery()
    }

    fun openEmail(email: String) {
        val success = ContactIntentHelpers.composeEmail(application, email) { stringResId ->
            showToastCallback(stringResId)
        }
        if (success) {
            onExternalNavigation()
        }
    }

    fun openCalendarEvent(event: CalendarEventInfo) {
        mainHandler.post {
            IntentHelpers.openCalendarEvent(application, event.eventId) { stringResId, _ ->
                showToastCallback(stringResId)
            }
        }
        onClearQuery()
    }
}
