package com.tk.quicksearch.search.fuzzy

import com.tk.quicksearch.search.core.SearchSection

private const val MEDIUM_QUERY_MIN_LENGTH = 3
private const val LONG_QUERY_MIN_LENGTH = 6
private const val DEFAULT_MIN_SCORE = 78
private const val ALIAS_MIN_SCORE = 72
private const val LOW_RAM_ALIAS_MIN_SCORE = 76
private const val APP_CANDIDATE_LIMIT = 2_000
private const val SMALL_SECTION_CANDIDATE_LIMIT = 500
private const val APP_SHORTCUT_CANDIDATE_LIMIT = 1_000

/**
 * Describes fuzzy-search thresholds and budgets for a search section.
 */
data class FuzzySearchPolicy(
    val enabled: Boolean,
    val minimumQueryLength: Int,
    val candidateLimit: Int,
    val minimumScore: Int,
    val maximumEditDistance: Int,
    val allowDistanceTwoForLongerQueries: Boolean,
    val lowRamOverride: LowRamFuzzySearchOverride = LowRamFuzzySearchOverride(),
) {
    fun supportsQuery(query: String): Boolean = enabled && query.trim().length >= minimumQueryLength

    fun effectiveMaximumEditDistance(query: String): Int {
        if (!allowDistanceTwoForLongerQueries || query.trim().length < LONG_QUERY_MIN_LENGTH) {
            return maximumEditDistance.coerceAtMost(1)
        }
        return maximumEditDistance
    }
}

data class LowRamFuzzySearchOverride(
    val enabled: Boolean? = null,
    val candidateLimit: Int? = null,
    val minimumScore: Int? = null,
    val maximumEditDistance: Int? = null,
    val allowDistanceTwoForLongerQueries: Boolean? = null,
)

object FuzzySearchPolicyResolver {
    fun effectivePolicy(
        section: SearchSection,
        query: String,
        isLowRamDevice: Boolean,
    ): FuzzySearchPolicy {
        val basePolicy = policyFor(section)
        val policy =
            if (isLowRamDevice) {
                basePolicy.withLowRamOverride()
            } else {
                basePolicy
            }

        val trimmedQueryLength = query.trim().length
        return policy.copy(
            enabled = policy.enabled && trimmedQueryLength >= policy.minimumQueryLength,
            maximumEditDistance = policy.effectiveMaximumEditDistance(query),
        )
    }

    fun policyFor(section: SearchSection): FuzzySearchPolicy =
        when (section) {
            SearchSection.APPS -> APP_POLICY
            SearchSection.CONTACTS -> CONTACT_POLICY
            SearchSection.FILES -> FILE_POLICY
            SearchSection.SETTINGS -> DEVICE_SETTINGS_POLICY
            SearchSection.APP_SETTINGS -> APP_SETTINGS_POLICY
            SearchSection.CALENDAR -> CALENDAR_POLICY
            SearchSection.APP_SHORTCUTS -> APP_SHORTCUT_POLICY
            SearchSection.NOTES -> NOTES_POLICY
        }

    private fun FuzzySearchPolicy.withLowRamOverride(): FuzzySearchPolicy =
        copy(
            enabled = lowRamOverride.enabled ?: enabled,
            candidateLimit = lowRamOverride.candidateLimit ?: candidateLimit,
            minimumScore = lowRamOverride.minimumScore ?: minimumScore,
            maximumEditDistance = lowRamOverride.maximumEditDistance ?: maximumEditDistance,
            allowDistanceTwoForLongerQueries =
                lowRamOverride.allowDistanceTwoForLongerQueries
                    ?: allowDistanceTwoForLongerQueries,
        )

    private val APP_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = APP_CANDIDATE_LIMIT,
            minimumScore = FuzzySearchConfig.DEFAULT_APP_CONFIG.matchThreshold,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    candidateLimit = SMALL_SECTION_CANDIDATE_LIMIT,
                    minimumScore = DEFAULT_MIN_SCORE,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val CONTACT_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = 600,
            minimumScore = ALIAS_MIN_SCORE,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    candidateLimit = 360,
                    minimumScore = LOW_RAM_ALIAS_MIN_SCORE,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val FILE_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = 700,
            minimumScore = ALIAS_MIN_SCORE,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    enabled = false,
                    candidateLimit = 420,
                    minimumScore = LOW_RAM_ALIAS_MIN_SCORE,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val DEVICE_SETTINGS_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = SMALL_SECTION_CANDIDATE_LIMIT,
            minimumScore = DEFAULT_MIN_SCORE,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    minimumScore = DEFAULT_MIN_SCORE + 2,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val APP_SETTINGS_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = SMALL_SECTION_CANDIDATE_LIMIT,
            minimumScore = DEFAULT_MIN_SCORE,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    minimumScore = DEFAULT_MIN_SCORE + 2,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val CALENDAR_POLICY =
        FuzzySearchPolicy(
            enabled = false,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = 60,
            minimumScore = DEFAULT_MIN_SCORE,
            maximumEditDistance = 1,
            allowDistanceTwoForLongerQueries = false,
        )

    private val APP_SHORTCUT_POLICY =
        FuzzySearchPolicy(
            enabled = true,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = APP_SHORTCUT_CANDIDATE_LIMIT,
            minimumScore = DEFAULT_MIN_SCORE,
            maximumEditDistance = 2,
            allowDistanceTwoForLongerQueries = true,
            lowRamOverride =
                LowRamFuzzySearchOverride(
                    minimumScore = DEFAULT_MIN_SCORE + 2,
                    maximumEditDistance = 1,
                    allowDistanceTwoForLongerQueries = false,
                ),
        )

    private val NOTES_POLICY =
        FuzzySearchPolicy(
            enabled = false,
            minimumQueryLength = MEDIUM_QUERY_MIN_LENGTH,
            candidateLimit = SMALL_SECTION_CANDIDATE_LIMIT,
            minimumScore = DEFAULT_MIN_SCORE,
            maximumEditDistance = 1,
            allowDistanceTwoForLongerQueries = false,
        )
}
