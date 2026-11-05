package com.tk.quicksearch.searchEngines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmazonRegionDetectorTest {
    @Test
    fun `maps country codes case-insensitively to regional domains`() {
        assertEquals("amazon.in", AmazonRegionDetector.domainForCountry("in"))
        assertEquals("amazon.co.uk", AmazonRegionDetector.domainForCountry(" GB "))
        assertEquals("amazon.co.jp", AmazonRegionDetector.domainForCountry("jp"))
    }

    @Test
    fun `keeps default for US, unknown, and blank countries`() {
        assertNull(AmazonRegionDetector.domainForCountry("US"))
        assertNull(AmazonRegionDetector.domainForCountry("XX"))
        assertNull(AmazonRegionDetector.domainForCountry(""))
        assertNull(AmazonRegionDetector.domainForCountry(null))
    }

    @Test
    fun `prefers first non-blank country signal`() {
        assertEquals("DE", AmazonRegionDetector.firstCountry("de", "fr", "US"))
        assertEquals("FR", AmazonRegionDetector.firstCountry("", null, "fr"))
        assertNull(AmazonRegionDetector.firstCountry("", " ", null))
    }

    @Test
    fun `every mapped domain passes domain validation`() {
        listOf("AE", "AT", "AU", "BE", "BR", "CA", "DE", "EG", "ES", "FR", "GB", "IE", "IN", "IT", "JP", "MX", "NL", "PL", "SA", "SE", "SG", "TR", "ZA")
            .forEach { country ->
                val domain = AmazonRegionDetector.domainForCountry(country)
                assertTrue("$country -> $domain", domain != null && isValidAmazonDomain(domain))
            }
    }
}
