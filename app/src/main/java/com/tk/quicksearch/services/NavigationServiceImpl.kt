package com.tk.quicksearch.services

import android.app.Application
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.interfaces.NavigationService
import com.tk.quicksearch.interfaces.UiFeedbackService
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.IntentHelpers
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.contacts.ContactIntentHelpers
import com.tk.quicksearch.search.settings.SettingsSearchHandler

/**
 * Implementation of NavigationService for handling navigation and intent operations
 */
class NavigationServiceImpl(
    private val application: Application,
    private val userPreferences: UserAppPreferences,
    private val settingsSearchHandler: SettingsSearchHandler,
    private val onRequestDirectSearch: (String) -> Unit,
    private val onClearQuery: () -> Unit,
    private val clearQueryAfterSearchEngine: Boolean,
    private val uiFeedbackService: UiFeedbackService
) : NavigationService {

    override fun openUsageAccessSettings() {
        IntentHelpers.openUsageAccessSettings(application)
    }

    override fun openAppSettings() {
        IntentHelpers.openAppSettings(application)
    }

    override fun openAllFilesAccessSettings() {
        IntentHelpers.openAllFilesAccessSettings(application)
    }

    override fun openFilesPermissionSettings() {
        val targetMethod = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            IntentHelpers::openAllFilesAccessSettings
        } else {
            IntentHelpers::openAppSettings
        }
        targetMethod(application)
    }

    override fun openContactPermissionSettings() {
        IntentHelpers.openAppSettings(application)
    }

    override fun launchApp(appInfo: AppInfo) {
        IntentHelpers.launchApp(application, appInfo) { stringResId, formatArg ->
            uiFeedbackService.showToast(stringResId)
        }
        userPreferences.incrementAppLaunchCount(appInfo.packageName)
        onClearQuery()
    }

    override fun openAppInfo(appInfo: AppInfo) {
        IntentHelpers.openAppInfo(application, appInfo.packageName)
    }

    override fun requestUninstall(appInfo: AppInfo) {
        IntentHelpers.requestUninstall(application, appInfo) { stringResId, formatArg ->
            uiFeedbackService.showToast(stringResId)
        }
    }

    override fun openSearchUrl(query: String, searchEngine: SearchEngine, clearQueryAfterSearchEngine: Boolean) {
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
            uiFeedbackService.showToast(stringResId)
        }

        // Save the query to recent queries
        if (trimmedQuery.isNotEmpty()) {
            userPreferences.addRecentQuery(trimmedQuery)
        }

        if (clearQueryAfterSearchEngine) {
            onClearQuery()
        }
    }

    override fun searchIconPacks(clearQueryAfterSearchEngine: Boolean) {
        val query = application.getString(com.tk.quicksearch.R.string.settings_icon_pack_search_query)
        openSearchUrl(query, SearchEngine.GOOGLE_PLAY, clearQueryAfterSearchEngine)
    }

    override fun openFile(deviceFile: DeviceFile) {
        IntentHelpers.openFile(application, deviceFile) { stringResId, formatArg ->
            uiFeedbackService.showToast(stringResId)
        }
        if (clearQueryAfterSearchEngine) {
            onClearQuery()
        }
    }

    override fun openContact(contactInfo: ContactInfo, clearQueryAfter: Boolean) {
        ContactIntentHelpers.openContact(application, contactInfo) { stringResId ->
            uiFeedbackService.showToast(stringResId)
        }
        if (clearQueryAfter) {
            onClearQuery()
        }
    }

    override fun openEmail(email: String) {
        ContactIntentHelpers.composeEmail(application, email) { stringResId ->
            uiFeedbackService.showToast(stringResId)
        }
    }
}