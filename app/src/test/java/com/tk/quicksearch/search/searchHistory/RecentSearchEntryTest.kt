package com.tk.quicksearch.search.searchHistory

import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentSearchEntryTest {
    @Test
    fun everyEntryTypeSurvivesAStorageRoundTrip() {
        val entries =
            listOf(
                RecentSearchEntry.Contact(42L),
                RecentSearchEntry.File("content://media/external/file/7"),
                RecentSearchEntry.Setting("wifi"),
                RecentSearchEntry.AppShortcut("com.example/compose"),
                RecentSearchEntry.AppSetting("app_theme"),
                RecentSearchEntry.Note(3L),
            )

        entries.forEach { entry ->
            assertEquals(entry, RecentSearchEntry.fromRaw(entry.toJsonString()))
        }
    }

    @Test
    fun queryKeepsItsAiAnswerSnapshot() {
        val entry =
            RecentSearchEntry.Query(
                query = "weather tokyo",
                aiAnswer = "Sunny, 24°C",
                aiUsedModelId = "gemini-flash",
                aiLlmProviderId = AiSearchLlmProviderId.GEMINI,
            )

        assertEquals(entry, RecentSearchEntry.fromRaw(entry.toJsonString()))
    }

    @Test
    fun queryIsStoredTrimmed() {
        val stored = RecentSearchEntry.Query("  hello  ").toJsonString()

        assertEquals(RecentSearchEntry.Query("hello"), RecentSearchEntry.fromRaw(stored))
    }

    @Test
    fun legacyPlainStringEntriesLoadAsQueries() {
        assertEquals(RecentSearchEntry.Query("old search"), RecentSearchEntry.fromRaw(" old search "))
        assertNull(RecentSearchEntry.fromRaw("   "))
    }

    @Test
    fun invalidStoredEntriesAreDroppedInsteadOfShownAsRawJson() {
        assertNull(RecentSearchEntry.fromRaw("""{"type":"note","noteId":0}"""))
        assertNull(RecentSearchEntry.fromRaw("""{"type":"future_type","id":"x"}"""))
        assertNull(RecentSearchEntry.fromRaw("""{"type":"query","query":" "}"""))
    }
}
