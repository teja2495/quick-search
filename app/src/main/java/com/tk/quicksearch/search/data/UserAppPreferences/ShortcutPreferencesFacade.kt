package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.data.preferences.ShortcutPreferences

/**
 * Facade for shortcut preference operations
 */
class ShortcutPreferencesFacade(
    private val shortcutPreferences: ShortcutPreferences
) {
    fun areShortcutsEnabled(): Boolean = shortcutPreferences.areShortcutsEnabled()

    fun setShortcutsEnabled(enabled: Boolean) = shortcutPreferences.setShortcutsEnabled(enabled)

    fun getShortcutCode(engine: SearchEngine): String = shortcutPreferences.getShortcutCode(engine)

    fun setShortcutCode(
            engine: SearchEngine,
            code: String,
    ) = shortcutPreferences.setShortcutCode(engine, code)

    fun getShortcutCode(targetId: String): String? = shortcutPreferences.getShortcutCode(targetId)

    fun setShortcutCode(
            targetId: String,
            code: String,
    ) = shortcutPreferences.setShortcutCode(targetId, code)

    fun isShortcutEnabled(engine: SearchEngine): Boolean =
            shortcutPreferences.isShortcutEnabled(engine)

    fun setShortcutEnabled(
            engine: SearchEngine,
            enabled: Boolean,
    ) = shortcutPreferences.setShortcutEnabled(engine, enabled)

    fun getAllShortcutCodes(): Map<SearchEngine, String> = shortcutPreferences.getAllShortcutCodes()
}