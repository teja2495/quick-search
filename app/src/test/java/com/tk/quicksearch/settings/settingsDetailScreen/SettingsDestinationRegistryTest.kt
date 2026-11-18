package com.tk.quicksearch.settings.settingsDetailScreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDestinationRegistryTest {

    @Test
    fun searchablePageIdsAreStableAndUnique() {
        val ids = SettingsDetailType.entries.mapNotNull { it.searchShortcut?.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals("app_settings_gemini_api", SettingsDetailType.GEMINI_API_CONFIG.searchShortcut?.id)
        assertEquals("app_settings_bookmarks_list", SettingsDetailType.BOOKMARKS.searchShortcut?.id)
    }

    @Test
    fun resolveBackDestinationUsesExpectedFallbacks() {
        assertEquals(
            SettingsDetailType.SEARCH_RESULTS,
            SettingsDetailType.APP_MANAGEMENT.resolveBackDestination(sourceDetailType = null),
        )
        assertEquals(
            SettingsDetailType.TOOLS,
            SettingsDetailType.UNIT_CONVERTER_INFO.resolveBackDestination(sourceDetailType = null),
        )
        assertNull(SettingsDetailType.TOOLS.resolveBackDestination(sourceDetailType = null))
    }

    @Test
    fun geminiApiConfigPrefersSourceDestinationWhenProvided() {
        assertEquals(
            SettingsDetailType.TOOLS,
            SettingsDetailType.GEMINI_API_CONFIG.resolveBackDestination(
                sourceDetailType = SettingsDetailType.TOOLS,
            ),
        )
        assertNull(
            SettingsDetailType.GEMINI_API_CONFIG.resolveBackDestination(sourceDetailType = null),
        )
    }
}
