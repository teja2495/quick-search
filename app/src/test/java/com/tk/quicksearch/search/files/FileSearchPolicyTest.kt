package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSearchPolicyTest {

    @Test
    fun multiWordQueriesMatchHyphenSeparatedFileNames() {
        assertFileMatches("teja-karlapudi.pdf", "teja karlapudi")
    }

    @Test
    fun multiWordQueriesMatchRepeatedSeparators() {
        assertFileMatches("teja--karlapudi.pdf", "teja karlapudi")
    }

    @Test
    fun multiWordQueriesMatchOtherCommonFileNameSeparators() {
        assertFileMatches("teja_karlapudi.pdf", "teja karlapudi")
        assertFileMatches("teja.karlapudi.pdf", "teja karlapudi")
    }

    @Test
    fun separatorInsensitiveMatchingStillRequiresAllQueryTokens() {
        val priority =
            FileSearchPolicy.matchPriority(
                displayName = "teja-resume.pdf",
                nickname = null,
                query = SearchQueryContext.fromRawQuery("teja karlapudi"),
            )

        assertFalse(DefaultSearchMatcher.isMatch(priority))
    }

    private fun assertFileMatches(
        displayName: String,
        query: String,
    ) {
        val priority =
            FileSearchPolicy.matchPriority(
                displayName = displayName,
                nickname = null,
                query = SearchQueryContext.fromRawQuery(query),
            )

        assertTrue(DefaultSearchMatcher.isMatch(priority))
    }
}
