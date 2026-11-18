package com.tk.quicksearch.search.data.userAppPreferences

import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPreferenceDecoderTest {
    @Test
    fun preservesColdAndReloadDirectDialDefaultsWithoutOverridingSavedValues() {
        val empty = emptyMap<String, Any>()
        assertTrue(decodeStartupPreferences(empty, false, true).directDialEnabled)
        assertFalse(decodeStartupPreferences(empty, false, false).directDialEnabled)

        val stored = mapOf(BasePreferences.KEY_DIRECT_DIAL_ENABLED to false)
        assertFalse(decodeStartupPreferences(stored, false, true).directDialEnabled)
    }

    @Test
    fun looksUpTheSelectedThemeValuesAndRestoresKnownPreferences() {
        val stored = mapOf(
            UiPreferences.KEY_APP_THEME_MODE to AppThemeMode.LIGHT.name,
            UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA to 0.2f,
            UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT to 0.4f,
            UiPreferences.KEY_BACKGROUND_SOURCE to "obsolete",
            UiPreferences.KEY_OVERLAY_MODE_ENABLED to true,
            BasePreferences.KEY_ENABLED_FILE_TYPES to setOf(FileType.AUDIO.name, "UNKNOWN"),
            "file_previews_enabled" to false,
        )
        val decoded = decodeStartupPreferences(stored, systemDark = true, defaultDirectDialEnabled = false)
        assertEquals(0.4f, decoded.wallpaperBackgroundAlpha)
        assertEquals(BackgroundSource.THEME, decoded.backgroundSource)
        assertEquals(setOf(FileType.AUDIO), decoded.enabledFileTypes)
        assertEquals(false, decoded.booleanSettings["file_previews_enabled"])
    }
}
