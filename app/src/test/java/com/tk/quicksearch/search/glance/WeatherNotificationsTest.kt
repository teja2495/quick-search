package com.tk.quicksearch.search.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherNotificationsTest {
    private fun temperature(text: String) = WeatherNotifications.temperatureIn(text)

    @Test
    fun readsTemperatureWithOrWithoutUnit() {
        assertEquals("72°", temperature("72° Partly cloudy"))
        assertEquals("72°F", temperature("Sunny · 72 °F"))
        assertEquals("18.5°C", temperature("Feels like 18.5°C"))
        assertEquals("-3°", temperature("Snow, −3°"))
    }

    @Test
    fun readsFirstTemperature() {
        assertEquals("75°", temperature("High 75° / Low 60°"))
    }

    @Test
    fun ignoresTextWithoutDegrees() {
        assertNull(temperature("Rain starting in 15 min"))
        assertNull(temperature("Severe thunderstorm warning until 9:00 PM"))
    }
}
