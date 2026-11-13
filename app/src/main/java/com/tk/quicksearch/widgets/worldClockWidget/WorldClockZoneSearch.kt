package com.tk.quicksearch.widgets.worldClockWidget

/**
 * Finds places by city, country or time zone. Every word of the query must appear in one of a
 * place's names; places are then ranked by how well the whole query matches their best name,
 * whichever kind it is, so "india" puts India's clock ahead of cities that merely contain "india".
 */
internal object WorldClockZoneSearch {
    /** How a query matched one name: [tier] 0 is best; [coverage] is the share of the name it spans. */
    private data class Match(
        val tier: Int,
        val coverage: Float,
    )

    private const val TIER_EXACT = 0
    private const val TIER_PREFIX = 1
    private const val TIER_WORD_PREFIX = 2
    private const val TIER_CONTAINS = 3
    private const val TIER_WORDS_ONLY = 4

    private val ranking = compareBy<Pair<WorldClockZone, Match>>({ it.second.tier }, { -it.second.coverage })

    fun search(
        query: String,
        zones: List<WorldClockZone>,
    ): List<WorldClockZone> {
        val normalized = WorldClockZones.normalize(query)
        if (normalized.isEmpty()) return zones
        val words = normalized.split(' ')
        // sortedWith is stable, so equally good matches keep their order by country and zone.
        return zones
            .filter { zone -> words.all { word -> zone.searchTerms.any { it.contains(word) } } }
            .map { zone -> zone to bestMatch(normalized, zone.searchTerms) }
            .sortedWith(ranking)
            .map { it.first }
    }

    /**
     * The folded-in city [query] is looking for, e.g. "San Francisco" in the Pacific Time place, when
     * it matches that city better than the place's own city, country and zone names.
     */
    fun matchedCity(
        query: String,
        zone: WorldClockZone,
    ): String? {
        val normalized = WorldClockZones.normalize(query)
        if (normalized.isEmpty()) return null
        val ownTerms = zone.searchTerms - zone.otherCities.map(WorldClockZones::normalize).toSet()
        val own = bestMatch(normalized, ownTerms)
        return zone.otherCities
            .mapNotNull { city -> match(normalized, WorldClockZones.normalize(city))?.let { city to it } }
            .minWithOrNull(compareBy({ it.second.tier }, { -it.second.coverage }))
            ?.takeIf { (_, match) -> match.tier < own.tier }
            ?.first
    }

    private fun bestMatch(
        query: String,
        terms: List<String>,
    ): Match =
        terms
            .mapNotNull { match(query, it) }
            .minWithOrNull(compareBy<Match>({ it.tier }, { -it.coverage }))
            ?: Match(TIER_WORDS_ONLY, 0f)

    private fun match(
        query: String,
        term: String,
    ): Match? {
        if (term == query) return Match(TIER_EXACT, 1f)
        if (term.startsWith(query)) return Match(TIER_PREFIX, query.length.toFloat() / term.length)
        // Not at 0, which the prefix check above already covered.
        var index = term.indexOf(query)
        if (index < 0) return null
        while (index >= 0) {
            if (term[index - 1] == ' ') {
                // Share of the matched word(s), so "york" fully covers the "york" in "new york".
                val end = term.indexOf(' ', index + query.length).let { if (it < 0) term.length else it }
                return Match(TIER_WORD_PREFIX, query.length.toFloat() / (end - index))
            }
            index = term.indexOf(query, index + 1)
        }
        return Match(TIER_CONTAINS, query.length.toFloat() / term.length)
    }
}
