package com.tk.quicksearch.search.core

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
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.GeminiModelCatalog
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tools.tasker.TaskerIntentTool

// IconPackInfo moved here to avoid circular imports
data class IconPackInfo(
        val packageName: String,
        val label: String,
)

enum class SearchEngine {
        DIRECT_SEARCH,
        GOOGLE,
        CHATGPT,
        GEMINI,
        PERPLEXITY,
        GOOGLE_PLAY,
        FDROID,
        YOUTUBE,
        GOOGLE_MAPS,
        WAZE,
        GROK,
        REDDIT,
        AMAZON,
        X,
        YOUTUBE_MUSIC,
        SPOTIFY,
        CLAUDE,
        GOOGLE_DRIVE,
        GOOGLE_PHOTOS,
        AI_MODE,
        DUCKDUCKGO,
        BRAVE,
        FACEBOOK_MARKETPLACE,
        YOU_COM,
        WIKIPEDIA,
        BING,
        STARTPAGE,
        GOOGLE_TRANSLATE,
        KAGI,
        KAGI_ASSISTANT,
        MUSE,
}

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
        APP_SETTINGS,
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

enum class AppTheme {
        FOREST,
        AURORA,
        SUNSET,
        MONOCHROME,
}

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

enum class SearchToolType {
        CALCULATOR,
        UNIT_CONVERTER,
        DATE_CALCULATOR,
        COLOR_VISUALIZER,
}

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
        val partOfSpeech: String? = null,
        val meaning: String? = null,
        val example: String? = null,
        val synonyms: List<String> = emptyList(),
        val activeQuery: String? = null,
        val usedModelId: String? = null,
        val llmProviderId: AiSearchLlmProviderId? = null,
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
        val expression: String? = null,
        val isCalculatorMode: Boolean = false,
        val isUnitConverterMode: Boolean = false,
        val isDateCalculatorMode: Boolean = false,
        val isColorVisualizerMode: Boolean = false,
        /** Parsed opaque ARGB color for the Color Visualizer tool. */
        val colorArgb: Int? = null,
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
        val timeContextLabel2: String? = null,
) {
        val isToolMode: Boolean
                get() = isCalculatorMode || isUnitConverterMode || isDateCalculatorMode || isColorVisualizerMode
}

data class PhoneNumberSelection(
        val contactInfo: com.tk.quicksearch.search.models.ContactInfo,
        val isCall: Boolean, // true for call, false for SMS
)

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
