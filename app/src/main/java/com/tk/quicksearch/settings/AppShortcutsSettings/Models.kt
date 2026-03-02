package com.tk.quicksearch.settings.AppShortcutsSettings

import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut

data class AppShortcutGroup(
    val packageName: String,
    val appLabel: String,
    val shortcuts: List<StaticShortcut>,
    val sources: List<AppShortcutSource>,
    val searchTargetSources: List<SearchTargetShortcutSource> = emptyList(),
)

data class SearchTargetShortcutSource(
    val target: SearchTarget,
    val packageName: String,
    val label: String,
    val kind: SearchTargetShortcutKind,
)

enum class SearchTargetShortcutKind {
    QUERY,
    URL,
}

enum class ShortcutSearchMatchPriority {
    EXACT,
    STARTS_WITH,
    WORD_STARTS_WITH,
    CONTAINS,
}

enum class ShortcutFilterOption(val labelResId: Int) {
    ALL(com.tk.quicksearch.R.string.settings_app_shortcuts_filter_all_apps),
    APPS_WITH_SHORTCUTS(com.tk.quicksearch.R.string.settings_app_shortcuts_filter_apps_with_shortcuts),
    SEARCH_ENGINES(com.tk.quicksearch.R.string.settings_app_shortcuts_filter_search_engines),
    BROWSERS(com.tk.quicksearch.R.string.settings_app_shortcuts_filter_browsers),
}