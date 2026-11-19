package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import android.content.Context
import android.os.Build
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.util.isTablet
import com.tk.quicksearch.settings.toolsSettings.ToolSettingsRegistry

/**
 * Searchable toggle and inline-control rows, plus the pages that sit beside them in search order.
 * Called from [AppSettingsRepository.loadSettings] after the page rows; order here is search order.
 */
internal fun AppSettingsCatalogBuilder.addToggleAndInlineSettings(context: Context) {
    addToggle(
        id = "app_toggle_overlay_mode",
        titleRes = R.string.settings_overlay_mode_title,
        descriptionRes = R.string.settings_overlay_mode_desc,
        toggleKey = AppSettingsToggleKey.OVERLAY_MODE,
    )
    addToggle(
        id = "app_toggle_one_handed_mode",
        titleRes = R.string.settings_layout_option_bottom_title,
        descriptionRes = R.string.settings_layout_option_bottom_desc,
        toggleKey = AppSettingsToggleKey.ONE_HANDED_MODE,
    )
    addToggle(
        id = "app_toggle_bottom_searchbar",
        titleRes = R.string.settings_bottom_searchbar_title,
        descriptionRes = R.string.settings_bottom_searchbar_desc,
        toggleKey = AppSettingsToggleKey.BOTTOM_SEARCHBAR,
    )
    addToggle(
        id = "app_toggle_compact_layout",
        titleRes = R.string.settings_compact_layout_title,
        descriptionRes = R.string.settings_compact_layout_desc,
        toggleKey = AppSettingsToggleKey.COMPACT_LAYOUT,
        keywords = listOf("cards", "card layout", "background", "full width", "dense", "flat"),
    )
    addToggle(
        id = "app_toggle_unified_pinned_items",
        titleRes = R.string.settings_unified_pinned_items_title,
        descriptionRes = R.string.settings_unified_pinned_items_desc,
        toggleKey = AppSettingsToggleKey.UNIFIED_PINNED_ITEMS,
        keywords = listOf("pinned", "home", "sections", "list"),
    )
    addNavigation(
        id = "app_settings_pinned_sections_order",
        titleRes = R.string.settings_pinned_sections_order_title,
        descriptionRes = R.string.settings_pinned_sections_order_desc,
        destination = AppSettingsRouteAction.PINNED_SECTIONS_ORDER,
        keywords = listOf("pinned", "sections", "order", "reorder", "home"),
    )
    addToggle(
        id = "app_toggle_pinned_app_shortcuts_in_app_grid",
        titleRes = R.string.settings_pinned_app_shortcuts_in_app_grid_title,
        descriptionRes = R.string.settings_pinned_app_shortcuts_in_app_grid_desc,
        toggleKey = AppSettingsToggleKey.PINNED_APP_SHORTCUTS_IN_APP_GRID,
        keywords = listOf("pinned", "shortcuts", "apps", "grid"),
    )
    addToggle(
        id = "app_toggle_show_status_bar",
        titleRes = R.string.settings_show_status_bar_title,
        descriptionRes = R.string.settings_show_status_bar_desc,
        toggleKey = AppSettingsToggleKey.SHOW_STATUS_BAR,
        keywords = listOf("status bar", "fullscreen", "immersive", "hide", "system bar"),
    )
    addToggle(
        id = "app_toggle_search_chrome_blur",
        titleRes = R.string.settings_search_chrome_blur_title,
        descriptionRes = R.string.settings_search_chrome_blur_desc,
        toggleKey = AppSettingsToggleKey.SEARCH_CHROME_BLUR,
        keywords = listOf("blur", "frosted", "glass", "transparent", "translucent", "search bar", "keyboard"),
    )
    addToggle(
        id = "app_toggle_search_hints",
        titleRes = R.string.settings_search_hints_title,
        descriptionRes = R.string.settings_search_hints_desc,
        toggleKey = AppSettingsToggleKey.SEARCH_HINTS,
    )
    addToggle(
        id = "app_toggle_settings_icon",
        titleRes = R.string.settings_icon_title,
        descriptionRes = R.string.settings_icon_desc,
        toggleKey = AppSettingsToggleKey.SETTINGS_ICON,
    )
    addToggle(
        id = "app_toggle_mic_icon",
        titleRes = R.string.settings_mic_icon_title,
        descriptionRes = R.string.settings_mic_icon_desc,
        toggleKey = AppSettingsToggleKey.MIC_ICON,
        keywords = listOf("voice", "microphone", "speech"),
    )
    addToggle(
        id = "app_toggle_app_labels",
        titleRes = R.string.settings_show_app_labels_title,
        descriptionRes = R.string.settings_show_app_labels_desc,
        toggleKey = AppSettingsToggleKey.APP_LABELS,
        keywords = listOf("app names"),
    )
    addToggle(
        id = "app_toggle_use_system_font",
        titleRes = R.string.settings_use_system_font_title,
        descriptionRes = R.string.settings_use_system_font_desc,
        toggleKey = AppSettingsToggleKey.USE_SYSTEM_FONT,
        keywords = listOf("font", "typeface"),
    )
    addToggle(
        id = "app_setting_search_engine_style",
        titleRes = R.string.settings_search_engine_display_title,
        toggleKey = AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE,
        keywords = listOf("search engine style", "inline", "compact", "search engine rows"),
    )
    addToggle(
        id = "app_toggle_alias_after_query",
        titleRes = R.string.settings_search_engine_alias_suffix_title,
        descriptionRes = R.string.settings_search_engine_alias_suffix_desc,
        toggleKey = AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
        keywords = listOf("suffix", "search engine"),
    )
    addToggle(
        id = "app_toggle_alias_trigger_after_space",
        titleRes = R.string.alias_trigger_after_space,
        toggleKey = AppSettingsToggleKey.ALIAS_TRIGGER_AFTER_SPACE,
        keywords = listOf("alias after query", "suffix", "search engine", "space"),
    )
    ToolSettingsRegistry.definitions.sortedBy { it.searchSettingsOrder }.forEach { tool ->
        val booleanSetting = tool.booleanSetting
        if (booleanSetting != null) {
            addBooleanSetting(booleanSetting, destination = tool.searchDestination)
        } else {
            addToggle(
                id = tool.searchId,
                titleRes = tool.titleResId,
                descriptionRes = tool.defaultDescriptionResId,
                toggleKey = checkNotNull(tool.toggleKey),
                keywords = tool.keywords,
            )
        }
    }
    addToggle(
        id = "app_toggle_app_suggestions",
        titleRes = R.string.app_suggestions_toggle_title,
        descriptionRes = R.string.app_suggestions_toggle_desc,
        toggleKey = AppSettingsToggleKey.APP_SUGGESTIONS,
    )
    addNavigation(
        id = "app_settings_app_suggestion_tabs",
        titleRes = R.string.app_suggestions_tabs_dialog_title,
        descriptionRes = R.string.app_suggestions_tabs_dialog_message,
        destination = AppSettingsRouteAction.APP_SUGGESTION_TABS,
        keywords = listOf("app suggestions", "tabs", "recents", "most used", "new", "updated"),
    )
    addToggle(
        id = "app_toggle_notification_dots",
        titleRes = R.string.notification_dots_toggle_title,
        descriptionRes = R.string.notification_dots_toggle_desc,
        toggleKey = AppSettingsToggleKey.NOTIFICATION_DOTS,
        keywords = listOf("badge", "notification", "dot", "unread"),
    )
    addToggle(
        id = "app_toggle_app_lock_outside_quick_search",
        titleRes = R.string.app_lock_outside_quick_search_title,
        descriptionRes = R.string.app_lock_outside_quick_search_desc,
        toggleKey = AppSettingsToggleKey.APP_LOCK_OUTSIDE_QUICK_SEARCH,
        keywords = listOf("app lock", "lock", "biometric", "fingerprint", "privacy", "launcher"),
    )
    addToggle(
        id = "app_toggle_speed_bump_outside_quick_search",
        titleRes = R.string.speed_bump_outside_quick_search_title,
        descriptionRes = R.string.speed_bump_outside_quick_search_desc,
        toggleKey = AppSettingsToggleKey.SPEED_BUMP_OUTSIDE_QUICK_SEARCH,
        keywords = listOf("speedbump", "speed bump", "pause", "delay", "launcher", "mindful", "addiction"),
    )
    addToggle(
        id = "app_toggle_show_all_apps_button",
        titleRes = R.string.settings_app_shortcuts_filter_all_apps,
        descriptionRes = R.string.show_all_apps_button_toggle_desc,
        toggleKey = AppSettingsToggleKey.SHOW_ALL_APPS_BUTTON,
        keywords = listOf("all apps", "app drawer"),
    )
    addToggle(
        id = "app_toggle_include_non_launchable_apps",
        titleRes = R.string.include_non_launchable_apps_toggle_title,
        descriptionRes = R.string.include_non_launchable_apps_toggle_desc,
        toggleKey = AppSettingsToggleKey.INCLUDE_NON_LAUNCHABLE_APPS_IN_SEARCH,
        keywords = listOf("app info", "launch activity", "hidden apps"),
    )
    addToggle(
        id = "app_toggle_include_archived_apps",
        titleRes = R.string.include_archived_apps_toggle_title,
        descriptionRes = R.string.include_archived_apps_toggle_desc,
        toggleKey = AppSettingsToggleKey.INCLUDE_ARCHIVED_APPS_IN_SEARCH,
        keywords = listOf("archive", "archived", "offload", "restore"),
    )
    addToggle(
        id = "app_toggle_show_in_recents",
        titleRes = R.string.show_in_recents_toggle_title,
        descriptionRes = R.string.show_in_recents_toggle_desc,
        toggleKey = AppSettingsToggleKey.SHOW_IN_RECENTS,
        keywords = listOf("recent apps", "overview", "task"),
    )
    addToggle(
        id = "app_toggle_web_suggestions",
        titleRes = R.string.web_search_suggestions_title,
        toggleKey = AppSettingsToggleKey.WEB_SUGGESTIONS,
        keywords = listOf("autocomplete"),
    )
    addToggle(
        id = "app_settings_app_result_rows",
        titleRes = R.string.settings_app_result_rows_title,
        toggleKey = AppSettingsToggleKey.APP_RESULT_ROWS,
        keywords = listOf("apps", "rows", "results"),
    )
    addToggle(
        id = "app_settings_top_matches",
        titleRes = R.string.top_matches_title,
        descriptionRes = R.string.top_matches_toggle_desc,
        toggleKey = AppSettingsToggleKey.TOP_MATCHES,
        keywords = listOf("top matches", "best results", "searches"),
    )
    addNavigation(
        id = "app_settings_top_matches_priority",
        titleRes = R.string.top_matches_priority_title,
        descriptionRes = R.string.top_matches_priority_desc,
        destination = AppSettingsRouteAction.TOP_MATCHES_PRIORITY,
        keywords = listOf("top matches", "priority", "order", "reorder"),
    )
    addNavigation(
        id = TOP_MATCHES_COUNT_SETTING_ID,
        titleRes = R.string.top_matches_count_label,
        destination = AppSettingsTarget.Page(SettingsDetailType.SEARCH_RESULTS),
        keywords = listOf("top matches", "count", "number", "limit"),
    )
    addToggle(
        id = "app_toggle_recent_queries",
        titleRes = R.string.recent_queries_toggle_title,
        descriptionRes = R.string.recent_queries_toggle_desc,
        toggleKey = AppSettingsToggleKey.RECENT_QUERIES,
        keywords = listOf("recent"),
    )
    addToggle(
        id = "app_toggle_fuzzy_search",
        titleRes = R.string.fuzzy_search_toggle_title,
        descriptionRes = R.string.fuzzy_search_toggle_desc,
        toggleKey = AppSettingsToggleKey.FUZZY_SEARCH,
        keywords = listOf("typo", "approximate", "matching"),
    )
    addNavigation(
        id = "app_settings_search_result_ranking",
        titleRes = R.string.secondary_ranking_title,
        descriptionRes = R.string.secondary_ranking_dialog_desc,
        destination = AppSettingsRouteAction.SEARCH_RESULT_RANKING,
        keywords = listOf("ranking", "recency", "most opened", "sort", "order"),
    )
    addToggle(
        id = "app_toggle_open_top_result_using_keyboard",
        titleRes = R.string.open_top_result_using_keyboard_toggle_title,
        descriptionRes = R.string.open_top_result_using_keyboard_toggle_desc,
        toggleKey = AppSettingsToggleKey.OPEN_TOP_RESULT_USING_KEYBOARD,
        keywords = listOf("keyboard", "enter", "done", "top result"),
    )
    addToggle(
        id = "app_toggle_top_result_indicator",
        titleRes = R.string.top_result_indicator_toggle_title,
        descriptionRes = R.string.top_result_indicator_toggle_desc,
        toggleKey = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
    )
    addToggle(
        id = "app_toggle_open_keyboard",
        titleRes = R.string.open_keyboard_toggle_title,
        descriptionRes = R.string.open_keyboard_toggle_desc,
        toggleKey = AppSettingsToggleKey.OPEN_KEYBOARD,
    )
    addToggle(
        id = "app_toggle_clear_query",
        titleRes = R.string.clear_query_toggle_title,
        descriptionRes = R.string.clear_query_toggle_desc,
        toggleKey = AppSettingsToggleKey.CLEAR_QUERY,
    )
    addToggle(
        id = "app_toggle_auto_close_overlay",
        titleRes = R.string.auto_close_overlay_toggle_title,
        descriptionRes = R.string.auto_close_overlay_toggle_desc,
        toggleKey = AppSettingsToggleKey.AUTO_CLOSE_OVERLAY,
    )
    addToggle(
        id = "app_toggle_circular_app_icons",
        titleRes = R.string.settings_circular_app_icons_title,
        descriptionRes = R.string.settings_circular_app_icons_desc,
        toggleKey = AppSettingsToggleKey.CIRCULAR_APP_ICONS,
        keywords = listOf("icon shape", "circle"),
    )
    addToggle(
        id = "app_toggle_direct_dial",
        titleRes = R.string.settings_direct_dial_title,
        descriptionRes = R.string.settings_direct_dial_desc,
        toggleKey = AppSettingsToggleKey.DIRECT_DIAL,
        keywords = listOf("call"),
    )
    addAll(context.searchSectionToggleRows())
    addToggle(
        id = "app_toggle_wallpaper_accent",
        titleRes = R.string.settings_wallpaper_accent_title,
        descriptionRes = R.string.settings_wallpaper_accent_desc,
        toggleKey = AppSettingsToggleKey.ACCENT_COLOR,
        keywords = listOf("wallpaper", "background", "accent", "custom color"),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        addToggle(
            id = "app_toggle_device_theme",
            titleRes = R.string.settings_device_theme_title,
            descriptionRes = R.string.settings_device_theme_desc,
            toggleKey = AppSettingsToggleKey.DEVICE_THEME,
            keywords = listOf("material you", "dynamic color", "palette", "theme"),
        )
    }
    addToggle(
        id = "app_toggle_amoled_theme",
        titleRes = R.string.settings_amoled_theme_title,
        descriptionRes = R.string.settings_amoled_theme_desc,
        toggleKey = AppSettingsToggleKey.AMOLED_THEME,
        keywords = listOf("amoled", "true black", "oled", "mono", "dark"),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        addToggle(
            id = "app_toggle_themed_icons",
            titleRes = R.string.settings_themed_icons_title,
            descriptionRes = R.string.settings_themed_icons_desc,
            toggleKey = AppSettingsToggleKey.THEMED_ICONS,
        )
    }
    if (!isTablet(context)) {
        addToggle(
            id = "app_setting_apps_per_row",
            titleRes = R.string.settings_app_columns_title,
            toggleKey = AppSettingsToggleKey.APPS_PER_ROW,
            keywords = listOf("grid", "layout"),
        )
    }
}
