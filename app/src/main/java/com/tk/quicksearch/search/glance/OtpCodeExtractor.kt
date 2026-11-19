package com.tk.quicksearch.search.glance

/**
 * Finds a one-time code in a message's text. The approach follows OTP Helper
 * (github.com/jd1378/otphelper): a message only counts when it names a code with a keyword such as
 * "code", "OTP" or "verification", and the code is the candidate nearest that keyword. Links,
 * amounts, dates and card or account endings are never taken as the code.
 */
internal object OtpCodeExtractor {
    /** How far (in characters) a code may sit from the keyword that names it. */
    private const val MAX_KEYWORD_DISTANCE = 120

    fun extract(text: String): String? {
        if (text.isBlank()) return null
        val clean = CLEANUP.replace(normalizeDigits(text), " ")
        if (IGNORED.containsMatchIn(clean)) return null
        val keywords = KEYWORDS.findAll(clean).map { it.range }.toList()
        if (keywords.isEmpty()) return null
        return CANDIDATE.findAll(clean)
            .mapNotNull { match ->
                val group = match.groups[1] ?: return@mapNotNull null
                val start = group.range.first
                val before = clean.substring((start - 24).coerceAtLeast(0), start)
                val after = clean.substring(group.range.last + 1, (group.range.last + 12).coerceAtMost(clean.length))
                if (CURRENCY_BEFORE.containsMatchIn(before) || CURRENCY_AFTER.containsMatchIn(after)) {
                    return@mapNotNull null
                }
                if (ACCOUNT_BEFORE.containsMatchIn(before) || MASKED.matches(group.value)) return@mapNotNull null
                // The candidate is itself a keyword's neighbour, not the keyword (such as "2FA").
                if (keywords.any { it.first <= start && it.last >= group.range.last }) return@mapNotNull null
                val distance =
                    keywords.minOf { keyword ->
                        when {
                            keyword.last < start -> start - keyword.last
                            keyword.first > group.range.last -> keyword.first - group.range.last
                            else -> 0
                        }
                    }
                if (distance > MAX_KEYWORD_DISTANCE) return@mapNotNull null
                // "code is 123456" and "code: 123456" name the code outright.
                val score = if (INTRODUCED.containsMatchIn(before)) distance - MAX_KEYWORD_DISTANCE else distance
                score to group.value
            }.minByOrNull { it.first }
            ?.second
            ?.filter { it.isLetterOrDigit() }
            ?.uppercase()
    }

    /** Arabic-Indic, Persian and full-width digits to ASCII. */
    private fun normalizeDigits(text: String): String =
        buildString(text.length) {
            for (char in text) {
                append(
                    when (char) {
                        in '٠'..'٩' -> '0' + (char - '٠')
                        in '۰'..'۹' -> '0' + (char - '۰')
                        in '０'..'９' -> '0' + (char - '０')
                        else -> char
                    },
                )
            }
        }

    private const val NOT_WORD_BEFORE = "(?<![\\p{L}\\p{N}])"
    private const val NOT_WORD_AFTER = "(?![\\p{L}\\p{N}])"

    /** Letters of scripts that separate words with spaces, for use inside a character class. */
    private const val SPACED_LETTER = "[\\p{L}&&[^\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]]"

    /** Links and domains (their paths often hold digits), and the SMS Retriever `<#>` prefix. */
    private val CLEANUP =
        Regex("""https?://\S+|\b(?:[a-z0-9-]+\.)+[a-z]{2,}(?:/\S*)?|<#>""", RegexOption.IGNORE_CASE)

    private val KEYWORDS =
        Regex(
            listOf(
                // "code" also ends compounds such as Bestätigungscode, verificatiecode and Sicherheitscode.
                "code${NOT_WORD_AFTER}",
                "codes${NOT_WORD_AFTER}",
                "${NOT_WORD_BEFORE}otp",
                "${NOT_WORD_BEFORE}one[- ]?time",
                "${NOT_WORD_BEFORE}pass(?:code|word|key)",
                "${NOT_WORD_BEFORE}pin$NOT_WORD_AFTER",
                "${NOT_WORD_BEFORE}2fa$NOT_WORD_AFTER",
                "${NOT_WORD_BEFORE}m?tan$NOT_WORD_AFTER",
                "${NOT_WORD_BEFORE}v[eé]rif",
                "${NOT_WORD_BEFORE}authenti",
                "${NOT_WORD_BEFORE}c[oó]digo",
                "${NOT_WORD_BEFORE}codice",
                "${NOT_WORD_BEFORE}kod",
                "${NOT_WORD_BEFORE}kode$NOT_WORD_AFTER",
                "${NOT_WORD_BEFORE}clave",
                "${NOT_WORD_BEFORE}contraseña",
                "${NOT_WORD_BEFORE}mot de passe",
                "${NOT_WORD_BEFORE}einmal",
                "код",
                "пароль",
                "验证码",
                "校验码",
                "驗證碼",
                "認証",
                "確認コード",
                "ワンタイム",
                "인증번호",
                "कोड",
                "ओटीपी",
                "కోడ్",
                "ఓటీపీ",
                "رمز",
                "كود",
                "کد",
            ).joinToString("|"),
            RegexOption.IGNORE_CASE,
        )

    /** Promotions that also say "code". */
    private val IGNORED =
        Regex(
            "${NOT_WORD_BEFORE}(?:promo|coupon|discount|referral|voucher|gift ?card|barcode|zip code|postal code)",
            RegexOption.IGNORE_CASE,
        )

    /**
     * 4 to 8 digits, or two groups of 3 or 4 split by a space or hyphen, optionally behind a short
     * prefix like Google's "G-"; or 4 to 10 upper case letters and digits with at least one of each.
     * Chinese, Japanese and Korean don't space words, so their letters may touch a code.
     * A candidate joined to more digits by `.,/-` (or followed by `:` and a digit) is part of an
     * amount, date, time or number.
     */
    private val CANDIDATE =
        Regex(
            "(?<![$SPACED_LETTER\\p{N}.,/-])(?:[A-Z]{1,3}-)?" +
                "(\\d{3,4}[ -]\\d{3,4}|\\d{4,8}|(?=[A-Z0-9]{0,9}\\d)(?=[A-Z0-9]{0,9}[A-Z])[A-Z0-9]{4,10})" +
                "(?![$SPACED_LETTER\\p{N}]|[.,/:-]\\d)",
        )

    private val CURRENCY_BEFORE =
        Regex("""(?:[$€£₹¥]|(?<![\p{L}])(?:rs|inr|usd|eur|gbp)\.?)\s*$""", RegexOption.IGNORE_CASE)
    private val CURRENCY_AFTER = Regex("""^\s*(?:[$€£₹¥]|(?:rs|inr|usd|eur|gbp)(?![\p{L}]))""", RegexOption.IGNORE_CASE)

    /** Card and account endings, such as "card ending 1234" or "a/c no. 1234". */
    private val ACCOUNT_BEFORE =
        Regex(
            """(?:ending(?:\s+(?:in|with))?|a/c|acct|account|card|no\.|[x*]{2,})\s*[:#]?\s*$""",
            RegexOption.IGNORE_CASE,
        )
    private val MASKED = Regex("X{2,}.*")

    private val INTRODUCED = Regex("""(?:(?<![\p{L}])is|[:：]|是|为)\s*["'「]?$""", RegexOption.IGNORE_CASE)
}
