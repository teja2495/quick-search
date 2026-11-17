package com.tk.quicksearch.tools.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorUtilsTest {
    @Test
    fun `recognizes arithmetic expressions`() {
        assertTrue(CalculatorUtils.isMathExpression("2+2"))
        assertTrue(CalculatorUtils.isMathExpression(" (3 * 4) / 2 "))
        assertTrue(CalculatorUtils.isMathExpression("6×7"))
        assertTrue(CalculatorUtils.isMathExpression("9÷3"))
        assertTrue(CalculatorUtils.isMathExpression("-5"))
        assertTrue(CalculatorUtils.isMathExpression("3x4"))
        assertTrue(CalculatorUtils.isMathExpression("(2+1) X 5"))
    }

    @Test
    fun `rejects plain numbers text and single characters`() {
        assertFalse(CalculatorUtils.isMathExpression(""))
        assertFalse(CalculatorUtils.isMathExpression("   "))
        assertFalse(CalculatorUtils.isMathExpression("42"))
        assertFalse(CalculatorUtils.isMathExpression("+"))
        assertFalse(CalculatorUtils.isMathExpression("wi-fi"))
        assertFalse(CalculatorUtils.isMathExpression("2+x"))
        assertFalse(CalculatorUtils.isMathExpression("50%"))
        assertFalse(CalculatorUtils.isMathExpression("x-ray"))
        assertFalse(CalculatorUtils.isMathExpression("2x"))
    }

    @Test
    fun `recognizes percent phrases`() {
        assertTrue(CalculatorUtils.isMathExpression("20% of 150"))
        assertTrue(CalculatorUtils.isMathExpression("15 % OFF 80"))
    }

    @Test
    fun `respects operator precedence and brackets`() {
        assertEquals("14", CalculatorUtils.evaluateExpression("2+3*4"))
        assertEquals("20", CalculatorUtils.evaluateExpression("(2+3)*4"))
        assertEquals("2", CalculatorUtils.evaluateExpression("10-4-4"))
        assertEquals("1", CalculatorUtils.evaluateExpression("8/4/2"))
    }

    @Test
    fun `handles unary signs and unicode operators`() {
        assertEquals("-3", CalculatorUtils.evaluateExpression("-(1+2)"))
        assertEquals("6", CalculatorUtils.evaluateExpression("2*-(-3)"))
        assertEquals("42", CalculatorUtils.evaluateExpression("6 × 7"))
        assertEquals("3", CalculatorUtils.evaluateExpression("9 ÷ 3"))
        assertEquals("10", CalculatorUtils.evaluateExpression("2·5"))
        assertEquals("12", CalculatorUtils.evaluateExpression("3x4"))
        assertEquals("15", CalculatorUtils.evaluateExpression("(2+1) X 5"))
        assertEquals("1", CalculatorUtils.evaluateExpression("0.5x2"))
    }

    @Test
    fun `rounds to two decimals and trims trailing zeros`() {
        assertEquals("0.33", CalculatorUtils.evaluateExpression("1/3"))
        assertEquals("2.5", CalculatorUtils.evaluateExpression("5/2"))
        assertEquals("0.3", CalculatorUtils.evaluateExpression("0.1+0.2"))
        assertEquals("100", CalculatorUtils.evaluateExpression("99.999+0.001"))
    }

    @Test
    fun `evaluates percent phrases`() {
        assertEquals("30", CalculatorUtils.evaluateExpression("20% of 150"))
        assertEquals("68", CalculatorUtils.evaluateExpression("15% off 80"))
        assertEquals("1.5", CalculatorUtils.evaluateExpression("0.5% OF 300"))
    }

    @Test
    fun `evaluates percent operator inside expressions`() {
        assertTrue(CalculatorUtils.isMathExpression("100-10%"))
        assertEquals("90", CalculatorUtils.evaluateExpression("100-10%"))
        assertEquals("110", CalculatorUtils.evaluateExpression("100 + 10%"))
        assertEquals("20", CalculatorUtils.evaluateExpression("200*10%"))
        assertEquals("10", CalculatorUtils.evaluateExpression("(50+50)*10%"))
    }

    @Test
    fun `returns null for invalid expressions`() {
        assertNull(CalculatorUtils.evaluateExpression("1/0"))
        assertNull(CalculatorUtils.evaluateExpression("(1+2"))
        assertNull(CalculatorUtils.evaluateExpression("1+2)"))
        assertNull(CalculatorUtils.evaluateExpression("3+"))
        assertNull(CalculatorUtils.evaluateExpression("1..2+1"))
        assertNull(CalculatorUtils.evaluateExpression(""))
    }
}
