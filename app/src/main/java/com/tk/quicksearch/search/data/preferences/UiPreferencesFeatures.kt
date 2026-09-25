package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

open class UiPreferencesFeatures(context: Context) : UiPreferencesCore(context) {
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

    fun isIconPackUnsupportedIconMaskEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED, false)

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED, enabled)
    }

    fun getAppIconShape(): AppIconShape {
        val saved = prefs.getString(UiPreferences.KEY_APP_ICON_SHAPE, null)
        return saved?.let { runCatching { AppIconShape.valueOf(it) }.getOrNull() }
                ?: AppIconShape.DEFAULT
    }

    fun setAppIconShape(shape: AppIconShape) {
        prefs.edit().putString(UiPreferences.KEY_APP_ICON_SHAPE, shape.name).apply()
    }

    fun getLauncherAppIcon(): LauncherAppIcon {
        val saved = prefs.getString(UiPreferences.KEY_LAUNCHER_APP_ICON, null)
        return saved?.let { runCatching { LauncherAppIcon.valueOf(it) }.getOrNull() }
                ?: LauncherAppIcon.DEFAULT
    }

    fun setLauncherAppIcon(selection: LauncherAppIcon) {
        prefs.edit().putString(UiPreferences.KEY_LAUNCHER_APP_ICON, selection.name).apply()
    }

    fun isThemedIconsEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_THEMED_ICONS_ENABLED, false)

    fun setThemedIconsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_THEMED_ICONS_ENABLED, enabled)
    }

    fun isDeviceThemeEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_DEVICE_THEME_ENABLED, false)

    fun setDeviceThemeEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DEVICE_THEME_ENABLED, enabled)
    }

    fun isAmoledThemeEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_AMOLED_THEME_ENABLED, false)

    fun setAmoledThemeEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_AMOLED_THEME_ENABLED, enabled)
    }

    fun isWallpaperAccentEnabled(): Boolean =
            getAccentColorMode() == AccentColorMode.FROM_WALLPAPER

    fun setWallpaperAccentEnabled(enabled: Boolean) {
        setAccentColorMode(
                if (enabled) AccentColorMode.FROM_WALLPAPER else AccentColorMode.NONE,
        )
    }

    fun getAccentColorMode(): AccentColorMode =
            UiPreferences.resolveAccentColorMode(
                    savedModeName = prefs.getString(UiPreferences.KEY_ACCENT_COLOR_MODE, null),
                    wallpaperAccentEnabled = getBooleanPref(UiPreferences.KEY_WALLPAPER_ACCENT_ENABLED, true),
            )

    fun setAccentColorMode(mode: AccentColorMode) {
        prefs.edit()
                .putString(UiPreferences.KEY_ACCENT_COLOR_MODE, mode.name)
                .putBoolean(UiPreferences.KEY_WALLPAPER_ACCENT_ENABLED, mode == AccentColorMode.FROM_WALLPAPER)
                .apply()
    }

    fun getCustomAccentColorArgb(): Int =
            prefs.getInt(UiPreferences.KEY_CUSTOM_ACCENT_COLOR_ARGB, UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB)

    fun setCustomAccentColorArgb(argb: Int) {
        prefs.edit().putInt(UiPreferences.KEY_CUSTOM_ACCENT_COLOR_ARGB, argb).apply()
    }

    fun shouldShowAppLabels(): Boolean = getBooleanPref(UiPreferences.KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(show: Boolean) {
        setBooleanPref(UiPreferences.KEY_SHOW_APP_LABELS, show)
    }

    fun getPhoneAppGridColumns(): Int =
            prefs.getInt(UiPreferences.KEY_PHONE_APP_GRID_COLUMNS, UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS)

    fun setPhoneAppGridColumns(columns: Int) {
        prefs.edit().putInt(UiPreferences.KEY_PHONE_APP_GRID_COLUMNS, columns.coerceIn(4, 5)).apply()
    }

    fun getAppIconSizeStep(): Int =
            prefs.getInt(UiPreferences.KEY_APP_ICON_SIZE_STEP, UiPreferences.DEFAULT_APP_ICON_SIZE_STEP)
                    .coerceIn(UiPreferences.MIN_APP_ICON_SIZE_STEP, UiPreferences.MAX_APP_ICON_SIZE_STEP)

    fun setAppIconSizeStep(step: Int) {
        prefs
                .edit()
                .putInt(
                        UiPreferences.KEY_APP_ICON_SIZE_STEP,
                        step.coerceIn(UiPreferences.MIN_APP_ICON_SIZE_STEP, UiPreferences.MAX_APP_ICON_SIZE_STEP),
                )
                .apply()
    }

    fun isAiSearchSetupExpanded(): Boolean =
            getBooleanPref(UiPreferences.KEY_AI_SEARCH_SETUP_EXPANDED, true)

    fun setAiSearchSetupExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_AI_SEARCH_SETUP_EXPANDED, expanded)
    }

    fun isDisabledSearchEnginesExpanded(): Boolean =
            getBooleanPref(UiPreferences.KEY_DISABLED_SEARCH_ENGINES_EXPANDED, true)

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_DISABLED_SEARCH_ENGINES_EXPANDED, expanded)
    }

    fun isHomePinnedSectionExpanded(section: SearchSection): Boolean =
            getBooleanPref(
                    "${UiPreferences.KEY_HOME_PINNED_SECTION_EXPANDED_PREFIX}${section.name}",
                    true,
            )

    fun setHomePinnedSectionExpanded(
            section: SearchSection,
            expanded: Boolean,
    ) {
        setBooleanPref(
                "${UiPreferences.KEY_HOME_PINNED_SECTION_EXPANDED_PREFIX}${section.name}",
                expanded,
        )
    }

    fun isUnifiedPinnedItemsExpanded(): Boolean =
            getBooleanPref(UiPreferences.KEY_UNIFIED_PINNED_ITEMS_EXPANDED, true)

    fun setUnifiedPinnedItemsExpanded(expanded: Boolean) {
        setBooleanPref(UiPreferences.KEY_UNIFIED_PINNED_ITEMS_EXPANDED, expanded)
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

    fun isFuzzySearchEnabled(): Boolean =
            getBooleanPref(BasePreferences.KEY_FUZZY_SEARCH_ENABLED, true)

    fun setFuzzySearchEnabled(enabled: Boolean) {
        setBooleanPref(BasePreferences.KEY_FUZZY_SEARCH_ENABLED, enabled)
    }

    fun getSecondaryRankingSignal(): com.tk.quicksearch.search.models.SecondaryRankingSignal =
        com.tk.quicksearch.search.models.SecondaryRankingSignal.fromStorage(
            prefs.getString(BasePreferences.KEY_SECONDARY_RANKING_SIGNAL, null),
        )

    fun setSecondaryRankingSignal(signal: com.tk.quicksearch.search.models.SecondaryRankingSignal) {
        prefs.edit().putString(BasePreferences.KEY_SECONDARY_RANKING_SIGNAL, signal.name).apply()
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

    fun isAccessibilityPermissionDisclaimerPending(): Boolean =
        firstLaunchPrefs.getBoolean(
            UiPreferences.KEY_ACCESSIBILITY_PERMISSION_DISCLAIMER_PENDING,
            false,
        )

    fun setAccessibilityPermissionDisclaimerPending(pending: Boolean) {
        firstLaunchPrefs
            .edit()
            .putBoolean(UiPreferences.KEY_ACCESSIBILITY_PERMISSION_DISCLAIMER_PENDING, pending)
            .apply()
    }

    fun hasSeenAccessibilityPermissionDisclaimer(): Boolean =
        firstLaunchPrefs.getBoolean(
            UiPreferences.KEY_HAS_SEEN_ACCESSIBILITY_PERMISSION_DISCLAIMER,
            false,
        )

    fun setHasSeenAccessibilityPermissionDisclaimer(seen: Boolean) {
        firstLaunchPrefs
            .edit()
            .putBoolean(UiPreferences.KEY_HAS_SEEN_ACCESSIBILITY_PERMISSION_DISCLAIMER, seen)
            .apply()
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

    fun shouldShowAllAppsButton(): Boolean =
            getBooleanPref(UiPreferences.KEY_SHOW_ALL_APPS_BUTTON, false)

    fun setShowAllAppsButton(enabled: Boolean) {
        commitBooleanPref(UiPreferences.KEY_SHOW_ALL_APPS_BUTTON, enabled)
    }

    fun shouldIncludeNonLaunchableAppsInSearch(): Boolean =
            getBooleanPref(UiPreferences.KEY_INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH, false)

    fun setIncludeNonLaunchableAppsInSearch(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH, enabled)
    }

    fun shouldShowInRecents(): Boolean =
            getBooleanPref(UiPreferences.KEY_SHOW_IN_RECENTS, false)

    fun setShowInRecents(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_SHOW_IN_RECENTS, enabled)
    }

    fun areNotificationDotsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_NOTIFICATION_DOTS_ENABLED, false)

    fun setNotificationDotsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_NOTIFICATION_DOTS_ENABLED, enabled)
    }

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areWebSuggestionsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, DEFAULT_WEB_SUGGESTIONS_ENABLED)

    fun setWebSuggestionsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WEB_SUGGESTIONS_ENABLED, enabled)
    }

    /** Get the maximum number of web suggestions to show. Default is 3. */
    fun getWebSuggestionsCount(): Int = prefs.getInt(UiPreferences.KEY_WEB_SUGGESTIONS_COUNT, 3)

    /** Set the maximum number of web suggestions to show. */
    fun setWebSuggestionsCount(count: Int) {
        prefs.edit().putInt(UiPreferences.KEY_WEB_SUGGESTIONS_COUNT, count).apply()
    }

    fun getRecentQueriesDisplayCount(): Int =
        prefs.getInt(
            UiPreferences.KEY_RECENT_QUERIES_DISPLAY_COUNT,
            UiPreferences.DEFAULT_RECENT_QUERIES_DISPLAY_COUNT,
        ).takeIf { it in UiPreferences.RECENT_QUERIES_DISPLAY_COUNT_OPTIONS }
            ?: UiPreferences.DEFAULT_RECENT_QUERIES_DISPLAY_COUNT

    fun setRecentQueriesDisplayCount(count: Int) {
        val normalized =
            count.takeIf { it in UiPreferences.RECENT_QUERIES_DISPLAY_COUNT_OPTIONS }
                ?: UiPreferences.DEFAULT_RECENT_QUERIES_DISPLAY_COUNT
        prefs.edit().putInt(UiPreferences.KEY_RECENT_QUERIES_DISPLAY_COUNT, normalized).apply()
    }

    fun getAppResultRowCount(): Int =
        prefs.getInt(
            UiPreferences.KEY_APP_RESULT_ROW_COUNT,
            UiPreferences.DEFAULT_APP_RESULT_ROW_COUNT,
        ).takeIf { it in UiPreferences.APP_RESULT_ROW_COUNT_OPTIONS }
            ?: UiPreferences.DEFAULT_APP_RESULT_ROW_COUNT

    fun setAppResultRowCount(rowCount: Int) {
        val normalized =
            rowCount.takeIf { it in UiPreferences.APP_RESULT_ROW_COUNT_OPTIONS }
                ?: UiPreferences.DEFAULT_APP_RESULT_ROW_COUNT
        prefs.edit().putInt(UiPreferences.KEY_APP_RESULT_ROW_COUNT, normalized).apply()
    }

    // ============================================================================
    // Calculator Preferences
    // ============================================================================
}
