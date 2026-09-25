package com.tk.quicksearch.search.core

import android.content.Context
import android.os.SystemClock
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchHandler
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingsSearchHandler
import com.tk.quicksearch.search.apps.AppSearchPerformanceLogger
import com.tk.quicksearch.search.contacts.ContactSearchPolicy
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.CalendarRepository
import com.tk.quicksearch.search.data.preferences.CalendarPreferences
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.DeviceSettingsSearchHandler
import com.tk.quicksearch.search.files.FileSearchHandler
import com.tk.quicksearch.search.files.FileSearchPolicy
import com.tk.quicksearch.search.files.FolderPathPatternMatcher
import com.tk.quicksearch.search.fuzzy.FuzzySearchPolicyResolver
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileClassifier
import com.tk.quicksearch.search.utils.FuzzyMatcher
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.tk.quicksearch.searchEngines.SecondarySearchDataSource

data class UnifiedSearchResults(
        val contactResults: List<ContactInfo> = emptyList(),
        val fileResults: List<DeviceFile> = emptyList(),
        val settingResults: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting> =
                emptyList(),
        val calendarEvents: List<CalendarEventInfo> = emptyList(),
        val reminderResults: List<ReminderInfo> = emptyList(),
        val noteResults: List<NoteInfo> = emptyList(),
        val appSettingResults: List<AppSettingResult> = emptyList(),
        val appShortcutResults: List<StaticShortcut> = emptyList(),
        val recencyIndex: RecentResultRankingUtils.RecencyIndex =
                RecentResultRankingUtils.RecencyIndex(),
)

data class UnifiedSectionSearchConfig(
        val shouldSearch: Boolean = false,
        val enableFuzzyMatching: Boolean = false,
)

sealed interface UnifiedSectionSearchResult {
        val section: SearchSection

        data class Skipped(override val section: SearchSection) : UnifiedSectionSearchResult

        data class Contacts(val results: List<ContactInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.CONTACTS
        }

        data class Files(val results: List<DeviceFile>) : UnifiedSectionSearchResult {
                override val section = SearchSection.FILES
        }

        data class Settings(
                val results: List<com.tk.quicksearch.search.deviceSettings.DeviceSetting>,
        ) : UnifiedSectionSearchResult {
                override val section = SearchSection.SETTINGS
        }

        data class Calendar(val results: List<CalendarEventInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.CALENDAR
        }

        data class Reminders(val results: List<ReminderInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.REMINDERS
        }

        data class Notes(val results: List<NoteInfo>) : UnifiedSectionSearchResult {
                override val section = SearchSection.NOTES
        }

        data class AppSettings(val results: List<AppSettingResult>) : UnifiedSectionSearchResult {
                override val section = SearchSection.APP_SETTINGS
        }

        data class AppShortcuts(val results: List<StaticShortcut>) : UnifiedSectionSearchResult {
                override val section = SearchSection.APP_SHORTCUTS
        }
}
