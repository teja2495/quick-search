package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo

internal data class SectionSearchSpec(
        val section: SearchSection,
        val shouldSearch: Boolean,
        val search: suspend () -> SectionSearchResultPayload,
        val applyResult: suspend (SectionSearchResultPayload) -> UnifiedSectionSearchResult,
)

internal fun UnifiedSectionSearchResult.resultCount(): Int =
        when (this) {
                is UnifiedSectionSearchResult.Skipped -> 0
                is UnifiedSectionSearchResult.Contacts -> results.size
                is UnifiedSectionSearchResult.Files -> results.size
                is UnifiedSectionSearchResult.Settings -> results.size
                is UnifiedSectionSearchResult.Calendar -> results.size
                is UnifiedSectionSearchResult.Reminders -> results.size
                is UnifiedSectionSearchResult.Bookmarks -> results.size
                is UnifiedSectionSearchResult.Notes -> results.size
                is UnifiedSectionSearchResult.Noted -> results.size
                is UnifiedSectionSearchResult.AppSettings -> results.size
                is UnifiedSectionSearchResult.AppShortcuts -> results.size
        }

internal sealed interface SectionSearchResultPayload {
        data object Skipped : SectionSearchResultPayload

        data class Contacts(
                val results: List<ContactInfo>,
        ) : SectionSearchResultPayload

        data class Files(
                val results: List<DeviceFile>,
        ) : SectionSearchResultPayload

        data class Settings(
                val results: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
        ) : SectionSearchResultPayload

        data class Calendar(
                val results: List<CalendarEventInfo>,
        ) : SectionSearchResultPayload

        data class AppSettings(
                val results: List<AppSettingResult>,
        ) : SectionSearchResultPayload

        data class Reminders(
                val results: List<ReminderInfo>,
        ) : SectionSearchResultPayload

        data class Bookmarks(
                val results: List<BookmarkInfo>,
        ) : SectionSearchResultPayload

        data class Notes(
                val results: List<NoteInfo>,
        ) : SectionSearchResultPayload

        data class AppShortcuts(
                val results: List<StaticShortcut>,
        ) : SectionSearchResultPayload

        data class Noted(
                val results: List<com.tk.quicksearch.search.models.NotedItem>,
        ) : SectionSearchResultPayload
}
