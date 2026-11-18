package com.tk.quicksearch.search.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactLayoutMigrationTest {
    @Test
    fun resetsOldOptInAndRestoresOnlyAppearanceValuesStillUsingCompactDefaults() {
        val prefs = InMemorySharedPreferences()
        with(UiPreferences) {
            prefs.edit()
                .putBoolean("card_layout_enabled", false)
                .putInt(KEY_COMPACT_PRIOR_APP_GRID_COLUMNS, 4)
                .putInt(KEY_PHONE_APP_GRID_COLUMNS, COMPACT_LAYOUT_PHONE_APP_GRID_COLUMNS)
                .putFloat(KEY_COMPACT_PRIOR_FONT_SCALE_MULTIPLIER, 1f)
                .putFloat(KEY_FONT_SCALE_MULTIPLIER, 1.1f)
                .putFloat(KEY_COMPACT_PRIOR_LIGHT_WALLPAPER_ALPHA, 0.4f)
                .putFloat(KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT, COMPACT_LAYOUT_LIGHT_WALLPAPER_BACKGROUND_ALPHA)
                .apply()

            var resetCount = 0
            resetLegacyCompactLayout(prefs) { resetCount++ }

            assertTrue(prefs.getBoolean(KEY_CARD_LAYOUT_ENABLED, true))
            assertFalse(prefs.contains("card_layout_enabled"))
            assertFalse(prefs.contains(KEY_COMPACT_PRIOR_APP_GRID_COLUMNS))
            assertFalse(prefs.contains(KEY_COMPACT_PRIOR_FONT_SCALE_MULTIPLIER))
            assertFalse(prefs.contains(KEY_COMPACT_PRIOR_LIGHT_WALLPAPER_ALPHA))
            assertEquals(4, prefs.getInt(KEY_PHONE_APP_GRID_COLUMNS, 0))
            assertEquals(1.1f, prefs.getFloat(KEY_FONT_SCALE_MULTIPLIER, 0f), 0f)
            assertEquals(0.4f, prefs.getFloat(KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT, 0f), 0f)
            assertEquals(1, resetCount)

            prefs.edit().putBoolean(KEY_CARD_LAYOUT_ENABLED, false).apply()
            resetLegacyCompactLayout(prefs) { resetCount++ }

            assertFalse(prefs.getBoolean(KEY_CARD_LAYOUT_ENABLED, true))
            assertEquals(1, resetCount)
        }
    }

    @Test
    fun leavesExistingCardLayoutAndCustomAppearanceInPlace() {
        val prefs = InMemorySharedPreferences().apply {
            edit()
                .putBoolean("card_layout_enabled", true)
                .putFloat(UiPreferences.KEY_FONT_SCALE_MULTIPLIER, 1.1f)
                .apply()
        }

        resetLegacyCompactLayout(prefs) { error("Appearance should not be reset") }

        assertTrue(prefs.getBoolean(UiPreferences.KEY_CARD_LAYOUT_ENABLED, true))
        assertEquals(1.1f, prefs.getFloat(UiPreferences.KEY_FONT_SCALE_MULTIPLIER, 0f), 0f)
    }
}
