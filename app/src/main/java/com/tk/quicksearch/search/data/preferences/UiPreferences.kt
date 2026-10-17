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

/** Preferences for UI-related settings such as layout, messaging app, banners, etc. */
class UiPreferences(context: Context) : UiPreferencesFeatures(context) {
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

    fun isColorVisualizerEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_COLOR_VISUALIZER_ENABLED, true)

    fun setColorVisualizerEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_COLOR_VISUALIZER_ENABLED, enabled)
    }

    fun isWorldClockEnabled(): Boolean =
            getBooleanPref(UiPreferences.KEY_WORD_CLOCK_ENABLED, true)

    fun setWorldClockEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_ENABLED, enabled)
    }

    fun isDictionaryEnabled(): Boolean = getBooleanPref(UiPreferences.KEY_DICTIONARY_ENABLED, true)

    fun setDictionaryEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_DICTIONARY_ENABLED, enabled)
    }

    fun getCurrencyConverterModel(): String =
        prefs.getString(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL, "").orEmpty()

    fun setCurrencyConverterModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit()
            .putString(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL, normalized)
            .apply()
    }

    fun clearCurrencyConverterModel() {
        prefs.edit().remove(UiPreferences.KEY_CURRENCY_CONVERTER_MODEL).apply()
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

    fun getWorldClockModel(): String =
        if (prefs.contains(UiPreferences.KEY_WORD_CLOCK_MODEL)) {
            prefs.getString(UiPreferences.KEY_WORD_CLOCK_MODEL, "").orEmpty()
        } else {
            getCurrencyConverterModel()
        }

    fun hasWorldClockModelPreference(): Boolean =
        prefs.contains(UiPreferences.KEY_WORD_CLOCK_MODEL)

    fun setWorldClockModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit().putString(UiPreferences.KEY_WORD_CLOCK_MODEL, normalized).apply()
    }

    fun clearWorldClockModel() {
        prefs.edit().putString(UiPreferences.KEY_WORD_CLOCK_MODEL, "").apply()
    }

    fun getWorldClockProviderId(): AiSearchLlmProviderId =
        AiSearchLlmProviderId.fromStorageValue(
            prefs.getString(UiPreferences.KEY_WORD_CLOCK_PROVIDER_ID, null),
        )

    fun getWorldClockProviderIdOverride(): AiSearchLlmProviderId? =
        prefs.getString(UiPreferences.KEY_WORD_CLOCK_PROVIDER_ID, null)?.let(
            AiSearchLlmProviderId::fromStorageValue,
        )

    fun setWorldClockProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(UiPreferences.KEY_WORD_CLOCK_PROVIDER_ID, providerId.storageValue).apply()
    }

    fun isWorldClockGroundingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_WORD_CLOCK_GROUNDING_ENABLED, false)

    fun setWorldClockGroundingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_GROUNDING_ENABLED, enabled)
    }

    fun isWorldClockThinkingEnabled(): Boolean =
        getBooleanPref(UiPreferences.KEY_WORD_CLOCK_THINKING_ENABLED, false)

    fun getWorldClockThinkingOverride(): Boolean? =
        if (prefs.contains(UiPreferences.KEY_WORD_CLOCK_THINKING_ENABLED)) {
            isWorldClockThinkingEnabled()
        } else {
            null
        }

    fun setWorldClockThinkingEnabled(enabled: Boolean) {
        setBooleanPref(UiPreferences.KEY_WORD_CLOCK_THINKING_ENABLED, enabled)
    }

    fun getWorldClockAdvancedPayload(): Pair<Boolean, String> =
        getAdvancedPayload(UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD, UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD_ENABLED)

    fun setWorldClockAdvancedPayload(payload: String?, enabled: Boolean) =
        setAdvancedPayload(UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD, UiPreferences.KEY_WORD_CLOCK_ADVANCED_PAYLOAD_ENABLED, payload, enabled)

    fun getDictionaryModel(): String =
        if (prefs.contains(UiPreferences.KEY_DICTIONARY_MODEL)) {
            prefs.getString(UiPreferences.KEY_DICTIONARY_MODEL, "").orEmpty()
        } else {
            getCurrencyConverterModel()
        }

    fun hasDictionaryModelPreference(): Boolean =
        prefs.contains(UiPreferences.KEY_DICTIONARY_MODEL)

    fun setDictionaryModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isEmpty()) return
        prefs.edit().putString(UiPreferences.KEY_DICTIONARY_MODEL, normalized).apply()
    }

    fun clearDictionaryModel() {
        prefs.edit().putString(UiPreferences.KEY_DICTIONARY_MODEL, "").apply()
    }

    fun getDictionaryProviderId(): AiSearchLlmProviderId =
        AiSearchLlmProviderId.fromStorageValue(
            prefs.getString(UiPreferences.KEY_DICTIONARY_PROVIDER_ID, null),
        )

    fun getDictionaryProviderIdOverride(): AiSearchLlmProviderId? =
        prefs.getString(UiPreferences.KEY_DICTIONARY_PROVIDER_ID, null)?.let(
            AiSearchLlmProviderId::fromStorageValue,
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

    fun getDictionaryThinkingOverride(): Boolean? =
        if (prefs.contains(UiPreferences.KEY_DICTIONARY_THINKING_ENABLED)) {
            isDictionaryThinkingEnabled()
        } else {
            null
        }

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
    // In-App Review Preferences
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

    fun getInAppReviewLastRequestedAt(): Long =
        timingPrefs.getLong(UiPreferences.KEY_IN_APP_REVIEW_LAST_REQUESTED_AT, 0L)

    fun recordInAppReviewRequested() {
        timingPrefs
            .edit()
            .putLong(UiPreferences.KEY_IN_APP_REVIEW_LAST_REQUESTED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Whether an established user may be asked for an in-app review. Google decides whether the
     * review sheet actually appears, so a request is recorded whether or not it was shown.
     */
    fun shouldRequestInAppReview(totalAppLaunchCount: Int): Boolean {
        if (!RATE_QUICK_SEARCH_ENABLED) return false

        val firstOpenTime = getFirstAppOpenTime()
        if (firstOpenTime == 0L) return false
        if (totalAppLaunchCount < IN_APP_REVIEW_MIN_APP_LAUNCHES) return false

        val now = System.currentTimeMillis()
        if (now - firstOpenTime < IN_APP_REVIEW_MIN_DAYS_USED * DAY_IN_MILLIS) return false

        val lastRequestedAt = getInAppReviewLastRequestedAt()
        return lastRequestedAt == 0L || now - lastRequestedAt >= IN_APP_REVIEW_COOLDOWN_MS
    }

    // ============================================================================
    // App Update Prompt Preferences
    // ============================================================================

    fun shouldShowUpdateCard(): Boolean {
        val lastDismissedAt = timingPrefs.getLong(UiPreferences.KEY_UPDATE_CARD_LAST_DISMISSED_AT, 0L)
        return lastDismissedAt == 0L ||
            System.currentTimeMillis() - lastDismissedAt >= UPDATE_CARD_DISMISS_COOLDOWN_MS
    }

    fun recordUpdateCardDismissed() {
        timingPrefs
            .edit()
            .putLong(UiPreferences.KEY_UPDATE_CARD_LAST_DISMISSED_AT, System.currentTimeMillis())
            .apply()
    }

    // ============================================================================
    // AI Search Web Search Fallback Tip Preferences
    // ============================================================================

    fun shouldShowWebSearchFallbackTip(): Boolean {
        val lastShownAt =
            timingPrefs.getLong(UiPreferences.KEY_WEB_SEARCH_FALLBACK_TIP_LAST_SHOWN_AT, 0L)
        return lastShownAt == 0L ||
            System.currentTimeMillis() - lastShownAt >= DAY_IN_MILLIS
    }

    fun recordWebSearchFallbackTipShown() {
        timingPrefs
            .edit()
            .putLong(
                UiPreferences.KEY_WEB_SEARCH_FALLBACK_TIP_LAST_SHOWN_AT,
                System.currentTimeMillis(),
            ).apply()
    }

    companion object {
        // UI preferences keys
        const val KEY_ONE_HANDED_MODE = "one_handed_mode"
        const val KEY_BOTTOM_SEARCH_BAR_ENABLED = "bottom_search_bar_enabled"
        const val KEY_UNIFIED_PINNED_ITEMS_ENABLED = "unified_pinned_items_enabled"
        const val KEY_SEARCH_HINTS_ENABLED = "search_hints_enabled"
        const val KEY_CARD_LAYOUT_ENABLED = "card_layout_enabled"
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
        const val KEY_OPEN_TOP_RESULT_USING_KEYBOARD_ENABLED = "open_top_result_using_keyboard_enabled"
        const val KEY_TOP_RESULT_INDICATOR_MANUALLY_DISABLED = "top_result_indicator_manually_disabled"
        const val KEY_TOP_MATCHES_ENABLED = "top_matches_enabled"
        const val KEY_TOP_MATCHES_LIMIT = "top_matches_limit"
        const val KEY_TOP_MATCHES_SECTION_ORDER = "top_matches_section_order"
        const val KEY_DISABLED_TOP_MATCHES_SECTIONS = "disabled_top_matches_sections"
        const val KEY_HOME_PINNED_SECTION_ORDER = "home_pinned_section_order"
        const val KEY_PINNED_APP_SHORTCUTS_IN_APP_GRID = "pinned_app_shortcuts_in_app_grid"
        const val KEY_PINNED_APP_GRID_ORDER = "pinned_app_grid_order"
        const val KEY_CLEAR_QUERY_ON_LAUNCH = "clear_query_on_launch"
        const val KEY_AUTO_CLOSE_OVERLAY = "auto_close_overlay"
        const val KEY_OVERLAY_MODE_ENABLED = "overlay_mode_enabled"
        const val KEY_MESSAGING_APP = "messaging_app"
        const val KEY_CALLING_APP = "calling_app"
        const val KEY_PRIMARY_CONTACT_BUTTON = "primary_contact_button"
        const val KEY_SECONDARY_CONTACT_BUTTON = "secondary_contact_button"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_INSTALL_TIME = "install_time"
        const val KEY_WALLPAPER_BACKGROUND_ALPHA = "wallpaper_background_alpha"
        const val KEY_WALLPAPER_BLUR_RADIUS = "wallpaper_blur_radius"
        const val KEY_WALLPAPER_BACKGROUND_ALPHA_LIGHT = "wallpaper_background_alpha_light"
        const val KEY_WALLPAPER_BLUR_RADIUS_LIGHT = "wallpaper_blur_radius_light"
        const val KEY_APP_THEME = "app_theme"
        const val KEY_APP_THEME_MODE = "app_theme_mode"
        const val KEY_OVERLAY_THEME_INTENSITY = "overlay_theme_intensity"
        const val KEY_FONT_SCALE_MULTIPLIER = "font_scale_multiplier"
        const val KEY_USE_SYSTEM_FONT = "use_system_font"
        const val KEY_HOME_TEXT_COLOR_OVERRIDE = "home_text_color_override"
        const val KEY_BACKGROUND_SOURCE = "background_source"
        const val KEY_CUSTOM_IMAGE_URI = "custom_image_uri"
        const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        const val KEY_ICON_PACK_UNSUPPORTED_ICON_MASK_ENABLED =
                "icon_pack_unsupported_icon_mask_enabled"
        const val KEY_APP_ICON_SHAPE = "app_icon_shape"
        const val KEY_LAUNCHER_APP_ICON = "launcher_app_icon"
        const val KEY_THEMED_ICONS_ENABLED = "themed_icons_enabled"
        const val KEY_DEVICE_THEME_ENABLED = "device_theme_enabled"
        const val KEY_AMOLED_THEME_ENABLED = "amoled_theme_enabled"
        const val KEY_WALLPAPER_ACCENT_ENABLED = "wallpaper_accent_enabled"
        const val KEY_ACCENT_COLOR_MODE = "accent_color_mode"
        const val KEY_CUSTOM_ACCENT_COLOR_ARGB = "custom_accent_color_argb"
        const val DEFAULT_CUSTOM_ACCENT_COLOR_ARGB = -10011996 // #6750A4

        fun resolveAccentColorMode(
                savedModeName: String?,
                wallpaperAccentEnabled: Boolean,
        ): AccentColorMode {
            savedModeName
                    ?.let { runCatching { AccentColorMode.valueOf(it) }.getOrNull() }
                    ?.let { return it }
            return if (wallpaperAccentEnabled) {
                AccentColorMode.FROM_WALLPAPER
            } else {
                AccentColorMode.NONE
            }
        }
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
        const val DEFAULT_TOP_MATCHES_LIMIT = 3
        val TOP_MATCHES_LIMIT_OPTIONS = listOf(1, 3, 5, 7, 10)
        val DEFAULT_TOP_MATCHES_SECTION_ORDER: List<SearchSection>
            get() = SearchSectionRegistry.orderedSections
        const val TOP_MATCHES_SECTION_ORDER_SEPARATOR = ","

        /** Sections that render as separate pinned blocks on home when unified pinned items is off. */
        val DEFAULT_HOME_PINNED_SECTION_ORDER: List<SearchSection>
            get() =
                SearchSectionRegistry.orderedSections.filter { section ->
                    section != SearchSection.APPS && section != SearchSection.APP_SETTINGS
                }
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
        const val KEY_ACCESSIBILITY_PERMISSION_DISCLAIMER_PENDING =
            "accessibility_permission_disclaimer_pending"
        // Versioned so existing users see the disclosure again whenever its scope changes.
        const val KEY_HAS_SEEN_ACCESSIBILITY_PERMISSION_DISCLAIMER =
            "has_seen_accessibility_permission_disclaimer_v2"
        const val KEY_AI_SEARCH_SETUP_EXPANDED = "direct_search_setup_expanded"
        const val KEY_DISABLED_SEARCH_ENGINES_EXPANDED = "disabled_search_engines_expanded"
        const val KEY_HOME_PINNED_SECTION_EXPANDED_PREFIX = "home_pinned_section_expanded_"
        const val KEY_UNIFIED_PINNED_ITEMS_EXPANDED = "unified_pinned_items_expanded"
        const val KEY_INSTANT_STARTUP_SURFACE_ENABLED = "instant_startup_surface_v1"
        const val KEY_HAS_SEEN_SEARCH_BAR_WELCOME = "has_seen_search_bar_welcome"
        const val KEY_FORCE_SEARCH_BAR_WELCOME_ON_NEXT_OPEN =
                "force_search_bar_welcome_on_next_open"
        const val KEY_HAS_SEEN_CONTACT_ACTION_HINT = "has_seen_contact_action_hint"
        const val KEY_HAS_SEEN_OVERLAY_ASSISTANT_TIP = "has_seen_overlay_assistant_tip"
        const val KEY_HAS_SEEN_SETTINGS_SEARCH_TIP = "has_seen_settings_search_tip"
        // Section preferences keys
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
        const val KEY_RECENT_QUERIES_DISPLAY_COUNT = "recent_queries_display_count"
        const val DEFAULT_RECENT_QUERIES_DISPLAY_COUNT = 1
        val RECENT_QUERIES_DISPLAY_COUNT_OPTIONS = setOf(1, 3, 5, 7, 10)
        const val KEY_APP_RESULT_ROW_COUNT = "app_result_row_count"
        const val DEFAULT_APP_RESULT_ROW_COUNT = 1
        val APP_RESULT_ROW_COUNT_OPTIONS = setOf(1, 2)

        // App suggestions preferences keys
        const val KEY_APP_SUGGESTIONS_ENABLED = "app_suggestions_enabled"
        const val KEY_SHOW_ALL_APPS_BUTTON = "show_all_apps_button"
        const val KEY_INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH =
            "include_non_launchable_apps_in_search"
        const val KEY_INCLUDE_ARCHIVED_APPS_IN_SEARCH = "include_archived_apps_in_search"
        const val KEY_SHOW_IN_RECENTS = "show_in_recents"
        const val KEY_NOTIFICATION_DOTS_ENABLED = "notification_dots_enabled"
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
        const val MIN_OVERLAY_THEME_INTENSITY =
                DEFAULT_OVERLAY_THEME_INTENSITY -
                        (OVERLAY_THEME_INTENSITY_STEP * OVERLAY_THEME_INTENSITY_DELTA_STEPS)
        const val MAX_OVERLAY_THEME_INTENSITY =
                DEFAULT_OVERLAY_THEME_INTENSITY +
                        (OVERLAY_THEME_INTENSITY_STEP * OVERLAY_THEME_INTENSITY_DELTA_STEPS)
        const val MIN_FONT_SCALE_MULTIPLIER = 0.90f
        const val MAX_FONT_SCALE_MULTIPLIER = 1.10f
        const val MAX_WALLPAPER_BLUR_RADIUS = 40f

        // Calculator preferences keys
        const val KEY_CALCULATOR_ENABLED = "calculator_enabled"
        const val KEY_UNIT_CONVERTER_ENABLED = "unit_converter_enabled"
        const val KEY_DATE_CALCULATOR_ENABLED = "date_calculator_enabled"
        const val KEY_CURRENCY_CONVERTER_ENABLED = "currency_converter_enabled"
        const val KEY_COLOR_VISUALIZER_ENABLED = "color_visualizer_enabled"
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
        // In-app review keys
        const val KEY_FIRST_APP_OPEN_TIME = "first_app_open_time"
        const val KEY_APP_OPEN_COUNT = "app_open_count"
        const val KEY_IN_APP_REVIEW_LAST_REQUESTED_AT = "in_app_review_last_requested_at"

        const val KEY_UPDATE_CARD_LAST_DISMISSED_AT = "update_card_last_dismissed_at"
        const val KEY_WEB_SEARCH_FALLBACK_TIP_LAST_SHOWN_AT =
            "web_search_fallback_tip_last_shown_at"

        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        private const val IN_APP_REVIEW_MIN_DAYS_USED = 7L
        private const val IN_APP_REVIEW_MIN_APP_LAUNCHES = 10
        private const val IN_APP_REVIEW_COOLDOWN_MS = 90 * DAY_IN_MILLIS
        private const val UPDATE_CARD_DISMISS_COOLDOWN_MS = 4 * DAY_IN_MILLIS

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
