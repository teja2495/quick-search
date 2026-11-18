package com.tk.quicksearch.search.fuzzy

import com.tk.quicksearch.search.core.SearchSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchPolicyResolverTest {

    @Test
    fun lowRamDisablesFuzzyFileSearch() {
        val normal =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.FILES,
                query = "report",
                isLowRamDevice = false,
            )
        val lowRam =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.FILES,
                query = "report",
                isLowRamDevice = true,
            )

        assertTrue(normal.enabled)
        assertFalse(lowRam.enabled)
    }

    @Test
    fun calendarAndNotesPoliciesKeepFuzzyDisabled() {
        val calendar =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.CALENDAR,
                query = "meeting",
                isLowRamDevice = false,
            )
        val notes =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.NOTES,
                query = "grocery",
                isLowRamDevice = false,
            )

        assertFalse(calendar.enabled)
        assertFalse(notes.enabled)
    }
}
