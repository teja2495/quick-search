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
 * - **Special Cards**: Calculator Result, Direct Search Result
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
        DIRECT_SEARCH_RESULT,

        // Section groups (contain multiple search results)
        APPS_SECTION,
        APP_SHORTCUTS_SECTION,
        FILES_SECTION,
        CONTACTS_SECTION,
        SETTINGS_SECTION,

        // Suggestion items
        WEB_SUGGESTIONS,
        RECENT_QUERIES,
        SEARCH_ENGINES_INLINE,
        SEARCH_ENGINES_COMPACT,

        // Other
        ERROR_BANNER,
        NO_RESULTS_MESSAGE
    }

    /**
     * Item rendering order when query is present (searching state).
     *
     * Layout:
     * 1. Error banner (if present)
     * 2. Calculator result (if query matches calculation)
     * 3. Direct search result (if available)
     * 4. Search result sections (in priority order)
     * 5. Web suggestions (if no results)
     * 6. Inline search engines (if enabled)
     * 7. No results message (if nothing found)
     *
     * Section priority within search results: APPS > APP_SHORTCUTS > FILES > CONTACTS > SETTINGS
     */
    val SEARCHING_STATE_LAYOUT: List<ItemType> = listOf(
        ItemType.ERROR_BANNER,
        ItemType.CALCULATOR_RESULT,
        ItemType.DIRECT_SEARCH_RESULT,
        // Search result sections (sub-ordered by section priority)
        ItemType.APPS_SECTION,
        ItemType.APP_SHORTCUTS_SECTION,
        ItemType.FILES_SECTION,
        ItemType.CONTACTS_SECTION,
        ItemType.SETTINGS_SECTION,
        // Fallback suggestions
        ItemType.WEB_SUGGESTIONS,
        ItemType.SEARCH_ENGINES_INLINE,
        ItemType.NO_RESULTS_MESSAGE
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
     * Section priority: APPS > APP_SHORTCUTS > FILES > CONTACTS > SETTINGS
     */
    val APP_OPEN_STATE_LAYOUT: List<ItemType> = listOf(
        ItemType.ERROR_BANNER,
        // Pinned sections (in priority order)
        ItemType.APPS_SECTION,
        ItemType.APP_SHORTCUTS_SECTION,
        ItemType.FILES_SECTION,
        ItemType.CONTACTS_SECTION,
        ItemType.SETTINGS_SECTION,
        // Recent queries/suggestions
        ItemType.RECENT_QUERIES
    )

    /**
     * Section priority order - used within each layout for rendering sections.
     *
     * Priority (highest to lowest): APPS > APP_SHORTCUTS > FILES > CONTACTS > SETTINGS
     */
    val searchingStatePriority: List<SearchSection> = listOf(
        SearchSection.APPS,
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS
    )

    /**
     * Section priority order for app open state (pinned items and recent apps).
     *
     * Priority (highest to lowest): APPS > APP_SHORTCUTS > FILES > CONTACTS > SETTINGS
     */
    val appOpenStatePriority: List<SearchSection> = listOf(
        SearchSection.APPS,
        SearchSection.APP_SHORTCUTS,
        SearchSection.FILES,
        SearchSection.CONTACTS,
        SearchSection.SETTINGS
    )

    /**
     * Gets the complete layout order based on the current query state.
     *
     * @param hasQuery true when query is present (searching), false when query is absent (app open)
     * @return ordered list of all items to render
     */
    fun getLayoutOrder(hasQuery: Boolean): List<ItemType> {
        return if (hasQuery) SEARCHING_STATE_LAYOUT else APP_OPEN_STATE_LAYOUT
    }

    /**
     * Gets the complete layout for searching state (query present).
     * Includes calculator, direct search, all sections, web suggestions, and search engines.
     *
     * @return ordered list of items for searching state
     */
    fun getSearchingStateOrder(): List<ItemType> {
        return SEARCHING_STATE_LAYOUT
    }

    /**
     * Gets the complete layout for app open state (no query).
     * Includes pinned sections and recent queries.
     *
     * @return ordered list of items for app open state
     */
    fun getAppOpenStateOrder(): List<ItemType> {
        return APP_OPEN_STATE_LAYOUT
    }

    /**
     * Gets the priority order for search results sections.
     * This is used when rendering section results (query present state).
     *
     * @return ordered list of sections by search result priority
     */
    fun getSearchResultsPriority(): List<SearchSection> {
        return searchingStatePriority
    }

    /**
     * Gets the priority order for pinned items sections and app open state.
     * This is used when displaying pinned sections and recent apps (query absent state).
     *
     * @return ordered list of sections by app open state priority
     */
    fun getAppOpenPriority(): List<SearchSection> {
        return appOpenStatePriority
    }

    /**
     * Gets the priority order based on the current query state.
     * Returns the appropriate section ordering for the current state.
     *
     * @param hasQuery true when query is present (searching), false when query is absent (app open)
     * @return ordered list of sections by priority
     */
    fun getPriorityOrder(hasQuery: Boolean): List<SearchSection> {
        return if (hasQuery) searchingStatePriority else appOpenStatePriority
    }
}
