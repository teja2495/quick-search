package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection

/**
 * Preferences for UI-related settings such as layout, messaging app, banners, etc.
 */
class UiPreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isOneHandedMode(): Boolean = getBooleanPref(UiPreferences.KEY_ONE_HANDED_MODE, false)

    fun setOneHandedMode(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_ONE_HANDED_MODE, enabled)
    }

    fun getMessagingApp(): MessagingApp {
        // Migrate from old boolean preference if it exists
        val oldKeyExists = prefs.contains(UiPreferences.KEY_USE_WHATSAPP_FOR_MESSAGES)
        if (oldKeyExists) {
            val useWhatsApp = getBooleanPref(UiPreferences.KEY_USE_WHATSAPP_FOR_MESSAGES, false)
            val migratedApp = if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            // Save migrated value and remove old key
            setMessagingApp(migratedApp)
            prefs.edit().remove(UiPreferences.KEY_USE_WHATSAPP_FOR_MESSAGES).apply()
            return migratedApp
        }

        // Read new enum preference
        val appName = prefs.getString(UiPreferences.KEY_MESSAGING_APP, null)
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
        prefs.edit().putString(UiPreferences.KEY_MESSAGING_APP, app.name).apply()
    }


    fun isFirstLaunch(): Boolean {
        syncInstallTimeWithBackup()
        return getFirstLaunchFlag()
    }

    fun setFirstLaunchCompleted() {
        setFirstLaunchFlag(false)
        recordCurrentInstallTime()
    }

    fun shouldShowWallpaperBackground(): Boolean = getBooleanPref(UiPreferences.KEY_SHOW_WALLPAPER_BACKGROUND, true)

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        setBooleanPref(UiPreferences.KEY_SHOW_WALLPAPER_BACKGROUND, showWallpaper)
    }

    fun getWallpaperBackgroundAlpha(): Float {
        return prefs.getFloat(
            UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA,
            UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
        )
    }

    fun setWallpaperBackgroundAlpha(alpha: Float) {
        prefs.edit()
            .putFloat(
                UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA,
                alpha.coerceIn(0f, 1f)
            )
            .apply()
    }

    fun getWallpaperBlurRadius(): Float {
        return prefs.getFloat(
            UiPreferences.KEY_WALLPAPER_BLUR_RADIUS,
            UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
        )
    }

    fun setWallpaperBlurRadius(radius: Float) {
        prefs.edit()
            .putFloat(
                UiPreferences.KEY_WALLPAPER_BLUR_RADIUS,
                radius.coerceIn(0f, MAX_WALLPAPER_BLUR_RADIUS)
            )
            .apply()
    }

    fun shouldClearQueryAfterSearchEngine(): Boolean = getBooleanPref(UiPreferences.KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE, true)

    fun setClearQueryAfterSearchEngine(clearQuery: Boolean) {
        setBooleanPref(UiPreferences.KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE, clearQuery)
    }

    fun getSelectedIconPackPackage(): String? {
        return prefs.getString(UiPreferences.KEY_SELECTED_ICON_PACK, null)
    }

    fun setSelectedIconPackPackage(packageName: String?) {
        val editor = prefs.edit()
        if (packageName.isNullOrBlank()) {
            editor.remove(UiPreferences.KEY_SELECTED_ICON_PACK)
        } else {
            editor.putString(UiPreferences.KEY_SELECTED_ICON_PACK, packageName)
        }
        editor.apply()
    }




    fun isDirectSearchSetupExpanded(): Boolean = getBooleanPref(UiPreferences.KEY_DIRECT_SEARCH_SETUP_EXPANDED, true)

    fun setDirectSearchSetupExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_DIRECT_SEARCH_SETUP_EXPANDED, expanded)
    }

    fun hasSeenSearchBarWelcome(): Boolean = getBooleanPref(UiPreferences.KEY_HAS_SEEN_SEARCH_BAR_WELCOME, false)

    fun setHasSeenSearchBarWelcome(seen: Boolean) {
        setBooleanPref(UiPreferences.KEY_HAS_SEEN_SEARCH_BAR_WELCOME, seen)
    }

    fun hasSeenContactActionHint(): Boolean =
            getBooleanPref(UiPreferences.KEY_HAS_SEEN_CONTACT_ACTION_HINT, false)

    fun setHasSeenContactActionHint(seen: Boolean) {
        setBooleanPref(UiPreferences.KEY_HAS_SEEN_CONTACT_ACTION_HINT, seen)
    }

    fun hasSeenPersonalContextHint(): Boolean =
            getBooleanPref(UiPreferences.KEY_HAS_SEEN_PERSONAL_CONTEXT_HINT, false)

    fun setHasSeenPersonalContextHint(seen: Boolean) {
        setBooleanPref(UiPreferences.KEY_HAS_SEEN_PERSONAL_CONTEXT_HINT, seen)
    }

    fun getLastSeenVersionName(): String? = prefs.getString(UiPreferences.KEY_LAST_SEEN_VERSION, null)

    fun setLastSeenVersionName(versionName: String?) {
        val normalized = versionName?.trim()
        val editor = prefs.edit()
        if (normalized.isNullOrEmpty()) {
            editor.remove(UiPreferences.KEY_LAST_SEEN_VERSION)
        } else {
            editor.putString(UiPreferences.KEY_LAST_SEEN_VERSION, normalized)
        }
        editor.apply()
    }

    fun getUsagePermissionBannerDismissCount(): Int {
        return firstLaunchPrefs.getInt(UiPreferences.KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, 0)
    }

    fun incrementUsagePermissionBannerDismissCount() {
        val currentCount = getUsagePermissionBannerDismissCount()
        firstLaunchPrefs.edit().putInt(UiPreferences.KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, currentCount + 1).apply()
    }

    fun isUsagePermissionBannerSessionDismissed(): Boolean {
        return firstLaunchPrefs.getBoolean(UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false)
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        firstLaunchPrefs.edit().putBoolean(UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, dismissed).apply()
    }

    fun resetUsagePermissionBannerSessionDismissed() {
        firstLaunchPrefs.edit().putBoolean(UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false).apply()
    }

    fun shouldShowUsagePermissionBanner(): Boolean {
        // Show banner if: total dismiss count < 2 AND session not dismissed
        return getUsagePermissionBannerDismissCount() < 2 && !isUsagePermissionBannerSessionDismissed()
    }

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areWebSuggestionsEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, true)

    fun setWebSuggestionsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, enabled)
    }

    /**
     * Get the maximum number of web suggestions to show.
     * Default is 3.
     */
    fun getWebSuggestionsCount(): Int {
        return prefs.getInt(UiPreferences.KEY_WEB_SUGGESTIONS_COUNT, 3)
    }

    /**
     * Set the maximum number of web suggestions to show.
     */
    fun setWebSuggestionsCount(count: Int) {
        prefs.edit().putInt(UiPreferences.KEY_WEB_SUGGESTIONS_COUNT, count).apply()
    }

    // ============================================================================
    // Calculator Preferences
    // ============================================================================

    fun isCalculatorEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_CALCULATOR_ENABLED, true)

    fun setCalculatorEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_CALCULATOR_ENABLED, enabled)
    }

    // ============================================================================
    // Shortcut hint banner preferences
    // ============================================================================

    fun getShortcutHintBannerDismissCount(): Int {
        return firstLaunchPrefs.getInt(UiPreferences.KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT, 0)
    }

    fun incrementShortcutHintBannerDismissCount() {
        val currentCount = getShortcutHintBannerDismissCount()
        firstLaunchPrefs.edit().putInt(UiPreferences.KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT, currentCount + 1).apply()
    }

    fun isShortcutHintBannerSessionDismissed(): Boolean {
        return firstLaunchPrefs.getBoolean(UiPreferences.KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, false)
    }

    fun setShortcutHintBannerSessionDismissed(dismissed: Boolean) {
        firstLaunchPrefs.edit().putBoolean(UiPreferences.KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, dismissed).apply()
    }

    fun resetShortcutHintBannerSessionDismissed() {
        firstLaunchPrefs.edit().putBoolean(UiPreferences.KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED, false).apply()
    }

    fun shouldShowShortcutHintBanner(): Boolean {
        return getShortcutHintBannerDismissCount() < 2 && !isShortcutHintBannerSessionDismissed()
    }

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getSectionOrder(): List<String> = getStringListPref(UiPreferences.KEY_SECTION_ORDER)

    fun setSectionOrder(order: List<String>) {
        setStringListPref(UiPreferences.KEY_SECTION_ORDER, order)
    }

    fun getDisabledSections(): Set<String> {
        if (!prefs.contains(UiPreferences.KEY_DISABLED_SECTIONS)) {
            return setOf(SearchSection.APP_SHORTCUTS.name)
        }
        return getStringSet(UiPreferences.KEY_DISABLED_SECTIONS)
    }

    fun setDisabledSections(disabled: Set<String>) {
        prefs.edit().putStringSet(UiPreferences.KEY_DISABLED_SECTIONS, disabled).apply()
    }

    // ============================================================================
    // In-App Review Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long {
        return prefs.getLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, 0L)
    }

    fun recordFirstAppOpenTime() {
        if (getFirstAppOpenTime() == 0L) {
            prefs.edit().putLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun getLastReviewPromptTime(): Long {
        return prefs.getLong(UiPreferences.KEY_LAST_REVIEW_PROMPT_TIME, 0L)
    }

    fun recordReviewPromptTime() {
        prefs.edit().putLong(UiPreferences.KEY_LAST_REVIEW_PROMPT_TIME, System.currentTimeMillis()).apply()
    }

    fun getReviewPromptedCount(): Int {
        return prefs.getInt(UiPreferences.KEY_REVIEW_PROMPTED_COUNT, 0)
    }

    fun incrementReviewPromptedCount() {
        val currentCount = getReviewPromptedCount()
        prefs.edit().putInt(UiPreferences.KEY_REVIEW_PROMPTED_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCount(): Int {
        return prefs.getInt(UiPreferences.KEY_APP_OPEN_COUNT, 0)
    }

    fun incrementAppOpenCount() {
        val currentCount = getAppOpenCount()
        prefs.edit().putInt(UiPreferences.KEY_APP_OPEN_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCountAtLastPrompt(): Int {
        return prefs.getInt(UiPreferences.KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, 0)
    }

    fun recordAppOpenCountAtPrompt() {
        val currentOpenCount = getAppOpenCount()
        prefs.edit().putInt(UiPreferences.KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, currentOpenCount).apply()
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

    // ============================================================================
    // In-App Update Session Tracking
    // ============================================================================

    /**
     * Check if an update check was performed this session.
     * This is used to avoid showing both update and review prompts in the same session.
     */
    fun hasShownUpdateCheckThisSession(): Boolean {
        return prefs.getBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, false)
    }

    /**
     * Mark that an update check was shown this session.
     */
    fun setUpdateCheckShownThisSession() {
        prefs.edit().putBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, true).apply()
    }

    /**
     * Reset the update check session flag.
     * Should be called when the app starts.
     */
    fun resetUpdateCheckSession() {
        prefs.edit().putBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, false).apply()
    }

    companion object {
        // UI preferences keys
        const val KEY_ONE_HANDED_MODE = "one_handed_mode"
        const val KEY_USE_WHATSAPP_FOR_MESSAGES = "use_whatsapp_for_messages" // Deprecated, kept for migration
        const val KEY_MESSAGING_APP = "messaging_app"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_INSTALL_TIME = "install_time"
        const val KEY_SHOW_WALLPAPER_BACKGROUND = "show_wallpaper_background"
        const val KEY_WALLPAPER_BACKGROUND_ALPHA = "wallpaper_background_alpha"
        const val KEY_WALLPAPER_BLUR_RADIUS = "wallpaper_blur_radius"
        const val KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE = "clear_query_after_search_engine"
        const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_DIRECT_SEARCH_SETUP_EXPANDED = "direct_search_setup_expanded"
        const val KEY_HAS_SEEN_SEARCH_BAR_WELCOME = "has_seen_search_bar_welcome"
        const val KEY_HAS_SEEN_CONTACT_ACTION_HINT = "has_seen_contact_action_hint"
        const val KEY_HAS_SEEN_PERSONAL_CONTEXT_HINT = "has_seen_personal_context_hint"

        // Section preferences keys
        const val KEY_SECTION_ORDER = "section_order"
        const val KEY_DISABLED_SECTIONS = "disabled_sections"

        // Amazon domain preferences keys
        const val KEY_AMAZON_DOMAIN = "amazon_domain"

        // Usage permission banner preferences keys
        const val KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT = "usage_permission_banner_dismiss_count"
        const val KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED = "usage_permission_banner_session_dismissed"

        // Shortcut hint banner preferences keys
        const val KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT = "shortcut_hint_banner_dismiss_count"
        const val KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED = "shortcut_hint_banner_session_dismissed"

        // Web search suggestions preferences keys
        const val KEY_WEB_SUGGESTIONS_ENABLED = "web_suggestions_enabled"
        const val KEY_WEB_SUGGESTIONS_COUNT = "web_suggestions_count"

        // Recent queries preferences keys
        const val KEY_RECENT_QUERIES = "recent_queries"
        const val KEY_RECENT_QUERIES_ENABLED = "recent_queries_enabled"
        const val KEY_RECENT_QUERIES_COUNT = "recent_queries_count"

        const val DEFAULT_WALLPAPER_BACKGROUND_ALPHA = 0.5f
        const val DEFAULT_WALLPAPER_BLUR_RADIUS = 20f
        const val MAX_WALLPAPER_BLUR_RADIUS = 40f

        // Calculator preferences keys
        const val KEY_CALCULATOR_ENABLED = "calculator_enabled"

        // In-app review preferences keys
        const val KEY_FIRST_APP_OPEN_TIME = "first_app_open_time"
        const val KEY_LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time"
        const val KEY_REVIEW_PROMPTED_COUNT = "review_prompted_count"
        const val KEY_APP_OPEN_COUNT = "app_open_count"
        const val KEY_APP_OPEN_COUNT_AT_LAST_PROMPT = "app_open_count_at_last_prompt"

        // In-app update session tracking keys
        const val KEY_UPDATE_CHECK_SHOWN_THIS_SESSION = "update_check_shown_this_session"
    }
}
