package com.tk.quicksearch.search.appShortcuts

import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutSearchPolicyTest {
    @Test
    fun multiWordQueryRejectsShortcutWhenAQueryTokenIsNotCovered() {
        val covered =
            AppShortcutSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("teja passport"),
                displayName = "Call Teja",
                appLabel = "Phone",
                nickname = null,
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            )

        assertFalse(covered)
    }

    @Test
    fun multiWordQueryCanBeCoveredBySupportingText() {
        val covered =
            AppShortcutSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("teja passport"),
                displayName = "Call Teja",
                appLabel = "Passport Office",
                nickname = null,
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            )

        assertTrue(covered)
    }
}
