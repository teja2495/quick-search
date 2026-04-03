package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.AppTheme

/** Preferences for UI-related settings such as layout, messaging app, banners, etc. */
class UiPreferences(
        context: Context,
) : BasePreferences(context) {
    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isOneHandedMode(): Boolean = getBooleanPref(UiPreferences.KEY_ONE_HANDED_MODE, false)

    fun setOneHandedMode(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_ONE_HANDED_MODE, enabled)
    }

    fun isBottomSearchBarEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_BOTTOM_SEARCH_BAR_ENABLED, false)

    fun setBottomSearchBarEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_BOTTOM_SEARCH_BAR_ENABLED, enabled)
    }

    fun isOpenKeyboardOnLaunchEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, true)

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, enabled)
    }

    fun isTopResultIndicatorEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, true)

    fun setTopResultIndicatorEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, enabled)
    }

    fun isClearQueryOnLaunchEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_CLEAR_QUERY_ON_LAUNCH, true)

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_CLEAR_QUERY_ON_LAUNCH, enabled)
    }

    fun isAutoCloseOverlayEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_AUTO_CLOSE_OVERLAY, true)

    fun setAutoCloseOverlayEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_AUTO_CLOSE_OVERLAY, enabled)
    }

    fun isOverlayModeEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_OVERLAY_MODE_ENABLED, false)

    fun setOverlayModeEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_OVERLAY_MODE_ENABLED, enabled)
    }

    fun getMessagingApp(): MessagingApp {
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

    fun getCallingApp(): CallingApp {
        val appName = prefs.getString(UiPreferences.KEY_CALLING_APP, null)
        return if (appName != null) {
            try {
                CallingApp.valueOf(appName)
            } catch (e: IllegalArgumentException) {
                CallingApp.CALL
            }
        } else {
            CallingApp.CALL
        }
    }

    fun setCallingApp(app: CallingApp) {
        prefs.edit().putString(UiPreferences.KEY_CALLING_APP, app.name).apply()
    }

    fun isFirstLaunch(): Boolean {
        syncInstallTimeWithBackup()
        return getFirstLaunchFlag()
    }

    fun setFirstLaunchCompleted() {
        setFirstLaunchFlag(false)
        recordCurrentInstallTime()
    }

    fun getWallpaperBackgroundAlpha(isDarkMode: Boolean): Float =
            prefs.getFloat(
                    if (isDarkMode) UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA
                    else UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT,
                    if (isDarkMode) UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA
                    else UiPreferences.DEFAULT_WALLPAPER_BACKGROUND_ALPHA_LIGHT,
            )

    fun setWallpaperBackgroundAlpha(alpha: Float, isDarkMode: Boolean) {
        prefs.edit()
                .putFloat(
                        if (isDarkMode) UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA
                        else UiPreferences.KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT,
                        alpha.coerceIn(0f, 1f),
                )
                .apply()
    }

    fun getWallpaperBlurRadius(isDarkMode: Boolean): Float =
            prefs.getFloat(
                    if (isDarkMode) UiPreferences.KEY_WALLPAPER_BLUR_RADIUS
                    else UiPreferences.KEY_WALLPAPER_BLUR_RADIUS_LIGHT,
                    if (isDarkMode) UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS
                    else UiPreferences.DEFAULT_WALLPAPER_BLUR_RADIUS_LIGHT,
            )

    fun setWallpaperBlurRadius(radius: Float, isDarkMode: Boolean) {
        prefs.edit()
                .putFloat(
                        if (isDarkMode) UiPreferences.KEY_WALLPAPER_BLUR_RADIUS
                        else UiPreferences.KEY_WALLPAPER_BLUR_RADIUS_LIGHT,
                        radius.coerceIn(0f, MAX_WALLPAPER_BLUR_RADIUS),
                )
                .apply()
    }

    fun getAppTheme(): AppTheme {
        val saved =
                prefs.getString(KEY_APP_THEME, null)
                        ?: prefs.getString(KEY_OVERLAY_GRADIENT_THEME, DEFAULT_APP_THEME)
        return saved?.let {
            runCatching { AppTheme.valueOf(it) }.getOrNull()
        } ?: AppTheme.MONOCHROME
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit()
                .putString(KEY_APP_THEME, theme.name)
                .remove(KEY_OVERLAY_GRADIENT_THEME)
                .apply()
    }

    fun getAppThemeMode(): com.tk.quicksearch.search.core.AppThemeMode {
        val saved = prefs.getString(KEY_APP_THEME_MODE, null)
        return saved?.let {
            runCatching { com.tk.quicksearch.search.core.AppThemeMode.valueOf(it) }.getOrNull()
        } ?: com.tk.quicksearch.search.core.AppThemeMode.SYSTEM
    }

    fun setAppThemeMode(theme: com.tk.quicksearch.search.core.AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME_MODE, theme.name).apply()
    }

    fun getOverlayThemeIntensity(): Float =
            prefs.getFloat(
                    UiPreferences.KEY_OVERLAY_THEME_INTENSITY,
                    UiPreferences.DEFAULT_OVERLAY_THEME_INTENSITY,
            ).coerceIn(
                    UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                    UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
            )

    fun setOverlayThemeIntensity(intensity: Float) {
        prefs.edit()
                .putFloat(
                        UiPreferences.KEY_OVERLAY_THEME_INTENSITY,
                        intensity.coerceIn(
                                UiPreferences.MIN_OVERLAY_THEME_INTENSITY,
                                UiPreferences.MAX_OVERLAY_THEME_INTENSITY,
                        ),
                )
                .apply()
    }

    fun getFontScaleMultiplier(): Float =
            prefs.getFloat(
                    UiPreferences.KEY_FONT_SCALE_MULTIPLIER,
                    UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
            ).coerceIn(UiPreferences.MIN_FONT_SCALE_MULTIPLIER, UiPreferences.MAX_FONT_SCALE_MULTIPLIER)

    fun setFontScaleMultiplier(multiplier: Float) {
        prefs.edit()
                .putFloat(
                        UiPreferences.KEY_FONT_SCALE_MULTIPLIER,
                        multiplier.coerceIn(
                                UiPreferences.MIN_FONT_SCALE_MULTIPLIER,
                                UiPreferences.MAX_FONT_SCALE_MULTIPLIER,
                        ),
                )
                .apply()
    }

    fun getBackgroundSource(): BackgroundSource {
        val saved = prefs.getString(KEY_BACKGROUND_SOURCE, null)
        if (saved != null) {
            return runCatching { BackgroundSource.valueOf(saved) }.getOrDefault(BackgroundSource.THEME)
        }

        val defaultSource =
                if (isOverlayModeEnabled()) {
                    BackgroundSource.THEME
                } else {
                    BackgroundSource.SYSTEM_WALLPAPER
                }
        prefs.edit().putString(KEY_BACKGROUND_SOURCE, defaultSource.name).apply()
        return defaultSource
    }

    fun setBackgroundSource(source: BackgroundSource) {
        prefs.edit().putString(KEY_BACKGROUND_SOURCE, source.name).apply()
    }

    fun getCustomImageUri(): String? =
            prefs.getString(KEY_CUSTOM_IMAGE_URI, null)?.takeIf { it.isNotBlank() }

    fun setCustomImageUri(uri: String?) {
        val normalized = uri?.trim()
        val editor = prefs.edit()
        if (normalized.isNullOrEmpty()) {
            editor.remove(KEY_CUSTOM_IMAGE_URI)
        } else {
            editor.putString(KEY_CUSTOM_IMAGE_URI, normalized)
        }
        editor.apply()
    }

    fun getSelectedIconPackPackage(): String? =
            sessionPrefs.getString(UiPreferences.KEY_SELECTED_ICON_PACK, null)

    fun setSelectedIconPackPackage(packageName: String?) {
        val editor = sessionPrefs.edit()
        if (packageName.isNullOrBlank()) {
            editor.remove(UiPreferences.KEY_SELECTED_ICON_PACK)
        } else {
            editor.putString(UiPreferences.KEY_SELECTED_ICON_PACK, packageName)
        }
        editor.apply()
    }

    fun getAppIconShape(): AppIconShape {
        val saved = prefs.getString(KEY_APP_ICON_SHAPE, null)
        return saved?.let { runCatching { AppIconShape.valueOf(it) }.getOrNull() }
                ?: AppIconShape.DEFAULT
    }

    fun setAppIconShape(shape: AppIconShape) {
        prefs.edit().putString(KEY_APP_ICON_SHAPE, shape.name).apply()
    }

    fun getLauncherAppIcon(): LauncherAppIcon {
        val saved = prefs.getString(KEY_LAUNCHER_APP_ICON, null)
        return saved?.let { runCatching { LauncherAppIcon.valueOf(it) }.getOrNull() }
                ?: LauncherAppIcon.AUTO
    }

    fun setLauncherAppIcon(selection: LauncherAppIcon) {
        prefs.edit().putString(KEY_LAUNCHER_APP_ICON, selection.name).apply()
    }

    fun isThemedIconsEnabled(): Boolean = getBooleanPref(KEY_THEMED_ICONS_ENABLED, false)

    fun setThemedIconsEnabled(enabled: Boolean) {
        setBooleanPref(KEY_THEMED_ICONS_ENABLED, enabled)
    }

    fun isWallpaperAccentEnabled(): Boolean = getBooleanPref(KEY_WALLPAPER_ACCENT_ENABLED, true)

    fun setWallpaperAccentEnabled(enabled: Boolean) {
        setBooleanPref(KEY_WALLPAPER_ACCENT_ENABLED, enabled)
    }

    fun shouldShowAppLabels(): Boolean = getBooleanPref(KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(show: Boolean) {
        setBooleanPref(KEY_SHOW_APP_LABELS, show)
    }

    fun getPhoneAppGridColumns(): Int =
            prefs.getInt(KEY_PHONE_APP_GRID_COLUMNS, DEFAULT_PHONE_APP_GRID_COLUMNS)

    fun setPhoneAppGridColumns(columns: Int) {
        prefs.edit().putInt(KEY_PHONE_APP_GRID_COLUMNS, columns.coerceIn(4, 5)).apply()
    }

    fun isDirectSearchSetupExpanded(): Boolean =
            getBooleanPref(UiPreferences.KEY_DIRECT_SEARCH_SETUP_EXPANDED, true)

    fun setDirectSearchSetupExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_DIRECT_SEARCH_SETUP_EXPANDED, expanded)
    }

    fun isDisabledSearchEnginesExpanded(): Boolean =
            getBooleanPref(UiPreferences.KEY_DISABLED_SEARCH_ENGINES_EXPANDED, true)

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_DISABLED_SEARCH_ENGINES_EXPANDED, expanded)
    }

    fun isInstantStartupSurfaceEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_INSTANT_STARTUP_SURFACE_ENABLED, true)

    fun setInstantStartupSurfaceEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_INSTANT_STARTUP_SURFACE_ENABLED, enabled)
    }

    fun hasSeenSearchBarWelcome(): Boolean =
            sessionPrefs.getBoolean(UiPreferences.KEY_HAS_SEEN_SEARCH_BAR_WELCOME, false)

    fun setHasSeenSearchBarWelcome(seen: Boolean) {
        sessionPrefs.edit().putBoolean(UiPreferences.KEY_HAS_SEEN_SEARCH_BAR_WELCOME, seen).apply()
    }

    fun shouldForceSearchBarWelcomeOnNextOpen(): Boolean =
            sessionPrefs.getBoolean(
                    UiPreferences.KEY_FORCE_SEARCH_BAR_WELCOME_ON_NEXT_OPEN,
                    false,
            )

    fun setForceSearchBarWelcomeOnNextOpen(force: Boolean) {
        sessionPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_FORCE_SEARCH_BAR_WELCOME_ON_NEXT_OPEN, force)
                .apply()
    }

    fun hasSeenContactActionHint(): Boolean =
            getBooleanPref(UiPreferences.KEY_HAS_SEEN_CONTACT_ACTION_HINT, false)

    fun setHasSeenContactActionHint(seen: Boolean) {
        setBooleanPref(UiPreferences.KEY_HAS_SEEN_CONTACT_ACTION_HINT, seen)
    }

    fun hasDismissedSearchHistoryTip(): Boolean =
            firstLaunchPrefs.getBoolean(UiPreferences.KEY_SEARCH_HISTORY_TIP_DISMISSED, false)

    fun setSearchHistoryTipDismissed(dismissed: Boolean) {
        firstLaunchPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_SEARCH_HISTORY_TIP_DISMISSED, dismissed)
                .apply()
    }

    fun hasSeenOverlayAssistantTip(): Boolean =
            sessionPrefs.getBoolean(UiPreferences.KEY_HAS_SEEN_OVERLAY_ASSISTANT_TIP, false)

    fun setHasSeenOverlayAssistantTip(seen: Boolean) {
        sessionPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_HAS_SEEN_OVERLAY_ASSISTANT_TIP, seen)
                .apply()
    }

    fun hasSeenSettingsSearchTip(): Boolean =
            firstLaunchPrefs.getBoolean(UiPreferences.KEY_HAS_SEEN_SETTINGS_SEARCH_TIP, false)

    fun setHasSeenSettingsSearchTip(seen: Boolean) {
        firstLaunchPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_HAS_SEEN_SETTINGS_SEARCH_TIP, seen)
                .apply()
    }

    fun getLastSeenVersionName(): String? =
            sessionPrefs.getString(UiPreferences.KEY_LAST_SEEN_VERSION, null)

    fun setLastSeenVersionName(versionName: String?) {
        val normalized = versionName?.trim()
        val editor = sessionPrefs.edit()
        if (normalized.isNullOrEmpty()) {
            editor.remove(UiPreferences.KEY_LAST_SEEN_VERSION)
        } else {
            editor.putString(UiPreferences.KEY_LAST_SEEN_VERSION, normalized)
        }
        editor.apply()
    }

    fun getLastSeenVersionCode(): Long? =
            if (sessionPrefs.contains(UiPreferences.KEY_LAST_SEEN_VERSION_CODE)) {
                sessionPrefs.getLong(UiPreferences.KEY_LAST_SEEN_VERSION_CODE, 0L)
            } else {
                null
            }

    fun setLastSeenVersionCode(versionCode: Long) {
        sessionPrefs.edit().putLong(UiPreferences.KEY_LAST_SEEN_VERSION_CODE, versionCode).apply()
    }

    fun getUsagePermissionBannerDismissCount(): Int =
            firstLaunchPrefs.getInt(UiPreferences.KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, 0)

    fun incrementUsagePermissionBannerDismissCount() {
        val currentCount = getUsagePermissionBannerDismissCount()
        firstLaunchPrefs
                .edit()
                .putInt(UiPreferences.KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, currentCount + 1)
                .apply()
    }

    fun isUsagePermissionBannerSessionDismissed(): Boolean =
            firstLaunchPrefs.getBoolean(
                    UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED,
                    false,
            )

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        firstLaunchPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, dismissed)
                .apply()
    }

    fun resetUsagePermissionBannerSessionDismissed() {
        firstLaunchPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false)
                .apply()
    }

    fun shouldShowUsagePermissionBanner(): Boolean {
        // Show banner if: total dismiss count < 2 AND session not dismissed
        return getUsagePermissionBannerDismissCount() < 2 &&
                !isUsagePermissionBannerSessionDismissed()
    }

    // ============================================================================
    // App Suggestions Preferences
    // ============================================================================

    fun areAppSuggestionsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_APP_SUGGESTIONS_ENABLED, true)

    fun setAppSuggestionsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_APP_SUGGESTIONS_ENABLED, enabled)
    }

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areWebSuggestionsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, true)

    fun setWebSuggestionsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, enabled)
    }

    /** Get the maximum number of web suggestions to show. Default is 3. */
    fun getWebSuggestionsCount(): Int = prefs.getInt(UiPreferences.KEY_WEB_SUGGESTIONS_COUNT, 3)

    /** Set the maximum number of web suggestions to show. */
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

    fun isUnitConverterEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_UNIT_CONVERTER_ENABLED, true)

    fun setUnitConverterEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_UNIT_CONVERTER_ENABLED, enabled)
    }

    fun isDateCalculatorEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_DATE_CALCULATOR_ENABLED, true)

    fun setDateCalculatorEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DATE_CALCULATOR_ENABLED, enabled)
    }

    fun isCurrencyConverterEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_ENABLED, true)

    fun setCurrencyConverterEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_ENABLED, enabled)
    }

    fun isWordClockEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_WORD_CLOCK_ENABLED, true)

    fun setWordClockEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_ENABLED, enabled)
    }

    fun isDictionaryEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_DICTIONARY_ENABLED, true)

    fun setDictionaryEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DICTIONARY_ENABLED, enabled)
    }

    fun getCurrencyConverterModel(): String {
        val stored = prefs.getString(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL, null)
        if (stored != null) return stored
        val legacy =
                prefs.getString(UiPreferences.LEGACY_KEY_CURRENCY_CONVERTER_MODEL_PREF, "").orEmpty()
        if (legacy.isNotEmpty()) {
            prefs.edit()
                    .putString(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL, legacy)
                    .remove(UiPreferences.LEGACY_KEY_CURRENCY_CONVERTER_MODEL_PREF)
                    .apply()
        }
        return legacy
    }

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getDisabledSections(): Set<String> {
        if (!prefs.contains(UiPreferences.KEY_DISABLED_SECTIONS)) {
            return emptySet()
        }
        return getStringSet(UiPreferences.KEY_DISABLED_SECTIONS)
    }

    fun setDisabledSections(disabled: Set<String>) {
        prefs.edit().putStringSet(UiPreferences.KEY_DISABLED_SECTIONS, disabled).apply()
    }

    // ============================================================================
    // In-App Review Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long = timingPrefs.getLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, 0L)

    fun recordFirstAppOpenTime() {
        if (getFirstAppOpenTime() == 0L) {
            timingPrefs
                    .edit()
                    .putLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, System.currentTimeMillis())
                    .apply()
        }
    }

    fun getLastReviewPromptTime(): Long =
            timingPrefs.getLong(UiPreferences.KEY_LAST_REVIEW_PROMPT_TIME, 0L)

    fun recordReviewPromptTime() {
        timingPrefs
                .edit()
                .putLong(UiPreferences.KEY_LAST_REVIEW_PROMPT_TIME, System.currentTimeMillis())
                .apply()
    }

    fun getReviewPromptedCount(): Int =
            timingPrefs.getInt(UiPreferences.KEY_REVIEW_PROMPTED_COUNT, 0)

    fun incrementReviewPromptedCount() {
        val currentCount = getReviewPromptedCount()
        timingPrefs.edit().putInt(UiPreferences.KEY_REVIEW_PROMPTED_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCount(): Int = timingPrefs.getInt(UiPreferences.KEY_APP_OPEN_COUNT, 0)

    fun incrementAppOpenCount() {
        val currentCount = getAppOpenCount()
        timingPrefs.edit().putInt(UiPreferences.KEY_APP_OPEN_COUNT, currentCount + 1).apply()
    }

    fun getAppOpenCountAtLastPrompt(): Int =
            timingPrefs.getInt(UiPreferences.KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, 0)

    fun recordAppOpenCountAtPrompt() {
        val currentOpenCount = getAppOpenCount()
        timingPrefs
                .edit()
                .putInt(UiPreferences.KEY_APP_OPEN_COUNT_AT_LAST_PROMPT, currentOpenCount)
                .apply()
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
                // First review: at least 2 days AND at least 5 opens
                daysSinceFirstOpen >= 2 && totalOpens >= 5
            }
            1 -> {
                if (lastPromptTime == 0L) {
                    false
                } else {
                    val daysSinceLastPrompt = (currentTime - lastPromptTime) / (1000 * 60 * 60 * 24)
                    val opensSinceLastPrompt = totalOpens - opensAtLastPrompt
                    // Second review: at least 4 days AND at least 5 more opens
                    daysSinceLastPrompt >= 4 && opensSinceLastPrompt >= 5
                }
            }
            else -> {
                false
            } // Never show after 2 prompts
        }
    }

    // ============================================================================
    // In-App Update Session Tracking
    // ============================================================================

    /**
     * Check if an update check was performed this session. This is used to avoid showing both
     * update and review prompts in the same session.
     */
    fun hasShownUpdateCheckThisSession(): Boolean =
            sessionPrefs.getBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, false)

    /** Mark that an update check was shown this session. */
    fun setUpdateCheckShownThisSession() {
        sessionPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, true)
                .apply()
    }

    /** Reset the update check session flag. Should be called when the app starts. */
    fun resetUpdateCheckSession() {
        sessionPrefs
                .edit()
                .putBoolean(UiPreferences.KEY_UPDATE_CHECK_SHOWN_THIS_SESSION, false)
                .apply()
    }

    companion object {
        // UI preferences keys
        const val KEY_ONE_HANDED_MODE = "one_handed_mode"
        const val KEY_BOTTOM_SEARCH_BAR_ENABLED = "bottom_search_bar_enabled"
        const val KEY_OPEN_KEYBOARD_ON_LAUNCH = "open_keyboard_on_launch"
        const val KEY_TOP_RESULT_INDICATOR_ENABLED = "top_result_indicator_enabled"
        const val KEY_CLEAR_QUERY_ON_LAUNCH = "clear_query_on_launch"
        const val KEY_AUTO_CLOSE_OVERLAY = "auto_close_overlay"
        const val KEY_OVERLAY_MODE_ENABLED = "overlay_mode_enabled"
        const val KEY_USE_WHATSAPP_FOR_MESSAGES =
                "use_whatsapp_for_messages" // Deprecated, kept for migration
        const val KEY_MESSAGING_APP = "messaging_app"
        const val KEY_CALLING_APP = "calling_app"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_INSTALL_TIME = "install_time"
        const val KEY_WALLPAPER_BACKGROUND_ALPHA = "wallpaper_background_alpha"
        const val KEY_WALLPAPER_BLUR_RADIUS = "wallpaper_blur_radius"
        const val KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT = "wallpaper_background_alpha_light"
        const val KEY_WALLPAPER_BLUR_RADIUS_LIGHT = "wallpaper_blur_radius_light"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_OVERLAY_GRADIENT_THEME = "overlay_gradient_theme"
        const val KEY_APP_THEME_MODE = "app_theme_mode"
        const val KEY_OVERLAY_THEME_INTENSITY = "overlay_theme_intensity"
        const val KEY_FONT_SCALE_MULTIPLIER = "font_scale_multiplier"
        const val KEY_BACKGROUND_SOURCE = "background_source"
        const val KEY_CUSTOM_IMAGE_URI = "custom_image_uri"
        const val KEY_SHOW_WALLPAPER_BACKGROUND = "show_wallpaper_background" // Legacy only.
        const val KEY_OVERLAY_BACKGROUND_SOURCE = "overlay_background_source" // Legacy only.
        const val KEY_OVERLAY_CUSTOM_IMAGE_URI = "overlay_custom_image_uri" // Legacy only.
        const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        const val KEY_APP_ICON_SHAPE = "app_icon_shape"
        const val KEY_LAUNCHER_APP_ICON = "launcher_app_icon"
        const val KEY_THEMED_ICONS_ENABLED = "themed_icons_enabled"
        const val KEY_WALLPAPER_ACCENT_ENABLED = "wallpaper_accent_enabled"
        const val KEY_SHOW_APP_LABELS = "show_app_labels"
        const val KEY_PHONE_APP_GRID_COLUMNS = "phone_app_grid_columns"
        const val DEFAULT_PHONE_APP_GRID_COLUMNS = 4
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        const val KEY_DIRECT_SEARCH_SETUP_EXPANDED = "direct_search_setup_expanded"
        const val KEY_DISABLED_SEARCH_ENGINES_EXPANDED = "disabled_search_engines_expanded"
        const val KEY_INSTANT_STARTUP_SURFACE_ENABLED = "instant_startup_surface_v1"
        const val KEY_HAS_SEEN_SEARCH_BAR_WELCOME = "has_seen_search_bar_welcome"
        const val KEY_FORCE_SEARCH_BAR_WELCOME_ON_NEXT_OPEN =
                "force_search_bar_welcome_on_next_open"
        const val KEY_HAS_SEEN_CONTACT_ACTION_HINT = "has_seen_contact_action_hint"
        const val KEY_SEARCH_HISTORY_TIP_DISMISSED = "search_history_tip_dismissed"
        const val KEY_HAS_SEEN_OVERLAY_ASSISTANT_TIP = "has_seen_overlay_assistant_tip"
        const val KEY_HAS_SEEN_SETTINGS_SEARCH_TIP = "has_seen_settings_search_tip"
        // Section preferences keys
        const val KEY_SECTION_ORDER = "section_order"
        const val KEY_DISABLED_SECTIONS = "disabled_sections"

        // Amazon domain preferences keys
        const val KEY_AMAZON_DOMAIN = "amazon_domain"

        // Usage permission banner preferences keys
        const val KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT =
                "usage_permission_banner_dismiss_count"
        const val KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED =
                "usage_permission_banner_session_dismissed"

        // Web search suggestions preferences keys
        const val KEY_WEB_SUGGESTIONS_ENABLED = "web_suggestions_enabled"
        const val KEY_WEB_SUGGESTIONS_COUNT = "web_suggestions_count"

        // App suggestions preferences keys
        const val KEY_APP_SUGGESTIONS_ENABLED = "app_suggestions_enabled"

        // Recent queries preferences keys
        const val KEY_RECENT_QUERIES = "recent_queries"
        const val KEY_RECENT_QUERIES_ENABLED = "recent_queries_enabled"

        const val DEFAULT_WALLPAPER_BACKGROUND_ALPHA = 0.5f
        const val DEFAULT_WALLPAPER_BLUR_RADIUS = 20f
        const val DEFAULT_WALLPAPER_BACKGROUND_ALPHA_LIGHT = 0.10f
        const val DEFAULT_WALLPAPER_BLUR_RADIUS_LIGHT = 4.8f
        const val DEFAULT_APP_THEME = "MONOCHROME"
        const val DEFAULT_OVERLAY_THEME_INTENSITY = 0.5f
        const val DEFAULT_FONT_SCALE_MULTIPLIER = 1f
        const val OVERLAY_THEME_INTENSITY_STEP = 0.1f
        const val OVERLAY_THEME_INTENSITY_DELTA_STEPS = 2
        const val FONT_SCALE_MULTIPLIER_STEP = 0.05f
        const val MIN_OVERLAY_THEME_INTENSITY =
                DEFAULT_OVERLAY_THEME_INTENSITY -
                        (OVERLAY_THEME_INTENSITY_STEP * OVERLAY_THEME_INTENSITY_DELTA_STEPS)
        const val MAX_OVERLAY_THEME_INTENSITY =
                DEFAULT_OVERLAY_THEME_INTENSITY +
                        (OVERLAY_THEME_INTENSITY_STEP * OVERLAY_THEME_INTENSITY_DELTA_STEPS)
        const val MIN_FONT_SCALE_MULTIPLIER = 0.90f
        const val MAX_FONT_SCALE_MULTIPLIER = 1.05f
        const val MAX_WALLPAPER_BLUR_RADIUS = 40f

        // Calculator preferences keys
        const val KEY_CALCULATOR_ENABLED = "calculator_enabled"
        const val KEY_UNIT_CONVERTER_ENABLED = "unit_converter_enabled"
        const val KEY_DATE_CALCULATOR_ENABLED = "date_calculator_enabled"
        const val KEY_CURRENCY_CONVERTER_ENABLED = "currency_converter_enabled"
        const val KEY_WORD_CLOCK_ENABLED = "word_clock_enabled"
        const val KEY_DICTIONARY_ENABLED = "dictionary_enabled"
        const val KEY_CURRENCY_CONVERTER_MODEL = "currency_converter_model"
        /** Previous preference key; migrated automatically on read/write. */
        const val LEGACY_KEY_CURRENCY_CONVERTER_MODEL_PREF = "currency_converter_gemini_model"

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
