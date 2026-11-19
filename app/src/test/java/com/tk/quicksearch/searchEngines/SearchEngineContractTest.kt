package com.tk.quicksearch.searchEngines

import android.content.Intent
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.intentHelpers.SearchEngineIntents
import com.tk.quicksearch.shared.util.PackageConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineContractTest {
    @Test
    fun existingEngineIdsRetainTheirDefaultOrder() {
        val existingIds = listOf(
            "DIRECT_SEARCH", "GOOGLE", "CHATGPT", "GEMINI", "PERPLEXITY",
            "GOOGLE_PLAY", "FDROID", "YOUTUBE", "GOOGLE_MAPS", "WAZE",
            "GROK", "REDDIT", "AMAZON", "X", "YOUTUBE_MUSIC", "SPOTIFY",
            "CLAUDE", "GOOGLE_DRIVE", "GOOGLE_PHOTOS", "AI_MODE", "DUCKDUCKGO",
            "BRAVE", "FACEBOOK_MARKETPLACE", "YOU_COM", "WIKIPEDIA", "BING",
            "STARTPAGE", "GOOGLE_TRANSLATE", "KAGI", "KAGI_ASSISTANT", "MUSE",
        )

        assertEquals(existingIds, SearchEngine.entries.take(existingIds.size).map { it.name })
    }

    @Test
    fun configuredShortcutCodesAreUnique() {
        val shortcutCodes =
            SearchEngine.entries
                .map { it.defaultShortcutCode.lowercase() }
                .filter(String::isNotBlank)

        assertEquals(shortcutCodes.size, shortcutCodes.distinct().size)
    }

    @Test
    fun searchableEnginesHaveAQueryPlaceholder() {
        SearchEngine.entries
            .filterNot { it == SearchEngine.DIRECT_SEARCH }
            .forEach { engine ->
                assertTrue(
                    "$engine must place the encoded query in its URL",
                    engine.urlTemplate.contains("%s"),
                )
            }
    }

    @Test
    fun installDependentDefinitionsDeclarePackageCandidates() {
        SearchEngine.entries
            .filter { it.installOnly || it.defaultDisableIfAppMissing }
            .forEach { engine ->
                assertFalse(
                    "$engine depends on installation state but has no package",
                    engine.appPackages.isEmpty(),
                )
            }
    }

    @Test
    fun bundledIconsArePresentUnlessInstallOnly() {
        SearchEngine.entries.forEach { engine ->
            if (engine.installOnly) return@forEach
            assertNotNull(
                "$engine needs a bundled icon when it can appear without its app",
                engine.drawableResId,
            )
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

    @Test
    fun museNativeShareContractCarriesTrimmedQuery() {
        val spec = SearchEngineIntents.buildMuseShareIntentSpec("  plan my week  ")

        assertEquals(Intent.ACTION_SEND, spec.action)
        assertEquals(PackageConstants.MUSE_PACKAGE, spec.packageName)
        assertEquals(
            "com.facebook.aura.share.AuraShareIntentHandlerActivity",
            spec.className,
        )
        assertEquals("text/plain", spec.mimeType)
        assertEquals("plan my week", spec.text)
    }
}
