package com.tk.quicksearch.search.contacts

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchPolicyTest {
    @Test
    fun `number search matches a phone number suffix when enabled`() {
        val priority =
            ContactSearchPolicy.matchPriority(
                displayName = "Example Contact",
                nickname = null,
                query = SearchQueryContext.fromRawQuery("9552"),
                phoneNumbers = listOf("(716) 507-9552"),
                allowNumberSearch = true,
            )

        assertTrue(DefaultSearchMatcher.isMatch(priority))
    }

    @Test
    fun `number search does not match phone numbers when disabled`() {
        val priority =
            ContactSearchPolicy.matchPriority(
                displayName = "Example Contact",
                nickname = null,
                query = SearchQueryContext.fromRawQuery("9552"),
                phoneNumbers = listOf("7165079552"),
                allowNumberSearch = false,
            )

        assertFalse(DefaultSearchMatcher.isMatch(priority))
    }

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
