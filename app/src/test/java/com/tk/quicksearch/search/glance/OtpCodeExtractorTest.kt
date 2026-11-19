package com.tk.quicksearch.search.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpCodeExtractorTest {
    private fun code(text: String) = OtpCodeExtractor.extract(text)

    @Test
    fun readsCodeAfterKeyword() {
        assertEquals("482913", code("Your verification code is 482913. Don't share it with anyone."))
        assertEquals("123456", code("Your WhatsApp code: 123-456"))
        assertEquals("654321", code("Your Instagram code is 654 321"))
    }

    @Test
    fun readsCodeBeforeKeyword() {
        assertEquals("123456", code("G-123456 is your Google verification code."))
        assertEquals("482913", code("Use 482913 to verify your account"))
        assertEquals("7788", code("7788 is your OTP for login"))
    }

    @Test
    fun readsAlphanumericCodesInCapitals() {
        assertEquals("AB12CD", code("Your one-time passcode: AB12CD"))
    }

    @Test
    fun skipsAmountsAndCardEndings() {
        assertEquals(
            "567890",
            code("OTP for txn of INR 5000.00 at AMAZON on HDFC Bank card ending 1234 is 567890. Valid for 10 mins."),
        )
        assertEquals("998877", code("Rs. 2500 debited from a/c XX4321. OTP: 998877"))
    }

    @Test
    fun skipsLinksAndDates() {
        assertEquals("246810", code("Code 246810 expires 2026-09-27. Visit example.com/r/99887766"))
    }

    @Test
    fun readsLocalizedMessages() {
        assertEquals("583920", code("Ihr Bestätigungscode lautet 583920"))
        assertEquals("583920", code("您的验证码是583920，5分钟内有效"))
        assertEquals("583920", code("Ваш код: ٥٨٣٩٢٠"))
    }

    @Test
    fun ignoresMessagesWithoutCodes() {
        assertNull(code("Meeting moved to room 4012 at 3pm"))
        assertNull(code("Use promo code SAVE2026 for 20% off"))
        assertNull(code("Your code is ready for review"))
        assertNull(code("Tracking code 1234567890123"))
        assertNull(code(""))
    }
}
