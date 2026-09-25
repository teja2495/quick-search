package com.tk.quicksearch.shared.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageManagerTest {

    @Test
    fun supportedLanguageTagsIncludesIndonesian() {
        assertTrue(
            "supportedLanguageTags must include 'id'",
            AppLanguageManager.supportedLanguageTags.contains("id"),
        )
        assertFalse(
            "supportedLanguageTags should use modern 'id' instead of legacy 'in'",
            AppLanguageManager.supportedLanguageTags.contains("in"),
        )
    }

    @Test
    fun normalizeLanguageTagMapsLegacyInToId() {
        assertEquals("id", AppLanguageManager.normalizeLanguageTag("in"))
        assertEquals("id", AppLanguageManager.normalizeLanguageTag("id"))
        assertEquals("en", AppLanguageManager.normalizeLanguageTag("en"))
        assertEquals(null, AppLanguageManager.normalizeLanguageTag(null))
    }

    @Test
    fun isSameLanguageHandlesIndonesianEquivalence() {
        assertTrue(AppLanguageManager.isSameLanguage("id", "in"))
        assertTrue(AppLanguageManager.isSameLanguage("in", "id"))
        assertTrue(AppLanguageManager.isSameLanguage("id", "id"))
        assertTrue(AppLanguageManager.isSameLanguage("in", "in"))
        assertTrue(AppLanguageManager.isSameLanguage("en", "en"))
        assertFalse(AppLanguageManager.isSameLanguage("en", "id"))
        assertFalse(AppLanguageManager.isSameLanguage("en", "in"))
    }
}
