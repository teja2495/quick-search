package com.tk.quicksearch.searchEngines

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Picks a regional Amazon storefront from permission-free country signals (SIM, current
 * network, then the user's locale region). No location permission or network request is used.
 */
object AmazonRegionDetector {
    private val DOMAINS_BY_COUNTRY =
        mapOf(
            "AE" to "amazon.ae",
            "AT" to "amazon.de",
            "AU" to "amazon.com.au",
            "BE" to "amazon.com.be",
            "BR" to "amazon.com.br",
            "CA" to "amazon.ca",
            "DE" to "amazon.de",
            "EG" to "amazon.eg",
            "ES" to "amazon.es",
            "FR" to "amazon.fr",
            "GB" to "amazon.co.uk",
            "IE" to "amazon.ie",
            "IN" to "amazon.in",
            "IT" to "amazon.it",
            "JP" to "amazon.co.jp",
            "MX" to "amazon.com.mx",
            "NL" to "amazon.nl",
            "PL" to "amazon.pl",
            "SA" to "amazon.sa",
            "SE" to "amazon.se",
            "SG" to "amazon.sg",
            "TR" to "amazon.com.tr",
            "ZA" to "amazon.co.za",
        )

    /** Returns the regional domain for an ISO 3166 country code, or null to keep amazon.com. */
    fun domainForCountry(countryIso: String?): String? =
        countryIso
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.let(DOMAINS_BY_COUNTRY::get)

    /** Returns the first non-blank country code from [candidates], in order. */
    fun firstCountry(vararg candidates: String?): String? =
        candidates.firstOrNull { !it.isNullOrBlank() }?.trim()?.uppercase(Locale.ROOT)

    fun detectCountryIso(context: Context): String? {
        val telephony = runCatching { context.getSystemService(TelephonyManager::class.java) }.getOrNull()
        val simCountry = runCatching { telephony?.simCountryIso }.getOrNull()
        val networkCountry = runCatching { telephony?.networkCountryIso }.getOrNull()
        val localeCountry = LocaleListCompat.getDefault()[0]?.country
        return firstCountry(simCountry, networkCountry, localeCountry)
    }

    fun detectDomain(context: Context): String? = domainForCountry(detectCountryIso(context))
}
