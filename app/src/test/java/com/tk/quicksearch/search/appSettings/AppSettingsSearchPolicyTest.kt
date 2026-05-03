package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsSearchPolicyTest {
    @Test
    fun multiWordQueryRejectsSettingWhenAQueryTokenIsNotCovered() {
        val covered =
            AppSettingsSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("teja passport"),
                title = "Teja options",
                description = "general preferences",
                keywords = listOf("appearance", "layout"),
                fuzzyMinScore = 78,
                fuzzyMaxEditDistance = 2,
            )

        assertFalse(covered)
    }

    @Test
    fun multiWordQueryCanBeCoveredByDescriptionOrKeywords() {
        val covered =
            AppSettingsSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("teja passport"),
                title = "Teja options",
                description = "passport preferences",
                keywords = listOf("appearance", "layout"),
                fuzzyMinScore = 78,
                fuzzyMaxEditDistance = 2,
            )

        assertTrue(covered)
    }
}
