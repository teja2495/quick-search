package com.tk.quicksearch.data.preferences

import android.content.Context

import com.tk.quicksearch.search.core.MessagingApp

/**
 * Preferences for UI-related settings such as layout, messaging app, banners, etc.
 */
class UiPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isKeyboardAlignedLayout(): Boolean = getBooleanPref(KEY_KEYBOARD_ALIGNED_LAYOUT, false)

    fun setKeyboardAlignedLayout(enabled: Boolean) {
        setBooleanPref(KEY_KEYBOARD_ALIGNED_LAYOUT, enabled)
    }

    fun getMessagingApp(): MessagingApp {
        // Migrate from old boolean preference if it exists
        val oldKeyExists = prefs.contains(KEY_USE_WHATSAPP_FOR_MESSAGES)
        if (oldKeyExists) {
            val useWhatsApp = getBooleanPref(KEY_USE_WHATSAPP_FOR_MESSAGES, false)
            val migratedApp = if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            // Save migrated value and remove old key
            setMessagingApp(migratedApp)
            prefs.edit().remove(KEY_USE_WHATSAPP_FOR_MESSAGES).apply()
            return migratedApp
        }

        // Read new enum preference
        val appName = prefs.getString(KEY_MESSAGING_APP, null)
        return if (appName != null) {
            try {
                MessagingApp.valueOf(appName)
            } catch (e: IllegalArgumentException) {
                MessagingApp.MESSAGES
            }
        } else {
            MessagingApp.MESSAGES
        }
    }

    fun setMessagingApp(app: MessagingApp) {
        prefs.edit().putString(KEY_MESSAGING_APP, app.name).apply()
    }

    @Deprecated("Use getMessagingApp() instead", ReplaceWith("getMessagingApp()"))
    fun useWhatsAppForMessages(): Boolean {
        return getMessagingApp() == MessagingApp.WHATSAPP
    }

    @Deprecated("Use setMessagingApp() instead", ReplaceWith("setMessagingApp(MessagingApp.WHATSAPP)"))
    fun setUseWhatsAppForMessages(useWhatsApp: Boolean) {
        setMessagingApp(if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.MESSAGES)
    }

    fun isFirstLaunch(): Boolean {
        syncInstallTimeWithBackup()
        return getFirstLaunchFlag()
    }

    fun setFirstLaunchCompleted() {
        setFirstLaunchFlag(false)
        recordCurrentInstallTime()
    }

    fun shouldShowWallpaperBackground(): Boolean = getBooleanPref(KEY_SHOW_WALLPAPER_BACKGROUND, true)

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        setBooleanPref(KEY_SHOW_WALLPAPER_BACKGROUND, showWallpaper)
    }

    fun shouldClearQueryAfterSearchEngine(): Boolean = getBooleanPref(KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE, true)

    fun setClearQueryAfterSearchEngine(clearQuery: Boolean) {
        setBooleanPref(KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE, clearQuery)
    }

    fun shouldShowAllResults(): Boolean = getBooleanPref(KEY_SHOW_ALL_RESULTS, false)

    fun setShowAllResults(showAllResults: Boolean) {
        setBooleanPref(KEY_SHOW_ALL_RESULTS, showAllResults)
    }

    fun getSelectedIconPackPackage(): String? {
        return prefs.getString(KEY_SELECTED_ICON_PACK, null)
    }

    fun setSelectedIconPackPackage(packageName: String?) {
        val editor = prefs.edit()
        if (packageName.isNullOrBlank()) {
            editor.remove(KEY_SELECTED_ICON_PACK)
        } else {
            editor.putString(KEY_SELECTED_ICON_PACK, packageName)
        }
        editor.apply()
    }

    fun shouldSortAppsByUsage(): Boolean = getBooleanPref(KEY_SORT_APPS_BY_USAGE, true)

    fun setSortAppsByUsage(sortAppsByUsage: Boolean) {
        setBooleanPref(KEY_SORT_APPS_BY_USAGE, sortAppsByUsage)
    }

    fun isDirectSearchSetupExpanded(): Boolean = getBooleanPref(KEY_DIRECT_SEARCH_SETUP_EXPANDED, true)

    fun setDirectSearchSetupExpanded(expanded: Boolean) {
        setBooleanPref(KEY_DIRECT_SEARCH_SETUP_EXPANDED, expanded)
    }

    fun hasSeenSearchBarWelcome(): Boolean = getBooleanPref(KEY_HAS_SEEN_SEARCH_BAR_WELCOME, false)

    fun setHasSeenSearchBarWelcome(seen: Boolean) {
        setBooleanPref(KEY_HAS_SEEN_SEARCH_BAR_WELCOME, seen)
    }

    fun getLastSeenVersionName(): String? = prefs.getString(KEY_LAST_SEEN_VERSION, null)

    fun setLastSeenVersionName(versionName: String?) {
        val normalized = versionName?.trim()
        val editor = prefs.edit()
        if (normalized.isNullOrEmpty()) {
            editor.remove(KEY_LAST_SEEN_VERSION)
        } else {
            editor.putString(KEY_LAST_SEEN_VERSION, normalized)
        }
        editor.apply()
    }

    fun getUsagePermissionBannerDismissCount(): Int {
        return firstLaunchPrefs.getInt(KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, 0)
    }

    fun incrementUsagePermissionBannerDismissCount() {
        val currentCount = getUsagePermissionBannerDismissCount()
        firstLaunchPrefs.edit().putInt(KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, currentCount + 1).apply()
    }

    fun isUsagePermissionBannerSessionDismissed(): Boolean {
        return firstLaunchPrefs.getBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false)
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        firstLaunchPrefs.edit().putBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, dismissed).apply()
    }

    fun resetUsagePermissionBannerSessionDismissed() {
        firstLaunchPrefs.edit().putBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false).apply()
    }

    fun shouldShowUsagePermissionBanner(): Boolean {
        // Show banner if: total dismiss count < 2 AND session not dismissed
        return getUsagePermissionBannerDismissCount() < 2 && !isUsagePermissionBannerSessionDismissed()
    }

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areWebSuggestionsEnabled(): Boolean = getBooleanPref(KEY_WEB_SUGGESTIONS_ENABLED, true)

    fun setWebSuggestionsEnabled(enabled: Boolean) {
        setBooleanPref(KEY_WEB_SUGGESTIONS_ENABLED, enabled)
    }

    // ============================================================================
    // Calculator Preferences
    // ============================================================================

    fun isCalculatorEnabled(): Boolean = getBooleanPref(KEY_CALCULATOR_ENABLED, true)

    fun setCalculatorEnabled(enabled: Boolean) {
        setBooleanPref(KEY_CALCULATOR_ENABLED, enabled)
    }

    // ============================================================================
    // Shortcut hint banner preferences
    // ============================================================================

    fun getShortcutHintBannerDismissCount(): Int {
        return firstLaunchPrefs.getInt(KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT, 0)
    }

    fun incrementShortcutHintBannerDismissCount() {
        val currentCount = getShortcutHintBannerDismissCount()
        firstLaunchPrefs.edit().putInt(KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT, currentCount + 1).apply()
    }

    fun isShortcutHintBannerSessionDismissed(): Boolean {
        return firstLaunchPrefs.getBoolean(KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, false)
    }

    fun setShortcutHintBannerSessionDismissed(dismissed: Boolean) {
        firstLaunchPrefs.edit().putBoolean(KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, dismissed).apply()
    }

    fun resetShortcutHintBannerSessionDismissed() {
        firstLaunchPrefs.edit().putBoolean(KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, false).apply()
    }

    fun shouldShowShortcutHintBanner(): Boolean {
        return getShortcutHintBannerDismissCount() < 2 && !isShortcutHintBannerSessionDismissed()
    }

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getSectionOrder(): List<String> = getStringListPref(KEY_SECTION_ORDER)

    fun setSectionOrder(order: List<String>) {
        setStringListPref(KEY_SECTION_ORDER, order)
    }

    fun getDisabledSections(): Set<String> = getStringSet(KEY_DISABLED_SECTIONS)

    fun setDisabledSections(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SECTIONS, disabled).apply()
    }

    // ============================================================================
    // In-App Review Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long {
        return prefs.getLong(KEY_FIRST_APP_OPEN_TIME, 0L)
    }

    fun recordFirstAppOpenTime() {
        if (getFirstAppOpenTime() == 0L) {
            prefs.edit().putLong(KEY_FIRST_APP_OPEN_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun getLastReviewPromptTime(): Long {
        return prefs.getLong(KEY_LAST_REVIEW_PROMPT_TIME, 0L)
    }

    fun recordReviewPromptTime() {
        prefs.edit().putLong(KEY_LAST_REVIEW_PROMPT_TIME, System.currentTimeMillis()).apply()
    }

    fun getReviewPromptedCount(): Int {
        return prefs.getInt(KEY_REVIEW_PROMPTED_COUNT, 0)
    }

    fun incrementReviewPromptedCount() {
        val currentCount = getReviewPromptedCount()
        prefs.edit().putInt(KEY_REVIEW_PROMPTED_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCount(): Int {
        return prefs.getInt(KEY_APP_OPEN_COUNT, 0)
    }

    fun incrementAppOpenCount() {
        val currentCount = getAppOpenCount()
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCountAtLastPrompt(): Int {
        return prefs.getInt(KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, 0)
    }

    fun recordAppOpenCountAtPrompt() {
        val currentOpenCount = getAppOpenCount()
        prefs.edit().putInt(KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, currentOpenCount).apply()
    }

    fun shouldShowReviewPrompt(): Boolean {
        val firstOpenTime = getFirstAppOpenTime()
        val promptedCount = getReviewPromptedCount()
        val lastPromptTime = getLastReviewPromptTime()
        val totalOpens = getAppOpenCount()
        val opensAtLastPrompt = getAppOpenCountAtLastPrompt()
        
        // If never opened before, can't show review
        if (firstOpenTime == 0L) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val daysSinceFirstOpen = (currentTime - firstOpenTime) / (1000 * 60 * 60 * 24)
        
        return when (promptedCount) {
            0 -> {
                // First review: at least 5 opens AND at least 2 days
                daysSinceFirstOpen >= 2 && totalOpens >= 5
            }
            1 -> {
                if (lastPromptTime == 0L) false
                else {
                    val daysSinceLastPrompt = (currentTime - lastPromptTime) / (1000 * 60 * 60 * 24)
                    val opensSinceLastPrompt = totalOpens - opensAtLastPrompt
                    // Second review: at least 4 days AND at least 5 more opens
                    daysSinceLastPrompt >= 4 && opensSinceLastPrompt >= 5
                }
            }
            else -> false  // Never show after 2 prompts
        }
    }
}
