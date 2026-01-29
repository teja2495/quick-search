package com.tk.quicksearch.search.fuzzy

/**
 * Configuration for fuzzy search behavior.
 * Centralized configuration to avoid scattering across multiple files.
 */
data class FuzzySearchConfig(
    val enabled: Boolean = false,
    val matchThreshold: Int = 70,
    val minQueryLength: Int = 3,
    val priority: Int = 5,
) {
    companion object {
        /**
         * Default configuration for app search.
         */
        val DEFAULT_APP_CONFIG =
            FuzzySearchConfig(
                enabled = false,
                matchThreshold = 70,
                minQueryLength = 3,
                priority = 5,
            )

        /**
         * Creates a config from individual parameters.
         */
        fun create(
            enabled: Boolean = DEFAULT_APP_CONFIG.enabled,
            matchThreshold: Int = DEFAULT_APP_CONFIG.matchThreshold,
            minQueryLength: Int = DEFAULT_APP_CONFIG.minQueryLength,
            priority: Int = DEFAULT_APP_CONFIG.priority,
        ) = FuzzySearchConfig(enabled, matchThreshold, minQueryLength, priority)
    }

    /**
     * Validates the configuration parameters.
     */
    fun validate(): Boolean =
        matchThreshold in 0..100 &&
            minQueryLength >= 1 &&
            priority >= 0
}
