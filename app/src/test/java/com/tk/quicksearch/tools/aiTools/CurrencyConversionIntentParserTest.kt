package com.tk.quicksearch.tools.aiTools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConversionIntentParserTest {
    @Test
    fun parsesCodesNamesAndSymbolsOnEitherSideOfTheAmount() {
        assertParsed("10 inr to usd", "10", "INR", "USD")
        assertParsed("10 indian rupees to dollars", "10", "INR", "USD")
        assertParsed("₹10 to $", "10", "INR", "USD")
        assertParsed("10₹ to dollar", "10", "INR", "USD")
        assertParsed("₹ 10 to usd", "10", "INR", "USD")
    }

    @Test
    fun multiCharacterSymbolsWinOverTheirSingleCharacterSuffix() {
        assertParsed("HK$ 50 to usd", "50", "HKD", "USD")
        assertParsed("10 C$ to usd", "10", "CAD", "USD")
    }

    @Test
    fun acceptsAlternateSeparatorsAndConvertPrefixes() {
        assertParsed("100 inr in usd", "100", "INR", "USD")
        assertParsed("100 usd into eur", "100", "USD", "EUR")
        assertParsed("100 usd = eur", "100", "USD", "EUR")
        assertParsed("convert 5 gbp to eur", "5", "GBP", "EUR")
        assertParsed("exchange 5 gbp to eur", "5", "GBP", "EUR")
    }

    @Test
    fun commaDecimalIsNormalizedToADot() {
        assertParsed("10,5 eur to usd", "10.5", "EUR", "USD")
    }

    @Test
    fun thousandsSeparatorsAreNotReadAsDecimals() {
        assertParsed("1,000 inr to usd", "1000", "INR", "USD")
        assertParsed("$1,234,567 to eur", "1234567", "USD", "EUR")
        assertParsed("1,234.50 usd to eur", "1234.50", "USD", "EUR")
        assertParsed("1.234,50 eur to usd", "1234.50", "EUR", "USD")
        assertNull(CurrencyConversionIntentParser.parseConfirmed("1,00,0 usd to eur"))
    }

    @Test
    fun onlyRealCurrencyCodesAreAccepted() {
        assertParsed("10 isk to usd", "10", "ISK", "USD")
        assertNull(CurrencyConversionIntentParser.parseConfirmed("10 usd to abc"))
        assertNull(CurrencyConversionIntentParser.parseConfirmed("5 lbs to kgs"))
    }

    @Test
    fun rejectsIncompleteOrSameCurrencyQueries() {
        assertNull(CurrencyConversionIntentParser.parseConfirmed("usd to eur"))
        assertNull(CurrencyConversionIntentParser.parseConfirmed("10 usd to"))
        assertNull(CurrencyConversionIntentParser.parseConfirmed("10 usd to dollars"))
        assertNull(CurrencyConversionIntentParser.parseConfirmed("10 widgets to usd"))
    }

    @Test
    fun candidateScanRequiresDigitSeparatorAndCurrencyHint() {
        assertTrue(CurrencyConversionIntentParser.isCandidate("10 inr to usd"))
        assertTrue(CurrencyConversionIntentParser.isCandidate("€5 in £"))
        assertFalse(CurrencyConversionIntentParser.isCandidate("inr to usd"))
        assertFalse(CurrencyConversionIntentParser.isCandidate("10 inr usd"))
        assertFalse(CurrencyConversionIntentParser.isCandidate("10 apples to oranges"))
    }

    private fun assertParsed(
        query: String,
        amount: String,
        from: String,
        to: String,
    ) {
        val parsed = CurrencyConversionIntentParser.parseConfirmed(query)
        assertEquals(query, ConfirmedCurrencyQuery(amount, from, to, query), parsed)
    }
}
