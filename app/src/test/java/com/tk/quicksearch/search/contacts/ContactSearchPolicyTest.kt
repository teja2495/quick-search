package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchPolicyTest {
    @Test
    fun multiWordQueryRejectsContactWhenAQueryTokenIsNotCovered() {
        val covered =
            ContactSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("Teja passport"),
                displayName = "Teja Teppala",
                nickname = null,
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            )

        assertFalse(covered)
    }

    @Test
    fun multiWordQueryAllowsMinorTypoWhenTokenIsStillFuzzyCovered() {
        val covered =
            ContactSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("Teja passprot"),
                displayName = "Teja Passport",
                nickname = null,
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            )

        assertTrue(covered)
    }

    @Test
    fun multiWordQueryCanBeCoveredByNickname() {
        val covered =
            ContactSearchPolicy.areAllQueryTokensCovered(
                query = SearchQueryContext.fromRawQuery("Teja passport"),
                displayName = "Teja Teppala",
                nickname = "Passport Office",
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            )

        assertTrue(covered)
    }
}
