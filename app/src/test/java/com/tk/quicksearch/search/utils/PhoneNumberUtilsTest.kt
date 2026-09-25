package com.tk.quicksearch.search.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberUtilsTest {
    @Test
    fun formattingDifferencesDoNotMakeNumbersDistinct() {
        assertTrue(PhoneNumberUtils.isSameNumber("(123) 456-7890", "123.456.7890"))
    }

    @Test
    fun countryCodeOnOnlyOneSideStillMatches() {
        assertTrue(PhoneNumberUtils.isSameNumber("+11234567890", "1234567890"))
        assertTrue(PhoneNumberUtils.isSameNumber("1234567890", "+91 12345 67890"))
        assertTrue(PhoneNumberUtils.isSameNumber("+358 1234567890", "1234567890"))
    }

    @Test
    fun differentNumbersDoNotMatch() {
        assertFalse(PhoneNumberUtils.isSameNumber("+11234567890", "+911234567890"))
        assertFalse(PhoneNumberUtils.isSameNumber("11234567890", "1234567890"))
        assertFalse(PhoneNumberUtils.isSameNumber("+11234567890", "9876543210"))
    }

    @Test
    fun cleanKeepsLeadingPlusAndDropsFormatting() {
        assertEquals("+11234567890", PhoneNumberUtils.cleanPhoneNumber(" +1 (123) 456-7890 "))
        assertEquals("1234567890", PhoneNumberUtils.cleanPhoneNumber("123-456-7890"))
    }

    @Test
    fun numbersShorterThanSevenDigitsAreRejected() {
        assertNull(PhoneNumberUtils.cleanPhoneNumber("12-34-56"))
        assertNull(PhoneNumberUtils.cleanPhoneNumber("   "))
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("123456"))
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("123 4567"))
    }
}
