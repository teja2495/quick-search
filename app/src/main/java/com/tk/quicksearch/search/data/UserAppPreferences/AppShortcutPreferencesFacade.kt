package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.AppShortcutPreferences

/**
 * Facade for app shortcut preference operations
 */
class AppShortcutPreferencesFacade(
    private val appShortcutPreferences: AppShortcutPreferences
) {
    fun getPinnedAppShortcutIds(): Set<String> = appShortcutPreferences.getPinnedAppShortcutIds()

    fun getExcludedAppShortcutIds(): Set<String> =
            appShortcutPreferences.getExcludedAppShortcutIds()

    fun getDisabledAppShortcutIds(): Set<String> =
            appShortcutPreferences.getDisabledAppShortcutIds()

    fun pinAppShortcut(id: String): Set<String> = appShortcutPreferences.pinAppShortcut(id)

    fun unpinAppShortcut(id: String): Set<String> = appShortcutPreferences.unpinAppShortcut(id)

    fun excludeAppShortcut(id: String): Set<String> = appShortcutPreferences.excludeAppShortcut(id)

    fun removeExcludedAppShortcut(id: String): Set<String> =
            appShortcutPreferences.removeExcludedAppShortcut(id)

    fun clearAllExcludedAppShortcuts(): Set<String> =
            appShortcutPreferences.clearAllExcludedAppShortcuts()

    fun setAppShortcutEnabled(
            id: String,
            enabled: Boolean,
    ): Set<String> = appShortcutPreferences.setAppShortcutEnabled(id, enabled)
}