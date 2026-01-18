package com.tk.quicksearch.navigation

import android.app.Application
import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.contacts.utils.ContactIntentHelpers
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.deviceSettings.DeviceSetting

class NavigationHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: DeviceSettingsSearchHandler,
    private val onRequestDirectSearch: (String) -> Unit,
    private val onClearQuery: () -> Unit,
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

    fun openAppInfo(packageName: String) {
        IntentHelpers.openAppInfo(application, packageName)
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(application, appInfo) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
    }

    fun openSearchUrl(query: String, searchEngine: SearchEngine) {
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

        // Always clear query after search
        onClearQuery()
    }

    fun searchIconPacks() {
        val query = application.getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY)
    }

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(application, deviceFile) { stringResId, formatArg ->
            // For now, just show the string resource ID since we can't format from UI layer
            // TODO: Consider passing formatted strings or extending the callback
            showToastCallback(stringResId)
        }
        // Always clear query after opening file
        onClearQuery()
    }

    fun openSetting(setting: DeviceSetting) {
        settingsSearchHandler.openSetting(setting)
        onClearQuery()
    }

    fun openContact(contactInfo: ContactInfo) {
        ContactIntentHelpers.openContact(application, contactInfo) { stringResId ->
            showToastCallback(stringResId)
        }
        // Always clear query after opening contact
        onClearQuery()
    }

    fun openEmail(email: String) {
        ContactIntentHelpers.composeEmail(application, email) { stringResId ->
            showToastCallback(stringResId)
        }
    }
}
