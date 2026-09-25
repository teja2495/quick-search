package com.tk.quicksearch.search.data.userAppPreferences

import android.content.Context
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.preferences.*
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchHistory.SearchHistoryPreferences
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.shared.util.isPhysicalKeyboardConnected
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.CustomLlmProviderConfig
import com.tk.quicksearch.tools.aiSearch.TavilyWebSearchMode
import com.tk.quicksearch.tools.tasker.TaskerIntentTool

/** User-facing preference facade, grouped across the core and feature preference classes. */
class UserAppPreferences(context: Context) : UserAppPreferencesFeatures(context) {
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areAppSuggestionsEnabled(): Boolean = uiPreferences.areAppSuggestionsEnabled()

    fun setAppSuggestionsEnabled(enabled: Boolean) = uiPreferences.setAppSuggestionsEnabled(enabled)

    fun shouldShowAllAppsButton(): Boolean = uiPreferences.shouldShowAllAppsButton()

    fun setShowAllAppsButton(enabled: Boolean) = uiPreferences.setShowAllAppsButton(enabled)

    fun shouldIncludeNonLaunchableAppsInSearch(): Boolean =
            uiPreferences.shouldIncludeNonLaunchableAppsInSearch()

    fun setIncludeNonLaunchableAppsInSearch(enabled: Boolean) =
            uiPreferences.setIncludeNonLaunchableAppsInSearch(enabled)

    fun shouldShowInRecents(): Boolean = uiPreferences.shouldShowInRecents()

    fun setShowInRecents(enabled: Boolean) = uiPreferences.setShowInRecents(enabled)

    fun areNotificationDotsEnabled(): Boolean = uiPreferences.areNotificationDotsEnabled()

    fun setNotificationDotsEnabled(enabled: Boolean) =
            uiPreferences.setNotificationDotsEnabled(enabled)

    fun getSelectedAppSuggestionTab(): AppSuggestionTabType =
            uiPreferences.getSelectedAppSuggestionTab()

    fun setSelectedAppSuggestionTab(tab: AppSuggestionTabType) =
            uiPreferences.setSelectedAppSuggestionTab(tab)

    fun getEnabledAppSuggestionTabs(): Set<AppSuggestionTabType> =
            uiPreferences.getEnabledAppSuggestionTabs()

    fun setEnabledAppSuggestionTabs(tabs: Set<AppSuggestionTabType>) =
            uiPreferences.setEnabledAppSuggestionTabs(tabs)

    fun shouldShowAppLabels(): Boolean = uiPreferences.shouldShowAppLabels()

    fun setShowAppLabels(show: Boolean) = uiPreferences.setShowAppLabels(show)

    fun getPhoneAppGridColumns(): Int = uiPreferences.getPhoneAppGridColumns()

    fun setPhoneAppGridColumns(columns: Int) = uiPreferences.setPhoneAppGridColumns(columns)

    fun getAppIconSizeStep(): Int = uiPreferences.getAppIconSizeStep()

    fun setAppIconSizeStep(step: Int) = uiPreferences.setAppIconSizeStep(step)

    fun areWebSuggestionsEnabled(): Boolean = uiPreferences.areWebSuggestionsEnabled()

    fun getWebSuggestionsCount(): Int = uiPreferences.getWebSuggestionsCount()

    fun setWebSuggestionsCount(count: Int) {
        uiPreferences.setWebSuggestionsCount(count)
    }

    fun getRecentQueriesDisplayCount(): Int = uiPreferences.getRecentQueriesDisplayCount()

    fun setRecentQueriesDisplayCount(count: Int) {
        uiPreferences.setRecentQueriesDisplayCount(count)
    }

    fun getAppResultRowCount(): Int = uiPreferences.getAppResultRowCount()

    fun setAppResultRowCount(rowCount: Int) {
        uiPreferences.setAppResultRowCount(rowCount)
    }

    fun setWebSuggestionsEnabled(enabled: Boolean) = uiPreferences.setWebSuggestionsEnabled(enabled)

    // ============================================================================
    // Calculator Preferences
    // ============================================================================

    fun isCalculatorEnabled(): Boolean = uiPreferences.isCalculatorEnabled()

    fun setCalculatorEnabled(enabled: Boolean) = uiPreferences.setCalculatorEnabled(enabled)

    fun isUnitConverterEnabled(): Boolean = uiPreferences.isUnitConverterEnabled()

    fun setUnitConverterEnabled(enabled: Boolean) = uiPreferences.setUnitConverterEnabled(enabled)

    fun isDateCalculatorEnabled(): Boolean = uiPreferences.isDateCalculatorEnabled()

    fun setDateCalculatorEnabled(enabled: Boolean) = uiPreferences.setDateCalculatorEnabled(enabled)

    fun isCurrencyConverterEnabled(): Boolean = uiPreferences.isCurrencyConverterEnabled()

    fun setCurrencyConverterEnabled(enabled: Boolean) = uiPreferences.setCurrencyConverterEnabled(enabled)

    fun isColorVisualizerEnabled(): Boolean = uiPreferences.isColorVisualizerEnabled()

    fun setColorVisualizerEnabled(enabled: Boolean) = uiPreferences.setColorVisualizerEnabled(enabled)

    fun isWorldClockEnabled(): Boolean = uiPreferences.isWorldClockEnabled()

    fun setWorldClockEnabled(enabled: Boolean) = uiPreferences.setWorldClockEnabled(enabled)

    fun isDictionaryEnabled(): Boolean = uiPreferences.isDictionaryEnabled()

    fun setDictionaryEnabled(enabled: Boolean) = uiPreferences.setDictionaryEnabled(enabled)

    fun getCurrencyConverterModel(): String =
        uiPreferences.getCurrencyConverterModel().ifBlank {
            getLlmModel(getCurrencyConverterProviderId())
        }
    fun setCurrencyConverterModel(modelId: String) = uiPreferences.setCurrencyConverterModel(modelId)
    fun clearCurrencyConverterModel() = uiPreferences.clearCurrencyConverterModel()
    fun getCurrencyConverterAdvancedPayload(): Pair<Boolean, String> = uiPreferences.getCurrencyConverterAdvancedPayload()
    fun setCurrencyConverterAdvancedPayload(payload: String?, enabled: Boolean) = uiPreferences.setCurrencyConverterAdvancedPayload(payload, enabled)
    fun getCurrencyConverterProviderId(): AiSearchLlmProviderId =
        uiPreferences.getCurrencyConverterProviderId()
    fun setCurrencyConverterProviderId(providerId: AiSearchLlmProviderId) =
        uiPreferences.setCurrencyConverterProviderId(providerId)
    fun isCurrencyConverterGroundingEnabled(): Boolean =
        uiPreferences.isCurrencyConverterGroundingEnabled()
    fun setCurrencyConverterGroundingEnabled(enabled: Boolean) =
        uiPreferences.setCurrencyConverterGroundingEnabled(enabled)
    fun isCurrencyConverterThinkingEnabled(): Boolean =
        uiPreferences.isCurrencyConverterThinkingEnabled()
    fun setCurrencyConverterThinkingEnabled(enabled: Boolean) =
        uiPreferences.setCurrencyConverterThinkingEnabled(enabled)

    fun getWorldClockModel(): String =
        if (uiPreferences.hasWorldClockModelPreference()) {
            uiPreferences.getWorldClockModel()
        } else {
            uiPreferences.getWorldClockModel().ifBlank {
                getLlmModel(getWorldClockProviderId())
            }
        }
    fun setWorldClockModel(modelId: String) = uiPreferences.setWorldClockModel(modelId)
    fun clearWorldClockModel() = uiPreferences.clearWorldClockModel()
    fun getWorldClockAdvancedPayload(): Pair<Boolean, String> = uiPreferences.getWorldClockAdvancedPayload()
    fun setWorldClockAdvancedPayload(payload: String?, enabled: Boolean) = uiPreferences.setWorldClockAdvancedPayload(payload, enabled)
    fun getWorldClockProviderId(): AiSearchLlmProviderId =
        uiPreferences.getWorldClockProviderIdOverride()
            ?: if (uiPreferences.getWorldClockModel().isBlank()) {
                getAiSearchProviderId()
            } else {
                AiSearchLlmProviderId.GEMINI
            }
    fun setWorldClockProviderId(providerId: AiSearchLlmProviderId) =
        uiPreferences.setWorldClockProviderId(providerId)
    fun isWorldClockGroundingEnabled(): Boolean =
        uiPreferences.isWorldClockGroundingEnabled()
    fun setWorldClockGroundingEnabled(enabled: Boolean) =
        uiPreferences.setWorldClockGroundingEnabled(enabled)
    fun isWorldClockThinkingEnabled(): Boolean =
        uiPreferences.getWorldClockThinkingOverride()
            ?: isLlmThinkingEnabled(getWorldClockProviderId())
    fun getWorldClockThinkingOverride(): Boolean? = uiPreferences.getWorldClockThinkingOverride()
    fun setWorldClockThinkingEnabled(enabled: Boolean) =
        uiPreferences.setWorldClockThinkingEnabled(enabled)

    fun getDictionaryModel(): String =
        if (uiPreferences.hasDictionaryModelPreference()) {
            uiPreferences.getDictionaryModel()
        } else {
            uiPreferences.getDictionaryModel().ifBlank {
                getLlmModel(getDictionaryProviderId())
            }
        }
    fun setDictionaryModel(modelId: String) = uiPreferences.setDictionaryModel(modelId)
    fun clearDictionaryModel() = uiPreferences.clearDictionaryModel()
    fun getDictionaryAdvancedPayload(): Pair<Boolean, String> = uiPreferences.getDictionaryAdvancedPayload()
    fun setDictionaryAdvancedPayload(payload: String?, enabled: Boolean) = uiPreferences.setDictionaryAdvancedPayload(payload, enabled)
    fun getDictionaryProviderId(): AiSearchLlmProviderId =
        uiPreferences.getDictionaryProviderIdOverride()
            ?: if (uiPreferences.getDictionaryModel().isBlank()) {
                getAiSearchProviderId()
            } else {
                AiSearchLlmProviderId.GEMINI
            }
    fun setDictionaryProviderId(providerId: AiSearchLlmProviderId) =
        uiPreferences.setDictionaryProviderId(providerId)
    fun isDictionaryGroundingEnabled(): Boolean =
        uiPreferences.isDictionaryGroundingEnabled()
    fun setDictionaryGroundingEnabled(enabled: Boolean) =
        uiPreferences.setDictionaryGroundingEnabled(enabled)
    fun isDictionaryThinkingEnabled(): Boolean =
        uiPreferences.getDictionaryThinkingOverride()
            ?: isLlmThinkingEnabled(getDictionaryProviderId())
    fun getDictionaryThinkingOverride(): Boolean? = uiPreferences.getDictionaryThinkingOverride()
    fun setDictionaryThinkingEnabled(enabled: Boolean) =
        uiPreferences.setDictionaryThinkingEnabled(enabled)

    fun isWeatherEnabled(): Boolean = weatherPreferences.isEnabled()
    fun setWeatherEnabled(enabled: Boolean) = weatherPreferences.setEnabled(enabled)
    fun getWeatherLocation(): String = weatherPreferences.getLocation()
    fun setWeatherLocation(location: String) = weatherPreferences.setLocation(location)
    fun getWeatherSystemPrompt(): String = weatherPreferences.getSystemPrompt()
    fun setWeatherSystemPrompt(prompt: String) = weatherPreferences.setSystemPrompt(prompt)
    fun getWeatherModel(): String =
        if (weatherPreferences.hasModelPreference()) {
            weatherPreferences.getModel()
        } else {
            getLlmModel(getWeatherProviderId())
        }
    fun setWeatherModel(modelId: String) = weatherPreferences.setModel(modelId)
    fun clearWeatherModel() = weatherPreferences.clearModel()
    fun getWeatherProviderId(): AiSearchLlmProviderId =
        weatherPreferences.getProviderIdOverride()
            ?: if (weatherPreferences.getModel().isBlank()) {
                getAiSearchProviderId()
            } else {
                AiSearchLlmProviderId.GEMINI
            }
    fun setWeatherProviderId(providerId: AiSearchLlmProviderId) =
        weatherPreferences.setProviderId(providerId)
    fun isWeatherGroundingEnabled(): Boolean = true
    fun setWeatherGroundingEnabled(enabled: Boolean) =
        weatherPreferences.setGroundingEnabled(true)
    fun isWeatherThinkingEnabled(): Boolean =
        weatherPreferences.getThinkingOverride() ?: isLlmThinkingEnabled(getWeatherProviderId())
    fun getWeatherThinkingOverride(): Boolean? = weatherPreferences.getThinkingOverride()
    fun setWeatherThinkingEnabled(enabled: Boolean) = weatherPreferences.setThinkingEnabled(enabled)
    fun getWeatherTemperatureUnit(): com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit =
        weatherPreferences.getTemperatureUnit()
    fun setWeatherTemperatureUnit(
        unit: com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit,
    ) = weatherPreferences.setTemperatureUnit(unit)
    fun getWeatherWindSpeedUnit(): com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit =
        weatherPreferences.getWindSpeedUnit()
    fun setWeatherWindSpeedUnit(
        unit: com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit,
    ) = weatherPreferences.setWindSpeedUnit(unit)
    fun getWeatherAdvancedPayload(): Pair<Boolean, String> = weatherPreferences.getAdvancedPayload()
    fun setWeatherAdvancedPayload(payload: String?, enabled: Boolean) =
        weatherPreferences.setAdvancedPayload(payload, enabled)

    // ============================================================================
    // Recent Queries Preferences
    // ============================================================================

    fun getRecentItems(): List<com.tk.quicksearch.search.searchHistory.RecentSearchEntry> =
            recentSearchesPreferences.getRecentItems()

    fun addRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) {
        if (entry is com.tk.quicksearch.search.searchHistory.RecentSearchEntry.Query) {
            recentSearchesPreferences.addRecentItem(entry)
        } else {
            recentResultOpensPreferences.addRecentResultOpen(entry)
        }
    }

    fun getRecentResultOpens(): List<com.tk.quicksearch.search.searchHistory.RecentSearchEntry> =
            recentResultOpensPreferences.getRecentResultOpens()

    fun getRecentResultOpenCounts(): Map<String, Int> =
        recentResultOpensPreferences.getRecentResultOpenCounts()

    fun getRecentResultLastOpenedTimes(): Map<String, Long> =
        recentResultOpensPreferences.getRecentResultLastOpenedTimes()

    fun recordCalendarEventOpen(eventId: Long) =
        recentResultOpensPreferences.recordCalendarEventOpen(eventId)

    fun clearRecentQueries() {
        recentSearchesPreferences.clearRecentQueries()
        recentResultOpensPreferences.clearRecentResultOpens()
    }

    fun deleteRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) {
        recentSearchesPreferences.deleteRecentItem(entry)
        recentResultOpensPreferences.deleteRecentResultOpen(entry)
    }

    fun areRecentQueriesEnabled(): Boolean = recentSearchesPreferences.areRecentQueriesEnabled()

    fun setRecentQueriesEnabled(enabled: Boolean) =
            recentSearchesPreferences.setRecentQueriesEnabled(enabled)

    fun isFuzzySearchEnabled(): Boolean = uiPreferences.isFuzzySearchEnabled()

    fun setFuzzySearchEnabled(enabled: Boolean) = uiPreferences.setFuzzySearchEnabled(enabled)

    fun getSecondaryRankingSignal(): com.tk.quicksearch.search.models.SecondaryRankingSignal =
        uiPreferences.getSecondaryRankingSignal()

    fun setSecondaryRankingSignal(signal: com.tk.quicksearch.search.models.SecondaryRankingSignal) =
        uiPreferences.setSecondaryRankingSignal(signal)

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getDisabledSections(): Set<String> = uiPreferences.getDisabledSections()

    fun setDisabledSections(disabled: Set<String>) = uiPreferences.setDisabledSections(disabled)

    // ============================================================================
    // Rate Quick Search Prompt Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long = uiPreferences.getFirstAppOpenTime()

    fun recordFirstAppOpenTime() = uiPreferences.recordFirstAppOpenTime()

    fun getAppOpenCount(): Int = uiPreferences.getAppOpenCount()

    fun incrementAppOpenCount() = uiPreferences.incrementAppOpenCount()

    fun hasCompletedRateQuickSearch(): Boolean = uiPreferences.hasCompletedRateQuickSearch()

    fun markRateQuickSearchCompleted() = uiPreferences.markRateQuickSearchCompleted()

    fun getRateQuickSearchLastDismissedAt(): Long = uiPreferences.getRateQuickSearchLastDismissedAt()

    fun recordRateQuickSearchDismissed() = uiPreferences.recordRateQuickSearchDismissed()

    fun shouldShowRateQuickSearchCard(): Boolean = uiPreferences.shouldShowRateQuickSearchCard()

    fun shouldShowUpdateCard(): Boolean = uiPreferences.shouldShowUpdateCard()

    fun recordUpdateCardDismissed() = uiPreferences.recordUpdateCardDismissed()

    // ============================================================================
    // In-App Update Session Tracking
    // ============================================================================

    fun hasShownUpdateCheckThisSession(): Boolean = uiPreferences.hasShownUpdateCheckThisSession()

    fun setUpdateCheckShownThisSession() = uiPreferences.setUpdateCheckShownThisSession()

    fun resetUpdateCheckSession() = uiPreferences.resetUpdateCheckSession()
}
