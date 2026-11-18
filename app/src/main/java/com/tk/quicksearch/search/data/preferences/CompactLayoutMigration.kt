package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.search.startup.StartupSurfaceStore

private const val LEGACY_CARD_LAYOUT_KEY = "card_layout_enabled"

internal fun migrateCompactLayoutPreference(prefs: SharedPreferences, context: Context) {
    resetLegacyCompactLayout(prefs) { StartupSurfaceStore(context).clear() }
}

/** Consumes the legacy toggle once, so a fresh opt-in survives app restarts and later updates. */
internal fun resetLegacyCompactLayout(prefs: SharedPreferences, onReset: () -> Unit) {
    synchronized(prefs) {
        if (!prefs.contains(LEGACY_CARD_LAYOUT_KEY)) return
        val oldCardLayoutEnabled = prefs.getBoolean(LEGACY_CARD_LAYOUT_KEY, true)

        val editor = prefs.edit().remove(LEGACY_CARD_LAYOUT_KEY)
        if (!oldCardLayoutEnabled && !prefs.contains(UiPreferences.KEY_CARD_LAYOUT_ENABLED)) {
            with(UiPreferences) {
                prefs.restoreCompactDefault(
                    editor, KEY_PHONE_APP_GRID_COLUMNS,
                    KEY_COMPACT_PRIOR_APP_GRID_COLUMNS, COMPACT_LAYOUT_PHONE_APP_GRID_COLUMNS,
                )
                prefs.restoreCompactDefault(
                    editor, KEY_APP_ICON_SIZE_STEP,
                    KEY_COMPACT_PRIOR_APP_ICON_SIZE_STEP, COMPACT_LAYOUT_APP_ICON_SIZE_STEP,
                )
                prefs.restoreCompactDefault(
                    editor, KEY_FONT_SCALE_MULTIPLIER,
                    KEY_COMPACT_PRIOR_FONT_SCALE_MULTIPLIER, COMPACT_LAYOUT_FONT_SCALE_MULTIPLIER,
                )
                prefs.restoreCompactDefault(
                    editor, KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT,
                    KEY_COMPACT_PRIOR_LIGHT_WALLPAPER_ALPHA, COMPACT_LAYOUT_LIGHT_WALLPAPER_BACKGROUND_ALPHA,
                )
            }
            onReset()
        }
        editor.apply()
    }
}

private fun SharedPreferences.restoreCompactDefault(
    editor: SharedPreferences.Editor,
    key: String,
    priorKey: String,
    compactDefault: Int,
) {
    if (contains(priorKey) && getInt(key, Int.MIN_VALUE) == compactDefault) {
        editor.putInt(key, getInt(priorKey, compactDefault))
    }
    editor.remove(priorKey)
}

private fun SharedPreferences.restoreCompactDefault(
    editor: SharedPreferences.Editor,
    key: String,
    priorKey: String,
    compactDefault: Float,
) {
    if (contains(priorKey) && getFloat(key, Float.NaN) == compactDefault) {
        editor.putFloat(key, getFloat(priorKey, compactDefault))
    }
    editor.remove(priorKey)
}
