package com.tk.quicksearch.appNotifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tk.quicksearch.appNotifications.AppNotificationApp
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.appNotifications.AppNotificationsSettings
import com.tk.quicksearch.search.data.AppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An installed app the user can add to App notifications. */
internal class AppNotificationPickerApp(
    val packageName: String,
    val label: String,
)

/**
 * State and edits for [AppNotificationsActivity]: the saved apps and keywords, read off the main
 * thread, and every installed app for the Add App picker. Edits save through
 * [AppNotificationsSettings], which Home follows, or, when the activity was started with an At a
 * Glance widget's list, stay in [savedStateHandle] until the activity returns them.
 */
internal class AppNotificationsViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    /** Whether this edits an At a Glance widget's own list rather than the app's. */
    val isWidget: Boolean = savedStateHandle.contains(EXTRA_WIDGET_FILTERS)

    /** Null until the settings have been read. */
    val filters: StateFlow<AppNotificationFilters?> =
        if (isWidget) {
            savedStateHandle.getStateFlow<AppNotificationFilters?>(EXTRA_WIDGET_FILTERS, null)
        } else {
            AppNotificationsSettings.config
                .map { it?.filters }
                .stateIn(viewModelScope, SharingStarted.Eagerly, AppNotificationsSettings.config.value?.filters)
        }

    private val installedAppsState = MutableStateFlow<List<AppNotificationPickerApp>?>(null)

    /** Every launchable app but this one, A to Z; null until [loadInstalledApps] has finished. */
    val installedApps: StateFlow<List<AppNotificationPickerApp>?> = installedAppsState.asStateFlow()

    init {
        if (!isWidget && AppNotificationsSettings.config.value == null) {
            viewModelScope.launch(Dispatchers.IO) { AppNotificationsSettings.load(application) }
        }
    }

    /** Reads the installed apps once, for the Add App picker. */
    fun loadInstalledApps() {
        if (installedAppsState.value != null) return
        viewModelScope.launch {
            installedAppsState.value = withContext(Dispatchers.IO) { readInstalledApps() }
        }
    }

    fun addApp(packageName: String) =
        edit { filters ->
            if (filters.apps.any { it.packageName == packageName }) {
                filters
            } else {
                filters.copy(apps = filters.apps + AppNotificationApp(packageName))
            }
        }

    fun removeApp(packageName: String) =
        edit { filters -> filters.copy(apps = filters.apps.filterNot { it.packageName == packageName }) }

    fun setAnyAppKeywords(keywords: List<String>) = edit { it.copy(keywords = keywords) }

    fun setAppKeywords(
        packageName: String,
        keywords: List<String>,
    ) = edit { filters ->
        filters.copy(apps = filters.apps.map { if (it.packageName == packageName) it.copy(keywords = keywords) else it })
    }

    /** Applies [change] to the saved filters; does nothing before they have been read. */
    private fun edit(change: (AppNotificationFilters) -> AppNotificationFilters) {
        val current = filters.value ?: return
        val updated = change(current)
        if (updated == current) return
        if (isWidget) {
            savedStateHandle[EXTRA_WIDGET_FILTERS] = updated
        } else {
            AppNotificationsSettings.setFilters(getApplication(), updated)
        }
    }

    private suspend fun readInstalledApps(): List<AppNotificationPickerApp> {
        val context = getApplication<Application>()
        val repository = AppsRepository(context)
        return (repository.loadCachedApps() ?: repository.loadLaunchableApps())
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { AppNotificationPickerApp(it.packageName, it.appName) }
            .sortedBy { it.label.lowercase() }
    }
}
