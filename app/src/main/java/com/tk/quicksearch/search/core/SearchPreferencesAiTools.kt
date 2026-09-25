package com.tk.quicksearch.search.core

import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.tools.aiSearch.AiSearchHandler
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tools.aiSearch.resolveModelSelection
import com.tk.quicksearch.settings.settingsDetailScreen.AiBackedToolConfigId
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun SearchPreferencesDelegate.setLlmModel(
        providerId: AiSearchLlmProviderId,
        modelId: String?,
    ) {
        scope.launch(Dispatchers.IO) {
            aiSearchHandler.setSelectedModelId(providerId, modelId)
            var models: List<GeminiTextModel> = emptyList()
            updateFeatureState {
                models = it.availableLlmModelsByProvider[providerId].orEmpty()
                it.copy(
                    aiSearchLlmProviderId = aiSearchHandler.getAiSearchProviderId(),
                    personalContext = aiSearchHandler.getPersonalContext(),
                    geminiModel = aiSearchHandler.getGeminiModel(),
                    geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled(),
                    geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled(),
                    availableGeminiModels = models,
                    availableLlmModelsByProvider =
                        it.availableLlmModelsByProvider + (providerId to models),
                )
            }
            aiSearchHandler.updateAvailableModels(models)
        }
    }

internal fun SearchPreferencesDelegate.setCustomLlmAdvancedPayload(
        providerId: AiSearchLlmProviderId,
        payload: String?,
        enabled: Boolean,
    ) {
        if (!providerId.isCustom) return
        scope.launch(Dispatchers.IO) {
            userPreferences.setCustomLlmAdvancedPayload(providerId, payload, enabled)
            updateFeatureState {
                it.copy(
                    customLlmAdvancedPayloadByProvider =
                        userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                )
            }
        }
    }

internal fun SearchPreferencesDelegate.setAiBackedToolSettings(
        toolId: AiBackedToolConfigId,
        providerId: AiSearchLlmProviderId,
        modelId: String,
        groundingEnabled: Boolean,
        thinkingEnabled: Boolean,
        advancedPayload: String?,
        advancedPayloadEnabled: Boolean,
        systemPrompt: String = "",
        location: String = "",
        temperatureUnit: com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit =
            com.tk.quicksearch.search.data.preferences.WeatherTemperatureUnit.CELSIUS,
        windSpeedUnit: com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit =
            com.tk.quicksearch.search.data.preferences.WeatherWindSpeedUnit.KILOMETERS_PER_HOUR,
    ) {
        scope.launch(Dispatchers.IO) {
            val normalizedModelId = modelId.trim()
            if (normalizedModelId.isBlank()) return@launch
            when (toolId) {
                AiBackedToolConfigId.CURRENCY_CONVERTER -> {
                    userPreferences.setCurrencyConverterProviderId(providerId)
                    userPreferences.setCurrencyConverterModel(normalizedModelId)
                    userPreferences.setCurrencyConverterGroundingEnabled(groundingEnabled)
                    userPreferences.setCurrencyConverterThinkingEnabled(thinkingEnabled)
                    userPreferences.setCurrencyConverterAdvancedPayload(advancedPayload, advancedPayloadEnabled)
                }
                AiBackedToolConfigId.WORD_CLOCK -> {
                    userPreferences.setWorldClockProviderId(providerId)
                    userPreferences.setWorldClockModel(normalizedModelId)
                    userPreferences.setWorldClockGroundingEnabled(groundingEnabled)
                    if (userPreferences.getWorldClockThinkingOverride() != null ||
                        thinkingEnabled != userPreferences.isLlmThinkingEnabled(providerId)
                    ) {
                        userPreferences.setWorldClockThinkingEnabled(thinkingEnabled)
                    }
                    userPreferences.setWorldClockAdvancedPayload(advancedPayload, advancedPayloadEnabled)
                }
                AiBackedToolConfigId.DICTIONARY -> {
                    userPreferences.setDictionaryProviderId(providerId)
                    userPreferences.setDictionaryModel(normalizedModelId)
                    userPreferences.setDictionaryGroundingEnabled(groundingEnabled)
                    if (userPreferences.getDictionaryThinkingOverride() != null ||
                        thinkingEnabled != userPreferences.isLlmThinkingEnabled(providerId)
                    ) {
                        userPreferences.setDictionaryThinkingEnabled(thinkingEnabled)
                    }
                    userPreferences.setDictionaryAdvancedPayload(advancedPayload, advancedPayloadEnabled)
                }
                AiBackedToolConfigId.WEATHER -> {
                    userPreferences.setWeatherProviderId(providerId)
                    userPreferences.setWeatherModel(normalizedModelId)
                    userPreferences.setWeatherGroundingEnabled(true)
                    if (userPreferences.getWeatherThinkingOverride() != null ||
                        thinkingEnabled != userPreferences.isLlmThinkingEnabled(providerId)
                    ) {
                        userPreferences.setWeatherThinkingEnabled(thinkingEnabled)
                    }
                    userPreferences.setWeatherAdvancedPayload(advancedPayload, advancedPayloadEnabled)
                    userPreferences.setWeatherSystemPrompt(systemPrompt)
                    userPreferences.setWeatherLocation(location)
                    userPreferences.setWeatherTemperatureUnit(temperatureUnit)
                    userPreferences.setWeatherWindSpeedUnit(windSpeedUnit)
                    updateFeatureState {
                        val normalizedLocation = location.trim()
                        it.copy(
                            weatherLocationConfigured = normalizedLocation.isNotBlank(),
                            weatherLocation = normalizedLocation,
                        )
                    }
                }
            }
        }
    }

internal fun SearchPreferencesDelegate.setGeminiGroundingEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            aiSearchHandler.setGeminiGroundingEnabled(enabled)
            updateFeatureState { it.copy(geminiGroundingEnabled = enabled) }
        }
    }

internal fun SearchPreferencesDelegate.setGeminiThinkingEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            aiSearchHandler.setGeminiThinkingEnabled(enabled)
            updateFeatureState {
                it.copy(geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled())
            }
        }
    }

internal fun SearchPreferencesDelegate.refreshAvailableGeminiModels() {
        scope.launch(Dispatchers.IO) {
            val configuredProviderIds = userPreferences.getLlmApiKeyLast4ByProvider().keys
            val now = System.currentTimeMillis()
            if (configuredProviderIds == lastModelRefreshProviderIds &&
                now - lastModelRefreshAtMillis < MODEL_REFRESH_MIN_INTERVAL_MS
            ) {
                return@launch
            }
            // Keep the cached catalogs visible while refetching; only providers with no cached
            // list show the loading state.
            val activeProviderId = aiSearchHandler.getAiSearchProviderId()
            val results =
                coroutineScope {
                    configuredProviderIds
                        .associateWith { providerId ->
                            async {
                                val apiKey = userPreferences.getLlmApiKey(providerId)
                                if (apiKey.isNullOrBlank()) {
                                    Result.success(emptyList())
                                } else {
                                    fetchAvailableModels(providerId, apiKey)
                                }
                            }
                        }.mapValues { (_, deferred) -> deferred.await() }
                }
            results.forEach { (providerId, result) ->
                result.getOrNull()?.let { models ->
                    val selectedModelId = userPreferences.getLlmModel(providerId)
                    if (resolveModelSelection(selectedModelId, models) != selectedModelId) {
                        userPreferences.setLlmModel(providerId, null)
                        if (providerId == activeProviderId) {
                            aiSearchHandler.setSelectedModelId(null)
                        }
                    }
                    if (userPreferences.getCurrencyConverterProviderId() == providerId &&
                        userPreferences.getCurrencyConverterModel().let { it.isNotBlank() && models.none { model -> model.id == it } }
                    ) {
                        userPreferences.clearCurrencyConverterModel()
                    }
                    if (userPreferences.getWorldClockProviderId() == providerId &&
                        userPreferences.getWorldClockModel().let { it.isNotBlank() && models.none { model -> model.id == it } }
                    ) {
                        userPreferences.clearWorldClockModel()
                    }
                    if (userPreferences.getDictionaryProviderId() == providerId &&
                        userPreferences.getDictionaryModel().let { it.isNotBlank() && models.none { model -> model.id == it } }
                    ) {
                        userPreferences.clearDictionaryModel()
                    }
                    if (userPreferences.getWeatherProviderId() == providerId &&
                        userPreferences.getWeatherModel().let { it.isNotBlank() && models.none { model -> model.id == it } }
                    ) {
                        userPreferences.clearWeatherModel()
                    }
                }
            }
            val refreshedCustomTools =
                userPreferences.getCustomTools().map { tool ->
                    val models = results[tool.providerId]?.getOrNull() ?: return@map tool
                    if (tool.modelId.isNotBlank() && models.none { it.id == tool.modelId }) {
                        tool.copy(modelId = "")
                    } else {
                        tool
                    }
                }
            userPreferences.setCustomTools(refreshedCustomTools)
            var activeModels: List<GeminiTextModel> = emptyList()
            updateFeatureState {
                // A failed fetch keeps the previously cached catalog instead of blanking it.
                val configuredProviderModels =
                    results.mapValues { (providerId, result) ->
                        result.getOrNull()
                            ?: it.availableLlmModelsByProvider[providerId]
                            ?: emptyList()
                    }
                activeModels = configuredProviderModels[activeProviderId].orEmpty()
                it.copy(
                    geminiModel = aiSearchHandler.getGeminiModel(),
                    customTools = refreshedCustomTools,
                    availableGeminiModels = activeModels,
                    availableLlmModelsByProvider = configuredProviderModels,
                )
            }
            aiSearchHandler.updateAvailableModels(activeModels)
            if (results.values.all { it.isSuccess }) {
                lastModelRefreshProviderIds = configuredProviderIds
                lastModelRefreshAtMillis = now
            }
        }
    }

internal suspend fun SearchPreferencesDelegate.showCustomProviderModelsError(error: Throwable) {
        val app = applicationProvider()
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
        withContext(Dispatchers.Main) {
            Toast.makeText(
                app,
                app.getString(R.string.custom_provider_models_load_failed, detail),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

internal suspend fun SearchPreferencesDelegate.fetchAvailableModels(
        providerId: AiSearchLlmProviderId,
        apiKey: String,
    ): Result<List<GeminiTextModel>> {
        val provider = AiSearchLlmProviderRegistry.get(providerId, applicationProvider())
        return provider
            .fetchAvailableTextModels(apiKey.trim(), applicationProvider())
    }

internal fun SearchPreferencesDelegate.updateBooleanPreference(
        value: Boolean,
        preferenceSetter: (Boolean) -> Unit,
        stateUpdater: (Boolean) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            preferenceSetter(value)
            stateUpdater(value)
        }
    }

internal fun SearchPreferencesDelegate.addCustomTool(
        name: String,
        prompt: String,
        providerId: AiSearchLlmProviderId,
        modelId: String,
        groundingEnabled: Boolean = false,
        aliasCode: String = "",
        thinkingEnabled: Boolean = false,
        advancedPayload: String? = null,
        advancedPayloadEnabled: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val resolvedModelId = resolveCustomToolModelId(providerId, modelId)
            val id = "custom_tool:${java.util.UUID.randomUUID()}"
            val newTool = CustomTool(
                id = id,
                name = trimmedName,
                prompt = prompt.trim(),
                modelId = resolvedModelId,
                providerId = providerId,
                groundingEnabled = groundingEnabled,
                thinkingEnabled = thinkingEnabled,
                advancedPayload = advancedPayload?.trim()?.takeIf { it.isNotEmpty() },
                advancedPayloadEnabled = advancedPayloadEnabled,
            )
            val existing = userPreferences.getCustomTools()
            val updated = existing + newTool
            userPreferences.setCustomTools(updated)
            val normalizedAlias = aliasCode.trim()
            if (normalizedAlias.isNotBlank()) {
                userPreferences.setAliasCode(id, normalizedAlias)
            }
            updateFeatureState { state ->
                state.copy(
                    customTools = updated,
                    shortcutCodes = if (normalizedAlias.isNotBlank()) {
                        state.shortcutCodes + (id to normalizedAlias)
                    } else {
                        state.shortcutCodes
                    },
                )
            }
        }
    }

internal fun SearchPreferencesDelegate.updateCustomTool(
        id: String,
        name: String,
        prompt: String,
        providerId: AiSearchLlmProviderId,
        modelId: String,
        groundingEnabled: Boolean = false,
        thinkingEnabled: Boolean = false,
        advancedPayload: String? = null,
        advancedPayloadEnabled: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val resolvedModelId = resolveCustomToolModelId(providerId, modelId)
            val existing = userPreferences.getCustomTools()
            val updated =
                existing.map { tool ->
                    if (tool.id == id) {
                        tool.copy(
                            name = trimmedName,
                            prompt = prompt.trim(),
                            modelId = resolvedModelId,
                            providerId = providerId,
                            groundingEnabled = groundingEnabled,
                            thinkingEnabled = thinkingEnabled,
                            advancedPayload = advancedPayload?.trim()?.takeIf { it.isNotEmpty() },
                            advancedPayloadEnabled = advancedPayloadEnabled,
                        )
                    } else {
                        tool
                    }
                }
            userPreferences.setCustomTools(updated)
            updateFeatureState { it.copy(customTools = updated) }
        }
    }

internal fun SearchPreferencesDelegate.deleteCustomTool(id: String) {
        scope.launch(Dispatchers.IO) {
            val existing = userPreferences.getCustomTools()
            val updated = existing.filterNot { it.id == id }
            userPreferences.setCustomTools(updated)
            val disabled = userPreferences.getDisabledCustomTools() - id
            userPreferences.setDisabledCustomTools(disabled)
            updateFeatureState { it.copy(customTools = updated, disabledCustomToolIds = disabled) }
        }
    }

internal fun SearchPreferencesDelegate.setCustomToolEnabled(
        id: String,
        enabled: Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            val disabled = userPreferences.getDisabledCustomTools().toMutableSet()
            if (enabled) {
                disabled.remove(id)
            } else {
                disabled.add(id)
            }
            userPreferences.setDisabledCustomTools(disabled)
            updateFeatureState { it.copy(disabledCustomToolIds = disabled) }
        }
    }

internal fun SearchPreferencesDelegate.rerunSecondarySearchIfNeeded() {
        val query = resultsStateProvider().query
        if (query.isNotBlank()) {
            secondarySearchOrchestrator.performSecondarySearches(query)
        }
    }

internal fun SearchPreferencesDelegate.resolveCustomToolModelId(
        providerId: AiSearchLlmProviderId,
        modelId: String,
    ): String {
        val normalizedModelId = modelId.trim()
        if (normalizedModelId.isNotBlank()) return normalizedModelId

        return ""
    }


private const val MODEL_REFRESH_MIN_INTERVAL_MS = 10 * 60 * 1000L
