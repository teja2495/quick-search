package com.tk.quicksearch.search.core

/**
 * Centralized configuration for ALL item priorities in the search screen.
 * Defines the rendering order for every type of item displayed, both when query is present (searching)
 * and when it's absent (app open state).
 *
 * This is the **single source of truth** for all item ordering throughout the application.
 *
 * ### Item Types Covered
 * - **Sections**: Apps, App Shortcuts, Files, Contacts, Settings
 * - **Special Cards**: Calculator Result, Currency Converter Result, Direct Search Result
 * - **Suggestions**: Web Suggestions, Recent Queries
 * - **Search Engines**: Inline/Compact search engine cards
 *
 * ### Usage
 * - **Search Results (Query Present)**: Use [getSearchingStateOrder] when displaying search results
 * - **App Open State (No Query)**: Use [getAppOpenStateOrder] when displaying pinned items and recent apps
 * - **Section Order Only**: Use [getSearchResultsPriority] for section ordering
 * - **Dynamic**: Use [getPriorityOrder] when you need ordering based on query state
 *
 * ### Customization
 * To change item ordering globally, modify only [SEARCHING_STATE_LAYOUT] or [APP_OPEN_STATE_LAYOUT].
 * All ordering throughout the app uses these configurations as the source of truth.
 *
 * ### Layout Structure
 * Each layout is a list of items describing what gets rendered and in what order.
 * Some items include sub-ordering (e.g., section order within search results).
 */
object ItemPriorityConfig {
    /**
     * Enum representing different item types that can be displayed in the search screen.
     * Used to document and configure rendering priorities.
     */
    enum class ItemType {
        // Special result items (always evaluated first)
        CALCULATOR_RESULT,
        CURRENCY_CONVERTER_RESULT,
        DIRECT_SEARCH_RESULT,

        // Section groups (contain multiple search results)
        APPS_SECTION,
        APP_SHORTCUTS_SECTION,
        FILES_SECTION,
        CONTACTS_SECTION,
        SETTINGS_SECTION,
        CALENDAR_SECTION,
        APP_SETTINGS_SECTION,

        // Suggestion items
        WEB_SUGGESTIONS,
        RECENT_QUERIES,
        SEARCH_ENGINES_INLINE,
        SEARCH_ENGINES_COMPACT,

        // Other
        ERROR_BANNER,
        NO_RESULTS_MESSAGE,
    }

    /**
     * Item rendering order when query is present (searching state).
     *
     * Layout:
     * 1. Error banner (if present)
     * 2. Calculator result (if query matches calculation)
     * 3. Currency converter result (if query matches conversion)
     * 4. Direct search result (if available)
     * 5. Search result sections (in priority order)
     * 6. Web suggestions (if no results)
     * 7. Inline search engines (if enabled)
     * 8. No results message (if nothing found)
     *
     * Section priority within search results: APPS > APP_SHORTCUTS > CONTACTS > FILES > CALENDAR > SETTINGS > APP_SETTINGS
     */
    val SEARCHING_STATE_LAYOUT: List<ItemType> =
        listOf(
            ItemType.ERROR_BANNER,
            ItemType.CALCULATOR_RESULT,
            ItemType.CURRENCY_CONVERTER_RESULT,
            ItemType.DIRECT_SEARCH_RESULT,
            // Search result sections (sub-ordered by section priority)
            ItemType.APPS_SECTION,
            ItemType.APP_SHORTCUTS_SECTION,
            ItemType.CONTACTS_SECTION,
            ItemType.FILES_SECTION,
            ItemType.CALENDAR_SECTION,
            ItemType.SETTINGS_SECTION,
            ItemType.APP_SETTINGS_SECTION,
            // Fallback suggestions
            ItemType.WEB_SUGGESTIONS,
            ItemType.SEARCH_ENGINES_INLINE,
            ItemType.NO_RESULTS_MESSAGE,
        )

    /**
     * Item rendering order when query is absent (app open state).
     *
     * Layout:
     * 1. Error banner (if present)
     * 2. Pinned app sections (in priority order)
     * 3. Recent queries
     * 4. Recent apps (displayed after pinned apps)
     *
     * Section priority: APPS > APP_SHORTCUTS > CONTACTS > FILES > CALENDAR > SETTINGS > APP_SETTINGS
     */
    val APP_OPEN_STATE_LAYOUT: List<ItemType> =
        listOf(
            ItemType.ERROR_BANNER,
            // Pinned sections (in priority order)
            ItemType.APPS_SECTION,
            ItemType.APP_SHORTCUTS_SECTION,
            ItemType.CONTACTS_SECTION,
            ItemType.FILES_SECTION,
            ItemType.CALENDAR_SECTION,
            ItemType.SETTINGS_SECTION,
            ItemType.APP_SETTINGS_SECTION,
            // Recent queries/suggestions
            ItemType.RECENT_QUERIES,
        )

    /**
     * Section priority order - used within each layout for rendering sections.
     *
     * Priority (highest to lowest): APPS > APP_SHORTCUTS > CONTACTS > FILES > CALENDAR > SETTINGS > APP_SETTINGS
     */
    val searchingStatePriority: List<SearchSection> =
        listOf(
            SearchSection.APPS,
            SearchSection.APP_SHORTCUTS,
            SearchSection.CONTACTS,
            SearchSection.FILES,
            SearchSection.CALENDAR,
            SearchSection.SETTINGS,
            SearchSection.APP_SETTINGS,
        )

    /**
     * Section priority order for app open state (pinned items and recent apps).
     *
     * Priority (highest to lowest): APPS > APP_SHORTCUTS > CONTACTS > FILES > CALENDAR > SETTINGS > APP_SETTINGS
     */
    val appOpenStatePriority: List<SearchSection> =
        listOf(
            SearchSection.APPS,
            SearchSection.APP_SHORTCUTS,
            SearchSection.CONTACTS,
            SearchSection.FILES,
            SearchSection.CALENDAR,
            SearchSection.SETTINGS,
            SearchSection.APP_SETTINGS,
        )

    /**
     * Gets the complete layout order based on the current query state.
     *
     * @param hasQuery true when query is present (searching), false when query is absent (app open)
     * @return ordered list of all items to render
     */
    fun getLayoutOrder(hasQuery: Boolean): List<ItemType> = if (hasQuery) SEARCHING_STATE_LAYOUT else APP_OPEN_STATE_LAYOUT

    /**
     * Gets the priority order for search results sections.
     * This is used when rendering section results (query present state).
     *
     * @return ordered list of sections by search result priority
     */
    fun getSearchResultsPriority(): List<SearchSection> = searchingStatePriority
}
