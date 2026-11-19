package com.tk.quicksearch.appNotifications

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.glance.GlanceNotificationsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether App notifications is on, and the apps and keywords it shows. */
internal data class AppNotificationsConfig(
    val enabled: Boolean,
    val filters: AppNotificationFilters,
) {
    /** Whether apps or keywords are set, so matching is worth doing for Home or an At a Glance widget. */
    val hasFilters: Boolean get() = !filters.isEmpty

    /** Whether Home's At a Glance shows any notification. */
    val isActive: Boolean get() = enabled && hasFilters
}

/**
 * App notifications' toggle, apps and keywords, read from [GlancePreferences] once per process and
 * kept current as they change (including settings import), shared by Home, settings and
 * [GlanceNotificationsStore].
 */
internal object AppNotificationsSettings {
    private val state = MutableStateFlow<AppNotificationsConfig?>(null)

    @Volatile
    private var preferences: GlancePreferences? = null

    // Preferences hold change listeners weakly. Written under the object's lock.
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    /** Null until [load] has run. */
    val config: StateFlow<AppNotificationsConfig?> = state.asStateFlow()

    /** Reads the settings on first use and returns the cached ones after that. */
    @Synchronized
    fun load(context: Context): AppNotificationsConfig {
        state.value?.let { return it }
        val loaded = GlancePreferences(context.applicationContext)
        preferences = loaded
        listener = loaded.observeAppNotifications(::reload)
        return read(loaded).also { state.value = it }
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        preferencesFor(context).setShowAppNotificationsEnabled(enabled)
        reload()
    }

    fun setFilters(
        context: Context,
        filters: AppNotificationFilters,
    ) {
        preferencesFor(context).setAppNotificationFilters(filters)
        reload()
    }

    private fun preferencesFor(context: Context): GlancePreferences {
        load(context)
        return checkNotNull(preferences)
    }

    /**
     * Re-reads the settings and re-matches the posted notifications when they changed. Holds the
     * same lock as [load] while reading, but not while re-matching, so the store never waits on it.
     */
    private fun reload() {
        synchronized(this) {
            val loaded = preferences ?: return
            val updated = read(loaded)
            if (updated == state.value) return
            state.value = updated
        }
        GlanceNotificationsStore.refreshAppNotifications()
    }

    private fun read(preferences: GlancePreferences) =
        AppNotificationsConfig(
            enabled = preferences.isShowAppNotificationsEnabled(),
            filters = preferences.getAppNotificationFilters(),
        )
}
