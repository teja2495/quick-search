package com.tk.quicksearch.interfaces

import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.model.SettingShortcut
import com.tk.quicksearch.search.core.SearchEngine

/**
 * Service for handling navigation and intent-based operations
 */
interface NavigationService {
    fun launchApp(appInfo: AppInfo)
    fun openAppInfo(appInfo: AppInfo)
    fun requestUninstall(appInfo: AppInfo)
    fun openSearchUrl(query: String, searchEngine: SearchEngine, clearQueryAfterSearchEngine: Boolean)
    fun searchIconPacks(clearQueryAfterSearchEngine: Boolean)
    fun openFile(deviceFile: DeviceFile)
    fun openContact(contactInfo: ContactInfo, clearQueryAfterSearchEngine: Boolean)
    fun openEmail(email: String)
    fun openUsageAccessSettings()
    fun openAppSettings()
    fun openAllFilesAccessSettings()
    fun openFilesPermissionSettings()
    fun openContactPermissionSettings()
}