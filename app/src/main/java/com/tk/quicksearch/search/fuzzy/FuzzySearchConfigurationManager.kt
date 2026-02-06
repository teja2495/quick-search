package com.tk.quicksearch.search.fuzzy

/**
 * Centralized manager for fuzzy search configuration.
 * Handles configuration for all fuzzy search strategies across the app.
 */
class FuzzySearchConfigurationManager(
    private val preferences: FuzzySearchPreferences,
) {
    constructor(uiPreferences: com.tk.quicksearch.search.data.preferences.UiPreferences) :
        this(UiPreferencesFuzzySearchAdapter(uiPreferences))

    /**
     * Gets the current fuzzy search configuration for apps.
     */
    fun getAppFuzzyConfig(): FuzzySearchConfig =
        FuzzySearchConfig.create(
            matchThreshold = preferences.getAppFuzzyMatchThreshold(),
            minQueryLength = preferences.getAppFuzzyMinQueryLength(),
            priority = preferences.getAppFuzzyPriority(),
        )

    /**
     * Updates the app fuzzy search configuration.
     */
    fun updateAppFuzzyConfig(config: FuzzySearchConfig) {
        preferences.setAppFuzzyMatchThreshold(config.matchThreshold)
        preferences.setAppFuzzyMinQueryLength(config.minQueryLength)
        preferences.setAppFuzzyPriority(config.priority)
    }

    companion object {
        /**
         * Creates a manager with default configuration (for testing).
         */
        fun createWithDefaults(): FuzzySearchConfigurationManager = FuzzySearchConfigurationManager(DefaultFuzzySearchPreferences())
    }

    /**
     * Default implementation for testing or when UiPreferences is not available.
     */
    private class DefaultFuzzySearchPreferences : FuzzySearchPreferences {
        override fun getAppFuzzyMatchThreshold(): Int = FuzzySearchConfig.DEFAULT_APP_CONFIG.matchThreshold

        override fun setAppFuzzyMatchThreshold(threshold: Int) {}

        override fun getAppFuzzyMinQueryLength(): Int = FuzzySearchConfig.DEFAULT_APP_CONFIG.minQueryLength

        override fun setAppFuzzyMinQueryLength(length: Int) {}

        override fun getAppFuzzyPriority(): Int = FuzzySearchConfig.DEFAULT_APP_CONFIG.priority

        override fun setAppFuzzyPriority(priority: Int) {}
    }
}

/**
 * Interface for fuzzy search preferences.
 * Abstracts the preference storage mechanism.
 */
interface FuzzySearchPreferences {
    fun getAppFuzzyMatchThreshold(): Int

    fun setAppFuzzyMatchThreshold(threshold: Int)

    fun getAppFuzzyMinQueryLength(): Int

    fun setAppFuzzyMinQueryLength(length: Int)

    fun getAppFuzzyPriority(): Int

    fun setAppFuzzyPriority(priority: Int)

    // Future: Add methods for other search types (contacts, files, etc.)
    // fun isContactFuzzySearchEnabled(): Boolean
    // fun getContactFuzzyMatchThreshold(): Int
    // etc.
}

/**
 * Adapter that implements FuzzySearchPreferences using UiPreferences.
 */
class UiPreferencesFuzzySearchAdapter(
    private val uiPreferences: com.tk.quicksearch.search.data.preferences.UiPreferences,
) : FuzzySearchPreferences {
    override fun getAppFuzzyMatchThreshold(): Int {
        return 70 // Default threshold
    }

    override fun setAppFuzzyMatchThreshold(threshold: Int) {
        // No-op: always use default
    }

    override fun getAppFuzzyMinQueryLength(): Int {
        return 3 // Default min length
    }

    override fun setAppFuzzyMinQueryLength(length: Int) {
        // No-op: always use default
    }

    override fun getAppFuzzyPriority(): Int {
        return 5 // Default priority
    }

    override fun setAppFuzzyPriority(priority: Int) {
        // No-op: always use default
    }
}
