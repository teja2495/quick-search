package com.tk.quicksearch.searchEngines

import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.UnifiedSearchResults
import com.tk.quicksearch.search.core.UnifiedSectionSearchResult
import com.tk.quicksearch.search.core.UnifiedSectionSearchConfig
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondarySearchOrchestratorTest {
    @Test
    fun `rapid query changes publish the latest search`() = runTest {
        val harness = Harness(this)

        harness.search("first")
        advanceTimeBy(100)
        harness.search("second")
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), harness.dataSource.queries)
        assertFalse(harness.state.isSecondarySearchInProgress)
    }

    @Test
    fun `late non-cancellable result cannot overwrite the latest query`() = runTest {
        val harness =
            Harness(
                scope = this,
                disabledSections = allSecondarySections - SearchSection.CONTACTS,
                initialState = SearchUiState(hasContactPermission = true),
            ) { query ->
                withContext(NonCancellable) {
                    delay(if (query == "old") 400 else 50)
                }
                UnifiedSearchResults(contactResults = listOf(contact(query)))
            }

        harness.search("old")
        advanceTimeBy(150)
        runCurrent()
        harness.search("new")
        advanceUntilIdle()

        assertEquals(listOf("old", "new"), harness.dataSource.queries)
        assertEquals(listOf("new"), harness.state.contactResults.map { it.displayName })
        assertFalse(harness.state.isSecondarySearchInProgress)
    }

    @Test
    fun `no-result prefix skips longer query and backspace makes section searchable again`() = runTest {
        val harness =
            Harness(
                scope = this,
                disabledSections = allSecondarySections - SearchSection.CONTACTS,
                initialState = SearchUiState(hasContactPermission = true),
            )

        harness.search("ab")
        advanceUntilIdle()
        harness.search("abc")
        advanceUntilIdle()
        harness.search("ab")
        advanceUntilIdle()

        assertEquals(listOf("ab", "ab"), harness.dataSource.queries)
    }

    @Test
    fun `top matches can search a section disabled from regular results`() = runTest {
        val harness =
            Harness(
                scope = this,
                disabledSections = allSecondarySections,
                initialState =
                    SearchUiState(
                        hasContactPermission = true,
                        topMatchesEnabled = true,
                        topMatchesSectionOrder = listOf(SearchSection.CONTACTS),
                    ),
            )

        harness.search("ada")
        advanceUntilIdle()

        assertTrue(harness.dataSource.configs.single().getValue(SearchSection.CONTACTS).shouldSearch)
    }

    @Test
    fun `permission-off section does not run and staged app results are flushed`() = runTest {
        val pendingApp = app("pending")
        val harness =
            Harness(
                scope = this,
                disabledSections = allSecondarySections - SearchSection.CONTACTS,
                initialState = SearchUiState(pendingSearchResults = listOf(pendingApp)),
            )

        harness.search("ada")
        advanceUntilIdle()

        assertTrue(harness.dataSource.queries.isEmpty())
        assertEquals(listOf(pendingApp), harness.state.searchResults)
        assertNull(harness.state.pendingSearchResults)
    }

    @Test
    fun `clearing query cancels work and clears secondary and staged results`() = runTest {
        val harness =
            Harness(
                scope = this,
                initialState =
                    SearchUiState(
                        contactResults = listOf(contact("stale")),
                        pendingSearchResults = listOf(app("pending")),
                    ),
            )

        harness.search("query")
        advanceTimeBy(100)
        harness.search("")
        advanceUntilIdle()

        assertEquals(listOf("query"), harness.dataSource.queries)
        assertTrue(harness.state.contactResults.isEmpty())
        assertTrue(harness.state.searchResults.isEmpty())
        assertNull(harness.state.pendingSearchResults)
        assertFalse(harness.state.isSecondarySearchInProgress)
    }

    private class Harness(
        scope: TestScope,
        disabledSections: Set<SearchSection> = emptySet(),
        initialState: SearchUiState = SearchUiState(),
        responder: suspend (String) -> UnifiedSearchResults = { UnifiedSearchResults() },
    ) {
        var state = initialState
        val dataSource = FakeDataSource(responder)
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        private val orchestrator =
            SecondarySearchOrchestrator(
                scope = scope,
                unifiedSearchHandler = dataSource,
                webSuggestionHandler = FakeWebSuggestions,
                sectionManager = FixedDisabledSections(disabledSections),
                uiStateUpdater = { transform -> state = transform(state) },
                currentStateProvider = { state },
                workerDispatcher = dispatcher,
                mainDispatcher = dispatcher,
                mainThreadChecker = { true },
            )

        fun search(query: String) {
            state = state.copy(query = query)
            orchestrator.performSecondarySearches(query)
        }
    }

    private class FakeDataSource(
        private val responder: suspend (String) -> UnifiedSearchResults,
    ) : SecondarySearchDataSource {
        val queries = mutableListOf<String>()
        val configs = mutableListOf<Map<SearchSection, UnifiedSectionSearchConfig>>()

        override suspend fun performSearch(
            query: String,
            enabledFileTypes: Set<FileType>,
            sectionSearchConfig: Map<SearchSection, UnifiedSectionSearchConfig>,
            showFolders: Boolean,
            showSystemFiles: Boolean,
            aliasSection: SearchSection?,
            sectionSearchDelayMillis: Map<SearchSection, Long>,
            onSectionResult: suspend (
                result: UnifiedSectionSearchResult,
                recencyIndex: RecentResultRankingUtils.RecencyIndex,
            ) -> Unit,
        ): UnifiedSearchResults {
            queries += query
            configs += sectionSearchConfig
            val results = responder(query)
            sectionSearchConfig.filterValues { it.shouldSearch }.keys.forEach { section ->
                val result =
                    when (section) {
                        SearchSection.CONTACTS -> UnifiedSectionSearchResult.Contacts(results.contactResults)
                        SearchSection.FILES -> UnifiedSectionSearchResult.Files(results.fileResults)
                        SearchSection.SETTINGS -> UnifiedSectionSearchResult.Settings(results.settingResults)
                        SearchSection.CALENDAR -> UnifiedSectionSearchResult.Calendar(results.calendarEvents)
                        SearchSection.REMINDERS -> UnifiedSectionSearchResult.Reminders(results.reminderResults)
                        SearchSection.NOTES -> UnifiedSectionSearchResult.Notes(results.noteResults)
                        SearchSection.APP_SETTINGS ->
                            UnifiedSectionSearchResult.AppSettings(results.appSettingResults)
                        SearchSection.APP_SHORTCUTS ->
                            UnifiedSectionSearchResult.AppShortcuts(results.appShortcutResults)
                        SearchSection.APPS -> UnifiedSectionSearchResult.Skipped(section)
                    }
                onSectionResult(result, results.recencyIndex)
            }
            return results
        }
    }

    private data class FixedDisabledSections(
        override val disabledSections: Set<SearchSection>,
    ) : DisabledSearchSectionsProvider

    private object FakeWebSuggestions : SecondaryWebSuggestionController {
        override val isEnabled: Boolean = false

        override fun fetchWebSuggestions(
            query: String,
            currentQueryVersion: Long,
            activeQueryVersionProvider: () -> Long,
            activeQueryProvider: () -> String,
        ) = Unit

        override fun cancelSuggestions() = Unit
    }

    private companion object {
        val allSecondarySections: Set<SearchSection>
            get() = SearchSectionRegistry.secondarySearchDefinitions.map { it.section }.toSet()

        fun contact(name: String) =
            ContactInfo(
                contactId = name.hashCode().toLong(),
                lookupKey = name,
                displayName = name,
                phoneNumbers = emptyList(),
            )

        fun app(name: String) =
            AppInfo(
                appName = name,
                packageName = "test.$name",
                lastUsedTime = 0L,
                totalTimeInForeground = 0L,
                firstInstallTime = 0L,
                isSystemApp = false,
            )
    }
}
