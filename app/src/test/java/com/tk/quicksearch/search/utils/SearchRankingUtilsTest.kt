package com.tk.quicksearch.search.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankingUtilsTest {
    @Test
    fun singleTokenQueryMatchesMultiWordTextWhenSpacesAreOmitted() {
        val priority = SearchRankingUtils.calculateMatchPriority("Bala Guna Teja", "balagunateja")

        assertTrue(DefaultSearchMatcher.isMatch(priority))
    }

    @Test
    fun multiWordQueryMatchesTextWhenTargetSpacesAreOmitted() {
        val priority = SearchRankingUtils.calculateMatchPriority("BalaGunaTeja", "bala guna teja")

        assertTrue(DefaultSearchMatcher.isMatch(priority))
    }

    @Test
    fun compactMatchingStillRequiresContiguousCharacters() {
        val priority = SearchRankingUtils.calculateMatchPriority("Bala Teja", "balagunateja")

        assertFalse(DefaultSearchMatcher.isMatch(priority))
    }

    @Test
    fun compactMatchingIgnoresPunctuationSeparators() {
        listOf("fdr", "fdro", "fdroi").forEach { query ->
            val priority = SearchRankingUtils.calculateMatchPriority("F-Droid", query)

            assertTrue("Expected F-Droid to match $query", DefaultSearchMatcher.isMatch(priority))
        }
    }

    @Test
    fun nicknameMatchingIsSpaceInsensitive() {
        val priority =
            SearchRankingUtils.calculateMatchPriorityWithNickname(
                primaryText = "Contact",
                nickname = "Bala Guna Teja",
                query = "balagunateja",
            )

        assertTrue(DefaultSearchMatcher.isMatch(priority))
    }
}
