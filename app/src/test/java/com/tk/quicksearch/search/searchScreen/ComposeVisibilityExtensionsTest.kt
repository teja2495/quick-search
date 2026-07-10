package com.tk.quicksearch.search.searchScreen

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.models.AppInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeVisibilityExtensionsTest {
    @Test
    fun topMatchesAppsCountAsVisibleResultsWhenRegularAppsSectionIsDisabled() {
        val state =
            SearchUiState(
                query = "tiktok",
                searchResults = listOf(appResult("TikTok")),
                disabledSections = setOf(SearchSection.APPS),
                topMatchesEnabled = true,
                topMatchesSectionOrder = listOf(SearchSection.APPS),
                disabledTopMatchesSections = emptySet(),
            )

        assertTrue(hasAnySearchResults(state))
    }

    @Test
    fun disabledSectionWithoutTopMatchesDoesNotCountAsVisibleResults() {
        val state =
            SearchUiState(
                query = "tiktok",
                searchResults = listOf(appResult("TikTok")),
                disabledSections = setOf(SearchSection.APPS),
                topMatchesEnabled = false,
            )

        assertFalse(hasAnySearchResults(state))
    }

    private fun appResult(name: String) =
        AppInfo(
            appName = name,
            packageName = "com.example.${name.lowercase()}",
            lastUsedTime = 1L,
            totalTimeInForeground = 1L,
            firstInstallTime = 1L,
            isSystemApp = false,
            hasLaunchIntent = true,
        )
}
