package com.tk.quicksearch.search.appSettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BooleanSettingTest {
    @Test
    fun definitionUsesExistingFilePreviewKeyAndDefault() {
        val setting = BooleanSettings.FILE_PREVIEWS.setting

        assertEquals("app_toggle_file_previews", setting.searchId)
        assertTrue(setting.valueIn(readBooleanSettings(emptyMap<String, Boolean>())))
        assertFalse(setting.valueIn(readBooleanSettings(mapOf("file_previews_enabled" to false))))
    }

    @Test
    fun registeredSettingsHaveUniqueStorageAndSearchIds() {
        val settings = registeredBooleanSettings

        assertEquals(settings.size, settings.map { it.preferenceKey }.toSet().size)
        assertEquals(settings.size, settings.map { it.searchId }.toSet().size)
    }
}
