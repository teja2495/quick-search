package com.tk.quicksearch.search.fuzzy

import com.tk.quicksearch.search.core.SearchSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchPolicyResolverTest {

    @Test
    fun appsUseDistanceOneForMediumQueriesAndDistanceTwoForLongQueries() {
        val medium =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APPS,
                query = "sett",
                isLowRamDevice = false,
            )
        val long =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APPS,
                query = "settings",
                isLowRamDevice = false,
            )

        assertTrue(medium.enabled)
        assertEquals(1, medium.maximumEditDistance)
        assertTrue(long.enabled)
        assertEquals(2, long.maximumEditDistance)
    }

    @Test
    fun lowRamAppsDoNotAllowDistanceTwo() {
        val policy =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APPS,
                query = "settings",
                isLowRamDevice = true,
            )

        assertEquals(1, policy.maximumEditDistance)
        assertFalse(policy.allowDistanceTwoForLongerQueries)
    }

    @Test
    fun shortQueriesDisableFuzzyViaEffectivePolicy() {
        val apps =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APPS,
                query = "ab",
                isLowRamDevice = false,
            )
        val appSettings =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APP_SETTINGS,
                query = "zz",
                isLowRamDevice = false,
            )

        assertFalse(apps.enabled)
        assertFalse(appSettings.enabled)
    }

    @Test
    fun contactsPolicyExposesCandidateCapForRepositoryFetches() {
        val normal =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.CONTACTS,
                query = "alex",
                isLowRamDevice = false,
            )
        val lowRam =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.CONTACTS,
                query = "alex",
                isLowRamDevice = true,
            )

        assertEquals(600, normal.candidateLimit)
        assertEquals(360, lowRam.candidateLimit)
    }

    @Test
    fun filesPolicyExposesCandidateCapAndLowRamDisablesFuzzy() {
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
        assertEquals(700, normal.candidateLimit)
        assertFalse(lowRam.enabled)
    }

    @Test
    fun smallStaticSectionsAllowDistanceTwoOnlyForLongQueries() {
        val medium =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APP_SETTINGS,
                query = "wifi",
                isLowRamDevice = false,
            )
        val long =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.SETTINGS,
                query = "bluetooth",
                isLowRamDevice = false,
            )

        assertEquals(1, medium.maximumEditDistance)
        assertEquals(2, long.maximumEditDistance)
    }

    @Test
    fun lowRamSmallStaticSectionsTightenScoresAndDistance() {
        val device =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.SETTINGS,
                query = "display",
                isLowRamDevice = true,
            )
        val appSettings =
            FuzzySearchPolicyResolver.effectivePolicy(
                section = SearchSection.APP_SETTINGS,
                query = "search",
                isLowRamDevice = true,
            )

        assertEquals(1, device.maximumEditDistance)
        assertEquals(80, device.minimumScore)
        assertEquals(1, appSettings.maximumEditDistance)
        assertEquals(80, appSettings.minimumScore)
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

        assertFalse(FuzzySearchPolicyResolver.policyFor(SearchSection.CALENDAR).enabled)
        assertFalse(FuzzySearchPolicyResolver.policyFor(SearchSection.NOTES).enabled)
        assertFalse(calendar.enabled)
        assertFalse(notes.enabled)
    }
}
