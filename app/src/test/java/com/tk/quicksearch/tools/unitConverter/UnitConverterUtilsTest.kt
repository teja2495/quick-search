package com.tk.quicksearch.tools.unitConverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UnitConverterUtilsTest {

    @Test
    fun convertsLargeNumbersCorrectly() {
        val result = UnitConverterUtils.convertQuery("10000 metres to miles")
        assertEquals("6.21 miles (mi)", result)
    }

    @Test
    fun trimsTrailingZerosToAtMostTwoDecimals() {
        val result = UnitConverterUtils.convertQuery("1500 meters to kilometers")
        assertEquals("1.5 kilometres (km)", result)
    }

    @Test
    fun convertsTemperatureCelsiusToFahrenheit() {
        val result = UnitConverterUtils.convertQuery("100 celsius to fahrenheit")
        assertEquals("212 \u00B0F", result)
    }

    @Test
    fun convertsTemperatureFahrenheitToCelsius() {
        val result = UnitConverterUtils.convertQuery("32 f to c")
        assertEquals("0 \u00B0C", result)
    }

    @Test
    fun supportsNoSpaceTemperatureInput() {
        val result = UnitConverterUtils.convertQuery("100c to f")
        assertNotNull(result)
        assertEquals("212 \u00B0F", result)
    }

    @Test
    fun supportsKmphAlias() {
        val result = UnitConverterUtils.convertQuery("60 kmph to m/s")
        assertEquals("16.67 metres per second (m/s)", result)
    }

    @Test
    fun supportsDefaultConversionForCompactImperialWeightInput() {
        val result = UnitConverterUtils.convertQuery("5lbs")
        assertEquals("2.27 kilograms (kg)", result)
    }

    @Test
    fun supportsDefaultConversionForCompactTemperatureInput() {
        val result = UnitConverterUtils.convertQuery("2F")
        assertEquals("-16.67 \u00B0C", result)
    }

    @Test
    fun supportsDefaultConversionForOtherCommonCompactInputs() {
        assertEquals("6.21 miles (mi)", UnitConverterUtils.convertQuery("10km"))
        assertEquals("0.91 metres (m)", UnitConverterUtils.convertQuery("3ft"))
        assertEquals("60.96 centimetres (cm)", UnitConverterUtils.convertQuery("24in"))
        assertEquals("340.19 grams (g)", UnitConverterUtils.convertQuery("12oz"))
        assertEquals("7.57 litres (L)", UnitConverterUtils.convertQuery("2gal"))
    }

    @Test
    fun convertsFromInchesWrittenAsIn() {
        assertEquals("25.4 centimetres (cm)", UnitConverterUtils.convertQuery("10 in to cm"))
        assertEquals("0.17 feet (ft)", UnitConverterUtils.convertQuery("2 in to ft"))
        assertEquals("12.7 centimetres (cm)", UnitConverterUtils.convertQuery("5 in in cm"))
        assertEquals("19.35 square centimetres (cm2)", UnitConverterUtils.convertQuery("3 sq in to sq cm"))
        assertEquals("49.16 cubic centimetres (cm3)", UnitConverterUtils.convertQuery("3 cu in to cm3"))
        assertEquals("254 centimetres (cm)", UnitConverterUtils.convertQuery("100 in to cm"))
        assertEquals("39.37 inches (in)", UnitConverterUtils.convertQuery("100 cm to in"))
    }
}
