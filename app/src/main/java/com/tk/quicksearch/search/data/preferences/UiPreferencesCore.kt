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

open class UiPreferencesCore(context: Context) : BasePreferences(context) {

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
        val wasDefaultLauncher = prefs.getBoolean(UiPreferences.KEY_WAS_DEFAULT_LAUNCHER, false)
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
            .putBoolean(UiPreferences.KEY_WAS_DEFAULT_LAUNCHER, true)
            .putBoolean(
                UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED,
                currentBottomSearchBarEnabled,
            )
            .putBoolean(
                UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH,
                currentOpenKeyboardOnLaunch,
            )
            .putBoolean(UiPreferences.KEY_BOTTOM_SEARCH_BAR_ENABLED, true)
            .putBoolean(UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH, false)
            .putStringSet(
                UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS,
                currentEnabledTabs.map { it.name }.toSet(),
            )
            .putStringSet(
                UiPreferences.KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS,
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
                .putBoolean(UiPreferences.KEY_WAS_DEFAULT_LAUNCHER, false)
                .remove(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED)
                .remove(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH)
                .remove(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS)
                .remove(UiPreferences.KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS)

        var restoredAny = false
        if (
            isBottomSearchBarEnabled() == true &&
                prefs.contains(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED)
        ) {
            editor.putBoolean(
                UiPreferences.KEY_BOTTOM_SEARCH_BAR_ENABLED,
                prefs.getBoolean(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_BOTTOM_SEARCH_BAR_ENABLED, false),
            )
            restoredAny = true
        }
        if (
            isOpenKeyboardOnLaunchEnabled() == false &&
                prefs.contains(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH)
        ) {
            editor.putBoolean(
                UiPreferences.KEY_OPEN_KEYBOARD_ON_LAUNCH,
                prefs.getBoolean(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_OPEN_KEYBOARD_ON_LAUNCH, true),
            )
            restoredAny = true
        }
        val previousEnabledTabsRaw =
            prefs.getStringSet(UiPreferences.KEY_DEFAULT_LAUNCHER_PREVIOUS_ENABLED_APP_SUGGESTION_TABS, null)
        val autoEnabledTabsRaw =
            prefs.getStringSet(UiPreferences.KEY_DEFAULT_LAUNCHER_AUTO_ENABLED_APP_SUGGESTION_TABS, null)
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
            getBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, false)

    fun isOpenTopResultUsingKeyboardEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_OPEN_TOP_RESULT_USING_KEYBOARD_ENABLED, true)

    fun isTopResultIndicatorManuallyDisabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_MANUALLY_DISABLED, false)

    fun setTopResultIndicatorEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_ENABLED, enabled)
    }

    fun setOpenTopResultUsingKeyboardEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_OPEN_TOP_RESULT_USING_KEYBOARD_ENABLED, enabled)
    }

    fun setTopResultIndicatorManuallyDisabled(disabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_TOP_RESULT_INDICATOR_MANUALLY_DISABLED, disabled)
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
        return (withRemindersAfterCalendar(savedOrder) + defaultOrder)
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

    fun getHomePinnedSectionOrder(): List<SearchSection> {
        val defaultOrder = UiPreferences.DEFAULT_HOME_PINNED_SECTION_ORDER
        val savedOrder =
                prefs.getString(UiPreferences.KEY_HOME_PINNED_SECTION_ORDER, null)
                        ?.split(UiPreferences.TOP_MATCHES_SECTION_ORDER_SEPARATOR)
                        ?.mapNotNull { name ->
                            runCatching { SearchSection.valueOf(name) }.getOrNull()
                        }
                        .orEmpty()
        return (withRemindersAfterCalendar(savedOrder) + defaultOrder)
                .distinct()
                .filter { section -> section in defaultOrder }
    }

    /**
     * Orders saved before Reminders existed would otherwise get it appended at the end, so slot it
     * right below Calendar Events.
     */
    private fun withRemindersAfterCalendar(savedOrder: List<SearchSection>): List<SearchSection> {
        if (SearchSection.REMINDERS in savedOrder) return savedOrder
        val calendarIndex = savedOrder.indexOf(SearchSection.CALENDAR)
        if (calendarIndex == -1) return savedOrder
        return savedOrder.toMutableList().apply { add(calendarIndex + 1, SearchSection.REMINDERS) }
    }

    fun isPinnedAppShortcutsInAppGridEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_PINNED_APP_SHORTCUTS_IN_APP_GRID, false)

    fun setPinnedAppShortcutsInAppGridEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_PINNED_APP_SHORTCUTS_IN_APP_GRID, enabled)
    }

    /** Combined drag order of pinned apps and pinned shortcuts shown in the app grid. */
    fun getPinnedAppGridOrder(): List<String> =
            getStringListPref(UiPreferences.KEY_PINNED_APP_GRID_ORDER)

    fun setPinnedAppGridOrder(order: List<String>) {
        setStringListPref(UiPreferences.KEY_PINNED_APP_GRID_ORDER, order.distinct())
    }

    fun setHomePinnedSectionOrder(order: List<SearchSection>) {
        val defaultOrder = UiPreferences.DEFAULT_HOME_PINNED_SECTION_ORDER
        val normalized =
                (order + defaultOrder)
                        .distinct()
                        .filter { section -> section in defaultOrder }
        prefs.edit()
                .putString(
                        UiPreferences.KEY_HOME_PINNED_SECTION_ORDER,
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
                        radius.coerceIn(0f, UiPreferences.MAX_WALLPAPER_BLUR_RADIUS),
                )
                .apply()
    }

    fun getAppTheme(): AppTheme {
        val saved = prefs.getString(UiPreferences.KEY_APP_THEME, UiPreferences.DEFAULT_APP_THEME)
        return saved?.let {
            runCatching { AppTheme.valueOf(it) }.getOrNull()
        } ?: AppTheme.MONOCHROME
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(UiPreferences.KEY_APP_THEME, theme.name).apply()
    }

    fun getAppThemeMode(): com.tk.quicksearch.search.core.AppThemeMode {
        val saved = prefs.getString(UiPreferences.KEY_APP_THEME_MODE, null)
        return saved?.let {
            runCatching { com.tk.quicksearch.search.core.AppThemeMode.valueOf(it) }.getOrNull()
        } ?: com.tk.quicksearch.search.core.AppThemeMode.SYSTEM
    }

    fun setAppThemeMode(theme: com.tk.quicksearch.search.core.AppThemeMode) {
        prefs.edit().putString(UiPreferences.KEY_APP_THEME_MODE, theme.name).apply()
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

    fun getHomeTextColorOverride(): com.tk.quicksearch.search.core.HomeTextColor? =
            prefs.getString(UiPreferences.KEY_HOME_TEXT_COLOR_OVERRIDE, null)?.let { value ->
                runCatching { com.tk.quicksearch.search.core.HomeTextColor.valueOf(value) }.getOrNull()
            }

    fun setHomeTextColorOverride(color: com.tk.quicksearch.search.core.HomeTextColor) {
        prefs.edit().putString(UiPreferences.KEY_HOME_TEXT_COLOR_OVERRIDE, color.name).apply()
    }

    fun clearHomeTextColorOverride() {
        prefs.edit().remove(UiPreferences.KEY_HOME_TEXT_COLOR_OVERRIDE).apply()
    }

    fun getAppLanguageTag(): String? = prefs.getString(BasePreferences.KEY_APP_LANGUAGE_TAG, null)?.takeIf { it.isNotBlank() }

    fun setAppLanguageTag(languageTag: String?) {
        prefs.edit().putString(BasePreferences.KEY_APP_LANGUAGE_TAG, languageTag?.takeIf { it.isNotBlank() }).apply()
    }

    fun getBackgroundSource(): BackgroundSource {
        val saved = prefs.getString(UiPreferences.KEY_BACKGROUND_SOURCE, null)
        if (saved != null) {
            return runCatching { BackgroundSource.valueOf(saved) }.getOrDefault(BackgroundSource.THEME)
        }

        val defaultSource =
                if (isOverlayModeEnabled()) {
                    BackgroundSource.THEME
                } else {
                    BackgroundSource.SYSTEM_WALLPAPER
                }
        prefs.edit().putString(UiPreferences.KEY_BACKGROUND_SOURCE, defaultSource.name).apply()
        return defaultSource
    }

    fun setBackgroundSource(source: BackgroundSource) {
        prefs.edit().putString(UiPreferences.KEY_BACKGROUND_SOURCE, source.name).apply()
    }

    fun getCustomImageUri(): String? =
            prefs.getString(UiPreferences.KEY_CUSTOM_IMAGE_URI, null)?.takeIf { it.isNotBlank() }

    fun setCustomImageUri(uri: String?) {
        val normalized = uri?.trim()
        val editor = prefs.edit()
        if (normalized.isNullOrEmpty()) {
            editor.remove(UiPreferences.KEY_CUSTOM_IMAGE_URI)
        } else {
            editor.putString(UiPreferences.KEY_CUSTOM_IMAGE_URI, normalized)
        }
        editor.apply()
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

    protected fun persistEnabledAppSuggestionTabs(
        editor: android.content.SharedPreferences.Editor,
        tabs: Set<AppSuggestionTabType>,
    ) {
        val normalizedTabs =
            tabs
                .ifEmpty { AppSuggestionTabType.DefaultEnabledTabs }
                .toMutableSet()
                .apply { add(AppSuggestionTabType.PINNED) }
        editor.putStringSet(
            UiPreferences.KEY_ENABLED_APP_SUGGESTION_TABS,
            normalizedTabs.map { it.name }.toSet(),
        )
        val selectedTab = getSelectedAppSuggestionTab()
        if (selectedTab !in normalizedTabs) {
            editor.putString(
                UiPreferences.KEY_SELECTED_APP_SUGGESTION_TAB,
                normalizedTabs.firstOrNull()?.name ?: AppSuggestionTabType.RECENTS.name,
            )
        }
    }

}
