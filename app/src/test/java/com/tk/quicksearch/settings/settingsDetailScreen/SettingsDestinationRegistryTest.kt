package com.tk.quicksearch.settings.settingsDetailScreen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDestinationRegistryTest {

    @Test
    fun registryCoversAllSettingsDestinations() {
        assertEquals(
            SettingsDetailType.entries.size,
            SettingsDestinationRegistry.coveredDestinationCount(),
        )
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
