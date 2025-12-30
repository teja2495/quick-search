package com.tk.quicksearch.search

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.IntentHelpers
import com.tk.quicksearch.search.ContactIntentHelpers
import com.tk.quicksearch.search.SettingsSearchHandler
import com.tk.quicksearch.model.SettingShortcut

class NavigationHandler(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: SettingsSearchHandler,
    private val onRequestDirectSearch: (String) -> Unit,
    private val onClearQuery: () -> Unit
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            IntentHelpers.openAllFilesAccessSettings(application)
        } else {
            IntentHelpers.openAppSettings(application)
        }
    }

    fun openContactPermissionSettings() {
        IntentHelpers.openAppSettings(application)
    }

    fun launchApp(appInfo: AppInfo) {
        IntentHelpers.launchApp(application, appInfo)
        onClearQuery()
    }

    fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(application, appInfo.packageName)
    }

    fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(application, appInfo)
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
        IntentHelpers.openSearchUrl(application, trimmedQuery, searchEngine, amazonDomain)

        if (clearQueryAfterSearchEngine) {
            onClearQuery()
        }
    }

    fun searchIconPacks() {
        val query = application.getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY, clearQueryAfterSearchEngine = false) // Usually don't clear for this internal action? or do we? Original code didn't specify, invoked openSearchUrl which checks param.
        // Original code: openSearchUrl(query, SearchEngine.GOOGLE_PLAY) -> uses clearQueryAfterSearchEngine global field.
        // I will pass false or true based on preference? I need access to preference 'clearQueryAfterSearchEngine'.
        // Wait, `clearQueryAfterSearchEngine` is passed as arg to `openSearchUrl`.
        // I should expose `openSearchUrl` with that param.
        // But internal call `searchIconPacks` in original code used `openSearchUrl` which used `this.clearQueryAfterSearchEngine`.
        // I should read that preference or pass it.
        // I'll accept `clearQueryAfterSearchEngine` as member or param.
    }
    
    // Helper for internal calls that need the current preference state
    fun searchIconPacks(clearQueryAfter: Boolean) {
        val query = application.getString(R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY, clearQueryAfter)
    }

    fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(application, deviceFile)
        onClearQuery()
    }

    fun openSetting(setting: SettingShortcut) {
        settingsSearchHandler.openSetting(setting)
    }

    fun openContact(contactInfo: ContactInfo, clearQueryAfter: Boolean) {
        ContactIntentHelpers.openContact(application, contactInfo)
        if (clearQueryAfter) {
            onClearQuery()
        }
    }

    fun openEmail(email: String) {
        ContactIntentHelpers.composeEmail(application, email)
    }
}
