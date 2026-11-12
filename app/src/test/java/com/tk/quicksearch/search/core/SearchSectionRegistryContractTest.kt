package com.tk.quicksearch.search.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSectionRegistryContractTest {
    @Test
    fun enabledSectionsHaveUniqueSearchAndSettingsMappings() {
        val definitions = SearchSectionRegistry.orderedDefinitions

        assertEquals(definitions.size, definitions.map { it.section }.distinct().size)
        assertEquals(definitions.size, definitions.map { it.aliasTargetId }.distinct().size)
        assertEquals(definitions.size, definitions.map { it.itemType }.distinct().size)
        val toggleKeys = definitions.mapNotNull { it.appSettingsToggleKey }
        assertEquals(toggleKeys.size, toggleKeys.distinct().size)
        assertTrue(definitions.all { it.aliasTargetId.isNotBlank() })
    }

    @Test
    fun everyEnabledSectionCanBeResolvedThroughEachRegistrationKey() {
        SearchSectionRegistry.orderedDefinitions.forEach { definition ->
            assertEquals(definition, SearchSectionRegistry.definitionFor(definition.section))
            assertEquals(
                definition,
                SearchSectionRegistry.definitionForAliasTargetId(definition.aliasTargetId),
            )
            definition.appSettingsToggleKey?.let { toggleKey ->
                assertEquals(definition.section, SearchSectionRegistry.sectionForToggle(toggleKey))
            }
            assertEquals(
                definition.section,
                SearchSectionRegistry.sectionForItemType(definition.itemType),
            )
        }
    }

    @Test
    fun everyEnabledSectionExceptNotedHasAnAppSettingsToggle() {
        // Noted shows only when the Noted app grants access, so it deliberately has no setting.
        SearchSectionRegistry.orderedSections.forEach { section ->
            val toggleKey = SearchSectionRegistry.definitionFor(section).appSettingsToggleKey
            if (section == SearchSection.NOTED) assertNull(toggleKey) else assertNotNull(toggleKey)
        }
    }
}
