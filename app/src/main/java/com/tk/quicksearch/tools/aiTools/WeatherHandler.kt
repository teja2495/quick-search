package com.tk.quicksearch.tools.aiTools

import android.content.Context
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.LlmRequest
import com.tk.quicksearch.tools.aiSearch.modelSupportsGrounding
import com.tk.quicksearch.tools.aiSearch.prepareWebSearch
import com.tk.quicksearch.tools.aiSearch.providerSupportsNativeSearch

class WeatherHandler(
    private val context: Context,
    private val userPreferences: UserAppPreferences,
) {
    suspend fun getWeather(
        confirmed: ConfirmedWeatherQuery,
    ): Result<Pair<WeatherModelResult, String>> {
        val configuredLocation = userPreferences.getWeatherLocation()
        val location = confirmed.requestedLocation?.trim().orEmpty().ifBlank { configuredLocation }
        if (location.isBlank()) {
            return Result.failure(
                IllegalStateException(context.getString(R.string.weather_error_location_required)),
            )
        }
        val temperatureUnit = userPreferences.getWeatherTemperatureUnit()
        val windSpeedUnit = userPreferences.getWeatherWindSpeedUnit()
        val providerId = userPreferences.getWeatherProviderId()
        val provider = AiSearchLlmProviderRegistry.get(providerId, context)
        val apiKey = userPreferences.getLlmApiKey(providerId)?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return Result.failure(
                IllegalStateException(context.getString(R.string.direct_search_error_no_key)),
            )
        }
        val modelId = userPreferences.getWeatherModel().trim()
        if (modelId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    context.getString(R.string.ai_error_selected_model_unavailable),
                ),
            )
        }
        val advancedPayload = userPreferences.getWeatherAdvancedPayload()
        val weatherQuery =
            buildWeatherRequestQuery(location, temperatureUnit.promptValue, windSpeedUnit.promptValue)
        // Weather always wants fresh web data, so native search is requested unconditionally.
        val webSearch =
            prepareWebSearch(
                userPreferences = userPreferences,
                searchQuery = "current weather forecast $location",
                prompt = weatherQuery,
                nativeSearchSupported =
                    providerSupportsNativeSearch(providerId) &&
                        modelSupportsGrounding(
                            modelId,
                            provider.fallbackTextModels,
                            providerId,
                        ),
                nativeSearchRequested = true,
            )
        return provider.fetchAnswer(
            apiKey = apiKey,
            context = context,
            request =
                LlmRequest(
                    query = webSearch.prompt,
                    modelId = modelId,
                    useGroundingWithGoogleSearch = webSearch.useNativeSearch,
                    thinkingEnabled = userPreferences.isWeatherThinkingEnabled(),
                    useSystemInstruction = true,
                    systemInstruction = userPreferences.getWeatherSystemPrompt(),
                    responseMimeType = "text/plain",
                    advancedPayloadJson = advancedPayload.second.takeIf { advancedPayload.first },
                ),
        ).mapCatching { response ->
            val summary = response.text.trim()
            if (summary.isBlank()) error("empty weather response")
            WeatherModelResult(location = location, summary = summary) to modelId
        }
    }
}

data class WeatherModelResult(
    val location: String,
    val summary: String,
)

internal fun buildWeatherRequestQuery(
    location: String,
    temperatureUnit: String,
    windSpeedUnit: String,
): String =
    "Today's weather for $location. Use $temperatureUnit for temperatures and $windSpeedUnit " +
        "for wind speed. Respond with plain text only."
