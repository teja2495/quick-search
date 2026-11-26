package com.tk.quicksearch.searchEngines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AliasValidatorTest {
    @Test
    fun normalizeKeepsEachCommaSeparatedAliasOnce() {
        assertEquals("yt,tube", AliasValidator.normalizeShortcutCodeInput(" YT , tube,,yt, "))
        assertEquals("g", AliasValidator.normalizeShortcutCodeInput("G"))
        assertEquals("", AliasValidator.normalizeShortcutCodeInput(" , "))
    }

    @Test
    fun sanitizeKeepsTrailingCommaWhileTyping() {
        assertEquals("yt,", AliasValidator.sanitizeAliasInput("Yt, "))
    }

    @Test
    fun onlyCommasIsNotAValidAlias() {
        assertFalse(AliasValidator.isValidGeneralAliasCode(",,"))
        assertTrue(AliasValidator.isValidGeneralAliasCode("a,"))
    }

    @Test
    fun exactConflictChecksEveryAliasOnBothSides() {
        val existing = mapOf("google" to "g,gg", "bing" to "b")
        assertTrue(AliasValidator.hasExactAliasConflict("x,gg", existing))
        assertTrue(AliasValidator.hasExactAliasConflict("b", existing))
        assertFalse(AliasValidator.hasExactAliasConflict("x,y", existing))
    }

    @Test
    fun triggerConflictChecksEveryWordOnBothSides() {
        assertTrue(AliasValidator.hasTriggerAliasConflict("x,Café", listOf("mail,cafe")))
        assertFalse(AliasValidator.hasTriggerAliasConflict("x,y", listOf("mail,cafe")))
    }
}
