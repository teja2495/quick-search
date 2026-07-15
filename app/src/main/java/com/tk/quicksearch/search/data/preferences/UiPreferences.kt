package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

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

    fun isUnifiedPinnedItemsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_UNIFIED_PINNED_ITEMS_ENABLED, false)

    fun setUnifiedPinnedItemsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_UNIFIED_PINNED_ITEMS_ENABLED, enabled)
    }

    fun isSearchHintsEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_SEARCH_HINTS_ENABLED, true)

    fun setSearchHintsEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_SEARCH_HINTS_ENABLED, enabled)
    }

    fun isSettingsIconEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_SETTINGS_ICON_ENABLED, true)

    fun setSettingsIconEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_SETTINGS_ICON_ENABLED, enabled)
    }

    fun isOpenKeyboardOnLaunchEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, true)

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, enabled)
    }

    /**
     * Last measured soft-keyboard height (in dp) per orientation. Used to reserve the
     * keyboard space in the overlay from the first frame so the surface does not visibly
     * resize when the (deferred) keyboard animates in. 0f means "not measured yet".
     */
    fun getReservedKeyboardHeightDp(isLandscape: Boolean): Float =
            prefs.getFloat(
                    if (isLandscape) UiPreferences.KEY_RESERVED_KEYBOARD_HEIGHT_LANDSCAPE
                    else UiPreferences.KEY_RESERVED_KEYBOARD_HEIGHT_PORTRAIT,
                    0f,
            )

    fun setReservedKeyboardHeightDp(isLandscape: Boolean, dp: Float) {
        if (dp <= 0f) return
        prefs.edit()
                .putFloat(
                        if (isLandscape) UiPreferences.KEY_RESERVED_KEYBOARD_HEIGHT_LANDSCAPE
                        else UiPreferences.KEY_RESERVED_KEYBOARD_HEIGHT_PORTRAIT,
                        dp,
                )
                .apply()
    }

    fun applyDefaultLauncherPreferencesIfNeeded(isDefaultLauncher: Boolean): Boolean {
        val wasDefaultLauncher = prefs.getBoolean(KEY_WAS_DEFAULT_LAUNCHER, false)
        if (!isDefaultLauncher) {
            return restoreDefaultLauncherPreferencesIfNeeded(wasDefaultLauncher)
        }

        if (wasDefaultLauncher) return false

        val currentBottomSearchBarEnabled = isBottomSearchBarEnabled()
        val currentOpenKeyboardOnLaunch = isOpenKeyboardOnLaunchEnabled()
        val currentEnabledTabs = getEnabledAppSuggestionTabs()
        val autoEnabledTabs = currentEnabledTabs

        val editor =
            prefs.edit()
            .putBoolean(KEY_WAS_DEFAULT_LAUNCHER, true)
            .putBoolean(
                KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED,
                currentBottomSearchBarEnabled,
            )
            .putBoolean(
                KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH,
                currentOpenKeyboardOnLaunch,
            )
            .putBoolean(KEY_BOTTOM_SEARCH_BAR_ENABLED, true)
            .putBoolean(KEY_OPEN_KEYBOARD_ON_LAUNCH, false)
            .putStringSet(
                KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS,
                currentEnabledTabs.map { it.name }.toSet(),
            )
            .putStringSet(
                KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS,
                autoEnabledTabs.map { it.name }.toSet(),
            )
        persistEnabledAppSuggestionTabs(editor, autoEnabledTabs)
        editor.apply()
        return true
    }

    private fun restoreDefaultLauncherPreferencesIfNeeded(wasDefaultLauncher: Boolean): Boolean {
        if (!wasDefaultLauncher) return false

        val editor =
            prefs.edit()
                .putBoolean(KEY_WAS_DEFAULT_LAUNCHER, false)
                .remove(KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED)
                .remove(KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH)
                .remove(KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS)
                .remove(KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS)

        var restoredAny = false
        if (
            isBottomSearchBarEnabled() == true &&
                prefs.contains(KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED)
        ) {
            editor.putBoolean(
                KEY_BOTTOM_SEARCH_BAR_ENABLED,
                prefs.getBoolean(KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED, false),
            )
            restoredAny = true
        }
        if (
            isOpenKeyboardOnLaunchEnabled() == false &&
                prefs.contains(KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH)
        ) {
            editor.putBoolean(
                KEY_OPEN_KEYBOARD_ON_LAUNCH,
                prefs.getBoolean(KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH, true),
            )
            restoredAny = true
        }
        val previousEnabledTabsRaw =
            prefs.getStringSet(KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS, null)
        val autoEnabledTabsRaw =
            prefs.getStringSet(KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS, null)
        if (previousEnabledTabsRaw != null && autoEnabledTabsRaw != null) {
            val currentEnabledTabs = getEnabledAppSuggestionTabs()
            val autoEnabledTabs = AppSuggestionTabType.parseEnabledTabs(autoEnabledTabsRaw)
            if (currentEnabledTabs == autoEnabledTabs) {
                persistEnabledAppSuggestionTabs(
                    editor,
                    AppSuggestionTabType.parseEnabledTabs(previousEnabledTabsRaw),
                )
                restoredAny = true
            }
        }
        editor.apply()
        return restoredAny
    }

    fun isTopResultIndicatorEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, true)

    fun setTopResultIndicatorEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, enabled)
    }

    fun isTopMatchesEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_TOP_MATCHES_ENABLED, false)

    fun setTopMatchesEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_TOP_MATCHES_ENABLED, enabled)
    }

    fun getTopMatchesLimit(): Int =
            prefs.getInt(UiPreferences.KEY_TOP_MATCHES_LIMIT, UiPreferences.DEFAULT_TOP_MATCHES_LIMIT)
                    .takeIf { it in UiPreferences.TOP_MATCHES_LIMIT_OPTIONS }
                    ?: UiPreferences.DEFAULT_TOP_MATCHES_LIMIT

    fun setTopMatchesLimit(limit: Int) {
        prefs.edit()
                .putInt(
                        UiPreferences.KEY_TOP_MATCHES_LIMIT,
                        if (limit in UiPreferences.TOP_MATCHES_LIMIT_OPTIONS) {
                            limit
                        } else {
                            UiPreferences.DEFAULT_TOP_MATCHES_LIMIT
                        },
                )
                .apply()
    }

    fun getTopMatchesSectionOrder(): List<SearchSection> {
        val savedOrder =
                prefs.getString(UiPreferences.KEY_TOP_MATCHES_SECTION_ORDER, null)
                        ?.split(UiPreferences.TOP_MATCHES_SECTION_ORDER_SEPARATOR)
                        ?.mapNotNull { name ->
                            runCatching { SearchSection.valueOf(name) }.getOrNull()
                        }
                        .orEmpty()
        val defaultOrder = UiPreferences.DEFAULT_TOP_MATCHES_SECTION_ORDER
        return (savedOrder + defaultOrder)
                .distinct()
                .filter { section -> section in defaultOrder }
    }

    fun setTopMatchesSectionOrder(order: List<SearchSection>) {
        val normalized =
                (order + UiPreferences.DEFAULT_TOP_MATCHES_SECTION_ORDER)
                        .distinct()
                        .filter { section -> section in UiPreferences.DEFAULT_TOP_MATCHES_SECTION_ORDER }
        prefs.edit()
                .putString(
                        UiPreferences.KEY_TOP_MATCHES_SECTION_ORDER,
                        normalized.joinToString(UiPreferences.TOP_MATCHES_SECTION_ORDER_SEPARATOR) { it.name },
                )
                .apply()
    }

    fun getDisabledTopMatchesSections(): Set<SearchSection> =
            getStringSet(UiPreferences.KEY_DISABLED_TOP_MATCHES_SECTIONS)
                    .mapNotNull { name -> runCatching { SearchSection.valueOf(name) }.getOrNull() }
                    .filter { section -> section in UiPreferences.DEFAULT_TOP_MATCHES_SECTION_ORDER }
                    .toSet()

    fun setDisabledTopMatchesSections(disabledSections: Set<SearchSection>) {
        prefs.edit()
                .putStringSet(
                        UiPreferences.KEY_DISABLED_TOP_MATCHES_SECTIONS,
                        disabledSections
                                .filter { section -> section in UiPreferences.DEFAULT_TOP_MATCHES_SECTION_ORDER }
                                .map { it.name }
                                .toSet(),
                )
                .apply()
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
        return getFirstLaunchFlag().also { BootstrapPreferences.setFirstLaunch(context, it) }
    }

    fun setFirstLaunchCompleted() {
        setFirstLaunchFlag(false)
        BootstrapPreferences.setFirstLaunch(context, false)
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

    fun shouldUseSystemFont(): Boolean = getBooleanPref(UiPreferences.KEY_USE_SYSTEM_FONT, false)

    fun setUseSystemFont(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_USE_SYSTEM_FONT, enabled)
    }

    fun getAppLanguageTag(): String? = prefs.getString(BasePreferences.KEY_APP_LANGUAGE_TAG, null)?.takeIf { it.isNotBlank() }

    fun setAppLanguageTag(languageTag: String?) {
        prefs.edit().putString(BasePreferences.KEY_APP_LANGUAGE_TAG, languageTag?.takeIf { it.isNotBlank() }).apply()
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

    fun isIconPackUnsupportedIconMaskEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED, false)

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED, enabled)
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
                ?: LauncherAppIcon.DEFAULT
    }

    fun setLauncherAppIcon(selection: LauncherAppIcon) {
        prefs.edit().putString(KEY_LAUNCHER_APP_ICON, selection.name).apply()
    }

    fun isThemedIconsEnabled(): Boolean = getBooleanPref(KEY_THEMED_ICONS_ENABLED, false)

    fun setThemedIconsEnabled(enabled: Boolean) {
        setBooleanPref(KEY_THEMED_ICONS_ENABLED, enabled)
    }

    fun isDeviceThemeEnabled(): Boolean = getBooleanPref(KEY_DEVICE_THEME_ENABLED, false)

    fun setDeviceThemeEnabled(enabled: Boolean) {
        setBooleanPref(KEY_DEVICE_THEME_ENABLED, enabled)
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

    fun getAppIconSizeStep(): Int =
            prefs.getInt(KEY_APP_ICON_SIZE_STEP, DEFAULT_APP_ICON_SIZE_STEP)
                    .coerceIn(MIN_APP_ICON_SIZE_STEP, MAX_APP_ICON_SIZE_STEP)

    fun setAppIconSizeStep(step: Int) {
        prefs
                .edit()
                .putInt(
                        KEY_APP_ICON_SIZE_STEP,
                        step.coerceIn(MIN_APP_ICON_SIZE_STEP, MAX_APP_ICON_SIZE_STEP),
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

    fun getSelectedAppSuggestionTab(): AppSuggestionTabType {
        val raw = prefs.getString(UiPreferences.KEY_SELECTED_APP_SUGGESTION_TAB, null)
        return raw
            ?.let { value -> runCatching { AppSuggestionTabType.valueOf(value) }.getOrNull() }
            ?: AppSuggestionTabType.RECENTS
    }

    fun setSelectedAppSuggestionTab(tab: AppSuggestionTabType) {
        prefs.edit().putString(UiPreferences.KEY_SELECTED_APP_SUGGESTION_TAB, tab.name).apply()
    }

    fun getEnabledAppSuggestionTabs(): Set<AppSuggestionTabType> {
        val raw = prefs.getStringSet(UiPreferences.KEY_ENABLED_APP_SUGGESTION_TABS, null)
        return AppSuggestionTabType.parseEnabledTabs(raw)
    }

    fun setEnabledAppSuggestionTabs(tabs: Set<AppSuggestionTabType>) {
        prefs.edit().also { editor -> persistEnabledAppSuggestionTabs(editor, tabs) }.apply()
    }

    private fun persistEnabledAppSuggestionTabs(
        editor: android.content.SharedPreferences.Editor,
        tabs: Set<AppSuggestionTabType>,
    ) {
        val normalizedTabs =
            tabs.ifEmpty { AppSuggestionTabType.DefaultEnabledTabs }
        editor.putStringSet(
            UiPreferences.KEY_ENABLED_APP_SUGGESTION_TABS,
            normalizedTabs.map { it.name }.toSet(),
        )
        val selectedTab = getSelectedAppSuggestionTab()
        if (selectedTab !in normalizedTabs && selectedTab != AppSuggestionTabType.PINNED) {
            editor.putString(
                UiPreferences.KEY_SELECTED_APP_SUGGESTION_TAB,
                normalizedTabs.firstOrNull()?.name ?: AppSuggestionTabType.RECENTS.name,
            )
        }
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

    fun setCurrencyConverterModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit()
            .putString(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL, normalized)
            .remove(UiPreferences.LEGACY_KEY_CURRENCY_CONVERTER_MODEL_PREF)
            .apply()
    }

    fun getCurrencyConverterProviderId(): AiSearchLlmProviderId =
        AiSearchLlmProviderId.fromStorageValue(
            prefs.getString(UiPreferences.KEY_CURRENCY_CONVERTER_PROVIDER_ID, null),
        )

    fun setCurrencyConverterProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit()
            .putString(UiPreferences.KEY_CURRENCY_CONVERTER_PROVIDER_ID, providerId.storageValue)
            .apply()
    }

    fun isCurrencyConverterGroundingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_GROUNDING_ENABLED, true)

    fun setCurrencyConverterGroundingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_GROUNDING_ENABLED, enabled)
    }

    fun isCurrencyConverterThinkingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_THINKING_ENABLED, false)

    fun setCurrencyConverterThinkingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_CURRENCY_CONVERTER_THINKING_ENABLED, enabled)
    }

    fun getCurrencyConverterAdvancedPayload(): Pair<Boolean, String> =
        getAdvancedPayload(UiPreferences.KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD, UiPreferences.KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD_ENABLED)

    fun setCurrencyConverterAdvancedPayload(payload: String?, enabled: Boolean) =
        setAdvancedPayload(UiPreferences.KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD, UiPreferences.KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD_ENABLED, payload, enabled)

    fun getWordClockModel(): String =
        prefs.getString(UiPreferences.KEY_WORD_CLOCK_MODEL, null).orEmpty().ifBlank {
            getCurrencyConverterModel()
        }

    fun setWordClockModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit().putString(UiPreferences.KEY_WORD_CLOCK_MODEL, normalized).apply()
    }

    fun getWordClockProviderId(): AiSearchLlmProviderId =
        AiSearchLlmProviderId.fromStorageValue(
            prefs.getString(UiPreferences.KEY_WORD_CLOCK_PROVIDER_ID, null),
        )

    fun setWordClockProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(UiPreferences.KEY_WORD_CLOCK_PROVIDER_ID, providerId.storageValue).apply()
    }

    fun isWordClockGroundingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_WORD_CLOCK_GROUNDING_ENABLED, true)

    fun setWordClockGroundingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_GROUNDING_ENABLED, enabled)
    }

    fun isWordClockThinkingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_WORD_CLOCK_THINKING_ENABLED, false)

    fun setWordClockThinkingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_THINKING_ENABLED, enabled)
    }

    fun getWordClockAdvancedPayload(): Pair<Boolean, String> =
        getAdvancedPayload(UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD, UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD_ENABLED)

    fun setWordClockAdvancedPayload(payload: String?, enabled: Boolean) =
        setAdvancedPayload(UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD, UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD_ENABLED, payload, enabled)

    fun getDictionaryModel(): String =
        prefs.getString(UiPreferences.KEY_DICTIONARY_MODEL, null).orEmpty().ifBlank {
            getCurrencyConverterModel()
        }

    fun setDictionaryModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit().putString(UiPreferences.KEY_DICTIONARY_MODEL, normalized).apply()
    }

    fun getDictionaryProviderId(): AiSearchLlmProviderId =
        AiSearchLlmProviderId.fromStorageValue(
            prefs.getString(UiPreferences.KEY_DICTIONARY_PROVIDER_ID, null),
        )

    fun setDictionaryProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(UiPreferences.KEY_DICTIONARY_PROVIDER_ID, providerId.storageValue).apply()
    }

    fun isDictionaryGroundingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_DICTIONARY_GROUNDING_ENABLED, false)

    fun setDictionaryGroundingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DICTIONARY_GROUNDING_ENABLED, enabled)
    }

    fun getDictionaryAdvancedPayload(): Pair<Boolean, String> =
        getAdvancedPayload(UiPreferences.KEY_DICTIONARY_ADVANCED_PAYLOAD, UiPreferences.KEY_DICTIONARY_ADVANCED_PAYLOAD_ENABLED)

    fun setDictionaryAdvancedPayload(payload: String?, enabled: Boolean) =
        setAdvancedPayload(UiPreferences.KEY_DICTIONARY_ADVANCED_PAYLOAD, UiPreferences.KEY_DICTIONARY_ADVANCED_PAYLOAD_ENABLED, payload, enabled)

    private fun getAdvancedPayload(payloadKey: String, enabledKey: String): Pair<Boolean, String> =
        getBooleanPref(enabledKey, false) to prefs.getString(payloadKey, "").orEmpty()

    private fun setAdvancedPayload(payloadKey: String, enabledKey: String, payload: String?, enabled: Boolean) {
        val normalized = payload?.trim().orEmpty()
        prefs.edit().putString(payloadKey, normalized).putBoolean(enabledKey, enabled && normalized.isNotEmpty()).apply()
    }

    fun isDictionaryThinkingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_DICTIONARY_THINKING_ENABLED, false)

    fun setDictionaryThinkingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DICTIONARY_THINKING_ENABLED, enabled)
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
    // Rate Quick Search Prompt Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long = timingPrefs.getLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, 0L)

    fun recordFirstAppOpenTime() {
        if (getFirstAppOpenTime() != 0L) return

        timingPrefs
            .edit()
            .putLong(UiPreferences.KEY_FIRST_APP_OPEN_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getAppOpenCount(): Int = timingPrefs.getInt(UiPreferences.KEY_APP_OPEN_COUNT, 0)

    fun incrementAppOpenCount() {
        timingPrefs
            .edit()
            .putInt(UiPreferences.KEY_APP_OPEN_COUNT, getAppOpenCount() + 1)
            .apply()
    }

    fun hasCompletedRateQuickSearch(): Boolean =
        timingPrefs.getBoolean(UiPreferences.KEY_RATE_QUICK_SEARCH_COMPLETED, false)

    fun markRateQuickSearchCompleted() {
        timingPrefs
            .edit()
            .putBoolean(UiPreferences.KEY_RATE_QUICK_SEARCH_COMPLETED, true)
            .apply()
    }

    fun getRateQuickSearchLastDismissedAt(): Long =
        timingPrefs.getLong(UiPreferences.KEY_RATE_QUICK_SEARCH_LAST_DISMISSED_AT, 0L)

    fun getRateQuickSearchDismissCount(): Int =
        timingPrefs.getInt(UiPreferences.KEY_RATE_QUICK_SEARCH_DISMISS_COUNT, 0)

    fun recordRateQuickSearchDismissed() {
        timingPrefs
            .edit()
            .putLong(
                UiPreferences.KEY_RATE_QUICK_SEARCH_LAST_DISMISSED_AT,
                System.currentTimeMillis(),
            ).putInt(
                UiPreferences.KEY_RATE_QUICK_SEARCH_DISMISS_COUNT,
                getRateQuickSearchDismissCount() + 1,
            ).apply()
    }

    fun shouldShowRateQuickSearchCard(): Boolean {
        if (!RATE_QUICK_SEARCH_ENABLED) return false
        if (hasCompletedRateQuickSearch()) return false
        if (getRateQuickSearchDismissCount() >= RATE_QUICK_SEARCH_MAX_DISMISS_COUNT) return false

        val firstOpenTime = getFirstAppOpenTime()
        if (firstOpenTime == 0L) return false
        if (getAppOpenCount() < RATE_QUICK_SEARCH_MIN_OPEN_COUNT) return false

        val now = System.currentTimeMillis()
        val daysSinceFirstOpen = (now - firstOpenTime) / DAY_IN_MILLIS
        if (daysSinceFirstOpen < RATE_QUICK_SEARCH_MIN_DAYS_USED) return false

        val lastDismissedAt = getRateQuickSearchLastDismissedAt()
        return lastDismissedAt == 0L || now - lastDismissedAt >= RATE_QUICK_SEARCH_DISMISS_COOLDOWN_MS
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
        const val KEY_UNIFIED_PINNED_ITEMS_ENABLED = "unified_pinned_items_enabled"
        const val KEY_SEARCH_HINTS_ENABLED = "search_hints_enabled"
        const val KEY_SETTINGS_ICON_ENABLED = "settings_icon_enabled"
        const val KEY_OPEN_KEYBOARD_ON_LAUNCH = "open_keyboard_on_launch"
        const val KEY_RESERVED_KEYBOARD_HEIGHT_PORTRAIT = "reserved_keyboard_height_portrait"
        const val KEY_RESERVED_KEYBOARD_HEIGHT_LANDSCAPE = "reserved_keyboard_height_landscape"
        const val KEY_WAS_DEFAULT_LAUNCHER = "was_default_launcher"
        const val KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED =
                "default_launcher_previous_bottom_search_bar_enabled"
        const val KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH =
                "default_launcher_previous_open_keyboard_on_launch"
        const val KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS =
            "default_launcher_previous_enabled_app_suggestion_tabs"
        const val KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS =
            "default_launcher_auto_enabled_app_suggestion_tabs"
        const val KEY_TOP_RESULT_INDICATOR_ENABLED = "top_result_indicator_enabled"
        const val KEY_TOP_MATCHES_ENABLED = "top_matches_enabled"
        const val KEY_TOP_MATCHES_LIMIT = "top_matches_limit"
        const val KEY_TOP_MATCHES_SECTION_ORDER = "top_matches_section_order"
        const val KEY_DISABLED_TOP_MATCHES_SECTIONS = "disabled_top_matches_sections"
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
        const val KEY_USE_SYSTEM_FONT = "use_system_font"
        const val KEY_BACKGROUND_SOURCE = "background_source"
        const val KEY_CUSTOM_IMAGE_URI = "custom_image_uri"
        const val KEY_SHOW_WALLPAPER_BACKGROUND = "show_wallpaper_background" // Legacy only.
        const val KEY_OVERLAY_BACKGROUND_SOURCE = "overlay_background_source" // Legacy only.
        const val KEY_OVERLAY_CUSTOM_IMAGE_URI = "overlay_custom_image_uri" // Legacy only.
        const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        const val KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED =
                "icon_pack_unsupported_icon_mask_enabled"
        const val KEY_APP_ICON_SHAPE = "app_icon_shape"
        const val KEY_LAUNCHER_APP_ICON = "launcher_app_icon"
        const val KEY_THEMED_ICONS_ENABLED = "themed_icons_enabled"
        const val KEY_DEVICE_THEME_ENABLED = "device_theme_enabled"
        const val KEY_WALLPAPER_ACCENT_ENABLED = "wallpaper_accent_enabled"
        const val KEY_SHOW_APP_LABELS = "show_app_labels"
        const val KEY_PHONE_APP_GRID_COLUMNS = "phone_app_grid_columns"
        const val KEY_APP_ICON_SIZE_STEP = "app_icon_size_step"
        const val DEFAULT_PHONE_APP_GRID_COLUMNS = 4
        const val MIN_APP_ICON_SIZE_STEP = 0
        const val MAX_APP_ICON_SIZE_STEP = 10
        // Keep the existing default icon rendering while expressing it as 80%.
        const val DEFAULT_APP_ICON_SIZE_STEP = 6
        private const val APP_ICON_SIZE_PERCENT_DELTA = 5
        private const val MIN_APP_ICON_SIZE_PERCENT = 50
        private const val DEFAULT_APP_ICON_SIZE_PERCENT = 80
        private const val MAX_APP_ICON_SIZE_PERCENT = 100
        const val DEFAULT_TOP_MATCHES_LIMIT = 3
        val TOP_MATCHES_LIMIT_OPTIONS = listOf(1, 3, 5, 7, 10)
        val DEFAULT_TOP_MATCHES_SECTION_ORDER: List<SearchSection>
            get() = SearchSectionRegistry.orderedSections
        const val TOP_MATCHES_SECTION_ORDER_SEPARATOR = ","
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        const val KEY_AI_SEARCH_SETUP_EXPANDED = "direct_search_setup_expanded"
        const val KEY_DISABLED_SEARCH_ENGINES_EXPANDED = "disabled_search_engines_expanded"
        const val KEY_HOME_PINNED_SECTION_EXPANDED_PREFIX = "home_pinned_section_expanded_"
        const val KEY_UNIFIED_PINNED_ITEMS_EXPANDED = "unified_pinned_items_expanded"
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
        const val KEY_SHOW_ALL_APPS_BUTTON = "show_all_apps_button"
        const val KEY_INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH =
            "include_non_launchable_apps_in_search"
        const val KEY_SELECTED_APP_SUGGESTION_TAB = "selected_app_suggestion_tab"
        const val KEY_ENABLED_APP_SUGGESTION_TABS = "enabled_app_suggestion_tabs"

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
        const val KEY_WORD_CLOCK_MODEL = "word_clock_model"
        const val KEY_DICTIONARY_MODEL = "dictionary_model"
        const val KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD = "currency_converter_advanced_payload"
        const val KEY_CURRENCY_CONVERTER_ADVANCED_PAYLOAD_ENABLED = "currency_converter_advanced_payload_enabled"
        const val KEY_WORD_CLOCK_ADVANCED_PAYLOAD = "word_clock_advanced_payload"
        const val KEY_WORD_CLOCK_ADVANCED_PAYLOAD_ENABLED = "word_clock_advanced_payload_enabled"
        const val KEY_DICTIONARY_ADVANCED_PAYLOAD = "dictionary_advanced_payload"
        const val KEY_DICTIONARY_ADVANCED_PAYLOAD_ENABLED = "dictionary_advanced_payload_enabled"
        const val KEY_CURRENCY_CONVERTER_PROVIDER_ID = "currency_converter_provider_id"
        const val KEY_WORD_CLOCK_PROVIDER_ID = "word_clock_provider_id"
        const val KEY_DICTIONARY_PROVIDER_ID = "dictionary_provider_id"
        const val KEY_CURRENCY_CONVERTER_GROUNDING_ENABLED = "currency_converter_grounding_enabled"
        const val KEY_CURRENCY_CONVERTER_THINKING_ENABLED = "currency_converter_thinking_enabled"
        const val KEY_WORD_CLOCK_GROUNDING_ENABLED = "word_clock_grounding_enabled"
        const val KEY_WORD_CLOCK_THINKING_ENABLED = "word_clock_thinking_enabled"
        const val KEY_DICTIONARY_GROUNDING_ENABLED = "dictionary_grounding_enabled"
        const val KEY_DICTIONARY_THINKING_ENABLED = "dictionary_thinking_enabled"
        /** Previous preference key; migrated automatically on read/write. */
        const val LEGACY_KEY_CURRENCY_CONVERTER_MODEL_PREF = "currency_converter_gemini_model"

        // Rate Quick Search prompt keys
        const val KEY_FIRST_APP_OPEN_TIME = "first_app_open_time"
        const val KEY_APP_OPEN_COUNT = "app_open_count"
        const val KEY_RATE_QUICK_SEARCH_LAST_DISMISSED_AT =
            "rate_quick_search_last_dismissed_at"
        const val KEY_RATE_QUICK_SEARCH_DISMISS_COUNT = "rate_quick_search_dismiss_count"
        const val KEY_RATE_QUICK_SEARCH_COMPLETED = "rate_quick_search_completed"

        // In-app update session tracking keys
        const val KEY_UPDATE_CHECK_SHOWN_THIS_SESSION = "update_check_shown_this_session"

        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val RATE_QUICK_SEARCH_MIN_DAYS_USED = 3L
        private const val RATE_QUICK_SEARCH_MIN_OPEN_COUNT = 6
        private const val RATE_QUICK_SEARCH_DISMISS_COOLDOWN_MS = 14 * DAY_IN_MILLIS
        private const val RATE_QUICK_SEARCH_MAX_DISMISS_COUNT = 2

        fun appIconSizeScale(step: Int): Float {
            val normalized = step.coerceIn(MIN_APP_ICON_SIZE_STEP, MAX_APP_ICON_SIZE_STEP)
            val percent =
                    MIN_APP_ICON_SIZE_PERCENT + (normalized * APP_ICON_SIZE_PERCENT_DELTA)
            return percent / DEFAULT_APP_ICON_SIZE_PERCENT.toFloat()
        }

        fun appIconSizePercent(step: Int): Int =
                MIN_APP_ICON_SIZE_PERCENT +
                        (step.coerceIn(MIN_APP_ICON_SIZE_STEP, MAX_APP_ICON_SIZE_STEP) *
                                APP_ICON_SIZE_PERCENT_DELTA)
    }
}
