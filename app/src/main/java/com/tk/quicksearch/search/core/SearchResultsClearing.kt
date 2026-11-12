package com.tk.quicksearch.search.core

/** Clears every secondary section's results and web suggestions, leaving app results and tools as they are. */
internal fun SearchResultsState.withoutSecondaryResults(): SearchResultsState =
    copy(
        contactResults = emptyList(),
        fileResults = emptyList(),
        settingResults = emptyList(),
        appSettingResults = emptyList(),
        appShortcutResults = emptyList(),
        calendarEvents = emptyList(),
        reminderResults = emptyList(),
        bookmarkResults = emptyList(),
        notedResults = emptyList(),
        noteResults = emptyList(),
        webSuggestions = emptyList(),
        webSuggestionsLoading = false,
    )
