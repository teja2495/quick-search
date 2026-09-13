package com.tk.quicksearch.searchEngines

import android.content.Intent
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchEngineIntents
import com.tk.quicksearch.shared.util.PackageConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineRegistryContractTest {
    @Test
    fun everyBuiltInEngineHasExactlyOneDefinition() {
        val definitionsByEngine = SearchEngineRegistry.definitions.groupBy { it.engine }

        assertEquals(SearchEngine.entries.toSet(), definitionsByEngine.keys)
        assertTrue(definitionsByEngine.values.all { it.size == 1 })
    }

    @Test
    fun configuredShortcutCodesAreUnique() {
        val shortcutCodes =
            SearchEngineRegistry.definitions
                .map { it.defaultShortcutCode }
                .filter(String::isNotBlank)

        assertEquals(shortcutCodes.size, shortcutCodes.distinct().size)
    }

    @Test
    fun searchableEnginesHaveAQueryPlaceholder() {
        SearchEngineRegistry.definitions
            .filterNot { it.engine == SearchEngine.DIRECT_SEARCH }
            .forEach { definition ->
                assertTrue(
                    "${definition.engine} must place the encoded query in its URL",
                    definition.urlTemplate.contains("%s"),
                )
            }
    }

    @Test
    fun installDependentDefinitionsDeclarePackageCandidates() {
        SearchEngineRegistry.definitions
            .filter { it.installOnly || it.defaultDisableIfAppMissing }
            .forEach { definition ->
                assertFalse(
                    "${definition.engine} depends on installation state but has no package",
                    definition.appPackages.isEmpty(),
                )
            }
    }

    @Test
    fun nativeLaunchMetadataAlwaysResolvesToAHandler() {
        SearchEngineRegistry.definitions.forEach { definition ->
            if (definition.nativeLaunchMode == SearchEngineNativeLaunchMode.NONE) {
                assertNull(SearchEngineIntents.getNativeHandler(definition.engine))
            } else {
                assertNotNull(
                    "${definition.engine} declares a native launch mode without a handler",
                    SearchEngineIntents.getNativeHandler(definition.engine),
                )
            }
        }
    }

    @Test
    fun kagiNativeShareContractCarriesTrimmedQuery() {
        val spec = SearchEngineIntents.buildKagiShareIntentSpec("  privacy search  ")

        assertEquals(Intent.ACTION_SEND, spec.action)
        assertEquals(PackageConstants.KAGI_PACKAGE, spec.packageName)
        assertEquals("com.kagi.search.HomeActivity", spec.className)
        assertEquals("text/plain", spec.mimeType)
        assertEquals("privacy search", spec.text)
    }

    @Test
    fun kagiAssistantLaunchSpecPrefillsNewThreadText() {
        val spec = SearchEngineIntents.buildKagiAssistantLaunchSpec("  ask assistant  ")

        assertEquals("ask assistant", spec.text)
        assertEquals("com.kagi.assistant://thread/new?text=ask%20assistant", spec.uriString)
    }

    @Test
    fun kagiAssistantLaunchSpecOmitsTextWhenQueryIsBlank() {
        val spec = SearchEngineIntents.buildKagiAssistantLaunchSpec("   ")

        assertNull(spec.text)
        assertEquals("com.kagi.assistant://thread/new", spec.uriString)
    }
}
