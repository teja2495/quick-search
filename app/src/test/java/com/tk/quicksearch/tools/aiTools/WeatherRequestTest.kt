package com.tk.quicksearch.tools.aiTools

import com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit
import com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRequestTest {
    @Test
    fun requestIncludesSelectedUnits() {
        val query =
            buildWeatherRequestQuery(
                location = "Herndon",
                temperatureUnit = WeatherTemperatureUnit.FAHRENHEIT.promptValue,
                windSpeedUnit = WeatherWindSpeedUnit.MILES_PER_HOUR.promptValue,
            )

        assertTrue(query.contains("Herndon"))
        assertTrue(query.contains("Fahrenheit (°F)"))
        assertTrue(query.contains("miles per hour (mph)"))
    }
}
