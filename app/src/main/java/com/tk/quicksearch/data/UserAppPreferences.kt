package com.tk.quicksearch.data

import android.content.Context
import com.tk.quicksearch.data.preferences.*
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchEngine

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 * Manages preferences for apps, contacts, files, search engines, shortcuts, and UI settings.
 * This class now delegates to specialized preference classes for better organization.
 */
class UserAppPreferences(context: Context) {

    // Feature-specific preference managers - lazy to avoid blocking construction
    private val appPreferences by lazy { AppPreferences(context) }
    private val contactPreferences by lazy { ContactPreferences(context) }
    private val filePreferences by lazy { FilePreferences(context) }
    private val settingsPreferences by lazy { SettingsPreferences(context) }
    private val nicknamePreferences by lazy { NicknamePreferences(context) }
    private val searchEnginePreferences by lazy { SearchEnginePreferences(context) }
    private val shortcutPreferences by lazy { ShortcutPreferences(context) }
    private val geminiPreferences by lazy { GeminiPreferences(context) }
    private val uiPreferences by lazy { UiPreferences(context) }
    private val amazonPreferences by lazy { AmazonPreferences(context) }

    /**
     * Minimal preferences needed for first frame render - only layout-affecting values.
     */
    data class CriticalPreferences(
        val keyboardAlignedLayout: Boolean
    )

    fun getCriticalPreferences(): CriticalPreferences {
        return CriticalPreferences(
            keyboardAlignedLayout = uiPreferences.isKeyboardAlignedLayout()
        )
    }

    /**
     * Data class to hold all preferences needed during app startup for performance optimization.
     */
    data class StartupPreferences(
        val enabledFileTypes: Set<FileType>,
        val excludedFileExtensions: Set<String>,
        val keyboardAlignedLayout: Boolean,
        val directDialEnabled: Boolean,
        val hasSeenDirectDialChoice: Boolean,
        val hasSeenSearchEngineOnboarding: Boolean,
        val showWallpaperBackground: Boolean,
        val clearQueryAfterSearchEngine: Boolean,
        val showAllResults: Boolean,
        val sortAppsByUsage: Boolean,
        val amazonDomain: String?,
        val pinnedPackages: Set<String>,
        val suggestionHiddenPackages: Set<String>,
        val resultHiddenPackages: Set<String>
    )

    /**
     * Loads all preferences needed during startup in a single batch operation.
     * This reduces the number of SharedPreferences reads for better startup performance.
     */
    fun getStartupPreferences(): StartupPreferences {
        return StartupPreferences(
            enabledFileTypes = filePreferences.getEnabledFileTypes(),
            excludedFileExtensions = filePreferences.getExcludedFileExtensions(),
            keyboardAlignedLayout = uiPreferences.isKeyboardAlignedLayout(),
            directDialEnabled = contactPreferences.isDirectDialEnabled(),
            hasSeenDirectDialChoice = contactPreferences.hasSeenDirectDialChoice(),
            hasSeenSearchEngineOnboarding = searchEnginePreferences.hasSeenSearchEngineOnboarding(),
            showWallpaperBackground = uiPreferences.shouldShowWallpaperBackground(),
            clearQueryAfterSearchEngine = uiPreferences.shouldClearQueryAfterSearchEngine(),
            showAllResults = uiPreferences.shouldShowAllResults(),
            sortAppsByUsage = uiPreferences.shouldSortAppsByUsage(),
            amazonDomain = amazonPreferences.getAmazonDomain(),
            pinnedPackages = appPreferences.getPinnedPackages(),
            suggestionHiddenPackages = appPreferences.getSuggestionHiddenPackages(),
            resultHiddenPackages = appPreferences.getResultHiddenPackages()
        )
    }

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = appPreferences.getSuggestionHiddenPackages()

    fun getResultHiddenPackages(): Set<String> = appPreferences.getResultHiddenPackages()

    fun getPinnedPackages(): Set<String> = appPreferences.getPinnedPackages()

    fun hidePackageInSuggestions(packageName: String): Set<String> = appPreferences.hidePackageInSuggestions(packageName)

    fun hidePackageInResults(packageName: String): Set<String> = appPreferences.hidePackageInResults(packageName)

    fun unhidePackageInSuggestions(packageName: String): Set<String> = appPreferences.unhidePackageInSuggestions(packageName)

    fun unhidePackageInResults(packageName: String): Set<String> = appPreferences.unhidePackageInResults(packageName)

    fun pinPackage(packageName: String): Set<String> = appPreferences.pinPackage(packageName)

    fun unpinPackage(packageName: String): Set<String> = appPreferences.unpinPackage(packageName)

    fun clearAllHiddenAppsInSuggestions(): Set<String> = appPreferences.clearAllHiddenAppsInSuggestions()

    fun clearAllHiddenAppsInResults(): Set<String> = appPreferences.clearAllHiddenAppsInResults()

    fun getAppLaunchCount(packageName: String): Int = appPreferences.getAppLaunchCount(packageName)

    fun incrementAppLaunchCount(packageName: String) = appPreferences.incrementAppLaunchCount(packageName)

    fun getAllAppLaunchCounts(): Map<String, Int> = appPreferences.getAllAppLaunchCounts()

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = contactPreferences.getPinnedContactIds()

    fun getExcludedContactIds(): Set<Long> = contactPreferences.getExcludedContactIds()

    fun pinContact(contactId: Long): Set<Long> = contactPreferences.pinContact(contactId)

    fun unpinContact(contactId: Long): Set<Long> = contactPreferences.unpinContact(contactId)

    fun excludeContact(contactId: Long): Set<Long> = contactPreferences.excludeContact(contactId)

    fun removeExcludedContact(contactId: Long): Set<Long> = contactPreferences.removeExcludedContact(contactId)

    fun clearAllExcludedContacts(): Set<Long> = contactPreferences.clearAllExcludedContacts()

    fun getPreferredPhoneNumber(contactId: Long): String? = contactPreferences.getPreferredPhoneNumber(contactId)

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) = contactPreferences.setPreferredPhoneNumber(contactId, phoneNumber)

    fun getLastShownPhoneNumber(contactId: Long): String? = contactPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(contactId: Long, phoneNumber: String) = contactPreferences.setLastShownPhoneNumber(contactId, phoneNumber)

    fun isDirectDialEnabled(): Boolean = contactPreferences.isDirectDialEnabled()

    fun setDirectDialEnabled(enabled: Boolean) = contactPreferences.setDirectDialEnabled(enabled)

    fun hasSeenDirectDialChoice(): Boolean = contactPreferences.hasSeenDirectDialChoice()

    fun setHasSeenDirectDialChoice(seen: Boolean) = contactPreferences.setHasSeenDirectDialChoice(seen)

    fun isDirectDialManuallyDisabled(): Boolean = contactPreferences.isDirectDialManuallyDisabled()

    fun setDirectDialManuallyDisabled(disabled: Boolean) = contactPreferences.setDirectDialManuallyDisabled(disabled)

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

    fun addExcludedFileExtension(extension: String): Set<String> = filePreferences.addExcludedFileExtension(extension)

    fun removeExcludedFileExtension(extension: String): Set<String> = filePreferences.removeExcludedFileExtension(extension)

    fun clearAllExcludedFileExtensions(): Set<String> = filePreferences.clearAllExcludedFileExtensions()

    fun getEnabledFileTypes(): Set<com.tk.quicksearch.model.FileType> = filePreferences.getEnabledFileTypes()

    fun setEnabledFileTypes(enabled: Set<com.tk.quicksearch.model.FileType>) = filePreferences.setEnabledFileTypes(enabled)

    // ============================================================================
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = settingsPreferences.getPinnedSettingIds()

    fun getExcludedSettingIds(): Set<String> = settingsPreferences.getExcludedSettingIds()

    fun pinSetting(id: String): Set<String> = settingsPreferences.pinSetting(id)

    fun unpinSetting(id: String): Set<String> = settingsPreferences.unpinSetting(id)

    fun excludeSetting(id: String): Set<String> = settingsPreferences.excludeSetting(id)

    fun removeExcludedSetting(id: String): Set<String> = settingsPreferences.removeExcludedSetting(id)

    fun clearAllExcludedSettings(): Set<String> = settingsPreferences.clearAllExcludedSettings()

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    fun getAllAppNicknames(): Map<String, String> = nicknamePreferences.getAllAppNicknames()

    fun getAppNickname(packageName: String): String? = nicknamePreferences.getAppNickname(packageName)

    fun setAppNickname(packageName: String, nickname: String?) = nicknamePreferences.setAppNickname(packageName, nickname)

    fun getContactNickname(contactId: Long): String? = nicknamePreferences.getContactNickname(contactId)

    fun setContactNickname(contactId: Long, nickname: String?) = nicknamePreferences.setContactNickname(contactId, nickname)

    fun getFileNickname(uri: String): String? = nicknamePreferences.getFileNickname(uri)

    fun setFileNickname(uri: String, nickname: String?) = nicknamePreferences.setFileNickname(uri, nickname)

    fun getSettingNickname(id: String): String? = nicknamePreferences.getSettingNickname(id)

    fun setSettingNickname(id: String, nickname: String?) = nicknamePreferences.setSettingNickname(id, nickname)

    /**
     * Finds contact IDs that have nicknames matching the query.
     */
    fun findContactsWithMatchingNickname(query: String): Set<Long> = nicknamePreferences.findContactsWithMatchingNickname(query)

    /**
     * Finds file URIs that have nicknames matching the query.
     */
    fun findFilesWithMatchingNickname(query: String): Set<String> = nicknamePreferences.findFilesWithMatchingNickname(query)

    /**
     * Finds settings that have nicknames matching the query.
     */
    fun findSettingsWithMatchingNickname(query: String): Set<String> = nicknamePreferences.findSettingsWithMatchingNickname(query)


    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean = searchEnginePreferences.hasDisabledSearchEnginesPreference()

    fun getDisabledSearchEngines(): Set<String> = searchEnginePreferences.getDisabledSearchEngines()

    fun setDisabledSearchEngines(disabled: Set<String>) = searchEnginePreferences.setDisabledSearchEngines(disabled)

    fun getSearchEngineOrder(): List<String> = searchEnginePreferences.getSearchEngineOrder()

    fun setSearchEngineOrder(order: List<String>) = searchEnginePreferences.setSearchEngineOrder(order)

    fun isSearchEngineCompactMode(): Boolean = searchEnginePreferences.isSearchEngineCompactMode()

    fun setSearchEngineCompactMode(enabled: Boolean) = searchEnginePreferences.setSearchEngineCompactMode(enabled)

    fun hasSeenSearchEngineOnboarding(): Boolean = searchEnginePreferences.hasSeenSearchEngineOnboarding()

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) = searchEnginePreferences.setHasSeenSearchEngineOnboarding(seen)

    // ============================================================================
    // Shortcut Preferences
    // ============================================================================

    fun areShortcutsEnabled(): Boolean = shortcutPreferences.areShortcutsEnabled()

    fun setShortcutsEnabled(enabled: Boolean) = shortcutPreferences.setShortcutsEnabled(enabled)

    fun getShortcutCode(engine: SearchEngine): String = shortcutPreferences.getShortcutCode(engine)

    fun setShortcutCode(engine: SearchEngine, code: String) = shortcutPreferences.setShortcutCode(engine, code)

    fun isShortcutEnabled(engine: SearchEngine): Boolean = shortcutPreferences.isShortcutEnabled(engine)

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) = shortcutPreferences.setShortcutEnabled(engine, enabled)

    fun getAllShortcutCodes(): Map<SearchEngine, String> = shortcutPreferences.getAllShortcutCodes()

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? = amazonPreferences.getAmazonDomain()

    fun setAmazonDomain(domain: String?) = amazonPreferences.setAmazonDomain(domain)

    // ============================================================================
    // Gemini API Preferences
    // ============================================================================

    fun getGeminiApiKey(): String? = geminiPreferences.getGeminiApiKey()

    fun setGeminiApiKey(key: String?) = geminiPreferences.setGeminiApiKey(key)

    fun getPersonalContext(): String? = geminiPreferences.getPersonalContext()

    fun setPersonalContext(context: String?) = geminiPreferences.setPersonalContext(context)

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isKeyboardAlignedLayout(): Boolean = uiPreferences.isKeyboardAlignedLayout()

    fun setKeyboardAlignedLayout(enabled: Boolean) = uiPreferences.setKeyboardAlignedLayout(enabled)

    fun getMessagingApp(): MessagingApp = uiPreferences.getMessagingApp()

    fun setMessagingApp(app: MessagingApp) = uiPreferences.setMessagingApp(app)


    fun isFirstLaunch(): Boolean = uiPreferences.isFirstLaunch()

    fun setFirstLaunchCompleted() = uiPreferences.setFirstLaunchCompleted()

    fun shouldShowWallpaperBackground(): Boolean = uiPreferences.shouldShowWallpaperBackground()

    fun setShowWallpaperBackground(showWallpaper: Boolean) = uiPreferences.setShowWallpaperBackground(showWallpaper)

    fun shouldClearQueryAfterSearchEngine(): Boolean = uiPreferences.shouldClearQueryAfterSearchEngine()

    fun setClearQueryAfterSearchEngine(clearQuery: Boolean) = uiPreferences.setClearQueryAfterSearchEngine(clearQuery)

    fun shouldShowAllResults(): Boolean = uiPreferences.shouldShowAllResults()

    fun setShowAllResults(showAllResults: Boolean) = uiPreferences.setShowAllResults(showAllResults)

    fun getSelectedIconPackPackage(): String? = uiPreferences.getSelectedIconPackPackage()

    fun setSelectedIconPackPackage(packageName: String?) = uiPreferences.setSelectedIconPackPackage(packageName)

    fun shouldSortAppsByUsage(): Boolean = uiPreferences.shouldSortAppsByUsage()

    fun setSortAppsByUsage(sortAppsByUsage: Boolean) = uiPreferences.setSortAppsByUsage(sortAppsByUsage)

    fun isDirectSearchSetupExpanded(): Boolean = uiPreferences.isDirectSearchSetupExpanded()

    fun setDirectSearchSetupExpanded(expanded: Boolean) = uiPreferences.setDirectSearchSetupExpanded(expanded)

    fun hasSeenSearchBarWelcome(): Boolean = uiPreferences.hasSeenSearchBarWelcome()

    fun setHasSeenSearchBarWelcome(seen: Boolean) = uiPreferences.setHasSeenSearchBarWelcome(seen)

    fun getLastSeenVersionName(): String? = uiPreferences.getLastSeenVersionName()

    fun setLastSeenVersionName(versionName: String?) = uiPreferences.setLastSeenVersionName(versionName)

    fun getUsagePermissionBannerDismissCount(): Int = uiPreferences.getUsagePermissionBannerDismissCount()

    fun incrementUsagePermissionBannerDismissCount() = uiPreferences.incrementUsagePermissionBannerDismissCount()

    fun isUsagePermissionBannerSessionDismissed(): Boolean = uiPreferences.isUsagePermissionBannerSessionDismissed()

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) = uiPreferences.setUsagePermissionBannerSessionDismissed(dismissed)

    fun resetUsagePermissionBannerSessionDismissed() = uiPreferences.resetUsagePermissionBannerSessionDismissed()

    fun shouldShowUsagePermissionBanner(): Boolean = uiPreferences.shouldShowUsagePermissionBanner()

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areWebSuggestionsEnabled(): Boolean = uiPreferences.areWebSuggestionsEnabled()

    fun setWebSuggestionsEnabled(enabled: Boolean) = uiPreferences.setWebSuggestionsEnabled(enabled)

    // ============================================================================
    // Calculator Preferences
    // ============================================================================

    fun isCalculatorEnabled(): Boolean = uiPreferences.isCalculatorEnabled()

    fun setCalculatorEnabled(enabled: Boolean) = uiPreferences.setCalculatorEnabled(enabled)

    // ============================================================================
    // Usage Permission Banner Preferences
    // ============================================================================


    fun getShortcutHintBannerDismissCount(): Int = uiPreferences.getShortcutHintBannerDismissCount()

    fun incrementShortcutHintBannerDismissCount() = uiPreferences.incrementShortcutHintBannerDismissCount()

    fun isShortcutHintBannerSessionDismissed(): Boolean = uiPreferences.isShortcutHintBannerSessionDismissed()

    fun setShortcutHintBannerSessionDismissed(dismissed: Boolean) = uiPreferences.setShortcutHintBannerSessionDismissed(dismissed)

    fun resetShortcutHintBannerSessionDismissed() = uiPreferences.resetShortcutHintBannerSessionDismissed()

    fun shouldShowShortcutHintBanner(): Boolean = uiPreferences.shouldShowShortcutHintBanner()

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getSectionOrder(): List<String> = uiPreferences.getSectionOrder()

    fun setSectionOrder(order: List<String>) = uiPreferences.setSectionOrder(order)

    fun getDisabledSections(): Set<String> = uiPreferences.getDisabledSections()

    fun setDisabledSections(disabled: Set<String>) = uiPreferences.setDisabledSections(disabled)

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
