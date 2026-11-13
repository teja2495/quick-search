package com.tk.quicksearch.widgets.worldClockWidget

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockZoneSearchTest {
    private fun zone(
        id: String,
        city: String,
        country: String,
        zone: String,
        otherCities: List<String> = emptyList(),
        countryAliases: List<String> = emptyList(),
        zoneAliases: List<String> = emptyList(),
    ) = WorldClockZone(
        zoneId = id,
        city = city,
        country = country,
        zoneName = zone,
        cityTerms = (listOf(city) + otherCities).map(WorldClockZones::normalize),
        countryTerms = (listOf(country) + countryAliases).map(WorldClockZones::normalize),
        zoneTerms = (listOf(zone) + zoneAliases).map(WorldClockZones::normalize),
    )

    private val newYork =
        zone("America/New_York", "New York", "United States", "Eastern Time", listOf("Indianapolis"), listOf("usa"), listOf("est"))
    private val kolkata = zone("Asia/Kolkata", "Kolkata", "India", "India Standard Time", zoneAliases = listOf("ist", "utc+5:30"))
    private val losAngeles = zone("America/Los_Angeles", "Los Angeles", "United States", "Pacific Time", zoneAliases = listOf("pst"))
    private val zones = listOf(kolkata, losAngeles, newYork)

    private fun ids(query: String) = WorldClockZoneSearch.search(query, zones).map { it.zoneId }

    @Test
    fun countryMatchBeatsLongerCityStartingTheSame() {
        assertEquals(listOf(kolkata.zoneId, newYork.zoneId), ids("india"))
    }

    @Test
    fun findsByTimeZoneAbbreviationAndOffset() {
        assertEquals(listOf(losAngeles.zoneId), ids("PST"))
        assertEquals(listOf(kolkata.zoneId), ids("utc+5:30"))
    }

    @Test
    fun findsFoldedCity() {
        assertEquals(listOf(newYork.zoneId), ids("indianapolis"))
    }

    @Test
    fun everyWordMustMatchSomewhere() {
        assertEquals(listOf(losAngeles.zoneId), ids("pacific united"))
        assertEquals(emptyList<String>(), ids("pacific india"))
    }

    @Test
    fun emptyQueryKeepsAllInOrder() {
        assertEquals(zones.map { it.zoneId }, ids("  "))
    }
}
