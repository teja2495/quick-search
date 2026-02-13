package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Preferences for app shortcut settings such as pinned and excluded shortcuts.
 */
class AppShortcutPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getPinnedAppShortcutIds(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED_APP_SHORTCUTS)

    fun getExcludedAppShortcutIds(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS)

    fun getDisabledAppShortcutIds(): Set<String> = getStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS)

    fun pinAppShortcut(id: String): Set<String> = pinStringItem(BasePreferences.KEY_PINNED_APP_SHORTCUTS, id)

    fun unpinAppShortcut(id: String): Set<String> = unpinStringItem(BasePreferences.KEY_PINNED_APP_SHORTCUTS, id)

    fun excludeAppShortcut(id: String): Set<String> = excludeStringItem(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS, id)

    fun removeExcludedAppShortcut(id: String): Set<String> = removeExcludedStringItem(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS, id)

    fun clearAllExcludedAppShortcuts(): Set<String> = clearAllExcludedStringItems(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS)

    fun setAppShortcutEnabled(
        id: String,
        enabled: Boolean,
    ): Set<String> =
        updateStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS) { disabledIds ->
            if (enabled) {
                disabledIds.remove(id)
            } else {
                disabledIds.add(id)
            }
        }
}
