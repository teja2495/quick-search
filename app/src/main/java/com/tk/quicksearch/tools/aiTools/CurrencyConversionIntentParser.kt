package com.tk.quicksearch.tools.aiTools

/**
 * Detects and parses currency conversion queries.
 *
 * Supported formats (amount can be integer or decimal with . or ,):
 *   10 inr to usd
 *   10 indian rupees to dollars
 *   ₹10 to $
 *   10₹ to dollar
 *   ₹ 10 to usd
 *   100 inr in usd
 *   100 usd into eur
 *   100 usd = eur
 *
 * Two-stage: cheap candidate scan, then full structured parse.
 */
object CurrencyConversionIntentParser {

    /** Currency symbol → ISO 4217 code. Sorted longest-first for greedy matching. */
    private val SYMBOL_TO_CODE: Map<String, String> = linkedMapOf(
        "HK\$" to "HKD",
        "NZ\$" to "NZD",
        "A\$" to "AUD",
        "C\$" to "CAD",
        "S\$" to "SGD",
        "Kč" to "CZK",
        "Rp" to "IDR",
        "RM" to "MYR",
        "zł" to "PLN",
        "₹" to "INR",
        "\$" to "USD",
        "€" to "EUR",
        "£" to "GBP",
        "¥" to "JPY",
        "元" to "CNY",
        "₩" to "KRW",
        "₣" to "CHF",
        "฿" to "THB",
        "₫" to "VND",
        "₱" to "PHP",
        "₦" to "NGN",
        "₺" to "TRY",
        "₴" to "UAH",
        "₸" to "KZT",
        "﷼" to "SAR",
        "₭" to "LAK",
        "₮" to "MNT",
        "৳" to "BDT",
        "₲" to "PYG",
        "₡" to "CRC",
        "₵" to "GHS",
    )

    /** Currency name (lowercase, singular & plural) → ISO 4217 code. */
    private val NAME_TO_CODE: Map<String, String> = buildMap {
        fun add(code: String, vararg names: String) = names.forEach { put(it.lowercase(), code) }

        // Americas
        add("USD", "dollar", "dollars", "us dollar", "us dollars", "american dollar", "american dollars")
        add("CAD", "canadian dollar", "canadian dollars")
        add("AUD", "australian dollar", "australian dollars")
        add("NZD", "new zealand dollar", "new zealand dollars")
        add("MXN", "mexican peso", "mexican pesos", "peso", "pesos")
        add("BRL", "real", "reais", "brazilian real", "brazilian reais")
        add("ARS", "argentinian peso", "argentinian pesos", "argentine peso", "argentine pesos")
        add("CLP", "chilean peso", "chilean pesos")
        add("COP", "colombian peso", "colombian pesos")
        add("PEN", "sol", "soles", "peruvian sol", "peruvian soles")
        add("BOB", "boliviano", "bolivianos")
        add("PYG", "guarani", "guaranis", "paraguayan guarani")
        add("CRC", "colon", "colones", "costa rican colon")

        // Europe
        add("EUR", "euro", "euros")
        add("GBP", "pound", "pounds", "british pound", "british pounds", "pound sterling", "sterling")
        add("CHF", "franc", "francs", "swiss franc", "swiss francs")
        add("NOK", "norwegian krone", "norwegian kroner", "norwegian krones")
        add("SEK", "swedish krona", "swedish kronor", "swedish krone")
        add("DKK", "danish krone", "danish kroner")
        add("PLN", "zloty", "zlotys", "polish zloty")
        add("CZK", "koruna", "korunas", "czech koruna")
        add("HUF", "forint", "forints", "hungarian forint")
        add("RON", "leu", "lei", "romanian leu")
        add("BGN", "lev", "leva", "bulgarian lev")
        add("HRK", "kuna", "kunas", "croatian kuna")
        add("RSD", "serbian dinar", "serbian dinars")
        add("TRY", "lira", "liras", "lire", "turkish lira", "turkish liras")
        add("RUB", "ruble", "rubles", "rouble", "roubles", "russian ruble", "russian rubles")
        add("UAH", "hryvnia", "hryvnias", "ukrainian hryvnia")
        add("GEL", "lari", "laris", "georgian lari")
        add("AMD", "dram", "drams", "armenian dram")
        add("AZN", "manat", "manats", "azerbaijani manat")

        // Asia-Pacific
        add("JPY", "yen", "japanese yen")
        add("CNY", "yuan", "renminbi", "chinese yuan", "rmb")
        add("TWD", "new taiwan dollar", "taiwan dollar", "taiwan dollars")
        add("HKD", "hong kong dollar", "hong kong dollars")
        add("SGD", "singapore dollar", "singapore dollars")
        add("KRW", "won", "south korean won", "korean won")
        add("INR", "rupee", "rupees", "indian rupee", "indian rupees")
        add("PKR", "pakistani rupee", "pakistani rupees")
        add("LKR", "sri lankan rupee", "sri lankan rupees")
        add("BDT", "taka", "takas", "bangladeshi taka")
        add("NPR", "nepalese rupee", "nepalese rupees", "nepali rupee", "nepali rupees")
        add("MYR", "ringgit", "ringgits", "malaysian ringgit")
        add("IDR", "rupiah", "rupiyas", "indonesian rupiah")
        add("THB", "baht", "thai baht")
        add("VND", "dong", "vietnamese dong")
        add("PHP", "philippine peso", "philippine pesos")
        add("MMK", "kyat", "burmese kyat")
        add("KHR", "riel", "riels", "cambodian riel")
        add("LAK", "kip", "kips", "laotian kip")
        add("MNT", "tugrik", "tugriks", "mongolian tugrik")
        add("KZT", "tenge", "kazakhstani tenge")
        add("UZS", "uzbekistani som", "uzbek som")
        add("KGS", "kyrgyzstani som", "kyrgyz som")
        add("TJS", "somoni", "tajikistani somoni")
        add("TMT", "turkmenistani manat")
        add("AFN", "afghani", "afghan afghani")

        // Middle East & Africa
        add("SAR", "saudi riyal", "saudi riyals", "riyal", "riyals")
        add("AED", "dirham", "dirhams", "uae dirham", "emirati dirham")
        add("QAR", "qatari riyal", "qatari riyals")
        add("KWD", "kuwaiti dinar", "kuwaiti dinars")
        add("BHD", "bahraini dinar", "bahraini dinars")
        add("OMR", "omani rial", "omani rials", "rial", "rials")
        add("JOD", "jordanian dinar", "jordanian dinars")
        add("ILS", "shekel", "shekels", "new shekel", "new shekels", "israeli shekel", "israeli shekels")
        add("EGP", "egyptian pound", "egyptian pounds")
        add("LYD", "libyan dinar", "libyan dinars")
        add("TND", "tunisian dinar", "tunisian dinars")
        add("MAD", "moroccan dirham", "moroccan dirhams")
        add("DZD", "algerian dinar", "algerian dinars")
        add("ZAR", "rand", "south african rand")
        add("NGN", "naira", "nigerian naira")
        add("GHS", "cedi", "cedis", "ghanaian cedi")
        add("KES", "kenyan shilling", "kenyan shillings", "shilling", "shillings")
        add("TZS", "tanzanian shilling", "tanzanian shillings")
        add("UGX", "ugandan shilling", "ugandan shillings")
        add("ETB", "birr", "birrs", "ethiopian birr")

        // Crypto
        add("BTC", "bitcoin")
        add("ETH", "ethereum")
    }

    // Per-symbol regexes pre-compiled once at startup, longest-first for greedy matching.
    // Each entry: (symbol, symBeforeAmountRegex, amountBeforeSymRegex)
    private val SYMBOL_REGEXES: List<Triple<String, Regex, Regex>> =
        SYMBOL_TO_CODE.keys
            .sortedByDescending { it.length }
            .map { sym ->
                val e = Regex.escape(sym)
                Triple(
                    sym,
                    Regex("""^$e\s*(\d+(?:[.,]\d+)?)$"""),
                    Regex("""^(\d+(?:[.,]\d+)?)\s*$e$"""),
                )
            }

    // --- Candidate (cheap) check ---
    private val HAS_DIGIT = Regex("""\d""")
    private val HAS_SEP = Regex("""(?i)\b(?:to|into|in)\b|=""")
    private val CANDIDATE_SYMBOLS = Regex("""[₹€£¥₩₣฿₫₱₦₺₴₸﷼₭₮₲₡₵$]""")
    private val CANDIDATE_CODES = Regex(
        """(?i)\b(?:usd|eur|gbp|inr|jpy|cny|cad|aud|chf|hkd|sgd|nzd|nok|sek|dkk|mxn|zar|try|rub""" +
            """|krw|idr|brl|thb|vnd|php|ngn|egp|uah|pln|czk|huf|ron|myr|bdt|pkr|lkr|sar|aed|ils""" +
            """|kwd|qar|bhd|omr|jod|ars|clp|cop|pen|kes|ghs|btc|eth|twd|gel|azn|kzt|bgn|hrk|rsd""" +
            """|tnd|mad|dzd|etb|afn|npr|mmk|khr|lak|mnt|uzs|kgs|tjs|tmt|lyd|ves|bob|pyg|crc)\b"""
    )
    private val CANDIDATE_NAMES = Regex(
        """(?i)\b(?:dollar|euro|pound|rupee|yen|yuan|won|baht|franc|peso|lira|ruble|dong""" +
            """|ringgit|shekel|dinar|dirham|riyal|rand|real|naira|koruna|forint|leu|taka|shilling""" +
            """|cedi|bitcoin|ethereum|sterling|renminbi|rupiah|hryvnia|manat|tenge|somoni)\b"""
    )

    fun isCandidate(trimmedQuery: String): Boolean {
        if (!HAS_DIGIT.containsMatchIn(trimmedQuery)) return false
        if (!HAS_SEP.containsMatchIn(trimmedQuery)) return false
        return CANDIDATE_SYMBOLS.containsMatchIn(trimmedQuery) ||
            CANDIDATE_CODES.containsMatchIn(trimmedQuery) ||
            CANDIDATE_NAMES.containsMatchIn(trimmedQuery)
    }

    private val OPTIONAL_PREFIX_RE = Regex("""^(?i)(?:convert(?:ion)?(?:\s+of)?\s+|exchange\s+)""")

    // Separator: requires surrounding whitespace for "to/into/in" to avoid matching inside words
    private val SEPARATOR_RE = Regex("""(?i)(?:\s+(?:to|into)\s+|\s+in\s+|\s*=\s*)""")

    fun parseConfirmed(trimmedQuery: String): ConfirmedCurrencyQuery? {
        val cleaned = OPTIONAL_PREFIX_RE.replace(trimmedQuery.trim(), "").trim()
        val sepMatch = SEPARATOR_RE.find(cleaned) ?: return null

        val beforeSep = cleaned.substring(0, sepMatch.range.first).trim()
        val afterSep = cleaned.substring(sepMatch.range.last + 1).trim()

        if (beforeSep.isBlank() || afterSep.isBlank()) return null

        val (amount, fromStr) = extractAmountAndCurrency(beforeSep) ?: return null
        val fromCode = resolveCurrency(fromStr) ?: return null
        val toCode = resolveCurrency(afterSep) ?: return null

        if (fromCode == toCode) return null
        val amountNormalized = amount.replace(",", ".")
        if (amountNormalized.toDoubleOrNull() == null) return null

        return ConfirmedCurrencyQuery(
            amount = amountNormalized,
            fromCurrency = fromCode,
            toCurrency = toCode,
            originalQuery = trimmedQuery.trim(),
        )
    }

    private val AMOUNT_THEN_NAME_RE = Regex("""^(\d+(?:[.,]\d+)?)\s+(.+)$""")

    /**
     * Extracts (amount, currencyString) from strings like:
     *   "₹10", "10₹", "$ 10", "10 inr", "10 indian rupees"
     */
    private fun extractAmountAndCurrency(s: String): Pair<String, String>? {
        for ((sym, symBeforeRe, amountBeforeRe) in SYMBOL_REGEXES) {
            // Symbol before amount (e.g. "₹10", "₹ 10", "HK$ 50")
            symBeforeRe.matchEntire(s)?.let { return it.groupValues[1] to sym }
            // Amount before symbol (e.g. "10₹", "50 HK$")
            amountBeforeRe.matchEntire(s)?.let { return it.groupValues[1] to sym }
        }
        // Amount then name/code separated by whitespace (e.g. "10 inr", "10 indian rupees")
        AMOUNT_THEN_NAME_RE.matchEntire(s)?.let {
            return it.groupValues[1] to it.groupValues[2].trim()
        }
        return null
    }

    /** Resolves a currency string (symbol, code, or name) to an ISO 4217 code. */
    private fun resolveCurrency(s: String): String? {
        val trimmed = s.trim()
        // Exact symbol match (case-sensitive)
        SYMBOL_TO_CODE[trimmed]?.let { return it }
        // Name match (case-insensitive)
        NAME_TO_CODE[trimmed.lowercase()]?.let { return it }
        // 3-letter code assumed valid as-is
        if (trimmed.length == 3 && trimmed.all { it.isLetter() }) return trimmed.uppercase()
        return null
    }
}

data class ConfirmedCurrencyQuery(
    val amount: String,
    val fromCurrency: String,
    val toCurrency: String,
    val originalQuery: String,
)
