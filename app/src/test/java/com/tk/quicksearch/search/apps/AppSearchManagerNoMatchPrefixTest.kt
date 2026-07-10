package com.tk.quicksearch.search.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchManagerNoMatchPrefixTest {
    @Test
    fun longerQueryWithSameNoMatchPrefixIsSkipped() {
        assertTrue(
            AppSearchManager.shouldSkipDueToNoMatchPrefix(
                normalizedQuery = "tiktok video",
                noMatchPrefix = "tiktok",
            ),
        )
    }

    @Test
    fun shorterQueryClearsTheNoMatchBehavior() {
        assertFalse(
            AppSearchManager.shouldSkipDueToNoMatchPrefix(
                normalizedQuery = "tik",
                noMatchPrefix = "tiktok",
            ),
        )
    }

    @Test
    fun singleCharacterQueriesAreNeverSkipped() {
        assertFalse(
            AppSearchManager.shouldSkipDueToNoMatchPrefix(
                normalizedQuery = "t",
                noMatchPrefix = "ti",
            ),
        )
    }
}
