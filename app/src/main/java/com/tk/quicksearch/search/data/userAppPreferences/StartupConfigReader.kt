package com.tk.quicksearch.search.data.userAppPreferences

import android.content.Context
import android.content.res.Configuration
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.migrateCompactLayoutPreference

internal class StartupConfigReader(private val context: Context) {
    fun loadStartupConfig(): StartupPreferencesFacade.StartupConfig {
        // Preserve the cold-start default for users who have never made the direct-dial choice.
        val preferences = readPreferences(defaultDirectDialEnabled = true)
        val appCachePrefs = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
        return StartupPreferencesFacade.StartupConfig(
            oneHandedMode = preferences.oneHandedMode,
            cachedAppsLastUpdate = appCachePrefs.getLong("last_update", 0L),
            startupPreferences = preferences,
        )
    }

    fun readPreferences(defaultDirectDialEnabled: Boolean): StartupPreferencesFacade.StartupPreferences {
        val prefs = context.getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        migrateCompactLayoutPreference(prefs, context)
        val systemDark =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return decodeStartupPreferences(prefs.all, systemDark, defaultDirectDialEnabled)
    }
}
