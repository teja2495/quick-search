package com.tk.quicksearch.search.appSettings

import android.content.Context
import android.os.Build
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.shared.util.isTablet

private val WHITESPACE_REGEX = "\\s+".toRegex()

class AppSettingsRepository(
    private val context: Context,
) {
    fun loadSettings(): List<AppSettingResult> {
        return buildList {
            addNavigation(
                id = "app_settings_appearance",
                titleRes = R.string.settings_appearance_title,
                descriptionRes = R.string.settings_appearance_desc,
                destination = AppSettingsDestination.APPEARANCE,
                keywords =
                    listOf(
                        "theme",
                        "wallpaper",
                        "style",
                        "search engines style",
                        "font size",
                        "text size",
                        "icons",
                        "icon pack",
                        "inline",
                        "compact"
                    ),
            )
            addNavigation(
                id = "app_settings_search_results",
                titleRes = R.string.settings_search_results_title,
                descriptionRes = R.string.settings_search_results_desc,
                destination = AppSettingsDestination.SEARCH_RESULTS,
                keywords = listOf("history", "suggestions"),
            )
            addNavigation(
                id = "app_settings_search_engines",
                titleRes = R.string.settings_app_shortcuts_filter_search_engines,
                descriptionRes = R.string.settings_search_engines_desc,
                destination = AppSettingsDestination.SEARCH_ENGINES,
                keywords = listOf("ai", "direct", "alias"),
            )
            addNavigation(
                id = "app_settings_gemini_api",
                titleRes = R.string.settings_backup_export_option_gemini_title,
                descriptionRes = R.string.settings_gemini_api_desc,
                destination = AppSettingsDestination.GEMINI_API,
                keywords = listOf("ai provider", "gemini", "openai", "groq", "claude"),
            )
            addNavigation(
                id = "app_settings_tools",
                titleRes = R.string.settings_tools_title,
                descriptionRes = R.string.settings_tools_desc,
                destination = AppSettingsDestination.TOOLS,
                keywords = listOf("calculator", "date", "time", "unit converter", "currency converter", "conversion", "alias"),
            )
            addNavigation(
                id = "app_settings_launch_options",
                titleRes = R.string.settings_launch_options_title,
                descriptionRes = R.string.settings_launch_options_desc,
                destination = AppSettingsDestination.LAUNCH_OPTIONS,
            )
            addNavigation(
                id = "app_settings_default_assistant",
                titleRes = R.string.settings_default_assistant_title,
                descriptionRes = R.string.settings_default_assistant_desc,
                destination = AppSettingsDestination.SET_DEFAULT_ASSISTANT,
                keywords = listOf("digital assistant"),
            )
            addToggle(
                id = "app_settings_assistant_voice_mode",
                titleRes = R.string.settings_assistant_voice_mode_title,
                descriptionRes = R.string.settings_assistant_voice_mode_desc,
                toggleKey = AppSettingsToggleKey.ASSISTANT_LAUNCH_VOICE_MODE,
            )
            addNavigation(
                id = "app_settings_home_screen_widget",
                titleRes = R.string.settings_home_screen_widget_title,
                descriptionRes = R.string.settings_home_screen_widget_desc,
                destination = AppSettingsDestination.ADD_HOME_SCREEN_WIDGET,
            )
            addNavigation(
                id = "app_settings_quick_settings_tile",
                titleRes = R.string.settings_quick_settings_tile_title,
                descriptionRes = R.string.settings_quick_settings_tile_desc,
                destination = AppSettingsDestination.ADD_QUICK_SETTINGS_TILE,
            )
            addNavigation(
                id = "app_settings_more_options",
                titleRes = R.string.settings_more_options_title,
                descriptionRes = R.string.settings_more_options_desc,
                destination = AppSettingsDestination.MORE_OPTIONS,
            )
            addNavigation(
                id = "app_settings_permissions",
                titleRes = R.string.settings_permissions_title,
                descriptionRes = R.string.settings_permissions_desc,
                destination = AppSettingsDestination.PERMISSIONS,
                keywords = listOf("access"),
            )
            addNavigation(
                id = "app_settings_app_management",
                titleRes = R.string.section_apps,
                descriptionRes = R.string.settings_manage_apps_desc,
                destination = AppSettingsDestination.APP_MANAGEMENT,
                keywords = listOf("uninstall", "app info", "apps"),
            )
            addNavigation(
                id = "app_settings_app_shortcuts",
                titleRes = R.string.section_app_shortcuts,
                descriptionRes = R.string.settings_manage_shortcuts_desc,
                destination = AppSettingsDestination.APP_SHORTCUTS,
                keywords = listOf("deep link", "custom", "activity"),
            )
            addNavigation(
                id = "app_settings_calls_texts",
                titleRes = R.string.settings_calls_texts_title,
                descriptionRes = R.string.settings_manage_calls_texts_contacts_desc,
                destination = AppSettingsDestination.CALLS_TEXTS,
                keywords = listOf("contacts", "calling", "messaging", "whatsapp", "telegram", "signal"),
            )
            addNavigation(
                id = "app_settings_default_calling_app",
                titleRes = R.string.settings_calling_card_title,
                destination = AppSettingsDestination.CALLS_TEXTS,
            )
            addNavigation(
                id = "app_settings_default_messaging_app",
                titleRes = R.string.settings_messaging_card_title,
                destination = AppSettingsDestination.CALLS_TEXTS,
                keywords = listOf("texting"),
            )
            addNavigation(
                id = "app_settings_files",
                titleRes = R.string.settings_file_types_title,
                descriptionRes = R.string.settings_manage_files_desc,
                destination = AppSettingsDestination.FILES,
                keywords =
                    listOf(
                        "filters",
                        "whitelist",
                        "blacklist",
                    ),
            )
            addNavigation(
                id = "app_settings_reload_apps",
                titleRes = R.string.settings_refresh_apps_title,
                destination = AppSettingsDestination.RELOAD_APPS,
                keywords = listOf("refresh apps"),
            )
            addNavigation(
                id = "app_settings_reload_contacts",
                titleRes = R.string.settings_refresh_contacts_title,
                destination = AppSettingsDestination.RELOAD_CONTACTS,
                keywords = listOf("refresh contacts", "sync contacts"),
            )
            addNavigation(
                id = "app_settings_reload_files",
                titleRes = R.string.settings_refresh_files_title,
                destination = AppSettingsDestination.RELOAD_FILES,
                keywords = listOf("refresh files"),
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
            )
            addNavigation(
                id = "app_settings_calendar_events",
                titleRes = R.string.section_calendar,
                descriptionRes = R.string.settings_calendar_view_all_events_desc,
                destination = AppSettingsDestination.CALENDAR_EVENTS,
            )
            addToggle(
                id = "app_toggle_calendar_today_events",
                titleRes = R.string.settings_calendar_show_today_events_title,
                descriptionRes = R.string.settings_calendar_show_today_events_desc,
                toggleKey = AppSettingsToggleKey.SHOW_TODAY_EVENTS,
                keywords = listOf("today"),
            )
            addNavigation(
                id = "app_settings_create_calendar_event",
                titleRes = R.string.app_settings_create_calendar_event_title,
                descriptionRes = R.string.app_settings_create_calendar_event_desc,
                destination = AppSettingsDestination.CREATE_CALENDAR_EVENT,
                keywords = listOf("add event", "new event", "create event"),
            )
            addNavigation(
                id = "app_settings_send_feedback",
                titleRes = R.string.settings_feedback_send_title,
                descriptionRes = R.string.settings_feedback_send_desc,
                destination = AppSettingsDestination.SEND_FEEDBACK,
                keywords = listOf("support", "bug", "request"),
            )
            addNavigation(
                id = "app_settings_rate_quick_search",
                titleRes = R.string.settings_feedback_rate_title,
                descriptionRes = R.string.settings_feedback_rate_desc,
                destination = AppSettingsDestination.RATE_QUICK_SEARCH,
                keywords = listOf("review"),
            )
            addNavigation(
                id = "app_settings_development",
                titleRes = R.string.settings_feedback_github_title,
                descriptionRes = R.string.settings_feedback_github_desc,
                destination = AppSettingsDestination.DEVELOPMENT,
                keywords = listOf("github", "code"),
            )
            addNavigation(
                id = "app_settings_features_list",
                titleRes = R.string.settings_all_quick_search_features,
                descriptionRes = R.string.settings_all_quick_search_features_desc,
                destination = AppSettingsDestination.FEATURES_LIST,
            )
            addNavigation(
                id = "app_settings_create_note",
                titleRes = R.string.app_settings_search_create_note_title,
                descriptionRes = R.string.app_settings_search_create_note_desc,
                destination = AppSettingsDestination.CREATE_NOTE,
                keywords = listOf("new note", "memo", "write"),
            )
            addNavigation(
                id = "app_settings_notes_list",
                titleRes = R.string.section_notes,
                descriptionRes = R.string.settings_notes_view_all_desc,
                destination = AppSettingsDestination.NOTES_LIST,
                keywords = listOf("notebook", "memo", "list"),
            )

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
                id = "app_toggle_app_labels",
                titleRes = R.string.settings_show_app_labels_title,
                descriptionRes = R.string.settings_show_app_labels_desc,
                toggleKey = AppSettingsToggleKey.APP_LABELS,
                keywords = listOf("app names"),
            )
            addToggle(
                id = "app_toggle_alias_after_query",
                titleRes = R.string.settings_search_engine_alias_suffix_title,
                descriptionRes = R.string.settings_search_engine_alias_suffix_desc,
                toggleKey = AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
                keywords = listOf("suffix", "search engine"),
            )
            addToggle(
                id = "app_toggle_calculator",
                titleRes = R.string.calculator_toggle_title,
                descriptionRes = R.string.calculator_toggle_desc,
                toggleKey = AppSettingsToggleKey.CALCULATOR,
            )
            addToggle(
                id = "app_toggle_unit_converter",
                titleRes = R.string.unit_converter_info_title,
                descriptionRes = R.string.unit_converter_toggle_action_desc,
                toggleKey = AppSettingsToggleKey.UNIT_CONVERTER,
                keywords = listOf("conversion"),
            )
            addNavigation(
                id = "app_nav_unit_converter_info",
                titleRes = R.string.unit_converter_info_title,
                descriptionRes = R.string.date_calculator_toggle_desc,
                destination = AppSettingsDestination.UNIT_CONVERTER_INFO,
                keywords = listOf("conversion", "examples"),
            )
            addToggle(
                id = "app_toggle_date_calculator",
                titleRes = R.string.date_calculator_info_title,
                descriptionRes = R.string.date_calculator_toggle_action_desc,
                toggleKey = AppSettingsToggleKey.DATE_CALCULATOR,
            )
            addNavigation(
                id = "app_nav_date_calculator_info",
                titleRes = R.string.date_calculator_info_title,
                descriptionRes = R.string.date_calculator_toggle_desc,
                destination = AppSettingsDestination.DATE_CALCULATOR_INFO,
                keywords = listOf("examples"),
            )
            addToggle(
                id = "app_toggle_dictionary",
                titleRes = R.string.dictionary_toggle_title,
                descriptionRes = R.string.dictionary_toggle_desc,
                toggleKey = AppSettingsToggleKey.DICTIONARY,
            )
            addToggle(
                id = "app_toggle_app_suggestions",
                titleRes = R.string.app_suggestions_toggle_title,
                descriptionRes = R.string.app_suggestions_toggle_desc,
                toggleKey = AppSettingsToggleKey.APP_SUGGESTIONS,
            )
            addToggle(
                id = "app_toggle_web_suggestions",
                titleRes = R.string.web_search_suggestions_title,
                toggleKey = AppSettingsToggleKey.WEB_SUGGESTIONS,
                keywords = listOf("autocomplete"),
            )
            addToggle(
                id = "app_toggle_recent_queries",
                titleRes = R.string.recent_queries_toggle_title,
                descriptionRes = R.string.recent_queries_toggle_desc,
                toggleKey = AppSettingsToggleKey.RECENT_QUERIES,
                keywords = listOf("recent"),
            )
            addToggle(
                id = "app_toggle_top_result_indicator",
                titleRes = R.string.top_result_indicator_toggle_title,
                descriptionRes = R.string.top_result_indicator_toggle_desc,
                toggleKey = AppSettingsToggleKey.TOP_RESULT_INDICATOR,
            )
            addToggle(
                id = "app_toggle_open_keyboard",
                titleRes = R.string.action_open_keyboard,
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
            addSearchSectionToggles()
            addToggle(
                id = "app_toggle_wallpaper_accent",
                titleRes = R.string.settings_wallpaper_accent_title,
                descriptionRes = R.string.settings_wallpaper_accent_desc,
                toggleKey = AppSettingsToggleKey.WALLPAPER_ACCENT,
                keywords = listOf("wallpaper", "background"),
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addToggle(
                    id = "app_toggle_themed_icons",
                    titleRes = R.string.settings_themed_icons_title,
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

    private fun MutableList<AppSettingResult>.addSearchSectionToggles() {
        SearchSectionRegistry.orderedDefinitions.forEach { definition ->
            addToggle(
                id = searchSectionToggleId(definition.section),
                titleRes = searchSectionToggleTitleRes(definition.section),
                descriptionRes = searchSectionToggleDescriptionRes(definition.section),
                toggleKey = definition.appSettingsToggleKey,
            )
        }
    }

    private fun searchSectionToggleId(section: SearchSection): String =
        when (section) {
            SearchSection.APPS -> "app_toggle_search_apps"
            SearchSection.APP_SHORTCUTS -> "app_toggle_search_app_shortcuts"
            SearchSection.CONTACTS -> "app_toggle_search_contacts"
            SearchSection.FILES -> "app_toggle_search_files"
            SearchSection.SETTINGS -> "app_toggle_search_device_settings"
            SearchSection.CALENDAR -> "app_toggle_search_calendar"
            SearchSection.NOTES -> "app_toggle_search_notes"
            SearchSection.APP_SETTINGS -> "app_toggle_search_app_settings"
        }

    private fun searchSectionToggleTitleRes(section: SearchSection): Int =
        when (section) {
            SearchSection.APPS -> R.string.search_section_apps_toggle_title
            SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_title
            SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_title
            SearchSection.FILES -> R.string.search_section_files_toggle_title
            SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_title
            SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_title
            SearchSection.NOTES -> R.string.search_section_notes_toggle_title
            SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_title
        }

    private fun searchSectionToggleDescriptionRes(section: SearchSection): Int =
        when (section) {
            SearchSection.APPS -> R.string.search_section_apps_toggle_desc
            SearchSection.APP_SHORTCUTS -> R.string.search_section_app_shortcuts_toggle_desc
            SearchSection.CONTACTS -> R.string.search_section_contacts_toggle_desc
            SearchSection.FILES -> R.string.search_section_files_toggle_desc
            SearchSection.SETTINGS -> R.string.search_section_device_settings_toggle_desc
            SearchSection.CALENDAR -> R.string.search_section_calendar_toggle_desc
            SearchSection.NOTES -> R.string.search_section_notes_toggle_desc
            SearchSection.APP_SETTINGS -> R.string.search_section_app_settings_toggle_desc
        }

    fun resolveSearchDescription(
        setting: AppSettingResult,
        queryContext: SearchQueryContext,
    ): String? {
        if (!setting.isNavigateAction || queryContext.tokens.isEmpty()) {
            return setting.description
        }
        if (setting.destination == AppSettingsDestination.APPEARANCE) {
            val appearanceDescription = getAppearanceSearchDescription(queryContext.tokens.toSet())
            if (appearanceDescription != null) {
                return appearanceDescription
            }
        }

        val matchedKeyword = findBestMatchingKeyword(setting.keywords, queryContext.tokens)
        return matchedKeyword?.let {
            context.getString(R.string.settings_search_dynamic_description_template, it)
        } ?: setting.description
    }

    private fun getAppearanceSearchDescription(queryTokens: Set<String>): String? {
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_THEME_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_app_theme)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_ICON_PACK_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_icon_pack)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_WALLPAPER_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_wallpaper)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_FONT_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_font_size)
        }
        if (queryTokens.any { tokenMatchesAny(it, APPEARANCE_LAYOUT_TOKENS) }) {
            return context.getString(R.string.settings_search_description_change_layout)
        }
        return null
    }

    private fun findBestMatchingKeyword(
        keywords: List<String>,
        queryTokens: List<String>,
    ): String? {
        if (keywords.isEmpty() || queryTokens.isEmpty()) return null

        return keywords
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { keyword ->
                val normalizedKeywordTokens =
                    WHITESPACE_REGEX.split(keyword.lowercase()).filter { it.isNotBlank() }
                val score =
                    queryTokens.count { queryToken ->
                        normalizedKeywordTokens.any { keywordToken ->
                            tokenMatches(queryToken, keywordToken)
                        }
                    }
                keyword to score
            }.filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun tokenMatchesAny(
        queryToken: String,
        candidates: Set<String>,
    ): Boolean = candidates.any { candidate -> tokenMatches(queryToken, candidate) }

    private fun tokenMatches(
        queryToken: String,
        candidateToken: String,
    ): Boolean {
        val query = queryToken.trim().lowercase()
        val candidate = candidateToken.trim().lowercase()
        if (query.isEmpty() || candidate.isEmpty()) return false
        return query == candidate || query.startsWith(candidate) || candidate.startsWith(query)
    }

    private companion object {
        val APPEARANCE_THEME_TOKENS = setOf("themes", "dark", "light", "system", "background")
        val APPEARANCE_ICON_PACK_TOKENS = setOf("icon", "packs")
        val APPEARANCE_WALLPAPER_TOKENS =
            setOf("wallpaper", "blur", "transparency")
        val APPEARANCE_FONT_TOKENS = setOf("fonts", "size", "text")
        val APPEARANCE_LAYOUT_TOKENS =
            setOf("layout", "one handed", "bottom", "searchbar")
    }
}
