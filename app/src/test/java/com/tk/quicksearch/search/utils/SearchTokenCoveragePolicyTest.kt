package com.tk.quicksearch.search.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTokenCoveragePolicyTest {
    @Test
    fun everyQueryTokenMustBeCoveredAcrossPrimaryAndSupportingText() {
        val query = SearchQueryContext.fromRawQuery("teja passport")

        assertTrue(
            SearchTokenCoveragePolicy.areAllTokensCovered(
                query = query,
                primaryText = "Call Teja",
                supportingText = "Passport office",
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            ),
        )
        assertFalse(
            SearchTokenCoveragePolicy.areAllTokensCovered(
                query = query,
                primaryText = "Call Teja",
                supportingText = "Phone",
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            ),
        )
    }

    @Test
    fun minorTypoCanStillCoverAQueryToken() {
        assertTrue(
            SearchTokenCoveragePolicy.areAllTokensCovered(
                query = SearchQueryContext.fromRawQuery("teja passprot"),
                primaryText = "Teja Passport",
                supportingText = null,
                fuzzyMinScore = 72,
                fuzzyMaxEditDistance = 2,
            ),
        )
    }
}
