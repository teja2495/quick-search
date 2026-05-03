package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.data.preferences.*
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchHistory.SearchHistoryPreferences
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps. Manages preferences
 * for apps, contacts, files, search engines, shortcuts, and UI settings. This class now delegates
 * to specialized preference classes for better organization.
 */
class UserAppPreferences(
        private val context: Context,
) {
    private val sharedPrefs by lazy {
        context.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
        )
    }

    // Feature-specific preference managers - lazy to avoid blocking construction
    private val appPreferences by lazy { AppPreferences(context) }
    private val contactPreferences by lazy { ContactPreferences(context) }
    private val filePreferences by lazy { FilePreferences(context) }
    private val settingsPreferences by lazy { SettingsPreferences(context) }
    private val calendarPreferences by lazy { CalendarPreferences(context) }
    private val notesPreferences by lazy { NotesPreferences(context) }
    private val appShortcutPreferences by lazy { AppShortcutPreferences(context) }
    private val nicknamePreferences by lazy { NicknamePreferences(context) }
    private val triggerPreferences by lazy { TriggerPreferences(context) }
    private val searchEnginePreferences by lazy { SearchEnginePreferences(context) }
    private val aliasPreferences by lazy { AliasPreferences(context) }
    private val geminiPreferences by lazy { GeminiPreferences(context) }
    private val openAiPreferences by lazy { OpenAiPreferences(context) }
    private val anthropicPreferences by lazy { AnthropicPreferences(context) }
    private val groqPreferences by lazy { GroqPreferences(context) }
    private val llmPreferences by lazy { LlmPreferences(context) }
    val uiPreferences by lazy { UiPreferences(context) }
    private val amazonPreferences by lazy { AmazonPreferences(context) }
    private val recentSearchesPreferences by lazy { SearchHistoryPreferences(context) }
    private val recentResultOpensPreferences by lazy { RecentResultOpensPreferences(context) }
    private val startupPreferences by lazy { StartupPreferencesFacade(this, context) }


    /**
     * Optimized: Loads all preferences needed during startup in a single batch operation. Uses
     * SharedPreferences.getAll() to minimize disk I/O operations.
     */
    fun getStartupPreferences(): StartupPreferencesFacade.StartupPreferences = startupPreferences.getStartupPreferences()

    /**
     * Loads all startup configuration in a single atomic operation for maximum performance. This
     * consolidates critical preferences, cached apps data, and startup preferences into one batch
     * read operation, minimizing disk I/O during app launch.
     */
    fun loadStartupConfig(): StartupPreferencesFacade.StartupConfig = startupPreferences.loadStartupConfig()

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = appPreferences.getSuggestionHiddenPackages()

    fun getResultHiddenPackages(): Set<String> = appPreferences.getResultHiddenPackages()

    fun getPinnedPackages(): Set<String> = appPreferences.getPinnedPackages()

    fun hidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.hidePackageInSuggestions(packageName)

    fun hidePackageInResults(packageName: String): Set<String> =
            appPreferences.hidePackageInResults(packageName)

    fun unhidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.unhidePackageInSuggestions(packageName)

    fun unhidePackageInResults(packageName: String): Set<String> =
            appPreferences.unhidePackageInResults(packageName)

    fun pinPackage(packageName: String): Set<String> = appPreferences.pinPackage(packageName)

    fun unpinPackage(packageName: String): Set<String> = appPreferences.unpinPackage(packageName)

    fun clearAllHiddenAppsInSuggestions(): Set<String> =
            appPreferences.clearAllHiddenAppsInSuggestions()

    fun clearAllHiddenAppsInResults(): Set<String> = appPreferences.clearAllHiddenAppsInResults()

    fun getAppLaunchCount(packageName: String): Int = appPreferences.getAppLaunchCount(packageName)

    fun getAppLaunchCount(packageName: String, userHandleId: Int?): Int =
        appPreferences.getAppLaunchCount(packageName, userHandleId)

    fun incrementAppLaunchCount(packageName: String) =
        appPreferences.incrementAppLaunchCount(packageName)

    fun incrementAppLaunchCount(packageName: String, userHandleId: Int?) =
        appPreferences.incrementAppLaunchCount(packageName, userHandleId)

    fun getAllAppLaunchCounts(): Map<String, Int> = appPreferences.getAllAppLaunchCounts()

    fun getRecentAppLaunches(): List<String> = appPreferences.getRecentAppLaunches()

    fun setRecentAppLaunches(packageNames: List<String>): List<String> =
            appPreferences.setRecentAppLaunches(packageNames)

    fun addRecentAppLaunch(packageName: String): List<String> =
            appPreferences.addRecentAppLaunch(packageName)

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = contactPreferences.getPinnedContactIds()

    fun getExcludedContactIds(): Set<Long> = contactPreferences.getExcludedContactIds()

    fun pinContact(contactId: Long): Set<Long> = contactPreferences.pinContact(contactId)

    fun unpinContact(contactId: Long): Set<Long> = contactPreferences.unpinContact(contactId)

    fun excludeContact(contactId: Long): Set<Long> = contactPreferences.excludeContact(contactId)

    fun removeExcludedContact(contactId: Long): Set<Long> =
            contactPreferences.removeExcludedContact(contactId)

    fun clearAllExcludedContacts(): Set<Long> = contactPreferences.clearAllExcludedContacts()

    fun getPreferredPhoneNumber(contactId: Long): String? =
            contactPreferences.getPreferredPhoneNumber(contactId)

    fun setPreferredPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setPreferredPhoneNumber(contactId, phoneNumber)

    fun getLastShownPhoneNumber(contactId: Long): String? =
            contactPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setLastShownPhoneNumber(contactId, phoneNumber)

    fun isDirectDialEnabled(): Boolean = contactPreferences.isDirectDialEnabled()

    fun setDirectDialEnabled(enabled: Boolean) = contactPreferences.setDirectDialEnabled(enabled)

    fun hasSeenDirectDialChoice(): Boolean = contactPreferences.hasSeenDirectDialChoice()

    fun setHasSeenDirectDialChoice(seen: Boolean) =
            contactPreferences.setHasSeenDirectDialChoice(seen)

    fun isDirectDialManuallyDisabled(): Boolean =
            sharedPrefs.getBoolean(
                    com.tk.quicksearch.search.data.preferences.BasePreferences
                            .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                    false,
            )

    fun setDirectDialManuallyDisabled(disabled: Boolean) {
        sharedPrefs
                .edit()
                .putBoolean(
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                        disabled,
                ).apply()
    }

    // ============================================================================
    // File Preferences
    // ============================================================================

    fun getPinnedFileUris(): Set<String> = filePreferences.getPinnedFileUris()

    fun getExcludedFileUris(): Set<String> = filePreferences.getExcludedFileUris()

    fun pinFile(uri: String): Set<String> = filePreferences.pinFile(uri)

    fun unpinFile(uri: String): Set<String> = filePreferences.unpinFile(uri)

    fun excludeFile(uri: String): Set<String> = filePreferences.excludeFile(uri)

    fun removeExcludedFile(uri: String): Set<String> = filePreferences.removeExcludedFile(uri)

    fun clearAllExcludedFiles(): Set<String> = filePreferences.clearAllExcludedFiles()

    fun getExcludedFileExtensions(): Set<String> = filePreferences.getExcludedFileExtensions()

    fun addExcludedFileExtension(extension: String): Set<String> =
            filePreferences.addExcludedFileExtension(extension)

    fun removeExcludedFileExtension(extension: String): Set<String> =
            filePreferences.removeExcludedFileExtension(extension)

    fun clearAllExcludedFileExtensions(): Set<String> =
            filePreferences.clearAllExcludedFileExtensions()

    fun getEnabledFileTypes(): Set<com.tk.quicksearch.search.models.FileType> =
            filePreferences.getEnabledFileTypes()

    fun setEnabledFileTypes(enabled: Set<com.tk.quicksearch.search.models.FileType>) =
            filePreferences.setEnabledFileTypes(enabled)

    fun clearEnabledFileTypes(): Set<com.tk.quicksearch.search.models.FileType> =
            filePreferences.clearEnabledFileTypes()

    fun getShowFoldersInResults(): Boolean = filePreferences.getShowFoldersInResults()

    fun setShowFoldersInResults(show: Boolean) = filePreferences.setShowFoldersInResults(show)

    fun getShowSystemFiles(): Boolean = filePreferences.getShowSystemFiles()

    fun setShowSystemFiles(show: Boolean) = filePreferences.setShowSystemFiles(show)

    fun getFolderWhitelistPatterns(): Set<String> = filePreferences.getFolderWhitelistPatterns()

    fun setFolderWhitelistPatterns(patterns: Set<String>) =
            filePreferences.setFolderWhitelistPatterns(patterns)

    fun getFolderBlacklistPatterns(): Set<String> = filePreferences.getFolderBlacklistPatterns()

    fun setFolderBlacklistPatterns(patterns: Set<String>) =
            filePreferences.setFolderBlacklistPatterns(patterns)

    // ============================================================================
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = settingsPreferences.getPinnedSettingIds()

    fun getExcludedSettingIds(): Set<String> = settingsPreferences.getExcludedSettingIds()

    fun pinSetting(id: String): Set<String> = settingsPreferences.pinSetting(id)

    fun unpinSetting(id: String): Set<String> = settingsPreferences.unpinSetting(id)

    fun excludeSetting(id: String): Set<String> = settingsPreferences.excludeSetting(id)

    fun removeExcludedSetting(id: String): Set<String> =
            settingsPreferences.removeExcludedSetting(id)

    fun clearAllExcludedSettings(): Set<String> = settingsPreferences.clearAllExcludedSettings()

    fun isAssistantLaunchVoiceModeEnabled(): Boolean =
            settingsPreferences.isAssistantLaunchVoiceModeEnabled()

    fun setAssistantLaunchVoiceModeEnabled(enabled: Boolean) =
            settingsPreferences.setAssistantLaunchVoiceModeEnabled(enabled)

    // ============================================================================
    // Calendar Preferences
    // ============================================================================

    fun getPinnedCalendarEventIds(): Set<Long> = calendarPreferences.getPinnedEventIds()

    fun getExcludedCalendarEventIds(): Set<Long> = calendarPreferences.getExcludedEventIds()

    fun pinCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.pinEvent(eventId)

    fun unpinCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.unpinEvent(eventId)

    fun excludeCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.excludeEvent(eventId)

    fun removeExcludedCalendarEvent(eventId: Long): Set<Long> =
            calendarPreferences.removeExcludedEvent(eventId)

    fun clearAllExcludedCalendarEvents(): Set<Long> = calendarPreferences.clearAllExcludedEvents()

    fun getShowTodayEvents(): Boolean = calendarPreferences.getShowTodayEvents()

    fun setShowTodayEvents(show: Boolean) = calendarPreferences.setShowTodayEvents(show)

    fun getArchivedTodayEventIds(): Set<Long> = calendarPreferences.getArchivedTodayEventIds()

    fun archiveTodayCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.archiveTodayEvent(eventId)

    // ============================================================================
    // Notes Preferences
    // ============================================================================

    fun getPinnedNoteIds(): Set<Long> = notesPreferences.getPinnedNoteIds()

    fun pinNote(noteId: Long): Set<Long> = notesPreferences.pinNote(noteId)

    fun unpinNote(noteId: Long): Set<Long> = notesPreferences.unpinNote(noteId)

    fun getNotesJson(): String = notesPreferences.getNotesJson()

    fun setNotesJson(json: String) = notesPreferences.setNotesJson(json)

    fun nextNoteId(): Long = notesPreferences.nextNoteId()

    fun ensureNoteIdCounterAtLeast(nextCandidate: Long) =
            notesPreferences.ensureNoteIdCounterAtLeast(nextCandidate)

    // ============================================================================
    // App Shortcut Preferences
    // ============================================================================

    fun getPinnedAppShortcutIds(): Set<String> = appShortcutPreferences.getPinnedAppShortcutIds()

    fun getExcludedAppShortcutIds(): Set<String> =
            appShortcutPreferences.getExcludedAppShortcutIds()

    fun getDisabledAppShortcutIds(): Set<String> =
            appShortcutPreferences.getDisabledAppShortcutIds()

    fun pinAppShortcut(id: String): Set<String> = appShortcutPreferences.pinAppShortcut(id)

    fun unpinAppShortcut(id: String): Set<String> = appShortcutPreferences.unpinAppShortcut(id)

    fun excludeAppShortcut(id: String): Set<String> = appShortcutPreferences.excludeAppShortcut(id)

    fun removeExcludedAppShortcut(id: String): Set<String> =
            appShortcutPreferences.removeExcludedAppShortcut(id)

    fun clearAllExcludedAppShortcuts(): Set<String> =
            appShortcutPreferences.clearAllExcludedAppShortcuts()

    fun setAppShortcutEnabled(
            id: String,
            enabled: Boolean,
    ): Set<String> = appShortcutPreferences.setAppShortcutEnabled(id, enabled)

    fun getAppShortcutIconOverride(id: String): String? =
            appShortcutPreferences.getAppShortcutIconOverride(id)

    fun getAllAppShortcutIconOverrides(): Map<String, String> =
            appShortcutPreferences.getAllAppShortcutIconOverrides()

    fun setAppShortcutIconOverride(
            id: String,
            iconBase64: String?,
    ) = appShortcutPreferences.setAppShortcutIconOverride(id, iconBase64)

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    fun reloadNicknameCaches() = nicknamePreferences.reloadCaches()

    fun getAllAppNicknames(): Map<String, String> = nicknamePreferences.getAllAppNicknames()

    fun getAppNickname(packageName: String): String? =
            nicknamePreferences.getAppNickname(packageName)

    fun setAppNickname(
            packageName: String,
            nickname: String?,
    ) = nicknamePreferences.setAppNickname(packageName, nickname)

    fun getAllAppShortcutNicknames(): Map<String, String> =
            nicknamePreferences.getAllAppShortcutNicknames()

    fun getAppShortcutNickname(shortcutId: String): String? =
            nicknamePreferences.getAppShortcutNickname(shortcutId)

    fun setAppShortcutNickname(
            shortcutId: String,
            nickname: String?,
    ) = nicknamePreferences.setAppShortcutNickname(shortcutId, nickname)

    fun getContactNickname(contactId: Long): String? =
            nicknamePreferences.getContactNickname(contactId)

    fun setContactNickname(
            contactId: Long,
            nickname: String?,
    ) = nicknamePreferences.setContactNickname(contactId, nickname)

    fun getFileNickname(uri: String): String? = nicknamePreferences.getFileNickname(uri)

    fun setFileNickname(
            uri: String,
            nickname: String?,
    ) = nicknamePreferences.setFileNickname(uri, nickname)

    fun getSettingNickname(id: String): String? = nicknamePreferences.getSettingNickname(id)

    fun setSettingNickname(
            id: String,
            nickname: String?,
    ) = nicknamePreferences.setSettingNickname(id, nickname)

    /** Finds contact IDs that have nicknames matching the query. */
    fun findContactsWithMatchingNickname(query: String): Set<Long> =
            nicknamePreferences.findContactsWithMatchingNickname(query) +
                    triggerPreferences.findContactsWithMatchingTrigger(query)

    /** Finds file URIs that have nicknames matching the query. */
    fun findFilesWithMatchingNickname(query: String): Set<String> =
            nicknamePreferences.findFilesWithMatchingNickname(query) +
                    triggerPreferences.findFilesWithMatchingTrigger(query)

    /** Finds settings that have nicknames matching the query. */
    fun findSettingsWithMatchingNickname(query: String): Set<String> =
            nicknamePreferences.findSettingsWithMatchingNickname(query) +
                    triggerPreferences.findSettingsWithMatchingTrigger(query)

    /** Finds calendar event IDs that have nicknames matching the query. */
    fun findCalendarEventsWithMatchingNickname(query: String): Set<Long> =
            nicknamePreferences.findCalendarEventsWithMatchingNickname(query)

    fun getCalendarEventNickname(eventId: Long): String? =
            nicknamePreferences.getCalendarEventNickname(eventId)

    fun setCalendarEventNickname(
            eventId: Long,
            nickname: String?,
    ) = nicknamePreferences.setCalendarEventNickname(eventId, nickname)

    fun hasAnyNicknameItems(): Boolean =
            getAllAppNicknames().isNotEmpty() ||
                    getAllAppShortcutNicknames().isNotEmpty() ||
                    nicknamePreferences.getAllContactNicknames().isNotEmpty() ||
                    nicknamePreferences.getAllFileNicknames().isNotEmpty() ||
                    nicknamePreferences.getAllSettingNicknames().isNotEmpty() ||
                    nicknamePreferences.getAllCalendarEventNicknames().isNotEmpty()

    // ============================================================================
    // Trigger Preferences
    // ============================================================================

    fun getAppTrigger(packageName: String): ResultTrigger? = triggerPreferences.getAppTrigger(packageName)

    fun setAppTrigger(packageName: String, trigger: ResultTrigger?) =
            triggerPreferences.setAppTrigger(packageName, trigger)

    fun getAppShortcutTrigger(shortcutId: String): ResultTrigger? =
            triggerPreferences.getAppShortcutTrigger(shortcutId)

    fun setAppShortcutTrigger(shortcutId: String, trigger: ResultTrigger?) =
            triggerPreferences.setAppShortcutTrigger(shortcutId, trigger)

    fun getContactTrigger(contactId: Long): ResultTrigger? =
            triggerPreferences.getContactTrigger(contactId)

    fun setContactTrigger(contactId: Long, trigger: ResultTrigger?) =
            triggerPreferences.setContactTrigger(contactId, trigger)

    fun getFileTrigger(uri: String): ResultTrigger? = triggerPreferences.getFileTrigger(uri)

    fun setFileTrigger(uri: String, trigger: ResultTrigger?) =
            triggerPreferences.setFileTrigger(uri, trigger)

    fun getSettingTrigger(id: String): ResultTrigger? = triggerPreferences.getSettingTrigger(id)

    fun setSettingTrigger(id: String, trigger: ResultTrigger?) =
            triggerPreferences.setSettingTrigger(id, trigger)

    fun getNoteTrigger(noteId: Long): ResultTrigger? = triggerPreferences.getNoteTrigger(noteId)

    fun setNoteTrigger(noteId: Long, trigger: ResultTrigger?) =
            triggerPreferences.setNoteTrigger(noteId, trigger)

    fun getAllTriggerWordsById(): Map<String, String> =
            buildMap {
                triggerPreferences.getAllAppTriggers().forEach { (id, trigger) ->
                    put("app:$id", trigger.word)
                }
                triggerPreferences.getAllAppShortcutTriggers().forEach { (id, trigger) ->
                    put("shortcut:$id", trigger.word)
                }
                triggerPreferences.getAllContactTriggers().forEach { (id, trigger) ->
                    put("contact:$id", trigger.word)
                }
                triggerPreferences.getAllFileTriggers().forEach { (id, trigger) ->
                    put("file:$id", trigger.word)
                }
                triggerPreferences.getAllSettingTriggers().forEach { (id, trigger) ->
                    put("setting:$id", trigger.word)
                }
                triggerPreferences.getAllNoteTriggers().forEach { (id, trigger) ->
                    put("note:$id", trigger.word)
                }
            }

    fun hasAnyTriggerItems(): Boolean = getAllTriggerWordsById().isNotEmpty()

    fun findNotesWithMatchingTrigger(query: String): Set<Long> =
            triggerPreferences.findNotesWithMatchingTrigger(query)

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean =
            searchEnginePreferences.hasDisabledSearchEnginesPreference()

    fun getDisabledSearchEngines(): Set<String> = searchEnginePreferences.getDisabledSearchEngines()

    fun setDisabledSearchEngines(disabled: Set<String>) =
            searchEnginePreferences.setDisabledSearchEngines(disabled)

    fun getSearchEngineOrder(): List<String> = searchEnginePreferences.getSearchEngineOrder()

    fun setSearchEngineOrder(order: List<String>) =
            searchEnginePreferences.setSearchEngineOrder(order)

    fun isSearchEngineCompactMode(): Boolean = searchEnginePreferences.isSearchEngineCompactMode()

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineCompactMode(enabled)

    fun getSearchEngineCompactRowCount(): Int = searchEnginePreferences.getSearchEngineCompactRowCount()

    fun setSearchEngineCompactRowCount(rowCount: Int) =
            searchEnginePreferences.setSearchEngineCompactRowCount(rowCount)

    fun isSearchEngineAliasSuffixEnabled(): Boolean =
            searchEnginePreferences.isSearchEngineAliasSuffixEnabled()

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineAliasSuffixEnabled(enabled)

    fun isAliasTriggerAfterSpaceEnabled(): Boolean =
            searchEnginePreferences.isAliasTriggerAfterSpaceEnabled()

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) =
            searchEnginePreferences.setAliasTriggerAfterSpaceEnabled(enabled)

    fun hasSeenSearchEngineOnboarding(): Boolean =
            searchEnginePreferences.hasSeenSearchEngineOnboarding()

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) =
            searchEnginePreferences.setHasSeenSearchEngineOnboarding(seen)

    fun getCustomSearchEngines(): List<CustomSearchEngine> =
            searchEnginePreferences.getCustomSearchEngines()

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) =
            searchEnginePreferences.setCustomSearchEngines(engines)

    fun getCustomTools(): List<CustomTool> = searchEnginePreferences.getCustomTools()

    fun setCustomTools(tools: List<CustomTool>) = searchEnginePreferences.setCustomTools(tools)

    fun getDisabledCustomTools(): Set<String> = searchEnginePreferences.getDisabledCustomTools()

    fun setDisabledCustomTools(disabled: Set<String>) =
            searchEnginePreferences.setDisabledCustomTools(disabled)

    // ============================================================================
    // Alias Preferences
    // ============================================================================

    fun areAliasesEnabled(): Boolean = aliasPreferences.areAliasesEnabled()

    fun setAliasesEnabled(enabled: Boolean) = aliasPreferences.setAliasesEnabled(enabled)

    fun getAliasCode(engine: SearchEngine): String = aliasPreferences.getAliasCode(engine)

    fun setAliasCode(
            engine: SearchEngine,
            code: String,
    ) = aliasPreferences.setAliasCode(engine, code)

    fun getAliasCode(targetId: String): String? = aliasPreferences.getAliasCode(targetId)

    fun getAliasCodeAllowSingleChar(targetId: String): String? =
            aliasPreferences.getAliasCodeAllowSingleChar(targetId)

    fun setAliasCode(
            targetId: String,
            code: String,
    ) = aliasPreferences.setAliasCode(targetId, code)

    fun clearAliasCode(targetId: String) = aliasPreferences.clearAliasCode(targetId)

    fun setAliasCodeAllowSingleChar(
            targetId: String,
            code: String,
    ) = aliasPreferences.setAliasCodeAllowSingleChar(targetId, code)

    fun isAliasEnabled(engine: SearchEngine): Boolean =
            aliasPreferences.isAliasEnabled(engine)

    fun setAliasEnabled(
            engine: SearchEngine,
            enabled: Boolean,
    ) = aliasPreferences.setAliasEnabled(engine, enabled)

    fun isAliasEnabled(
            targetId: String,
            defaultValue: Boolean,
    ): Boolean = aliasPreferences.isAliasEnabled(targetId, defaultValue)

    fun setAliasEnabled(
            targetId: String,
            enabled: Boolean,
    ) = aliasPreferences.setAliasEnabled(targetId, enabled)

    fun getAllAliasCodes(): Map<SearchEngine, String> = aliasPreferences.getAllAliasCodes()

    fun areShortcutsEnabled(): Boolean = areAliasesEnabled()

    fun setShortcutsEnabled(enabled: Boolean) = setAliasesEnabled(enabled)

    fun getShortcutCode(engine: SearchEngine): String = getAliasCode(engine)

    fun setShortcutCode(
            engine: SearchEngine,
            code: String,
    ) = setAliasCode(engine, code)

    fun getShortcutCode(targetId: String): String? = getAliasCode(targetId)

    fun setShortcutCode(
            targetId: String,
            code: String,
    ) = setAliasCode(targetId, code)

    fun isShortcutEnabled(engine: SearchEngine): Boolean = isAliasEnabled(engine)

    fun setShortcutEnabled(
            engine: SearchEngine,
            enabled: Boolean,
    ) = setAliasEnabled(engine, enabled)

    fun getAllShortcutCodes(): Map<SearchEngine, String> = getAllAliasCodes()

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? = amazonPreferences.getAmazonDomain()

    fun setAmazonDomain(domain: String?) = amazonPreferences.setAmazonDomain(domain)

    // ============================================================================
    // Gemini API Preferences
    // ============================================================================

    fun getAiSearchProviderId(): AiSearchLlmProviderId =
            llmPreferences.getAiSearchProviderId()

    fun setAiSearchProviderId(providerId: AiSearchLlmProviderId) =
            llmPreferences.setAiSearchProviderId(providerId)

    /**
     * Generic LLM API key accessor used by provider-aware callers.
     * New providers should extend this when provider-specific credential keys are added.
     */
    fun getLlmApiKey(providerId: AiSearchLlmProviderId): String? =
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getGeminiApiKey()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getApiKey()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getApiKey()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getApiKey()
            }

    fun setLlmApiKey(providerId: AiSearchLlmProviderId, key: String?) {
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiApiKey(key)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setApiKey(key)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setApiKey(key)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setApiKey(key)
        }
    }

    /** Clear the API key for every provider (used when resetting). */
    fun clearAllLlmApiKeys() {
        geminiPreferences.setGeminiApiKey(null)
        openAiPreferences.setApiKey(null)
        anthropicPreferences.setApiKey(null)
        groqPreferences.setApiKey(null)
    }

    fun getLlmModel(providerId: AiSearchLlmProviderId): String =
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getGeminiModel()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getModel()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getModel()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getModel()
            }

    fun setLlmModel(providerId: AiSearchLlmProviderId, modelId: String?) {
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiModel(modelId)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setModel(modelId)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setModel(modelId)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setModel(modelId)
        }
    }

    fun isLlmGroundingEnabled(providerId: AiSearchLlmProviderId): Boolean =
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.isGeminiGroundingEnabled()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.isGroundingEnabled()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.isGroundingEnabled()
                AiSearchLlmProviderId.GROQ -> groqPreferences.isGroundingEnabled()
            }

    fun setLlmGroundingEnabled(providerId: AiSearchLlmProviderId, enabled: Boolean) {
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiGroundingEnabled(enabled)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setGroundingEnabled(enabled)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setGroundingEnabled(enabled)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setGroundingEnabled(enabled)
        }
    }

    fun isLlmThinkingEnabled(providerId: AiSearchLlmProviderId): Boolean =
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.isThinkingEnabled()
                AiSearchLlmProviderId.OPENAI -> false
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.isThinkingEnabled()
                AiSearchLlmProviderId.GROQ -> groqPreferences.isThinkingEnabled()
            }

    fun setLlmThinkingEnabled(providerId: AiSearchLlmProviderId, enabled: Boolean) {
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setThinkingEnabled(enabled)
            AiSearchLlmProviderId.OPENAI -> Unit
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setThinkingEnabled(enabled)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setThinkingEnabled(enabled)
        }
    }

    fun getLlmPersonalContext(providerId: AiSearchLlmProviderId): String? =
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getPersonalContext()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getPersonalContext()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getPersonalContext()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getPersonalContext()
            }

    fun setLlmPersonalContext(providerId: AiSearchLlmProviderId, context: String?) {
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setPersonalContext(context)
        }
    }

    /** Returns true if any supported LLM provider has a stored API key. */
    fun hasAnyLlmApiKey(): Boolean =
        !geminiPreferences.getGeminiApiKey().isNullOrBlank() ||
            !openAiPreferences.getApiKey().isNullOrBlank() ||
            !anthropicPreferences.getApiKey().isNullOrBlank() ||
            !groqPreferences.getApiKey().isNullOrBlank()

    // Backward-compatible Gemini facade methods kept for existing call sites.
    fun getGeminiApiKey(): String? = geminiPreferences.getGeminiApiKey()

    fun setGeminiApiKey(key: String?) = geminiPreferences.setGeminiApiKey(key)

    fun getPersonalContext(): String? = geminiPreferences.getPersonalContext()

    fun setPersonalContext(context: String?) = geminiPreferences.setPersonalContext(context)

    fun getGeminiModel(): String = geminiPreferences.getGeminiModel()

    fun setGeminiModel(modelId: String?) = geminiPreferences.setGeminiModel(modelId)

    fun isGeminiGroundingEnabled(): Boolean = geminiPreferences.isGeminiGroundingEnabled()

    fun setGeminiGroundingEnabled(enabled: Boolean) =
            geminiPreferences.setGeminiGroundingEnabled(enabled)

    fun isGeminiThinkingEnabled(): Boolean = geminiPreferences.isThinkingEnabled()

    fun setGeminiThinkingEnabled(enabled: Boolean) =
            geminiPreferences.setThinkingEnabled(enabled)

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isOneHandedMode(): Boolean = uiPreferences.isOneHandedMode()

    fun setOneHandedMode(enabled: Boolean) = uiPreferences.setOneHandedMode(enabled)

    fun isBottomSearchBarEnabled(): Boolean = uiPreferences.isBottomSearchBarEnabled()

    fun setBottomSearchBarEnabled(enabled: Boolean) =
            uiPreferences.setBottomSearchBarEnabled(enabled)

    fun isOpenKeyboardOnLaunchEnabled(): Boolean = uiPreferences.isOpenKeyboardOnLaunchEnabled()

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) =
            uiPreferences.setOpenKeyboardOnLaunchEnabled(enabled)

    fun isTopResultIndicatorEnabled(): Boolean = uiPreferences.isTopResultIndicatorEnabled()

    fun setTopResultIndicatorEnabled(enabled: Boolean) =
            uiPreferences.setTopResultIndicatorEnabled(enabled)

    fun isClearQueryOnLaunchEnabled(): Boolean = uiPreferences.isClearQueryOnLaunchEnabled()

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) =
            uiPreferences.setClearQueryOnLaunchEnabled(enabled)

    fun isAutoCloseOverlayEnabled(): Boolean = uiPreferences.isAutoCloseOverlayEnabled()

    fun setAutoCloseOverlayEnabled(enabled: Boolean) =
            uiPreferences.setAutoCloseOverlayEnabled(enabled)

    fun isOverlayModeEnabled(): Boolean = uiPreferences.isOverlayModeEnabled()

    fun setOverlayModeEnabled(enabled: Boolean) = uiPreferences.setOverlayModeEnabled(enabled)

    fun getMessagingApp(): MessagingApp = uiPreferences.getMessagingApp()

    fun setMessagingApp(app: MessagingApp) = uiPreferences.setMessagingApp(app)

    fun getCallingApp(): CallingApp = uiPreferences.getCallingApp()

    fun setCallingApp(app: CallingApp) = uiPreferences.setCallingApp(app)

    fun isFirstLaunch(): Boolean = uiPreferences.isFirstLaunch()

    fun setFirstLaunchCompleted() = uiPreferences.setFirstLaunchCompleted()

    fun getWallpaperBackgroundAlpha(isDarkMode: Boolean): Float =
            uiPreferences.getWallpaperBackgroundAlpha(isDarkMode)

    fun setWallpaperBackgroundAlpha(alpha: Float, isDarkMode: Boolean) =
            uiPreferences.setWallpaperBackgroundAlpha(alpha, isDarkMode)

    fun getWallpaperBlurRadius(isDarkMode: Boolean): Float =
            uiPreferences.getWallpaperBlurRadius(isDarkMode)

    fun setWallpaperBlurRadius(radius: Float, isDarkMode: Boolean) =
            uiPreferences.setWallpaperBlurRadius(radius, isDarkMode)

    fun getAppTheme(): AppTheme = uiPreferences.getAppTheme()

    fun setAppTheme(theme: AppTheme) =
            uiPreferences.setAppTheme(theme)

    fun getAppThemeMode(): com.tk.quicksearch.search.core.AppThemeMode = uiPreferences.getAppThemeMode()

    fun setAppThemeMode(theme: com.tk.quicksearch.search.core.AppThemeMode) =
            uiPreferences.setAppThemeMode(theme)

    fun getOverlayThemeIntensity(): Float = uiPreferences.getOverlayThemeIntensity()

    fun setOverlayThemeIntensity(intensity: Float) = uiPreferences.setOverlayThemeIntensity(intensity)

    fun getFontScaleMultiplier(): Float = uiPreferences.getFontScaleMultiplier()

    fun setFontScaleMultiplier(multiplier: Float) = uiPreferences.setFontScaleMultiplier(multiplier)

    fun shouldUseSystemFont(): Boolean = uiPreferences.shouldUseSystemFont()

    fun setUseSystemFont(enabled: Boolean) = uiPreferences.setUseSystemFont(enabled)

    fun getBackgroundSource(): BackgroundSource = uiPreferences.getBackgroundSource()

    fun setBackgroundSource(source: BackgroundSource) =
            uiPreferences.setBackgroundSource(source)

    fun getCustomImageUri(): String? = uiPreferences.getCustomImageUri()

    fun setCustomImageUri(uri: String?) = uiPreferences.setCustomImageUri(uri)

    fun getSelectedIconPackPackage(): String? = uiPreferences.getSelectedIconPackPackage()

    fun setSelectedIconPackPackage(packageName: String?) =
            uiPreferences.setSelectedIconPackPackage(packageName)

    fun isIconPackUnsupportedIconMaskEnabled(): Boolean =
            uiPreferences.isIconPackUnsupportedIconMaskEnabled()

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) =
            uiPreferences.setIconPackUnsupportedIconMaskEnabled(enabled)

    fun getAppIconShape(): com.tk.quicksearch.search.core.AppIconShape =
            uiPreferences.getAppIconShape()

    fun setAppIconShape(shape: com.tk.quicksearch.search.core.AppIconShape) =
            uiPreferences.setAppIconShape(shape)

    fun getLauncherAppIcon(): com.tk.quicksearch.search.core.LauncherAppIcon =
            uiPreferences.getLauncherAppIcon()

    fun setLauncherAppIcon(selection: com.tk.quicksearch.search.core.LauncherAppIcon) =
            uiPreferences.setLauncherAppIcon(selection)

    fun isThemedIconsEnabled(): Boolean = uiPreferences.isThemedIconsEnabled()

    fun setThemedIconsEnabled(enabled: Boolean) = uiPreferences.setThemedIconsEnabled(enabled)

    fun isDeviceThemeEnabled(): Boolean = uiPreferences.isDeviceThemeEnabled()

    fun setDeviceThemeEnabled(enabled: Boolean) = uiPreferences.setDeviceThemeEnabled(enabled)

    fun isWallpaperAccentEnabled(): Boolean = uiPreferences.isWallpaperAccentEnabled()

    fun setWallpaperAccentEnabled(enabled: Boolean) = uiPreferences.setWallpaperAccentEnabled(enabled)

    fun isAiSearchSetupExpanded(): Boolean = uiPreferences.isAiSearchSetupExpanded()

    fun setAiSearchSetupExpanded(expanded: Boolean) =
            uiPreferences.setAiSearchSetupExpanded(expanded)

    fun isDisabledSearchEnginesExpanded(): Boolean = uiPreferences.isDisabledSearchEnginesExpanded()

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) =
            uiPreferences.setDisabledSearchEnginesExpanded(expanded)

    fun isInstantStartupSurfaceEnabled(): Boolean = uiPreferences.isInstantStartupSurfaceEnabled()

    fun setInstantStartupSurfaceEnabled(enabled: Boolean) =
            uiPreferences.setInstantStartupSurfaceEnabled(enabled)

    fun hasSeenSearchBarWelcome(): Boolean = uiPreferences.hasSeenSearchBarWelcome()

    fun setHasSeenSearchBarWelcome(seen: Boolean) = uiPreferences.setHasSeenSearchBarWelcome(seen)

    fun shouldForceSearchBarWelcomeOnNextOpen(): Boolean =
            uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()

    fun setForceSearchBarWelcomeOnNextOpen(force: Boolean) =
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(force)

    fun consumeForceSearchBarWelcomeOnNextOpen(): Boolean {
        val shouldForce = uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()
        if (shouldForce) {
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        return shouldForce
    }

    fun hasSeenContactActionHint(): Boolean = uiPreferences.hasSeenContactActionHint()

    fun setHasSeenContactActionHint(seen: Boolean) = uiPreferences.setHasSeenContactActionHint(seen)

    fun hasDismissedSearchHistoryTip(): Boolean = uiPreferences.hasDismissedSearchHistoryTip()

    fun setSearchHistoryTipDismissed(dismissed: Boolean) =
            uiPreferences.setSearchHistoryTipDismissed(dismissed)

    fun hasSeenOverlayAssistantTip(): Boolean = uiPreferences.hasSeenOverlayAssistantTip()

    fun setHasSeenOverlayAssistantTip(seen: Boolean) =
            uiPreferences.setHasSeenOverlayAssistantTip(seen)

    fun hasSeenSettingsSearchTip(): Boolean = uiPreferences.hasSeenSettingsSearchTip()

    fun setHasSeenSettingsSearchTip(seen: Boolean) =
            uiPreferences.setHasSeenSettingsSearchTip(seen)

    fun getLastSeenVersionName(): String? = uiPreferences.getLastSeenVersionName()

    fun setLastSeenVersionName(versionName: String?) =
            uiPreferences.setLastSeenVersionName(versionName)

    fun getLastSeenVersionCode(): Long? = uiPreferences.getLastSeenVersionCode()

    fun setLastSeenVersionCode(versionCode: Long) =
            uiPreferences.setLastSeenVersionCode(versionCode)

    fun getUsagePermissionBannerDismissCount(): Int =
            uiPreferences.getUsagePermissionBannerDismissCount()

    fun incrementUsagePermissionBannerDismissCount() =
            uiPreferences.incrementUsagePermissionBannerDismissCount()

    fun isUsagePermissionBannerSessionDismissed(): Boolean =
            uiPreferences.isUsagePermissionBannerSessionDismissed()

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) =
            uiPreferences.setUsagePermissionBannerSessionDismissed(dismissed)

    fun resetUsagePermissionBannerSessionDismissed() =
            uiPreferences.resetUsagePermissionBannerSessionDismissed()

    fun shouldShowUsagePermissionBanner(): Boolean = uiPreferences.shouldShowUsagePermissionBanner()

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areAppSuggestionsEnabled(): Boolean = uiPreferences.areAppSuggestionsEnabled()

    fun setAppSuggestionsEnabled(enabled: Boolean) = uiPreferences.setAppSuggestionsEnabled(enabled)

    fun shouldShowAppLabels(): Boolean = uiPreferences.shouldShowAppLabels()

    fun setShowAppLabels(show: Boolean) = uiPreferences.setShowAppLabels(show)

    fun getPhoneAppGridColumns(): Int = uiPreferences.getPhoneAppGridColumns()

    fun setPhoneAppGridColumns(columns: Int) = uiPreferences.setPhoneAppGridColumns(columns)

    fun areWebSuggestionsEnabled(): Boolean = uiPreferences.areWebSuggestionsEnabled()

    fun getWebSuggestionsCount(): Int = uiPreferences.getWebSuggestionsCount()

    fun setWebSuggestionsCount(count: Int) {
        uiPreferences.setWebSuggestionsCount(count)
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

    fun isWordClockEnabled(): Boolean = uiPreferences.isWordClockEnabled()

    fun setWordClockEnabled(enabled: Boolean) = uiPreferences.setWordClockEnabled(enabled)

    fun isDictionaryEnabled(): Boolean = uiPreferences.isDictionaryEnabled()

    fun setDictionaryEnabled(enabled: Boolean) = uiPreferences.setDictionaryEnabled(enabled)

    fun getCurrencyConverterModel(): String = uiPreferences.getCurrencyConverterModel()

    // ============================================================================
    // Recent Queries Preferences
    // ============================================================================

    fun getRecentItems(): List<com.tk.quicksearch.search.searchHistory.RecentSearchEntry> =
            recentSearchesPreferences.getRecentItems()

    fun addRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) {
        recentSearchesPreferences.addRecentItem(entry)
        recentResultOpensPreferences.addRecentResultOpen(entry)
    }

    fun getRecentResultOpens(): List<com.tk.quicksearch.search.searchHistory.RecentSearchEntry> =
            recentResultOpensPreferences.getRecentResultOpens()

    fun clearRecentQueries() = recentSearchesPreferences.clearRecentQueries()

    fun deleteRecentItem(entry: com.tk.quicksearch.search.searchHistory.RecentSearchEntry) {
        recentSearchesPreferences.deleteRecentItem(entry)
        recentResultOpensPreferences.deleteRecentResultOpen(entry)
    }

    fun areRecentQueriesEnabled(): Boolean = recentSearchesPreferences.areRecentQueriesEnabled()

    fun setRecentQueriesEnabled(enabled: Boolean) =
            recentSearchesPreferences.setRecentQueriesEnabled(enabled)

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getDisabledSections(): Set<String> = uiPreferences.getDisabledSections()

    fun setDisabledSections(disabled: Set<String>) = uiPreferences.setDisabledSections(disabled)

    /**
     * One-time migration to ensure calendar section is default-off for all users, including
     * existing installs.
     */
    fun ensureCalendarSectionDefaultDisabledMigration() {
        if (sharedPrefs.getBoolean(BasePreferences.KEY_CALENDAR_SECTION_DEFAULT_MIGRATION_DONE, false)) {
            return
        }

        val updatedDisabledSections = getDisabledSections().toMutableSet().apply { add("CALENDAR") }
        setDisabledSections(updatedDisabledSections)
        sharedPrefs
                .edit()
                .putBoolean(BasePreferences.KEY_CALENDAR_SECTION_DEFAULT_MIGRATION_DONE, true)
                .apply()
    }

    // ============================================================================
    // In-App Review Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long = uiPreferences.getFirstAppOpenTime()

    fun recordFirstAppOpenTime() = uiPreferences.recordFirstAppOpenTime()

    fun getLastReviewPromptTime(): Long = uiPreferences.getLastReviewPromptTime()

    fun recordReviewPromptTime() = uiPreferences.recordReviewPromptTime()

    fun getReviewPromptedCount(): Int = uiPreferences.getReviewPromptedCount()

    fun incrementReviewPromptedCount() = uiPreferences.incrementReviewPromptedCount()

    fun getAppOpenCount(): Int = uiPreferences.getAppOpenCount()

    fun incrementAppOpenCount() = uiPreferences.incrementAppOpenCount()

    fun recordAppOpenCountAtPrompt() = uiPreferences.recordAppOpenCountAtPrompt()

    fun shouldShowReviewPrompt(): Boolean = uiPreferences.shouldShowReviewPrompt()

    // ============================================================================
    // In-App Update Session Tracking
    // ============================================================================

    fun hasShownUpdateCheckThisSession(): Boolean = uiPreferences.hasShownUpdateCheckThisSession()

    fun setUpdateCheckShownThisSession() = uiPreferences.setUpdateCheckShownThisSession()

    fun resetUpdateCheckSession() = uiPreferences.resetUpdateCheckSession()
}
