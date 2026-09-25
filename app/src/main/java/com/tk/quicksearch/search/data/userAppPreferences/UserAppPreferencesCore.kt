package com.tk.quicksearch.search.data

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

internal const val AI_STARTUP_PREFS_NAME = "ai_startup_state"
internal const val KEY_HAS_CONFIGURED_AI_PROVIDER = "has_configured_ai_provider"

open class UserAppPreferencesCore(protected val context: Context) {

    protected val sharedPrefs by lazy {
        context.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
        )
    }

    // Feature-specific preference managers - lazy to avoid blocking construction
    protected val appPreferences by lazy { AppPreferences(context) }
    protected val contactPreferences by lazy { ContactPreferences(context) }
    protected val filePreferences by lazy { FilePreferences(context) }
    protected val settingsPreferences by lazy { SettingsPreferences(context) }
    protected val calendarPreferences by lazy { CalendarPreferences(context) }
    protected val notesPreferences by lazy { NotesPreferences(context) }
    protected val reminderPreferences by lazy { ReminderPreferences(context) }
    protected val gesturesPreferences by lazy { GesturesPreferences(context) }
    protected val edgeGesturePreferences by lazy { EdgeGesturePreferences(context) }
    protected val floatingButtonPreferences by lazy { FloatingButtonPreferences(context) }
    protected val appShortcutPreferences by lazy { AppShortcutPreferences(context) }
    protected val folderPreferences by lazy { FolderPreferences(context) }
    protected val nicknamePreferences by lazy { NicknamePreferences(context) }
    protected val triggerPreferences by lazy { TriggerPreferences(context) }
    protected val taskerIntentPreferences by lazy { TaskerIntentPreferences(context) }
    protected val weatherPreferences by lazy { WeatherPreferences(context) }
    protected val searchEnginePreferences by lazy { SearchEnginePreferences(context) }
    protected val aliasPreferences by lazy { AliasPreferences(context) }
    protected val geminiPreferences by lazy { GeminiPreferences(context) }
    protected val openAiPreferences by lazy { OpenAiPreferences(context) }
    protected val anthropicPreferences by lazy { AnthropicPreferences(context) }
    protected val groqPreferences by lazy { GroqPreferences(context) }
    protected val metaPreferences by lazy { MetaPreferences(context) }
    protected val customLlmProviderPreferences by lazy { CustomLlmProviderPreferences(context) }
    protected val tavilyPreferences by lazy { TavilyPreferences(context) }
    protected val llmPreferences by lazy { LlmPreferences(context) }
    val uiPreferences by lazy { UiPreferences(context) }
    protected val amazonPreferences by lazy { AmazonPreferences(context) }
    protected val recentSearchesPreferences by lazy { SearchHistoryPreferences(context) }
    protected val recentResultOpensPreferences by lazy { RecentResultOpensPreferences(context) }
    protected val startupPreferences by lazy { StartupPreferencesFacade(context) }
    protected val aiStartupPreferences by lazy {
        context.applicationContext.getSharedPreferences(
                AI_STARTUP_PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
        )
    }


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

    fun getPinnedNonAppItemOrder(): List<String> =
            PreferenceUtils.getStringListPref(
                    sharedPrefs,
                    BasePreferences.KEY_PINNED_NON_APP_ITEM_ORDER,
            )

    fun setPinnedNonAppItemOrder(order: List<String>): List<String> =
            order.distinct().also {
                PreferenceUtils.setStringListPref(
                        sharedPrefs,
                        BasePreferences.KEY_PINNED_NON_APP_ITEM_ORDER,
                        it,
                )
            }

    fun getSuggestionHiddenPackages(): Set<String> = appPreferences.getSuggestionHiddenPackages()

    fun getResultHiddenPackages(): Set<String> = appPreferences.getResultHiddenPackages()

    fun getPinnedPackages(): Set<String> = appPreferences.getPinnedPackages()

    fun getPinnedPackageOrder(): List<String> = appPreferences.getPinnedPackageOrder()

    fun setPinnedPackageOrder(packageNames: List<String>): List<String> =
            appPreferences.setPinnedPackageOrder(packageNames)

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

    fun getAppIconOverride(packageName: String) = appPreferences.getAppIconOverride(packageName)

    fun setAppIconOverride(
        packageName: String,
        iconPackPackage: String,
        drawableName: String,
    ) = appPreferences.setAppIconOverride(packageName, iconPackPackage, drawableName)

    fun setAppIconToSystemDefault(packageName: String) =
            appPreferences.setAppIconToSystemDefault(packageName)

    fun getAppIconOverridePackageNames(): Set<String> =
            appPreferences.getAppIconOverridePackageNames()

    fun clearAppIconOverrides(packageNames: Collection<String>) =
            appPreferences.clearAppIconOverrides(packageNames)

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

    fun getPinnedContactOrder(): List<Long> = contactPreferences.getPinnedContactOrder()

    fun setPinnedContactOrder(order: List<Long>): List<Long> =
            contactPreferences.setPinnedContactOrder(order)

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

    fun isNumberSearchEnabled(): Boolean = contactPreferences.isNumberSearchEnabled()

    fun setNumberSearchEnabled(enabled: Boolean) = contactPreferences.setNumberSearchEnabled(enabled)

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

    fun getPinnedFileOrder(): List<String> = filePreferences.getPinnedFileOrder()

    fun setPinnedFileOrder(order: List<String>): List<String> =
            filePreferences.setPinnedFileOrder(order)

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

    fun areFilePreviewsEnabled(): Boolean = filePreferences.areFilePreviewsEnabled()

    fun setFilePreviewsEnabled(enabled: Boolean) = filePreferences.setFilePreviewsEnabled(enabled)

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

    fun getPinnedSettingOrder(): List<String> = settingsPreferences.getPinnedSettingOrder()

    fun setPinnedSettingOrder(order: List<String>): List<String> =
            settingsPreferences.setPinnedSettingOrder(order)

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

    fun getPinnedCalendarEventOrder(): List<Long> = calendarPreferences.getPinnedEventOrder()

    fun setPinnedCalendarEventOrder(order: List<Long>): List<Long> =
            calendarPreferences.setPinnedEventOrder(order)

    fun getExcludedCalendarEventIds(): Set<Long> = calendarPreferences.getExcludedEventIds()

    fun pinCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.pinEvent(eventId)

    fun unpinCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.unpinEvent(eventId)

    fun excludeCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.excludeEvent(eventId)

    fun removeExcludedCalendarEvent(eventId: Long): Set<Long> =
            calendarPreferences.removeExcludedEvent(eventId)

    fun clearAllExcludedCalendarEvents(): Set<Long> = calendarPreferences.clearAllExcludedEvents()

    fun getShowTodayEvents(): Boolean = calendarPreferences.getShowTodayEvents()

    fun setShowTodayEvents(show: Boolean) = calendarPreferences.setShowTodayEvents(show)

    fun getDefaultCalendarPackage(): String? = calendarPreferences.getDefaultCalendarPackage()

    fun setDefaultCalendarPackage(packageName: String?) =
        calendarPreferences.setDefaultCalendarPackage(packageName)

    fun getArchivedTodayEventIds(): Set<Long> = calendarPreferences.getArchivedTodayEventIds()

    fun archiveTodayCalendarEvent(eventId: Long): Set<Long> = calendarPreferences.archiveTodayEvent(eventId)

    // ============================================================================
    // Notes Preferences
    // ============================================================================

    fun getPinnedNoteIds(): Set<Long> = notesPreferences.getPinnedNoteIds()

    fun getPinnedNoteOrder(): List<Long> = notesPreferences.getPinnedNoteOrder()

    fun setPinnedNoteOrder(order: List<Long>): List<Long> =
            notesPreferences.setPinnedNoteOrder(order)

    fun pinNote(noteId: Long): Set<Long> = notesPreferences.pinNote(noteId)

    fun unpinNote(noteId: Long): Set<Long> = notesPreferences.unpinNote(noteId)

    fun getNotesJson(): String = notesPreferences.getNotesJson()

    fun setNotesJson(json: String) = notesPreferences.setNotesJson(json)

    fun nextNoteId(): Long = notesPreferences.nextNoteId()

    fun ensureNoteIdCounterAtLeast(nextCandidate: Long) =
            notesPreferences.ensureNoteIdCounterAtLeast(nextCandidate)

    fun isQuickNoteEnabled(): Boolean = notesPreferences.isQuickNoteEnabled()

    fun setQuickNoteEnabled(enabled: Boolean) = notesPreferences.setQuickNoteEnabled(enabled)

    // ============================================================================
    // Edge Gesture Preferences
    // ============================================================================

    fun getEdgeGestureConfig(): EdgeGestureConfig = edgeGesturePreferences.getConfig()

    fun setEdgeGestureEnabled(enabled: Boolean) = edgeGesturePreferences.setEnabled(enabled)

    fun setEdgeGestureSide(side: EdgeGestureSide) = edgeGesturePreferences.setSide(side)

    fun setEdgeGesturePosition(position: Float) = edgeGesturePreferences.setPosition(position)

    fun setEdgeGestureSize(size: Float) = edgeGesturePreferences.setSize(size)

    fun setEdgeGestureWidthDp(widthDp: Int) = edgeGesturePreferences.setWidthDp(widthDp)

    fun setEdgeGestureOpacity(opacity: Float) = edgeGesturePreferences.setOpacity(opacity)

    fun setEdgeGestureActivation(activation: EdgeGestureActivation) =
        edgeGesturePreferences.setActivation(activation)

    // ============================================================================
    // Floating Button Preferences
    // ============================================================================

    fun getFloatingButtonConfig(): FloatingButtonConfig = floatingButtonPreferences.getConfig()

    fun setFloatingButtonEnabled(enabled: Boolean) = floatingButtonPreferences.setEnabled(enabled)

    fun setFloatingButtonSizeDp(sizeDp: Int) = floatingButtonPreferences.setSizeDp(sizeDp)

    fun setFloatingButtonOpacity(opacity: Float) = floatingButtonPreferences.setOpacity(opacity)

    // ============================================================================
    // Reminder Preferences
    // ============================================================================

    fun getPinnedReminderIds(): Set<Long> = reminderPreferences.getPinnedReminderIds()

    fun getPinnedReminderOrder(): List<Long> = reminderPreferences.getPinnedReminderOrder()

    fun setPinnedReminderOrder(order: List<Long>): List<Long> =
            reminderPreferences.setPinnedReminderOrder(order)

    fun pinReminder(reminderId: Long): Set<Long> = reminderPreferences.pinReminder(reminderId)

    fun unpinReminder(reminderId: Long): Set<Long> = reminderPreferences.unpinReminder(reminderId)


    fun getIncludePastReminders(): Boolean = reminderPreferences.getIncludePastReminders()

    fun setIncludePastReminders(value: Boolean) = reminderPreferences.setIncludePastReminders(value)

    // ============================================================================
    // Gesture Preferences
    // ============================================================================

    fun getSwipeRightAction(): SwipeGestureAction = gesturesPreferences.getSwipeRightAction()

    fun setSwipeRightAction(action: SwipeGestureAction) = gesturesPreferences.setSwipeRightAction(action)

    fun getSwipeRightCustomAction(): String? = gesturesPreferences.getSwipeRightCustomAction()

    fun setSwipeRightCustomAction(actionJson: String?) = gesturesPreferences.setSwipeRightCustomAction(actionJson)
    fun getSwipeRightAliasTarget(): String? = gesturesPreferences.getSwipeRightAliasTarget()
    fun setSwipeRightAliasTarget(targetId: String?) = gesturesPreferences.setSwipeRightAliasTarget(targetId)

    fun isLauncherSwipeRightEnabled(): Boolean = gesturesPreferences.isLauncherSwipeRightEnabled()

    fun setLauncherSwipeRightEnabled(enabled: Boolean) = gesturesPreferences.setLauncherSwipeRightEnabled(enabled)

    fun getSwipeLeftAction(): SwipeGestureAction = gesturesPreferences.getSwipeLeftAction()

    fun setSwipeLeftAction(action: SwipeGestureAction) = gesturesPreferences.setSwipeLeftAction(action)

    fun getSwipeLeftCustomAction(): String? = gesturesPreferences.getSwipeLeftCustomAction()

    fun setSwipeLeftCustomAction(actionJson: String?) = gesturesPreferences.setSwipeLeftCustomAction(actionJson)
    fun getSwipeLeftAliasTarget(): String? = gesturesPreferences.getSwipeLeftAliasTarget()
    fun setSwipeLeftAliasTarget(targetId: String?) = gesturesPreferences.setSwipeLeftAliasTarget(targetId)

    fun getSwipeUpAction(): SwipeGestureAction = gesturesPreferences.getSwipeUpAction()

    fun setSwipeUpAction(action: SwipeGestureAction) = gesturesPreferences.setSwipeUpAction(action)

    fun getSwipeUpCustomAction(): String? = gesturesPreferences.getSwipeUpCustomAction()

    fun setSwipeUpCustomAction(actionJson: String?) = gesturesPreferences.setSwipeUpCustomAction(actionJson)
    fun getSwipeUpAliasTarget(): String? = gesturesPreferences.getSwipeUpAliasTarget()
    fun setSwipeUpAliasTarget(targetId: String?) = gesturesPreferences.setSwipeUpAliasTarget(targetId)

    fun getSwipeDownAction(): SwipeGestureAction = gesturesPreferences.getSwipeDownAction()

    fun setSwipeDownAction(action: SwipeGestureAction) = gesturesPreferences.setSwipeDownAction(action)

    fun getSwipeDownCustomAction(): String? = gesturesPreferences.getSwipeDownCustomAction()

    fun setSwipeDownCustomAction(actionJson: String?) = gesturesPreferences.setSwipeDownCustomAction(actionJson)
    fun getSwipeDownAliasTarget(): String? = gesturesPreferences.getSwipeDownAliasTarget()
    fun setSwipeDownAliasTarget(targetId: String?) = gesturesPreferences.setSwipeDownAliasTarget(targetId)

    fun getHomeSwipeUpAction(): HomeSwipeGestureAction = gesturesPreferences.getHomeSwipeUpAction()

    fun setHomeSwipeUpAction(action: HomeSwipeGestureAction) = gesturesPreferences.setHomeSwipeUpAction(action)

    fun getHomeSwipeUpCustomAction(): String? = gesturesPreferences.getHomeSwipeUpCustomAction()

    fun setHomeSwipeUpCustomAction(actionJson: String?) = gesturesPreferences.setHomeSwipeUpCustomAction(actionJson)
    fun getHomeSwipeUpAliasTarget(): String? = gesturesPreferences.getHomeSwipeUpAliasTarget()
    fun setHomeSwipeUpAliasTarget(targetId: String?) = gesturesPreferences.setHomeSwipeUpAliasTarget(targetId)

    fun getHomeSwipeDownAction(isDefaultLauncher: Boolean = true): HomeSwipeGestureAction =
        gesturesPreferences.getHomeSwipeDownAction(isDefaultLauncher)

    fun setHomeSwipeDownAction(action: HomeSwipeGestureAction) = gesturesPreferences.setHomeSwipeDownAction(action)

    fun getHomeSwipeDownCustomAction(): String? = gesturesPreferences.getHomeSwipeDownCustomAction()

    fun setHomeSwipeDownCustomAction(actionJson: String?) = gesturesPreferences.setHomeSwipeDownCustomAction(actionJson)
    fun getHomeSwipeDownAliasTarget(): String? = gesturesPreferences.getHomeSwipeDownAliasTarget()
    fun setHomeSwipeDownAliasTarget(targetId: String?) = gesturesPreferences.setHomeSwipeDownAliasTarget(targetId)

    fun getHomeDoubleTapAction(): HomeSwipeGestureAction = gesturesPreferences.getHomeDoubleTapAction()

    fun setHomeDoubleTapAction(action: HomeSwipeGestureAction) = gesturesPreferences.setHomeDoubleTapAction(action)

    fun getHomeDoubleTapCustomAction(): String? = gesturesPreferences.getHomeDoubleTapCustomAction()

    fun setHomeDoubleTapCustomAction(actionJson: String?) = gesturesPreferences.setHomeDoubleTapCustomAction(actionJson)
    fun getHomeDoubleTapAliasTarget(): String? = gesturesPreferences.getHomeDoubleTapAliasTarget()
    fun setHomeDoubleTapAliasTarget(targetId: String?) = gesturesPreferences.setHomeDoubleTapAliasTarget(targetId)

    // ============================================================================
    // App Shortcut Preferences
    // ============================================================================

    fun getPinnedAppShortcutIds(): Set<String> = appShortcutPreferences.getPinnedAppShortcutIds()

    fun getPinnedAppShortcutOrder(): List<String> = appShortcutPreferences.getPinnedAppShortcutOrder()

    fun setPinnedAppShortcutOrder(order: List<String>): List<String> =
            appShortcutPreferences.setPinnedAppShortcutOrder(order)

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

    fun setAppShortcutsEnabled(
            ids: Collection<String>,
            enabled: Boolean,
    ): Set<String> = appShortcutPreferences.setAppShortcutsEnabled(ids, enabled)

    fun setAllAppShortcutsEnabled(
            packageName: String,
            enabled: Boolean,
    ): Set<String> = appShortcutPreferences.setAllAppShortcutsEnabled(packageName, enabled)

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

    fun getContactActionTrigger(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
    ): ResultTrigger? =
            triggerPreferences.getContactActionTrigger(contactId, action)

    fun setContactActionTrigger(
            contactId: Long,
            action: com.tk.quicksearch.search.contacts.models.ContactCardAction,
            trigger: ResultTrigger?,
    ) = triggerPreferences.setContactActionTrigger(contactId, action, trigger)

    fun getAllContactActionTriggers():
            Map<com.tk.quicksearch.search.data.preferences.ContactActionTriggerKey, ResultTrigger> =
            triggerPreferences.getAllContactActionTriggers()

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
            triggerPreferences.getAllTriggerWordsById()

    fun getAllAliasWordsById(): Map<String, String> =
            aliasPreferences.getAllAliasWordsById()

    fun hasAnyTriggerItems(): Boolean = getAllTriggerWordsById().isNotEmpty()

    fun findNotesWithMatchingTrigger(query: String): Set<Long> =
            triggerPreferences.findNotesWithMatchingTrigger(query)

    // ============================================================================
}
