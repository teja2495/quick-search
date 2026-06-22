package com.tk.quicksearch.search.core

enum class AppSuggestionTabType {
    NEW_UPDATED,
    PINNED,
    RECENTS,
    MOST_USED,
    ALL_APPS,
    ;

    companion object {
        val DefaultEnabledTabs: Set<AppSuggestionTabType> =
            setOf(NEW_UPDATED, PINNED, RECENTS, MOST_USED)

        fun parseEnabledTabs(rawValues: Set<*>?): Set<AppSuggestionTabType> {
            if (rawValues == null) return DefaultEnabledTabs
            val tabs =
                rawValues
                .mapNotNull { value -> (value as? String)?.let { runCatching { valueOf(it) }.getOrNull() } }
                .toSet()
            return tabs.ifEmpty { DefaultEnabledTabs }
        }
    }
}
