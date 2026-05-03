package com.tk.quicksearch.tools.dateCalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateCalculatorUtilsTest {

    @Test
    fun parseTimeDiffQuery_returnsSameDayDuration() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("9:00 AM to 6:00 PM")
        assertNotNull(result)
        assertEquals("9 hours", result?.label)
    }

    @Test
    fun parseTimeDiffQuery_returnsOvernightForwardDuration() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("6:00 PM to 9:00 AM")
        assertNotNull(result)
        assertEquals("15 hours", result?.label)
    }

    @Test
    fun parseTimeDiffQuery_returnsZeroForEqualTimes() {
        val result = DateCalculatorUtils.parseTimeDiffQuery("6:00 PM to 6:00 PM")
        assertNotNull(result)
        assertEquals("0 minutes", result?.label)
    }
}
