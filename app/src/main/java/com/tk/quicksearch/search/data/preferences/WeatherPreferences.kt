package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId

class WeatherPreferences(context: Context) : BasePreferences(context) {
    fun isEnabled(): Boolean = getBooleanPref(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) = setBooleanPref(KEY_ENABLED, enabled)

    fun getLocation(): String = prefs.getString(KEY_LOCATION, "").orEmpty().trim()

    fun setLocation(location: String) {
        prefs.edit().putString(KEY_LOCATION, location.trim()).apply()
    }

    fun getSystemPrompt(): String {
        val storedPrompt = prefs.getString(KEY_SYSTEM_PROMPT, null).orEmpty()
        return storedPrompt
            .takeUnless {
                    it.isBlank() ||
                    it == LEGACY_DEFAULT_SYSTEM_PROMPT ||
                    it == PREVIOUS_DEFAULT_SYSTEM_PROMPT ||
                    it == PREVIOUS_ALERTS_DEFAULT_SYSTEM_PROMPT
            }
            ?: DEFAULT_SYSTEM_PROMPT
    }

    fun setSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt.trim().ifBlank { DEFAULT_SYSTEM_PROMPT }).apply()
    }

    fun getModel(): String = prefs.getString(KEY_MODEL, "").orEmpty()

    fun hasModelPreference(): Boolean = prefs.contains(KEY_MODEL)

    fun setModel(modelId: String) {
        val normalized = modelId.trim()
        if (normalized.isNotEmpty()) prefs.edit().putString(KEY_MODEL, normalized).apply()
    }

    fun clearModel() {
        prefs.edit().putString(KEY_MODEL, "").apply()
    }

    fun getProviderIdOverride(): AiSearchLlmProviderId? =
        prefs.getString(KEY_PROVIDER_ID, null)?.let(AiSearchLlmProviderId::fromStorageValue)

    fun setProviderId(providerId: AiSearchLlmProviderId) {
        prefs.edit().putString(KEY_PROVIDER_ID, providerId.storageValue).apply()
    }

    fun isThinkingEnabled(): Boolean = getBooleanPref(KEY_THINKING_ENABLED, false)

    fun getThinkingOverride(): Boolean? =
        if (prefs.contains(KEY_THINKING_ENABLED)) isThinkingEnabled() else null

    fun setThinkingEnabled(enabled: Boolean) = setBooleanPref(KEY_THINKING_ENABLED, enabled)

    fun getTemperatureUnit(): WeatherTemperatureUnit =
        WeatherTemperatureUnit.fromStorageValue(prefs.getString(KEY_TEMPERATURE_UNIT, null))

    fun setTemperatureUnit(unit: WeatherTemperatureUnit) {
        prefs.edit().putString(KEY_TEMPERATURE_UNIT, unit.storageValue).apply()
    }

    fun getWindSpeedUnit(): WeatherWindSpeedUnit =
        WeatherWindSpeedUnit.fromStorageValue(prefs.getString(KEY_WIND_SPEED_UNIT, null))

    fun setWindSpeedUnit(unit: WeatherWindSpeedUnit) {
        prefs.edit().putString(KEY_WIND_SPEED_UNIT, unit.storageValue).apply()
    }

    fun getAdvancedPayload(): Pair<Boolean, String> =
        getBooleanPref(KEY_ADVANCED_PAYLOAD_ENABLED, false) to
            prefs.getString(KEY_ADVANCED_PAYLOAD, "").orEmpty()

    fun setAdvancedPayload(payload: String?, enabled: Boolean) {
        val normalized = payload?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_ADVANCED_PAYLOAD, normalized)
            .putBoolean(KEY_ADVANCED_PAYLOAD_ENABLED, enabled && normalized.isNotEmpty())
            .apply()
    }

    companion object {
        private const val LEGACY_DEFAULT_SYSTEM_PROMPT =
            "A brief summary of today's weather in a few lines for the specified location."

        private const val PREVIOUS_DEFAULT_SYSTEM_PROMPT =
            "Provide a concise summary of today's weather for the requested location. Start with today's high and low temperatures and the general conditions, such as sunny, rainy, cloudy, or snowy. Then summarize the rest of the day's forecast, including precipitation chance, wind, humidity, and any important weather alerts. Use the requested temperature units."

        private const val PREVIOUS_ALERTS_DEFAULT_SYSTEM_PROMPT =
            "Provide a concise summary of today's weather for the requested location. Start with today's high and low temperatures and the general conditions, such as sunny, rainy, cloudy, or snowy. Then summarize the rest of the day's forecast, including precipitation chance, wind, and humidity. Mention weather alerts only when there are active alerts; otherwise do not mention them. Use the requested temperature units."

        const val DEFAULT_SYSTEM_PROMPT =
            "Provide a concise summary of today's weather for the requested location. Start with today's high and low temperatures and the general conditions, such as sunny, rainy, cloudy, or snowy. Then summarize the rest of the day's forecast, including precipitation chance, wind, and humidity. Mention weather alerts only when there are active alerts; otherwise do not mention them. Use the requested temperature units."

        private const val KEY_ENABLED = "weather_enabled"
        private const val KEY_LOCATION = "weather_location"
        private const val KEY_SYSTEM_PROMPT = "weather_system_prompt"
        private const val KEY_MODEL = "weather_model"
        private const val KEY_PROVIDER_ID = "weather_provider_id"
        private const val KEY_THINKING_ENABLED = "weather_thinking_enabled"
        private const val KEY_TEMPERATURE_UNIT = "weather_temperature_unit"
        private const val KEY_WIND_SPEED_UNIT = "weather_wind_speed_unit"
        private const val KEY_ADVANCED_PAYLOAD = "weather_advanced_payload"
        private const val KEY_ADVANCED_PAYLOAD_ENABLED = "weather_advanced_payload_enabled"
    }
}

enum class WeatherTemperatureUnit(
    val storageValue: String,
    val promptValue: String,
) {
    CELSIUS("celsius", "Celsius (°C)"),
    FAHRENHEIT("fahrenheit", "Fahrenheit (°F)"),
    ;

    companion object {
        fun fromStorageValue(value: String?): WeatherTemperatureUnit =
            entries.firstOrNull { it.storageValue == value } ?: CELSIUS
    }
}

enum class WeatherWindSpeedUnit(
    val storageValue: String,
    val promptValue: String,
) {
    KILOMETERS_PER_HOUR("kilometers_per_hour", "kilometers per hour (km/h)"),
    MILES_PER_HOUR("miles_per_hour", "miles per hour (mph)"),
    METERS_PER_SECOND("meters_per_second", "meters per second (m/s)"),
    KNOTS("knots", "knots (kn)"),
    ;

    companion object {
        fun fromStorageValue(value: String?): WeatherWindSpeedUnit =
            entries.firstOrNull { it.storageValue == value } ?: KILOMETERS_PER_HOUR
    }
}
