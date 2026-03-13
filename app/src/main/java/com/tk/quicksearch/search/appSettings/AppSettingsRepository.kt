package com.tk.quicksearch.search.appSettings

import android.content.Context
import com.tk.quicksearch.R

class AppSettingsRepository(
    private val context: Context,
) {
    fun loadSettings(): List<AppSettingResult> {
        return buildList {
            addNavigation(
                id = "app_settings_root",
                titleRes = R.string.settings_title,
                descriptionRes = R.string.settings_search_results_desc,
                destination = AppSettingsDestination.ROOT,
                keywords = listOf("preferences", "configuration"),
            )
            addNavigation(
                id = "app_settings_appearance",
                titleRes = R.string.settings_appearance_title,
                descriptionRes = R.string.settings_appearance_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords = listOf("theme", "wallpaper", "style", "font"),
            )
            addNavigation(
                id = "app_settings_search_results",
                titleRes = R.string.settings_search_results_title,
                descriptionRes = R.string.settings_search_results_desc,
                destination = AppSettingsDestination.SEARCH_RESULTS,
                keywords = listOf("results", "history", "web suggestions"),
            )
            addNavigation(
                id = "app_settings_search_engines",
                titleRes = R.string.settings_search_engines_title,
                descriptionRes = R.string.settings_search_engines_desc,
                destination = AppSettingsDestination.SEARCH_ENGINES,
                keywords = listOf("engines", "gemini", "google", "alias"),
            )
            addNavigation(
                id = "app_settings_tools",
                titleRes = R.string.settings_tools_title,
                descriptionRes = R.string.settings_tools_desc,
                destination = AppSettingsDestination.TOOLS,
                keywords = listOf("calculator"),
            )
            addNavigation(
                id = "app_settings_launch_options",
                titleRes = R.string.settings_launch_options_title,
                descriptionRes = R.string.settings_launch_options_desc,
                destination = AppSettingsDestination.LAUNCH_OPTIONS,
                keywords = listOf("assistant", "widget", "tile"),
            )
            addNavigation(
                id = "app_settings_permissions",
                titleRes = R.string.settings_permissions_title,
                descriptionRes = R.string.settings_permissions_desc,
                destination = AppSettingsDestination.PERMISSIONS,
                keywords = listOf("usage access", "contacts", "files", "call"),
            )
            addNavigation(
                id = "app_settings_app_management",
                titleRes = R.string.settings_manage_apps_title,
                descriptionRes = R.string.settings_manage_apps_desc,
                destination = AppSettingsDestination.APP_MANAGEMENT,
                keywords = listOf("apps", "uninstall", "info"),
            )
            addNavigation(
                id = "app_settings_app_shortcuts",
                titleRes = R.string.section_app_shortcuts,
                descriptionRes = R.string.settings_manage_shortcuts_desc,
                destination = AppSettingsDestination.APP_SHORTCUTS,
                keywords = listOf("shortcuts", "deeplink", "custom"),
            )
            addNavigation(
                id = "app_settings_calls_texts",
                titleRes = R.string.settings_calls_texts_title,
                descriptionRes = R.string.settings_manage_calls_texts_contacts_desc,
                destination = AppSettingsDestination.CALLS_TEXTS,
                keywords = listOf("calling", "messaging", "whatsapp", "telegram", "signal"),
            )
            addNavigation(
                id = "app_settings_files",
                titleRes = R.string.settings_file_types_title,
                descriptionRes = R.string.settings_manage_files_desc,
                destination = AppSettingsDestination.FILES,
                keywords = listOf("files", "folders", "extensions"),
            )
            addNavigation(
                id = "app_settings_device_settings",
                titleRes = R.string.section_settings,
                descriptionRes = R.string.settings_view_all_desc,
                destination = AppSettingsDestination.DEVICE_SETTINGS,
                keywords = listOf("android settings", "system settings"),
            )
            addNavigation(
                id = "app_settings_excluded_items",
                titleRes = R.string.settings_excluded_items_title,
                descriptionRes = R.string.settings_excluded_items_desc,
                destination = AppSettingsDestination.EXCLUDED_ITEMS,
                keywords = listOf("excluded", "hidden", "blocked"),
            )
            addNavigation(
                id = "app_settings_direct_search_configure",
                titleRes = R.string.settings_direct_search_configure_title,
                descriptionRes = R.string.settings_direct_search_desc,
                destination = AppSettingsDestination.DIRECT_SEARCH_CONFIGURE,
                keywords = listOf("gemini", "api", "direct search"),
            )
            addNavigation(
                id = "app_settings_features_list",
                titleRes = R.string.settings_all_quick_search_features,
                descriptionRes = R.string.settings_all_quick_search_features_desc,
                destination = AppSettingsDestination.FEATURES_LIST,
                keywords = listOf("features", "capabilities"),
            )
            addNavigation(
                id = "app_settings_open_source_licenses",
                titleRes = R.string.settings_open_source_licenses_title,
                destination = AppSettingsDestination.OPEN_SOURCE_LICENSES,
                keywords = listOf("oss", "licenses", "open source"),
            )

            addToggle(
                id = "app_toggle_overlay_mode",
                titleRes = R.string.settings_overlay_mode_title,
                descriptionRes = R.string.settings_overlay_mode_desc,
                toggleKey = AppSettingsToggleKey.OVERLAY_MODE,
                keywords = listOf("overlay", "floating"),
            )
            addToggle(
                id = "app_toggle_one_handed_mode",
                titleRes = R.string.settings_layout_option_bottom_title,
                descriptionRes = R.string.settings_layout_option_bottom_desc,
                toggleKey = AppSettingsToggleKey.ONE_HANDED_MODE,
                keywords = listOf("one handed", "bottom"),
            )
            addToggle(
                id = "app_toggle_bottom_searchbar",
                titleRes = R.string.settings_bottom_searchbar_title,
                descriptionRes = R.string.settings_bottom_searchbar_desc,
                toggleKey = AppSettingsToggleKey.BOTTOM_SEARCHBAR,
                keywords = listOf("bottom", "search bar"),
            )
            addToggle(
                id = "app_toggle_app_labels",
                titleRes = R.string.settings_show_app_labels_title,
                descriptionRes = R.string.settings_show_app_labels_desc,
                toggleKey = AppSettingsToggleKey.APP_LABELS,
                keywords = listOf("labels", "app names"),
            )
            addToggle(
                id = "app_toggle_alias_after_query",
                titleRes = R.string.settings_search_engine_alias_suffix_title,
                descriptionRes = R.string.settings_search_engine_alias_suffix_desc,
                toggleKey = AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
                keywords = listOf("alias", "suffix", "search engine"),
            )
            addToggle(
                id = "app_toggle_calculator",
                titleRes = R.string.calculator_toggle_title,
                descriptionRes = R.string.calculator_toggle_desc,
                toggleKey = AppSettingsToggleKey.CALCULATOR,
                keywords = listOf("calculator", "math"),
            )
            addToggle(
                id = "app_toggle_app_suggestions",
                titleRes = R.string.app_suggestions_toggle_title,
                descriptionRes = R.string.app_suggestions_toggle_desc,
                toggleKey = AppSettingsToggleKey.APP_SUGGESTIONS,
                keywords = listOf("suggestions", "recommended apps"),
            )
            addToggle(
                id = "app_toggle_web_suggestions",
                titleRes = R.string.web_search_suggestions_title,
                descriptionRes = R.string.settings_search_results_desc,
                toggleKey = AppSettingsToggleKey.WEB_SUGGESTIONS,
                keywords = listOf("web", "suggestions", "autocomplete"),
            )
            addToggle(
                id = "app_toggle_recent_queries",
                titleRes = R.string.recent_queries_toggle_title,
                descriptionRes = R.string.recent_queries_toggle_desc,
                toggleKey = AppSettingsToggleKey.RECENT_QUERIES,
                keywords = listOf("history", "recent queries"),
            )
            addToggle(
                id = "app_toggle_top_result_indicator",
                titleRes = R.string.top_result_indicator_toggle_title,
                descriptionRes = R.string.top_result_indicator_toggle_desc,
                toggleKey = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
                keywords = listOf("top result", "indicator"),
            )
            addToggle(
                id = "app_toggle_open_keyboard",
                titleRes = R.string.open_keyboard_toggle_title,
                descriptionRes = R.string.open_keyboard_toggle_desc,
                toggleKey = AppSettingsToggleKey.OPEN_KEYBOARD,
                keywords = listOf("keyboard", "launch"),
            )
            addToggle(
                id = "app_toggle_clear_query",
                titleRes = R.string.clear_query_toggle_title,
                descriptionRes = R.string.clear_query_toggle_desc,
                toggleKey = AppSettingsToggleKey.CLEAR_QUERY,
                keywords = listOf("clear", "query", "launch"),
            )
            addToggle(
                id = "app_toggle_show_folders",
                titleRes = R.string.settings_folders_toggle,
                descriptionRes = R.string.settings_manage_files_desc,
                toggleKey = AppSettingsToggleKey.SHOW_FOLDERS,
                keywords = listOf("folders", "directories"),
            )
            addToggle(
                id = "app_toggle_show_system_files",
                titleRes = R.string.settings_system_files_toggle,
                descriptionRes = R.string.settings_manage_files_desc,
                toggleKey = AppSettingsToggleKey.SHOW_SYSTEM_FILES,
                keywords = listOf("system files"),
            )
            addToggle(
                id = "app_toggle_show_hidden_files",
                titleRes = R.string.settings_hidden_files_toggle,
                descriptionRes = R.string.settings_manage_files_desc,
                toggleKey = AppSettingsToggleKey.SHOW_HIDDEN_FILES,
                keywords = listOf("hidden files", "dot files"),
            )
            addToggle(
                id = "app_toggle_direct_dial",
                titleRes = R.string.settings_direct_dial_title,
                descriptionRes = R.string.settings_direct_dial_desc,
                toggleKey = AppSettingsToggleKey.DIRECT_DIAL,
                keywords = listOf("direct dial", "call"),
            )
        }
    }

    private fun MutableList<AppSettingResult>.addNavigation(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        destination: AppSettingsDestination,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.NAVIGATE,
                destination = destination,
            ),
        )
    }

    private fun MutableList<AppSettingResult>.addToggle(
        id: String,
        titleRes: Int,
        descriptionRes: Int? = null,
        toggleKey: AppSettingsToggleKey,
        keywords: List<String> = emptyList(),
    ) {
        add(
            AppSettingResult(
                id = id,
                title = context.getString(titleRes),
                description = descriptionRes?.let(context::getString),
                keywords = keywords,
                action = AppSettingResultAction.TOGGLE,
                toggleKey = toggleKey,
            ),
        )
    }
}
