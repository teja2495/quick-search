package com.tk.quicksearch.tools.aiTools

import android.content.Context
import android.util.Xml
import com.tk.quicksearch.R
import com.tk.quicksearch.tools.aiSearch.trackCancellation
import com.tk.quicksearch.tools.aiSearch.withCancellableHttp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.util.Currency
import java.util.Locale
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

// EUR-based rates for ~200 currencies, served from two mirrors. Both chains end at roots that
// Android 7 already trusts, unlike ECB's Sectigo Root E46 chain, so ECB is only the last fallback.
private const val CURRENCY_API_URL =
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.json"
private const val CURRENCY_API_MIRROR_URL = "https://latest.currency-api.pages.dev/v1/currencies/eur.json"
private const val ECB_RATES_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
private const val RATES_PREFERENCES = "currency_exchange_rates"
// v2: snapshots from the wider currency list; older ECB-only snapshots are ignored.
private const val RATES_JSON_KEY = "rates_v2_json"
private const val RATES_FETCHED_AT_KEY = "rates_v2_fetched_at"
private const val LEGACY_RATES_JSON_KEY = "rates_json"
private const val LEGACY_RATES_FETCHED_AT_KEY = "rates_fetched_at"
private val CURRENCY_CODE_REGEX = Regex("[a-z]{3}")
private const val CACHE_DURATION_MS = 4 * 60 * 60 * 1000L
private val CALCULATION_CONTEXT = MathContext(16, RoundingMode.HALF_UP)

class CurrencyConverterHandler(
        private val context: Context,
) {
    private val preferences = context.getSharedPreferences(RATES_PREFERENCES, Context.MODE_PRIVATE)

    /** Fetches one complete EUR-based rate snapshot on demand, then reuses it for four hours. */
    suspend fun convert(confirmed: ConfirmedCurrencyQuery): Result<CurrencyConversionModelResult> =
            withCancellableHttp {
                runCatching {
                    val ratesSnapshot = loadRates()
                    val sourceRate = ratesSnapshot.rates[confirmed.fromCurrency]
                            ?: error(unsupportedCurrencyMessage(confirmed.fromCurrency))
                    val targetRate = ratesSnapshot.rates[confirmed.toCurrency]
                            ?: error(unsupportedCurrencyMessage(confirmed.toCurrency))
                    val converted = confirmed.amount.toBigDecimal()
                            .multiply(targetRate, CALCULATION_CONTEXT)
                            .divide(sourceRate, CALCULATION_CONTEXT)
                    CurrencyConversionModelResult(
                            convertedAmount = converted.stripTrailingZeros().toPlainString(),
                            targetCurrencyCode = confirmed.toCurrency,
                            targetCurrencyName = currencyDisplayName(confirmed.toCurrency),
                            sourceAmount = confirmed.amount,
                            sourceCurrencyCode = confirmed.fromCurrency,
                            ratesFetchedAtMillis = ratesSnapshot.fetchedAtMillis,
                    )
                }
            }

    private suspend fun loadRates(): CachedRates {
        readCachedRates()?.takeIf { it.fetchedAtMillis + CACHE_DURATION_MS > System.currentTimeMillis() }
                ?.let { return it }
        val rates = fetchRates()
        currentCoroutineContext().ensureActive()
        return saveRates(rates)
    }

    /** Tries each source in order; raw network/TLS errors are replaced with a readable message. */
    private suspend fun fetchRates(): Map<String, BigDecimal> {
        val sources = listOf(
                CURRENCY_API_URL to ::parseCurrencyApiRates,
                CURRENCY_API_MIRROR_URL to ::parseCurrencyApiRates,
                ECB_RATES_URL to ::parseEcbRates,
        )
        for ((url, parse) in sources) {
            currentCoroutineContext().ensureActive()
            val rates = runCatching { fetchFrom(url, parse) }.getOrNull()
            currentCoroutineContext().ensureActive()
            if (rates != null) return rates
        }
        error(context.getString(R.string.currency_converter_rates_unavailable))
    }

    private fun fetchFrom(
            url: String,
            parse: (InputStream) -> Map<String, BigDecimal>,
    ): Map<String, BigDecimal> {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).trackCancellation().apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            val rates = connection.inputStream.use(parse)
            check(rates.size > 1) { "No rates" }
            return rates
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseCurrencyApiRates(input: InputStream): Map<String, BigDecimal> {
        val eurRates = JSONObject(input.bufferedReader().readText()).getJSONObject("eur")
        val rates = mutableMapOf("EUR" to BigDecimal.ONE)
        eurRates.keys().forEach { code ->
            if (!CURRENCY_CODE_REGEX.matches(code)) return@forEach
            val rate = eurRates.optString(code).toBigDecimalOrNull()
            if (rate != null && rate > BigDecimal.ZERO) rates[code.uppercase(Locale.ROOT)] = rate
        }
        return rates
    }

    private fun parseEcbRates(input: InputStream): Map<String, BigDecimal> {
        val rates = mutableMapOf("EUR" to BigDecimal.ONE)
        val parser = Xml.newPullParser().apply { setInput(input, null) }
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "Cube") continue
            val code = parser.getAttributeValue(null, "currency")?.uppercase(Locale.ROOT)
            val rate = parser.getAttributeValue(null, "rate")?.toBigDecimalOrNull()
            if (code != null && code.length == 3 && rate != null && rate > BigDecimal.ZERO) {
                rates[code] = rate
            }
        }
        return rates
    }

    private fun readCachedRates(): CachedRates? = runCatching {
        val fetchedAtMillis = preferences.getLong(RATES_FETCHED_AT_KEY, 0L)
        val rawRates = preferences.getString(RATES_JSON_KEY, null) ?: return null
        val json = JSONObject(rawRates)
        val rates = buildMap {
            json.keys().forEach { code ->
                json.optString(code).toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }?.let {
                    put(code, it)
                }
            }
        }
        if (fetchedAtMillis <= 0L || rates["EUR"] != BigDecimal.ONE || rates.size <= 1) null
        else CachedRates(fetchedAtMillis, rates)
    }.getOrNull()

    private fun saveRates(rates: Map<String, BigDecimal>): CachedRates {
        val fetchedAtMillis = System.currentTimeMillis()
        val json = JSONObject().apply {
            rates.forEach { (code, rate) -> put(code, rate.toPlainString()) }
        }
        preferences.edit().putString(RATES_JSON_KEY, json.toString())
                .putLong(RATES_FETCHED_AT_KEY, fetchedAtMillis)
                .remove(LEGACY_RATES_JSON_KEY)
                .remove(LEGACY_RATES_FETCHED_AT_KEY)
                .apply()
        return CachedRates(fetchedAtMillis, rates)
    }

    private fun unsupportedCurrencyMessage(code: String): String =
            context.getString(R.string.currency_converter_unsupported_currency, code)

    private fun currencyDisplayName(code: String): String =
            runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)

    private data class CachedRates(
            val fetchedAtMillis: Long,
            val rates: Map<String, BigDecimal>,
    )
}

data class CurrencyConversionModelResult(
        val convertedAmount: String,
        val targetCurrencyCode: String,
        val targetCurrencyName: String,
        val sourceAmount: String,
        val sourceCurrencyCode: String,
        val ratesFetchedAtMillis: Long,
)
