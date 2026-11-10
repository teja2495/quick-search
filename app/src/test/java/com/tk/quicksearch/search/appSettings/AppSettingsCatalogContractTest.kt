package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.core.SearchSectionRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsCatalogContractTest {
    @Test
    fun validCatalogRequiresExactlyOneToggleForEveryEnabledSearchSection() {
        validateAppSettingsCatalog(validCatalog())
    }

    @Test
    fun duplicateIdsAreRejected() {
        val duplicate = validCatalog().first()

        assertContractViolation(validCatalog() + duplicate)
    }

    @Test
    fun missingSearchSectionToggleIsRejected() {
        assertContractViolation(validCatalog().dropLast(1))
    }

    @Test
    fun blankIdsAreRejected() {
        val blankId = validCatalog().first().copy(id = "")

        assertContractViolation(listOf(blankId) + validCatalog().drop(1))
    }

    private fun validCatalog(): List<AppSettingResult> =
        SearchSectionRegistry.orderedDefinitions.mapNotNull { definition ->
            val toggleKey = definition.appSettingsToggleKey ?: return@mapNotNull null
            AppSettingResult(
                id = "toggle_${definition.section.name.lowercase()}",
                title = definition.section.name,
                action = AppSettingResultAction.TOGGLE,
                toggleKey = toggleKey,
            )
        }

    private fun assertContractViolation(settings: List<AppSettingResult>) {
        assertTrue(runCatching { validateAppSettingsCatalog(settings) }.isFailure)
    }
}
