package com.tk.quicksearch.widgets.worldClockWidget

import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/** A searchable place: a canonical time zone with its city and country in the user's language. */
internal data class WorldClockZone(
    val zoneId: String,
    val city: String,
    val country: String,
    /** The zone's generic name, e.g. "Pacific Time". */
    val zoneName: String,
    /** Other words the place can be found by: English country name, country code and nicknames. */
    val keywords: String,
) {
    /** Lower-cased, accent-free text that a search query is matched against. */
    internal val searchCity: String = WorldClockZones.normalize(city)
    internal val searchCountry: String = WorldClockZones.normalize("$country $keywords")
    internal val searchOther: String = WorldClockZones.normalize("$zoneName ${zoneId.replace('_', ' ')}")
}

/** The places a World Clock widget can show, from the time zone database on the device. */
internal object WorldClockZones {
    private val nonSpacingMarks = Regex("\\p{Mn}+")

    /** Short or informal country names people search by, keyed by ISO country code. */
    private val countryAliases =
        mapOf(
            "US" to "usa america united states",
            "GB" to "uk england britain great britain scotland wales",
            "AE" to "uae emirates dubai",
            "KR" to "korea south korea",
            "KP" to "north korea",
            "RU" to "russia",
            "CZ" to "czech republic czechia",
            "NL" to "holland netherlands",
            "TR" to "turkey turkiye",
            "CI" to "ivory coast",
            "MM" to "burma",
            "CD" to "congo drc",
            "CG" to "congo",
            "VN" to "vietnam",
            "IR" to "iran",
            "SY" to "syria",
            "LA" to "laos",
            "TW" to "taiwan",
            "BO" to "bolivia",
            "VE" to "venezuela",
            "TZ" to "tanzania",
            "MD" to "moldova",
        )

    @Volatile private var cached: Pair<Locale, List<WorldClockZone>>? = null

    fun isValid(zoneId: String): Boolean = runCatching { ZoneId.of(zoneId) }.isSuccess

    /** Every city time zone (one per tz database location), sorted by city. Slow on first call. */
    fun all(locale: Locale = Locale.getDefault()): List<WorldClockZone> {
        cached?.takeIf { it.first == locale }?.let { return it.second }
        val names = TimeZoneNames.getInstance(locale)
        val zones =
            TimeZone
                .getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null)
                .filter(::isValid)
                .map { id -> zoneFor(id, locale, names) }
                .sortedBy { it.searchCity }
        cached = locale to zones
        return zones
    }

    /**
     * Places where every word of [query] starts a word of the city, country or zone name, or appears
     * in them. Cities matching the first word come first, then places in a matching country.
     */
    fun search(
        query: String,
        zones: List<WorldClockZone>,
    ): List<WorldClockZone> {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return zones
        val first = words.first()
        return zones
            .filter { zone ->
                words.all { zone.searchCity.contains(it) || zone.searchCountry.contains(it) || zone.searchOther.contains(it) }
            }.sortedBy { zone ->
                when {
                    zone.searchCity.startsWith(first) -> 0
                    zone.searchCity.contains(first) -> 1
                    zone.searchCountry.startsWith(first) || zone.searchCountry.contains(" $first") -> 2
                    zone.searchCountry.contains(first) -> 3
                    else -> 4
                }
            }
    }

    /** The city a zone is named after, e.g. "New York" for America/New_York. */
    fun cityName(
        zoneId: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val canonical = canonicalId(zoneId)
        return runCatching { TimeZoneNames.getInstance(locale).getExemplarLocationName(canonical) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: canonical.substringAfterLast('/').replace('_', ' ')
    }

    /** The country a zone is in, e.g. "India" for Asia/Kolkata; empty for zones without one. */
    fun countryName(
        zoneId: String,
        locale: Locale = Locale.getDefault(),
    ): String =
        regionOf(canonicalId(zoneId))
            ?.let { Locale("", it).getDisplayCountry(locale) }
            .orEmpty()

    /** What a clock is titled: its custom label, or else its city. */
    fun displayName(location: WorldClockLocation): String =
        location.label.trim().ifEmpty { cityName(location.zoneId) }

    /**
     * How far [zoneId] is ahead of (+) or behind (−) this device right now, e.g. "+5:30" or "−8:00";
     * null when both show the same time.
     */
    fun offsetFromDevice(
        zoneId: String,
        now: Instant = Instant.now(),
    ): String? {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrNull() ?: return null
        val diffSeconds =
            zone.rules.getOffset(now).totalSeconds -
                ZoneId.systemDefault().rules.getOffset(now).totalSeconds
        if (diffSeconds == 0) return null
        val minutes = abs(diffSeconds) / 60
        val sign = if (diffSeconds > 0) "+" else "−"
        return String.format(Locale.getDefault(), "%s%d:%02d", sign, minutes / 60, minutes % 60)
    }

    fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(nonSpacingMarks, "")
            .trim()

    private fun canonicalId(zoneId: String): String =
        runCatching { TimeZone.getCanonicalID(zoneId) }.getOrNull() ?: zoneId

    /** The zone's two-letter ISO country code, or null for zones such as UTC that have none. */
    private fun regionOf(zoneId: String): String? =
        runCatching { TimeZone.getRegion(zoneId) }
            .getOrNull()
            ?.takeIf { it.length == 2 && it != "001" }

    private fun zoneFor(
        id: String,
        locale: Locale,
        names: TimeZoneNames,
    ): WorldClockZone {
        val city =
            runCatching { names.getExemplarLocationName(id) }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: id.substringAfterLast('/').replace('_', ' ')
        val region = regionOf(id)
        val countryLocale = region?.let { Locale("", it) }
        val country = countryLocale?.getDisplayCountry(locale).orEmpty()
        val keywords =
            listOfNotNull(
                countryLocale?.getDisplayCountry(Locale.ENGLISH),
                region,
                region?.let { countryAliases[it] },
            ).joinToString(" ")
        val zoneName =
            runCatching { TimeZone.getTimeZone(id).getDisplayName(false, TimeZone.LONG_GENERIC, locale) }
                .getOrNull()
                .orEmpty()
        return WorldClockZone(zoneId = id, city = city, country = country, zoneName = zoneName, keywords = keywords)
    }
}
