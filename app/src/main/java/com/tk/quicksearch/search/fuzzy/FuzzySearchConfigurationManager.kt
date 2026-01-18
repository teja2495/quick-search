package com.tk.quicksearch.search.fuzzy

/**
 * Centralized manager for fuzzy search configuration.
 * Handles configuration for all fuzzy search strategies across the app.
 */
class FuzzySearchConfigurationManager(
    private val preferences: FuzzySearchPreferences
) {

    constructor(uiPreferences: com.tk.quicksearch.search.data.preferences.UiPreferences) :
        this(UiPreferencesFuzzySearchAdapter(uiPreferences))

    /**
     * Gets the current fuzzy search configuration for apps.
     */
    fun getAppFuzzyConfig(): FuzzySearchConfig {
        return FuzzySearchConfig.create(
            enabled = preferences.isAppFuzzySearchEnabled(),
            matchThreshold = preferences.getAppFuzzyMatchThreshold(),
            minQueryLength = preferences.getAppFuzzyMinQueryLength(),
            priority = preferences.getAppFuzzyPriority()
        )
    }

    /**
     * Updates the app fuzzy search configuration.
     */
    fun updateAppFuzzyConfig(config: FuzzySearchConfig) {
        preferences.setAppFuzzySearchEnabled(config.enabled)
        preferences.setAppFuzzyMatchThreshold(config.matchThreshold)
        preferences.setAppFuzzyMinQueryLength(config.minQueryLength)
        preferences.setAppFuzzyPriority(config.priority)
    }

    /**
     * Enables or disables fuzzy search for apps.
     */
    fun setAppFuzzySearchEnabled(enabled: Boolean) {
        preferences.setAppFuzzySearchEnabled(enabled)
    }

    /**
     * Gets the current enabled state for app fuzzy search.
     */
    fun isAppFuzzySearchEnabled(): Boolean {
        return preferences.isAppFuzzySearchEnabled()
    }

    companion object {
        /**
         * Creates a manager with default configuration (for testing).
         */
        fun createWithDefaults(): FuzzySearchConfigurationManager {
            return FuzzySearchConfigurationManager(DefaultFuzzySearchPreferences())
        }
    }

    /**
     * Default implementation for testing or when UiPreferences is not available.
     */
    private class DefaultFuzzySearchPreferences : FuzzySearchPreferences {
        override fun isAppFuzzySearchEnabled(): Boolean = FuzzySearchConfig.DEFAULT_APP_CONFIG.enabled
        override fun setAppFuzzySearchEnabled(enabled: Boolean) {}
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

    // App fuzzy search settings
    fun isAppFuzzySearchEnabled(): Boolean
    fun setAppFuzzySearchEnabled(enabled: Boolean)

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
    private val uiPreferences: com.tk.quicksearch.search.data.preferences.UiPreferences
) : FuzzySearchPreferences {

    override fun isAppFuzzySearchEnabled(): Boolean {
        return uiPreferences.isFuzzyAppSearchEnabled()
    }

    override fun setAppFuzzySearchEnabled(enabled: Boolean) {
        uiPreferences.setFuzzyAppSearchEnabled(enabled)
    }

    override fun getAppFuzzyMatchThreshold(): Int {
        return uiPreferences.getFuzzyAppSearchMatchThreshold()
    }

    override fun setAppFuzzyMatchThreshold(threshold: Int) {
        uiPreferences.setFuzzyAppSearchMatchThreshold(threshold)
    }

    override fun getAppFuzzyMinQueryLength(): Int {
        return uiPreferences.getFuzzyAppSearchMinQueryLength()
    }

    override fun setAppFuzzyMinQueryLength(length: Int) {
        uiPreferences.setFuzzyAppSearchMinQueryLength(length)
    }

    override fun getAppFuzzyPriority(): Int {
        return uiPreferences.getFuzzyAppSearchPriority()
    }

    override fun setAppFuzzyPriority(priority: Int) {
        uiPreferences.setFuzzyAppSearchPriority(priority)
    }
}