package com.tk.quicksearch.data.preferences

import android.content.Context

import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.searchengines.getDefaultShortcutCode
import com.tk.quicksearch.search.handlers.ShortcutValidator.isValidShortcutCode
import com.tk.quicksearch.search.handlers.ShortcutValidator.normalizeShortcutCodeInput

/**
 * Preferences for shortcut-related settings such as enabled shortcuts and custom codes.
 */
class ShortcutPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Shortcut Preferences
    // ============================================================================

    fun areShortcutsEnabled(): Boolean = getBooleanPref(KEY_SHORTCUTS_ENABLED, true)

    fun setShortcutsEnabled(enabled: Boolean) {
        setBooleanPref(KEY_SHORTCUTS_ENABLED, enabled)
    }

    fun getShortcutCode(engine: SearchEngine): String {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val defaultCode = engine.getDefaultShortcutCode()
        val storedCode = prefs.getString(key, defaultCode) ?: defaultCode
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        return if (isValidShortcutCode(normalizedCode)) {
            normalizedCode
        } else {
            defaultCode
        }
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (!isValidShortcutCode(normalizedCode)) {
            return
        }
        prefs.edit().putString(key, normalizedCode).apply()
    }

    fun isShortcutEnabled(engine: SearchEngine): Boolean {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        return getBooleanPref(key, true)
    }

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        setBooleanPref(key, enabled)
    }

    fun getAllShortcutCodes(): Map<SearchEngine, String> {
        return SearchEngine.values().associateWith { getShortcutCode(it) }
    }
}
