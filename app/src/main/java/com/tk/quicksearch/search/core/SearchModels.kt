package com.tk.quicksearch.search.core

import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.preferences.DEFAULT_WEB_SUGGESTIONS_ENABLED
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiTools.DictionarySense
import com.tk.quicksearch.tools.aiSearch.LlmTextModel
import com.tk.quicksearch.tools.tasker.TaskerIntentTool
import com.tk.quicksearch.tools.inlineSearch.InlineToolData
import com.tk.quicksearch.tools.inlineSearch.SearchToolType

data class IconPackInfo(
        val packageName: String,
        val label: String,
)

data class BrowserApp(
        val packageName: String,
        val label: String,
)

data class CustomSearchEngine(
        val id: String,
        val name: String,
        val urlTemplate: String,
        val faviconBase64: String? = null,
        val browserPackage: String? = null,
)

data class CustomTool(
        val id: String,
        val name: String,
        val prompt: String,
        val modelId: String,
        val providerId: AiSearchLlmProviderId = AiSearchLlmProviderId.GEMINI,
        val groundingEnabled: Boolean = false,
        val thinkingEnabled: Boolean = false,
        val advancedPayload: String? = null,
        val advancedPayloadEnabled: Boolean = false,
)

sealed class SearchTarget {
        data class Engine(
                val engine: SearchEngine,
        ) : SearchTarget()

        data class Browser(
                val app: BrowserApp,
        ) : SearchTarget()

        data class Custom(
                val custom: CustomSearchEngine,
        ) : SearchTarget()
}

enum class SearchSection {
        APPS,
        APP_SHORTCUTS,
        CONTACTS,
        FILES,
        SETTINGS,
        CALENDAR,
        REMINDERS,
        NOTES,
        NOTED,
        APP_SETTINGS,
        BOOKMARKS,
}

enum class MessagingApp {
        MESSAGES,
        WHATSAPP,
        WHATSAPP_BUSINESS,
        TELEGRAM,
        SIGNAL,
}

enum class CallingApp {
        CALL,
        GOOGLE_MEET,
        WHATSAPP,
        WHATSAPP_BUSINESS,
        TELEGRAM,
        SIGNAL,
}

/**
 * Persisted app theme. [MONOCHROME] is the "Default" theme; every other value is a palette
 * offered under "Custom" (see [CUSTOM_APP_THEME_PALETTES]).
 */
enum class AppTheme {
        FOREST,
        AURORA,
        SUNSET,
        MONOCHROME,
        LAVENDER,
        TEAL,
        AMBER,
        INDIGO,
        SLATE,
        ROSE,
        MOCHA,
}

/**
 * Preset palettes shown in the Colorful theme picker, in display order: one or two per popular
 * color family (roughly by hue, then brown and grey).
 */
val CUSTOM_APP_THEME_PALETTES: List<AppTheme> =
        listOf(
                AppTheme.ROSE,
                AppTheme.SUNSET,
                AppTheme.AMBER,
                AppTheme.FOREST,
                AppTheme.TEAL,
                AppTheme.AURORA,
                AppTheme.INDIGO,
                AppTheme.LAVENDER,
                AppTheme.MOCHA,
                AppTheme.SLATE,
        )

enum class BackgroundSource {
        THEME,
        SYSTEM_WALLPAPER,
        CUSTOM_IMAGE,
}

enum class AccentColorMode {
        NONE,
        FROM_WALLPAPER,
        CUSTOM,
}

enum class AppThemeMode {
        LIGHT,
        DARK,
        SYSTEM,
}

enum class StartupPhase {
        PHASE_0_SHELL,
        PHASE_1_CACHE_PREFS,
        PHASE_2_HEAVY_FEATURES,
        COMPLETE,
}

enum class AiSearchStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class CurrencyConverterStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class WorldClockStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class DictionaryStatus {
        Idle,
        Loading,
        Success,
        Error,
}

enum class WeatherStatus {
        Idle,
        Loading,
        Success,
        Error,
}

sealed interface ScreenTimeState {
        data object Hidden : ScreenTimeState
        data object Loading : ScreenTimeState
        data class Available(
                val durationMillis: Long,
                val topApps: List<ScreenTimeAppUsage>,
        ) : ScreenTimeState
}

data class ScreenTimeAppUsage(
        val packageName: String,
        val appName: String,
        val durationMillis: Long,
)

data class AiSearchState(
        val status: AiSearchStatus = AiSearchStatus.Idle,
        val answer: String? = null,
        val isFollowUp: Boolean = false,
        val webSearchDisabledForRequest: Boolean = false,
        val showWebSearchFallbackTip: Boolean = false,
        val errorMessage: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val llmProviderId: AiSearchLlmProviderId? = null,
        val isQuickSearchHelp: Boolean = false,
        /** False for setup errors (missing key or model) where retrying the request can't help. */
        val canRetry: Boolean = true,
)

data class CurrencyConverterState(
        val status: CurrencyConverterStatus = CurrencyConverterStatus.Idle,
        val convertedAmount: String? = null,
        val targetCurrencyCode: String? = null,
        val targetCurrencyName: String? = null,
        val sourceAmount: String? = null,
        val sourceCurrencyCode: String? = null,
        val ratesFetchedAtMillis: Long? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val errorMessage: String? = null,
)

data class WorldClockState(
        val status: WorldClockStatus = WorldClockStatus.Idle,
        val worldClockText: String? = null,
        val sourceTimeText: String? = null,
        val placeText: String? = null,
        val timeZoneText: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val llmProviderId: AiSearchLlmProviderId? = null,
        val errorMessage: String? = null,
)

data class DictionaryState(
        val status: DictionaryStatus = DictionaryStatus.Idle,
        val word: String? = null,
        val senses: List<DictionarySense> = emptyList(),
        val activeQuery: String? = null,
        val errorMessage: String? = null,
)

data class WeatherState(
        val status: WeatherStatus = WeatherStatus.Idle,
        val location: String? = null,
        val summary: String? = null,
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val llmProviderId: AiSearchLlmProviderId? = null,
        val errorMessage: String? = null,
)

data class CalculatorState(
        val result: String? = null,
        val isToolMode: Boolean = false,
        val toolData: InlineToolData? = null,
        val toolType: SearchToolType = SearchToolType.CALCULATOR,
        val showInvalidExpression: Boolean = false,
        /** Epoch millis for the date parsed by the date calculator tool. */
        val parsedDateMillis: Long? = null,
        /** True when the user entered a relative expression (e.g. "2 years ago") and the result is an absolute date. */
        val isReverseDateMode: Boolean = false,
        /** Human-readable difference label when two dates are compared, e.g. "1 year 2 months 3 days". */
        val dateDiffLabel: String? = null,
        /** Pre-formatted time result, e.g. "3:45 PM", "in 4 hours 20 minutes", "8 hours 30 minutes". */
        val timeResultLabel: String? = null,
        /** Optional day context shown below the time, e.g. "tomorrow" or "yesterday". Null for today. */
        val timeContextLabel: String? = null,
        /** True when timeResultLabel is an absolute clock time (e.g. "3:45 PM"). */
        val isTimeAbsoluteResult: Boolean = false,
        /** Secondary time result shown alongside the primary (used for absolute time queries: past + future). */
        val timeResultLabel2: String? = null,
) {
        val isCalculatorMode: Boolean
                get() = isToolMode && toolType == SearchToolType.CALCULATOR

        val isUnitConverterMode: Boolean
                get() = isToolMode && toolType == SearchToolType.UNIT_CONVERTER

        val hasResult: Boolean
                get() = result != null || toolData != null || parsedDateMillis != null ||
                        dateDiffLabel != null || timeResultLabel != null

        val hasExclusiveResult: Boolean
                get() = toolType.replacesSearchResults && hasResult

        val isVisible: Boolean
                get() = isToolMode || hasResult
}

data class PhoneNumberSelection(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        // Contact button action to run with the chosen number, already resolved for this contact
        val action: ContactButtonAction,
        val isPrimary: Boolean,
) {
        val isCall: Boolean
                get() = !action.isMessage
}

data class DirectDialChoice(
        val contactName: String,
        val phoneNumber: String,
)

data class PendingThirdPartyCall(
        val app: CallingApp,
        val dataId: Long? = null,
        val phoneNumber: String? = null,
        val isVideoCall: Boolean = false,
)

enum class DirectDialOption {
        DIRECT_CALL,
        DIALER,
}

data class ContactActionPickerRequest(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isPrimary: Boolean,
        val currentAction: ContactCardAction?,
)

// Sealed classes for visibility states
