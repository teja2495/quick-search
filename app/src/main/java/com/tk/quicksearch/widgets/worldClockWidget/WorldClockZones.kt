package com.tk.quicksearch.widgets.worldClockWidget

import android.icu.text.TimeZoneNames
import android.icu.text.TimeZoneNames.NameType
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/**
 * A searchable place: one per time zone and country, shown as its capital or best-known city, with
 * the country and time zone in the user's language. Other cities on the same clock in that country
 * are folded into it, so they can be searched for.
 */
internal data class WorldClockZone(
    val zoneId: String,
    val city: String,
    val country: String,
    /** The zone's generic name, e.g. "Pacific Time". */
    val zoneName: String,
    /** Normalized names the place is found by: its city and the cities folded into it. */
    val cityTerms: List<String>,
    /** The cities folded into this place, as written, e.g. "San Francisco" for Pacific Time. */
    val otherCities: List<String> = emptyList(),
    /** Normalized country names: localized, English, ISO code and nicknames. */
    val countryTerms: List<String>,
    /** Normalized time zone names: generic, English, abbreviations such as "pst", and "utc+5:30". */
    val zoneTerms: List<String>,
) {
    internal val searchTerms: List<String> = cityTerms + countryTerms + zoneTerms
}

/** The places a World Clock widget can show, from the time zone database on the device. */
internal object WorldClockZones {
    private val nonSpacingMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    /** Short or informal country names people search by, keyed by ISO country code. */
    private val countryAliases =
        mapOf(
            "US" to listOf("usa", "america", "united states"),
            "GB" to listOf("uk", "england", "britain", "great britain", "scotland", "wales"),
            "AE" to listOf("uae", "emirates", "dubai"),
            "KR" to listOf("korea", "south korea"),
            "KP" to listOf("north korea"),
            "RU" to listOf("russia"),
            "CZ" to listOf("czech republic", "czechia"),
            "NL" to listOf("holland", "netherlands"),
            "TR" to listOf("turkey", "turkiye"),
            "CI" to listOf("ivory coast"),
            "MM" to listOf("burma"),
            "CD" to listOf("congo", "drc"),
            "CG" to listOf("congo"),
            "VN" to listOf("vietnam"),
            "IR" to listOf("iran"),
            "SY" to listOf("syria"),
            "LA" to listOf("laos"),
            "TW" to listOf("taiwan"),
            "BO" to listOf("bolivia"),
            "VE" to listOf("venezuela"),
            "TZ" to listOf("tanzania"),
            "MD" to listOf("moldova"),
        )

    /** English locales whose time zone abbreviations people search by, e.g. PST, BST, IST, AEST. */
    private val abbreviationLocales =
        listOf(ULocale.US, ULocale.UK, ULocale("en_IN"), ULocale("en_AU"), ULocale("en_NZ"), ULocale("en_CA"))

    @Volatile private var cached: Pair<Locale, List<WorldClockZone>>? = null

    fun isValid(zoneId: String): Boolean = runCatching { ZoneId.of(zoneId) }.isSuccess

    /**
     * One place per time zone in each country, sorted by country and zone: e.g. New York for every
     * US zone on Eastern Time, but Phoenix apart from Denver as it skips daylight saving. Slow on
     * first call.
     */
    fun all(locale: Locale = Locale.getDefault()): List<WorldClockZone> {
        cached?.takeIf { it.first == locale }?.let { return it.second }
        val names = TimeZoneNames.getInstance(locale)
        val now = System.currentTimeMillis()
        val zones =
            TimeZone
                .getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, null, null)
                .filter(::isValid)
                .groupBy { id -> sameClockKey(id, names, now) }
                .values
                .map { group -> zoneFor(group, locale, names, now) }
                .sortedWith(
                    compareBy<WorldClockZone>(
                        { it.country.isEmpty() },
                        { normalize(it.country) },
                        { normalize(it.zoneName) },
                        { it.cityTerms.first() },
                    ),
                )
        cached = locale to zones
        return zones
    }

    /** The city a zone is shown as, e.g. "New York" for America/New_York, "New Delhi" for Asia/Kolkata. */
    fun cityName(
        zoneId: String,
        locale: Locale = Locale.getDefault(),
    ): String = cityOf(canonicalId(zoneId), TimeZoneNames.getInstance(locale))

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

    fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(nonSpacingMarks, "")
            .replace(whitespace, " ")
            .trim()

    private fun canonicalId(zoneId: String): String =
        runCatching { TimeZone.getCanonicalID(zoneId) }.getOrNull() ?: zoneId

    /** The zone's two-letter ISO country code, or null for zones such as UTC that have none. */
    private fun regionOf(zoneId: String): String? =
        runCatching { TimeZone.getRegion(zoneId) }
            .getOrNull()
            ?.takeIf { it.length == 2 && it != "001" }

    /**
     * Zones that always show the same time in the same country share a key: same metazone (e.g.
     * America_Eastern), offsets and daylight saving. Zones without a metazone stay on their own.
     */
    private fun sameClockKey(
        id: String,
        names: TimeZoneNames,
        now: Long,
    ): String {
        val metaZone = runCatching { names.getMetaZoneID(id, now) }.getOrNull() ?: return id
        val zone = TimeZone.getTimeZone(id)
        return "${regionOf(id)}|$metaZone|${zone.getOffset(now)}|${zone.rawOffset}|${zone.useDaylightTime()}"
    }

    /** The zone a group of same-clock zones is shown as: the one CLDR names for that country, if any. */
    private fun representative(
        group: List<String>,
        names: TimeZoneNames,
        now: Long,
    ): String {
        if (group.size == 1) return group.first()
        val metaZone = runCatching { names.getMetaZoneID(group.first(), now) }.getOrNull()
        val preferred =
            metaZone?.let { mz ->
                listOfNotNull(regionOf(group.first()), "001")
                    .mapNotNull { region -> runCatching { names.getReferenceZoneID(mz, region) }.getOrNull() }
                    .firstOrNull { it in group }
            }
        return preferred ?: group.minWith(compareBy<String>({ id -> id.count { it == '/' } }, { it }))
    }

    /** The city [id] is shown as: a better-known one where set, such as New Delhi, else its namesake. */
    private fun cityOf(
        id: String,
        names: TimeZoneNames,
    ): String = WorldClockPopularCities.headline(id) ?: namesakeOf(id, names)

    private fun namesakeOf(
        id: String,
        names: TimeZoneNames,
    ): String =
        runCatching { names.getExemplarLocationName(id) }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: id.substringAfterLast('/').replace('_', ' ')

    private fun zoneFor(
        group: List<String>,
        locale: Locale,
        names: TimeZoneNames,
        now: Long,
    ): WorldClockZone {
        val id = representative(group, names, now)
        val city = cityOf(id, names)
        val region = regionOf(id)
        val countryLocale = region?.let { Locale("", it) }
        val country = countryLocale?.getDisplayCountry(locale).orEmpty()
        val zone = TimeZone.getTimeZone(id)
        val zoneName =
            runCatching { zone.getDisplayName(false, TimeZone.LONG_GENERIC, locale) }.getOrNull().orEmpty()
        // Every zone's namesake and the well-known cities on this clock, though only one is shown.
        val otherCities =
            group
                .flatMap { listOf(namesakeOf(it, names)) + WorldClockPopularCities.citiesIn(it) }
                .filter { it != city }
                .distinct()
        val countryTerms =
            listOfNotNull(country, countryLocale?.getDisplayCountry(Locale.ENGLISH), region) +
                region?.let { countryAliases[it] }.orEmpty()
        return WorldClockZone(
            zoneId = id,
            city = city,
            country = country,
            zoneName = zoneName,
            cityTerms = searchable(listOf(city) + otherCities),
            otherCities = otherCities,
            countryTerms = searchable(countryTerms),
            zoneTerms =
                searchable(
                    listOf(zoneName, id.replace('_', ' ').replace('/', ' ')) + zoneNameTerms(id, zone, now),
                ),
        )
    }

    /** English names and abbreviations of [id]'s time zone, plus its UTC offset, e.g. "utc+5:30". */
    private fun zoneNameTerms(
        id: String,
        zone: TimeZone,
        now: Long,
    ): List<String> {
        val english = TimeZoneNames.getInstance(ULocale.ENGLISH)
        val longTypes = listOf(NameType.LONG_GENERIC, NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT)
        val shortTypes = listOf(NameType.SHORT_GENERIC, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT)
        val longNames = longTypes.mapNotNull { runCatching { english.getDisplayName(id, it, now) }.getOrNull() }
        val abbreviations =
            abbreviationLocales
                .flatMap { locale ->
                    val localeNames = TimeZoneNames.getInstance(locale)
                    shortTypes.mapNotNull { runCatching { localeNames.getDisplayName(id, it, now) }.getOrNull() }
                }.filterNot { it.startsWith("GMT") || it.startsWith("UTC") }
        return longNames + abbreviations + offsetNames(zone.getOffset(now))
    }

    /** "utc+5:30" and "gmt+5:30" for +05:30, "utc-8" and "gmt-8" for -08:00, "utc" and "gmt" for 0. */
    private fun offsetNames(offsetMillis: Int): List<String> {
        val minutes = abs(offsetMillis) / 60_000
        val sign = if (offsetMillis > 0) "+" else "-"
        val offset =
            when {
                minutes == 0 -> ""
                minutes % 60 == 0 -> "$sign${minutes / 60}"
                else -> String.format(Locale.ROOT, "%s%d:%02d", sign, minutes / 60, minutes % 60)
            }
        return listOf("utc$offset", "gmt$offset")
    }

    private fun searchable(terms: List<String>): List<String> =
        terms.map(::normalize).filter { it.isNotEmpty() }.distinct()
}
