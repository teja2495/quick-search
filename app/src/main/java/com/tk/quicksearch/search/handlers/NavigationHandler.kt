package com.tk.quicksearch.search.handlers

import android.app.Application
import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.contacts.ContactIntentHelpers
import com.tk.quicksearch.search.settings.SettingsSearchHandler
import com.tk.quicksearch.model.SettingShortcut

class NavigationHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: SettingsSearchHandler,
    private val onRequestDirectSearch: (String) -> Unit,
    private val onClearQuery: () -> Unit,
    private val clearQueryAfterSearchEngine: Boolean,
    private val showToastCallback: (Int) -> Unit
) {
    private val context: Context get() = application.applicationContext

    fun openUsageAccessSettings() {
        IntentHelpers.openUsageAccessSettings(application)
    }

    fun openAppSettings() {
        IntentHelpers.openAppSettings(application)
    }

    fun openAllFilesAccessSettings() {
        IntentHelpers.openAllFilesAccessSettings(application)
    }

    fun openFilesPermissionSettings() {
        val targetMethod = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            IntentHelpers::openAllFilesAccessSettings
        } else {
            IntentHelpers::openAppSettings
        }
        targetMethod(application)
    }

    fun openContactPermissionSettings() {
        IntentHelpers.openAppSettings(application)
    }

    fun launchApp(appInfo: AppInfo) {
        IntentHelpers.launchApp(application, appInfo) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
        userPreferences.incrementAppLaunchCount(appInfo.packageName)
        onClearQuery()
    }

    fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(application, appInfo.packageName)
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(application, appInfo) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
    }

    fun openSearchUrl(query: String, searchEngine: SearchEngine, clearQueryAfterSearchEngine: Boolean) {
        val trimmedQuery = query.trim()
        if (searchEngine == SearchEngine.DIRECT_SEARCH) {
            onRequestDirectSearch(trimmedQuery)
            return
        }
        val amazonDomain = if (searchEngine == SearchEngine.AMAZON) {
            userPreferences.getAmazonDomain()
        } else {
            null
        }
        IntentHelpers.openSearchUrl(application, trimmedQuery, searchEngine, amazonDomain) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }

        // Save the query to recent queries
        if (trimmedQuery.isNotEmpty()) {
            userPreferences.addRecentQuery(trimmedQuery)
        }

        if (clearQueryAfterSearchEngine) {
            onClearQuery()
        }
    }

    fun searchIconPacks(clearQueryAfter: Boolean = false) {
        val query = application.getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY, clearQueryAfter)
    }

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(application, deviceFile) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
        if (clearQueryAfterSearchEngine) {
            onClearQuery()
        }
    }

    fun openSetting(setting: SettingShortcut) {
        settingsSearchHandler.openSetting(setting)
        onClearQuery()
    }

    fun openContact(contactInfo: ContactInfo, clearQueryAfter: Boolean) {
        ContactIntentHelpers.openContact(application, contactInfo) { stringResId ->
            showToastCallback(stringResId)
        }
        if (clearQueryAfter) {
            onClearQuery()
        }
    }

    fun openEmail(email: String) {
        ContactIntentHelpers.composeEmail(application, email) { stringResId ->
            showToastCallback(stringResId)
        }
    }
}
